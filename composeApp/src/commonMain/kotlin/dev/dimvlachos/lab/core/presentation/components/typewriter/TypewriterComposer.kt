package dev.dimvlachos.lab.core.presentation.components.typewriter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import dev.dimvlachos.lab.core.platform.SilentSoundPlayer
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.typewriter_badge
import dev.dimvlachos.lab.resources.typewriter_hint_full
import dev.dimvlachos.lab.resources.typewriter_hint_pull
import dev.dimvlachos.lab.resources.typewriter_hint_type
import dev.dimvlachos.lab.resources.typewriter_label
import dev.dimvlachos.lab.resources.typewriter_model
import dev.dimvlachos.lab.resources.typewriter_send
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource

/**
 * A chat composer that is a typewriter, typed on its own keys: round, chrome-rimmed keys with glass
 * tops, in a QWERTY layout, with Shift, Backspace, Return, a space bar and a page of figures and
 * stops. The phone's keyboard never opens. Every key dips as it is pressed and strikes the sheet: a
 * type bar flies up from the basket, the ribbon lifts, and the carriage steps on. The bell rings
 * five letters before the margin, and at the margin, or at a space once the bell has rung, the
 * lever swings and the carriage returns by itself; Return starts a new line too. Backspace runs the
 * carriage back and the correction tape lifts the letter off; held, it goes on, slowly. A hardware
 * keyboard types too, once the machine has focus, which it takes as it opens and on a tap on it.
 *
 * To send, pull the sheet up out of the roller: it gives like a rubber band and the ratchet clicks
 * as it comes; let go short of the threshold and it slides back in, past it or flung, and it zips
 * out and [onSend] hears it, with where the whole sheet was in the root as it came free, for a
 * caller to fly it into its thread. Ctrl or ⌘ and Return, or a screen reader's Send action, pull it
 * out by themselves. A fresh sheet then feeds in as the carriage goes home.
 *
 * Its sounds play through [sound], and nothing plays while the phone is on silent; a tick of
 * haptics goes with each strike, a firmer one with the bell and the carriage's bang, and the
 * ratchet ticks under the finger. The machine sits at the foot of the space it is given, the paper
 * standing up over it as lines feed, its keys in its body; its body runs on down behind [insets]'
 * foot. While not [enabled], as while a script plays, it takes neither keys nor touches. Nothing
 * recomposes as the machine moves: only a change of what the hint says, or of the keys' page.
 */
