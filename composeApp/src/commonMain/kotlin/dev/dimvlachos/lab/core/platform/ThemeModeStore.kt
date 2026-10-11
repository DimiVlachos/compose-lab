package dev.dimvlachos.lab.core.platform

import androidx.compose.runtime.Composable
import dev.dimvlachos.lab.core.presentation.ui.ThemeMode

/** Where the chosen theme mode is kept between launches. */
internal interface ThemeModeStore {
    /** The saved mode, or [ThemeMode.System] when none has been saved. */
    fun load(): ThemeMode

    fun save(mode: ThemeMode)
}

/** A store that keeps the mode only as long as it lives: for tests and previews. */
internal class InMemoryThemeModeStore(private var mode: ThemeMode = ThemeMode.System) :
    ThemeModeStore {
    override fun load(): ThemeMode = mode

    override fun save(mode: ThemeMode) {
        this.mode = mode
    }
}

/** The platform's store for the theme mode: the app's preferences, kept on the device. */
@Composable internal expect fun rememberThemeModeStore(): ThemeModeStore
