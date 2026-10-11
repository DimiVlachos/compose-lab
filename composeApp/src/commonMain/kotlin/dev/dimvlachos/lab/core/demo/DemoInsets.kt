package dev.dimvlachos.lab.core.demo

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable

/**
 * What a demo keeps its controls clear of: the sides and the foot of the screen. The demo screen's
 * bar already holds the top. While recording, none: the clip's frame sits in the middle of the
 * screen and touches no edge, but the cutout (Android) and the safe area (iOS) still report insets.
 */
@Composable
internal fun demoInsets(state: DemoState): WindowInsets =
    if (state.recording) WindowInsets(0)
    else WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
