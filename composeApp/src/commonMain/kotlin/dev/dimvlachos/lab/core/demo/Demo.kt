package dev.dimvlachos.lab.core.demo

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource

class Demo(
    val id: String,
    val title: StringResource,
    val script: DemoScript,
    // A demo wider than it is tall: the app turns to landscape while it runs, and its clip is 16:9.
    val landscape: Boolean = false,
    val content: @Composable (DemoState) -> Unit,
)
