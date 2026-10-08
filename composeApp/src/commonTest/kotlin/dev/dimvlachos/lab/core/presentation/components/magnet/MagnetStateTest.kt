package dev.dimvlachos.lab.core.presentation.components.magnet

import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.photo_corfu
import dev.dimvlachos.lab.resources.photo_milos
import dev.dimvlachos.lab.resources.photo_paxos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

// A 360 × 720 dp table at density 2: three photos in a row across its middle, and three tags.
private const val D = 2f

internal val TestPhotos =
    listOf(
        MagnetPhoto(
            "strong",
            Res.drawable.photo_corfu,
            "Strong",
            mapOf("a" to 1f, "b" to 0.9f),
            caption = "A strong match",
        ),
        MagnetPhoto("weak", Res.drawable.photo_paxos, "Weak", mapOf("a" to 0.3f, "c" to 0.4f)),
        MagnetPhoto("none", Res.drawable.photo_milos, "None", emptyMap()),
    )

internal val TestTags =
    listOf(MagnetTag("a", "Alpha"), MagnetTag("b", "Beta"), MagnetTag("c", "Gamma"))

class MagnetStateTest {
    private fun table() =
        MagnetState(TestPhotos, TestTags).apply { layOut((360 * D).toInt(), (720 * D).toInt(), D) }

    private fun MagnetState.run(seconds: Float) {
        repeat((seconds * 60).toInt()) { advance(1f / 60f) }
    }

    private fun MagnetState.body(id: String) = bodies.bodies.first { it.id == id }

    @Test
    fun resultsChangeOnlyWhenAPhotoSticksOrComesOff() {
        val table = table()
        assertTrue(table.apply("a"))
        assertEquals(mapOf("a" to emptySet<String>()), table.results)
        table.run(2f)
        assertEquals(setOf("strong"), table.results["a"])
        val stuck = table.results
        table.run(2f)
        // Picked up and carried about with its cluster, nothing sticks or comes off.
        val at = table.magnetPosition("a")!!
        assertTrue(table.place("a", at))
        table.drag("a", at + Offset(30f * D, 20f * D))
        table.run(0.5f)
        table.release("a", Offset.Zero)
        table.run(0.5f)
        assertSame(stuck, table.results)
    }

    @Test
    fun pullingAPhotoOffRemovesItFromTheResultsForGood() {
        val table = table()
        table.apply("a")
        table.run(2f)
        val magnet = table.magnetPosition("a")!!
        table.pullOff("strong", magnet + Offset(0f, 90f * D))
        assertEquals(emptySet(), table.results["a"])
        table.run(2f)
        assertEquals(emptySet(), table.results["a"], "it must not snap back")
        // Put away and brought out again, the magnet may have it back.
        table.remove("a")
        table.apply("a")
        table.run(2f)
        assertEquals(setOf("strong"), table.results["a"])
    }

    @Test
    fun aFlickIntoTheStripReleasesEverythingOnTheMagnet() {
        val table = table()
        table.apply("a")
        table.run(2f)
        val at = table.magnetPosition("a")!!
        assertTrue(table.place("a", at))
        table.release("a", Offset(0f, 6_000f))
        assertTrue(table.results.isEmpty())
        assertTrue(table.bodies.bodies.all { it.stuckTo.isEmpty() })
        table.run(3f)
        for (body in table.bodies.bodies) {
            assertTrue((body.at - body.home).getDistance() < 1f, "${body.id} is at ${body.at}")
        }
        assertEquals(table.slotPosition("a"), table.magnetPosition("a"))
        assertFalse(table.isOut("a"))
    }

    @Test
    fun aMagnetLetGoGentlyOnTheTableStaysThere() {
        val table = table()
        val slot = table.magnetPosition("b")!!
        assertTrue(table.place("b", slot))
        table.drag("b", Offset(200f * D, 200f * D))
        table.release("b", Offset.Zero)
        assertTrue(table.isOut("b"))
        assertEquals(Offset(200f * D, 200f * D), table.magnetPosition("b"))
    }

    @Test
    fun aThirdMagnetStaysInTheStrip() {
        val table = table()
        assertTrue(table.apply("a"))
        assertTrue(table.apply("b"))
        val slot = table.magnetPosition("c")!!
        assertFalse(table.place("c", slot))
        assertFalse(table.apply("c"))
        assertEquals(setOf("a", "b"), table.results.keys)
        assertEquals(slot, table.magnetPosition("c"))
    }

