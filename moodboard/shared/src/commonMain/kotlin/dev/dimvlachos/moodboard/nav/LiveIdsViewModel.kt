package dev.dimvlachos.moodboard.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.dimvlachos.moodboard.MoodboardGraph
import dev.dimvlachos.moodboard.domain.MoodboardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LiveIds(val photoIds: Set<String>, val boardIds: Set<String>)

/** Which photos and boards exist, so navigation can drop screens for deleted ones. */
class LiveIdsViewModel(repository: MoodboardRepository = MoodboardGraph.repository) : ViewModel() {
    val state: StateFlow<LiveIds> =
        combine(repository.photos, repository.boards) { photos, boards ->
                LiveIds(photos.map { it.id }.toSet(), boards.map { it.id }.toSet())
            }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                LiveIds(
                    repository.photos.value.map { it.id }.toSet(),
                    repository.boards.value.map { it.id }.toSet(),
                ),
            )
}
