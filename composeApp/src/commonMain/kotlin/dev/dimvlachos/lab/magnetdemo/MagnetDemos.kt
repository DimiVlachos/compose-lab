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
    // Sunset swept up from its slot to the top left, away from most of its photos, which slide
    // across the table to it; Sea put down at the lower right, apart, a second search; then Sea
    // brought up beside Sunset, close enough to snap to its side.
    val SunsetPath = listOf(Offset(0.18f, 0.62f), Offset(0.22f, 0.42f), Offset(0.27f, 0.27f))
    val SeaPath = listOf(Offset(0.5f, 0.74f), Offset(0.64f, 0.66f), Offset(0.72f, 0.6f))
    val SeaJoinPath = listOf(Offset(0.66f, 0.46f), Offset(0.56f, 0.33f), Offset(0.5f, 0.28f))

    // Each step lasts as long as its fingertip is on the stage, fading in and out included, so
    // the clip's length is the script's.
    // Sunset onto the table, and every sunset photo comes to it, even from across the table; Sea
    // put down apart, two searches side by side; Sea snapped to Sunset's side, the two one search,
    // and only the photos of both stay; those fanned out, Corfu opened and closed, and the grid
    // folded back; then Sea flicked home, and Sunset, a search on its own again, gathers all its
    // photos back before it follows. The end holds long enough for every photo to slide home, so
    // the clip loops.
    private val filter =
        demoScript(holdEnd = 3.seconds) {
            at(0.seconds, lasts = 2.4.seconds) {
                dragMagnet(IslandTags.Sunset, SunsetPath, 1.8.seconds)
            }
            at(3.2.seconds, lasts = 2.2.seconds) {
                dragMagnet(IslandTags.Sea, SeaPath, 1.6.seconds)
            }
            at(7.8.seconds, lasts = 1.8.seconds) {
                dragMagnet(IslandTags.Sea, SeaJoinPath, 1.2.seconds)
            }
            at(10.6.seconds, lasts = 500.milliseconds) { tapMagnet(IslandTags.Sunset) }
            at(11.8.seconds, lasts = 500.milliseconds) { tapPhoto(Corfu) }
            at(14.2.seconds, lasts = 500.milliseconds) { tapPhoto(Corfu) }
            at(15.2.seconds, lasts = 500.milliseconds) { tapMagnet(IslandTags.Sunset) }
            at(16.4.seconds, lasts = 800.milliseconds) {
                releaseMagnet(IslandTags.Sea, 400.milliseconds)
            }
            at(18.6.seconds, lasts = 800.milliseconds) {
                releaseMagnet(IslandTags.Sunset, 400.milliseconds)
            }
        }

    // The photo the clip opens: a sunset over the sea.
    private const val Corfu = "corfu"

    // A table is to play with: on the phone it waits for the hand, and the recorder plays this.
    val all =
        listOf(
            Demo("magnet.filter", Res.string.demo_magnet_filter, filter, autoplay = false) {
                MagnetDemo(it)
            }
        )
}
