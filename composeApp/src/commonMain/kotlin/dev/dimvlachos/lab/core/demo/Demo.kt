package dev.dimvlachos.lab.core.demo

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource

internal class Demo(
    val id: String,
    val title: StringResource,
    val script: DemoScript,
    // A demo wider than it is tall: the app turns to landscape while it runs, and its clip is 16:9.
    val landscape: Boolean = false,
    // A demo that needs more height than 4:5 gives it, like a feed: its clip is 9:16.
    val tall: Boolean = false,
    /** Whether the script starts by itself on the phone; a recording always plays it. */
    val autoplay: Boolean = true,
    val content: @Composable (DemoState) -> Unit,
) {
    init {
        require(!(landscape && tall)) { "Demo $id can't be both landscape and tall" }
    }
}
