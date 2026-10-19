package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import dev.dimvlachos.lab.core.presentation.canvas2d.imageBitmapOf
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Point2
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfLayout
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfLighting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.mix
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.smoothStep
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Bitmap
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.BlurMaskFilter
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Canvas
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Paint
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Path
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.createBitmap
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.getPixels
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.recycle
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.toDegrees
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.withSave
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

internal const val LIGHT_CELL = 4f
private const val SIGMA_PER_RADIUS = 0.57735f
private const val SUN_GAIN = 0.6f
internal const val SUN_SHADOW_DX = 52f
internal const val SUN_SHADOW_DY = 64f

internal class ShelfLightMaps(val light: Bitmap, val glow: Bitmap, val leaves: Bitmap) {
    val width: Float
        get() = light.width * LIGHT_CELL

    val height: Float
        get() = light.height * LIGHT_CELL
}

internal class ShadowLift(val bookId: Int, val maps: ShelfLightMaps, val restore: Bitmap)

internal fun bakeShelfLight(layout: ShelfLayout, lighting: ShelfLighting): ShelfLightMaps {
    val columns = ceil(layout.width / LIGHT_CELL).toInt() + 1
    val rows = ceil(layout.height / LIGHT_CELL).toInt() + 1
    val sun = sunMask(layout, lighting, columns, rows, withoutBookId = null)
    val (light, glow) = lightPixels(layout, lighting, sun, columns, rows)
    return ShelfLightMaps(
        light = imageBitmapOf(light, columns, rows),
        glow = imageBitmapOf(glow, columns, rows),
        leaves = leafMap(layout, lighting, sun, columns, rows),
    )
}

internal fun bakeShadowLift(
    layout: ShelfLayout,
    lighting: ShelfLighting,
    full: ShelfLightMaps,
    bookId: Int,
): ShadowLift {
    val columns = full.light.width
    val rows = full.light.height
    val sun = sunMask(layout, lighting, columns, rows, withoutBookId = bookId)
    val (without, _) = lightPixels(layout, lighting, sun, columns, rows)
    val lit = IntArray(columns * rows)
    full.light.getPixels(lit, 0, columns, 0, 0, columns, rows)
    val restore =
        IntArray(lit.size) { i ->
            packRgb(
                ratio(lit[i] shr 16, without[i] shr 16),
                ratio(lit[i] shr 8, without[i] shr 8),
                ratio(lit[i], without[i]),
            )
        }
    return ShadowLift(
        bookId = bookId,
        maps =
            ShelfLightMaps(
                light = imageBitmapOf(without, columns, rows),
                glow = full.glow,
                leaves = full.leaves,
            ),
        restore = imageBitmapOf(restore, columns, rows),
    )
}

private fun ratio(lit: Int, unshadowed: Int): Float =
    255f * (lit and 0xFF) / (unshadowed and 0xFF).coerceAtLeast(1)

private fun lightPixels(
    layout: ShelfLayout,
    lighting: ShelfLighting,
    sun: FloatArray,
    columns: Int,
    rows: Int,
): Pair<IntArray, IntArray> {
    val k = layout.scale
    val rowShade =
        FloatArray(rows) { y ->
            val cy = y * LIGHT_CELL
            var ao = 1f
            for (plank in layout.planks) {
                if (cy > plank.plankY - 34f * k && cy < plank.plankY) {
                    ao -= 0.3f * smoothStep(plank.plankY - 34f * k, plank.plankY - 12f * k, cy)
                }
            }
            ao
        }
    val rowVignette =
        FloatArray(rows) { y ->
            val v = (y * LIGHT_CELL / layout.height - 0.55f).coerceAtLeast(0f) / 0.45f
            0.1f * v * v
        }
    val columnVignette =
        FloatArray(columns) { x ->
            val v = abs(x * LIGHT_CELL / layout.width - 0.5f) * 2f
            0.14f * v * v
        }
    val ambient = lighting.ambient
    val sunColor = lighting.sun * SUN_GAIN
    val glowColor = lighting.glow
    val light = IntArray(columns * rows)
    val glow = IntArray(columns * rows)
    for (y in 0 until rows) {
        val ao = rowShade[y]
        for (x in 0 until columns) {
            val i = y * columns + x
            val s = sun[i]
            val f = ao * (1f - columnVignette[x] - rowVignette[y])
            light[i] =
                packRgb(
                    (ambient.r + sunColor.r * s) * f,
                    (ambient.g + sunColor.g * s) * f,
                    (ambient.b + sunColor.b * s) * f,
                )
            glow[i] = packRgb(glowColor.r * s * ao, glowColor.g * s * ao, glowColor.b * s * ao)
        }
    }
    return light to glow
}

