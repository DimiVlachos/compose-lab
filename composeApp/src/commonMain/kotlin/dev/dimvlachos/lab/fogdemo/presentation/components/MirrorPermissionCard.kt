package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.ic_photo_camera
import dev.dimvlachos.lab.resources.mirror_card_allow
import dev.dimvlachos.lab.resources.mirror_card_body_what
import dev.dimvlachos.lab.resources.mirror_card_camera_blocked
import dev.dimvlachos.lab.resources.mirror_card_camera_why
import dev.dimvlachos.lab.resources.mirror_card_not_now
import dev.dimvlachos.lab.resources.mirror_card_open_settings
import dev.dimvlachos.lab.resources.mirror_card_promise
import dev.dimvlachos.lab.resources.mirror_card_title
import dev.dimvlachos.lab.resources.mirror_view
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val CardIconSize = 32.dp
private val CardMaxWidth = 360.dp
private val CompactCardMaxWidth = 600.dp
private val CardLightElevation = 12.dp

/** What the card still needs for the camera. */
internal enum class Need {
    Nothing,

    /** The system will still ask. */
    Ask,

    /** Refused for good: only settings can turn it on. */
    Settings,
}

/**
 * Explains, before the system asks, what to do on the mirror and why the camera it still [Need]s
 * helps: the camera is the mirror. Allow while it can be asked for; otherwise it offers settings.
 * [compact] for a stage wider than it is tall, as on a phone on its side: wider, without the icon,
 * its buttons kept in sight while only the words scroll.
 */
@Composable
internal fun MirrorPermissionCard(
    camera: Need,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val words: @Composable () -> Unit = {
        CardText(
            Res.string.mirror_card_title,
            LabTheme.colors.textPrimary,
            if (compact) LabTheme.typography.subtitle else LabTheme.typography.title,
        )
        CardText(Res.string.mirror_card_body_what, LabTheme.colors.textPrimary)
        if (camera == Need.Ask)
            CardText(Res.string.mirror_card_camera_why, LabTheme.colors.textMuted)
        CardText(Res.string.mirror_card_promise, LabTheme.colors.textMuted)
        if (camera == Need.Settings) {
            CardText(Res.string.mirror_card_camera_blocked, LabTheme.colors.textPrimary)
        }
    }
    val card =
        modifier
            .widthIn(max = if (compact) CompactCardMaxWidth else CardMaxWidth)
            .padding(horizontal = LabTheme.spacing.large)
            // On the light palette a soft shadow lifts the white card off the white tiles.
            .then(
                if (LabTheme.isDark) Modifier
                else Modifier.shadow(CardLightElevation, LabTheme.shapes.extraLarge)
            )
            .background(LabTheme.colors.surface, LabTheme.shapes.extraLarge)
    if (compact) {
        // Short of height: the buttons stay put at the foot and only the words above them scroll.
        Column(
            card.padding(LabTheme.spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.small),
        ) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.small),
            ) {
                words()
            }
            CardButtons(camera, onAllow, onOpenSettings, onNotNow)
        }
    } else {
        Column(
            card
                // Large text can make it taller than the stage: it scrolls, so the buttons are
                // always within reach.
                .verticalScroll(rememberScrollState())
                .padding(LabTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.smallMedium),
        ) {
            CardIcon()
            words()
            CardButtons(camera, onAllow, onOpenSettings, onNotNow)
        }
    }
}

// Not now, and Allow or, once refused for good, Open settings.
@Composable
private fun CardButtons(
    camera: Need,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onNotNow: () -> Unit,
) {
    val canAsk = camera == Need.Ask
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LabTheme.spacing.small, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onNotNow,
            colors = ButtonDefaults.textButtonColors(contentColor = LabTheme.colors.textMuted),
        ) {
            Text(stringResource(Res.string.mirror_card_not_now))
        }
        Button(
            onClick = if (canAsk) onAllow else onOpenSettings,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = LabTheme.colors.accent,
                    contentColor = LabTheme.colors.onAccent,
                ),
        ) {
            Text(
                stringResource(
                    if (canAsk) Res.string.mirror_card_allow
                    else Res.string.mirror_card_open_settings
                )
            )
        }
    }
}

@Composable
private fun CardIcon() {
    Icon(
        painterResource(Res.drawable.ic_photo_camera),
        contentDescription = null,
        tint = LabTheme.colors.accent,
        modifier = Modifier.size(CardIconSize),
    )
}

@Composable
private fun CardText(
    text: StringResource,
    color: Color,
    style: TextStyle = LabTheme.typography.body,
) {
    Text(
        stringResource(text),
        color = color,
        style = style,
        textAlign = TextAlign.Center,
    )
}

// The card as it first asks, on the bathroom's tiles.
@Composable
private fun MirrorPermissionCardPreview(dark: Boolean) {
    LabTheme(darkTheme = dark) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painterResource(Res.drawable.mirror_view),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
            MirrorPermissionCard(
                camera = Need.Ask,
                onAllow = {},
                onOpenSettings = {},
                onNotNow = {},
                modifier = Modifier.padding(vertical = LabTheme.spacing.large),
            )
        }
    }
}

@Preview
@Composable
private fun MirrorPermissionCardDarkPreview() = MirrorPermissionCardPreview(dark = true)

@Preview
@Composable
private fun MirrorPermissionCardLightPreview() = MirrorPermissionCardPreview(dark = false)
