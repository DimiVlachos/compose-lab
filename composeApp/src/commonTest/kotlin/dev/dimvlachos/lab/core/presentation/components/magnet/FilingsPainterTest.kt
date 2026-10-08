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
}
