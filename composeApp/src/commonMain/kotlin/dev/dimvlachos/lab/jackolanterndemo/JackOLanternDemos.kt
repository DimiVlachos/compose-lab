package dev.dimvlachos.lab.jackolanterndemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.jackolanterndemo.presentation.components.JackOLanternDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_jackolantern
import dev.dimvlachos.lab.resources.demo_jackolantern_description
import dev.dimvlachos.lab.resources.ic_demo_jackolantern
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object JackOLanternDemos {
    // The script's moves, by name: each plays the prototype's way round from where the candle
    // started, out (a tap, then a flick) or lit in the dark theme (a flick, then a tap).
    const val Change = "change"
    const val ChangeBack = "change-back"
    const val ChangeAndTurn = "change-and-turn"

    // How long each takes, as the prototype's demo moves its finger: to the pumpkin, the tap and
    // away, then the night spreading; to the flick's start, the flick and away, then the night
    // drawing back; and a tap whose light a flick turns round halfway.
    val LightTakes = 5000.milliseconds
    val BlowTakes = 5000.milliseconds
    val TurnTakes = 6600.milliseconds

    // The prototype's runDemo(): a tap lights the candle and the night spreads out of its face; a
    // flick across the flame blows it out and the night draws back in. Then the other way to
    // change your mind: a tap, and while the night is still spreading, a flick that turns it
    // round. It ends where it began: a clip by day, a replay in the dark theme by night.
    private val theme = demoScript {
        at(.4.seconds, lasts = LightTakes) { tapControl(Change) }
        at(.4.seconds + LightTakes, lasts = BlowTakes) { tapControl(ChangeBack) }
        at(.4.seconds + LightTakes + BlowTakes + .6.seconds, lasts = TurnTakes) {
            tapControl(ChangeAndTurn)
        }
        at(.4.seconds + LightTakes + BlowTakes + .6.seconds + TurnTakes + .6.seconds) {
            resetFeed()
        }
    }

    val all =
        listOf(
            Demo(
                "jackolantern.theme",
                Res.string.demo_jackolantern,
                theme,
                description = Res.string.demo_jackolantern_description,
                icon = Res.drawable.ic_demo_jackolantern,
                tags = setOf(DemoTag.Gesture, DemoTag.Canvas, DemoTag.Motion),
                tall = true,
            ) {
                JackOLanternDemo(it)
            },
        )
}
