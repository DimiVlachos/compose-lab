package dev.dimvlachos.lab.core.presentation.components.mailbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MailboxStateTest {
    private val cues = mutableListOf<Pair<MailboxCue, Float>>()

    private fun mailbox(reduced: Boolean = false) =
        MailboxState(reduced).also { it.cue = { cue, volume -> cues += cue to volume } }

    private fun MailboxState.run(ms: Int) {
        repeat(ms / 16) { advance(0.016f) }
    }

    @Test
    fun mailRaisesTheFlagWithATwangAndItBouncesPastUpright() {
        val box = mailbox()
        box.setUnread(1)
        assertEquals(MailboxCue.Twang, cues.single().first)
        var highest = 0f
        repeat(40) {
            box.advance(0.016f)
            highest = maxOf(highest, box.flagAngle)
        }
        assertTrue(highest > MailboxState.UpAngle, "it should swing past upright, reached $highest")
        box.run(1500)
        assertEquals(MailboxState.UpAngle, box.flagAngle, 0.01f)
        assertEquals(1f, box.numberAlpha, 0.01f)
        assertEquals(1, box.shown)
    }

    @Test
    fun moreMailWhileTheFlagIsUpJigglesItAndTicks() {
        val box = mailbox()
        box.setUnread(1)
        box.run(1500)
        cues.clear()
        box.setUnread(2)
        assertEquals(listOf(MailboxCue.Tick), cues.map { it.first })
        box.advance(0.016f)
        assertTrue(box.flagAngle < MailboxState.UpAngle, "a jiggle drops it a little")
        assertEquals(2, box.shown)
    }

    @Test
    fun readingTheLastLowersTheFlagAMomentLaterAndItLandsOnItsStop() {
        val box = mailbox()
        box.setUnread(2)
        box.run(1500)
        cues.clear()
        box.setUnread(0)
        box.run(600)
        assertEquals(MailboxState.UpAngle, box.flagAngle, 0.02f)
        box.run(3000)
        assertEquals(0f, box.flagAngle, 0.01f)
        // The number keeps the last count while it fades, so it doesn't read 0 on the way down.
        assertEquals(2, box.shown)
        assertEquals(0f, box.numberAlpha, 0.01f)
        assertTrue(box.isSettled)
    }

    @Test
    fun aDeliveryCracksTheDoorSlipsAnEnvelopeInAndShutsBeforeTheCountGoesUp() {
        val box = mailbox()
        var landed = false
        box.deliver { landed = true }
        assertEquals(MailboxCue.Creak, cues.single().first)
        box.run(400)
        assertTrue(box.doorAngle > 0.4f, "door ${box.doorAngle}")
        assertEquals(1, box.flights.size)
        assertTrue(MailboxCue.Slip in cues.map { it.first })
        assertFalse(landed)
        box.run(600)
        assertTrue(landed)
        box.run(800)
        assertEquals(0f, box.doorAngle, 0.01f)
        assertTrue(MailboxCue.Tunk in cues.map { it.first }, "the door claps shut on tin")
        assertTrue(box.flights.isEmpty())
    }

    @Test
    fun openingDropsTheDoorLeansTheLettersOutAndThenOpensTheInbox() {
        val box = mailbox()
        var opened = false
        box.open { opened = true }
        box.run(200)
        assertFalse(opened)
        box.run(200)
        assertTrue(opened)
        box.run(1200)
        assertTrue(box.doorAngle > 1.8f)
        assertEquals(1f, box.peek, 0.01f)
        box.close()
        box.run(1500)
        assertEquals(0f, box.doorAngle, 0.01f)
        assertEquals(0f, box.peek, 0.01f)
    }

    @Test
    fun theDoorStopsAtEachEnd() {
        val box = mailbox()
        box.open {}
        repeat(200) {
            box.advance(0.008f)
            assertTrue(box.doorAngle <= MailboxState.DoorStop + 1e-4f)
            assertTrue(box.doorAngle >= 0f)
        }
    }

    @Test
    fun aDragPastHalfwayOpensAndAShortOneShutsTheDoor() {
        val box = mailbox()
        box.dragDoor(15f)
        box.run(300)
        assertFalse(box.releaseDoor())
        box.run(1000)
        assertEquals(0f, box.doorAngle, 0.01f)
        box.dragDoor(36f)
        box.run(300)
        assertTrue(box.releaseDoor())
    }

    @Test
    fun reducedMotionSnapsEverythingAndDeliversAtOnce() {
        val box = mailbox(reduced = true)
        var landed = false
        box.deliver { landed = true }
        assertTrue(landed)
        box.setUnread(1)
        box.advance(0.016f)
        assertEquals(MailboxState.UpAngle, box.flagAngle)
        assertEquals(1f, box.numberAlpha)
        box.open {}
        // What the opening sets going waits for the frame after, as in the prototype.
        box.advance(0.016f)
        box.advance(0.016f)
        assertEquals(MailboxState.OpenDoor, box.doorAngle)
        assertEquals(1f, box.peek)
    }

    @Test
    fun resetPutsEverythingBack() {
        val box = mailbox()
        box.setUnread(3)
        box.open {}
        box.run(500)
        box.reset()
        box.run(200)
        assertEquals(0f, box.flagAngle)
        assertEquals(0f, box.doorAngle)
        assertEquals(0, box.unread)
        assertFalse(box.isOpen)
        assertTrue(box.isSettled)
    }

    @Test
    fun closingBeforeTheDoorIsDownDropsTheOpenThatWasUnderWay() {
        val box = mailbox()
        var opened = 0
        box.open { opened++ }
        box.run(96)
        box.close()
        box.run(1500)
        assertEquals(0, opened, "the inbox must not slide in after it was closed")
        assertEquals(0f, box.doorAngle, 0.01f)
        assertEquals(0f, box.peek, 0.01f)
    }

    @Test
    fun openingAgainRightAfterClosingKeepsTheDoorOpen() {
        val box = mailbox()
        box.open {}
        box.run(1000)
        box.close()
        box.run(96)
        var opened = 0
        box.open { opened++ }
        box.run(1500)
        assertEquals(1, opened)
        assertTrue(box.isOpen)
        assertEquals(MailboxState.OpenDoor, box.doorAngle, 0.01f)
        assertEquals(1f, box.peek, 0.01f)
    }

    @Test
    fun mailArrivingWhileAFingerHasTheDoorLeavesItToTheFinger() {
        val box = mailbox()
        box.dragDoor(10f)
        box.run(300)
        val held = box.doorAngle
        cues.clear()
        box.deliver {}
        box.run(300)
        assertEquals(held, box.doorAngle, 0.01f)
        assertFalse(cues.any { it.first == MailboxCue.Creak })
    }

    @Test
    fun closingTucksTheLettersInBeforeTheDoorRisesOverThem() {
        val box = mailbox()
        box.open {}
        box.run(1500)
        assertEquals(1f, box.letterReach, 0.01f)
        box.close()
        // Every step of the close: once the door has risen past where it hangs below the
        // opening, no letter may still lean out, or it shows outside the shut door.
        repeat(400) {
            box.advance(1f / 240f)
            if (box.doorAngle < MailboxState.LettersOutFrom) {
                assertEquals(
                    0f,
                    box.letterReach,
                    0.001f,
                    "door ${box.doorAngle}, reach ${box.letterReach}",
                )
            }
        }
        assertEquals(0f, box.doorAngle, 0.01f)
    }

    @Test
    fun aCrackedDoorForADeliveryLetsNoLetterOut() {
        val box = mailbox()
        box.setUnread(2)
        box.run(1500)
        box.deliver {}
        repeat(300) {
            box.advance(1f / 240f)
            assertEquals(0f, box.letterReach, 0.001f)
        }
    }
}
