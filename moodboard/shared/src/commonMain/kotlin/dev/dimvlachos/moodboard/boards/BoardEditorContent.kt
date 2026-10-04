package dev.dimvlachos.moodboard.boards

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.dimvlachos.moodboard.domain.Photo
import dev.dimvlachos.moodboard.ui.components.PhotoImage

/** Rename a board and pick its photos; every change applies immediately. */
@Composable
fun BoardEditorContent(
    state: BoardEditorState,
    onAction: (BoardEditorAction) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = WindowInsets.safeDrawing.asPaddingValues(),
) {
    // The field owns its text; each edit is sent on, never waited for, so the cursor and an
    // IME composition can't be reset by the round trip through the repository.
    var name by rememberSaveable { mutableStateOf(state.name) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(104.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onAction(BoardEditorAction.Rename(it))
                },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("boardName"),
            )
        }
        items(state.photos, key = { it.id }) { photo ->
            EditorTile(
                photo = photo,
                selected = photo.id in state.selected,
                onToggle = { onAction(BoardEditorAction.Toggle(photo.id)) },
            )
        }
    }
}

/**
 * A photo that shrinks into its tile and shows a filled check when picked, as in Google Photos; a
 * checkbox on top of the photo got lost on bright pictures.
 */
@Composable
private fun EditorTile(photo: Photo, selected: Boolean, onToggle: () -> Unit) {
    val inset by animateDpAsState(if (selected) SelectedInset else 0.dp)
    Box(
        Modifier.padding(2.dp)
            .aspectRatio(1f)
            .clip(TileShape)
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
            )
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() })
            .testTag("photo-${photo.id}")
    ) {
        PhotoImage(
            photo.path,
            photo.title,
            Modifier.fillMaxSize().padding(inset).clip(TileShape),
        )
        CheckMark(selected, Modifier.align(Alignment.TopStart).padding(6.dp))
    }
}

/** A filled circle with a tick when [selected]; an empty ring, legible on any photo, when not. */
@Composable
private fun CheckMark(selected: Boolean, modifier: Modifier = Modifier) {
    val fill = MaterialTheme.colorScheme.primary
    val tick = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier.size(24.dp)) {
        val radius = size.minDimension / 2
        if (selected) {
            drawCircle(fill, radius)
            val path =
                Path().apply {
                    moveTo(size.width * 0.28f, size.height * 0.52f)
                    lineTo(size.width * 0.44f, size.height * 0.67f)
                    lineTo(size.width * 0.73f, size.height * 0.36f)
                }
            drawPath(path, tick, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        } else {
            drawCircle(Color.Black.copy(alpha = 0.25f), radius)
            drawCircle(Color.White, radius - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
        }
    }
}

private val SelectedInset = 10.dp
private val TileShape = RoundedCornerShape(8.dp)
