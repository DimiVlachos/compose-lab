package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The paper and inks of the card file's contact cards, which look the same whatever the theme: the
 * scene is always night under the lamp.
 */
internal object CardFilePaper {
    /** A pulled-out card's cream stock. */
    val cream = CardFileColors.paper

    /** The typed name across the top. */
    val typed = Color(0xFF2A2420)

    /** The faint red rule under the name. */
    val rule = Color(0x55B04040)

    /** The blue ink of the handwritten number and note. */
    val ink = Color(0xFF1F3A7A)
}
