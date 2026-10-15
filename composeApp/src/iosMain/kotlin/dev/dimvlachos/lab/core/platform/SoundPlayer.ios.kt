package dev.dimvlachos.lab.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import dev.dimvlachos.lab.resources.Res
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioEngineConfigurationChangeNotification
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioPlayerNodeBufferInterrupts
import platform.AVFAudio.AVAudioPlayerNodeBufferLoops
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioUnitVarispeed
import platform.AVFAudio.setActive
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.darwin.NSObjectProtocol

@Composable
internal actual fun rememberSoundPlayer(sounds: Set<Sound>): SoundPlayer {
    val player = remember { EngineSoundPlayer() }
    LaunchedEffect(player, sounds) { player.load(sounds) }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}

/**
 * Plays clips through an [AVAudioEngine]. The session is ambient, so the ring/silent switch mutes
 * it and it mixes with music already playing.
 *
 * One-shots take turns on a few voices per clip format, each a player node through a varispeed, so
 * the rate resamples (pitch moves with speed) as SoundPool's does on Android, and clicks overlap
 * when typing fast. A loop gets a voice of its own, which no later sound can take from it.
 *
 * The engine stops when the audio route changes or a call interrupts it; it is started again then,
 * and before anything plays, and the loops that were sounding start over.
 */
@OptIn(ExperimentalForeignApi::class)
internal class EngineSoundPlayer : SoundPlayer {
    private val engine = AVAudioEngine()
    private val clips = mutableMapOf<Sound, AVAudioPCMBuffer>()
    private val voices = mutableMapOf<ClipFormat, VoicePool>()
    private val loops = mutableListOf<EngineLoop>()
    private var released = false
    private val observers: List<NSObjectProtocol>

    init {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryAmbient, error = null)
        session.setActive(true, error = null)
        val center = NSNotificationCenter.defaultCenter
        val queue = NSOperationQueue.mainQueue
        observers =
            listOf(
                center.addObserverForName(
                    AVAudioEngineConfigurationChangeNotification,
                    engine,
                    queue,
                ) { _ ->
                    start()
                },
                center.addObserverForName(AVAudioSessionInterruptionNotification, session, queue) {
                    _ ->
                    start()
                },
            )
    }

    suspend fun load(sounds: Set<Sound>) {
        for (sound in sounds) {
            if (released) return
            if (sound in clips) continue
            val buffer =
                try {
                    pcmBuffer(decodeWave(Res.readBytes(sound.path)))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.w(e) { "Could not load ${sound.file}" }
                    continue
                }
            if (released) return
            voices.getOrPut(ClipFormat(buffer)) { VoicePool(buffer.format) }
            clips[sound] = buffer
        }
        if (clips.isNotEmpty()) start()
    }

    /**
     * Starts the engine if it has stopped, and the loops over if it had, since a stopped engine
     * stops its nodes. Playing on a stopped engine would throw.
     */
    private fun start(): Boolean {
        if (released) return false
        if (engine.running) return true
        AVAudioSession.sharedInstance().setActive(true, error = null)
        engine.prepare()
        if (!engine.startAndReturnError(null)) return false
        loops.forEach { it.restart() }
        return true
    }

    override fun play(sound: Sound, volume: Float, rate: Float) {
        val buffer = clips[sound] ?: return
        val pool = voices[ClipFormat(buffer)] ?: return
        if (!start()) return
        val voice = pool.next()
        voice.varispeed.rate = rate.coerceIn(0.5f, 2f)
        voice.node.volume = volume.coerceIn(0f, 1f)
        voice.node.scheduleBuffer(
            buffer,
            atTime = null,
            options = AVAudioPlayerNodeBufferInterrupts,
            completionHandler = null,
        )
        if (!voice.node.playing) voice.node.play()
    }

    override fun loop(sound: Sound, volume: Float): SoundLoop {
        if (released) return SilentSoundPlayer.loop(sound, volume)
        val buffer = clips[sound] ?: return SilentSoundPlayer.loop(sound, volume)
        return EngineLoop(buffer, volume.coerceIn(0f, 1f)).also {
            loops += it
            if (start()) it.restart()
        }
    }

    fun release() {
        if (released) return
        loops.toList().forEach { it.stop() }
        released = true
        observers.forEach { NSNotificationCenter.defaultCenter.removeObserver(it) }
        engine.stop()
        clips.clear()
        voices.clear()
    }

    /** A loop on a player node of its own, wired straight to the mixer. */
    private inner class EngineLoop(private val buffer: AVAudioPCMBuffer, volume: Float) :
        SoundLoop {
        private val node = AVAudioPlayerNode()

        init {
            engine.attachNode(node)
            engine.connect(node, to = engine.mainMixerNode, format = buffer.format)
            node.volume = volume
        }

        fun restart() {
            node.scheduleBuffer(
                buffer,
                atTime = null,
                options = AVAudioPlayerNodeBufferLoops or AVAudioPlayerNodeBufferInterrupts,
                completionHandler = null,
            )
            node.play()
        }

        override fun setVolume(volume: Float) {
            node.volume = volume.coerceIn(0f, 1f)
        }

        override fun stop() {
            if (!loops.remove(this)) return
            node.stop()
            engine.detachNode(node)
        }
    }

    /** A few voices for clips of one format, taken in turn. */
    private inner class VoicePool(format: AVAudioFormat) {
        private val voices = List(VoicesPerFormat) { Voice(format) }
        private var next = 0

        fun next(): Voice = voices[next].also { next = (next + 1) % voices.size }
    }

    /** A player node through a varispeed, so its rate moves the pitch with the speed. */
    private inner class Voice(format: AVAudioFormat) {
        val node = AVAudioPlayerNode()
        val varispeed = AVAudioUnitVarispeed()

        init {
            engine.attachNode(node)
            engine.attachNode(varispeed)
            engine.connect(node, to = varispeed, format = format)
            engine.connect(varispeed, to = engine.mainMixerNode, format = format)
        }
    }

    /** Voices are wired for one format, so clips of different rates or channels get their own. */
    private data class ClipFormat(val sampleRate: Double, val channels: UInt) {
        constructor(
            buffer: AVAudioPCMBuffer
        ) : this(buffer.format.sampleRate, buffer.format.channelCount)
    }

    private companion object {
        const val VoicesPerFormat = 6
        val log = Logger.withTag("SoundPlayer")
    }
}

/** [clip]'s samples in a buffer the engine can play: 32-bit float, one channel after another. */
@OptIn(ExperimentalForeignApi::class)
private fun pcmBuffer(clip: WaveClip): AVAudioPCMBuffer {
    val format =
        AVAudioFormat(
            standardFormatWithSampleRate = clip.sampleRate.toDouble(),
            channels = clip.channels.toUInt(),
        )
    val buffer =
        checkNotNull(AVAudioPCMBuffer(pCMFormat = format, frameCapacity = clip.frames.toUInt()))
    buffer.frameLength = clip.frames.toUInt()
    val channels = checkNotNull(buffer.floatChannelData)
    for (c in 0 until clip.channels) {
        val channel = checkNotNull(channels[c])
        for (f in 0 until clip.frames) channel[f] = clip.samples[f * clip.channels + c]
    }
    return buffer
}
