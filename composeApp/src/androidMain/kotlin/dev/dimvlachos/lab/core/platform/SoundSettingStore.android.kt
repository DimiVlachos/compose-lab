package dev.dimvlachos.lab.core.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberSoundSettingStore(): SoundSettingStore {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        PreferencesSoundSettingStore(
            context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE),
        )
    }
}

private class PreferencesSoundSettingStore(private val preferences: SharedPreferences) :
    SoundSettingStore {
    override fun load(): Boolean = preferences.getBoolean(SoundKey, false)

    override fun save(on: Boolean) {
        preferences.edit().putBoolean(SoundKey, on).apply()
    }
}

// The same file as the theme mode's.
private const val PreferencesName = "lab_settings"
private const val SoundKey = "sound_on"
