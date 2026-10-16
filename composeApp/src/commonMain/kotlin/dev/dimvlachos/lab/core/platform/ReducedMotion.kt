package dev.dimvlachos.lab.core.platform

import androidx.compose.runtime.Composable

/**
 * Whether the person has asked the system for less motion: animations off on Android, Reduce Motion
 * on iOS. Read once as the screen is composed.
 */
@Composable internal expect fun rememberReducedMotion(): Boolean
