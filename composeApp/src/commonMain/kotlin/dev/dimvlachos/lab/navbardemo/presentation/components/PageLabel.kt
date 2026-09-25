package dev.dimvlachos.lab.navbardemo.presentation.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.dimvlachos.lab.core.presentation.ui.LabTheme

@Composable
internal fun PageLabel(label: String) {
    Text(
        label,
        color = LabTheme.colors.textPrimary.copy(alpha = 0.18f),
        style = LabTheme.typography.pageLabel,
        modifier = Modifier.fillMaxSize().wrapContentSize(),
    )
}
