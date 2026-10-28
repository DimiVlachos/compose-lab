package dev.dimvlachos.lab.core.presentation.components.graveundo

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

/** One task on the bakery's list: its [title], the line under it ([sub]) and its [time]. */
internal class GraveTask(val title: String, val sub: String, val time: String)

/** What a grave is doing, as the prototype's `phase`. */
internal enum class GravePhase {
    Fly,
    Rest,
    Rise,
    Held,
    Leave,
    Undo,
    Land,
    Gone,
}

/** What the grave says under the buried task's name, crossfading as it changes. */
internal enum class GraveStatus {
    None,
    LaidToRest,
    TapToUndo,
    BackFromGrave,
    RestInPeace,
}

/** What the list does that is heard, felt or said. */
internal enum class GraveEvent {
    /** A swipe passed the line where letting go buries the task. */
    Arm,

    /** A task was swiped off and is flying to the grave. */
    Buried,

    /** It landed in the soil. */
    Thud,

    /** The hand starts up through the dirt. */
    Rise,

    /** The hand handed it back. */
    Restored,
}

/** A task on the list, with how its row is drawn right now. */
@Stable
internal class GraveItem(val task: GraveTask, val index: Int) {
    var done by mutableStateOf(false)
    var gone = false
        internal set

    /** How far the row has closed up, 0 open to 1 shut; [opening] while an undo opens it again. */
    var collapse = 0f
        internal set

    var opening = false
        internal set

    /** The row's face, dragged this far across, in dp. */
    var dx = 0f
        internal set

    /** -1 while it is dragged left, 1 right, 0 at rest: which "Bury" label shows under it. */
    var side = 0
        internal set

    var armed = false
        internal set

    /** The two labels' opacity and the armed one's grow, eased as the prototype's transitions. */
    var labelL = 0f
        internal set

    var labelR = 0f
        internal set

    var armK = 0f
        internal set

    /** Its face is drawn by the grave, not the list. */
    var faceHidden = false
        internal set

    internal var snapFrom = 0f
    internal var snapT = -1f
}

/** A task in the ground, as the prototype's grave object. */
internal class Grave(
    val item: GraveItem,
    val dir: Float,
    val startX: Float,
    val startY: Float,
    val done: Boolean,
) {
    var phase = GravePhase.Fly
        internal set

    var t = 0f
        internal set

    var rise = 0f
        internal set

    var sway = 0f
        internal set

    var wig = 0f
        internal set

    var engr = 0f
        internal set

    var alpha = 1f
        internal set

    internal var thud = false
    internal var fast = false
    internal var fading = false
    internal var riseFrom = 0f
    internal var engrFrom = 0f
    internal var fromX = 0f
    internal var fromY = 0f
    internal var fromRot = 0f
    internal var toX = 0f
    internal var toY = 0f
}

/** Where a card is drawn: its centre, scale, tilt and how lifted it looks (corners, shadow). */
internal class CardPose {
    var x = 0f
    var y = 0f
    var s = 1f
    var rot = 0f
    var lift = 0f
}

/**
 * The grave undo, as the prototype runs it, in dp and simulated milliseconds: the list of tasks,
 * each swiped row's drag, the graves (a task flying in, resting, held up by the hand, sinking or
 * flying home), the pill-shaped graveyard that slides up where the snackbar goes, the dirt and the
 * hint. [step] moves it all on; [frame] changes each time, for the canvas to redraw.
 *
 * The motion is the prototype's, about 1.3 times slower and eased in and out throughout.
 */