@Composable
internal fun TypewriterComposer(
    state: TypewriterState,
    onSend: (SentSheet) -> Unit,
    modifier: Modifier = Modifier,
    sound: SoundPlayer = SilentSoundPlayer,
    enabled: Boolean = true,
    insets: WindowInsets = WindowInsets(0),
) {
    ReportComposition()
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val currentSound by rememberUpdatedState(sound)
    DisposableEffect(state, haptics) {
        val feel = MachineFeel({ currentSound }, haptics)
        state.feel = feel
        onDispose { if (state.feel === feel) state.feel = object : TypewriterFeel {} }
    }
    // The machine follows the text: what is typed strikes, what is deleted is lifted off.
    LaunchedEffect(state) { snapshotFlow { state.field.text.toString() }.collect(state::typedNow) }
    // The carriage, the paper and the bars step on a frame at a time while anything moves; the
    // loop sleeps once the machine is still.
    LaunchedEffect(state) {
        while (true) {
            snapshotFlow { state.awake }.first { it }
            var last = withFrameNanos { it }
            while (state.awake) {
                withFrameNanos { now ->
                    state.advance((now - last) / 1_000_000_000f)
                    last = now
                }
            }
        }
    }
    val painter = remember { TypewriterPainter() }
    val colors = LabTheme.colors
    val focus = remember { FocusRequester() }
    val currentEnabled by rememberUpdatedState(enabled)
    // Focused as it opens, so a hardware keyboard types straight away; it is no text field, so
    // the phone's keyboard stays down.
    LaunchedEffect(enabled) { if (enabled) runCatching { focus.requestFocus() } }
    // Where the machine lies in the root, and how big a unit is, for a sent sheet's bounds.
    val placed = remember { Placed() }
    val currentOnSend by rememberUpdatedState(onSend)
    SideEffect {
        state.onSent = { sheet ->
            val u = placed.unit
            val o = placed.machine
            val b = sheet.bounds
            currentOnSend(
                SentSheet(
                    sheet.text,
                    sheet.lines,
                    Rect(o.x + b.left * u, o.y + b.top * u, o.x + b.right * u, o.y + b.bottom * u),
                )
            )
        }
    }
    val label = stringResource(Res.string.typewriter_label)
    val sendLabel = stringResource(Res.string.typewriter_send)
    val badgeText = stringResource(Res.string.typewriter_badge)
    val modelText = stringResource(Res.string.typewriter_model)
    BoxWithConstraints(
        modifier.fillMaxWidth().focusRequester(focus).focusable().onPreviewKeyEvent { event ->
            currentEnabled && event.type == KeyEventType.KeyDown && state.hardwareKey(event)
        }
    ) {
        val unit = TypewriterDimens.unit(maxWidth, maxHeight)
        SideEffect { state.unit = unit }
        val u = with(density) { unit.toPx() }
        val pitch = keyPitch(maxWidth, maxHeight)
        val keysHeight = pitch * KeyRowHeight * KeyRows + KeysPadding * 2
        val below = with(density) { keysHeight.toPx() }
        val atlas = rememberGlyphAtlas(unit)
        val measurer = rememberTextMeasurer()
        val badgeSize = with(density) { (unit * BadgeSize).toSp() }
        val badge =
            remember(measurer, badgeText, badgeSize) {
                measurer.measure(
                    badgeText,
                    TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = badgeSize,
                        letterSpacing = 0.28.em,
                    ),
                )
            }
        val model =
            remember(measurer, modelText, badgeSize) {
                measurer.measure(
                    modelText,
                    TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = badgeSize * ModelShare,
                        letterSpacing = 0.18.em,
                    ),
                )
            }
        // On a stage wider than a phone, the machine keeps to the middle; the body runs across.
        val left = ((constraints.maxWidth - TypewriterDimens.StageUnits * u) / 2f).coerceAtLeast(0f)
        Box(
            Modifier.fillMaxWidth()
                .layout { measurable, constraints ->
                    val foot = insets.getBottom(this)
                    val height =
                        ((TypewriterDimens.HintRoom + state.fed + TypewriterDimens.MachineHeight) *
                                u + below)
                            .roundToInt() + foot
                    val placeable =
                        measurable.measure(Constraints.fixed(constraints.maxWidth, height))
                    layout(constraints.maxWidth, height) { placeable.place(0, 0) }
                }
                .onGloballyPositioned { coordinates ->
                    val foot = insets.getBottom(density)
                    val top =
                        coordinates.size.height - foot - below - TypewriterDimens.MachineHeight * u
                    placed.machine = coordinates.positionInRoot() + Offset(left, top)
                    placed.unit = u
                }
        ) {
            // The machine; a tap on it gives it focus, for a hardware keyboard, and a drag up on
            // the sheet pulls it out. A screen reader hears what is on the sheet, and can send it.
            Spacer(
                Modifier.matchParentSize()
                    .drawBehind {
                        with(painter) {
                            drawMachine(
                                state,
                                atlas,
                                colors,
                                u,
                                Offset(
                                    left,
                                    size.height -
                                        insets.getBottom(this@drawBehind) -
                                        below -
                                        TypewriterDimens.MachineHeight * u,
                                ),
                                bottom = size.height,
                                badge = badge,
                                model = model,
                            )
                        }
                    }
                    .pointerInput(state) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            if (!currentEnabled) return@awaitEachGesture
                            val machineTop =
                                size.height -
                                    insets.getBottom(this@pointerInput) -
                                    below -
                                    TypewriterDimens.MachineHeight * u
                            val at = (down.position - Offset(left, machineTop)) / u
                            val onSheet =
                                at.y < TypewriterDimens.BodyTop &&
                                    at.y > state.sheetTop - SheetReach &&
                                    at.x > state.carriage - SheetReach &&
                                    at.x < state.carriage + TypewriterDimens.SheetWidth + SheetReach
                            val tracker = VelocityTracker()
                            tracker.addPosition(down.uptimeMillis, down.position)
                            var pulling = false
                            var travel = 0f
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                tracker.addPosition(change.uptimeMillis, change.position)
                                val moved = change.positionChange()
                                travel += abs(moved.y) + abs(moved.x)
                                if (!pulling && onSheet && travel > viewConfiguration.touchSlop) {
                                    pulling = state.grab()
                                }
                                if (pulling) {
                                    change.consume()
                                    state.drag(-moved.y / u)
                                }
                                if (!change.pressed) break
                            }
                            if (pulling) {
                                state.release(-tracker.calculateVelocity().y / u)
                            } else if (travel <= viewConfiguration.touchSlop) {
                                runCatching { focus.requestFocus() }
                            }
                        }
                    }
                    .semantics {
                        contentDescription = label
                        customActions =
                            listOf(CustomAccessibilityAction(sendLabel) { state.send() })
                    }
            )
            SheetText(state, Modifier.align(Alignment.TopCenter))
            Hint(
                state,
                Modifier.align(Alignment.TopCenter)
                    .offset(y = with(density) { (HintTop * u).toDp() }),
            )
            TypewriterKeys(
                state,
                pitch,
                enabled,
                Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = with(density) { insets.getBottom(this).toDp() })
                    .height(keysHeight)
                    .padding(vertical = KeysPadding),
            )
        }
    }
}

