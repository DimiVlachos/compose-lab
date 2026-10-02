package dev.dimvlachos.lab.core.presentation.components.paperplane

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// How a sheet of writing paper is folded into a dart, worked out on the flat sheet before anything
// moves. A classic dart, its nose at the right-hand end:
//  1. the two right-hand corners over onto the long centre line, on 45° creases from the nose;
//  2. the slanting edges that leaves over onto the centre line again, which sharpens the nose;
//  3. the bottom half up over the top, along the centre line;
//  4. the wings down from the nose to the tail, their creases running back from the nose to a
//     keel's depth above the spine at the tail, and opened out from it.
//
// Each fold turns the paper away from the eye, as if the sheet were folded from its back: its
// front ends up inside, so from above the dart shows the paper's outside on its wings, and the
// inside on the flaps folded over them.
//
// The paper is cut along every crease it ever gets into facets, flat pieces that each stay
// rigid. Before each fold all the paper lies flat, so a facet's place is a flat move of the sheet
// (a turn, a shift, maybe a mirror) and a layer in the stack; a fold turns the facets on one side
// of its crease about it.

/** The sheet every plane is folded from: writing paper's proportions, in sheet units. */
internal val DartSheet = Size(150f, 100f)

/** A flat move: p' = (a x + b y + tx, c x + d y + ty), a rotation or a mirror plus a shift. */
internal class Flat(
    val a: Float,
    val b: Float,
    val c: Float,
    val d: Float,
    val tx: Float,
    val ty: Float,
) {
    fun map(p: Offset) = Offset(a * p.x + b * p.y + tx, c * p.x + d * p.y + ty)

    /** Undoes the move: a rotation or mirror is undone by its transpose. */
    fun unmap(p: Offset): Offset {
        val x = p.x - tx
        val y = p.y - ty
        return Offset(a * x + c * y, b * x + d * y)
    }

    /** This move, then a mirror in the line through [at] along unit [along]. */
    fun mirrored(at: Offset, along: Offset): Flat {
        val r00 = 2f * along.x * along.x - 1f
        val r01 = 2f * along.x * along.y
        val r11 = 2f * along.y * along.y - 1f
        // p' = R (p - at) + at
        val sx = at.x - (r00 * at.x + r01 * at.y)
        val sy = at.y - (r01 * at.x + r11 * at.y)
        return Flat(
            a = r00 * a + r01 * c,
            b = r00 * b + r01 * d,
            c = r01 * a + r11 * c,
            d = r01 * b + r11 * d,
            tx = r00 * tx + r01 * ty + sx,
            ty = r01 * tx + r11 * ty + sy,
        )
    }

    companion object {
        val None = Flat(1f, 0f, 0f, 1f, 0f, 0f)
    }
}

internal enum class Stack {
    All,
    /** The layers the fold in half turned over. */
    Turned,
    /** The layers it left where they were. */
    Kept,
}

/**
 * A crease in the flat stack: through [at] along unit [along]; the paper on the [side] of it (a
 * unit normal) turns by [angle] degrees, up towards the eye or away from it.
 */
internal class Crease(
    val at: Offset,
    val along: Offset,
    val side: Offset,
    val towardEye: Boolean,
    val angle: Float = 180f,
    val stack: Stack = Stack.All,
) {
    fun reaches(p: Offset) = (p.x - at.x) * side.x + (p.y - at.y) * side.y > 0f
}

/**
 * One fold: its creases turn at once, each over a part of the paper none of the others moves. The
 * paper a fold that [halves] turns over is the stack's turned half from then on.
 */
internal class Fold(val creases: List<Crease>, val halves: Boolean = false)

/**
 * A flat piece of paper between creases: its outline on the sheet, and for each fold, where it lies
 * before it ([poses], [layers]) and which crease moves it, if any.
 */
internal class Facet(
    val polygon: List<Offset>,
    val poses: List<Flat>,
    val layers: List<Int>,
    val moves: List<Crease?>,
) {
    /** Its middle on the sheet. */
    val middle: Offset = polygon.fold(Offset.Zero) { sum, p -> sum + p } / polygon.size.toFloat()
}

/**
 * A sheet's folds and the facets they cut it into. [top] and [bottom] are the highest and lowest
 * layers before each fold, the faces of the stack a fold turns about.
 */
