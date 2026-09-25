package dev.dimvlachos.lab.core.demo

import kotlin.time.Duration

class DemoStep(
    val at: Duration,
    val action: suspend DemoController.() -> Unit,
)
