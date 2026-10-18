package dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf

import androidx.compose.runtime.staticCompositionLocalOf
import dev.dimvlachos.lab.core.platform.SilentSoundPlayer
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.SoundPlayer

/** The sounds a shelf makes: none, unless a player is given. */
internal val LocalShelfSounds = staticCompositionLocalOf<SoundPlayer> { SilentSoundPlayer }

/** Every sound the shelf can make, to load up front. */
internal val ShelfSoundSet = setOf(Sound.BookSlide, Sound.BookOpen, Sound.PageFlip, Sound.RingTick)

/**
 * The sound of the shelf going [from] one mode [to] another: a book sliding out or back, its cover
 * opening; a tip that falls back makes none.
 */
internal fun shelfSoundFor(from: ShelfMode, to: ShelfMode): Sound? =
    when {
        to == ShelfMode.Out && from != ShelfMode.Out && from != ShelfMode.Open -> Sound.BookSlide
        to == ShelfMode.Back && from != ShelfMode.Back -> Sound.BookSlide
        to == ShelfMode.Open && from == ShelfMode.Out -> Sound.BookOpen
        else -> null
    }
