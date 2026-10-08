package dev.dimvlachos.lab.pullcorddemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.Recreation
import dev.dimvlachos.lab.core.presentation.components.pullcord.LocalPullCordCompositionProbe
import dev.dimvlachos.lab.core.presentation.components.pullcord.PullCordState
import dev.dimvlachos.lab.core.presentation.components.pullcord.rememberPullCordState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.pullcorddemo.PullCordDemos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The bead's switch, as a screen reader reads it.
private const val CordLabel = "Lamp cord"

@OptIn(ExperimentalTestApi::class)
class PullCordDemoUiTest {
    @Test
    fun theScriptSwitchesTheLampOnAndBackOffAndTheCordComesToRest() = runComposeUiTest {
        mainClock.autoAdvance = false
        val demo = PullCordDemos.all.single()
        val state = DemoState()
        var finished = false
        var compositions = 0
        var lamp: PullCordState? = null
        setContent {
            LabTheme {
                CompositionLocalProvider(
                    LocalPullCordCompositionProbe provides { compositions++ }
                ) {
                    Box(Modifier.size(400.dp, 800.dp)) {
                        val lampState = rememberPullCordState()
                        lamp = lampState
                        PullCordDemo(state, lampState)
                    }
                }
                LaunchedEffect(Unit) {
                    demo.script.play(state)
                    finished = true
                }
            }
        }
        val cord = onNodeWithContentDescription(CordLabel)
        cord.assertIsOff()
        // The first pull has switched it on, and the light has spread.
        mainClock.advanceTimeBy(2_600)
        cord.assertIsOn()
        assertFalse(lamp!!.revealing, "the night look must have spread over the whole screen")
        // The tug sideways swings the cord, and nothing recomposes while it does.
        val beforeTug = compositions
        mainClock.advanceTimeBy(3_400)
        cord.assertIsOn()
        assertEquals(beforeTug, compositions, "a swinging cord must not recompose the lamp")
        // Off again by the end, and hanging still, so the clip loops.
        mainClock.advanceTimeBy(demo.script.nominalDuration.inWholeMilliseconds - 6_000 + 100)
        assertTrue(finished)
        cord.assertIsOff()
        assertTrue(lamp!!.rig.atRest, "the cord must hang still by the end")
    }

    @Test
    fun theDarkThemeSwitchOnTheScreenSwitchesTheLampToo() = runComposeUiTest {
        setContent { LabTheme { Box(Modifier.size(400.dp, 800.dp)) { PullCordDemo(DemoState()) } } }
        onNodeWithText("Dark theme").performSemanticsAction(SemanticsActions.OnClick)
        mainClock.advanceTimeBy(1_000)
        onNodeWithContentDescription(CordLabel).assertIsOn()
    }

    @Test
    fun aTapOffTheBeadReachesTheScreen() = runComposeUiTest {
        setContent { LabTheme { Box(Modifier.size(400.dp, 800.dp)) { PullCordDemo(DemoState()) } } }
        onNodeWithText("Dark theme").performClick()
        mainClock.advanceTimeBy(1_000)
        onNodeWithContentDescription(CordLabel).assertIsOn()
    }

    @Test
    fun theBeadHangsClearOfTheRows() = runComposeUiTest {
        setContent { LabTheme { Box(Modifier.size(400.dp, 800.dp)) { PullCordDemo(DemoState()) } } }
        val bead = onNodeWithContentDescription(CordLabel).getUnclippedBoundsInRoot()
        val row = onNodeWithText("Dark theme").getUnclippedBoundsInRoot()
        assertTrue(
            row.top >= bead.bottom,
            "the row starts at ${row.top}, the bead's target ends at ${bead.bottom}",
        )
    }

    @Test
    fun rightToLeftTheLampHangsLeftOfTheMiddle() = runComposeUiTest {
        var lamp: PullCordState? = null
        setContent {
            LabTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Box(Modifier.size(400.dp, 800.dp)) {
                        val lampState = rememberPullCordState()
                        lamp = lampState
                        PullCordDemo(DemoState(), lampState)
                    }
                }
            }
        }
        waitForIdle()
        // The rig works in dp: left of the middle of a 400 dp screen.
        val pivot = lamp!!.rig.pivot
        assertTrue(pivot.x < 200f, "it hangs at $pivot")
    }

    @Test
    fun theProfileCardReadsAsOneWithoutItsInitials() = runComposeUiTest {
        setContent { LabTheme { Box(Modifier.size(400.dp, 800.dp)) { PullCordDemo(DemoState()) } } }
        onNode(hasText("Alex Morgan") and hasText("@alex.morgan.lab")).assertExists()
        onNodeWithText("AM").assertDoesNotExist()
    }

    @Test
    fun theSettingsOutliveTheScreen() = runComposeUiTest {
        val recreation = Recreation(this)
        recreation.setContent {
            LabTheme { Box(Modifier.size(400.dp, 800.dp)) { PullCordDemo(DemoState()) } }
        }
        onNodeWithText("Sounds").assertIsOff().performClick()
        onNodeWithText("Dark theme").performClick()
        mainClock.advanceTimeBy(1_000)
        recreation.saveAndRestore()
        onNodeWithText("Sounds").assertIsOn()
        onNodeWithContentDescription(CordLabel).assertIsOn()
    }

    @Test
    fun aPullStartedAsTheLastOnesFingertipFadesStillPulls() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        var lamp: PullCordState? = null
        setContent {
            LabTheme {
                Box(Modifier.size(400.dp, 800.dp)) {
                    val lampState = rememberPullCordState()
                    lamp = lampState
                    PullCordDemo(state, lampState)
                }
                LaunchedEffect(Unit) {
                    launch { state.pullCord(PullCordDemos.Pull, 500.milliseconds) }
                    // The first finger has let go and is fading out as the second comes down.
                    delay(820)
                    launch { state.pullCord(PullCordDemos.Pull, 500.milliseconds) }
                }
            }
        }
        mainClock.advanceTimeBy(4_000)
        // On with the first pull and off again with the second.
        assertFalse(lamp!!.lit, "the second pull was lost")
    }
}
