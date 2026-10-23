package dev.dimvlachos.lab.keyhookdemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.keyhookdemo.presentation.components.KeyHookDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_keyhook
import dev.dimvlachos.lab.resources.demo_keyhook_description
import dev.dimvlachos.lab.resources.ic_demo_keyhook
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object KeyHookDemos {
    /** Jordan's set carried off the card and hung on his hook. */
    const val HangJordan = "hang-jordan"

    /** Ava's set taken off her hook and carried up to the card. */
    const val TakeAva = "take-ava"

    // The prototype's clip: Jordan hangs his keys up and is signed out; Ava takes hers down,
    // carries them up to the card and is signed in. Then the rack is put back as it began, Jordan's
    // keys on the card, so the clip loops.
    private val signout = demoScript {
        at(.4.seconds, lasts = 1900.milliseconds) { tapControl(HangJordan) }
        at(4.0.seconds, lasts = 3100.milliseconds) { tapControl(TakeAva) }
        at(8.6.seconds, lasts = 600.milliseconds) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "keyhook.signout",
                Res.string.demo_keyhook,
                signout,
                description = Res.string.demo_keyhook_description,
                icon = Res.drawable.ic_demo_keyhook,
                tags = setOf(DemoTag.Physics, DemoTag.Gesture, DemoTag.Canvas),
                tall = true,
            ) {
                KeyHookDemo(it)
            },
        )
}
