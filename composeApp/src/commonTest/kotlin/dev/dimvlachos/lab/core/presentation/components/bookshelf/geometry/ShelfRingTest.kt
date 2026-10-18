package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShelfRingTest {

    private val specs =
        (1..40).map { id ->
            ShelfBookSpec(
                id = id,
                thickness = MIN_SPINE_THICKNESS + (id * 7 % 20),
                height = 198f + (id * 53 % 42),
                depth = 140f,
            )
        }

    private val ring = ShelfRing(width = 412f, height = 410f, specs = specs)

    @Test
    fun bookCentresIncreaseAlongTheRing() {
        ring.books.zipWithNext { a, b -> assertTrue(b.centre > a.centre) }
    }

    @Test
    fun spineThicknessVariesByAtMostTwentyPercent() {
        val widths = specs.map { ringThicknessFor(it.thickness) }
        assertTrue(widths.max() / widths.min() <= 1.2f / 0.8f + 1e-4f)
    }

    @Test
    fun aSnappedScrollCentresABookFacingTheCamera() {
        val scroll = ring.snapTarget(ring.books[12].centre + 3f)
        val placed = requireNotNull(ring.place(12, scroll))

        assertEquals(ring.books[12].centre, scroll, 1e-4f)
        assertEquals(0f, placed.angle, 1e-5f)
    }

    @Test
    fun neighboursNeverOverlapAtAnyScroll() {
        var scroll = ring.start
        while (scroll <= ring.end) {
            val placed = ring.books.indices.mapNotNull { ring.place(it, scroll) }
            placed.zipWithNext { a, b ->
                val aRight = a.angle * ring.radius + a.pose.dimensions.thickness / 2f
                val bLeft = b.angle * ring.radius - b.pose.dimensions.thickness / 2f
                assertTrue(bLeft >= aRight, "overlap at $scroll between ${a.index} and ${b.index}")
            }
            scroll += 1.7f
        }
    }

    @Test
    fun aboutSevenSpinesFaceTheViewer() {
        val facing =
            ring.visible(ring.books[20].centre).count {
                abs(it.angle) < (50.0 * kotlin.math.PI / 180.0)
            }
        assertTrue(facing in 6..9)
    }

    @Test
    fun booksAreDrawnFarToNear() {
        val placed = ring.visible(ring.books[20].centre)
        placed.zipWithNext { a, b -> assertTrue(abs(a.angle) >= abs(b.angle)) }
        assertEquals(20, placed.last().index)
    }

    @Test
    fun booksNearTheCentreStandSquareAndEdgeBooksTurnWithTheRing() {
        val scroll = ring.books[20].centre
        val near = requireNotNull(ring.place(21, scroll))
        val far = requireNotNull(ring.place(26, scroll))

        assertTrue(abs(near.pose.yaw) < abs(near.angle) * 0.2f)
        assertTrue(abs(far.pose.yaw) > abs(far.angle) * 0.6f)
        val normal = far.pose.mapDirection(Vec3(0f, 0f, 1f))
        assertEquals(kotlin.math.sin(far.pose.yaw), normal.x, 1e-5f)
        assertEquals(kotlin.math.cos(far.pose.yaw), normal.z, 1e-5f)
    }

    @Test
    fun theTwoBooksEitherSideOfTheCentreNeverCutIntoEachOther() {
        var scroll = ring.books[18].centre
        while (scroll <= ring.books[22].centre) {
            val placed = ring.books.indices.mapNotNull { ring.place(it, scroll) }
            val a = placed.filter { it.angle < 0f }.maxBy { it.angle }
            val b = placed.filter { it.angle >= 0f }.minBy { it.angle }
            val right = footprint(a).maxOf { it.x }
            val left = footprint(b).minOf { it.x }
            assertTrue(left >= right, "books ${a.index} and ${b.index} cross at $scroll")
            scroll += 0.9f
        }
    }

    private fun footprint(p: RingPlacement): List<Vec3> {
        val (t, _, d) = p.pose.dimensions
        return listOf(Vec3(0f, 0f, 0f), Vec3(t, 0f, 0f), Vec3(t, 0f, -d), Vec3(0f, 0f, -d))
            .map(p.pose::map)
    }

    @Test
    fun aBookTakenOffTheRingStartsExactlyWhereTheRingDrewIt() {
        val scroll = ring.books[20].centre + 7f
        for (index in listOf(17, 20, 23)) {
            val id = ring.books[index].id
            val ringPose = requireNotNull(ring.place(index, scroll)).pose
            val layout = ring.heroLayout { ring.rest(it, scroll) }.onStage(30f, 120f, 480f, 900f)
            val plank = layout.planks.first { p -> p.slots.any { it.bookId == id } }
            val slot = plank.slots.single()
            val rest = requireNotNull(layout.rest?.invoke(id))
            val heldPose =
                BookPose(
                    dimensions = BookDimensions(slot.thickness, slot.height, slot.depth),
                    slotX = rest.x,
                    plankY = plank.plankY,
                    shelfCamera = requireNotNull(layout.camera),
                    held = HeldPlacement(centerX = 240f, centerY = 450f, scale = 2.4f),
                    liftArc = 40f,
                    travel = 0f,
                    tilt = 0f,
                    open = 0f,
                    restZ = rest.z,
                    restYaw = rest.yaw,
                    row = rest.row,
                )
            val (t, h, d) = heldPose.dimensions
            for (corner in
                listOf(Vec3(0f, 0f, 0f), Vec3(t, h, 0f), Vec3(t, h, -d), Vec3(0f, h, -d))) {
                val drawn = ring.camera.project(ringPose.map(corner))
                val held = heldPose.camera.project(heldPose.map(corner))
                assertEquals(drawn.x + 30f, held.x, 1e-3f, message = "book $index x")
                assertEquals(drawn.y + 120f, held.y, 1e-3f, message = "book $index y")
            }
            assertTrue(heldPose.rowDistance(heldPose.map(Vec3(0f, h, -d))) < 0f)
            assertTrue(heldPose.rowDistance(heldPose.map(Vec3(t, h, -d))) < 0f)
        }
    }

    @Test
    fun onlyNearerBooksHideABookBeingPulled() {
        val scroll = ring.books[20].centre
        val side = requireNotNull(ring.rest(ring.books[23].id, scroll))
        val centre = requireNotNull(ring.rest(ring.books[20].id, scroll))

        assertTrue(side.occluders.isNotEmpty())
        assertTrue(side.row != null)
        assertTrue(centre.occluders.isEmpty())
        assertTrue(centre.row == null)
        assertTrue(side.shade <= centre.shade)
    }

    @Test
    fun nearestIndexTracksTheScroll() {
        assertEquals(0, ring.nearestIndex(ring.start - 100f))
        assertEquals(ring.books.lastIndex, ring.nearestIndex(ring.end + 100f))
        assertEquals(7, ring.nearestIndex(ring.books[7].centre))
    }
}
