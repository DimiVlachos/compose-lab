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

// The window's interface style follows the theme mode: the system's own for System, so it turns
// with it, or light or dark as chosen. The status bar's icons, the keyboard and the system's alerts
// follow it. Screens' requests for bar icons are left alone here: iOS has no navigation bar, its
// home indicator picks its own colour from what is under it, and the status bar sits over the top
// bar, which wears the theme. The style can't be bent to a request either, since Compose reads the
// system's dark theme from it, and the whole palette would turn with it.
@Composable
internal actual fun SystemAppearance(mode: ThemeMode, dark: Boolean, requests: SystemBarRequests) {
    val controller = LocalHostViewController.current ?: return
    val style =
        when (mode) {
            ThemeMode.System -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
            ThemeMode.Light -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
            ThemeMode.Dark -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
        }
    LaunchedEffect(controller, style) {
        // On the first composition the view may not be in its window yet.
        while (controller.viewIfLoaded?.window == null) withFrameNanos {}
        controller.viewIfLoaded?.window?.overrideUserInterfaceStyle = style
    }
}
