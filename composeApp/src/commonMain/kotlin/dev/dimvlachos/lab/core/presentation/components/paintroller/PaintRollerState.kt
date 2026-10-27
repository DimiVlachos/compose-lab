package dev.dimvlachos.lab.core.presentation.components.paintroller

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/** Something the roller does that is heard or felt. */
internal enum class RollerEvent {
    /** The roller is rolled in a tray's paint. */
    Dip,
    /** The wall is covered: the new colour is the accent now. */
    Commit,
    /** The roller sets down in its tray. */
    Set,
}

/**
 * What the roller is doing: resting in a tray, moving by itself, lifting off to the finger, or
 * following it.
 */
internal enum class RollerMode {
    Rest,
    Auto,
    Lift,
    Follow,
}

/**
 * A tray as drawn: centred on [cx], its top at [top], [tw] by [th], and when its paint last
 * rippled.
 */
internal class Tray(val cx: Float, val top: Float, val tw: Float, val th: Float) {
    internal var ripple = -1e9
}

/** A rectangle the roller has swept, in dp, and when it was last painted. */
internal class Swept(var x0: Float, var x1: Float, var y0: Float, var y1: Float, var t: Double)

/**
 * The paint roller's state, ported from the prototype's `st`, `R`, `layout()`, the painting
 * (`addRect`, `paintSeg`, the coverage grid), its flows (`dip`, `roll`, `strokes`, `finishAndHome`,
 * `pickTray`, Undo) and its tray gesture (`trayDown`, `trayMove`, `trayUp`). Units are dp, which
 * play the prototype's CSS px; [layout] gives the screen's geometry as composed.
 *
 * [now] is the roller's own clock in ms, which [step] advances in 1/120 s substeps. The prototype's
 * `async` flows are suspend functions here, on a scheduler of its own: `animate` and `wait` suspend
 * until a substep finishes them, so a test steps them exactly as the screen does.
 *
 * [current] is the accent the base coat wears; while painting, [coat] is the colour on the roller,
 * and the new coat shows it wherever [rects] cover.
 */
@Stable
internal class PaintRollerState(val paintCount: Int = 5, val wetCount: Int = 7) {
    var reducedMotion = false

    /** Hears what the roller does. */
    var onEvent: ((RollerEvent) -> Unit)? = null

    /** Bumped on every [step], so a drawing that reads it redraws each frame. */
    var frame by mutableIntStateOf(0)
        private set

    /** Bumped whenever something sets the roller moving, so a resting frame loop wakes. */
    var wake by mutableIntStateOf(0)
        private set

    /** The accent the base coat wears (`st.cur`). */
    var current by mutableIntStateOf(0)
        private set

    /** The colour on the roller while it paints, or -1 (`st.to`). */
    var coat by mutableIntStateOf(-1)
        private set

    /** The snackbar with Undo is up. */
    var snackOn by mutableStateOf(false)
        private set

    /** What was last announced: the colour, and a count, so the same colour said again is heard. */
    var said by mutableStateOf(-1 to 0)
        private set

    /** The screen's geometry has been given. */
    var laidOut by mutableStateOf(false)
        private set

    /** Where the snackbar's bottom edge sits above the screen's bottom, in dp. */
    var snackBottom by mutableStateOf(12f)
        private set

    var now = 0.0
        private set

    /** Where the snackbar's Undo is, in dp, once it has been placed: for a scripted tap. */
    var undoCentre: Offset? = null

    // ---- Layout ----
    var width = 0f
        private set

    var height = 0f
        private set

    var k = 1f
        private set

    /** The roller's length and diameter at full size. */
    var rw = 0f
        private set

    var rd = 0f
        private set

    /** How far the roller's centre sits above the finger. */
    var grip = 0f
        private set

    /** Where the wall ends: just above the accent section. */
    var paintBottom = 0f
        private set

    val trays = mutableListOf<Tray>()

    /** The trays' touch targets, in dp. */
    var trayRects: List<Rect> = emptyList()
        private set

    private var wetPos: List<Offset> = emptyList()

    // ---- The roller (`R`) ----
    var rx = 0f
        private set

    var ry = 0f
        private set

    var rs = .3f
        private set

    var phase = 0f
        private set

