package dev.dimvlachos.lab.core.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger
import dev.dimvlachos.lab.resources.Res
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal actual fun rememberSoundPlayer(sounds: Set<Sound>): SoundPlayer {
    val context = LocalContext.current.applicationContext
    val player = remember(context) { SoundPoolPlayer(context) }
    LaunchedEffect(player, sounds) { player.load(sounds) }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}

/**
 * Plays clips through a [SoundPool]. The pool loads from files, so each clip's bytes are copied out
 * of the app's resources into the cache once, the first time it is wanted.
 */
private class SoundPoolPlayer(private val context: Context) : SoundPlayer {
    private val pool =
        SoundPool.Builder()
            .setMaxStreams(MaxStreams)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    private val audio = context.getSystemService(AudioManager::class.java)
    private val ids = mutableMapOf<Sound, Int>()
    private val ready = mutableSetOf<Int>()

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready += id }
    }

    suspend fun load(sounds: Set<Sound>) {
        for (sound in sounds) {
            if (sound in ids) continue
            val file =
                try {
                    withContext(Dispatchers.IO) { cacheFile(sound) }
                } catch (e: Exception) {
                    log.w(e) { "Could not load ${sound.file}" }
                    continue
                }
            ids[sound] = pool.load(file.path, 1)
        }
    }

    private suspend fun cacheFile(sound: Sound): File {
        val file = File(context.cacheDir, "sounds/${sound.file}.wav")
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.writeBytes(Res.readBytes(sound.path))
        }
        return file
    }

    /** Silent and vibrate both mean the person has asked the phone to keep quiet. */
    private val audible: Boolean
        get() = audio?.ringerMode == AudioManager.RINGER_MODE_NORMAL

    private fun readyId(sound: Sound): Int? = ids[sound]?.takeIf { it in ready && audible }

    override fun play(sound: Sound, volume: Float, rate: Float) {
        val id = readyId(sound) ?: return
        val v = volume.coerceIn(0f, 1f)
        pool.play(id, v, v, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    override fun loop(sound: Sound, volume: Float): SoundLoop {
        val id = readyId(sound) ?: return SilentSoundPlayer.loop(sound, volume)
        val v = volume.coerceIn(0f, 1f)
        val stream = pool.play(id, v, v, 1, -1, 1f)
        return object : SoundLoop {
            override fun setVolume(volume: Float) {
                val w = volume.coerceIn(0f, 1f)
                pool.setVolume(stream, w, w)
            }

            override fun stop() = pool.stop(stream)
        }
    }

    fun release() = pool.release()

    private companion object {
        const val MaxStreams = 8
        val log = Logger.withTag("SoundPlayer")
    }
}
