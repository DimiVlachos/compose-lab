package dev.dimvlachos.lab.cardfiledemo

/** A person in the card file, with the [note] written on their card; [seed] varies the hand. */
internal data class Contact(
    val id: String,
    val first: String,
    val last: String,
    val number: String,
    val note: String,
    val seed: Int,
) {
    val name: String
        get() = "$first $last"
}
