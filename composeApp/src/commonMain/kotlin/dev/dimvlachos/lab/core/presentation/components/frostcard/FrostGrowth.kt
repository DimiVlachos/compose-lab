package dev.dimvlachos.lab.core.presentation.components.frostcard

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * The prototype's seeded numbers, `rng(seed)`: Park and Miller's minimal standard, so the same seed
 * grows the same ferns as the HTML sketch did.
 */
internal class ParkMiller(seed: Int) {
    private var s = seed.toLong()

    fun next(): Double {
        s = (s * 16807L) % 2147483647L
        return s.toDouble() / 2147483647.0
    }
}

/**
 * The prototype's `growFrost()`, run once per card size ([w] by [h], in dp): dendrites seeded round
 * the card's edges grow inward, branching in alternating ferns that stop where they meet ice
 * another fern already laid; a few loose crystals start in the middle; and thousands of tiny
 * needles of frost fur fill in behind. Every segment remembers when the freezing front ([F], 0 to
 * 1) reaches it, and segments are kept sorted by that, so freezing draws only the new ones.
 *
 * [arr] is a coarse arrival grid of [cell] dp cells ([gw] by [gh]): when the card goes pale there.
 * [dens] is how thick the haze gets in each cell.
 */
internal class FrostGrowth(val w: Float, val h: Float) {
    /**
     * Segment i runs from ([x1], [y1]) to ([x2], [y2]), of generation [gen] (0 a main spine, 3
     * fur).
     */
    val x1: FloatArray
    val y1: FloatArray
    val x2: FloatArray
    val y2: FloatArray
    val gen: IntArray
    val born: FloatArray
    val count: Int

    /** The glints: needle tips that catch the light now and then. */
    val sparkX: FloatArray
    val sparkY: FloatArray
    val sparkBorn: FloatArray
    val sparkPhase: FloatArray
    val sparkSpeed: FloatArray
    val sparkSize: FloatArray
    val sparks: Int

    val cell = 4
    val gw: Int
    val gh: Int
    val arr: FloatArray
    val dens: FloatArray

