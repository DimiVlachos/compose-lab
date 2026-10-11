package dev.dimvlachos.lab.catalog.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import org.jetbrains.compose.resources.stringResource

/**
 * A row of chips, one for each of [tags], that scrolls sideways when they don't all fit; the
 * [selected] ones are lit in the accent, and a tap on one [onToggle]s it.
 */
@Composable
internal fun TagFilterRow(
    tags: List<DemoTag>,
    selected: Set<DemoTag>,
    onToggle: (DemoTag) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val colors = LabTheme.colors
    val chipColors =
        FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = colors.textMuted,
            selectedContainerColor = colors.accentSoft,
            selectedLabelColor = colors.textPrimary,
        )
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(LabTheme.spacing.small),
    ) {
        for (tag in tags) {
            val isSelected = tag in selected
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(tag) },
                label = { Text(stringResource(tag.label), style = LabTheme.typography.label) },
                colors = chipColors,
                border =
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = colors.chipBorder,
                        selectedBorderColor = colors.accent,
                    ),
            )
        }
    }
}

@Composable
private fun TagFilterRowSample() {
    TagFilterRow(
        tags = DemoTag.entries,
        selected = setOf(DemoTag.Physics),
        onToggle = {},
        contentPadding = PaddingValues(LabTheme.spacing.medium),
    )
}

@Preview
@Composable
private fun TagFilterRowLightPreview() {
    LabTheme(darkTheme = false) { TagFilterRowSample() }
}

@Preview
@Composable
private fun TagFilterRowDarkPreview() {
    LabTheme(darkTheme = true) { TagFilterRowSample() }
}