@Stable
internal class GraveUndoState(
    private val tasks: List<GraveTask>,
    private val random: Random = Random.Default,
) {
    var frame by mutableLongStateOf(0L)
        private set

    /** Changes when a task is buried, goes for good, comes back or is ticked. */
    var listVersion by mutableIntStateOf(0)
        private set

    var reducedMotion = false

    /** While the script plays, the hint stays down. */
    var scripted = false

    /** Hears what happens; set by the list. */
    var onEvent: ((GraveEvent, GraveItem?) -> Unit)? = null

    var items: List<GraveItem> = emptyList()
        private set

    val graves = mutableListOf<Grave>()

    /** The last grave that sank by itself, whose name stays on the stone as it slides away. */
    var lastGone: Grave? = null
        private set

    // ---------- Layout, in dp ----------

    var width = 0f
        private set

    var height = 0f
        private set

    /** The prototype's `u`: everything in the grave scales with the screen's width. */
    var u = 1f
        private set

    var listTop = 0f
        private set

    var rowH = RowH
        private set

    /** How much bigger the rows' text is, from the font setting. */
    var textScale = 1f
        private set

    private var bottomInset = 0f

    /** How far the list is scrolled up, when it is taller than the room above the grave. */
    var scroll = 0f
        private set

    fun layout(width: Float, height: Float, listTop: Float, bottomInset: Float, fontScale: Float) {
        this.width = width
        this.height = height
        this.listTop = listTop
        this.bottomInset = bottomInset
        textScale = fontScale.coerceIn(1f, 1.6f)
        rowH = RowH * textScale
        u = (width / 390f).coerceIn(.85f, 1.2f)
        scroll = scroll.coerceIn(0f, maxScroll())
        bump()
    }

    // ---------- The grave's place ----------

    var pillV = 0f
        private set

    private var pillWant = false
    private var pillFrom = 0f
    private var pillT = 1e9f

    val slotX
        get() = 12f

    val slotW
        get() = width - 24f

    val slotH
        get() = 100f

    /** The grave's top when it is all the way up. */
    val slotY0
        get() = height - bottomInset - 12f - slotH

    /** How far below its place it is, as it slides up or away. */
    val slide
        get() = (1 - ease(pillV.coerceIn(0f, 1f))) * (slotH + 30f)

    val slotY
        get() = slotY0 + slide

    val soil
        get() = slotY + round(slotH * .72f)

    /** Where the headstone, the hand and the text stand across the grave. */
    val stoneX
        get() = slotX + 46 * u

    val handX
        get() = slotX + 96 * u

    val textX
        get() = slotX + 140 * u

    // ---------- Status text ----------

    var status = GraveStatus.None
        private set

    var statusPrev = GraveStatus.None
        private set

    /** How far the crossfade from [statusPrev] to [status] has gone, 0 to 1. */
    var statusK = 1f
        private set

    private var statusT = 1e9f

    // ---------- Hint, dirt, hit targets ----------

    var hint = 0f
        private set

    private var idleSince = 0f

    /** Simulated time, in ms. */
    var simNow = 0f
        private set

    val crumbs = Crumbs()

    /** Where a tap on the hand undoes, in dp; empty while no hand is up. */
    var handHitOn = false
        private set

    var handHitX = 0f
        private set

    var handHitY = 0f
        private set

    var handHitW = 0f
        private set

    var handHitH = 0f
        private set

    // ---------- Drag ----------

    private var drag: Drag? = null
    val dragging
        get() = drag != null

    private class Drag(
        val item: GraveItem?,
        val x0: Float,
        val y0: Float,
        val scroll0: Float,
        val scripted: Boolean,
    ) {
        var dx = 0f
        var lock = 0 // 0 undecided, 1 across (the row), 2 up and down (the list)
        var armed = false
        val xs = FloatArray(4)
        val ts = DoubleArray(4)
        var n = 0

        fun sample(x: Float, t: Double) {
            if (n == 4) {
                for (i in 0 until 3) {
                    xs[i] = xs[i + 1]
                    ts[i] = ts[i + 1]
                }
                n = 3
            }
            xs[n] = x
            ts[n] = t
            n++
        }
    }

    private val inHand = CardPose()

    /**
     * The task a tap on the grave would bring back, for screen readers and keys: it changes as a
     * grave comes to rest or goes, which recomposes the grave's button.
     */
    var undoItem by mutableStateOf<GraveItem?>(null)
        private set

    init {
        reset()
    }

    /** Every task back on the list, nothing in the ground. */
    fun reset() {
        items = tasks.mapIndexed { i, t -> GraveItem(t, i) }
        graves.clear()
        crumbs.clear()
        drag = null
        pillV = 0f
        pillWant = false
        pillFrom = 0f
        pillT = 1e9f
        lastGone = null
        status = GraveStatus.None
        statusPrev = GraveStatus.None
        statusK = 1f
        scroll = 0f
        idleSince = simNow
        handHitOn = false
        undoItem = null
        listVersion++
        bump()
    }

    /** As it began: every task there, unticked, and nothing in the ground. */
    val asBegun
        get() =
            graves.isEmpty() &&
                items.size == tasks.size &&
                items.none { it.gone || it.done || it.dx != 0f }

    // ---------- The list ----------

    /** Tasks still on the list: not gone, and not in a grave that isn't bringing them back. */
    val liveItems: List<GraveItem>
        get() = items.filter { it ->
            !it.gone &&
                graves.none { g ->
                    g.item === it && g.phase != GravePhase.Undo && g.phase != GravePhase.Land
                }
        }

    val liveCount
        get() = liveItems.size

    /** Nothing left on the list and nothing in the ground. */
    val empty
        get() = items.all { it.gone } && graves.isEmpty()

    /** The top of [item]'s row now, as the rows above it close up or open. */
    fun rowTop(item: GraveItem): Float {
        var y = listTop + 1f - scroll
        for (it in items) {
            if (it === item) return y
            if (!it.gone) y += rowH * (1 - it.collapse)
        }
        return y
    }

    /** The row at ([x], [y]), if any is there and can be dragged. */
    fun rowAt(y: Float): GraveItem? {
        if (y < listTop + 1f) return null
        var top = listTop + 1f - scroll
        for (it in items) {
            if (it.gone) continue
            val h = rowH * (1 - it.collapse)
            if (y >= top && y < top + h) return it
            top += h
        }
        return null
    }

    /** The bottom of the last row. */
    private fun contentBottom(): Float {
        var y = listTop + 1f
        for (it in items) if (!it.gone) y += rowH * (1 - it.collapse)
        return y
    }

    // The list scrolls only when its rows run down under the grave's top.
    private fun maxScroll() = max(0f, contentBottom() - (slotY0 - 8f))

    fun toggleDone(item: GraveItem) {
        item.done = !item.done
        listVersion++
        bump()
    }

    // ---------- Graves ----------

    private fun busy() = graves.any {
        it.phase == GravePhase.Fly || it.phase == GravePhase.Undo || it.phase == GravePhase.Land
    }

    /** The grave whose hand a tap would take the task back from, if there is one. */
    fun undoable(): Grave? {
        for (i in graves.indices.reversed()) {
            val g = graves[i]
            if (
                g.phase == GravePhase.Rest ||
                    g.phase == GravePhase.Rise ||
                    g.phase == GravePhase.Held
            )
                return g
        }
        return null
    }

    /**
     * Swipes [item] off to the grave, flying out [dir] (-1 left, 1 right). Refused while another
     * task is flying, and then its row springs back.
     */
    fun bury(item: GraveItem, dir: Float): Boolean {
        if (busy() || item.gone || graves.any { it.item === item }) {
            snapBack(item)
            return false
        }
        val top = rowTop(item)
        // Any earlier grave still showing (even one already sinking on its own) clears its carving
        // fast, so two names never overlap on the stone.
        for (g in graves) {
            if (
                g.phase == GravePhase.Rest ||
                    g.phase == GravePhase.Rise ||
                    g.phase == GravePhase.Held ||
                    (g.phase == GravePhase.Leave && !g.fast)
            )
                leave(g, true)
        }
        val g =
            Grave(
                item,
                if (dir == 0f) -1f else sign(dir),
                width / 2 + item.dx,
                top + rowH / 2,
                item.done,
            )
        item.faceHidden = true
        item.dx = 0f
        item.snapT = -1f
        graves += g
        // A name still fading off a stone that's sliding away eases out rather than vanishing.
        lastGone?.fading = true
        onEvent?.invoke(GraveEvent.Buried, item)
        listVersion++
        bump()
        return true
    }

    private fun leave(g: Grave, fast: Boolean) {
        g.phase = GravePhase.Leave
        g.t = 0f
        g.fast = fast
        g.riseFrom = g.rise
        g.engrFrom = g.engr
    }

    /** The hand gives the task back: it flies home and the list opens a gap for it. */
    fun undo(): Boolean {
        val g = undoable() ?: return false
        if (g.phase == GravePhase.Rest) {
            g.fromX = handX
            g.fromY = soil - 2 * u
            g.fromRot = 0f
        } else {
            cardInHand(g, inHand)
            g.fromX = inHand.x
            g.fromY = inHand.y
            g.fromRot = inHand.rot
        }
        g.phase = GravePhase.Undo
        g.t = 0f
        g.riseFrom = g.rise
        g.engrFrom = g.engr
        g.toX = width / 2
        g.toY = rowTop(g.item) + rowH / 2
        onEvent?.invoke(GraveEvent.Restored, g.item)
        listVersion++
        bump()
        return true
    }

    private fun finalize(g: Grave) {
        g.item.gone = true
        listVersion++
    }

    // ---------- Poses ----------

    /** The hand's wrist, under the soil while it is down: from the grave's own origin. */
    fun wrist(g: Grave): Float {
        val hu = u * HU
        return -30 * hu + (1 - g.rise) * 82 * hu
    }

    fun sway(g: Grave) = if (reducedMotion) 0f else g.sway

    /** Where the card the hand holds up is, into [out]. */
    fun cardInHand(g: Grave, out: CardPose) {
        val sw = sway(g)
        val cy = wrist(g) - 37 * u * HU
        val c = cos(sw)
        val s = sin(sw)
        out.x = handX - s * cy
        out.y = soil + c * cy
        out.s = CardS
        out.rot = sw + if (reducedMotion) 0f else .05f * sin(simNow * .0011f + 1)
        out.lift = 1f
    }

    /** Where a card flying into the ground is, [e] of the way. */
    fun flyPos(g: Grave, e: Float, out: CardPose) {
        val p0x = g.startX
        val p0y = g.startY
        val p3x = handX
        val p3y = slotY0 + round(slotH * .72f) + rowH * CardS / 2 + 4
        val p1x = p0x + g.dir * 44 * u
        val p1y = p0y - 46 * u
        val p2x = p3x
        val p2y = p3y - 96 * u
        bezier(p0x, p0y, p1x, p1y, p2x, p2y, p3x, p3y, e, out)
        out.s = lerp(1f, CardS, smooth(0f, .72f, e))
        out.rot = g.dir * .16f * sin(PI.toFloat() * min(1f, e / .86f))
        out.lift = smooth(0f, .22f, e)
    }

    /** Where a card flying home is, [e] of the way. */
    fun undoPos(g: Grave, e: Float, out: CardPose) {
        bezier(
            g.fromX,
            g.fromY,
            g.fromX,
            g.fromY - 90 * u,
            g.toX - g.dir * 30 * u,
            g.toY + 70 * u,
            g.toX,
            g.toY,
            e,
            out,
        )
        out.s = lerp(CardS, 1f, smooth(.15f, 1f, e))
        out.rot = lerp(g.fromRot, 0f, smooth(0f, .8f, e)) - g.dir * .1f * sin(PI.toFloat() * e)
        out.lift = 1 - smooth(.8f, 1f, e)
    }

    /** How far through its flight a grave flying in is, eased. */
    fun flyE(g: Grave) = ease((g.t / D.fly).coerceIn(0f, 1f))

    fun undoE(g: Grave) = ease((g.t / D.undo).coerceIn(0f, 1f))

    // ---------- Drag on a row ----------

    /**
     * A finger down at ([x], [y]) at [t] ms: on a row it may drag it, elsewhere scroll the list.
     * The script's finger is [scripted]; a real one first lets go of whatever the script held.
     */
    fun down(x: Float, y: Float, t: Double, scripted: Boolean = false) {
        if (!scripted) dropScripted()
        val item = rowAt(y)?.takeIf { it -> graves.none { g -> g.item === it } }
        drag = Drag(item, x, y, scroll, scripted).also { it.sample(x, t) }
    }

    /** Lets go of the script's drag, if it has one, as a cancelled touch: the row springs back. */
    fun dropScripted() {
        if (drag?.scripted == true) up(cancel = true)
    }

    fun move(x: Float, y: Float, t: Double, scripted: Boolean = false) {
        val d = drag ?: return
        if (d.scripted != scripted) return
        val dx = x - d.x0
        val dy = y - d.y0
        if (d.lock == 0) {
            if (d.item != null && abs(dx) > 8 && abs(dx) > abs(dy)) d.lock = 1
            else if (abs(dy) > 10) d.lock = if (maxScroll() > 0f) 2 else 3
        }
        when (d.lock) {
            1 -> {
                val it = d.item ?: return
                d.dx = dx
                d.sample(x, t)
                it.dx = dx
                it.side = if (dx > 0) 1 else -1
                it.snapT = -1f
                val armed = abs(dx) > armT()
                if (armed != d.armed) {
                    d.armed = armed
                    it.armed = armed
                    if (armed) onEvent?.invoke(GraveEvent.Arm, it)
                }
            }
            2 -> scroll = (d.scroll0 - dy).coerceIn(0f, maxScroll())
        }
        bump()
    }

    /**
     * The finger lifts: a row dragged past the line, or flicked, is buried; short, it springs back.
     */
    fun up(cancel: Boolean = false, scripted: Boolean = false): Boolean {
        val d = drag ?: return false
        if (d.scripted != scripted && !cancel) return false
        drag = null
        val it = d.item
        if (d.lock != 1 || it == null) {
            bump()
            return d.lock == 0 && it != null
        }
        val a = max(0, d.n - 3)
        val b = d.n - 1
        val v = if (d.ts[b] > d.ts[a]) ((d.xs[b] - d.xs[a]) / (d.ts[b] - d.ts[a])).toFloat() else 0f
        val flick = abs(v) > .55f && abs(d.dx) > 36 && sign(v) == sign(d.dx)
        if (!cancel && (d.armed || flick)) bury(it, sign(d.dx)) else snapBack(it, d.dx)
        return false
    }

    /** The task a press that stayed a tap was on, if it was. */
    fun tapped(x: Float, y: Float): GraveItem? = rowAt(y)

    private fun armT() = max(88f, width * .3f)

    private fun snapBack(item: GraveItem, from: Float = item.dx) {
        item.armed = false
        item.snapFrom = from
        item.snapT = 0f
        if (from == 0f) {
            item.snapT = -1f
            item.side = 0
        }
        bump()
    }

    // ---------- Stepping ----------

    /** Nothing is moving: the frame loop can sleep until a touch or the script moves something. */
    val settled
        get() =
            drag == null &&
                graves.isEmpty() &&
                pillV <= 0f &&
                crumbs.n == 0 &&
                items.none { it.snapT >= 0f || it.labelL > 0f || it.labelR > 0f || it.armK > 0f } &&
                abs(hint - hintTarget()) < .002f &&
                (scripted || (simNow - idleSince) > 1700f || liveItems.isEmpty())

    /** Moves everything on by [dt] seconds. */
    fun step(dt: Float) {
        var left = dt.coerceIn(0f, .15f)
        while (left > 1e-6f) {
            val h = min(Step, left)
            stepGraves(h)
            crumbs.step(h, soil, u)
            stepItems(h)
            simNow += h * 1000
            statusT += h * 1000
            left -= h
        }
        statusK = smooth(0f, D.text, statusT)
        if (drag != null || graves.isNotEmpty()) idleSince = simNow
        val want = hintTarget()
        hint += (want - hint) * (1 - (1 - .08f).pow(dt * 60))
        if (abs(hint - want) < .002f) hint = want
        updateHandHit()
        bump()
    }

    /**
     * Brings [undoItem] up to date. Called after each frame rather than in it, so the grave's
     * button recomposes on the next frame.
     */
    fun syncUndoItem() {
        val target = if (slotOn) undoable()?.item else null
        if (target !== undoItem) undoItem = target
    }

    private fun hintTarget(): Float {
        val idle = (simNow - idleSince) / 1000
        val want =
            !scripted &&
                drag == null &&
                graves.isEmpty() &&
                pillV < .01f &&
                liveItems.isNotEmpty() &&
                idle > 1.6f
        return if (want) 1f else 0f
    }

    private fun stepItems(dt: Float) {
        val ms = dt * 1000
        for (it in items) {
            if (it.snapT >= 0f) {
                it.snapT += ms
                val p = (it.snapT / D.snap).coerceIn(0f, 1f)
                it.dx = it.snapFrom * (1 - ease(p))
                if (p >= 1f) {
                    it.dx = 0f
                    it.snapT = -1f
                    it.side = 0
                }
            }
            // The labels fade in .2 s and the armed one grows in .3 s, as the prototype's
            // transitions; with reduced motion they jump.
            val wantL = if (it.side == 1) (if (it.armed) 1f else .7f) else 0f
            val wantR = if (it.side == -1) (if (it.armed) 1f else .7f) else 0f
            val wantA = if (it.armed) 1f else 0f
            it.labelL = approach(it.labelL, wantL, ms / D.label)
            it.labelR = approach(it.labelR, wantR, ms / D.label)
            it.armK = approach(it.armK, wantA, ms / D.arm)
        }
    }

    private fun approach(v: Float, to: Float, by: Float) =
        if (reducedMotion) to else if (v < to) min(to, v + by) else max(to, v - by)

    private fun stepGraves(dt: Float) {
        val ms = dt * 1000
        for (g in graves) {
            g.t += ms
            val held = g.phase == GravePhase.Held || g.phase == GravePhase.Rise
            g.sway = g.rise * .07f * sin(simNow * .0017f)
            val wave = max(0f, sin(simNow * .0011f - .6f)).pow(8)
            g.wig =
                when {
                    g.phase == GravePhase.Rise -> .9f
                    g.phase == GravePhase.Undo -> 1.1f
                    held -> .22f + .8f * wave
                    else -> .3f
                }
            when (g.phase) {
                GravePhase.Fly -> {
                    val p = (g.t / D.fly).coerceIn(0f, 1f)
                    val e = ease(p)
                    setCollapse(g.item, ease(smooth(0f, .8f, p)), false)
                    g.engr = smooth(.8f, 1f, e)
                    if (!g.thud && e > .86f) {
                        g.thud = true
                        onEvent?.invoke(GraveEvent.Thud, g.item)
                        dirt(10, 1f)
                    }
                    if (p >= 1f) {
                        g.phase = GravePhase.Rest
                        g.t = 0f
                    }
                }
                GravePhase.Rest ->
                    if (g.t >= D.rest) {
                        g.phase = GravePhase.Rise
                        g.t = 0f
                        onEvent?.invoke(GraveEvent.Rise, g.item)
                        dirt(8, .8f)
                    }
                GravePhase.Rise -> {
                    val p = (g.t / D.rise).coerceIn(0f, 1f)
                    g.rise = ease(p)
                    if (p < .5f && random.nextFloat() < dt * 14) dirt(1, .6f)
                    if (p >= 1f) {
                        g.phase = GravePhase.Held
                        g.t = 0f
                    }
                }
                GravePhase.Held -> if (g.t >= D.hold) leave(g, false)
                GravePhase.Leave -> {
                    val l = if (g.fast) D.leaveFast else D.leave
                    val p = (g.t / l).coerceIn(0f, 1f)
                    g.rise = g.riseFrom * (1 - ease(p))
                    if (g.fast) g.engr = g.engrFrom * (1 - smooth(0f, 1f, p))
                    if (p >= 1f) {
                        g.phase = GravePhase.Gone
                        finalize(g)
                        if (!g.fast) lastGone = g
                    }
                }
                GravePhase.Undo -> {
                    val p = (g.t / D.undo).coerceIn(0f, 1f)
                    setCollapse(g.item, 1 - ease(smooth(0f, .92f, p)), true)
                    g.rise = g.riseFrom * (1 - ease(smooth(.22f, .9f, p)))
                    g.engr = g.engrFrom * (1 - smooth(.2f, .8f, p))
                    if (p >= 1f) {
                        val it = g.item
                        it.collapse = 0f
                        it.opening = false
                        it.faceHidden = false
                        it.side = 0
                        it.armed = false
                        it.labelL = 0f
                        it.labelR = 0f
                        it.armK = 0f
                        g.phase = GravePhase.Land
                        g.t = 0f
                        listVersion++
                    }
                }
                GravePhase.Land -> {
                    g.alpha = 1 - (g.t / D.land).coerceIn(0f, 1f)
                    if (g.t >= D.land) g.phase = GravePhase.Gone
                }
                GravePhase.Gone -> Unit
            }
        }
        graves.removeAll { it.phase == GravePhase.Gone }
        lastGone?.let { lg ->
            if (lg.fading) {
                lg.engr = max(0f, lg.engr - ms / 500f)
                if (lg.engr <= 0f) lastGone = null
            }
        }
        // The grave stays up while anything is in it; it slides away once the last hand is mostly
        // down.
        val want = graves.any {
            it.phase == GravePhase.Fly ||
                it.phase == GravePhase.Rest ||
                it.phase == GravePhase.Rise ||
                it.phase == GravePhase.Held ||
                (it.phase == GravePhase.Undo && it.t < D.undo * .62f) ||
                (it.phase == GravePhase.Leave && !it.fast && it.t < D.leave * .62f)
        }
        if (want != pillWant) {
            pillWant = want
            pillFrom = pillV
            pillT = 0f
        }
        pillT += ms
        pillV = lerp(pillFrom, if (want) 1f else 0f, ease((pillT / D.pill).coerceIn(0f, 1f)))
        if (graves.isEmpty() && pillV < .001f) {
            pillV = if (want) pillV else 0f
            lastGone = null
        }
        val s = statusFor()
        if (s != status && s != GraveStatus.None) {
            statusPrev = status
            status = s
            statusT = 0f
        }
    }

    private fun statusFor(): GraveStatus {
        val g = graves.lastOrNull() ?: return status
        return when (g.phase) {
            GravePhase.Fly,
            GravePhase.Rest -> GraveStatus.LaidToRest
            GravePhase.Rise,
            GravePhase.Held -> GraveStatus.TapToUndo
            GravePhase.Undo,
            GravePhase.Land -> GraveStatus.BackFromGrave
            else -> GraveStatus.RestInPeace
        }
    }

    /** The buried task the grave's text names: the newest, or the one that just sank. */
    val named: Grave?
        get() = graves.lastOrNull() ?: lastGone

    private fun setCollapse(item: GraveItem, c: Float, opening: Boolean) {
        item.collapse = c
        item.opening = opening
    }

    private fun dirt(n: Int, k: Float) {
        if (reducedMotion) return
        repeat(n) {
            crumbs.add(
                handX + (random.nextFloat() - .5f) * 34 * u,
                soil - 3 * u,
                (random.nextFloat() - .5f) * 90 * u * k,
                -(50 + random.nextFloat() * 120) * u * k,
                (.8f + random.nextFloat() * 1.5f) * u,
                random.nextFloat() < .5f,
            )
        }
    }

    private fun updateHandHit() {
        handHitOn = false
        for (g in graves) {
            if (g.rise > .35f && (g.phase == GravePhase.Rise || g.phase == GravePhase.Held)) {
                cardInHand(g, inHand)
                handHitOn = true
                handHitX = inHand.x - 64 * u
                handHitY = inHand.y - 20 * u
                handHitW = 128 * u
                handHitH = soil - inHand.y + 20 * u
            }
        }
    }

    /** The grave can be tapped to undo: something is in it and it is mostly up. */
    val slotOn
        get() = undoable() != null && pillV > .5f

    private fun bump() {
        frame++
    }

    /** The prototype's soft timings, in simulated ms, about 1.3 times its own. */
    internal val D: Timings
        get() = if (reducedMotion) Timings.Reduced else Timings.Soft

    internal class Timings(
        val fly: Float,
        val rest: Float,
        val rise: Float,
        val hold: Float,
        val leave: Float,
        val leaveFast: Float,
        val undo: Float,
        val land: Float,
        val pill: Float,
        val snap: Float,
        val label: Float,
        val arm: Float,
        val text: Float,
    ) {
        companion object {
            val Soft =
                Timings(
                    fly = 1950f,
                    rest = 550f,
                    rise = 1690f,
                    hold = 6500f,
                    leave = 1950f,
                    leaveFast = 1040f,
                    undo = 1820f,
                    land = 260f,
                    pill = 1015f,
                    snap = 494f,
                    label = 260f,
                    arm = 390f,
                    text = 416f,
                )
            val Reduced = Timings(1f, 1f, 1f, 9000f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)
        }
    }

    companion object {
        const val RowH = 64f
        const val CardS = .3f
        const val HU = 1.45f
        private const val Step = 1f / 120f

        /** The prototype's cubic ease in-out. */
        fun ease(t: Float): Float =
            if (t < .5f) 4 * t * t * t else 1 - (-2 * t + 2).let { it * it * it } / 2

        fun smooth(a: Float, b: Float, x: Float): Float {
            val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
            return t * t * (3 - 2 * t)
        }

        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

        private fun bezier(
            p0x: Float,
            p0y: Float,
            p1x: Float,
            p1y: Float,
            p2x: Float,
            p2y: Float,
            p3x: Float,
            p3y: Float,
            e: Float,
            out: CardPose,
        ) {
            val k = 1 - e
            out.x = k * k * k * p0x + 3 * k * k * e * p1x + 3 * k * e * e * p2x + e * e * e * p3x
            out.y = k * k * k * p0y + 3 * k * k * e * p1y + 3 * k * e * e * p2y + e * e * e * p3y
        }
    }
}

