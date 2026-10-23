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
    PencilClink("pencil_clink"),
    PencilLift("pencil_lift"),
    PencilPick("pencil_pick"),
    LocketOpen("locket_open"),
    LocketSnap("locket_snap"),
    LocketPlace("locket_place"),
    LocketLift("locket_lift"),
    LocketBack("locket_back"),
    LocketChain("locket_chain");

    /** The clip's path, as `Res.readBytes` takes it. */
    val path: String
        get() = "files/sounds/$file.wav"
}

/**
 * Plays [Sound]s. Clips load in the background, and one asked for before it has loaded is skipped,
 * not queued: a late click is worse than a missing one. On Android they play as media, at the media
 * volume, whatever the ringer is set to; on iOS the ring/silent switch mutes them. Nothing is heard
 * while the app's sound is off ([LocalSoundOn]).
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
     * starts sounding when [sound] has loaded.
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
 * The platform's player, loading only [sounds], and letting them go when it leaves the composition.
 */
@Composable internal expect fun rememberSoundPlayer(sounds: Set<Sound>): SoundPlayer
