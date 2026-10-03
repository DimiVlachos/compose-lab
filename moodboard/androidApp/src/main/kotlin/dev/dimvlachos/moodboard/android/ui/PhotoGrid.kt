package dev.dimvlachos.moodboard.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import dev.dimvlachos.moodboard.domain.Photo
import dev.dimvlachos.moodboard.morph.MorphEnd
import dev.dimvlachos.moodboard.ui.components.PhotoImage

/**
 * Three columns of photos. A tap opens one through the container transform; a long press drops a
 * menu from the cell, Android's counterpart to the iOS context menu.
 */
@Composable
fun PhotoGrid(
    photos: List<Photo>,
    morphScope: String,
    onOpen: (Photo) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    empty: @Composable () -> Unit = {},
    menu: @Composable ColumnScope.(photo: Photo, dismiss: () -> Unit) -> Unit,
) {
    if (photos.isEmpty()) {
        // Consumed, so an empty state's own insets (the keyboard) don't count the bars twice.
        Box(modifier.fillMaxSize().padding(contentPadding).consumeWindowInsets(contentPadding)) {
            empty()
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(photos, key = { it.id }) { photo -> PhotoCell(photo, morphScope, onOpen, menu) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoCell(
    photo: Photo,
    morphScope: String,
    onOpen: (Photo) -> Unit,
    menu: @Composable ColumnScope.(photo: Photo, dismiss: () -> Unit) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    Box(Modifier.aspectRatio(1f).padding(2.dp)) {
        PhotoImage(
            photo.path,
            photo.title,
            Modifier.fillMaxSize()
                .photoMorphEnd(photo.id, morphScope, MorphEnd.Card)
                // No ripple, as in the lab's MorphPhotoCard: the photo itself answers the tap by
                // morphing, and a white wash over it would flash right before the morph starts.
                .combinedClickable(
                    interactionSource = null,
                    indication = null,
                    onClick = { onOpen(photo) },
                    onLongClickLabel = "More options",
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    },
                ),
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            menu(photo) { menuOpen = false }
        }
    }
}
