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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.dimvlachos.lab.core.platform.SilentSoundPlayer
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.platform.keepSystemGesturesOff
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.typewriter_hint_full
import dev.dimvlachos.lab.resources.typewriter_hint_pull
import dev.dimvlachos.lab.resources.typewriter_hint_type
import dev.dimvlachos.lab.resources.typewriter_key_new_line
import dev.dimvlachos.lab.resources.typewriter_key_space
import dev.dimvlachos.lab.resources.typewriter_label
import dev.dimvlachos.lab.resources.typewriter_lever
import dev.dimvlachos.lab.resources.typewriter_said_deleted
import dev.dimvlachos.lab.resources.typewriter_send
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * A chat composer that is a typewriter, typed on its own keys: round, chrome-rimmed keys with glass
 * tops, in a QWERTY layout, with Shift, Delete, New line, a space bar and a page of figures and
 * stops. The phone's keyboard never opens. Every key dips as it is pressed and strikes the sheet: a
 * type bar flies up from the basket, the ribbon lifts, and the carriage steps on. The bell rings
 * five letters before the margin, and at the margin, or at a space once the bell has rung, the
 * lever swings and the carriage returns by itself; New line starts a new line too. Delete runs the
 * carriage back and the correction tape lifts the letter off; held, it goes on, slowly. A hardware
 * keyboard types too, once the machine has focus, which it takes as it opens and on a tap on it; a
 * character the machine has no key for types nothing.
 *
 * To send, pull the sheet up out of the roller: it gives like a rubber band and the ratchet clicks
 * as it comes; let go short of the threshold and it slides back in, past it or flung, and it zips
 * out and [onSend] hears it, with where the whole sheet was in the root as it came free, for a
 * caller to fly it into its thread. Ctrl or ⌘ and Return, or a screen reader's Send action, pull it
 * out by themselves. A fresh sheet then feeds in as the carriage goes home. A blank sheet is never
 * sent.
 *
 * A tick of haptics goes with each strike, a firmer one with the bell and the carriage's bang, and
 * the ratchet ticks under the finger, none while the screen is in the background. The machine sits
 * at the foot of the space it is given, the paper standing up over it as lines feed, its keys in
 * its body; its body runs on down behind [insets]' foot. [badge] and [model] are lettered on its
 * body. [atlas] is its type, which a thread showing its slips can share. While not [enabled], as
 * while a script plays, it takes neither keys nor touches.
 *
 * A screen reader finds the machine as "Typewriter", with a Send action, and each key as a button
 * named for what it types ("q", or "Q" after Shift, "Space", "Delete", "New line"); it hears what
 * was typed or deleted once the typing pauses. Nothing recomposes as the machine moves; a change of
 * what the hint says, of the keys' page or of Shift recomposes only those.
 */
@Composable
public fun TypewriterComposer(
    state: TypewriterState,
    onSend: (SentSheet) -> Unit,
    modifier: Modifier = Modifier,
    badge: String = "",
    model: String = "",
    enabled: Boolean = true,
    insets: WindowInsets = WindowInsets(0),
    atlas: GlyphAtlas = rememberGlyphAtlas(state.unit),
) {
    TypewriterComposer(
        state,
        onSend,
        SilentSoundPlayer,
        modifier,
        badge,
        model,
        enabled,
        insets,
        atlas,
    )
}

/**
 * The [TypewriterComposer], heard through [sound]: the lab's own hook for its synthesized clips,
 * three strikes, the bell, the carriage return, the tape, the ratchet and the sheet coming free.
 * Nothing plays while the phone is on silent, nor while the screen is in the background.
 */
