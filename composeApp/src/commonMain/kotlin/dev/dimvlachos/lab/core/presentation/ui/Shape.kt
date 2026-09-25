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

object AppShape {
    val ExtraSmall = RoundedCornerShape(ShapeTokens.ExtraSmall)
    val Small = RoundedCornerShape(ShapeTokens.Small)
    val Medium = RoundedCornerShape(ShapeTokens.Medium)
    val Large = RoundedCornerShape(ShapeTokens.Large)
    val ExtraLarge = RoundedCornerShape(ShapeTokens.ExtraLarge)
    val Full = RoundedCornerShape(ShapeTokens.Full)
}
