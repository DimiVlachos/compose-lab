package dev.dimvlachos.lab.birdcagedemo.presentation.components

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeDown
import dev.dimvlachos.lab.birdcagedemo.BirdcageDemos
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.birdcage.BirdcageState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class BirdcageDemoUiTest {
    @Test
    fun aTapOnTheCageMutesAndATapAgainUnmutes() = runComposeUiTest {
        // The canary breathes on every frame, so the clock is moved by hand.
        mainClock.autoAdvance = false
        val cage = BirdcageState()
        setContent { BirdcageDemo(DemoState(), cage) }
        onNodeWithTag("birdcage").assertIsOff()
        onNodeWithTag("state").assertTextEquals("Notifications on")
        onNodeWithTag("birdcage").performClick()
        mainClock.advanceTimeBy(400)
        onNodeWithTag("birdcage").assertIsOn()
        onNodeWithTag("state").assertTextEquals("Muted")
        onNodeWithTag("tip").assertTextEquals("Lift the cover to hear new messages")
        assertTrue(cage.muted)
        onNodeWithTag("birdcage").performClick()
        mainClock.advanceTimeBy(100)
        assertFalse(cage.muted)
    }

    @Test
    fun theStateLinesCrossfadeInPlace() = runComposeUiTest {
        // The canary breathes on every frame, so the clock is moved by hand.
        mainClock.autoAdvance = false
        val cage = BirdcageState()
        setContent { BirdcageDemo(DemoState(), cage) }
        onNodeWithTag("birdcage").performClick()
        // Partway through, the old line is still fading out under the new one.
        mainClock.advanceTimeBy(StateFadeMs / 2L)
        onAllNodesWithTag("state").assertCountEquals(2)
        onAllNodesWithTag("tip").assertCountEquals(2)
        mainClock.advanceTimeBy(StateFadeMs + 100L)
        onAllNodesWithTag("state").assertCountEquals(1)
        onNodeWithTag("state").assertTextEquals("Muted")
    }

    @Test
    fun theSwitchIsAScreenReadersToo() = runComposeUiTest {
        // The canary breathes on every frame, so the clock is moved by hand.
        mainClock.autoAdvance = false
        val cage = BirdcageState()
        setContent { BirdcageDemo(DemoState(), cage) }
        onNodeWithTag("birdcage").performSemanticsAction(SemanticsActions.OnClick)
        assertTrue(cage.muted)
    }

    @Test
    fun pullingTheCoverDownMutes() = runComposeUiTest {
        // The canary breathes on every frame, so the clock is moved by hand.
        mainClock.autoAdvance = false
        val cage = BirdcageState()
        setContent { BirdcageDemo(DemoState(), cage) }
        onNodeWithTag("birdcage").performTouchInput { swipeDown(top + 20f, bottom, 300) }
        mainClock.advanceTimeBy(1_500)
        assertTrue(cage.muted)
        onNodeWithTag("state").assertTextEquals("Muted")
    }

    @Test
    fun theScriptSingsMutesCountsAndResets() = runComposeUiTest {
        // The canary breathes on every frame, so the clock is moved by hand.
        mainClock.autoAdvance = false
        val state = DemoState(replay = true)
        val cage = BirdcageState()
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            BirdcageDemo(state, cage)
        }
        mainClock.advanceTimeBy(100)
        scope.launch { state.tapControl(BirdcageDemos.Message) }
        mainClock.advanceTimeBy(600)
        assertEquals(1, cage.chirps)
        onNodeWithTag("latest").assertTextEquals("Jordan: Coffee after at Bluebonnet Bakery?")
        scope.launch { state.tapControl(BirdcageDemos.CoverDown) }
        mainClock.advanceTimeBy(3_000)
        assertTrue(cage.muted, "cover ${cage.cover}")
        scope.launch { state.tapControl(BirdcageDemos.Message) }
        mainClock.advanceTimeBy(600)
        onNodeWithTag("silenced").assertTextEquals("1 silenced")
        scope.launch { state.tapControl(BirdcageDemos.CoverUp) }
        mainClock.advanceTimeBy(3_000)
        assertFalse(cage.muted, "cover ${cage.cover}")
        scope.launch { state.resetFeed() }
        mainClock.advanceTimeBy(300)
        onNodeWithTag("latest").assertTextEquals("Maya: Saturday long run starts at 7:00 AM.")
        assertEquals(0, cage.silenced)
    }
}
