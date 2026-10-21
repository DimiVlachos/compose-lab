package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import dev.dimvlachos.lab.core.presentation.canvas2d.imageBitmapOf
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Rgb
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Bitmap
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.BitmapShader
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.BlendMode
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Canvas
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.LinearGradient
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Matrix
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Paint
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Shader
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Typeface
import kotlin.math.sin

private const val GILT_DARK = 0xFF9A7334.toInt()
private const val GILT_LIGHT = 0xFFF1D590.toInt()
private const val GILT_MID = 0xFFC99A4C.toInt()
private const val GILT_PALE = 0xFFF6E2A8.toInt()

internal const val TEXT_FLAGS =
    Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG

internal class ShelfKit(val fonts: ShelfFonts, val noise: ShelfNoise)

internal class ShelfFonts(
    val serif: Typeface,
    val serifItalic: Typeface,
    val sans: Typeface,
    val sansBold: Typeface,
)

internal class ShelfNoise
private constructor(val plaster: Bitmap, val weave: Bitmap, val grain: Bitmap) {
    companion object {
        fun create(): ShelfNoise {
            val random = ShelfRandom(7)
            return ShelfNoise(
                plaster = tile(random, size = 128, amplitude = 26f, lines = 0f),
                weave = tile(random, size = 48, amplitude = 30f, lines = 34f),
                grain = tile(random, size = 96, amplitude = 16f, lines = 0f),
            )
        }

        private fun tile(random: ShelfRandom, size: Int, amplitude: Float, lines: Float): Bitmap {
            val pixels = IntArray(size * size)
            for (i in pixels.indices) {
                val x = i % size
                val y = i / size
                var v = 128f + (random.next() - 0.5f) * amplitude
                if (lines > 0f) {
                    v +=
                        (sin(x * 1.9f) * 0.5f + 0.5f) * lines * (random.next() * 0.6f + 0.4f) -
                            (sin(y * 1.7f + 1f) * 0.5f + 0.5f) * lines * 0.6f
                }
                val c = v.toInt().coerceIn(0, 255)
                pixels[i] = (0xFF shl 24) or (c shl 16) or (c shl 8) or c
            }
            return imageBitmapOf(pixels, size, size)
        }
    }
}

internal class ShelfRandom(seed: Long) {
    private var state = seed

    fun next(): Float {
        state = (state * 16807L) % 2147483647L
        return state / 2147483647f
    }
}

internal fun Canvas.texture(
    tile: Bitmap,
    worldSize: Float,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    alpha: Float,
    mode: BlendMode = BlendMode.OVERLAY,
) {
    val shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    shader.setLocalMatrix(
        Matrix().apply { setScale(worldSize / tile.width, worldSize / tile.width) },
    )
    val paint =
        Paint().apply {
            this.shader = shader
            blendMode = mode
            this.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
        }
    drawRect(left, top, right, bottom, paint)
}

internal fun giltShader(x0: Float, y0: Float, x1: Float, y1: Float): Shader =
    LinearGradient(
        x0,
        y0,
        x1,
        y1,
        intArrayOf(GILT_DARK, GILT_LIGHT, GILT_MID, GILT_PALE),
        floatArrayOf(0f, 0.35f, 0.55f, 1f),
        Shader.TileMode.CLAMP,
    )

internal fun argb(rgb: Int, alpha: Float = 1f): Int =
    ((alpha.coerceIn(0f, 1f) * 255f).toInt() shl 24) or (rgb and 0xFFFFFF)

internal fun shade(rgb: Int, factor: Float): Int = Rgb.of(rgb).times(factor).toArgb()

internal fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }

internal fun multiplyPaint(color: Int) =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        blendMode = BlendMode.MULTIPLY
    }

internal fun verticalGradient(top: Float, bottom: Float, vararg stops: Pair<Float, Int>): Shader =
    LinearGradient(
        0f,
        top,
        0f,
        bottom,
        stops.map { it.second }.toIntArray(),
        stops.map { it.first }.toFloatArray(),
        Shader.TileMode.CLAMP,
    )

internal fun horizontalGradient(left: Float, right: Float, vararg stops: Pair<Float, Int>): Shader =
    LinearGradient(
        left,
        0f,
        right,
        0f,
        stops.map { it.second }.toIntArray(),
        stops.map { it.first }.toFloatArray(),
        Shader.TileMode.CLAMP,
    )

internal fun black(alpha: Float) = argb(0x000000, alpha)

internal fun white(alpha: Float) = argb(0xFFFFFF, alpha)
