package dev.dimvlachos.lab.core.presentation.components.pigeonhole

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** A message as the mail app lists it: who it is [from], its subject, the preview line and time. */
internal class MailItem(
    val id: String,
    val from: String,
    val subj: String,
    val prev: String,
    val time: String,
)

/** A folder: a cubby in the rack, by its [id] and the [name] on its brass plate. */
internal class Cubby(val id: String, val name: String)

/**
 * A message in the model. [home] and [idx] remember the folder and place it was moved from, so
 * pulling it back out puts it where it was.
 */
internal class Mail(val item: MailItem) {
    val id: String
        get() = item.id

    var home: String? = null
    var idx = 0
}

/** A letter on the canvas: a message somewhere between its row's card (fold 0) and a letter. */
internal class Letter(val mail: Mail) {
    var x = 0f
    var y = 0f
    var f = 0f
    var s = 1f
    var a = 0f
    var av = 0f
    var lift = 0f
    var dim = 0f

    /** In a cubby, behind its rail (the prototype's phase `in`), or in the air above everything. */
    var inCubby = false
    var cub = -1

    /** The card-to-paper turn, on its own clock ([PigeonholeState]'s `paper()`); NaN until set. */
    var pm = Float.NaN
    internal var pt = -1

    // Where it stood in its cubby when its folder opened: its place, offset, tilt and dim.
    internal var je = 0
    internal var ox = 0f
    internal var a0 = 0f
    internal var dim0 = 0f
}

/** The letter in hand, and where it came from: a row of [srcView], or cubby [srcCub]. */
internal class Carry(
    val letter: Letter,
    val fromRow: Boolean,
    val srcView: String,
    val srcIdx: Int,
    val srcCub: Int,
    val offX: Float,
    val offY: Float,
    var px: Float,
    var py: Float,
    var hover: Int,
    var kb: Boolean,
)

/** What the rack and letters do that is heard. */
internal enum class PigeonholeSound {
    Lift,
    Fold,
    Slide,
    Tock,
    Rack,
    Tick,
}

/** What is announced, as the prototype's `say()`. */
internal sealed interface PigeonholeSay {
    data class Choose(val subject: String) : PigeonholeSay

    data class Moved(val folder: String?) : PigeonholeSay

    data object NotMoved : PigeonholeSay

    data object DragHint : PigeonholeSay

    data class Opened(val folder: String, val count: Int) : PigeonholeSay

    data object Inbox : PigeonholeSay

    data object Folders : PigeonholeSay
}

/** Where the keyboard's focus goes next. */
internal sealed interface PigeonholeFocus {
    data class Cub(val index: Int) : PigeonholeFocus

    data class Row(val id: String) : PigeonholeFocus

    data object Folders : PigeonholeFocus
}

/**
 * The pigeonhole rack and the mail list, as the prototype's model: the folders' messages, the
 * list's rows (their slots, scroll, glides and fades), the rack sliding up and down, the letters on
 * the canvas and the one in hand. Ported from its `renderList`, layout, `pickUp`, `pullOut`,
 * `post`, `returnRow`, `insertIntoView`, `openFolder`, `closeFolder`, pointer rules, `step()` and
 * `paper()`; units are the prototype's CSS px, which are dp here, and its times are in ms.
 *
 * The prototype's `async` flows are suspend functions on a small scheduler of its own: [tick]
 * advances the simulated clock and resumes them, so they run without a dispatcher or a real clock.
 * [frame] is bumped by every tick and change, so a drawing that reads it redraws.
 */
