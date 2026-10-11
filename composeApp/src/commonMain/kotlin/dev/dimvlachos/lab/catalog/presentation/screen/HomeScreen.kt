package dev.dimvlachos.lab.catalog.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.dimvlachos.lab.catalog.Catalog
import dev.dimvlachos.lab.catalog.CatalogEntry
import dev.dimvlachos.lab.catalog.presentation.components.CatalogGrid
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.presentation.components.topbar.LabTopBar
import dev.dimvlachos.lab.core.presentation.components.topbar.LabTopBarTitle
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.catalog_title
import org.jetbrains.compose.resources.stringResource

/** The home screen: the lab's name in the bar, and a card for every demo and folder under it. */
@Composable
internal fun HomeScreen(
    entries: List<CatalogEntry>,
    onOpenDemo: (Demo) -> Unit,
    onOpenGroup: (CatalogEntry.Group) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(LabTheme.colors.background)) {
        LabTopBar { LabTopBarTitle(stringResource(Res.string.catalog_title)) }
        CatalogGrid(
            entries = entries,
            onOpenDemo = onOpenDemo,
            onOpenGroup = onOpenGroup,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    LabTheme { HomeScreen(entries = Catalog.entries, onOpenDemo = {}, onOpenGroup = {}) }
}