    /** The paint on the roller, by index. */
    var rollerPaint = 0
        private set

    var mode = RollerMode.Rest
        private set

    private var tx = 0f
    private var ty = 0f
    private var paintOn = false
    private var liftAt = 0.0
    private var lx = 0f
    private var ly = 0f
    private var ls = 0f
    var dip = 0f
        private set

    // ---- Painting (`st`) ----
    private var rest = 0
    var busy = false
        private set

    var previous = -1
        private set

    private val paintedAt = DoubleArray(wetCount) { -1e9 }
    private val baseWetAt = DoubleArray(wetCount) { -1e9 }
    private var coatAt = 0.0

    /** The swept rectangles, in dp: the new coat's clip is their union. */
    val rects = mutableListOf<Swept>()

    /** Bumped whenever [rects] change, so the clip is rebuilt, and redrawn, only then. */
    var paintVersion by mutableIntStateOf(0)
        private set

    private var grid = ByteArray(0)
    var cols = 0
        private set

    var rows = 0
        private set

    private var covered = 0
    private var undoing = false
    private var snackAt = -1.0

    /** How loud the roll is, 0 to .5, as the prototype's `rollLevel`. */
    var rollLevel = 0f
        private set

    private var lastRX = 0f
    private var lastRY = 0f

    // ---- Scheduler ----
    private class Anim(
        val t0: Double,
        val ms: Double,
        val tok: Int?,
        val fn: (Float) -> Unit,
        val done: () -> Unit,
    )

    private class Timer(val at: Double, val fn: () -> Unit)

    private class Pending {
        var done = false
        private val waiters = mutableListOf<() -> Unit>()

        fun complete() {
            done = true
            val w = waiters.toList()
            waiters.clear()
            w.forEach { it() }
        }

        suspend fun await() {
            if (done) return
            suspendCoroutine { c -> waiters += { c.resume(Unit) } }
        }
    }

    private val anims = mutableListOf<Anim>()
    private val timers = mutableListOf<Timer>()
    private val finished = mutableListOf<() -> Unit>()
    private var runTok = 0

    private suspend fun animate(ms: Double, tok: Int? = null, fn: (Float) -> Unit) =
        suspendCoroutine { c ->
            anims += Anim(now, ms, tok, fn) { c.resume(Unit) }
        }

    private suspend fun wait(ms: Double) = suspendCoroutine { c ->
        timers += Timer(now + ms) { c.resume(Unit) }
    }

    /** The prototype's `untilSim`: true once [cond] holds, false if [ms] pass first. */
    private suspend fun until(ms: Double, cond: () -> Boolean): Boolean {
        val end = now + ms
        while (true) {
            if (cond() || now >= end) return cond()
            wait(16.0)
        }
    }

    private fun launch(block: suspend () -> Unit): Pending {
        val p = Pending()
        block.startCoroutine(
            Continuation(EmptyCoroutineContext) {
                it.getOrThrow()
                p.complete()
            },
        )
        poke()
        return p
    }

    private fun poke() {
        wake++
    }

    // ---- Layout ----

    /**
     * The screen as composed: its size, the top of the accent section, the trays' touch targets,
     * the centres of the accent fills (as [wetCount]) and the bottom of the trays, all in dp.
     */
    fun layout(
        w: Float,
        h: Float,
        accentTop: Float,
        trayButtons: List<Rect>,
        wetCentres: List<Offset>,
    ) {
        if (w <= 0f || h <= 0f || trayButtons.size != paintCount) return
        val pb = accentTop - 2f
        if (
            laidOut &&
                w == width &&
                h == height &&
                pb == paintBottom &&
                trayButtons == trayRects &&
                wetCentres == wetPos
        )
            return
        width = w
        height = h
        k = (w / 360f).coerceIn(.85f, 1.3f)
        rw = w * .56f
        rd = rw / 5.4f
        grip = 74f * k
        paintBottom = pb
        trayRects = trayButtons
        trays.clear()
        for (r in trayButtons) {
            val tw = min(r.width - 4f, 62f * k)
            trays += Tray(r.left + r.width / 2f, r.top + 4f, tw, tw * .86f)
        }
        wetPos = wetCentres
        // A cell counts if its centre is on the wall: inside the screen and above the trays.
        val c = max(1, ceil((w - 4f) / 8f).toInt())
        val rr = max(1, ceil((paintBottom - 4f) / 8f).toInt())
        if (grid.size != c * rr || c != cols) {
            cols = c
            rows = rr
            grid = ByteArray(c * rr)
            rebuildGrid()
        }
        // The snackbar sits at the bottom unless that would cover the trays; then just above them.
        val trayBot = trayButtons[0].bottom
        snackBottom = if (h - trayBot < 76f) h - paintBottom + 6f else 12f
        if (mode == RollerMode.Rest) {
            restPose(rest).let { (x, y, s) ->
                rx = x
                ry = y
                rs = s
            }
            if (!laidOut) {
                lastRX = rx
                lastRY = ry
                rollerPaint = current
            }
        }
        laidOut = true
        paintVersion++
        poke()
    }

