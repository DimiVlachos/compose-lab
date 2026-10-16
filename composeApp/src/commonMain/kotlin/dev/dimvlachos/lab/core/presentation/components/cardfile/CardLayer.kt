package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.dimvlachos.lab.core.presentation.ui.CardFileColors
import kotlin.math.abs

/**
 * Card [index] on the wheel: turned on the rod by its [CardWheel.cardAngle] at the state's current
 * position, shaded by how far it faces away from the lamp, and drawn in front of or behind its
 * neighbours by depth. Everything that moves is read in the layer and draw lambdas.
 */
@Composable
internal fun BoxScope.CardLayer(
    index: Int,
    card: FileCard,
    state: CardFileState,
    /** The card's place in its siblings' draw and hit order, nearer cards higher. */
    zIndex: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier.align(Alignment.Center)
            // Outermost, so draw and hit order among the cards follow depth.
            .zIndex(zIndex)
            .size(CardFileDimens.CardWidth, CardFileDimens.CardHeight)
            .graphicsLayer {
                val angle = CardWheel.cardAngle(index, state.position)
                rotationX = -angle
                val behind = (index - state.position).coerceIn(0f, CardFileDimens.BehindLiftCards)
                translationY = -behind * CardFileDimens.BehindLift.toPx()
                transformOrigin =
                    TransformOrigin(0.5f, 1f + CardFileDimens.RodDrop.toPx() / size.height)
                cameraDistance = 14f * density
                // The pulled card leaves its slot empty.
                alpha = if (state.pulled == index) 1f - state.pullProgress else 1f
            }
            // The caller's taps and semantics go inside the layer, so they follow the card as
            // it turns and rises rather than its upright slot.
            .then(modifier)
            .drawWithContent {
                drawContent()
                val angle = CardWheel.cardAngle(index, state.position)
                val shade =
                    (abs(angle) / 110f + (index - state.position).coerceAtLeast(0f) * 0.05f)
                        .coerceIn(0f, 0.7f)
                drawRect(Color.Black.copy(alpha = shade))
            }
            .background(
                if (card is FileCard.Divider) CardFileColors.dividerPaper else CardFileColors.paper,
                RoundedCornerShape(6.dp),
            )
            .border(0.5.dp, CardFileColors.cardEdge, RoundedCornerShape(6.dp))
    ) {
        if (card is FileCard.Divider) {
            Text(
                card.letter.toString(),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = CardFileColors.dividerInk,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            Box(Modifier.padding(16.dp)) { content() }
        }
        // Notches for the rail.
        for (x in listOf(-48, 48)) {
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .offset(x = x.dp, y = 4.dp)
                    .size(14.dp, 10.dp)
                    .background(
                        CardFileColors.notch,
                        RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp),
                    )
            )
        }
    }
}
