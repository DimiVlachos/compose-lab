package dev.dimvlachos.lab.paperfandemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.paperfandemo.presentation.components.PaperFanDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_paperfan
import dev.dimvlachos.lab.resources.demo_paperfan_description
import dev.dimvlachos.lab.resources.ic_demo_paperfan
import kotlin.time.Duration.Companion.milliseconds

internal object PaperFanDemos {
    private const val TapPrefix = "tap:"
    private const val FoldPrefix = "fold:"

    /** The control that taps question [index] with the fingertip. */
    fun tap(index: Int) = TapPrefix + index

    /**
     * The control that rides the fingertip along with question [index]'s row as the list settles,
     * then sweeps its open fan shut by hand, from the leading guard round to the left.
     */
    fun fold(index: Int) = FoldPrefix + index

    /** Which question [tap] taps, or null for another control. */
    fun tapped(control: String): Int? =
        control.removePrefix(TapPrefix).takeIf { it != control }?.toIntOrNull()

    /** Which question's fan [fold] folds, or null for another control. */
    fun folded(control: String): Int? =
        control.removePrefix(FoldPrefix).takeIf { it != control }?.toIntOrNull()

    /** How long a tap takes: the glide to the row, and the press. */
    const val GlideMs = 520
    const val PressMs = 110L

    /** How long the finger rides the row up, gets to the fan, sweeps it shut and leaves. */
    const val RideMs = 2600
    const val ReachMs = 500
    const val SweepMs = 1000
    const val LeaveMs = 500L

    // The prototype's clip: the returns question is tapped and its fan snaps open; then the Trail
    // Rewards question, so the first folds away as the second opens; the finger rides the row up
    // as the list settles and sweeps the open fan shut by hand. It ends with every fan folded, so
    // the clip loops.
    private val faq = demoScript {
        at(300.milliseconds, lasts = (GlideMs + PressMs).milliseconds) { tapControl(tap(1)) }
        at(3530.milliseconds, lasts = (GlideMs + PressMs).milliseconds) { tapControl(tap(3)) }
        at(
            4160.milliseconds,
            lasts = (RideMs + ReachMs + SweepMs + LeaveMs).milliseconds,
        ) {
            tapControl(fold(3))
        }
        at(8900.milliseconds, lasts = 400.milliseconds) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "paperfan.faq",
                Res.string.demo_paperfan,
                faq,
                description = Res.string.demo_paperfan_description,
                icon = Res.drawable.ic_demo_paperfan,
                tags = setOf(DemoTag.Physics, DemoTag.Canvas, DemoTag.Gesture),
                tall = true,
            ) {
                PaperFanDemo(it)
            },
        )
}
