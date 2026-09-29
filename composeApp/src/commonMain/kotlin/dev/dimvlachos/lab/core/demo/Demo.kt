package dev.dimvlachos.lab.core.demo

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource

/**
 * An [interactive] demo is used, not played: it has no script to stop or replay, so its screen
 * leaves those controls out.
 */
class Demo(
    val id: String,
    val title: StringResource,
    val script: DemoScript,
    val interactive: Boolean = false,
    val content: @Composable (DemoState) -> Unit,
)
