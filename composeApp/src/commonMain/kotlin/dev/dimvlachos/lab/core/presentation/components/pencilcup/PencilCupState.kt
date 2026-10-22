package dev.dimvlachos.lab.core.presentation.components.pencilcup

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * One tool in the cup, in design units that [PencilCupState.k] scales: its [length] and [width],
 * how far from the cup its tip lands when fanned ([reach]) at [fanAngle], where it stands in the
 * cup ([restX], [restAngle]), the label's [gap] from its tip, and the most it may lean right
 * ([lean]), where the cup's wall stops it.
 */
internal class CupToolSpec(
    val length: Float,
    val width: Float,
    val reach: Float,
    val fanAngle: Float,
    val restX: Float,
    val restAngle: Float,
    val gap: Float,
    val lean: Float = Float.NaN,
)

/**
 * The four tools, back to front: a loupe, scissors, a highlighter and a pencil, as the prototype.
 */
internal object CupTools {
    const val Scan = 0
    const val Clip = 1
    const val List = 2
    const val Note = 3

    val specs =
        listOf(
            CupToolSpec(108f, 40f, 134f, -1.28f, -11f, -.27f, 28f),
            CupToolSpec(112f, 18f, 150f, -.86f, 3f, .23f, 16f, lean = .3f),
            CupToolSpec(116f, 17f, 166f, -.43f, 3f, .05f, 14f),
            CupToolSpec(128f, 11f, 196f, -.07f, -6f, -.11f, 14f),
        )

    /** The menu reads top to bottom: note, checklist, clip, scan. */
    val menuOrder = intArrayOf(Note, List, Clip, Scan)

    val count = specs.size
}

/** Something the cup does that is heard or felt. */
internal enum class CupEvent {
    /** A tool leaps out of the cup, or a drag lights it. */
    Lift,
    /** A tool was picked and flies off into its sheet. */
    Pick,
    /** A tool lands in the cup; the strength is how hard, .15 to 1. */
    Clink,
}

/**
 * Where a tool is in its life: in the cup, fanned out, falling home, flying off, or gone into its
 * sheet.
 */
internal enum class ToolMode {
    Rest,
    Fan,
    Home,
    Fly,
    Away,
}

/** A tool's live pose: its foot at ([x], [y]), turned [a] radians, scaled [s], [al] opaque. */
internal class CupTool {
    var mode = ToolMode.Rest
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var a = 0f
    var va = 0f
    var s = 1f
    var al = 1f
    internal var flyAt = 0f
    internal var fromX = 0f
    internal var fromY = 0f
    internal var fromA = 0f
}

/**
 * The pencil cup's springs, timers and gesture, ported from the prototype's `st`, `step()`,
 * `open()`, `close()`, `pick()`, `hideSheet()` and its cup pointer handlers. Units are dp, which
 * play the prototype's CSS pixels; [layout] gives the space the cup's screen fills.
 *
 * [now] is the cup's own clock in ms, which [step] advances; the prototype's staggered `later()`
 * timers run on it, so a test steps them as the screen does.
 */
@Stable
internal class PencilCupState {
    var reducedMotion = false

    /** Hears what the cup does: the event, its strength, and which tool. */
    var onEvent: ((CupEvent, Float, Int) -> Unit)? = null

    /** Bumped on every [step], so a drawing that reads it redraws each frame. */
    var frame by mutableIntStateOf(0)
        private set

    /** Bumped whenever something sets the cup moving, so a resting frame loop wakes. */
    var wake by mutableIntStateOf(0)
        private set

    /** The fan is out (or on its way out). */
    var isOpen by mutableStateOf(false)
        private set

    /** A picked tool's sheet is up. */
    var sheetOpen by mutableStateOf(false)
        private set

    /** The tool whose sheet shows, or showed last; it stays while the sheet slides away. */
    var sheetTool by mutableIntStateOf(CupTools.Note)
        private set

    /** The tool a drag has lit, or -1. */
    var hover by mutableIntStateOf(-1)
        private set

    private val labels = mutableStateListOf(false, false, false, false)

    /** Whether tool [i]'s label shows: a little after its tool has left the cup. */
    fun labelShown(i: Int) = labels[i]

    val tools = List(CupTools.count) { CupTool() }

    var now = 0f
        private set

