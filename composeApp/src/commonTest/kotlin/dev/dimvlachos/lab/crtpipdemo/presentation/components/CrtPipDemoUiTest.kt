package dev.dimvlachos.lab.crtpipdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.crtpip.CrtPipState
import dev.dimvlachos.lab.core.presentation.components.crtpip.PipMode
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.crtpipdemo.CrtPipDemos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CrtPipDemoUiTest {
    private fun ComposeUiTest.show(
        pip: CrtPipState,
        state: DemoState = DemoState(),
        play: Boolean = false,
    ) {
        // The show plays on, so the screen is never idle: the clock is stepped by hand.
        mainClock.autoAdvance = false
        setContent {
            LabTheme {
                Box(Modifier.size(412.dp, 860.dp)) { CrtPipDemo(state, pip, reducedMotion = false) }
                if (play) LaunchedEffect(Unit) { CrtPipDemos.all.single().script.play(state) }
            }
        }
    }

    private fun ComposeUiTest.advanceUntil(what: String, most: Long = 6_000, done: () -> Boolean) {
        var waited = 0L
        while (!done()) {
            assertTrue(waited < most, "waited $most ms for $what")
            mainClock.advanceTimeByFrame()
            waited += 16
        }
    }

    @Test
    fun theWatchPageShowsTheVideoAndUpNext() = runComposeUiTest {
        val pip = CrtPipState()
        show(pip)
        mainClock.advanceTimeByFrame()
        onNodeWithText("Cast-iron honey cornbread in 20 minutes").assertExists()
        onNodeWithText("Camp-stove pancakes at Yosemite").assertExists()
        assertEquals(PipMode.Full, pip.mode)
    }

    @Test
    fun aSwipeDownShrinksTheVideoIntoTheSetAndATapBringsItBack() = runComposeUiTest {
        val pip = CrtPipState()
        show(pip)
        mainClock.advanceTimeByFrame()
        onNodeWithTag("crt-player").performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(0f, 60f * density))
            moveBy(androidx.compose.ui.geometry.Offset(0f, 80f * density))
            up()
        }
        advanceUntil("the set") { pip.mode == PipMode.Mini }
        onNodeWithTag("crt-set").assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
        onNodeWithTag("crt-set").performTouchInput {
            down(center)
            up()
        }
        advanceUntil("the full player") { pip.mode == PipMode.Full }
    }

    @Test
    fun stopPlaybackShowsTheToastAndUndoBringsTheSetBack() = runComposeUiTest {
        val pip = CrtPipState()
        show(pip)
        mainClock.advanceTimeByFrame()
        pip.minimize()
        advanceUntil("the set") { pip.mode == PipMode.Mini }
        val actions =
            onNodeWithTag("crt-set").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals("Stop playback", actions.single().label)
        actions.single().action()
        advanceUntil("the toast") { pip.toastOn }
        onNodeWithText("Playback stopped").assertExists()
        onNodeWithTag("crt-undo").performClick()
        advanceUntil("the set again") { pip.mode == PipMode.Mini }
    }

    @Test
    fun theTourEndsOnTheFullPlayerAtTheTop() = runComposeUiTest {
        val pip = CrtPipState()
        show(pip, DemoState(replay = true), play = true)
        advanceUntil("the set", most = 8_000) { pip.mode == PipMode.Mini }
        advanceUntil("the end", most = 16_000) {
            pip.mode == PipMode.Full && pip.corner == "br" && pip.now > 11_000
        }
        assertEquals(PipMode.Full, pip.mode)
    }
}