    @Test
    fun aMagnetLetGoRightOnAnotherSnapsBesideIt() {
        val table = table()
        table.apply("a")
        val a = table.magnetPosition("a")!!
        table.place("b", table.magnetPosition("b")!!)
        table.drag("b", a)
        table.release("b", Offset.Zero)
        assertTrue(table.joined)
        assertEquals(
            MagnetDimens.Dock.value * D,
            (table.magnetPosition("b")!! - a).getDistance(),
            0.5f,
        )
    }

    @Test
    fun aMagnetDraggedOffTheTableIsKeptToItsEdge() {
        val table = table()
        table.place("a", table.magnetPosition("a")!!)
        val r = MagnetDimens.MagnetRadius.value * D
        table.drag("a", Offset(-500f, -500f))
        assertEquals(Offset(r, r), table.magnetPosition("a"))
        table.drag("a", Offset(99_999f, 99_999f))
        assertEquals(Offset(360f * D - r, 720f * D - r), table.magnetPosition("a"))
    }

    @Test
    fun aLongStallMovesNothingFurtherThanAShortFrame() {
        // A two-second stall is stepped as the longest frame, and no further.
        val stalled = table()
        val short = table()
        stalled.apply("a")
        short.apply("a")
        stalled.advance(2f)
        short.advance(MagnetDimens.MostFrameSeconds)
        stalled.bodies.bodies.forEachIndexed { i, body ->
            assertTrue(body.at.x.isFinite() && body.at.y.isFinite())
            assertEquals(short.bodies.bodies[i].at, body.at, body.id)
        }
    }

    @Test
    fun resizingKeepsTheCardsOnTheTableAndOnTheirMagnets() {
        val table = table()
        table.apply("a")
        table.run(2f)
        val stuck = table.results
        // Turned to landscape.
        table.layOut((720 * D).toInt(), (400 * D).toInt(), D)
        table.run(1f)
        assertEquals(stuck, table.results)
        val tableHeight = 400f - MagnetDimens.StripHeight.value
        for (body in table.bodies.bodies) {
            assertTrue(
                body.at.x in 0f..720f && body.at.y in 0f..tableHeight,
                "${body.id} ${body.at}",
            )
        }
        val magnet = table.magnetPosition("a")!! / D
        assertTrue(magnet.y < tableHeight, "the magnet stays on the table: $magnet")
    }

    @Test
    fun turnedWithTwoMagnetsOutThePhotosTheyShareStayShared() {
        val table = table()
        table.apply("a")
        table.apply("b")
        table.run(3f)
        val shared = table.results
        assertEquals(setOf("strong"), shared["b"])
        // A much wider table: scaled along with it, the two would be pulled apart past sharing.
        table.layOut((860 * D).toInt(), (400 * D).toInt(), D)
        table.run(1f)
        assertEquals(shared, table.results)
    }

    @Test
    fun withEveryMagnetPutAwayTheFilingsCalmOverAboutASecond() {
        val table = table()
        table.apply("a")
        table.run(1f)
        assertEquals(0f, table.calm)
        table.remove("a")
        table.run(0.5f)
        assertTrue(table.calm in 0.2f..0.8f, "calm ${table.calm}")
        assertTrue(table.awake, "the filings are still settling")
        val version = table.fieldVersion
        table.run(0.1f)
        assertTrue(table.fieldVersion > version, "settling filings are drawn again")
        table.run(1.5f)
        assertEquals(1f, table.calm)
        table.run(2f)
        assertFalse(table.awake)
    }

    @Test
    fun whileAMagnetIsOutWhatItDoesNotPullDims() {
        val table = table()
        assertEquals(0f, table.dim)
        table.apply("a")
        table.run(0.5f)
        assertEquals(1f, table.dim)
        table.remove("a")
        table.run(0.5f)
        assertEquals(0f, table.dim)
    }

    @Test
    fun aMagnetPutAwayWhileFannedOutFoldsItsPhotosBackRatherThanDroppingThem() {
        val table = table()
        table.apply("a")
        table.run(2f)
        table.fanOut("a")
        table.run(0.5f)
        val inGrid = table.photoCentre(0)
        table.remove("a")
        table.run(0.1f)
        // Still folding: the photo it held is on its way from the grid, not gone from it.
        assertEquals("a", table.fanShown)
        assertTrue(table.fan in 0.01f..0.99f, "fan ${table.fan}")
        val folding = table.photoCentre(0)
        val home = table.bodies.bodies[0].at
        assertTrue((folding - home).getDistance() > 1f && (folding - inGrid).getDistance() > 1f)
        table.run(1f)
        assertNull(table.fanShown)
        assertEquals(table.bodies.bodies[0].at, table.photoCentre(0))
    }

