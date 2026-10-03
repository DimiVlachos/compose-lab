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
    private val favorites = setOf("santorini", "book_spread_1")

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
            (1..8).map { n ->
                val id = "book_spread_$n"
                Photo(
                    id = id,
                    title = "Little Nemo · Spread $n",
                    path = "files/photos/$id.jpg",
                    tags = setOf("comic", "little nemo"),
                    isFavorite = id in favorites,
                )
            }

    val boards: List<Board> =
        listOf(
            Board("islands", "Islands", islands),
            Board("little-nemo", "Little Nemo", (1..8).map { "book_spread_$it" }),
            Board("blue", "Blue", listOf("santorini", "mykonos", "milos")),
        )
}