    private fun miniS() = if (trays.isNotEmpty()) trays[0].tw * 1.08f / rw else .3f

    private fun restPose(i: Int): Triple<Float, Float, Float> {
        val t = trays[i]
        return Triple(t.cx, t.top + t.th * .3f, miniS())
    }

    // ---- Painting ----

    private fun rebuildGrid() {
        grid.fill(0)
        covered = 0
        for (r in rects) markRect(r)
    }

    private fun markRect(r: Swept) {
        val c0 = max(0, ceil((r.x0 - 4f) / 8f).toInt())
        val c1 = min(cols - 1, floor((r.x1 - 4f) / 8f).toInt())
        val r0 = max(0, ceil((r.y0 - 4f) / 8f).toInt())
        val r1 = min(rows - 1, floor((r.y1 - 4f) / 8f).toInt())
        for (j in r0..r1) for (i in c0..c1) {
            val n = j * cols + i
            if (grid[n].toInt() == 0) {
                grid[n] = 1
                covered++
            }
        }
    }

    /** Every cell of the wall is covered. */
    val done
        get() = covered >= cols * rows

    /** How much of the wall the new coat covers, 0 to 1. */
    val coverage
        get() = covered / max(1, cols * rows).toFloat()

    /** The grid cell at column [c], row [r] is painted. */
    fun isPainted(c: Int, r: Int) = grid[r * cols + c].toInt() != 0

    private fun addRect(x: Float, y: Float) {
        val r = Swept(x - rw / 2f, x + rw / 2f, y - rd / 2f, y + rd / 2f, now)
        val l = rects.lastOrNull()
        // Straight up-and-down travel grows the last rectangle rather than piling up new ones.
        if (
            l != null &&
                abs(l.x0 - r.x0) < .75f &&
                r.y0 <= l.y1 + 1 &&
                r.y1 >= l.y0 - 1 &&
                now - l.t < 400
        ) {
            l.y0 = min(l.y0, r.y0)
            l.y1 = max(l.y1, r.y1)
            l.t = now
            markRect(l)
        } else if (
            l != null &&
                abs(l.y0 - r.y0) < .75f &&
                abs(l.y1 - r.y1) < .75f &&
                r.x0 <= l.x1 + 1 &&
                r.x1 >= l.x0 - 1 &&
                now - l.t < 400
        ) {
            // Sideways travel grows it the other way.
            l.x0 = min(l.x0, r.x0)
            l.x1 = max(l.x1, r.x1)
            l.t = now
            markRect(l)
        } else {
            // Going back over fresh paint adds nothing new: refresh the rectangle that covers it.
            for (j in rects.size - 1 downTo max(0, rects.size - 24)) {
                val q = rects[j]
                if (
                    r.x0 >= q.x0 - .5f &&
                        r.x1 <= q.x1 + .5f &&
                        r.y0 >= q.y0 - .5f &&
                        r.y1 <= q.y1 + .5f
                ) {
                    q.t = now
                    return
                }
            }
            rects += r
            markRect(r)
        }
    }

    private fun paintSeg(x0: Float, y0: Float, x1: Float, y1: Float) {
        val n = max(1, ceil(hypot(x1 - x0, y1 - y0) / 5f).toInt())
        for (i in 1..n) {
            val x = lerp(x0, x1, i / n.toFloat())
            val y = lerp(y0, y1, i / n.toFloat())
            if (y - rd / 2f < paintBottom) addRect(x, y)
        }
        paintVersion++
    }

