package dev.dimvlachos.lab.cardfiledemo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.cardfiledemo.Contact
import dev.dimvlachos.lab.core.presentation.ui.CardFilePaper
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.cardfile_call
import dev.dimvlachos.lab.resources.cardfile_message
import org.jetbrains.compose.resources.stringResource

/** The card pulled out of the file, large and facing you, with Call and Message under it. */
@Composable
internal fun PulledContact(contact: Contact, onCall: () -> Unit, onMessage: () -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(
            Modifier.fillMaxWidth()
                // A 5 × 3 index card, larger than the one on the wheel.
                .aspectRatio(5f / 3f)
                .shadow(16.dp, RoundedCornerShape(8.dp))
                .background(CardFilePaper.cream, RoundedCornerShape(8.dp))
                .padding(24.dp)
        ) {
            ContactCardFace(contact, scale = 1.4f)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onCall,
                modifier = Modifier.testTag("call"),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = LabTheme.colors.accent,
                        contentColor = LabTheme.colors.onAccent,
                    ),
            ) {
                Text(stringResource(Res.string.cardfile_call))
            }
            FilledTonalButton(
                onClick = onMessage,
                modifier = Modifier.testTag("message"),
                colors =
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = LabTheme.colors.bar,
                        contentColor = LabTheme.colors.textPrimary,
                    ),
            ) {
                Text(stringResource(Res.string.cardfile_message))
            }
        }
    }
}
