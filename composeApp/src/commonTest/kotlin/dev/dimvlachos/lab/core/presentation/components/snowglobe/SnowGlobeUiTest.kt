package dev.dimvlachos.lab.core.presentation.components.snowglobe

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SnowGlobeUiTest {
    @Test
    fun theGlobeIsARefreshButtonAndEnterShakesIt() = runComposeUiTest {
        var shaken = 0
        var settled = 0
        mainClock.autoAdvance = false
        setContent {
            SnowGlobe(
                scene = GlobeScene.Sunny,
                refreshPending = false,
                onShaken = { shaken++ },
                onSettled = { settled++ },
                modifier = Modifier.size(320.dp),
                shakeSensorEnabled = false,
                description = "Refresh forecast",
            )
        }
        onNodeWithTag("snowglobe")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        onNodeWithTag("snowglobe").requestFocus()
        onNodeWithTag("snowglobe").performKeyInput { pressKey(Key.Enter) }
        // The keyboard's shake swings the glass for 1.1 s; its third swing back counts.
        mainClock.advanceTimeBy(1_200)
        assertEquals(1, shaken)
        mainClock.advanceTimeBy(8_000)
        assertEquals(1, settled)
    }

    @Test
    fun clickingTheGlobeShakesIt() = runComposeUiTest {
        var shaken = 0
        mainClock.autoAdvance = false
        setContent {
            SnowGlobe(
                GlobeScene.Sunny,
                false,
                { shaken++ },
                {},
                Modifier.size(320.dp),
                shakeSensorEnabled = false,
            )
        }
        onNodeWithTag("snowglobe").performSemanticsAction(SemanticsActions.OnClick)
        // The keyboard's shake swings the glass for 1.1 s; its third swing back counts.
        mainClock.advanceTimeBy(1_200)
        assertEquals(1, shaken)
    }

    @Test
    fun steadyFramesDoNotRecompose() = runComposeUiTest {
        var compositions = 0
        mainClock.autoAdvance = false
        val state = SnowGlobeState()
        setContent {
            CompositionLocalProvider(LocalSnowGlobeCompositionProbe provides { compositions++ }) {
                SnowGlobe(
                    GlobeScene.Sunny,
                    false,
                    {},
                    {},
                    Modifier.size(320.dp),
                    state = state,
                    shakeSensorEnabled = false,
                )
            }
        }
        mainClock.advanceTimeBy(100)
        val settledCount = compositions
        assertTrue(settledCount > 0, "the probe never reported")
        val frameBefore = state.frame
        state.shake()
        mainClock.advanceTimeBy(2_000)
        assertEquals(settledCount, compositions)
        // The loop really ran while nothing recomposed.
        assertTrue(state.frame > frameBefore + 60, "only ${state.frame - frameBefore} frames")
    }

    @Test
    fun leavingTheScreenStopsTheLoop() = runComposeUiTest {
        var show by mutableStateOf(true)
        var shaken = 0
        val state = SnowGlobeState()
        mainClock.autoAdvance = false
        setContent {
            if (show) {
                SnowGlobe(
                    GlobeScene.Sunny,
                    false,
                    { shaken++ },
                    {},
                    Modifier.size(320.dp),
                    state = state,
                    shakeSensorEnabled = false,
                )
            }
        }
        mainClock.advanceTimeBy(100)
        show = false
        mainClock.advanceTimeBy(100)
        val frame = state.frame
        state.shake()
        mainClock.advanceTimeBy(1_000)
        assertEquals(frame, state.frame)
        assertEquals(0, shaken)
    }

    private class Owner : LifecycleOwner {
        override val lifecycle =
            LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    }

    @Test
    fun aPausedAppStopsTheLoopAndAResumedOneStartsItAgain() = runComposeUiTest {
        val owner = Owner()
        val state = SnowGlobeState()
        mainClock.autoAdvance = false
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                SnowGlobe(
                    GlobeScene.Sunny,
                    false,
                    {},
                    {},
                    Modifier.size(320.dp),
                    state = state,
                    shakeSensorEnabled = false,
                )
            }
        }
        mainClock.advanceTimeBy(200)
        owner.lifecycle.currentState = Lifecycle.State.STARTED
        mainClock.advanceTimeBy(100)
        val paused = state.frame
        mainClock.advanceTimeBy(1_000)
        assertEquals(paused, state.frame, "the loop ran in the background")
        owner.lifecycle.currentState = Lifecycle.State.RESUMED
        mainClock.advanceTimeBy(500)
        assertTrue(state.frame > paused + 10, "the loop didn't come back")
    }

    @Test
    fun reducedMotionReachesTheState() = runComposeUiTest {
        val state = SnowGlobeState()
        mainClock.autoAdvance = false
        setContent {
            SnowGlobe(
                GlobeScene.Sunny,
                false,
                {},
                {},
                Modifier.size(320.dp),
                state = state,
                shakeSensorEnabled = false,
                reducedMotion = true,
            )
        }
        mainClock.advanceTimeBy(100)
        assertEquals(true, state.reducedMotion)
    }

    @Test
    fun theScenesArePaintedOnceHoweverOftenTheGlobeIsShown() = runComposeUiTest {
        val before = SantoriniScenes.paints
        var show by mutableStateOf(true)
        mainClock.autoAdvance = false
        setContent {
            if (show) {
                SnowGlobe(
                    GlobeScene.Sunny,
                    false,
                    {},
                    {},
                    Modifier.size(320.dp),
                    shakeSensorEnabled = false,
                )
            }
        }
        repeat(3) {
            mainClock.advanceTimeBy(500)
            show = !show
        }
        assertTrue(
            SantoriniScenes.paints - before <= 1,
            "painted ${SantoriniScenes.paints - before} times",
        )
    }
}
