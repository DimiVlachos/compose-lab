package dev.dimvlachos.lab.rubberbanddemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_rubberband
import dev.dimvlachos.lab.resources.demo_rubberband_description
import dev.dimvlachos.lab.resources.ic_demo_rubberband
import dev.dimvlachos.lab.rubberbanddemo.presentation.components.RubberBandDemo
import kotlin.time.Duration.Companion.milliseconds

internal object RubberBandDemos {
    // The script's moves, by name.
    const val TypeFirst = "type-first"
    const val Send = "send"
    const val PullBack = "pull-back"
    const val Fix = "fix"
    const val SendAgain = "send-again"
    const val Reply = "reply"

    /** What the tour types first, then what it puts in place of the 7, and Jess's reply. */
    const val Typed = "Table’s booked at Rosie’s for 7"
    const val Fixed = "8:30"
    const val Replied = "Perfect, see you there 🙌"

    /** How long each key takes, as the prototype's `typeIn()`. */
    fun keyMs(ch: Char): Long = if (ch == ' ') 110L else 50L + (ch.code * 37 % 45)

    // The prototype's finger: a tap moves there in 480 ms and holds 110; it then moves aside in
    // 400.
    const val TapMs = 480L
    const val HoldMs = 110L
    const val AsideMs = 400L

    // The pull: to the bubble in 520 ms, held 160, pulled 84 dp down over 700, held 380, let go.
    const val ToBubbleMs = 520L
    const val GripMs = 160L
    const val PullMs = 700L
    const val HoldPulledMs = 380L

    /**
     * After the second send the finger lifts away after 500 ms, then the band is left to run out.
     */
    const val LiftAfterMs = 500L
    const val FadeMs = 260L
    const val TypingMs = 1400L

    private val tap = (TapMs + HoldMs).milliseconds
    val TypeFirstTakes = tap + AsideMs.milliseconds + Typed.sumOf { keyMs(it) }.milliseconds
    val SendTakes = tap
    val PullTakes = (ToBubbleMs + GripMs + PullMs + HoldPulledMs).milliseconds
    val FixTakes = tap + (AsideMs + 260L).milliseconds + Fixed.sumOf { keyMs(it) }.milliseconds
    val SendAgainTakes = tap + (LiftAfterMs + 200L).milliseconds

    // The prototype's runDemo(): type a message and send it; grab the bubble and pull it back
    // (oops, it's
    // 8:30), so it comes back into the field; swap the 7 for 8:30 and send again; leave the band to
    // thin and
    // snap; Jess types and replies. Then the thread fades back to its opening four rows, so the
    // clip loops.
    // It waits 1.9 s after the reply, not the prototype's 2.4, to keep the clip under 26 s.
    private val unsend =
        demoScript(holdStart = 800.milliseconds, holdEnd = 0.milliseconds) {
            var t = 0.milliseconds
            at(t, lasts = TypeFirstTakes) { tapControl(TypeFirst) }
            t += TypeFirstTakes + 350.milliseconds
            at(t, lasts = SendTakes) { tapControl(Send) }
            t += SendTakes + 1100.milliseconds
            at(t, lasts = PullTakes) { tapControl(PullBack) }
            t += PullTakes + 1500.milliseconds
            at(t, lasts = FixTakes) { tapControl(Fix) }
            t += FixTakes + 350.milliseconds
            at(t, lasts = SendAgainTakes) { tapControl(SendAgain) }
            // The band holds nine seconds from landing, and the prototype waits 9.3 after the
            // finger lifts.
            t += tap + LiftAfterMs.milliseconds + 9300.milliseconds
            at(t, lasts = TypingMs.milliseconds) { tapControl(Reply) }
            t += TypingMs.milliseconds + 1900.milliseconds
            at(t, lasts = (FadeMs * 2).milliseconds) { resetFeed() }
        }

    val all =
        listOf(
            Demo(
                "rubberband.unsend",
                Res.string.demo_rubberband,
                unsend,
                description = Res.string.demo_rubberband_description,
                icon = Res.drawable.ic_demo_rubberband,
                tags = setOf(DemoTag.Gesture, DemoTag.Physics, DemoTag.Canvas, DemoTag.Text),
                tall = true,
            ) {
                RubberBandDemo(it)
            },
        )
}
