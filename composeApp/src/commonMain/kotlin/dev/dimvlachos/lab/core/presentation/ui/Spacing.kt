package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The lab's spacing scale, in dp: from [none] and a 2 dp [minimum] up to a 40 dp [huge]. */
@Immutable
public data class Spacing(
    val none: Dp = 0.dp,
    val minimum: Dp = 2.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val smallMedium: Dp = 12.dp,
    val medium: Dp = 16.dp,
    val mediumLarge: Dp = 20.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val huge: Dp = 40.dp,
)
