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
    // The script's moves, by name: bury the first task (or the second) still on the list, take a
    // task back by tapping the hand or by tapping the grave.
    const val Bury = "bury"
    const val BurySecond = "bury-second"
    const val Undo = "undo"
    const val UndoAtGrave = "undo-at-grave"

    // The prototype's finger, about 1.3 times slower as the rest of the motion is: to the first
    // task in 800 ms, held 195, dragged half the row's width left over 1070, held 180, let go and
    // aside in 545.
    const val ToRowMs = 800L
    const val GripMs = 195L
    const val DragMs = 1070L
    const val HoldMs = 180L
    const val AsideMs = 545L

    // The undo: once the hand is up, it is left showing 0.6 s, then the finger comes to the card
    // in 830 ms, presses 170, and goes aside in 680.
    const val ShowHandMs = 600L
    const val ToHandMs = 830L
    const val PressMs = 170L
    const val UndoAsideMs = 680L

    // The undo at the grave: once the task lands in the soil, 300 ms, then the finger comes to the
    // headstone and presses as the undo by the hand does, so the task comes back mid-rise.
    const val SeeRestMs = 300L

    val BuryTakes = (ToRowMs + GripMs + DragMs + HoldMs + AsideMs).milliseconds
    val UndoTakes = (ShowHandMs + ToHandMs + PressMs + UndoAsideMs).milliseconds
    val UndoAtGraveTakes = (SeeRestMs + ToHandMs + PressMs + UndoAsideMs).milliseconds

    // Every way in and out. A swipe waits until no task is flying, and an undo until its grave is
    // ready, so on a slow device a step simply starts later. Bury Order 50 lb of flour and tap the
    // grave while the hand is still rising: it flies home. Bury it again and, while the hand holds
    // it up, bury Pick up pumpkin purée: the flour's name clears off the stone and it is gone for
    // good, then the purée's hand comes up and a tap on the hand brings it back. Last, bury Frost
    // 4 dozen ghost cookies and leave it: the hand sinks after its 6.5 s and the grave slides
    // away. The script then puts every task back, so the clip loops.
    private val undo =
        demoScript(holdStart = 800.milliseconds) {
            at(0.seconds, lasts = BuryTakes) { tapControl(Bury) }
            at(4.seconds, lasts = UndoAtGraveTakes) { tapControl(UndoAtGrave) }
            at(6.8.seconds, lasts = BuryTakes) { tapControl(Bury) }
            at(12.8.seconds, lasts = BuryTakes) { tapControl(Bury) }
            at(17.5.seconds, lasts = UndoTakes) { tapControl(Undo) }
            at(21.5.seconds, lasts = BuryTakes) { tapControl(BurySecond) }
            at(38.3.seconds, lasts = 200.milliseconds) { resetFeed() }
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
