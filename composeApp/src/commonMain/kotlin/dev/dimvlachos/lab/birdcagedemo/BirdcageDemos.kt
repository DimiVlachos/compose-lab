package dev.dimvlachos.lab.birdcagedemo

import dev.dimvlachos.lab.birdcagedemo.presentation.components.BirdcageDemo
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_birdcage
import dev.dimvlachos.lab.resources.demo_birdcage_description
import dev.dimvlachos.lab.resources.ic_demo_birdcage
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object BirdcageDemos {
    // The steps the script takes, by name.
    const val Message = "message"
    const val CoverDown = "cover-down"
    const val CoverUp = "cover-up"
    const val Tap = "tap"

    // A message comes in and the canary sings; the finger pulls the cover down over the cage and
    // the chat is muted, so the next message only stirs the cloth and is counted; the finger pushes
    // the cover back up, the canary wakes, and the next message is sung again. Then a tap drops the
    // cover and a second tap lifts it, as the switch does. The first message is put back at the
    // end, so the clip loops.
    private val mute = demoScript {
        at(0.9.seconds) { tapControl(Message) }
        at(2.6.seconds, lasts = 2.1.seconds) { tapControl(CoverDown) }
        at(6.18.seconds, lasts = 600.milliseconds) { tapControl(Message) }
        at(8.18.seconds, lasts = 1.95.seconds) { tapControl(CoverUp) }
        at(11.21.seconds, lasts = 1.5.seconds) { tapControl(Message) }
        at(13.4.seconds, lasts = 1.2.seconds) { tapControl(Tap) }
        at(15.6.seconds, lasts = 1.2.seconds) { tapControl(Tap) }
        at(17.8.seconds) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "birdcage.mute",
                Res.string.demo_birdcage,
                mute,
                description = Res.string.demo_birdcage_description,
                icon = Res.drawable.ic_demo_birdcage,
                tags = setOf(DemoTag.Canvas, DemoTag.Physics, DemoTag.Gesture),
                tall = true,
            ) {
                BirdcageDemo(it)
            },
        )
}
