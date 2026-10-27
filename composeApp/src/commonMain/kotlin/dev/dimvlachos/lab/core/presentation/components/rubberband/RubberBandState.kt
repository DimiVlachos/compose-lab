package dev.dimvlachos.lab.core.presentation.components.rubberband

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

/** Who a row of the thread is from: me, the other person, or the other person typing. */
internal enum class Who {
    Me,
    Them,
    Typing,
}

/** A row of the thread; [gap] when it starts a new speaker's run, so it sits a little lower. */
internal class ChatRow(val id: Long, val who: Who, val text: String, val gap: Boolean)

/**
 * What the message tied to the band is doing: springing up, held by the band, yanked back, or sent.
 */
internal enum class TetherMode {
    Rise,
    Live,
    Fly,
    Done,
}

/** The moments the band is heard and felt. */
internal enum class BandEvent {
    Send,
    Grab,
    Taut,
    Boing,
    Yank,
    Burst,
    Tick,
    Twang,
}

/** What is announced: sent (and how to unsend), delivered for good, or back in the field. */
internal enum class BandSay {
    Sent,
    Delivered,
    Unsent,
}

/**
 * Where a message's letters are, for the burst: in the bubble they leave and in the field they land
 * in. Both come from real text layout, the bubble's own and the field's style measured with a
 * `TextMeasurer`.
 */
internal interface GlyphPlaces {
    /**
     * Writes the centre of each letter of [text] that starts at [starts] into [out] (x, y pairs),
     * from the bubble's top-left, at its laid-out size.
     */
    fun inBubble(text: String, starts: IntArray, out: FloatArray)

    /**
     * Writes the centre x of each letter into [out], from the start of [text] set in the field's
     * style, and returns how wide the whole text is there.
     */
    fun inField(text: String, starts: IntArray, out: FloatArray): Float
}

/** The message that can still be pulled back, as the prototype's `tether`. */
internal class Tether(val row: ChatRow) {
    var mode = TetherMode.Rise
    var held = false

    /** How much of the window has run, in ms of simulated time. */
    var elapsed = 0.0

    /**
     * Where along the band it was last plucked, and that point's offset from rest (springs to
     * zero).
     */
    var u = .5f
    var d = Offset.Zero
    var dv = Offset.Zero

    /** The bubble's lean (or rise or flight), and its scale about its left middle. */
    var off = Offset.Zero
    var offV = Offset.Zero
    var sc = 1f

    var riseT0 = 0.0

    /** Where the rise starts from, once the bubble has been laid out. */
    var rise: Offset? = null

    var flyO0 = Offset.Zero
    var flyS0 = 1f
    var flyTo = Offset.Zero
    var flyT0 = 0.0
    var flyD0 = Offset.Zero
    var popped = false
}

/** A letter of a message that burst, on its way into the field. */
internal class FlyingLetter(
    val ch: String,
    val index: Int,
    val x0: Float,
    val y0: Float,
    val x1: Float,
    val y1: Float,
    val s0: Float,
    val s1: Float,
    val t0: Double,
    val up: Float,
    val rot: Float,
    val clip: Boolean,
)

/** A curve the band is drawn along, from 0 at the bubble to 1 at the eye. */
internal fun interface Curve {
    fun at(t: Float): Offset
}

/**
 * The band's curve as the prototype's `bandOf()`: the rest curve split at the pluck point [u] (de
 * Casteljau), that point moved by the pull, each half bending with half of it and straightening as
 * it stretches.
 */
internal class Band : Curve {
    var a = Offset.Zero
    var b = Offset.Zero
    var r = Offset.Zero
    var p = Offset.Zero
    var c1 = Offset.Zero
    var c2 = Offset.Zero
    var u = .5f
    var length = 0f
    var rest = 0f
    var stretch = 0f

    override fun at(t: Float): Offset =
        if (t <= u) q(a, c1, p, t / u) else q(p, c2, b, (t - u) / (1 - u))
}

/** A band that snapped, each half curling back into its end, from [t0]. */
internal class BandBreak(val band: Band, val t0: Double, val w: Float)

