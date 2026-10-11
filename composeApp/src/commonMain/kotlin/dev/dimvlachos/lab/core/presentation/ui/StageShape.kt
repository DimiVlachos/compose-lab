package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.unit.Dp

/**
 * Whether a demo's stage is wider than it is tall, as on a phone turned on its side: the cue for a
 * side-by-side layout. A clip's 4:5 and 9:16 frames never are, so a recording keeps its portrait
 * layout.
 */
internal fun isWideStage(width: Dp, height: Dp): Boolean = width > height
