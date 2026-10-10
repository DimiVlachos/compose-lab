package dev.dimvlachos.lab.popupdemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PopUpDemoUiTest {
    @Test
    fun theTourStartsOnTheShutCoverWithAnOpenButton() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { LabTheme { PopUpDemo(DemoState()) } }
        mainClock.advanceTimeBy(500)
        onNodeWithText("A pop-up guide to the islands. Open the cover to begin.")
            .assertIsDisplayed()
        onNodeWithText("Open").assertHasClickAction()
    }

    @Test
    fun openingTheCoverShowsTheFirstSpreadsCaption() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { LabTheme { PopUpDemo(DemoState()) } }
        mainClock.advanceTimeBy(500)
        onNodeWithText("Open").performClick()
        mainClock.advanceTimeBy(3_000)
        onNodeWithText("Find your island").assertIsDisplayed()
        onNodeWithText("Next").assertHasClickAction()
    }

    @Test
    fun theScriptsSelectTurnsTheBook() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        setContent { LabTheme { PopUpDemo(state) } }
        mainClock.advanceTimeBy(500)
        state.select(2)
        mainClock.advanceTimeBy(4_000)
        onNodeWithText("Plan the crossing").assertIsDisplayed()
    }

    @Test
    fun theEndCaptionGoesOnceTheBookTurnsBack() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        setContent { LabTheme { PopUpDemo(state) } }
        mainClock.advanceTimeBy(500)
        onNodeWithText("Skip").performClick()
        mainClock.advanceTimeBy(4_000)
        onNodeWithText("Καλό ταξίδι!").assertIsDisplayed()
        // The tour plays on and shuts the book: the cover's caption is back.
        state.select(3)
        mainClock.advanceTimeBy(100)
        state.select(0)
        mainClock.advanceTimeBy(5_000)
        onNodeWithText("A pop-up guide to the islands. Open the cover to begin.")
            .assertIsDisplayed()
    }

    @Test
    fun theCaptionAndTheBookStayPutAsTheCaptionChanges() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        // A phone's width, so the longer captions wrap onto more lines than the short ones.
        setContent { LabTheme { Box(Modifier.size(360.dp, 760.dp)) { PopUpDemo(state) } } }
        mainClock.advanceTimeBy(500)
        state.select(1)
        mainClock.advanceTimeBy(3_000)
        val first = onNodeWithText("Find your island").getUnclippedBoundsInRoot().top
        state.select(3)
        mainClock.advanceTimeBy(4_000)
        val last = onNodeWithText("Ride the meltemi").getUnclippedBoundsInRoot().top
        assertEquals(first, last)
    }

    @Test
    fun readAgainGoesStraightToTheCoversCaption() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { LabTheme { PopUpDemo(DemoState()) } }
        mainClock.advanceTimeBy(500)
        onNodeWithText("Skip").performClick()
        mainClock.advanceTimeBy(4_000)
        onNodeWithText("Read again").performClick()
        // While the leaves turn back, the caption is the cover's, never a spread passed on the way.
        repeat(12) {
            mainClock.advanceTimeBy(250)
            onNodeWithText("A pop-up guide to the islands. Open the cover to begin.")
                .assertIsDisplayed()
            onAllNodesWithText("Ride the meltemi").assertCountEquals(0)
            onAllNodesWithText("Plan the crossing").assertCountEquals(0)
            onAllNodesWithText("Find your island").assertCountEquals(0)
        }
    }

    @Test
    fun skipGoesStraightToTheEndCaption() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { LabTheme { PopUpDemo(DemoState()) } }
        mainClock.advanceTimeBy(500)
        onNodeWithText("Skip").performClick()
        repeat(12) {
            mainClock.advanceTimeBy(250)
            onNodeWithText("Καλό ταξίδι!").assertIsDisplayed()
            onAllNodesWithText("Find your island").assertCountEquals(0)
            onAllNodesWithText("Plan the crossing").assertCountEquals(0)
        }
    }

    @Test
    fun theBookLiesOnASunlitSurfaceNotOnBlack() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            LabTheme {
                Box(Modifier.size(360.dp, 760.dp).testTag("screen")) { PopUpDemo(DemoState()) }
            }
        }
        mainClock.advanceTimeBy(500)
        val pixels = onNodeWithTag("screen").captureToImage().toPixelMap()
        // Beside the book, and down by the buttons.
        for ((x, y) in listOf(6 to pixels.height / 3, pixels.width / 2 to pixels.height - 30)) {
            val c = pixels[x, y]
            val light = (c.red + c.green + c.blue) / 3f
            assertTrue(light > 0.75f, "($x, $y) is $c")
        }
    }

    @Test
    fun theSkyFollowsTheSpreadFromMiddayBlueToSunsetWhileTheCaptionStaysLight() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        setContent {
            LabTheme { Box(Modifier.size(360.dp, 760.dp).testTag("screen")) { PopUpDemo(state) } }
        }
        mainClock.advanceTimeBy(500)
        fun sky() = onNodeWithTag("screen").captureToImage().toPixelMap().let { it[6, 6] }
        fun caption() =
            onNodeWithTag("screen").captureToImage().toPixelMap().let {
                it[it.width / 2, it.height - 30]
            }
        state.select(1)
        mainClock.advanceTimeBy(4_000)
        val midday = sky()
        assertTrue(midday.blue > midday.red + 0.05f, "midday sky $midday")
        state.select(3)
        mainClock.advanceTimeBy(5_000)
        val sunset = sky()
        assertTrue(sunset.red > sunset.blue, "sunset sky $sunset")
        val light = caption()
        assertTrue((light.red + light.green + light.blue) / 3f > 0.85f, "caption area $light")
    }

    @Test
    fun theCaptionDissolvesIntoTheNextInsteadOfSnapping() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { LabTheme { PopUpDemo(DemoState()) } }
        mainClock.advanceTimeBy(500)
        // How much ink the caption shows, frame by frame.
        fun ink(): Float {
            val pixels = onNodeWithTag(CaptionTag).captureToImage().toPixelMap()
            var sum = 0f
            for (x in 0 until pixels.width step 3) for (y in 0 until pixels.height step 3) {
                val c = pixels[x, y]
                sum += 1f - (c.red + c.green + c.blue) / 3f
            }
            return sum
        }
        val before = ink()
        onNodeWithText("Open").performClick()
        val seen = mutableListOf<Float>()
        repeat(60) {
            mainClock.advanceTimeBy(33)
            seen += ink()
        }
        val after = ink()
        // A snap shows the old caption, then the new: no frame unlike both. A dissolve shows
        // many, as the two blend.
        val blended = seen.count { abs(it - before) > 0.5f && abs(it - after) > 0.5f }
        assertTrue(blended >= 5, "no dissolve between $before and $after: $seen")
        onNodeWithText("Beaches, villages and chapels on 24 inhabited islands.").assertIsDisplayed()
    }

    @Test
    fun theButtonGrowsSmoothlyToItsNewLabel() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        setContent { LabTheme { PopUpDemo(state) } }
        mainClock.advanceTimeBy(500)
        state.select(2)
        mainClock.advanceTimeBy(4_000)
        fun width() = onNodeWithTag("popup.next").getUnclippedBoundsInRoot().width
        val before = width()
        onNodeWithTag("popup.next").performClick()
        val seen = mutableListOf<androidx.compose.ui.unit.Dp>()
        repeat(60) {
            mainClock.advanceTimeBy(33)
            seen += width()
        }
        val after = width()
        assertTrue(after > before, "Start exploring is wider than Next: $before -> $after")
        assertTrue(
            seen.any { it > before + 2.dp && it < after - 2.dp },
            "no width in between: $seen",
        )
    }

    @Test
    fun readAgainRightAfterSkipSettlesOnTheCover() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { LabTheme { PopUpDemo(DemoState()) } }
        mainClock.advanceTimeBy(500)
        onNodeWithText("Skip").performClick()
        mainClock.advanceTimeBy(300)
        onNodeWithText("Read again").performClick()
        mainClock.advanceTimeBy(8_000)
        onNodeWithText("A pop-up guide to the islands. Open the cover to begin.")
            .assertIsDisplayed()
        onNodeWithText("Open").assertIsDisplayed()
    }

    @Test
    fun afterSkipTheTourStillShowsEachSpreadsCaption() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        setContent { LabTheme { PopUpDemo(state) } }
        mainClock.advanceTimeBy(500)
        onNodeWithText("Skip").performClick()
        mainClock.advanceTimeBy(4_000)
        // The script plays on: back to the start, then on to the windmill.
        state.select(0)
        mainClock.advanceTimeBy(5_000)
        state.select(3)
        mainClock.advanceTimeBy(5_000)
        onNodeWithText("Ride the meltemi").assertIsDisplayed()
    }

    @Test
    fun atTheLargestFontTheCaptionStillStaysPut() = runComposeUiTest {
        mainClock.autoAdvance = false
        val state = DemoState()
        setContent {
            LabTheme {
                val d = androidx.compose.ui.platform.LocalDensity.current
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides
                        androidx.compose.ui.unit.Density(d.density, 2f)
                ) {
                    Box(Modifier.size(360.dp, 760.dp)) { PopUpDemo(state) }
                }
            }
        }
        mainClock.advanceTimeBy(500)
        state.select(1)
        mainClock.advanceTimeBy(3_000)
        val first = onNodeWithText("Find your island").getUnclippedBoundsInRoot().top
        state.select(3)
        mainClock.advanceTimeBy(4_000)
        val last = onNodeWithText("Ride the meltemi").getUnclippedBoundsInRoot().top
        assertEquals(first, last)
    }

    @Test
    fun inLandscapeTheBookAndTheCaptionSitSideBySide() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { LabTheme { Box(Modifier.size(760.dp, 360.dp)) { PopUpDemo(DemoState()) } } }
        mainClock.advanceTimeBy(500)
        val book = onNodeWithTag(BookTag).getUnclippedBoundsInRoot()
        val text =
            onNodeWithText("A pop-up guide to the islands. Open the cover to begin.")
                .getUnclippedBoundsInRoot()
        assertTrue(
            book.right <= text.left || book.bottom <= text.top,
            "book $book overlaps caption $text",
        )
        assertTrue(book.bottom <= 360.dp && book.right <= 760.dp, "book $book spills out")
        // And it is still a book worth looking at, not squeezed into what the caption leaves.
        assertTrue(book.bottom - book.top >= 240.dp, "book $book is too small")
    }

    @Test
    fun aQuickNextAndBackFadeTheSkyWithoutAJump() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent {
            LabTheme {
                Box(Modifier.size(360.dp, 760.dp).testTag("screen")) { PopUpDemo(DemoState()) }
            }
        }
        mainClock.advanceTimeBy(500)
        onNodeWithText("Open").performClick()
        mainClock.advanceTimeBy(3_000)
        onNodeWithText("Next").performClick()
        mainClock.advanceTimeBy(3_000)
        fun sky() = onNodeWithTag("screen").captureToImage().toPixelMap().let { it[6, 6] }
        // From the crossing's blue towards the meltemi's sunset, and back before it gets there.
        onNodeWithText("Next").performClick()
        var last = sky()
        var worst = 0f
        repeat(80) { frame ->
            if (frame == 30) onNodeWithText("Back").performClick()
            mainClock.advanceTimeByFrame()
            val now = sky()
            worst =
                maxOf(
                    worst,
                    abs(now.red - last.red),
                    abs(now.green - last.green),
                    abs(now.blue - last.blue),
                )
            last = now
        }
        assertTrue(worst < 0.06f, "the sky jumped by $worst in one frame")
    }
}
