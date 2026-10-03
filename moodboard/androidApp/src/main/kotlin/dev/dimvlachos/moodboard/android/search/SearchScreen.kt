package dev.dimvlachos.moodboard.android.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.dimvlachos.moodboard.R
import dev.dimvlachos.moodboard.android.ui.MoodboardIcon
import dev.dimvlachos.moodboard.android.ui.PhotoGrid
import dev.dimvlachos.moodboard.search.SearchAction
import dev.dimvlachos.moodboard.search.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onOpenPhoto: (String) -> Unit, bottomBar: @Composable () -> Unit) {
    val viewModel = viewModel { SearchViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            Column(Modifier.fillMaxWidth().statusBarsPadding()) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = state.query,
                            onQueryChange = { viewModel.onAction(SearchAction.QueryChanged(it)) },
                            onSearch = {},
                            expanded = false,
                            onExpandedChange = {},
                            placeholder = { Text("Search photos and tags") },
                            leadingIcon = { MoodboardIcon(R.drawable.ic_search, null) },
                        )
                    },
                    expanded = false,
                    onExpandedChange = {},
                    windowInsets = WindowInsets(0),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {}
                FlowRow(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.suggestions.forEach { tag ->
                        SuggestionChip(
                            onClick = { viewModel.onAction(SearchAction.TagTapped(tag)) },
                            label = { Text(tag) },
                        )
                    }
                }
            }
        },
        bottomBar = bottomBar,
    ) { padding ->
        PhotoGrid(
            photos = state.results,
            onOpen = { onOpenPhoto(it.id) },
            contentPadding = padding,
        ) { photo, dismiss ->
            DropdownMenuItem(
                text = { Text("Open") },
                onClick = {
                    dismiss()
                    onOpenPhoto(photo.id)
                },
            )
        }
    }
}