// What is on the sheet, for a screen reader: read with the machine, and live, so a key a screen
// reader presses is heard landing.
@Composable
private fun SheetText(state: TypewriterState, modifier: Modifier = Modifier) {
    val text = state.field.text.toString()
    Spacer(
        modifier.size(1.dp).semantics {
            stateDescription = text
            liveRegion = LiveRegionMode.Polite
        }
    )
}

// How far apart the keys are: ten across the width, a thumb's width at the most, and on a short
// stage, as a phone on its side, small enough to leave the thread some room.
private fun keyPitch(width: Dp, height: Dp): Dp {
    var pitch = minOf(MostKeyPitch, (width - KeysSide * 2) / KeysAcross)
    if (height != Dp.Infinity) pitch = minOf(pitch, height * ShortStageKeyShare)
    return pitch
}

// What the hint over the paper says: how to type, how to send, or that the sheet is full. Nothing
// while a sheet is on its way; faded out while the sheet is pulled.
@Composable
private fun Hint(state: TypewriterState, modifier: Modifier = Modifier) {
    val text =
        when {
            state.phase != SheetPhase.Idle -> null
            state.jammed -> stringResource(Res.string.typewriter_hint_full)
            state.inked -> stringResource(Res.string.typewriter_hint_pull)
            else -> stringResource(Res.string.typewriter_hint_type)
        } ?: return
    val colors = LabTheme.colors
    Text(
        text,
        style = LabTheme.typography.label,
        color = colors.textMuted,
        modifier =
            modifier
                .graphicsLayer {
                    state.frame
                    alpha = state.hintShown
                }
                .background(colors.surface, HintShape)
                .border(1.dp, colors.chipBorder, HintShape)
                .padding(horizontal = 11.dp, vertical = 4.dp),
    )
}

/** Where the machine lies in the root, its top left, and how big a unit is, in px. */
private class Placed {
    var machine = Offset.Zero
    var unit = 1f
}

