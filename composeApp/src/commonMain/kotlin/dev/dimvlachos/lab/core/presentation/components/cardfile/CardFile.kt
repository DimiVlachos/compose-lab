package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.dimvlachos.lab.core.platform.SilentSoundPlayer
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.archivo_extrabold
import dev.dimvlachos.lab.resources.cardfile_next
import dev.dimvlachos.lab.resources.cardfile_previous
import dev.dimvlachos.lab.resources.cardfile_pull_out
import dev.dimvlachos.lab.resources.caveat_semibold
import dev.dimvlachos.lab.resources.courier_prime_bold
import dev.dimvlachos.lab.resources.courier_prime_regular
import kotlin.math.abs
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.stringResource

/** What a finger on the card file is doing, decided where it lands. */
private enum class Grab {
    Wheel,
    Knob,
    Push,
    Outside,
}

/** The cards as the scene paints them: each divider numbered along, each entry as [paper] says. */
internal fun paperCards(
    cards: List<FileCard>,
    paper: (FileCard.Entry) -> PaperCard.Contact,
): List<PaperCard> {
    var ordinal = 0
    return cards.map {
        when (it) {
            is FileCard.Divider -> PaperCard.Divider(it.letter, ordinal++)
            is FileCard.Entry -> paper(it)
        }
    }
}

/**
 * A mid-century rotary card file holding [cards], on a walnut desk under a lamp, painted as its
 * prototype paints it. Drag the cards or a knob to spin the wheel, tap a card behind or a divider's
 * tab to spin to it, tap the front card to pull it out and push it down, tap outside, press back or
 * Escape to file it. [paper] says what is typed and written on an entry's card; [describe] says
 * what a card is, for the wheel's description.
 *
 * The wheel is one focusable node: the cards and tabs take taps but expose nothing of their own to
 * accessibility or focus.
 */
