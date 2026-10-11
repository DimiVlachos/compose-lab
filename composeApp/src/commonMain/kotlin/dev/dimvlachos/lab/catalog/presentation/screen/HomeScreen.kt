package dev.dimvlachos.lab.catalog.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import dev.dimvlachos.lab.catalog.Catalog
import dev.dimvlachos.lab.catalog.CatalogEntry
import dev.dimvlachos.lab.catalog.filterCatalog
import dev.dimvlachos.lab.catalog.presentation.CatalogFilterState
import dev.dimvlachos.lab.catalog.presentation.components.CatalogGrid
import dev.dimvlachos.lab.catalog.presentation.components.CatalogSearchField
import dev.dimvlachos.lab.catalog.presentation.components.TagFilterRow
import dev.dimvlachos.lab.catalog.presentation.components.ThemeModeButton
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.presentation.components.topbar.LabIconButton
import dev.dimvlachos.lab.core.presentation.components.topbar.LabTopBar
import dev.dimvlachos.lab.core.presentation.components.topbar.LabTopBarTitle
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.ThemeMode
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_back
import dev.dimvlachos.lab.resources.action_search
import dev.dimvlachos.lab.resources.catalog_no_results
import dev.dimvlachos.lab.resources.catalog_search_clear
import dev.dimvlachos.lab.resources.catalog_title
import dev.dimvlachos.lab.resources.ic_arrow_back
import dev.dimvlachos.lab.resources.ic_close
import dev.dimvlachos.lab.resources.ic_search
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The home screen: the lab's name in the bar over a row of tags to filter by, and a card for every
 * demo and folder under it, filtered by [filter]. The bar's search button ([onOpenSearch]) turns
 * the title into a search field while [searchOpen]; its back arrow ([onCloseSearch]) turns it back.
 */
@Composable
internal fun HomeScreen(
    entries: List<CatalogEntry>,
    filter: CatalogFilterState,
    searchOpen: Boolean,
    themeMode: ThemeMode,
    onCycleTheme: () -> Unit,
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
    val shown = filterCatalog(entries, filter.query, words)
    Column(Modifier.fillMaxSize().background(LabTheme.colors.background)) {
        LabTopBar(
            navigationIcon =
                if (searchOpen) {
                    {
                        LabIconButton(
                            Res.drawable.ic_arrow_back,
                            contentDescription = stringResource(Res.string.action_back),
                            onClick = {
                                leaving()
                                onCloseSearch()
                            },
                        )
                    }
                } else null,
            actions = {
                if (!searchOpen) {
                    LabIconButton(
                        Res.drawable.ic_search,
                        contentDescription = stringResource(Res.string.action_search),
                        onClick = onOpenSearch,
                    )
                    ThemeModeButton(mode = themeMode, onCycle = onCycleTheme)
                } else if (filter.text.isNotEmpty()) {
                    LabIconButton(
                        Res.drawable.ic_close,
                        contentDescription = stringResource(Res.string.catalog_search_clear),
                        onClick = { filter.text = "" },
                    )
                }
            },
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
        ) {
            AnimatedContent(
                targetState = searchOpen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                contentAlignment = Alignment.CenterStart,
            ) { open ->
                if (open) {
                    CatalogSearchField(value = filter.text, onValueChange = { filter.text = it })
                } else {
                    LabTopBarTitle(stringResource(Res.string.catalog_title))
                }
            }
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
            themeMode = ThemeMode.System,
            onCycleTheme = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onOpenDemo = {},
            onOpenGroup = {},
        )
    }
}
