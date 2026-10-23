package dev.dimvlachos.lab.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Where the app's sound switch is kept between launches. */
internal interface SoundSettingStore {
    /** Whether sound is on: off when nothing has been saved, so the app starts quiet. */
    fun load(): Boolean

    fun save(on: Boolean)
}

/** A store that keeps the switch only as long as it lives: for tests and previews. */
internal class InMemorySoundSettingStore(private var on: Boolean = false) : SoundSettingStore {
    override fun load(): Boolean = on

    override fun save(on: Boolean) {
        this.on = on
    }
}

/**
 * The app's sound switch, the one on the home screen: read from [store] and saved back to it on
 * every [toggle]. Each demo starts its own screen's switch from it.
 */
internal class SoundSetting(private val store: SoundSettingStore) {
    var on by mutableStateOf(store.load())
        private set

    fun toggle() {
        on = !on
        store.save(on)
    }
}

/** The platform's store for the sound switch: the app's preferences, kept on the device. */
@Composable internal expect fun rememberSoundSettingStore(): SoundSettingStore
