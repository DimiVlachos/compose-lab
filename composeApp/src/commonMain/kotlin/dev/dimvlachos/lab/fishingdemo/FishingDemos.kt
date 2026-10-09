package dev.dimvlachos.lab.fishingdemo

import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.fishingdemo.presentation.components.FishingDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_fishing
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object FishingDemos {
    // A firm pull, the list taking half of it, well past the threshold so the rod bends deep; and
    // how long it takes.
    val Pull = 260.dp
    private val PullTime = 900.milliseconds

    // How long a pull lasts in all: the fingertip coming down, the pull, its hold and the lift.
    private val PullLasts = 1.4.seconds

    // Four refreshes, each pulled once the last has played out and the water has closed: two
    // islands caught, nothing new, a snapped line, and one more island. The end holds long enough
    // for the line and the water to come to rest, so the next run starts from still water.
    private val feed =
        demoScript(holdEnd = 7.seconds) {
            at(0.6.seconds, lasts = PullLasts) { pullToRefresh(Pull, PullTime) }
            at(6.4.seconds, lasts = PullLasts) { pullToRefresh(Pull, PullTime) }
            at(10.6.seconds, lasts = PullLasts) { pullToRefresh(Pull, PullTime) }
            at(16.2.seconds, lasts = PullLasts) { pullToRefresh(Pull, PullTime) }
        }

    // A feed is to pull: the script would pull it out of the hand, so on the phone it waits for
    // Replay.
    val all =
        listOf(
            Demo("feed.fishing", Res.string.demo_fishing, feed, autoplay = false) {
                FishingDemo(it)
            }
        )
}
