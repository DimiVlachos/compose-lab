package dev.dimvlachos.lab.core.demo

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/**
 * A demo in the catalog: its [id] and [title], the [script] that plays it, and its [content], the
 * stage. Its card shows its [icon] and a one-line [description], and its [tags] (at least one) let
 * the catalog be filtered.
 *
 * On the phone the stage sits under the demo screen's top bar, which holds the top of the screen:
 * the status bar and the cutout's top inset are already taken. The stage reaches the sides and the
 * foot of the screen, so a demo keeps its controls clear of them with [demoInsets], which is empty
 * while recording, when the clip's frame touches no edge.
 */
internal class Demo(
    val id: String,
    val title: StringResource,
    val script: DemoScript,
    val description: StringResource,
    val icon: DrawableResource,
    val tags: Set<DemoTag>,
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
        require(tags.isNotEmpty()) { "Demo $id needs at least one tag" }
    }
}
