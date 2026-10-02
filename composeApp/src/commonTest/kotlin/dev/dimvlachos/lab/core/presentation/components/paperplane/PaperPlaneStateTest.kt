package dev.dimvlachos.lab.core.presentation.components.paperplane

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class PaperPlaneStateTest {
    private val takeoff = Takeoff(Offset(1000f, 2120f), PaperPlaneDimens.IconHeading, 40f)
    private val at = Offset(500f, 1950f)

    private class Planes(
        val plane: PaperPlaneState,
        val scope: CoroutineScope,
        val text: TextLayoutResult,
    )

    private fun ComposeUiTest.planes(line: String = "Incoming!"): Planes {
        mainClock.autoAdvance = false
        lateinit var plane: PaperPlaneState
        lateinit var scope: CoroutineScope
        lateinit var text: TextLayoutResult
        setContent {
            plane = remember { PaperPlaneState(EmptyCoroutineContext) }
            scope = rememberCoroutineScope()
            val measurer = rememberTextMeasurer()
            text = remember { measurer.measure(line, TextStyle(fontSize = 16.sp)) }
        }
        plane.stage = Rect(0f, 0f, 1080f, 2200f)
        plane.density = 2.75f
        return Planes(plane, scope, text)
    }

    @Test
    fun itDropsEveryLetterIntoItsPlaceAsItPassesAndFliesOff() = runComposeUiTest {
        val planes = planes()
        var done = false
        runOnUiThread {
            planes.scope.launch {
                planes.plane.launch("hello", takeoff, planes.text) { at }
                done = true
            }
        }
        mainClock.advanceTimeBy(32)
        val flight = planes.plane.flights.single()
        // Thrown the size of the glyph it left as, nothing let go yet.
        assertTrue(flight.placement(0f).scale * flight.plan.length < 41f)
        assertEquals(0f, planes.plane.delivered("hello"))
        // Let go left to right, as it passes over each letter.
        val lets = flight.drops.sortedBy { it.box.left }.map { it.letGo }
        assertEquals(lets.sorted(), lets)
        // Over each letter's place, less the way it is carried on, when that one is let go.
        for (drop in flight.drops) {
            val over = flight.placement(drop.letGo).at
            assertTrue(over.x < at.x + drop.box.center.x, "let go after its place")
            assertTrue(over.y < at.y + drop.box.top, "let go below the text")
        }
        // All of it down within the drop's time.
        val down = lets.last() + PaperPlaneDimens.FallMs - lets.first()
        assertTrue(down <= PaperPlaneDimens.DropMs + 1f, "down in $down ms")
        mainClock.advanceTimeBy(flight.endMs.toLong() + 64L)
        assertTrue(done)
        assertTrue(planes.plane.flights.isEmpty())
        // Gone off the right of the stage by the end.
        assertTrue(flight.placement(flight.pace.totalMs).at.x > 1080f)
    }

    @Test
    fun aLongMessageStillComesDownInTheDropsTime() = runComposeUiTest {
        val planes = planes("A much longer message, the whole width of the bubble across")
        runOnUiThread {
            planes.scope.launch {
                planes.plane.launch(1L, takeoff, planes.text) { Offset(20f, 1900f) }
            }
        }
        mainClock.advanceTimeBy(32)
        val lets = planes.plane.flights.single().drops.map { it.letGo }
        val down = lets.max() + PaperPlaneDimens.FallMs - lets.min()
        assertTrue(down <= PaperPlaneDimens.DropMs + 1f, "down in $down ms")
    }

    @Test
    fun severalPlanesCanBeInTheAirAtOnce() = runComposeUiTest {
        val planes = planes()
        runOnUiThread {
            planes.scope.launch { planes.plane.launch(1L, takeoff, planes.text) { at } }
            planes.scope.launch {
                planes.plane.launch(2L, takeoff, planes.text) { at - Offset(0f, 120f) }
            }
        }
        mainClock.advanceTimeBy(32)
        assertEquals(listOf<Any>(1L, 2L), planes.plane.flights.map { it.key })
    }

    @Test
    fun stoppedMidFlightItLeavesNothingInTheAir() = runComposeUiTest {
        val planes = planes()
        var job: Job? = null
        runOnUiThread {
            job = planes.scope.launch { planes.plane.launch("hello", takeoff, planes.text) { at } }
        }
        mainClock.advanceTimeBy(400)
        runOnUiThread { job?.cancel() }
        mainClock.advanceTimeBy(32)
        assertTrue(planes.plane.flights.isEmpty())
    }
}
