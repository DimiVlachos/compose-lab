package dev.dimvlachos.lab.core.presentation.components.imagemorph

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import dev.dimvlachos.lab.core.presentation.ui.LabTheme

// The grid end. The shared node is the photo alone; the label sits under it, outside the node, so
// nothing has to vanish or reappear when the photo leaves for the detail and comes back.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun MorphPhotoCard(
    painter: Painter,
    title: String,
    key: String,
    layers: MorphLayers,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ReportMorphComposition(MorphEnd.Card)
    // No ripple: the card answers a tap by becoming the detail, and a ripple would be drawn on the
    // card left behind in the grid.
    Column(
        modifier.clickable(
            interactionSource = null,
            indication = null,
            role = Role.Button,
            onClick = onClick,
        )
    ) {
        MorphNode(
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            key = key,
            end = MorphEnd.Card,
            layers = layers,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Text(
            title,
            color = LabTheme.colors.textPrimary,
            style = LabTheme.typography.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
                Modifier.padding(
                    horizontal = LabTheme.spacing.extraSmall,
                    vertical = LabTheme.spacing.small,
                ),
        )
    }
}
