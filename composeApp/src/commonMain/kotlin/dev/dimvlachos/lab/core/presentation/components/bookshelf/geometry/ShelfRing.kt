package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val RING_THICKNESS = 46f
private const val RING_SPREAD = 0.2f
private const val RING_TALLEST = 240f
private const val RING_PLANK_Y = 0.8f
private const val RING_BOOK_BAND = 0.62f
private const val RING_RADIUS = 0.62f
// The ring is an oval: its depth is this share of its width, so it curves gently away.
private const val RING_OVAL = 0.5f
private const val RING_GAP = 2.5f
private const val RING_CAMERA_LIFT = 150f
private const val RING_MIN_FOCAL = 900f
private const val RING_FOCAL_WIDTHS = 2.8f
private const val EDGE_FROM = 0.6f
private const val EDGE_TO = 1.5f
private const val EDGE_DARK = 0.55f
private const val VISIBLE_ANGLE = 1.75f

internal data class RingBook(
    val id: Int,
    val thickness: Float,
    val height: Float,
    val depth: Float,
    val centre: Float,
)

internal class RingPlacement(
    val index: Int,
    val angle: Float,
    val shade: Float,
    val pose: RingBookPose,
)

internal class ShelfRing(val width: Float, val height: Float, specs: List<ShelfBookSpec>) {
    val scale = height * RING_BOOK_BAND / RING_TALLEST
    val plankY = height * RING_PLANK_Y
    val centreX = width / 2f
    val camera =
        ShelfCamera(
            vanishingX = centreX,
            vanishingY = plankY - RING_CAMERA_LIFT * scale,
            focal = maxOf(RING_MIN_FOCAL, width * RING_FOCAL_WIDTHS),
            fixed = true,
        )
    val books: List<RingBook>
    val radius: Float
    val oval = RING_OVAL

    init {
        var edge = 0f
        books = specs.mapIndexed { i, spec ->
            val t = ringThicknessFor(spec.thickness) * scale
            if (i > 0) edge += RING_GAP * scale
            RingBook(spec.id, t, spec.height * scale, spec.depth * scale, edge + t / 2f).also {
                edge += t
            }
        }
        radius = width * RING_RADIUS
    }

    val start: Float
        get() = books.firstOrNull()?.centre ?: 0f

    val end: Float
        get() = books.lastOrNull()?.centre ?: 0f

    fun indexOf(bookId: Int): Int = books.indexOfFirst { it.id == bookId }

    fun nearestIndex(scroll: Float): Int {
        if (books.isEmpty()) return -1
        var best = 0
        for (i in books.indices) {
            if (abs(books[i].centre - scroll) < abs(books[best].centre - scroll)) best = i
        }
        return best
    }

    fun snapTarget(scroll: Float): Float = books.getOrNull(nearestIndex(scroll))?.centre ?: scroll

    fun heroLayout(rest: (Int) -> BookRest?): ShelfLayout {
        val planks = books.map { book ->
            val slot =
                ShelfSlot(
                    book.id,
                    centreX - book.thickness / 2f,
                    book.thickness,
                    book.height,
                    book.depth,
                )
            PlankLayout(plankY, 0f, listOf(slot), slot.x, slot.x + slot.thickness)
        }
        return ShelfLayout(width, height, scale, planks, camera = camera, rest = rest)
    }

    fun rest(bookId: Int, scroll: Float): BookRest? {
        val placed = visible(scroll)
        val book = placed.firstOrNull { books[it.index].id == bookId } ?: return null
        val a = book.angle
        val nearer = placed.filter { abs(it.angle) < abs(a) }
        val neighbour = nearer.maxByOrNull { abs(it.angle) }?.pose
        return BookRest(
            x = centreX + radius * sin(a) - books[book.index].thickness / 2f,
            z = -radius * (1f - cos(a)) * oval,
            yaw = book.pose.yaw,
            shade = book.shade,
            occluders =
                nearer.flatMap { near -> buildBookFaces(near.pose, null).map { it.corners } },
            row =
                neighbour?.let {
                    RowPlane(
                        it.map(Vec3(it.dimensions.thickness / 2f, 0f, 0f)),
                        it.mapDirection(Vec3(0f, 0f, 1f)),
                    )
                },
        )
    }

    fun place(index: Int, scroll: Float): RingPlacement? {
        val book = books.getOrNull(index) ?: return null
        val angle = (book.centre - scroll) / radius
        if (abs(angle) > VISIBLE_ANGLE) return null
        val pose =
            RingBookPose(
                dimensions = BookDimensions(book.thickness, book.height, book.depth),
                angle = angle,
                plankY = plankY,
                radius = radius,
                centreX = centreX,
                camera = camera,
                oval = oval,
            )
        val shade = 1f - EDGE_DARK * smoothStep(EDGE_FROM, EDGE_TO, abs(angle))
        return RingPlacement(index, angle, shade, pose)
    }

    fun visible(scroll: Float): List<RingPlacement> =
        books.indices.mapNotNull { place(it, scroll) }.sortedByDescending { abs(it.angle) }
}

internal class RingBookPose(
    override val dimensions: BookDimensions,
    val angle: Float,
    plankY: Float,
    radius: Float,
    centreX: Float,
    override val camera: ShelfCamera,
    oval: Float,
) : BookTransform {
    override val open: Float = 0f
    val yaw = ringYaw(angle, oval)
    private val cosA = cos(yaw)
    private val sinA = sin(yaw)
    private val anchor =
        Vec3(centreX + radius * sin(angle), plankY, -radius * (1f - cos(angle)) * oval)

    override fun map(local: Vec3): Vec3 {
        val r = rotate(Vec3(local.x - dimensions.thickness / 2f, local.y, local.z))
        return Vec3(anchor.x + r.x, anchor.y - r.y, anchor.z + r.z)
    }

    override fun mapDirection(local: Vec3): Vec3 {
        val r = rotate(local)
        return Vec3(r.x, -r.y, r.z)
    }

    private fun rotate(q: Vec3) = Vec3(q.x * cosA + q.z * sinA, q.y, -q.x * sinA + q.z * cosA)
}

// Each book faces square out from the oval, its spine along the curve, as on a carousel.
internal fun ringYaw(angle: Float, oval: Float): Float = atan2(oval * sin(angle), cos(angle))

internal fun ringThicknessFor(spineThickness: Float): Float {
    val t =
        ((spineThickness - MIN_SPINE_THICKNESS) / (MAX_SPINE_THICKNESS - MIN_SPINE_THICKNESS))
            .coerceIn(0f, 1f)
    return RING_THICKNESS * (1f - RING_SPREAD + 2f * RING_SPREAD * t)
}
