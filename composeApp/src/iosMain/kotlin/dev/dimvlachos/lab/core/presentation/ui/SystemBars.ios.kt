package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController

/**
 * The view controller the app is shown in, provided by the iOS app; null where there is none, as in
 * the tests, which then leave the window alone.
 */
internal val LocalHostViewController = staticCompositionLocalOf<UIViewController?> { null }

// iOS has no navigation bar, and its home indicator picks its own colour from what is under it, so
// only the status bar is set. Its icons follow the window's interface style: the system's own for
// System, so they turn with it, or light or dark as chosen, or as a screen asks. The same style
// colours the keyboard and the system's alerts.
@Composable
internal actual fun SystemAppearance(mode: ThemeMode, dark: Boolean, requests: SystemBarRequests) {
    val controller = LocalHostViewController.current ?: return
    val style =
        when (requests.statusDarkIcons) {
            true -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
            false -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
            null ->
                when (mode) {
                    ThemeMode.System -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
                    ThemeMode.Light -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
                    ThemeMode.Dark -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
                }
        }
    LaunchedEffect(controller, style) {
        // On the first composition the view may not be in its window yet.
        while (controller.viewIfLoaded?.window == null) withFrameNanos {}
        controller.viewIfLoaded?.window?.overrideUserInterfaceStyle = style
    }
}
