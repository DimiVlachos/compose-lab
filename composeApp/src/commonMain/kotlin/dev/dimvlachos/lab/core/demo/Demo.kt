package dev.dimvlachos.lab.core.demo

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource

class Demo(
    val id: String,
    val title: StringResource,
    val script: DemoScript,
    val content: @Composable (DemoState) -> Unit,
)
