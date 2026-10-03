package dev.dimvlachos.moodboard.android.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
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
    onOpen: (Photo) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    menu: @Composable ColumnScope.(photo: Photo, dismiss: () -> Unit) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(photos, key = { it.id }) { photo -> PhotoCell(photo, onOpen, menu) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoCell(
    photo: Photo,
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
                .photoMorphEnd(photo.id, MorphEnd.Card)
                .combinedClickable(
                    onClick = { onOpen(photo) },
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
