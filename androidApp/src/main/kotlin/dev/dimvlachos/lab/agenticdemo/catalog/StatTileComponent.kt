package dev.dimvlachos.lab.agenticdemo.catalog

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.dimvlachos.lab.core.presentation.ui.LabTheme

/**
 * A labelled figure. Both texts are data bindings, so an `updateDataModel` from the agent changes
 * just this tile, animated, without the surface being rebuilt.
 */
object StatTileComponent : A2uiComponent {
    private val labelProp = A2uiProperty.dynamicString("label", required = true)
    private val valueProp = A2uiProperty.dynamicString("value", required = true)
    private val trendProp =
        A2uiProperty.stringEnum(
            "trend",
            enumValues = listOf("up", "down", "flat"),
            description = "Optional arrow next to the value.",
        )

    override val name = "StatTile"
    override val description = "A small tile: a caption above a prominent value, e.g. price."
    override val properties = listOf(labelProp, valueProp, trendProp)

    @Composable
    override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
        properties.bind(valueProp) != null

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val label = properties.bind(labelProp).orEmpty()
        val value = properties.bind(valueProp).orEmpty()
        val arrow =
            when (properties[trendProp]) {
                "up" -> " ▲"
                "down" -> " ▼"
                else -> ""
            }
        Column(
            // Tiles usually sit side by side in a Row, and the agent cannot set a Row's spacing.
            modifier
                .padding(end = LabTheme.spacing.small)
                .background(LabTheme.colors.bar, LabTheme.shapes.Medium)
                .padding(
                    horizontal = LabTheme.spacing.smallMedium,
                    vertical = LabTheme.spacing.small,
                )
        ) {
            Text(label, color = LabTheme.colors.textMuted, style = LabTheme.typography.label)
            AnimatedContent(targetState = value + arrow, label = "stat") {
                Text(it, color = LabTheme.colors.textPrimary, style = LabTheme.typography.stat)
            }
        }
    }
}
