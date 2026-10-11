package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.dimvlachos.lab.core.platform.ThemeModeStore

/** Which palette the app wears: the system's choice, or always light, or always dark. */
internal enum class ThemeMode(
    /** The name it is saved under, which stays the same if the entries are renamed. */
    internal val key: String
) {
    System("system"),
    Light("light"),
    Dark("dark");

    /** The mode the theme button moves on to: System, then Light, then Dark, then round again. */
    fun next(): ThemeMode = entries[(ordinal + 1) % entries.size]

    /** Whether this mode is dark, when the system's own theme is dark or not. */
    fun isDark(systemDark: Boolean): Boolean =
        when (this) {
            System -> systemDark
            Light -> false
            Dark -> true
        }

    companion object {
        /** The mode saved under [key]; System when nothing was saved, or the key is unknown. */
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: System
    }
}

/**
 * Whether the app is dark: as [mode] says, given whether the system is dark, except while a clip is
 * being recorded ([record]), which is always dark so every clip looks the same.
 */
internal fun useDarkTheme(mode: ThemeMode, systemDark: Boolean, record: Boolean): Boolean =
    record || mode.isDark(systemDark)

/**
 * Whether the theme button's next switch from [mode] changes how the app looks, given whether the
 * system is dark, and so is worth spreading over the screen: from following a light system to
 * light, say, it doesn't. Never while recording, which is always dark.
 */
internal fun switchShows(mode: ThemeMode, systemDark: Boolean, record: Boolean): Boolean =
    !record && mode.next().isDark(systemDark) != mode.isDark(systemDark)

/**
 * The theme mode the app is in, read from [store] when it is made, and saved back to it each time
 * [cycle] moves it on.
 */
@Stable
internal class ThemeController(private val store: ThemeModeStore) {
    var mode by mutableStateOf(store.load())
        private set

    /** Moves on to the next mode and saves it. */
    fun cycle() {
        mode = mode.next()
        store.save(mode)
    }
}
