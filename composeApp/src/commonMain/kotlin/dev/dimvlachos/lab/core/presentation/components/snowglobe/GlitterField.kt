package dev.dimvlachos.lab.core.presentation.components.snowglobe

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The gold flakes in the water: laid out as the prototype's `flakes` and `seedFlakes`, in flat
 * arrays so a frame allocates nothing. Positions are in the globe's [-1, 1] space; a flake comes to
 * rest on [floorAt], the roofs, the dome or the sea.
 *
 * The water lags the glass: a shove one way sets it running the other way through the middle and
 * back round the top and bottom, and each turn of a shake sheds a pair of eddies off the sides. A
 * flake wanders in it on its own, follows it as quickly as it is light, is thrown against a shove
 * as much as it is heavy, and sinks the faster the bigger it is.
 */
internal class GlitterField(private val floorAt: (Float) -> Float) {
    val count = 320
    val x = FloatArray(count)
    val y = FloatArray(count)
    val vx = FloatArray(count)
    val vy = FloatArray(count)
    val ang = FloatArray(count)
    val rot = FloatArray(count)
    val spin = FloatArray(count)
    val s = FloatArray(count)
    val sink = FloatArray(count)
    val ph = FloatArray(count)
    val fl = FloatArray(count)
    val restA = FloatArray(count)
    val liftAt = FloatArray(count)
    val dy = FloatArray(count)
    val tw = FloatArray(count)
    val rest = BooleanArray(count)
    val near = BooleanArray(count)
    val neutral = BooleanArray(count)

    // Each flake's own wander in the stirred water, how long it takes to follow the water, how hard
    // a shove of the glass throws it, and what its drift is scaled by to sink as its size does.
    private val nx = FloatArray(count)
    private val ny = FloatArray(count)
    private val lag = FloatArray(count)
    private val heft = FloatArray(count)
    private val weight = FloatArray(count)

    // The water: the current a shove leaves through the middle, and the eddies a shake sheds.
    private var current = 0f
    private var lastShove = 0f
    private val ex = FloatArray(Eddies)
    private val ey = FloatArray(Eddies)
    private val eg = FloatArray(Eddies)
    private var nextEddy = 0

    // The prototype lifts flakes with Math.random(); a seeded one keeps the tests repeatable.
    private var random = Random(31)

    init {
        seed()
    }

    /** Lays the flakes out as the prototype's `seedFlakes` does, from the same mulberry(31). */
    fun seed() {
        val r = mulberry(31)
        fun R() = r().toFloat()
        random = Random(31)
        current = 0f
        lastShove = 0f
        eg.fill(0f)
        nextEddy = 0
        for (i in 0 until count) {
            vx[i] = 0f
            vy[i] = 0f
            // In the order the prototype's object literal reads them.
            ang[i] = R() * TAU
            rot[i] = R() * TAU
            spin[i] = (R() - .5f) * 6
            s[i] = .014f + R() * .014f
            sink[i] = .2f + R() * .2f
            ph[i] = R() * TAU
            fl[i] = 1.4f + R() * 2.2f
            rest[i] = true
            restA[i] = R() * TAU
            near[i] = R() < .06f
            neutral[i] = false
            liftAt[i] = .14f + R() * .55f
            dy[i] = R() * .028f
            tw[i] = .3f + R()
            if (near[i]) s[i] = .03f + R() * .012f
            nx[i] = 0f
            ny[i] = 0f
            var px: Float
            var py: Float
            do {
                px = (R() * 2 - 1) * .74f
                py = floorAt(px)
            } while (hypot(px, py) > .88f)
            x[i] = px
            y[i] = py + if (py >= FLOOR) dy[i] else 0f
            if (i < 10) {
                neutral[i] = true
                near[i] = false
                s[i] = .012f + R() * .01f
                rest[i] = false
                x[i] = (R() * 2 - 1) * .6f
                y[i] = -.6f + R() * .9f
                sink[i] = .02f
            } else if (i < 26) {
                rest[i] = false
                x[i] = (R() * 2 - 1) * .7f
                y[i] = -.75f + R() * 1.1f
            }
            val size = ((s[i] - .012f) / .03f).coerceIn(0f, 1f)
            lag[i] = .08f + .3f * size
            heft[i] = .0018f * (.6f + .8f * size) / lag[i]
            // How fast it sinks is mostly how big it is, a tenth either way.
            weight[i] = (.2f + 7 * (s[i] - .012f)) * (.9f + .2f * random.nextFloat()) / sink[i]
        }
    }

