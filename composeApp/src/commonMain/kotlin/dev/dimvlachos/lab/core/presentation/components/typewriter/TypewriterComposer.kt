package dev.dimvlachos.lab.core.presentation.components.typewriter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
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
 * A chat composer that is a typewriter. What is typed goes into a real text field under the paper,
 * through the keyboard, and every letter strikes the sheet: a type bar flies up from the basket,
 * the ribbon lifts, and the carriage steps on. The bell rings five letters before the margin, and
 * at the margin, or at a space once the bell has rung, the lever swings and the carriage returns by
 * itself; Return starts a new line too. Deleting runs the carriage back and the correction tape
 * lifts the letter off.
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
 * standing up over it as lines feed; it is as tall as it needs to be, plus [insets]' foot, which
 * its body runs down behind, as behind a keyboard. While not [enabled], as while a script plays, it
 * takes neither keys nor touches. Nothing recomposes as the machine moves: only a change of what
 * the hint says.
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
    // The machine follows the field: what is typed strikes, what is deleted is lifted off.
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
    val keyboard = LocalSoftwareKeyboardController.current
    val currentEnabled by rememberUpdatedState(enabled)
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
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val unit = TypewriterDimens.unit(maxWidth, maxHeight)
        SideEffect { state.unit = unit }
        val u = with(density) { unit.toPx() }
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
                                u)
                            .roundToInt() + foot
                    val placeable =
                        measurable.measure(Constraints.fixed(constraints.maxWidth, height))
                    layout(constraints.maxWidth, height) { placeable.place(0, 0) }
                }
                .onGloballyPositioned { coordinates ->
                    val foot = insets.getBottom(density)
                    val top = coordinates.size.height - foot - TypewriterDimens.MachineHeight * u
                    placed.machine = coordinates.positionInRoot() + Offset(left, top)
                    placed.unit = u
                }
        ) {
            // The real field: under the paper, unseen, holding what is typed for the keyboard and a
            // screen reader, which can send it with a custom action.
            BasicTextField(
                state = state.field,
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .padding(bottom = with(density) { insets.getBottom(this).toDp() })
                        .fillMaxWidth()
                        .height(with(density) { (TypewriterDimens.MachineHeight * u).toDp() })
                        .focusRequester(focus)
                        .onPreviewKeyEvent { event ->
                            val send =
                                event.key == Key.Enter &&
                                    (event.isCtrlPressed || event.isMetaPressed)
                            if (send && event.type == KeyEventType.KeyDown && currentEnabled) {
                                state.send()
                            }
                            send
                        }
                        .semantics {
                            contentDescription = label
                            customActions =
                                listOf(CustomAccessibilityAction(sendLabel) { state.send() })
                        },
                readOnly = !enabled,
                inputTransformation =
                    remember(state) {
                        InputTransformation {
                            // Not while the sheet is coming out; and never off the sheet's foot.
                            if (state.phase == SheetPhase.Zipping) {
                                revertAllChanges()
                            } else if (layOutSheet(asCharSequence()).overflow) {
                                revertAllChanges()
                                state.jam()
                            }
                        }
                    },
                textStyle = TextStyle(color = Color.Transparent),
                cursorBrush = SolidColor(Color.Transparent),
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrectEnabled = false,
                    ),
                lineLimits = TextFieldLineLimits.MultiLine(),
            )
            // The machine, drawn over the field; a tap on it brings the keyboard up, and a drag up
            // on the sheet pulls it out.
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
                                focus.requestFocus()
                                keyboard?.show()
                            }
                        }
                    }
            )
            Hint(
                state,
                Modifier.align(Alignment.TopCenter)
                    .offset(y = with(density) { (HintTop * u).toDp() }),
            )
        }
    }
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
