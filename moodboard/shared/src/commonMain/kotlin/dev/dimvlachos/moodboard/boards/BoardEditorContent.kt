package dev.dimvlachos.moodboard.boards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.dimvlachos.moodboard.ui.components.PhotoImage

private const val UnselectedAlpha = 0.55f

/** Rename a board and pick its photos; every change applies immediately. */
@Composable
fun BoardEditorContent(
    state: BoardEditorState,
    onAction: (BoardEditorAction) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = WindowInsets.safeDrawing.asPaddingValues(),
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(104.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { onAction(BoardEditorAction.Rename(it)) },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("boardName"),
            )
        }
        items(state.photos, key = { it.id }) { photo ->
            val selected = photo.id in state.selected
            Box(
                Modifier.padding(2.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAction(BoardEditorAction.Toggle(photo.id)) }
                    .testTag("photo-${photo.id}")
            ) {
                PhotoImage(
                    photo.path,
                    photo.title,
                    Modifier.fillMaxSize().graphicsLayer {
                        alpha = if (selected) 1f else UnselectedAlpha
                    },
                )
                Checkbox(
                    checked = selected,
                    onCheckedChange = null,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}
