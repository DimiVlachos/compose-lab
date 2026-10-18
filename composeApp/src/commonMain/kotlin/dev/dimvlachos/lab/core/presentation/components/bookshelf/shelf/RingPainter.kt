package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.FaceTexture
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Point2
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ProjectedFace
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Rgb
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.RingPlacement
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfLighting
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.ShelfRing
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.Vec3
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.buildBookFaces
import dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry.smoothStep
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Bitmap
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Canvas
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.LightingColorFilter
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.LinearGradient
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Matrix
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Paint
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Path
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Rect
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.RectF
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.Shader
import dev.dimvlachos.lab.core.presentation.components.bookshelf.gfx.withSave
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val PLANK_STEP = 0.04f
private const val PLANK_REACH = 1.8f
private const val PLANK_LIP = 8f
private const val PLANK_BACK = 34f
private const val WALL_FADE = 1.2f
private const val PLANK_EDGE_FROM = 0.6f
private const val PLANK_EDGE_TO = 1.6f
private const val PLANK_EDGE_DARK = 0.6f
private const val GRAIN_LINES = 7
private const val WALL_BLUR = 9f
private const val CONTACT_BLUR = 3f
private const val CONTACT_X = 0.1f
private const val CONTACT_Z = 0.16f
private const val CONTACT_ALPHA = 0.5f
private const val WALL_DX = 0.3f
private const val WALL_DY = 0.12f
private const val WALL_ALPHA = 0.22f
private const val SHADOW_INK = 0x28140A
private const val MIN_TEXTURED_AREA = 60f

internal class RingBookAssets(
    val spine: Bitmap?,
    val shell: BookShell?,
    val shellAlpha: Float,
    val plain: (FaceTexture) -> Int,
)

