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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.dimvlachos.lab.catalog.Catalog
import dev.dimvlachos.lab.catalog.CatalogSection
import dev.dimvlachos.lab.catalog.presentation.components.CatalogCard
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.catalog_demo_count
import dev.dimvlachos.lab.resources.catalog_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The home screen: one card per section, each opening that section's demos. */
@Composable
internal fun CatalogScreen(onOpen: (CatalogSection) -> Unit) {
    LazyColumn(
        modifier =
            Modifier.fillMaxSize()
                .background(LabTheme.colors.background)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(LabTheme.spacing.mediumLarge),
        verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.smallMedium),
    ) {
        item {
            Text(
                stringResource(Res.string.catalog_title),
                color = LabTheme.colors.textPrimary,
                style = LabTheme.typography.title,
            )
        }
        items(Catalog.sections, key = { it.title.key }) { section ->
            CatalogCard(
                title = stringResource(section.title),
                subtitle =
                    pluralStringResource(
                        Res.plurals.catalog_demo_count,
                        section.demos.size,
                        section.demos.size,
                    ),
                onClick = { onOpen(section) },
            )
        }
    }
}

@Preview
@Composable
private fun CatalogScreenPreview() {
    LabTheme { CatalogScreen(onOpen = {}) }
}