@Stable
internal class PigeonholeState(
    val cubbies: List<Cubby>,
    private val start: Map<String, List<MailItem>>,
) {
    var reducedMotion = false

    var onSound: ((PigeonholeSound, Float) -> Unit)? = null
    var onSay: ((PigeonholeSay) -> Unit)? = null
    var onFocus: ((PigeonholeFocus) -> Unit)? = null

    var frame by mutableIntStateOf(0)
        private set

    /** Bumped when the list's rows, which are hidden, a count or the folder change. */
    var listVersion by mutableIntStateOf(0)
        private set

    /** The row being pressed, shaded as the prototype's `.press`. */
    var pressed by mutableStateOf<String?>(null)
        private set

    /** Whether the rack is up (the prototype's rack UI not `inert`). */
    var rackUp by mutableStateOf(false)
        private set

    /** The rack's title: null for Folders, else the sender of the letter being moved. */
    var rackFrom by mutableStateOf<String?>(null)
        private set

    // ---- Mail ----
    private val box = mutableMapOf<String, MutableList<Mail>>()

    /** The open folder: [Inbox] or a cubby's id. */
    var view = Inbox
        private set

    /** The rows the list shows, in order: the prototype's DOM rows. */
    val rows = mutableListOf<Mail>()

    private val hidden = mutableSetOf<String>()
    private val lifted = mutableSetOf<String>()
    private val landed = mutableSetOf<String>()
    private var emptyCub: String? = null

    fun messages(folder: String): List<Mail> = box[folder].orEmpty()

    /** Message [id], in whichever folder it is or in the air between two, or null. */
    fun mail(id: String): Mail? {
        for (ms in box.values) for (m in ms) if (m.id == id) return m
        for (l in letters) if (l.mail.id == id) return l.mail
        return carry?.letter?.mail?.takeIf { it.id == id }
    }

    // Each message's place among them all as they began, which never changes.
    private val ranks = start.values.flatten().withIndex().associate { (i, m) -> m.id to i }

    /** Message [id]'s fixed place among all the messages. */
    fun rank(id: String) = ranks[id] ?: Int.MAX_VALUE

    /** Every message's id, in every folder. */
    fun mailIds(): List<String> = box.values.flatMap { ms -> ms.map { it.id } }

    fun isHidden(id: String) = id in hidden

    fun cubIndex(id: String) = cubbies.indexOfFirst { it.id == id }

    // ---- Layout ----
    var width = 0f
        private set

    var height = 0f
        private set

    /** The list's top on the screen and how tall it shows. */
    var listTop = 0f
        private set

    var listH = 0f
        private set

    /** The scale everything on the rack is drawn at. */
    var k = 1f
        private set

    var rackH = 0f
        private set

    /** Bumped when the layout changes, so baked images are made again. */
    var layoutVersion = 0
        private set

    val rowW: Float
        get() = width - 2 * ListPad

    /** How much bigger the user's font setting makes a row's 19 lines: 1 at the default size. */
    var textScale = 1f
        private set

    /** A row's height: its three lines, which grow with the font setting, and its padding. */
    val rowH: Float
        get() = RowH + 3 * LineH * (textScale - 1)

    fun layout(w: Float, h: Float, top: Float, listHeight: Float, fontScale: Float = 1f) {
        if (
            w == width &&
                h == height &&
                top == listTop &&
                listHeight == listH &&
                fontScale == textScale
        )
            return
        textScale = fontScale
        width = w
        height = h
        listTop = top
        listH = listHeight
        k = min(w / 360f, h / 740f).coerceIn(.86f, 1.2f)
        rackH = (Head + 2 * CubH + Gap + Foot) * k
        scrollTop = scrollTop.coerceIn(0f, maxScroll())
        layoutVersion++
        changed()
    }

    fun upTop() = height - rackH

    fun cubX(i: Int) = SideM * k + (i % 3) * (cubW() + Gap * k)

    fun cubY(i: Int, top: Float) = top + Head * k + (i / 3) * (CubH * k + Gap * k)

    fun cubW() = (width - 2 * SideM * k - 2 * Gap * k) / 3

    fun cubH() = CubH * k

    fun railTop(i: Int, top: Float) = cubY(i, top) + cubH() - Rail * k

    /** Where a letter stands in a cubby: j = 0 is the front one; those behind stand higher. */
    fun slotX(c: Int) = cubX(c) + cubW() / 2

    fun slotY(c: Int, j: Float, top: Float) = railTop(c, top) - 4 * k - 7 * k * j

    fun cubCenterX(c: Int) = slotX(c)

    fun cubCenterY(c: Int, top: Float = upTop()) = cubY(c, top) + (cubH() - Rail * k) * .55f

    // ---- The list ----
    var scrollTop = 0f
        private set

    private fun contentHeight(): Float {
        val n = rows.size
        return if (n == 0) EmptyH + ListFoot else n * rowH + (n - 1) * RowGap + ListFoot
    }

    fun maxScroll() = max(0f, contentHeight() - listH)

    /** Scrolls the list by [delta] (down is positive); returns how much it moved. */
    fun scrollBy(delta: Float): Float {
        val to = (scrollTop + delta).coerceIn(0f, maxScroll())
        val moved = to - scrollTop
        if (moved != 0f) {
            scrollTop = to
            kick()
        }
        return moved
    }

    /** The row [i]'s top in the list's content, at rest. */
    fun rowTop(i: Int) = i * (rowH + RowGap)

    private val flipFrom = mutableMapOf<String, Float>()
    private var flipAt = -1e9
    private var freshAt = -1e9
    private var fadeFrom = 1f
    private var fadeTo = 1f
    private var fadeAt = -1e9

    /** How far row [id] is off its slot now, as the prototype's FLIP glide and `.fresh` fade-in. */
    fun rowShift(id: String): Float {
        var dy = 0f
        val from = flipFrom[id]
        if (from != null) {
            dy += from * (1 - FlipEase(((simNow - flipAt) / FlipMs).toFloat().coerceIn(0f, 1f)))
        }
        val fp = ((simNow - freshAt) / FreshMs).toFloat()
        if (fp < 1f) dy += -6f * (1 - FreshEase(fp.coerceIn(0f, 1f)))
        return dy
    }

    /** How opaque row [id] is: the `.fresh` fade-in. Hidden rows are left to the caller. */
    fun rowAlpha(): Float {
        val fp = ((simNow - freshAt) / FreshMs).toFloat()
        return if (fp >= 1f) 1f else FreshEase(fp.coerceIn(0f, 1f))
    }

    /** The whole list's opacity: the `.fade` it takes while a folder opens. */
    fun listAlpha(): Float {
        val p = ((simNow - fadeAt) / FadeMs).toFloat().coerceIn(0f, 1f)
        return fadeFrom + (fadeTo - fadeFrom) * CssEase(p)
    }

    private fun fade(on: Boolean) {
        val to = if (on) 0f else 1f
        fadeFrom = if (reducedMotion) to else listAlpha()
        fadeTo = to
        fadeAt = simNow
    }

    private fun renderList(flip: Boolean = false, fresh: Boolean = false) {
        unpark(all = true)
        val before = mutableMapOf<String, Float>()
        if (flip) rows.forEachIndexed { i, m -> before[m.id] = rowTop(i) }
        rows.clear()
        rows += box.getValue(view)
        flipFrom.clear()
        if (flip && !reducedMotion) {
            rows.forEachIndexed { i, m ->
                val b = before[m.id]
                if (b != null && b != rowTop(i)) flipFrom[m.id] = b - rowTop(i)
            }
            flipAt = simNow
        }
        freshAt = if (fresh && !reducedMotion) simNow else -1e9
        scrollTop = scrollTop.coerceIn(0f, maxScroll())
        changed()
    }

    private fun changed() {
        listVersion++
        kick()
    }

    /** Where row [id] stands in the list, or -1. */
    fun rowIndex(id: String) = rows.indexOfFirst { it.id == id }

    /** A row's resting centre on the screen, net of the scroll, ignoring any glide it's in. */
    private fun slotOf(i: Int): Pair<Float, Float> =
        width / 2 to listTop + rowTop(i) - scrollTop + rowH / 2

    // A letter only lands in a slot you can see: a row scrolled out of view is brought back first.
    fun reveal(i: Int) {
        val t = rowTop(i) - 4
        val b = rowTop(i) + rowH + 4
        if (t < scrollTop) scrollTop = t.coerceIn(0f, maxScroll())
        else if (b > scrollTop + listH) scrollTop = (b - listH).coerceIn(0f, maxScroll())
        kick()
    }

    /** The row under ([x], [y]), as drawn, that isn't hidden; or null. */
    fun rowAt(x: Float, y: Float): String? {
        if (y < listTop || y > listTop + listH || x < ListPad || x > width - ListPad) return null
        if (onRack(y)) return null
        rows.forEachIndexed { i, m ->
            val top = listTop + rowTop(i) - scrollTop + rowShift(m.id)
            if (y >= top && y < top + rowH && m.id !in hidden) return m.id
        }
        return null
    }

    /** Where row [id] stands on the screen now: its centre, or null when it isn't listed. */
    fun rowCenter(id: String): Pair<Float, Float>? {
        val i = rowIndex(id)
        return if (i < 0) null else slotOf(i)
    }

    // ---- The rack ----
    private var rackFrom0 = 1.04f
    private var rackAt = -1e9

    fun rackSlide(): Float {
        val to = if (rackUp) 0f else 1.04f
        if (reducedMotion) return to
        val p = ((simNow - rackAt) / RackMs).toFloat().coerceIn(0f, 1f)
        return rackFrom0 + (to - rackFrom0) * RackEase(p)
    }

    fun rackTop() = upTop() + rackH * rackSlide()

    /**
     * Whether a press at height [y] lands on the rack, up or on its way, which covers the list: the
     * rows behind it are out of reach (the prototype's rack let presses through to them).
     */
    fun onRack(y: Float) = y >= rackTop()

    private fun raiseRack(from: String?) {
        rackFrom = from
        if (rackUp) return
        rackFrom0 = rackSlide()
        rackAt = simNow
        rackUp = true
        snd(PigeonholeSound.Rack)
        kick()
    }

    private fun lowerRack() {
        if (!rackUp) return
        rackFrom0 = rackSlide()
        rackAt = simNow
        rackUp = false
        snd(PigeonholeSound.Rack, .6f)
        kick()
    }

    /** How many letters cubby [i] holds, not counting one being pulled out. */
    fun cubCount(i: Int) = box[cubbies[i].id]?.count { it.id !in lifted } ?: 0

    /**
     * What cubby [i] shows, front first, into [out] (at most its size): its letters, minus one
     * being pulled out, and none while its folder is open on screen. Returns how many.
     */
    fun cubVisible(i: Int, out: Array<Mail?>): Int {
        var n = 0
        val id = cubbies[i].id
        for (m in box[id].orEmpty()) {
            if (n >= out.size) break
            if (m.id !in lifted && (emptyCub != id || m.id in landed)) out[n++] = m
        }
        return n
    }

    private val one = arrayOfNulls<Mail>(1)

    // While a letter comes in or goes out, the rest of the stack eases back or forward one place.
    private val inc = DoubleArray(6) { Double.NaN }
    private val dec = DoubleArray(6) { Double.NaN }
    private val bump = DoubleArray(6) { Double.NaN }

    /** How far cubby [i]'s stack is shifted back now. */
    fun stackShift(i: Int): Float {
        var shift = 0f
        if (!inc[i].isNaN()) shift += ((simNow - inc[i]) / d(200.0)).toFloat().coerceIn(0f, 1f)
        if (!dec[i].isNaN()) shift += 1 - ((simNow - dec[i]) / d(200.0)).toFloat().coerceIn(0f, 1f)
        return shift
    }

    /** How big cubby [i]'s brass plate is now: a bounce as a letter lands. */
    fun plateScale(i: Int): Float {
        val bt = if (bump[i].isNaN()) 9.0 else (simNow - bump[i]) / 1000
        return (1 + .14 * exp(-bt * 9) * sin(bt * 34)).toFloat()
    }

    /** The cubby the letter is about to go into, while it does, as the prototype's `st.target`. */
    var target = -1
        private set

    /** Whether cubby [i] lights up: hovered by the letter in hand, or the one being posted to. */
    fun hot(i: Int) = carry?.hover == i || target == i

    // ---- Letters ----
    val letters = mutableListOf<Letter>()

    var carry: Carry? = null
        private set

    private var busy = 0

    /** Nothing is moving: the frame loop can sleep until something changes. */
    val settled: Boolean
        get() =
            timers.isEmpty() &&
                anims.isEmpty() &&
                letters.size == (if (carry != null && still) 1 else 0) &&
                simNow - quiet >= SettleMs

    // Whether the letter in hand has come to rest where it is held.
    private var still = false

    private var quiet = 0.0

    private fun kick() {
        quiet = simNow
        frame++
    }

    private fun snd(kind: PigeonholeSound, v: Float = 1f) = onSound?.invoke(kind, v)

    private fun say(s: PigeonholeSay) = onSay?.invoke(s)

    private fun d(ms: Double) = if (reducedMotion) 1.0 else ms

    private fun drop(l: Letter) {
        letters.remove(l)
    }

    // Letters landed in rows the rack still covers, waiting on top for it to uncover them.
    private val parked = mutableListOf<Letter>()

    // Whether row [i]'s slot is under the rack where it is now.
    private fun covered(i: Int) = i >= 0 && slotOf(i).second + rowH / 2 > rackTop()

    // Lets each parked letter become its row once the rack has uncovered it, or at once if [all];
    // until then it keeps to its row's slot as the list scrolls.
    private fun unpark(all: Boolean = false) {
        if (parked.isEmpty()) return
        val it = parked.iterator()
        while (it.hasNext()) {
            val l = it.next()
            val i = rowIndex(l.mail.id)
            if (all || i < 0 || !covered(i)) {
                it.remove()
                drop(l)
                hidden -= l.mail.id
                changed()
            } else {
                val (rx, ry) = slotOf(i)
                l.x = rx
                l.y = ry
            }
        }
    }

    private fun pickUp(id: String, fx: Float, fy: Float): Boolean {
        val list = box.getValue(view)
        val i = list.indexOfFirst { it.id == id }
        val ri = rowIndex(id)
        if (i < 0 || ri < 0) return false
        val m = list[i]
        val (rx, ry) = slotOf(ri)
        val l =
            Letter(m).apply {
                x = rx
                y = ry
            }
        letters += l
        hidden += id
        pressed = null
        carry = Carry(l, true, view, i, -1, rx - fx, ry - fy, fx, fy, -1, false)
        raiseRack(m.item.from)
        snd(PigeonholeSound.Lift)
        changed()
        return true
    }

    private fun pullOut(c: Int, fx: Float, fy: Float): Boolean {
        val n = cubVisible(c, one)
        if (n == 0 || cubbies[c].id == emptyCub) return false
        val m = one[0]!!
        val px = slotX(c)
        val py = slotY(c, 0f, rackTop())
        val l =
            Letter(m).apply {
                x = px
                y = py
                f = 1f
                s = .86f
            }
        letters += l
        lifted += m.id
        dec[c] = simNow
        carry = Carry(l, false, view, -1, c, px - fx, py - fy, fx, fy, -1, false)
        rackFrom = m.item.from
        snd(PigeonholeSound.Slide, .7f)
        changed()
        return true
    }

    /** Picks row [id] up with the keyboard: folded over the rack, waiting for a cubby. */
    fun kbPick(id: String) {
        if (busy > 0 || carry != null) return
        val ri = rowIndex(id)
        if (ri < 0) return
        val (rx, ry) = slotOf(ri)
        if (!pickUp(id, rx, ry)) return
        val c = carry!!
        c.kb = true
        val first = cubbies.indexOfFirst { it.id != view }
        c.hover = first
        onFocus?.invoke(PigeonholeFocus.Cub(first))
        say(PigeonholeSay.Choose(c.letter.mail.item.subj))
    }

    /** Which cubby a point means, judged against the rack where it ends up; the open one never. */
    fun cubbyAt(x: Float, y: Float): Int {
        val top = upTop()
        if (y < top + Head * k * .5f) return -1
        var best = -1
        var bd = 1e9f
        for (i in cubbies.indices) {
            val cx = cubX(i) + cubW() / 2
            val cy = cubY(i, top) + cubH() / 2
            val dd = hypot((x - cx) / cubW(), (y - cy) / cubH())
            if (dd < bd) {
                bd = dd
                best = i
            }
        }
        if (best >= 0 && cubbies[best].id == view) return -1
        return best
    }

    private fun dropAt(x: Float, y: Float) {
        val c0 = carry ?: return
        if (c0.kb) {
            go { returnRow() }
            return
        }
        val c = cubbyAt(x, y)
        when {
            c0.fromRow -> if (c >= 0) go { post(c) } else go { returnRow() }
            c >= 0 -> go { post(c) }
            y < upTop() - 4 * k -> go { insertIntoView() }
            else -> go { post(c0.srcCub) }
        }
    }

    private suspend fun post(c: Int) {
        val c0 = carry ?: return
        carry = null
        busy++
        target = c
        val l = c0.letter
        val m = l.mail
        val cid = cubbies[c].id
        // Out of wherever it was. A row's slot closes now; it remembers where it came from.
        if (c0.fromRow) {
            box.getValue(c0.srcView).remove(m)
            m.home = c0.srcView
            m.idx = c0.srcIdx
            hidden -= m.id
            if (c0.srcView == view) renderList(flip = true)
        } else {
            box.getValue(cubbies[c0.srcCub].id).remove(m)
            lifted -= m.id
            dec[c0.srcCub] = Double.NaN
        }
        val ax = l.x
        val ay = l.y
        val af = l.f
        val aS = l.s
        val aa = l.a
        snd(PigeonholeSound.Slide)
        animate(d(250.0)) { p ->
            val e = ease(p)
            val tx = slotX(c)
            val ty = slotY(c, 0f, rackTop())
            l.x = lerp(ax, tx, e)
            l.y = lerp(ay, ty - 28 * k, e)
            l.f = lerp(af, 1f, e)
            l.s = lerp(aS, .93f, e)
            l.a = lerp(aa, 0f, e)
            l.lift = lerp(1f, .2f, e)
        }
        l.inCubby = true
        l.cub = c
        val myInc = simNow
        inc[c] = myInc
        animate(d(190.0)) { p ->
            val ty = slotY(c, 0f, rackTop())
            l.x = slotX(c)
            l.y = lerp(ty - 28 * k, ty, p * p)
            l.s = lerp(.93f, .86f, p)
            l.lift = .2f * (1 - p)
        }
        drop(l)
        box.getValue(cid).add(0, m)
        if (inc[c] == myInc) inc[c] = Double.NaN
        bump[c] = simNow
        target = -1
        snd(PigeonholeSound.Tock)
        changed()
        say(PigeonholeSay.Moved(cubbies[c].name))
        busy--
        later(d(520.0)) { if (carry == null && busy == 0 && ptr == null) lowerRack() }
    }

    private suspend fun returnRow() {
        val c0 = carry ?: return
        carry = null
        busy++
        val l = c0.letter
        val ri = rowIndex(l.mail.id)
        if (ri >= 0) {
            reveal(ri)
            val (rx, ry) = slotOf(ri)
            val ax = l.x
            val ay = l.y
            val af = l.f
            val aS = l.s
            val aa = l.a
            val al = l.lift
            animate(d(340.0)) { p ->
                val e = ease(p)
                l.x = lerp(ax, rx, e)
                l.y = lerp(ay, ry, e)
                l.f = lerp(af, 0f, e)
                l.s = lerp(aS, 1f, e)
                l.a = lerp(aa, 0f, e)
                l.lift = lerp(al, 0f, e)
            }
        }
        drop(l)
        hidden -= l.mail.id
        changed()
        busy--
        lowerRack()
        say(PigeonholeSay.NotMoved)
        if (c0.kb && ri >= 0) onFocus?.invoke(PigeonholeFocus.Row(l.mail.id))
    }

    private suspend fun insertIntoView() {
        val c0 = carry ?: return
        carry = null
        busy++
        val l = c0.letter
        val m = l.mail
        box.getValue(cubbies[c0.srcCub].id).remove(m)
        lifted -= m.id
        dec[c0.srcCub] = Double.NaN
        val arr = box.getValue(view)
        val idx = if (m.home == view) min(m.idx, arr.size) else 0
        arr.add(idx, m)
        hidden += m.id
        renderList(flip = true)
        val ri = rowIndex(m.id)
        reveal(ri)
        val (rx, ry) = slotOf(ri)
        val ax = l.x
        val ay = l.y
        val af = l.f
        val aS = l.s
        val aa = l.a
        val al = l.lift
        animate(d(380.0)) { p ->
            val e = ease(p)
            l.x = lerp(ax, rx, e)
            l.y = lerp(ay, ry, e)
            l.f = lerp(af, 0f, e)
            l.s = lerp(aS, 1f, e)
            l.a = lerp(aa, 0f, e)
            l.lift = lerp(al, 0f, e)
        }
        drop(l)
        hidden -= m.id
        changed()
        snd(PigeonholeSound.Lift, .6f)
        say(PigeonholeSay.Moved(if (view == Inbox) null else cubbies[cubIndex(view)].name))
        busy--
        later(d(380.0)) { if (carry == null && busy == 0 && ptr == null) lowerRack() }
    }

    private suspend fun openFolder(c: Int) {
        if (busy > 0 || carry != null) return
        val cid = cubbies[c].id
        if (cid == view) {
            closeFolder()
            return
        }
        if (view != Inbox) closeFolder()
        busy++
        raiseRack(null)
        fade(true)
        wait(d(170.0))
        view = cid
        val ms = box.getValue(cid).toList()
        hidden.clear()
        ms.forEach { hidden += it.id }
        renderList()
        scrollTop = 0f
        fade(false)
        say(PigeonholeSay.Opened(cubbies[c].name, ms.size))
        // The cubby's whole stack becomes moving letters in one frame, each standing exactly where
        // the cubby drew it, so nothing blinks.
        emptyCub = cid
        landed.clear()
        val ls = ms.mapIndexed { j, m ->
            val h = hash(m.id)
            val je = min(j, 2)
            Letter(m).apply {
                this.je = je
                ox = ((h % 3) - 1) * 6 * k * min(je, 1)
                a0 = ((h % 5) - 2) * .03f
                dim0 = je * .16f
                f = 1f
                s = .86f
                inCubby = true
                cub = c
                pm = 1f
                x = slotX(c) + ox
                y = slotY(c, je.toFloat(), rackTop())
                a = a0
                dim = dim0
            }
        }
        letters += ls.asReversed() // back to front, so the front letter is drawn last
        // One after another the letters lift out of the cubby and unfold into their rows.
        all(
            ls.mapIndexed { j, l ->
                suspend {
                    val m = l.mail
                    wait(d(j * 90.0))
                    snd(PigeonholeSound.Slide, .5f)
                    animate(d(150.0)) { p ->
                        val e = ease(p)
                        l.x = slotX(c) + l.ox * (1 - e)
                        l.y = slotY(c, l.je * (1 - e), rackTop()) - 36 * k * e
                        l.s = lerp(.86f, .95f, p)
                        l.a = l.a0 * (1 - e)
                        l.dim = l.dim0 * (1 - e)
                    }
                    l.inCubby = false
                    val (rx, ry) = slotOf(rowIndex(m.id))
                    val ax = l.x
                    val ay = l.y
                    animate(d(440.0)) { p ->
                        val e = ease(p)
                        val arc = sin(p * PI.toFloat())
                        l.x = lerp(ax, rx, e)
                        l.y = lerp(ay, ry, e) - arc * 26 * k
                        l.f = 1 - ease((p * 1.15f).coerceIn(0f, 1f))
                        l.s = lerp(.95f, 1f, e)
                        l.lift = arc * .7f
                        l.a = arc * -.05f
                    }
                    // A row the rack still covers would take the letter out of sight behind it at
                    // once: the letter stays on top until the rack, going down, has uncovered it.
                    if (covered(rowIndex(m.id))) {
                        parked += l
                    } else {
                        drop(l)
                        hidden -= m.id
                        changed()
                    }
                }
            },
        )
        busy--
        later(d(160.0)) { if (busy == 0 && carry == null && ptr == null) lowerRack() }
    }

    private suspend fun closeFolder() {
        if (busy > 0 || carry != null || view == Inbox) return
        busy++
        val c = cubIndex(view)
        // The rows the rising rack will cover lift off as letters first, where they lie, so the
        // rack
        // comes up under them instead of each popping out on top of it when its turn comes.
        val early = HashMap<String, Letter>()
        if (!rackUp) {
            rows.forEachIndexed { i, m ->
                val (rx, ry) = slotOf(i)
                val bottom = ry + rowH / 2
                val seen = bottom <= rackTop() && ry - rowH / 2 < listTop + listH
                if (seen && bottom > upTop() && m.id !in hidden) {
                    val l =
                        Letter(m).apply {
                            x = rx
                            y = ry
                        }
                    hidden += m.id
                    letters += l
                    early[m.id] = l
                }
            }
            if (early.isNotEmpty()) changed()
        }
        raiseRack(null)
        wait(d(if (rackFrom0 > .5f) 320.0 else 60.0))
        emptyCub = view
        landed.clear()
        val ms = box.getValue(view).toList()
        val n = ms.size
        // The inbox comes back as soon as the last row has lifted, not after every letter landed.
        val home: suspend () -> Unit = {
            wait(d(max(0, n - 1) * 75.0 + 220))
            view = Inbox
            hidden.clear()
            renderList(fresh = true)
            scrollTop = 0f
            say(PigeonholeSay.Inbox)
        }
        // Back in from the bottom row up, so the newest goes in last and stands at the front.
        all(
            listOf(home) +
                ms.mapIndexed { j, m ->
                    suspend {
                        wait(d((n - 1 - j) * 75.0))
                        val ri = rowIndex(m.id)
                        if (ri < 0) {
                            landed += m.id
                        } else {
                            // A row scrolled out of sight leaves from the edge of the list.
                            val (rx, ry0) = slotOf(ri)
                            val ry =
                                ry0.coerceIn(listTop - rowH * .3f, listTop + listH + rowH * .3f)
                            val l =
                                early[m.id]
                                    ?: Letter(m).apply {
                                        x = rx
                                        y = ry
                                        hidden += m.id
                                        letters += this
                                        changed()
                                    }
                            animate(d(420.0)) { p ->
                                val e = ease(p)
                                val ty = slotY(c, 0f, rackTop())
                                l.x = lerp(rx, slotX(c), e)
                                l.y = lerp(ry, ty - 28 * k, e) - sin(p * PI.toFloat()) * 22 * k
                                l.f = ease((p * 1.25f).coerceIn(0f, 1f))
                                l.s = lerp(1f, .93f, e)
                                l.lift = sin(p * PI.toFloat()) * .7f
                            }
                            l.inCubby = true
                            l.cub = c
                            val myInc = simNow
                            inc[c] = myInc
                            animate(d(170.0)) { p ->
                                val ty = slotY(c, 0f, rackTop())
                                l.y = lerp(ty - 28 * k, ty, p * p)
                                l.s = lerp(.93f, .86f, p)
                            }
                            drop(l)
                            landed += m.id
                            if (inc[c] == myInc) inc[c] = Double.NaN
                            snd(PigeonholeSound.Tock, .45f)
                        }
                    }
                },
        )
        bump[c] = simNow
        emptyCub = null
        landed.clear()
        changed()
        busy--
        later(d(420.0)) { if (busy == 0 && carry == null && ptr == null) lowerRack() }
    }

    /** The Back arrow: the folder's rows fold up and go home. */
    fun back() {
        go { closeFolder() }
        kick()
    }

    /** The rack icon: the rack comes up on its own, or goes down. */
    fun toggleRack() {
        if (busy > 0 || carry != null) return
        if (rackUp) lowerRack()
        else {
            raiseRack(null)
            say(PigeonholeSay.Folders)
        }
        kick()
    }

    /** Done on the rack: a letter carried by keys goes back; else the rack goes down. */
    fun done() {
        val c = carry
        // A finger's carry isn't Done's to end.
        if (c != null && !c.kb) return
        if (c?.kb == true) go { returnRow() } else if (busy == 0) lowerRack()
        kick()
    }

    /** Escape: a letter carried by keys goes back; or an idle rack goes down. */
    fun escape(): Boolean {
        val c = carry
        return when {
            c != null && c.kb -> {
                go { returnRow() }
                kick()
                true
            }
            rackUp && busy == 0 && c == null -> {
                lowerRack()
                onFocus?.invoke(PigeonholeFocus.Folders)
                kick()
                true
            }
            else -> false
        }
    }

    /** Whether the system Back has something here to undo: a carry, the rack, or an open folder. */
    val backable: Boolean
        get() = carry != null || rackUp || view != Inbox

    /**
     * The system Back: what Escape does, else the folder's Back arrow. A letter in a finger's hand
     * is the finger's, so Back leaves it be rather than the screen.
     */
    fun systemBack() {
        if (escape()) return
        if (carry == null && view != Inbox) back()
    }

    /** A cubby tapped (or pressed with keys): post a key-carried letter, or open its folder. */
    fun cubTap(c: Int) {
        val c0 = carry
        if (c0 != null && c0.kb) {
            if (cubbies[c].id != view) go { post(c) }
        } else {
            go { openFolder(c) }
        }
        kick()
    }

    /** The keyboard's focus on cubby [c]: a key-carried letter waits over it. */
    fun focusCub(c: Int) {
        val c0 = carry ?: return
        if (c0.kb && cubbies[c].id != view) {
            c0.hover = c
            kick()
        }
    }

    // ---- Physics ----

    // The hovering letter's centre: its bottom edge just clear of the cubby's top frame.
    private fun hoverY(c: Int, top: Float, l: Letter) = cubY(c, top) - LH * k * l.s / 2 - 4 * k

    private fun step(dt: Float) {
        val c0 = carry
        if (c0 == null) {
            still = false
            return
        }
        val l = c0.letter
        val top = rackTop()
        var tx: Float
        var ty: Float
        val fT: Float
        if (c0.kb) {
            val h = c0.hover
            if (h >= 0) {
                tx = slotX(h)
                ty = hoverY(h, top, l)
            } else {
                tx = width / 2
                ty = top - 34 * k
            }
            fT = 1f
        } else {
            val u = upTop()
            tx = c0.px + c0.offX * (1 - l.f)
            ty = c0.py + c0.offY * (1 - l.f)
            // The nearer the finger is to the rack, the more folded: flat 160 above it, a letter
            // just inside it.
            fT = smooth(((c0.py - (u - 160 * k)) / (170 * k)).coerceIn(0f, 1f))
            // Over a cubby the letter is drawn toward its slot but held above its top frame.
            if (c0.hover >= 0) {
                val hy = hoverY(c0.hover, top, l)
                tx = lerp(tx, slotX(c0.hover), .3f)
                ty = min(lerp(ty, hy, .3f), hy)
            }
        }
        val a = 1 - exp(-dt * 30)
        val ox = l.x
        l.x += (tx - l.x) * a
        l.y += (ty - l.y) * a
        val f0 = l.f
        l.f += (fT - l.f) * (1 - exp(-dt * 12))
        if ((f0 < .5f) != (l.f < .5f)) snd(PigeonholeSound.Fold)
        l.s += (1 - l.s) * (1 - exp(-dt * 10))
        l.lift += (1 - l.lift) * (1 - exp(-dt * 10))
        // It swings a little in the direction it's dragged, more once it's a small letter.
        val at = ((l.x - ox) / dt * .00028f).coerceIn(-.18f, .18f) * l.f
        l.av += ((at - l.a) * 260 - l.av * 24) * dt
        l.a += l.av * dt
        still =
            abs(tx - l.x) < .05f &&
                abs(ty - l.y) < .05f &&
                abs(fT - l.f) < 1e-3f &&
                abs(l.av) < 1e-3f &&
                abs(at - l.a) < 1e-3f &&
                1 - l.s < 1e-3f &&
                1 - l.lift < 1e-3f
    }

    // Card or paper: it follows the fold with a little hysteresis, but turns over on its own 70 ms
    // clock, so it never rests half way (in dark mode a grey card with grey text).
    private fun paper(dt: Float) {
        for (l in letters) {
            l.pt =
                when {
                    l.f > .16f -> 1
                    l.f < .08f -> 0
                    l.pt >= 0 -> l.pt
                    l.f > .12f -> 1
                    else -> 0
                }
            l.pm =
                if (l.pm.isNaN() || reducedMotion) l.pt.toFloat()
                else (l.pm + (if (l.pt == 1) 1 else -1) * dt * 1000 / PaperMs).coerceIn(0f, 1f)
        }
    }

    // ---- Input ----

    private class Ptr(
        val row: String?,
        val cub: Int,
        val x0: Float,
        val y0: Float,
        val touch: Boolean,
        val t0: Double,
        val scripted: Boolean,
        val owner: Any?,
    ) {
        var x = x0
        var y = y0
        var moved = false
        var scroll = false
    }

    private var ptr: Ptr? = null

    /** Whether the finger [owner] on the list has been given to the list to scroll. */
    fun scrollingFor(owner: Any?) = ptr?.let { it.scroll && it.owner == owner } == true

    /** Whether a scripted finger has hold. */
    val scriptedTouch: Boolean
        get() = ptr?.scripted == true

    /** A press on row [id] at ([x], [y]), at [now] ms; [touch] for a finger, which may scroll. */
    fun rowDown(
        id: String,
        x: Float,
        y: Float,
        touch: Boolean = false,
        now: Double = 0.0,
        scripted: Boolean = false,
        owner: Any? = null,
    ) {
        if (busy > 0 || carry != null || ptr != null) return
        ptr = Ptr(id, -1, x, y, touch, now, scripted, owner)
        pressed = id
        kick()
    }

    /** A press on cubby [c]; a key-carried letter is posted there at once. */
    fun cubDown(c: Int, x: Float, y: Float, scripted: Boolean = false, owner: Any? = null) {
        val c0 = carry
        if (c0 != null && c0.kb) {
            cubTap(c)
            return
        }
        if (busy > 0 || c0 != null || ptr != null) return
        ptr = Ptr(null, c, x, y, false, 0.0, scripted, owner)
        kick()
    }

    // On a touch screen the list scrolls: a quick drag mostly up or down is a scroll, except one
    // down while the list is at its top, which heads for the rack. Sideways, or a hold, picks up.
    private fun scrolls(p: Ptr, x: Float, y: Float, now: Double): Boolean {
        if (p.row == null || !p.touch || now - p.t0 >= HoldMs) return false
        val dx = x - p.x0
        val dy = y - p.y0
        if (abs(dy) <= abs(dx) * 1.2f) return false
        return dy < 0 || scrollTop > 0
    }

    private fun unpress(p: Ptr?) {
        if (p?.row != null && pressed == p.row) pressed = null
    }

    private fun begin(p: Ptr): Boolean {
        // A flow started while the press waited (a folder opening or closing, a letter carried by
        // keys): the press is spent, or it would pick up a row that is on its way somewhere else.
        if (busy > 0 || carry != null) {
            unpress(p)
            ptr = null
            kick()
            return false
        }
        val ok = if (p.row != null) pickUp(p.row, p.x0, p.y0) else pullOut(p.cub, p.x0, p.y0)
        p.moved = true
        return ok
    }

    /**
     * The finger moved to ([x], [y]) at [now] ms; [scripted] for the demo's, which only moves its
     * own.
     */
    fun ptrMove(
        x: Float,
        y: Float,
        now: Double = 0.0,
        scripted: Boolean = false,
        owner: Any? = null,
    ) {
        val p = ptr ?: return
        if (p.scroll || p.scripted != scripted || p.owner != owner) return
        p.x = x
        p.y = y
        if (!p.moved && hypot(x - p.x0, y - p.y0) > Slop) {
            if (scrolls(p, x, y, now)) {
                p.scroll = true
                unpress(p)
                kick()
                return
            }
            begin(p)
            if (ptr !== p) return
        }
        val c = carry
        if (c != null && !c.kb) {
            c.px = x
            c.py = y
            val h = cubbyAt(x, y)
            if (h != c.hover) {
                c.hover = h
                if (h >= 0) snd(PigeonholeSound.Tick)
            }
        }
        kick()
    }

    /** The finger lifted at ([x], [y]); [scripted] for the demo's, which only lifts its own. */
    fun ptrUp(x: Float, y: Float, scripted: Boolean = false, owner: Any? = null) {
        val p = ptr ?: return
        if (p.scripted != scripted || p.owner != owner) return
        unpress(p)
        kick()
        if (p.scroll) {
            ptr = null
            return
        }
        // Decided from the release point alone: a press and a far release still drops.
        if (!p.moved && hypot(x - p.x0, y - p.y0) > Slop) {
            begin(p)
            if (ptr !== p) return
        }
        ptr = null
        if (p.moved) {
            val c = carry
            if (c != null && !c.kb) {
                c.px = x
                c.py = y
                dropAt(x, y)
            }
            return
        }
        if (p.row != null) say(PigeonholeSay.DragHint) else cubTap(p.cub)
    }

    /** The touch was cancelled: a carried letter drops where it was last seen; else nothing. */
    fun ptrCancel(owner: Any? = null) {
        val p = ptr ?: return
        if (p.owner != owner) return
        // Nothing picked up: nothing to drop, and a cubby cancelled is not a tap.
        if (!p.moved) {
            unpress(p)
            ptr = null
            kick()
        } else {
            ptrUp(p.x, p.y, p.scripted, owner)
        }
    }

    /** Lets go of a scripted finger's hold where it is, as the prototype's `demoStop()`. */
    fun releaseScripted() {
        val p = ptr ?: return
        if (!p.scripted) return
        if (p.moved) ptrUp(p.x, p.y, scripted = true, owner = p.owner)
        else {
            unpress(p)
            ptr = null
            kick()
        }
    }

    // ---- Clock ----

    /** The simulated clock, in ms. */
    var simNow = 0.0
        private set

    private class Timer(val at: Double, val fn: () -> Unit)

    private class Anim(
        val t0: Double,
        val ms: Double,
        val fn: (Float) -> Unit,
        val done: () -> Unit,
    )

    private val timers = mutableListOf<Timer>()
    private val anims = mutableListOf<Anim>()
    private val ready = ArrayDeque<() -> Unit>()

    private fun later(ms: Double, fn: () -> Unit) {
        timers += Timer(simNow + ms, fn)
    }

    private suspend fun wait(ms: Double) = suspendCoroutine { c ->
        timers += Timer(simNow + ms) { ready += { c.resume(Unit) } }
    }

    private suspend fun animate(ms: Double, fn: (Float) -> Unit) = suspendCoroutine { c ->
        anims += Anim(simNow, ms, fn) { ready += { c.resume(Unit) } }
    }

    private fun go(block: suspend () -> Unit) {
        block.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    }

    // Like Promise.all: runs every block at once and resumes when the last has finished.
    private suspend fun all(blocks: List<suspend () -> Unit>) = suspendCoroutine { c ->
        var left = blocks.size
        if (left == 0) c.resume(Unit)
        else
            for (b in blocks) {
                go {
                    b()
                    if (--left == 0) c.resume(Unit)
                }
            }
    }

    /** Advances everything by [dt] seconds: timers, timed moves, the letter in hand, the paper. */
    fun tick(dt: Float) {
        simNow += dt * 1000
        for (i in timers.indices.reversed()) {
            if (i < timers.size && timers[i].at <= simNow) timers.removeAt(i).fn()
        }
        for (i in anims.indices.reversed()) {
            if (i >= anims.size) continue
            val a = anims[i]
            val p = ((simNow - a.t0) / a.ms).toFloat().coerceIn(0f, 1f)
            a.fn(p)
            if (p >= 1f) {
                anims.removeAt(i)
                a.done()
            }
        }
        var left = dt
        while (left > 0f) {
            val h = min(left, 1f / 240)
            step(h)
            left -= h
        }
        paper(dt)
        while (ready.isNotEmpty()) ready.removeFirst()()
        unpark()
        if (
            timers.isNotEmpty() ||
                anims.isNotEmpty() ||
                letters.size > (if (carry != null && still) 1 else 0)
        ) {
            quiet = simNow
        }
        frame++
    }

    /** Every message back where it began, the inbox open, the rack down, nothing in the air. */
    fun reset() {
        parked.clear()
        timers.clear()
        anims.clear()
        ready.clear()
        box.clear()
        for ((key, items) in start) box[key] = items.mapTo(mutableListOf()) { Mail(it) }
        for (c in cubbies) box.getOrPut(c.id) { mutableListOf() }
        box.getOrPut(Inbox) { mutableListOf() }
        view = Inbox
        hidden.clear()
        lifted.clear()
        landed.clear()
        emptyCub = null
        letters.clear()
        ptr = null
        pressed = null
        inc.fill(Double.NaN)
        dec.fill(Double.NaN)
        bump.fill(Double.NaN)
        rackUp = false
        rackFrom0 = 1.04f
        rackAt = -1e9
        rackFrom = null
        carry = null
        busy = 0
        target = -1
        fadeFrom = 1f
        fadeTo = 1f
        scrollTop = 0f
        renderList()
    }

    /** Whether everything is where [reset] puts it: nothing moved, the inbox open, at rest. */
    /**
     * Nothing under way: no flow, carry or press, no timer or animation, nothing in the air, and
     * the rack at rest. The script waits for it before each step, so a slow device runs late rather
     * than tapping into a flow that would ignore it.
     */
    val idle: Boolean
        get() =
            busy == 0 &&
                carry == null &&
                ptr == null &&
                timers.isEmpty() &&
                anims.isEmpty() &&
                letters.isEmpty() &&
                (reducedMotion || simNow - rackAt >= RackMs)

    val asBegun: Boolean
        get() =
            view == Inbox &&
                !rackUp &&
                carry == null &&
                letters.isEmpty() &&
                start.all { (key, items) -> box[key]?.map { it.id } == items.map { it.id } }

    init {
        reset()
    }

    companion object {
        const val Inbox = "inbox"

        // The prototype's sizes: a folded letter, the rack's head, cubbies, rail and margins.
        const val LW = 78f
        const val LH = 46f
        const val Head = 46f
        const val CubH = 92f
        const val Rail = 30f
        const val Gap = 8f
        const val SideM = 12f
        const val Foot = 16f

        // A row: three 19 lines, 9 padding and a 1 border each side; 8 between rows; the list's
        // 16 sides and 40 foot. An empty folder's note is 40 + 21.7 + 40 tall.
        const val RowH = 77f
        const val LineH = 19f
        const val RowGap = 8f
        const val ListPad = 16f
        const val ListFoot = 40f
        const val EmptyH = 101.7f

        const val RackMs = 380.0
        const val FlipMs = 380.0
        const val FreshMs = 450.0
        const val FadeMs = 160.0
        const val PaperMs = 70f
        const val Slop = 10f
        const val HoldMs = 280.0
        private const val SettleMs = 1200.0

        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

        fun ease(t: Float) = if (t < .5f) 2 * t * t else 1 - (-2 * t + 2).pow(2) / 2

        fun smooth(t: Float) = t * t * (3 - 2 * t)

        /** The prototype's sender hash: the same sender, the same stamp, in every cubby. */
        fun hash(s: String): Int {
            var h = 7
            for (ch in s) h = (h * 31 + ch.code) % 9973
            return h
        }

        /** The first name in pen; after a title, the surname ("Ms. Rivera" is "Rivera"). */
        fun penName(s: String): String {
            val w = s.split(' ')
            return if (w[0].endsWith('.') && w.size > 1 && w[1].isNotEmpty()) w[1] else w[0]
        }

        private fun cubicBezier(x1: Float, y1: Float, x2: Float, y2: Float): (Float) -> Float {
            fun f(t: Float, a: Float, b: Float) =
                3 * a * t * (1 - t) * (1 - t) + 3 * b * t * t * (1 - t) + t * t * t
            return { x ->
                var lo = 0f
                var hi = 1f
                var t = x
                repeat(30) {
                    t = (lo + hi) / 2
                    if (f(t, x1, x2) < x) lo = t else hi = t
                }
                f(t, y1, y2)
            }
        }

        val RackEase = cubicBezier(.2f, .9f, .25f, 1f)
        val FlipEase = RackEase
        val FreshEase = cubicBezier(0f, 0f, .58f, 1f)
        val CssEase = cubicBezier(.25f, .1f, .25f, 1f)
    }
}