    /** The water's flow at ([x], [y]), into [out] as x then y, the flakes' own wander aside. */
    fun waterAt(x: Float, y: Float, out: FloatArray) {
        // The current through the middle, returning round the top and bottom: the flow of a
        // stream function current · y · (1 − r²/R²), which runs along the glass and never through
        // it.
        val r2 = Rim * Rim
        val yc = y - .05f
        var u = current * (1 - (x * x + 3 * yc * yc) / r2)
        var v = current * 2 * x * yc / r2
        for (e in 0 until Eddies) {
            val g = eg[e]
            if (g == 0f) continue
            val dx = x - ex[e]
            val dy = y - ey[e]
            val d2 = dx * dx + dy * dy + 1e-6f
            // A Lamb–Oseen eddy: turning as a solid near its core, falling off as 1/r beyond it.
            val k = g / (TAU * d2) * (1 - exp(-d2 / (EddyCore * EddyCore)))
            u -= dy * k
            v += dx * k
        }
        out[0] = u
        out[1] = v
    }

    private val flow = FloatArray(2)

    /** A Gaussian draw, for each flake's wander. */
    private fun gauss(): Float {
        val u1 = random.nextFloat().coerceAtLeast(1e-7f)
        val u2 = random.nextFloat()
        return sqrt(-2 * ln(u1)) * cos(TAU * u2)
    }

    /**
     * How brightly flake [i] catches the light at [t]: a resting one flashes as the glass's [tilt]
     * and [shift] (its slide over the radius) turn it to the light, a falling one as it tumbles.
     */
    fun brightness(i: Int, t: Float, tilt: Float, shift: Float): Float =
        if (rest[i])
            .3f + .7f * abs(cos(restA[i] + tilt * 7 + shift * 2 + t * .12f * tw[i])).pow(30)
        else .28f + .72f * abs(cos(ang[i])).pow(16)

