package dev.dimvlachos.lab.photocopierdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.photocopier.CopierMode
import dev.dimvlachos.lab.core.presentation.components.photocopier.PhotocopierState
import dev.dimvlachos.lab.photocopierdemo.PhotocopierDemos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class PhotocopierDemoUiTest {
    @Test
    fun theListShowsTheParkersSixLists() = runComposeUiTest {
        lateinit var copier: PhotocopierState
        setContent {
            copier = rememberPhotocopier()
            Phone { PhotocopierDemo(DemoState(), copier) }
        }
        waitForIdle()
        onNodeWithTag("count").assertTextEquals("The Parker household · 6 lists")
        // Compose resources draw an Android-style \' as typed: no list may carry an escape.
        for (c in copier.cards) {
            val i = c.item
            assertTrue('\\' !in i.title + i.summary + i.meta, "escaped: ${i.title}")
        }
        val label =
            onNodeWithTag("card:Weekly meal plan")
                .fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
        assertEquals(
            listOf(
                "Weekly meal plan. Mon tacos · Tue sheet-pan chicken · Wed leftovers · Thu chili. " +
                    "12 items · Edited Sunday",
            ),
            label,
        )
        onNode(hasContentDescription("More options for Weekly meal plan")).assertExists()
    }

    @Test
    fun aScreenReadersDuplicateMakesACopyAndUndoFeedsItBack() = runComposeUiTest {
        lateinit var copier: PhotocopierState
        setContent {
            copier = rememberPhotocopier()
            Phone { PhotocopierDemo(DemoState(), copier, reducedMotion = false) }
        }
        waitForIdle()
        val duplicate =
            onNodeWithTag("card:Grocery run")
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
                .first { it.label == "Duplicate" }
        // The clock moves only as the test says, so the Undo window doesn't run out on its own.
        mainClock.autoAdvance = false
        runOnIdle { duplicate.action() }
        mainClock.advanceTimeBy(6_000)
        onNodeWithTag("count").assertTextEquals("The Parker household · 7 lists")
        assertEquals("Grocery run (copy)", copier.cards[4].item.title)
        assertEquals(CopierMode.Peek, copier.copMode)
        onNode(hasContentDescription("Duplicated Grocery run")).assertExists()
        onNodeWithTag("undo").performSemanticsAction(SemanticsActions.OnClick)
        mainClock.advanceTimeBy(3_000)
        onNodeWithTag("count").assertTextEquals("The Parker household · 6 lists")
        onNode(hasContentDescription("Copy removed")).assertExists()
    }

    @Test
    fun aRightClickOpensTheMenuAtOnce() = runComposeUiTest {
        lateinit var copier: PhotocopierState
        setContent {
            copier = rememberPhotocopier()
            Phone { PhotocopierDemo(DemoState(), copier, reducedMotion = false) }
        }
        waitForIdle()
        mainClock.autoAdvance = false
        onNodeWithTag("card:Grocery run").performMouseInput { rightClick() }
        mainClock.advanceTimeBy(50)
        assertEquals(copier.cards[3].item.id, copier.menuFor, "no hold first")
        assertFalse(copier.pressing)
    }

    @Test
    fun aDraggedCopyFollowsTheFingerWhileThePressedCardIsScaled() = runComposeUiTest {
        lateinit var copier: PhotocopierState
        setContent {
            copier = rememberPhotocopier()
            Phone { PhotocopierDemo(DemoState(), copier, reducedMotion = false) }
        }
        waitForIdle()
        val duplicate =
            onNodeWithTag("card:Grocery run")
                .fetchSemanticsNode()
                .config[SemanticsActions.CustomActions]
                .first { it.label == "Duplicate" }
        mainClock.autoAdvance = false
        runOnIdle { duplicate.action() }
        mainClock.advanceTimeBy(6_000)
        assertEquals(CopierMode.Peek, copier.copMode)
        val id = copier.cards[4].item.id
        val k = density.density
        onNodeWithTag("card:Grocery run (copy)").performTouchInput { down(Offset(40 * k, 36 * k)) }
        // Held until the card has pressed down to .985, then dragged 60 dp right and 30 dp down.
        mainClock.advanceTimeBy(200)
        onNodeWithTag("card:Grocery run (copy)").performTouchInput {
            moveBy(Offset(60 * k, 30 * k))
        }
        mainClock.advanceTimeBy(16)
        val f = copier.fliers.last()
        assertEquals(copier.rectX() + 60, f.x, .05f)
        assertEquals(copier.rectY(id) + 30, f.y, .05f)
        onNodeWithTag("card:Grocery run (copy)").performTouchInput { up() }
    }

    @Test
    fun theScriptGoesThroughTheGestureAndEndsAsItBegan() = runComposeUiTest {
        val state = DemoState()
        lateinit var copier: PhotocopierState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            copier = rememberPhotocopier()
            Phone { PhotocopierDemo(state, copier, reducedMotion = false) }
        }
        waitForIdle()
        scope.launch { state.tapControl(PhotocopierDemos.LongPress) }
        mainClock.advanceTimeBy(2_000)
        assertEquals(copier.cards[0].item.id, copier.menuFor, "the hold opens the menu")
        scope.launch { state.tapControl(PhotocopierDemos.TapDuplicate) }
        mainClock.advanceTimeBy(7_000)
        assertEquals(7, copier.cards.size)
        scope.launch { state.tapControl(PhotocopierDemos.FeedBack) }
        mainClock.advanceTimeBy(5_000)
        assertEquals(6, copier.cards.size, "the copy went back into the slot")
        scope.launch { state.resetFeed() }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertTrue(copier.asBegun)
    }

    @Test
    fun withReducedMotionTheScriptStillEndsAsItBegan() = runComposeUiTest {
        val state = DemoState()
        lateinit var copier: PhotocopierState
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            copier = rememberPhotocopier()
            Phone { PhotocopierDemo(state, copier, reducedMotion = true) }
        }
        waitForIdle()
        scope.launch { state.tapControl(PhotocopierDemos.LongPress) }
        mainClock.advanceTimeBy(2_000)
        scope.launch { state.tapControl(PhotocopierDemos.TapDuplicate) }
        mainClock.advanceTimeBy(2_000)
        assertEquals(7, copier.cards.size, "the copy just appears")
        val feed = scope.launch { state.tapControl(PhotocopierDemos.FeedBack) }
        mainClock.advanceTimeBy(3_000)
        assertTrue(feed.isCompleted, "no copier to wait for")
        assertTrue(copier.asBegun)
    }

    // A phone's stage.
    @Composable
    private fun Phone(content: @Composable () -> Unit) {
        Box(Modifier.size(412.dp, 820.dp)) { content() }
    }
}
