package dev.dimvlachos.moodboard.android.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.dimvlachos.moodboard.R
import dev.dimvlachos.moodboard.android.share.rememberShareLauncher
import dev.dimvlachos.moodboard.android.ui.MoodboardIcon
import dev.dimvlachos.moodboard.android.ui.PhotoGrid
import dev.dimvlachos.moodboard.search.SearchAction
import dev.dimvlachos.moodboard.search.SearchViewModel
import dev.dimvlachos.moodboard.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(onOpenPhoto: (String) -> Unit, bottomBar: @Composable () -> Unit) {
    val viewModel = viewModel { SearchViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val share = rememberShareLauncher()
    // The keyboard goes on a search and before a photo opens, instead of riding along over it.
    val focusManager = LocalFocusManager.current
    val open: (String) -> Unit = { id ->
        focusManager.clearFocus()
        onOpenPhoto(id)
    }
    // Owned here, like the iOS search field; the ViewModel hears each change.
    var query by rememberSaveable { mutableStateOf(state.query) }
    val setQuery: (String) -> Unit = {
        query = it
        viewModel.onAction(SearchAction.QueryChanged(it))
    }
    // After process death the field restores its text but the ViewModel starts empty: resync.
    LaunchedEffect(Unit) {
        if (query != state.query) viewModel.onAction(SearchAction.QueryChanged(query))
    }
    Scaffold(
        topBar = {
            // Opaque: scrolled results pass under the bar and chips, not through them.
            Column(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = query,
                            onQueryChange = setQuery,
                            onSearch = { focusManager.clearFocus() },
                            expanded = false,
                            onExpandedChange = {},
                            placeholder = { Text("Search photos and tags") },
                            leadingIcon = { MoodboardIcon(R.drawable.ic_search, null) },
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { setQuery("") }) {
                                        MoodboardIcon(R.drawable.ic_close, "Clear search")
                                    }
                                }
                            },
                        )
                    },
                    expanded = false,
                    onExpandedChange = {},
                    windowInsets = WindowInsets(0),
                    // Full width on the chips' 16dp margins, rather than centred at its own size.
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {}
                FlowRow(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.suggestions.forEach { tag ->
                        SuggestionChip(
                            onClick = {
                                setQuery(tag)
                                focusManager.clearFocus()
                            },
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
            onOpen = { open(it.id) },
            morphScope = "Search",
            contentPadding = padding,
            // Clear of the keyboard: the last results can scroll above it.
            modifier = Modifier.consumeWindowInsets(padding).imePadding(),
            empty = {
                // A blank query lists every photo, so empty then means none are left.
                EmptyState(
                    title = if (query.isBlank()) "No photos" else "No results",
                    message =
                        if (query.isBlank())
                            "Deleted photos come back the next time the app starts."
                        else "Nothing matches “${query.trim()}”. Try a place or a tag.",
                    icon = { MoodboardIcon(R.drawable.ic_search, null) },
                )
            },
        ) { photo, dismiss ->
            // The same single action as the iOS search results' context menu.
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = { MoodboardIcon(R.drawable.ic_share, null) },
                onClick = {
                    dismiss()
                    share.launch(context, photo)
                },
            )
        }
    }
}
