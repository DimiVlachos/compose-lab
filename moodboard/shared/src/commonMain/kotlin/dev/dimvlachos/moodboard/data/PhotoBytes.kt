package dev.dimvlachos.moodboard.data

import androidx.compose.ui.graphics.ImageBitmap
import co.touchlab.kermit.Logger
import dev.dimvlachos.moodboard.resources.Res
import org.jetbrains.compose.resources.decodeToImageBitmap

/** Reads bundled photos; decoded bitmaps are kept for the Compose screens. */
class PhotoBytes {
    private val bitmaps = LruCache<String, ImageBitmap>(capacity = 40)

    suspend fun bytes(path: String): ByteArray? =
        runCatching { Res.readBytes(path) }
            .onFailure { Logger.w(it) { "Missing photo $path" } }
            .getOrNull()

    suspend fun bitmap(path: String): ImageBitmap? =
        bitmaps[path] ?: bytes(path)?.decodeToImageBitmap()?.also { bitmaps[path] = it }
}
