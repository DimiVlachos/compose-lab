package dev.dimvlachos.lab.catalog.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.catalog.CatalogEntry
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import org.jetbrains.compose.resources.stringResource

// Wide enough for a card's title and description on one line each: one column on a phone held
// upright, two or more on its side or on a tablet.
private val MinCardWidth = 300.dp

/**
 * The catalog's cards in a grid, each opening a demo or a folder of versions, with [emptyMessage]
 * across it when there are no [entries]. It scrolls under the system bars at its foot and sides,
 * and keeps its last card clear of them.
 */
@Composable
internal fun CatalogGrid(
    entries: List<CatalogEntry>,
    onOpenDemo: (Demo) -> Unit,
    onOpenGroup: (CatalogEntry.Group) -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String? = null,
) {
    val spacing = LabTheme.spacing
    LazyVerticalGrid(
        columns = GridCells.Adaptive(MinCardWidth),
        modifier = modifier,
        contentPadding =
            WindowInsets.safeDrawing
                .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                .add(
                    WindowInsets(
                        spacing.mediumLarge,
                        spacing.mediumLarge,
                        spacing.mediumLarge,
                        spacing.mediumLarge,
                    )
                )
                .asPaddingValues(),
        verticalArrangement = Arrangement.spacedBy(spacing.smallMedium),
        horizontalArrangement = Arrangement.spacedBy(spacing.smallMedium),
    ) {
        items(entries, key = { it.key }) { entry ->
            val card = Modifier.animateItem()
            when (entry) {
                is CatalogEntry.Single ->
                    CatalogCard(
                        title = stringResource(entry.demo.title),
                        subtitle = entry.demo.id,
                        onClick = { onOpenDemo(entry.demo) },
                        modifier = card,
                    )
                is CatalogEntry.Group ->
                    CatalogCard(
                        title = stringResource(entry.title),
                        subtitle = entry.id,
                        onClick = { onOpenGroup(entry) },
                        modifier = card,
                    )
            }
        }
        if (entries.isEmpty() && emptyMessage != null) {
            item(key = EmptyKey, span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    emptyMessage,
                    color = LabTheme.colors.textMuted,
                    style = LabTheme.typography.body,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(vertical = spacing.extraLarge)
                            .animateItem(),
                )
            }
        }
    }
}

private const val EmptyKey = "catalog.empty"

private val CatalogEntry.key: String
    get() =
        when (this) {
            is CatalogEntry.Single -> demo.id
            is CatalogEntry.Group -> id
        }
