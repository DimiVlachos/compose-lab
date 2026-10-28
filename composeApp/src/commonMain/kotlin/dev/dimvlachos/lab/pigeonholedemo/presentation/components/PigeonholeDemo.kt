package dev.dimvlachos.lab.pigeonholedemo.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.scale
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.Cubby
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeMail
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeSounds
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeState
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeText
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.PigeonholeScreenColors
import dev.dimvlachos.lab.pigeonholedemo.LarkspurFolders
import dev.dimvlachos.lab.pigeonholedemo.LarkspurMail
import dev.dimvlachos.lab.pigeonholedemo.PigeonholeHand
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.pigeonhole_account
import dev.dimvlachos.lab.resources.pigeonhole_back
import dev.dimvlachos.lab.resources.pigeonhole_choose
import dev.dimvlachos.lab.resources.pigeonhole_cubby
import dev.dimvlachos.lab.resources.pigeonhole_cubby_open
import dev.dimvlachos.lab.resources.pigeonhole_done
import dev.dimvlachos.lab.resources.pigeonhole_empty
import dev.dimvlachos.lab.resources.pigeonhole_folder_archive
import dev.dimvlachos.lab.resources.pigeonhole_folder_family
import dev.dimvlachos.lab.resources.pigeonhole_folder_receipts
import dev.dimvlachos.lab.resources.pigeonhole_folder_school
import dev.dimvlachos.lab.resources.pigeonhole_folder_travel
import dev.dimvlachos.lab.resources.pigeonhole_folder_work
import dev.dimvlachos.lab.resources.pigeonhole_folders
import dev.dimvlachos.lab.resources.pigeonhole_hint
import dev.dimvlachos.lab.resources.pigeonhole_inbox
import dev.dimvlachos.lab.resources.pigeonhole_list
import dev.dimvlachos.lab.resources.pigeonhole_messages
import dev.dimvlachos.lab.resources.pigeonhole_move_action
import dev.dimvlachos.lab.resources.pigeonhole_move_to
import dev.dimvlachos.lab.resources.pigeonhole_moved
import dev.dimvlachos.lab.resources.pigeonhole_not_moved
import dev.dimvlachos.lab.resources.pigeonhole_opened
import dev.dimvlachos.lab.resources.pigeonhole_search
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The prototype's Larkspur Mail: seven messages in the inbox, six folders in the rack. */
@Composable
internal fun rememberLarkspurMail(): PigeonholeState {
    val names =
        listOf(
            stringResource(Res.string.pigeonhole_folder_work),
            stringResource(Res.string.pigeonhole_folder_receipts),
            stringResource(Res.string.pigeonhole_folder_family),
            stringResource(Res.string.pigeonhole_folder_travel),
            stringResource(Res.string.pigeonhole_folder_school),
            stringResource(Res.string.pigeonhole_folder_archive),
        )
    return remember { PigeonholeState(LarkspurFolders.zip(names, ::Cubby), LarkspurMail) }
}

/**
 * The prototype's mail screen, whose Move to… is the pigeonhole rack. The script's carries and taps
 * go through the same calls a finger makes.
 */
@Composable
internal fun PigeonholeDemo(
    state: DemoState,
    mail: PigeonholeState = rememberLarkspurMail(),
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val sounds = rememberSoundPlayer(PigeonholeSounds)
    SideEffect { mail.reducedMotion = reducedMotion }
    val hand = remember(mail) { PigeonholeHand(mail) }
    // The script's steps take turns, as the prototype awaits each: on a slow device a step that
    // runs late holds the next back rather than steering the same fingertip at once.
    val turn = remember { Mutex() }
    DisposableEffect(state, hand) {
        suspend fun step(block: suspend () -> Unit) {
            try {
                turn.withLock { block() }
            } catch (e: CancellationException) {
                hand.stop()
                throw e
            }
        }
        state.setControlTapHandler { control -> step { hand.play(control) } }
        // Puts every message back as it began if a stopped run left one moved, so the clip loops.
        state.setFeedResetHandler {
            step {
                hand.hide()
                if (!mail.asBegun) mail.reset()
            }
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A script the user takes over leaves no fingertip behind.
    LaunchedEffect(state.replay) { if (!state.replay) hand.stop() }

    val colors = if (LabTheme.isDark) PigeonholeScreenColors.Dark else PigeonholeScreenColors.Light
    val text = larkspurText()
    val touch = LabTheme.colors.touch
    Box(Modifier.fillMaxSize().background(colors.background)) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(demoInsets(state))) {
            PigeonholeMail(
                mail,
                text,
                sounds,
                colors,
                Modifier.fillMaxSize(),
                onPlacedFolders = { hand.folders = it },
                onPlacedBack = { hand.back = it },
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
}

@Composable
private fun larkspurText(): PigeonholeText {
    val inbox = stringResource(Res.string.pigeonhole_inbox)
    val account = stringResource(Res.string.pigeonhole_account)
    val search = stringResource(Res.string.pigeonhole_search)
    val moveTo = stringResource(Res.string.pigeonhole_move_to)
    val folders = stringResource(Res.string.pigeonhole_folders)
    val done = stringResource(Res.string.pigeonhole_done)
    val back = stringResource(Res.string.pigeonhole_back)
    val empty = stringResource(Res.string.pigeonhole_empty)
    val list = stringResource(Res.string.pigeonhole_list)
    val moveAction = stringResource(Res.string.pigeonhole_move_action)
    val choose = stringResource(Res.string.pigeonhole_choose, Slot)
    val moved = stringResource(Res.string.pigeonhole_moved, Slot)
    val notMoved = stringResource(Res.string.pigeonhole_not_moved)
    val hint = stringResource(Res.string.pigeonhole_hint)
    return remember(
        inbox,
        account,
        search,
        moveTo,
        folders,
        done,
        back,
        empty,
        list,
        moveAction,
        choose,
        moved,
        notMoved,
        hint,
    ) {
        PigeonholeText(
            inbox = inbox,
            account = account,
            search = search,
            moveTo = moveTo,
            folders = folders,
            done = done,
            back = back,
            empty = empty,
            list = list,
            moveAction = moveAction,
            choose = choose,
            moved = moved,
            notMoved = notMoved,
            hint = hint,
            inboxSaid = inbox,
            messages = { n -> pluralStringResource(Res.plurals.pigeonhole_messages, n, n) },
            opened = { name, n -> pluralStringResource(Res.plurals.pigeonhole_opened, n, name, n) },
            cubby = { name, n, open ->
                if (open) pluralStringResource(Res.plurals.pigeonhole_cubby_open, n, name, n)
                else pluralStringResource(Res.plurals.pigeonhole_cubby, n, name, n)
            },
        )
    }
}

// Stands in for a subject or folder in the announcements, filled in when one is made.
private const val Slot = "%1\$s"
