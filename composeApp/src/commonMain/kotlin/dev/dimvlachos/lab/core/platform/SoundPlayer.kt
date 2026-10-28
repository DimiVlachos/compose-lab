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
    LocketChain("locket_chain"),
    BirdcageChirp("birdcage_chirp"),
    BirdcageSettle("birdcage_settle"),
    BirdcageLift("birdcage_lift"),
    BirdcageRustle("birdcage_rustle"),
    BirdcageSwish("birdcage_swish"),
    ZipClack("zip_clack"),
    ZipTick("zip_tick"),
    ZipTeeth("zip_teeth"),
    ZipLift("zip_lift"),
    KeyTink("key_tink"),
    KeyClack("key_clack"),
    KeyLift("key_lift"),
    PaperFanFwap("paperfan_fwap"),
    PaperFanClick("paperfan_click"),
    PaperFanSwish("paperfan_swish"),
    PaperFanClack("paperfan_clack"),
    NoteGrain("note_grain"),
    NoteTick("note_tick"),
    NoteRip("note_rip"),
    NoteStick("note_stick"),
    NotePat("note_pat"),
    NoteLift("note_lift"),
    RollerDip("roller_dip"),
    RollerSet("roller_set"),
    RollerRoll("roller_roll"),
    PressPour("press_pour"),
    PressRise("press_rise"),
    PressTick("press_tick"),
    PressTink("press_tink"),
    PressGlug("press_glug"),
    PressLift("press_lift"),
    PressHiss("press_hiss"),
    CopierRise("copier_rise"),
    CopierLidUp("copier_lidup"),
    CopierThunk("copier_thunk"),
    CopierSlap("copier_slap"),
    CopierBeep("copier_beep"),
    CopierHum("copier_hum"),
    CopierPaper("copier_paper"),
    CopierFeed("copier_feed"),
    CopierLift("copier_lift"),
    CopierSettle("copier_settle"),
    CopierTick("copier_tick"),
    PigeonholeLift("pigeonhole_lift"),
    PigeonholeFold("pigeonhole_fold"),
    PigeonholeSlide("pigeonhole_slide"),
    PigeonholeTock("pigeonhole_tock"),
    PigeonholeRack("pigeonhole_rack"),
    PigeonholeTick("pigeonhole_tick"),
    CrtOff("crt_off"),
    CrtFly("crt_fly"),
    CrtOn("crt_on"),
    BandSend("band_send"),
    BandGrab("band_grab"),
    BandTaut("band_taut"),
    BandBoing("band_boing"),
    BandYank("band_yank"),
    BandBurst("band_burst"),
    BandTick("band_tick"),
    BandTwang("band_twang"),
    CoinClink("coin_clink"),
    CoinRattle("coin_rattle"),
    CoinTick("coin_tick"),
    CoinTip("coin_tip"),
    CoinOut("coin_out"),
    CoinThunk("coin_thunk"),
    DecoderTick("decoder_tick"),
    DecoderSettle("decoder_settle"),
    DecoderDone("decoder_done"),
    DecoderBack("decoder_back"),
    GraveTick("grave_tick"),
    GraveSwipe("grave_swipe"),
    GraveThud("grave_thud"),
    GraveCrumble("grave_crumble"),
    GraveOoo("grave_ooo"),
    GraveClick("grave_click"),
    GraveLift("grave_lift");

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