    @Test
    fun anotherMagnetTappedWhileAGridFoldsFansOutOnceItHasFolded() {
        val table = table()
        table.apply("a")
        table.apply("b")
        table.run(3f)
        table.fanOut("a")
        table.run(0.5f)
        table.fanOut(null)
        table.run(0.1f)
        table.fanOut("b")
        assertEquals("a", table.fanShown, "the folding grid keeps its photos")
        table.run(1f)
        assertEquals("b", table.fannedOut)
        assertEquals("b", table.fanShown)
        assertEquals(1f, table.fan)
    }

    @Test
    fun aPhotoInTheGridOpensAndClosesBackIntoIt() {
        val table = table()
        table.apply("a")
        table.run(2f)
        // Not in a grid yet: nothing to open.
        table.open("strong")
        assertNull(table.opened)
        table.fanOut("a")
        table.run(0.5f)
        table.open("none")
        assertNull(table.opened, "a photo the grid doesn't show doesn't open")
        table.open("strong")
        assertEquals("strong", table.opened)
        table.run(0.6f)
        assertEquals(1f, table.openness)
        table.close()
        assertNull(table.opened)
        table.run(0.6f)
        assertEquals(0f, table.openness)
        assertEquals(-1, table.openIndex)
        assertEquals("a", table.fannedOut, "closing goes back to the grid")
    }

    @Test
    fun foldingTheGridOrPuttingItsMagnetAwayClosesAnOpenPhoto() {
        val table = table()
        table.apply("a")
        table.run(2f)
        table.fanOut("a")
        table.run(0.5f)
        table.open("strong")
        table.fanOut(null)
        assertNull(table.opened)
        table.run(1f)
        table.fanOut("a")
        table.run(0.5f)
        table.open("strong")
        table.remove("a")
        assertNull(table.opened)
        // Long enough for the photo to close, the cards to slide home, the filings to settle and
        // the magnet to stop swinging on its nail.
        table.run(6f)
        assertEquals(-1, table.openIndex)
        assertFalse(table.awake)
    }

    @Test
    fun pullingOffACardAFingerHoldsLeavesItInTheFinger() {
        val table = table()
        val card = table.photoPosition("none")!!
        assertTrue(table.grabPhoto(card) != null)
        table.pullOff("none", card + Offset(100f, 100f))
        assertTrue(table.bodies.bodies.first { it.id == "none" }.held, "the finger still has it")
        // Moved inwards, clear of the table's edge.
        val to = card + Offset(if (card.x > 360f) -40f else 40f, 0f)
        table.dragPhoto(to)
        assertEquals(to, table.photoPosition("none"))
        table.dropPhoto()
    }

    @Test
    fun squeezedToNoTableAndBackTheMagnetsKeepTheirPlaces() {
        val table = table()
        table.apply("a")
        table.run(1f)
        val at = table.magnetPosition("a")!!
        // A sliver no taller than the strip: no table at all for a moment.
        table.layOut((360 * D).toInt(), (80 * D).toInt(), D)
        table.layOut((360 * D).toInt(), (720 * D).toInt(), D)
        assertEquals(at, table.magnetPosition("a"))
    }

    @Test
    fun aTallerStripLeavesLessTable() {
        val table = MagnetState(TestPhotos, TestTags)
        table.layOut((360 * D).toInt(), (720 * D).toInt(), D, stripHeight = 140f)
        assertEquals(720f - 140f, table.tableHeight)
        assertEquals(720f - 70f, table.slotPosition("a")!!.y / D)
    }

    @Test
    fun aMagnetFlickedHomeHooksOntoItsNailSwingsAndComesToRest() {
        val table = table()
        table.apply("a")
        table.run(1f)
        val at = table.magnetPosition("a")!!
        table.place("a", at)
        table.release("a", Offset(3_000f, 6_000f))
        var widest = 0f
        repeat(240) {
            table.advance(1f / 60f)
            widest = maxOf(widest, kotlin.math.abs(table.hangAngle("a")))
        }
        assertTrue(widest > 0.05f, "it hooked on without a swing: $widest")
        assertTrue(widest <= MagnetDimens.MostHang, "it swung up to $widest")
        table.run(6f)
        assertEquals(0f, table.hangAngle("a"), 0.002f)
        assertFalse(table.awake)
    }

