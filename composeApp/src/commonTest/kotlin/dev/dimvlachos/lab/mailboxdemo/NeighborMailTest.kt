package dev.dimvlachos.lab.mailboxdemo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NeighborMailTest {
    @Test
    fun startsWithLastWeeksMailAllRead() {
        val mail = NeighborMail()
        assertEquals(
            listOf("Maple Grove Library", "Ted Okafor", "Cedar Park Rec"),
            mail.letters.map { it.from },
        )
        assertEquals(0, mail.unread)
    }

    @Test
    fun newMailArrivesOnTopUnreadWithTheMorningClockMovingOn() {
        val mail = NeighborMail()
        val first = mail.arrive()
        val second = mail.arrive()
        assertEquals("Dana Whitfield", first.from)
        assertEquals("8:48 AM", first.time)
        assertEquals("Hollow Oak Hardware", second.from)
        assertEquals("8:58 AM", second.time)
        assertEquals(listOf(second.id, first.id), mail.letters.take(2).map { it.id })
        assertEquals(2, mail.unread)
    }

    @Test
    fun openingAMessageReadsItAndOpeningItAgainClosesIt() {
        val mail = NeighborMail()
        val letter = mail.arrive()
        mail.toggle(letter.id)
        assertTrue(mail.letters.first().read)
        assertTrue(mail.letters.first().expanded)
        assertEquals(0, mail.unread)
        mail.toggle(letter.id)
        assertFalse(mail.letters.first().expanded)
        assertTrue(mail.letters.first().read)
    }

    @Test
    fun markAllReadAndResetPutTheInboxBack() {
        val mail = NeighborMail()
        repeat(3) { mail.arrive() }
        mail.readAll()
        assertEquals(0, mail.unread)
        mail.reset()
        assertEquals(3, mail.letters.size)
        assertEquals("Dana Whitfield", mail.arrive().from)
    }
}
