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
    // The script's moves, by name: tap Clear history, tap Keep history, clear again by brushing
    // across the rows, smear the mandala, sweep it away, tap Undo.
    const val Clear = "clear"
    const val Keep = "keep"
    const val Brush = "brush"
    const val Smear = "smear"
    const val Sweep = "sweep"
    const val Undo = "undo"

    // The prototype's finger, about 1.3 times slower: to a button in 730 ms, pressed 180, and
    // aside in 650 (780 after Undo); Keep waits 0.8 s on the mandala; the brush waits 0.6 s on
    // the rows, rests 420 ms and crosses a row in 700; the smear waits 1.3 s on the mandala,
    // rests, presses 160 and makes one leg of 546; the sweep follows 0.5 s later in three legs of
    // 546, then lifts and goes aside in 680; Undo waits 1.4 s on the cleared screen.
    const val ShowMs = 200L
    const val ToButtonMs = 730L
    const val PressMs = 180L
    const val AsideMs = 650L
    const val UndoAsideMs = 780L
    const val SeeKeepMs = 800L
    const val SeeRowsMs = 600L
    const val BrushMs = 700L
    const val SeeMandalaMs = 1300L
    const val SeeSmearMs = 500L
    const val RestMs = 420L
    const val GripMs = 160L
    const val LegMs = 546L
    const val SweepAsideMs = 680L
    const val SeeClearedMs = 1400L

    val ClearTakes = (ShowMs + ToButtonMs + PressMs + AsideMs + ShowMs).milliseconds
    val KeepTakes = (SeeKeepMs + ShowMs + ToButtonMs + PressMs + AsideMs + ShowMs).milliseconds
    val BrushTakes = (SeeRowsMs + ShowMs + RestMs + BrushMs + AsideMs + ShowMs).milliseconds
    val SmearTakes = (SeeMandalaMs + ShowMs + RestMs + GripMs + LegMs + SweepAsideMs).milliseconds
    val SweepTakes = (SeeSmearMs + RestMs + GripMs + 3 * LegMs + SweepAsideMs + ShowMs).milliseconds
    val UndoTakes =
        (SeeClearedMs + ShowMs + ToButtonMs + PressMs + UndoAsideMs + ShowMs).milliseconds

    // Every way in and out: Clear history, the rows crumble into the mandala; Keep history sends
    // every grain home; a brush across the rows clears it again; a short stroke only smears the
    // sand; one unhurried sweep through the centre lets it go into the heap, which pours away;
    // Undo, and it all comes back into the rows. Each move waits for the sand before it, so a slow
    // device just runs later.
    private val history =
        demoScript(holdStart = 900.milliseconds) {
            at(0.seconds, lasts = ClearTakes) { tapControl(Clear) }
            at(2.seconds, lasts = 6.seconds) { tapControl(Keep) }
            at(8.seconds, lasts = 5.seconds) { tapControl(Brush) }
            at(13.seconds, lasts = 4.seconds) { tapControl(Smear) }
            at(17.seconds, lasts = 6.seconds) { tapControl(Sweep) }
            at(23.seconds, lasts = 6.seconds) { tapControl(Undo) }
            at(36.seconds, lasts = 200.milliseconds) { resetFeed() }
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
