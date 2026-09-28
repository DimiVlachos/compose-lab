package dev.dimvlachos.lab.core.presentation.components.gallery

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.dimvlachos.lab.core.presentation.components.imagemorph.rememberMorphPainters
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class GalleryGridUiTest {
    private val query = mutableStateOf("")
    private val opened = mutableListOf<Int>()

    private fun ComposeUiTest.showGrid() {
        mainClock.autoAdvance = false
        setContent {
            LabTheme {
                val photos = testIslands()
                val painters = rememberMorphPainters(photos)
                SharedTransitionLayout(Modifier.fillMaxSize()) {
                    GalleryGrid(
                        photos,
                        painters,
                        query,
                        openPhoto = 0,
                        sharedTransitionScope = this,
                        onOpen = { opened += it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        mainClock.advanceTimeBy(500)
    }

    private fun ComposeUiTest.type(value: String) {
        runOnUiThread { query.value = value }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(1_500)
    }

    @Test
    fun clickingACardReportsItsIndex() = runComposeUiTest {
        showGrid()
        onNodeWithText("Milos").performClick()
        assertEquals(listOf(4), opened)
    }

    @Test
    fun theQueryKeepsTheMatchesInTheFirstSlots() = runComposeUiTest {
        showGrid()
        val firstSlot = onNodeWithText("Corfu").getBoundsInRoot()
        type("xos")
        for (gone in listOf("Corfu", "Santorini", "Milos", "Hydra")) {
            onNodeWithText(gone).assertDoesNotExist()
        }
        val paxos = onNodeWithText("Paxos").getBoundsInRoot()
        val naxos = onNodeWithText("Naxos").getBoundsInRoot()
        assertEquals(firstSlot.left, paxos.left, "Paxos glides into the first slot")
        assertEquals(firstSlot.top, paxos.top)
        assertEquals(paxos.top, naxos.top, "Naxos glides up next to it")
        assertTrue(naxos.left > paxos.left)
    }

    @Test
    fun clearingTheQueryBringsEveryCardBack() = runComposeUiTest {
        showGrid()
        type("xos")
        type("")
        for (title in IslandTitles) onNodeWithText(title).assertExists()
        assertEquals(
            onNodeWithText("Corfu").getBoundsInRoot().top,
            onNodeWithText("Paxos").getBoundsInRoot().top,
            "Paxos is back in the first row",
        )
    }
}
