package dev.dimvlachos.lab.typewriterdemo

import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_typewriter
import dev.dimvlachos.lab.resources.demo_typewriter_description
import dev.dimvlachos.lab.resources.ic_demo_typewriter
import dev.dimvlachos.lab.resources.typewriterchat_script_1
import dev.dimvlachos.lab.resources.typewriterchat_script_1b
import dev.dimvlachos.lab.resources.typewriterchat_script_2
import dev.dimvlachos.lab.typewriterdemo.presentation.components.TypewriterDemo
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object TypewriterDemos {
    // A pull too short to send, which slides back in, and a pull well past the threshold.
    val ShortPull = 34.dp
    val FullPull = 150.dp

    // Two messages, each typed and pulled out. The first rings the bell at the end of a word and
    // a fingertip pushes the lever over for a new line, then, after a pull too short that slides
    // back, it is pulled out; the second returns the carriage by itself at the bell and strikes
    // out a typo with the tape. Jess answers each. Once both answers are in, everything sent is
    // taken back, so the next run starts from the same thread.
    private val thread =
        demoScript(holdEnd = 400.milliseconds) {
            at(0.6.seconds, lasts = 1.8.seconds) {
                sendMessage(Res.string.typewriterchat_script_1, typing = 1.6.seconds)
            }
            at(2.6.seconds, lasts = 1.seconds) { pushTypewriterLever(550.milliseconds) }
            at(3.9.seconds, lasts = 1.7.seconds) {
                sendMessage(Res.string.typewriterchat_script_1b, typing = 1.5.seconds)
            }
            at(5.9.seconds, lasts = 0.8.seconds) {
                pullTypewriterSheet(ShortPull, 350.milliseconds)
            }
            at(7.seconds, lasts = 0.9.seconds) { pullTypewriterSheet(FullPull, 450.milliseconds) }
            at(10.8.seconds, lasts = 3.2.seconds) {
                sendMessage(Res.string.typewriterchat_script_2, typing = 3.seconds)
            }
            at(14.4.seconds, lasts = 0.9.seconds) {
                pullTypewriterSheet(FullPull, 450.milliseconds)
            }
            at(19.4.seconds, lasts = 0.6.seconds) { clearMessages() }
        }

    // A chat is to type into: the script would type over the hand, so on the phone it waits for
    // Replay.
    val all =
        listOf(
            Demo(
                "typewriter.composer",
                Res.string.demo_typewriter,
                thread,
                description = Res.string.demo_typewriter_description,
                icon = Res.drawable.ic_demo_typewriter,
                tags = setOf(DemoTag.Text, DemoTag.Gesture, DemoTag.Physics, DemoTag.Canvas),
                tall = true,
            ) {
                TypewriterDemo(it)
            }
        )
}
