package dev.dimvlachos.lab.gallerydemo.presentation.components

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.photo_corfu_title
import dev.dimvlachos.lab.resources.photo_naxos_caption
import dev.dimvlachos.lab.resources.photo_santorini_caption
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class GalleryDemoUiTest {
    private val state = DemoState()

    private fun ComposeUiTest.select(index: Int, thenMs: Long = 2_000) {
        runOnUiThread { state.select(index) }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(thenMs)
    }

    @Test
    fun theTourShowsAPhotoThenSearchResultsAndComesBack() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { LabTheme { GalleryDemo(state) } }
        val santorini = runBlocking { getString(Res.string.photo_santorini_caption) }
        val naxos = runBlocking { getString(Res.string.photo_naxos_caption) }
        val corfu = runBlocking { getString(Res.string.photo_corfu_title) }
        select(3)
        onNodeWithText(santorini).assertExists()
        select(0)
        select(9, thenMs = 2_500)
        onNodeWithText(corfu).assertDoesNotExist()
        select(14)
        onNodeWithText(naxos).assertExists()
        select(0)
        onNodeWithText(corfu).assertExists()
    }
}
