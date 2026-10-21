package dev.dimvlachos.lab.core.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import co.touchlab.kermit.Logger
import dev.dimvlachos.lab.resources.Res
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal actual fun rememberSoundPlayer(sounds: Set<Sound>): SoundPlayer {
    val context = LocalContext.current.applicationContext
    val capturable = LocalSoundCapturable.current
    val player = remember(context, capturable) { SoundPoolPlayer(context, capturable) }
    LaunchedEffect(player, sounds) { player.load(sounds) }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}

/**
 * Plays clips through a [SoundPool], as a game's sounds when [capturable] so a screen recorder can
 * take them ([LocalSoundCapturable]). Loops are [LiveLoop]s, started once their clip has loaded and
 * paused while the ringer is on silent or vibrate, as [ringer] reports it.
 *
 * The pool loads from files, so each clip's bytes are copied out of the app's resources into the
 * cache, under a name that follows the bytes ([soundCacheName]), so a clip changed by an update
 * gets a fresh copy. A copy is written beside its name and moved into place, so one cut short by
 * the app being killed is never played.
 */
private class SoundPoolPlayer(private val context: Context, capturable: Boolean) : SoundPlayer {
    private val pool =
        SoundPool.Builder()
            .setMaxStreams(MaxStreams)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(
                        if (capturable) AudioAttributes.USAGE_GAME
                        else AudioAttributes.USAGE_ASSISTANCE_SONIFICATION,
                    )
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
    private val audio = context.getSystemService(AudioManager::class.java)
    private val ids = mutableMapOf<Sound, Int>()
    private val ready = mutableSetOf<Int>()
    private val loops = mutableMapOf<LiveLoop, Sound>()
    private var released = false

    /**
     * Silent and vibrate both mean the person has asked the phone to keep quiet. Kept up to date by
     * [ringer] rather than asked for on every click, since asking is a call to another process.
     */
    private var audible = audio?.ringerMode == AudioManager.RINGER_MODE_NORMAL

    private val ringer =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val mode =
                    intent.getIntExtra(
                        AudioManager.EXTRA_RINGER_MODE,
                        AudioManager.RINGER_MODE_NORMAL,
                    )
                audible = mode == AudioManager.RINGER_MODE_NORMAL
                loops.keys.forEach { it.audible = audible }
            }
        }

    init {
        pool.setOnLoadCompleteListener { _, id, status ->
            if (status != 0) return@setOnLoadCompleteListener
            ready += id
            loops.forEach { (loop, sound) -> if (ids[sound] == id) loop.ready = true }
        }
        ContextCompat.registerReceiver(
            context,
            ringer,
            IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    suspend fun load(sounds: Set<Sound>) {
        for (sound in sounds) {
            if (released) return
            if (sound in ids) continue
            val file =
                try {
                    withContext(Dispatchers.IO) { cacheFile(sound) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.w(e) { "Could not load ${sound.file}" }
                    continue
                }
            if (released) return
            ids[sound] = pool.load(file.path, 1)
        }
    }

    private suspend fun cacheFile(sound: Sound): File {
        val bytes = Res.readBytes(sound.path)
        val dir = File(context.cacheDir, "sounds").apply { mkdirs() }
        val name = soundCacheName(sound, bytes)
        val file = File(dir, name)
        if (!file.exists()) {
            val part = File.createTempFile(sound.file, ".part", dir)
            try {
                part.writeBytes(bytes)
                check(part.renameTo(file) || file.exists()) { "Could not move $part to $file" }
            } finally {
                part.delete()
            }
            dir.listFiles { f -> isStaleSoundCopy(sound, f.name, name) }?.forEach { it.delete() }
        }
        return file
    }

    private fun readyId(sound: Sound): Int? = ids[sound]?.takeIf { it in ready }

    override fun play(sound: Sound, volume: Float, rate: Float) {
        if (!audible) return
        val id = readyId(sound) ?: return
        val v = volume.coerceIn(0f, 1f)
        pool.play(id, v, v, OneShotPriority, 0, rate.coerceIn(0.5f, 2f))
    }

    override fun loop(sound: Sound, volume: Float): SoundLoop {
        if (released) return SilentSoundPlayer.loop(sound, volume)
        val loop = LiveLoop(StreamVoice(sound), volume, onStop = { loops -= it })
        loops[loop] = sound
        loop.audible = audible
        loop.ready = readyId(sound) != null
        return loop
    }

    fun release() {
        if (released) return
        loops.keys.toList().forEach { it.stop() }
        released = true
        context.unregisterReceiver(ringer)
        pool.release()
    }

    /** A looping stream of [sound]. It outranks one-shots, so a burst of clicks never takes it. */
    private inner class StreamVoice(private val sound: Sound) : LoopVoice {
        private var stream = 0

        override fun start(volume: Float) {
            val id = ids[sound] ?: return
            if (stream != 0) pool.stop(stream)
            stream = pool.play(id, volume, volume, LoopPriority, -1, 1f)
        }

        override fun setVolume(volume: Float) = pool.setVolume(stream, volume, volume)

        override fun pause() = pool.pause(stream)

        override fun resume() = pool.resume(stream)

        override fun stop() = pool.stop(stream)
    }

    private companion object {
        const val MaxStreams = 8
        const val OneShotPriority = 1
        const val LoopPriority = 2
        val log = Logger.withTag("SoundPlayer")
    }
}
