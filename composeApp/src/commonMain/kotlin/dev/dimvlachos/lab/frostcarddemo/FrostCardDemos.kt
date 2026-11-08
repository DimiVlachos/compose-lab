package dev.dimvlachos.lab.frostcarddemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.frostcarddemo.presentation.components.FrostCardDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_frostcard
import dev.dimvlachos.lab.resources.demo_frostcard_description
import dev.dimvlachos.lab.resources.ic_demo_frostcard
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object FrostCardDemos {
    // The script's moves, by name: flip the switch, then thaw the card with a thumb.
    const val Freeze = "freeze"
    const val Thaw = "thaw"

    // The prototype's finger, about 1.3 times slower as the rest of the motion is: to the switch in
    // 910 ms, pressed 180, aside in 780; then to the card in 975, a drift of 1820 while the warm
    // circle grows, held at most 4.8 s more, and lifted aside in 845.
    const val ToSwitchMs = 910L
    const val PressMs = 180L
    const val AsideMs = 780L
    const val ToCardMs = 975L
    const val DriftMs = 1820L
    const val HoldMaxMs = 4800L
    const val LiftAsideMs = 845L

    val FreezeTakes = (ToSwitchMs + PressMs + AsideMs + 200).milliseconds

    // The thaw starts once the card has frozen over (2.8 s after the flip) and has been seen a
    // moment; a thumb clears it in about 1.5 s, inside the drift.
    val ThawTakes = (ToCardMs + DriftMs + LiftAsideMs + 200 + 400).milliseconds

    // Flip Freeze card and watch the frost grow in from the edges; then rest a thumb on the card
    // until it is clear and the melt runs off. It ends warm and unfrozen, so the clip loops.
    private val freeze =
        demoScript(holdStart = 900.milliseconds, holdEnd = 1.2.seconds) {
            at(0.seconds, lasts = FreezeTakes) { tapControl(Freeze) }
            at(5.4.seconds, lasts = ThawTakes) { tapControl(Thaw) }
            at(5.4.seconds + ThawTakes + 2.seconds, lasts = 200.milliseconds) { resetFeed() }
        }

    val all =
        listOf(
            Demo(
                "frostcard.freeze",
                Res.string.demo_frostcard,
                freeze,
                description = Res.string.demo_frostcard_description,
                icon = Res.drawable.ic_demo_frostcard,
                tags = setOf(DemoTag.Gesture, DemoTag.Physics, DemoTag.Canvas),
                tall = true,
            ) {
                FrostCardDemo(it)
            },
        )
}
