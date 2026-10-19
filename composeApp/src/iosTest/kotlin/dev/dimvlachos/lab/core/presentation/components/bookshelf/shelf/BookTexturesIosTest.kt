package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.BookDimensions
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfBookSpec
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfLighting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.layoutShelf
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.shelfBookStyle
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Typeface
import kotlin.test.Test
import kotlin.test.assertTrue

class BookTexturesIosTest {
    // Skia copies a changeable bitmap into a new image every time it is drawn, and the shelf draws
    // its spines, faces and light every frame, so each baked texture must be frozen.
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun bakedTexturesAreFrozen() = runComposeUiTest {
        lateinit var measurer: TextMeasurer
        setContent { measurer = rememberTextMeasurer() }
        waitForIdle()
        val serif = Typeface(FontFamily.Serif, measurer)
        val kit = ShelfKit(ShelfFonts(serif, serif, serif, serif), ShelfNoise.create())
        val book =
            ShelfBookVisual(7, "The Salt Road", "June Whitaker", shelfBookStyle(7, "#2E5E6B"))
        val dims = BookDimensions(9f, 60f, 40f)
        val spine = createSpineBitmap(book, dims.thickness, dims.height, 3f, kit)
        val shell = createBookShell(book, dims, 3f, spine, null, kit)
        val light =
            bakeShelfLight(
                layoutShelf(412f, 640f, listOf((1..6).map { ShelfBookSpec(it, 30f, 210f, 140f) })),
                ShelfLighting.Day,
            )

        fun frozen(what: String, image: ImageBitmap) =
            assertTrue(image.asSkiaBitmap().isImmutable, "$what is still changeable")

        frozen("the spine", spine)
        shell.faces.forEach { (face, bitmap) -> frozen("the $face face", bitmap) }
        frozen("the ribbon band", createRibbonBand(dims, 3f))
        frozen("the light", light.light)
        frozen("the glow", light.glow)
        frozen("the leaves", light.leaves)
        frozen("the weave", kit.noise.weave)
    }
}