    private fun clearPaint() {
        rects.clear()
        rebuildGrid()
        paintVersion++
    }

    /**
     * The new coat's clip: the swept rectangles, each wound the same way so the nonzero rule unions
     * them, from just above the screen down to the top of the trays, scaled to px.
     */
    fun paintedPath(path: Path, density: Float) {
        path.reset()
        for (r in rects) {
            path.addRect(
                Rect(
                    r.x0 * density,
                    max(-2f, r.y0) * density,
                    r.x1 * density,
                    min(paintBottom, r.y1) * density,
                ),
            )
        }
    }

    // ---- Actions ----

    private fun say(i: Int) {
        said = i to said.second + 1
    }

    private fun beginCoat(i: Int) {
        coat = i
        busy = true
        coatAt = now
        clearPaint()
        hideSnack()
    }

    private fun commit() {
        if (coat < 0) return
        previous = current
        current = coat
        coat = -1
        baseWetAt.indices.forEach { baseWetAt[it] = paintedAt[it] }
        clearPaint()
        say(current)
        if (!undoing) showSnack() else hideSnack()
        undoing = false
        onEvent?.invoke(RollerEvent.Commit)
    }

    private fun showSnack() {
        snackOn = true
        snackShown++
        snackAt = now + 5000
    }

    /** How many times the snackbar has come up, so a timer can tell its showing from the next. */
    var snackShown by mutableIntStateOf(0)
        private set

    /**
     * The snackbar's five seconds are up while the roller's clock may be asleep: takes down showing
     * [shown], if it is still the one up.
     */
    fun timeOutSnack(shown: Int) {
        if (snackOn && snackShown == shown) hideSnack()
    }

    private fun hideSnack() {
        snackOn = false
        snackAt = -1.0
    }

    /** Reduced motion: the colour changes at once, with no trip for the roller. */
    private fun instant(i: Int) {
        beginCoat(i)
        commit()
        rest = i
        busy = false
        restPose(i).let { (x, y, s) ->
            rx = x
            ry = y
            rs = s
        }
        mode = RollerMode.Rest
        rollerPaint = i
        lastRX = rx
        lastRY = ry
    }

    /** Hop the roller to tray [i] and roll it in the paint. */
    private suspend fun dip(i: Int, quick: Boolean, tok: Int) {
        val t = trays[i]
        val fx = rx
        val fy = ry
        val (toX, toY, toS) = restPose(i)
        mode = RollerMode.Auto
        if (rest != i || hypot(fx - toX, fy - toY) > 2f) {
            animate(if (quick) 160.0 else 280.0, tok) { p ->
                val e = ease(p)
                val arc = sin(p * PI.toFloat())
                rx = lerp(fx, toX, e)
                ry = lerp(fy, toY, e) - arc * 26f * k
                rs = toS * (1f + arc * .12f)
            }
            if (tok != runTok) return
        }
        rest = i
        onEvent?.invoke(RollerEvent.Dip)
        t.ripple = now
        rollerPaint = i
        animate(if (quick) 150.0 else 320.0, tok) { p ->
            ry = toY + sin(p * PI.toFloat() * 2f) * t.th * .14f
            phase += 2.4f
            dip = sin(p * PI.toFloat())
        }
        dip = 0f
    }

    /** Move the roller by itself, painting where it goes; [s] is the scale it grows to. */
    private suspend fun roll(
        x: Float,
        y: Float,
        s: Float,
        ms: Double,
        paint: Boolean = true,
        curve: (Float) -> Float = ::ease,
    ) {
        val x0 = rx
        val y0 = ry
        val s0 = rs
        animate(ms) { p ->
            val e = curve(p)
            rx = lerp(x0, x, e)
            ry = lerp(y0, y, e)
            rs = lerp(s0, s, min(1f, e * 1.6f))
            paintOn = paint
        }
    }

    val colL
        get() = rw / 2f - 3f

    val colR
        get() = width - rw / 2f + 3f

    val topY
        get() = rd / 2f - 3f

    val botY
        get() = paintBottom - rd / 2f + 3f

    private suspend fun autoPaint(i: Int, tok: Int) {
        dip(i, false, tok)
        if (tok != runTok) return
        strokes(tok)
    }

