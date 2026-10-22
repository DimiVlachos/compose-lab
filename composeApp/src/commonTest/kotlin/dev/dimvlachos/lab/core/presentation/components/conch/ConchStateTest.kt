package dev.dimvlachos.lab.core.presentation.components.conch

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConchStateTest {
    // The prototype's phone: 412 wide, the button centred at (380, 845), the composer at 818.
    private fun conch(reduced: Boolean = false) =
        ConchState().apply {
            reducedMotion = reduced
            layout(412f, 380f, 845f, 818f)
            reset()
        }

    private val events = mutableListOf<ConchEvent>()

    private fun ConchState.listen() = apply { onEvent = { e, _ -> events += e } }

    private fun ConchState.run(ms: Int) {
        repeat(ms / 16) { step(.016f) }
    }

    private fun ConchState.mine() = notes.count { it.mine }

    @Test
    fun startsIdleWithOneReceivedShell() {
        val c = conch()
        assertEquals(ConchMode.Idle, c.mode)
        assertEquals(1, c.notes.size)
        assertFalse(c.notes[0].mine)
        assertEquals(4, c.notes[0].shownSeconds)
        assertTrue(c.settled)
    }

    @Test
    fun mayasNoteIsTheSameEveryReset() {
        val c = conch()
        val first = c.notes[0].levels.copyOf()
        c.run(500)
        c.reset()
        assertContentEquals(first, c.notes[0].levels)
    }

    @Test
    fun aQuickTapOnlyShowsTheHint() {
        val c = conch().listen()
        c.startRec()
        c.run(120)
        c.finishRec()
        c.run(1000)
        assertEquals(0, c.mine())
        assertEquals(0, c.sent)
        assertTrue(c.hint)
        assertEquals(listOf(ConchEvent.Start, ConchEvent.Hint), events)
        c.run(1000)
        assertFalse(c.hint, "the hint goes after 1.8 s")
        assertEquals(ConchMode.Idle, c.mode)
    }

    @Test
    fun holdingRecordsLiftsAndSpreadsRings() {
        val c = conch()
        c.startRec()
        c.run(340)
        assertEquals(ConchMode.Rec, c.mode)
        assertTrue(c.lift > .3f, "lift ${c.lift}")
        c.run(1200)
        assertTrue(c.rings.isNotEmpty())
        assertTrue(c.levelS >= 0f)
        assertEquals(2, c.recSeconds, "1.54 s rounds to 0:02, as the prototype shows it")
    }

    @Test
    fun lettingGoSendsAndTheRingsGoWithTheFlight() {
        val c = conch().listen()
        c.startRec()
        c.run(1540)
        c.finishRec()
        assertEquals(ConchMode.Fly, c.mode)
        assertTrue(c.rings.isEmpty())
        val note = c.notes.last()
        assertTrue(note.mine && note.hidden)
        c.run(800)
        assertEquals(ConchMode.Idle, c.mode)
        assertFalse(note.hidden)
        assertEquals(1, c.mine())
        assertTrue(note.shownSeconds in 1..2, "${note.shownSeconds}")
        assertTrue(note.levels.size > 5 && note.levels.max() > .2f)
        assertEquals(
            listOf(ConchEvent.Start, ConchEvent.Sent, ConchEvent.Landed),
            events,
        )
    }

    @Test
    fun theFlightEndsAtTheBubblesShell() {
        val c = conch()
        c.startRec()
        c.run(1000)
        c.finishRec()
        c.flyTarget(60f, 700f)
        c.run(272)
        assertTrue(c.flyP in .3f..1f)
        c.step(.3f)
        assertEquals(ConchMode.Idle, c.mode)
    }

    @Test
    fun oneLongSlideLeftArmsAndReleasingCancels() {
        val c = conch().listen()
        c.startRec()
        c.run(700)
        c.dragTo(-160f)
        assertTrue(c.armed)
        c.run(300)
        assertTrue(c.cool > .9f, "cool ${c.cool}")
        c.finishRec()
        assertEquals(ConchMode.Back, c.mode)
        assertEquals(1, c.cancels)
        assertEquals(14, c.puffs.size)
        c.run(1500)
        assertEquals(ConchMode.Idle, c.mode)
        assertEquals(0, c.mine())
        assertTrue(c.puffs.isEmpty())
        assertEquals(
            listOf(ConchEvent.Start, ConchEvent.Arm, ConchEvent.Canceled),
            events,
        )
    }

    @Test
    fun slidingBackDisarmsAndSends() {
        val c = conch().listen()
        c.startRec()
        c.run(700)
        c.dragTo(-8f)
        c.dragTo(-150f)
        assertTrue(c.armed)
        c.dragTo(-20f)
        assertFalse(c.armed)
        c.finishRec()
        c.run(800)
        assertEquals(1, c.mine())
        assertTrue(ConchEvent.Disarm in events)
    }

    @Test
    fun theSlideNeverTakesTheShellPastTheLeftEdge() {
        val c = conch()
        c.startRec()
        c.dragTo(-2000f)
        c.run(1000)
        assertTrue(c.off >= -(412f * .5f - 190f * .42f) - .5f, "off ${c.off}")
    }

    @Test
    fun tappingAShellPlaysItAndAgainPauses() {
        val c = conch()
        val maya = c.notes[0]
        c.togglePlay(maya)
        c.run(900)
        assertEquals(maya, c.playing)
        assertTrue(maya.pos > .1f)
        assertTrue(c.humLevel > 0f)
        c.togglePlay(maya)
        assertNull(c.playing)
        assertTrue(maya.pos > .1f)
        c.togglePlay(maya)
        c.run(4000)
        assertNull(c.playing, "it stops at the end")
        assertEquals(0f, maya.pos)
        assertEquals(4, maya.shownSeconds)
    }

    @Test
    fun recordingStopsPlayback() {
        val c = conch()
        c.togglePlay(c.notes[0])
        c.run(200)
        c.startRec()
        assertNull(c.playing)
    }

    @Test
    fun reducedMotionSnapsWithoutRings() {
        val c = conch(reduced = true)
        c.startRec()
        c.step(.016f)
        assertEquals(1f, c.lift)
        c.run(1500)
        assertTrue(c.rings.isEmpty())
        c.finishRec()
        c.step(.016f)
        assertEquals(ConchMode.Idle, c.mode)
        assertEquals(1f, c.regrow)
        assertEquals(1, c.mine())
    }

    @Test
    fun removeSentLeavesMaya() {
        val c = conch()
        c.startRec()
        c.run(1000)
        c.finishRec()
        c.run(800)
        c.removeSent()
        assertEquals(1, c.notes.size)
        assertFalse(c.notes[0].mine)
        c.run(1500)
        assertTrue(c.settled)
    }

    @Test
    fun clockTextIsMinutesAndSeconds() {
        assertEquals("0:04", clockText(seconds(4400f)))
        assertEquals("1:05", clockText(65))
    }
}
