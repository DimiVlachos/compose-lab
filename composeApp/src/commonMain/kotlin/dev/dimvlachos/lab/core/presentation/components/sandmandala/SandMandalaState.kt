package dev.dimvlachos.lab.core.presentation.components.sandmandala

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** One visit in the history: which site, and the two lines of its row. */
internal class SandVisit(val site: Int, val title: String, val sub: String)

/** A heading over the rows that follow it, or a visit. */
internal sealed interface SandEntry {
    class Section(val label: String) : SandEntry

    class Visit(val visit: SandVisit) : SandEntry
}

/** Where the history rests: as rows, as the finished mandala, as one heap, or swept away. */
internal enum class SandAt {
    Rows,
    Mandala,
    Heap,
    Cleared,
}

/** What moves: the grains gathering into the mandala, sweeping into the heap, pouring away. */
internal enum class SandPhase {
    Gather,
    Sweep,
    Pour,
}

/** What the history says or plays as it goes. */
internal enum class SandEvent {
    /** A brush across the rows started the clear. */
    Brushed,

    /** The mandala is complete. */
    Formed,

    /** Keep history: the grains are back in their rows. */
    Kept,

    /** Undo: the grains are back in their rows. */
    Restored,

    /** The sweep let the mandala go. */
    Swept,

    /** The heap has poured away. */
    Cleared,
}

/**
 * The prototype's sand mandala that clears a browser's history, as a pure state. Clear it and every
 * row crumbles into coloured sand, its site's colour and its own words, and the grains flow into a
 * mandala; Keep history sends them home. A sweep across the mandala spirals every grain into one
 * grey heap, which pours away; Undo plays it all backwards.
 *
 * Every length is in the list's dp, its top left at 0, 0, as the prototype's CSS pixels. The rows
 * are laid out here, so the painter and the grains agree on where they are; their text pixels come
 * from [build], drawn by the painter.
 */