    /**
     * Lift off the tray to the foot of the left stroke, up the left, across the top, down the
     * right.
     */
    private suspend fun strokes(tok: Int) {
        roll(colL, botY, 1f, 300.0, false, ::easeOut)
        if (tok != runTok) return
        roll(colL, topY, 1f, 720.0)
        if (tok != runTok) return
        roll(colR, topY, 1f, 280.0)
        if (tok != runTok) return
        roll(colR, botY, 1f, 720.0)
        if (tok != runTok) return
        finishAndHome(tok)
    }

    /** Roll any column that still shows the old colour, then put the roller back in its tray. */
    private suspend fun finishAndHome(tok: Int) {
        var pass = 0
        while (pass < 2 && !done) {
            for (cx in floatArrayOf(colL, colR)) {
                var lo = Int.MAX_VALUE
                var hi = -1
                val c0 = max(0, ceil((cx - rw / 2f - 4f) / 8f).toInt())
                val c1 = min(cols - 1, floor((cx + rw / 2f - 4f) / 8f).toInt())
                for (j in 0 until rows) for (c in c0..c1) {
                    if (grid[j * cols + c].toInt() == 0) {
                        lo = min(lo, j)
                        hi = max(hi, j)
                    }
                }
                if (hi < 0) continue
                val ya = (lo * 8f + 4f + rd / 2f - 6f).coerceIn(topY, botY)
                val yb = (hi * 8f + 4f - rd / 2f + 6f).coerceIn(topY, botY)
                // Start from whichever end of the gap is nearer, then roll through it.
                val (y1, y2) = if (abs(ry - ya) < abs(ry - yb)) ya to yb else yb to ya
                roll(cx, y1, 1f, 240.0, true)
                if (tok != runTok) return
                roll(
                    cx,
                    y2 + sign(y2 - y1) * 2f,
                    1f,
                    (abs(y2 - y1) * 1.4).coerceIn(160.0, 700.0),
                )
                if (tok != runTok) return
            }
            pass++
        }
        if (!done) {
            grid.fill(1)
            covered = cols * rows
        }
        commit()
        val (hx, hy, hs) = restPose(rest)
        paintOn = false
        roll(hx, hy, hs, 420.0, false)
        if (tok != runTok) return
        onEvent?.invoke(RollerEvent.Set)
        mode = RollerMode.Rest
        busy = false
    }

    /**
     * Picks paint [i], as a tap on its tray does: the roller hops over, dips and paints the screen
     * in two strokes. Ignored while it is busy; the current colour is only announced. [viaUndo]
     * leaves the snackbar down.
     */
    fun pickTray(i: Int, viaUndo: Boolean = false): Boolean {
        if (busy || !laidOut) return false
        if (i == current) {
            say(i)
            return false
        }
        undoing = viaUndo
        if (reducedMotion) {
            instant(i)
            poke()
            return true
        }
        beginCoat(i)
        val tok = ++runTok
        launch { autoPaint(i, tok) }
        return true
    }

    /** Arrow keys on the trays: the next colour round, or the one before. */
    fun pickNext(by: Int): Int {
        val j = ((current + by) % paintCount + paintCount) % paintCount
        pickTray(j)
        return j
    }

    /** Undo rolls the old colour back on; tapped while the roller goes home, it waits for it. */
    fun undo() {
        if (previous < 0) return
        val p = previous
        if (!busy) {
            pickTray(p, true)
        } else {
            launch {
                val ok = until(1500.0) { !busy }
                if (ok && current != p) pickTray(p, true)
            }
        }
    }

    /** Reset to default: Blue Ridge rolled back on. */
    fun resetToDefault() {
        pickTray(0)
    }

    // ---- Tray gesture ----

    private class Drag(
        val i: Int,
        val x0: Float,
        val y0: Float,
        val tok: Int,
        val demo: Boolean,
    ) {
        var moved = false
        var dipP: Pending? = null
    }

    private var drag: Drag? = null

    /** A finger is down on a tray. */
    val dragging
        get() = drag != null

