package dev.dimvlachos.lab.cardfiledemo

import dev.dimvlachos.lab.cardfiledemo.presentation.components.CardFileDemo
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_cardfile
import dev.dimvlachos.lab.resources.demo_cardfile_description
import dev.dimvlachos.lab.resources.ic_demo_cardfile
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object CardFileDemos {
    const val Pull = "pull"
    const val File = "file"

    private val deck = ContactDeck(DemoContacts)

    /** The wheel starts on Ben Hollis, as the prototype's does. */
    val StartIndex: Int = deck.indexOf("ben-hollis")

    /** From three cards on, the flick that lands on Maya Brooks. */
    val FlickToMaya: Int = deck.indexOf("maya-brooks") - StartIndex - 3

    // The prototype's clip: three cards turned slowly over the top, a flick to Maya Brooks, her
    // card pulled out and pushed back, and the wheel put back on Ben Hollis so the clip loops.
    private val contacts = demoScript {
        at(1.1.seconds, lasts = 1300.milliseconds) { spinCards(3, 1300.milliseconds) }
        at(2.8.seconds, lasts = 900.milliseconds) { spinCards(FlickToMaya, 900.milliseconds) }
        at(5.4.seconds, lasts = 600.milliseconds) { tapControl(Pull) }
        at(8.3.seconds, lasts = 700.milliseconds) { tapControl(File) }
        // The reset springs back from Maya to Ben, which takes a little over a second.
        at(9.6.seconds, lasts = 1500.milliseconds) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "cardfile.contacts",
                Res.string.demo_cardfile,
                contacts,
                description = Res.string.demo_cardfile_description,
                icon = Res.drawable.ic_demo_cardfile,
                tags = setOf(DemoTag.Gesture, DemoTag.Physics, DemoTag.ThreeD),
                tall = true,
            ) {
                CardFileDemo(it)
            }
        )
}
