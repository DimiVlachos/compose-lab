package dev.dimvlachos.moodboard.android.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.dimvlachos.moodboard.R
import dev.dimvlachos.moodboard.android.nav.Search
import dev.dimvlachos.moodboard.android.share.rememberShareLauncher
import dev.dimvlachos.moodboard.android.ui.ContentEndSpacing
import dev.dimvlachos.moodboard.android.ui.MoodboardIcon
import dev.dimvlachos.moodboard.android.ui.PhotoGrid
import dev.dimvlachos.moodboard.android.ui.plusBottom
import dev.dimvlachos.moodboard.search.SearchAction
import dev.dimvlachos.moodboard.search.SearchViewModel
import dev.dimvlachos.moodboard.ui.components.EmptyState

/**
 * Search as in edda: only the field stays at the top. An empty field offers recent searches and the
 * tags to browse; once there is something to search for, the matching photos take their place.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenPhoto: (String) -> Unit,
    bottomBar: @Composable () -> Unit,
    isTopScreen: Boolean,
    onBackHandled: () -> Unit,
) {
    val viewModel = viewModel { SearchViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val share = rememberShareLauncher()
    // The keyboard goes on a search and before a photo opens, instead of riding along over it.
    val focusManager = LocalFocusManager.current
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
    val submit: () -> Unit = {
        viewModel.onAction(SearchAction.Submitted)
        focusManager.clearFocus()
    }
    val open: (String) -> Unit = { id ->
        focusManager.clearFocus()
        viewModel.onAction(SearchAction.ResultOpened)
        onOpenPhoto(id)
    }

    // Back from results returns to the suggestions first, as Android's search does; from the
    // suggestions it leaves the tab. Only while Search is on top: under a photo that is still
    // opening, Back belongs to the photo, not to this screen's query.
    BackHandler(enabled = isTopScreen && query.isNotBlank()) {
        onBackHandled()
        setQuery("")
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = { SearchField(query, setQuery, onSearch = submit) },
        bottomBar = bottomBar,
    ) { padding ->
        if (query.isBlank()) {
            SearchSuggestions(
                recent = state.recent,
                tags = state.suggestions,
                tagCounts = state.tagCounts,
                contentPadding = padding,
                onRecent = {
                    setQuery(it)
                    submit()
                },
                onRemoveRecent = { viewModel.onAction(SearchAction.RemoveRecent(it)) },
                onTag = { tag ->
                    query = tag
                    viewModel.onAction(SearchAction.TagTapped(tag))
                    focusManager.clearFocus()
                },
            )
        } else {
            PhotoGrid(
                photos = state.results,
                onOpen = { open(it.id) },
                morphScope = Search.toString(),
                contentPadding = padding,
                // Clear of the keyboard: the last results can scroll above it.
                modifier = Modifier.consumeWindowInsets(padding).imePadding(),
                empty = {
                    EmptyState(
                        title = "No results",
                        message = "Nothing matches “${query.trim()}”. Try a place or a tag.",
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
}

/** The one thing pinned at the top: the field, opaque so content passes under it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(bottom = 8.dp)
    ) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = { onSearch() },
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text("Search photos and tags") },
                    leadingIcon = { MoodboardIcon(R.drawable.ic_search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                MoodboardIcon(R.drawable.ic_close, "Clear search")
                            }
                        }
                    },
                )
            },
            expanded = false,
            onExpandedChange = {},
            windowInsets = WindowInsets(0),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {}
    }
}

/** Recent searches, then every tag with how many photos carry it. */
@Composable
private fun SearchSuggestions(
    recent: List<String>,
    tags: List<String>,
    tagCounts: Map<String, Int>,
    contentPadding: PaddingValues,
    onRecent: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onTag: (String) -> Unit,
) {
    // Consumed, so the keyboard's padding doesn't count the bottom bar twice.
    val modifier = Modifier.fillMaxSize().consumeWindowInsets(contentPadding).imePadding()
    if (recent.isEmpty() && tags.isEmpty()) {
        Box(modifier.padding(contentPadding)) {
            EmptyState(
                title = "No photos",
                message = "Deleted photos come back the next time the app starts.",
                icon = { MoodboardIcon(R.drawable.ic_search, null) },
            )
        }
        return
    }
    LazyColumn(modifier, contentPadding = contentPadding.plusBottom(ContentEndSpacing)) {
        if (recent.isNotEmpty()) {
            item(key = "recent-header") { SectionHeader("Recent searches") }
            items(recent, key = { "recent-$it" }) { search ->
                ListItem(
                    headlineContent = { Text(search) },
                    leadingContent = { MoodboardIcon(R.drawable.ic_history, null) },
                    trailingContent = {
                        IconButton(onClick = { onRemoveRecent(search) }) {
                            MoodboardIcon(
                                R.drawable.ic_close,
                                "Remove $search from recent searches",
                            )
                        }
                    },
                    modifier = Modifier.clickable { onRecent(search) },
                )
            }
        }
        if (tags.isNotEmpty()) {
            item(key = "tags-header") { SectionHeader("Browse by tag") }
            items(tags, key = { "tag-$it" }) { tag ->
                val count = tagCounts[tag] ?: 0
                ListItem(
                    headlineContent = { Text(tag) },
                    supportingContent = { Text(if (count == 1) "1 photo" else "$count photos") },
                    leadingContent = { MoodboardIcon(R.drawable.ic_tag, null) },
                    modifier = Modifier.clickable { onTag(tag) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp).semantics {
                heading()
            },
    )
}
