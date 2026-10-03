package dev.dimvlachos.moodboard.detail

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.dimvlachos.moodboard.ui.components.DetailMaxPixel
import dev.dimvlachos.moodboard.ui.components.PhotoImage

private const val MaxZoom = 4f
private const val DoubleTapZoom = 2.5f

/**
 * The photo, its tags and the boards holding it. No chrome: each platform puts its own bars around
 * it. [imageModifier] lets Android attach its shared-element morph to the photo.
 */
@Composable
fun PhotoDetailContent(
    state: PhotoDetailState,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    contentPadding: PaddingValues = WindowInsets.safeDrawing.asPaddingValues(),
) {
    // Once deleted, the screen leaves on the next frame; it keeps showing the photo on the way out
    // instead of going blank.
    var shown by remember { mutableStateOf(state.photo) }
    state.photo?.let { shown = it }
    val photo = shown ?: return
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ZoomablePhoto(photo.path, photo.title, imageModifier)
        Column(
            Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(photo.title, style = MaterialTheme.typography.headlineMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                photo.tags.sorted().forEach { AssistChip(onClick = {}, label = { Text(it) }) }
            }
            state.boards
                .filter { photo.id in it.photoIds }
                .forEach { Text("In ${it.name}", style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

/** Pinch to zoom up to 4x and pan while zoomed; double-tap toggles 2.5x. */
@Composable
private fun ZoomablePhoto(path: String, title: String, modifier: Modifier) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, MaxZoom)
        offset = if (scale > 1f) offset + pan else Offset.Zero
    }
    PhotoImage(
        path = path,
        contentDescription = title,
        maxPixel = DetailMaxPixel,
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            scale = if (scale > 1f) 1f else DoubleTapZoom
                            offset = Offset.Zero
                        }
                    )
                }
                .transformable(transform, canPan = { scale > 1f })
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
    )
}