    @Test
    fun aBreathOfAirSetsTheHangingMagnetsSwayingAndTheySettle() {
        val table = table()
        table.run(2f)
        assertFalse(table.awake)
        table.stir()
        assertTrue(table.awake)
        // A quarter of a swing on: each is at its widest.
        table.run(0.15f)
        assertTrue(listOf("a", "b", "c").all { kotlin.math.abs(table.hangAngle(it)) > 0.01f })
        table.run(8f)
        assertTrue(listOf("a", "b", "c").all { kotlin.math.abs(table.hangAngle(it)) < 0.002f })
        assertFalse(table.awake)
    }

    @Test
    fun aMagnetOutOfTheStripHangsFromNothing() {
        val table = table()
        table.stir()
        table.run(0.2f)
        table.apply("a")
        assertEquals(0f, table.hangAngle("a"))
    }

    @Test
    fun photosThatStickWhileTheGridIsOutJoinIt() {
        // Three photos Alpha pulls hard, one near the middle and two out at the sides.
        val photos = List(3) { MagnetPhoto("p$it", TestPhotos[0].image, "P$it", mapOf("a" to 1f)) }
        val table =
            MagnetState(photos, TestTags).apply { layOut((360 * D).toInt(), (720 * D).toInt(), D) }
        table.apply("a")
        while (table.results["a"].orEmpty().isEmpty()) table.advance(1f / 60f)
        table.fanOut("a")
        table.run(2f)
        val stuck = table.results["a"].orEmpty()
        assertTrue(stuck.size > 1, "only $stuck stuck")
        for (i in photos.indices) {
            if (photos[i].id in stuck)
                assertTrue(table.inFan(i), "${photos[i].id} is stuck but not in the grid")
        }
    }

    @Test
    fun anOpenPhotoClosesWhenItsGridGoes() {
        val table = table()
        table.apply("a")
        table.apply("b")
        table.run(3f)
        table.fanOut("a")
        table.run(0.5f)
        table.open("strong")
        // Pulled off its magnet: nothing left in the grid, which folds, and the photo with it.
        table.pullOff("strong", table.photoPosition("strong")!! + Offset(0f, 200f * D))
        assertNull(table.fannedOut)
        assertNull(table.opened)
    }

    @Test
    fun anotherMagnetFannedOutClosesAnOpenPhoto() {
        val table = table()
        table.apply("a")
        table.apply("b")
        table.run(3f)
        table.fanOut("a")
        table.run(0.5f)
        table.open("strong")
        table.fanOut("b")
        assertNull(table.opened)
    }

    @Test
    fun puttingTheLastMagnetAwayLiftsTheShadeWithoutAFlash() {
        val table = table()
        table.apply("c")
        table.run(1f)
        val strong = table.bodies.bodies.indexOfFirst { it.id == "strong" }
        // Gamma doesn't pull it: shaded.
        assertEquals(1f, table.shadeOf(strong))
        val weak = table.bodies.bodies.indexOfFirst { it.id == "weak" }
        assertEquals(MagnetDimens.LeanShade, table.shadeOf(weak), 0.01f, "Gamma pulls it a little")
        table.remove("c")
        table.advance(1f / 60f)
        // The weak one, no longer pulled, mustn't jump to fully shaded as the shade lifts.
        assertTrue(
            table.shadeOf(weak) < MagnetDimens.LeanShade,
            "it flashed to ${table.shadeOf(weak)}",
        )
        table.run(1f)
        assertEquals(0f, table.shadeOf(strong))
        assertEquals(0f, table.shadeOf(weak))
    }

    @Test
    fun aWeakMatchIsShadedUnlessTheMagnetIsCloseEnoughToPullIt() {
        // One weak match in the middle of a long table.
        val photos = listOf(MagnetPhoto("w", TestPhotos[0].image, "W", mapOf("a" to 0.3f)))
        val table =
            MagnetState(photos, TestTags).apply { layOut((360 * D).toInt(), (1600 * D).toInt(), D) }
        val middle = table.photoPosition("w")!!
        // Out at the far end of the table, well beyond its reach: it can't pull it, so it shades.
        table.place("a", table.magnetPosition("a")!!)
        table.drag("a", Offset(middle.x, 40f * D))
        table.release("a", Offset.Zero)
        table.run(1f)
        assertEquals(1f, table.shadeOf(0), "out of reach")
        // Brought close: it stays where it lies, but is only lightly shaded, being partly what was
        // asked for.
        table.place("a", table.magnetPosition("a")!!)
        table.drag("a", middle - Offset(0f, 150f * D))
        table.release("a", Offset.Zero)
        table.run(1f)
        assertEquals(MagnetDimens.LeanShade, table.shadeOf(0), 0.01f)
    }

