package dev.dimvlachos.lab.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.dimvlachos.lab.core.presentation.ui.ThemeMode
import platform.Foundation.NSUserDefaults

@Composable
internal actual fun rememberThemeModeStore(): ThemeModeStore = remember {
    UserDefaultsThemeModeStore
}

private object UserDefaultsThemeModeStore : ThemeModeStore {
    private val defaults
        get() = NSUserDefaults.standardUserDefaults

    override fun load(): ThemeMode = ThemeMode.fromKey(defaults.stringForKey(ModeKey))

    override fun save(mode: ThemeMode) {
        defaults.setObject(mode.key, forKey = ModeKey)
    }
}

private const val ModeKey = "theme_mode"
