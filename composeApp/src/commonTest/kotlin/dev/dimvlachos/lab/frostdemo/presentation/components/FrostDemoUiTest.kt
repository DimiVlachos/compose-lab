package dev.dimvlachos.lab.frostdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.frost.FrostState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class FrostDemoUiTest {
    @Test
    fun scriptedWipesDrawStrokesAndTheLoopRefrosts() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        val frost = FrostState()
        var scope: CoroutineScope? = null
        setContent {
            scope = rememberCoroutineScope()
            // The clip's own 4:5 frame, so the script's points land where they say.
            LabTheme { Box(Modifier.size(400.dp, 500.dp)) { FrostDemo(state, frost) } }
        }
        mainClock.advanceTimeByFrame()
        runOnUiThread {
            state.select(1)
            scope!!.launch {
                state.wipe(listOf(Offset(0.1f, 0.5f), Offset(0.9f, 0.5f)), 400.milliseconds)
            }
        }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(600)

        assertEquals(1, frost.strokes.size)
        val stroke = frost.strokes.single()
        assertTrue(
            (stroke.first() - Offset(0.1f, 0.5f)).getDistance() < 0.001f,
            "${stroke.first()}",
        )
        assertTrue(stroke.last().x > 0.89f, "the finger reaches the end: $stroke")

        runOnUiThread { state.select(0) }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeByFrame()

        assertTrue(frost.strokes.isEmpty())
    }
}
