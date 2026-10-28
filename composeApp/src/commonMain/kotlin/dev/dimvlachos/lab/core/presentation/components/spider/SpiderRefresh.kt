package dev.dimvlachos.lab.core.presentation.components.spider

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer
import dev.dimvlachos.lab.core.presentation.canvas2d.Canvas2D
import dev.dimvlachos.lab.core.presentation.components.spider.SpiderRefreshState.Companion.lerp
import dev.dimvlachos.lab.core.presentation.ui.SpiderRefreshScreenColors
import kotlin.math.roundToInt
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collectLatest

/** The clips the spider plays. */
internal val SpiderRefreshSounds =
    setOf(
        Sound.SpiderArm,
        Sound.SpiderDisarm,
        Sound.SpiderPatter,
        Sound.SpiderClimb,
        Sound.SpiderSpin,
        Sound.SpiderCatch,
        Sound.SpiderSettle,
        Sound.SpiderEmpty,
    )

/**
 * Plays what [state] does and steps it each frame while anything moves, while the screen is in
 * front. A finger pulling it past the threshold is felt, as the prototype's `vibrate(8)`.
 */
@Composable
internal fun SpiderRefreshEffects(state: SpiderRefreshState, sounds: SoundPlayer) {
    val haptics = LocalHapticFeedback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state, sounds, haptics) {
        val listener: (SpiderEvent) -> Unit = { event ->
            when (event) {
                SpiderEvent.ArmByFinger -> {
                    sounds.play(Sound.SpiderArm, volume = .7f)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
                SpiderEvent.Arm -> sounds.play(Sound.SpiderArm, volume = .7f)
                SpiderEvent.Disarm -> sounds.play(Sound.SpiderDisarm, volume = .45f)
                SpiderEvent.Patter -> sounds.play(Sound.SpiderPatter, volume = .6f)
                SpiderEvent.Climb -> sounds.play(Sound.SpiderClimb, volume = .6f)
                SpiderEvent.Spin -> sounds.play(Sound.SpiderSpin, volume = .7f)
                SpiderEvent.Catch -> sounds.play(Sound.SpiderCatch, volume = .85f)
                SpiderEvent.Settle -> sounds.play(Sound.SpiderSettle, volume = .75f)
                SpiderEvent.Empty -> sounds.play(Sound.SpiderEmpty, volume = .7f)
            }
        }
        state.onEvent = listener
        try {
            awaitCancellation()
        } finally {
            if (state.onEvent === listener) state.onEvent = null
        }
    }
    LaunchedEffect(state, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { state.awake }
                .collectLatest { awake ->
                    if (!awake) return@collectLatest
                    var last = 0L
                    while (true) {
                        withFrameNanos { now ->
                            // The first frame moves nothing; a stall can't jump the reel.
                            val dt =
                                if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(.15f)
                            last = now
                            state.step(dt)
                        }
                    }
                }
        }
    }
}

/**
 * The feed the spider lives over: [state]'s posts as [item] draws them, top first, under a gap the
 * pull opens. The posts are pushed down in the draw phase, so a pull never recomposes; the batch
 * being reeled in hangs over them, and the spider, its thread and the strands are drawn on top.
 * [onTouch] hears a finger go down, so a script can stop. [label] says what the feed is and how to
 * refresh it.
 */
@Composable
internal fun SpiderRefreshFeed(
    state: SpiderRefreshState,
    colors: SpiderRefreshScreenColors,
    accent: Color,
    label: String,
    modifier: Modifier = Modifier,
    onTouch: () -> Unit = {},
    item: @Composable (id: Int) -> Unit,
) {
    val paints = remember(colors) { SpiderPaints(colors) }
    val dp = LocalDensity.current.density
    val touched by rememberUpdatedState(onTouch)
    Box(
        modifier
            .clipToBounds()
            .testTag("feed")
            .semantics {
                contentDescription = label
                // A screen reader scrolls the posts as a finger would.
                verticalScrollAxisRange =
                    ScrollAxisRange(value = { state.scroll }, maxValue = { state.maxScroll })
                scrollBy { _, dy -> state.scrollBy(dy / dp) }
            }
            .onSizeChanged { state.layout(it.width / dp, it.height / dp) }
            .pointerInput(state) {
                awaitEachGesture {
                    val first = awaitFirstDown(requireUnconsumed = false)
                    touched()
                    val id = first.id
                    var done = false
                    try {
                        state.down(
                            first.position.x / density,
                            first.position.y / density,
                            real = true,
                            atMs = first.uptimeMillis,
                        )
                        while (true) {
                            val event = awaitPointerEvent()
                            val c = event.changes.firstOrNull { it.id == id } ?: break
                            val x = c.position.x / density
                            val y = c.position.y / density
                            if (!c.pressed) {
                                state.up(x, y, c.uptimeMillis)
                                done = true
                                break
                            }
                            state.move(x, y, c.uptimeMillis)
                            c.consume()
                        }
                    } finally {
                        // A gesture taken away, or a pointer lost, is a cancel: it never fetches.
                        if (!done) state.cancel()
                    }
                }
            },
    ) {
        Column(
            Modifier.fillMaxWidth()
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .onSizeChanged { state.listHeight = it.height / dp }
                .graphicsLayer {
                    state.frame
                    translationY = (state.listShift - state.scroll) * density
                }
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = SpiderRefreshState.Pad.dp,
                    bottom = 18.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(SpiderRefreshState.Gap.dp),
        ) {
            for (id in state.posts) key(id) { item(id) }
        }
        // The new posts hanging below the spider, each grown from its top centre as the ball opens.
        state.hanging.forEachIndexed { i, id ->
            key(id) {
                Box(
                    Modifier.padding(horizontal = 12.dp)
                        .fillMaxWidth()
                        .wrapContentHeight(Alignment.Top, unbounded = true)
                        .onSizeChanged { state.cardMeasured(i, it.height / dp) }
                        .graphicsLayer {
                            state.frame
                            val u = state.unfold
                            val sc = lerp(.32f, 1f, u)
                            translationY = (state.reelT + state.cardOffsets[i] * u) * density
                            scaleX = sc
                            scaleY = sc
                            transformOrigin = TransformOrigin(.5f, 0f)
                            alpha = if (state.reeling) (u * 1.7f).coerceIn(0f, 1f) else 0f
                        },
                ) {
                    item(id)
                }
            }
        }
        Canvas(Modifier.matchParentSize()) {
            state.frame
            drawIntoCanvas { canvas ->
                val c = Canvas2D(canvas, size.width.roundToInt(), size.height.roundToInt())
                c.scale(density, density)
                c.drawSpiderFeed(state, paints, accent)
            }
        }
    }
}