@Composable
internal fun TypewriterComposer(
    state: TypewriterState,
    onSend: (SentSheet) -> Unit,
    sound: SoundPlayer,
    modifier: Modifier = Modifier,
    badge: String = "",
    model: String = "",
    enabled: Boolean = true,
    insets: WindowInsets = WindowInsets(0),
    atlas: GlyphAtlas = rememberGlyphAtlas(state.unit),
) {
    ReportComposition()
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentSound by rememberUpdatedState(sound)
    DisposableEffect(state, haptics, lifecycle) {
        val feel =
            MachineFeel(
                sound = { currentSound },
                haptics = haptics,
                // Heard and felt only while the screen is in front: a script typing on behind it
                // stays silent.
                active = { lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) },
            )
        state.feel = feel
        onDispose {
            if (state.feel === feel) state.feel = object : TypewriterFeel {}
            // A machine taken away under a finger lets go of what the finger held.
            state.letGo()
        }
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
    val shell = remember { MachineShell() }
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
    val leverLabel = stringResource(Res.string.typewriter_lever)
    val newLineLabel = stringResource(Res.string.typewriter_key_new_line)
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(
        modifier.fillMaxWidth().focusRequester(focus).focusable().onPreviewKeyEvent { event ->
            currentEnabled && event.type == KeyEventType.KeyDown && state.hardwareKey(event)
        }
    ) {
        val foot = with(density) { insets.getBottom(this).toDp() }
        val hint = hintHeight(measurer, maxWidth)
        // The machine, the hint over it and the keys fit the stage, a phone on its side too.
        val fit =
            remember(maxWidth, maxHeight, foot, hint) {
                TypewriterDimens.fit(maxWidth, maxHeight - foot, hint, foot)
            }
        val unit = fit.unit
        SideEffect { state.unit = unit }
        val u = with(density) { unit.toPx() }
        val pitch = fit.pitch
        val keysHeight = fit.keysHeight
        val lip = fit.lip
        val below = with(density) { (keysHeight + lip).toPx() }
        val hintRoom = with(density) { fit.hintRoom.toPx() }
        val badgeText =
            badge.takeIf { it.isNotEmpty() }?.let { lettering(measurer, it, unit, true) }
        val modelText =
            model.takeIf { it.isNotEmpty() }?.let { lettering(measurer, it, unit, false) }
        // On a stage wider than a phone, the machine keeps to the middle; the body runs across.
        val left = ((constraints.maxWidth - TypewriterDimens.StageUnits * u) / 2f).coerceAtLeast(0f)
        Box(
            Modifier.fillMaxWidth()
                .layout { measurable, constraints ->
                    val foot = insets.getBottom(this)
                    // The paper stands up over the machine as lines feed; on a stage too short
                    // for it, it rises out of sight under the composer's top edge instead.
                    val wanted =
                        (hintRoom + (state.fed + TypewriterDimens.MachineHeight) * u + below)
                            .roundToInt() + foot
                    val height = wanted.coerceAtMost(constraints.maxHeight)
                    placed.clipped = height < wanted
                    placed.machineTop = height - foot - below - TypewriterDimens.MachineHeight * u
                    val placeable =
                        measurable.measure(Constraints.fixed(constraints.maxWidth, height))
                    layout(constraints.maxWidth, height) { placeable.place(0, 0) }
                }
                .drawWithContent {
                    if (placed.clipped) clipRect { this@drawWithContent.drawContent() }
                    else drawContent()
                }
                .onGloballyPositioned { coordinates ->
                    val foot = insets.getBottom(density)
                    val top =
                        coordinates.size.height - foot - below - TypewriterDimens.MachineHeight * u
                    placed.machine = coordinates.positionInRoot() + Offset(left, top)
                    placed.unit = u
                }
        ) {
            // The parts that never move, drawn once in a layer of their own: the shell, the keys'
            // well, the basket's opening and the bell.
            val machineHeight = with(density) { (TypewriterDimens.MachineHeight * u).toDp() }
            Spacer(
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(machineHeight + keysHeight + lip + foot)
                    .graphicsLayer {
                        state.frame
                        translationY = state.shudder * TypewriterDimens.Shudder.toPx()
                    }
                    .drawWithCache {
                        val keysTop = TypewriterDimens.MachineHeight * u
                        val padding = TypewriterDimens.KeysPadding.toPx()
                        val keysWidth = pitch.toPx() * TypewriterDimens.KeysAcross
                        onDrawBehind {
                            with(shell) {
                                drawShell(
                                    colors,
                                    u,
                                    Offset(left, 0f),
                                    keysTop + padding,
                                    keysTop + keysHeight.toPx() - padding,
                                    keysWidth,
                                    TypewriterDimens.KeyRows,
                                    badgeText,
                                    modelText,
                                )
                            }
                        }
                    }
            )
            // The moving parts; a tap on them gives the machine focus, for a hardware keyboard,
            // and a drag up on the sheet pulls it out. A screen reader finds it with a Send action.
            Spacer(
                Modifier.matchParentSize()
                    .graphicsLayer {
                        state.frame
                        translationY = state.shudder * TypewriterDimens.Shudder.toPx()
                    }
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
                            )
                        }
                    }
                    // Keyed by the machine's size and place, so a turned or resized stage pulls
                    // the sheet where it now is.
                    .pointerInput(state, u, left, below) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            if (!currentEnabled) return@awaitEachGesture
                            val machineTop =
                                size.height -
                                    insets.getBottom(this@pointerInput) -
                                    below -
                                    TypewriterDimens.MachineHeight * u
                            val at = (down.position - Offset(left, machineTop)) / u
                            // The lever first: a finger on it, or a thumb's width from it all
                            // along its arm, pushes it rather than pulling the sheet.
                            val reach = TypewriterDimens.LeverReach.toPx() / u
                            if (nearSegment(at, state.leverPivot, state.leverKnob, reach)) {
                                if (state.grabLever()) pushTheLever(state, down, u)
                                return@awaitEachGesture
                            }
                            val onSheet =
                                at.y < TypewriterDimens.BodyTop &&
                                    at.y > state.sheetTop - SheetReach &&
                                    at.x > state.carriage - SheetReach &&
                                    at.x < state.carriage + TypewriterDimens.SheetWidth + SheetReach
                            val tracker = VelocityTracker()
                            tracker.addPosition(down.uptimeMillis, down.position)
                            var pulling = false
                            var travel = 0f
                            // Let go however the gesture ends: lifted, or cut off as the stage
                            // turns or the machine leaves, a sheet held would stay held for good.
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change =
                                        event.changes.firstOrNull { it.id == down.id } ?: break
                                    tracker.addPosition(change.uptimeMillis, change.position)
                                    val moved = change.positionChange()
                                    travel += abs(moved.y) + abs(moved.x)
                                    if (
                                        !pulling && onSheet && travel > viewConfiguration.touchSlop
                                    ) {
                                        pulling = state.grab()
                                    }
                                    if (pulling) {
                                        change.consume()
                                        state.drag(-moved.y / u)
                                    }
                                    if (!change.pressed) break
                                }
                            } finally {
                                if (pulling) state.release(-tracker.calculateVelocity().y / u)
                            }
                            if (!pulling && travel <= viewConfiguration.touchSlop) {
                                runCatching { focus.requestFocus() }
                            }
                        }
                    }
                    .semantics {
                        contentDescription = label
                        // No Send while the machine is not to be touched, as while a script plays.
                        if (enabled) {
                            customActions =
                                listOf(CustomAccessibilityAction(sendLabel) { state.send() })
                        }
                    }
            )
            // The band the lever swings in, kept from the system's edge swipes: at the end of a
            // long
            // line the lever is at the screen's left edge, where a push would otherwise go back.
            Spacer(
                Modifier.align(Alignment.TopStart)
                    .fillMaxWidth()
                    .offset {
                        val top =
                            placed.machineTop +
                                (TypewriterDimens.LeverPivotY - TypewriterDimens.LeverLength) * u
                        IntOffset(0, (top - TypewriterDimens.LeverReach.toPx()).roundToInt())
                    }
                    .height(
                        with(density) { (TypewriterDimens.LeverLength * u).toDp() } +
                            TypewriterDimens.LeverReach * 2
                    )
                    .keepSystemGesturesOff()
            )
            // The lever, for a screen reader: where it stands with the carriage home, so its focus
            // doesn't chase the carriage, with a New line action.
            Spacer(
                Modifier.align(Alignment.TopStart)
                    .offset {
                        val reach = TypewriterDimens.LeverReach.toPx()
                        val x = left + (TypewriterDimens.HomeX - TypewriterDimens.LeverOffset) * u
                        val y =
                            placed.machineTop +
                                (TypewriterDimens.LeverPivotY - TypewriterDimens.LeverLength / 2f) *
                                    u
                        IntOffset((x - reach).roundToInt(), (y - reach).roundToInt())
                    }
                    .size(TypewriterDimens.LeverReach * 2)
                    .semantics {
                        contentDescription = leverLabel
                        if (enabled) {
                            customActions =
                                listOf(CustomAccessibilityAction(newLineLabel) { state.newLine() })
                        }
                    }
            )
            Said(state, Modifier.align(Alignment.TopCenter))
            // Left out on a stage too short for it, where it would hide the line being typed.
            if (fit.hinted)
                Hint(
                    state,
                    Modifier.align(Alignment.TopCenter).offset(y = unit * TypewriterDimens.HintTop),
                )
            TypewriterKeys(
                state,
                pitch,
                fit.rowHeight,
                enabled,
                measurer,
                Modifier.align(Alignment.BottomCenter)
                    .graphicsLayer {
                        state.frame
                        translationY = state.shudder * TypewriterDimens.Shudder.toPx()
                    }
                    .padding(bottom = foot + lip)
                    .height(keysHeight)
                    .padding(vertical = TypewriterDimens.KeysPadding),
            )
        }
    }
}

