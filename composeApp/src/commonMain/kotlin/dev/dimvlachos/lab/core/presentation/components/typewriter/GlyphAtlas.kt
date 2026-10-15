package dev.dimvlachos.lab.core.presentation.components.typewriter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.special_elite
import kotlin.math.ceil
import org.jetbrains.compose.resources.Font

/**
 * The typewriter's type: each letter drawn the once, in [ink], into a small picture of its own, to
 * draw from wherever it is struck, turned and as dark as it came out. Set from the layout every
 * frame, each letter would be shaped again; drawn from its picture it costs a copy.
 */
public class GlyphAtlas
internal constructor(
    private val measurer: TextMeasurer,
    private val style: TextStyle,
    private val ink: Color,
) {
    private class Picture(val image: ImageBitmap, val baseline: Float)

    // The pictures of the plain letters, by character code, so a frame looks each up without
    // boxing it; anything past them, by character.
    private val plain = arrayOfNulls<Picture>(PlainChars)
    private val drawnPlain = BooleanArray(PlainChars)
    private val others = mutableMapOf<Char, Picture?>()

    private fun DrawScope.pictureOf(char: Char): Picture? {
        val code = char.code
        if (code >= PlainChars) {
            // A character with no picture is remembered as none, so it isn't measured again.
            if (char !in others) others[char] = drawn(char)
            return others[char]
        }
        if (!drawnPlain[code]) {
            plain[code] = drawn(char)
            drawnPlain[code] = true
        }
        return plain[code]
    }

    /**
     * Draws [char] with its middle at [x] and its foot on [baseline], turned [tilt] degrees about
     * its middle, at [alpha].
     */
    internal fun DrawScope.drawGlyph(
        char: Char,
        x: Float,
        baseline: Float,
        tilt: Float,
        alpha: Float,
    ) {
        if (alpha <= 0f) return
        val picture = pictureOf(char) ?: return
        val image = picture.image
        val left = x - image.width / 2f
        val top = baseline - picture.baseline
        val pivot = Offset(x, baseline - picture.baseline / 3f)
        // Drawn where it falls, between px, so it moves smoothly with the paper under it.
        rotate(tilt, pivot) {
            drawImage(image, topLeft = Offset(left, top), alpha = alpha.coerceIn(0f, 1f))
        }
    }

    private fun DrawScope.drawn(char: Char): Picture? {
        if (char.isWhitespace()) return null
        val layout = measurer.measure(char.toString(), style)
        val size = layout.size
        if (size.width <= 0 || size.height <= 0) return null
        // A little room either side, for a letter whose ink runs past its advance.
        val pad = ceil(size.height * Overhang).toInt()
        val image = ImageBitmap(size.width + pad * 2, size.height)
        CanvasDrawScope().draw(
            this,
            layoutDirection,
            Canvas(image),
            Size(image.width.toFloat(), image.height.toFloat()),
        ) {
            drawText(layout, color = ink, topLeft = Offset(pad.toFloat(), 0f))
        }
        return Picture(image, layout.firstBaseline)
    }

    private companion object {
        const val Overhang = 0.15f
        const val PlainChars = 128
    }
}

/**
 * A [GlyphAtlas] in the typewriter's face, its letters [unit] machine units of
 * [FontSize][TypewriterDimens.FontSize] tall, in the theme's ink.
 */
@Composable
public fun rememberGlyphAtlas(unit: Dp): GlyphAtlas {
    val font = Font(Res.font.special_elite)
    val family = remember(font) { FontFamily(font) }
    val measurer = rememberTextMeasurer(cacheSize = 0)
    val ink = LabTheme.colors.typeInk
    val density = LocalDensity.current
    val size = with(density) { (unit * TypewriterDimens.FontSize).toSp() }
    return remember(family, measurer, ink, size) {
        GlyphAtlas(measurer, TextStyle(fontFamily = family, fontSize = size), ink)
    }
}
