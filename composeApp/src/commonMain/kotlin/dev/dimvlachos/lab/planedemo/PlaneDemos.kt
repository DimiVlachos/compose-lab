package dev.dimvlachos.lab.planedemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.planedemo.presentation.components.PlaneDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.chat_script_1
import dev.dimvlachos.lab.resources.chat_script_2
import dev.dimvlachos.lab.resources.demo_paper_plane
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object PlaneDemos {
    // A send takes about four seconds from the first letter: the typing, the letters going into
    // the button, the throw, and the letters dropped where the message lies. Once both have
    // been read, they are taken back, so the next run starts from the same conversation.
    private val chat =
        demoScript(holdEnd = 400.milliseconds) {
            at(0.6.seconds, lasts = 4.6.seconds) {
                sendMessage(Res.string.chat_script_1, typing = 1.5.seconds)
            }
            at(6.2.seconds, lasts = 3.6.seconds) {
                sendMessage(Res.string.chat_script_2, typing = 0.6.seconds)
            }
            at(11.seconds, lasts = 0.6.seconds) { clearMessages() }
        }

    // A chat is to type into: the script would type over the hand, so on the phone it waits for
    // Replay.
    val all =
        listOf(
            Demo("chat.plane", Res.string.demo_paper_plane, chat, autoplay = false) {
                PlaneDemo(it)
            }
        )
}
