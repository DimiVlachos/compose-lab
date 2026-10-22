package dev.dimvlachos.lab.stickynotedemo.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.stickynote.NoteAction
import dev.dimvlachos.lab.core.presentation.components.stickynote.NoteWords
import dev.dimvlachos.lab.core.presentation.components.stickynote.Reminder
import dev.dimvlachos.lab.core.presentation.components.stickynote.StickyNoteList
import dev.dimvlachos.lab.core.presentation.components.stickynote.StickyNoteSounds
import dev.dimvlachos.lab.core.presentation.components.stickynote.StickyNoteState
import dev.dimvlachos.lab.core.presentation.components.stickynote.StickyNoteText
import dev.dimvlachos.lab.core.presentation.components.stickynote.TitleSlot
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.StickyNoteScreenColors
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.stickynote_archive
import dev.dimvlachos.lab.resources.stickynote_archived
import dev.dimvlachos.lab.resources.stickynote_back
import dev.dimvlachos.lab.resources.stickynote_done
import dev.dimvlachos.lab.resources.stickynote_item_1
import dev.dimvlachos.lab.resources.stickynote_item_1_meta
import dev.dimvlachos.lab.resources.stickynote_item_2
import dev.dimvlachos.lab.resources.stickynote_item_2_meta
import dev.dimvlachos.lab.resources.stickynote_item_3
import dev.dimvlachos.lab.resources.stickynote_item_3_meta
import dev.dimvlachos.lab.resources.stickynote_item_4
import dev.dimvlachos.lab.resources.stickynote_item_4_meta
import dev.dimvlachos.lab.resources.stickynote_item_5
import dev.dimvlachos.lab.resources.stickynote_item_5_meta
import dev.dimvlachos.lab.resources.stickynote_item_6
import dev.dimvlachos.lab.resources.stickynote_item_6_meta
import dev.dimvlachos.lab.resources.stickynote_later
import dev.dimvlachos.lab.resources.stickynote_left
import dev.dimvlachos.lab.resources.stickynote_list
import dev.dimvlachos.lab.resources.stickynote_pad_done
import dev.dimvlachos.lab.resources.stickynote_pad_done_empty
import dev.dimvlachos.lab.resources.stickynote_pad_later
import dev.dimvlachos.lab.resources.stickynote_pad_later_empty
import dev.dimvlachos.lab.resources.stickynote_put_back
import dev.dimvlachos.lab.resources.stickynote_snooze
import dev.dimvlachos.lab.resources.stickynote_snoozed
import dev.dimvlachos.lab.resources.stickynote_subtitle
import dev.dimvlachos.lab.resources.stickynote_swipe_left
import dev.dimvlachos.lab.resources.stickynote_swipe_right
import dev.dimvlachos.lab.resources.stickynote_title
import dev.dimvlachos.lab.resources.stickynote_tomorrow
import dev.dimvlachos.lab.stickynotedemo.StickyNoteHand
import kotlin.coroutines.cancellation.CancellationException
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The prototype's six reminders, in its order and colours. */
@Composable
internal fun rememberStickyNotes(): StickyNoteState {
    val items =
        listOf(
            Reminder(
                stringResource(Res.string.stickynote_item_1),
                stringResource(Res.string.stickynote_item_1_meta),
                0,
            ),
            Reminder(
                stringResource(Res.string.stickynote_item_2),
                stringResource(Res.string.stickynote_item_2_meta),
                1,
            ),
            Reminder(
                stringResource(Res.string.stickynote_item_3),
                stringResource(Res.string.stickynote_item_3_meta),
                2,
            ),
            Reminder(
                stringResource(Res.string.stickynote_item_4),
                stringResource(Res.string.stickynote_item_4_meta),
                3,
            ),
            Reminder(
                stringResource(Res.string.stickynote_item_5),
                stringResource(Res.string.stickynote_item_5_meta),
                4,
            ),
            Reminder(
                stringResource(Res.string.stickynote_item_6),
                stringResource(Res.string.stickynote_item_6_meta),
                5,
            ),
        )
    return remember { StickyNoteState(items) }
}

/**
 * The prototype's Today list of reminders, each a sticky note. The script's pulls and taps go
 * through the same gesture a finger makes.
 */
