package dev.dimvlachos.lab.core.camera

import androidx.compose.runtime.Composable

@Composable
internal actual fun rememberCameraAccess(enabled: Boolean): CameraAccess = CameraAccess.Unavailable