    /**
     * A finger comes down on tray [i] at ([x], [y]); [demo] marks the script's finger. True when
     * the press is now this finger's drag: only then are its moves and lift to be passed on.
     */
    fun trayDown(i: Int, x: Float, y: Float, demo: Boolean = false): Boolean {
        if (!laidOut) return false
        if (reducedMotion) {
            pickTray(i)
            return false
        }
        if (busy) return false
        val d = Drag(i, x, y, ++runTok, demo)
        drag = d
        busy = true
        // Dip at once, quickly, so a drag can begin straight away.
        if (i != current) {
            beginCoat(i)
            d.dipP = launch { dip(i, true, d.tok) }
        } else {
            rollerPaint = i
        }
        poke()
        return true
    }

    fun trayMove(x: Float, y: Float) {
        val d = drag ?: return
        if (!d.moved && hypot(x - d.x0, y - d.y0) > Slop) {
            d.moved = true
            if (d.i == current) {
                drag = null
                busy = false
                return
            }
            runTok++ // cancel the dip's tail; the finger takes over
            startLift(d.i)
        }
        if (d.moved) {
            tx = x
            ty = y - grip
        }
        poke()
    }

    private fun startLift(i: Int) {
        mode = RollerMode.Lift
        liftAt = now
        lx = rx
        ly = ry
        ls = rs
        rollerPaint = i
        dip = 0f
    }

    fun trayUp(x: Float, y: Float) {
        val d = drag ?: return
        drag = null
        poke()
        val far = d.moved || hypot(x - d.x0, y - d.y0) > Slop
        if (!far) {
            // A tap: the dip has already started; carry on into the two strokes.
            busy = false
            if (d.i == current) {
                say(d.i)
                return
            }
            busy = true
            val tok = d.tok
            launch {
                d.dipP?.await()
                if (tok == runTok) strokes(tok)
            }
            return
        }
        // The current colour: nothing to paint, the roller stays put.
        if (d.i == current) {
            busy = false
            return
        }
        if (!d.moved) {
            // Released far away with no moves in between: lift straight to the release point.
            runTok++
            startLift(d.i)
        }
        tx = x
        ty = y - grip
        val tok = ++runTok
        // Let the roller catch up with where the finger left it, then finish the wall.
        launch {
            until(900.0) { mode == RollerMode.Follow && hypot(tx - rx, ty - ry) < 1.5f }
            if (tok != runTok) return@launch
            mode = RollerMode.Auto
            finishAndHome(tok)
        }
    }

    /** A drag the system took away still finishes, from where the finger last was. */
    fun trayCancel() {
        val d = drag ?: return
        if (d.moved) trayUp(tx, ty + grip) else trayUp(d.x0, d.y0)
    }

    /** The script stopped mid-drag: its finger lets go where it was, so the roller finishes. */
    fun releaseDemo(x: Float, y: Float) {
        if (drag?.demo == true) trayUp(x, y)
    }

    // ---- Frame ----

    /** Advances the roller by [dt] seconds, in 1/120 s substeps. */
    fun step(dt: Float) {
        if (!laidOut) return
        var left = dt.coerceIn(0f, .15f)
        do {
            val h = min(left, 1f / 120f)
            now += h * 1000.0
            var i = timers.size - 1
            while (i >= 0) {
                if (i < timers.size && timers[i].at <= now) finished += timers.removeAt(i).fn
                i--
            }
            i = anims.size - 1
            while (i >= 0) {
                if (i < anims.size) {
                    val a = anims[i]
                    if (a.tok != null && a.tok != runTok) {
                        anims.removeAt(i)
                        finished += a.done
                    } else {
                        val p = ((now - a.t0) / a.ms).coerceIn(0.0, 1.0).toFloat()
                        a.fn(p)
                        if (p >= 1f) {
                            anims.removeAt(i)
                            finished += a.done
                        }
                    }
                }
                i--
            }
            follow(h)
            afterMove(h)
            // What finished resumes after the substep, as the prototype's promises resolve after
            // the frame that settled them.
            while (finished.isNotEmpty()) {
                val f = finished.toList()
                finished.clear()
                f.forEach { it() }
            }
            left -= h
        } while (left > 1e-6f)
        frame++
    }

