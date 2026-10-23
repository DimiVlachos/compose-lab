package dev.dimvlachos.lab.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun rememberSoundSettingStore(): SoundSettingStore = remember {
    UserDefaultsSoundSettingStore
}

private object UserDefaultsSoundSettingStore : SoundSettingStore {
    private val defaults
        get() = NSUserDefaults.standardUserDefaults

    // boolForKey is false for a key never set, so the app starts quiet.
    override fun load(): Boolean = defaults.boolForKey(SoundKey)

    override fun save(on: Boolean) {
        defaults.setBool(on, forKey = SoundKey)
    }
}

private const val SoundKey = "sound_on"