internal class FoldPlan(
    val folds: List<Fold>,
    val facets: List<Facet>,
    val top: List<Int>,
    val bottom: List<Int>,
    /** Where the folded dart balances, on its wing crease midway along: what it turns about. */
    val balance: Offset,
    /** How long the folded dart is, from the tail to the nose. */
    val length: Float,
)

internal fun planFolds(sheet: Size): FoldPlan {
    val w = sheet.width
    val h = sheet.height / 2f
    val folds = mutableListOf<Fold>()
    val tail = 0f
    val nose = Offset(w, h)
    val r = 1f / sqrt(2f)
    folds +=
        Fold(
            listOf(
                Crease(nose, Offset(-r, -r), Offset(r, -r), towardEye = false),
                Crease(nose, Offset(-r, r), Offset(r, r), towardEye = false),
            )
        )
    // Halfway between the slanting edge and the centre line: 22.5° off the line, from the nose.
    val c = cos(PI.toFloat() / 8f)
    val sn = sin(PI.toFloat() / 8f)
    folds +=
        Fold(
            listOf(
                Crease(nose, Offset(-c, -sn), Offset(sn, -c), towardEye = false),
                Crease(nose, Offset(-c, sn), Offset(sn, c), towardEye = false),
            )
        )
    folds +=
        Fold(
            listOf(Crease(Offset(0f, h), Offset(1f, 0f), Offset(0f, 1f), towardEye = false)),
            halves = true,
        )
    // From the nose back to the tail, where the keel is deepest.
    val keelTop = h * (1f - PaperPlaneDimens.KeelShare)
    val back = Offset(tail - w, keelTop - h)
    val along = back / back.getDistance()
    // The wing is the paper above the crease, towards the top edge.
    val up = Offset(along.y, -along.x).let { if (it.y > 0f) -it else it }
    val open = PaperPlaneDimens.WingOpen
    folds +=
        Fold(
            listOf(
                Crease(nose, along, up, towardEye = false, angle = open, stack = Stack.Turned),
                Crease(nose, along, up, towardEye = true, angle = open, stack = Stack.Kept),
            )
        )
    val middle = (tail + w) / 2f
    val balance = Offset(middle, h + (keelTop - h) * (w - middle) / (w - tail))
    return cut(sheet, folds, balance, w - tail)
}

// A facet while the plan is cut: where it lies now, and its history so far.
private data class Cutting(
    val polygon: List<Offset>,
    val pose: Flat,
    val layer: Int,
    val turned: Boolean,
    val poses: List<Flat>,
    val layers: List<Int>,
    val moves: List<Crease?>,
)

private fun cut(sheet: Size, folds: List<Fold>, balance: Offset, length: Float): FoldPlan {
    var facets =
        listOf(
            Cutting(
                listOf(
                    Offset.Zero,
                    Offset(sheet.width, 0f),
                    Offset(sheet.width, sheet.height),
                    Offset(0f, sheet.height),
                ),
                Flat.None,
                0,
                turned = false,
                emptyList(),
                emptyList(),
                emptyList(),
            )
        )
    val tops = mutableListOf<Int>()
    val bottoms = mutableListOf<Int>()
    for (fold in folds) {
        val top = facets.maxOf { it.layer }
        val bottom = facets.minOf { it.layer }
        tops += top
        bottoms += bottom
        facets = facets.flatMap { facet ->
            // Cut by each crease that can move this layer; a piece is moved by the first one
            // that reaches it.
            var pieces = listOf<Pair<List<Offset>, Crease?>>(facet.polygon to null)
            for (crease in fold.creases) {
                val applies =
                    when (crease.stack) {
                        Stack.All -> true
                        Stack.Turned -> facet.turned
                        Stack.Kept -> !facet.turned
                    }
                if (!applies) continue
                pieces = pieces.flatMap { (polygon, moved) ->
                    if (moved != null) return@flatMap listOf(polygon to moved)
                    val flat = polygon.map(facet.pose::map)
                    val beyond = flat.clipTo(crease.side, crease.at)
                    val before = flat.clipTo(-crease.side, crease.at)
                    listOf(
                        before.map(facet.pose::unmap) to null,
                        beyond.map(facet.pose::unmap) to crease,
                    )
                }
            }
            pieces
                .filter { (polygon, _) -> polygon.size >= 3 && area(polygon) > MinFacetArea }
                .map { (polygon, crease) ->
                    // A fold short of flat leaves the paper where the plan last had it flat.
                    val turned = crease?.takeIf { it.angle == 180f }
                    val pose = turned?.let { facet.pose.mirrored(it.at, it.along) } ?: facet.pose
                    val layer =
                        when {
                            turned == null -> facet.layer
                            // Turned over onto the top of the stack, it lies the other way up.
                            turned.towardEye -> 2 * top + 1 - facet.layer
                            else -> 2 * bottom - 1 - facet.layer
                        }
                    Cutting(
                        polygon,
                        pose,
                        layer,
                        facet.turned || (fold.halves && crease != null),
                        facet.poses + facet.pose,
                        facet.layers + facet.layer,
                        facet.moves + crease,
                    )
                }
        }
        // Only the order of the layers counts: numbered afresh, the stack is as thick as the paper
        // in it, not as the turns that built it.
        val rank =
            facets.map { it.layer }.distinct().sorted().withIndex().associate { (i, l) -> l to i }
        facets = facets.map { it.copy(layer = rank.getValue(it.layer)) }
    }
    return FoldPlan(
        folds,
        facets.map { Facet(it.polygon, it.poses, it.layers, it.moves) },
        tops,
        bottoms,
        balance,
        length,
    )
}

