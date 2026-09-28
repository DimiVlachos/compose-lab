package dev.dimvlachos.lab.core.presentation.components.profile

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphDimens
import dev.dimvlachos.lab.core.presentation.components.imagemorph.MorphEnd
import dev.dimvlachos.lab.core.presentation.components.imagemorph.RadiusMorphNode
import dev.dimvlachos.lab.core.presentation.components.imagemorph.ReportMorphComposition
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphChrome
import dev.dimvlachos.lab.core.presentation.components.imagemorph.morphWidthFraction
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_cancel
import dev.dimvlachos.lab.resources.add_image_camera
import dev.dimvlachos.lab.resources.add_image_gallery
import dev.dimvlachos.lab.resources.edit_photo_title
import dev.dimvlachos.lab.resources.ic_edit
import dev.dimvlachos.lab.resources.ic_image
import dev.dimvlachos.lab.resources.ic_photo_camera
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal const val FabKey = "profile_fab"
internal const val FabSourceTag = "fabSource"
internal const val FabDialogTag = "fabDialog"
internal val FabRadius = 16.dp
internal val DialogRadius = 28.dp
internal val FabSize = 56.dp
private const val DialogWidth = 0.8f
private const val IconFadeMs = 100
private const val DialogScrimAlpha = 0.5f
internal const val DialogScrimTag = "dialogScrim"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun FabSource(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ReportMorphComposition(MorphEnd.Card)
    RadiusMorphNode(
        sharedTransitionScope,
        animatedVisibilityScope,
        FabKey,
        restRadius = FabRadius,
        counterpartRadius = DialogRadius,
        modifier =
            modifier
                .size(FabSize)
                .testTag(FabSourceTag)
                .clickable(role = Role.Button, onClick = onOpen),
        color = LabTheme.colors.accent,
    ) {
        // The pencil goes first on an open and comes back last on a close, so it never stretches
        // with the container.
        Icon(
            painterResource(Res.drawable.ic_edit),
            contentDescription = stringResource(Res.string.edit_photo_title),
            tint = LabTheme.colors.onAccent,
            modifier =
                with(animatedVisibilityScope) {
                        Modifier.animateEnterExit(
                            enter =
                                fadeIn(
                                    tween(
                                        IconFadeMs,
                                        delayMillis = MorphDimens.CloseMs - IconFadeMs,
                                        easing = LinearEasing,
                                    )
                                ),
                            exit = fadeOut(tween(IconFadeMs, easing = LinearEasing)),
                        )
                    }
                    .align(Alignment.Center),
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun FabDialogTarget(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClose: () -> Unit,
) {
    ReportMorphComposition(MorphEnd.Detail)
    val fraction = remember { mutableFloatStateOf(1f) }
    val chrome = Modifier.morphChrome(animatedVisibilityScope, fraction)
    Box(Modifier.fillMaxSize()) {
        // Lighter than the avatar's: the zoomed avatar stays visible under the dialog.
        ProfileScrim(
            animatedVisibilityScope,
            onClose,
            maxAlpha = DialogScrimAlpha,
            tag = DialogScrimTag,
        )
        RadiusMorphNode(
            sharedTransitionScope,
            animatedVisibilityScope,
            FabKey,
            restRadius = DialogRadius,
            counterpartRadius = FabRadius,
            modifier =
                Modifier.align(Alignment.Center)
                    .fillMaxWidth(DialogWidth)
                    .wrapContentHeight()
                    .testTag(FabDialogTag),
            preMorphRadius = FabRadius,
            overlayZIndex = 1f,
            color = LabTheme.colors.surface,
        ) {
            // Inside the node, the contents ride the container and fade with its width.
            Column(
                Modifier.morphWidthFraction(fraction).padding(LabTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.small),
            ) {
                Text(
                    stringResource(Res.string.edit_photo_title),
                    color = LabTheme.colors.textPrimary,
                    style = LabTheme.typography.subtitle,
                    modifier = chrome.padding(bottom = LabTheme.spacing.small),
                )
                DialogRow(
                    painterResource(Res.drawable.ic_photo_camera),
                    stringResource(Res.string.add_image_camera),
                    chrome,
                )
                DialogRow(
                    painterResource(Res.drawable.ic_image),
                    stringResource(Res.string.add_image_gallery),
                    chrome,
                )
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        stringResource(Res.string.action_cancel),
                        color = LabTheme.colors.accent,
                        style = LabTheme.typography.body,
                        modifier =
                            chrome
                                .clip(RoundedCornerShape(LabTheme.spacing.small))
                                .clickable(role = Role.Button, onClick = onClose)
                                .padding(LabTheme.spacing.small),
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogRow(icon: Painter, label: String, chrome: Modifier) {
    Row(
        chrome
            .fillMaxWidth()
            .clip(RoundedCornerShape(LabTheme.spacing.smallMedium))
            .clickable(role = Role.Button, onClick = {})
            .padding(vertical = LabTheme.spacing.smallMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LabTheme.spacing.medium),
    ) {
        Icon(icon, null, tint = LabTheme.colors.textPrimary)
        Text(label, color = LabTheme.colors.textPrimary, style = LabTheme.typography.body)
    }
}
