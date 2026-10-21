package dev.dimvlachos.lab.magnetdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.magnet.MagnetDimens
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
                start + ((seconds + 0.6) * 1_000).toLong() - mainClock.currentTime,
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
    fun onAPhoneOnItsSideTheScriptStillSearchesApartThenTogether() = runComposeUiTest {
        mainClock.autoAdvance = false
        val demo = MagnetDemos.all.single()
        val state = DemoState()
        var table: MagnetState? = null
        setContent {
            LabTheme {
                // A phone on its side, the stage under the top bar.
                Box(Modifier.size(780.dp, 270.dp)) {
                    val islands = rememberIslandMagnetState()
                    table = islands
                    MagnetDemo(state, islands)
                }
                LaunchedEffect(Unit) { demo.script.play(state) }
            }
        }
        val start = mainClock.currentTime
        fun at(seconds: Double) =
            mainClock.advanceTimeBy(
                start + ((seconds + 0.6) * 1_000).toLong() - mainClock.currentTime,
            )
        fun results(tag: String) = table!!.results[tag].orEmpty()
        // The hint lies over the table, which has every bit of height over the strip.
        at(0.0)
        assertEquals(270f - table!!.stripHeight, table!!.tableHeight, 0.5f)
        // Put down apart, two searches.
        at(7.7)
        assertFalse(table!!.joined, "Sea must first be put down apart")
        assertTrue(results(IslandTags.Sunset).isNotEmpty())
        assertTrue(results(IslandTags.Sea).isNotEmpty())
        // Brought up close enough to snap to Sunset's side: one search, for the photos of both.
        at(10.4)
        assertTrue(table!!.joined, "Sea must snap to Sunset's side")
        assertEquals(setOf("corfu", "naxos"), results(IslandTags.Sunset))
        at(11.3)
        assertEquals(IslandTags.Sunset, table!!.fannedOut)
        // Fanned out over the whole stage, strip and all, the cards grow as far as they may.
        assertEquals(MagnetDimens.FanScale, table!!.fanScale())
        at(12.8)
        assertEquals("corfu", table!!.opened)
        // Opened, the photo takes most of the stage's height, not a sliver of the table's.
        val opened = table!!.openedBounds
        val openedHeight = opened.height / table!!.density
        assertTrue(openedHeight >= 270f * 0.6f, "the photo opened $openedHeight dp tall")
        assertTrue(opened.top >= 0f && opened.bottom <= 270f * table!!.density, "$opened")
        at(demo.script.nominalDuration.inWholeMilliseconds / 1_000.0 - 0.5)
        assertTrue(table!!.results.isEmpty(), "${table!!.results}")
        for (body in table!!.bodies.bodies) {
            assertTrue((body.at - body.home).getDistance() < 2f, "${body.id} is not home")
        }
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

    @Test
    fun theHintLiesAboveTheTableSoNoPhotoCanLieUnderIt() = runComposeUiTest {
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
        val hint = onNodeWithText("Drag a magnet over the photos to filter them")
        val hintBottom = hint.getUnclippedBoundsInRoot().bottom
        // A card can lie anywhere on the table, its top edge right at the table's, so the table's
        // top, worked out from where a card lies on it, must be below the hint.
        val photo = table!!.photos.first()
        val cardTop = onNodeWithContentDescription(photo.title).getUnclippedBoundsInRoot().top
        val onTable = table!!.photoCentre(0).y - MagnetDimens.CardHeight.value / 2f
        val tableTop = cardTop - onTable.dp
        assertTrue(tableTop >= hintBottom, "the table starts at $tableTop, under the hint")
    }
}