// Slivers left where a crease grazes a corner are too small to see and only cost a draw.
private const val MinFacetArea = 0.5f

internal fun area(polygon: List<Offset>): Float = abs(signedArea(polygon))

// What of the polygon lies on the [side] of the line through [at] (Sutherland–Hodgman).
private fun List<Offset>.clipTo(side: Offset, at: Offset): List<Offset> {
    if (isEmpty()) return this
    fun beyond(p: Offset) = (p.x - at.x) * side.x + (p.y - at.y) * side.y
    val out = mutableListOf<Offset>()
    for (i in indices) {
        val p = this[i]
        val q = this[(i + 1) % size]
        val bp = beyond(p)
        val bq = beyond(q)
        if (bp >= 0f) out += p
        if ((bp > 0f && bq < 0f) || (bp < 0f && bq > 0f)) out += p + (q - p) * (bp / (bp - bq))
    }
    // A crease through a corner leaves it twice: once is enough, and a zero-length edge has no
    // outward side to grow along.
    return out.filterIndexed { i, p -> (p - out[(i + 1) % out.size]).getDistance() > SameCorner }
}

private const val SameCorner = 1e-3f

/**
 * The polygon pushed out by [by] along every edge: a convex facet, grown to overlap its neighbours.
 * A sharp corner is cut off square rather than drawn out to a point: grown to a point, the 8°
 * corners at the nose would reach a dozen units past it.
 */
internal fun List<Offset>.grown(by: Float): List<Offset> {
    if (size < 3) return this
    val sign = if (signedArea(this) > 0f) 1f else -1f
    // Each edge moved outward, then the corners where neighbouring edges meet again.
    val lines = indices.map { i ->
        val p = this[i]
        val q = this[(i + 1) % size]
        val e = q - p
        val len = e.getDistance().coerceAtLeast(1e-6f)
        val out = Offset(e.y, -e.x) * (sign / len)
        Pair(p + out * by, e)
    }
    val out = mutableListOf<Offset>()
    for (i in indices) {
        val (p1, e1) = lines[(i + size - 1) % size]
        val (p2, e2) = lines[i]
        val cross = e1.x * e2.y - e1.y * e2.x
        if (abs(cross) < 1e-6f) {
            out += p2
            continue
        }
        val t = ((p2.x - p1.x) * e2.y - (p2.y - p1.y) * e2.x) / cross
        val corner = p1 + e1 * t
        if ((corner - this[i]).getDistance() <= MitreLimit * by) {
            out += corner
        } else {
            // Bevelled: the end of the one edge, moved out, and the start of the next.
            out += p1 + e1
            out += p2
        }
    }
    return out
}

private const val MitreLimit = 2f

internal fun signedArea(polygon: List<Offset>): Float {
    var twice = 0f
    for (i in polygon.indices) {
        val p = polygon[i]
        val q = polygon[(i + 1) % polygon.size]
        twice += p.x * q.y - q.x * p.y
    }
    return twice / 2f
}
