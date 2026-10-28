package dev.dimvlachos.lab.graveundodemo.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.graveundo.GraveTask
import dev.dimvlachos.lab.core.presentation.components.graveundo.GraveTitleSlot
import dev.dimvlachos.lab.core.presentation.components.graveundo.GraveUndoList
import dev.dimvlachos.lab.core.presentation.components.graveundo.GraveUndoSounds
import dev.dimvlachos.lab.core.presentation.components.graveundo.GraveUndoState
import dev.dimvlachos.lab.core.presentation.components.graveundo.GraveUndoText
import dev.dimvlachos.lab.core.presentation.components.graveundo.GraveWords
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.GraveUndoScreenColors
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.graveundodemo.GraveUndoHand
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.graveundo_back_from_grave
import dev.dimvlachos.lab.resources.graveundo_bury
import dev.dimvlachos.lab.resources.graveundo_business
import dev.dimvlachos.lab.resources.graveundo_date
import dev.dimvlachos.lab.resources.graveundo_delete
import dev.dimvlachos.lab.resources.graveundo_deleted
import dev.dimvlachos.lab.resources.graveundo_done
import dev.dimvlachos.lab.resources.graveundo_empty
import dev.dimvlachos.lab.resources.graveundo_here_lies
import dev.dimvlachos.lab.resources.graveundo_hint
import dev.dimvlachos.lab.resources.graveundo_laid_to_rest
import dev.dimvlachos.lab.resources.graveundo_list
import dev.dimvlachos.lab.resources.graveundo_mark_done
import dev.dimvlachos.lab.resources.graveundo_not_done
import dev.dimvlachos.lab.resources.graveundo_rest_in_peace
import dev.dimvlachos.lab.resources.graveundo_restore
import dev.dimvlachos.lab.resources.graveundo_restored
import dev.dimvlachos.lab.resources.graveundo_tap_to_undo
import dev.dimvlachos.lab.resources.graveundo_task_1
import dev.dimvlachos.lab.resources.graveundo_task_1_sub
import dev.dimvlachos.lab.resources.graveundo_task_1_time
import dev.dimvlachos.lab.resources.graveundo_task_2
import dev.dimvlachos.lab.resources.graveundo_task_2_sub
import dev.dimvlachos.lab.resources.graveundo_task_2_time
import dev.dimvlachos.lab.resources.graveundo_task_3
import dev.dimvlachos.lab.resources.graveundo_task_3_sub
import dev.dimvlachos.lab.resources.graveundo_task_3_time
import dev.dimvlachos.lab.resources.graveundo_task_4
import dev.dimvlachos.lab.resources.graveundo_task_4_sub
import dev.dimvlachos.lab.resources.graveundo_task_4_time
import dev.dimvlachos.lab.resources.graveundo_task_5
import dev.dimvlachos.lab.resources.graveundo_task_5_sub
import dev.dimvlachos.lab.resources.graveundo_task_5_time
import dev.dimvlachos.lab.resources.graveundo_task_6
import dev.dimvlachos.lab.resources.graveundo_task_6_sub
import dev.dimvlachos.lab.resources.graveundo_task_6_time
import dev.dimvlachos.lab.resources.graveundo_task_7
import dev.dimvlachos.lab.resources.graveundo_task_7_sub
import dev.dimvlachos.lab.resources.graveundo_task_7_time
import dev.dimvlachos.lab.resources.graveundo_title
import dev.dimvlachos.lab.resources.graveundo_undo
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The prototype's seven tasks at Maple Street Bakery, the day before Halloween. */
@Composable
internal fun rememberGraveUndo(): GraveUndoState {
    val tasks =
        listOf(
            task(
                Res.string.graveundo_task_1,
                Res.string.graveundo_task_1_sub,
                Res.string.graveundo_task_1_time,
            ),
            task(
                Res.string.graveundo_task_2,
                Res.string.graveundo_task_2_sub,
                Res.string.graveundo_task_2_time,
            ),
            task(
                Res.string.graveundo_task_3,
                Res.string.graveundo_task_3_sub,
                Res.string.graveundo_task_3_time,
            ),
            task(
                Res.string.graveundo_task_4,
                Res.string.graveundo_task_4_sub,
                Res.string.graveundo_task_4_time,
            ),
            task(
                Res.string.graveundo_task_5,
                Res.string.graveundo_task_5_sub,
                Res.string.graveundo_task_5_time,
            ),
            task(
                Res.string.graveundo_task_6,
                Res.string.graveundo_task_6_sub,
                Res.string.graveundo_task_6_time,
            ),
            task(
                Res.string.graveundo_task_7,
                Res.string.graveundo_task_7_sub,
                Res.string.graveundo_task_7_time,
            ),
        )
    return remember { GraveUndoState(tasks) }
}

