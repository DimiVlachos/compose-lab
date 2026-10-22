package dev.dimvlachos.lab.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

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
    Inhale("inhale"),
    GlobeSwish("globe_swish"),
    MusicBoxPhrase("music_box_phrase"),
    CardTick("card_tick"),
    BookSlide("book_slide"),
    BookOpen("book_open"),
    RingTick("ring_tick"),
    PageTurn("page_turn"),
    PageLand("page_land"),
    BagDrop("bag_drop"),
    BagLift("bag_lift"),
    BagTip("bag_tip"),
    StickerPeel("sticker_peel"),
    StickerPress("sticker_press"),
    StickerSettle("sticker_settle"),
    TinTunk("tin_tunk"),
    FlagTwang("flag_twang"),
    DoorCreak("door_creak"),
    EnvelopeSlip("envelope_slip"),
    MailTick("mail_tick"),
    PadlockClunk("padlock_clunk"),
    PadlockPop("padlock_pop"),
    PadlockRattle("padlock_rattle"),
    PadlockSwing("padlock_swing"),
    KeyTink("key_tink"),
    KeyClack("key_clack"),
    KeyLift("key_lift"),
    ClipSnap("clip_snap");

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
    /**
     * Plays [sound] once, at [volume] (0 to 1) and [rate] (0.5 to 2, 1 is as recorded). The rate
     * resamples the clip on both platforms, like a tape run faster or slower: its pitch moves with
     * its speed.
     */
    fun play(sound: Sound, volume: Float = 1f, rate: Float = 1f)

    /**
     * Plays [sound] over and over until the returned loop is stopped. The loop is live at once: it
     * starts sounding when [sound] has loaded, and keeps quiet while the phone is set to silent.
     */
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
 * Whether a screen recorder may capture what the players under it play: true while a demo is
 * recorded as a clip. On Android, an app's own capture only takes media and game sounds, so a
 * recorded demo's clicks go out as a game's; otherwise they are the system's sonification.
 */
internal val LocalSoundCapturable = staticCompositionLocalOf { false }

/**
 * The platform's player, loading only [sounds], and letting them go when it leaves the composition.
 */
@Composable internal expect fun rememberSoundPlayer(sounds: Set<Sound>): SoundPlayer
