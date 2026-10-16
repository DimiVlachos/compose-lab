package dev.dimvlachos.lab.core.presentation.components.snowglobe

import dev.dimvlachos.lab.core.presentation.canvas2d.TAU
import dev.dimvlachos.lab.core.presentation.canvas2d.mulberry
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * The gold flakes in the water: the prototype's `flakes`, `seedFlakes` and the flake half of
 * `step()`, ported line by line, in flat arrays so a frame allocates nothing. Positions are in the
 * globe's [-1, 1] space; a flake comes to rest on [floorAt], the roofs, the dome or the sea.
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
        }
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
     * One step of the water's pull on every flake: [turb] and [swirl] as the shake left them, the
     * glass's acceleration [am] (its size) and [aN] (signed, across), and its [tilt], which turns
     * which way is down.
     */
    fun step(dt: Float, t: Float, turb: Float, swirl: Float, am: Float, aN: Float, tilt: Float) {
        val gxl = sin(tilt)
        val gyl = cos(tilt)
        val calm = 1 - min(turb, 1f)
        val kk = min(1f, dt * 2.6f)
        for (i in 0 until count) {
            if (rest[i]) {
                if (turb > liftAt[i] || am > 55 + liftAt[i] * 90) {
                    rest[i] = false
                    vy[i] = -(.2f + turb * .45f)
                    vx[i] = (random.nextFloat() - .5f) * .4f
                }
                continue
            }
            val fx0 = x[i]
            val fy0 = y[i]
            val dxc = fx0
            val dyc = fy0 - .05f
            val u = swirl * -dyc * 1.7f
            val v = swirl * dxc * 1.7f
            val fx =
                turb *
                    .85f *
                    (sin(2.1f * fy0 + t * 1.7f + ph[i]) +
                        .6f * sin(3.3f * fy0 - 2.2f * fx0 + t * 2.3f))
            val fy =
                turb *
                    .85f *
                    (cos(1.9f * fx0 - t * 1.4f + ph[i]) +
                        .6f * cos(2.7f * fx0 + 3.1f * fy0 - t * 1.9f)) - turb * .3f
            val sk = if (neutral[i]) .02f + .02f * sin(t * .3f + ph[i]) else sink[i]
            val flut = sin(t * fl[i] + ph[i]) * .07f * calm
            val tvx = u + fx + gxl * sk + flut * gyl
            val tvy = v + fy + gyl * sk - flut * gxl
            vx[i] += (tvx - vx[i]) * kk
            vy[i] += (tvy - vy[i]) * kk
            vx[i] -= aN * .03f * dt
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
                val nx = x[i] / r
                val ny = y[i] / r
                x[i] = nx * .9f
                y[i] = ny * .9f
                val vn = vx[i] * nx + vy[i] * ny
                if (vn > 0) {
                    vx[i] -= 1.6f * vn * nx
                    vy[i] -= 1.6f * vn * ny
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
}