@Composable
private fun task(
    title: org.jetbrains.compose.resources.StringResource,
    sub: org.jetbrains.compose.resources.StringResource,
    time: org.jetbrains.compose.resources.StringResource,
) = GraveTask(stringResource(title), stringResource(sub), stringResource(time))

/**
 * The prototype's bakery to-do list, where a task swiped away is buried in the graveyard where the
 * snackbar goes and a skeleton hand hands it back. The script's swipe and tap go through the same
 * drag and tap a finger makes.
 */
@Composable
internal fun GraveUndoDemo(
    state: DemoState,
    list: GraveUndoState = rememberGraveUndo(),
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val sounds = rememberSoundPlayer(GraveUndoSounds)
    SideEffect { list.reducedMotion = reducedMotion }
    val hand = remember(list) { GraveUndoHand(list) }
    // The script's moves take turns: on a slow device one can run past its slot, and the next
    // waits for it rather than steering the hand at the same time.
    val turn = remember { Mutex() }
    DisposableEffect(state, hand) {
        suspend fun step(block: suspend () -> Unit) {
            try {
                block()
            } catch (e: CancellationException) {
                hand.stop()
                throw e
            }
        }
        state.setControlTapHandler { control -> turn.withLock { step { hand.play(control) } } }
        // Puts every task back as it began if a stopped run left one out, so the clip loops.
        state.setFeedResetHandler {
            turn.withLock {
                hand.stop()
                if (!list.asBegun) list.reset()
            }
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A script the user takes over leaves no fingertip behind.
    LaunchedEffect(state.replay) { if (!state.replay) hand.stop() }

    val dark = LabTheme.isDark
    val colors = if (dark) GraveUndoScreenColors.Dark else GraveUndoScreenColors.Light
    val words =
        GraveWords(
            laidToRest = stringResource(Res.string.graveundo_laid_to_rest),
            tapToUndo = stringResource(Res.string.graveundo_tap_to_undo),
            backFromGrave = stringResource(Res.string.graveundo_back_from_grave),
            restInPeace = stringResource(Res.string.graveundo_rest_in_peace),
            hereLies = stringResource(Res.string.graveundo_here_lies),
            bury = stringResource(Res.string.graveundo_bury),
        )
    val label = stringResource(Res.string.graveundo_list)
    val delete = stringResource(Res.string.graveundo_delete)
    val markDone = stringResource(Res.string.graveundo_mark_done, GraveTitleSlot)
    val done = stringResource(Res.string.graveundo_done)
    val notDone = stringResource(Res.string.graveundo_not_done)
    val undo = stringResource(Res.string.graveundo_undo, GraveTitleSlot)
    val deleted = stringResource(Res.string.graveundo_deleted, GraveTitleSlot)
    val restored = stringResource(Res.string.graveundo_restored, GraveTitleSlot)
    // Made once for the strings, so the list's painter and event listener aren't made again.
    val text =
        remember(
            words.laidToRest,
            words.bury,
            words.hereLies,
            label,
            delete,
            markDone,
            undo,
            deleted,
        ) {
            GraveUndoText(words, label, delete, markDone, done, notDone, undo, deleted, restored)
        }
    val touch = LabTheme.colors.touch
    val insets =
        if (state.recording) WindowInsets(0)
        else
            WindowInsets.systemBars
                .union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    Box(Modifier.fillMaxSize().background(colors.background).windowInsetsPadding(insets)) {
        GraveUndoList(
            list,
            text,
            colors,
            sounds,
            ring = colors.accent,
            modifier = Modifier.fillMaxSize(),
            header = { Header(list, colors) },
            hint = { Hint() },
            emptyState = { Empty(list, colors) },
        )
        Canvas(Modifier.fillMaxSize()) {
            val a = hand.shown
            if (a <= 0f) return@Canvas
            val at = hand.at * density
            val k = if (hand.down) .82f else 1f
            scale(k, k, pivot = at) { drawTouch(touch, at, a) }
        }
    }
}

/** The bakery's name with its orange dot, "Today", and the date with how many tasks are left. */
@Composable
private fun Header(list: GraveUndoState, colors: GraveUndoScreenColors) {
    list.listVersion
    val n = list.liveCount
    Column(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 14.dp),
    ) {
        Row(
            Modifier.clearAndSetSemantics {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(Modifier.size(8.dp).background(colors.accent, CircleShape))
            BasicText(
                stringResource(Res.string.graveundo_business),
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                color = { colors.muted },
            )
        }
        BasicText(
            stringResource(Res.string.graveundo_title),
            Modifier.padding(top = 6.dp, bottom = 2.dp).semantics { heading() },
            style =
                TextStyle(
                    fontSize = 31.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 1.1.em,
                    letterSpacing = (-.02).em,
                ),
            color = { colors.ink },
        )
        BasicText(
            pluralStringResource(Res.plurals.graveundo_date, n, n),
            style = TextStyle(fontSize = 13.5.sp, fontFeatureSettings = "tnum"),
            color = { colors.muted },
        )
    }
}

/** The pill over the foot of the list that says how to bury a task. */
@Composable
private fun Hint() {
    Box(Modifier.fillMaxSize().clearAndSetSemantics {}, contentAlignment = Alignment.BottomCenter) {
        Row(
            Modifier.padding(bottom = 40.dp)
                .background(GraveUndoScreenColors.HintPill, RoundedCornerShape(999.dp))
                .border(1.dp, GraveUndoScreenColors.HintEdge, RoundedCornerShape(999.dp))
                .padding(start = 11.dp, end = 13.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Canvas(Modifier.size(14.dp)) {
                val s = size.width / 14f
                val stroke = Stroke(1.8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                val p =
                    androidx.compose.ui.graphics.Path().apply {
                        moveTo(12f * s, 7f * s)
                        lineTo(2.5f * s, 7f * s)
                        moveTo(6f * s, 3.5f * s)
                        lineTo(2.5f * s, 7f * s)
                        lineTo(6f * s, 10.5f * s)
                    }
                drawPath(p, Color.White, style = stroke)
            }
            BasicText(
                stringResource(Res.string.graveundo_hint),
                style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
                color = { Color.White },
                maxLines = 1,
            )
        }
    }
}

/** Nothing left: a line and a button that brings every task back. */
@Composable
private fun Empty(list: GraveUndoState, colors: GraveUndoScreenColors) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BasicText(
            stringResource(Res.string.graveundo_empty),
            style = TextStyle(fontSize = 14.5.sp, textAlign = TextAlign.Center),
            color = { colors.muted },
        )
        Box(
            Modifier.heightIn(min = 44.dp)
                .background(colors.accent, RoundedCornerShape(999.dp))
                .clickable(role = Role.Button) { list.reset() }
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                stringResource(Res.string.graveundo_restore),
                Modifier.padding(vertical = 12.dp),
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = { Color.White },
            )
        }
    }
}