    private fun follow(dt: Float) {
        if (mode != RollerMode.Follow && mode != RollerMode.Lift) return
        val a = 1f - exp(-dt * 30f)
        if (mode == RollerMode.Lift) {
            val p = ((now - liftAt) / 230.0).coerceIn(0.0, 1.0).toFloat()
            val e = easeOut(p)
            rx = lerp(lx, tx, e)
            ry = lerp(ly, ty, e)
            rs = lerp(ls, 1f, e)
            if (p >= 1f) mode = RollerMode.Follow
        } else {
            rx += (tx - rx) * a
            ry += (ty - ry) * a
            rs += (1f - rs) * a
        }
    }

    private fun afterMove(dt: Float) {
        val dx = rx - lastRX
        val dy = ry - lastRY
        val painting =
            coat >= 0 &&
                rs > .9f &&
                (mode == RollerMode.Follow ||
                    mode == RollerMode.Lift ||
                    (mode == RollerMode.Auto && paintOn))
        if (painting && (dx != 0f || dy != 0f)) paintSeg(lastRX, lastRY, rx, ry)
        phase += dy * 1.6f
        val speed = if (painting && dt > 0f) abs(dy) / dt else 0f
        rollLevel = lerp(rollLevel, (speed / 900f).coerceIn(0f, 1f) * .5f, .25f)
        lastRX = rx
        lastRY = ry
        // Accent fills dry: wet from the moment the roller first covers their middle.
        if (coat >= 0) {
            for (i in wetPos.indices) {
                if (i >= wetCount) break
                val (x, y) = wetPos[i]
                if (y >= paintBottom) continue
                val c = ((x - 4f) / 8f).roundToInt().coerceIn(0, cols - 1)
                val r = ((y - 4f) / 8f).roundToInt().coerceIn(0, rows - 1)
                if (grid[r * cols + c].toInt() != 0 && paintedAt[i] < coatAt) paintedAt[i] = now
            }
        }
        if (snackAt >= 0 && now > snackAt) hideSnack()
    }

    /** How wet the new coat's fill [i] is, 0 to 1: each coat dries on its own clock. */
    fun wetCoat(i: Int) = wet(paintedAt[i])

    /** How wet the base coat's fill [i] still is from the paint it wears. */
    fun wetBase(i: Int) = wet(baseWetAt[i])

    private fun wet(at: Double): Float {
        if (reducedMotion) return 0f
        val w = (1.0 - (now - at) / 2600.0).coerceIn(0.0, 1.0).toFloat()
        return w * w
    }

    /** Nothing moves, dries, fades or waits: the frame loop can sleep. */
    val settled: Boolean
        get() {
            if (!laidOut) return true
            // A finger held still on the current colour's tray moves nothing.
            val holding = drag?.let { it.i == current && !it.moved } == true
            if (anims.isNotEmpty() || timers.isNotEmpty() || (busy && !holding)) return false
            if (drag != null && !holding) return false
            if (mode != RollerMode.Rest || rollLevel > .002f) return false
            if (paintedAt.any { now - it < 2600 } || baseWetAt.any { now - it < 2600 }) return false
            if (trays.any { now - it.ripple < 700 }) return false
            return true
        }

    /** Back to Blue Ridge at rest, as the demo begins. */
    fun reset() {
        timers.clear()
        anims.clear()
        finished.clear()
        runTok++
        drag = null
        undoing = false
        current = 0
        coat = -1
        rest = 0
        busy = false
        previous = -1
        paintedAt.fill(-1e9)
        baseWetAt.fill(-1e9)
        coatAt = 0.0
        clearPaint()
        hideSnack()
        mode = RollerMode.Rest
        rollerPaint = 0
        dip = 0f
        paintOn = false
        rollLevel = 0f
        if (trays.isNotEmpty())
            restPose(0).let { (x, y, s) ->
                rx = x
                ry = y
                rs = s
            }
        lastRX = rx
        lastRY = ry
        poke()
    }

    internal companion object {
        /** How far a finger moves before a press on a tray is a drag. */
        const val Slop = 10f

        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

        /** The prototype's ease: quadratic in, quadratic out. */
        fun ease(t: Float) = if (t < .5f) 2 * t * t else 1 - (-2 * t + 2).pow(2) / 2

        /** Cubic ease-out. */
        fun easeOut(t: Float) = 1 - (1 - t).pow(3)
    }
}
