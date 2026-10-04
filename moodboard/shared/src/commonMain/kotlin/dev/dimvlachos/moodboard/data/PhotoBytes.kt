package dev.dimvlachos.moodboard.data

import androidx.compose.ui.graphics.ImageBitmap
import co.touchlab.kermit.Logger
import dev.dimvlachos.moodboard.resources.Res
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads bundled photos. Reads and decodes run on [background] (compose-resources reads
 * synchronously); the bitmap cache is touched only on the caller's thread, which is main.
 */
class PhotoBytes(
    private val background: CoroutineDispatcher = Dispatchers.Default,
    private val read: suspend (String) -> ByteArray = { Res.readBytes(it) },
) {
    private val bitmaps = LruCache<String, ImageBitmap>(capacity = 40)

    suspend fun bytes(path: String): ByteArray? = withContext(background) { readOrNull(path) }

    /** A bitmap already decoded at this size, so a returning screen draws it on its first frame. */
    fun cached(path: String, maxPixel: Int): ImageBitmap? = bitmaps["$path@$maxPixel"]

    /**
     * The bitmap at [maxPixel], or else the same photo at the [smaller] size: a detail opening from
     * the grid starts with the very pixels its card showed, never an empty frame.
     */
    fun cachedOrSmaller(path: String, maxPixel: Int, smaller: Int): ImageBitmap? =
        cached(path, maxPixel) ?: cached(path, smaller)

    /** Decoded so its longer side is at most [maxPixel]: grid cells never hold full images. */
    suspend fun bitmap(path: String, maxPixel: Int): ImageBitmap? {
        cached(path, maxPixel)?.let {
            return it
        }
        val key = "$path@$maxPixel"
        val decoded =
            withContext(background) { readOrNull(path)?.let { decodeSampled(it, maxPixel) } }
        return decoded?.also { bitmaps[key] = it }
    }

    private suspend fun readOrNull(path: String): ByteArray? =
        try {
            read(path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "Missing photo $path" }
            null
        }
}
