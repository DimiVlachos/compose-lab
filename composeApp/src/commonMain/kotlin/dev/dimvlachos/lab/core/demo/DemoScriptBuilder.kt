package dev.dimvlachos.lab.core.demo

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class DemoScriptBuilder internal constructor() {
    private val steps = mutableListOf<DemoStep>()

    /** Plays [action] at [time]; one that goes on, such as a mist, [lasts] that long. */
    fun at(
        time: Duration,
        lasts: Duration = Duration.ZERO,
        action: suspend DemoController.() -> Unit,
    ) {
        steps += DemoStep(time, action, lasts)
    }

    internal fun build(holdStart: Duration, holdEnd: Duration) =
        DemoScript(holdStart, holdEnd, steps.toList())
}

fun demoScript(
    holdStart: Duration = 600.milliseconds,
    holdEnd: Duration = 600.milliseconds,
    block: DemoScriptBuilder.() -> Unit,
): DemoScript = DemoScriptBuilder().apply(block).build(holdStart, holdEnd)
