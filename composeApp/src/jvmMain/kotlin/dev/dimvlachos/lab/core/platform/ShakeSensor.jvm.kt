package dev.dimvlachos.lab.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset

// No motion sensor where the tests run.
@Composable internal actual fun ShakeImpulses(enabled: Boolean, onImpulse: (Offset) -> Unit) = Unit
