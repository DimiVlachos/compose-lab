package dev.dimvlachos.lab.core.presentation.components.pageturn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PageTurnBookUiTest {
    private val red = Color.Red
    private val blue = Color.Blue
    private val green = Color.Green
    private val yellow = Color.Yellow

    // Each spread is two flat colours, one per page, so every pixel says which page it shows.
    private fun spread(left: Color, right: Color): ImageBitmap {
        val image = ImageBitmap(360, 180)
        val canvas = Canvas(image)
        canvas.drawRect(Rect(0f, 0f, 180f, 180f), Paint().apply { color = left })
        canvas.drawRect(Rect(180f, 0f, 360f, 180f), Paint().apply { color = right })
        return image
    }

    private fun ComposeUiTest.showBook(): PageTurnState {
        mainClock.autoAdvance = false
        lateinit var state: PageTurnState
        val spreads = listOf(spread(red, blue), spread(green, yellow))
        setContent {
            LabTheme {
                state = rememberPageTurnState(spreadCount = 2)
                Box(Modifier.size(440.dp, 260.dp), contentAlignment = Alignment.Center) {
                    PageTurnBook(
                        spreads,
                        state,
                        Modifier.fillMaxWidth().padding(20.dp).testTag(BookTag),
                    )
                }
            }
        }
        mainClock.advanceTimeBy(100)
        return state
    }

    // The colour across the book at [xFraction] of its width, half way down.
    private fun ComposeUiTest.colorAt(xFraction: Float): Color {
        val pixels = onNodeWithTag(BookTag).captureToImage().toPixelMap()
        return pixels[(pixels.width * xFraction).toInt(), pixels.height / 2]
    }

    private fun assertShows(expected: Color, actual: Color, where: String) {
        // The leaf's shade darkens its colour; the hue still has to be the page's.
        val dominant = listOf(expected.red, expected.green, expected.blue)
        val seen = listOf(actual.red, actual.green, actual.blue)
        assertEquals(dominant.indexOf(dominant.max()), seen.indexOf(seen.max()), "$where: $actual")
        if (expected == yellow)
            assertTrue(actual.red > 0.5f && actual.green > 0.5f, "$where: $actual")
    }

    @Test
    fun theOpenSpreadLiesAcrossBothPages() = runComposeUiTest {
        showBook()
        assertShows(red, colorAt(0.25f), "left page")
        assertShows(blue, colorAt(0.75f), "right page")
    }

    @Test
    fun aTapOnTheRightPageTurnsToTheNextSpreadAndTheLeftTurnsBack() = runComposeUiTest {
        val state = showBook()
        onNodeWithTag(BookTag).performTouchInput { click(Offset(width * 0.8f, height / 2f)) }
        mainClock.advanceTimeBy(3_000)
        assertEquals(1, state.spread)
        assertShows(green, colorAt(0.25f), "left page")
        assertShows(yellow, colorAt(0.75f), "right page")
        onNodeWithTag(BookTag).performTouchInput { click(Offset(width * 0.2f, height / 2f)) }
        mainClock.advanceTimeBy(3_000)
        assertEquals(0, state.spread)
    }

    @Test
    fun swipesTurnThePageBothWays() = runComposeUiTest {
        val state = showBook()
        onNodeWithTag(BookTag).performTouchInput { swipeLeft() }
        mainClock.advanceTimeBy(3_000)
        assertEquals(1, state.spread)
        onNodeWithTag(BookTag).performTouchInput { swipeRight() }
        mainClock.advanceTimeBy(3_000)
        assertEquals(0, state.spread)
    }

    @Test
    fun aCornerPulledAwayOnASlantTurnsThePage() = runComposeUiTest {
        val state = showBook()
        // From near the top corner, down and across at 45 degrees.
        onNodeWithTag(BookTag).performTouchInput {
            swipe(
                start = Offset(width * 0.9f, height * 0.12f),
                end = Offset(width * 0.3f, height * 0.12f + width * 0.6f),
                durationMillis = 300,
            )
        }
        mainClock.advanceTimeBy(3_000)
        assertEquals(1, state.spread)
    }

    @Test
    fun aNearlyUprightDragLeavesThePage() = runComposeUiTest {
        val state = showBook()
        onNodeWithTag(BookTag).performTouchInput {
            swipe(
                start = Offset(width * 0.8f, height * 0.1f),
                end = Offset(width * 0.75f, height * 0.9f),
                durationMillis = 300,
            )
        }
        mainClock.advanceTimeBy(3_000)
        assertEquals(0, state.spread)
        assertEquals(false, state.isTurning)
    }

    @Test
    fun earlyInATurnTheLeafStillCoversTheSpineAndUncoversTheNextPage() = runComposeUiTest {
        val state = showBook()
        dragTo(state, progress = 0.25f)
        assertShows(red, colorAt(0.25f), "left page")
        assertShows(blue, colorAt(0.55f), "the leaf's front by the spine")
        assertShows(yellow, colorAt(0.92f), "the next right page, uncovered")
    }

    @Test
    fun lateInATurnTheLeafsBackLiesOverTheLeftPage() = runComposeUiTest {
        val state = showBook()
        dragTo(state, progress = 0.75f)
        assertShows(red, colorAt(0.08f), "this left page, not yet covered")
        assertShows(green, colorAt(0.45f), "the leaf's back by the spine")
        assertShows(yellow, colorAt(0.75f), "the next right page")
    }

    @Test
    fun quickSwipesEachTurnAPage() = runComposeUiTest {
        mainClock.autoAdvance = false
        lateinit var state: PageTurnState
        val spreads = List(4) { spread(red, blue) }
        setContent {
            LabTheme {
                state = rememberPageTurnState(spreadCount = 4)
                Box(Modifier.size(440.dp, 260.dp), contentAlignment = Alignment.Center) {
                    PageTurnBook(
                        spreads,
                        state,
                        Modifier.fillMaxWidth().padding(20.dp).testTag(BookTag),
                    )
                }
            }
        }
        mainClock.advanceTimeBy(100)
        repeat(3) {
            // A short flick: a quarter of the book, let go fast, so the page is still landing.
            onNodeWithTag(BookTag).performTouchInput {
                swipe(
                    start = Offset(width * 0.8f, height / 2f),
                    end = Offset(width * 0.55f, height / 2f),
                    durationMillis = 60,
                )
            }
            // The next swipe comes while this page is still landing.
            mainClock.advanceTimeBy(40)
        }
        mainClock.advanceTimeBy(3_000)
        assertEquals(3, state.spread)
    }

    // Takes the right page by the middle of its outer edge and moves the finger to where that
    // edge lies, seen from above, a [progress] of the way through a turn.
    private fun ComposeUiTest.dragTo(state: PageTurnState, progress: Float) {
        runOnUiThread {
            val layout = state.layout!!
            val spine = layout.left + layout.geometry.spineX
            val y = layout.size.height / 2f
            val reach = 0.95f * layout.geometry.page
            val down = Offset(spine + reach, y)
            state.dragStart(down, down - Offset(10f, 0f))
            state.dragTo(Offset(spine + reach * cos(PI.toFloat() * progress), y))
        }
        mainClock.advanceTimeBy(50)
    }

    private companion object {
        const val BookTag = "book"
    }
}