/**
 * The rubber band unsend, as the prototype runs it: a thread, its newest message tied to the
 * composer's brass eye for [UnsendMs] after it lands, a finger's pull on it, the yank back and the
 * burst into letters, and the snap when the window runs out. Pure: the screen tells it where things
 * are laid out (in dp, from the screen's top-left) and steps it with [advance]; what to draw is
 * read off it.
 */
@Stable
internal class RubberBandState(private val opening: List<Pair<Who, String>>) {
    var reducedMotion = false

    /** The rows of the thread, top to bottom. */
    val rows = mutableStateListOf<ChatRow>()

    /** The row tied to the band, whose bubble the screen's canvas draws; null when none is. */
    var tetheredId by mutableStateOf<Long?>(null)
        private set

    /** The row that says Delivered. */
    var receiptId by mutableStateOf<Long?>(null)
        private set

    /** A message is coming back: Send waits. */
    var busy by mutableStateOf(false)
        private set

    /** Letters are landing in the field: its placeholder is blank. */
    var landing by mutableStateOf(false)
        private set

    /** The thread's opacity, for the tour's fade back to the opening thread. */
    var threadAlpha by mutableFloatStateOf(1f)

    /** Bumped every step while the thread glides, for its column to be placed again. */
    var glideTick by mutableIntStateOf(0)
        private set

    /** Bumped every step, for the canvas to draw again. */
    var frame by mutableIntStateOf(0)
        private set

    /** Bumped when something sets the band moving, to wake a resting frame loop. */
    var wake by mutableIntStateOf(0)
        private set

    var onEvent: ((BandEvent, Float) -> Unit)? = null
    var onSay: ((BandSay) -> Unit)? = null

    /** The message is back: put [text] in the field, focused if [byHand]. */
    var onRestore: ((text: String, byHand: Boolean) -> Unit)? = null

    var glyphs: GlyphPlaces? = null

    // ---- Simulation ----
    var now = 0.0
        private set

    var tether: Tether? = null
        private set

    var letters: List<FlyingLetter> = emptyList()
        private set

    /** The burst bubble's shell, shrinking away, from [shellT0]. */
    var shell: Rect? = null
        private set

    var shellT0 = 0.0
        private set

    /** The band whipping into the eye after the burst. */
    var retractA = Offset.Zero
        private set

    var retractB = Offset.Zero
        private set

    var retractW = 0f
        private set

    var retractT0 = -1.0
        private set

    val breaks = ArrayList<BandBreak>()

    /** How much of the "Message" placeholder shows: none while a band is tied to the field. */
    var ph = 1f
        private set

    var unsent = 0
        private set

    var snaps = 0
        private set

    var sent = 0
        private set

    private var nextId = 0L
    private val timers = ArrayList<Pair<Double, () -> Unit>>()
    private var drag: Drag? = null
    private var unsendScripted = false

    private class Drag(
        val t: Tether,
        val p0: Offset,
        val f0: Offset,
        val scripted: Boolean,
    ) {
        var taut = false
        var pull = 0f
    }

    /** Whether a finger or the script is holding the band, and past the pull that unsends. */
    val isTaut: Boolean
        get() = drag?.let { it.taut && it.t === tether } ?: false

    // ---- Layout, from the screen ----
    /** The prototype's scale, from the screen's size without the keyboard. */
    var k = 1f
        private set

    var eye = Offset.Zero
        private set

    val eyeR: Float
        get() = 5.2f * k

    var fieldX = 0f
        private set

    var fieldRight = 0f
        private set

    var fieldCy = 0f
        private set

    /** The tethered bubble as laid out, before its lean, rise or flight. */
    var bubbleBase: Rect? = null
        private set

    /** The tethered row's caption text's top-left. */
    var captionAt: Offset? = null
        private set

    var threadTop = 0f
        private set

    private var threadHeight = -1f
    private var glidePending = false
    private var glideFrom = 0f
    private var glideT0 = -1e9
    private var joltId = -1L
    private var joltT0 = -1e9
    private var capOn = false
    private var capFrom = 0f
    private var capTo = 0f
    private var capT0 = -1e9

    init {
        reset()
    }

    /**
     * The screen's size without the keyboard, and where the field and the send button are, for a
     * script.
     */
    var width = 0f
        private set

