package dev.dimvlachos.moodboard.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import dev.dimvlachos.moodboard.MoodboardGraph

/** Big enough for a third of a phone's width at 3x density. */
const val GridMaxPixel = 512

/** A full-screen photo, with room to zoom. */
const val DetailMaxPixel = 2048

/** A bundled photo; a neutral tile while it loads or when it's missing. */
@Composable
fun PhotoImage(
    path: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    maxPixel: Int = GridMaxPixel,
) {
    // Keyed on the photo: a new path shows its cached bitmap at once, never the previous photo.
    val bitmap =
        remember(path, maxPixel) {
            mutableStateOf(MoodboardGraph.photoBytes.cached(path, maxPixel))
        }
    LaunchedEffect(path, maxPixel) {
        if (bitmap.value == null) bitmap.value = MoodboardGraph.photoBytes.bitmap(path, maxPixel)
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        bitmap.value?.let {
            Image(it, contentDescription, Modifier.matchParentSize(), contentScale = contentScale)
        }
    }
}
