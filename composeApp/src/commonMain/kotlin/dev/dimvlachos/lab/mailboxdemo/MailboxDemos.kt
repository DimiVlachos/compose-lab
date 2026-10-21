package dev.dimvlachos.lab.mailboxdemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.mailboxdemo.presentation.components.MailboxDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_mailbox
import dev.dimvlachos.lab.resources.demo_mailbox_description
import dev.dimvlachos.lab.resources.ic_demo_mailbox
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object MailboxDemos {
    // The steps the script takes, by name.
    const val Arrive = "arrive"
    const val OpenBox = "mailbox"
    const val ReadFirst = "read-first"
    const val MarkAllRead = "mark-all-read"
    const val Back = "back"

    // As the prototype plays it: a letter comes, and a second while the flag is up; the finger taps
    // the mailbox and the inbox slides in, reads the newest letter, marks the rest read so the flag
    // comes down, and goes back. Then everything is put back as it began, which it already is.
    private val delivery = demoScript {
        at(0.7.seconds, lasts = 900.milliseconds) { tapControl(Arrive) }
        at(2.7.seconds, lasts = 900.milliseconds) { tapControl(Arrive) }
        at(4.7.seconds, lasts = 1.3.seconds) { tapControl(OpenBox) }
        at(7.0.seconds, lasts = 1.2.seconds) { tapControl(ReadFirst) }
        at(9.0.seconds, lasts = 1.2.seconds) { tapControl(MarkAllRead) }
        at(12.0.seconds, lasts = 1.3.seconds) { tapControl(Back) }
        at(15.2.seconds) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "mailbox.badge",
                Res.string.demo_mailbox,
                delivery,
                description = Res.string.demo_mailbox_description,
                icon = Res.drawable.ic_demo_mailbox,
                tags = setOf(DemoTag.Canvas, DemoTag.Physics, DemoTag.Gesture),
                tall = true,
            ) {
                MailboxDemo(it)
            },
        )
}
