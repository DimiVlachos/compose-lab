package dev.dimvlachos.lab.pencilcupdemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.pencilcupdemo.presentation.components.PencilCupDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_pencilcup
import dev.dimvlachos.lab.resources.demo_pencilcup_description
import dev.dimvlachos.lab.resources.ic_demo_pencilcup
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object PencilCupDemos {
    // The script's moves, by name.
    const val TapCup = "tap-cup"
    const val SlideToNote = "slide-to-note"
    const val Done = "done"

    // How long each move takes, as the prototype's demo moves its finger.
    val TapCupTakes = 590.milliseconds
    val SlideTakes = 1630.milliseconds
    val DoneTakes = 610.milliseconds

    // The prototype's runDemo(): the cup tapped open, and tapped shut so the tools clatter back;
    // then in one move the cup pressed, the finger slid up onto the pencil and let go, so it flies
    // into a new note's sheet; then Done, and the pencil falls back into the cup from above. The
    // list is put back to its six notes at the end, so the clip loops.
    private val fab = demoScript {
        var t = .9.seconds
        at(t, lasts = TapCupTakes) { tapControl(TapCup) }
        t += TapCupTakes + 1500.milliseconds
        at(t, lasts = TapCupTakes) { tapControl(TapCup) }
        t += TapCupTakes + 1300.milliseconds
        at(t, lasts = SlideTakes) { tapControl(SlideToNote) }
        t += SlideTakes + 1500.milliseconds
        at(t, lasts = DoneTakes) { tapControl(Done) }
        t += DoneTakes + 1600.milliseconds
        at(t) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "pencilcup.fab",
                Res.string.demo_pencilcup,
                fab,
                description = Res.string.demo_pencilcup_description,
                icon = Res.drawable.ic_demo_pencilcup,
                tags = setOf(DemoTag.Canvas, DemoTag.Physics, DemoTag.Gesture),
                tall = true,
            ) {
                PencilCupDemo(it)
            },
        )
}