internal class RingPainter {
    private val faceMatrix = Matrix()
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shadowPaint = Paint()
    private val corners = FloatArray(8)
    private val target = FloatArray(8)
    private val fill = Paint()
    private val grain =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
            color = argb(0x5A3214, 0.22f)
        }
    private val lip =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = argb(0xFFF0D7, 0.35f)
        }
    private val strip = Path()
    private val line = Path()

    fun wallBlur(ring: ShelfRing) = WALL_BLUR * ring.scale

    fun contactBlur(ring: ShelfRing) = CONTACT_BLUR * ring.scale

    fun Canvas.drawWallShadows(
        ring: ShelfRing,
        placed: List<RingPlacement>,
        alpha: (RingPlacement) -> Float,
    ) {
        for (p in placed) {
            val (t, h, d) = p.pose.dimensions
            val wall =
                listOf(Vec3(0f, h, 0f), Vec3(t, h, 0f), Vec3(t, 0f, 0f), Vec3(0f, 0f, 0f)).map {
                    val w = p.pose.map(it)
                    ring.camera.project(Vec3(w.x + WALL_DX * h, w.y + WALL_DY * h, w.z - d))
                }
            drawSoft(
                wall,
                WALL_ALPHA * p.shade * smoothStep(WALL_FADE, 0f, abs(p.angle)) * alpha(p),
            )
        }
    }

    fun Canvas.drawContactShadows(
        ring: ShelfRing,
        placed: List<RingPlacement>,
        alpha: (RingPlacement) -> Float,
    ) {
        for (p in placed) {
            val (t, h, d) = p.pose.dimensions
            val contact =
                listOf(Vec3(0f, 0f, 0f), Vec3(t, 0f, 0f), Vec3(t, 0f, -d), Vec3(0f, 0f, -d)).map {
                    val w = p.pose.map(it)
                    ring.camera.project(Vec3(w.x + CONTACT_X * h, w.y, w.z - CONTACT_Z * h))
                }
            drawSoft(contact, CONTACT_ALPHA * p.shade * alpha(p))
        }
    }

    fun Canvas.drawRingPlank(ring: ShelfRing, lighting: ShelfLighting, scroll: Float) {
        val k = ring.scale
        val front = ring.radius + PLANK_LIP * k
        val back = ring.radius - PLANK_BACK * k
        val y = ring.plankY
        val frontH = plankFrontHeight(k)
        val top = plankTopColor(lighting)
        val angles =
            generateSequence(-PLANK_REACH) { it + PLANK_STEP }
                .takeWhile { it <= PLANK_REACH }
                .filter { facesCamera(ring, front, it, y) }
                .toList()
        if (angles.size < 2) return
        val lip = angles.map { ring.camera.project(Ring(front, it, y).world(ring)) }
        val left = lip.first().x
        val right = lip.last().x
        if (right <= left) return
        val stops = FloatArray(lip.size) { ((lip[it].x - left) / (right - left)).coerceIn(0f, 1f) }
        outline(
            ring,
            angles.map { Ring(front, it, y) } + angles.reversed().map { Ring(back, it, y) },
        )
        fill.shader =
            LinearGradient(
                left,
                0f,
                right,
                0f,
                IntArray(angles.size) { (top * plankShade(angles[it])).toArgb() },
                stops,
                Shader.TileMode.CLAMP,
            )
        drawPath(strip, fill)
        outline(
            ring,
            angles.map { Ring(front, it, y) } +
                angles.reversed().map { Ring(front, it, y + frontH) },
        )
        fill.shader =
            LinearGradient(
                left,
                0f,
                right,
                0f,
                IntArray(angles.size) { i ->
                    val a = angles[i]
                    val lit = lighting.lambert(Vec3(sin(a), -0.2f, cos(a)).normalized())
                    val oak = lighting.oak * plankShade(a)
                    Rgb(oak.r * lit.r / 255f, oak.g * lit.g / 255f, oak.b * lit.b / 255f).toArgb()
                },
                stops,
                Shader.TileMode.CLAMP,
            )
        drawPath(strip, fill)
        fill.shader = null
        drawGrain(ring, front, y, frontH, scroll)
        drawLipLight(ring, front, y, frontH)
    }

    fun Canvas.drawRingBook(
        placed: RingPlacement,
        lighting: ShelfLighting,
        assets: RingBookAssets,
    ) {
        val faces = buildBookFaces(placed.pose, null)
        for (face in faces) {
            val light = lighting.faceLight(face.normal, 0f) * placed.shade
            val flat = face.area() < MIN_TEXTURED_AREA
            val spine = assets.spine.takeIf { face.texture == FaceTexture.Spine && !flat }
            if (spine != null) {
                drawLitFace(face, spine, light, 255)
                continue
            }
            val plain = Rgb.of(assets.plain(face.texture))
            fill.color =
                Rgb(plain.r * light.r / 255f, plain.g * light.g / 255f, plain.b * light.b / 255f)
                    .toArgb()
            fill.isAntiAlias = true
            drawPath(polygon(face.corners), fill)
            fill.isAntiAlias = false
            val shell = assets.shell?.faces?.get(face.texture)
            if (shell != null && assets.shellAlpha > 0f && !flat) {
                drawLitFace(face, shell, light, (assets.shellAlpha * 255f).roundToInt())
            }
        }
    }

    private fun Canvas.drawLitFace(face: ProjectedFace, bitmap: Bitmap, light: Rgb, alpha: Int) {
        val uv = face.uv
        val src =
            Rect(
                (uv.u0 * bitmap.width).roundToInt(),
                (uv.v0 * bitmap.height).roundToInt(),
                (uv.u1 * bitmap.width).roundToInt(),
                (uv.v1 * bitmap.height).roundToInt(),
            )
        if (src.isEmpty) return
        val c = face.corners
        corners[0] = src.left.toFloat()
        corners[1] = src.top.toFloat()
        corners[2] = src.right.toFloat()
        corners[3] = src.top.toFloat()
        corners[4] = src.right.toFloat()
        corners[5] = src.bottom.toFloat()
        corners[6] = src.left.toFloat()
        corners[7] = src.bottom.toFloat()
        for (i in 0 until 4) {
            target[i * 2] = c[i].x
            target[i * 2 + 1] = c[i].y
        }
        if (!faceMatrix.setPolyToPoly(corners, 0, target, 0, 4)) return
        bitmapPaint.colorFilter = LightingColorFilter(light.toArgb(), 0)
        bitmapPaint.alpha = alpha
        withSave {
            concat(faceMatrix)
            drawBitmap(bitmap, src, RectF(src), bitmapPaint)
        }
        bitmapPaint.alpha = 255
        bitmapPaint.colorFilter = null
    }

    private fun Canvas.outline(ring: ShelfRing, points: List<Ring>) {
        strip.reset()
        points.forEachIndexed { i, r ->
            val p = ring.camera.project(r.world(ring))
            if (i == 0) strip.moveTo(p.x, p.y) else strip.lineTo(p.x, p.y)
        }
        strip.close()
    }

    fun hit(placed: List<RingPlacement>, point: Point2): RingPlacement? =
        placed.asReversed().firstOrNull { p ->
            buildBookFaces(p.pose, null).any { it.contains(point) }
        }

    private fun Canvas.drawSoft(corners: List<Point2>, alpha: Float) {
        if (alpha <= 0f) return
        shadowPaint.color = argb(SHADOW_INK, alpha)
        drawPath(polygon(corners), shadowPaint)
    }

    private fun Canvas.drawGrain(
        ring: ShelfRing,
        front: Float,
        y: Float,
        frontH: Float,
        scroll: Float,
    ) {
        val k = ring.scale
        for (i in 0 until GRAIN_LINES) {
            line.reset()
            var a = -PLANK_REACH
            var first = true
            while (a <= PLANK_REACH) {
                val arc = a * ring.radius + scroll
                val yy =
                    y +
                        frontH * (0.12f + i * 0.12f) +
                        (sin(arc * 0.02f + i * 1.7f) * 1.4f + sin(arc * 0.007f + i) * 2f) * k * 0.6f
                if (facesCamera(ring, front, a, y)) {
                    val p = ring.camera.project(Ring(front, a, yy).world(ring))
                    if (first) line.moveTo(p.x, p.y) else line.lineTo(p.x, p.y)
                    first = false
                }
                a += PLANK_STEP
            }
            drawPath(line, grain)
        }
    }

    private fun Canvas.drawLipLight(ring: ShelfRing, front: Float, y: Float, frontH: Float) {
        line.reset()
        var a = -PLANK_REACH
        var first = true
        while (a <= PLANK_REACH) {
            if (facesCamera(ring, front, a, y)) {
                val p = ring.camera.project(Ring(front, a, y + frontH * 0.06f).world(ring))
                if (first) line.moveTo(p.x, p.y) else line.lineTo(p.x, p.y)
                first = false
            }
            a += PLANK_STEP
        }
        lip.strokeWidth = frontH * 0.12f
        drawPath(line, lip)
    }

    private fun facesCamera(ring: ShelfRing, radius: Float, angle: Float, y: Float): Boolean {
        val p = Ring(radius, angle, y).world(ring)
        val camera = ring.camera
        return (camera.vanishingX - p.x) * sin(angle) + (camera.focal - p.z) * cos(angle) > 0f
    }

    private data class Ring(val radius: Float, val angle: Float, val y: Float) {
        fun world(ring: ShelfRing) =
            Vec3(
                ring.centreX + radius * sin(angle),
                y,
                -ring.radius + radius * cos(angle),
            )
    }
}

private fun plankShade(angle: Float) =
    1f - PLANK_EDGE_DARK * smoothStep(PLANK_EDGE_FROM, PLANK_EDGE_TO, abs(angle))

private fun plankTopColor(lighting: ShelfLighting): Rgb {
    val up = lighting.lambert(Vec3(0f, -1f, 0f))
    return Rgb(
        lighting.oak.r * up.r / 255f,
        lighting.oak.g * up.g / 255f,
        lighting.oak.b * up.b / 255f,
    ) * 1.05f
}

private fun ProjectedFace.contains(point: Point2): Boolean {
    var inside = false
    var j = corners.lastIndex
    for (i in corners.indices) {
        val a = corners[i]
        val b = corners[j]
        if (
            (a.y > point.y) != (b.y > point.y) &&
                point.x < (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}

private fun ProjectedFace.area(): Float {
    var sum = 0f
    var j = corners.lastIndex
    for (i in corners.indices) {
        sum += (corners[j].x + corners[i].x) * (corners[j].y - corners[i].y)
        j = i
    }
    return abs(sum) / 2f
}
