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

    /** The wheel starts on the first contact, after the A divider. */
    const val StartIndex = 1

    /** From three cards in, the flick that lands on Maya Brooks. */
    val FlickToMaya: Int = ContactDeck(DemoContacts).indexOf("brooks-maya") - StartIndex - 3

    // Three slow flips to show the cards turning over the top, a flick to Maya Brooks, her card
    // pulled out and pushed back, and the wheel put back on the first contact so the clip loops.
    private val contacts = demoScript {
        at(0.8.seconds, lasts = 600.milliseconds) { spinCards(1, 600.milliseconds) }
        at(1.8.seconds, lasts = 600.milliseconds) { spinCards(1, 600.milliseconds) }
        at(2.8.seconds, lasts = 600.milliseconds) { spinCards(1, 600.milliseconds) }
        at(4.seconds, lasts = 900.milliseconds) { spinCards(FlickToMaya, 900.milliseconds) }
        at(6.seconds, lasts = 500.milliseconds) { tapControl(Pull) }
        at(9.5.seconds, lasts = 700.milliseconds) { tapControl(File) }
        // The reset springs back from Maya to the first contact, which takes about a second.
        at(11.seconds, lasts = 1200.milliseconds) { resetFeed() }
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
