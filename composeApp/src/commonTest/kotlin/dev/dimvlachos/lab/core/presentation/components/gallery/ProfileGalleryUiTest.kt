package dev.dimvlachos.lab.core.presentation.components.gallery

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.dimvlachos.lab.core.presentation.components.imagemorph.LocalMorphCompositionProbe
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphEnd
import dev.dimvlachos.lab.core.presentation.components.profile.AvatarSourceTag
import dev.dimvlachos.lab.core.presentation.components.profile.AvatarTargetTag
import dev.dimvlachos.lab.core.presentation.components.profile.DialogScrimTag
import dev.dimvlachos.lab.core.presentation.components.profile.FabDialogTag
import dev.dimvlachos.lab.core.presentation.components.profile.FabSourceTag
import dev.dimvlachos.lab.core.presentation.components.profile.ScrimTag
import dev.dimvlachos.lab.core.presentation.components.profile.SearchSourceTag
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.portrait
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ProfileGalleryUiTest {
    private var scene by mutableStateOf(GalleryScene())
    private val reported = mutableListOf<GalleryScene>()

    private fun ComposeUiTest.showGallery(probe: ((MorphEnd) -> Unit)? = null) {
        mainClock.autoAdvance = false
        setContent {
            LabTheme {
                CompositionLocalProvider(LocalMorphCompositionProbe provides probe) {
                    ProfileGallery(
                        name = "Alex Morgan",
                        portrait = Res.drawable.portrait,
                        photos = testIslands(),
                        searchQuery = "xos",
                        scene = scene,
                        onSceneChange = { reported += it },
                    )
                }
            }
        }
        mainClock.advanceTimeBy(500)
    }

    private fun ComposeUiTest.go(to: GalleryScene, thenMs: Long = 1_500) {
        runOnUiThread { scene = to }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(thenMs)
    }

    @Test
    fun eachTriggerReportsTheNextScene() = runComposeUiTest {
        showGallery()
        onNodeWithText("Milos").performClick()
        onNodeWithTag(AvatarSourceTag).performClick()
        onNodeWithTag(SearchSourceTag).performClick()
        assertEquals(
            listOf(
                GalleryScene(photo = 4),
                GalleryScene(avatar = true),
                GalleryScene(search = true),
            ),
            reported,
        )
    }

    @Test
    fun photoCardAndDetailOverlapWhileMorphing() = runComposeUiTest {
        showGallery()
        go(GalleryScene(photo = 3), thenMs = 100)
        onAllNodesWithText("Santorini").assertCountEquals(2)
    }

    @Test
    fun theFabOnlyExistsOverTheZoomedAvatarAndOpensTheDialog() = runComposeUiTest {
        showGallery()
        onNodeWithTag(FabSourceTag).assertDoesNotExist()
        go(GalleryScene(avatar = true))
        onNodeWithTag(FabSourceTag).performClick()
        assertEquals(GalleryScene(avatar = true, dialog = true), reported.last())
        go(GalleryScene(avatar = true, dialog = true))
        onNodeWithTag(FabDialogTag).assertExists()
        onNodeWithTag(FabSourceTag).assertDoesNotExist()
        onNodeWithTag(DialogScrimTag).performClick()
        assertEquals(GalleryScene(avatar = true), reported.last(), "only the dialog closes")
        go(GalleryScene(avatar = true))
        onNodeWithTag(FabSourceTag).assertExists()
        onNodeWithTag(AvatarTargetTag).assertExists()
    }

    @Test
    fun theAvatarScrimClosesWithoutReachingTheScreen() = runComposeUiTest {
        showGallery()
        go(GalleryScene(avatar = true))
        onNodeWithTag(ScrimTag).performClick()
        assertEquals(listOf(GalleryScene()), reported)
    }

    @Test
    fun searchFiltersTheGridAndFoldsTheHeader() = runComposeUiTest {
        showGallery()
        go(GalleryScene(search = true), thenMs = 2_500)
        onNodeWithTag(ProfileNameTag).assertDoesNotExist()
        onNodeWithText("Paxos").assertExists()
        onNodeWithText("Naxos").assertExists()
        for (gone in listOf("Corfu", "Santorini", "Milos", "Hydra")) {
            onNodeWithText(gone).assertDoesNotExist()
        }
        go(GalleryScene(), thenMs = 2_000)
        onNodeWithTag(ProfileNameTag).assertExists()
        for (title in IslandTitles) onNodeWithText(title).assertExists()
    }

    @Test
    fun aPhotoOpensFromTheFilteredGridAndReturnsToIt() = runComposeUiTest {
        showGallery()
        go(GalleryScene(search = true), thenMs = 2_500)
        onNodeWithText("Naxos").performClick()
        assertEquals(GalleryScene(photo = 5, search = true), reported.last())
        go(GalleryScene(photo = 5, search = true))
        onNodeWithText("Caption Naxos").assertExists()
        go(GalleryScene(search = true))
        onNodeWithText("Caption Naxos").assertDoesNotExist()
        onNodeWithText("Naxos").assertExists()
        onNodeWithText("Corfu").assertDoesNotExist()
    }

    @Test
    fun anIllegalSceneShowsTheNearestLegalOne() = runComposeUiTest {
        scene = GalleryScene(photo = 9, dialog = true)
        showGallery()
        for (title in IslandTitles) onAllNodesWithText(title).assertCountEquals(1)
        onNodeWithTag(AvatarTargetTag).assertDoesNotExist()
        onNodeWithTag(FabDialogTag).assertDoesNotExist()
    }

    @Test
    fun nothingRecomposesDuringThePhotoAndAvatarMorphs() = runComposeUiTest {
        var sources = 0
        var targets = 0
        showGallery { end -> if (end == MorphEnd.Card) sources++ else targets++ }
        for (open in listOf(GalleryScene(photo = 2), GalleryScene(avatar = true))) {
            go(open) // warm-up: first decode and first overlay
            go(GalleryScene())
            waitForIdle()
            go(open, thenMs = 32)
            val afterOpen = sources to targets
            mainClock.advanceTimeBy(1_500)
            assertEquals(afterOpen, sources to targets, "$open recomposed while animating")
            go(GalleryScene())
        }
    }

    @Test
    fun theAvatarScrimClosesFromAnywhereAroundTheCircle() = runComposeUiTest {
        showGallery()
        val spots: List<TouchInjectionScope.() -> Offset> =
            listOf({ topCenter }, { centerRight }, { bottomCenter }, { bottomLeft }, { centerLeft })
        for (spot in spots) {
            reported.clear()
            go(GalleryScene(avatar = true))
            onNodeWithTag(ScrimTag).performTouchInput { click(spot()) }
            assertEquals(
                listOf(GalleryScene()),
                reported,
                "a tap at ${onNodeWithTag(ScrimTag).fetchSemanticsNode().size} failed",
            )
            go(GalleryScene())
        }
    }
}