private fun packRgb(r: Float, g: Float, b: Float): Int =
    (0xFF shl 24) or
        (r.toInt().coerceIn(0, 255) shl 16) or
        (g.toInt().coerceIn(0, 255) shl 8) or
        b.toInt().coerceIn(0, 255)

private fun sunMask(
    layout: ShelfLayout,
    lighting: ShelfLighting,
    columns: Int,
    rows: Int,
    withoutBookId: Int?,
): FloatArray {
    val w = layout.width
    val h = layout.height
    val k = layout.scale
    val bitmap = createBitmap(columns, rows)
    val canvas = Canvas(bitmap)
    canvas.drawColor(black(1f))

    val patch = lighting.sunPatch.map { Point2(w * it.x, h * it.y) }
    canvas.softFill(patch, white(1f), 30f * k)
    val leftSlope = (patch[3].x - patch[0].x) / (patch[3].y - patch[0].y)
    val topSlope = (patch[1].y - patch[0].y) / (patch[1].x - patch[0].x)
    val barX = w * 0.66f
    val barW = 11f * k
    canvas.softFill(
        listOf(
            Point2(barX, h * 0.06f),
            Point2(barX + barW, h * 0.06f),
            Point2(barX + barW + leftSlope * h * 0.8f, h * 0.86f),
            Point2(barX + leftSlope * h * 0.8f, h * 0.86f),
        ),
        black(1f),
        9f * k,
    )
    val barY = patch[0].y + (patch[3].y - patch[0].y) * 0.3f
    val barH = 10f * k
    canvas.softFill(
        listOf(
            Point2(-w * 0.2f, barY - w * 0.2f * topSlope),
            Point2(w * 1.3f, barY + w * 1.3f * topSlope),
            Point2(w * 1.3f, barY + barH + w * 1.3f * topSlope),
            Point2(-w * 0.2f, barY + barH - w * 0.2f * topSlope),
        ),
        black(1f),
        9f * k,
    )

    val sx = SUN_SHADOW_DX * k
    val sy = SUN_SHADOW_DY * k
    for (plank in layout.planks) {
        for (slot in plank.slots) {
            if (slot.bookId == withoutBookId) continue
            val top = plank.plankY - slot.height
            canvas.softFill(
                listOf(
                    Point2(slot.x + 1f, top + 1f),
                    Point2(slot.x + slot.thickness - 1f, top + 1f),
                    Point2(slot.x + slot.thickness + sx, top + sy),
                    Point2(slot.x + slot.thickness + sx, plank.plankY),
                    Point2(slot.x + 1f, plank.plankY),
                ),
                black(0.92f),
                7f * k,
            )
        }
        val left = plank.left - 26f * k
        val right = plank.right + 26f * k
        canvas.softFill(
            listOf(
                Point2(left, plank.plankY + 10f * k),
                Point2(right, plank.plankY + 10f * k),
                Point2(right + sx * 0.9f, plank.plankY + sy * 1.15f),
                Point2(left + sx * 0.9f, plank.plankY + sy * 1.15f),
            ),
            black(0.95f),
            12f * k,
        )
    }

    val pixels = IntArray(columns * rows)
    bitmap.getPixels(pixels, 0, columns, 0, 0, columns, rows)
    bitmap.recycle()
    return FloatArray(pixels.size) { ((pixels[it] shr 16) and 0xFF) / 255f }
}

