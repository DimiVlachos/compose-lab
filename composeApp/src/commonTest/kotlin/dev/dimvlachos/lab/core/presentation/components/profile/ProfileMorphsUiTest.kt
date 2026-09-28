package dev.dimvlachos.lab.core.presentation.components.profile

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.dimvlachos.lab.core.presentation.components.imagemorph.LocalMorphCompositionProbe
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphEnd
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.portrait
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ProfileMorphsUiTest {
    private val candidates = listOf("Corfu", "Corinth", "Naxos")

    @Test
    fun clickingEachTriggerReportsItsIndex() = runComposeUiTest {
        val opened = mutableListOf<Int>()
        setContent {
            LabTheme {
                ProfileMorphs(
                    "Profile",
                    "Alex Morgan",
                    Res.drawable.portrait,
                    "Cor",
                    candidates,
                    0,
                    { opened += it },
                    {},
                )
            }
        }
        onNodeWithTag(AvatarSourceTag).performClick()
        onNodeWithTag(SearchSourceTag).performClick()
        onNodeWithTag(FabSourceTag).performClick()
        assertEquals(listOf(1, 2, 3), opened)
    }

    @Test
    fun nameDoesNotMoveWhileTheAvatarIsOpen() = runComposeUiTest {
        mainClock.autoAdvance = false
        var open by mutableIntStateOf(0)
        setContent {
            LabTheme {
                ProfileMorphs(
                    "Profile",
                    "Alex Morgan",
                    Res.drawable.portrait,
                    "Cor",
                    candidates,
                    open,
                    {},
                    {},
                )
            }
        }
        mainClock.advanceTimeBy(500)
        val before = onNodeWithTag(ProfileNameTag).getBoundsInRoot()
        runOnUiThread { open = 1 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(1_000)
        assertEquals(before, onNodeWithTag(ProfileNameTag).getBoundsInRoot())
    }

    @Test
    fun sourceAndTargetOverlapWhileMorphing() = runComposeUiTest {
        mainClock.autoAdvance = false
        var open by mutableIntStateOf(0)
        setContent {
            LabTheme {
                ProfileMorphs(
                    "Profile",
                    "Alex Morgan",
                    Res.drawable.portrait,
                    "Cor",
                    candidates,
                    open,
                    {},
                    {},
                )
            }
        }
        mainClock.advanceTimeBy(500)
        runOnUiThread { open = 3 }
        Snapshot.sendApplyNotifications()
        mainClock.advanceTimeBy(100)
        onNodeWithTag(FabSourceTag).assertExists()
        onNodeWithTag(FabDialogTag).assertExists()
    }

    @Test
    fun reopeningMidCloseEndsOpen() = runComposeUiTest {
        mainClock.autoAdvance = false
        var open by mutableIntStateOf(0)
        setContent {
            LabTheme {
                ProfileMorphs(
                    "Profile",
                    "Alex Morgan",
                    Res.drawable.portrait,
                    "Cor",
                    candidates,
                    open,
                    {},
                    {},
                )
            }
        }
        mainClock.advanceTimeBy(500)
        for (value in listOf(3, 0, 3)) {
            runOnUiThread { open = value }
            Snapshot.sendApplyNotifications()
            mainClock.advanceTimeBy(60)
        }
        mainClock.advanceTimeBy(1_500)
        onNodeWithTag(FabDialogTag).assertExists()
    }

    @Test
    fun outOfRangeIndexShowsTheBaseScreen() = runComposeUiTest {
        setContent {
            LabTheme {
                ProfileMorphs(
                    "Profile",
                    "Alex Morgan",
                    Res.drawable.portrait,
                    "Cor",
                    candidates,
                    7,
                    {},
                    {},
                )
            }
        }
        onNodeWithTag(AvatarSourceTag).assertExists()
        onNodeWithTag(AvatarTargetTag).assertDoesNotExist()
        onNodeWithTag(FabDialogTag).assertDoesNotExist()
    }

    @Test
    fun nothingRecomposesDuringEachMorph() = runComposeUiTest {
        var sources = 0
        var targets = 0
        var open by mutableIntStateOf(0)
        setContent {
            LabTheme {
                CompositionLocalProvider(
                    LocalMorphCompositionProbe provides
                        { end ->
                            if (end == MorphEnd.Card) sources++ else targets++
                        }
                ) {
                    ProfileMorphs(
                        "Profile",
                        "Alex Morgan",
                        Res.drawable.portrait,
                        "Cor",
                        candidates,
                        open,
                        {},
                        {},
                    )
                }
            }
        }
        mainClock.autoAdvance = false
        for (index in
            listOf(1, 3)) { // search is excluded: typing recomposes its child texts by design
            // warm-up cycle, then measure a second open
            for (value in listOf(index, 0)) {
                runOnUiThread { open = value }
                Snapshot.sendApplyNotifications()
                mainClock.advanceTimeBy(1_500)
            }
            waitForIdle()
            runOnUiThread { open = index }
            Snapshot.sendApplyNotifications()
            mainClock.advanceTimeBy(32)
            val afterOpen = sources to targets
            mainClock.advanceTimeBy(1_500)
            assertEquals(afterOpen, sources to targets, "morph $index recomposed while animating")
            runOnUiThread { open = 0 }
            Snapshot.sendApplyNotifications()
            mainClock.advanceTimeBy(1_500)
        }
    }
}
