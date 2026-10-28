package dev.dimvlachos.lab.photocopierdemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.photocopierdemo.presentation.components.PhotocopierDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_photocopier
import dev.dimvlachos.lab.resources.demo_photocopier_description
import dev.dimvlachos.lab.resources.ic_demo_photocopier
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object PhotocopierDemos {
    // The script's moves, by name.
    const val LongPress = "long-press"
    const val TapDuplicate = "tap-duplicate"
    const val FeedBack = "feed-back"
    const val TapUndo = "tap-undo"
    const val LongPressSecond = "long-press-second"
    const val TapPin = "tap-pin"

    // How long each move takes, as the prototype's demo moves its finger.
    val LongPressTakes = 1650.milliseconds
    val TapTakes = 1520.milliseconds
    val FeedTakes = 4100.milliseconds
    val UndoTakes = 4000.milliseconds
    val PinTakes = 2000.milliseconds

    // The prototype's tour: Weekly meal plan held until its menu opens, Duplicate tapped, and
    // once the copy has landed and the copier peeks, the copy dragged down into the slot, so the
    // rollers take it back in. Then the other ways: duplicated again and fed back with Undo, and
    // Pin to top on the second list, then on the first, so the clip ends with the six lists in
    // the order it began with.
    private val duplicate = demoScript {
        var t = .9.seconds
        at(t, lasts = LongPressTakes) { tapControl(LongPress) }
        t += LongPressTakes + 520.milliseconds
        at(t, lasts = TapTakes) { tapControl(TapDuplicate) }
        // The copier's run, from the tap until it peeks, then the prototype's 0.8 s.
        t += 5600.milliseconds
        at(t, lasts = FeedTakes) { tapControl(FeedBack) }
        t += FeedTakes + 300.milliseconds
        at(t, lasts = LongPressTakes) { tapControl(LongPress) }
        t += LongPressTakes + 520.milliseconds
        at(t, lasts = TapTakes) { tapControl(TapDuplicate) }
        t += 5600.milliseconds
        at(t, lasts = UndoTakes) { tapControl(TapUndo) }
        t += UndoTakes + 300.milliseconds
        at(t, lasts = LongPressTakes) { tapControl(LongPressSecond) }
        t += LongPressTakes + 520.milliseconds
        at(t, lasts = PinTakes) { tapControl(TapPin) }
        t += PinTakes + 300.milliseconds
        at(t, lasts = LongPressTakes) { tapControl(LongPressSecond) }
        t += LongPressTakes + 520.milliseconds
        at(t, lasts = PinTakes) { tapControl(TapPin) }
        t += PinTakes + 300.milliseconds
        at(t) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "photocopier.duplicate",
                Res.string.demo_photocopier,
                duplicate,
                description = Res.string.demo_photocopier_description,
                icon = Res.drawable.ic_demo_photocopier,
                tags = setOf(DemoTag.Canvas, DemoTag.Physics, DemoTag.Gesture),
                tall = true,
            ) {
                PhotocopierDemo(it)
            },
        )
}
