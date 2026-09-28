package dev.dimvlachos.lab.core.presentation.components.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphEnd
import dev.dimvlachos.lab.core.presentation.components.imagemorph.ReportMorphComposition
import dev.dimvlachos.lab.core.presentation.components.imagemorph.ShapedMorphNode
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphChrome
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphWidthFraction

internal const val AvatarKey = "profile_avatar"
internal const val AvatarSourceTag = "avatarSource"
internal const val AvatarTargetTag = "avatarTarget"
internal val AvatarSize = 88.dp
private const val AvatarOpenWidth = 0.72f

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun AvatarSource(
    painter: Painter,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ReportMorphComposition(MorphEnd.Card)
    ShapedMorphNode(
        sharedTransitionScope,
        animatedVisibilityScope,
        AvatarKey,
        CircleShape,
        modifier
            .size(AvatarSize)
            .testTag(AvatarSourceTag)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onOpen,
            ),
    ) {
        Image(painter, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

// Instagram's: a circle at both ends, grown into the middle of the screen over a scrim.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun AvatarTarget(
    painter: Painter,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClose: () -> Unit,
    overlay: @Composable BoxScope.(chrome: Modifier) -> Unit = {},
) {
    ReportMorphComposition(MorphEnd.Detail)
    val fraction = remember { mutableFloatStateOf(1f) }
    Box(Modifier.fillMaxSize()) {
        ProfileScrim(animatedVisibilityScope, onClose)
        ShapedMorphNode(
            sharedTransitionScope,
            animatedVisibilityScope,
            AvatarKey,
            CircleShape,
            Modifier.align(Alignment.Center)
                .fillMaxWidth(AvatarOpenWidth)
                .aspectRatio(1f)
                .testTag(AvatarTargetTag),
            overlayZIndex = 1f,
        ) {
            // The circle's own width, so what the overlay shows lands with it.
            Image(
                painter,
                null,
                Modifier.fillMaxSize().morphWidthFraction(fraction),
                contentScale = ContentScale.Crop,
            )
        }
        overlay(Modifier.morphChrome(animatedVisibilityScope, fraction))
    }
}
