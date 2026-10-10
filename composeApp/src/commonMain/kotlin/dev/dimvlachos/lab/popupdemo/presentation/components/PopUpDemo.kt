package dev.dimvlachos.lab.popupdemo.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.popup.PopUpBook
import dev.dimvlachos.lab.core.presentation.components.popup.PopUpBookState
import dev.dimvlachos.lab.core.presentation.components.popup.rememberPopUpBookState
import dev.dimvlachos.lab.core.presentation.ui.CycladesPaper as Paper
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.popupdemo.rememberCycladesBook
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.popup_again
import dev.dimvlachos.lab.resources.popup_back
import dev.dimvlachos.lab.resources.popup_brand
import dev.dimvlachos.lab.resources.popup_caption_0_body
import dev.dimvlachos.lab.resources.popup_caption_0_title
import dev.dimvlachos.lab.resources.popup_caption_1_body
import dev.dimvlachos.lab.resources.popup_caption_1_title
import dev.dimvlachos.lab.resources.popup_caption_2_body
import dev.dimvlachos.lab.resources.popup_caption_2_title
import dev.dimvlachos.lab.resources.popup_caption_3_body
import dev.dimvlachos.lab.resources.popup_caption_3_title
import dev.dimvlachos.lab.resources.popup_end_body
import dev.dimvlachos.lab.resources.popup_end_title
import dev.dimvlachos.lab.resources.popup_next
import dev.dimvlachos.lab.resources.popup_open
import dev.dimvlachos.lab.resources.popup_skip
import dev.dimvlachos.lab.resources.popup_start
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// The cover, then three spreads.
private const val Spreads = 3

// Lines kept for a caption's text: the longest wraps onto three on a phone.
private const val CaptionLines = 3

// The main button, for tests.
internal const val NextTag = "popup.next"

// How long the buttons' labels take to change.
private const val LabelMs = 360

// Between a script's taps when it turns several leaves.
private const val RiffleMs = 380L

private val captions: List<Pair<StringResource, StringResource>> =
    listOf(
        Res.string.popup_caption_0_title to Res.string.popup_caption_0_body,
        Res.string.popup_caption_1_title to Res.string.popup_caption_1_body,
        Res.string.popup_caption_2_title to Res.string.popup_caption_2_body,
        Res.string.popup_caption_3_title to Res.string.popup_caption_3_body,
    )

/**
 * A travel app's onboarding as a pop-up book: the Cyclades in three spreads, with a caption under
 * the book and Back and Next beneath it. The script's select(i) taps its way to spread i (0 is the
 * shut cover); its drags and tab pulls are played by a drawn fingertip through the book's calls.
 */
