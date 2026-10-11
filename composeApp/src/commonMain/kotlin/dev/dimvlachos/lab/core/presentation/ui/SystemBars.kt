package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * What the screen on show asks of the system bars' icons: dark (`true`) or light (`false`), or null
 * to leave them to the theme, dark on the light palette and light on the dark one. A demo whose
 * stage stays light or dark whatever the theme asks through [RequestSystemBarIcons].
 */
@Stable
internal class SystemBarRequests {
    /**
     * Dark icons on the navigation bar (or gesture handle), light ones, or null for the theme's.
     */
    var navigationDarkIcons: Boolean? by mutableStateOf(null)

    /** Dark icons on the status bar, light ones, or null for the theme's. */
    var statusDarkIcons: Boolean? by mutableStateOf(null)
}

/** The requests the app applies to the system bars; one set, provided by the app. */
internal val LocalSystemBarRequests = staticCompositionLocalOf { SystemBarRequests() }

/**
 * Asks for dark (`true`) or light (`false`) icons on the navigation bar and the status bar while
 * this is in the composition; null leaves that bar alone. When it leaves, the bars go back to what
 * was asked before it.
 */
@Composable
internal fun RequestSystemBarIcons(
    navigationDarkIcons: Boolean? = null,
    statusDarkIcons: Boolean? = null,
) {
    val requests = LocalSystemBarRequests.current
    DisposableEffect(requests, navigationDarkIcons, statusDarkIcons) {
        val navigationBefore = requests.navigationDarkIcons
        val statusBefore = requests.statusDarkIcons
        if (navigationDarkIcons != null) requests.navigationDarkIcons = navigationDarkIcons
        if (statusDarkIcons != null) requests.statusDarkIcons = statusDarkIcons
        onDispose {
            if (navigationDarkIcons != null) requests.navigationDarkIcons = navigationBefore
            if (statusDarkIcons != null) requests.statusDarkIcons = statusBefore
        }
    }
}

/** Whether a bar's icons are dark: as [request]ed, or else dark on the light palette. */
internal fun darkBarIcons(request: Boolean?, dark: Boolean): Boolean = request ?: !dark

/**
 * Sets the window around the app to the theme: the system bars' icons, dark or light as [dark] and
 * [requests] say, and on each platform the window's own light or dark style for [mode], so the
 * system's pieces (the launch screen, the keyboard) match. Not used while recording.
 */
@Composable
internal expect fun SystemAppearance(mode: ThemeMode, dark: Boolean, requests: SystemBarRequests)
