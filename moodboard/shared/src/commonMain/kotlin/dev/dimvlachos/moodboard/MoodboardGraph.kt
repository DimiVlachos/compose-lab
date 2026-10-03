package dev.dimvlachos.moodboard

import dev.dimvlachos.moodboard.data.InMemoryMoodboardRepository
import dev.dimvlachos.moodboard.data.PhotoBytes
import dev.dimvlachos.moodboard.domain.MoodboardRepository

/** The app's only dependencies, shared by Android, iOS and every hosted Compose screen. */
object MoodboardGraph {
    val repository: MoodboardRepository = InMemoryMoodboardRepository()
    val photoBytes = PhotoBytes()
}
