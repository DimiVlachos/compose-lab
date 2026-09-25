package dev.dimvlachos.lab.core.presentation.components.navbar

import androidx.compose.ui.unit.dp

internal object NavBarDimens {
    val BarHeight = 72.dp
    val CollapsedBarHeight = 56.dp
    val CollapsedSlot = 64.dp
    val PillInset = 8.dp
    val BubbleSize = 52.dp
    val BubbleOverhang = 20.dp
    val NotchGap = 6.dp
    val NotchFillet = 16.dp
    val FloatLift = 12.dp
    val IconSize = 26.dp
    val ActionGap = 8.dp
    const val BubbleStretchRatio = 1.35f
    const val BubblePinchRatio = 0.6f
    const val BubbleLeadGain = 1f
    const val BubbleTrailGain = 0.5f
    const val BubbleLandingGain = 2f
    val BubbleHandoff: Float =
        bubbleHandoff(
            bubbleOverhang = BubbleOverhang.value,
            barHeight = BarHeight.value,
            collapsedBarHeight = CollapsedBarHeight.value,
            pillInset = PillInset.value,
        )
}
