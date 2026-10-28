package dev.dimvlachos.lab.pigeonholedemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.pigeonholedemo.presentation.components.PigeonholeDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_pigeonhole
import dev.dimvlachos.lab.resources.demo_pigeonhole_description
import dev.dimvlachos.lab.resources.ic_demo_pigeonhole
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object PigeonholeDemos {
    // The steps the script takes, by name.
    const val Post = "post"
    const val OpenRack = "open-rack"
    const val OpenReceipts = "open-receipts"
    const val Back = "back"
    const val PullBack = "pull-back"

    // The prototype's clip, on its own timeline (less the script's 600 ms hold): the Corner Market
    // receipt is carried down to Receipts and posted; the rack icon brings the rack up and
    // Receipts is tapped, its letters unfolding into the folder; Back files them away; and the
    // rack comes up again for the receipt to be pulled back out onto the inbox, where it was, so
    // the clip ends as it began.
    private val move = demoScript {
        at(0.3.seconds, lasts = 2680.milliseconds) { tapControl(Post) }
        at(4.58.seconds, lasts = 670.milliseconds) { tapControl(OpenRack) }
        at(5.9.seconds, lasts = 670.milliseconds) { tapControl(OpenReceipts) }
        at(9.07.seconds, lasts = 670.milliseconds) { tapControl(Back) }
        at(12.44.seconds, lasts = 670.milliseconds) { tapControl(OpenRack) }
        at(13.76.seconds, lasts = 2000.milliseconds) { tapControl(PullBack) }
        at(17.26.seconds, lasts = 300.milliseconds) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "pigeonhole.move",
                Res.string.demo_pigeonhole,
                move,
                description = Res.string.demo_pigeonhole_description,
                icon = Res.drawable.ic_demo_pigeonhole,
                tags = setOf(DemoTag.Gesture, DemoTag.Physics, DemoTag.Canvas),
                tall = true,
            ) {
                PigeonholeDemo(it)
            },
        )
}
