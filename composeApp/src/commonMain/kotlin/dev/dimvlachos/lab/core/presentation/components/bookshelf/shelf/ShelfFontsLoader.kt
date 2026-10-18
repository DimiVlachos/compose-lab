package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.dimvlachos.lab.core.presentation.components.bookshelf.book.BookPageSetting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.RectF
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Typeface
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.instrument_sans_bold
import dev.dimvlachos.lab.resources.instrument_sans_medium
import dev.dimvlachos.lab.resources.instrument_serif_italic
import dev.dimvlachos.lab.resources.instrument_serif_regular
import kotlinx.coroutines.CoroutineDispatcher
import org.jetbrains.compose.resources.Font

/**
 * Where everything that sets text for the shelf is baked, off the main thread: several at once
 * where the platform's font machinery can be entered from several threads, one at a time where it
 * cannot.
 */
internal val ShelfTextBakers: CoroutineDispatcher = shelfTextBakers()

internal expect fun shelfTextBakers(): CoroutineDispatcher

/**
 * The shelf's four faces, Instrument Serif and Sans. Textures are baked on background threads, so
 * their text is measured by a measurer that keeps no cache to share between them.
 */
@Composable
internal fun rememberShelfFonts(): ShelfFonts {
    val serif = FontFamily(Font(Res.font.instrument_serif_regular))
    val serifItalic = FontFamily(Font(Res.font.instrument_serif_italic, style = FontStyle.Italic))
    val sans = FontFamily(Font(Res.font.instrument_sans_medium, weight = FontWeight.Medium))
    val sansBold = FontFamily(Font(Res.font.instrument_sans_bold, weight = FontWeight.Bold))
    val resolver = LocalFontFamilyResolver.current
    val measurer =
        remember(resolver) {
            TextMeasurer(resolver, Density(1f), LayoutDirection.Ltr, cacheSize = 0)
        }
    return remember(serif, serifItalic, sans, sansBold, measurer) {
        ShelfFonts(
            serif = Typeface(serif, measurer),
            serifItalic = Typeface(serifItalic, measurer, style = FontStyle.Italic),
            sans = Typeface(sans, measurer, FontWeight.Medium),
            sansBold = Typeface(sansBold, measurer, FontWeight.Bold),
        )
    }
}

/** How the open book's pages are set, for a page [page] dp in size, or null before it is known. */
@Composable
internal fun rememberBookPageSetting(page: RectF?): BookPageSetting? {
    val density = LocalDensity.current
    val fonts = rememberShelfFonts()
    val body = ShelfTheme.typography.bookPage
    val caption = ShelfTheme.typography.bookPageCaption
    val key = page?.let { listOf(it.left, it.top, it.right, it.bottom) }
    return remember(key, density, fonts) {
        page ?: return@remember null
        with(density) {
            BookPageSetting(
                pageWidth = page.width() * this.density,
                pageHeight = page.height() * this.density,
                textSize = body.fontSize.toPx(),
                lineHeight = body.lineHeight.toPx(),
                captionSize = caption.fontSize.toPx(),
                captionSpacingEm = caption.letterSpacing.value / caption.fontSize.value,
                serif = fonts.serif.family,
                measurer = fonts.serif.measurer,
                density = density,
            )
        }
    }
}
