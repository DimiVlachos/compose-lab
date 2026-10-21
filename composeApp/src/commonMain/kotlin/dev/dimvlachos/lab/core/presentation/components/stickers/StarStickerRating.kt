package dev.dimvlachos.lab.core.presentation.components.stickers

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.core.platform.SilentSoundPlayer
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.jetbrains_mono_semibold
import kotlin.math.hypot
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.Font

/** The sounds the rating plays, for the caller to load. */
internal val StickerSounds = setOf(Sound.StickerPeel, Sound.StickerPress, Sound.StickerSettle)

/**
 * The fingertip a script moves over the rating, in the rating's own CSS pixels, and how far down it
 * is. The rating draws it; [StickerHand] moves it and drives the same gesture a finger does.
 */
@Stable
internal class StickerFinger {
    var at by mutableStateOf(Offset.Zero)
    var alpha by mutableFloatStateOf(0f)
}

/**
 * What a touch on the rating does, a finger's or a script's: a drag counts the star centres passed,
 * a tap jumps to the nearest star, and the finger's x tips the light.
 */
internal class StickerGesture(
    private val state: StarStickerState,
    private val light: StickerLight,
    private val layout: () -> StickerLayout,
) {
    private var start: Offset? = null
    private var moved = false

    fun down(at: Offset) {
        start = at
        moved = false
        tilt(at.x)
    }

    fun move(at: Offset) {
        val from = start ?: return
        if (!moved && hypot(at.x - from.x, at.y - from.y) > TouchSlop) moved = true
        tilt(at.x)
        if (moved) state.ask(layout().countAt(at.x))
    }

    fun up(at: Offset) {
        if (start == null) return
        if (!moved) state.ask(layout().nearestAt(at.x))
        cancel()
    }

    fun cancel() {
        start = null
        light.pointerTiltGoal = 0f
    }

    private fun tilt(x: Float) {
        val w = layout().width
        light.pointerTiltGoal = ((x - w / 2) / (w / 2)).coerceIn(-1f, 1f)
    }

    private companion object {
        const val TouchSlop = 6f
    }
}

/**
 * A star rating as a teacher's sheet of gold foil star stickers under a row of dashed slots. Drag
 * across either and each star passed peels off the sheet with a curl, flies up and is pressed into
 * its slot, with a glint and a little bounce; drag back and the last ones go back on the sheet. A
 * tap jumps to that star. [label] names how many have landed, under the row; [valueText] is what a
 * screen reader hears for a score, after [description].
 *
 * It is one slider for a screen reader and the keyboard: the arrows step it, Home and End go to the
 * ends, and the digits 0 to 5 set it. With [reducedMotion] the stars fade in place.
 */
@Composable
internal fun StarStickerRating(
    state: StarStickerState,
    label: (Int) -> String,
    valueText: (Int) -> String,
    description: String,
    labelColor: Color,
    labelSetColor: Color,
    modifier: Modifier = Modifier,
    inks: StickerInks = StickerInks.Day,
    sounds: SoundPlayer = SilentSoundPlayer,
    reducedMotion: Boolean = false,
    finger: StickerFinger? = null,
    hand: StickerHand? = null,
) {
    val haptics = LocalHapticFeedback.current
    val light = remember { StickerLight() }
    val reduced by rememberUpdatedState(reducedMotion)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state, sounds, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            var acc = 0f
            while (true) {
                // At rest the loop sleeps until a star is asked to move or the finger tips the
                // light. The prototype's light drifted forever; here the foil holds still between
                // gestures, so an idle screen draws nothing.
                if (!state.moving && light.settled) {
                    val asleep = state.target to light.pointerTiltGoal
                    snapshotFlow { state.target to light.pointerTiltGoal }.first { it != asleep }
                    last = 0L
                }
                withFrameNanos { now ->
                    // The first frame moves nothing; a stall moves at most .15 s, as the prototype.
                    val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                    last = now
                    acc += dt
                    while (acc >= StickerStep) {
                        acc -= StickerStep
                        for (e in state.step(StickerStep, reduced)) {
                            when (e) {
                                is StickerEvent.Peel -> sounds.play(Sound.StickerPeel, .7f)
                                is StickerEvent.Press -> {
                                    sounds.play(Sound.StickerPress, .9f)
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                                is StickerEvent.Settle -> sounds.play(Sound.StickerSettle, .6f)
                            }
                        }
                    }
                    light.step(dt)
                }
            }
        }
    }

    val measurer = rememberTextMeasurer()
    val mono = Font(Res.font.jetbrains_mono_semibold, FontWeight.SemiBold)
    val fonts = remember(mono) { mapOf(MonoFont to FontFamily(mono)) }
    val landed = state.landed
    val target = state.target
    val color by
        animateColorAsState(
            if (landed > 0) labelSetColor else labelColor,
            tween(if (reducedMotion) 0 else 250),
            label = "sticker label",
        )

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val layout = remember(maxWidth) { StickerLayout(maxWidth.value) }
        val gesture = remember(state, light) { StickerGesture(state, light) { layout } }
        val density = LocalDensity.current.density
        SideEffect { hand?.bind(layout, gesture, finger) }
        Box(
            Modifier.fillMaxWidth()
                .height(layout.height.dp)
                .testTag("stickers")
                .onKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val n =
                        when (e.key) {
                            Key.DirectionRight,
                            Key.DirectionUp -> target + 1
                            Key.DirectionLeft,
                            Key.DirectionDown -> target - 1
                            Key.MoveHome -> 0
                            Key.MoveEnd -> StickerCount
                            Key.Zero,
                            Key.NumPad0 -> 0
                            Key.One,
                            Key.NumPad1 -> 1
                            Key.Two,
                            Key.NumPad2 -> 2
                            Key.Three,
                            Key.NumPad3 -> 3
                            Key.Four,
                            Key.NumPad4 -> 4
                            Key.Five,
                            Key.NumPad5 -> 5
                            else -> return@onKeyEvent false
                        }
                    state.ask(n)
                    true
                }
                .focusable()
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    stateDescription = valueText(target)
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(target.toFloat(), 0f..StickerCount.toFloat(), 4)
                    setProgress { v ->
                        state.ask(kotlin.math.round(v).toInt())
                        true
                    }
                }
                .pointerInput(gesture, density) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        hand?.interrupt()
                        gesture.down(down.position / density)
                        var pointer = down
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointer.id }
                            if (change == null) {
                                gesture.cancel()
                                break
                            }
                            pointer = change
                            if (!change.pressed) {
                                gesture.up(change.position / density)
                                break
                            }
                            change.consume()
                            gesture.move(change.position / density)
                        }
                    }
                },
        ) {
            Canvas(Modifier.matchParentSize()) {
                state.frame
                val f = finger
                drawIntoCanvas { canvas ->
                    val c = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                    c.textMeasurer = measurer
                    c.fonts = fonts
                    c.setTransform(density, 0f, 0f, density, 0f, 0f)
                    c.paintStickers(layout, state, light, inks, reduced, density)
                }
                if (f != null) drawTouch(Color.White, f.at * density, f.alpha)
            }
            Text(
                label(landed),
                // The slider's own description already says the score.
                Modifier.clearAndSetSemantics {}
                    .fillMaxWidth()
                    .offset(y = layout.labelTop.dp)
                    .height(StickerLayout.LabelHeight.dp)
                    .wrapContentHeight(),
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}
