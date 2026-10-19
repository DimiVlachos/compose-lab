package dev.dimvlachos.lab.demo.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.action_stop
import dev.dimvlachos.lab.resources.action_watch
import dev.dimvlachos.lab.resources.demo_tour_playing
import dev.dimvlachos.lab.resources.ic_play_arrow
import dev.dimvlachos.lab.resources.ic_stop
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
            Text(
                stringResource(if (playing) Res.string.action_stop else Res.string.action_watch),
                style = LabTheme.typography.label,
            )
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

/** Over a demo while its tour plays: what is happening, and that a touch ends it. */
@Composable
internal fun TourPlayingPill(modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(LabTheme.colors.accentSoft, LabTheme.shapes.full)
            .padding(horizontal = LabTheme.spacing.medium, vertical = LabTheme.spacing.small)
    ) {
        Text(
            stringResource(Res.string.demo_tour_playing),
            color = LabTheme.colors.textPrimary,
            style = LabTheme.typography.label,
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