    init {
        // The JS does its geometry in doubles; so does this, so every break and branch, and so
        // every number drawn after it, falls exactly as the prototype's.
        val w = this.w.toDouble()
        val h = this.h.toDouble()
        val r = ParkMiller(2203)
        val sx1 = FrostDoubles()
        val sy1 = FrostDoubles()
        val sx2 = FrostDoubles()
        val sy2 = FrostDoubles()
        val sg = FrostInts()
        val sb = FrostDoubles()
        val px = FrostDoubles()
        val py = FrostDoubles()
        val pb = FrostDoubles()
        val pph = FrostDoubles()
        val psp = FrostDoubles()
        val pk = FrostDoubles()
        val k = 1.0 / 240.0
        val oc = 2.5
        val ow = ceil(w / oc).toInt() + 1
        val occ = IntArray(ow * (ceil(h / oc).toInt() + 1))
        var nextId = 1
        fun inside(x: Double, y: Double) = x > .5 && y > .5 && x < w - .5 && y < h - .5

        fun branch(
            x0: Double,
            y0: Double,
            ang0: Double,
            len: Double,
            g: Int,
            b0: Double,
            root: Int = 0,
        ) {
            val id = if (root != 0) root else nextId++
            val stepL = if (g == 0) 2.4 else 1.9
            val bend = (r.next() - .5) * (if (g == 0) .022 else .04)
            var x = x0
            var y = y0
            var ang = ang0
            var d = 0.0
            var n = 0
            var side = if (r.next() < .5) 1.0 else -1.0
            var next = (if (g == 0) 3.0 else 2.4) + r.next() * 3.0
            while (d < len) {
                ang += bend + (r.next() - .5) * (if (g == 0) .1 else .16)
                val nx = x + cos(ang) * stepL
                val ny = y + sin(ang) * stepL
                if (!inside(nx, ny)) break
                val oi = (ny / oc).toInt() * ow + (nx / oc).toInt()
                val o = occ[oi]
                if (o != 0 && o != id && n > 1) break
                occ[oi] = id
                sx1.add(x)
                sy1.add(y)
                sx2.add(nx)
                sy2.add(ny)
                sg.add(g)
                sb.add(b0 + d * k)
                x = nx
                y = ny
                d += stepL
                n++
                if (g < 3 && d >= next && len - d > 3.0) {
                    // Barbs lean forward at about 55°, longest near the base of each fern,
                    // alternating sides.
                    val rem = len - d
                    val cap =
                        when (g) {
                            0 -> 16.0 + r.next() * 10.0
                            1 -> 6.0 + r.next() * 3.0
                            else -> 2.5
                        }
                    val cl = min(cap, rem * (.5 + r.next() * .25))
                    val da = .92 + (r.next() - .5) * .16
                    branch(x, y, ang + side * da, cl, g + 1, b0 + d * k, id)
                    if (g > 0 || r.next() < .7) {
                        val cl2 = cl * (.7 + r.next() * .3)
                        branch(x, y, ang - side * da, cl2, g + 1, b0 + d * k, id)
                    }
                    side = -side
                    next = d + if (g == 0) 2.6 + r.next() * 2.4 else 2.2 + r.next() * 2.0
                }
            }
            if (g <= 1 && r.next() < .22) {
                px.add(x)
                py.add(y)
                pb.add(b0 + d * k)
                pph.add(r.next() * 6.28)
                psp.add(.7 + r.next() * 1.6)
                pk.add(.6 + r.next() * .6)
            }
        }

        // Seeds round the perimeter, a little closer at the corners where cold arrives first.
        val per = 2 * (w + h)
        val n = floor(per / 19.0 + .5).toInt()
        for (i in 0 until n) {
            val s = (i + r.next() * .8) / n * per
            val x: Double
            val y: Double
            val nrm: Double
            when {
                s < w -> {
                    x = s
                    y = 1.0
                    nrm = PI / 2
                }
                s < w + h -> {
                    x = w - 1
                    y = s - w
                    nrm = PI
                }
                s < 2 * w + h -> {
                    x = w - (s - w - h)
                    y = h - 1
                    nrm = -PI / 2
                }
                else -> {
                    x = 1.0
                    y = h - (s - 2 * w - h)
                    nrm = 0.0
                }
            }
            val corner = minOf(hypot(x, y), hypot(w - x, y), hypot(x, h - y), hypot(w - x, h - y))
            val delay = .02 + r.next() * .1 + smooth(10.0, 90.0, corner) * .12
            val len = 40.0 + r.next().pow(1.4) * h * .75
            val a = nrm + (r.next() - .5) * 1.15
            r.next() // The fern's width, which only the prototype's data kept.
            branch(x, y, a, len, 0, delay)
        }
        // A few loose crystals start inside, from specks of dust.
        repeat(4) {
            val x = w * (.22 + r.next() * .56)
            val y = h * (.25 + r.next() * .5)
            val b = .3 + r.next() * .22
            val a0 = r.next() * 6.28
            val l = 12.0 + r.next() * 20.0
            val arms = 4 + (r.next() * 3).toInt()
            for (kk in 0 until arms) {
                val a = a0 + kk * PI / 3 + (r.next() - .5) * .35
                val len = l * (.35 + r.next() * .7)
                branch(x, y, a, len, 1, b + r.next() * .04)
            }
        }
        // Normalise birth times so the slowest needle arrives a little before the end.
        var maxB = 0.0
        for (i in 0 until sb.size) maxB = max(maxB, sb[i])
        val sc = .9 / maxB
        for (i in 0 until sb.size) sb[i] *= sc
        for (i in 0 until pb.size) pb[i] *= sc

        // A coarse arrival grid for the haze: where the front has been, the card goes pale.
        gw = ceil(w / cell).toInt()
        gh = ceil(h / cell).toInt()
        val a = DoubleArray(gw * gh) { 9.0 }
        val de = DoubleArray(gw * gh)
        for (i in 0 until sb.size) {
            val mx = ((sx1[i] + sx2[i]) / 2 / cell).toInt()
            val my = ((sy1[i] + sy2[i]) / 2 / cell).toInt()
            val rad =
                when (sg[i]) {
                    0 -> 2
                    1 -> 1
                    else -> 0
                }
            for (yy in my - rad..my + rad) for (xx in mx - rad..mx + rad) {
                if (xx < 0 || yy < 0 || xx >= gw || yy >= gh) continue
                val j = yy * gw + xx
                val v = sb[i] + hypot((xx - mx).toDouble(), (yy - my).toDouble()) * .012
                if (v < a[j]) a[j] = v
            }
        }
        fun blur() {
            val t = a.copyOf()
            for (y in 0 until gh) for (x in 0 until gw) {
                var sum = 0.0
                var m = 0
                for (dy in -1..1) for (dx in -1..1) {
                    val xx = x + dx
                    val yy = y + dy
                    if (xx < 0 || yy < 0 || xx >= gw || yy >= gh) continue
                    sum += min(t[yy * gw + xx], 1.2)
                    m++
                }
                a[y * gw + x] = min(t[y * gw + x], sum / m + .01)
            }
        }
        // Spread into the gaps (a two-pass chamfer), so the haze fills in behind the needles.
        val st2 = .018
        repeat(2) {
            for (y in 0 until gh) for (x in 0 until gw) {
                val i = y * gw + x
                if (x > 0) a[i] = min(a[i], a[i - 1] + st2)
                if (y > 0) a[i] = min(a[i], a[i - gw] + st2)
            }
            for (y in gh - 1 downTo 0) for (x in gw - 1 downTo 0) {
                val i = y * gw + x
                if (x < gw - 1) a[i] = min(a[i], a[i + 1] + st2)
                if (y < gh - 1) a[i] = min(a[i], a[i + gw] + st2)
            }
        }
        blur()
        blur()
        for (y in 0 until gh) for (x in 0 until gw) {
            val i = y * gw + x
            val ex = min(x, gw - 1 - x).toDouble() / gw
            val ey = min(y, gh - 1 - y).toDouble() / gh
            val edge = 1 - smooth(0.0, .32, min(ex * 1.6, ey))
            a[i] = min(a[i], .93 + r.next() * .05)
            de[i] =
                (.74 + edge * .22 + (r.next() - .5) * .1 + .05 * sin(x * .7 + y * .3)).coerceIn(
                    .62,
                    .97,
                )
        }
        // Fine frost fur: thousands of tiny needles, born as the haze arrives.
        repeat(3200) {
            val x = r.next() * w
            val y = r.next() * h
            val gi = (y / cell).toInt() * gw + (x / cell).toInt()
            if (r.next() > de[gi]) return@repeat
            val ang = floor(r.next() * 6) * PI / 3 + (r.next() - .5) * .3
            val l = 1.2 + r.next() * 2.6
            sx1.add(x)
            sy1.add(y)
            sx2.add(x + cos(ang) * l)
            sy2.add(y + sin(ang) * l)
            sg.add(3)
            sb.add(min(.99, a[gi] + .02 + r.next() * .05))
        }
        // Sorted by birth, keeping the order of equal ones as the browser's sort does.
        val order = (0 until sb.size).sortedBy { sb[it] }
        count = order.size
        x1 = FloatArray(count) { sx1[order[it]].toFloat() }
        y1 = FloatArray(count) { sy1[order[it]].toFloat() }
        x2 = FloatArray(count) { sx2[order[it]].toFloat() }
        y2 = FloatArray(count) { sy2[order[it]].toFloat() }
        gen = IntArray(count) { sg[order[it]] }
        born = FloatArray(count) { sb[order[it]].toFloat() }
        sparks = pb.size
        sparkX = px.toFloats()
        sparkY = py.toFloats()
        sparkBorn = pb.toFloats()
        sparkPhase = pph.toFloats()
        sparkSpeed = psp.toFloats()
        sparkSize = pk.toFloats()
        arr = FloatArray(a.size) { a[it].toFloat() }
        dens = FloatArray(de.size) { de[it].toFloat() }
    }

