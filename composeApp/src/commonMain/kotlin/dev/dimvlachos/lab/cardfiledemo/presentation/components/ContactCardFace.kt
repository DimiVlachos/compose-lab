package dev.dimvlachos.lab.cardfiledemo.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.cardfiledemo.Contact
import dev.dimvlachos.lab.core.presentation.ui.CardFilePaper

/**
 * A contact's index card: the name typed across the top, the number and note in blue ink. The ink
 * leans and sits off the line by the contact's seed, so no two cards are written alike.
 */
@Composable
internal fun ContactCardFace(contact: Contact, scale: Float = 1f) {
    // The seed may be negative: the lean runs from -1.5° to 1.5°, the shifts both ways.
    val tilt = (contact.seed % 31) / 20f
    val shift = (contact.seed % 7).toFloat()
    val numberDrop = ((contact.seed / 7) % 3).toFloat()
    val noteDrop = ((contact.seed / 21) % 3).toFloat()
    Column(Modifier.fillMaxWidth()) {
        Text(
            contact.last.uppercase() + ", " + contact.first,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = (15 * scale).sp,
            color = CardFilePaper.typed,
        )
        HorizontalDivider(Modifier.padding(top = 4.dp), color = CardFilePaper.rule)
        Spacer(Modifier.height((10 * scale).dp))
        Column(
            Modifier.graphicsLayer {
                rotationZ = tilt
                translationX = shift * density
            }
        ) {
            Text(
                contact.number,
                Modifier.graphicsLayer { translationY = numberDrop * density },
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = (19 * scale).sp,
                color = CardFilePaper.ink,
            )
            Text(
                contact.note,
                Modifier.graphicsLayer { translationY = noteDrop * density },
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = (15 * scale).sp,
                color = CardFilePaper.ink,
            )
        }
    }
}