    var height = 0f
        private set

    var fieldBox = Rect.Zero
        private set

    var sendBox = Rect.Zero
        private set

    fun boxesAt(field: Rect, send: Rect) {
        fieldBox = field
        sendBox = send
    }

    fun size(width: Float, height: Float) {
        this.width = width
        this.height = height
        val kk = (min(width / 360f, height / 740f)).coerceIn(.85f, 1.25f)
        if (kk != k) {
            k = kk
        }
    }

    fun eyeAt(center: Offset) {
        if (center != eye) {
            eye = center
        }
    }

    /** The field's text box: where its text starts and ends, and its middle line. */
    fun fieldAt(x: Float, right: Float, cy: Float) {
        if (x != fieldX || right != fieldRight || cy != fieldCy) {
            fieldX = x
            fieldRight = right
            fieldCy = cy
        }
    }

    fun threadAt(top: Float) {
        if (top != threadTop) {
            threadTop = top
        }
    }

    /** Row [id]'s bubble is laid out at [rect]; heard only for the tethered row. */
    fun bubbleAt(id: Long, rect: Rect) {
        val t = tether ?: return
        if (t.row.id != id || rect == bubbleBase) return
        bubbleBase = rect
        if (t.rise == null && t.mode == TetherMode.Rise) {
            // Rise from where the text sat in the field.
            t.rise = Offset(fieldX - 4 * k - rect.left, fieldCy - (rect.top + rect.height / 2))
            t.off = t.rise!!
        }
    }

    fun captionAt(id: Long, at: Offset) {
        val t = tether ?: return
        if (t.row.id != id || at == captionAt) return
        captionAt = at
    }

    /**
     * The thread's column measured [height] tall. A change a row's coming or going asked for
     * glides, as the prototype's `shiftThread()`, from where it was; what is left of a glide still
     * running is carried in.
     */
    fun threadMeasured(height: Float) {
        val was = threadHeight
        threadHeight = height
        if (was < 0f || height == was) return
        if (!glidePending) return
        glidePending = false
        if (reducedMotion) return
        glideFrom = height - was + glideY()
        glideT0 = now
        wake++
    }

    /** How far the thread sits below its place, gliding back to it. */
    fun glideY(): Float {
        val p = ((now - glideT0) / RowMs).toFloat().coerceIn(0f, 1f)
        if (p >= 1f) return 0f
        val e = 1 - (1 - p).pow(3)
        return glideFrom * (1 - e)
    }

    /** The small jolt of row [id]'s bubble as its band lets go. */
    fun jolt(id: Long): Offset {
        if (id != joltId) return Offset.Zero
        val p = ((now - joltT0) / 320.0).toFloat()
        if (p !in 0f..1f) return Offset.Zero
        return Offset(
            sin(p * PI.toFloat() * 3) * (1 - p) * 2.5f * k,
            -sin(p * PI.toFloat()) * 3 * k,
        )
    }

    /** The caption's opacity, fading over 0.3 s as the prototype's CSS transition. */
    fun captionAlpha(): Float {
        val p = ((now - capT0) / 300.0).toFloat().coerceIn(0f, 1f)
        return capFrom + (capTo - capFrom) * cssEase(p)
    }

    private fun showCaption(on: Boolean) {
        if (on == capOn) return
        capOn = on
        capFrom = captionAlpha()
        capTo = if (on) 1f else 0f
        capT0 = if (reducedMotion) -1e9 else now
    }

    // ---- The bubble and band ----

    /**
     * The tethered bubble as drawn: its laid-out rect moved by its lean and scaled about its left
     * middle.
     */
    fun bubbleRect(t: Tether): Rect? {
        val r = bubbleBase ?: return null
        val cy = r.top + r.height / 2
        val h = r.height * t.sc
        val x = r.left + t.off.x
        val y = cy + t.off.y - h / 2
        return Rect(x, y, x + r.width * t.sc, y + h)
    }

    private fun knot(t: Tether): Offset {
        val r = bubbleRect(t) ?: return Offset.Zero
        return Offset(r.left + 13 * k, r.bottom - 1.5f * k)
    }

