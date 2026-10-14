package dev.dimvlachos.lab.core.platform

import androidx.compose.runtime.Composable

/**
 * The short clips the demos play, synthesized by `scripts/synth-sounds.py` into
 * `composeResources/files/sounds/`.
 */
internal enum class Sound(val file: String) {
    Strike1("strike_1"),
    Strike2("strike_2"),
    Strike3("strike_3"),
    Bell("bell"),
    CarriageReturn("carriage_return"),
    RatchetTick("ratchet_tick"),
    SheetRelease("sheet_release"),
    Tape("tape"),
    BurnLoop("burn_loop"),
    MistWhoosh("mist_whoosh"),
    Inhale("inhale");

    /** The clip's path, as `Res.readBytes` takes it. */
    val path: String
        get() = "files/sounds/$file.wav"
}

/**
 * Plays [Sound]s. Clips load in the background, and one asked for before it has loaded is skipped,
 * not queued: a late click is worse than a missing one. Nothing plays while the phone is set to
 * silent or vibrate.
 */
internal interface SoundPlayer {
    /** Plays [sound] once, at [volume] (0 to 1) and [rate] (0.5 to 2, 1 is as recorded). */
    fun play(sound: Sound, volume: Float = 1f, rate: Float = 1f)

    /** Plays [sound] over and over until the returned loop is stopped. */
    fun loop(sound: Sound, volume: Float = 1f): SoundLoop
}

/** A [Sound] playing over and over. */
internal interface SoundLoop {
    fun setVolume(volume: Float)

    fun stop()
}

/** A player that plays nothing: for tests, previews, and before the platform's has been made. */
internal object SilentSoundPlayer : SoundPlayer {
    override fun play(sound: Sound, volume: Float, rate: Float) = Unit

    override fun loop(sound: Sound, volume: Float): SoundLoop = SilentLoop

    private object SilentLoop : SoundLoop {
        override fun setVolume(volume: Float) = Unit

        override fun stop() = Unit
    }
}

/**
 * The platform's player, loading only [sounds], and letting them go when it leaves the composition.
 */
@Composable internal expect fun rememberSoundPlayer(sounds: Set<Sound>): SoundPlayer
