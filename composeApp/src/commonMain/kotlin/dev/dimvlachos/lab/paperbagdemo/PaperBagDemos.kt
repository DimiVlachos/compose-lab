package dev.dimvlachos.lab.paperbagdemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.paperbagdemo.presentation.components.PaperBagDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_paperbag
import dev.dimvlachos.lab.resources.demo_paperbag_description
import dev.dimvlachos.lab.resources.ic_demo_paperbag
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object PaperBagDemos {
    // The controls the script taps: a product's Add, the bag, a product's − in the open bag, and
    // Done.
    const val AddPrefix = "add-"
    const val Bag = "bag"
    const val RemovePrefix = "remove-"
    const val Done = "done"

    // The fingertip's trip to a control and its press, as the prototype's demo times them.
    private val Tap = 750.milliseconds

    // The prototype's demo: the baguette, the celery, the apples and the sunflowers hop into the
    // bag; it is opened, the apples are lifted back out, and it is set back in the corner. Then it
    // is opened again, what is left rises out of it one by one, and it goes back empty, so the
    // clip loops.
    private val shopping = demoScript {
        at(0.5.seconds, lasts = Tap) { tapControl(AddPrefix + "bread") }
        at(1.75.seconds, lasts = Tap) { tapControl(AddPrefix + "celery") }
        at(3.0.seconds, lasts = Tap) { tapControl(AddPrefix + "apples") }
        at(4.25.seconds, lasts = Tap) { tapControl(AddPrefix + "flowers") }
        at(5.85.seconds, lasts = Tap) { tapControl(Bag) }
        at(7.95.seconds, lasts = Tap) { tapControl(RemovePrefix + "apples") }
        at(10.05.seconds, lasts = Tap) { tapControl(Done) }
        at(11.6.seconds, lasts = Tap) { tapControl(Bag) }
        at(13.2.seconds, lasts = 1200.milliseconds) { resetFeed() }
        at(14.9.seconds, lasts = Tap) { tapControl(Done) }
    }

    val all =
        listOf(
            Demo(
                "paperbag.cart",
                Res.string.demo_paperbag,
                shopping,
                description = Res.string.demo_paperbag_description,
                icon = Res.drawable.ic_demo_paperbag,
                tags = setOf(DemoTag.Canvas, DemoTag.Motion, DemoTag.Physics),
                tall = true,
            ) {
                PaperBagDemo(it)
            },
        )
}
