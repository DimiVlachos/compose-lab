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
    // The script's moves, by name: hold the warm card until it freezes, peek under the ice and
    // let go early, thaw it with a thumb, then flip the switch on and off.
    const val Hold = "hold"
    const val Peek = "peek"
    const val Thaw = "thaw"
    const val Freeze = "freeze"
    const val Unfreeze = "unfreeze"

    // The prototype's finger, about 1.3 times slower as the rest of the motion is: to the switch in
    // 910 ms, pressed 180, aside in 780; to the card in 975, held 900 to freeze it (it freezes at
    // 450), or 800 to peek; the thaw waits for the peek's hole to close and 400 more, then drifts
    // 1820 while the warm circle grows, held at most 4.8 s more, and lifted aside in 845. The
    // switch goes off once the switched frost has been seen for 700.
    const val ToSwitchMs = 910L
    const val PressMs = 180L
    const val AsideMs = 780L
    const val ToCardMs = 975L
    const val HoldFreezeMs = 900L
    const val PeekMs = 800L
    const val SeeClosedMs = 400L
    const val DriftMs = 1820L
    const val HoldMaxMs = 4800L
    const val LiftAsideMs = 845L
    const val SeeFrozenMs = 700L

    // The frost takes 2.8 s to cover the card, and a peek's hole about 2 s to close again.
    const val FrostSeconds = 2.8
    const val CloseSeconds = 2.0

    val FlipTakes = (200 + ToSwitchMs + PressMs + AsideMs + 200).milliseconds
    val HoldTakes = (ToCardMs + HoldFreezeMs + LiftAsideMs + 200).milliseconds
    val PeekTakes = (ToCardMs + PeekMs + LiftAsideMs + 200).milliseconds

    // A thumb clears it in about 1.5 s, inside the drift.
    val ThawTakes = (SeeClosedMs + ToCardMs + DriftMs + LiftAsideMs + 200).milliseconds

    // Every way in and out: hold the warm card and the frost grows in from the edges; a thumb opens
    // a warm hole that closes back in when it lifts too soon; held, the thumb clears the card and
    // the melt runs off. Then the switch freezes it again, and switched off it thaws from the
    // middle by itself. It ends warm and unfrozen, so the clip loops. Each move waits for the ice
    // before it, so a slow device just runs later.
    private val freeze =
        demoScript(holdStart = 900.milliseconds, holdEnd = 1.2.seconds) {
            at(0.seconds, lasts = HoldTakes) { tapControl(Hold) }
            at(4.seconds, lasts = PeekTakes) { tapControl(Peek) }
            at(7.seconds, lasts = ThawTakes + 2.seconds) { tapControl(Thaw) }
            at(13.seconds, lasts = FlipTakes) { tapControl(Freeze) }
            at(15.5.seconds, lasts = FlipTakes + 2.seconds) { tapControl(Unfreeze) }
            at(22.seconds, lasts = 200.milliseconds) { resetFeed() }
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
