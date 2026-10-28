package dev.dimvlachos.lab.spiderdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.spider.SpiderPhase
import dev.dimvlachos.lab.core.presentation.components.spider.SpiderRefreshState
import dev.dimvlachos.lab.spiderdemo.SpiderDemos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class SpiderRefreshDemoUiTest {
    @Test
    fun theRefreshButtonReelsInTwoPostsAndSaysSo() = runComposeUiTest {
        val spider = SpiderRefreshState()
        setContent { Phone { SpiderRefreshDemo(DemoState(), spider, reducedMotion = false) } }
        waitForIdle()
        onNodeWithTag("feed")
            .assertContentDescriptionEquals("Neighborhood feed. Pull down to refresh.")
        onNodeWithTag("refresh").performSemanticsAction(SemanticsActions.OnClick)
        mainClock.advanceTimeBy(3_000)
        // The ball has opened into the new posts, hanging on silk and measured.
        assertTrue(spider.reeling)
        assertEquals(listOf(5, 6), spider.hanging.toList())
        mainClock.advanceTimeBy(5_000)
        waitForIdle()
        assertEquals(SpiderPhase.Idle, spider.phase)
        assertEquals(listOf(5, 6, 0, 1, 2, 3, 4), spider.posts.toList())
        onNodeWithTag("live").assertContentDescriptionEquals("2 new posts from your neighbors.")
        onNodeWithText(
                "Pumpkin carving on our porch Saturday at 4. Bring a pumpkin, we’ve got the tools and hot cider.",
            )
            .assertExists()
    }

    @Test
    fun theScriptPullsThroughTheGestureAndLoopsBackToTheFirstFeed() = runComposeUiTest {
        val state = DemoState()
        val spider = SpiderRefreshState()
        lateinit var scope: CoroutineScope
        setContent {
            scope = rememberCoroutineScope()
            Phone { SpiderRefreshDemo(state, spider, reducedMotion = false) }
        }
        waitForIdle()
        scope.launch { state.tapControl(SpiderDemos.PullShort) }
        mainClock.advanceTimeBy(1_400)
        assertTrue(spider.held, "the hand pulls the feed")
        assertTrue(spider.pull > 0f, "pull ${spider.pull}")
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
        assertEquals(SpiderPhase.Idle, spider.phase)
        assertEquals(SpiderRefreshState.DefaultFeed, spider.posts.toList())
        scope.launch { state.tapControl(SpiderDemos.Pull) }
        mainClock.advanceTimeBy(9_000)
        waitForIdle()
        assertEquals(7, spider.posts.size)
        scope.launch { state.resetFeed() }
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
        assertEquals(SpiderRefreshState.DefaultFeed, spider.posts.toList())
    }

    @Composable
    private fun Phone(content: @Composable () -> Unit) {
        Box(Modifier.size(412.dp, 860.dp)) { content() }
    }
}
