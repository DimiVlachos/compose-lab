package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.PlankLayout
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Rgb
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfLayout
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfLighting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Vec3
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Bitmap
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.BlendMode
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Canvas
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Paint
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Path
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.RectF
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.withSave
import kotlin.math.roundToInt
import kotlin.math.sin

internal const val RIBBON_RED = 0xA01E24
private const val RIBBON_WIDTH = 3f
private const val RIBBON_NOTCH = 2.5f
private const val RIBBON_TIP = 5f
private const val RIBBON_TIP_HASH = 17
private const val RIBBON_TIP_SPREAD = 3
private const val RIBBON_AT_FROM = 0.25f
private const val RIBBON_AT_HASH = 37
private const val RIBBON_AT_STEPS = 41
private const val RIBBON_NOTCH_SHARE = 0.35f
private const val RIBBON_SHADOW_X = 1.2f

internal class ShelfScene(
    val layout: ShelfLayout,
    val books: Map<Int, ShelfBookVisual>,
    val lighting: ShelfLighting,
    val ribbonBookIds: Set<Int>,
)

internal fun Canvas.drawStaticShelf(
    scene: ShelfScene,
    maps: ShelfLightMaps,
    spines: Map<Int, Bitmap>,
    missingBookId: Int?,
    kit: ShelfKit,
) {
    drawShelfBackdrop(scene.layout.width, scene.layout.height, scene.lighting, maps, kit)
    for (plank in scene.layout.planks) drawPlank(scene, plank, missingBookId, spines, kit)
}

internal fun Canvas.drawShelfBackdrop(
    w: Float,
    h: Float,
    lighting: ShelfLighting,
    maps: ShelfLightMaps,
    kit: ShelfKit,
) {
    drawRect(0f, 0f, w, h, fillPaint(lighting.wall.toArgb()))
    texture(kit.noise.plaster, 60f, 0f, 0f, w, h, 0.22f)
    drawRect(
        0f,
        0f,
        w,
        h,
        Paint().apply { shader = verticalGradient(0f, h, 0f to white(0.08f), 1f to black(0.06f)) },
    )
    drawLightMap(maps)
}

internal fun Canvas.drawLeaves(maps: ShelfLightMaps, swayDp: Float) {
    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { blendMode = BlendMode.MULTIPLY }
    drawBitmap(
        maps.leaves,
        null,
        RectF(swayDp, swayDp * 0.4f, maps.width + swayDp, maps.height + swayDp * 0.4f),
        paint,
    )
}

internal fun leafSway(timeMillis: Long): Float {
    val s = timeMillis / 1000f
    return sin(s * 0.9f) * 1.6f + sin(s * 2.3f) * 0.6f
}

private fun Canvas.drawLightMap(maps: ShelfLightMaps) {
    val target = RectF(0f, 0f, maps.width, maps.height)
    drawBitmap(
        maps.light,
        null,
        target,
        Paint(Paint.FILTER_BITMAP_FLAG).apply { blendMode = BlendMode.MULTIPLY },
    )
    drawBitmap(
        maps.glow,
        null,
        target,
        Paint(Paint.FILTER_BITMAP_FLAG).apply { blendMode = BlendMode.PLUS },
    )
}

