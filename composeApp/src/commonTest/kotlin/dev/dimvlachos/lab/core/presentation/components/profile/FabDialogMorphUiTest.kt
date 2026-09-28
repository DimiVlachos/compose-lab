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
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_cancel
import dev.dimvlachos.lab.resources.add_image_camera
import dev.dimvlachos.lab.resources.add_image_title
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class FabDialogMorphUiTest {
    @Test
    fun fabOpensTheDialogAndItsControlsClose() = runComposeUiTest {
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
                            FabSource(this@SharedTransitionLayout, this, { opened++ })
                        }
                        AnimatedVisibility(
                            open == 3,
                            enter = EnterTransition.None,
                            exit = ExitTransition.None,
                        ) {
                            FabDialogTarget(this@SharedTransitionLayout, this, { closed++ })
                        }
                    }
                }
            }
        }
        mainClock.advanceTimeBy(500)
        onNodeWithTag(FabSourceTag).performClick()
        assertEquals(1, opened)
        runOnUiThread { open = 3 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(1_500)
        onNodeWithTag(FabDialogTag).assertExists()
        onNodeWithText(runBlocking { getString(Res.string.add_image_title) }).assertExists()
        onNodeWithText(runBlocking { getString(Res.string.add_image_camera) }).assertExists()
        onNodeWithText(runBlocking { getString(Res.string.action_cancel) }).performClick()
        assertEquals(1, closed)
        onNodeWithTag(ScrimTag).performClick()
        assertEquals(2, closed)
        assertEquals(1, opened, "the scrim tap must not reach the FAB under it")
    }
}
