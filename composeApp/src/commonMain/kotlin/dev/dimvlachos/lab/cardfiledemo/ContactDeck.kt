package dev.dimvlachos.lab.cardfiledemo

import dev.dimvlachos.lab.core.presentation.components.cardfile.FileCard

/**
 * The contacts as cards on the wheel: a divider before the first of each letter, then its people,
 * filed by the first letter of the name on the card.
 */
internal class ContactDeck(contacts: List<Contact>) {
    private val byId = contacts.associateBy { it.id }

    val cards: List<FileCard> = buildList {
        var letter: Char? = null
        for (contact in contacts) {
            val initial = contact.name.first().uppercaseChar()
            if (initial != letter) {
                add(FileCard.Divider(initial))
                letter = initial
            }
            add(FileCard.Entry(contact.id, initial))
        }
    }

    val letters: List<Char> = cards.filterIsInstance<FileCard.Divider>().map { it.letter }

    fun contact(card: FileCard.Entry): Contact = byId.getValue(card.key)

    fun firstContactIndex(letter: Char): Int = cards.indexOfFirst {
        it is FileCard.Entry && it.letter == letter
    }

    fun indexOf(contactId: String): Int = cards.indexOfFirst { it.key == contactId }
}
