package dev.dimvlachos.lab.sandmandalademo.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.platform.rememberReducedMotion
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandAt
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandEntry
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandMandalaHistory
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandMandalaSounds
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandMandalaState
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandMandalaText
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandPlaces
import dev.dimvlachos.lab.core.presentation.components.sandmandala.SandVisit
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.SandMandalaScreenColors
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.sandmandala_clear
import dev.dimvlachos.lab.resources.sandmandala_clear_now
import dev.dimvlachos.lab.resources.sandmandala_cleared
import dev.dimvlachos.lab.resources.sandmandala_empty_body
import dev.dimvlachos.lab.resources.sandmandala_empty_title
import dev.dimvlachos.lab.resources.sandmandala_formed
import dev.dimvlachos.lab.resources.sandmandala_hint
import dev.dimvlachos.lab.resources.sandmandala_keep
import dev.dimvlachos.lab.resources.sandmandala_kept
import dev.dimvlachos.lab.resources.sandmandala_list
import dev.dimvlachos.lab.resources.sandmandala_restore
import dev.dimvlachos.lab.resources.sandmandala_restored
import dev.dimvlachos.lab.resources.sandmandala_search
import dev.dimvlachos.lab.resources.sandmandala_title
import dev.dimvlachos.lab.resources.sandmandala_today
import dev.dimvlachos.lab.resources.sandmandala_undo
import dev.dimvlachos.lab.resources.sandmandala_visit_1
import dev.dimvlachos.lab.resources.sandmandala_visit_10
import dev.dimvlachos.lab.resources.sandmandala_visit_10_sub
import dev.dimvlachos.lab.resources.sandmandala_visit_1_sub
import dev.dimvlachos.lab.resources.sandmandala_visit_2
import dev.dimvlachos.lab.resources.sandmandala_visit_2_sub
import dev.dimvlachos.lab.resources.sandmandala_visit_3
import dev.dimvlachos.lab.resources.sandmandala_visit_3_sub
import dev.dimvlachos.lab.resources.sandmandala_visit_4
import dev.dimvlachos.lab.resources.sandmandala_visit_4_sub
import dev.dimvlachos.lab.resources.sandmandala_visit_5
import dev.dimvlachos.lab.resources.sandmandala_visit_5_sub
import dev.dimvlachos.lab.resources.sandmandala_visit_6
import dev.dimvlachos.lab.resources.sandmandala_visit_6_sub
import dev.dimvlachos.lab.resources.sandmandala_visit_7
import dev.dimvlachos.lab.resources.sandmandala_visit_7_sub
import dev.dimvlachos.lab.resources.sandmandala_visit_8
import dev.dimvlachos.lab.resources.sandmandala_visit_8_sub
import dev.dimvlachos.lab.resources.sandmandala_visit_9
import dev.dimvlachos.lab.resources.sandmandala_visit_9_sub
import dev.dimvlachos.lab.resources.sandmandala_yesterday
import dev.dimvlachos.lab.sandmandalademo.SandMandalaHand
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The prototype's week of browsing: five visits today and five yesterday, from nine sites, each
 * site a pigment of the mandala.
 */
@Composable
internal fun rememberSandMandala(): SandMandalaState {
    val today = stringResource(Res.string.sandmandala_today)
    val yesterday = stringResource(Res.string.sandmandala_yesterday)
    val visits =
        listOf(
            visit(0, Res.string.sandmandala_visit_1, Res.string.sandmandala_visit_1_sub),
            visit(3, Res.string.sandmandala_visit_2, Res.string.sandmandala_visit_2_sub),
            visit(2, Res.string.sandmandala_visit_3, Res.string.sandmandala_visit_3_sub),
            visit(1, Res.string.sandmandala_visit_4, Res.string.sandmandala_visit_4_sub),
            visit(4, Res.string.sandmandala_visit_5, Res.string.sandmandala_visit_5_sub),
            visit(5, Res.string.sandmandala_visit_6, Res.string.sandmandala_visit_6_sub),
            visit(6, Res.string.sandmandala_visit_7, Res.string.sandmandala_visit_7_sub),
            visit(7, Res.string.sandmandala_visit_8, Res.string.sandmandala_visit_8_sub),
            visit(8, Res.string.sandmandala_visit_9, Res.string.sandmandala_visit_9_sub),
            visit(3, Res.string.sandmandala_visit_10, Res.string.sandmandala_visit_10_sub),
        )
    return remember {
        val entries = buildList {
            add(SandEntry.Section(today))
            visits.take(5).forEach { add(SandEntry.Visit(it)) }
            add(SandEntry.Section(yesterday))
            visits.drop(5).forEach { add(SandEntry.Visit(it)) }
        }
        val key = buildString {
            append(today).append('|').append(yesterday)
            visits.forEach { append('|').append(it.site).append(it.title).append(it.sub) }
        }
        KeptSand.take(key)
            ?: SandMandalaState(entries, SandMandalaScreenColors.Sites).also {
                KeptSand.keep(key, it)
            }
    }
}

