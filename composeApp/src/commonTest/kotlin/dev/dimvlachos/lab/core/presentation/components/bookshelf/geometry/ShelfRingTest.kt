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
    fun neighbouringSpinesNeverOverlapAnywhereOnTheOval() {
        // Measured where the eye sees them: the spines' own front corners, along the oval.
        var scroll = ring.start
        while (scroll <= ring.end) {
            // Past the oval's sides the books face away, round behind; only fronts that face the
            // viewer can be seen to touch.
            val placed =
                ring.books.indices
                    .mapNotNull { ring.place(it, scroll) }
                    .filter { abs(it.angle) < kotlin.math.PI.toFloat() / 2f }
                    .sortedBy { it.angle }
            placed.zipWithNext { a, b ->
                val aRight = a.pose.map(Vec3(a.pose.dimensions.thickness, 0f, 0f)).x
                val bLeft = b.pose.map(Vec3(0f, 0f, 0f)).x
                assertTrue(
                    bLeft >= aRight - 1e-3f,
                    "spines ${a.index} and ${b.index} overlap by ${aRight - bLeft} at $scroll",
                )
            }
            scroll += 7.3f
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

    @Test
    fun everyBookTurnsAlongTheOval() {
        // Facing out from the oval: square to it, so along its tangent, flatter than a circle's.
        for (angle in listOf(-1.2f, -0.6f, -0.2f, 0f, 0.2f, 0.6f, 1.2f)) {
            val expected =
                kotlin.math.atan2(ring.oval * kotlin.math.sin(angle), kotlin.math.cos(angle))
            assertEquals(expected, ringYaw(angle, ring.oval), 1e-6f)
        }
        assertTrue(abs(ringYaw(0.6f, ring.oval)) in 0.1f..0.6f)
    }

    @Test
    fun theRingIsAnOvalShallowerThanItIsWide() {
        val scroll = ring.books[20].centre
        val off = requireNotNull(ring.place(26, scroll))
        val world = off.pose.map(Vec3(off.pose.dimensions.thickness / 2f, 0f, 0f))
        val circleDepth = ring.radius * (1f - kotlin.math.cos(off.angle))

        assertTrue(ring.oval < 1f)
        assertEquals(-circleDepth * ring.oval, world.z, 1e-3f)
    }

    @Test
    fun theRingRunsPastTheScreenEdges() {
        val scroll = ring.books[20].centre
        val placed = ring.visible(scroll)
        val xs = placed.map { it.pose.map(Vec3(0f, 0f, 0f)).x }

        assertTrue(
            xs.minOrNull()!! < 0f && xs.maxOrNull()!! > ring.width,
            "x ${xs.minOrNull()}..${xs.maxOrNull()}",
        )
    }

    @Test
    fun theRingKeepsItsRadiusHoweverManyBooks() {
        assertEquals(412f * 0.62f, ring.radius, 1e-3f)
        assertEquals(384f * 0.62f, ShelfRing(384f, 330f, specs.take(8)).radius, 1e-3f)
    }
}