private fun leafMap(
    layout: ShelfLayout,
    lighting: ShelfLighting,
    sun: FloatArray,
    columns: Int,
    rows: Int,
): Bitmap {
    val w = layout.width
    val h = layout.height
    val k = layout.scale
    val bitmap = createBitmap(columns, rows)
    val canvas = Canvas(bitmap)
    val random = ShelfRandom(31)

    fun leaf(
        x: Float,
        y: Float,
        angle: Float,
        length: Float,
        width: Float,
        alpha: Float,
        blur: Float,
    ) {
        val paint = blurPaint(black(alpha), blur)
        canvas.withSave {
            canvas.translate(x / LIGHT_CELL, y / LIGHT_CELL)
            canvas.rotate(toDegrees(angle.toDouble()).toFloat())
            val l = length / LIGHT_CELL
            val wd = width / LIGHT_CELL
            canvas.drawPath(
                Path().apply {
                    moveTo(-l / 2f, 0f)
                    quadTo(0f, -wd, l / 2f, 0f)
                    quadTo(0f, wd, -l / 2f, 0f)
                },
                paint,
            )
        }
    }

    fun twig(x: Float, y: Float, startAngle: Float, length: Float, segments: Int, depth: Int) {
        var px = x
        var py = y
        var angle = startAngle
        val stroke =
            blurPaint(black(0.6f), (3f + depth * 2f) * k).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.4f * k / LIGHT_CELL
            }
        val path = Path().apply { moveTo(px / LIGHT_CELL, py / LIGHT_CELL) }
        val points = mutableListOf<Triple<Float, Float, Float>>()
        repeat(segments) {
            angle += (random.next() - 0.5f) * 0.3f
            px += cos(angle) * length / segments
            py += sin(angle) * length / segments
            path.lineTo(px / LIGHT_CELL, py / LIGHT_CELL)
            points += Triple(px, py, angle)
        }
        canvas.drawPath(path, stroke)
        for ((qx, qy, qa) in points) {
            for (side in intArrayOf(-1, 1)) {
                if (random.next() < 0.2f) continue
                val la = qa + side * (0.45f + random.next() * 0.5f)
                val ll = (22f + random.next() * 12f) * k
                leaf(
                    qx + cos(la) * ll * 0.48f,
                    qy + sin(la) * ll * 0.48f,
                    la,
                    ll,
                    (3.6f + random.next() * 1.6f) * k,
                    0.75f + random.next() * 0.25f,
                    (1f + random.next() * 4f) * k,
                )
            }
        }
    }

    twig(-14f * k, h * 0.17f, 0.28f, w * 0.62f, 11, 0)
    twig(w * 0.16f, h * 0.19f, 0.95f, w * 0.22f, 5, 1)
    twig(w * 0.02f, h * 0.27f, -0.05f, w * 0.3f, 6, 1)

    val alpha = IntArray(columns * rows)
    bitmap.getPixels(alpha, 0, columns, 0, 0, columns, rows)
    val lit = lighting.ambient + lighting.sun * SUN_GAIN
    val pixels =
        IntArray(alpha.size) { i ->
            val a = minOf(1f, (alpha[i] ushr 24) / 255f * 1.25f) * sun[i] * 0.9f
            val r = 255f * mix(1f, lighting.ambient.r / lit.r.coerceAtLeast(1f), a)
            val g = 255f * mix(1f, lighting.ambient.g / lit.g.coerceAtLeast(1f), a)
            val b = 255f * mix(1f, lighting.ambient.b / lit.b.coerceAtLeast(1f), a)
            packRgb(r, g, b)
        }
    return imageBitmapOf(pixels, columns, rows)
}

private fun Canvas.softFill(polygon: List<Point2>, color: Int, blur: Float) {
    val path = Path()
    polygon.forEachIndexed { i, p ->
        if (i == 0) path.moveTo(p.x / LIGHT_CELL, p.y / LIGHT_CELL)
        else path.lineTo(p.x / LIGHT_CELL, p.y / LIGHT_CELL)
    }
    path.close()
    drawPath(path, blurPaint(color, blur))
}

private fun blurPaint(color: Int, blurDp: Float): Paint {
    val sigma = blurDp / LIGHT_CELL
    return Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        if (sigma > 0.5f) {
            maskFilter =
                BlurMaskFilter((sigma - 0.5f) / SIGMA_PER_RADIUS, BlurMaskFilter.Blur.NORMAL)
        }
    }
}