/**
 * Watch starts the demo afresh, and building 20,000 grains again would leave the screen blank for a
 * few frames: a flash. So the next run takes over the sand already built, back at its rows.
 */
private object KeptSand {
    private var key: String? = null
    private var state: SandMandalaState? = null

    fun take(key: String): SandMandalaState? {
        val kept = state?.takeIf { this.key == key } ?: return null
        if (kept.at != SandAt.Rows) kept.reset()
        return kept
    }

    fun keep(key: String, state: SandMandalaState) {
        this.key = key
        this.state = state
    }
}

@Composable
private fun visit(site: Int, title: StringResource, sub: StringResource) =
    SandVisit(site, stringResource(title), stringResource(sub))

/**
 * The prototype's browser History screen, where Clear history turns the week's rows into a sand
 * mandala; the script taps Clear history, sweeps the mandala away through the same brush a finger
 * makes, and taps Undo.
 */
@Composable
internal fun SandMandalaDemo(
    state: DemoState,
    history: SandMandalaState = rememberSandMandala(),
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val sounds = rememberSoundPlayer(SandMandalaSounds)
    SideEffect { history.reducedMotion = reducedMotion }
    val places = remember { SandPlaces() }
    val hand = remember(history) { SandMandalaHand(history, places) }
    // The script's moves take turns: each waits for the sand before it.
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
        // Back to the rows if a stopped run left the sand anywhere else, so the clip loops.
        state.setFeedResetHandler {
            turn.withLock {
                hand.stop()
                if (history.at != SandAt.Rows) history.reset()
            }
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A script the user takes over leaves no fingertip behind.
    LaunchedEffect(state.replay) { if (!state.replay) hand.stop() }

    val colors =
        if (LabTheme.isDark) SandMandalaScreenColors.Dark else SandMandalaScreenColors.Light
    val text =
        SandMandalaText(
            title = stringResource(Res.string.sandmandala_title),
            clear = stringResource(Res.string.sandmandala_clear),
            keep = stringResource(Res.string.sandmandala_keep),
            search = stringResource(Res.string.sandmandala_search),
            hint = stringResource(Res.string.sandmandala_hint),
            clearNow = stringResource(Res.string.sandmandala_clear_now),
            emptyTitle = stringResource(Res.string.sandmandala_empty_title),
            emptyBody = stringResource(Res.string.sandmandala_empty_body),
            restore = stringResource(Res.string.sandmandala_restore),
            cleared = stringResource(Res.string.sandmandala_cleared),
            undo = stringResource(Res.string.sandmandala_undo),
            formed = stringResource(Res.string.sandmandala_formed),
            kept = stringResource(Res.string.sandmandala_kept),
            restored = stringResource(Res.string.sandmandala_restored),
            list = stringResource(Res.string.sandmandala_list),
        )
    val stableText = remember(text.title, text.clear, text.formed, text.list, text.restore) { text }
    val touch = LabTheme.colors.touch
    val density = LocalDensity.current
    val insets =
        if (state.recording) WindowInsets(0)
        else
            WindowInsets.systemBars
                .union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    Box(
        Modifier.fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(insets)
            .onSizeChanged {
                hand.screenW = it.width / density.density
                hand.screenH = it.height / density.density
            },
    ) {
        SandMandalaHistory(
            history,
            stableText,
            colors,
            sounds,
            modifier = Modifier.fillMaxSize(),
            places = places,
            onTouch = { if (history.scripted) hand.stop() },
        )
        Canvas(Modifier.fillMaxSize()) {
            val a = hand.shown
            if (a <= 0f) return@Canvas
            val at = hand.at * this.density
            val k = if (hand.down) .82f else 1f
            scale(k, k, pivot = at) { drawTouch(touch, at, a) }
        }
    }
}
