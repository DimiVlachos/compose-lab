package dev.dimvlachos.lab.catalog.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.dimvlachos.lab.catalog.Catalog
import dev.dimvlachos.lab.catalog.CatalogSection
import dev.dimvlachos.lab.catalog.presentation.components.CatalogCard
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_back
import org.jetbrains.compose.resources.stringResource

/** One section's demos, in story order, under the same Back bar as a demo. */
@Composable
internal fun SectionScreen(section: CatalogSection, onOpen: (Demo) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(LabTheme.colors.background)) {
        Row(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    )
                )
                .padding(horizontal = LabTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(contentColor = LabTheme.colors.accent),
            ) {
                Text(stringResource(Res.string.action_back))
            }
            Text(
                stringResource(section.title),
                color = LabTheme.colors.textPrimary,
                style = LabTheme.typography.body,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = LabTheme.spacing.small),
            )
        }
        LazyColumn(
            modifier =
                Modifier.fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                        )
                    ),
            contentPadding = PaddingValues(LabTheme.spacing.mediumLarge),
            verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.smallMedium),
        ) {
            items(section.demos, key = { it.id }) { demo ->
                CatalogCard(
                    title = stringResource(demo.title),
                    subtitle = demo.id,
                    onClick = { onOpen(demo) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun SectionScreenPreview() {
    LabTheme { SectionScreen(section = Catalog.sections.first(), onOpen = {}, onBack = {}) }
}
