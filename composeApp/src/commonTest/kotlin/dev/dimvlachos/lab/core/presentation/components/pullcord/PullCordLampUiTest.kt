package dev.dimvlachos.lab.core.presentation.components.pullcord

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.components.Recreation
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// The bead's switch, as a screen reader reads it.
private const val CordLabel = "Lamp cord"

// Long enough after a pull for the cord and the shade to have all but settled, so a second pull
// takes the bead where it hangs; and long enough for them to settle completely and the frame loop
// to sleep.
private const val SettleMs = 6_000L
private const val RestMs = 9_000L

@OptIn(ExperimentalTestApi::class)
class PullCordLampUiTest {
    private class Ticks : HapticFeedback {
        val types = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            types += hapticFeedbackType
        }
    }

    private class Lamp(val state: PullCordState, val ticks: Ticks, val compositions: () -> Int)

    // A lamp in the middle of a 300 × 600 dp screen that is white by day and black lit, or shows
    // [content].
    private fun ComposeUiTest.lamp(
        ticks: Ticks = Ticks(),
        onSwitch: (Boolean) -> Unit = {},
        content: @Composable (Boolean) -> Unit = { lit ->
            Box(Modifier.fillMaxSize().background(if (lit) Color.Black else Color.White))
        },
    ): Lamp {
        var state: PullCordState? = null
        var compositions = 0
        setContent {
            CompositionLocalProvider(
                LocalHapticFeedback provides ticks,
                LocalPullCordCompositionProbe provides { compositions++ },
            ) {
                val lamp = rememberPullCordState()
                state = lamp
                PullCordLamp(
                    lamp,
                    Modifier.size(300.dp, 600.dp).testTag("lamp"),
                    onSwitch = onSwitch,
                    content = content,
                )
            }
        }
        waitForIdle()
        return Lamp(state!!, ticks, { compositions })
    }

    // A finger on the bead, moved by ([across], [down]) in eight even moves, and lifted.
    private fun ComposeUiTest.pull(down: Dp, across: Dp = 0.dp) {
        onNodeWithContentDescription(CordLabel).performTouchInput {
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
        onNodeWithContentDescription(CordLabel).assertIsOn()
        mainClock.advanceTimeBy(SettleMs)
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
        mainClock.advanceTimeBy(RestMs)
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
    fun switchedBackMidSpreadTheCircleShrinksAndNothingElseChanges() = runComposeUiTest {
        mainClock.autoAdvance = false
        val lamp = lamp()
        runOnUiThread { lamp.state.toggle() }
        mainClock.advanceTimeBy(PullCordDimens.RevealMs / 2L)
        assertTrue(lightAt(4.dp, 596.dp) > 0.9f, "the far corner, still day")
        runOnUiThread { lamp.state.toggle() }
        mainClock.advanceTimeByFrame()
        // The day outside the circle stays day: it never flashes over to night.
        assertTrue(lightAt(4.dp, 596.dp) > 0.9f, "the far corner, switched back")
        mainClock.advanceTimeBy(PullCordDimens.RevealMs.toLong())
        assertFalse(lamp.state.lit)
        assertFalse(lamp.state.revealing)
        assertTrue(lightAt(150.dp, 150.dp) > 0.9f, "under the bulb, back to day")
        assertTrue(lightAt(4.dp, 596.dp) > 0.9f, "the far corner, after")
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
        val start = mainClock.currentTime
        runOnUiThread { lamp.state.toggle() }
        val samples = mutableListOf<Pair<Long, Float>>()
        repeat(40) {
            mainClock.advanceTimeBy(10)
            samples += (mainClock.currentTime - start) to lamp.state.brightness.value
        }
        val values = samples.map { it.second }
        var dips = 0
        for (i in 1 until values.size - 1) {
            val low = values[i] < values[i - 1] && values[i] <= values[i + 1]
            if (low && values[i] < 0.6f && i > 2) dips++
        }
        assertEquals(2, dips, "brightness $samples")
        // Each dip as deep as it is meant to be, give or take where the frames fall.
        fun lowest(from: Int, to: Int) = samples.filter { it.first in from..to }.minOf { it.second }
        val first = lowest(PullCordDimens.FlickerFullMs, PullCordDimens.FlickerBackMs)
        val second = lowest(PullCordDimens.FlickerBackMs, PullCordDimens.FlickerMs)
        assertTrue(
            first in PullCordDimens.FirstDip - 0.01f..PullCordDimens.FirstDip + 0.2f,
            "$first",
        )
        assertTrue(
            second in PullCordDimens.SecondDip - 0.01f..PullCordDimens.SecondDip + 0.2f,
            "$second",
        )
        assertEquals(1f, values.last())
    }

    @Test
    fun switchedBackOnAsItGoesOutItBrightensFromWhereItIs() = runComposeUiTest {
        mainClock.autoAdvance = false
        val lamp = lamp()
        runOnUiThread { lamp.state.toggle() }
        mainClock.advanceTimeBy(1_000)
        runOnUiThread { lamp.state.toggle() }
        mainClock.advanceTimeBy(PullCordDimens.OffMs / 3L)
        val fading = lamp.state.brightness.value
        assertTrue(fading in 0.2f..0.9f, "going out, it is at $fading")
        runOnUiThread { lamp.state.toggle() }
        // Up from there to full, not down to dark first; the dips come after.
        repeat(2) {
            mainClock.advanceTimeByFrame()
            val now = lamp.state.brightness.value
            assertTrue(now >= fading - 0.02f, "it dropped from $fading to $now")
        }
        mainClock.advanceTimeBy(PullCordDimens.FlickerMs.toLong())
        assertEquals(1f, lamp.state.brightness.value)
    }

    @Test
    fun switchedOffMidFlickerTheBulbGoesOut() = runComposeUiTest {
        mainClock.autoAdvance = false
        val lamp = lamp()
        runOnUiThread { lamp.state.toggle() }
        mainClock.advanceTimeBy(PullCordDimens.FirstDipMs.toLong())
        runOnUiThread { lamp.state.toggle() }
        mainClock.advanceTimeBy(PullCordDimens.RevealMs + 100L)
        assertFalse(lamp.state.lit)
        assertFalse(lamp.state.revealing)
        assertEquals(0f, lamp.state.brightness.value)
        assertTrue(lightAt(150.dp, 240.dp) > 0.95f, "the light is still on the day look")
    }

    @Test
    fun goingOutTheLightFadesWithTheLitLook() = runComposeUiTest {
        mainClock.autoAdvance = false
        val lamp = lamp()
        runOnUiThread { lamp.state.toggle() }
        mainClock.advanceTimeBy(1_000)
        runOnUiThread { lamp.state.toggle() }
        // The bulb is out, but the light still falls on what is left of the lit look.
        mainClock.advanceTimeBy(PullCordDimens.RevealMs / 3L)
        assertEquals(0f, lamp.state.brightness.value)
        assertTrue(lamp.state.glow > 0.2f, "the light has faded to ${lamp.state.glow}")
        mainClock.advanceTimeBy(PullCordDimens.RevealMs.toLong())
        assertFalse(lamp.state.revealing)
        assertEquals(0f, lamp.state.glow)
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
        onNodeWithContentDescription(CordLabel).assertIsOff()
        onNodeWithContentDescription(CordLabel).performSemanticsAction(SemanticsActions.OnClick)
        assertTrue(lamp.state.lit)
        onNodeWithContentDescription(CordLabel).assertIsOn()
    }

    @Test
    fun hungFurtherAcrossTheLampMovesWithItsCord() = runComposeUiTest {
        var across by mutableStateOf(0.5f)
        var state: PullCordState? = null
        setContent {
            val lamp = rememberPullCordState()
            state = lamp
            PullCordLamp(lamp, Modifier.size(300.dp, 600.dp), across = across) { Box(Modifier) }
        }
        waitForIdle()
        val lamp = state!!
        val before = lamp.bead
        across = 0.8f
        waitForIdle()
        val moved = 300.dp.value * 0.3f * density.density
        assertEquals(before.x + moved, lamp.bead.x, 1f)
        assertTrue(lamp.rig.atRest)
    }

    @Test
    fun onSwitchHearsEachSwitchAfterItsTick() = runComposeUiTest {
        val ticks = Ticks()
        lateinit var state: () -> PullCordState
        // Whether it is lit, the ticks played so far, and whether the new look has begun spreading.
        val heard = mutableListOf<Triple<Boolean, Int, Boolean>>()
        val lamp =
            lamp(
                ticks,
                onSwitch = { lit -> heard += Triple(lit, ticks.types.size, state().revealing) },
            )
        state = { lamp.state }
        pull(down = 80.dp)
        mainClock.advanceTimeBy(SettleMs)
        pull(down = 80.dp)
        assertEquals(listOf(Triple(true, 1, true), Triple(false, 2, true)), heard)
    }

    @Test
    fun snapToSetsTheLampWithoutASwitch() = runComposeUiTest {
        mainClock.autoAdvance = false
        val heard = mutableListOf<Boolean>()
        val lamp = lamp(onSwitch = { heard += it })
        runOnUiThread { lamp.state.snapTo(true) }
        mainClock.advanceTimeByFrame()
        assertTrue(lamp.state.lit)
        assertFalse(lamp.state.revealing)
        assertEquals(1f, lamp.state.brightness.value)
        assertTrue(lightAt(4.dp, 596.dp) < 0.3f, "the far corner is lit at once")
        // Mid-spread too: the spread stops where it is and the look it is set to shows alone.
        runOnUiThread { lamp.state.toggle() }
        mainClock.advanceTimeBy(PullCordDimens.RevealMs / 3L)
        runOnUiThread { lamp.state.snapTo(true) }
        mainClock.advanceTimeByFrame()
        assertTrue(lamp.state.lit)
        assertFalse(lamp.state.revealing)
        assertTrue(lightAt(150.dp, 150.dp) < 0.5f, "under the bulb")
        assertTrue(lightAt(4.dp, 596.dp) < 0.3f, "the far corner")
        onNodeWithContentDescription(CordLabel).assertIsOn()
        // Only the toggle in between was a switch.
        assertEquals(listOf(false), heard)
        assertEquals(1, lamp.ticks.types.size)
    }

    @Test
    fun aThirdSwitchMidShrinkSpreadsOnAgain() = runComposeUiTest {
        mainClock.autoAdvance = false
        val lamp = lamp()
        runOnUiThread { lamp.state.toggle() }
        mainClock.advanceTimeBy(PullCordDimens.RevealMs / 2L)
        runOnUiThread { lamp.state.toggle() }
        mainClock.advanceTimeBy(PullCordDimens.RevealMs / 8L)
        runOnUiThread { lamp.state.toggle() }
        mainClock.advanceTimeByFrame()
        // On again: the circle spreads on from where it had shrunk to, and nothing flashes.
        assertTrue(lightAt(150.dp, 150.dp) < 0.5f, "under the bulb")
        assertTrue(lightAt(4.dp, 596.dp) > 0.9f, "the far corner, still day")
        mainClock.advanceTimeBy(PullCordDimens.RevealMs * 2L)
        assertTrue(lamp.state.lit)
        assertFalse(lamp.state.revealing)
        assertTrue(lightAt(4.dp, 596.dp) < 0.3f, "the far corner, after")
    }

    @Test
    fun aTapMidShrinkReachesTheLiveScreen() = runComposeUiTest {
        mainClock.autoAdvance = false
        val taps = mutableListOf<Boolean>()
        val lamp = lamp { lit ->
            Box(
                Modifier.fillMaxSize().background(if (lit) Color.Black else Color.White).clickable {
                    taps += lit
                }
            )
        }
        runOnUiThread { lamp.state.toggle() }
        mainClock.advanceTimeBy(PullCordDimens.RevealMs / 2L)
        // Switched back: the lit look shrinks into the bulb over the day look, which is live.
        runOnUiThread { lamp.state.toggle() }
        mainClock.advanceTimeByFrame()
        assertTrue(lightAt(150.dp, 160.dp) < 0.5f, "inside the shrinking circle")
        onNodeWithTag("lamp").performTouchInput { click(Offset(150.dp.toPx(), 160.dp.toPx())) }
        mainClock.advanceTimeByFrame()
        assertEquals(listOf(false), taps)
    }

    @Test
    fun aThumbRestingOnTheScreenDoesNotStopAPull() = runComposeUiTest {
        val lamp = lamp()
        val bead = lamp.state.bead
        onNodeWithTag("lamp").performTouchInput {
            down(1, Offset(20.dp.toPx(), 580.dp.toPx()))
            down(0, bead)
            repeat(8) { moveBy(0, Offset(0f, 10.dp.toPx())) }
            up(0)
            up(1)
        }
        assertTrue(lamp.state.lit)
    }

    @Test
    fun aCancelledPullLetsGoOfTheBead() = runComposeUiTest {
        // The clock moved by hand: a held bead keeps the frames coming, so the screen is never
        // idle.
        mainClock.autoAdvance = false
        var shown by mutableStateOf(true)
        var state: PullCordState? = null
        setContent {
            val lamp = rememberPullCordState()
            state = lamp
            if (shown) {
                PullCordLamp(lamp, Modifier.size(300.dp, 600.dp)) { lit ->
                    Box(Modifier.fillMaxSize().background(if (lit) Color.Black else Color.White))
                }
            }
        }
        waitForIdle()
        // A finger on the bead, and the lamp taken away under it mid-pull: the pull is cancelled.
        onNodeWithContentDescription(CordLabel).performTouchInput {
            down(center)
            moveBy(Offset(0f, 20.dp.toPx()))
        }
        val lamp = state!!
        assertTrue(lamp.rig.held)
        shown = false
        mainClock.advanceTimeByFrame()
        assertFalse(lamp.rig.held)
        assertFalse(lamp.lit)
    }

    @Test
    fun theFrameLoopWakesAgainAfterSleeping() = runComposeUiTest {
        mainClock.autoAdvance = false
        val lamp = lamp()
        pull(down = 4.dp, across = 60.dp)
        mainClock.advanceTimeBy(RestMs)
        assertFalse(lamp.state.awake, "asleep once the cord hangs still")
        val rest = lamp.state.bead
        pull(down = 4.dp, across = 90.dp)
        var furthest = 0f
        repeat(60) {
            mainClock.advanceTimeByFrame()
            furthest = maxOf(furthest, abs(lamp.state.bead.x - rest.x))
        }
        assertTrue(furthest > 10f, "woken, it swung only $furthest px")
    }

    @Test
    fun atAnotherDensityTheClickIsStill48Dp() = runComposeUiTest {
        val scale = 3f
        var state: PullCordState? = null
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(scale)) {
                val lamp = rememberPullCordState()
                state = lamp
                PullCordLamp(lamp, Modifier.size(150.dp, 250.dp).testTag("lamp")) { lit ->
                    Box(Modifier.fillMaxSize().background(if (lit) Color.Black else Color.White))
                }
            }
        }
        waitForIdle()
        val lamp = state!!
        // Taken a little above the bead, so the whole pull stays on the test's screen.
        fun pullBy(dp: Float) {
            val from = lamp.bead - Offset(0f, 16f * scale)
            onNodeWithTag("lamp").performTouchInput {
                down(from)
                repeat(8) { moveBy(Offset(0f, dp * scale / 8f)) }
                up()
            }
        }
        pullBy(PullCordDimens.ClickPull.value - 4f)
        assertFalse(lamp.lit, "short of 48 dp")
        mainClock.advanceTimeBy(SettleMs)
        pullBy(PullCordDimens.ClickPull.value + 4f)
        assertTrue(lamp.lit, "past 48 dp")
    }

    @Test
    fun theCordsSwitchStaysPutAsTheCordSwingsAndSaysWhetherTheLightIsOn() = runComposeUiTest {
        mainClock.autoAdvance = false
        val lamp = lamp()
        val cord = onNodeWithContentDescription(CordLabel)
        cord.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Light off"))
        val rest = cord.getUnclippedBoundsInRoot()
        pull(down = 4.dp, across = 90.dp)
        repeat(20) {
            mainClock.advanceTimeByFrame()
            assertEquals(rest, cord.getUnclippedBoundsInRoot())
        }
        runOnUiThread { lamp.state.toggle() }
        mainClock.advanceTimeByFrame()
        cord.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Light on"))
    }

    @Test
    fun aLampLeavingLeavesTheOtherLampsHook() = runComposeUiTest {
        var first by mutableStateOf(true)
        val heard = mutableListOf<String>()
        var state: PullCordState? = null
        setContent {
            val lamp = rememberPullCordState()
            state = lamp
            Box {
                if (first) {
                    PullCordLamp(lamp, Modifier.size(100.dp), onSwitch = { heard += "first" }) {
                        Box(Modifier)
                    }
                }
                PullCordLamp(lamp, Modifier.size(100.dp), onSwitch = { heard += "second" }) {
                    Box(Modifier)
                }
            }
        }
        waitForIdle()
        first = false
        waitForIdle()
        runOnUiThread { state!!.toggle() }
        assertEquals(listOf("second"), heard)
    }

    @Test
    fun aStrayAcrossIsKeptToTheStage() = runComposeUiTest {
        var across by mutableStateOf(0.5f)
        var state: PullCordState? = null
        setContent {
            val lamp = rememberPullCordState()
            state = lamp
            PullCordLamp(lamp, Modifier.size(300.dp, 600.dp), across = across) { Box(Modifier) }
        }
        waitForIdle()
        val lamp = state!!
        val before = lamp.bead
        across = Float.NaN
        waitForIdle()
        assertEquals(before, lamp.bead, "no number at all leaves it where it was")
        across = 1.5f
        waitForIdle()
        val toTheEdge = 300.dp.value * 0.5f * density.density
        assertEquals(before.x + toTheEdge, lamp.bead.x, 1f)
    }

    @Test
    fun whetherItIsLitOutlivesTheScreen() = runComposeUiTest {
        val recreation = Recreation(this)
        var state: PullCordState? = null
        recreation.setContent {
            val lamp = rememberPullCordState()
            state = lamp
            PullCordLamp(lamp, Modifier.size(300.dp, 600.dp)) { Box(Modifier) }
        }
        waitForIdle()
        val before = state
        runOnUiThread { state!!.toggle() }
        waitForIdle()
        recreation.saveAndRestore()
        val after = state!!
        assertTrue(after !== before, "the screen was made again")
        assertTrue(after.lit)
        assertFalse(after.revealing, "put back lit, not switched on again")
        onNodeWithContentDescription(CordLabel).assertIsOn()
    }
}
