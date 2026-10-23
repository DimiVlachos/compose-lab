package dev.dimvlachos.lab.keyhookdemo

import dev.dimvlachos.lab.core.presentation.components.keyhook.KeyHolder
import dev.dimvlachos.lab.core.presentation.components.keyhook.KeyItem
import dev.dimvlachos.lab.core.presentation.components.keyhook.KeyMetal

/** A show someone is part way through: its [progress] (0 to 1) and its thumbnail's two colours. */
internal class Show(
    val title: String,
    val episode: String,
    val progress: Float,
    val from: Long,
    val to: Long,
)

/** The prototype's household, the Millers, and what each of them is watching. */
internal object Household {
    val people =
        listOf(
            KeyHolder(
                name = "Jordan Miller",
                first = "Jordan",
                plate = "JORDAN",
                initials = "JM",
                color = "#2f5288",
                items =
                    listOf(
                        KeyItem.Key(KeyMetal.Brass),
                        KeyItem.Fob,
                        KeyItem.Tag("#2b4874", "JM"),
                    ),
            ),
            KeyHolder(
                name = "Ava Miller",
                first = "Ava",
                plate = "AVA",
                initials = "AM",
                color = "#c4523a",
                items =
                    listOf(
                        KeyItem.Key(KeyMetal.Nickel),
                        KeyItem.Tag("#c7553c", "AM"),
                        KeyItem.Key(KeyMetal.Brass, .72f),
                    ),
            ),
            KeyHolder(
                name = "Sam Miller",
                first = "Sam",
                plate = "SAM",
                initials = "SM",
                color = "#3d7a55",
                items =
                    listOf(
                        KeyItem.Key(KeyMetal.Brass),
                        KeyItem.Key(KeyMetal.Nickel, .72f),
                        KeyItem.Tag("#c3962b", "SM"),
                    ),
            ),
            KeyHolder(
                name = "Grandpa Lou",
                first = "Lou",
                plate = "LOU",
                initials = "LM",
                color = "#8a5a2b",
                items = listOf(KeyItem.Key(KeyMetal.Brass), KeyItem.Bobber),
            ),
        )

    /** What each person, by [people]'s order, has on the go. */
    val shows =
        listOf(
            listOf(
                Show("Harbor Lights", "S2 · E4", .62f, 0xFF1F4E6E, 0xFFE08D4F),
                Show("Big Sky Kitchen", "E7", .3f, 0xFF7A3B23, 0xFFE7B04A),
            ),
            listOf(
                Show("Midnight at Maple High", "S1 · E9", .8f, 0xFF3A2A6B, 0xFFD65F8F),
                Show("Comet Club", "E3", .15f, 0xFF163D4A, 0xFF5CC3B5),
            ),
            listOf(
                Show("Pixel Pioneers", "E12", .45f, 0xFF1D3557, 0xFF4CC9F0),
                Show("Rivals of Route 9", "S3 · E1", .1f, 0xFF5B1F1F, 0xFFF2A541),
            ),
            listOf(
                Show("Bass Lake Mornings", "E5", .7f, 0xFF2C5D3F, 0xFFD9C27A),
                Show("Old Glory Garage", "E2", .38f, 0xFF5A2E1A, 0xFFC9C1B2),
            ),
        )

    fun showsOf(holder: KeyHolder): List<Show> = shows[people.indexOf(holder)]
}
