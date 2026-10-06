package dev.dimvlachos.lab.pullcorddemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.pullcord.LocalPullCordCompositionProbe
import dev.dimvlachos.lab.core.presentation.components.pullcord.PullCordState
import dev.dimvlachos.lab.core.presentation.components.pullcord.rememberPullCordState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.pullcorddemo.PullCordDemos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        val cord = onNodeWithContentDescription("Lamp cord")
        cord.assertIsOff()
        // The first pull has switched it on, and the light has spread.
        mainClock.advanceTimeBy(2_600)
        cord.assertIsOn()
        // The tug sideways swings the cord, and nothing recomposes while it does.
        val beforeTug = compositions
        mainClock.advanceTimeBy(3_400)
        cord.assertIsOn()
        assertEquals(beforeTug, compositions, "a swinging cord must not recompose the demo")
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
        onNodeWithContentDescription("Lamp cord").assertIsOn()
    }
}
