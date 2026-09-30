package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.ic_mic
import dev.dimvlachos.lab.resources.ic_photo_camera
import dev.dimvlachos.lab.resources.mirror_card_allow
import dev.dimvlachos.lab.resources.mirror_card_body_what
import dev.dimvlachos.lab.resources.mirror_card_body_what_bathroom
import dev.dimvlachos.lab.resources.mirror_card_camera_blocked
import dev.dimvlachos.lab.resources.mirror_card_camera_why
import dev.dimvlachos.lab.resources.mirror_card_mic_blocked
import dev.dimvlachos.lab.resources.mirror_card_mic_why
import dev.dimvlachos.lab.resources.mirror_card_not_now
import dev.dimvlachos.lab.resources.mirror_card_open_settings
import dev.dimvlachos.lab.resources.mirror_card_promise
import dev.dimvlachos.lab.resources.mirror_card_title
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val CardIconSize = 32.dp
private val CardMaxWidth = 360.dp

/** What the card still needs for one permission. */
internal enum class Need {
    Nothing,

    /** The system will still ask. */
    Ask,

    /** Refused for good: only settings can turn it on. */
    Settings,
}

/**
 * Explains, before the system asks, what to do on the mirror and why each permission it still
 * [Need]s helps: the camera is the mirror, and the microphone hears a breath a phone cannot feel.
 * Allow while anything can be asked; otherwise it offers settings.
 */
@Composable
internal fun MirrorPermissionCard(
    camera: Need,
    mic: Need,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
    reflection: Boolean = true,
) {
    val canAsk = camera == Need.Ask || mic == Need.Ask
    Column(
        modifier
            .widthIn(max = CardMaxWidth)
            .padding(horizontal = LabTheme.spacing.large)
            .background(LabTheme.colors.surface, LabTheme.shapes.ExtraLarge)
            // Landscape or large text can make it taller than the stage: it scrolls, so the
            // buttons are always within reach.
            .verticalScroll(rememberScrollState())
            .padding(LabTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.smallMedium),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(LabTheme.spacing.smallMedium)) {
            if (camera != Need.Nothing) CardIcon(Res.drawable.ic_photo_camera)
            if (mic != Need.Nothing) CardIcon(Res.drawable.ic_mic)
        }
        CardText(Res.string.mirror_card_title, LabTheme.colors.textPrimary, title = true)
        CardText(
            if (reflection) Res.string.mirror_card_body_what
            else Res.string.mirror_card_body_what_bathroom,
            LabTheme.colors.textPrimary,
        )
        if (camera == Need.Ask)
            CardText(Res.string.mirror_card_camera_why, LabTheme.colors.textMuted)
        if (mic == Need.Ask) CardText(Res.string.mirror_card_mic_why, LabTheme.colors.textMuted)
        CardText(Res.string.mirror_card_promise, LabTheme.colors.textMuted)
        if (camera == Need.Settings) {
            CardText(Res.string.mirror_card_camera_blocked, LabTheme.colors.textPrimary)
        }
        if (mic == Need.Settings) {
            CardText(Res.string.mirror_card_mic_blocked, LabTheme.colors.textPrimary)
        }
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
}

@Composable
private fun CardIcon(icon: DrawableResource) {
    Icon(
        painterResource(icon),
        contentDescription = null,
        tint = LabTheme.colors.accent,
        modifier = Modifier.size(CardIconSize),
    )
}

@Composable
private fun CardText(text: StringResource, color: Color, title: Boolean = false) {
    Text(
        stringResource(text),
        color = color,
        style = if (title) LabTheme.typography.title else LabTheme.typography.body,
        textAlign = TextAlign.Center,
    )
}

@Preview
@Composable
private fun MirrorPermissionCardPreview() {
    LabTheme {
        MirrorPermissionCard(
            camera = Need.Ask,
            mic = Need.Ask,
            onAllow = {},
            onOpenSettings = {},
            onNotNow = {},
        )
    }
}
