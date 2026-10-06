package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

internal object ShapeTokens {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 24.dp
    val Full = 48.dp
}

/** The lab's rounded corners, from 4 dp to 48 dp. Read them through [LabTheme.shapes]. */
public object AppShape {
    /** 4 dp corners. */
    public val extraSmall: RoundedCornerShape = RoundedCornerShape(ShapeTokens.ExtraSmall)

    /** 8 dp corners. */
    public val small: RoundedCornerShape = RoundedCornerShape(ShapeTokens.Small)

    /** 12 dp corners. */
    public val medium: RoundedCornerShape = RoundedCornerShape(ShapeTokens.Medium)

    /** 16 dp corners. */
    public val large: RoundedCornerShape = RoundedCornerShape(ShapeTokens.Large)

    /** 24 dp corners. */
    public val extraLarge: RoundedCornerShape = RoundedCornerShape(ShapeTokens.ExtraLarge)

    /** 48 dp corners: a pill, for anything up to 96 dp tall. */
    public val full: RoundedCornerShape = RoundedCornerShape(ShapeTokens.Full)
}
