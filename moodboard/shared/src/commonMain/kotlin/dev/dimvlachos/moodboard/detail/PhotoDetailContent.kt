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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.dimvlachos.moodboard.domain.Photo
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
    chromeModifier: Modifier = Modifier,
    contentPadding: PaddingValues = WindowInsets.safeDrawing.asPaddingValues(),
) {
    // Once deleted, the screen leaves on the next frame; it keeps showing the photo on the way out
    // instead of going blank. A plain holder, not snapshot state: nothing should recompose on it.
    val last = remember { LastPhoto() }
    state.photo?.let { last.photo = it }
    val photo = last.photo ?: return
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // The headline below names the photo; the image itself stays unlabelled.
        ZoomablePhoto(photo.path, imageModifier)
        // chromeModifier lets a platform stage the text around the photo's transition.
        Column(
            chromeModifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                photo.title,
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
            )
            if (photo.tags.isNotEmpty()) LabelSection("Tags", photo.tags.sorted())
            val boards = state.boards.filter { photo.id in it.photoIds }.map { it.name }
            if (boards.isNotEmpty()) LabelSection("Boards", boards)
        }
    }
}

/** A small heading over a row of pills. Plain labels: there is nothing to tap here. */
@Composable
private fun LabelSection(title: String, labels: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            labels.forEach { label ->
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(
                        label,
                        Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

private class LastPhoto {
    var photo: Photo? = null
}

/**
 * Pinch to zoom up to 4x and pan while zoomed, never past the photo's own edges; double-tap toggles
 * 2.5x.
 */
@Composable
private fun ZoomablePhoto(path: String, modifier: Modifier) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, MaxZoom)
        offset = if (scale > 1f) (offset + pan).clampedTo(size, scale) else Offset.Zero
    }
    PhotoImage(
        path = path,
        contentDescription = null,
        maxPixel = DetailMaxPixel,
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .onSizeChanged { size = it }
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

/** The farthest a photo scaled by [scale] about its centre can move before showing an edge. */
private fun Offset.clampedTo(size: IntSize, scale: Float): Offset {
    val maxX = size.width * (scale - 1f) / 2f
    val maxY = size.height * (scale - 1f) / 2f
    return Offset(x.coerceIn(-maxX, maxX), y.coerceIn(-maxY, maxY))
}
