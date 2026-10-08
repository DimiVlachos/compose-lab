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
        MagnetPhoto("strong", Res.drawable.photo_corfu, "Strong", mapOf("a" to 1f, "b" to 0.9f)),
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
    fun twoMagnetsDroppedOnEachOtherAreKeptApart() {
        val table = table()
        table.apply("a")
        val a = table.magnetPosition("a")!!
        table.place("b", table.magnetPosition("b")!!)
        table.drag("b", a)
        table.release("b", Offset.Zero)
        val b = table.magnetPosition("b")!!
        assertTrue((a - b).getDistance() >= MagnetDimens.MagnetGap.value * D - 0.5f, "${a - b}")
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
    fun theScreenReadersApplyPutsASecondMagnetCloseEnoughToShare() {
        val table = table()
        table.apply("a")
        table.apply("b")
        val a = table.magnetPosition("a")!!
        val b = table.magnetPosition("b")!!
        assertTrue((a - b).getDistance() <= MagnetDimens.BridgeSpan.value * D)
        table.run(3f)
        assertEquals(setOf("strong"), table.results["a"])
        assertEquals(setOf("strong"), table.results["b"])
    }
}
