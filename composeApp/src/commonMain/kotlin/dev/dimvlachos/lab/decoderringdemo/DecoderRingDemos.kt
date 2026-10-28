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
    const val TwistShort = "twist-short"
    const val Twist = "twist"
    const val Tap = "tap"
    const val TwistBack = "twist-back"

    // First a short twist, let go too soon, so the letters spring back to Spanish. Then the
    // prototype's clip: the fingertip twists the ring round most of a turn and lets go, and the
    // letters finish clicking down into English. 2.6 s later a tap turns it back to the Spanish,
    // and another to the English again; then it twists it back the other way and the Spanish
    // returns. It lifts away 1.7 s after that, the message in Spanish, so it loops.
    private val translate = demoScript {
        at(0.9.seconds, lasts = 2000.milliseconds) { tapControl(TwistShort) }
        at(3.7.seconds, lasts = 2250.milliseconds) { tapControl(Twist) }
        at(8.55.seconds, lasts = 800.milliseconds) { tapControl(Tap) }
        at(12.35.seconds, lasts = 800.milliseconds) { tapControl(Tap) }
        at(16.15.seconds, lasts = 2150.milliseconds) { tapControl(TwistBack) }
        at(20.0.seconds, lasts = 300.milliseconds) { resetFeed() }
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
