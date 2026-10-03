package dev.dimvlachos.moodboard.android.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.FileProvider
import dev.dimvlachos.moodboard.MoodboardGraph
import dev.dimvlachos.moodboard.domain.Photo
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val Authority = "dev.dimvlachos.moodboard.share"

/** Starts one share at a time: a second tap while the photo is being prepared does nothing. */
class ShareLauncher internal constructor(private val scope: CoroutineScope) {
    private var job: Job? = null

    fun launch(context: Context, photo: Photo) {
        if (job?.isActive == true) return
        job = scope.launch { sharePhoto(context, photo) }
    }
}

@Composable
fun rememberShareLauncher(): ShareLauncher {
    val scope = rememberCoroutineScope()
    return remember(scope) { ShareLauncher(scope) }
}

/** Copies the bundled photo into the cache and hands it to the system share sheet. */
private suspend fun sharePhoto(context: Context, photo: Photo) {
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
            .putExtra(Intent.EXTRA_TITLE, photo.title)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // ClipData gives the share sheet its preview thumbnail.
            .apply { clipData = ClipData.newRawUri(photo.title, uri) }
    context.startActivity(Intent.createChooser(send, photo.title))
}
