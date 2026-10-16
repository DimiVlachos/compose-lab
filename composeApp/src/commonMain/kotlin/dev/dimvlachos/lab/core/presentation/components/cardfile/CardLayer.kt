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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
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
    /** The card's size, and how far its upright slot sits below the stage's centre. */
    size: DpSize,
    centreOffset: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier.align(Alignment.Center)
            // Outermost, so draw and hit order among the cards follow depth.
            .zIndex(zIndex)
            .offset(y = centreOffset)
            .size(size)
            .graphicsLayer {
                val angle = CardWheel.cardAngle(index, state.position)
                // Positive turns the top away from the viewer: the cards behind lean back, and a
                // flipped card's negative angle brings its top toward you and down past level.
                rotationX = angle
                val behind = (index - state.position).coerceIn(0f, CardFileDimens.BehindLiftCards)
                translationY = -behind * CardFileDimens.BehindLift.toPx()
                transformOrigin =
                    TransformOrigin(0.5f, 1f + CardFileDimens.RodDrop.toPx() / this.size.height)
                cameraDistance = 28f * density
                // The pulled card leaves its slot empty.
                alpha = if (state.pulled == index) 1f - state.pullProgress else 1f
            }
            // The caller's taps and semantics go inside the layer, so they follow the card as
            // it turns and rises rather than its upright slot.
            .then(modifier)
            .drawWithContent {
                val angle = CardWheel.cardAngle(index, state.position)
                val d = index - state.position
                if (angle < -90f) {
                    // Fallen past level, the card shows its plain back, in the shadow under the
                    // lamp; the further down the pile, the darker.
                    drawRoundRect(CardFileColors.cardBack, cornerRadius = corner(6.dp.toPx()))
                    val shade = (0.62f + (-d - 1f).coerceAtLeast(0f) * 0.06f).coerceIn(0f, 0.8f)
                    drawRoundRect(
                        Color.Black.copy(alpha = shade),
                        cornerRadius = corner(6.dp.toPx()),
                    )
                } else {
                    drawContent()
                    // Upright at the front, the card takes the lamp's full light; turned away or
                    // further back, less of it.
                    val shade = (abs(angle) / 160f + d.coerceIn(0f, 4f) * 0.07f).coerceIn(0f, 0.6f)
                    if (shade > 0f) drawRect(Color.Black.copy(alpha = shade))
                }
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
        for (x in listOf(-0.2f, 0.2f)) {
            Box(
                Modifier.align(Alignment.BottomCenter)
                    .offset(x = size.width * x, y = 4.dp)
                    .size(14.dp, 10.dp)
                    .background(
                        CardFileColors.notch,
                        RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp),
                    )
            )
        }
    }
}

private fun corner(r: Float) = CornerRadius(r, r)
