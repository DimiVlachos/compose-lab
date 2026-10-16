package dev.dimvlachos.lab.core.presentation.components.cardfile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.dimvlachos.lab.core.platform.SilentSoundPlayer
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.presentation.ui.CardFileColors
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.cardfile_next
import dev.dimvlachos.lab.resources.cardfile_previous
import dev.dimvlachos.lab.resources.cardfile_pull_out
import kotlin.math.abs
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * A chrome rotary card file holding [cards]. Drag the cards or a knob to spin the wheel, tap a
 * raised tab to spin to its letter, tap the front card to pull it out (drawn by [pulled]) and push
 * it down, tap outside, press back or Escape to file it. [face] draws an entry's face on the wheel;
 * [describe] says what a card is, for the wheel's description.
 *
 * The wheel is one focusable node: the cards and tabs take taps but expose nothing of their own to
 * accessibility or focus.
 */
@Composable
internal fun CardFile(
    cards: List<FileCard>,
    face: @Composable (FileCard.Entry) -> Unit,
    pulled: @Composable (FileCard.Entry) -> Unit,
    describe: (FileCard) -> String,
    modifier: Modifier = Modifier,
    state: CardFileState = rememberCardFileState(),
    sounds: SoundPlayer = SilentSoundPlayer,
) {
    ReportCardFileComposition()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val screenDensity = LocalDensity.current
    val cardPx = with(screenDensity) { CardFileDimens.CardDrag.toPx() }
    val wheelFocus = remember { FocusRequester() }
    val pulledFocus = remember { FocusRequester() }
    // The deck size is known now, so the first frame shows the starting card, not card 0. Done
    // before `front` is read below; a shrink still goes through updateCount.
    remember(state, cards.size) { state.growCount(cards.size) }
    // The wheel's position moves every frame of a spin; the front card changes only as a card
    // crosses the top, so composition reads this and never the position.
    val front by remember(state) { derivedStateOf { state.front } }

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

    fun pullOrOpen() = scope.launch {
        when (val card = cards.getOrNull(state.front)) {
            is FileCard.Divider -> {
                val first = cards.indexOfFirst { it is FileCard.Entry && it.letter == card.letter }
                if (first >= 0) state.spinTo(first)
            }
            is FileCard.Entry -> if (state.pull()) pulledFocus.requestFocus()
            null -> Unit
        }
    }

    fun file() = scope.launch {
        state.file()
        wheelFocus.requestFocus()
    }

    fun spinToLetter(letter: Char) {
        val first = cards.indexOfFirst { it is FileCard.Entry && it.letter == letter }
        if (first >= 0) scope.launch { state.spinTo(first) }
    }

    // The cards' and tabs' tap detectors outlive a recomposition, so they call the latest of these.
    val tapCard by rememberUpdatedState { i: Int ->
        // The front card pulls out; a card behind it spins to the front.
        if (i == state.front) pullOrOpen() else scope.launch { state.spinTo(i) }
    }
    val tapTab by rememberUpdatedState { letter: Char -> spinToLetter(letter) }

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
    val actions =
        if (cards.isEmpty()) emptyList()
        else
            buildList {
                if (front > 0) {
                    add(
                        CustomAccessibilityAction(previousLabel) {
                            scope.launch { state.spinTo(state.front - 1) }
                            true
                        }
                    )
                }
                if (front < cards.lastIndex) {
                    add(
                        CustomAccessibilityAction(nextLabel) {
                            scope.launch { state.spinTo(state.front + 1) }
                            true
                        }
                    )
                }
                if (cards[front] is FileCard.Entry && pulledIndex == null) {
                    add(
                        CustomAccessibilityAction(pullOutLabel) {
                            pullOrOpen()
                            true
                        }
                    )
                }
            }

    BoxWithConstraints(modifier.background(CardFileColors.night)) {
        val rod =
            with(screenDensity) {
                Offset(
                    constraints.maxWidth / 2f,
                    constraints.maxHeight * 0.52f +
                        CardFileDimens.CardHeight.toPx() / 2 +
                        CardFileDimens.RodDrop.toPx(),
                )
            }
        val halfWidth = with(screenDensity) { CardFileDimens.CardWidth.toPx() / 2 + 18.dp.toPx() }
        Spacer(Modifier.fillMaxSize().desk(rod))

        // The wheel: one focusable node for the whole file.
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
                            scope.launch { state.spinTo(state.front + 1) }
                            true
                        }
                        Key.DirectionUp -> {
                            scope.launch { state.spinTo(state.front - 1) }
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
                .pointerInput(state, cardPx) {
                    val tracker = VelocityTracker()
                    detectVerticalDragGestures(
                        onDragStart = {
                            tracker.resetTracking()
                            // A finger catches a spinning wheel where it is.
                            scope.launch { state.beginDrag() }
                        },
                        onDragEnd = {
                            val v = tracker.calculateVelocity().y / cardPx
                            scope.launch { state.release(v) }
                        },
                        onDragCancel = { scope.launch { state.release(0f) } },
                    ) { change, dy ->
                        change.consume()
                        tracker.addPosition(change.uptimeMillis, change.position)
                        scope.launch { state.dragBy(dy / cardPx) }
                    }
                }
        ) {
            // Only the window around the front is composed; reading `front` here recomposes this
            // block only when the window moves by a card. Each card is keyed, so its face and
            // anything it remembers stay with it as the window slides.
            val window = CardWheel.window(front.toFloat(), cards.size)
            for (i in window) {
                val card = cards[i]
                key(card.key) {
                    val index by rememberUpdatedState(i)
                    val depth = i - front
                    CardLayer(
                        i,
                        card,
                        state,
                        zIndex = -abs(depth).toFloat() + if (depth < 0) 0.5f else 0f,
                        modifier =
                            Modifier.clearAndSetSemantics { testTag = "card-${card.key}" }
                                .pointerInput(Unit) { detectTapGestures { tapCard(index) } },
                    ) {
                        if (card is FileCard.Entry) face(card)
                    }
                }
            }
            // The raised tabs of the next few dividers, peeking up behind the front card.
            val tabs = window.filter { it > front && cards[it] is FileCard.Divider }.take(3)
            tabs.forEachIndexed { n, i ->
                val letter = (cards[i] as FileCard.Divider).letter
                key(letter) {
                    Box(
                        Modifier.align(Alignment.Center)
                            .offset(
                                x = (-80 + n * 80).dp,
                                y = -(CardFileDimens.CardHeight / 2) - 18.dp,
                            )
                            // Over the cards behind, which they stand taller than; under the front.
                            .zIndex(-0.25f)
                            .size(44.dp, 26.dp)
                            .background(
                                CardFileColors.dividerPaper,
                                RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                            )
                            .clearAndSetSemantics { testTag = "tab-$letter" }
                            .pointerInput(letter) { detectTapGestures { tapTab(letter) } }
                    ) {
                        Text(
                            letter.toString(),
                            Modifier.align(Alignment.Center),
                            color = CardFileColors.dividerInk,
                        )
                    }
                }
            }
        }

        // The chrome frame; only its knurling redraws in a spin, reading the position as it draws.
        Spacer(Modifier.fillMaxSize().frame(rod, halfWidth) { state.position })

        // A knob turns the wheel at a quarter of a finger's speed on the cards, for fine control.
        // It is turned, not flicked: letting go settles on the nearest card, with no momentum.
        for ((tag, side) in listOf("knob-left" to -1, "knob-right" to 1)) {
            Box(
                Modifier.offset {
                        IntOffset(
                            (rod.x + side * (halfWidth + 18f) - 28.dp.toPx()).toInt(),
                            (rod.y - 28.dp.toPx()).toInt(),
                        )
                    }
                    .size(56.dp)
                    .testTag(tag)
                    .pointerInput(state, cardPx) {
                        detectVerticalDragGestures(
                            onDragStart = { scope.launch { state.beginDrag() } },
                            onDragEnd = { scope.launch { state.release(0f) } },
                            onDragCancel = { scope.launch { state.release(0f) } },
                        ) { change, dy ->
                            change.consume()
                            scope.launch { state.dragBy(dy / cardPx * CardFileDimens.KnobRatio) }
                        }
                    }
            )
        }

        // The pulled-out card, over a dimmed desk.
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
                Box(
                    Modifier.fillMaxSize()
                        .testTag("scrim")
                        .graphicsLayer { alpha = state.pullProgress }
                        .background(CardFileColors.scrim)
                        .clickable(remember { MutableInteractionSource() }, indication = null) {
                            file()
                        }
                )
                Box(
                    Modifier.align(Alignment.Center)
                        // At least a card's footprint, so there is a card to push down whatever is
                        // on it.
                        .defaultMinSize(CardFileDimens.CardWidth, CardFileDimens.CardHeight)
                        .testTag("pulled")
                        .focusRequester(pulledFocus)
                        .onKeyEvent { e ->
                            if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) {
                                file()
                                true
                            } else false
                        }
                        .graphicsLayer {
                            val p = state.pullProgress
                            translationY =
                                (1f - p) * 220f * this.density + state.pushDown * size.height
                            scaleX = 0.9f + 0.25f * p
                            scaleY = scaleX
                        }
                        .pointerInput(state) {
                            val tracker = VelocityTracker()
                            detectVerticalDragGestures(
                                onDragStart = { tracker.resetTracking() },
                                onDragEnd = {
                                    scope.launch {
                                        state.releasePush(
                                            tracker.calculateVelocity().y / size.height
                                        )
                                    }
                                },
                                onDragCancel = { scope.launch { state.releasePush(0f) } },
                            ) { change, dy ->
                                change.consume()
                                tracker.addPosition(change.uptimeMillis, change.position)
                                scope.launch { state.pushBy(dy / size.height) }
                            }
                        }
                        .focusable(),
                    contentAlignment = Alignment.Center,
                ) {
                    pulled(entry)
                }
            }
        }
    }
}
