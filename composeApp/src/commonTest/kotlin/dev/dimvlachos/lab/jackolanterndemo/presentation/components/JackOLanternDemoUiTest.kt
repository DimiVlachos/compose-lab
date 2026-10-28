package dev.dimvlachos.lab.jackolanterndemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.jackolantern.JackOLanternState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.jackolanterndemo.JackOLanternDemos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class JackOLanternDemoUiTest {
    private val label =
        "Night theme. A jack-o’-lantern: light the candle for the night theme, blow it out for day."

    private fun ComposeUiTest.show(lantern: JackOLanternState) {
        setContent {
            LabTheme(darkTheme = false) {
                Box(Modifier.size(412.dp, 800.dp)) {
                    JackOLanternDemo(DemoState(), lantern = lantern, reducedMotion = false)
                }
            }
        }
    }

    /**
     * The demo as the app shows it: its own lantern, in [dark] or not, its script played if [play].
     */
    private fun ComposeUiTest.showAsApp(dark: Boolean, state: DemoState, play: Boolean = false) {
        // Lit, the candle flickers every frame and the screen is never idle.
        mainClock.autoAdvance = false
        setContent {
            LabTheme(darkTheme = dark) {
                Box(Modifier.size(412.dp, 800.dp)) {
                    JackOLanternDemo(state, reducedMotion = false)
                }
                if (play) LaunchedEffect(Unit) { JackOLanternDemos.all.single().script.play(state) }
            }
        }
    }

    private fun ComposeUiTest.isOn() =
        onNodeWithContentDescription(label)
            .fetchSemanticsNode()
            .config[SemanticsProperties.ToggleableState] == ToggleableState.On

    @Test
    fun theFarmShowsTonightWithThePumpkinOut() = runComposeUiTest {
        val lantern = JackOLanternState()
        show(lantern)
        onNodeWithText("Tonight at the farm").assertExists()
        onNodeWithText("Haunted Hayride").assertExists()
        onNodeWithText("Bonfire and s’mores").assertExists()
        onNodeWithContentDescription(label).assertIsOff()
        assertTrue(lantern.laidOut)
        // The pumpkin sits in the top bar's right-hand slot.
        assertTrue(lantern.px > 300f && lantern.py < 60f, "${lantern.px}, ${lantern.py}")
    }

    // Lit, the candle flickers on every frame and the screen is never idle: the clock is moved
    // by hand.
    private fun ComposeUiTest.run(ms: Int) {
        repeat(ms / 16) { mainClock.advanceTimeByFrame() }
    }

    @Test
    fun theSwitchsClickActionAndItsKeysLightAndBlowItOut() = runComposeUiTest {
        val lantern = JackOLanternState()
        show(lantern)
        mainClock.autoAdvance = false
        val switch = onNodeWithContentDescription(label)
        switch.performSemanticsAction(SemanticsActions.OnClick)
        run(200)
        assertTrue(lantern.lit)
        switch.requestFocus()
        switch.performKeyInput { pressKey(Key.Spacebar) }
        run(200)
        assertFalse(lantern.lit)
        switch.performKeyInput { pressKey(Key.Enter) }
        run(200)
        assertTrue(lantern.lit)
    }

    @Test
    fun inTheDarkThemeItOpensLitButARecordingStartsOut() = runComposeUiTest {
        var recording by mutableStateOf(false)
        mainClock.autoAdvance = false
        setContent {
            LabTheme(darkTheme = true) {
                Box(Modifier.size(412.dp, 800.dp)) {
                    key(recording) {
                        JackOLanternDemo(DemoState(recording = recording), reducedMotion = false)
                    }
                }
            }
        }
        mainClock.autoAdvance = false
        run(100)
        onNodeWithContentDescription(label).assertIsOn()
        recording = true
        run(100)
        onNodeWithContentDescription(label).assertIsOff()
    }

    @Test
    fun aReplayInTheDarkThemeStartsLitBlowsOutFirstAndEndsLit() = runComposeUiTest {
        showAsApp(dark = true, DemoState(replay = true), play = true)
        mainClock.autoAdvance = false
        run(200)
        assertTrue(isOn(), "the replay opens by night, as the screen was")
        // The first move flicks it out.
        run(4000)
        assertFalse(isOn())
        // Then a tap lights it again, and the change of mind ends lit too.
        var sawOnAfter = false
        repeat(22_000 / 16) {
            mainClock.advanceTimeByFrame()
            if (isOn()) sawOnAfter = true
        }
        assertTrue(sawOnAfter)
        assertTrue(isOn(), "it ends lit, where it began")
    }

    @Test
    fun onAShortStageThePageScrollsThePumpkinAndItsHintOffTheTop() = runComposeUiTest {
        val lantern = JackOLanternState()
        setContent {
            LabTheme(darkTheme = false) {
                Box(Modifier.size(800.dp, 220.dp)) {
                    JackOLanternDemo(DemoState(), lantern = lantern, reducedMotion = false)
                }
            }
        }
        mainClock.autoAdvance = false
        // A quiet while brings the hint up under the pumpkin.
        run(4200)
        assertTrue(lantern.hintOn)
        onRoot().performTouchInput { swipeUp(startY = centerY, endY = top + 2f) }
        run(1000)
        assertTrue(lantern.py < 0f, "scrolled up with the page: ${lantern.py}")
        onNodeWithText("Tonight").assertExists()
    }

    @Test
    fun theSwitchLightsTheCandleAndTheNightSpreads() = runComposeUiTest {
        val lantern = JackOLanternState()
        show(lantern)
        mainClock.autoAdvance = false
        onNodeWithContentDescription(label).performClick()
        run(4000)
        onNodeWithContentDescription(label).assertIsOn()
        assertEquals(1f, lantern.spread())
        onNodeWithContentDescription("Night theme. The jack-o’-lantern is lit.").assertExists()
    }

    @Test
    fun aTapOnThePumpkinLightsItAndAFlickAcrossBlowsItOut() = runComposeUiTest {
        val lantern = JackOLanternState()
        show(lantern)
        mainClock.autoAdvance = false
        val d = density.density
        val at = Offset(lantern.px * d, lantern.py * d)
        onRoot().performTouchInput { click(at) }
        run(4000)
        assertTrue(lantern.lit)
        onRoot().performTouchInput {
            swipe(
                Offset((lantern.px - 120f) * d, (lantern.py + 40f) * d),
                Offset((lantern.px + 80f) * d, (lantern.py - 10f) * d),
                durationMillis = 120,
            )
        }
        assertFalse(lantern.lit)
        run(4000)
        assertEquals(0f, lantern.spread())
        onNodeWithContentDescription(label).assertIsOff()
    }
}