private fun Canvas.drawPlank(
    scene: ShelfScene,
    plank: PlankLayout,
    missingBookId: Int?,
    spines: Map<Int, Bitmap>,
    kit: ShelfKit,
) {
    val k = scene.layout.scale
    val lighting = scene.lighting
    val topH = plankTopHeight(k)
    val left = plank.left - 26f * k
    val right = plank.right + 26f * k
    val up = lighting.lambert(Vec3(0f, -1f, 0f))
    val oakTop =
        Rgb(
            lighting.oak.r * up.r / 255f,
            lighting.oak.g * up.g / 255f,
            lighting.oak.b * up.b / 255f,
        ) * 1.05f

    drawRect(left, plank.plankY - topH, right, plank.plankY, fillPaint(oakTop.toArgb()))
    texture(kit.noise.grain, 30f, left, plank.plankY - topH, right, plank.plankY, 0.3f)
    drawRect(
        left,
        plank.plankY - topH,
        right,
        plank.plankY,
        Paint().apply {
            shader =
                verticalGradient(
                    plank.plankY - topH,
                    plank.plankY,
                    0f to argb(0x281408, 0.45f),
                    0.5f to argb(0x281408, 0.12f),
                    1f to argb(0xFFF0D2, 0.1f),
                )
        },
    )

    val missing = plank.slots.indexOfFirst { it.bookId == missingBookId }
    if (missing >= 0) drawGap(scene, plank, missing, topH, oakTop)

    val spineLight = lighting.lambert(Vec3(0f, 0f, 1f)).toArgb()
    val spinePaint = Paint(Paint.FILTER_BITMAP_FLAG)
    val standing =
        plank.slots
            .filter { it.bookId != missingBookId }
            .mapNotNull { slot -> spines[slot.bookId]?.let { slot to it } }
    for ((slot, spine) in standing) {
        val rect = RectF(slot.x, plank.plankY - slot.height, slot.x + slot.thickness, plank.plankY)
        drawBitmap(spine, null, rect, spinePaint)
        drawRect(rect, multiplyPaint(spineLight))
    }

    val frontH = plankFrontHeight(k)
    withSave {
        translate(left, plank.plankY)
        val width = right - left
        drawRect(0f, 0f, width, frontH, fillPaint(lighting.oak.toArgb()))
        val grainLine =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 0.6f
                color = argb(0x5A3214, 0.22f)
            }
        for (i in 0 until 9) {
            val yy = frontH * (0.12f + i * 0.1f)
            val path = Path()
            var x = 0f
            while (x <= width) {
                val y = yy + sin(x * 0.02f + i * 1.7f) * 1.4f + sin(x * 0.007f + i) * 2f
                if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                x += 6f
            }
            drawPath(path, grainLine)
        }
        texture(kit.noise.grain, 30f, 0f, 0f, width, frontH, 0.3f)
        drawRect(
            0f,
            0f,
            width,
            frontH,
            Paint().apply {
                shader =
                    verticalGradient(
                        0f,
                        frontH,
                        0f to argb(0xFFF0D7, 0.55f),
                        0.12f to argb(0xFFF0D7, 0.08f),
                        0.85f to black(0.08f),
                        1f to black(0.3f),
                    )
            },
        )
        drawRect(
            0f,
            0f,
            width,
            frontH,
            multiplyPaint(lighting.lambert(Vec3(0f, -0.2f, 0.98f).normalized()).toArgb()),
        )
    }

    val marked =
        plank.slots.filter { it.bookId in scene.ribbonBookIds && it.bookId != missingBookId }
    for (slot in marked) {
        drawHangingRibbon(
            slot.x + shelfRibbonOffset(slot.bookId, slot.thickness, k),
            plank.plankY,
            shelfRibbonWidth(k),
            shelfRibbonTip(slot.bookId, k),
            shelfRibbonNotch(k),
            k,
            lighting,
        )
    }
}

private fun Canvas.drawGap(
    scene: ShelfScene,
    plank: PlankLayout,
    index: Int,
    topH: Float,
    oakTop: Rgb,
) {
    val slot = plank.slots[index]
    val leftH = plank.slots.getOrNull(index - 1)?.height ?: 0f
    val rightH = plank.slots.getOrNull(index + 1)?.height ?: 0f
    val neighbour = if (leftH == 0f) rightH else if (rightH == 0f) leftH else minOf(leftH, rightH)
    val top = plank.plankY - neighbour
    drawRect(
        slot.x - 0.4f,
        top,
        slot.x + slot.thickness + 0.8f,
        plank.plankY,
        Paint().apply {
            shader =
                verticalGradient(
                    top,
                    plank.plankY,
                    0f to argb(0x140C08, 0f),
                    0.25f to argb(0x140C08, 0.42f),
                    1f to argb(0x140C08, 0.62f),
                )
        },
    )
    drawRect(
        slot.x,
        plank.plankY - topH * 0.9f,
        slot.x + slot.thickness + 0.8f,
        plank.plankY,
        fillPaint((oakTop * 0.55f).toArgb()),
    )
    val right = plank.slots.getOrNull(index + 1) ?: return
    val rightBook = scene.books[right.bookId] ?: return
    val k = scene.layout.scale
    drawRect(
        right.x - 3f * k,
        plank.plankY - right.height,
        right.x,
        plank.plankY,
        Paint().apply {
            shader =
                horizontalGradient(
                    right.x - 3f * k,
                    right.x,
                    0f to black(0f),
                    1f to argb(shade(rightBook.style.base, 0.45f), 0.9f),
                )
        },
    )
}

