package dev.dimvlachos.lab.core.audio

import androidx.compose.runtime.Composable

// No microphone on iOS yet: the demo falls back to hold-to-breathe.
@Composable actual fun rememberMicAccess(enabled: Boolean): MicAccess = MicAccess.Unavailable
