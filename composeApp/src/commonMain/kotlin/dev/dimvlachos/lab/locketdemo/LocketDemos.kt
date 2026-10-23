package dev.dimvlachos.lab.locketdemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.core.presentation.components.locket.Locket
import dev.dimvlachos.lab.core.presentation.components.locket.LocketHand
import dev.dimvlachos.lab.core.presentation.components.locket.LocketState
import dev.dimvlachos.lab.locketdemo.presentation.components.LocketDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_locket
import dev.dimvlachos.lab.resources.demo_locket_description
import dev.dimvlachos.lab.resources.ic_demo_locket
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object LocketDemos {
    const val OpenCatch = "open-catch"
    const val DragPhoto = "drag-photo"
    const val PushShut = "push-shut"
    const val Swing = "swing"
    const val Leave = "leave"

    // The finger taps the catch and the locket springs open, drags the Folly Beach photo up from
    // Recent into it, waits while it trims to the oval, and pushes the lid shut by hand. Then it
    // takes the shut locket and swings it on its chain, lets go and leaves it swinging. The Blue
    // Ridge photo goes back in at the end, so the clip ends where it began.
    private val avatar = demoScript {
        at(.64.seconds, lasts = 1.1.seconds) { tapControl(OpenCatch) }
        at(2.6.seconds, lasts = 1.9.seconds) { tapControl(DragPhoto) }
        at(5.7.seconds, lasts = 1.3.seconds) { tapControl(PushShut) }
        at(7.9.seconds, lasts = 1.6.seconds) { tapControl(Swing) }
        at(10.6.seconds, lasts = 300.milliseconds) { tapControl(Leave) }
        at(12.6.seconds, lasts = 100.milliseconds) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "locket.avatar",
                Res.string.demo_locket,
                avatar,
                description = Res.string.demo_locket_description,
                icon = Res.drawable.ic_demo_locket,
                tags = setOf(DemoTag.Physics, DemoTag.Gesture, DemoTag.Canvas),
                tall = true,
            ) {
                LocketDemo(it)
            },
        )
}

/**
 * Plays the scripted move [control] with the hand, as the prototype's `runDemo` moves its finger.
 */
internal suspend fun LocketHand.play(control: String, locket: LocketState) {
    val s = locket
    when (control) {
        LocketDemos.OpenCatch -> {
            appear(250f, 250f)
            tapAt(s.toWorldX(Locket.RX + 2f, Locket.BC), s.toWorldY(Locket.RX + 2f, Locket.BC))
        }
        LocketDemos.DragPhoto -> {
            val b = s.box
            val lx = s.toWorldX(0f, Locket.BC)
            val ly = s.toWorldY(0f, Locket.BC)
            dragAlong(
                listOf(
                    b.tcx(0) to b.tcy,
                    b.tcx(0) + 14f to b.tcy - 60f,
                    lx - 4f to ly + 40f,
                    lx to ly,
                ),
                260f,
            )
        }
        LocketDemos.PushShut -> {
            // From near the open lid's free edge, across past the hinge, so it lets go short of
            // open and the lid snaps shut.
            val from = s.freeEdge() + 22f
            dragAlong(
                listOf(
                    s.toWorldX(from, Locket.BC) to s.toWorldY(from, Locket.BC),
                    s.toWorldX(40f, Locket.BC) to s.toWorldY(40f, Locket.BC),
                ),
                420f,
            )
        }
        LocketDemos.Swing -> {
            // The shut locket taken by its body and swung out to one side, then the other, and let
            // go there, so it swings on its chain under gravity.
            val y = s.toWorldY(0f, Locket.BC + 10f)
            val x = s.toWorldX(0f, Locket.BC + 10f)
            dragAlong(listOf(x to y, x + 55f to y - 8f, x - 50f to y - 8f), 320f)
        }
        LocketDemos.Leave -> hide()
    }
}
