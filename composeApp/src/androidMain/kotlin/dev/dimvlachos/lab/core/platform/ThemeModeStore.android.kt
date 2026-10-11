package dev.dimvlachos.lab.core.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.dimvlachos.lab.core.presentation.ui.ThemeMode

@Composable
internal actual fun rememberThemeModeStore(): ThemeModeStore {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        PreferencesThemeModeStore(
            context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        )
    }
}

private class PreferencesThemeModeStore(private val preferences: SharedPreferences) :
    ThemeModeStore {
    override fun load(): ThemeMode = ThemeMode.fromKey(preferences.getString(ModeKey, null))

    override fun save(mode: ThemeMode) {
        preferences.edit().putString(ModeKey, mode.key).apply()
    }
}

private const val PreferencesName = "lab_settings"
private const val ModeKey = "theme_mode"
