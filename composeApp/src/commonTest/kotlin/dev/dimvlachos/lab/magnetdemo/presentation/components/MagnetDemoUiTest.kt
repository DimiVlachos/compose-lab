package dev.dimvlachos.lab.magnetdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.magnet.MagnetState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.magnetdemo.IslandTags
import dev.dimvlachos.lab.magnetdemo.MagnetDemos
import dev.dimvlachos.lab.magnetdemo.rememberIslandMagnetState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MagnetDemoUiTest {
    @Test
    fun theScriptSearchesApartThenTogetherOpensAPhotoAndPutsEverythingBack() = runComposeUiTest {
        mainClock.autoAdvance = false
        val demo = MagnetDemos.all.single()
        val state = DemoState()
        var finished = false
        var table: MagnetState? = null
        setContent {
            LabTheme {
                Box(Modifier.size(400.dp, 800.dp)) {
                    val islands = rememberIslandMagnetState()
                    table = islands
                    MagnetDemo(state, islands)
                }
                LaunchedEffect(Unit) {
                    demo.script.play(state)
                    finished = true
                }
            }
        }
        // The script's clock runs 600 ms behind the screen's, held still at the start.
        val start = mainClock.currentTime
        fun at(seconds: Double) =
            mainClock.advanceTimeBy(
                start + ((seconds + 0.6) * 1_000).toLong() - mainClock.currentTime
            )
        fun results(tag: String) = table!!.results[tag].orEmpty()
        val corfuAndNaxos = setOf("corfu", "naxos")
        // Sunset and Sea apart, two searches: Sunset has every one of its photos, even those that
        // lay across the table, and Sea every one of its own that Sunset hadn't taken first.
        at(7.7)
        assertEquals(
            setOf("corfu", "santorini", "naxos", "folegandros"),
            results(IslandTags.Sunset),
        )
        assertEquals(setOf("milos", "crete", "rhodes", "zakynthos"), results(IslandTags.Sea))
        // Sea snapped to Sunset's side: one search, for the photos of both.
        at(10.4)
        assertEquals(corfuAndNaxos, results(IslandTags.Sunset))
        assertEquals(corfuAndNaxos, results(IslandTags.Sea))
        // Fanned out, Corfu opened and closed, and folded back.
        at(11.3)
        assertEquals(IslandTags.Sunset, table!!.fannedOut)
        at(12.8)
        assertEquals("corfu", table!!.opened)
        at(14.9)
        assertNull(table!!.opened)
        at(15.9)
        assertNull(table!!.fannedOut)
        // Sea flicked home: Sunset is a search on its own again, and has all its photos back.
        at(18.4)
        assertEquals(
            setOf("corfu", "santorini", "naxos", "folegandros"),
            results(IslandTags.Sunset),
        )
        assertTrue(IslandTags.Sea !in table!!.results, "${table!!.results}")
        // Flicked, Sea tore straight off the pair: Sunset was left where the two were joined.
        val sunsetAt = table!!.magnetPosition(IslandTags.Sunset)!! / table!!.density
        val joinedAt = MagnetDemos.SunsetPath.last()
        assertTrue(
            (sunsetAt - Offset(joinedAt.x * 400f, joinedAt.y * 800f)).getDistance() < 60f,
            "Sunset was dragged off to $sunsetAt",
        )
        // Both flicked back by the end: nothing out, everything home, and the table asleep.
        at(demo.script.nominalDuration.inWholeMilliseconds / 1_000.0 - 0.5)
        assertTrue(finished, "the script must have played to its end")
        assertTrue(table!!.results.isEmpty(), "${table!!.results}")
        for (body in table!!.bodies.bodies) {
            assertTrue((body.at - body.home).getDistance() < 2f, "${body.id} is not home")
        }
        assertFalse(table!!.awake, "the table must be still by the end")
    }

    @Test
    fun theHintGivesWayWhileAMagnetIsOut() = runComposeUiTest {
        var table: MagnetState? = null
        setContent {
            LabTheme {
                Box(Modifier.size(400.dp, 800.dp)) {
                    val islands = rememberIslandMagnetState()
                    table = islands
                    MagnetDemo(DemoState(), islands)
                }
            }
        }
        val hint = "Drag a magnet over the photos to filter them"
        onNodeWithText(hint).assertExists()
        runOnUiThread { table!!.apply(IslandTags.Sunset) }
        mainClock.advanceTimeBy(1_000)
        onNodeWithText(hint).assertDoesNotExist()
        runOnUiThread { table!!.remove(IslandTags.Sunset) }
        mainClock.advanceTimeBy(1_000)
        onNodeWithText(hint).assertExists()
    }
}
