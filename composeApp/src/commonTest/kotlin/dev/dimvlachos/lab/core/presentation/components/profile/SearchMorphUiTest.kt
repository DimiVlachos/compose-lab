package dev.dimvlachos.lab.core.presentation.components.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SearchMorphUiTest {
    private val candidates = listOf("Corfu", "Corfu Old Town", "Corinth", "Naxos", "Paxos")

    @Test
    fun barTypesTheQueryAfterLandingAndRestartsOnReopen() = runComposeUiTest {
        mainClock.autoAdvance = false
        var open by mutableIntStateOf(0)
        var opened = 0
        var closed = 0
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
                                "Cor",
                                candidates,
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
        onNodeWithTag(SearchSourceTag).performClick()
        assertEquals(1, opened)
        runOnUiThread { open = 2 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(300)
        onNodeWithText("Corfu").assertDoesNotExist() // still morphing, nothing typed yet
        mainClock.advanceTimeBy(1_500)
        onNodeWithText("Corfu").assertExists()
        onNodeWithText("Naxos").assertDoesNotExist()
        onNodeWithTag(SearchBackTag).performClick()
        assertEquals(1, closed)
        runOnUiThread { open = 0 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(1_000)
        onNodeWithTag(SearchPillTag).assertDoesNotExist()
        runOnUiThread { open = 2 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(300)
        onNodeWithText("Corfu").assertDoesNotExist() // typing restarted from ""
    }
}
