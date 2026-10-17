package dev.dimvlachos.lab.core.presentation.components.cardfile

/** A card on the wheel: an A–Z divider with a raised tab, or an entry the caller draws. */
internal sealed interface FileCard {
    val key: String

    data class Divider(val letter: Char) : FileCard {
        override val key: String
            get() = "divider-$letter"
    }

    data class Entry(override val key: String, val letter: Char) : FileCard
}
