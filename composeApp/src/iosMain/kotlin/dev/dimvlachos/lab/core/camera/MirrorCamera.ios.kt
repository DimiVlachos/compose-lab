package dev.dimvlachos.lab.core.camera

import androidx.compose.runtime.Composable

// No camera on iOS yet: the mirror shows its still reflection.
@Composable
actual fun rememberCameraAccess(enabled: Boolean): CameraAccess = CameraAccess.Unavailable
