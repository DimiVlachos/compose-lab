package dev.dimvlachos.lab.core.presentation.ui

import android.app.UiModeManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat

// The bars' icons through the window's insets controller. The app's night mode goes to the system
// too, which keeps it between launches: the window behind Compose (the launch screen, a rotation)
// then comes up in the right colour from the first frame, from values or values-night. The
// activity takes the uiMode change itself (see the manifest), so it isn't recreated.
@Composable
internal actual fun SystemAppearance(mode: ThemeMode, dark: Boolean, requests: SystemBarRequests) {
    val activity = LocalActivity.current ?: return
    val statusDark = darkBarIcons(requests.statusDarkIcons, dark)
    val navigationDark = darkBarIcons(requests.navigationDarkIcons, dark)
    SideEffect {
        val window = activity.window
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = statusDark
            isAppearanceLightNavigationBars = navigationDark
        }
    }
    LaunchedEffect(activity, mode) {
        val nightMode =
            when (mode) {
                ThemeMode.System -> UiModeManager.MODE_NIGHT_AUTO
                ThemeMode.Light -> UiModeManager.MODE_NIGHT_NO
                ThemeMode.Dark -> UiModeManager.MODE_NIGHT_YES
            }
        activity.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(nightMode)
    }
}
