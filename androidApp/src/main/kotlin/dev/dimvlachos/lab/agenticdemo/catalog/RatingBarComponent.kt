package dev.dimvlachos.lab.agenticdemo.catalog

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import kotlin.math.roundToInt

/**
 * A 0-5 star rating. When the agent binds `value` to a data-model path the stars become tappable
 * and write the new rating straight back into the data model ([bindUpdater]); a literal value
 * leaves them read-only.
 */
object RatingBarComponent : A2uiComponent {
    private val valueProp =
        A2uiProperty.dynamicNumber(
            "value",
            required = true,
            description = "Rating from 0 to 5. Bind to a path to let the user change it.",
        )
    private val labelProp =
        A2uiProperty.dynamicString("label", description = "Accessibility label, e.g. 'Rating'.")

    override val name = "RatingBar"
    override val description = "Five stars showing a 0-5 rating, editable when bound to data."
    override val properties = listOf(valueProp, labelProp)

    @Composable
    override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean =
        properties.bind(valueProp) != null

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val rating = properties.bind(valueProp)?.toFloat()?.roundToInt()?.coerceIn(0, MAX) ?: 0
        val label = properties.bind(labelProp) ?: name
        val update = properties.bindUpdater(valueProp)
        Row(
            modifier.semantics { contentDescription = "$label $rating/$MAX" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (star in 1..MAX) {
                Text(
                    if (star <= rating) "★" else "☆",
                    color =
                        if (star <= rating) LabTheme.colors.accent else LabTheme.colors.textMuted,
                    style = LabTheme.typography.subtitle,
                    modifier =
                        Modifier.padding(end = LabTheme.spacing.minimum)
                            .then(
                                if (update != null) Modifier.clickable { update(star) }
                                else Modifier
                            ),
                )
            }
        }
    }

    private const val MAX = 5
}