@Composable
internal fun PopUpDemo(state: DemoState) {
    val art = rememberCycladesBook()
    val book = rememberPopUpBookState(Spreads)
    val finger = remember { PopUpFinger() }
    val sky = rememberTourSky()
    val scope = rememberCoroutineScope()
    var finished by rememberSaveable { mutableStateOf(false) }
    // Where the book is headed while it turns several leaves at once: the caption shows that
    // spread at once instead of each one the leaves pass on the way.
    var heading by remember { mutableStateOf<Int?>(null) }
    suspend fun jumpTo(target: Int, tapWith: PopUpFinger?) {
        val goal = target.coerceIn(0, Spreads)
        if (abs(goal - book.destination) > 1) heading = goal
        try {
            turnTo(book, tapWith, goal)
            snapshotFlow { book.spread }.first { it == goal }
        } finally {
            heading = null
        }
    }
    val type = LabTheme.typography
    val spacing = LabTheme.spacing

    LaunchedEffect(book, state.selectedIndex) { jumpTo(state.selectedIndex, tapWith = finger) }
    DisposableEffect(state, book) {
        state.setPopUpPageHandler { from, to, duration -> finger.drag(book, from, to, duration) }
        state.setPopUpTabHandler { out, duration -> finger.pullTab(book, out, duration) }
        onDispose {
            state.setPopUpPageHandler(null)
            state.setPopUpTabHandler(null)
        }
    }

    val shown = heading ?: book.spread
    // The tour's end shows only while the book lies open at its last spread: once the book is
    // turned back, by a finger or by the script, the captions follow it again.
    val ended = finished && shown == Spreads
    // The book's name for a screen reader: the caption's title.
    val titleText = stringResource(if (ended) Res.string.popup_end_title else captions[shown].first)
    sky.showFor(if (ended) Paper.skies.lastIndex else shown)
    Column(
        Modifier.fillMaxSize()
            .then(with(sky) { Modifier.drawSky() })
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = spacing.mediumLarge, vertical = spacing.medium)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.popup_brand),
                style = type.subtitle,
                color = Paper.ink,
            )
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = {
                    finished = true
                    scope.launch { jumpTo(Spreads, tapWith = null) }
                }
            ) {
                Text(stringResource(Res.string.popup_skip), color = Paper.inkMuted)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            PopUpBook(
                cover = art.cover,
                spreads = art.spreads,
                state = book,
                description = titleText,
                modifier = with(finger) { Modifier.drawFinger(Paper.ink) },
            )
        }
        Dots(selected = shown - 1, Paper.button, Paper.inkFaint)
        Spacer(Modifier.height(spacing.medium))
        // The caption drifts softly from one to the next as the book moves on.
        DriftingCaption(index = if (ended) captions.size else shown) { at ->
            val (atTitle, atBody) =
                if (at == captions.size) Res.string.popup_end_title to Res.string.popup_end_body
                else captions[at]
            Column {
                Text(
                    stringResource(atTitle),
                    style = type.title,
                    color = Paper.ink,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(spacing.small))
                // Room for the longest caption, so a shorter one doesn't let the book grow and
                // jump.
                Text(
                    stringResource(atBody),
                    style = type.body,
                    color = Paper.inkMuted,
                    minLines = CaptionLines,
                )
            }
        }
        Spacer(Modifier.height(spacing.large))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AnimatedVisibility(
                shown > 0 && !ended,
                enter = fadeIn(tween(LabelMs)),
                exit = fadeOut(tween(LabelMs)),
            ) {
                TextButton(onClick = { book.previous() }) {
                    Text(stringResource(Res.string.popup_back), color = Paper.ink)
                }
            }
            Spacer(Modifier.weight(1f))
            val next =
                when {
                    ended -> Res.string.popup_again
                    shown == 0 -> Res.string.popup_open
                    shown < Spreads -> Res.string.popup_next
                    else -> Res.string.popup_start
                }
            Button(
                onClick = {
                    when {
                        ended -> {
                            finished = false
                            scope.launch { jumpTo(0, tapWith = null) }
                        }
                        shown < Spreads -> {
                            finished = false
                            book.next()
                        }
                        else -> finished = true
                    }
                },
                modifier = Modifier.testTag(NextTag),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Paper.button,
                        contentColor = Paper.onButton,
                    ),
            ) {
                // The label fades from one to the next while the button eases to its new width,
                // so it reads Open, Next, Start exploring as the tour goes, without a jump.
                AnimatedContent(
                    next,
                    transitionSpec = {
                        (fadeIn(tween(LabelMs, LabelMs / 3)) togetherWith
                                fadeOut(tween(LabelMs / 2)))
                            .using(SizeTransform(clip = false) { _, _ -> tween(LabelMs) })
                    },
                    contentAlignment = Alignment.Center,
                    label = "next",
                ) { label ->
                    Text(stringResource(label), maxLines = 1)
                }
            }
        }
    }
}

// Turns the book to spread [target] a leaf at a time, tapping with [finger] if there is one.
private suspend fun turnTo(book: PopUpBookState, finger: PopUpFinger?, target: Int) {
    val goal = target.coerceIn(0, Spreads)
    while (true) {
        val at = book.destination
        if (at == goal) break
        val forward = goal > at
        if (finger != null) finger.tap(book, forward)
        else if (forward) book.next() else book.previous()
        delay(RiffleMs)
    }
}

// Where in the tour the book is: a dot per spread, the open one drawn long.
@Composable
private fun Dots(selected: Int, on: Color, off: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until Spreads) {
            val width by animateDpAsState(if (i == selected) 18.dp else 6.dp)
            Box(
                Modifier.size(width = width, height = 6.dp)
                    .background(if (i == selected) on else off, CircleShape)
            )
        }
    }
}
