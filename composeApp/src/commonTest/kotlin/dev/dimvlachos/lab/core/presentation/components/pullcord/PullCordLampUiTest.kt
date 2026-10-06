package dev.dimvlachos.lab.core.presentation.components.pullcord

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PullCordLampUiTest {
    private class Ticks : HapticFeedback {
        val types = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            types += hapticFeedbackType
        }
    }

    private class Lamp(val state: PullCordState, val ticks: Ticks, val compositions: () -> Int)

    // A lamp in the middle of a 300 × 600 dp screen that is white by day and black lit.
    private fun ComposeUiTest.lamp(): Lamp {
        val ticks = Ticks()
        var state: PullCordState? = null
        var compositions = 0
        setContent {
            CompositionLocalProvider(
                LocalHapticFeedback provides ticks,
                LocalPullCordCompositionProbe provides { compositions++ },
            ) {
                val lamp = rememberPullCordState()
                state = lamp
                PullCordLamp(lamp, Modifier.size(300.dp, 600.dp).testTag("lamp")) { lit ->
                    Box(Modifier.fillMaxSize().background(if (lit) Color.Black else Color.White))
                }
            }
        }
        waitForIdle()
        return Lamp(state!!, ticks, { compositions })
    }

    // A finger on the bead, moved by ([across], [down]) in eight even moves, and lifted.
    private fun ComposeUiTest.pull(down: Dp, across: Dp = 0.dp) {
        onNodeWithContentDescription("Lamp cord").performTouchInput {
            down(center)
            repeat(8) { moveBy(Offset(across.toPx() / 8f, down.toPx() / 8f)) }
            up()
        }
    }

    // How light the screen is at ([x], [y]) dp: 0 black to 1 white.
    private fun ComposeUiTest.lightAt(x: Dp, y: Dp): Float {
        val pixels = onNodeWithTag("lamp").captureToImage().toPixelMap()
        val px = (x.value / 300f * pixels.width).toInt().coerceIn(0, pixels.width - 1)
        val py = (y.value / 600f * pixels.height).toInt().coerceIn(0, pixels.height - 1)
        return pixels[px, py].red
    }

    @Test
    fun aPullPastTheClickSwitchesTheLampOnceWithATick() = runComposeUiTest {
        val lamp = lamp()
        pull(down = 80.dp)
        assertTrue(lamp.state.lit)
        assertEquals(listOf(HapticFeedbackType.VirtualKey), lamp.ticks.types)
        onNodeWithContentDescription("Lamp cord").assertIsOn()
        mainClock.advanceTimeBy(6_000)
        pull(down = 80.dp)
        assertFalse(lamp.state.lit)
        assertEquals(
            listOf(HapticFeedbackType.VirtualKey, HapticFeedbackType.VirtualKey),
            lamp.ticks.types,
        )
    }

    @Test
    fun aShortPullClicksNothing() = runComposeUiTest {
        val lamp = lamp()
        pull(down = 30.dp)
        assertFalse(lamp.state.lit)
        assertTrue(lamp.ticks.types.isEmpty())
    }

    @Test
    fun aSidewaysTugSwingsTheCordWithoutRecomposingOrSwitching() = runComposeUiTest {
        mainClock.autoAdvance = false
        val lamp = lamp()
        val before = lamp.compositions()
        val rest = lamp.state.bead
        pull(down = 4.dp, across = 90.dp)
        var furthest = 0f
        repeat(90) {
            mainClock.advanceTimeByFrame()
            furthest = maxOf(furthest, abs(lamp.state.bead.x - rest.x))
        }
        assertFalse(lamp.state.lit)
        assertTrue(lamp.ticks.types.isEmpty())
        assertTrue(furthest > 10f, "it swung only $furthest px")
        assertEquals(before, lamp.compositions(), "a swinging cord must not recompose")
    }

    @Test
    fun letGoTheCordComesToRestAndTheFramesStop() = runComposeUiTest {
        mainClock.autoAdvance = false
        val lamp = lamp()
        val rest = lamp.state.bead
        pull(down = 80.dp)
        mainClock.advanceTimeBy(500)
        assertTrue(lamp.state.awake)
        mainClock.advanceTimeBy(9_000)
        assertFalse(lamp.state.awake)
        assertTrue(lamp.state.rig.atRest)
        assertEquals(rest.x, lamp.state.bead.x, 2f)
        assertEquals(rest.y, lamp.state.bead.y, 4f)
    }

    @Test
    fun theNewLookSpreadsFromTheBulbOverTheOld() = runComposeUiTest {
        mainClock.autoAdvance = false
        val lamp = lamp()
        runOnUiThread { lamp.state.toggle() }
        mainClock.advanceTimeBy(PullCordDimens.RevealMs / 3L)
        // Under the bulb it is already night; in the far corner still day.
        assertTrue(lightAt(150.dp, 150.dp) < 0.5f, "under the bulb")
        assertTrue(lightAt(4.dp, 596.dp) > 0.9f, "the far corner")
        mainClock.advanceTimeBy(PullCordDimens.RevealMs.toLong())
        assertTrue(lightAt(4.dp, 596.dp) < 0.3f, "the far corner, after")
        assertFalse(lamp.state.revealing)
    }

    @Test
    fun litTheLampThrowsLightBelowItAndNotAboveIt() = runComposeUiTest {
        mainClock.autoAdvance = false
        val lamp = lamp()
        runOnUiThread { lamp.state.toggle() }
        mainClock.advanceTimeBy(1_000)
        val below = lightAt(150.dp, 240.dp)
        val aside = lightAt(8.dp, 60.dp)
        assertTrue(below > aside + 0.08f, "below $below, beside $aside")
    }

    @Test
    fun switchedOnTheBulbFlickersTwiceThenStaysOn() = runComposeUiTest {
        mainClock.autoAdvance = false
        val lamp = lamp()
        runOnUiThread { lamp.state.toggle() }
        val samples = mutableListOf<Float>()
        repeat(40) {
            mainClock.advanceTimeBy(10)
            samples += lamp.state.brightness.value
        }
        var dips = 0
        for (i in 1 until samples.size - 1) {
            val low = samples[i] < samples[i - 1] && samples[i] <= samples[i + 1]
            if (low && samples[i] < 0.6f && i > 2) dips++
        }
        assertEquals(2, dips, "brightness $samples")
        assertEquals(1f, samples.last())
    }

    @Test
    fun theLightSpreadsWithoutRecomposingBetweenItsStartAndEnd() = runComposeUiTest {
        mainClock.autoAdvance = false
        val lamp = lamp()
        runOnUiThread { lamp.state.toggle() }
        mainClock.advanceTimeByFrame()
        val started = lamp.compositions()
        repeat((PullCordDimens.RevealMs - 80) / 16) { mainClock.advanceTimeByFrame() }
        assertEquals(started, lamp.compositions(), "a spreading light must not recompose")
        mainClock.advanceTimeBy(400)
        assertTrue(lamp.compositions() > started, "the old look is let go once covered")
    }

    @Test
    fun aScreenReaderCanSwitchTheLamp() = runComposeUiTest {
        val lamp = lamp()
        onNodeWithContentDescription("Lamp cord").assertIsOff()
        onNodeWithContentDescription("Lamp cord").performSemanticsAction(SemanticsActions.OnClick)
        assertTrue(lamp.state.lit)
        onNodeWithContentDescription("Lamp cord").assertIsOn()
    }
}
