package dev.dimvlachos.lab.cardfiledemo

import dev.dimvlachos.lab.core.presentation.components.cardfile.FileCard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ContactDeckTest {
    private val deck = ContactDeck(DemoContacts)

    @Test
    fun thereAreThePrototypesTwentyThreeAmericanContactsInOrder() {
        assertEquals(23, DemoContacts.size)
        assertEquals(DemoContacts.map { it.name }.sorted(), DemoContacts.map { it.name })
        assertEquals(23, DemoContacts.map { it.id }.toSet().size)
        assertEquals(23, DemoContacts.map { it.number }.toSet().size)
        assertTrue(DemoContacts.all { it.number.matches(Regex("""\(\d{3}\) 555-01\d\d""")) })
        assertEquals("Abby Carter", DemoContacts.first().name)
        assertEquals(31 + 17 * 13, DemoContacts.single { it.name == "Maya Brooks" }.seed)
    }

    @Test
    fun aDividerComesBeforeEachLettersFirstContactOnly() {
        val letters = DemoContacts.map { it.name.first() }.distinct()
        assertEquals(letters, deck.letters)
        assertEquals(23 + letters.size, deck.cards.size)
        deck.cards.zipWithNext().forEach { (a, b) ->
            if (b is FileCard.Divider) assertTrue(a !is FileCard.Divider, "two dividers in a row")
        }
        assertIs<FileCard.Divider>(deck.cards.first())
    }

    @Test
    fun aTabSpinsToItsFirstContact() {
        val i = deck.firstContactIndex('B')
        val card = assertIs<FileCard.Entry>(deck.cards[i])
        assertEquals('B', deck.contact(card).name.first())
        assertIs<FileCard.Divider>(deck.cards[i - 1])
    }

    @Test
    fun mayaBrooksCanBeFound() {
        val i = deck.indexOf("maya-brooks")
        assertEquals("Maya Brooks", deck.contact(deck.cards[i] as FileCard.Entry).name)
    }
}