    /** How many times a tool has landed hard enough to clink. */
    var clatters = 0
        private set

    var press = 0f
        private set

    var rock = 0f
        private set

    private var pressV = 0f
    private var rockV = 0f
    private var picked = -1
    private var pressing = false

    // The cup's drag: where it began, and whether it has moved past the slop.
    private var dragging = false
    private var dragMoved = false
    private var dragX0 = 0f
    private var dragY0 = 0f

    private class Timer(val at: Float, val fn: () -> Unit)

    private val timers = mutableListOf<Timer>()

    // ---- Layout ----

    var width = 0f
        private set

    var height = 0f
        private set

    /** The prototype's scale: the screen's size against a 360 × 740 phone, a touch larger. */
    var k = 1f
        private set

    var cupX = 0f
        private set

    var rimY = 0f
        private set

    var botY = 0f
        private set

    var topWidth = 0f
        private set

    var footWidth = 0f
        private set

    var rimDepth = 0f
        private set

    private val labelW = FloatArray(CupTools.count)
    private val labelH = FloatArray(CupTools.count)

    /** The cup fills [w] × [h] dp, [safe] dp of it at the foot kept clear. */
    fun layout(w: Float, h: Float, safe: Float = 0f) {
        if (w <= 0f || h <= 0f) return
        if (w == width && h == height) return
        width = w
        height = h
        k = (min(w / 360f, h / 740f)).coerceIn(.85f, 1.25f) * 1.18f
        topWidth = 66 * k
        footWidth = 56 * k
        rimDepth = 8 * k
        cupX = w - 16 * k - topWidth / 2
        botY = h - 26 * k - safe
        rimY = botY - 72 * k
        for (i in tools.indices) if (tools[i].mode == ToolMode.Rest) restInto(i, tools[i])
        wake++
    }

    /** Label [i] measures [w] × [h] dp, which a release on it is judged by. */
    fun setLabelSize(i: Int, w: Float, h: Float) {
        labelW[i] = w
        labelH[i] = h
    }

    private fun pivotY() = rimY + 8 * k

    fun restX(i: Int) = cupX + CupTools.specs[i].restX * k

    fun restY() = botY - 6 * k

    fun restA(i: Int) = CupTools.specs[i].restAngle

    private fun fanR(i: Int, extra: Float = 0f): Float {
        val t = CupTools.specs[i]
        return (t.reach - t.length + extra) * k
    }

    fun fanX(i: Int, extra: Float = 0f) = cupX + sin(CupTools.specs[i].fanAngle) * fanR(i, extra)

    fun fanY(i: Int, extra: Float = 0f) =
        pivotY() - cos(CupTools.specs[i].fanAngle) * fanR(i, extra)

    /** Where tool [i]'s tip lands when fanned out, which its label hangs off. */
    fun tipX(i: Int) = cupX + sin(CupTools.specs[i].fanAngle) * CupTools.specs[i].reach * k

    fun tipY(i: Int) = pivotY() - cos(CupTools.specs[i].fanAngle) * CupTools.specs[i].reach * k

    /** The right edge of label [i], and its middle. */
    fun labelRight(i: Int) = tipX(i) - CupTools.specs[i].gap * k

    fun labelY(i: Int) = tipY(i)

    /** A point [along] the way from tool [i]'s fanned foot to its tip. */
    fun alongTool(i: Int, along: Float) =
        Offset(lerp(fanX(i), tipX(i), along), lerp(fanY(i), tipY(i), along))

    /** Where the script's fingertip presses the cup. */
    val cupPoint: Offset
        get() = Offset(cupX, rimY + 30 * k)

    /** The cup's touch target: left, top, width, height. */
    val cupLeft
        get() = cupX - 38 * k

    val cupTop
        get() = rimY - 40 * k

    val cupWidth
        get() = 76 * k

    val cupHeight
        get() = botY - rimY + 52 * k

    private fun restInto(i: Int, t: CupTool) {
        t.x = restX(i)
        t.y = restY()
        t.a = restA(i)
    }

    // ---- Actions ----

