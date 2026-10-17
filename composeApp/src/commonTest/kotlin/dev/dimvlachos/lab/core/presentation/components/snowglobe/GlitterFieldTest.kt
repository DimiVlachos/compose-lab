package dev.dimvlachos.lab.core.presentation.components.snowglobe

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GlitterFieldTest {
    private val dt = 1f / 120f
    private val scenes = SantoriniScenes.shared

    private fun field() = GlitterField(scenes::floorAt)

    /** Steps the glitter in water stirred to [turb], as a shake leaves it, for [seconds]. */
    private fun GlitterField.run(seconds: Float, turb: Float, from: Float = 0f): Float {
        var t = from
        repeat((seconds / dt).toInt()) {
            step(dt, t, turb = turb, swirl = turb * .7f, am = turb * 300f, aN = 0f, tilt = 0f)
            t += dt
        }
        return t
    }

    private fun GlitterField.meanY() = (0 until count).sumOf { y[it].toDouble() } / count

    private fun GlitterField.restingPlace(i: Int): Float {
        val floor = scenes.floorAt(x[i])
        return floor + if (floor >= FLOOR) dy[i] else 0f
    }

    @Test
    fun itStartsAsThePrototypeSeedsIt() {
        val g = field()
        assertEquals(320, g.count)
        // Ten drift neutrally and sixteen more are still falling; the rest lie where they landed.
        for (i in 0 until 26) assertTrue(!g.rest[i], "flake $i should be in the water")
        for (i in 26 until g.count) {
            assertTrue(g.rest[i])
            assertEquals(g.restingPlace(i), g.y[i], 1e-5f)
        }
        assertTrue((0 until 10).all { g.neutral[it] })
    }

    @Test
    fun flakesStayInsideTheSphereUnderAViolentFlow() {
        val g = field()
        g.run(seconds = 4f, turb = 1.7f)
        for (i in 0 until g.count) {
            assertTrue(hypot(g.x[i], g.y[i]) <= .9f + 1e-4f, "flake $i escaped")
        }
    }

    @Test
    fun aStrongFlowLiftsTheGlitter() {
        val g = field()
        val before = g.meanY()
        g.run(seconds = 1f, turb = 1f)
        assertTrue(g.meanY() < before - 0.2, "mean y ${g.meanY()} vs $before")
    }

    @Test
    fun inStillWaterEveryFlakeComesToRestOnItsSurface() {
        val g = field()
        val t = g.run(seconds = 1.5f, turb = 1f)
        g.run(seconds = 8f, turb = 0f, from = t)
        // The neutral ten hang in the water by design.
        for (i in 10 until g.count) {
            assertTrue(g.rest[i], "flake $i still falling at y=${g.y[i]}")
            assertTrue(abs(g.y[i] - g.restingPlace(i)) < 1e-3f)
        }
    }

    @Test
    fun goldSettlesOnTheRoofs() {
        val g = field()
        val t = g.run(seconds = 1.5f, turb = 1f)
        g.run(seconds = 8f, turb = 0f, from = t)
        val onRoofs =
            (10 until g.count).count { i ->
                houses.any { h -> g.x[i] in h.x..h.x + h.w } && g.y[i] < FLOOR - .05f
            }
        assertTrue(onRoofs >= 10, "only $onRoofs flakes on the roofs")
    }

    @Test
    fun brightnessStaysInItsRange() {
        val g = field()
        g.run(seconds = 0.5f, turb = 1f)
        for (i in 0 until g.count) assertTrue(
            g.brightness(i, 0.5f, tilt = .1f, shift = .2f) in .28f..1f
        )
    }

    /** The flakes in the water, not the neutral ten that hang there by design. */
    private fun GlitterField.free() = (10 until count).filter { !rest[it] }

    /** Stirs the glitter up off its surfaces, then lets the glass push [aN] for [seconds]. */
    private fun GlitterField.stirThenPush(aN: Float, seconds: Float): Float {
        var t = run(seconds = 1.2f, turb = 1f)
        repeat((seconds / dt).toInt()) {
            step(dt, t, turb = .6f, swirl = .4f, am = abs(aN), aN = aN, tilt = 0f)
            t += dt
        }
        return t
    }

    @Test
    fun theWaterLagsTheGlassAndCirclesBack() {
        // Shoved right, the water through the middle of the globe runs left and comes back round
        // along the top, and keeps on a moment after the shove.
        for (push in listOf(160f, -160f)) {
            val g = field()
            var t = 0f
            val w = FloatArray(2)
            repeat((.2f / dt).toInt()) {
                g.step(dt, t, turb = .6f, swirl = .4f, am = abs(push), aN = push, tilt = 0f)
                t += dt
            }
            g.waterAt(0f, .05f, w)
            assertTrue(w[0] * push < 0 && abs(w[0]) > .3f, "pushed $push, the middle at ${w[0]}")
            g.waterAt(0f, -.75f, w)
            assertTrue(w[0] * push > 0, "pushed $push, the top at ${w[0]}")
            repeat((.3f / dt).toInt()) {
                g.step(dt, t, turb = .5f, swirl = .3f, am = 0f, aN = 0f, tilt = 0f)
                t += dt
            }
            g.waterAt(0f, .05f, w)
            assertTrue(w[0] * push < 0 && abs(w[0]) > .15f, "pushed $push, .3 s on at ${w[0]}")
        }
    }

    @Test
    fun theGoldIsThrownAgainstTheShoveThroughTheWater() {
        // Heavier than the water, the gold is thrown against a shove further than the water round
        // it.
        for (push in listOf(160f, -160f)) {
            val g = field()
            g.stirThenPush(aN = push, seconds = .2f)
            val w = FloatArray(2)
            val free = g.free()
            val vx =
                free.sumOf {
                    g.waterAt(g.x[it], g.y[it], w)
                    (g.vx[it] - w[0]).toDouble()
                } / free.size
            assertTrue(vx * push < 0 && abs(vx) > .15, "pushed $push, the gold through it at $vx")
        }
    }

    @Test
    fun theStirredGlitterDoesNotSwayAsOneCloud() {
        // In water stirred and left alone, the flakes' motion is their own: what they share,
        // sinking aside, is a small part of how they move. Read over a second, not one frame.
        val g = field()
        var t = g.run(seconds = 1f, turb = 1f)
        var shared = 0.0
        var all = 0.0
        repeat(12) {
            t = g.run(seconds = 1f / 12, turb = 1f, from = t)
            val free = g.free()
            val mx = free.sumOf { g.vx[it].toDouble() } / free.size
            shared += mx * mx
            all += free.sumOf { (g.vx[it] * g.vx[it]).toDouble() } / free.size
        }
        assertTrue(shared / all < .1, "the shared sway is ${shared / all} of the sideways motion")
    }

    @Test
    fun bigFlakesSinkFasterThanFineOnes() {
        val g = field()
        val t = g.run(seconds = 1.5f, turb = 1f)
        g.run(seconds = .8f, turb = 0f, from = t)
        val free = g.free()
        fun sinking(near: Boolean) =
            free.filter { g.near[it] == near }.map { g.vy[it].toDouble() }.average()
        assertTrue(
            sinking(true) > sinking(false) * 1.3,
            "big ${sinking(true)}, fine ${sinking(false)}",
        )
    }

    @Test
    fun theFinestFlakesAreTheLastToLand() {
        val g = field()
        var t = g.run(seconds = 1.5f, turb = 1f)
        val landed = mutableListOf<Int>()
        repeat((8f / dt).toInt()) {
            val was = BooleanArray(g.count) { g.rest[it] }
            g.step(dt, t, turb = 0f, swirl = 0f, am = 0f, aN = 0f, tilt = 0f)
            t += dt
            for (i in 10 until g.count) if (g.rest[i] && !was[i]) landed += i
        }
        assertTrue(landed.size > 100, "only ${landed.size} landed")
        val fifth = landed.size / 5
        fun size(of: List<Int>) = of.map { g.s[it].toDouble() }.average()
        val first = size(landed.take(fifth))
        val last = size(landed.takeLast(fifth))
        assertTrue(last < first, "the last to land average $last, the first $first")
    }
}
