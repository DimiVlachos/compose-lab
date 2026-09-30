package dev.dimvlachos.lab.core.camera

import androidx.compose.runtime.Composable

@Composable
actual fun rememberCameraAccess(enabled: Boolean): CameraAccess = CameraAccess.Unavailable
