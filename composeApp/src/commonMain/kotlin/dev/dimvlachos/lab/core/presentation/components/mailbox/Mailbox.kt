package dev.dimvlachos.lab.core.presentation.components.mailbox

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import kotlin.math.abs
import kotlinx.coroutines.flow.first

/** The clips the mailbox plays. */
internal val MailboxSounds =
    setOf(Sound.TinTunk, Sound.FlagTwang, Sound.DoorCreak, Sound.EnvelopeSlip, Sound.MailTick)

/**
 * An inbox button that is a tin rural mailbox: the red flag stands up with [unread] painted on it,
 * and comes down once it is none. Tapping it, or pulling down on it past halfway, calls [onClick];
 * the caller opens the inbox with [MailboxState.open], which drops the door open on [letters]
 * envelopes. Mail comes in through [MailboxState.deliver].
 *
 * It takes 52 by 48 dp, the size of a top bar's action, and draws a 68 dp mailbox over it.
 * [contentDescription] is what a screen reader says, such as "Inbox, 2 unread". [dark] picks the
 * prototype's dusk shades for the tin and the post.
 */
@Composable
internal fun Mailbox(
    state: MailboxState,
    unread: Int,
    letters: Int,
    contentDescription: String,
    dark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sounds: SoundPlayer = rememberSoundPlayer(MailboxSounds),
) {
    val haptics = LocalHapticFeedback.current
    SideEffect {
        state.cue = { cue, volume ->
            when (cue) {
                MailboxCue.Tunk -> sounds.play(Sound.TinTunk, volume)
                MailboxCue.LowTunk -> sounds.play(Sound.TinTunk, volume, rate = LowRate)
                MailboxCue.Twang -> {
                    sounds.play(Sound.FlagTwang, TwangVolume)
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
                MailboxCue.Creak -> sounds.play(Sound.DoorCreak, CreakVolume)
                MailboxCue.Slip -> sounds.play(Sound.EnvelopeSlip, SlipVolume)
                MailboxCue.Tick -> sounds.play(Sound.MailTick, TickVolume)
            }
        }
    }
    LaunchedEffect(state, unread) { state.setUnread(unread) }
    // Runs the parts while they move, and sleeps once they have settled until something stirs
    // them.
    LaunchedEffect(state) {
        var last = -1L
        while (true) {
            if (state.isSettled) {
                last = -1L
                val woken = state.wake
                snapshotFlow { state.wake }.first { it != woken }
            }
            withFrameNanos { t ->
                if (last >= 0) state.advance((t - last) / 1e9f)
                last = t
            }
        }
    }
    val measurer = rememberTextMeasurer()
    val interactions = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(52.dp, 48.dp)
            .pointerInput(state) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val dy = (change.position.y - down.position.y) / density
                        if (!moved && abs(dy) > DragSlopDp) moved = true
                        if (moved) {
                            change.consume()
                            state.dragDoor(dy)
                        }
                        if (!change.pressed) {
                            if (moved && state.releaseDoor()) onClick()
                            break
                        }
                    }
                    if (moved && state.dragging) state.cancelDrag()
                }
            }
            .clickable(interactions, indication = null, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            Modifier.requiredSize(CanvasSize)
                // The prototype sets the box a little left of the button's middle, so the raised
                // flag has room on the right.
                .offset(x = CanvasSize * -0.04f),
        ) {
            state.frame
            drawIntoCanvas { canvas ->
                val g = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                g.textMeasurer = measurer
                MailboxPainter(g, dark).draw(state, letters, size.width)
            }
        }
    }
}

private val CanvasSize = 68.dp

// A drag starts this far down or up; less is a tap.
private const val DragSlopDp = 6f

// The tin struck low is the clip played slower.
private const val LowRate = 0.7f
private const val TwangVolume = 0.8f
private const val CreakVolume = 0.7f
private const val SlipVolume = 0.6f
private const val TickVolume = 0.5f
