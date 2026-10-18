package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val PI_F = PI.toFloat()
private const val SHELF_CAMERA_RELEASE = 0.3f
private const val SHELF_FOCAL_STRETCH = 2.5f
private const val PULL_END = 1f
// How far an open book pushed down tips back, per unit it is pushed.
private const val PUSH_TILT_PER_UNIT = 0.0012f

internal data class BookDimensions(val thickness: Float, val height: Float, val depth: Float)

internal data class ShelfCamera(
    val vanishingX: Float,
    val vanishingY: Float,
    val focal: Float,
    val fixed: Boolean = false,
) {
    fun project(p: Vec3): Point2 {
        val s = focal / (focal - p.z)
        return Point2(vanishingX + (p.x - vanishingX) * s, vanishingY + (p.y - vanishingY) * s)
    }

    fun onShelf(bookCentreX: Float, swing: Float): ShelfCamera {
        if (fixed || swing >= SHELF_CAMERA_RELEASE) return this
        val out = smoothStep(0f, SHELF_CAMERA_RELEASE, swing)
        return copy(
            vanishingX = mix(bookCentreX, vanishingX, out),
            focal = mix(focal * SHELF_FOCAL_STRETCH, focal, out),
        )
    }
}

internal data class HeldPlacement(val centerX: Float, val centerY: Float, val scale: Float) {
    fun depth(camera: ShelfCamera) = camera.focal * (1f - 1f / scale)
}

internal data class RowPlane(val point: Vec3, val normal: Vec3) {
    fun distance(world: Vec3): Float = (world - point) dot normal
}

internal data class BookRest(
    val x: Float,
    val z: Float,
    val yaw: Float,
    val shade: Float,
    val occluders: List<List<Point2>>,
    val row: RowPlane? = null,
)

internal interface BookTransform {
    val dimensions: BookDimensions
    val camera: ShelfCamera
    val open: Float

    fun map(local: Vec3): Vec3

    fun mapDirection(local: Vec3): Vec3

    fun rowDistance(world: Vec3): Float = world.z
}

internal class BookPose(
    override val dimensions: BookDimensions,
    slotX: Float,
    plankY: Float,
    shelfCamera: ShelfCamera,
    held: HeldPlacement,
    liftArc: Float,
    val travel: Float,
    tilt: Float,
    override val open: Float,
    restZ: Float = 0f,
    restYaw: Float = 0f,
    row: RowPlane? = null,
    push: Float = 0f,
) : BookTransform {
    val swing: Float
    override val camera: ShelfCamera
    private val anchor: Vec3
    private val pivot: Vec3
    private val cosTilt: Float
    private val sinTilt: Float
    private val cosTurn: Float
    private val sinTurn: Float
    private val row: RowPlane

    init {
        val (t, h, d) = dimensions
        val endZ = held.depth(shelfCamera)
        val pull = smoothStep(0f, PULL_END, travel)
        val backZ = pull * (endZ + d / 2f) - d
        swing = ((backZ - t) / (endZ - d / 2f - t).coerceAtLeast(1f)).coerceIn(0f, 1f)
        camera = shelfCamera.onShelf(bookCentreX = slotX + t / 2f, swing = swing)
        val across = smoothStep(0f, 1f, swing)
        val heldY = shelfCamera.vanishingY + (held.centerY - shelfCamera.vanishingY) / held.scale
        val centred = maxOf(pull, smoothStep(0f, 0.2f, swing))
        val pivotZ = -d / 2f * centred
        val z = pull * (endZ + d / 2f) + pivotZ + (1f - pull) * restZ
        val start = slotX + t / 2f
        val onScreen =
            camera.vanishingX +
                (mix(start, held.centerX, across) - camera.vanishingX) * (camera.focal - z) /
                    camera.focal
        anchor =
            Vec3(
                mix(start, onScreen, across),
                mix(plankY, heldY + push / held.scale, across) - sin(PI_F * across) * liftArc,
                z,
            )
        pivot = Vec3(t / 2f, h / 2f * across, pivotZ)
        this.row = row ?: RowPlane(Vec3(start, plankY, restZ), Vec3(sin(restYaw), 0f, cos(restYaw)))
        val turn = mix(restYaw, -PI_F / 2f, smoothStep(0f, 0.9f, swing))
        val theta = if (travel > 0f) tilt - push * PUSH_TILT_PER_UNIT else tilt
        cosTilt = cos(theta)
        sinTilt = sin(theta)
        cosTurn = cos(turn)
        sinTurn = sin(turn)
    }

    override fun map(local: Vec3): Vec3 {
        val r = rotate(local - pivot)
        return Vec3(anchor.x + r.x, anchor.y - r.y, anchor.z + r.z)
    }

    override fun mapDirection(local: Vec3): Vec3 {
        val r = rotate(local)
        return Vec3(r.x, -r.y, r.z)
    }

    override fun rowDistance(world: Vec3): Float = row.distance(world)

    private fun rotate(q: Vec3): Vec3 {
        val y1 = q.y * cosTilt - q.z * sinTilt
        val z1 = q.y * sinTilt + q.z * cosTilt
        return Vec3(q.x * cosTurn + z1 * sinTurn, y1, -q.x * sinTurn + z1 * cosTurn)
    }
}

internal object TiltPaths {
    fun out(releaseTilt: Float, travel: Float): Float =
        releaseTilt * (1f - smoothStep(0f, 0.5f, travel)) - settleDip(travel)

    fun back(travel: Float): Float = -settleDip(travel)

    private fun settleDip(travel: Float) =
        0.08f * sin(PI_F * smoothStep(0.2f, 1f, travel)) * (1f - smoothStep(0.85f, 1f, travel))
}

/**
 * How much of a book's thickness its cover board is: all that swings when the cover closes alone.
 */
internal const val COVER_BOARD = 0.04f

/**
 * Where an open book splits: at its bookmark, or, with [coverOnly], just inside its cover, as when
 * the cover is closed from the first page.
 */
internal fun hingeX(thickness: Float, readFraction: Float?, coverOnly: Boolean = false): Float {
    if (coverOnly) return thickness * (1f - COVER_BOARD)
    val read =
        if (readFraction != null && readFraction > 0f) readFraction.coerceIn(0.12f, 0.88f) else 0.1f
    return thickness * (1f - read)
}

/**
 * How far a book whose cover closes on its own is moved back through its thickness, so its open
 * pages stay where its bookmark put them; nothing once the cover has shut.
 */
internal fun coverOnlyShift(thickness: Float, readFraction: Float?, open: Float): Float =
    (hingeX(thickness, readFraction, coverOnly = true) - hingeX(thickness, readFraction)) *
        open.coerceIn(0f, 1f)