// The badge or the model line under it, set for the body.
@Composable
private fun lettering(
    measurer: TextMeasurer,
    text: String,
    unit: Dp,
    name: Boolean,
) =
    with(LocalDensity.current) {
        val size = (unit * (if (name) BadgeSize else BadgeSize * ModelShare)).toSp()
        remember(measurer, text, size, name) {
            measurer.measure(
                text,
                TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (name) FontWeight.Bold else FontWeight.Normal,
                    fontSize = size,
                    letterSpacing = if (name) BadgeSpacing.em else ModelSpacing.em,
                ),
            )
        }
    }

// What a screen reader hears of the typing: once it pauses, what was typed, or what was deleted,
// or that the sheet is full; never the whole sheet again at every key.
@Composable
private fun Said(state: TypewriterState, modifier: Modifier = Modifier) {
    var said by remember { mutableStateOf("") }
    val space = stringResource(Res.string.typewriter_key_space)
    val newLine = stringResource(Res.string.typewriter_key_new_line)
    val full = stringResource(Res.string.typewriter_hint_full)
    LaunchedEffect(state) {
        snapshotFlow { state.speech }
            .collectLatest { speech ->
                if (speech.quiet) return@collectLatest
                delay(TypewriterDimens.SpeechPauseMs)
                fun spoken(text: String) =
                    when (text) {
                        " " -> space
                        "\n" -> newLine
                        else -> text.replace("\n", " $newLine ")
                    }
                said =
                    listOfNotNull(
                            speech.deleted
                                .takeIf { it.isNotEmpty() }
                                ?.let {
                                    getString(Res.string.typewriter_said_deleted, spoken(it))
                                },
                            speech.typed.takeIf { it.isNotEmpty() }?.let(::spoken),
                            full.takeIf { speech.jam },
                        )
                        .joinToString(". ")
                state.speech = Speech.Quiet
            }
    }
    // Heard; then gone, so the node holds no stale words for a screen reader to come upon.
    LaunchedEffect(said) {
        if (said.isNotEmpty()) {
            delay(SaidForMs)
            said = ""
        }
    }
    Spacer(
        modifier.size(1.dp).semantics {
            if (said.isNotEmpty()) contentDescription = said
            liveRegion = LiveRegionMode.Polite
        }
    )
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
    // The arrow up is only drawn: a screen reader hears the words.
    val arrow = if (state.inked && !state.jammed) HintArrow else ""
    Text(
        text + arrow,
        style = hintStyle(),
        color = colors.textMuted,
        textAlign = TextAlign.Center,
        modifier =
            modifier
                .clearAndSetSemantics { contentDescription = text }
                .graphicsLayer {
                    state.frame
                    alpha = state.hintShown
                }
                .background(colors.surface, HintShape)
                .border(HintBorder, colors.chipBorder, HintShape)
                .padding(horizontal = HintPadding.x, vertical = HintPadding.y),
    )
}