    private fun restC(a: Offset, b: Offset) = Offset((a.x + b.x) / 2, (a.y + b.y) / 2 + 7 * k)

    fun bandOf(t: Tether, out: Band): Band {
        val a = knot(t)
        val b = Offset(eye.x, eye.y + eyeR * .2f)
        val r = restC(a, b)
        val u = t.u
        val pr = q(a, r, b, u)
        val p = pr + t.d
        val straight = (hypot(t.d.x, t.d.y) / (40 * k)).coerceIn(0f, 1f) * .75f
        fun half(s: Offset, cr: Offset, e: Offset): Offset {
            val c = Offset(cr.x + t.d.x * .5f, cr.y + t.d.y * .5f)
            return Offset(
                lerp(c.x, (s.x + e.x) / 2, straight),
                lerp(c.y, (s.y + e.y) / 2, straight),
            )
        }
        out.a = a
        out.b = b
        out.r = r
        out.p = p
        out.u = u
        out.c1 = half(a, Offset(lerp(a.x, r.x, u), lerp(a.y, r.y, u)), p)
        out.c2 = half(p, Offset(lerp(r.x, b.x, u), lerp(r.y, b.y, u)), b)
        var l = 0f
        var q0 = a
        for (i in 1..40) {
            val qq = out.at(i / 40f)
            l += hypot(qq.x - q0.x, qq.y - q0.y)
            q0 = qq
        }
        out.length = l
        out.rest = curveLen(a, r, b)
        out.stretch = max(0f, l - out.rest - 1 * k)
        return out
    }

    fun life(t: Tether?): Float =
        if (t == null) 1f else (t.elapsed / UnsendMs).toFloat().coerceIn(0f, 1f)

    fun bandWidth(t: Tether?, b: Band): Float {
        val l = life(t)
        val base = lerp(5.4f, 2.1f, l.pow(1.3f)) * k
        return base * sqrt(b.rest / max(b.length, 1f)).coerceIn(.42f, 1f)
    }

    private val scratch = Band()

    // ---- Sending ----

    /** Sends [text], trimmed; false when there is nothing to send or a message is coming back. */
    fun send(text: String): Boolean {
        val s = text.trim()
        if (s.isEmpty() || busy) return false
        tether?.let { snap(it) }
        val last = rows.lastOrNull()
        val row = ChatRow(nextId++, Who.Me, s, gap = last?.who != Who.Me)
        rows.add(row)
        glidePending = true
        val t = Tether(row)
        tether = t
        tetheredId = row.id
        bubbleBase = null
        captionAt = null
        sent++
        capOn = false
        capFrom = 0f
        capTo = 0f
        if (reducedMotion) {
            t.mode = TetherMode.Live
            showCaption(true)
        } else {
            t.riseT0 = now
            t.sc = .86f
            later(RiseMs * .6) { if (tether === t && t.mode != TetherMode.Fly) showCaption(true) }
        }
        onEvent?.invoke(BandEvent.Send, 1f)
        onSay?.invoke(BandSay.Sent)
        wake++
        return true
    }

    // ---- Snap: the window ran out ----
    fun snap(t: Tether) {
        if (tether !== t) return
        val b = bandOf(t, Band())
        if (drag?.t === t) drag = null
        t.held = false
        tether = null
        tetheredId = null
        snaps++
        if (!reducedMotion) breaks.add(BandBreak(b, now, bandWidth(t, b)))
        t.mode = TetherMode.Done
        capOn = false
        capFrom = 0f
        capTo = 0f
        t.off = Offset.Zero
        t.sc = 1f
        if (!reducedMotion) {
            joltId = t.row.id
            joltT0 = now
        }
        receiptId = t.row.id
        onEvent?.invoke(BandEvent.Twang, 1f)
        onSay?.invoke(BandSay.Delivered)
        wake++
    }

