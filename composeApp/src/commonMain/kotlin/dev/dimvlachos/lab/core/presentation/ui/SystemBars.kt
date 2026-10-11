package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * What the screens on show ask of the system bars' icons: dark (`true`) or light (`false`), or null
 * to leave them to the theme, dark on the light palette and light on the dark one. A demo whose
 * stage stays light or dark whatever the theme asks through [RequestSystemBarIcons].
 *
 * Each request is kept until its screen leaves, and the latest one to ask about a bar decides it.
 * Requests can end in any order: as one screen slides off under the next, the leaving screen's
 * request ends after the arriving one's has begun.
 */
@Stable
internal class SystemBarRequests {
    private val requests = mutableStateListOf<Request>()

    /**
     * Dark icons on the navigation bar (or gesture handle), light ones, or null for the theme's.
     */
    val navigationDarkIcons: Boolean?
        get() = requests.lastOrNull { it.navigationDarkIcons != null }?.navigationDarkIcons

    /** Dark icons on the status bar, light ones, or null for the theme's. */
    val statusDarkIcons: Boolean?
        get() = requests.lastOrNull { it.statusDarkIcons != null }?.statusDarkIcons

    internal fun add(request: Request) {
        requests += request
    }

    internal fun remove(request: Request) {
        requests -= request
    }

    /** One screen's request; each is its own, even when two ask for the same. */
    internal class Request(val navigationDarkIcons: Boolean?, val statusDarkIcons: Boolean?)
}

/** The requests the app applies to the system bars; one set, provided by the app. */
internal val LocalSystemBarRequests = staticCompositionLocalOf { SystemBarRequests() }

/**
 * Asks for dark (`true`) or light (`false`) icons on the navigation bar and the status bar while
 * this is in the composition; null leaves that bar alone. When it leaves, its request goes, and the
 * bars follow whatever is still asked of them.
 */
@Composable
internal fun RequestSystemBarIcons(
    navigationDarkIcons: Boolean? = null,
    statusDarkIcons: Boolean? = null,
) {
    val requests = LocalSystemBarRequests.current
    DisposableEffect(requests, navigationDarkIcons, statusDarkIcons) {
        val request = SystemBarRequests.Request(navigationDarkIcons, statusDarkIcons)
        requests.add(request)
        onDispose { requests.remove(request) }
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
