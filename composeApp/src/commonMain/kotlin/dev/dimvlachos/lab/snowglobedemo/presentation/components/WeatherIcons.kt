package dev.dimvlachos.lab.snowglobedemo.presentation.components

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.presentation.components.snowglobe.GlobeScene
import dev.dimvlachos.lab.core.presentation.ui.SnowGlobeIconColors

/** The day cards' icons: the prototype's three SVGs, path for path. */
internal object WeatherIcons {
    private fun icon(name: String, block: ImageVector.Builder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    private fun ImageVector.Builder.fill(d: String, color: androidx.compose.ui.graphics.Color) {
        addPath(addPathNodes(d), fill = SolidColor(color))
    }

    private fun ImageVector.Builder.stroke(
        d: String,
        color: androidx.compose.ui.graphics.Color,
        width: Float,
    ) {
        addPath(
            addPathNodes(d),
            stroke = SolidColor(color),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
        )
    }

    val Sun: ImageVector by lazy {
        icon("Sun") {
            fill("M12 7.6a4.4 4.4 0 1 0 0.01 0z", SnowGlobeIconColors.sun)
            stroke(
                "M12 2.8v2.3M12 18.9v2.3M2.8 12h2.3M18.9 12h2.3M5.5 5.5l1.6 1.6M16.9 16.9l1.6 1.6" +
                    "M5.5 18.5l1.6-1.6M16.9 7.1l1.6-1.6",
                SnowGlobeIconColors.sun,
                1.8f,
            )
        }
    }

    val Wind: ImageVector by lazy {
        icon("Wind") {
            stroke("M3 8.5h10.5a2.6 2.6 0 1 0-2.6-2.6", SnowGlobeIconColors.wind, 1.9f)
            stroke("M3 12.5h15a2.7 2.7 0 1 1-2.7 2.7", SnowGlobeIconColors.wind, 1.9f)
            stroke("M3 16.5h6.5", SnowGlobeIconColors.wind, 1.9f)
        }
    }

    val Shower: ImageVector by lazy {
        icon("Shower") {
            fill(
                "M7.2 15.4h10a3.7 3.7 0 0 0 .4-7.4 5.3 5.3 0 0 0-10.2 1.3 3.1 3.1 0 0 0-.2 6.1z",
                SnowGlobeIconColors.cloud,
            )
            stroke(
                "M8.6 17.8l-.8 2.4M12.6 17.8l-.8 2.4M16.6 17.8l-.8 2.4",
                SnowGlobeIconColors.rain,
                1.8f,
            )
        }
    }

    fun of(scene: GlobeScene): ImageVector =
        when (scene) {
            GlobeScene.Sunny -> Sun
            GlobeScene.Windy -> Wind
            GlobeScene.Shower -> Shower
        }
}