    // ---- Unsend: the band yanks it back ----
    fun unsend(t: Tether, scripted: Boolean = false) {
        if (t.mode == TetherMode.Fly || t.mode == TetherMode.Done) return
        val base = bubbleBase ?: return
        t.mode = TetherMode.Fly
        t.held = false
        // A finger still on the band (the caption tapped with another) lets go with it.
        if (drag?.t === t) drag = null
        busy = true
        unsendScripted = scripted
        showCaption(false)
        t.flyO0 = t.off
        t.flyS0 = t.sc
        t.flyTo = Offset(fieldX - 3 * k - base.left, fieldCy - (base.top + base.height / 2))
        t.flyT0 = now
        t.flyD0 = t.d
        onEvent?.invoke(BandEvent.Yank, 1f)
        if (reducedMotion) finishUnsend(t, true)
        wake++
    }

    private fun pop(t: Tether) {
        val text = t.row.text
        val base = bubbleBase ?: return
        val r = bubbleRect(t) ?: return
        val starts = letterStarts(text)
        val n = starts.size
        val from = FloatArray(n * 2)
        val to = FloatArray(n)
        val g = glyphs
        val total =
            if (g != null) {
                g.inBubble(text, starts, from)
                g.inField(text, starts, to)
            } else 0f
        val bs = BubbleFont * t.sc
        val avail = fieldRight - fieldX
        // The field scrolls the caret into view at its end.
        val shift = if (total > avail) total + CaretW - avail else 0f
        val cy = base.top + base.height / 2
        letters =
            List(n) { i ->
                // The letter's place in the bubble, moved and scaled as the bubble is now.
                val x0 = base.left + t.off.x + from[i * 2] * t.sc
                val y0 = cy + t.off.y + (base.top + from[i * 2 + 1] - cy) * t.sc
                // Where it will sit once the field holds the text, scrolled to its end.
                val x1 = fieldX + to[i] - shift
                val seed = sin(i * 12.9898 + text.length) * 43758.5453
                val rnd = (seed - floor(seed)).toFloat()
                FlyingLetter(
                    text.substring(starts[i], if (i + 1 < n) starts[i + 1] else text.length),
                    i,
                    x0,
                    y0,
                    x1,
                    fieldCy,
                    bs,
                    FieldFont,
                    now + i * Stagger,
                    (18 + rnd * 26) * k,
                    (rnd - .5f) * 1.1f,
                    x1 < fieldX - 2,
                )
            }
        shell = r
        shellT0 = now
        val bb = bandOf(t, scratch)
        retractA = bb.a
        retractB = bb.b
        retractW = bandWidth(t, bb)
        retractT0 = now
        landing = true
        onEvent?.invoke(BandEvent.Burst, 1f)
        val end = LetterMs + Stagger * max(0, n - 1)
        // Ticks as the letters land, a few of them, not one per glyph.
        for (i in 0 until min(6, n)) later(LetterMs * .8 + i * end / 7.0) {
            onEvent?.invoke(BandEvent.Tick, i.toFloat())
        }
        later(end + 10.0) { finishUnsend(t, false) }
        later(140.0) {
            // Close the gap the row leaves.
            if (rows.remove(t.row)) glidePending = true
        }
    }

    private fun finishUnsend(t: Tether, instant: Boolean) {
        if (instant && rows.remove(t.row)) glidePending = !reducedMotion
        letters = emptyList()
        shell = null
        retractT0 = -1.0
        if (tether === t) {
            tether = null
            tetheredId = null
        }
        busy = false
        landing = false
        unsent++
        onRestore?.invoke(t.row.text, !unsendScripted)
        onSay?.invoke(BandSay.Unsent)
    }

    // ---- Input ----

    /**
     * Where on the band ([x], [y]) takes hold: -1 on the tethered bubble, else the point along the
     * band within 22 dp × k of it; null when it misses.
     */
    private fun hitTether(x: Float, y: Float): Float? {
        val t = tether ?: return null
        if (t.mode == TetherMode.Fly || t.mode == TetherMode.Done || busy) return null
        val r = bubbleRect(t) ?: return null
        if (x >= r.left - 6 && x <= r.right + 6 && y >= r.top - 6 && y <= r.bottom + 6) return -1f
        val b = bandOf(t, scratch)
        var bt = 0f
        var bd = 1e9f
        for (i in 0..48) {
            val tt = i / 48f
            val qq = b.at(tt)
            val d = hypot(qq.x - x, qq.y - y)
            if (d < bd) {
                bd = d
                bt = tt
            }
        }
        return if (bd < 22 * k) bt else null
    }

