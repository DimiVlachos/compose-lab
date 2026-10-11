package dev.dimvlachos.lab.fishingdemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingRefresh
import dev.dimvlachos.lab.core.presentation.components.fishing.FishingRefreshState
import dev.dimvlachos.lab.core.presentation.components.fishing.rememberFishingRefreshState
import dev.dimvlachos.lab.core.presentation.components.fishing.risingFromWater
import dev.dimvlachos.lab.core.presentation.components.touch.ScriptedTouch
import dev.dimvlachos.lab.core.presentation.components.touch.drawTouch
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.isWideStage
import dev.dimvlachos.lab.fishingdemo.FeedIsland
import dev.dimvlachos.lab.fishingdemo.IslandFeed
import dev.dimvlachos.lab.fishingdemo.rememberIslandFeed
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.fishing_feed_hint
import dev.dimvlachos.lab.resources.fishing_feed_title
import kotlin.math.min
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// The scripted finger holds the list this long at the end of a pull before it lets go.
private const val HoldMs = 120L

// Where the scripted finger comes down, below the top of the list.
private val FingerStart = 28.dp

// The photos' shape: a little wider than tall.
private const val PhotoAspect = 16f / 10f

// On a phone on its side the list is at most this wide, in the middle, and its photos sit beside
// their names, at most this tall and this share of the screen's height, so a whole card shows.
private val WideListWidth = 640.dp
private val WidePhotoHeight = 140.dp
private const val WidePhotoShare = 0.45f

/**
 * An island photo feed under a fishing refresh: pull it down to fish for new islands. Its source is
 * scripted for the clip: the first refresh catches two islands, the second finds nothing new, the
 * third fails and the line snaps, and the fourth catches one more. The script's pulls are played by
 * a fingertip drawn on the list, through the same nested scroll a finger's drag sends.
 */
