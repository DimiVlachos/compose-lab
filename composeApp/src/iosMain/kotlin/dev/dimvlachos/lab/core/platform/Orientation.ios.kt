package dev.dimvlachos.lab.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication
import platform.UIKit.UIInterfaceOrientationMaskLandscape
import platform.UIKit.UIInterfaceOrientationMaskPortrait
import platform.UIKit.UIWindowScene
import platform.UIKit.UIWindowSceneGeometryPreferencesIOS

// iOS 16 asks the scene for a geometry: landscape while the demo runs, portrait after it. The
// status bar hides itself in landscape on a phone.
@Composable
internal actual fun LockLandscape() {
    DisposableEffect(Unit) {
        requestOrientations(UIInterfaceOrientationMaskLandscape)
        onDispose { requestOrientations(UIInterfaceOrientationMaskPortrait) }
    }
}

private fun requestOrientations(mask: ULong) {
    val scene =
        UIApplication.sharedApplication.connectedScenes.firstOrNull { it is UIWindowScene }
            as? UIWindowScene ?: return
    scene.requestGeometryUpdateWithPreferences(
        UIWindowSceneGeometryPreferencesIOS(interfaceOrientations = mask),
        errorHandler = null,
    )
}
