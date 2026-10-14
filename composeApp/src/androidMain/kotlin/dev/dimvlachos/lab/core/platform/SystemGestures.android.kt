package dev.dimvlachos.lab.core.platform

import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.ui.Modifier

internal actual fun Modifier.keepSystemGesturesOff(): Modifier = systemGestureExclusion()
