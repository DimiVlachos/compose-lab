package dev.dimvlachos.lab.agenticdemo.presentation.components

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.first

/**
 * Keeps a chat pinned to its newest content: whenever the list could scroll further, because an
 * item arrived, a surface grew as its lines streamed in, or the keyboard shrank the viewport, it
 * moves to the bottom. Dragging up to read lets go; a drag that ends at the bottom, or [follow],
 * takes hold again.
 */
class FollowBottom internal constructor() {
    internal var following by mutableStateOf(true)

    fun follow() {
        following = true
    }
}

@Composable
fun rememberFollowBottom(listState: LazyListState): FollowBottom {
    val follow = remember(listState) { FollowBottom() }
    // Only the user's own drags decide whether to keep following; the list's own scrolls never do.
    LaunchedEffect(follow) {
        var dragged = false
        listState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> {
                    dragged = true
                    follow.following = false
                }
                is DragInteraction.Stop,
                is DragInteraction.Cancel ->
                    if (dragged) {
                        dragged = false
                        // Let the fling settle before judging where it ended.
                        snapshotFlow { listState.isScrollInProgress }.first { !it }
                        follow.following = !listState.canScrollForward
                    }
            }
        }
    }
    LaunchedEffect(follow) {
        while (true) {
            snapshotFlow { follow.following && listState.canScrollForward }.first { it }
            val last = listState.layoutInfo.totalItemsCount - 1
            // An offset past the end is clamped by the list, so this lands exactly on the bottom.
            if (last >= 0) listState.scrollToItem(last, scrollOffset = Int.MAX_VALUE / 2)
            // At most once a frame, however fast the content grows.
            withFrameNanos {}
        }
    }
    return follow
}