/** What the machine does, heard through [sound] and felt through [haptics]. */
private class MachineFeel(
    private val sound: () -> SoundPlayer,
    private val haptics: HapticFeedback,
) : TypewriterFeel {
    override fun strike(variant: Int, space: Boolean) {
        val clip = Strikes[variant % Strikes.size]
        if (space) {
            sound().play(clip, volume = SpaceVolume, rate = SpaceRate)
        } else {
            sound().play(clip, volume = StrikeVolume, rate = 1f + (variant - 1) * StrikeRateStep)
        }
        haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
    }

    override fun bell() {
        sound().play(Sound.Bell, volume = BellVolume)
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    override fun carriageReturn() {
        sound().play(Sound.CarriageReturn, volume = ReturnVolume)
    }

    override fun bang(speed: Float) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    override fun tape() {
        sound().play(Sound.Tape, volume = TapeVolume)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    override fun ratchet(pulling: Boolean) {
        sound().play(Sound.RatchetTick, volume = if (pulling) PullTickVolume else FeedTickVolume)
        if (pulling) haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    override fun release() {
        sound().play(Sound.SheetRelease, volume = ReleaseVolume)
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    override fun jam() {
        sound().play(Sound.Strike2, volume = JamVolume, rate = JamRate)
        haptics.performHapticFeedback(HapticFeedbackType.Reject)
    }

    private companion object {
        val Strikes = listOf(Sound.Strike1, Sound.Strike2, Sound.Strike3)
        const val StrikeVolume = 0.85f
        const val StrikeRateStep = 0.04f
        const val SpaceVolume = 0.4f
        const val SpaceRate = 0.8f
        const val BellVolume = 0.7f
        const val ReturnVolume = 0.9f
        const val TapeVolume = 0.8f
        const val PullTickVolume = 0.6f
        const val FeedTickVolume = 0.25f
        const val ReleaseVolume = 0.9f
        const val JamVolume = 0.6f
        const val JamRate = 0.6f
    }
}

/** Every sound the typewriter plays, for its caller to load. */
internal val TypewriterSounds: Set<Sound> =
    setOf(
        Sound.Strike1,
        Sound.Strike2,
        Sound.Strike3,
        Sound.Bell,
        Sound.CarriageReturn,
        Sound.RatchetTick,
        Sound.SheetRelease,
        Sound.Tape,
    )

private val HintShape = RoundedCornerShape(50)

// The badge's lettering, in units, and the model line under it as a share of that.
private const val BadgeSize = 10f
private const val ModelShare = 0.8f

// The hint's top, in units down from the composer's top.
private const val HintTop = 6f

// A finger this many units off the sheet still takes it.
private const val SheetReach = 14f

// The keys: at most this far apart, ten across with this much room at either side, in four rows
// each this many pitches tall, with this much room over and under them. On a short stage a pitch
// is at most this share of its height.
private val MostKeyPitch = 48.dp
private val KeysSide = 6.dp
private const val KeysAcross = 10
private const val KeyRows = 4
internal const val KeyRowHeight = 1.12f
private val KeysPadding = 6.dp
private const val ShortStageKeyShare = 0.075f

// A key from a hardware keyboard: Ctrl or ⌘ and Return send, Return is a new line, Backspace runs
// the tape, and a character types if the machine has a key for it, dipping that key. Says whether
// the key was the machine's.
private fun TypewriterState.hardwareKey(event: KeyEvent): Boolean {
    val command = event.isCtrlPressed || event.isMetaPressed
    when (event.key) {
        Key.Enter,
        Key.NumPadEnter -> {
            if (command) {
                send()
            } else {
                keyboard.dipped(TypeKey(KeyAction.Return).id)
                type('\n')
            }
            return true
        }
        Key.Backspace -> {
            keyboard.dipped(TypeKey(KeyAction.Backspace).id)
            backspace()
            return true
        }
    }
    if (command || event.isAltPressed) return false
    val code = event.utf16CodePoint
    if (code <= 0 || code > Char.MAX_VALUE.code) return false
    val char = code.toChar()
    if (char.isISOControl()) return false
    keyIdFor(char)?.let(keyboard::dipped)
    type(char)
    return true
}
