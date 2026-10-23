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
    const val TapList = "tap-list"
    const val SlideToNote = "slide-to-note"
    const val TapScan = "tap-scan"
    const val TypeItem = "type-item"
    const val TypeTitle = "type-title"
    const val Done = "done"

    /**
     * What the tour types as the new note's title, and how long each key takes, as the prototype's.
     */
    const val Typed = "Pick up dry cleaning"

    /** What the tour types as the new checklist's first item. */
    const val ItemTyped = "Tent stakes"

    fun keyMs(ch: Char): Long = if (ch == ' ') 120L else 55L + (ch.code * 37 % 50)

    // How long each move takes, as the prototype's demo moves its finger.
    val TapCupTakes = 590.milliseconds
    val TapToolTakes = 700.milliseconds
    val SlideTakes = 1630.milliseconds
    val DoneTakes = 610.milliseconds
    val TypeTakes = Typed.sumOf { keyMs(it) }.milliseconds
    val ItemTakes = ItemTyped.sumOf { keyMs(it) }.milliseconds

    // Each way in and three of the tools, each to its own sheet: the cup tapped open and the
    // highlighter tapped, so a checklist takes its first item; then in one move the cup pressed,
    // the finger slid up onto the pencil and let go, so a note is titled a letter at a time; then
    // the cup tapped open again and the loupe tapped, whose scan needs nothing typed. Each Done
    // sends its tool arcing back into the cup. The list is put back to its six notes at the end,
    // so the clip loops.
    private val fab = demoScript {
        var t = .9.seconds
        at(t, lasts = TapCupTakes) { tapControl(TapCup) }
        t += TapCupTakes + 1100.milliseconds
        at(t, lasts = TapToolTakes) { tapControl(TapList) }
        t += TapToolTakes + 900.milliseconds
        at(t, lasts = ItemTakes) { tapControl(TypeItem) }
        t += ItemTakes + 400.milliseconds
        at(t, lasts = DoneTakes) { tapControl(Done) }
        t += DoneTakes + 1300.milliseconds
        at(t, lasts = SlideTakes) { tapControl(SlideToNote) }
        t += SlideTakes + 900.milliseconds
        at(t, lasts = TypeTakes) { tapControl(TypeTitle) }
        t += TypeTakes + 400.milliseconds
        at(t, lasts = DoneTakes) { tapControl(Done) }
        t += DoneTakes + 1300.milliseconds
        at(t, lasts = TapCupTakes) { tapControl(TapCup) }
        t += TapCupTakes + 900.milliseconds
        at(t, lasts = TapToolTakes) { tapControl(TapScan) }
        t += TapToolTakes + 1100.milliseconds
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