    @Test
    fun aThirdMagnetRefusedJigglesOnItsNail() {
        val table = table()
        var refused = 0
        table.onRefuse = { refused++ }
        table.apply("a")
        table.apply("b")
        table.run(3f)
        assertFalse(table.place("c", table.magnetPosition("c")!!))
        assertEquals(1, refused)
        assertTrue(table.awake)
        var widest = 0f
        repeat(60) {
            table.advance(1f / 60f)
            widest = maxOf(widest, kotlin.math.abs(table.hangAngle("c")))
        }
        assertTrue(widest > 0.05f, "it hardly moved: $widest")
        assertFalse(table.isOut("c"))
    }

    @Test
    fun takingAwayTheFannedOutMagnetFoldsItsGrid() {
        val table = table()
        table.apply("a")
        table.run(2f)
        table.fanOut("a")
        table.run(0.5f)
        assertEquals("a", table.fannedOut)
        assertEquals(1f, table.fan)
        table.remove("a")
        assertNull(table.fannedOut)
        table.run(0.5f)
        assertEquals(0f, table.fan)
        assertNull(table.fanShown)
    }

    @Test
    fun fanningOutAMagnetWithNothingOnItDoesNothing() {
        val table = table()
        table.apply("c")
        table.fanOut("c")
        assertNull(table.fannedOut)
    }

    @Test
    fun nothingHappensBeforeTheTableIsLaidOut() {
        val table = MagnetState(TestPhotos, TestTags)
        assertFalse(table.place("a", Offset.Zero))
        table.advance(0.1f)
        assertFalse(table.awake)
    }

    @Test
    fun aSettledTableLetsTheFrameLoopSleep() {
        val table = table()
        table.apply("a")
        table.run(6f)
        assertFalse(table.awake)
    }

    @Test
    fun aScreenReadersApplyOfASecondMagnetJoinsItToTheFirst() {
        val table = table()
        table.apply("a")
        table.apply("b")
        assertTrue(table.joined)
        table.run(3f)
        assertEquals(setOf("strong"), table.results["a"])
        assertEquals(setOf("strong"), table.results["b"])
    }

    @Test
    fun aMagnetLetGoNearAnotherSnapsBesideItAndTheTwoSearchTogether() {
        val table = table()
        table.apply("a")
        table.run(2f)
        val a = table.magnetPosition("a")!!
        table.place("b", table.magnetPosition("b")!!)
        table.drag("b", a + Offset(0f, (MagnetDimens.SnapRange.value - 10f) * D))
        table.release("b", Offset.Zero)
        assertTrue(table.joined)
        assertEquals(
            MagnetDimens.Dock.value * D,
            (table.magnetPosition("b")!! - a).getDistance(),
            0.5f,
        )
        table.run(2f)
        // Only what matches both: Strong (A 1, B 0.9); nothing else matches B.
        assertEquals(setOf("strong"), table.results["a"])
        assertEquals(table.results["a"], table.results["b"])
    }

    @Test
    fun aMagnetLetGoFarFromTheOtherIsASearchOfItsOwn() {
        val table = table()
        table.apply("a")
        val a = table.magnetPosition("a")!!
        table.place("b", table.magnetPosition("b")!!)
        table.drag("b", a + Offset(0f, (MagnetDimens.SnapRange.value + 40f) * D))
        table.release("b", Offset.Zero)
        assertFalse(table.joined)
    }

    @Test
    fun draggingEitherOfTwoJoinedMagnetsCarriesBoth() {
        val table = table()
        table.apply("a")
        table.apply("b")
        val a = table.magnetPosition("a")!!
        val b = table.magnetPosition("b")!!
        table.place("a", a)
        table.drag("a", a + Offset(40f * D, -30f * D))
        table.release("a", Offset.Zero)
        assertEquals(b + Offset(40f * D, -30f * D), table.magnetPosition("b"))
        assertTrue(table.joined)
    }

    @Test
    fun puttingOneOfTwoJoinedMagnetsAwayLeavesTheOtherSearchingAlone() {
        val table = table()
        table.apply("a")
        table.apply("b")
        table.run(3f)
        table.remove("b")
        assertFalse(table.joined)
        assertEquals(setOf("strong"), table.results["a"], "what it held matches it, so it stays")
        assertTrue(table.isOut("a"))
    }
}