    /** Whether the freezing front at [f] has reached the point ([x], [y]). */
    fun arrivedAt(x: Float, y: Float, f: Float): Boolean {
        val gx = (x / cell).toInt().coerceIn(0, gw - 1)
        val gy = (y / cell).toInt().coerceIn(0, gh - 1)
        return arr[gy * gw + gx] < f
    }
}

internal fun smooth(a: Double, b: Double, x: Double): Double {
    val t = ((x - a) / (b - a)).coerceIn(0.0, 1.0)
    return t * t * (3 - 2 * t)
}

internal fun smooth(a: Float, b: Float, x: Float): Float {
    val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
    return t * t * (3 - 2 * t)
}

internal class FrostDoubles {
    private var a = DoubleArray(1024)
    var size = 0
        private set

    fun add(v: Double) {
        if (size == a.size) a = a.copyOf(size * 2)
        a[size++] = v
    }

    operator fun get(i: Int) = a[i]

    operator fun set(i: Int, v: Double) {
        a[i] = v
    }

    fun toFloats(): FloatArray = FloatArray(size) { a[it].toFloat() }
}

// Growable primitive lists, for building the frost without boxing tens of thousands of numbers.
internal class FrostInts {
    private var a = IntArray(1024)
    var size = 0
        private set

    fun add(v: Int) {
        if (size == a.size) a = a.copyOf(size * 2)
        a[size++] = v
    }

    operator fun get(i: Int) = a[i]
}
