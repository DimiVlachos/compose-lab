package dev.dimvlachos.lab.core.platform

import androidx.compose.ui.Modifier

/**
 * Keeps the system's own edge gestures, such as Android's swipe back, off this element, so a drag
 * that starts on it near the screen's edge is the app's. Nothing on a platform without them.
 */
internal expect fun Modifier.keepSystemGesturesOff(): Modifier
