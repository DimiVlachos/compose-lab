package dev.dimvlachos.lab.frostcarddemo.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.frostcard.FrostCardLayout
import dev.dimvlachos.lab.core.presentation.components.frostcard.FrostCardScreen
import dev.dimvlachos.lab.core.presentation.components.frostcard.FrostCardSounds
import dev.dimvlachos.lab.core.presentation.components.frostcard.FrostCardState
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.FrostCardScreenColors
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.frostcarddemo.FrostCardHand
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The prototype's bank card screen, where Freeze card really freezes the card. The script's tap and
 * thumb go through the same switch and press a finger uses.
 */
@Composable
internal fun FrostCardDemo(
    state: DemoState,
    card: FrostCardState = remember { FrostCardState() },
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val sounds = rememberSoundPlayer(FrostCardSounds)
    val haptics = LocalHapticFeedback.current
    SideEffect { card.reducedMotion = reducedMotion }
    val layout = remember { FrostCardLayout() }
    val toggle =
        remember(card, haptics) {
            {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                card.toggle()
            }
        }
    val hand = remember(card, layout, toggle) { FrostCardHand(card, layout, toggle) }
    // The script's moves take turns: on a slow device one can run past its slot, and the next
    // waits for it rather than moving the finger at the same time.
    val turn = remember { Mutex() }
    DisposableEffect(state, hand) {
        state.setControlTapHandler { control ->
            turn.withLock {
                try {
                    hand.play(control)
                } catch (e: CancellationException) {
                    hand.stop()
                    throw e
                }
            }
        }
        // Back to a warm card if a stopped run left it frozen, so the clip loops.
        state.setFeedResetHandler {
            turn.withLock {
                hand.stop()
                if (card.frozen || card.f > 0f) card.reset()
            }
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A script the user takes over leaves no fingertip behind.
    LaunchedEffect(state.replay) { if (!state.replay) hand.stop() }

    val colors = if (LabTheme.isDark) FrostCardScreenColors.Dark else FrostCardScreenColors.Light
    val touch = LabTheme.colors.touch
    val insets =
        if (state.recording) WindowInsets(0)
        else
            WindowInsets.systemBars
                .union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    Box(Modifier.fillMaxSize().windowInsetsPadding(insets)) {
        FrostCardScreen(card, layout, colors, sounds, toggle, Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            val a = hand.shown
            if (a <= 0f) return@Canvas
            val at = hand.at * density
            val k = if (hand.down) .82f else 1f
            scale(k, k, pivot = at) { drawTouch(touch, at, a) }
        }
    }
}
