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
import dev.dimvlachos.lab.core.presentation.components.topbar.LabIconButton
import dev.dimvlachos.lab.core.presentation.components.topbar.LabTopBar
import dev.dimvlachos.lab.core.presentation.components.topbar.LabTopBarTitle
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_back
import dev.dimvlachos.lab.resources.ic_arrow_back
import org.jetbrains.compose.resources.stringResource

/** A folder's screen: its title in the bar with a way [onBack], and a card for each version. */
@Composable
internal fun FolderScreen(
    group: CatalogEntry.Group,
    onOpenDemo: (Demo) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(LabTheme.colors.background)) {
        LabTopBar(
            navigationIcon = {
                LabIconButton(
                    Res.drawable.ic_arrow_back,
                    contentDescription = stringResource(Res.string.action_back),
                    onClick = onBack,
                )
            }
        ) {
            LabTopBarTitle(stringResource(group.title))
        }
        CatalogGrid(
            entries = group.demos.map { CatalogEntry.Single(it) },
            onOpenDemo = onOpenDemo,
            onOpenGroup = {},
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}

@Preview
@Composable
private fun FolderScreenPreview() {
    LabTheme {
        FolderScreen(
            group = Catalog.entries.filterIsInstance<CatalogEntry.Group>().first(),
            onOpenDemo = {},
            onBack = {},
        )
    }
}
