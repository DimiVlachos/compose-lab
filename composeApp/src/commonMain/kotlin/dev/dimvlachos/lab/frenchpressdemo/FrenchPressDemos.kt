package dev.dimvlachos.lab.frenchpressdemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.frenchpressdemo.presentation.components.FrenchPressDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_frenchpress
import dev.dimvlachos.lab.resources.demo_frenchpress_description
import dev.dimvlachos.lab.resources.ic_demo_frenchpress
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object FrenchPressDemos {
    // The steps the script takes, by name.
    const val Push = "push"
    const val Full = "full"

    // The prototype's clip: a second in, the finger pushes the knob down, riding it while the
    // words pour in, and lets go at the bottom; 2.2 s after the summary is ready it taps Full
    // article and the story pours back. Then, so the clip ends where it began, the story is made
    // whole if anything stopped it short.
    private val summary = demoScript {
        at(1.seconds, lasts = 3500.milliseconds) { tapControl(Push) }
        at(4.6.seconds, lasts = 8000.milliseconds) { tapControl(Full) }
        at(12.8.seconds, lasts = 300.milliseconds) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "frenchpress.summary",
                Res.string.demo_frenchpress,
                summary,
                description = Res.string.demo_frenchpress_description,
                icon = Res.drawable.ic_demo_frenchpress,
                tags = setOf(DemoTag.Gesture, DemoTag.Motion, DemoTag.Canvas),
                tall = true,
            ) {
                FrenchPressDemo(it)
            },
        )
}
