package dev.dimvlachos.lab.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import dev.dimvlachos.lab.resources.Res
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.setActive
import platform.Foundation.NSData
import platform.Foundation.create

@Composable
internal actual fun rememberSoundPlayer(sounds: Set<Sound>): SoundPlayer {
    val player = remember { AvSoundPlayer() }
    LaunchedEffect(player, sounds) { player.load(sounds) }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}

/**
 * Plays clips through [AVAudioPlayer]s. The session is ambient, so the ring/silent switch mutes it
 * and it mixes with music already playing. A player plays one sound at a time, so each clip gets a
 * few, taken in turn, for clicks that overlap when typing fast.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class AvSoundPlayer : SoundPlayer {
    private val voices = mutableMapOf<Sound, List<AVAudioPlayer>>()
    private val next = mutableMapOf<Sound, Int>()
    private var released = false

    init {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryAmbient, error = null)
        session.setActive(true, error = null)
    }

    suspend fun load(sounds: Set<Sound>) {
        for (sound in sounds) {
            if (released) return
            if (sound in voices) continue
            val bytes =
                try {
                    Res.readBytes(sound.path)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.w(e) { "Could not load ${sound.file}" }
                    continue
                }
            if (released) return
            val data = bytes.usePinned {
                NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
            }
            voices[sound] =
                List(VoicesPerSound) {
                    AVAudioPlayer(data = data, error = null).apply {
                        enableRate = true
                        prepareToPlay()
                    }
                }
        }
    }

    private fun voice(sound: Sound): AVAudioPlayer? {
        val list = voices[sound] ?: return null
        val i = next[sound] ?: 0
        next[sound] = (i + 1) % list.size
        return list[i]
    }

    override fun play(sound: Sound, volume: Float, rate: Float) {
        val player = voice(sound) ?: return
        player.stop()
        player.currentTime = 0.0
        player.volume = volume.coerceIn(0f, 1f)
        player.rate = rate.coerceIn(0.5f, 2f)
        player.numberOfLoops = 0
        player.play()
    }

    override fun loop(sound: Sound, volume: Float): SoundLoop {
        val player = voice(sound) ?: return SilentSoundPlayer.loop(sound, volume)
        player.currentTime = 0.0
        player.volume = volume.coerceIn(0f, 1f)
        player.rate = 1f
        player.numberOfLoops = -1
        player.play()
        return object : SoundLoop {
            override fun setVolume(volume: Float) {
                player.volume = volume.coerceIn(0f, 1f)
            }

            override fun stop() = player.stop()
        }
    }

    fun release() {
        released = true
        voices.values.flatten().forEach { it.stop() }
        voices.clear()
    }

    private companion object {
        const val VoicesPerSound = 3
        val log = Logger.withTag("SoundPlayer")
    }
}