@Composable
internal fun StickyNoteDemo(
    state: DemoState,
    notes: StickyNoteState = rememberStickyNotes(),
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val sounds = rememberSoundPlayer(StickyNoteSounds)
    SideEffect { notes.reducedMotion = reducedMotion }
    val hand = remember(notes) { StickyNoteHand(notes) }
    DisposableEffect(state, hand) {
        suspend fun step(block: suspend () -> Unit) {
            try {
                block()
            } catch (e: CancellationException) {
                hand.stop()
                throw e
            }
        }
        state.setControlTapHandler { control -> step { hand.play(control) } }
        // Puts every note back as it began if a stopped run left one out, so the clip loops.
        state.setFeedResetHandler {
            step { hand.hide() }
            if (!notes.asBegun) notes.reset()
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A script the user takes over leaves no fingertip behind.
    LaunchedEffect(state.replay) { if (!state.replay) hand.stop() }

    val colors = if (LabTheme.isDark) StickyNoteScreenColors.Dark else StickyNoteScreenColors.Light
    val words =
        NoteWords(
            archive = stringResource(Res.string.stickynote_archive),
            snooze = stringResource(Res.string.stickynote_snooze),
            tomorrow = stringResource(Res.string.stickynote_tomorrow),
            done = stringResource(Res.string.stickynote_done),
            later = stringResource(Res.string.stickynote_later),
            swipeLeft = stringResource(Res.string.stickynote_swipe_left),
            swipeRight = stringResource(Res.string.stickynote_swipe_right),
        )
    val label = stringResource(Res.string.stickynote_list)
    val archived = stringResource(Res.string.stickynote_archived, TitleSlot)
    val snoozed = stringResource(Res.string.stickynote_snoozed, TitleSlot)
    val putBack = stringResource(Res.string.stickynote_put_back, TitleSlot)
    // Made once for the strings, so the list's painter and event listener aren't made again.
    val text =
        remember(
            words.archive,
            words.snooze,
            words.tomorrow,
            words.done,
            words.later,
            words.swipeLeft,
            words.swipeRight,
            label,
            archived,
            snoozed,
            putBack,
        ) {
            StickyNoteText(words, label, archived, snoozed, putBack)
        }
    val touch = LabTheme.colors.touch
    Box(Modifier.fillMaxSize().background(colors.background)) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(demoInsets(state))) {
            StickyNoteList(
                notes,
                text,
                padLabel = { kind, n -> padLabel(kind, n) },
                sounds = sounds,
                ring = colors.accent,
                modifier = Modifier.fillMaxSize(),
            ) {
                AppBar(colors, notes)
            }
            Canvas(Modifier.fillMaxSize()) {
                val a = hand.shown
                if (a <= 0f) return@Canvas
                val at = hand.at * density
                val k = if (hand.down) .82f else 1f
                scale(k, k, pivot = at) { drawTouch(touch, at, a) }
            }
        }
    }
}

@Composable
private fun padLabel(kind: NoteAction, n: Int): String =
    when {
        kind == NoteAction.Archive && n == 0 -> stringResource(Res.string.stickynote_pad_done_empty)
        kind == NoteAction.Archive -> pluralStringResource(Res.plurals.stickynote_pad_done, n, n)
        n == 0 -> stringResource(Res.string.stickynote_pad_later_empty)
        else -> pluralStringResource(Res.plurals.stickynote_pad_later, n, n)
    }

/** The list's bar: back to the lists, the day, and how many reminders are left. */
@Composable
private fun AppBar(colors: StickyNoteScreenColors, notes: StickyNoteState) {
    Row(
        Modifier.fillMaxWidth().padding(start = 6.dp, end = 12.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // The reminders app's own back, drawn as the prototype's: the demo screen's bar goes back.
        Row(
            Modifier.heightIn(min = 44.dp)
                .padding(start = 4.dp, end = 10.dp)
                .clearAndSetSemantics {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Canvas(Modifier.size(22.dp)) {
                // M14.5 5.5 8 12l6.5 6.5 in a 24 box.
                val s = size.width / 24f
                val w = 2.1f * s
                drawLine(
                    colors.ink,
                    Offset(14.5f * s, 5.5f * s),
                    Offset(8f * s, 12f * s),
                    w,
                    StrokeCap.Round,
                )
                drawLine(
                    colors.ink,
                    Offset(8f * s, 12f * s),
                    Offset(14.5f * s, 18.5f * s),
                    w,
                    StrokeCap.Round,
                )
            }
            Text(
                stringResource(Res.string.stickynote_back),
                color = colors.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(Res.string.stickynote_title),
                Modifier.semantics { heading() },
                color = colors.ink,
                fontSize = 15.5.sp,
                lineHeight = BodyLine,
                fontWeight = FontWeight(650),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(Res.string.stickynote_subtitle),
                color = colors.muted,
                fontSize = 12.sp,
                lineHeight = BodyLine,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        notes.listVersion
        Text(
            stringResource(Res.string.stickynote_left, notes.rows.size),
            Modifier.testTag("count")
                .clip(CircleShape)
                .background(colors.chip)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            color = colors.ink,
            fontSize = 12.sp,
            lineHeight = BodyLine,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}

private val BodyLine = 1.55.em
