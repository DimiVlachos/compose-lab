package dev.dimvlachos.lab.pullcorddemo

import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.pullcorddemo.presentation.components.PullCordDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_pull_cord
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object PullCordDemos {
    // A firm pull, well past the click; a tug that is mostly sideways, too short to click; and
    // how far sideways it goes.
    val Pull = 72.dp
    val Tug = 6.dp
    val Swing = 84.dp

    // The lamp on with a pull, the cord left to sway; a tug sideways while it is lit, which only
    // swings it; and off again with a second pull. The end holds long enough for the cord and the
    // shade to hang still, so the next run starts from the same lamp.
    private val cord =
        demoScript(holdEnd = 4.8.seconds) {
            at(0.6.seconds, lasts = 1.seconds) { pullCord(Pull, 500.milliseconds) }
            at(3.4.seconds, lasts = 0.9.seconds) {
                pullCord(Tug, 450.milliseconds, across = Swing)
            }
            at(6.6.seconds, lasts = 1.seconds) { pullCord(Pull, 500.milliseconds) }
        }

    // A lamp is to pull: the script would pull it out of the hand, so on the phone it waits for
    // Replay.
    val all =
        listOf(
            Demo("lamp.cord", Res.string.demo_pull_cord, cord, autoplay = false) {
                PullCordDemo(it)
            }
        )
}
