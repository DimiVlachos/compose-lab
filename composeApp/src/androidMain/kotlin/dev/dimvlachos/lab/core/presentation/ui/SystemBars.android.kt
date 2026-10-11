package dev.dimvlachos.lab.core.presentation.ui

import android.app.UiModeManager
import android.graphics.drawable.ColorDrawable
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
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
    // The window behind Compose shows through for a frame on a rotation or a resize: it follows the
    // theme as it changes, not only the night mode it was created with.
    LaunchedEffect(activity, dark) {
        val background = if (dark) AppColors.Dark.background else AppColors.Light.background
        activity.window.setBackgroundDrawable(ColorDrawable(background.toArgb()))
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
