package dev.dimvlachos.lab.core.platform

import androidx.compose.runtime.Composable

/**
 * Holds the screen in landscape, with the system bars hidden, while it is in the composition, and
 * gives both back when it leaves.
 */
@Composable internal expect fun LockLandscape()
