package dev.dimvlachos.lab.fogdemo

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue

class OvalScrubTest {
    // The demo's own scrub, measured in dp on a 400 x 500 frame: the clip's 4:5.
    private val frame = Offset(400f, 500f)
    private val path = FogDemos.porthole.path().map { it.inFrame() }
    private val brush = FogDemos.ScrubBrush.value

    // The oval the fingertip traces; the brush fattens it into the clear oval.
    private val centre = FogDemos.PortholeCentre.inFrame()
    private val radii = FogDemos.PortholeRadii.inFrame()

    private fun Offset.inFrame() = Offset(x * frame.x, y * frame.y)

    private fun distanceToPath(point: Offset) = path.minOf { (it - point).getDistance() }

    // A 4 dp grid over the fingertip's oval.
    private fun gridInsideTheOval(): List<Offset> {
        val points = mutableListOf<Offset>()
        var y = centre.y - radii.y
        while (y <= centre.y + radii.y) {
            var x = centre.x - radii.x
            while (x <= centre.x + radii.x) {
                val dx = (x - centre.x) / radii.x
                val dy = (y - centre.y) / radii.y
                if (dx * dx + dy * dy <= 1f) points += Offset(x, y)
                x += 4f
            }
            y += 4f
        }
        return points
    }

    @Test
    fun thePassesJoinUpSoTheWholeOvalIsClear() {
        // Nowhere inside is beyond the brush's solid core, where overlapping dabs clear the fog
        // completely: no streaks left between the rows.
        val core = brush * 0.6f
        val gaps = gridInsideTheOval().filter { distanceToPath(it) > core }
        assertTrue(gaps.isEmpty(), "fog left at ${gaps.take(5)}")
    }

    @Test
    fun theGlassAroundTheOvalStaysFogged() {
        val ring =
            (0 until 72).map { step ->
                val angle = step * 5 * PI / 180
                Offset(
                    centre.x + (radii.x + brush * 1.5f) * cos(angle).toFloat(),
                    centre.y + (radii.y + brush * 1.5f) * sin(angle).toFloat(),
                )
            }
        val touched = ring.filter { distanceToPath(it) < brush }
        assertTrue(touched.isEmpty(), "wiped outside the oval at ${touched.take(5)}")
    }

    @Test
    fun theHandStartsAtTheTopAndWorksDownToTheBottom() {
        assertTrue(abs(path.first().y - (centre.y - radii.y)) < 0.5f, "starts at ${path.first()}")
        assertTrue(abs(path.last().y - (centre.y + radii.y)) < 0.5f, "ends at ${path.last()}")
    }

    @Test
    fun theScrubIsAQuickHandNotALongChore() {
        val seconds = FogDemos.porthole.duration.inWholeMilliseconds / 1000f
        assertTrue(seconds in 3f..6f, "the scrub takes ${seconds}s")
    }
}
