package dev.dimvlachos.lab.paintrollerdemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.paintrollerdemo.presentation.components.PaintRollerDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_paintroller
import dev.dimvlachos.lab.resources.demo_paintroller_description
import dev.dimvlachos.lab.resources.ic_demo_paintroller
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object PaintRollerDemos {
    // The script's moves, by name.
    const val PaintSedona = "paint-sedona"
    const val Undo = "undo"

    // How long each takes, as the prototype's demo moves its finger: the press and drag, then the
    // wait for the roller to finish the wall and go home, 1.6 s, and the tap on Undo while blue
    // rolls back on.
    val PaintTakes = 4000.milliseconds
    val UndoTakes = 6600.milliseconds

    // The prototype's runDemo(): after a second the finger presses Sedona and drags the roller up
    // the left, across the top and partway down the right, and lets go; the roller finishes the
    // wall by itself and goes home. Then Undo rolls Blue Ridge back on, so the clip ends where it
    // began.
    private val accent = demoScript {
        at(.4.seconds, lasts = PaintTakes) { tapControl(PaintSedona) }
        at(.4.seconds + PaintTakes, lasts = UndoTakes) { tapControl(Undo) }
        at(.4.seconds + PaintTakes + UndoTakes + 1.seconds) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "paintroller.accent",
                Res.string.demo_paintroller,
                accent,
                description = Res.string.demo_paintroller_description,
                icon = Res.drawable.ic_demo_paintroller,
                tags = setOf(DemoTag.Gesture, DemoTag.Canvas, DemoTag.Motion),
                tall = true,
            ) {
                PaintRollerDemo(it)
            },
        )
}