    /**
     * One step of the water and the flakes in it: [turb] as the shake left the water stirred, the
     * glass's acceleration [am] (its size) and [aN] (signed, across), and its [tilt], which turns
     * which way is down. [swirl] sets how hard a shake's eddies turn.
     */
    fun step(dt: Float, t: Float, turb: Float, swirl: Float, am: Float, aN: Float, tilt: Float) {
        val gxl = sin(tilt)
        val gyl = cos(tilt)
        val calm = 1 - min(turb, 1f)
        // The water lags the glass, and the glass's walls slow it again.
        current = (current - aN * WaterLag * dt).coerceIn(-1.6f, 1.6f) * exp(-dt / CurrentFades)
        // Each turn of a shake sheds a pair of eddies off the sides, turning opposite ways.
        if (aN != 0f) {
            if (lastShove != 0f && sign(aN) != sign(lastShove) && abs(current) > .25f) {
                val y0 = (random.nextFloat() - .5f) * .6f
                val strength = current * (.9f + swirl * .6f)
                for (side in intArrayOf(-1, 1)) {
                    val e = nextEddy
                    nextEddy = (nextEddy + 1) % Eddies
                    ex[e] = side * (.5f + random.nextFloat() * .15f)
                    ey[e] = y0 + (random.nextFloat() - .5f) * .3f
                    eg[e] = side * strength
                }
            }
            lastShove = aN
        }
        for (e in 0 until Eddies) {
            if (eg[e] == 0f) continue
            eg[e] *= exp(-dt / EddyFades)
            if (abs(eg[e]) < .01f) eg[e] = 0f
        }
        // Each flake's wander: a random drift that turns over in WanderTurns, as strong as the
        // water
        // is stirred.
        val sigma = WanderSize * sqrt(min(turb, 1.7f))
        val kick = sigma * sqrt(2 * dt / WanderTurns)
        val keep = exp(-dt / WanderTurns)
        for (i in 0 until count) {
            nx[i] = nx[i] * keep + kick * gauss()
            ny[i] = ny[i] * keep + kick * gauss()
        }
        for (i in 0 until count) {
            if (rest[i]) {
                waterAt(x[i], y[i], flow)
                val moving = hypot(flow[0] + nx[i], flow[1] + ny[i])
                if (turb > liftAt[i] || am > 55 + liftAt[i] * 90 || moving > .35f + liftAt[i]) {
                    rest[i] = false
                    vx[i] = flow[0] + (random.nextFloat() - .5f) * .4f
                    vy[i] = flow[1] - (.2f + turb * .45f)
                }
                continue
            }
            waterAt(x[i], y[i], flow)
            val sk = if (neutral[i]) .02f + .02f * sin(t * .3f + ph[i]) else sink[i] * weight[i]
            // A flat flake falling through still water swings from side to side as it goes.
            val flut = sin(t * fl[i] + ph[i]) * .3f * sk * calm
            val tvx = flow[0] + nx[i] + gxl * sk + flut * gyl
            // Churned water holds the gold up, at most as hard as it sinks, so it hangs rather than
            // rising to the dome; it sinks as the churning dies down.
            val tvy = flow[1] + ny[i] + gyl * sk * (1 - min(turb, 1f)) - flut * gxl
            val follow = min(1f, dt / lag[i])
            vx[i] += (tvx - vx[i]) * follow
            vy[i] += (tvy - vy[i]) * follow
            // Heavier than the water, the gold is thrown against a shove more than the water is.
            vx[i] -= aN * heft[i] * dt
            var sp = hypot(vx[i], vy[i])
            if (sp > 2.2f) {
                vx[i] *= 2.2f / sp
                vy[i] *= 2.2f / sp
                sp = 2.2f
            }
            x[i] += vx[i] * dt
            y[i] += vy[i] * dt
            ang[i] += spin[i] * (.6f + 3.5f * sp) * dt
            rot[i] += spin[i] * .15f * dt
            val r = hypot(x[i], y[i])
            if (r > .9f) {
                val nxr = x[i] / r
                val nyr = y[i] / r
                x[i] = nxr * .9f
                y[i] = nyr * .9f
                val vn = vx[i] * nxr + vy[i] * nyr
                if (vn > 0) {
                    vx[i] -= 1.6f * vn * nxr
                    vy[i] -= 1.6f * vn * nyr
                }
            }
            if (neutral[i]) {
                if (y[i] > .25f) vy[i] -= .4f * dt
                if (y[i] < -.75f) vy[i] += .4f * dt
                continue
            }
            val fl0 = floorAt(x[i])
            val floor = fl0 + if (fl0 >= FLOOR) dy[i] else 0f
            if (y[i] > floor) {
                y[i] = floor
                if (turb < .36f || sp < .45f) {
                    rest[i] = true
                    vx[i] = 0f
                    vy[i] = 0f
                    restA[i] = ang[i]
                } else {
                    vy[i] = -abs(vy[i]) * .3f
                }
            }
        }
    }

    private companion object {
        const val Rim = .9f
        const val Eddies = 8
        const val EddyCore = .2f
        const val EddyFades = 1.3f
        const val WaterLag = .035f
        const val CurrentFades = .9f
        const val WanderSize = .42f
        const val WanderTurns = .45f
    }
}
