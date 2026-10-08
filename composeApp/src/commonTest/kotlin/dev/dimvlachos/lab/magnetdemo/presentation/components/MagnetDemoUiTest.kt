package dev.dimvlachos.lab.magnetdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
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
    fun theScriptFiltersCombinesFansOutAndPutsEverythingBack() = runComposeUiTest {
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
        // Sunset is down, with photos on it.
        mainClock.advanceTimeBy(3_600)
        assertTrue(table!!.results[IslandTags.Sunset].orEmpty().isNotEmpty(), "${table!!.results}")
        // Sea beside it: what matches both hangs between them, on both.
        mainClock.advanceTimeBy(3_000)
        val sunset = table!!.results[IslandTags.Sunset].orEmpty()
        val sea = table!!.results[IslandTags.Sea].orEmpty()
        assertTrue((sunset intersect sea).isNotEmpty(), "sunset $sunset, sea $sea")
        // Sunset tapped: its photos fan out; tapped again, they fold back.
        mainClock.advanceTimeBy(800)
        assertEquals(IslandTags.Sunset, table!!.fannedOut)
        mainClock.advanceTimeBy(2_000)
        assertNull(table!!.fannedOut)
        // Both flicked back by the end: nothing out, everything home, and the table asleep.
        mainClock.advanceTimeBy(demo.script.nominalDuration.inWholeMilliseconds - 9_400 + 100)
        assertTrue(finished, "the script must have played to its end")
        assertTrue(table!!.results.isEmpty(), "${table!!.results}")
        for (body in table!!.bodies.bodies) {
            assertTrue((body.at - body.home).getDistance() < 2f, "${body.id} is not home")
        }
        assertFalse(table!!.awake, "the table must be still by the end")
    }
}
