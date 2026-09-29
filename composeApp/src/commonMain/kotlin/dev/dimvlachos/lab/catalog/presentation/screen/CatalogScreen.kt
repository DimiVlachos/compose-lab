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
import dev.dimvlachos.lab.catalog.presentation.components.CatalogCard
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.catalog_title
import org.jetbrains.compose.resources.stringResource

/** The home screen: one card per demo, each opening it. */
@Composable
internal fun CatalogScreen(demos: List<Demo>, onOpen: (Demo) -> Unit) {
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
        items(demos, key = { it.id }) { demo ->
            CatalogCard(
                title = stringResource(demo.title),
                subtitle = demo.id,
                onClick = { onOpen(demo) },
            )
        }
    }
}

@Preview
@Composable
private fun CatalogScreenPreview() {
    LabTheme { CatalogScreen(demos = Catalog.demos, onOpen = {}) }
}