    /** A finger (or the script) comes down at ([x], [y]); true when it took hold of the band. */
    fun down(x: Float, y: Float, scripted: Boolean = false): Boolean {
        if (!scripted && drag?.scripted == true) release()
        if (drag != null) return false
        val h = hitTether(x, y) ?: return false
        val t = tether ?: return false
        val b = bandOf(t, scratch)
        // A bubble grab takes hold of the band near its knot; a band grab where the finger is.
        // Either way the
        // band stays exactly where it is, and the pluck point then moves with the finger, so a tap
        // adds no stretch.
        val u = (if (h < 0f) .14f else h).coerceIn(.12f, .88f)
        val p0 = b.at(u)
        val pr = q(b.a, b.r, b.b, u)
        t.u = u
        t.d = p0 - pr
        if (t.mode == TetherMode.Rise) {
            t.mode = TetherMode.Live
            t.off = Offset.Zero
            t.sc = 1f
            showCaption(true)
        }
        t.held = true
        t.dv = Offset.Zero
        drag = Drag(t, p0, Offset(x, y), scripted)
        onEvent?.invoke(BandEvent.Grab, 1f)
        wake++
        return true
    }

    fun move(x: Float, y: Float, scripted: Boolean = false) {
        val dr = drag ?: return
        if (dr.scripted != scripted) return
        val t = dr.t
        if (t.mode == TetherMode.Fly || t.mode == TetherMode.Done) return
        val p = Offset(dr.p0.x + x - dr.f0.x, dr.p0.y + y - dr.f0.y)
        // Lean the bubble toward the pull, a little.
        val b0 = bandOf(t, scratch)
        val a0 = b0.a - t.off
        val dx = p.x - a0.x
        val dy = p.y - a0.y
        val dd = hypot(dx, dy).takeIf { it > 0f } ?: 1f
        val lean = min(b0.stretch * .12f, 9 * k)
        t.off = Offset(dx / dd * lean, dy / dd * lean)
        val a = a0 + t.off
        val pr = q(a, restC(a, b0.b), b0.b, t.u)
        t.d = p - pr
        // The pull is how far the finger has gone from where it grabbed, in any direction.
        dr.pull = hypot(x - dr.f0.x, y - dr.f0.y)
        val taut = dr.pull > Pull * k
        if (taut != dr.taut) {
            dr.taut = taut
            if (taut) onEvent?.invoke(BandEvent.Taut, 1f)
        }
        wake++
    }

    fun up(x: Float, y: Float, scripted: Boolean = false) {
        val dr = drag ?: return
        if (dr.scripted != scripted) return
        move(x, y, scripted)
        letGo()
    }

    /** The system took the gesture: it finishes from the last point it saw. */
    fun cancel(scripted: Boolean = false) {
        val dr = drag ?: return
        if (dr.scripted != scripted) return
        letGo()
    }

    /** The script's hold lets go from where it is. */
    fun releaseScripted() {
        if (drag?.scripted == true) letGo()
    }

    /** The script was stopped: its hold lets go where it is, unsending nothing. */
    fun dropScripted() {
        if (drag?.scripted == true) release()
    }

    private fun release() {
        drag?.t?.held = false
        drag = null
    }

    private fun letGo() {
        val dr = drag ?: return
        val t = dr.t
        val pull = dr.pull
        drag = null
        t.held = false
        // A band that snapped (a new send) or flew home while held has nothing to twang.
        if (t.mode == TetherMode.Fly || t.mode == TetherMode.Done) return
        if (pull > Pull * k) unsend(t, dr.scripted)
        else onEvent?.invoke(BandEvent.Boing, (pull / (Pull * k)).coerceIn(.2f, 1f))
        wake++
    }

    /** The caption button: unsends at once. */
    fun unsendNow() {
        val t = tether ?: return
        if (t.mode != TetherMode.Fly) unsend(t)
    }

    // ---- The tour's other side of the chat ----
    fun showTyping() {
        rows.add(ChatRow(nextId++, Who.Typing, "", gap = true))
        glidePending = true
        wake++
    }

