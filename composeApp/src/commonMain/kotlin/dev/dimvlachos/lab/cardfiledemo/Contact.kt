package dev.dimvlachos.lab.cardfiledemo

/**
 * A person or place in the card file, as its card reads: the [name] typed across the top, what they
 * are to you ([kind]) in small capitals, and the [number], a [line] and a [note] written in ink.
 * [seed] varies the hand.
 */
internal data class Contact(
    val id: String,
    val name: String,
    val kind: String,
    val number: String,
    val line: String,
    val note: String,
    val seed: Int,
)
