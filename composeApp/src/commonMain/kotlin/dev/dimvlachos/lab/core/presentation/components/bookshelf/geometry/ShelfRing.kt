package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

private const val RING_THICKNESS = 46f
private const val RING_SPREAD = 0.2f
private const val RING_TALLEST = 240f
private const val RING_PLANK_Y = 0.8f
private const val RING_BOOK_BAND = 0.62f
// Wide, so on a phone the shelf curves away only past the screen's edges.
private const val RING_RADIUS = 1.3f
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
private const val ARC_SAMPLES = 2048
private const val ARC_STEP = 2.5f / ARC_SAMPLES

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

    // How far along the oval each angle lies, from its front: books are spaced along the curve,
    // not by angle, so they stand as far apart at the edges as in front.
    private val arcs: FloatArray

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
        arcs = FloatArray(ARC_SAMPLES + 1)
        for (i in 1..ARC_SAMPLES) {
            val mid = (i - 0.5f) * ARC_STEP
            arcs[i] = arcs[i - 1] + radius * ovalSpeed(mid, oval) * ARC_STEP
        }
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

    /** The angle round the oval that lies [arc] along it from its front, either way. */
    fun angleAt(arc: Float): Float {
        val d = abs(arc)
        if (d >= arcs[ARC_SAMPLES]) return (ARC_SAMPLES * ARC_STEP) * sign(arc)
        var low = 0
        var high = ARC_SAMPLES
        while (high - low > 1) {
            val mid = (low + high) / 2
            if (arcs[mid] <= d) low = mid else high = mid
        }
        val t = (d - arcs[low]) / (arcs[high] - arcs[low])
        return (low + t) * ARC_STEP * sign(arc)
    }

    /** How far along the oval from its front the point at [angle] lies, either way. */
    fun arcAt(angle: Float): Float {
        val d = (abs(angle) / ARC_STEP).coerceAtMost(ARC_SAMPLES.toFloat())
        val i = d.toInt().coerceAtMost(ARC_SAMPLES - 1)
        return (arcs[i] + (arcs[i + 1] - arcs[i]) * (d - i)) * sign(angle)
    }

    fun place(index: Int, scroll: Float): RingPlacement? {
        val book = books.getOrNull(index) ?: return null
        val angle = angleAt(book.centre - scroll)
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

// How fast a point moves along the oval per radian round it, for a radius of one.
private fun ovalSpeed(angle: Float, oval: Float): Float {
    val c = cos(angle)
    val s = oval * sin(angle)
    return sqrt(c * c + s * s)
}

// Each book faces square out from the oval, its spine along the curve, as on a carousel.
internal fun ringYaw(angle: Float, oval: Float): Float = atan2(oval * sin(angle), cos(angle))

internal fun ringThicknessFor(spineThickness: Float): Float {
    val t =
        ((spineThickness - MIN_SPINE_THICKNESS) / (MAX_SPINE_THICKNESS - MIN_SPINE_THICKNESS))
            .coerceIn(0f, 1f)
    return RING_THICKNESS * (1f - RING_SPREAD + 2f * RING_SPREAD * t)
}
