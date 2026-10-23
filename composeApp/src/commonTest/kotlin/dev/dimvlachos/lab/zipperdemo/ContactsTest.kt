package dev.dimvlachos.lab.zipperdemo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContactsTest {
    @Test
    fun theBookHoldsThePrototypesEightyEightPeople() {
        assertEquals(88, Contacts.people.size)
        assertEquals("Aaron Ellis", Contacts.people[0].name)
        assertEquals("Abby Lambert", Contacts.people[1].name)
        assertEquals("Work · Harlow Dental", Contacts.people[1].sub)
        assertEquals("AE", Contacts.people[0].initials)
        assertEquals("Zoe", Contacts.people.last().first)
    }

    @Test
    fun eachLetterHasAHeaderBeforeItsFirstRow() {
        val headers = Contacts.items.filterIsInstance<ContactItem.Header>().map { it.letter }
        assertEquals(Contacts.people.map { it.first.take(1) }.distinct(), headers)
        assertTrue(Contacts.items.first() is ContactItem.Header)
        assertEquals(Contacts.KindHeader, Contacts.kinds[0])
        assertEquals(Contacts.KindFirstRow, Contacts.kinds[1])
        assertEquals(Contacts.KindRow, Contacts.kinds[2])
        assertEquals("A", Contacts.sectionLetters[0])
        assertEquals(null, Contacts.sectionLetters[1])
    }
}
