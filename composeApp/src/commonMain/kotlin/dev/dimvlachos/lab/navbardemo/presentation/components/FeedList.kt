package dev.dimvlachos.lab.navbardemo.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.presentation.components.navbar.AnimatedNavBarHeight
import dev.dimvlachos.lab.core.presentation.components.navbar.NavBarScrollState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme

// Room past the bar, so the last card scrolls clear of it with a gap: with the bar's own height
// and its 20 dp margin, the 140 dp the feed has always ended with.
private val FeedListSlack = 28.dp

@Composable
internal fun FeedList(state: DemoState, scrollState: NavBarScrollState, barBottom: Dp) {
    val spacing = LabTheme.spacing
    val insets = demoInsets(state).asPaddingValues()
    val direction = LocalLayoutDirection.current
    val listState = rememberLazyListState()
    // Programmatic scrolls skip nested scroll, so feed the bar the same deltas the list consumes.
    DisposableEffect(state, listState, scrollState) {
        state.setScrollHandler { px ->
            var previous = 0f
            animate(
                0f,
                px,
                animationSpec = tween(durationMillis = 1_000, easing = FastOutSlowInEasing),
            ) { value, _ ->
                val consumed = listState.dispatchRawDelta(value - previous)
                previous = value
                scrollState.onScroll(-consumed)
            }
        }
        onDispose { state.setScrollHandler(null) }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollState.nestedScrollConnection),
        state = listState,
        contentPadding =
            PaddingValues(
                start = spacing.mediumLarge + insets.calculateStartPadding(direction),
                end = spacing.mediumLarge + insets.calculateEndPadding(direction),
                top = spacing.mediumLarge,
                // Clear of the bar wherever it sits: its height, its margin and the gesture bar.
                bottom =
                    AnimatedNavBarHeight +
                        barBottom +
                        insets.calculateBottomPadding() +
                        FeedListSlack,
            ),
        verticalArrangement = Arrangement.spacedBy(spacing.smallMedium),
    ) {
        items(24) { index -> FeedCard(index) }
    }
}
