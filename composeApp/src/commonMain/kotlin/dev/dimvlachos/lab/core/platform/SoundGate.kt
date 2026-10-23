package dev.dimvlachos.lab.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

/**
 * Whether the players under it may sound: a demo screen's own sound switch, which starts from the
 * app's, or always on while a demo is recorded as a clip. It is read as it changes, so a loop
 * already running goes quiet, or comes back, the moment the switch is flipped.
 */
internal val LocalSoundOn = compositionLocalOf { true }

/**
 * [player], heard only while [LocalSoundOn] is on: a one-shot asked for while it is off is skipped,
 * and a loop keeps running at no volume, so it picks up where it is when sound comes back.
 */
@Composable
internal fun gate(player: SoundPlayer): SoundPlayer {
    val on = LocalSoundOn.current
    val gated = remember(player) { GatedSoundPlayer(player, on) }
    SideEffect { gated.on = on }
    return gated
}

/** [inner], let through only while [on]. */
internal class GatedSoundPlayer(private val inner: SoundPlayer, on: Boolean) : SoundPlayer {
    private val loops = mutableSetOf<GatedLoop>()

    var on = on
        set(value) {
            if (field == value) return
            field = value
            loops.forEach { it.apply() }
        }

    override fun play(sound: Sound, volume: Float, rate: Float) {
        if (on) inner.play(sound, volume, rate)
    }

    override fun loop(sound: Sound, volume: Float): SoundLoop =
        GatedLoop(inner.loop(sound, if (on) volume else 0f), volume).also { loops += it }

    private inner class GatedLoop(private val loop: SoundLoop, private var volume: Float) :
        SoundLoop {
        fun apply() = loop.setVolume(if (on) volume else 0f)

        override fun setVolume(volume: Float) {
            this.volume = volume
            apply()
        }

        override fun stop() {
            loops -= this
            loop.stop()
        }
    }
}