    fun open() {
        if (isOpen || sheetOpen) return
        isOpen = true
        hover = -1
        wake++
        for (i in tools.indices) {
            val t = tools[i]
            val go = {
                if (isOpen && t.mode != ToolMode.Away) {
                    t.mode = ToolMode.Fan
                    t.vy -= 260 * k
                    emit(CupEvent.Lift, 1f, i)
                    // The label shows once its tool is most of the way out, so it never floats
                    // on its own.
                    val show = { if (isOpen && t.mode == ToolMode.Fan) labels[i] = true }
                    if (reducedMotion) show() else later(250f, show)
                }
            }
            if (reducedMotion) {
                go()
                t.x = fanX(i)
                t.y = fanY(i)
                t.a = CupTools.specs[i].fanAngle
            } else {
                later(CupTools.menuOrder.indexOf(i) * 45f, go)
            }
        }
    }

    /** Drops every tool but [except] back into the cup, the outermost first. */
    fun close(except: Int = -1) {
        if (!isOpen) return
        isOpen = false
        hover = -1
        wake++
        for (i in labels.indices) labels[i] = false
        for (i in tools.indices) {
            if (i == except) continue
            val t = tools[i]
            val go = {
                if (!isOpen && t.mode != ToolMode.Away && t.mode != ToolMode.Fly) {
                    t.mode = ToolMode.Home
                    if (reducedMotion) {
                        restInto(i, t)
                        t.mode = ToolMode.Rest
                    }
                }
            }
            if (reducedMotion) go() else later(i * 40f, go)
        }
    }

    fun toggle() = if (isOpen) close() else open()

    /** Picks tool [i]: it flies off into its sheet while the rest go home. */
    fun pick(i: Int) {
        if (sheetOpen) return
        if (!isOpen) open()
        val t = tools[i]
        close(i)
        picked = i
        sheetTool = i
        t.fromX = t.x
        t.fromY = t.y
        t.fromA = t.a
        t.mode = ToolMode.Fly
        t.flyAt = now
        emit(CupEvent.Pick, 1f, i)
        sheetOpen = true
        wake++
    }

    /**
     * The sheet's Done: it goes, and the tool that made it falls back into the cup from above the
     * screen. Returns that tool, for the new item, or -1 if no sheet was up.
     */
    fun done(): Int {
        if (!sheetOpen) return -1
        val i = picked
        sheetOpen = false
        wake++
        if (i >= 0) {
            val t = tools[i]
            later(if (reducedMotion) 0f else 260f) {
                if (reducedMotion) {
                    restInto(i, t)
                    t.mode = ToolMode.Rest
                    t.s = 1f
                    t.al = 1f
                } else {
                    t.mode = ToolMode.Home
                    t.x = restX(i) - 14 * k
                    t.y = -10 * k
                    t.vx = 0f
                    t.vy = 0f
                    t.a = restA(i) + .35f
                    t.va = 0f
                    t.s = 1f
                    t.al = 1f
                }
            }
            picked = -1
        }
        return i
    }

    /** Puts everything back as it began: closed, no sheet, every tool at rest in the cup. */
    fun reset() {
        timers.clear()
        isOpen = false
        sheetOpen = false
        hover = -1
        picked = -1
        rock = 0f
        rockV = 0f
        press = 0f
        pressV = 0f
        pressing = false
        dragging = false
        for (i in labels.indices) labels[i] = false
        for (i in tools.indices) {
            val t = tools[i]
            t.mode = ToolMode.Rest
            t.vx = 0f
            t.vy = 0f
            t.va = 0f
            t.s = 1f
            t.al = 1f
            restInto(i, t)
        }
        wake++
        frame++
    }

    // ---- Gesture ----

    /** A finger comes down on the cup at ([x], [y]). */
    fun down(x: Float, y: Float) {
        dragging = true
        dragMoved = false
        dragX0 = x
        dragY0 = y
        pressing = true
        wake++
    }

    /** The finger on the cup moves: past the slop it opens the fan and lights the tool under it. */
    fun move(x: Float, y: Float) {
        if (!dragging) return
        if (!dragMoved && hypot(x - dragX0, y - dragY0) > Slop) {
            dragMoved = true
            if (!isOpen) open()
        }
        if (dragMoved) light(itemAt(x, y, 1.5f))
    }

