package dev.dimvlachos.lab.core.presentation.components.locket

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.cormorant_garamond_medium_italic
import dev.dimvlachos.lab.resources.great_vibes_regular
import kotlin.time.TimeSource
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.Font

/** The clips the locket plays. */
internal val LocketSounds =
    setOf(
        Sound.LocketOpen,
        Sound.LocketSnap,
        Sound.LocketPlace,
        Sound.LocketLift,
        Sound.LocketBack,
        Sound.LocketChain,
    )

/** What the locket says to screen readers and writes over its strip. */
internal class LocketLabels(
    val open: String,
    val close: String,
    val recent: String,
    val hint: String,
    /** "Use photo: …", one for each of the Recent photos, in order. */
    val usePhoto: List<String>,
)

/**
 * A gold locket on a fine chain holding the profile photo, over a strip of Recent [photos], as the
 * prototype draws it: tap the catch and the lid springs open on its hinge, drag a photo up into it
 * and it trims itself to the oval behind the glass, tap or push the lid shut and it snaps closed
 * and the chain swings. Grab the closed locket to swing it. It fills its space and draws itself
 * centred in it. [hand], when given, is a scripted fingertip drawn over it; a real touch calls
 * [onTouch] first.
 */
@Composable
internal fun Locket(
    state: LocketState,
    photos: List<ImageBitmap>,
    labels: LocketLabels,
    muted: Color,
    accent: Color,
    sounds: SoundPlayer,
    modifier: Modifier = Modifier,
    hand: LocketHand? = null,
    onTouch: () -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state, sounds, haptics) {
        val listener: (LocketEvent) -> Unit = { event ->
            when (event) {
                LocketEvent.Open -> {
                    sounds.play(Sound.LocketOpen)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                LocketEvent.Snap -> {
                    sounds.play(Sound.LocketSnap)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                LocketEvent.Lift -> {
                    sounds.play(Sound.LocketLift)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                }
                LocketEvent.Place -> {
                    sounds.play(Sound.LocketPlace)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                LocketEvent.Back -> sounds.play(Sound.LocketBack)
                LocketEvent.Chain -> sounds.play(Sound.LocketChain)
            }
        }
        state.onEvent = listener
        try {
            awaitCancellation()
        } finally {
            if (state.onEvent === listener) state.onEvent = null
        }
    }
    LaunchedEffect(state, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            while (true) {
                if (state.settled) {
                    // At rest the loop sleeps: open, until a touch; shut, until the catch's next
                    // glint comes round.
                    val rest = state.restMs()
                    if (rest == null || rest > 0f) {
                        val seen = state.wake
                        state.sleepMark = TimeSource.Monotonic.markNow()
                        val woke =
                            if (rest == null) {
                                snapshotFlow { state.wake }.first { it != seen }
                                true
                            } else {
                                withTimeoutOrNull(rest.toLong() + 1L) {
                                    snapshotFlow { state.wake }.first { it != seen }
                                } != null
                            }
                        if (woke) {
                            state.wakeUp()
                        } else {
                            state.sleepMark = null
                            state.idle(rest!! + 1f)
                        }
                        last = 0L
                    }
                }
                withFrameNanos { now ->
                    // The first frame moves nothing; a stall can't blow the springs up.
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                    last = now
                    state.step(dt)
                }
            }
        }
    }
    val dark = LabTheme.isDark
    val touch = LabTheme.colors.touch
    val measurer = rememberTextMeasurer(cacheSize = 16)
    val vibes = FontFamily(Font(Res.font.great_vibes_regular))
    val cormorant = FontFamily(Font(Res.font.cormorant_garamond_medium_italic))
    val fonts =
        remember(vibes, cormorant) { mapOf(InitialFont to vibes, EngravingFont to cormorant) }
    val ink =
        remember(muted, accent, labels) { LocketInk(muted, accent, labels.recent, labels.hint) }
    val touched by rememberUpdatedState(onTouch)
    BoxWithConstraints(
        modifier.onPreviewKeyEvent {
            if (it.type == KeyEventType.KeyDown && it.key == Key.Escape && state.isOpen) {
                touched()
                state.close()
                true
            } else false
        },
    ) {
        val box = remember(maxWidth, maxHeight) { LocketBox(maxWidth.value, maxHeight.value) }
        SideEffect { state.box = box }
        val current by rememberUpdatedState(box)
        Canvas(
            // Clipped to the hero, as the prototype's canvas is: the chain hangs from above it.
            Modifier.fillMaxSize().clipToBounds().pointerInput(state) {
                awaitEachGesture {
                    val first = awaitFirstDown(requireUnconsumed = false)
                    touched()
                    first.consume()
                    val b = current
                    fun bx(p: Offset) = b.toDesignX(p.x / density)
                    fun by(p: Offset) = b.toDesignY(p.y / density)
                    state.down(bx(first.position), by(first.position))
                    var ended = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val c = event.changes.firstOrNull { it.id == first.id } ?: break
                            if (c.changedToUpIgnoreConsumed()) {
                                // A cancelled pointer comes up already consumed: a carried photo
                                // flies home rather than landing where it was.
                                if (c.isConsumed) state.cancel()
                                else state.up(bx(c.position), by(c.position))
                                c.consume()
                                ended = true
                                break
                            }
                            if (c.positionChanged()) {
                                state.move(bx(c.position), by(c.position))
                                c.consume()
                            }
                        }
                    } finally {
                        if (!ended) state.cancel()
                    }
                }
            },
        ) {
            state.frame
            drawIntoCanvas { canvas ->
                val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                c.textMeasurer = measurer
                c.fonts = fonts
                // The labels and the engraving are small fonts under a large scale: laid out at the
                // size they are seen, their spaces stay open.
                c.textResolution = density * box.s
                c.scale(density, density)
                c.drawLocket(state, box, dark, photos, ink, density)
            }
            if (hand != null && hand.alpha > 0f) {
                val at =
                    Offset((box.ox + hand.x * box.s) * density, (box.oy + hand.y * box.s) * density)
                drawTouch(touch, at, hand.alpha)
            }
        }
        // The locket and each Recent photo are buttons for screen readers and keys; a finger goes
        // through to the canvas under them.
        val lockLabel = if (state.isOpen) labels.close else labels.open
        HitBox(
            box,
            Locket.AX - Locket.RX - 6f,
            Locket.AY + Locket.BC - Locket.RY - 6f,
            Locket.RX * 2f + 12f,
            Locket.RY * 2f + 12f,
            Modifier.testTag("locket").semantics {
                role = Role.Button
                contentDescription = lockLabel
                onClick {
                    touched()
                    state.toggle()
                    true
                }
                if (state.isOpen) {
                    collapse {
                        touched()
                        state.close()
                        true
                    }
                } else {
                    expand {
                        touched()
                        state.open()
                        true
                    }
                }
            },
        ) {
            touched()
            state.toggle()
        }
        for (j in 0 until Locket.PHOTOS) {
            HitBox(
                box,
                box.tcx(j) - box.ts / 2f,
                box.tcy - box.ts / 2f,
                box.ts,
                box.ts,
                Modifier.testTag("photo-$j").semantics {
                    role = Role.Button
                    contentDescription = labels.usePhoto[j]
                    onClick {
                        touched()
                        state.usePhoto(j)
                        true
                    }
                },
            ) {
                touched()
                state.usePhoto(j)
            }
        }
    }
}

/** An invisible target over part of the box, in its units, that Enter or Space works. */
@Composable
private fun HitBox(
    box: LocketBox,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    modifier: Modifier,
    onKey: () -> Unit,
) {
    Box(
        Modifier.offset((box.ox + x * box.s).dp, (box.oy + y * box.s).dp)
            .size((w * box.s).dp, (h * box.s).dp)
            .then(modifier)
            .onKeyEvent {
                if (
                    it.type == KeyEventType.KeyDown &&
                        (it.key == Key.Enter || it.key == Key.Spacebar)
                ) {
                    onKey()
                    true
                } else false
            }
            .focusable(),
    )
}
