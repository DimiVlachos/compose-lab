package dev.dimvlachos.lab.core.demo

import kotlin.time.Duration
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class DemoScript(
    val holdStart: Duration,
    val holdEnd: Duration,
    val steps: List<DemoStep>,
) {
    val nominalDuration: Duration
        get() = holdStart + (steps.maxOfOrNull { it.at + it.lasts } ?: Duration.ZERO) + holdEnd

    suspend fun play(controller: DemoController) {
        delay(holdStart)
        coroutineScope {
            var elapsed = Duration.ZERO
            for (step in steps.sortedBy { it.at }) {
                delay(step.at - elapsed)
                elapsed = step.at
                launch { step.action(controller) }
            }
        }
        delay(holdEnd)
    }
}
