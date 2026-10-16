package dev.dimvlachos.lab.cardfiledemo

import dev.dimvlachos.lab.core.presentation.components.cardfile.FileCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ContactDeckTest {
    private val deck = ContactDeck(DemoContacts)

    @Test
    fun thereAreThirtyAmericanContactsInOrder() {
        assertEquals(30, DemoContacts.size)
        assertEquals(DemoContacts.map { it.last }.sorted(), DemoContacts.map { it.last })
        assertEquals(30, DemoContacts.map { it.id }.toSet().size)
        assertEquals(30, DemoContacts.map { it.name }.toSet().size)
        assertEquals(30, DemoContacts.map { it.number }.toSet().size)
        assertTrue(DemoContacts.all { it.number.matches(Regex("""\(\d{3}\) 555-01\d\d""")) })
        assertTrue(DemoContacts.any { it.name == "Maya Brooks" })
    }

    @Test
    fun aDividerComesBeforeEachLettersFirstContactOnly() {
        val letters = DemoContacts.map { it.last.first() }.distinct()
        assertEquals(letters, deck.letters)
        assertEquals(30 + letters.size, deck.cards.size)
        deck.cards.zipWithNext().forEach { (a, b) ->
            if (b is FileCard.Divider) assertTrue(a !is FileCard.Divider, "two dividers in a row")
        }
        assertIs<FileCard.Divider>(deck.cards.first())
    }

    @Test
    fun aTabSpinsToItsFirstContact() {
        val i = deck.firstContactIndex('B')
        val card = assertIs<FileCard.Entry>(deck.cards[i])
        assertEquals('B', deck.contact(card).last.first())
        assertIs<FileCard.Divider>(deck.cards[i - 1])
    }

    @Test
    fun mayaBrooksCanBeFound() {
        val i = deck.indexOf("brooks-maya")
        assertEquals("Maya Brooks", deck.contact(deck.cards[i] as FileCard.Entry).name)
    }
}
