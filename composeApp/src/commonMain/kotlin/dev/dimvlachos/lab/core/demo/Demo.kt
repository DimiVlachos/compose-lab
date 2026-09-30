package dev.dimvlachos.lab.core.demo

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource

class Demo(
    val id: String,
    val title: StringResource,
    val script: DemoScript,
    /** Whether the script starts by itself on the phone; a recording always plays it. */
    val autoplay: Boolean = true,
    val content: @Composable (DemoState) -> Unit,
)
