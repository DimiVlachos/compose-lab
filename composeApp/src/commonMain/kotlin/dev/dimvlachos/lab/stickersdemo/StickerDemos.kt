package dev.dimvlachos.lab.stickersdemo

import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.core.presentation.components.stickers.StickerHand
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_stickers
import dev.dimvlachos.lab.resources.demo_stickers_description
import dev.dimvlachos.lab.resources.ic_demo_stickers
import dev.dimvlachos.lab.stickersdemo.presentation.components.StickerDemo
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object StickerDemos {
    const val DragAll = "drag-all"
    const val DragBack = "drag-back"
    const val TapFirst = "tap-first"
    const val TapFifth = "tap-fifth"
    const val ClearAll = "clear-all"

    // The prototype's clip: a drag across the sheet lays all five down one after another, back to
    // three, a tap on the first slot jumps down to one and a tap on the fifth flicks them all on.
    // Then, so the clip ends where it began, a drag back peels them all off onto the sheet again.
    private val rating = demoScript {
        at(.5.seconds, lasts = 2300.milliseconds) { tapControl(DragAll) }
        at(3.0.seconds, lasts = 1000.milliseconds) { tapControl(DragBack) }
        at(4.5.seconds, lasts = 800.milliseconds) { tapControl(TapFirst) }
        at(6.3.seconds, lasts = 900.milliseconds) { tapControl(TapFifth) }
        // The stars go back one after another, which takes a little over three seconds.
        at(8.6.seconds, lasts = 3600.milliseconds) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "stickers.rating",
                Res.string.demo_stickers,
                rating,
                description = Res.string.demo_stickers_description,
                icon = Res.drawable.ic_demo_stickers,
                tags = setOf(DemoTag.Gesture, DemoTag.Physics),
                tall = true,
            ) {
                StickerDemo(it)
            },
        )
}

/** Plays the scripted move [control] with the hand, as the prototype's demo moves its finger. */
internal suspend fun StickerHand.play(control: String) {
    val l = layout ?: return
    val s = l.scale
    when (control) {
        StickerDemos.DragAll -> {
            val from = onSheet(0, -.45f, .7f)
            appear(from + Offset(0f, 30 * s))
            glide(from, 340f)
            press()
            glide(onSheet(4, .1f, .7f), 1500f)
        }
        StickerDemos.DragBack -> {
            glide(onSheet(2, .05f, .7f), 800f)
            lift()
        }
        StickerDemos.TapFirst -> {
            glide(inSlot(0), 500f)
            tap()
        }
        StickerDemos.TapFifth -> {
            glide(inSlot(4), 600f)
            tap()
        }
        StickerDemos.ClearAll -> {
            glide(inSlot(4, .1f), 300f)
            press()
            glide(inSlot(0, -.5f), 1100f)
            lift()
            hide()
            kotlinx.coroutines.delay(1_600)
        }
    }
}
