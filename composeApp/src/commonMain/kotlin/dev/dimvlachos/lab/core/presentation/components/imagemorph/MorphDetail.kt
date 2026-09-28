package dev.dimvlachos.lab.core.presentation.components.imagemorph

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_close
import dev.dimvlachos.lab.resources.ic_close
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val ScrimAlpha = 0.7f
private const val CloseChipAlpha = 0.7f
private val CloseChipSize = 40.dp
private val CloseIconSize = 20.dp

// The detail end. The backdrop and the shared photo are siblings, so no alpha ever sits on an
// ancestor of the shared node: an alpha there would fade the morphing photo with it. The chrome
// is a function of the container's own geometry, read only in layer blocks.
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun MorphDetail(
    painter: Painter,
    title: String,
    caption: String,
    key: String,
    layers: MorphLayers,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClose: () -> Unit,
) {
    ReportMorphComposition(MorphEnd.Detail)
    val staged = layers.stagedChrome
    val chromeAlpha: (Float) -> Float = { if (staged) morphChromeAlpha(it) else 1f }
    MorphProgress(animatedVisibilityScope) { progress, closing ->
        val backdropAlpha: () -> Float = {
            if (staged) morphBackdropAlpha(progress.value, closing()) else 1f
        }
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize()
                    // A flat colour needs no offscreen buffer to fade.
                    .graphicsLayer {
                        alpha = backdropAlpha()
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    }
                    .background(LabTheme.colors.background)
                    // Swallows taps: a host that keeps its grid composed under the detail (the
                    // profile gallery) would otherwise open the card behind the photo.
                    .clickable(interactionSource = null, indication = null, onClick = {})
            )
            MorphNode(
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                key = key,
                end = MorphEnd.Detail,
                layers = layers,
                modifier = Modifier.fillMaxSize(),
                preMorphRadius = morphCounterpartRadius(MorphEnd.Detail, layers.pairedCorners),
                overlayZIndex = if (staged) 1f else 0f,
            ) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // Inside the node, the chrome rides the container the way the card's label does,
                // and RemeasureToBounds lays it out at the animated size every frame. Laid out
                // outside, at its final place, it would fade in over the backdrop with the photo
                // still growing under it. The naive shape (scaleToBounds) would scale it instead,
                // so there it stays out.
                if (layers.remeasure) {
                    DetailChrome(
                        sharedTransitionScope,
                        animatedVisibilityScope,
                        staged,
                        title,
                        caption,
                        onClose,
                        chromeAlpha,
                    )
                }
            }
            if (!layers.remeasure) {
                DetailChrome(
                    sharedTransitionScope,
                    animatedVisibilityScope,
                    staged,
                    title,
                    caption,
                    onClose,
                    chromeAlpha,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DetailChrome(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    staged: Boolean,
    title: String,
    caption: String,
    onClose: () -> Unit,
    alpha: (widthFraction: Float) -> Float,
) {
    // The text keeps its final measure while the container is still smaller, instead of
    // re-wrapping on every frame of the morph.
    val textModifier = with(sharedTransitionScope) { Modifier.skipToLookaheadSize() }
    // Inside the node this Box is the node's size, so the fraction is the container's own
    // geometry; outside it (the naive shape) it is the stage, and the chrome is simply there. The
    // alpha goes on the two small pieces, not on this full-size Box: a translucent layer the size
    // of the stage is an offscreen buffer on every frame, and that is where frames get dropped.
    val fraction = remember { mutableFloatStateOf(1f) }
    // A timed fade on top of the geometric one, on the morph's own curve: in over twice the open,
    // out quickly at the start of a close. The two multiply, so the chrome is fully there only
    // once both are done.
    val fade =
        if (staged) {
            with(animatedVisibilityScope) {
                Modifier.animateEnterExit(
                    enter =
                        fadeIn(tween(MorphDimens.ChromeFadeInMs, easing = MorphDimens.MorphEasing)),
                    exit = fadeOut(tween(MorphDimens.ChromeFadeOutMs, easing = LinearEasing)),
                )
            }
        } else {
            Modifier
        }
    Box(Modifier.fillMaxSize().morphWidthFraction(fraction)) {
        Box(
            Modifier.align(Alignment.TopEnd)
                .padding(LabTheme.spacing.mediumLarge)
                .then(fade)
                .graphicsLayer {
                    this.alpha = alpha(fraction.floatValue)
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                .size(CloseChipSize)
                // Clipped first, so the ripple stays inside the circle.
                .clip(CircleShape)
                .background(LabTheme.colors.bar.copy(alpha = CloseChipAlpha))
                .clickable(role = Role.Button, onClick = onClose)
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_close),
                contentDescription = stringResource(Res.string.action_close),
                tint = LabTheme.colors.textPrimary,
                modifier = Modifier.align(Alignment.Center).size(CloseIconSize),
            )
        }
        Column(
            Modifier.align(Alignment.BottomStart)
                .fillMaxWidth()
                .then(fade)
                .graphicsLayer {
                    this.alpha = alpha(fraction.floatValue)
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = ScrimAlpha))
                    )
                )
                // Inside the gradient, so the scrim still reaches the edge while the text clears
                // the navigation bar.
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(
                    start = LabTheme.spacing.mediumLarge,
                    top = LabTheme.spacing.mediumLarge,
                    end = LabTheme.spacing.mediumLarge,
                    bottom = LabTheme.spacing.medium,
                )
        ) {
            Text(
                title,
                color = LabTheme.colors.textPrimary,
                style = LabTheme.typography.title,
                modifier = textModifier,
            )
            Text(
                caption,
                color = LabTheme.colors.textMuted,
                style = LabTheme.typography.body,
                modifier = textModifier,
            )
        }
    }
}
