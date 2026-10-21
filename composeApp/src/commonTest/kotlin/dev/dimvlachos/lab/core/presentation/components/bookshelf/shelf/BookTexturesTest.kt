package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Binding
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.BookDimensions
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.FaceTexture
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.relativeLuminance
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.shelfBookStyle
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Bitmap
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Typeface
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.getPixels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BookTexturesTest {
    private fun withKit(block: (ShelfKit) -> Unit) = runComposeUiTest {
        lateinit var measurer: TextMeasurer
        setContent { measurer = rememberTextMeasurer() }
        runOnIdle {
            val serif = Typeface(FontFamily.Serif, measurer)
            block(
                ShelfKit(
                    ShelfFonts(
                        serif = serif,
                        serifItalic =
                            Typeface(FontFamily.Serif, measurer, style = FontStyle.Italic),
                        sans = Typeface(FontFamily.SansSerif, measurer, FontWeight.Medium),
                        sansBold = Typeface(FontFamily.SansSerif, measurer, FontWeight.Bold),
                    ),
                    ShelfNoise.create(),
                ),
            )
        }
    }

    private fun book(binding: Binding): ShelfBookVisual {
        val id = (1..200).first { shelfBookStyle(it, "#2E5E6B").binding == binding }
        return ShelfBookVisual(id, "The Salt Road", "June Whitaker", shelfBookStyle(id, "#2E5E6B"))
    }

    private fun Bitmap.pixels() =
        IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }

    private fun luminance(argb: Int): Float {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
    }

    private fun Bitmap.columnLuminance(x: Int): Float {
        val px = pixels()
        return (0 until height).map { luminance(px[it * width + x]) }.average().toFloat()
    }

    private val dims = BookDimensions(thickness = 32f, height = 220f, depth = 145f)

    @Test
    fun aClothSpineIsDarkWithGiltLettering() = withKit { kit ->
        val spine = createSpineBitmap(book(Binding.Cloth), dims.thickness, dims.height, 3f, kit)
        // As the style darkens cloth: WCAG relative luminance, linear light.
        val lums = spine.pixels().map { relativeLuminance(it and 0xFFFFFF) }.sorted()

        assertTrue(lums[lums.size / 2] <= 0.1f, "cloth median ${lums[lums.size / 2]}")
        assertTrue(lums.last() > 0.3f, "gilt ${lums.last()}")
    }

    @Test
    fun aPaperSpineIsLight() = withKit { kit ->
        val spine = createSpineBitmap(book(Binding.Paper), dims.thickness, dims.height, 3f, kit)
        val lums = spine.pixels().map(::luminance).sorted()

        assertTrue(lums[lums.size / 2] > 0.5f, "paper median ${lums[lums.size / 2]}")
    }

    @Test
    fun theSameBookAlwaysBakesTheSameSpine() = withKit { kit ->
        val visual = book(Binding.Cloth)
        val a = createSpineBitmap(visual, dims.thickness, dims.height, 3f, kit).pixels()
        val b = createSpineBitmap(visual, dims.thickness, dims.height, 3f, kit).pixels()

        assertTrue(a.contentEquals(b))
    }

    @Test
    fun eachPageDarkensTowardItsSpine() = withKit { kit ->
        val metrics = PageMetrics(16f, 23f, 11f, 0.145f)
        fun page(right: Boolean) =
            createPageFace(
                book(Binding.Paper),
                dims,
                3f,
                PageContent.Blank,
                right,
                metrics,
                null,
                kit,
            )
        val left = page(right = false)
        val right = page(right = true)

        assertTrue(left.columnLuminance(left.width - 2) < left.columnLuminance(left.width / 2))
        assertTrue(right.columnLuminance(1) < right.columnLuminance(right.width / 2))
    }

    @Test
    fun aTitlePageCarriesItsTitleInInk() = withKit { kit ->
        val metrics = PageMetrics(16f, 23f, 11f, 0.145f)
        val page =
            createPageFace(
                book(Binding.Paper),
                dims,
                3f,
                PageContent.TitlePage("The Salt Road", "June Whitaker", null),
                rightSide = true,
                metrics,
                null,
                kit,
            )

        assertTrue(page.pixels().any { luminance(it) < 0.35f })
    }

    @Test
    fun aBookShellHasEveryFace() = withKit { kit ->
        val visual = book(Binding.Cloth)
        val spine = createSpineBitmap(visual, dims.thickness, dims.height, 3f, kit)
        val shell = createBookShell(visual, dims, 3f, spine, cover = null, kit)

        assertEquals(
            setOf(
                FaceTexture.Spine,
                FaceTexture.Cover,
                FaceTexture.Back,
                FaceTexture.Head,
                FaceTexture.Fore,
            ),
            shell.faces.keys,
        )
    }
}
