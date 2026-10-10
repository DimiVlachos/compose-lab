package dev.dimvlachos.lab.core.presentation.components.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PopUpBookUiTest {
    // Flat colours, so every pixel says what it shows: a red cover, white near pages, grey far
    // pages, and one blue piece 100 wide and 100 tall standing at the gutter of the first spread.
    private val piece =
        PopUpPiece(
            art = ColorPainter(Color.Blue),
            side = PopUpSide.Near,
            fromGutter = 0f,
            x = -50f,
            width = 100f,
            height = 100f,
        )
    private val spreads =
        listOf(
            PopUpSpread(ColorPainter(Color.White), ColorPainter(Color.Gray), listOf(piece)),
            PopUpSpread(ColorPainter(Color.White), ColorPainter(Color.Gray), emptyList()),
        )

    private var width by mutableStateOf(320)
    private var compositions = 0

    private fun ComposeUiTest.showBook(): PopUpBookState {
        mainClock.autoAdvance = false
        lateinit var state: PopUpBookState
        setContent {
            LabTheme {
                state = rememberPopUpBookState(spreadCount = 2)
                SideEffect { compositions++ }
                Box(Modifier.background(Color.Black)) {
                    PopUpBook(
                        cover = ColorPainter(Color.Red),
                        spreads = spreads,
                        state = state,
                        modifier = Modifier.width(width.dp).testTag(BookTag),
                    )
                }
            }
        }
        mainClock.advanceTimeBy(100)
        return state
    }

    private fun ComposeUiTest.pixels(): PixelMap =
        onNodeWithTag(BookTag).captureToImage().toPixelMap()

    // The colour the eye sees at book point (x, y, z).
    private fun PixelMap.at(x: Float, y: Float, z: Float): Color {
        val seen = BookCamera(width.toFloat(), height.toFloat()).project(Vec3(x, y, z))
        return this[seen.x.toInt(), seen.y.toInt()]
    }

    private fun Color.isBlue() = blue > 0.5f && blue > red + 0.3f && blue > green + 0.3f

    private fun Color.brightness() = (red + green + blue) / 3f

    private fun ComposeUiTest.open(state: PopUpBookState) {
        state.next()
        repeat(240) { mainClock.advanceTimeByFrame() }
    }

    @Test
    fun shutNothingStandsAboveTheCover() = runComposeUiTest {
        showBook()
        assertEquals(Color.Black, pixels().at(0f, 0f, 50f))
    }

    @Test
    fun shutTheCoverShows() = runComposeUiTest {
        showBook()
        val cover = pixels().at(0f, -100f, 0f)
        assertTrue(cover.red > 0.5f && cover.green < 0.2f, "$cover")
    }

    @Test
    fun openTheBluePieceStandsAboveTheGutter() = runComposeUiTest {
        val state = showBook()
        open(state)
        assertEquals(1, state.spread)
        val seen = pixels().at(0f, 0f, 50f)
        assertTrue(seen.isBlue(), "$seen")
    }

    @Test
    fun aShadowDarkensThePageBehindAPiece() = runComposeUiTest {
        val state = showBook()
        open(state)
        val pixels = pixels()
        val shaded = pixels.at(65f, 40f, 0f)
        val clear = pixels.at(65f, 150f, 0f)
        assertTrue(shaded.brightness() < clear.brightness() - 0.05f, "$shaded vs $clear")
    }

    @Test
    fun aTapOnTheNearHalfTurnsForward() = runComposeUiTest {
        val state = showBook()
        onNodeWithTag(BookTag).performTouchInput {
            val camera = BookCamera(this.width.toFloat(), this.height.toFloat())
            click(camera.project(Vec3(0f, -120f, 0f)))
        }
        repeat(240) { mainClock.advanceTimeByFrame() }
        assertEquals(1, state.spread)
    }

    @Test
    fun aNewSizeRepaintsWithoutLosingTheTurn() = runComposeUiTest {
        val state = showBook()
        open(state)
        width = 380
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        assertEquals(1, state.spread)
        assertTrue(pixels().at(0f, 0f, 50f).isBlue())
    }

    @Test
    fun nothingRecomposesWhileTurning() = runComposeUiTest {
        val state = showBook()
        val before = compositions
        state.next()
        repeat(60) { mainClock.advanceTimeByFrame() }
        assertEquals(before, compositions)
    }

    @Test
    fun aPieceSeenFromBehindIsWhitePaperWithItsPrintShowingThrough() = runComposeUiTest {
        // The cover 60° up: the eye looks into the spread, at the back of a piece still folded low.
        val piece =
            PopUpPiece(
                ColorPainter(Color.Blue),
                PopUpSide.Near,
                fromGutter = 20f,
                x = -100f,
                width = 200f,
                height = 150f,
            )
        val spread =
            listOf(PopUpSpread(ColorPainter(Color.Gray), ColorPainter(Color.Gray), listOf(piece)))
        val state = PopUpBookState(spreadCount = 1)
        val cover = 60f * kotlin.math.PI.toFloat() / 180f
        state.angles[0] = cover
        mainClock.autoAdvance = false
        setContent {
            LabTheme {
                Box(Modifier.background(Color.Black)) {
                    PopUpBook(
                        ColorPainter(Color.Red),
                        spread,
                        state,
                        Modifier.width(width.dp).testTag(BookTag),
                    )
                }
            }
        }
        mainClock.advanceTimeByFrame()
        assertTrue(!PopUpMath.pieceFrontSeen(near = 0f, far = cover))
        val up = PopUpMath.up(near = 0f, far = cover)
        val base = PopUpMath.base(piece, near = 0f, far = cover, x = 0f)
        val seen = pixels().at(0f, base.y + up.y * 75f, base.z + up.z * 75f)
        // Light paper, not the sand of a page's edge, with a cool trace of the blue print.
        assertTrue(seen.brightness() > 0.7f, "$seen")
        assertTrue(seen.blue > seen.red, "$seen")
    }

    private companion object {
        const val BookTag = "popup"
    }
}
