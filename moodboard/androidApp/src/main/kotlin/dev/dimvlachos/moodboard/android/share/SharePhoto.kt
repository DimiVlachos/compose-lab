package dev.dimvlachos.moodboard.android.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dev.dimvlachos.moodboard.MoodboardGraph
import dev.dimvlachos.moodboard.domain.Photo
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val Authority = "dev.dimvlachos.moodboard.share"

/** Copies the bundled photo into the cache and hands it to the system share sheet. */
suspend fun sharePhoto(context: Context, photo: Photo) {
    val bytes = MoodboardGraph.photoBytes.bytes(photo.path) ?: return
    val file =
        withContext(Dispatchers.IO) {
            File(context.cacheDir, "shared")
                .apply { mkdirs() }
                .resolve("${photo.id}.jpg")
                .also {
                    it.writeBytes(bytes)
                }
        }
    val uri = FileProvider.getUriForFile(context, Authority, file)
    val send =
        Intent(Intent.ACTION_SEND)
            .setType("image/jpeg")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, photo.title))
}