// How tall the hint over the paper is at its tallest, whatever it says, on a stage [width] wide:
// it sets the room left for it, so a big font never puts it over the paper.
@Composable
private fun hintHeight(measurer: TextMeasurer, width: Dp): Dp {
    val texts =
        listOf(
            stringResource(Res.string.typewriter_hint_type),
            stringResource(Res.string.typewriter_hint_pull) + HintArrow,
            stringResource(Res.string.typewriter_hint_full),
        )
    val style = hintStyle()
    val density = LocalDensity.current
    return remember(measurer, texts, style, width, density) {
        with(density) {
            val room = (width - HintPadding.x * 2).roundToPx().coerceAtLeast(1)
            val tallest = texts.maxOf {
                measurer.measure(it, style, constraints = Constraints(maxWidth = room)).size.height
            }
            tallest.toDp() + HintPadding.y * 2 + HintBorder * 2
        }
    }
}

// The hint's type: the theme's label, grown with the font size no bigger than [HintMostText], so
// on a phone on its side a large font leaves the machine room.
@Composable
private fun hintStyle(): TextStyle {
    val label = LabTheme.typography.label
    val density = LocalDensity.current
    return remember(label, density) {
        with(density) {
            if (label.fontSize.toDp() <= HintMostText) {
                label
            } else {
                label.copy(
                    fontSize = HintMostText.toSp(),
                    lineHeight =
                        if (label.lineHeight.isSp) (HintMostText * HintLeading).toSp()
                        else label.lineHeight,
                )
            }
        }
    }
}

/**
 * Where the machine lies in the root, its top left, and how big a unit is, in px; and, as last laid
 * out, where its top is in the composer and whether the paper is cut off at the composer's top.
 */
private class Placed {
    var machine = Offset.Zero
    var unit = 1f
    var machineTop by mutableFloatStateOf(0f)
    var clipped = false
}

