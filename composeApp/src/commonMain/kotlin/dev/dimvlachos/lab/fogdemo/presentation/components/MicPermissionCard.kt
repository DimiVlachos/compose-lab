package dev.dimvlachos.lab.fogdemo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.ic_mic
import dev.dimvlachos.lab.resources.mic_card_allow
import dev.dimvlachos.lab.resources.mic_card_blocked
import dev.dimvlachos.lab.resources.mic_card_body_what
import dev.dimvlachos.lab.resources.mic_card_body_why
import dev.dimvlachos.lab.resources.mic_card_not_now
import dev.dimvlachos.lab.resources.mic_card_open_settings
import dev.dimvlachos.lab.resources.mic_card_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val MicIconSize = 32.dp
private val CardMaxWidth = 360.dp

/**
 * Explains, before the system asks, what to do on the glass and why the microphone is needed: a
 * phone cannot feel a breath, only hear it. [blocked] when the system will not ask any more, and
 * only settings can turn the microphone on.
 */
@Composable
internal fun MicPermissionCard(
    blocked: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .widthIn(max = CardMaxWidth)
            .padding(horizontal = LabTheme.spacing.large)
            .background(LabTheme.colors.surface, LabTheme.shapes.ExtraLarge)
            .padding(LabTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.smallMedium),
    ) {
        Icon(
            painterResource(Res.drawable.ic_mic),
            contentDescription = null,
            tint = LabTheme.colors.accent,
            modifier = Modifier.size(MicIconSize),
        )
        Text(
            stringResource(Res.string.mic_card_title),
            color = LabTheme.colors.textPrimary,
            style = LabTheme.typography.title,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(Res.string.mic_card_body_what),
            color = LabTheme.colors.textPrimary,
            style = LabTheme.typography.body,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(Res.string.mic_card_body_why),
            color = LabTheme.colors.textMuted,
            style = LabTheme.typography.body,
            textAlign = TextAlign.Center,
        )
        if (blocked) {
            Text(
                stringResource(Res.string.mic_card_blocked),
                color = LabTheme.colors.textPrimary,
                style = LabTheme.typography.body,
                textAlign = TextAlign.Center,
            )
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
                Text(stringResource(Res.string.mic_card_not_now))
            }
            Button(
                onClick = if (blocked) onOpenSettings else onAllow,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = LabTheme.colors.accent,
                        contentColor = LabTheme.colors.onAccent,
                    ),
            ) {
                Text(
                    stringResource(
                        if (blocked) Res.string.mic_card_open_settings
                        else Res.string.mic_card_allow
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun MicPermissionCardPreview() {
    LabTheme {
        MicPermissionCard(blocked = false, onAllow = {}, onOpenSettings = {}, onNotNow = {})
    }
}
