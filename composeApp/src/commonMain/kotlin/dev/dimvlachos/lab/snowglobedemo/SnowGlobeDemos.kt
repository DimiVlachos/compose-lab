package dev.dimvlachos.lab.snowglobedemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_snowglobe
import dev.dimvlachos.lab.resources.demo_snowglobe_description
import dev.dimvlachos.lab.resources.ic_demo_snowglobe
import dev.dimvlachos.lab.snowglobedemo.presentation.components.SnowGlobeDemo
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object SnowGlobeDemos {
    // The day tabs the script taps.
    const val Tomorrow = "day-1"
    const val Friday = "day-2"

    // As the prototype plays it: the globe is shaken, and once the glitter has settled tomorrow has
    // turned to a shower; Friday's meltemi is looked at, then tomorrow again, and the forecast goes
    // back as it began so the clip loops.
    private val weather = demoScript {
        at(0.8.seconds, lasts = 1.5.seconds) { shakeGlobe(1.5.seconds) }
        at(7.5.seconds, lasts = 500.milliseconds) { tapControl(Friday) }
        at(10.5.seconds, lasts = 500.milliseconds) { tapControl(Tomorrow) }
        at(12.6.seconds) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "snowglobe.weather",
                Res.string.demo_snowglobe,
                weather,
                description = Res.string.demo_snowglobe_description,
                icon = Res.drawable.ic_demo_snowglobe,
                tags = setOf(DemoTag.Gesture, DemoTag.Canvas, DemoTag.Motion, DemoTag.Physics),
                tall = true,
            ) {
                SnowGlobeDemo(it)
            },
        )
}
