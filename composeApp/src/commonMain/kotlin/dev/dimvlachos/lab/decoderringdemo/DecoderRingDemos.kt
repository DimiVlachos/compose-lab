package dev.dimvlachos.lab.decoderringdemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.decoderringdemo.presentation.components.DecoderRingDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_decoderring
import dev.dimvlachos.lab.resources.demo_decoderring_description
import dev.dimvlachos.lab.resources.ic_demo_decoderring
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object DecoderRingDemos {
    // The steps the script takes, by name.
    const val Twist = "twist"
    const val TwistBack = "twist-back"

    // The prototype's clip: the fingertip twists the ring round most of a turn and lets go, and
    // the letters finish clicking down into English; 2.6 s later it twists it back the other way
    // and the Spanish returns. It lifts away 1.7 s after that, the message in Spanish, so it loops.
    private val translate = demoScript {
        at(0.9.seconds, lasts = 2250.milliseconds) { tapControl(Twist) }
        at(5.75.seconds, lasts = 2150.milliseconds) { tapControl(TwistBack) }
        at(9.6.seconds, lasts = 300.milliseconds) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "decoderring.translate",
                Res.string.demo_decoderring,
                translate,
                description = Res.string.demo_decoderring_description,
                icon = Res.drawable.ic_demo_decoderring,
                tags = setOf(DemoTag.Gesture, DemoTag.Canvas),
                tall = true,
            ) {
                DecoderRingDemo(it)
            },
        )
}
