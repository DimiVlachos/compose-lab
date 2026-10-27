package dev.dimvlachos.lab.core.presentation.components.photocopier

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** A list in the app: its [title], a [summary] of what is on it and a [meta] line. */
@Immutable
internal class CopierItem(
    val id: Int,
    val title: String,
    val summary: String,
    val meta: String,
    /** The list this one was copied from, or 0 for one that isn't a copy. */
    val copyOf: Int = 0,
) {
    val isCopy: Boolean
        get() = copyOf != 0
}

/** Something the copier does that is heard or felt. */
internal enum class CopierEvent {
    Rise,
    LidUp,
    /** The lid shutting. */
    Thunk,
    /** The card landing on the glass. */
    Slap,
    Beep,
    /** The scan's motor, 1.2 s. */
    Hum,
    /** The copy feeding out of the slot. */
    Paper,
    /** The rollers taking a copy back in, .9 s. */
    Feed,
    Lift,
    Settle,
    /** The menu opening. */
    Tick,
    /** A held card opened its menu. */
    HeldOpen,
    /** The copy landed in the list. */
    Landed,
    /** A dragged copy came over the slot. */
    SlotHot,
}

/** What is said to a screen reader; the host words it, with the list's title where there is one. */
internal enum class CopierSaid {
    Duplicated,
    Removed,
    Pinned,
}

/** Where the copier is going: out of sight, its back edge peeking, or up. */
internal enum class CopierMode {
    Hidden,
    Peek,
    Full,
}

/**
 * A CSS transition on the copier's clock: from wherever it is to its target over a time on an
 * easing, as the browser runs `transition` when a class changes.
 */
internal class Tween(initial: Float) {
    private var from = initial
    var target = initial
        private set

    private var at = 0f
    private var ms = 0f
    private var easing: Easing = CssEase

    fun value(now: Float): Float {
        if (ms <= 0f) return target
        val p = ((now - at) / ms).coerceIn(0f, 1f)
        return from + (target - from) * easing.transform(p)
    }

    /** Sets off towards [to]; returns when it will be there. */
    fun to(to: Float, now: Float, ms: Float, easing: Easing): Float {
        if (to == target) return at + this.ms
        from = value(now)
        target = to
        at = now
        this.ms = ms
        this.easing = easing
        return now + ms
    }

    fun snap(v: Float) {
        from = v
        target = v
        ms = 0f
    }
}

/**
 * A card in the list as it is drawn: the prototype's `.card` and its classes, `pressed`, `lifted`,
 * `away` (on the glass, a dashed outline in its place), `ghost` (hidden while its twin flies),
 * `warm` and the toner flecks a fresh copy carries, and its FLIP slide.
 */
@Stable
internal class CopierCard(val item: CopierItem) {
    var away = false
        internal set

    var ghost = false
        internal set

    internal var pressed = false
    internal var lifted = false

    /** Pressed .985, lifted 1.012. */
    val scale = Tween(1f)

    /** The lifted card's shadow, 0 to 1. */
    val lift = Tween(0f)

    /** A copy's warmth: its tint, border and glow, 0 to 1. */
    val warm = Tween(0f)

    /** The toner flecks' opacity (`--sp`). */
    val speck = Tween(0f)

    /** How far its fill is from coming back after the dashed outline: 1 just back, 0 done. */
    val unaway = Tween(0f)

    internal var flipFrom = 0f
    internal var flipAt = 0f
    internal var flipMs = 0f

    /** The FLIP slide: how far it is drawn from its place, in dp. */
    fun slide(now: Float): Float {
        if (flipMs <= 0f || flipFrom == 0f) return 0f
        val p = ((now - flipAt) / flipMs).coerceIn(0f, 1f)
        return if (p >= 1f) 0f else flipFrom * (1 - SheetEase.transform(p))
    }
}

/**
 * A card in flight, drawn on the canvas at its real size scaled by [s], turned [rot], and flipped
 * [flip] (1 face up, -1 face down). [lift] lifts its shadow; [warm] and [speck] are a fresh copy's.
 * One [inSlot] is clipped at the slot's line; one [onGlass] lies under the lid.
 */
internal class Flier(
    val item: CopierItem,
    var x: Float,
    var y: Float,
    var s: Float = 1f,
    var flip: Float = 1f,
    var rot: Float = 0f,
    var lift: Float = 0f,
    var warm: Float = 0f,
    var speck: Float = 0f,
    var inSlot: Boolean = false,
) {
    var onGlass = false
}

/**
 * The photocopier and the list it copies, ported from the prototype's state, `duplicate()`,
 * `feed()`, `pin()`, its FLIP, its card pointer handlers and `step()`. Units are dp, which play the
 * prototype's CSS px; [layout] gives the screen's size and where its list sits.
 *
 * The prototype's `async` sequences are suspend functions here, run on [now], the copier's own
 * clock in ms, which [step] advances: `wait`, `animate` and `later` resume on it, so a test steps
 * them as the screen does. [reset] drops every one still waiting, as the prototype's does.
 */