@Stable
internal class SandMandalaState(
    /** The history, sections and visits in order. */
    val entries: List<SandEntry>,
    /** Each site's colour, `0xRRGGBB`. */
    val sitePigments: IntArray,
) {
    val visits: List<SandVisit> = entries.filterIsInstance<SandEntry.Visit>().map { it.visit }

    /** Bumped when what the screen shows around the sand changes: buttons, hint, snackbar. */
    var version by mutableIntStateOf(0)
        private set

    /** Bumped every step that moves something, and on every brush move: the canvas redraws. */
    var frame by mutableIntStateOf(0)
        private set

    var reducedMotion = false

    /** Called with what happened; the screen plays its sound and says its words. */
    var onEvent: ((SandEvent) -> Unit)? = null

    /** True while the script drives the brush; a real touch takes over. */
    var scripted = false

    // ---------- Layout ----------
    var width = 0f
        private set

    var height = 0f
        private set

    var dark = false
        private set

    /** The prototype's short screen (under 640 dp): rows 46 dp tall, no search field. */
    var short = false
        private set

    var cx = 0f
        private set

    var cy = 0f
        private set

    var r = 0f
        private set

    private var rh = 0f

    /** Each entry's top and height; for a section only [entryTop] matters. */
    val entryTop = FloatArray(entries.size)
    val entryH = FloatArray(entries.size)

    /** Each visit's row index into [entries]. */
    private val visitEntry = entries.indices.filter { entries[it] is SandEntry.Visit }.toIntArray()

    val rowH: Float
        get() = if (short) 46f else 52f

    /** Each visit's row top. */
    fun rowTop(visit: Int) = entryTop[visitEntry[visit]]

    /** The favicon's box in its row: 30 dp square, 20 dp in. */
    fun favX() = 20f

    fun favY(visit: Int) = rowTop(visit) + (rowH - 30f) / 2f

    private var key = ""

    /** True when [layout] changed something and [build] must be given the rows' text again. */
    var needsText = false
        private set

    /**
     * Lays the list out at [w] × [h] dp, [dark] or light, and places the mandala as the prototype
     * does. Returns true when the rows' text must be drawn again and handed to [build].
     */
    fun layout(w: Float, h: Float, dark: Boolean, short: Boolean, fontScale: Float = 1f): Boolean {
        if (w <= 0f || h <= 0f) return false
        val k = "$w,$h,$dark,$short,$fontScale"
        if (k == key) return false
        key = k
        width = w
        height = h
        this.dark = dark
        this.short = short
        var y = 0f
        for (i in entries.indices) {
            entryTop[i] = y
            entryH[i] = if (entries[i] is SandEntry.Section) SectionH else rowH
            y += entryH[i]
        }
        cx = w / 2f
        r = max(60f, min(w / 2f - 40f, h * .4f))
        cy = min(h * .46f, h - r - 56f)
        cy = max(cy, r + 8f)
        rh = r * .3f
        needsText = true
        return true
    }

    /** Where the hint sits under the mandala. */
    val hintTop: Float
        get() = cy + r + 12f

    // ---------- Grains ----------
    var n = 0
        private set

    internal var hx = FloatArray(0)
    internal var hy = FloatArray(0)
    internal var tx = FloatArray(0)
    internal var ty = FloatArray(0)
    internal var dx = FloatArray(0)
    internal var dy = FloatArray(0)
    internal var ccx = FloatArray(0)
    internal var ccy = FloatArray(0)
    internal var rel = FloatArray(0)
    internal var gd = FloatArray(0)
    internal var sd = FloatArray(0)
    internal var sdur = FloatArray(0)
    internal var pd = FloatArray(0)
    internal var q = FloatArray(0)
    internal var rnd = FloatArray(0)
    internal var r0 = FloatArray(0)
    internal var th0 = FloatArray(0)
    internal var hpx = FloatArray(0)
    internal var hpy = FloatArray(0)
    internal var hsh = FloatArray(0)

    /** Home, target and grey colours, three channels each, 0 to 255. */
    internal var cH = IntArray(0)
    internal var cT = IntArray(0)
    internal var cG = IntArray(0)

    internal var gt = 1f
    internal var st = 1f
    internal var pt = 1f

    /** Per visit: when its crumbling front starts, and the row's left and width. */
    internal val frontT0 = FloatArray(visits.size)
    private val frontX = FloatArray(visits.size)
    private val frontW = FloatArray(visits.size)

    /**
     * Makes the grains, as the prototype's build(): [text] is the rows' words drawn at 1 px per dp
     * over the list, [textW] × [textH], ARGB. Its opaque pixels become the cream grains; the rest
     * come out of the favicons.
     */
    fun build(text: IntArray, textW: Int, textH: Int) {
        needsText = false
        val rr = parkMiller(1234)
        // Each site's favicons in visible rows.
        val favs = Array(sitePigments.size) { ArrayList<FloatArray>() }
        val textRow = IntArray(textH) { -1 }
        for (i in visits.indices) {
            val ry = rowTop(i)
            val visible = ry + rowH * .45f < height
            frontT0[i] = i * RowStep
            frontX[i] = 0f
            frontW[i] = width
            if (!visible) continue
            favs[visits[i].site].add(floatArrayOf(favX(), favY(i), 30f, 30f, i.toFloat()))
        }
        // A text pixel's row: the first row whose box holds it.
        for (py in 0 until textH) {
            for (k in visits.indices) {
                val ry = rowTop(k)
                if (py >= ry && py < ry + rowH) {
                    textRow[py] = k
                    break
                }
            }
            if (textRow[py] < 0) textRow[py] = 0
        }
        // Sites whose rows are off the bottom borrow a visible row's favicons.
        val anyFav = favs.firstOrNull { it.isNotEmpty() }
        for (k in favs.indices) if (favs[k].isEmpty() && anyFav != null) favs[k] = anyFav
        var count = 0
        for (p in text) if ((p ushr 24) > 110) count++
        val textPx = IntArray(count)
        count = 0
        for (i in text.indices) if ((text[i] ushr 24) > 110) textPx[count++] = i

        // Mandala targets on a jittered hex grid, so the sand is dense but still shows grain.
        val sp = 1.9
        val rd = r.toDouble()
        val pts = DoubleList()
        var yy = -rd
        var row = 0
        while (yy <= rd) {
            var xx = -rd + (row and 1) * sp / 2
            while (xx <= rd) {
                val jx = xx + (rr.next() - .5) * sp * .7
                val jy = yy + (rr.next() - .5) * sp * .7
                if (hypot(jx, jy) <= rd) pts.add(jx, jy)
                xx += sp
            }
            yy += sp * .866
            row++
        }
        val count2 = pts.size / 2
        n = count2
        val p = pts.data
        // Shuffled, so where grains overlap (the heap) no drawing order shows through as a seam.
        for (i in count2 - 1 downTo 1) {
            val j = (rr.next() * (i + 1)).toInt()
            val ax = p[i * 2]
            val ay = p[i * 2 + 1]
            p[i * 2] = p[j * 2]
            p[i * 2 + 1] = p[j * 2 + 1]
            p[j * 2] = ax
            p[j * 2 + 1] = ay
        }
        hx = FloatArray(n)
        hy = FloatArray(n)
        tx = FloatArray(n)
        ty = FloatArray(n)
        dx = FloatArray(n)
        dy = FloatArray(n)
        ccx = FloatArray(n)
        ccy = FloatArray(n)
        rel = FloatArray(n)
        gd = FloatArray(n)
        sd = FloatArray(n)
        sdur = FloatArray(n)
        pd = FloatArray(n)
        q = FloatArray(n)
        rnd = FloatArray(n)
        r0 = FloatArray(n)
        th0 = FloatArray(n)
        hpx = FloatArray(n)
        hpy = FloatArray(n)
        hsh = FloatArray(n)
        cH = IntArray(n * 3)
        cT = IntArray(n * 3)
        cG = IntArray(n * 3)
        val cream = if (dark) intArrayOf(238, 228, 206) else intArrayOf(246, 238, 220)
        val grey = if (dark) intArrayOf(146, 138, 124) else intArrayOf(176, 166, 148)
        val pig =
            Array(sitePigments.size) { k ->
                val c = sitePigments[k]
                intArrayOf((c shr 16) and 255, (c shr 8) and 255, c and 255)
            }
        val home = DoubleArray(2)
        val cxd = cx.toDouble()
        val cyd = cy.toDouble()
        var gtMax = 0.0
        for (i in 0 until n) {
            val x = p[i * 2]
            val y = p[i * 2 + 1]
            val cat = design(x / rd, y / rd)
            val rn = if (rd != 0.0) hypot(x, y) / rd else 0.0
            tx[i] = (cxd + x).toFloat()
            ty[i] = (cyd + y).toFloat()
            rnd[i] = rr.next().toFloat()
            q[i] = rr.next().toFloat()
            val hc: IntArray
            val gRow: Int
            if (cat == Line && textPx.isNotEmpty()) {
                val px0 = textPx[(rr.next() * textPx.size).toInt()]
                val px = px0 % textW
                val py = px0 / textW
                home[0] = px + rr.next() - .5
                home[1] = py + rr.next() - .5
                val c = text[px0]
                hc = intArrayOf((c shr 16) and 255, (c shr 8) and 255, c and 255)
                gRow = textRow[py]
            } else {
                val opts = favs[if (cat == Line) 0 else cat]
                val f =
                    if (opts.isEmpty()) floatArrayOf(20f, 20f, 30f, 30f, 0f)
                    else opts[(rr.next() * opts.size).toInt()]
                home[0] = f[0] + 1 + rr.next() * (f[2] - 2)
                home[1] = f[1] + 1 + rr.next() * (f[3] - 2)
                hc = pig[if (cat == Line) 0 else cat]
                gRow = f[4].toInt()
            }
            hx[i] = home[0].toFloat()
            hy[i] = home[1].toFloat()
            val tcol = if (cat == Line) cream else pig[cat]
            // Each grain is its own tone: a little lighter or darker, a little warmer.
            val j = .84 + rr.next() * .3
            val warm = (rr.next() - .5) * 10
            for (c in 0 until 3) {
                cH[i * 3 + c] = byte(hc[c] * (.94 + (j - .99) * .5))
                cT[i * 3 + c] =
                    byte(tcol[c] * j + (if (c == 0) warm else if (c == 2) -warm else 0.0))
                cG[i * 3 + c] =
                    byte(lerp(tcol[c].toDouble(), grey[c].toDouble(), .8) * (.9 + (j - .84) * .6))
            }
            // Released when the row's crumbling front passes; inner grains settle first, like a
            // mandala built from the centre.
            rel[i] =
                (frontT0[gRow] +
                    Front * ((hx[i] - frontX[gRow]) / max(1f, frontW[gRow])).coerceIn(0f, 1f))
            gd[i] = (.9 + .8 * rn + .35 * rnd[i]).toFloat()
            gtMax = max(gtMax, (rel[i] + gd[i]).toDouble())
            // The flight bends round the centre, so the sand swirls in.
            val mx = (hx[i] + tx[i]) / 2.0 - cxd
            val my = (hy[i] + ty[i]) / 2.0 - cyd
            val sw = .7 + rr.next() * .5
            val cs = cos(sw)
            val sn = sin(sw)
            ccx[i] = (cxd + (mx * cs - my * sn) * 1.08).toFloat()
            ccy[i] = (cyd + (mx * sn + my * cs) * 1.08).toFloat()
            sd[i] = (.5 * (1 - min(1.0, rn)) + .06 * rr.next()).toFloat()
            sdur[i] = (1.15 + .35 * rn + .12 * rr.next()).toFloat()
        }
        gt = gtMax.toFloat()
        var stMax = 0f
        for (i in 0 until n) stMax = max(stMax, sd[i] + sdur[i])
        st = stMax
        // A layout change part-way lands on the nearest resting state.
        val moving = ph
        if (moving != null) {
            val dest =
                if (dir > 0)
                    when (moving) {
                        SandPhase.Gather -> SandAt.Mandala
                        SandPhase.Sweep -> SandAt.Heap
                        SandPhase.Pour -> SandAt.Cleared
                    }
                else
                    when (moving) {
                        SandPhase.Gather -> SandAt.Rows
                        SandPhase.Sweep -> SandAt.Mandala
                        SandPhase.Pour -> SandAt.Heap
                    }
            ph = null
            at = dest
            if (dest == SandAt.Heap) {
                prepS()
                at = SandAt.Cleared
                showCleared()
            }
        }
        if (at == SandAt.Heap || at == SandAt.Cleared) prepS()
        sync()
        frame++
    }

    // ---------- State ----------
    /** Where the sand rests, or null while it moves, as the prototype's S.at. */
    var at: SandAt? = SandAt.Rows
        private set

    var ph: SandPhase? = null
        private set

    var t = 0f
        private set

    var dir = 1
        private set

    internal var spin = 1
    private var undoing = false
    private var hold = 0f
    private var next: (() -> Unit)? = null
    private var snackT = 0f

    var snackOn = false
        private set

    var restoreOn = false
        private set

    /** How much of the sand is moving, 0 to 1, as the last frame drew it: for the hiss. */
    var moving = 0f

    /** The sand's hiss, 0 to 0.16 as the prototype's gain. */
    var hiss = 0f
        private set

    private var brushLevel = 0f

    fun len(p: SandPhase) =
        when (p) {
            SandPhase.Gather -> gt
            SandPhase.Sweep -> st
            SandPhase.Pour -> pt
        }

    internal fun prepS() {
        for (i in 0 until n) {
            val x = tx[i] + dx[i] - cx
            val y = ty[i] + dy[i] - cy
            val rn = min(1.2f, hypot(x, y) / r)
            r0[i] = hypot(x, y)
            th0[i] = atan2(y, x)
            val th = th0[i] + spin * (1.7f + 1.1f * rn)
            val rq = heapR(i)
            hpx[i] = cx + rq * cos(th)
            hpy[i] = cy + rq * sin(th)
            // A cone of sand, lit from the top left.
            val sq = min(1.2f, rq / rh)
            hsh[i] = 1.1f - .17f * sq * sq + .15f * sq * cos(th + PI.toFloat() * .75f)
        }
        // The heap drains over its lower lip in a thin stream, nearest grains first, so it erodes
        // from the bottom up.
        var ptMax = 0f
        val lip = lipY
        val fall = sqrt(2 * max(10f, height + 8 - lip) / Pa)
        for (i in 0 until n) {
            val dd = hypot(hpx[i] - cx, hpy[i] - lip) / (rh * 2.2f)
            pd[i] = 1.25f * dd.coerceIn(0f, 1.2f) + .12f * rnd[i]
            ptMax = max(ptMax, pd[i] + Lip + fall)
        }
        pt = ptMax
    }

    internal val lipY: Float
        get() = cy + rh * .92f

    // Most grains pack into a cone; a few roll out and lie scattered round its foot.
    internal fun heapR(i: Int): Float {
        val v = q[i]
        return if (v < .88f) rh * (v / .88f).pow(.62f) else rh * (1 + (v - .88f) * 2.6f)
    }

    private fun go(p: SandPhase, d: Int) {
        ph = p
        dir = d
        at = null
        t = if (d > 0) 0f else len(p)
        hold = 0f
        next = null
        sync()
        frame++
    }

    private fun flip(d: Int) {
        if (ph != null) {
            dir = d
            sync()
        }
    }

    /** Clear history: the rows crumble into the mandala. During the gather, carries on. */
    fun clearHistory() {
        if (ph == SandPhase.Gather) return flip(1)
        if (at != SandAt.Rows || n == 0) return
        dx.fill(0f)
        dy.fill(0f)
        if (reducedMotion) {
            at = SandAt.Mandala
            arrived(SandPhase.Gather, 1)
            return
        }
        go(SandPhase.Gather, 1)
    }

    /** Keep history: the mandala goes back into its rows, as do grains still on their way. */
    fun keep() {
        if (ph == SandPhase.Gather) return flip(-1)
        if (at != SandAt.Mandala) return
        if (reducedMotion) {
            at = SandAt.Rows
            arrived(SandPhase.Gather, -1)
            return
        }
        go(SandPhase.Gather, -1)
    }

    /** What the bar's button does now: clear, or keep while the mandala forms or is formed. */
    fun tapClear() {
        if (keeps) keep() else clearHistory()
    }

    /** The bar's button says Keep history. */
    val keeps: Boolean
        get() = at == SandAt.Mandala || (ph == SandPhase.Gather && dir > 0)

    /** The bar's button can be pressed. */
    val clearEnabled: Boolean
        get() = at == SandAt.Rows || at == SandAt.Mandala || ph == SandPhase.Gather

    /** The mandala, with its hint and Clear now, waiting for a sweep. */
    val inMandala: Boolean
        get() = at == SandAt.Mandala

    val hintOn: Boolean
        get() = inMandala && next == null

    /** The rows are drawn: at rest, or crumbling while the sand gathers. */
    val rowsShown: Boolean
        get() = at == SandAt.Rows || ph == SandPhase.Gather

    /** The sweep has let go: everything spirals the [spin] way into the heap. */
    fun commit(spin: Int = 1) {
        if (!inMandala) return
        brush = null
        this.spin = if (spin == 0) 1 else spin
        undoing = false
        prepS()
        if (reducedMotion) {
            at = SandAt.Cleared
            arrived(SandPhase.Pour, 1)
            return
        }
        onEvent?.invoke(SandEvent.Swept)
        go(SandPhase.Sweep, 1)
    }

    /** Undo: the sand comes back up, into the mandala, and climbs back into the rows. */
    fun undo() {
        if (at != SandAt.Cleared) return
        hideSnack()
        restoreOn = false
        // Back to the mandala as it was made, smudges and all forgiven.
        dx.fill(0f)
        dy.fill(0f)
        prepS()
        undoing = true
        if (reducedMotion) {
            at = SandAt.Rows
            arrived(SandPhase.Gather, -1)
            return
        }
        go(SandPhase.Pour, -1)
    }

    private fun arrived(p: SandPhase, d: Int) {
        ph = null
        frame++
        when {
            p == SandPhase.Gather && d > 0 -> {
                at = SandAt.Mandala
                onEvent?.invoke(SandEvent.Formed)
            }
            p == SandPhase.Gather -> {
                at = SandAt.Rows
                onEvent?.invoke(if (undoing) SandEvent.Restored else SandEvent.Kept)
                undoing = false
            }
            p == SandPhase.Sweep && d > 0 -> {
                at = SandAt.Heap
                hold = .45f
                next = { go(SandPhase.Pour, 1) }
            }
            p == SandPhase.Sweep -> {
                at = SandAt.Mandala
                hold = .75f
                next = { go(SandPhase.Gather, -1) }
            }
            p == SandPhase.Pour && d > 0 -> {
                at = SandAt.Cleared
                showCleared()
            }
            else -> {
                at = SandAt.Heap
                go(SandPhase.Sweep, -1)
            }
        }
        sync()
    }

    private fun showCleared() {
        snackOn = true
        snackT = SnackSeconds
        onEvent?.invoke(SandEvent.Cleared)
    }

    private fun hideSnack() {
        snackOn = false
        snackT = 0f
    }

    private fun sync() {
        version++
    }

    /** At rest: nothing moves and nothing is waiting, so the frame loop can sleep. */
    val settled: Boolean
        get() = ph == null && next == null && snackT <= 0f && brush == null && hiss < .0005f

    /** Moves everything on by [dt] seconds, in steps of at most 1/60 s, as the prototype's loop. */
    fun step(dt: Float) {
        var left = dt
        val before = ph != null || next != null
        while (left > 1e-6f) {
            val h = min(left, 1f / 60f)
            stepOnce(h)
            left -= h
        }
        // The hiss: how much sand moves and how hard the brush goes, eased as the prototype's.
        brushLevel *= exp(-dt * 6f)
        val target = min(.16f, moving * .22f + brushLevel * .12f)
        val k = 1f - (1f - .08f).pow(dt * 60f)
        hiss += (target - hiss) * k
        if (before || ph != null) frame++
    }

    private fun stepOnce(h: Float) {
        val p = ph
        if (p != null) {
            t += h * dir
            if (dir > 0 && t >= len(p)) {
                t = len(p)
                arrived(p, 1)
            } else if (dir < 0 && t <= 0f) {
                t = 0f
                arrived(p, -1)
            }
        } else if (next != null) {
            hold -= h
            if (hold <= 0f) {
                val nx = next
                next = null
                nx?.invoke()
            }
        }
        if (snackT > 0f) {
            snackT -= h
            if (snackT <= 0f) {
                hideSnack()
                if (at == SandAt.Cleared) restoreOn = true
                sync()
            }
        }
    }

    // ---------- Brush ----------
    private class Brush(var x: Float, var y: Float, val scripted: Boolean) {
        var path = 0f
        var cross = 0f
    }

    private var brush: Brush? = null

    val brushing: Boolean
        get() = brush != null

    /**
     * A finger comes down at [x], [y] in the list, or the script's if [scripted]. Moves and lifts
     * count only from whoever holds the brush, so a script taken over can't end a real stroke.
     */
    fun brushDown(x: Float, y: Float, scripted: Boolean = false) {
        brush = Brush(x, y, scripted)
    }

    /**
     * It moves to [x], [y]: across the rows it may start the clear; across the mandala it ploughs.
     */
    fun brushMove(x: Float, y: Float, scripted: Boolean = false) {
        val b = brush ?: return
        if (b.scripted != scripted) return
        val sx = x - b.x
        val sy = y - b.y
        val l = hypot(sx, sy)
        if (l < .5f) return
        if (at == SandAt.Rows) {
            b.path += if (abs(sx) > abs(sy) * .6f) l else l * .3f
            if (b.path > 70f) {
                brush = null
                onEvent?.invoke(SandEvent.Brushed)
                clearHistory()
                return
            }
        } else if (at == SandAt.Mandala && next == null) {
            val rb = max(18f, r * .19f)
            val ux = sx / l
            val uy = sy / l
            val push = min(l, 34f)
            val seed = (x * 131 + y * 7).toDouble().toInt()
            val rg = parkMiller(if (seed == 0) 3 else seed)
            for (i in 0 until n) {
                val px = tx[i] + dx[i]
                val py = ty[i] + dy[i]
                // Distance from the grain to this piece of the stroke.
                val wx = px - b.x
                val wy = py - b.y
                val tt = ((wx * sx + wy * sy) / (l * l)).coerceIn(0f, 1f)
                val qx = b.x + sx * tt - px
                val qy = b.y + sy * tt - py
                val d2 = qx * qx + qy * qy
                if (d2 > rb * rb) continue
                val f = 1 - sqrt(d2) / rb
                val ff = f * f
                // Pushed along the stroke, ploughed a little to either side.
                val side = if ((wx * uy - wy * ux) > 0) 1f else -1f
                dx[i] +=
                    ux * push * ff * (.55f + rg.next().toFloat() * .5f) - uy * side * ff * 3.5f +
                        (rg.next().toFloat() - .5f) * 3 * f
                dy[i] +=
                    uy * push * ff * (.55f + rg.next().toFloat() * .5f) +
                        ux * side * ff * 3.5f +
                        (rg.next().toFloat() - .5f) * 3 * f
            }
            val mx = (b.x + x) / 2 - cx
            val my = (b.y + y) / 2 - cy
            if (hypot(mx, my) < r * 1.15f) {
                b.path += l
                b.cross += mx * sy - my * sx
            }
            brushLevel = min(1f, brushLevel + l / 60f)
            frame++
            if (b.path > r * 1.3f) {
                b.x = x
                b.y = y
                commit(if (b.cross >= 0) 1 else -1)
                return
            }
        }
        b.x = x
        b.y = y
    }

    /** The finger lifts: a sweep of over half a mandala's width lets it go. */
    fun brushUp(scripted: Boolean = false) {
        val b = brush ?: return
        if (b.scripted != scripted) return
        brush = null
        if (inMandala && next == null && b.path > r * .55f) commit(if (b.cross >= 0) 1 else -1)
    }

    /** Drops a stroke without letting it count, as a cancelled touch or a stopped script. */
    fun brushCancel(scripted: Boolean = false) {
        if (brush?.scripted == scripted) brush = null
    }

    /** Back to the rows at once, as the prototype's reset(). */
    fun reset() {
        brush = null
        at = SandAt.Rows
        ph = null
        t = 0f
        dir = 1
        hold = 0f
        next = null
        undoing = false
        dx.fill(0f)
        dy.fill(0f)
        hideSnack()
        restoreOn = false
        sync()
        frame++
    }

    internal companion object {
        /** Sites' pigments are 0..8; 9 is the cream of the rows' own words. */
        const val Line = 9
        const val RowStep = .075f
        const val Front = .45f
        const val Pa = 1700f
        const val Lip = .2f
        const val SnackSeconds = 8f

        /** A section heading: 10 dp above, a 12.5 sp line at 1.55, 2 dp below. */
        const val SectionH = 10f + 12.5f * 1.55f + 2f

        fun lerp(a: Double, b: Double, t: Double) = a + (b - a) * t

        /** As a Uint8ClampedArray stores a number: rounded, held to 0..255. */
        fun byte(v: Double): Int = v.roundToInt().coerceIn(0, 255)

        fun ease(t: Float) = if (t < .5f) 2 * t * t else 1 - (-2 * t + 2).pow(2) / 2

        fun ease3(t: Float) = if (t < .5f) 4 * t * t * t else 1 - (-2 * t + 2).pow(3) / 2

        fun smooth(a: Float, b: Float, x: Float): Float {
            val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
            return t * t * (3 - 2 * t)
        }

        /** The prototype's rng(): Park–Miller, so the same seed lays the same sand. */
        fun parkMiller(seed: Int) = ParkMiller(seed)

        /**
         * The mandala's design: [x], [y] in units of the radius; a site's pigment, or [Line] for
         * the cream lines. An outer fire ring, vajra dots, lotus petals, a square palace with a
         * gate in each wall and four coloured quarters, and a lotus at the centre.
         */
        fun design(x: Double, y: Double): Int {
            val r = hypot(x, y)
            val a = atan2(y, x)
            val tau = PI * 2
            fun frac(v: Double) = ((v % 1) + 1) % 1
            if (r > .955) return 0
            if (r > .935) return Line
            if (r > .855) {
                val s = abs(frac(a / tau * 36) - .5) * 2
                val h = (r - .855) / .08
                return if (s < (1 - h) * .95) 7 else 3
            }
            if (r > .835) return Line
            if (r > .775) {
                val s = frac(a / tau * 52)
                val rr = abs((r - .805) / .03)
                return if (abs(s - .5) < .24 && rr < .55) Line else 6
            }
            if (r > .755) return Line
            if (r > .655) {
                val s = frac(a / tau * 28)
                val h = (r - .655) / .1
                return if (h < 1 - (abs(s - .5) * 2).pow(1.5)) 8 else 4
            }
            if (r > .635) return Line
            val ax = abs(x)
            val ay = abs(y)
            val m = max(ax, ay)
            val minor = min(ax, ay)
            // The palace: a square with a gate in the middle of each wall.
            val gate = minor < .12 && m < .6 && m > .4
            if (gate) {
                if (m > .585 || minor > .105) return Line
                return if (minor < .05) 0 else 3
            }
            if (m > .45) return 5
            if (m > .435) return Line
            if (m > .41) return 0
            if (m > .395) return 3
            if (m > .38) return Line
            if (r < .27) {
                if (r > .255) return Line
                if (r < .075) return if (r < .035) 0 else if (r > .062) Line else 3
                val s = frac(a / tau * 8 + .0625)
                val h = (r - .075) / .18
                val petal = h < 1 - (abs(s - .5) * 2).pow(1.3)
                val edge = petal && h > .92 - (abs(s - .5) * 2).pow(1.3)
                return if (edge) Line else if (petal) 8 else 6
            }
            if (abs(ax - ay) < .012) return Line
            if (ax > ay) return if (x > 0) 1 else 0
            return if (y > 0) 3 else 2
        }
    }
}

/** A growing list of doubles, two at a time, without boxing. */
private class DoubleList {
    var data = DoubleArray(4096)
    var size = 0

    fun add(a: Double, b: Double) {
        if (size + 2 > data.size) data = data.copyOf(data.size * 2)
        data[size++] = a
        data[size++] = b
    }
}

/** The prototype's rng(): Park–Miller, as a class so a draw boxes nothing. */
internal class ParkMiller(seed: Int) {
    private var s = seed.toLong()

    fun next(): Double {
        s = (s * 16807L) % 2147483647L
        return s.toDouble() / 2147483647.0
    }
}
