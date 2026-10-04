package dev.dimvlachos.moodboard.data

import dev.dimvlachos.moodboard.domain.Board
import dev.dimvlachos.moodboard.domain.Photo

internal object SeedData {
    private val islands =
        listOf(
            "santorini",
            "mykonos",
            "milos",
            "naxos",
            "paxos",
            "hydra",
            "kefalonia",
            "corfu",
            "crete",
            "rhodes",
            "folegandros",
            "zakynthos",
        )
    // Mainland places, each with the kind of place it is as a second tag.
    private val mainland =
        listOf(
            Place("meteora", "Meteora", "mountains"),
            Place("meteora_night", "Meteora at Night", "mountains"),
            Place("delphi", "Delphi", "ruins"),
            Place("acropolis", "Acropolis", "ruins"),
            Place("nafplio", "Nafplio", "town"),
            Place("monemvasia", "Monemvasia", "town"),
            Place("vikos", "Vikos Gorge", "mountains"),
            Place("kalogeriko", "Kalogeriko Bridge", "mountains"),
        )
    private val favorites = setOf("santorini", "meteora")

    val photos: List<Photo> =
        islands.map { id ->
            Photo(
                id = id,
                title = id.replaceFirstChar { it.uppercase() },
                path = "files/photos/photo_$id.jpg",
                tags = setOf("island", "greece", "sea"),
                isFavorite = id in favorites,
            )
        } +
            mainland.map { place ->
                Photo(
                    id = place.id,
                    title = place.title,
                    path = "files/photos/photo_${place.id}.jpg",
                    tags = setOf("mainland", "greece", place.kind),
                    isFavorite = place.id in favorites,
                )
            }

    val boards: List<Board> =
        listOf(
            Board("islands", "Islands", islands),
            Board("mainland", "Mainland", mainland.map { it.id }),
            Board("blue", "Blue", listOf("santorini", "mykonos", "milos")),
        )
}

private class Place(val id: String, val title: String, val kind: String)
