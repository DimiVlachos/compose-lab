package dev.dimvlachos.lab.profiledemo.presentation.components

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.add_image_title
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class ProfileDemoUiTest {
    @Test
    fun profileWalksSearchThenDialogAndBack() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        setContent { LabTheme { ProfileDemo(state) } }
        val dialogTitle = runBlocking { getString(Res.string.add_image_title) }
        runOnUiThread { state.select(2) }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(2_000)
        onNodeWithText("Corfu").assertExists()
        runOnUiThread { state.select(3) }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(2_000)
        onNodeWithText(dialogTitle).assertExists()
        runOnUiThread { state.select(0) }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(2_000)
        onNodeWithText(dialogTitle).assertDoesNotExist()
    }
}
