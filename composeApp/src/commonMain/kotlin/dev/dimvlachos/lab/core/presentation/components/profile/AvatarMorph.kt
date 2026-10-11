package dev.dimvlachos.lab.core.presentation.components.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphEnd
import dev.dimvlachos.lab.core.presentation.components.imagemorph.ReportMorphComposition
import dev.dimvlachos.lab.core.presentation.components.imagemorph.ShapedMorphNode
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphChrome
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphWidthFraction
import kotlin.math.min
import kotlin.math.roundToInt

internal const val AvatarKey = "profile_avatar"
internal const val AvatarSourceTag = "avatarSource"
internal const val AvatarTargetTag = "avatarTarget"
internal val AvatarSize = 88.dp
private const val AvatarOpenSide = 0.72f

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
            Modifier.align(Alignment.Center).openAvatarSize().testTag(AvatarTargetTag),
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

// A share of the stage's shorter side: of its width on a phone held upright, as before, and of its
// height on one turned on its side, where the width's share would run off the top and bottom.
private fun Modifier.openAvatarSize(): Modifier = layout { measurable, constraints ->
    val side = (min(constraints.maxWidth, constraints.maxHeight) * AvatarOpenSide).roundToInt()
    val placeable = measurable.measure(Constraints.fixed(side, side))
    layout(side, side) { placeable.placeRelative(0, 0) }
}
