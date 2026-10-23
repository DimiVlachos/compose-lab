package dev.dimvlachos.lab.locketdemo.presentation.components

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.locket.LocketState
import dev.dimvlachos.lab.locketdemo.LocketDemos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class LocketDemoUiTest {
    @Test
    fun itStartsShutWithTheBlueRidgePhoto() = runComposeUiTest {
        setContent { LocketDemo(DemoState()) }
        onNodeWithTag("status").assertTextEquals("Profile photo · Blue Ridge at dusk")
        onNodeWithTag("tip").assertTextEquals("Tap the catch to open the locket")
        onNodeWithTag("locket").assertContentDescriptionEquals("Open locket")
        onNodeWithTag("photo-1")
            .assertContentDescriptionEquals("Use photo: Biscuit at Carrier Park")
    }

    @Test
    fun theLocketButtonOpensAndClosesIt() = runComposeUiTest {
        val locket = LocketState()
        setContent { LocketDemo(DemoState(), locket) }
        onNodeWithTag("locket").performSemanticsAction(SemanticsActions.OnClick)
        mainClock.advanceTimeBy(1_000)
        onNodeWithTag("locket").assertContentDescriptionEquals("Close locket")
        onNodeWithTag("tip").assertTextEquals("Drag a photo in from Recent")
        assertTrue(locket.a > 2.3f, "a ${locket.a}")
        onNodeWithTag("locket").performSemanticsAction(SemanticsActions.OnClick)
        mainClock.advanceTimeBy(1_000)
        assertFalse(locket.isOpen)
        assertTrue(locket.a < .05f)
        assertEquals(1, locket.snaps)
    }

    @Test
    fun usingARecentPhotoPutsItInTheLocketAndNamesIt() = runComposeUiTest {
        val locket = LocketState()
        setContent { LocketDemo(DemoState(), locket) }
        onNodeWithTag("photo-3").performSemanticsAction(SemanticsActions.OnClick)
        mainClock.advanceTimeBy(1_500)
        assertEquals(3, locket.photo)
        onNodeWithTag("status").assertTextEquals("Profile photo · Sunflowers on Haywood Road")
        onNodeWithTag("tip").assertTextEquals("Tap the locket to close it")
    }

    @Test
    fun theTextHoldsStillWhileAPhotoTrimsInThenChangesOnce() = runComposeUiTest {
        val locket = LocketState()
        setContent { LocketDemo(DemoState(), locket) }
        waitForIdle()
        // Stepped by hand, so checking the text doesn't run the trim to its end first.
        mainClock.autoAdvance = false
        // Used on the closed locket: it opens and the photo trims in over 480 ms.
        onNodeWithTag("photo-3").performSemanticsAction(SemanticsActions.OnClick)
        mainClock.advanceTimeBy(200)
        assertTrue(locket.isOpen && locket.placingPhoto == 3, "mid-trim")
        // Neither the opening nor the trim changes the text.
        onNodeWithTag("status").assertTextEquals("Profile photo · Blue Ridge at dusk")
        onNodeWithTag("tip").assertTextEquals("Tap the catch to open the locket")
        mainClock.advanceTimeBy(1_300)
        onNodeWithTag("status").assertTextEquals("Profile photo · Sunflowers on Haywood Road")
        onNodeWithTag("tip").assertTextEquals("Tap the locket to close it")
    }

    @Test
    fun theScriptOpensDragsInClosesAndResets() = runComposeUiTest {
        val state = DemoState()
        val locket = LocketState()
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            LocketDemo(state, locket)
        }
        waitForIdle()
        scope.launch { state.tapControl(LocketDemos.OpenCatch) }
        mainClock.advanceTimeBy(2_000)
        assertTrue(locket.isOpen, "the tap on the catch opens it")
        scope.launch { state.tapControl(LocketDemos.DragPhoto) }
        mainClock.advanceTimeBy(3_000)
        assertEquals(0, locket.photo, "the drag puts Folly Beach in")
        scope.launch { state.tapControl(LocketDemos.CloseLid) }
        mainClock.advanceTimeBy(1_500)
        assertFalse(locket.isOpen, "the tap shuts it")
        scope.launch { state.resetFeed() }
        mainClock.advanceTimeBy(200)
        assertEquals(2, locket.photo)
        assertFalse(locket.isOpen)
    }
}