    fun reply(text: String) {
        rows.removeAll { it.who == Who.Typing }
        rows.add(ChatRow(nextId++, Who.Them, text, gap = true))
        glidePending = true
        wake++
    }

    /** Back to the opening thread, nothing tied, as the screen first shows it. */
    fun reset() {
        timers.clear()
        drag = null
        tether = null
        tetheredId = null
        receiptId = null
        letters = emptyList()
        shell = null
        retractT0 = -1.0
        breaks.clear()
        busy = false
        landing = false
        unsent = 0
        snaps = 0
        sent = 0
        ph = 1f
        capOn = false
        capFrom = 0f
        capTo = 0f
        glideFrom = 0f
        glidePending = false
        joltId = -1
        rows.clear()
        opening.forEachIndexed { i, (who, text) ->
            rows.add(ChatRow(nextId++, who, text, gap = i > 0 && opening[i - 1].first != who))
        }
        threadHeight = -1f
        frame++
        wake++
    }

    /** Nothing moves and nothing is waiting: the frame loop can sleep. */
    val settled: Boolean
        get() =
            tether == null &&
                timers.isEmpty() &&
                letters.isEmpty() &&
                shell == null &&
                breaks.isEmpty() &&
                ph == 1f &&
                now - glideT0 > RowMs &&
                now - joltT0 > 320.0 &&
                now - capT0 > 300.0

    // ---- Simulation ----

    /** Runs [dt] seconds: the timers that are due, then the springs in 1/240 s steps. */
    fun advance(dt: Float) {
        now += dt * 1000.0
        var i = timers.size - 1
        while (i >= 0) {
            if (i < timers.size && timers[i].first <= now) timers.removeAt(i).second()
            i--
        }
        var left = dt
        while (left > 0f) {
            val h = min(left, 1f / 240f)
            step(h)
            left -= h
        }
        // Breaks done curling are let go.
        breaks.removeAll { now - it.t0 >= SnapMs }
        if (now - glideT0 <= RowMs + 50) glideTick++
        frame++
    }

    private fun later(ms: Double, fn: () -> Unit) {
        timers.add((now + ms) to fn)
    }

    private fun step(h: Float) {
        val t = tether
        // The placeholder fades out over 250 ms while a band is tied to the field, and back when it
        // lets go.
        val phTo = if (t != null && t.mode != TetherMode.Done) 0f else 1f
        ph =
            if (reducedMotion) phTo
            else (ph + sign(phTo - ph) * h / .25f).coerceIn(min(ph, phTo), max(ph, phTo))
        if (t == null) return
        if (t.mode == TetherMode.Rise) {
            val p = ((now - t.riseT0) / RiseMs).toFloat().coerceIn(0f, 1f)
            val e = backOut(p)
            val rise = t.rise
            if (rise != null) t.off = Offset(rise.x * (1 - e), rise.y * (1 - e))
            t.sc = lerp(.86f, 1f, min(1f, e))
            if (p >= 1f) {
                t.mode = TetherMode.Live
                t.off = Offset.Zero
                t.sc = 1f
            }
        }
        if (t.mode == TetherMode.Live && !t.held) {
            // The band's bend and the bubble's lean spring back, underdamped, so a plucked band
            // wobbles.
            t.dv += Offset(-620f * t.d.x - 7.5f * t.dv.x, -620f * t.d.y - 7.5f * t.dv.y) * h
            t.d += t.dv * h
            t.offV += Offset(-380f * t.off.x - 16f * t.offV.x, -380f * t.off.y - 16f * t.offV.y) * h
            t.off += t.offV * h
            if (abs(t.off.x) + abs(t.off.y) < .02f && abs(t.offV.x) + abs(t.offV.y) < .05f) {
                t.off = Offset.Zero
                t.offV = Offset.Zero
            }
        }
        if (t.mode == TetherMode.Live || t.mode == TetherMode.Rise) {
            // The window starts once the bubble has landed, not while it is still springing up.
            if (!t.held && t.mode == TetherMode.Live) t.elapsed += h * 1000.0
            if (t.elapsed >= UnsendMs) snap(t)
        }
        if (t.mode == TetherMode.Fly && !reducedMotion) {
            val p = ((now - t.flyT0) / FlyMs).toFloat().coerceIn(0f, 1f)
            val e = p.pow(2.1f)
            t.off = Offset(lerp(t.flyO0.x, t.flyTo.x, e), lerp(t.flyO0.y, t.flyTo.y, e))
            t.sc = lerp(t.flyS0, .84f, e)
            t.d = t.flyD0 * (1 - p)
            if (p >= 1f && !t.popped) {
                t.popped = true
                pop(t)
            }
        }
    }

