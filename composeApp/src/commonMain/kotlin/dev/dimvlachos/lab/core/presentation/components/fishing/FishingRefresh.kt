package dev.dimvlachos.lab.core.presentation.components.fishing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.fishing_caught
import dev.dimvlachos.lab.resources.fishing_failed
import dev.dimvlachos.lab.resources.fishing_nothing_new
import dev.dimvlachos.lab.resources.fishing_refresh
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Pull-to-refresh where refreshing is fishing. Pull [content], a scrolling list, down from its top
 * and a rod bends over it as its line tightens; the further the pull, the more it bends, and a tick
 * of haptics marks the threshold. Let go past it and [onRefresh] is called: the line is cast and a
 * bobber lands in a band of water over the top of the list, with a splash, and floats there,
 * bobbing, for as long as the refresh takes.
 *
 * The loading is the caller's: it hoists [status], from a ViewModel say, sets
 * [FishingStatus.Refreshing] when [onRefresh] is called, and [FishingStatus.Landed] once it is
 * done. However quickly it lands, the bobber floats a moment first, and then the outcome plays:
 * - [FishingOutcome.Caught]: the bobber is pulled under, the line is reeled in, and the newest
 *   items rise out of the water into the top of the list (see [risingFromWater]).
 * - [FishingOutcome.NothingNew]: the line is reeled in to an empty hook.
 * - [FishingOutcome.Failed]: the line snaps, its slack falls and the bobber drifts off. Pulling
 *   again retries. Then the water closes and everything comes to rest. Setting
 *   [FishingStatus.Refreshing] without a pull, for a refresh the caller starts itself, casts from
 *   rest; going back to [FishingStatus.Idle] mid-refresh reels the line in without an outcome.
 *
 * The gesture is Material's [pullToRefresh]: nested scroll, overscroll and flings are handled by
 * it, and only the band is drawn here. While an outcome plays out, a new pull isn't taken. A screen
 * reader can refresh without the gesture, through a custom action, and hears each outcome politely:
 * how many new items came in, that nothing new did, or that the refresh failed.
 *
 * Nothing recomposes while the line, the water or a rising catch moves: the band is drawn in a
 * layer of its own, over the list, and the list moves down under it in a layer too. The frame loop
 * runs only while something moves or the band shows, and sleeps at rest.
 */
@Composable
public fun FishingRefresh(
    status: FishingStatus,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: FishingRefreshState = rememberFishingRefreshState(),
    content: @Composable () -> Unit,
) {
    ReportComposition()
    val haptics = LocalHapticFeedback.current
    val currentOnRefresh by rememberUpdatedState(onRefresh)
    DisposableEffect(state, haptics) {
        // A key's click at the threshold, as the pull cord's switch: the gesture haptics would say
        // more, but Android only plays them from 14 on.
        state.onThreshold = { haptics.performHapticFeedback(HapticFeedbackType.VirtualKey) }
        state.onBite = { haptics.performHapticFeedback(HapticFeedbackType.Confirm) }
        state.onSnap = { haptics.performHapticFeedback(HapticFeedbackType.Reject) }
        onDispose {
            state.onThreshold = {}
            state.onBite = {}
            state.onSnap = {}
        }
    }
    // Followed as the status is applied, before the frame is laid out: a catch the caller puts on
    // top of its list in the same change is under water from the first frame it is laid out in.
    SideEffect { state.follow(status) }
    // The rig steps on a frame at a time while anything moves or the band shows, and the loop
    // sleeps once everything is at rest out of sight.
    LaunchedEffect(state) {
        while (true) {
            snapshotFlow { state.awake || state.pull.distanceFraction > 0f }.first { it }
            state.awake = true
            var last = withFrameNanos { it }
            while (state.awake) {
                withFrameNanos { now ->
                    val seconds = (now - last) / 1_000_000_000f
                    state.advance(seconds.coerceIn(0f, FishingDimens.MostFrameSeconds))
                    last = now
                }
            }
        }
    }
    val painter = remember { FishingPainter() }
    val colors = LabTheme.colors
    val density = LocalDensity.current.density
    val refreshLabel = stringResource(Res.string.fishing_refresh)
    val busy = state.busy
    // Whether a refresh can be asked for: not while one is under way or still playing out.
    val canRefresh = !busy && status != FishingStatus.Refreshing
    Box(
        modifier
            .onSizeChanged { state.place(it.width, density) }
            .onPlaced { state.box = it }
            .pullToRefresh(
                isRefreshing = status == FishingStatus.Refreshing || busy,
                state = state.pull,
                enabled = !busy,
                threshold = FishingDimens.Band,
                onRefresh = onRefresh,
            )
    ) {
        // The list moves down under the band by the air above the water, in a layer, so it moves
        // without being laid out again; the water lies over its top.
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                translationY = FishingDimens.WaterLevel.toPx() * state.open
            }
        ) {
            content()
        }
        // The band's layer covers the whole of the fishing refresh, not just the band: a catch is
        // hauled up from deep in the list, and a layer draws nothing outside its own bounds.
        Spacer(
            Modifier.fillMaxSize().graphicsLayer().drawBehind {
                with(painter) { draw(state, colors) }
            }
        )
        // The screen reader's way in, as the pull is: a button only it sees, at the top, where the
        // pull starts. It takes no touches, so the list under it gets them all.
        Spacer(
            Modifier.size(RefreshTarget).semantics {
                contentDescription = refreshLabel
                role = Role.Button
                if (!canRefresh) disabled()
                onClick(label = refreshLabel) {
                    if (canRefresh) currentOnRefresh()
                    canRefresh
                }
            }
        )
        Announcer(state)
    }
}

// The screen reader's refresh button: the smallest target it is comfortable to find by touch.
private val RefreshTarget = 48.dp

// Says, politely, how the last refresh came out, from a node of its own that only this changes:
// the band above it redraws every frame without touching it.
@Composable
private fun Announcer(state: FishingRefreshState) {
    val text =
        when (val landed = state.announced) {
            is FishingStatus.Landed ->
                when (val outcome = landed.outcome) {
                    is FishingOutcome.Caught ->
                        pluralStringResource(
                            Res.plurals.fishing_caught,
                            outcome.count,
                            outcome.count,
                        )
                    FishingOutcome.NothingNew -> stringResource(Res.string.fishing_nothing_new)
                    is FishingOutcome.Failed -> stringResource(Res.string.fishing_failed)
                }
            else -> ""
        }
    Spacer(
        Modifier.size(1.dp).semantics {
            liveRegion = LiveRegionMode.Polite
            if (text.isNotEmpty()) contentDescription = text
        }
    )
}
