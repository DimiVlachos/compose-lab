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
    const val PushEarly = "push-early"
    const val Push = "push"
    const val Full = "full"
    const val Tap = "tap"
    const val PullUp = "pull-up"

    // A second in, the finger pushes the knob a little way and lets go, and the words fly home.
    // Then the prototype's clip: it pushes the knob down, riding it while the words pour in, and
    // lets go at the bottom; 2.2 s after the summary is ready it taps Full article and the story
    // pours back. Then the other two ways: a tap on the press summarizes, and pulling the knob up
    // gives the story back. Last, so the clip ends where it began, the story is made whole if
    // anything stopped it short, once the words still flying home have landed.
    private val summary = demoScript {
        at(1.seconds, lasts = 3200.milliseconds) { tapControl(PushEarly) }
        at(4.2.seconds, lasts = 3500.milliseconds) { tapControl(Push) }
        at(7.7.seconds, lasts = 9600.milliseconds) { tapControl(Full) }
        at(17.3.seconds, lasts = 6000.milliseconds) { tapControl(Tap) }
        at(23.3.seconds, lasts = 6500.milliseconds) { tapControl(PullUp) }
        at(29.8.seconds, lasts = 300.milliseconds) { resetFeed() }
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
