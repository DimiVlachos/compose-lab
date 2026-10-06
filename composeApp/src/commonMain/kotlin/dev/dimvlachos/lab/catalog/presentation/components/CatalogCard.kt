package dev.dimvlachos.lab.catalog.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import dev.dimvlachos.lab.core.presentation.ui.LabTheme

/** A catalog row: a title and a muted line under it, the whole card one button. */
@Composable
internal fun CatalogCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(LabTheme.shapes.large)
            .background(LabTheme.colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(LabTheme.spacing.medium)
    ) {
        Text(title, color = LabTheme.colors.textPrimary, style = LabTheme.typography.subtitle)
        Text(subtitle, color = LabTheme.colors.textMuted, style = LabTheme.typography.caption)
    }
}