@Composable
internal fun CardFile(
    cards: List<FileCard>,
    paper: (FileCard.Entry) -> PaperCard.Contact,
    describe: (FileCard) -> String,
    modifier: Modifier = Modifier,
    state: CardFileState = rememberCardFileState(),
    sounds: SoundPlayer = SilentSoundPlayer,
) {
    ReportCardFileComposition()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val wheelFocus = remember { FocusRequester() }
    val pulledFocus = remember { FocusRequester() }
    // The deck size is known now, so the first frame shows the starting card, not card 0. Done
    // before `front` is read below; a shrink still goes through updateCount.
    remember(state, cards.size) { state.growCount(cards.size) }
    // The wheel's position moves every frame of a spin; the front card changes only as a card
    // crosses the top, so composition reads this and never the position.
    val stateFront by remember(state) { derivedStateOf { state.front } }
    // The state learns of a shrunk deck a frame late, in updateCount; until then its front may lie
    // past the deck's end, so composition clamps it to the cards it has.
    val front = stateFront.coerceAtMost(cards.lastIndex).coerceAtLeast(0)

    LaunchedEffect(state, cards.size) { state.updateCount(cards.size) }
    DisposableEffect(state, cards, sounds, haptics) {
        state.canPull = { cards.getOrNull(it) is FileCard.Entry }
        state.onTick = {
            sounds.play(Sound.CardTick, 0.7f)
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
        onDispose {
            state.canPull = { true }
            state.onTick = null
        }
    }

    // The paper, type and ink the cards are painted with. Resource fonts arrive a frame or two
    // late; the families change when they do, and the cards are baked again.
    val measurer = rememberTextMeasurer()
    val typedRegular = Font(Res.font.courier_prime_regular, FontWeight.Normal)
    val typedBold = Font(Res.font.courier_prime_bold, FontWeight.Bold)
    val handFont = Font(Res.font.caveat_semibold, FontWeight.SemiBold)
    val sansFont = Font(Res.font.archivo_extrabold, FontWeight.ExtraBold)
    val grain = remember { makeGrain() }
    val ink =
        remember(measurer, typedRegular, typedBold, handFont, sansFont, grain) {
            CardInk(
                measurer,
                mapOf(
                    "TYPED" to FontFamily(typedRegular, typedBold),
                    "HAND" to FontFamily(handFont),
                    "SANS" to FontFamily(sansFont),
                ),
                grain,
            )
        }
    val latestPaper by rememberUpdatedState(paper)
    val scene = remember(cards) { CardFileScene(paperCards(cards) { latestPaper(it) }) }

    fun firstContactFrom(i: Int): Int =
        (i until cards.size).firstOrNull { cards[it] is FileCard.Entry } ?: i

    fun pullOrOpen() = scope.launch {
        when (cards.getOrNull(state.front)) {
            is FileCard.Divider -> state.spinTo(firstContactFrom(state.front))
            is FileCard.Entry -> if (state.pull()) pulledFocus.requestFocus()
            null -> Unit
        }
    }

    fun file(velocity: Float = 0f) = scope.launch {
        state.file(velocity)
        wheelFocus.requestFocus()
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.pulled != null,
        onBackCompleted = { file() },
    )

    val description = cards.getOrNull(front)?.let(describe).orEmpty()
    val previousLabel = stringResource(Res.string.cardfile_previous)
    val nextLabel = stringResource(Res.string.cardfile_next)
    val pullOutLabel = stringResource(Res.string.cardfile_pull_out)
    val pulledIndex = state.pulled
    // While a card is out the wheel can't turn, so it offers nothing.
    val actions =
        if (cards.isEmpty() || pulledIndex != null) emptyList()
        else
            buildList {
                if (front > 0) {
                    add(
                        CustomAccessibilityAction(previousLabel) {
                            scope.launch { state.step(-1) }
                            true
                        }
                    )
                }
                if (front < cards.lastIndex) {
                    add(
                        CustomAccessibilityAction(nextLabel) {
                            scope.launch { state.step(1) }
                            true
                        }
                    )
                }
                if (cards.getOrNull(front) is FileCard.Entry) {
                    add(
                        CustomAccessibilityAction(pullOutLabel) {
                            pullOrOpen()
                            true
                        }
                    )
                }
            }

    // A tap lands where the last frame drew the cards. These are read by a gesture detector that
    // outlives recompositions, so they are kept up to date.
    val tap by rememberUpdatedState { hit: Int, caught: Boolean ->
        val onFront = hit == state.front && abs(state.position - hit) < CardFileDimens.TapOnCard
        when {
            // A tap that caught a turning wheel only settles it, on the card under the finger.
            hit >= 0 && onFront && !caught -> pullOrOpen()
            hit >= 0 ->
                scope.launch {
                    state.spinTo(
                        if (cards.getOrNull(hit) is FileCard.Divider) firstContactFrom(hit) else hit
                    )
                }
            else -> scope.launch { state.release(0f) }
        }
    }
    val fileOnTap by rememberUpdatedState { file() }

    BoxWithConstraints(modifier) {
        val density = LocalDensity.current.density
        // Laid out in composition too, so the pulled card's focus target sits where it is drawn.
        scene.layout(constraints.maxWidth / density, constraints.maxHeight / density, density)

        // The wheel: one focusable node for the whole file, painted as the prototype paints it.
        Box(
            Modifier.fillMaxSize()
                .testTag("cardfile")
                .focusRequester(wheelFocus)
                .semantics {
                    contentDescription = description
                    customActions = actions
                }
                .onKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (e.key) {
                        Key.DirectionDown -> {
                            scope.launch { state.step(1) }
                            true
                        }
                        Key.DirectionUp -> {
                            scope.launch { state.step(-1) }
                            true
                        }
                        Key.Enter,
                        Key.NumPadEnter -> {
                            pullOrOpen()
                            true
                        }
                        // Focus moves to the pulled card once it is out; an Escape while it rises
                        // lands here.
                        Key.Escape ->
                            if (state.pulled != null) {
                                file()
                                true
                            } else false
                        else -> false
                    }
                }
                .focusable()
                .pointerInput(state, scene) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val d = this@pointerInput.density
                        val x0 = down.position.x / d
                        val y0 = down.position.y / d
                        state.pushTravel = scene.h * CardFileDimens.PushTravel
                        val grab =
                            when {
                                state.pulled != null ->
                                    when {
                                        state.pullProgress <= CardFileDimens.Grabbable -> null
                                        scene.inPulled(x0, y0) -> Grab.Push
                                        else -> Grab.Outside
                                    }
                                scene.knobAt(x0, y0) -> Grab.Knob
                                scene.onCards(x0, y0) -> Grab.Wheel
                                else -> null
                            } ?: return@awaitEachGesture
                        // A finger landing on a turning wheel stops it where it is.
                        val caught = state.isSpinning
                        val turning = grab == Grab.Wheel || grab == Grab.Knob
                        if (turning) scope.launch { state.beginDrag() }
                        val per =
                            scene.pitch * (if (grab == Grab.Knob) CardFileDimens.KnobRatio else 1f)
                        val tracker = VelocityTracker()
                        tracker.addPosition(down.uptimeMillis, down.position)
                        down.consume()
                        var lastY = down.position.y
                        var moved = false
                        var upAt = down.uptimeMillis
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            tracker.addPosition(change.uptimeMillis, change.position)
                            upAt = change.uptimeMillis
                            val travelled = (change.position - down.position).getDistance() / d
                            if (travelled > CardFileDimens.TapSlop) moved = true
                            if (!change.pressed) break
                            val dy = (change.position.y - lastY) / d
                            lastY = change.position.y
                            change.consume()
                            when (grab) {
                                Grab.Wheel,
                                Grab.Knob -> scope.launch { state.dragBy(dy / per) }
                                Grab.Push -> {
                                    val pushed = (change.position.y - down.position.y) / d
                                    scope.launch { state.pushTo(pushed) }
                                }
                                Grab.Outside -> Unit
                            }
                        }
                        val vy = tracker.calculateVelocity().y / d
                        val isTap = !moved && upAt - down.uptimeMillis < CardFileDimens.TapMs
                        when (grab) {
                            Grab.Wheel,
                            Grab.Knob ->
                                if (isTap) {
                                    tap(if (grab == Grab.Knob) -1 else scene.cardAt(x0, y0), caught)
                                } else {
                                    scope.launch { state.release(vy / per) }
                                }
                            Grab.Push -> scope.launch { state.releasePush(vy) }
                            // A tap outside the pulled card files it too.
                            Grab.Outside -> if (!moved) fileOnTap()
                        }
                    }
                }
                .drawBehind {
                    // Read here, so the cards are painted again when the fonts arrive.
                    if (scene.ink !== ink) scene.ink = ink
                    val d = density
                    scene.layout(size.width / d, size.height / d, d)
                    val position = state.position
                    val out = state.pulled ?: -1
                    val q = state.pullProgress
                    drawIntoCanvas { canvas ->
                        val g = Canvas2D(canvas, size.width.toInt(), size.height.toInt())
                        g.setTransform(d, 0f, 0f, d, 0f, 0f)
                        scene.paint(g, position, out, q, state.reducedMotion)
                    }
                }
        )

        // The pulled-out card is painted with the rest; this is where focus and its description
        // rest while it is out, and where Escape files it.
        val out = state.pulled
        if (out != null) {
            val entry = cards.getOrNull(out) as? FileCard.Entry
            if (entry == null) {
                // The deck changed under the pulled card: there is nothing left to show, so file
                // it.
                LaunchedEffect(out, cards) {
                    state.file()
                    wheelFocus.requestFocus()
                }
            } else {
                val t = scene.target
                Box(
                    Modifier.offset {
                            IntOffset(
                                ((t.cx - t.w / 2) * density).toInt(),
                                ((t.cy - t.h / 2) * density).toInt(),
                            )
                        }
                        .size(t.w.dp, t.h.dp)
                        .testTag("pulled")
                        .focusRequester(pulledFocus)
                        .semantics { contentDescription = describe(entry) }
                        .onKeyEvent { e ->
                            if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) {
                                file()
                                true
                            } else false
                        }
                        .focusable()
                )
            }
        }
    }
}
