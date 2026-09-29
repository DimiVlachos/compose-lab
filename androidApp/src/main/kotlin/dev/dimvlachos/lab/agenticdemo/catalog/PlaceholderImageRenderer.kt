package dev.dimvlachos.lab.agenticdemo.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.catalog.A2uiImageRenderer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.dimvlachos.lab.core.presentation.ui.LabTheme

/**
 * The agent has no real photos, so it asks for `placeholder://<subject>` and this draws a themed
 * tile captioned with the subject instead of loading anything.
 */
object PlaceholderImageRenderer : A2uiImageRenderer {
    @Composable
    override fun Image(
        url: String,
        contentDescription: String?,
        contentScale: ContentScale,
        modifier: Modifier,
        onError: (throwable: Throwable?) -> Unit,
    ) {
        val subject = url.substringAfter("placeholder://", missingDelimiterValue = "")
        Box(
            modifier
                .heightIn(min = LabTheme.spacing.huge * 2)
                .background(
                    Brush.linearGradient(listOf(LabTheme.colors.accentSoft, LabTheme.colors.bar)),
                    LabTheme.shapes.Medium,
                )
                .semantics { this.contentDescription = contentDescription ?: subject },
            contentAlignment = Alignment.BottomStart,
        ) {
            Text(
                subject,
                color = LabTheme.colors.textPrimary,
                style = LabTheme.typography.label,
                modifier = Modifier.padding(LabTheme.spacing.small),
            )
        }
    }
}
