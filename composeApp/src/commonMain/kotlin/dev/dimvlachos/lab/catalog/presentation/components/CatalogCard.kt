package dev.dimvlachos.lab.catalog.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.ic_chevron_right
import dev.dimvlachos.lab.resources.ic_demo_fog
import dev.dimvlachos.lab.resources.ic_demo_plane
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val IconTile = 40.dp
private val IconSize = 22.dp

/**
 * A catalog card: the demo's [icon] on a tile, its [title], a one-line [description] and a small
 * [caption] (its id, how many versions a folder holds, or the folder it is in), the whole card one
 * button. A [folder]'s card ends in a chevron, since it opens onto more cards.
 */
@Composable
internal fun CatalogCard(
    title: String,
    description: String,
    caption: String,
    icon: DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    folder: Boolean = false,
) {
    val colors = LabTheme.colors
    val spacing = LabTheme.spacing
    Row(
        modifier
            .fillMaxWidth()
            .clip(LabTheme.shapes.large)
            .background(colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(IconTile).clip(LabTheme.shapes.medium).background(colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(IconSize),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = colors.textPrimary,
                style = LabTheme.typography.subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                description,
                color = colors.textMuted,
                style = LabTheme.typography.body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                caption,
                color = colors.textMuted,
                style = LabTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = spacing.minimum),
            )
        }
        if (folder) {
            Icon(
                painterResource(Res.drawable.ic_chevron_right),
                contentDescription = null,
                tint = colors.textMuted,
            )
        }
    }
}

@Composable
private fun CatalogCardSamples() {
    Column(
        Modifier.background(LabTheme.colors.background).padding(LabTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.smallMedium),
    ) {
        CatalogCard(
            title = "Paper plane",
            description = "A chat that sends each message as a paper plane",
            caption = "chat.plane",
            icon = Res.drawable.ic_demo_plane,
            onClick = {},
        )
        CatalogCard(
            title = "Fogged mirror",
            description = "Wipe the steam off a bathroom mirror with your finger",
            caption = "2 versions",
            icon = Res.drawable.ic_demo_fog,
            onClick = {},
            folder = true,
        )
    }
}

@Preview
@Composable
private fun CatalogCardLightPreview() {
    LabTheme(darkTheme = false) { CatalogCardSamples() }
}

@Preview
@Composable
private fun CatalogCardDarkPreview() {
    LabTheme(darkTheme = true) { CatalogCardSamples() }
}
