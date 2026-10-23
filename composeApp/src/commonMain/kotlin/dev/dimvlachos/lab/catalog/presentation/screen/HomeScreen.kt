package dev.dimvlachos.lab.catalog.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.catalog.Catalog
import dev.dimvlachos.lab.catalog.CatalogEntry
import dev.dimvlachos.lab.catalog.filterCatalog
import dev.dimvlachos.lab.catalog.presentation.CatalogFilterState
import dev.dimvlachos.lab.catalog.presentation.components.CatalogGrid
import dev.dimvlachos.lab.catalog.presentation.components.CatalogSearchBar
import dev.dimvlachos.lab.catalog.presentation.components.TagFilterRow
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.presentation.components.topbar.LabTopBar
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.ThemeMode
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.catalog_no_results
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The home screen: the lab's name in the bar over a row of tags to filter by, and a card for every
 * demo and folder under it, filtered by [filter]. The bar's round search button ([onOpenSearch])
 * grows into a pill holding the search field while [searchOpen], focused when [focusSearch] says so
 * ([onSearchFocused] once it is); its back arrow ([onCloseSearch]) shrinks it back.
 */
@Composable
internal fun HomeScreen(
    entries: List<CatalogEntry>,
    filter: CatalogFilterState,
    searchOpen: Boolean,
    focusSearch: Boolean,
    onSearchFocused: () -> Unit,
    themeMode: ThemeMode,
    onCycleTheme: () -> Unit,
    soundOn: Boolean,
    onToggleSound: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onOpenDemo: (Demo) -> Unit,
    onOpenGroup: (CatalogEntry.Group) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    // The keyboard goes away before another screen slides in over this one.
    val leaving = {
        focusManager.clearFocus()
        keyboard?.hide()
    }
    val words = rememberCatalogWords(entries)
    // Only the tags some demo has, in the tags' own order.
    val tags = remember(entries) { DemoTag.entries.filter { it in catalogTags(entries) } }
    val query = filter.query
    val shown = remember(entries, query, words) { filterCatalog(entries, query, words) }
    Column(Modifier.fillMaxSize().background(LabTheme.colors.background)) {
        LabTopBar(
            bottom = {
                TagFilterRow(
                    tags = tags,
                    selected = filter.tags,
                    onToggle = filter::toggle,
                    contentPadding =
                        PaddingValues(
                            start = LabTheme.spacing.medium,
                            end = LabTheme.spacing.medium,
                            bottom = LabTheme.spacing.small,
                        ),
                )
            },
            titlePadding = 0.dp,
        ) {
            CatalogSearchBar(
                searchOpen = searchOpen,
                text = filter.text,
                onTextChange = { filter.text = it },
                focusSearch = focusSearch,
                onSearchFocused = onSearchFocused,
                themeMode = themeMode,
                onCycleTheme = onCycleTheme,
                soundOn = soundOn,
                onToggleSound = onToggleSound,
                onOpenSearch = onOpenSearch,
                onCloseSearch = {
                    leaving()
                    onCloseSearch()
                },
            )
        }
        CatalogGrid(
            entries = shown,
            onOpenDemo = {
                leaving()
                onOpenDemo(it)
            },
            onOpenGroup = {
                leaving()
                onOpenGroup(it)
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
            emptyMessage = stringResource(Res.string.catalog_no_results),
        )
    }
}

/**
 * Every string the catalog's search looks through, read here once, so the filter can look them up
 * as it goes.
 */
@Composable
private fun rememberCatalogWords(entries: List<CatalogEntry>): (StringResource) -> String {
    val resources = remember(entries) { catalogStrings(entries) }
    val texts = resources.map { stringResource(it) }
    return remember(resources, texts) {
        val byResource = resources.zip(texts).toMap()
        val lookUp: (StringResource) -> String = { resource -> byResource[resource].orEmpty() }
        lookUp
    }
}

private fun catalogTags(entries: List<CatalogEntry>): Set<DemoTag> =
    entries
        .flatMap { entry ->
            when (entry) {
                is CatalogEntry.Single -> entry.demo.tags
                is CatalogEntry.Group -> entry.tags
            }
        }
        .toSet()

private fun catalogStrings(entries: List<CatalogEntry>): List<StringResource> =
    (entries.flatMap { entry ->
            when (entry) {
                is CatalogEntry.Single -> listOf(entry.demo.title, entry.demo.description)
                is CatalogEntry.Group ->
                    listOf(entry.title, entry.description) +
                        entry.demos.flatMap { listOf(it.title, it.description) }
            }
        } + DemoTag.entries.map { it.label })
        .distinct()

@Preview
@Composable
private fun HomeScreenPreview() {
    LabTheme {
        HomeScreen(
            entries = Catalog.entries,
            filter = remember { CatalogFilterState() },
            searchOpen = false,
            focusSearch = false,
            onSearchFocused = {},
            themeMode = ThemeMode.System,
            onCycleTheme = {},
            soundOn = false,
            onToggleSound = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onOpenDemo = {},
            onOpenGroup = {},
        )
    }
}
