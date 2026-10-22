package dev.dimvlachos.lab.stickynotedemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_stickynote
import dev.dimvlachos.lab.resources.demo_stickynote_description
import dev.dimvlachos.lab.resources.ic_demo_stickynote
import dev.dimvlachos.lab.stickynotedemo.presentation.components.StickyNoteDemo
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object StickyNoteDemos {
    // The steps the script takes, by name.
    const val ShortPull = "short-pull"
    const val Archive = "archive"
    const val Snooze = "snooze"
    const val TapLater = "tap-later"
    const val TapDone = "tap-done"

    // The prototype's clip: a short pull on the second note that smooths back down, then a long
    // one that archives it to Done, then a pull right on the third that snoozes it to Later. Then,
    // so the clip ends where it began, Later and Done are tapped and each puts its note back where
    // it was: Later first, since a note goes back to the place it left.
    private val swipe = demoScript {
        at(0.7.seconds, lasts = 1850.milliseconds) { tapControl(ShortPull) }
        at(3.05.seconds, lasts = 1440.milliseconds) { tapControl(Archive) }
        at(5.64.seconds, lasts = 1820.milliseconds) { tapControl(Snooze) }
        at(8.61.seconds, lasts = 1090.milliseconds) { tapControl(TapLater) }
        at(10.6.seconds, lasts = 1090.milliseconds) { tapControl(TapDone) }
        at(12.99.seconds, lasts = 300.milliseconds) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "stickynote.swipe",
                Res.string.demo_stickynote,
                swipe,
                description = Res.string.demo_stickynote_description,
                icon = Res.drawable.ic_demo_stickynote,
                tags = setOf(DemoTag.Gesture, DemoTag.Physics, DemoTag.Canvas),
                tall = true,
            ) {
                StickyNoteDemo(it)
            },
        )
}
