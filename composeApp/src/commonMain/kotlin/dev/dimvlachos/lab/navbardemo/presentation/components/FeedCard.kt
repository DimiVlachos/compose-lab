package dev.dimvlachos.lab.navbardemo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.ui.LabTheme

@Composable
internal fun FeedCard(index: Int) {
    Column(
        Modifier.fillMaxWidth()
            .background(LabTheme.colors.surface, LabTheme.shapes.Large)
            .padding(LabTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.small),
    ) {
        SkeletonLine(
            widthFraction = if (index % 2 == 0) 0.6f else 0.45f,
            height = 14.dp,
            alpha = 0.5f,
        )
        SkeletonLine(widthFraction = 0.9f, height = 10.dp, alpha = 0.25f)
        SkeletonLine(widthFraction = 0.7f, height = 10.dp, alpha = 0.25f)
    }
}

@Composable
private fun SkeletonLine(widthFraction: Float, height: Dp, alpha: Float) {
    Box(
        Modifier.fillMaxWidth(widthFraction)
            .height(height)
            .background(LabTheme.colors.textMuted.copy(alpha = alpha), LabTheme.shapes.Full)
    )
}