    /**
     * The finger lifts at ([x], [y]). Decided from the release point alone, so even a swipe with no
     * move events in between picks the right thing.
     */
    fun up(x: Float, y: Float) {
        if (!dragging) return
        dragging = false
        pressing = false
        wake++
        val far = hypot(x - dragX0, y - dragY0) > Slop
        if (!far) {
            light(-1)
            toggle()
            return
        }
        val i = itemAt(x, y, 1.5f)
        light(-1)
        when {
            i >= 0 -> pick(i)
            overCup(x, y) -> close()
            !isOpen -> open()
        }
    }

    /** The finger was taken away without lifting, as when the system takes the gesture. */
    fun cancel() {
        dragging = false
        pressing = false
        light(-1)
        wake++
    }

    /** A tap on the dimmed page: a tool or label under it is picked, otherwise the fan closes. */
    fun tapScrim(x: Float, y: Float) {
        val i = itemAt(x, y, .8f)
        if (i >= 0) pick(i) else close()
    }

    private fun light(i: Int) {
        if (hover == i) return
        hover = i
        if (i >= 0) emit(CupEvent.Lift, 1f, i)
    }

    private fun overLabel(i: Int, x: Float, y: Float, pad: Float = 6f): Boolean {
        val w = labelW[i]
        val h = labelH[i]
        if (w <= 0f) return false
        val l = labelRight(i) - w
        val t = labelY(i) - h / 2
        return x >= l - pad && x <= l + w + pad && y >= t - pad && y <= t + h + pad
    }

    /**
     * Which tool a point means, judged from the fanned targets (not where the springs happen to
     * be), so it's the same however fast the finger moves.
     */
    fun itemAt(x: Float, y: Float, tol: Float = 1f): Int {
        var best = -1
        var bd = 1e9f
        for (i in tools.indices) {
            if (tools[i].mode == ToolMode.Away) continue
            if (overLabel(i, x, y)) {
                if (-1f < bd) {
                    best = i
                    bd = -1f
                }
                continue
            }
            val d = segDist(x, y, fanX(i), fanY(i), tipX(i), tipY(i))
            if (d < (CupTools.specs[i].width / 2 + 20 * tol) * k && d < bd) {
                best = i
                bd = d
            }
        }
        return best
    }

    private fun overCup(x: Float, y: Float) =
        x > cupX - topWidth * .7f &&
            x < cupX + topWidth * .7f &&
            y > rimY - 24 * k &&
            y < botY + 10 * k

    // ---- Physics ----

    /** At rest: nothing moving, nothing pending, so the frame loop can sleep. */
    val settled: Boolean
        get() {
            if (timers.isNotEmpty() || pressing) return false
            if (abs(press) > 1e-3f || abs(pressV) > 1e-3f) return false
            if (abs(rock) > 1e-3f || abs(rockV) > 1e-3f) return false
            for (i in tools.indices) {
                val t = tools[i]
                when (t.mode) {
                    ToolMode.Away -> Unit
                    ToolMode.Rest ->
                        if (
                            abs(t.x - restX(i)) > .02f ||
                                abs(t.y - restY()) > .02f ||
                                abs(t.a - restA(i)) > 1e-3f ||
                                abs(t.vx) > .05f ||
                                abs(t.vy) > .05f ||
                                abs(t.va) > 1e-3f
                        )
                            return false
                    else -> return false
                }
            }
            return true
        }

    /** Advances the cup [dt] seconds: due timers first, then the springs in 1/240 s substeps. */
    fun step(dt: Float) {
        now += dt * 1000f
        var i = timers.size - 1
        while (i >= 0) {
            if (i < timers.size && timers[i].at <= now) timers.removeAt(i).fn()
            i--
        }
        var left = dt
        while (left > 0f) {
            val h = min(left, 1f / 240f)
            physics(h)
            left -= h
        }
        frame++
    }

    private fun later(ms: Float, fn: () -> Unit) {
        timers += Timer(now + ms, fn)
        wake++
    }

    // The spring's new velocity, which [spring] leaves here so it needn't return a pair.
    private var sv = 0f

    private fun spring(x: Float, v: Float, target: Float, kk: Float, z: Float, dt: Float): Float {
        val c = 2 * sqrt(kk) * z
        sv = v + (kk * (target - x) - c * v) * dt
        return x + sv * dt
    }

