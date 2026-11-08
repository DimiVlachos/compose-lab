package dev.dimvlachos.lab.sandmandalademo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_sandmandala
import dev.dimvlachos.lab.resources.demo_sandmandala_description
import dev.dimvlachos.lab.resources.ic_demo_sandmandala
import dev.dimvlachos.lab.sandmandalademo.presentation.components.SandMandalaDemo
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object SandMandalaDemos {
    // The script's moves, by name: tap Clear history, sweep the finished mandala away, tap Undo.
    const val Clear = "clear"
    const val Sweep = "sweep"
    const val Undo = "undo"

    // The prototype's finger, about 1.3 times slower: to a button in 730 ms, pressed 180, and
    // aside in 650 (780 after Undo); the sweep waits 1.3 s on the finished mandala, rests 420 ms,
    // presses 160 and brushes in three legs of 546, then lifts and goes aside in 680; Undo waits
    // 1.4 s on the cleared screen.
    const val ShowMs = 200L
    const val ToButtonMs = 730L
    const val PressMs = 180L
    const val AsideMs = 650L
    const val UndoAsideMs = 780L
    const val SeeMandalaMs = 1300L
    const val RestMs = 420L
    const val GripMs = 160L
    const val LegMs = 546L
    const val SweepAsideMs = 680L
    const val SeeClearedMs = 1400L

    val ClearTakes = (ShowMs + ToButtonMs + PressMs + AsideMs + ShowMs).milliseconds
    val SweepTakes =
        (SeeMandalaMs + RestMs + GripMs + 3 * LegMs + SweepAsideMs + ShowMs).milliseconds
    val UndoTakes =
        (SeeClearedMs + ShowMs + ToButtonMs + PressMs + UndoAsideMs + ShowMs).milliseconds

    // The prototype's demo: Clear history, the rows crumble into the mandala, one unhurried sweep
    // through its centre lets it go into the heap, which pours away; Undo, and it all comes back
    // into the rows. Each move waits for the sand before it, so a slow device just runs later.
    private val history =
        demoScript(holdStart = 900.milliseconds) {
            at(0.seconds, lasts = ClearTakes) { tapControl(Clear) }
            at(2.seconds, lasts = 7.seconds) { tapControl(Sweep) }
            at(9.seconds, lasts = 8.seconds) { tapControl(Undo) }
            at(27.seconds, lasts = 200.milliseconds) { resetFeed() }
        }

    val all =
        listOf(
            Demo(
                "sandmandala.history",
                Res.string.demo_sandmandala,
                history,
                description = Res.string.demo_sandmandala_description,
                icon = Res.drawable.ic_demo_sandmandala,
                tags = setOf(DemoTag.Gesture, DemoTag.Physics, DemoTag.Canvas),
                tall = true,
            ) {
                SandMandalaDemo(it)
            },
        )
}
