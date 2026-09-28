package dev.dimvlachos.lab.core.presentation.components.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.portrait
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalTestApi::class)
class AvatarMorphUiTest {
    @Test
    fun avatarOpensAndClosesByState() = runComposeUiTest {
        mainClock.autoAdvance = false
        var open by mutableIntStateOf(0)
        var opened = 0
        var closed = 0
        setContent {
            LabTheme {
                val painter = painterResource(Res.drawable.portrait)
                SharedTransitionLayout(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize()) {
                        AnimatedVisibility(
                            open == 0,
                            enter = EnterTransition.None,
                            exit = ExitTransition.None,
                        ) {
                            AvatarSource(painter, this@SharedTransitionLayout, this, { opened++ })
                        }
                        AnimatedVisibility(
                            open == 1,
                            enter = EnterTransition.None,
                            exit = ExitTransition.None,
                        ) {
                            AvatarTarget(painter, this@SharedTransitionLayout, this, { closed++ })
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(500)
        onNodeWithTag(AvatarSourceTag).performClick()
        assertEquals(1, opened)
        runOnUiThread { open = 1 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(1_000)
        onNodeWithTag(AvatarTargetTag).assertExists()
        onNodeWithTag(AvatarSourceTag).assertDoesNotExist()
        onNodeWithTag(ScrimTag).performClick()
        assertEquals(1, closed)
        assertEquals(1, opened, "the scrim tap must not reach the avatar under it")
        runOnUiThread { open = 0 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(1_000)
        onNodeWithTag(AvatarTargetTag).assertDoesNotExist()
        onNodeWithTag(AvatarSourceTag).assertExists()
    }

    @Test
    fun overlayComesAndGoesWithTheZoomedAvatar() = runComposeUiTest {
        mainClock.autoAdvance = false
        var open by mutableIntStateOf(0)
        setContent {
            LabTheme {
                val painter = painterResource(Res.drawable.portrait)
                SharedTransitionLayout(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize()) {
                        AnimatedVisibility(
                            open == 1,
                            enter = EnterTransition.None,
                            exit = ExitTransition.None,
                        ) {
                            AvatarTarget(painter, this@SharedTransitionLayout, this, {}) { chrome ->
                                Box(chrome.testTag("overlay").size(10.dp))
                            }
                        }
                    }
                }
            }
        }
        onNodeWithTag("overlay").assertDoesNotExist()
        runOnUiThread { open = 1 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(1_500)
        onNodeWithTag("overlay").assertExists()
        runOnUiThread { open = 0 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(1_000)
        onNodeWithTag("overlay").assertDoesNotExist()
    }
}