@Stable
internal class PhotocopierState(
    private val starting: List<CopierItem>,
    /** What a copy's title is marked with: "Weekly meal plan (copy)", "(copy 2)". */
    private val copyWord: String = "copy",
    /** What a copy's meta line starts with. */
    private val justNow: String = "Just now",
) {
    var reducedMotion = false

    /** Hears what the copier does. */
    var onEvent: ((CopierEvent) -> Unit)? = null

    /** Hears what is to be announced, with the list's title. */
    var onSay: ((CopierSaid, String) -> Unit)? = null

    /** Bumped on every [step], so a drawing that reads it redraws each frame. */
    var frame by mutableIntStateOf(0)
        private set

    /** Bumped whenever something sets the copier moving, so a resting frame loop wakes. */
    var wake by mutableIntStateOf(0)
        private set

    /** The cards, in the list's order. */
    val cards = mutableStateListOf<CopierCard>()

    /** The copier's clock, in ms. */
    var now = 0f
        private set

    // ---------- Layout ----------

    var width = 0f
        private set

    var height = 0f
        private set

    /** The list's top edge on the screen, and how tall it shows. */
    var listTop = 0f
        private set

    var listHeight = 0f
        private set

    val cardW: Float
        get() = width - 32

    val cardH = CardH

    var bx = 12f
        private set

    var bw = 0f
        private set

    var bh = 0f
        private set

    var lidW = 0f
        private set

    var lidH = 0f
        private set

    var gw = 0f
        private set

    var gh = 0f
        private set

    var gs = 1f
        private set

    /** The screen is [w] by [h]; its list shows from [top] for [listH]. */
    fun layout(w: Float, h: Float, top: Float, listH: Float) {
        if (w == width && h == height && top == listTop && listH == listHeight) return
        width = w
        height = h
        listTop = top
        listHeight = listH
        bx = 12f
        bw = w - 24
        bh = (h * .4f).coerceIn(236f, 340f).roundToInt().toFloat()
        lidW = bw - 28
        lidH = (bh * .56f).roundToInt().toFloat()
        gw = lidW - 24
        gh = lidH - 24
        gs = min((gw - 26) / cardW, (gh - 26) / cardH)
        copY = copTarget()
        copV = 0f
        scroll = scroll
        frame++
    }

    // ---------- The list's scroll ----------

    private var padded = false
    private var padTok = 0

    /** The list's bottom padding: room to scroll the last card clear while a duplicate runs. */
    val padBottom: Float
        get() = if (padded) padFor() else 120f

    private fun padFor() = max(120f, bh + cardH + 40)

    private fun contentHeight(n: Int = cards.size) = 2 + n * cardH + max(0, n - 1) * Gap + padBottom

    val maxScroll: Float
        get() = max(0f, contentHeight() - listHeight)

    private var scrollState by mutableFloatStateOf(0f)
    private var menuScroll = 0f

    /** How far the list is scrolled, in dp; set, it is kept inside the list, as the browser's. */
    var scroll: Float
        get() = scrollState
        set(value) {
            val v = value.coerceIn(0f, maxScroll)
            if (v == scrollState) return
            scrollState = v
            // The menu is placed against its card, so it goes once the list scrolls under it.
            if (menuFor != 0 && abs(v - menuScroll) > 2) closeMenu()
        }

    /** A finger or a fling scrolls the list by [dy] dp; returns how much it took. */
    fun scrollBy(dy: Float): Float {
        val before = scroll
        scroll = before + dy
        return scroll - before
    }

    /** Where card [i] sits in the list's content, from its top. */
    fun cardTop(i: Int) = 2 + i * (cardH + Gap)

    /** Card [id]'s place on the screen, ignoring any slide it is animating with. */
    fun rectX() = 16f

    fun rectY(id: Int): Float {
        val i = cards.indexOfFirst { it.item.id == id }
        return listTop + cardTop(max(0, i)) - scroll
    }

    fun indexOf(id: Int) = cards.indexOfFirst { it.item.id == id }

    fun card(id: Int): CopierCard? = cards.firstOrNull { it.item.id == id }

    // ---------- The copier ----------

    var copMode = CopierMode.Hidden
        private set

    var copY = 2000f
        private set

    private var copV = 0f
    private var shake = 0f
    private var shakeV = 0f

    fun copTarget(m: CopierMode = copMode) =
        when (m) {
            CopierMode.Full -> height - bh + 12
            CopierMode.Peek -> height - Peek
            CopierMode.Hidden -> height + 34
        }

    /** The machine's top edge, shaken. */
    val by: Float
        get() = copY + shake

    val lx: Float
        get() = bx + LidIn

    val ly: Float
        get() = by + LidTop

    val gx: Float
        get() = lx + GlassIn

    val gy: Float
        get() = ly + GlassIn

    val slotY: Float
        get() = by + Slot

    /** Where a card lies on the glass: against the registration arrow in the back-left corner. */
    val glassX: Float
        get() = gx + 11

    val glassY: Float
        get() = gy + 11

    /** The lid, 0 shut to 1 open; the scan bar's travel, 0 to 1; the light's strength. */
    var lid = 0f
        private set

    var bar = 0f
        private set

    var scan = 0f
        private set

    /** Start lit. */
    var start = 0f
        private set

    /** A dragged copy is over the slot. */
    var hot = 0f
        private set

    var busy by mutableStateOf(false)
        private set

    val fliers = ArrayList<Flier>()

    // ---------- Chrome round the list ----------

    /** The card whose menu is open, or 0. */
    var menuFor by mutableIntStateOf(0)
        private set

    var menuX = 0f
        private set

    var menuY = 0f
        private set

    /** When the menu opened, for its pop. */
    var menuAt = 0f
        private set

    /** The Undo bar is up. */
    var snackOn by mutableStateOf(false)
        private set

    val snack = Tween(0f)
    val scrim = Tween(0f)

    /** The copy the Undo bar takes back, or 0. */
    var lastCopy = 0
        private set

    /** While the copier peeks after a duplicate, a drag on a copy feeds it. */
    var feedable = false
        private set

    private var snackTok = 0
    private var epoch = 0
    private var uid = 0
    private var quietAt = 0f

    private fun emit(e: CopierEvent) = onEvent?.invoke(e)

    private fun say(what: CopierSaid, title: String = "") = onSay?.invoke(what, title)

    private fun nudge() {
        wake++
        frame++
    }

    private fun Tween.go(to: Float, ms: Float, easing: Easing = CssEase) {
        quietAt = max(quietAt, to(to, now, ms, easing))
    }

    private fun CopierCard.restyle() {
        val s =
            when {
                lifted -> 1.012f
                pressed -> .985f
                else -> 1f
            }
        scale.go(s, 150f)
        if (lifted) lift.go(1f, 150f) else lift.go(0f, 2400f, CssEaseOut)
    }

    private fun CopierCard.setPressed(on: Boolean) {
        if (pressed == on) return
        pressed = on
        restyle()
    }

    private fun CopierCard.setLifted(on: Boolean) {
        if (lifted == on) return
        lifted = on
        restyle()
    }

    private fun CopierCard.setAway(on: Boolean) {
        away = on
        // On, it goes at once; off, its fill and border fade back as `.card`'s transition has it.
        if (on) unaway.snap(1f) else unaway.go(0f, 2400f, CssEaseOut)
    }

    // ---------- The sim clock's scheduler ----------

    private class Timer(val at: Float, val fn: (() -> Unit)?, val cont: Continuation<Unit>?)

    private class Anim(
        val t0: Float,
        val ms: Float,
        val fn: (Float) -> Unit,
        val cont: Continuation<Unit>,
    )

    /** A sequence started with [launch]; [join] waits for it, as `await` on its promise. */
    private class Task {
        var done = false
        val waiters = ArrayList<Continuation<Unit>>(1)
    }

    private val timers = ArrayList<Timer>()
    private val anims = ArrayList<Anim>()
    private val ready = ArrayList<Continuation<Unit>>()
    private val due = ArrayList<Timer>()
    private val running = ArrayList<Anim>()

    private suspend fun wait(ms: Float) = suspendCoroutine { timers += Timer(now + ms, null, it) }

    private fun later(ms: Float, fn: () -> Unit) = Timer(now + ms, fn, null).also { timers += it }

    private fun cancel(t: Timer?) {
        if (t != null) timers.remove(t)
    }

    private suspend fun animate(ms: Float, fn: (Float) -> Unit) = suspendCoroutine {
        // Its first pose at once: this frame is drawn after the step that started it, where the
        // prototype's first draw comes after its first tick.
        fn(0f)
        anims += Anim(now, ms, fn, it)
    }

    private fun launch(block: suspend () -> Unit): Task {
        val task = Task()
        block.startCoroutine(
            Continuation(EmptyCoroutineContext) { r ->
                r.getOrThrow()
                task.done = true
                val w = ArrayList(task.waiters)
                task.waiters.clear()
                for (c in w) c.resume(Unit)
            },
        )
        return task
    }

    private suspend fun Task.join() {
        if (!done) suspendCoroutine { waiters += it }
    }

    // ---------- FLIP, on the copier's clock ----------

    private val before = HashMap<Int, Float>()

    /** Runs [mutate] and slides every card that moved on screen from where it was. */
    private suspend fun flip(ms: Float, mutate: () -> Unit) {
        // Positions on screen, so a mutation that also scrolls the list slides instead of jumping.
        before.clear()
        for ((i, c) in cards.withIndex()) before[c.item.id] = cardTop(i) - scroll
        mutate()
        scroll = scroll
        var moved = false
        for ((i, c) in cards.withIndex()) {
            val b = before[c.item.id] ?: continue
            val d = b - (cardTop(i) - scroll)
            if (d != 0f) {
                moved = true
                c.flipFrom = d
                c.flipAt = now
                c.flipMs = ms
            }
        }
        if (reducedMotion || !moved) {
            for (c in cards) c.flipFrom = 0f
            return
        }
        quietAt = max(quietAt, now + ms)
        animate(ms) {}
        for (c in cards) if (now >= c.flipAt + c.flipMs) c.flipFrom = 0f
    }

    // ---------- Actions ----------

    private fun byId(id: Int): CopierItem? = card(id)?.item

    /** "Weekly meal plan (copy)", then "(copy 2)", never a name already in the list. */
    fun copyName(title: String): String {
        val m = Regex("^(.*) \\(${Regex.escape(copyWord)}(?: (\\d+))?\\)$").find(title)
        val base = m?.groupValues?.get(1) ?: title
        var n = 1
        while (true) {
            val name = "$base ($copyWord${if (n > 1) " $n" else ""})"
            if (cards.none { it.item.title == name }) return name
            n++
        }
    }

    private suspend fun lidTo(to: Float, ms: Float) {
        val from = lid
        if (reducedMotion) {
            lid = to
            return
        }
        if (to > from) emit(CopierEvent.LidUp)
        animate(ms) { p -> lid = lerp(from, to, if (to > from) backOut(p) else easeIn(p)) }
        lid = to
        if (to < from) {
            emit(CopierEvent.Thunk)
            shakeV += 140
        }
    }

    private fun land(card: CopierCard) {
        card.ghost = false
        card.warm.snap(1f)
        card.speck.snap(.62f)
        later(1700f) {
            card.warm.go(0f, 2400f, CssEaseOut)
            card.speck.go(0f, 2600f, CssEaseOut)
        }
    }

    private fun insertAfter(original: CopierCard, copy: CopierCard) {
        cards.add(cards.indexOf(original) + 1, copy)
    }

    /** Duplicates list [id]: it goes through the copier and its copy lands right under it. */
    fun duplicate(id: Int) {
        nudge()
        launch { runDuplicate(id) }
    }

    private suspend fun runDuplicate(id: Int) {
        val c0 = card(id) ?: return
        val d0 = c0.item
        if (busy) return
        closeMenu()
        endUndoWindow()
        val tok = epoch
        val alive = { tok == epoch }
        busy = true
        val copy =
            CopierItem(
                ++uid,
                copyName(d0.title),
                d0.summary,
                "$justNow · ${d0.meta.split(" · ")[0]}",
                d0.id,
            )
        if (reducedMotion) {
            insertAfter(c0, CopierCard(copy))
            busy = false
            lastCopy = copy.id
            startUndoWindow()
            say(CopierSaid.Duplicated, d0.title)
            return
        }
        scrim.go(1f, 300f)
        copMode = CopierMode.Full
        emit(CopierEvent.Rise)
        pad()
        // Scroll first if the original, or the copy's place under it, would end up behind the
        // raised copier (or its open lid).
        val s0 = scroll
        val r0 = rectY(id)
        val clear = copTarget(CopierMode.Full) - 44
        var dy = if (r0 < listTop + 4) r0 - listTop - 4 else max(0f, r0 + 2 * cardH + 8 - clear)
        dy = dy.coerceIn(-s0, maxScroll - s0)
        if (abs(dy) > 1) animate(240f) { p -> scroll = s0 + dy * ease(p) } else wait(240f)
        if (!alive()) return
        val fromX = rectX()
        val fromY = rectY(id)
        launch { lidTo(1f, 420f) }
        wait(250f)
        if (!alive()) return
        // The card itself goes to the glass, face down; its place in the list keeps a dashed
        // outline.
        c0.setAway(true)
        val fo = Flier(d0, fromX, fromY)
        fliers += fo
        emit(CopierEvent.Lift)
        animate(640f) { p ->
            val e = ease(p)
            val arc = sin(PI.toFloat() * p)
            fo.x = lerp(fromX, glassX, e)
            fo.y = lerp(fromY, glassY, e) - 30 * arc
            fo.s = lerp(1f, gs, e) * (1 + .07f * arc)
            fo.flip = cos(PI.toFloat() * e)
            fo.lift = arc
            fo.rot = .05f * arc
        }
        if (!alive()) return
        fo.onGlass = true
        fo.lift = 0f
        fo.rot = 0f
        emit(CopierEvent.Slap)
        wait(140f)
        if (!alive()) return
        lidTo(0f, 340f)
        if (!alive()) return
        wait(150f)
        if (!alive()) return
        start = 1f
        emit(CopierEvent.Beep)
        wait(150f)
        if (!alive()) return
        scan = 1f
        emit(CopierEvent.Hum)
        animate(800f) { p -> bar = ease(p) }
        if (!alive()) return
        scan = .5f
        animate(340f) { p -> bar = 1 - ease(p) }
        if (!alive()) return
        scan = 0f
        bar = 0f
        start = 0f
        // The copy feeds out of the slot along the back edge, warm.
        val fc = Flier(copy, 0f, 0f, s = gs, warm = 1f, speck = 1f, inSlot = true)
        fliers += fc
        emit(CopierEvent.Paper)
        animate(520f) { p ->
            fc.x = glassX
            fc.y = slotY - (cardH * gs + 4) * easeOut(p)
        }
        if (!alive()) return
        val cc = CopierCard(copy).apply { ghost = true }
        val gap = launch { flip(440f) { insertAfter(c0, cc) } }
        scrim.go(0f, 300f)
        val c0x = fc.x
        val c0y = fc.y
        val c0s = fc.s
        fc.inSlot = false
        // Meanwhile the lid lifts and the original flips back up into its place.
        val back = launch { flipBack(c0, fo, alive) }
        emit(CopierEvent.Lift)
        animate(600f) { p ->
            val e = ease(p)
            val arc = sin(PI.toFloat() * p)
            val toY = rectY(copy.id)
            fc.x = lerp(c0x, rectX(), e)
            fc.y = lerp(c0y, toY, e) - 26 * arc
            fc.s = lerp(c0s, 1f, e) * (1 + .05f * arc)
            fc.lift = arc
            fc.rot = -.05f * arc
        }
        if (!alive()) return
        fliers.remove(fc)
        land(cc)
        emit(CopierEvent.Settle)
        emit(CopierEvent.Landed)
        back.join()
        gap.join()
        if (!alive()) return
        lidTo(0f, 280f)
        if (!alive()) return
        copMode = CopierMode.Peek
        busy = false
        lastCopy = copy.id
        startUndoWindow()
        say(CopierSaid.Duplicated, d0.title)
    }

    private suspend fun flipBack(c0: CopierCard, fo: Flier, alive: () -> Boolean) {
        wait(200f)
        if (!alive()) return
        lidTo(1f, 320f)
        if (!alive()) return
        fo.onGlass = false
        val g0x = fo.x
        val g0y = fo.y
        val g0s = fo.s
        val toY = rectY(c0.item.id)
        emit(CopierEvent.Lift)
        animate(560f) { p ->
            val e = ease(p)
            val arc = sin(PI.toFloat() * p)
            fo.x = lerp(g0x, rectX(), e)
            fo.y = lerp(g0y, toY, e) - 24 * arc
            fo.s = lerp(g0s, 1f, e) * (1 + .06f * arc)
            fo.flip = -cos(PI.toFloat() * e)
            fo.lift = arc
        }
        if (!alive()) return
        fliers.remove(fo)
        c0.setAway(false)
        emit(CopierEvent.Settle)
    }

    /** Where a fed copy starts from: where it was let go, at its scale and turn. */
    private class Pose(val x: Float, val y: Float, val s: Float, val rot: Float)

    /** Feeds copy [id] back into the slot it came out of; the list closes up behind it. */
    private fun feed(id: Int, from: Pose? = null) {
        nudge()
        launch { runFeed(id, from) }
    }

    private suspend fun runFeed(id: Int, from: Pose?) {
        val c = card(id) ?: return
        if (busy) return
        val tok = epoch
        val alive = { tok == epoch }
        busy = true
        snackTok++
        setSnack(false)
        if (reducedMotion) {
            cards.remove(c)
            scroll = scroll
            copMode = CopierMode.Hidden
            busy = false
            lastCopy = 0
            say(CopierSaid.Removed)
            return
        }
        if (copMode == CopierMode.Hidden) copMode = CopierMode.Peek
        val start = from ?: Pose(rectX(), rectY(id), 1f, 0f)
        c.ghost = true
        val f = Flier(c.item, start.x, start.y, start.s, rot = start.rot, lift = 1f, speck = .3f)
        fliers += f
        animate(400f) { p ->
            val e = ease(p)
            f.x = lerp(start.x, glassX, e)
            f.y = lerp(start.y, slotY - cardH * gs - 2, e)
            f.s = lerp(start.s, gs, e)
            f.rot = lerp(start.rot, 0f, e)
            f.lift = 1 - e * .85f
        }
        if (!alive()) return
        f.inSlot = true
        f.lift = 0f
        emit(CopierEvent.Feed)
        val y0 = f.y
        // The rollers catch it after a beat, then pull it in faster and faster.
        animate(480f) { p -> f.y = y0 + (cardH * gs + 6) * easeIn(p) }
        if (!alive()) return
        fliers.remove(f)
        flip(380f) { cards.remove(c) }
        if (!alive()) return
        copMode = CopierMode.Hidden
        feedable = false
        busy = false
        lastCopy = 0
        hot = 0f
        launch { unpad() }
        say(CopierSaid.Removed)
    }

    /** Moves list [id] to the top; not while the copier is at work. */
    fun pin(id: Int) {
        if (busy) return
        val c = card(id) ?: return
        nudge()
        closeMenu()
        launch {
            flip(460f) {
                cards.remove(c)
                cards.add(0, c)
                scroll = 0f
            }
        }
        emit(CopierEvent.Settle)
        say(CopierSaid.Pinned, c.item.title)
    }

    /** Undo: the last copy is fed back into the slot. Returns the original's id, or 0. */
    fun undo(): Int {
        val c = card(lastCopy) ?: return 0
        feed(c.item.id)
        return c.item.copyOf
    }

    private fun setSnack(on: Boolean) {
        snackOn = on
        snack.go(if (on) 1f else 0f, 200f)
    }

    private fun startUndoWindow() {
        val t = ++snackTok
        setSnack(true)
        if (!reducedMotion) feedable = true
        later(5200f) { if (t == snackTok) endUndoWindow() }
    }

    private fun endUndoWindow() {
        snackTok++
        setSnack(false)
        feedable = false
        lastCopy = 0
        if (copMode == CopierMode.Peek) {
            copMode = CopierMode.Hidden
            launch { unpad() }
        }
    }

    // Extra room at the end of the list while a duplicate runs, so even the last list can scroll
    // clear of the raised copier with its copy under it; it goes again when the copier leaves,
    // the list easing back first so nothing jumps.
    private fun pad() {
        padTok++
        padded = true
    }

    private suspend fun unpad() {
        if (!padded) return
        val t = ++padTok
        val extra = padFor() - 120
        val s0 = scroll
        val s1 = max(0f, min(s0, contentHeight() - extra - listHeight))
        if (s1 < s0 - .5f) {
            animate(if (reducedMotion) 1f else 360f) { p ->
                if (t == padTok) scroll = lerp(s0, s1, SheetEase.transform(p))
            }
        }
        if (t == padTok) {
            padded = false
            scroll = scroll
        }
    }

    // ---------- Menu ----------

    /** Opens list [id]'s menu under it (or over it, near the foot of the screen). */
    fun openMenu(id: Int, byHold: Boolean = false) {
        if (busy) return
        if (menuFor == id) return
        closeMenu()
        val c = card(id) ?: return
        nudge()
        menuFor = id
        menuScroll = scroll
        c.setLifted(true)
        val ry = rectY(id)
        var top = ry + cardH + 6
        if (top + MenuH > height - 12) top = ry - MenuH - 6
        menuX = max(12f, rectX() + cardW - MenuW - 4)
        menuY = top
        menuAt = now
        quietAt = max(quietAt, now + 160)
        emit(CopierEvent.Tick)
        if (byHold) emit(CopierEvent.HeldOpen)
    }

    fun closeMenu() {
        if (menuFor == 0) return
        nudge()
        card(menuFor)?.setLifted(false)
        menuFor = 0
    }

    /** The ⋯ on list [id]: opens its menu, or closes it when it is the one open. */
    fun tapMore(id: Int) {
        if (menuFor == id) closeMenu() else openMenu(id)
    }

    // ---------- Input ----------

    private class Drag(val ox: Float, val oy: Float, val f: Flier)

    private class Press(
        val id: Int,
        val x0: Float,
        val y0: Float,
        val scripted: Boolean,
    ) {
        var x = x0
        var y = y0
        var moved = false
        var long = false
        var timer: Timer? = null
        var drag: Drag? = null
    }

    private var press: Press? = null

    /** A copy can be dragged into the slot: while the copier peeks after a duplicate. */
    fun canFeed(id: Int): Boolean {
        val d = byId(id) ?: return false
        return d.isCopy && copMode == CopierMode.Peek && !busy && !reducedMotion
    }

    private fun nearSlot(y: Float) = y > slotY - 70

    /** ([x], [y]) on the screen is on the open menu. */
    fun onMenu(x: Float, y: Float) =
        menuFor != 0 && x in menuX..(menuX + MenuW) && y in menuY..(menuY + MenuH)

    /** ([x], [y]) on the screen is on a card's ⋯: 40 dp, 5 dp in from its top-right corner. */
    fun onMore(x: Float, y: Float): Boolean {
        if (y < listTop || y > listTop + listHeight) return false
        val i =
            cards.indices.firstOrNull { i ->
                val top = listTop + cardTop(i) - scroll
                y in top..(top + cardH)
            } ?: return false
        return onMoreOf(cards[i].item.id, x - rectX(), y - (listTop + cardTop(i) - scroll))
    }

    /** ([x], [y]) inside card [id] is on its ⋯. */
    @Suppress("UNUSED_PARAMETER")
    fun onMoreOf(id: Int, x: Float, y: Float) = x in (cardW - 45)..(cardW - 5) && y in 5f..45f

    /** Something on the screen other than the menu or a ⋯ was pressed: the menu closes first. */
    fun pressElsewhere() = closeMenu()

    /**
     * A finger came down on list [id] at ([x], [y]) on the screen. Returns whether the press was
     * taken; a [scripted] one has no hold timer, as the script opens the menu itself.
     */
    fun cardDown(id: Int, x: Float, y: Float, scripted: Boolean = false): Boolean {
        if (!scripted && press?.scripted == true) releaseScripted()
        if (busy || press != null) return false
        val c = card(id) ?: return false
        nudge()
        val p = Press(id, x, y, scripted)
        press = p
        c.setPressed(true)
        // On the copier's clock, as the prototype's hold.
        if (!scripted) {
            p.timer =
                later(LongMs) {
                    if (press === p && !p.moved) {
                        p.long = true
                        c.setPressed(false)
                        openMenu(id, byHold = true)
                    }
                }
        }
        return true
    }

    fun cardMove(x: Float, y: Float) {
        val p = press ?: return
        p.x = x
        p.y = y
        if (!p.moved && hypot(x - p.x0, y - p.y0) > Slop) {
            p.moved = true
            cancel(p.timer)
            card(p.id)?.setPressed(false)
            if (!p.long && canFeed(p.id)) startDrag(p)
        }
        if (p.drag != null) moveDrag(p, x, y)
        nudge()
    }

    private fun startDrag(p: Press) {
        val item = byId(p.id) ?: return
        val rx = rectX()
        val ry = rectY(p.id)
        val f = Flier(item, rx, ry, lift = 1f, speck = .3f)
        p.drag = Drag(p.x0 - rx, p.y0 - ry, f)
        fliers += f
        card(p.id)?.ghost = true
        // The Undo bar steps aside so it never sits over the copy or the slot.
        snackTok++
        setSnack(false)
        emit(CopierEvent.Lift)
    }

    private fun moveDrag(p: Press, x: Float, y: Float) {
        val d = p.drag ?: return
        d.f.x = x - d.ox
        d.f.y = y - d.oy
        d.f.rot = ((x - p.x0) / 700).coerceIn(-.1f, .1f)
        val was = hot
        hot = if (nearSlot(y)) 1f else 0f
        if (hot > was) emit(CopierEvent.SlotHot)
    }

    fun cardUp(x: Float, y: Float) {
        val p = press ?: return
        nudge()
        press = null
        cancel(p.timer)
        card(p.id)?.setPressed(false)
        // Decided from the release point, so a swipe with no moves in between still counts.
        if (p.drag == null && !p.long && hypot(x - p.x0, y - p.y0) > Slop && canFeed(p.id)) {
            startDrag(p)
        }
        val d = p.drag ?: return
        moveDrag(p, x, y)
        hot = 0f
        val f = d.f
        if (nearSlot(y)) {
            fliers.remove(f)
            feed(p.id, Pose(f.x, f.y, 1f, f.rot))
            return
        }
        // Anywhere else: the copy glides back to its place and the Undo window starts over.
        val f0x = f.x
        val f0y = f.y
        val f0r = f.rot
        val tok = epoch
        launch {
            animate(if (reducedMotion) 1f else 320f) { q ->
                val e = easeOut(q)
                f.x = lerp(f0x, rectX(), e)
                f.y = lerp(f0y, rectY(p.id), e)
                f.rot = lerp(f0r, 0f, e)
                f.lift = 1 - e
            }
            if (tok != epoch) return@launch
            fliers.remove(f)
            card(p.id)?.ghost = false
            emit(CopierEvent.Settle)
            if (lastCopy != 0) {
                startUndoWindow()
            } else {
                later(1600f) {
                    if (!busy && lastCopy == 0 && copMode == CopierMode.Peek) {
                        copMode = CopierMode.Hidden
                        launch { unpad() }
                    }
                }
            }
        }
    }

    /** The system took the pointer (the list scrolled): it finishes from where it last was. */
    fun cardCancel() {
        val p = press ?: return
        cardUp(p.x, p.y)
    }

    /** A scripted press the user took over lets go without doing anything. */
    fun releaseScripted() {
        val p = press ?: return
        if (!p.scripted) return
        press = null
        card(p.id)?.setPressed(false)
        p.drag?.let {
            fliers.remove(it.f)
            card(p.id)?.ghost = false
            hot = 0f
        }
        nudge()
    }

    /**
     * The script's long press: held long enough, so the menu opens as the prototype's tour does.
     */
    fun holdScripted() {
        val p = press ?: return
        p.long = true
        card(p.id)?.setPressed(false)
        openMenu(p.id)
    }

    // ---------- Loop ----------

    /** Nothing is moving or waiting: the frame loop may sleep until [wake] changes. */
    val settled: Boolean
        get() =
            timers.isEmpty() &&
                anims.isEmpty() &&
                ready.isEmpty() &&
                now >= quietAt &&
                abs(copY - copTarget()) < .05f &&
                abs(copV) < .05f &&
                abs(shake) < .01f &&
                abs(shakeV) < .05f

    /**
     * Nothing moves until a timer is due: how long until the next one, in ms (infinite with none),
     * so the frame loop can sleep; null while anything is moving.
     */
    val idleMs: Float?
        get() {
            if (anims.isNotEmpty() || ready.isNotEmpty() || now < quietAt || !settledPose()) {
                return null
            }
            var next = Float.POSITIVE_INFINITY
            for (t in timers) next = min(next, t.at - now)
            return max(0f, next)
        }

    fun step(dt: Float) {
        val ms = dt * 1000
        now += ms
        due.clear()
        for (t in timers) if (t.at <= now) due += t
        val fired = due.isNotEmpty() || ready.isNotEmpty()
        timers.removeAll(due)
        for (t in due) {
            t.fn?.invoke()
            t.cont?.let { ready += it }
        }
        running.clear()
        running.addAll(anims)
        for (a in running) {
            val p = ((now - a.t0) / a.ms).coerceIn(0f, 1f)
            a.fn(p)
            if (p >= 1f) {
                anims.remove(a)
                ready += a.cont
            }
        }
        var left = dt
        while (left > 0f) {
            val h = min(left, 1f / 240f)
            physics(h)
            left -= h
        }
        if (settledPose()) {
            copY = copTarget()
            copV = 0f
            shake = 0f
            shakeV = 0f
        }
        // As the prototype's promises resolve after the frame's own work.
        while (ready.isNotEmpty()) {
            val c = ready.removeAt(0)
            c.resume(Unit)
        }
        // While only a timer is pending nothing on screen moves: the cards and the copier are
        // left as drawn, rather than every one of them drawn again each frame.
        if (fired || idleMs == null) frame++
    }

    private fun settledPose() =
        abs(copY - copTarget()) < .05f &&
            abs(copV) < .05f &&
            abs(shake) < .01f &&
            abs(shakeV) < .05f

    private fun physics(h: Float) {
        val t = copTarget()
        if (reducedMotion) {
            copY = t
            copV = 0f
        } else {
            val a = -260 * (copY - t) - 2 * sqrt(260f) * .82f * copV
            copV += a * h
            copY += copV * h
        }
        val sa = -1100 * shake - 34 * shakeV
        shakeV += sa * h
        shake += shakeV * h
    }

    /** Back to the six lists, the copier out of sight and nothing on its way. */
    fun reset() {
        epoch++
        timers.clear()
        anims.clear()
        ready.clear()
        fliers.clear()
        press?.let { cancel(it.timer) }
        press = null
        lid = 0f
        bar = 0f
        scan = 0f
        start = 0f
        hot = 0f
        busy = false
        menuFor = 0
        lastCopy = 0
        snackOn = false
        snack.snap(0f)
        scrim.snap(0f)
        snackTok++
        feedable = false
        padTok++
        padded = false
        copMode = CopierMode.Hidden
        copY = copTarget()
        copV = 0f
        shake = 0f
        shakeV = 0f
        cards.clear()
        cards.addAll(starting.map { CopierCard(it) })
        uid = starting.maxOfOrNull { it.id } ?: 0
        scrollState = 0f
        nudge()
    }

    /** The lists are as they began, in their order, and nothing is under way. */
    val asBegun: Boolean
        get() =
            !busy &&
                fliers.isEmpty() &&
                copMode == CopierMode.Hidden &&
                cards.map { it.item.id } == starting.map { it.id }

    init {
        // Last, once every list and the scheduler above exist.
        reset()
    }

    internal companion object {
        const val CardH = 72f
        const val Gap = 8f
        const val LidIn = 14f
        const val LidTop = 26f
        const val GlassIn = 12f
        const val Slot = 11f
        const val Peek = 46f
        const val Slop = 10f
        const val LongMs = 450f
        const val MenuW = 184f
        const val MenuH = 102f

        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

        fun ease(t: Float) = if (t < .5f) 2 * t * t else 1 - (-2 * t + 2).pow(2) / 2

        fun easeOut(t: Float) = 1 - (1 - t).pow(3)

        fun easeIn(t: Float) = t * t * t

        fun backOut(t: Float): Float {
            val c1 = 1.3f
            val c3 = c1 + 1
            return 1 + c3 * (t - 1).pow(3) + c1 * (t - 1).pow(2)
        }
    }
}

/** CSS `ease`. */
internal val CssEase = CubicBezierEasing(.25f, .1f, .25f, 1f)

/** CSS `ease-out`. */
internal val CssEaseOut = CubicBezierEasing(0f, 0f, .58f, 1f)

/** The prototype's sheet curve, which its FLIP slides on. */
internal val SheetEase = CubicBezierEasing(.2f, .9f, .25f, 1f)