internal fun shelfRibbonWidth(k: Float) = RIBBON_WIDTH * k

internal fun shelfRibbonNotch(k: Float) = RIBBON_NOTCH * k

internal fun shelfRibbonTip(bookId: Int, k: Float) =
    (RIBBON_TIP + (bookId * RIBBON_TIP_HASH).mod(RIBBON_TIP_SPREAD)) * k

internal fun shelfRibbonOffset(bookId: Int, thickness: Float, k: Float): Float {
    val at = RIBBON_AT_FROM + (bookId * RIBBON_AT_HASH).mod(RIBBON_AT_STEPS) / 100f
    return (thickness * at).coerceAtMost(thickness - shelfRibbonWidth(k) - k).coerceAtLeast(k)
}

@Suppress("LongParameterList")
internal fun Canvas.drawHangingRibbon(
    x: Float,
    top: Float,
    width: Float,
    length: Float,
    notch: Float,
    k: Float,
    lighting: ShelfLighting,
    alpha: Float = 1f,
) {
    val shadowX = RIBBON_SHADOW_X * width / shelfRibbonWidth(k)
    val layer = if (alpha < 1f) saveLayerAlpha(null, (alpha * 255f).roundToInt()) else null
    drawRect(
        x + shadowX,
        top,
        x + shadowX + width,
        top + length - k,
        fillPaint(argb(0x28140A, 0.25f)),
    )
    val path =
        Path().apply {
            moveTo(x, top - 1f)
            lineTo(x + width, top - 1f)
            lineTo(x + width, top + length)
            lineTo(x + width / 2f, top + length - minOf(notch, length * RIBBON_NOTCH_SHARE))
            lineTo(x, top + length)
            close()
        }
    drawPath(
        path,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader =
                horizontalGradient(
                    x,
                    x + width,
                    0f to shade(RIBBON_RED, 0.8f),
                    0.45f to shade(RIBBON_RED, 1.3f),
                    1f to shade(RIBBON_RED, 0.7f),
                )
        },
    )
    drawPath(path, multiplyPaint(lighting.lambert(Vec3(0f, 0f, 1f)).toArgb()))
    layer?.let(::restoreToCount)
}

internal fun plankTopHeight(k: Float) = maxOf(6f, 13f * k)

internal fun plankFrontHeight(k: Float) = maxOf(12f, 17f * k)

internal fun wallRegion(layout: ShelfLayout, missingBookId: Int?): Path {
    val k = layout.scale
    val covered = Path()
    for (plank in layout.planks) {
        covered.addRect(
            plank.left - 26f * k,
            plank.plankY - plankTopHeight(k),
            plank.right + 26f * k,
            plank.plankY + plankFrontHeight(k),
            Path.Direction.CW,
        )
        for (slot in plank.slots) {
            if (slot.bookId == missingBookId) continue
            covered.addRect(
                slot.x,
                plank.plankY - slot.height,
                slot.x + slot.thickness,
                plank.plankY,
                Path.Direction.CW,
            )
        }
    }
    return Path().apply {
        addRect(0f, 0f, layout.width, layout.height, Path.Direction.CW)
        op(covered, Path.Op.DIFFERENCE)
    }
}

internal fun Canvas.drawShadowRestore(lift: ShadowLift, wall: Path, alpha: Float) {
    withSave {
        clipPath(wall)
        drawBitmap(
            lift.restore,
            null,
            RectF(0f, 0f, lift.maps.width, lift.maps.height),
            Paint(Paint.FILTER_BITMAP_FLAG).apply {
                blendMode = BlendMode.MULTIPLY
                this.alpha = (alpha * 255f).toInt()
            },
        )
    }
}
