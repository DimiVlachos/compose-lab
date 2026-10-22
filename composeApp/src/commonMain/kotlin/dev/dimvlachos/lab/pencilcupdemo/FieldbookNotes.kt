package dev.dimvlachos.lab.pencilcupdemo

/** A note in the list: its [title] and the start of its text. */
internal class FieldNote(val id: Int, val title: String, val text: String)

/** The notes the list starts with, as the prototype writes them: an invented American family's. */
internal val StartingNotes =
    listOf(
        FieldNote(
            1,
            "Saturday soccer carpool",
            "Pick up Ethan and Ava at 8:15. Field 3 at Riverside Park.",
        ),
        FieldNote(
            2,
            "Kitchen remodel quotes",
            "Hollis & Sons came in at \$18,400. Still waiting on Bluebird Builders.",
        ),
        FieldNote(
            3,
            "Gift ideas for Mom",
            "Pottery class at Clay Street Studio, the blue scarf from the farmers market.",
        ),
        FieldNote(
            4,
            "Book club, chapter 5",
            "Bring the lemon bars. Discussion at Priya’s, 7 PM Thursday.",
        ),
        FieldNote(5, "Camping list", "Headlamps, bug spray, extra tent stakes, the good cooler."),
        FieldNote(6, "Dentist", "Thursday 3:40 PM with Dr. Morales. Ask about the night guard."),
    )
