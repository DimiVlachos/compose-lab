package dev.dimvlachos.lab.conchdemo

import dev.dimvlachos.lab.conchdemo.presentation.components.ConchDemo
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_conch
import dev.dimvlachos.lab.resources.demo_conch_description
import dev.dimvlachos.lab.resources.ic_demo_conch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object ConchDemos {
    /** The fingertip glides to Maya's shell and taps it, so it plays. */
    const val PlayMaya = "play-maya"

    /** The fingertip glides to the conch and holds it down: it lifts and records. */
    const val Hold = "hold"

    /** The fingertip slides 170 dp left while holding, arming cancel. */
    const val Slide = "slide"

    /** The fingertip lets go: the note is sent, or canceled if armed. */
    const val Release = "release"

    /** How far the script slides left to cancel, in dp. */
    const val SlideBy = 170f

    // The prototype's runDemo(): Maya's shell played for a moment; the conch held 3.2 s and let go,
    // so the note flies into the chat; held again, slid left until cancel arms, and let go, so it
    // sinks back in a puff of sand. Then the sent note is taken back out, so the clip loops.
    private val voice = demoScript {
        at(.3.seconds, lasts = 710.milliseconds) { tapControl(PlayMaya) }
        at(5.91.seconds, lasts = 700.milliseconds) { tapControl(Hold) }
        at(9.81.seconds) { tapControl(Release) }
        at(11.21.seconds, lasts = 400.milliseconds) { tapControl(Hold) }
        at(12.91.seconds, lasts = 520.milliseconds) { tapControl(Slide) }
        at(14.13.seconds) { tapControl(Release) }
        at(15.03.seconds, lasts = 400.milliseconds) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "conch.voice",
                Res.string.demo_conch,
                voice,
                description = Res.string.demo_conch_description,
                icon = Res.drawable.ic_demo_conch,
                tags = setOf(DemoTag.Gesture, DemoTag.Physics, DemoTag.Canvas),
                tall = true,
            ) {
                ConchDemo(it)
            },
        )
}
