package dev.dimvlachos.lab.core.demo

import kotlin.time.Duration

internal class DemoStep(
    val at: Duration,
    val action: suspend DemoController.() -> Unit,
    /** How long the action goes on for after it starts. */
    val lasts: Duration = Duration.ZERO,
)
