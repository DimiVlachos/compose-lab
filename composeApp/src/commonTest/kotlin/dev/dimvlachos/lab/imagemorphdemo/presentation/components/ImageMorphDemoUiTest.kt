package dev.dimvlachos.lab.imagemorphdemo.presentation.components

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphLayers
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_close
import dev.dimvlachos.lab.resources.photo_paxos_caption
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class ImageMorphDemoUiTest {
    @Test
    fun morphAllShowsTheDetailAfterSelectTwoAndReturnsOnSelectZero() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        setContent { LabTheme { ImageMorphDemo(state, MorphLayers.All) } }
        // Captions only exist on the detail end; titles are on the card labels too.
        val caption = runBlocking { getString(Res.string.photo_paxos_caption) }
        val close = runBlocking { getString(Res.string.action_close) }
        onNodeWithText(caption).assertDoesNotExist()

        runOnUiThread { state.select(2) }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(2_000)
        onNodeWithText(caption).assertExists()
        onNodeWithContentDescription(close).assertExists()

        runOnUiThread { state.select(0) }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(2_000)
        onNodeWithText(caption).assertDoesNotExist()
        onNodeWithContentDescription(close).assertDoesNotExist()
    }
}
