package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

internal const val MAX_BOOKS_PER_PLANK = 9
internal const val MIN_SPINE_THICKNESS = 22f
internal const val MAX_SPINE_THICKNESS = 42f

private const val MAX_SCALE = 1.35f
private const val TALLEST_BOOK = 240f
private const val BOOK_GAP = 0.8f
private const val SHORT_BOOK_SECONDS = 2 * 3600.0
private const val LONG_BOOK_SECONDS = 16 * 3600.0
private const val SINGLE_PLANK_Y = 0.78f
private const val TOP_PLANK_Y = 0.46f
private const val BOTTOM_PLANK_Y = 0.9f
private const val BOOK_BAND = 0.36f
private const val MIN_PLANK_SPAN = 0.72f
private const val SINGLE_PAGE_SHARE = 0.8f
private const val SPREAD_PAGE_SHARE = 0.94f

internal data class ShelfBookSpec(
    val id: Int,
    val thickness: Float,
    val height: Float,
    val depth: Float,
)

internal data class ShelfSlot(
    val bookId: Int,
    val x: Float,
    val thickness: Float,
    val height: Float,
    val depth: Float,
)

internal data class PlankLayout(
    val plankY: Float,
    val margin: Float,
    val slots: List<ShelfSlot>,
    val left: Float,
    val right: Float,
)

internal data class OpenBand(
    val top: Float,
    val bottom: Float,
    val left: Float,
    val right: Float,
    val spread: Boolean = false,
    val gutter: Float? = null,
)

internal data class OpenPlacement(val centerX: Float, val centerY: Float, val pageWidth: Float)

internal fun OpenBand.place(depth: Float, height: Float): OpenPlacement {
    val upright = (bottom - top) * depth / height
    val centerY = (top + bottom) / 2f
    if (!spread) {
        return OpenPlacement(
            (left + right) / 2f,
            centerY,
            minOf((right - left) * SINGLE_PAGE_SHARE, upright),
        )
    }
    val spine = gutter?.takeIf { it > left && it < right } ?: ((left + right) / 2f)
    val width = minOf(minOf(spine - left, right - spine) * SPREAD_PAGE_SHARE, upright)
    return OpenPlacement(spine + width / 2f, centerY, width)
}

internal data class ShelfLayout(
    val width: Float,
    val height: Float,
    val scale: Float,
    val planks: List<PlankLayout>,
    val openBand: OpenBand? = null,
    val camera: ShelfCamera? = null,
    val rest: ((Int) -> BookRest?)? = null,
)

internal fun layoutShelf(
    width: Float,
    height: Float,
    planks: List<List<ShelfBookSpec>>,
): ShelfLayout {
    if (planks.isEmpty()) return ShelfLayout(width, height, 1f, emptyList())
    val fractions =
        if (planks.size == 1) listOf(SINGLE_PLANK_Y) else listOf(TOP_PLANK_Y, BOTTOM_PLANK_Y)
    val bandScale = (if (planks.size == 1) BOOK_BAND * 1.4f else BOOK_BAND) * height / TALLEST_BOOK
    val scale = minOf(MAX_SCALE, bandScale)
    val margin = maxOf(16f, width * 0.05f)
    val laidOut =
        planks.take(fractions.size).mapIndexed { index, books ->
            layoutPlank(books, plankY = height * fractions[index], width, margin, scale)
        }
    val widest = laidOut.maxOf { plank -> plank.right - plank.left }
    val span = maxOf(widest, width * MIN_PLANK_SPAN).coerceAtMost(width - margin * 2f)
    val left = (width - span) / 2f
    return ShelfLayout(
        width,
        height,
        scale,
        laidOut.map { it.copy(left = minOf(left, it.left), right = maxOf(left + span, it.right)) },
    )
}

internal fun spineThicknessFor(durationSeconds: Double?): Float {
    val seconds = durationSeconds ?: return MIN_SPINE_THICKNESS
    val t =
        ((seconds - SHORT_BOOK_SECONDS) / (LONG_BOOK_SECONDS - SHORT_BOOK_SECONDS)).coerceIn(
            0.0,
            1.0,
        )
    return mix(MIN_SPINE_THICKNESS, MAX_SPINE_THICKNESS, t.toFloat())
}

private fun layoutPlank(
    books: List<ShelfBookSpec>,
    plankY: Float,
    width: Float,
    margin: Float,
    scale: Float,
): PlankLayout {
    val available = width - margin * 2f
    var used = 0f
    val fitting =
        books.take(MAX_BOOKS_PER_PLANK).takeWhile { book ->
            val next = used + book.thickness * scale + if (used > 0f) BOOK_GAP * scale else 0f
            (next <= available).also { if (it) used = next }
        }
    var x = (width - used) / 2f
    val slots = fitting.map { book ->
        ShelfSlot(book.id, x, book.thickness * scale, book.height * scale, book.depth * scale)
            .also { x += (book.thickness + BOOK_GAP) * scale }
    }
    val left = slots.firstOrNull()?.x ?: (width / 2f)
    val right = slots.lastOrNull()?.let { it.x + it.thickness } ?: (width / 2f)
    return PlankLayout(plankY, margin, slots, left, right)
}

internal fun ShelfLayout.onStage(
    x: Float,
    y: Float,
    stageWidth: Float,
    stageHeight: Float,
    openBand: OpenBand? = null,
): ShelfLayout =
    copy(
        width = stageWidth,
        height = stageHeight,
        openBand = openBand,
        camera =
            camera?.copy(vanishingX = camera.vanishingX + x, vanishingY = camera.vanishingY + y),
        rest = rest?.let { lookup -> { id -> lookup(id)?.shifted(x, y) } },
        planks =
            planks.map { plank ->
                plank.copy(
                    plankY = plank.plankY + y,
                    margin = plank.margin + x,
                    left = plank.left + x,
                    right = plank.right + x,
                    slots = plank.slots.map { it.copy(x = it.x + x) },
                )
            },
    )

private fun BookRest.shifted(x: Float, y: Float) =
    copy(
        x = this.x + x,
        occluders = occluders.map { outline -> outline.map { Point2(it.x + x, it.y + y) } },
        row = row?.let { it.copy(point = it.point + Vec3(x, y, 0f)) },
    )
