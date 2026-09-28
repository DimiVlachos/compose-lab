package dev.dimvlachos.lab.core.presentation.components.imagemorph

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_close
import dev.dimvlachos.lab.resources.photo_corfu
import dev.dimvlachos.lab.resources.photo_hydra
import dev.dimvlachos.lab.resources.photo_milos
import dev.dimvlachos.lab.resources.photo_naxos
import dev.dimvlachos.lab.resources.photo_paxos
import dev.dimvlachos.lab.resources.photo_santorini
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class ImageMorphUiTest {
    private val photos =
        listOf(
            MorphPhoto(Res.drawable.photo_corfu, "Corfu", "Sunset over the west coast"),
            MorphPhoto(Res.drawable.photo_paxos, "Paxos", "A harbour town at golden hour"),
            MorphPhoto(Res.drawable.photo_santorini, "Santorini", "Oia lit up at dusk"),
            MorphPhoto(Res.drawable.photo_milos, "Milos", "A boat under the white cliffs"),
            MorphPhoto(Res.drawable.photo_naxos, "Naxos", "The Portara at sunset"),
            MorphPhoto(Res.drawable.photo_hydra, "Hydra", "Sailing into the harbour"),
        )
    private val close = runBlocking { getString(Res.string.action_close) }

    @Test
    fun clickingACardReportsItsIndex() = runComposeUiTest {
        var expanded = -1
        setContent {
            LabTheme {
                ImageMorph(photos, expandedIndex = 0, onExpand = { expanded = it }, onCollapse = {})
            }
        }
        onNodeWithText("Paxos").performClick()
        assertEquals(2, expanded)
    }

    @Test
    fun closeChipReportsCollapse() = runComposeUiTest {
        var collapsed = false
        setContent {
            LabTheme {
                ImageMorph(
                    photos,
                    expandedIndex = 2,
                    onExpand = {},
                    onCollapse = { collapsed = true },
                )
            }
        }
        onNodeWithContentDescription(close).performClick()
        assertTrue(collapsed)
    }

    @Test
    fun outOfRangeIndexShowsTheGrid() = runComposeUiTest {
        setContent {
            LabTheme { ImageMorph(photos, expandedIndex = 9, onExpand = {}, onCollapse = {}) }
        }
        onNodeWithContentDescription(close).assertDoesNotExist()
        onAllNodesWithText("Hydra").assertCountEquals(1)
    }

    @Test
    fun gridStaysComposedWhileTheMorphRuns() = runComposeUiTest {
        mainClock.autoAdvance = false
        var expanded by mutableIntStateOf(0)
        setContent {
            LabTheme {
                ImageMorph(photos, expandedIndex = expanded, onExpand = {}, onCollapse = {})
            }
        }
        onAllNodesWithText("Paxos").assertCountEquals(1)
        runOnUiThread { expanded = 2 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(100)
        // The card's label and the detail's title are both on screen: a hand-over, never a swap.
        onAllNodesWithText("Paxos").assertCountEquals(2)
        mainClock.advanceTimeBy(2_000)
        onAllNodesWithText("Paxos").assertCountEquals(1)
    }

    @Test
    fun detailDoesNotRecomposeAfterTheOpenFrame() = runComposeUiTest {
        var cards = 0
        var details = 0
        var expanded by mutableIntStateOf(0)
        setContent {
            LabTheme {
                CompositionLocalProvider(
                    LocalMorphCompositionProbe provides
                        { end ->
                            if (end == MorphEnd.Card) cards++ else details++
                        }
                ) {
                    ImageMorph(photos, expandedIndex = expanded, onExpand = {}, onCollapse = {})
                }
            }
        }
        mainClock.autoAdvance = false
        // One full cycle first: the bitmaps decode asynchronously and recompose each card once
        // when they land (unless another test already warmed the cache), so the morph is measured
        // on a second open, with every image in place.
        runOnUiThread { expanded = 2 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(2_000)
        runOnUiThread { expanded = 0 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(2_000)
        waitForIdle()
        val detailsBefore = details

        runOnUiThread { expanded = 3 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(32)
        val afterOpen = cards to details
        assertEquals(detailsBefore + 1, details, "the detail composed once")
        mainClock.advanceTimeBy(2_000)
        assertEquals(afterOpen, cards to details, "no card or detail recomposed during the morph")
    }
}
