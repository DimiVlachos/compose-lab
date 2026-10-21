package dev.dimvlachos.lab.padlockdemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.padlockdemo.presentation.components.PadlockDemo
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_padlock
import dev.dimvlachos.lab.resources.demo_padlock_description
import dev.dimvlachos.lab.resources.ic_demo_padlock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object PadlockDemos {
    const val Focus = "focus"
    const val Peek = "peek"

    /** The script's typing pace, as the prototype's: one character every 150 ms. */
    val KeyEvery: Duration = 150.milliseconds

    /** Erasing is a touch slower. */
    val EraseEvery: Duration = 170.milliseconds

    private const val TypePrefix = "type:"
    private const val ErasePrefix = "erase:"

    /** The control that types [text] into the password field, a character at a time. */
    fun type(text: String) = TypePrefix + text

    /** The control that erases the last [count] characters. */
    fun erase(count: Int) = ErasePrefix + count

    /** What [type] typed, or null for another control. */
    fun typed(control: String): String? = control.removePrefix(TypePrefix).takeIf { it != control }

    /** How many characters [erase] erases, or null for another control. */
    fun erased(control: String): Int? =
        control.removePrefix(ErasePrefix).takeIf { it != control }?.toIntOrNull()

    // The prototype's clip: the field tapped, "maple42-River!" typed in the chunks a person types
    // it in, the lock climbing from weak to strong and dropping shut; the lock tapped to show the
    // password and again to hide it; three characters erased so it springs open, and "er!" typed so
    // it locks again. The field is cleared at the end, so the clip loops.
    private val password = demoScript {
        var t = 1.25.seconds
        at(t, lasts = 150.milliseconds) { tapControl(Focus) }
        t += 300.milliseconds
        for ((chunk, pause) in Chunks) {
            val typing = KeyEvery * chunk.length
            at(t, lasts = typing) { tapControl(type(chunk)) }
            t += typing + pause
        }
        t += 560.milliseconds
        at(t, lasts = 300.milliseconds) { tapControl(Peek) }
        t += 1960.milliseconds
        at(t, lasts = 300.milliseconds) { tapControl(Peek) }
        t += 500.milliseconds
        at(t, lasts = EraseEvery * 3) { tapControl(erase(3)) }
        t += EraseEvery * 3 + 800.milliseconds
        at(t, lasts = EraseEvery * 3) { tapControl(type("er!")) }
        t += EraseEvery * 3 + 1200.milliseconds
        at(t, lasts = 1500.milliseconds) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "padlock.password",
                Res.string.demo_padlock,
                password,
                description = Res.string.demo_padlock_description,
                icon = Res.drawable.ic_demo_padlock,
                tags = setOf(DemoTag.Physics, DemoTag.Canvas),
                tall = true,
            ) {
                PadlockDemo(it)
            },
        )
}

/** The chunks the script types, each with the pause after it, as the prototype's. */
private val Chunks =
    listOf(
        "maple" to 520.milliseconds,
        "42" to 520.milliseconds,
        "-" to 480.milliseconds,
        "R" to 380.milliseconds,
        "ive" to 120.milliseconds,
        "r!" to 900.milliseconds,
    )