    private fun physics(dt: Float) {
        press = spring(press, pressV, if (pressing) 1f else 0f, 520f, .6f, dt)
        pressV = sv
        rock = spring(rock, rockV, 0f, 300f, .16f, dt)
        rockV = sv
        for (i in tools.indices) {
            val t = tools[i]
            val rx = restX(i)
            val ry = restY()
            val ra = restA(i)
            when (t.mode) {
                ToolMode.Rest -> {
                    t.x = spring(t.x, t.vx, rx, 300f, .8f, dt)
                    t.vx = sv
                    t.y = spring(t.y, t.vy, ry, 300f, .8f, dt)
                    t.vy = sv
                    t.a = spring(t.a, t.va, ra + rock * .5f, 140f, .3f, dt)
                    t.va = sv
                }
                ToolMode.Fan -> {
                    val extra = if (hover == i) 10f else 0f
                    t.x = spring(t.x, t.vx, fanX(i, extra), 170f, .62f, dt)
                    t.vx = sv
                    t.y = spring(t.y, t.vy, fanY(i, extra), 170f, .62f, dt)
                    t.vy = sv
                    // The angle lags a touch, so each tool rises out of the cup before it leans.
                    t.a = spring(t.a, t.va, CupTools.specs[i].fanAngle, 110f, .58f, dt)
                    t.va = sv
                }
                ToolMode.Home -> {
                    t.x = spring(t.x, t.vx, rx, 240f, .9f, dt)
                    t.vx = sv
                    t.a = spring(t.a, t.va, ra, 150f, .32f, dt)
                    t.va = sv
                    if (t.y < ry - .5f || t.vy < 0f) {
                        t.vy += Gravity * k * dt
                        t.y += t.vy * dt
                        if (t.y >= ry) {
                            val hit = t.vy
                            t.y = ry
                            if (hit > 90 * k) {
                                val side = if (i % 2 == 1) 1f else -1f
                                t.vy = -hit * .26f
                                t.va += side * (hit / (900 * k)).coerceIn(.2f, 1.4f) * 4f
                                rockV += -side * (hit / (900 * k)).coerceIn(.1f, 1f) * .9f
                                clatters++
                                emit(CupEvent.Clink, (hit / (1100 * k)).coerceIn(.15f, 1f), i)
                            } else {
                                t.vy = 0f
                            }
                        }
                    } else {
                        t.y = spring(t.y, t.vy, ry, 300f, .8f, dt)
                        t.vy = sv
                        if (abs(t.va) < .05f && abs(t.a - ra) < .02f && abs(t.vy) < 5f) {
                            t.mode = ToolMode.Rest
                        }
                    }
                }
                ToolMode.Fly -> {
                    val p = ((now - t.flyAt) / 420f).coerceIn(0f, 1f)
                    val e = ease(p)
                    val tx = width / 2 + 40 * k
                    val ty = height * .36f + 40 * k
                    t.x = lerp(t.fromX, tx, e)
                    t.y = lerp(t.fromY, ty, e) - sin(p * PI.toFloat()) * 30 * k
                    t.a = lerp(t.fromA, -1.35f, e)
                    t.s = lerp(1f, .55f, e)
                    t.al = 1f - ((p - .55f) / .45f).coerceIn(0f, 1f)
                    if (p >= 1f) t.mode = ToolMode.Away
                }
                ToolMode.Away -> Unit
            }
            // The cup's right wall stops a tool that leans out, so it can't swing past the edge.
            val lean = CupTools.specs[i].lean
            if (
                !lean.isNaN() && (t.mode == ToolMode.Rest || t.mode == ToolMode.Home) && t.a > lean
            ) {
                t.a = lean
                if (t.va > 0f) t.va *= -.3f
            }
        }
    }

    private fun emit(event: CupEvent, strength: Float, tool: Int) {
        onEvent?.invoke(event, strength, tool)
    }

    internal companion object {
        /** How far a finger may move and still tap, in dp. */
        const val Slop = 10f

        const val Gravity = 2600f

        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

        fun ease(t: Float) = if (t < .5f) 2 * t * t else 1 - (-2 * t + 2).pow(2) / 2

        fun segDist(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
            val vx = bx - ax
            val vy = by - ay
            val len = vx * vx + vy * vy
            val u =
                (((px - ax) * vx + (py - ay) * vy) / (if (len == 0f) 1f else len)).coerceIn(0f, 1f)
            return hypot(px - ax - vx * u, py - ay - vy * u)
        }
    }
}
