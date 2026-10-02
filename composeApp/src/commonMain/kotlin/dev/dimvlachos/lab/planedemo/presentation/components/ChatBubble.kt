package dev.dimvlachos.lab.planedemo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dev.dimvlachos.lab.core.presentation.components.paperplane.ease
import dev.dimvlachos.lab.core.presentation.ui.LabTheme

// A bubble's tail is a sharp corner: a sent one's by the send button, a received one's at the top
// on the sender's side, where a reply starts.
internal val MineShape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
internal val TheirsShape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
internal val BubblePadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
internal val BubbleMaxWidth = 264.dp

// Coming in, a bubble fades up over this first share of its letters coming down.
private const val BubbleFadeShare = 0.3f

/**
 * One message. While its letters are still coming down, [delivered] says how far (0 to 1): its text
 * is left out for the falling letters to stand in for, and the bubble grows in behind them from
 * where the first one lands, the left, out to its full width. [laidOut] hears where its text lies.
 */
@Composable
internal fun ChatBubble(
    text: String,
    mine: Boolean,
    modifier: Modifier = Modifier,
    delivered: (() -> Float)? = null,
    laidOut: ((TextLayoutResult, Offset) -> Unit)? = null,
) {
    val colors = LabTheme.colors
    val color = if (mine) colors.accent else colors.bubbleTheirs
    val shape = if (mine) MineShape else TheirsShape
    val layout = remember { arrayOfNulls<TextLayoutResult>(1) }
    Text(
        text,
        color = if (mine) colors.onAccent else colors.textPrimary,
        style = LabTheme.typography.body,
        onTextLayout = { layout[0] = it },
        modifier =
            modifier
                .then(
                    if (delivered == null) {
                        Modifier.background(color, shape)
                    } else {
                        Modifier.drawBehind {
                            val p = delivered()
                            if (p <= 0f) return@drawBehind
                            val grown = ease(p)
                            val width =
                                lerp(size.height, size.width, grown).coerceAtMost(size.width)
                            val outline =
                                shape.createOutline(Size(width, size.height), layoutDirection, this)
                            drawOutline(outline, color, alpha = ease(p / BubbleFadeShare))
                        }
                    }
                )
                .padding(BubblePadding)
                .then(
                    if (laidOut == null) Modifier
                    else
                        Modifier.onGloballyPositioned { at ->
                            layout[0]?.let { laidOut(it, at.positionInRoot()) }
                        }
                )
                .then(
                    if (delivered == null) {
                        Modifier
                    } else {
                        // Neither seen nor read out yet: it is read out as it lands.
                        Modifier.drawWithContent {}.clearAndSetSemantics { testTag = InFlightTag }
                    }
                ),
    )
}

/** What a message on its way is known by, until it lands. */
internal const val InFlightTag = "message-in-flight"