/** The dirt thrown up as a task lands and the hand comes up, in flat arrays. */
internal class Crumbs {
    var n = 0
        private set

    val x = FloatArray(Max)
    val y = FloatArray(Max)
    private val vx = FloatArray(Max)
    private val vy = FloatArray(Max)
    val r = FloatArray(Max)
    val dark = BooleanArray(Max)
    private val t = FloatArray(Max)

    fun clear() {
        n = 0
    }

    fun add(x0: Float, y0: Float, vx0: Float, vy0: Float, r0: Float, dark0: Boolean) {
        if (n == Max) return
        x[n] = x0
        y[n] = y0
        vx[n] = vx0
        vy[n] = vy0
        r[n] = r0
        dark[n] = dark0
        t[n] = 0f
        n++
    }

    fun step(dt: Float, soil: Float, u: Float) {
        var w = 0
        for (i in 0 until n) {
            t[i] += dt
            vy[i] += 700 * u * dt
            x[i] += vx[i] * dt
            y[i] += vy[i] * dt
            val keep = !(vy[i] > 0 && y[i] > soil + 2 * u) && t[i] < 2
            if (keep) {
                x[w] = x[i]
                y[w] = y[i]
                vx[w] = vx[i]
                vy[w] = vy[i]
                r[w] = r[i]
                dark[w] = dark[i]
                t[w] = t[i]
                w++
            }
        }
        n = w
    }

    private companion object {
        const val Max = 96
    }
}
