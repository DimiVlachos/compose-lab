package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.ui.geometry.Offset
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhotoBodiesTest {
    private val step = MagnetDimens.StepSeconds
    private val ring = MagnetDimens.ContactRing.value
    private val card = MagnetDimens.CardWidth.value

    private fun magnet(tag: String, at: Offset) =
        Magnet(tag).apply {
            this.at = at
            onTable = true
        }

    // Bodies on a 1000 × 1000 dp table, each lying where it is put.
    private fun bodies(vararg placed: Pair<PhotoBody, Offset>): PhotoBodies =
        PhotoBodies(placed.map { it.first }).apply {
            resize(1000f, 1000f)
            for ((body, at) in placed) {
                body.home = at
                body.at = at
            }
        }

    private fun PhotoBodies.run(seconds: Float, magnets: List<Magnet>) {
        repeat((seconds / step).toInt()) { step(step, magnets) }
    }

    @Test
    fun aStrongMatchSnapsAndSticksWithinAboutAThirdOfASecond() {
        val photo = PhotoBody("p", mapOf("a" to 0.9f), Offset.Zero)
        // A card's width beyond touching.
        val m = magnet("a", Offset(300f + ring + card, 500f))
        val table = bodies(photo to Offset(300f, 500f))
        var steps = 0
        while ("a" !in photo.stuckTo && steps < 120) {
            table.step(step, listOf(m))
            steps++
        }
        assertTrue("a" in photo.stuckTo, "never stuck")
        assertTrue(steps * step <= 0.4f, "took ${steps * step} s")
        assertEquals(1, table.sticks)
    }

    @Test
    fun aMatchUnderTheThresholdNeitherMovesNorSticks() {
        // Only what will be counted moves: a weak match stays where it lies.
        val photo = PhotoBody("p", mapOf("a" to 0.4f), Offset.Zero)
        val m = magnet("a", Offset(300f + ring + card, 500f))
        val table = bodies(photo to Offset(300f, 500f))
        table.run(3f, listOf(m))
        assertTrue(photo.stuckTo.isEmpty())
        assertEquals(photo.home, photo.at)
    }

    @Test
    fun aPhotoThatDoesNotMatchNeverMoves() {
        val photo = PhotoBody("p", mapOf("b" to 1f), Offset.Zero)
        val table = bodies(photo to Offset(300f, 500f))
        table.run(2f, listOf(magnet("a", Offset(400f, 500f))))
        assertEquals(photo.home, photo.at)
    }

    @Test
    fun aTagMissingFromAPhotosStrengthsCountsAsNothing() {
        val photo = PhotoBody("p", emptyMap(), Offset.Zero)
        assertEquals(0f, photo.strength("a"))
        assertFalse(photo.matches("a"))
    }

    @Test
    fun releasedEachPhotoSlidesBackHome() {
        val photos = List(3) { PhotoBody("p$it", mapOf("a" to 1f), Offset.Zero) }
        val m = magnet("a", Offset(500f, 500f))
        val table =
            bodies(
                photos[0] to Offset(380f, 500f),
                photos[1] to Offset(620f, 500f),
                photos[2] to Offset(500f, 380f),
            )
        table.run(2f, listOf(m))
        assertTrue(photos.all { "a" in it.stuckTo })
        assertTrue(table.releaseAll("a"))
        table.run(3f, emptyList())
        for (photo in photos) {
            assertTrue((photo.at - photo.home).getDistance() < 1f, "${photo.id} at ${photo.at}")
        }
        assertTrue(table.atRest)
    }

    @Test
    fun cardsOnAMagnetDoNotPileUp() {
        val photos = List(6) { PhotoBody("p$it", mapOf("a" to 1f), Offset.Zero) }
        val m = magnet("a", Offset(500f, 500f))
        val table =
            // All within the magnet's reach, a card's width or more apart.
            bodies(*photos.mapIndexed { i, p -> p to Offset(300f + i * 80f, 400f) }.toTypedArray())
        table.run(3f, listOf(m))
        val span = MagnetDimens.CardSpan.value
        for (i in photos.indices) for (j in i + 1 until photos.size) {
            val d = (photos[i].at - photos[j].at).getDistance()
            assertTrue(d >= span * 0.9f, "${photos[i].id} and ${photos[j].id} are $d apart")
        }
        assertTrue(table.atRest, "the cluster should settle")
    }

    @Test
    fun nothingGoesNaNAtZeroDistance() {
        val a = PhotoBody("a", mapOf("a" to 1f), Offset.Zero)
        val b = PhotoBody("b", mapOf("a" to 1f), Offset.Zero)
        val m = magnet("a", Offset(500f, 500f))
        val table = bodies(a to Offset(500f, 500f), b to Offset(500f, 500f))
        table.run(1f, listOf(m))
        for (p in listOf(a, b)) {
            assertTrue(p.at.x.isFinite() && p.at.y.isFinite(), "${p.id} at ${p.at}")
            assertTrue(p.velocity.x.isFinite() && p.velocity.y.isFinite())
        }
    }

    @Test
    fun aPhotoMatchingTwoNearbyMagnetsHangsBetweenThemOnBoth() {
        val photo = PhotoBody("p", mapOf("a" to 0.9f, "b" to 0.9f), Offset.Zero)
        val a = magnet("a", Offset(400f, 500f))
        val b = magnet("b", Offset(520f, 500f))
        val table = bodies(photo to Offset(460f, 620f))
        table.run(3f, listOf(a, b))
        assertEquals(setOf("a", "b"), photo.stuckTo)
        assertTrue((photo.at - Offset(460f, 500f)).getDistance() < 5f, "at ${photo.at}")
    }

    @Test
    fun broughtNearAPhotoAlreadyStuckToOneMagnetTheSecondSharesIt() {
        val photo = PhotoBody("p", mapOf("a" to 0.9f, "b" to 0.7f), Offset.Zero)
        val a = magnet("a", Offset(400f, 500f))
        val table = bodies(photo to Offset(470f, 500f))
        table.run(1f, listOf(a))
        assertEquals(setOf("a"), photo.stuckTo)
        val b = magnet("b", Offset(520f, 500f))
        table.run(1.5f, listOf(a, b))
        assertEquals(setOf("a", "b"), photo.stuckTo)
        assertTrue((photo.at - Offset(460f, 500f)).getDistance() < 5f, "between, at ${photo.at}")
    }

    // A photo on both of two magnets, A 0.9 and B 0.6, and the magnet [moved] taken away to the
    // right at [speed] dp a second, until they are too far apart to share it.
    private fun pullApart(moved: String, speed: Float): Set<String> {
        val photo = PhotoBody("p", mapOf("a" to 0.9f, "b" to 0.6f), Offset.Zero)
        val a = magnet("a", Offset(400f, 500f))
        val b = magnet("b", Offset(520f, 500f))
        val table = bodies(photo to Offset(460f, 560f))
        table.run(2f, listOf(a, b))
        assertEquals(setOf("a", "b"), photo.stuckTo)
        // The other magnet stays put; this one goes right, or left for A, out of sharing range.
        val going = if (moved == "a") a else b
        val away = if (moved == "a") -1f else 1f
        repeat(600) {
            if (photo.stuckTo.size == 1) return photo.stuckTo
            going.at += Offset(away * speed * step, 0f)
            table.step(step, listOf(a, b))
        }
        return photo.stuckTo
    }

    @Test
    fun pulledSlowlyApartAPhotoOnBothGoesWithTheTagItMatchesBetter() {
        assertEquals(setOf("a"), pullApart("b", speed = 40f))
        assertEquals(setOf("a"), pullApart("a", speed = 40f))
    }

    @Test
    fun yankedApartAPhotoOnBothStaysWithTheMagnetLeftBehind() {
        // Left nearer the magnet that stayed, it is pulled harder by it, whichever it matches
        // better.
        assertEquals(setOf("a"), pullApart("b", speed = 1_200f))
        assertEquals(setOf("b"), pullApart("a", speed = 1_200f))
    }

    @Test
    fun aPhotoTwoMagnetsShareHangsTouchingBothClusters() {
        // At the furthest two magnets still share it, the middle between them is no further from
        // either than a card on its first ring, and a card's width beyond: it reaches both.
        val furthest = MagnetDimens.BridgeSpan.value * MagnetDimens.BridgeLetGo
        assertTrue(
            furthest / 2f <= 46f + MagnetDimens.CardSpan.value,
            "it hangs ${furthest / 2f} dp off",
        )
    }

    @Test
    fun aPhotoPulledOffByHandDoesNotStickBackUntilItsMagnetIsPutAway() {
        val photo = PhotoBody("p", mapOf("a" to 1f), Offset.Zero)
        val m = magnet("a", Offset(500f, 500f))
        val table = bodies(photo to Offset(440f, 500f))
        table.run(1f, listOf(m))
        assertEquals(setOf("a"), photo.stuckTo)
        table.grab(photo)
        assertTrue(photo.stuckTo.isEmpty())
        photo.at = Offset(500f - ring - 5f, 500f)
        table.drop(photo)
        table.run(2f, listOf(m))
        assertTrue(photo.stuckTo.isEmpty(), "it must not snap back")
        // The magnet goes back to the strip and comes out again: now it may stick.
        table.releaseAll("a")
        table.run(2f, listOf(m))
        assertEquals(setOf("a"), photo.stuckTo)
    }

    @Test
    fun aPhotoFeelsOnlyMagnetsItMatchesAndHasNotRefused() {
        val photo = PhotoBody("p", mapOf("a" to 0.3f), Offset.Zero)
        val a = magnet("a", Offset.Zero)
        val b = magnet("b", Offset.Zero)
        assertTrue(photo.feelsAny(listOf(a, b)))
        assertFalse(photo.feelsAny(listOf(b)))
        photo.refused += "a"
        assertFalse(photo.feelsAny(listOf(a, b)))
        // A magnet in the strip pulls nothing.
        photo.refused.clear()
        a.onTable = false
        assertFalse(photo.feelsAny(listOf(a)))
    }

    // A magnet carrying one photo it holds swept right across [other], lying in its way, and on
    // past it; then left for three seconds. Returns how far [other] was pushed at most.
    private fun sweepPast(other: PhotoBody): Float {
        val match = PhotoBody("match", mapOf("a" to 1f), Offset.Zero)
        val m = magnet("a", Offset(300f, 500f))
        val table = bodies(match to Offset(300f, 560f), other to Offset(520f, 500f))
        table.run(1f, listOf(m))
        assertEquals(setOf("a"), match.stuckTo)
        var furthest = 0f
        repeat(300) {
            m.at = Offset(300f + it * 2f, 500f)
            table.step(step, listOf(m))
            furthest = maxOf(furthest, (other.at - other.home).getDistance())
            assertTrue(other.stuckTo.isEmpty(), "it stuck at step $it")
        }
        table.run(3f, listOf(m))
        return furthest
    }

    @Test
    fun aPhotoAMagnetDoesNotPullIsNudgedAsideWhereItTouchesAndSlidesBackHome() {
        val other = PhotoBody("other", mapOf("b" to 1f), Offset.Zero)
        val furthest = sweepPast(other)
        assertTrue(furthest > 10f, "it wasn't nudged")
        // Swept straight through, it is bumped, not carried off: past a nudge it slips under.
        assertTrue(furthest <= MagnetDimens.MostNudge.value * 1.5f, "it was carried $furthest dp")
        assertTrue((other.at - other.home).getDistance() < 1f, "it didn't slide home: ${other.at}")
    }

    @Test
    fun aWeakMatchIsNudgedTooButNeverDrawnIn() {
        val weak = PhotoBody("weak", mapOf("a" to 0.3f), Offset.Zero)
        val furthest = sweepPast(weak)
        assertTrue(furthest > 10f, "it wasn't nudged")
        assertTrue(furthest <= MagnetDimens.MostNudge.value * 1.5f, "it was carried $furthest dp")
        assertTrue((weak.at - weak.home).getDistance() < 1f, "it didn't slide home: ${weak.at}")
    }

    @Test
    fun aMagnetSetDownRightBesideAPhotoItDoesNotPullPushesItJustClearOfItself() {
        val other = PhotoBody("other", emptyMap(), Offset.Zero)
        val m = magnet("a", Offset(500f, 500f))
        val table = bodies(other to Offset(500f + 20f, 500f))
        table.run(1f, listOf(m))
        val apart = (other.at - m.at).getDistance()
        assertTrue(
            apart >= MagnetDimens.MagnetBody.value - 0.5f,
            "it lies under the magnet: $apart",
        )
        assertTrue(apart < MagnetDimens.ContactRing.value, "it was shoved out to the ring: $apart")
    }

    @Test
    fun aClusterLeftLyingOverAPhotosSpotLetsItLieStillUnderIt() {
        // Its spot right where the magnet's cluster comes to rest.
        val match = PhotoBody("match", mapOf("a" to 1f), Offset.Zero)
        val other = PhotoBody("other", emptyMap(), Offset.Zero)
        val m = magnet("a", Offset(300f, 500f))
        val table = bodies(match to Offset(300f, 560f), other to Offset(520f, 500f))
        table.run(1f, listOf(m))
        repeat(110) {
            m.at = Offset(300f + it * 2f, 500f)
            table.step(step, listOf(m))
        }
        // The magnet stays put over it; watch it for five seconds.
        var furthest = 0f
        var rested = false
        repeat(600) {
            table.step(step, listOf(m))
            if (it > 240) furthest = maxOf(furthest, other.velocity.getDistance())
            if (table.atRest) rested = true
        }
        assertTrue(furthest < MagnetDimens.StillSpeed, "it keeps kicking at up to $furthest dp/s")
        assertTrue(rested, "the table never comes to rest")
    }

    @Test
    fun cardsAreKeptOnTheTable() {
        val photo = PhotoBody("p", emptyMap(), Offset.Zero)
        val table = bodies(photo to Offset(500f, 500f))
        photo.at = Offset(-200f, 2000f)
        table.keepOnTable()
        assertEquals(Offset(card / 2f, 1000f - MagnetDimens.CardHeight.value / 2f), photo.at)
    }

    @Test
    fun theTopmostCardIsTheOneTouched() {
        val under = PhotoBody("under", emptyMap(), Offset.Zero)
        val over = PhotoBody("over", emptyMap(), Offset.Zero)
        val table = bodies(under to Offset(500f, 500f), over to Offset(510f, 500f))
        assertEquals("over", table.at(Offset(505f, 500f))?.id)
        over.held = true
        under.stuckTo += "a"
        assertEquals("over", table.at(Offset(505f, 500f))?.id)
        assertNull(table.at(Offset(100f, 100f)))
    }

    @Test
    fun theScatterFillsTheTableWithoutOverlap() {
        val spots = PhotoBodies.scatter(12, Random(29))
        assertEquals(12, spots.size)
        // On the smallest table the lab shows, 300 × 500 dp, no two homes overlap.
        val span = MagnetDimens.CardSpan.value
        for (i in spots.indices) for (j in i + 1 until spots.size) {
            val dx = (spots[i].x - spots[j].x) * 300f
            val dy = (spots[i].y - spots[j].y) * 500f
            assertTrue(kotlin.math.hypot(dx, dy) >= span, "$i and $j overlap")
        }
        assertTrue(spots.all { it.x in 0f..1f && it.y in 0f..1f })
    }
}
