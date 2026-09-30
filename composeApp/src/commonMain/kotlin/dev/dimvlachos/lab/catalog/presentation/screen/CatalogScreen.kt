package dev.dimvlachos.lab.catalog.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.dimvlachos.lab.catalog.Catalog
import dev.dimvlachos.lab.catalog.CatalogEntry
import dev.dimvlachos.lab.catalog.presentation.components.CatalogCard
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_back
import org.jetbrains.compose.resources.stringResource

/**
 * A list of cards, each opening a demo or a folder of versions: the home screen, and a folder's own
 * screen with its title and a way [onBack].
 */
@Composable
internal fun CatalogScreen(
    title: String,
    entries: List<CatalogEntry>,
    onOpenDemo: (Demo) -> Unit,
    onOpenGroup: (CatalogEntry.Group) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    LazyColumn(
        modifier =
            Modifier.fillMaxSize()
                .background(LabTheme.colors.background)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(LabTheme.spacing.mediumLarge),
        verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.smallMedium),
    ) {
        if (onBack != null) {
            item {
                TextButton(
                    onClick = onBack,
                    colors = ButtonDefaults.textButtonColors(contentColor = LabTheme.colors.accent),
                ) {
                    Text(stringResource(Res.string.action_back))
                }
            }
        }
        item {
            Text(title, color = LabTheme.colors.textPrimary, style = LabTheme.typography.title)
        }
        items(entries, key = { it.key }) { entry ->
            when (entry) {
                is CatalogEntry.Single ->
                    CatalogCard(
                        title = stringResource(entry.demo.title),
                        subtitle = entry.demo.id,
                        onClick = { onOpenDemo(entry.demo) },
                    )
                is CatalogEntry.Group ->
                    CatalogCard(
                        title = stringResource(entry.title),
                        subtitle = entry.id,
                        onClick = { onOpenGroup(entry) },
                    )
            }
        }
    }
}

private val CatalogEntry.key: String
    get() =
        when (this) {
            is CatalogEntry.Single -> demo.id
            is CatalogEntry.Group -> id
        }

@Preview
@Composable
private fun CatalogScreenPreview() {
    LabTheme {
        CatalogScreen(
            title = "compose-lab",
            entries = Catalog.entries,
            onOpenDemo = {},
            onOpenGroup = {},
        )
    }
}
