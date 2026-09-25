package dev.dimvlachos.lab.navbardemo.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.presentation.components.navbar.NavBarScrollState
import dev.dimvlachos.lab.core.presentation.ui.LabTheme

private val FeedListBottomInset = 140.dp

@Composable
internal fun FeedList(state: DemoState, scrollState: NavBarScrollState) {
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
                start = LabTheme.spacing.mediumLarge,
                end = LabTheme.spacing.mediumLarge,
                top = LabTheme.spacing.mediumLarge,
                bottom = FeedListBottomInset,
            ),
        verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.smallMedium),
    ) {
        items(24) { index -> FeedCard(index) }
    }
}