@Composable
internal fun FishingDemo(
    state: DemoState,
    feed: IslandFeed = rememberIslandFeed(),
    fishing: FishingRefreshState = rememberFishingRefreshState(),
) {
    val list = rememberLazyListState()
    val dispatcher = remember { NestedScrollDispatcher() }
    val finger = remember { FeedFinger() }
    val density = LocalDensity.current
    DisposableEffect(state, feed, density) {
        state.setRefreshPullHandler { distance, duration ->
            finger.pull(
                dispatcher,
                with(density) { distance.toPx() },
                duration.inWholeMilliseconds.toInt(),
            )
        }
        state.setFeedResetHandler { feed.reset() }
        onDispose {
            state.setRefreshPullHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    // A catch goes on top: the list stays at its top, so the catch rises into view.
    SideEffect { feed.onCatch = { list.requestScrollToItem(0) } }
    val colors = LabTheme.colors
    val spacing = LabTheme.spacing
    val type = LabTheme.typography
    val touch = colors.touch
    val inset = WindowInsets.navigationBars.asPaddingValues()
    val sides = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    val direction = LocalLayoutDirection.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Wide, as on a phone on its side: the header takes one line, and the list's cards, a photo
        // beside its name, keep to the middle; the water and the rod still run the whole width.
        val wide = isWideStage(maxWidth, maxHeight)
        val photoHeight = min(WidePhotoHeight, maxHeight * WidePhotoShare)
        val listSide =
            if (wide) {
                val room =
                    maxWidth -
                        sides.asPaddingValues().let {
                            it.calculateLeftPadding(direction) + it.calculateRightPadding(direction)
                        }
                max(spacing.medium, (room - WideListWidth) / 2)
            } else spacing.medium
        Column(Modifier.fillMaxSize().background(colors.background).windowInsetsPadding(sides)) {
            val title = stringResource(Res.string.fishing_feed_title)
            val hint = stringResource(Res.string.fishing_feed_hint)
            if (wide) {
                Row(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = spacing.mediumLarge, vertical = spacing.small),
                    horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                ) {
                    Text(
                        title,
                        style = type.title,
                        color = colors.textPrimary,
                        modifier = Modifier.alignByBaseline().semantics { heading() },
                    )
                    Text(
                        hint,
                        style = type.label,
                        color = colors.textMuted,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            } else {
                Column(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = spacing.mediumLarge, vertical = spacing.medium)
                ) {
                    Text(
                        title,
                        style = type.title,
                        color = colors.textPrimary,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(hint, style = type.label, color = colors.textMuted)
                }
            }
            FishingRefresh(
                status = feed.status,
                onRefresh = feed::refresh,
                modifier =
                    Modifier.fillMaxWidth().weight(1f).clipToBounds().drawWithContent {
                        drawContent()
                        with(finger) { drawFinger(touch) }
                    },
                state = fishing,
            ) {
                LazyColumn(
                    state = list,
                    modifier =
                        Modifier.fillMaxSize()
                            .testTag("feed")
                            .nestedScroll(
                                remember { object : NestedScrollConnection {} },
                                dispatcher,
                            ),
                    contentPadding =
                        PaddingValues(
                            start = listSide,
                            end = listSide,
                            top = spacing.small,
                            bottom = spacing.small + inset.calculateBottomPadding(),
                        ),
                ) {
                    itemsIndexed(feed.items, key = { _, island -> island.id }) { index, island ->
                        IslandCard(
                            island,
                            photoHeight = if (wide) photoHeight else null,
                            Modifier.risingFromWater(fishing, index),
                        )
                    }
                }
            }
        }
    }
}

// A photo with its island's name and a line about it, read as one by a screen reader: the name
// under the photo, or beside it, [photoHeight] tall, when there is one. The gap under it is part
// of it, so a card rising out of the water brings its own room with it.
@Composable
private fun IslandCard(island: FeedIsland, photoHeight: Dp?, modifier: Modifier) {
    val colors = LabTheme.colors
    val spacing = LabTheme.spacing
    Column(modifier.fillMaxWidth().padding(bottom = spacing.medium)) {
        val card =
            Modifier.fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .clip(LabTheme.shapes.extraLarge)
                .background(colors.surface)
        val photo = painterResource(island.photo)
        if (photoHeight == null) {
            Column(card) {
                Image(
                    photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(PhotoAspect),
                )
                IslandWords(island, Modifier.padding(spacing.medium))
            }
        } else {
            Row(card, verticalAlignment = Alignment.CenterVertically) {
                Image(
                    photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.height(photoHeight).aspectRatio(PhotoAspect),
                )
                IslandWords(island, Modifier.weight(1f).padding(spacing.medium))
            }
        }
    }
}

@Composable
private fun IslandWords(island: FeedIsland, modifier: Modifier) {
    val colors = LabTheme.colors
    Column(modifier) {
        Text(
            stringResource(island.title),
            style = LabTheme.typography.subtitle,
            color = colors.textPrimary,
        )
        Text(
            stringResource(island.caption),
            style = LabTheme.typography.label,
            color = colors.textMuted,
        )
    }
}

/**
 * The scripted fingertip on the feed, so a clip shows the hand: it comes down near the top of the
 * list, pulls it down and lifts. Its pull goes through the list's own nested scroll, as a finger's
 * drag at the top of a list does, so the refresh can't tell it from a finger. Nothing but the
 * script moves it.
 */
@Stable
private class FeedFinger {
    private val alpha = Animatable(0f)

    // How far the finger has moved down, in px, and how far its pull goes; read only while
    // drawing.
    private var travelled by mutableFloatStateOf(0f)
    private var distance by mutableFloatStateOf(0f)

    // Which pull has the fingertip. A pull that starts as the last one fades out takes it over,
    // and the last one, cancelled mid-fade, must not then hide the new one's fingertip.
    private var pulling = 0

    /**
     * Shows the fingertip, pulls [distance] px down over [durationMs] as a hand does, quick to
     * start and easing in, holds it a moment and lets go, through [dispatcher]. A script stopped
     * mid-pull still lets go, so the list isn't left held.
     */
    suspend fun pull(dispatcher: NestedScrollDispatcher, distance: Float, durationMs: Int) {
        val mine = ++pulling
        travelled = 0f
        this.distance = distance
        var holding = false
        try {
            alpha.animateTo(1f, tween(ScriptedTouch.DownMs))
            holding = true
            var pulled = 0f
            animate(
                0f,
                distance,
                animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
            ) { value, _ ->
                val step = Offset(0f, value - pulled)
                pulled = value
                travelled = value
                // As a list at its top does: offered first to those above it, and what they leave
                // it can't scroll, so it goes on up.
                val taken = dispatcher.dispatchPreScroll(step, NestedScrollSource.UserInput)
                dispatcher.dispatchPostScroll(
                    Offset.Zero,
                    step - taken,
                    NestedScrollSource.UserInput,
                )
            }
            delay(HoldMs)
            letGo(dispatcher)
            holding = false
            alpha.animateTo(0f, tween(ScriptedTouch.UpMs))
        } finally {
            if (holding) withContext(NonCancellable) { letGo(dispatcher) }
            if (mine == pulling) withContext(NonCancellable) { alpha.snapTo(0f) }
        }
    }

    private suspend fun letGo(dispatcher: NestedScrollDispatcher) {
        val taken = dispatcher.dispatchPreFling(Velocity.Zero)
        dispatcher.dispatchPostFling(taken, Velocity.Zero - taken)
    }

    // The whole pull is sent to the list, as the threshold asks; a list too short to show it, as
    // on a phone on its side, has the fingertip drawn moving less, so it stays on the screen.
    fun DrawScope.drawFinger(color: Color) {
        val alpha = alpha.value
        if (alpha <= 0f) return
        val start = FingerStart.toPx()
        val room = size.height - start - ScriptedTouch.Radius.toPx() * 2f
        val scale = if (distance > 0f) min(1f, room / distance) else 1f
        drawTouch(color, Offset(size.width / 2f, start + travelled * scale), alpha)
    }
}
