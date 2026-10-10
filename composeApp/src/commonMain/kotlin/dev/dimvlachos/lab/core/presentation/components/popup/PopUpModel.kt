package dev.dimvlachos.lab.core.presentation.components.popup

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * Which page of a spread a pop-up piece stands on: the near one, towards the reader, or the far.
 */
public enum class PopUpSide {
    Near,
    Far,
}

/** How a piece moves with its spread's pull tab. */
public sealed interface PieceMotion {
    /**
     * Slides along the gutter from x = [from] (the tab pushed in) to [to] (pulled all the way out),
     * bobbing [bob] book units up and down as a boat on a swell does.
     */
    public data class RidesTab(val from: Float, val to: Float, val bob: Float = 0f) : PieceMotion

    /**
     * Spins about ([pivotX], [pivotY]), in book units from the art's top left, when the tab is
     * pulled out, and coasts down once it lets go, as windmill sails do.
     */
    public data class Spins(val pivotX: Float, val pivotY: Float) : PieceMotion
}

/**
 * A piece of paper that stands up out of a spread as it opens: its [art], the page it stands on
 * ([side]), how far from the gutter its foot is ([fromGutter]), where it starts along the gutter
 * ([x], from the book's middle), and its [width] and [height], all in book units (a page is 300
 * wide and 190 deep). [lift] raises it along its own plane. Folded, it lies face down between the
 * pages.
 */
@Immutable
public class PopUpPiece(
    public val art: Painter,
    public val side: PopUpSide,
    public val fromGutter: Float,
    public val x: Float,
    public val width: Float,
    public val height: Float,
    public val lift: Float = 0f,
    public val castsShadow: Boolean = true,
    public val motion: PieceMotion? = null,
)

/**
 * A paper tab that slides out of a slit at the outer edge of a spread's near page, up to [travel]
 * book units, and springs back in when let go.
 */
@Immutable public class PullTab(public val travel: Float = 58f)

/**
 * One open spread of a pop-up book: its [near] page (towards the reader) and its [far] page, the
 * [pieces] that stand up between them, and an optional pull [tab].
 */
@Immutable
public class PopUpSpread(
    public val near: Painter,
    public val far: Painter,
    public val pieces: List<PopUpPiece>,
    public val tab: PullTab? = null,
)
