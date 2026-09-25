package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Velocity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class NavBarScrollStateTest {
    private fun state() = NavBarScrollState(collapseDistancePx = 100f)

    @Test
    fun scrollingTowardTheEndCollapsesProportionally() {
        val state = state()
        state.onScroll(-25f)
        assertEquals(0.25f, state.collapse, 0.0001f)
    }

    @Test
    fun scrollingBackExpands() {
        val state = state()
        state.onScroll(-100f)
        state.onScroll(40f)
        assertEquals(0.6f, state.collapse, 0.0001f)
    }

    @Test
    fun hugeDeltasAreClampedInBothDirections() {
        val state = state()
        state.onScroll(-100_000f)
        assertEquals(1f, state.collapse)
        state.onScroll(-5f)
        assertEquals(1f, state.collapse)
        state.onScroll(100_000f)
        assertEquals(0f, state.collapse)
        state.onScroll(5f)
        assertEquals(0f, state.collapse)
    }

    @Test
    fun nestedScrollObservesWithoutConsuming() {
        val state = state()
        val consumed =
            state.nestedScrollConnection.onPreScroll(Offset(0f, -50f), NestedScrollSource.UserInput)
        assertEquals(Offset.Zero, consumed)
        assertEquals(0.5f, state.collapse, 0.0001f)
    }

    @Test
    fun postFlingSettlesAPartialCollapseToTheNearerLowerEnd() = runComposeUiTest {
        val state = state()
        lateinit var scope: CoroutineScope
        setContent { scope = rememberCoroutineScope() }
        state.onScroll(-30f)

        runOnUiThread {
            scope.launch { state.nestedScrollConnection.onPostFling(Velocity.Zero, Velocity.Zero) }
        }
        waitForIdle()

        assertEquals(0f, state.collapse, 0.0001f)
    }

    @Test
    fun postFlingSettlesAPartialCollapseToTheNearerUpperEnd() = runComposeUiTest {
        val state = state()
        lateinit var scope: CoroutineScope
        setContent { scope = rememberCoroutineScope() }
        state.onScroll(-70f)

        runOnUiThread {
            scope.launch { state.nestedScrollConnection.onPostFling(Velocity.Zero, Velocity.Zero) }
        }
        waitForIdle()

        assertEquals(1f, state.collapse, 0.0001f)
    }

    @Test
    fun postFlingAtEitherEndDoesNothing() = runComposeUiTest {
        val atStart = state()
        val atEnd = state().also { it.onScroll(-1_000f) }
        lateinit var scope: CoroutineScope
        setContent { scope = rememberCoroutineScope() }

        runOnUiThread {
            scope.launch {
                atStart.nestedScrollConnection.onPostFling(Velocity.Zero, Velocity.Zero)
                atEnd.nestedScrollConnection.onPostFling(Velocity.Zero, Velocity.Zero)
            }
        }
        waitForIdle()

        assertEquals(0f, atStart.collapse)
        assertEquals(1f, atEnd.collapse)
    }

    @Test
    fun anExactTieSettlesInTheDirectionOfTheLastNegativeScrollDelta() = runComposeUiTest {
        val state = state()
        lateinit var scope: CoroutineScope
        setContent { scope = rememberCoroutineScope() }
        state.onScroll(-50f)

        runOnUiThread {
            scope.launch { state.nestedScrollConnection.onPostFling(Velocity.Zero, Velocity.Zero) }
        }
        waitForIdle()

        assertEquals(1f, state.collapse, 0.0001f)
    }

    @Test
    fun anExactTieSettlesInTheDirectionOfTheLastPositiveScrollDelta() = runComposeUiTest {
        val state = state()
        lateinit var scope: CoroutineScope
        setContent { scope = rememberCoroutineScope() }
        state.onScroll(-70f)
        state.onScroll(20f)

        runOnUiThread {
            scope.launch { state.nestedScrollConnection.onPostFling(Velocity.Zero, Velocity.Zero) }
        }
        waitForIdle()

        assertEquals(0f, state.collapse, 0.0001f)
    }

    @Test
    fun anExactTieWithNoDirectionalScrollSettlesToTheLowerEnd() = runComposeUiTest {
        val state = state()
        lateinit var scope: CoroutineScope
        setContent { scope = rememberCoroutineScope() }
        state.onScroll(-50f)
        state.onScroll(0f)

        runOnUiThread {
            scope.launch { state.nestedScrollConnection.onPostFling(Velocity.Zero, Velocity.Zero) }
        }
        waitForIdle()

        assertEquals(0f, state.collapse, 0.0001f)
    }

    @Test
    fun aNewScrollInterruptsASettleAndTakesOver() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = state()
        lateinit var scope: CoroutineScope
        setContent { scope = rememberCoroutineScope() }
        state.onScroll(-30f)

        runOnUiThread {
            scope.launch { state.nestedScrollConnection.onPostFling(Velocity.Zero, Velocity.Zero) }
        }
        mainClock.advanceTimeBy(48)
        val midSettle = state.collapse
        assertTrue(
            midSettle in 0f..<0.3f,
            "expected the settle to be under way, was $midSettle",
        )

        runOnUiThread {
            state.nestedScrollConnection.onPreScroll(
                Offset(0f, -1_000f),
                NestedScrollSource.UserInput,
            )
        }
        assertEquals(1f, state.collapse, 0.0001f, "the interrupting scroll must win immediately")

        mainClock.advanceTimeBy(5_000)
        assertEquals(1f, state.collapse, 0.0001f, "the cancelled settle must not resume")
    }

    @Test
    fun interruptingASettleLeavesTheFlingCallerCompletedNotCancelled() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = state()
        lateinit var scope: CoroutineScope
        setContent { scope = rememberCoroutineScope() }
        state.onScroll(-30f)

        lateinit var flingJob: Job
        runOnUiThread {
            flingJob = scope.launch {
                state.nestedScrollConnection.onPostFling(Velocity.Zero, Velocity.Zero)
            }
        }
        mainClock.advanceTimeBy(48)

        runOnUiThread {
            state.nestedScrollConnection.onPreScroll(
                Offset(0f, -1_000f),
                NestedScrollSource.UserInput,
            )
        }
        mainClock.advanceTimeBy(48)

        assertTrue(flingJob.isCompleted, "the coroutine that ran the fling must complete, not hang")
        assertTrue(
            !flingJob.isCancelled,
            "cancelling only the settle must not cancel Foundation's own fling coroutine",
        )
    }

    @Test
    fun aSecondPostFlingPreemptsTheFirstAndBothCallersCompleteNormally() = runComposeUiTest {
        val state = state()
        lateinit var scope: CoroutineScope
        setContent { scope = rememberCoroutineScope() }
        state.onScroll(-30f)

        lateinit var firstFlingJob: Job
        lateinit var secondFlingJob: Job
        runOnUiThread {
            firstFlingJob = scope.launch {
                state.nestedScrollConnection.onPostFling(Velocity.Zero, Velocity.Zero)
            }
            secondFlingJob = scope.launch {
                state.nestedScrollConnection.onPostFling(Velocity.Zero, Velocity.Zero)
            }
        }
        waitForIdle()

        assertEquals(0f, state.collapse, 0.0001f)
        assertTrue(firstFlingJob.isCompleted && !firstFlingJob.isCancelled)
        assertTrue(secondFlingJob.isCompleted && !secondFlingJob.isCancelled)
    }
}
