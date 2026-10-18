package dev.dimvlachos.lab.core.presentation.components.bookshelf.geometry

import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookPoseTest {

    private val dims = BookDimensions(thickness = 30f, height = 220f, depth = 150f)
    private val camera = ShelfCamera(vanishingX = 200f, vanishingY = 300f, focal = 1100f)
    private val held = HeldPlacement(centerX = 200f, centerY = 380f, scale = 1.8f)

    private fun pose(
        travel: Float = 0f,
        tilt: Float = 0f,
        open: Float = 0f,
    ) =
        BookPose(
            dimensions = dims,
            slotX = 40f,
            plankY = 450f,
            shelfCamera = camera,
            held = held,
            liftArc = 40f,
            travel = travel,
            tilt = tilt,
            open = open,
        )

    @Test
    fun aBookAtRestStandsOnItsSlotWithItsSpineInTheWallPlane() {
        val p = pose()

        assertVec(Vec3(40f, 450f, 0f), p.map(Vec3(0f, 0f, 0f)))
        assertVec(Vec3(70f, 230f, 0f), p.map(Vec3(30f, 220f, 0f)))
    }

    @Test
    fun tiltingRotatesTheBookAboutItsBottomFrontEdgeTowardsTheViewer() {
        val theta = 0.3f
        val top = pose(tilt = theta).map(Vec3(0f, 220f, 0f))

        assertVec(Vec3(40f, 450f - 220f * cos(theta), 220f * sin(theta)), top)
    }

    @Test
    fun aFullyTravelledBookIsCentredOnTheHeldPlacementWithItsCoverFacingTheCamera() {
        val p = pose(travel = 1f)
        val centre = p.map(Vec3(15f, 110f, -75f))

        val projected = camera.project(centre)
        assertEquals(held.centerX, projected.x, EPS)
        assertEquals(held.centerY, projected.y, EPS)
        assertVec(Vec3(0f, 0f, 1f), p.mapDirection(Vec3(1f, 0f, 0f)))
    }

    @Test
    fun projectionIsIdentityInTheWallPlaneAndScalesByTheHeldScaleAtHeldDepth() {
        assertEquals(Point2(123f, 45f), camera.project(Vec3(123f, 45f, 0f)))

        val depth = held.depth(camera)
        val p = camera.project(Vec3(camera.vanishingX + 10f, camera.vanishingY, depth))
        assertEquals(camera.vanishingX + 10f * held.scale, p.x, EPS)
    }

    @Test
    fun aBookOnTheShelfShowsItsSpineAndHidesItsForeEdge() {
        val faces = buildBookFaces(pose(), readFraction = null).map { it.texture }

        assertTrue(FaceTexture.Spine in faces)
        assertFalse(FaceTexture.Fore in faces)
    }

    @Test
    fun aFullyOpenBookShowsBothPagesAndHidesTheClosedCover() {
        val faces = buildBookFaces(pose(travel = 1f, open = 1f), readFraction = 0.45f)
        val textures = faces.map { it.texture }

        assertTrue(FaceTexture.RightPage in textures)
        assertTrue(FaceTexture.LeftPage in textures)
        assertFalse(FaceTexture.Cover in textures)
    }

    @Test
    fun facesAreOrderedFromFarToNear() {
        val depths = buildBookFaces(pose(travel = 0.5f, tilt = 0.2f), null).map { it.depth }

        assertEquals(depths.sorted(), depths)
    }

    @Test
    fun theHingeSplitsTheBlockAtTheReadFractionClamped() {
        assertEquals(30f * (1f - 0.45f), hingeX(30f, 0.45f), EPS)
        assertEquals(30f * (1f - 0.12f), hingeX(30f, 0.01f), EPS)
        assertEquals(30f * (1f - 0.1f), hingeX(30f, null), EPS)
    }

    @Test
    fun theOutPathStartsAtTheReleaseTiltAndLandsUpright() {
        assertEquals(0.4f, TiltPaths.out(releaseTilt = 0.4f, travel = 0f), EPS)
        assertEquals(0f, TiltPaths.out(releaseTilt = 0.4f, travel = 1f), EPS)
        assertEquals(0f, TiltPaths.back(travel = 0f), EPS)
        assertEquals(0f, TiltPaths.back(travel = 0.1f), EPS)
        assertTrue(TiltPaths.back(travel = 0.5f) <= 0f)
    }

    @Test
    fun aFaceWhollyInFrontOfTheRowHasNothingBehindTheSpines() {
        val square = listOf(Vec3(0f, 0f, 5f), Vec3(1f, 0f, 5f), Vec3(1f, 1f, 5f), Vec3(0f, 1f, 5f))

        assertEquals(4, square.inFrontOfRow().size)
        assertTrue(square.insideRow().isEmpty())
    }

    @Test
    fun aFaceThatCrossesTheSpineLineIsCutExactlyAtIt() {
        val square =
            listOf(Vec3(0f, 0f, 2f), Vec3(1f, 0f, 2f), Vec3(1f, 0f, -2f), Vec3(0f, 0f, -2f))

        val front = square.inFrontOfRow()
        val inside = square.insideRow()

        assertEquals(4, front.size)
        assertEquals(4, inside.size)
        assertTrue(front.all { it.z >= 0f })
        assertTrue(inside.all { it.z <= 0f })
        assertEquals(2, front.count { it.z == 0f })
    }

    @Test
    fun aTippedBookKeepsThePartStillInsideTheRowSeparate() {
        val faces = buildBookFaces(pose(tilt = 0.3f), readFraction = null)
        val spine = faces.first { it.texture == FaceTexture.Spine }
        val cover = faces.firstOrNull { it.texture == FaceTexture.Cover }

        assertTrue(spine.insideRow.isEmpty())
        cover?.let { assertTrue(it.insideRow.size >= 3) }
    }

    @Test
    fun theShelfCameraNeverMovesAnythingLyingOnTheSpineLine() {
        val shelfCamera = camera.onShelf(bookCentreX = 55f, swing = 0f)
        val onSpineLine = Vec3(40f, 300f, 0f)

        assertEquals(camera.project(onSpineLine), shelfCamera.project(onSpineLine))
    }

    @Test
    fun aTippingBookNeitherDriftsSidewaysNorSwellsAsMuch() {
        val shelfCamera = camera.onShelf(bookCentreX = 55f, swing = 0f)
        val left = Vec3(40f, 230f, 60f)
        val right = Vec3(70f, 230f, 60f)

        val plainWidth = camera.project(right).x - camera.project(left).x
        val shelfWidth = shelfCamera.project(right).x - shelfCamera.project(left).x
        val shelfCentre = (shelfCamera.project(left).x + shelfCamera.project(right).x) / 2f

        assertTrue(shelfWidth < plainWidth)
        assertEquals(55f, shelfCentre, 1e-3f)
    }

    @Test
    fun onceTheBookIsOutItUsesTheOrdinaryCamera() {
        assertEquals(camera, camera.onShelf(bookCentreX = 55f, swing = 0.3f))
        assertEquals(camera, camera.onShelf(bookCentreX = 55f, swing = 1f))
    }

    @Test
    fun aBookStaysInItsOwnColumnUntilItHasLeftTheRow() {
        for (tilt in listOf(0f, 0.3f)) {
            for (step in 0..100) {
                val p = pose(travel = step / 100f, tilt = tilt * (1f - step / 100f))
                val corners = boxCorners().map(p::map)
                if (corners.none { it.z < 0f }) continue
                corners.forEach {
                    assertTrue(it.x in 39.5f..70.5f, "travel ${step / 100f} x ${it.x}")
                }
            }
        }
    }

    @Test
    fun aBookOnlySwingsAndTurnsOnceItsBackIsClear() {
        val start = (0..100).first { pose(travel = it / 100f).swing > 0f } / 100f
        val clear = pose(travel = start)

        assertTrue(boxCorners().map(clear::map).all { it.z >= 0f })
        assertTrue(start in 0.15f..0.5f)
        assertEquals(1f, pose(travel = 1f).swing, EPS)
    }

    private fun boxCorners() =
        listOf(0f, 30f).flatMap { x ->
            listOf(0f, 220f).flatMap { y -> listOf(0f, -150f).map { z -> Vec3(x, y, z) } }
        }

    private fun assertVec(expected: Vec3, actual: Vec3) {
        assertEquals(expected.x, actual.x, EPS, message = "x")
        assertEquals(expected.y, actual.y, EPS, message = "y")
        assertEquals(expected.z, actual.z, EPS, message = "z")
    }

    private companion object {
        const val EPS = 1e-3f
    }
}
