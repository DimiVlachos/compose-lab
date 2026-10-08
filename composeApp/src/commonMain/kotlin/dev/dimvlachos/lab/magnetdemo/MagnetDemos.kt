package dev.dimvlachos.lab.magnetdemo

import androidx.compose.ui.geometry.Offset
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.magnetdemo.presentation.components.MagnetDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_magnet_filter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object MagnetDemos {
    // Sunset swept up from its slot over the left of the table; Sea brought in from the right to
    // stop beside it, close enough that the two share what matches both.
    val SunsetPath = listOf(Offset(0.2f, 0.62f), Offset(0.28f, 0.46f), Offset(0.34f, 0.36f))
    val SeaPath = listOf(Offset(0.8f, 0.6f), Offset(0.7f, 0.46f), Offset(0.64f, 0.4f))

    // Each step lasts as long as its fingertip is on the stage, fading in and out included, so
    // the clip's length is the script's.
    // Sunset onto the table, and its photos snap; Sea beside it, and the photos of both slide
    // between them; Sunset's photos fanned out and folded back; then Sea flicked home, and
    // Sunset. The end holds long enough for every photo to slide home, so the clip loops.
    private val filter =
        demoScript(holdEnd = 2.6.seconds) {
            at(0.seconds, lasts = 2.4.seconds) {
                dragMagnet(IslandTags.Sunset, SunsetPath, 1.8.seconds)
            }
            at(3.seconds, lasts = 2.2.seconds) { dragMagnet(IslandTags.Sea, SeaPath, 1.6.seconds) }
            at(6.4.seconds, lasts = 500.milliseconds) { tapMagnet(IslandTags.Sunset) }
            at(8.4.seconds, lasts = 500.milliseconds) { tapMagnet(IslandTags.Sunset) }
            at(9.8.seconds, lasts = 800.milliseconds) {
                releaseMagnet(IslandTags.Sea, 400.milliseconds)
            }
            at(10.6.seconds, lasts = 800.milliseconds) {
                releaseMagnet(IslandTags.Sunset, 400.milliseconds)
            }
        }

    // A table is to play with: on the phone it waits for the hand, and the recorder plays this.
    val all =
        listOf(
            Demo("magnet.filter", Res.string.demo_magnet_filter, filter, autoplay = false) {
                MagnetDemo(it)
            }
        )
}
