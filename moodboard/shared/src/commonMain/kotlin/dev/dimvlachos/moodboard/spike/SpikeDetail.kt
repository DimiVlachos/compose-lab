package dev.dimvlachos.moodboard.spike

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import dev.dimvlachos.moodboard.resources.Res
import org.jetbrains.compose.resources.decodeToImageBitmap

/** Spike only: proves a hosted Compose screen draws under native bars and sees their insets. */
@Composable
fun SpikeDetail() {
    var images by remember { mutableStateOf(emptyList<ImageBitmap>()) }
    LaunchedEffect(Unit) {
        images =
            listOf("photo_santorini", "photo_milos", "book_spread_1").map {
                Res.readBytes("files/photos/$it.jpg").decodeToImageBitmap()
            }
    }
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    MaterialTheme {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(
                "safeDrawing top=${insets.calculateTopPadding()} bottom=${insets.calculateBottomPadding()}",
                Modifier.padding(insets).padding(16.dp),
            )
            images.forEach {
                Image(it, null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
            }
        }
    }
}
