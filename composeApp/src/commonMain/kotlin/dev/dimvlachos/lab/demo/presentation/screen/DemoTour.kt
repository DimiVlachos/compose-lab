package dev.dimvlachos.lab.demo.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_stop
import dev.dimvlachos.lab.resources.action_watch
import dev.dimvlachos.lab.resources.demo_tour_playing
import dev.dimvlachos.lab.resources.ic_play_arrow
import dev.dimvlachos.lab.resources.ic_stop
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The bar's way into a demo's tour: Watch plays it, and while it plays, Stop hands the demo back.
 * Named in words, so it reads as a tour to watch rather than a control of the demo itself.
 */
@Composable
internal fun WatchTourChip(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LabTheme.colors
    AssistChip(
        onClick = onClick,
        modifier = modifier,
        label = {
            // Both words are laid out, the one not shown invisible, so the chip keeps the width
            // of the longer and the button beside it never shifts as Watch turns into Stop.
            Box(contentAlignment = Alignment.Center) {
                ChipWord(Res.string.action_watch, shown = !playing)
                ChipWord(Res.string.action_stop, shown = playing)
            }
        },
        leadingIcon = {
            Icon(
                painterResource(if (playing) Res.drawable.ic_stop else Res.drawable.ic_play_arrow),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        colors =
            AssistChipDefaults.assistChipColors(
                containerColor = if (playing) colors.accentSoft else colors.background,
                labelColor = colors.textPrimary,
                leadingIconContentColor = colors.accent,
            ),
        border =
            AssistChipDefaults.assistChipBorder(enabled = true, borderColor = colors.chipBorder),
    )
}

@Composable
private fun ChipWord(word: StringResource, shown: Boolean) {
    Text(
        stringResource(word),
        style = LabTheme.typography.label,
        modifier = if (shown) Modifier else Modifier.alpha(0f).clearAndSetSemantics {},
    )
}

/**
 * Under the demo's title while its tour plays: what is happening, and that a touch ends it. It
 * fades and slides in and out with the tour, inside the bar's own height, so nothing below moves.
 */
@Composable
internal fun TourPlayingNote(visible: Boolean, modifier: Modifier = Modifier) {
    val motion = LabTheme.motion
    fun <T> spec() = tween<T>(motion.mediumMs, easing = motion.ease)
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(spec()) + expandVertically(spec(), expandFrom = Alignment.Top),
        exit = fadeOut(spec()) + shrinkVertically(spec(), shrinkTowards = Alignment.Top),
    ) {
        // Short, and a size under the caption's, so it fits beside the bar's buttons on a 360 dp
        // phone.
        Text(
            stringResource(Res.string.demo_tour_playing),
            color = LabTheme.colors.accent,
            style = LabTheme.typography.caption.copy(fontSize = 12.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * While [playing], the first touch anywhere on the demo calls [onTakeOver], so the user takes the
 * demo from the tour instead of fighting it. The touch is only looked at, never taken: the demo
 * still gets it.
 */
internal fun Modifier.takeOverOnTouch(playing: Boolean, onTakeOver: () -> Unit): Modifier =
    if (!playing) this
    else
        pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                onTakeOver()
            }
        }