    internal companion object {
        /** How long the band holds, counted from when the bubble lands, in ms. */
        const val UnsendMs = 9000.0

        /** Finger travel from the grab (dp, times k) that counts as a pull, in any direction. */
        const val Pull = 36f
        const val FlyMs = 340.0
        const val LetterMs = 470.0
        const val Stagger = 12.0
        const val RowMs = 240.0
        const val RiseMs = 420.0
        const val SnapMs = 260.0

        /** The bubble's and the field's type sizes, as the prototype's. */
        const val BubbleFont = 15.5f
        const val FieldFont = 16f

        /** The field's caret, which a text scrolled to its end leaves room for. */
        const val CaretW = 2f

        fun ease(t: Float) = if (t < .5f) 2 * t * t else 1 - (-2 * t + 2).pow(2) / 2
    }
}

internal fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

internal fun q(a: Offset, c: Offset, b: Offset, t: Float): Offset {
    val u = 1 - t
    return Offset(
        u * u * a.x + 2 * u * t * c.x + t * t * b.x,
        u * u * a.y + 2 * u * t * c.y + t * t * b.y,
    )
}

private fun curveLen(a: Offset, c: Offset, b: Offset): Float {
    var l = 0f
    var p = a
    for (i in 1..20) {
        val qq = q(a, c, b, i / 20f)
        l += hypot(qq.x - p.x, qq.y - p.y)
        p = qq
    }
    return l
}

private fun backOut(t: Float): Float {
    val c = 1.6f
    return 1 + (c + 1) * (t - 1).pow(3) + c * (t - 1).pow(2)
}

/** CSS `ease`, cubic-bezier(.25, .1, .25, 1). */
internal fun cssEase(p: Float): Float {
    if (p <= 0f) return 0f
    if (p >= 1f) return 1f
    // Solve x(s) = p for s, then y(s).
    var s = p
    repeat(8) {
        val x = bez(s, .25f, .25f) - p
        val dx = bezD(s, .25f, .25f)
        if (abs(dx) < 1e-5f) return@repeat
        s = (s - x / dx).coerceIn(0f, 1f)
    }
    return bez(s, .1f, 1f)
}

private fun bez(s: Float, p1: Float, p2: Float): Float {
    val u = 1 - s
    return 3 * u * u * s * p1 + 3 * u * s * s * p2 + s * s * s
}

private fun bezD(s: Float, p1: Float, p2: Float): Float {
    val u = 1 - s
    return 3 * u * u * p1 + 6 * u * s * (p2 - p1) + 3 * s * s * (1 - p2)
}

/** Where each letter of [text] starts: by code point, so an emoji stays whole. */
internal fun letterStarts(text: String): IntArray {
    val out = ArrayList<Int>(text.length)
    var i = 0
    while (i < text.length) {
        out.add(i)
        i +=
            if (text[i].isHighSurrogate() && i + 1 < text.length && text[i + 1].isLowSurrogate()) 2
            else 1
    }
    return out.toIntArray()
}

/** The neck the band forms over the last three seconds of the window, from 0 to .72. */
internal fun neckOf(life: Float): Float {
    val w = (3000.0 / RubberBandState.UnsendMs).toFloat()
    return ((life - (1 - w)) / w).coerceIn(0f, 1f) * .72f
}

/**
 * The band's width along it at [t], with [neck] in the middle and a slight thickening at each end.
 */
internal fun ribbonWidth(w: Float, neck: Float, t: Float): Float =
    w * (1 - neck * exp(-((t - .5f) / .11f).pow(2))) * (1 + .18f * abs(2 * t - 1).pow(6))