/** What the machine does, heard through [sound] and felt through [haptics], while [active]. */
private class MachineFeel(
    private val sound: () -> SoundPlayer,
    private val haptics: HapticFeedback,
    private val active: () -> Boolean,
) : TypewriterFeel {
    private fun play(clip: Sound, volume: Float, rate: Float = 1f) {
        if (active()) sound().play(clip, volume, rate)
    }

    private fun feel(type: HapticFeedbackType) {
        if (active()) haptics.performHapticFeedback(type)
    }

    override fun strike(variant: Int, space: Boolean) {
        val clip = Strikes[variant % Strikes.size]
        if (space) {
            play(clip, volume = SpaceVolume, rate = SpaceRate)
        } else {
            play(clip, volume = StrikeVolume, rate = 1f + (variant - 1) * StrikeRateStep)
        }
        feel(HapticFeedbackType.KeyboardTap)
    }

    override fun bell() {
        play(Sound.Bell, volume = BellVolume)
        feel(HapticFeedbackType.Confirm)
    }

    override fun carriageReturn() {
        play(Sound.CarriageReturn, volume = ReturnVolume)
    }

    override fun bang() {
        feel(HapticFeedbackType.LongPress)
    }

    override fun tape() {
        play(Sound.Tape, volume = TapeVolume)
        feel(HapticFeedbackType.TextHandleMove)
    }

    override fun ratchet(pulling: Boolean) {
        play(Sound.RatchetTick, volume = if (pulling) PullTickVolume else FeedTickVolume)
        if (pulling) feel(HapticFeedbackType.SegmentFrequentTick)
    }

    override fun release() {
        play(Sound.SheetRelease, volume = ReleaseVolume)
        feel(HapticFeedbackType.Confirm)
    }

    override fun jam() {
        play(Sound.Strike2, volume = JamVolume, rate = JamRate)
        feel(HapticFeedbackType.Reject)
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

// The hint's padding inside its border, across and down, and its border.
private val HintPadding = DpOffset(11.dp, 4.dp)
private val HintBorder = 1.dp

// The hint's letters are no bigger than this on the screen, its lines this far apart for their
// size.
private val HintMostText = 18.dp
private const val HintLeading = 1.3f

// Drawn after the hint to pull the sheet up.
private const val HintArrow = " ↑"

// The badge's lettering, in units, and the model line under it as a share of that; and how far
// apart their letters are set.
private const val BadgeSize = 10f
private const val ModelShare = 0.8f
private const val BadgeSpacing = 0.28f
private const val ModelSpacing = 0.18f

// What was last heard is kept on its node this long.
private const val SaidForMs = 3_000L

// Pushes the held lever with the finger that went [down]: the lever goes over as far as the
// finger moves right, and is let go, at the finger's speed, as it lifts.
private suspend fun AwaitPointerEventScope.pushTheLever(
    state: TypewriterState,
    down: PointerInputChange,
    u: Float,
) {
    down.consume()
    val tracker = VelocityTracker()
    tracker.addPosition(down.uptimeMillis, down.position)
    val travel = TypewriterDimens.LeverThrow * u
    try {
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            change.consume()
            tracker.addPosition(change.uptimeMillis, change.position)
            state.pushLever((change.position.x - down.position.x) / travel)
            if (!change.pressed) break
        }
    } finally {
        state.releaseLever(tracker.calculateVelocity().x / travel)
    }
}

// Whether [at] is within [reach] of the segment from [from] to [to].
internal fun nearSegment(at: Offset, from: Offset, to: Offset, reach: Float): Boolean {
    val along = to - from
    val length = along.getDistanceSquared()
    val t =
        if (length == 0f) 0f
        else (((at - from).x * along.x + (at - from).y * along.y) / length).coerceIn(0f, 1f)
    return (at - (from + along * t)).getDistance() <= reach
}

// A finger this many units off the sheet still takes it.
private const val SheetReach = 14f

/**
 * A key from a hardware keyboard: Ctrl or ⌘ and Return send, Return is a new line, Backspace runs
 * the tape, and a character types if the machine has a key for it, dipping that key; one it has no
 * key for types nothing. AltGr and Option characters count as characters. Says whether the key was
 * the machine's.
 */
internal fun TypewriterState.hardwareKey(event: KeyEvent): Boolean {
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
    if (command) return false
    val code = event.utf16CodePoint
    if (code <= 0 || code > Char.MAX_VALUE.code) return false
    val char = code.toChar()
    val id = keyIdFor(char) ?: return false
    keyboard.dipped(id)
    type(char)
    return true
}
