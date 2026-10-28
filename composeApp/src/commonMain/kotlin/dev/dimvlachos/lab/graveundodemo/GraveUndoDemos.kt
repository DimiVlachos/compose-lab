package dev.dimvlachos.lab.graveundodemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.graveundodemo.presentation.components.GraveUndoDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_graveundo
import dev.dimvlachos.lab.resources.demo_graveundo_description
import dev.dimvlachos.lab.resources.ic_demo_graveundo
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object GraveUndoDemos {
    // The script's moves, by name.
    const val Bury = "bury"
    const val Undo = "undo"

    // The prototype's finger, about 1.3 times slower as the rest of the motion is: to the first
    // task in 800 ms, held 195, dragged half the row's width left over 1070, held 180, let go and
    // aside in 545.
    const val ToRowMs = 800L
    const val GripMs = 195L
    const val DragMs = 1070L
    const val HoldMs = 180L
    const val AsideMs = 545L

    // The undo: once the hand is up, it is left showing 1.5 s, then the finger comes to the card
    // in 830 ms, presses 170, and goes aside in 680.
    const val ShowHandMs = 1500L
    const val ToHandMs = 830L
    const val PressMs = 170L
    const val UndoAsideMs = 680L

    val BuryTakes = (ToRowMs + GripMs + DragMs + HoldMs + AsideMs).milliseconds
    val UndoTakes = (ShowHandMs + ToHandMs + PressMs + UndoAsideMs).milliseconds

    // The prototype's runDemo(): swipe the first task (Order 50 lb of flour) away; it flies into
    // the grave and the hand comes up holding it; then tap the hand and it flies home. The undo
    // waits for the hand to be up, so on a slow device it simply starts later. The list ends as
    // it began, so the clip loops.
    private val undo =
        demoScript(holdStart = 1300.milliseconds) {
            at(0.seconds, lasts = BuryTakes) { tapControl(Bury) }
            at(6.5.seconds, lasts = UndoTakes) { tapControl(Undo) }
            at(12.8.seconds, lasts = 200.milliseconds) { resetFeed() }
        }

    val all =
        listOf(
            Demo(
                "graveundo.undo",
                Res.string.demo_graveundo,
                undo,
                description = Res.string.demo_graveundo_description,
                icon = Res.drawable.ic_demo_graveundo,
                tags = setOf(DemoTag.Gesture, DemoTag.Physics, DemoTag.Canvas),
                tall = true,
            ) {
                GraveUndoDemo(it)
            },
        )
}
