package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilingsPainterTest {
    private val width = 400f
    private val height = 600f

    private fun out(at: Offset) =
        Magnet("a").apply {
            this.at = at
            onTable = true
        }

    @Test
    fun withNoMagnetOutTheFilingsLieAsTheyFell() {
        val filings = FilingsPainter(count = 200, seed = 1)
        val fell = List(200) { filings.angleOf(it) }
        filings.align(listOf(Magnet("a")), width, height)
        for (i in 0 until 200) {
            assertEquals(fell[i], filings.angleOf(i))
            assertEquals(0f, filings.strengthOf(i))
        }
    }

    @Test
    fun aFilingNearAMagnetPointsAwayFromIt() {
        val filings = FilingsPainter(count = 800, seed = 2)
        val magnet = Offset(200f, 300f)
        filings.align(listOf(out(magnet)), width, height)
        var checked = 0
        for (i in 0 until 800) {
            val spot = filings.spotOf(i)
            val at = Offset(spot.x * width, spot.y * height)
            val apart = at - magnet
            val distance = apart.getDistance()
            if (distance !in 40f..120f) continue
            val along = Offset(cos(filings.angleOf(i)), sin(filings.angleOf(i)))
            assertTrue((along.x * apart.x + along.y * apart.y) / distance > 0.99f, "filing $i")
            checked++
        }
        assertTrue(checked > 10, "only $checked filings near the magnet")
    }

    @Test
    fun filingsRiseMoreNearAMagnetThanFarFromIt() {
        val filings = FilingsPainter(count = 800, seed = 3)
        val magnet = Offset(200f, 300f)
        filings.align(listOf(out(magnet)), width, height)
        fun distance(i: Int): Float {
            val spot = filings.spotOf(i)
            return (Offset(spot.x * width, spot.y * height) - magnet).getDistance()
        }
        val nearest = (0 until 800).minBy(::distance)
        val farthest = (0 until 800).maxBy(::distance)
        assertTrue(filings.strengthOf(nearest) > filings.strengthOf(farthest) + 0.5f)
        assertTrue((0 until 800).all { filings.strengthOf(it) in 0f..1f })
    }

    @Test
    fun everyFilingIsTracedOnceAsALineAndTheRestLieOffTheTable() {
        val filings = FilingsPainter(count = 300, seed = 4)
        filings.align(listOf(out(Offset(200f, 300f))), width, height)
        filings.trace(width, height, density = 2f)
        var traced = 0
        for (bucket in 0 until filings.buckets) {
            val points = filings.pointsOf(bucket)
            val used = filings.tracedIn(bucket)
            traced += used
            // Each filing is a line from one point to the next, four floats.
            for (k in 0 until used) {
                val cx = (points[k * 4] + points[k * 4 + 2]) / 2f
                val cy = (points[k * 4 + 1] + points[k * 4 + 3]) / 2f
                assertTrue(cx in 0f..width * 2f && cy in 0f..height * 2f, "filing $k at $cx, $cy")
            }
            // What the bucket doesn't use is drawn nowhere near the table.
            for (k in used * 4 until points.size) assertTrue(
                points[k] < 0f,
                "spare $k is ${points[k]}",
            )
        }
        assertEquals(300, traced)
    }

    @Test
    fun withTheMagnetsGoneTheFilingsSettleBackAsTheyFell() {
        val filings = FilingsPainter(count = 200, seed = 5)
        val fell = List(200) { filings.angleOf(it) }
        filings.align(listOf(out(Offset(200f, 300f))), width, height)
        val aligned = List(200) { filings.angleOf(it) }
        assertTrue((0 until 200).any { axisApart(aligned[it], fell[it]) > 0.3f })
        // Just put away, they still lie along the field that was.
        filings.align(emptyList(), width, height, calm = 0f)
        for (i in 0 until 200) assertEquals(0f, axisApart(aligned[i], filings.angleOf(i)), 1e-4f)
        // Halfway calm, each lies between, no further from either than they are apart.
        filings.align(emptyList(), width, height, calm = 0.5f)
        for (i in 0 until 200) {
            val apart = axisApart(aligned[i], fell[i])
            assertTrue(axisApart(filings.angleOf(i), fell[i]) <= apart / 2f + 1e-3f, "filing $i")
        }
        // Calm, they lie as they fell.
        filings.align(emptyList(), width, height, calm = 1f)
        for (i in 0 until 200) assertEquals(0f, axisApart(fell[i], filings.angleOf(i)), 1e-4f)
    }

    // How far apart two filings' lines are, in radians: a line has no head, so 0 to π/2.
    private fun axisApart(a: Float, b: Float): Float {
        val pi = kotlin.math.PI.toFloat()
        var d = (a - b) % pi
        if (d < 0f) d += pi
        return kotlin.math.min(d, pi - d)
    }
}
