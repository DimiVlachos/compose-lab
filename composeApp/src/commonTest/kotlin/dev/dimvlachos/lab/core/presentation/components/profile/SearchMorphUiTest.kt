package dev.dimvlachos.lab.core.presentation.components.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SearchMorphUiTest {
    private val typed = mutableStateOf("")
    private var open by mutableIntStateOf(0)
    private var opened = 0
    private var closed = 0

    private fun ComposeUiTest.showSearch() {
        mainClock.autoAdvance = false
        setContent {
            LabTheme {
                SharedTransitionLayout(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize()) {
                        AnimatedVisibility(
                            open == 0,
                            enter = EnterTransition.None,
                            exit = ExitTransition.None,
                        ) {
                            SearchSource(
                                this@SharedTransitionLayout,
                                this,
                                { open == 2 },
                                { opened++ },
                            )
                        }
                        AnimatedVisibility(
                            open == 2,
                            enter = EnterTransition.None,
                            exit = ExitTransition.None,
                        ) {
                            SearchTarget(
                                "xos",
                                typed,
                                this@SharedTransitionLayout,
                                this,
                                { open == 2 },
                                { closed++ },
                            )
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(500)
    }

    private fun ComposeUiTest.set(value: Int, thenMs: Long) {
        runOnUiThread { open = value }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(thenMs)
    }

    @Test
    fun barTypesTheQueryAfterLandingAndRestartsOnReopen() = runComposeUiTest {
        showSearch()
        onNodeWithTag(SearchSourceTag).performClick()
        assertEquals(1, opened)
        set(2, thenMs = 300)
        onNodeWithText("xos").assertDoesNotExist() // still morphing, nothing typed yet
        mainClock.advanceTimeBy(1_500)
        onNodeWithText("xos").assertExists()
        assertEquals("xos", typed.value)
        onNodeWithTag(SearchBackTag).performClick()
        assertEquals(1, closed)
        set(0, thenMs = 1_000)
        onNodeWithTag(SearchPillTag).assertDoesNotExist()
        set(2, thenMs = 300)
        assertEquals("", typed.value, "typing restarts from an empty query")
    }

    @Test
    fun typingWaitsABeatAfterThePillLands() = runComposeUiTest {
        showSearch()
        set(2, thenMs = 800) // landed at 500 ms, still pausing
        assertEquals("", typed.value)
    }

    @Test
    fun typingStartsAfterItsPauseCentredInThePill() = runComposeUiTest {
        showSearch()
        // The pill lands at 500 ms, pauses 400 ms, and "xos" takes 360 ms.
        set(2, thenMs = 1_400)
        onNodeWithText("xos").assertExists()
        val pill = onNodeWithTag(SearchPillTag).getBoundsInRoot()
        val text = onNodeWithText("xos").getBoundsInRoot()
        assertEquals(
            (pill.top + pill.bottom).value / 2,
            (text.top + text.bottom).value / 2,
            1f,
            "the query is centred in the pill",
        )
    }
}
