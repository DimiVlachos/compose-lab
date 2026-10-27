package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/**
 * The running app's Appearance screen round the paint roller, as the prototype styles it (`--a-*`):
 * warm paper by day and near black by night. [chip] is the pills' and tracks' tint, [off] a switch
 * that is off, [map] the route map's ground and [gloss] how strongly wet paint shines: less on the
 * pale night accents, where a white sheen turns milky.
 */
internal class PaintRollerScreenColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val chip: Color,
    val off: Color,
    val map: Color,
    val gloss: Float,
    /** The ink on an accent fill. */
    val onAccent: Color,
) {
    internal companion object {
        val Light =
            PaintRollerScreenColors(
                background = Color(0xFFF3F1ED),
                card = Color(0xFFFFFFFF),
                ink = Color(0xFF1F2228),
                muted = Color(0xFF737780),
                line = Color(0xFFE3E0DA),
                chip = Color(30, 30, 40, (.06f * 255).toInt()),
                off = Color(0xFFD7D4CE),
                map = Color(0xFFE6EADF),
                gloss = 1f,
                onAccent = Color(0xFFFFFFFF),
            )
        val Dark =
            PaintRollerScreenColors(
                background = Color(0xFF141518),
                card = Color(0xFF1F2024),
                ink = Color(0xFFECEAE6),
                muted = Color(0xFF9D9EA6),
                line = Color(0xFF303137),
                chip = Color(255, 255, 255, (.07f * 255).toInt()),
                off = Color(0xFF44454C),
                map = Color(0xFF23282A),
                gloss = .5f,
                onAccent = Color(0xFF10131A),
            )

        /** The snackbar's ground and text, the same in both themes. */
        val Snack = Color(0xFF24262B)
        val SnackText = Color(0xFFF2F1EE)

        /** The map's green way under the route, `#9cc7a0` at .55. */
        val GreenWay = Color(0x8C9CC7A0)
    }
}

/** One of the five paints: its name's index and its light and dark accents, as CSS hex. */
internal class RollerPaint(val light: String, val dark: String) {
    fun css(dark: Boolean) = if (dark) this.dark else light
}

/** The prototype's `PAINTS`: Blue Ridge, Sedona, Evergreen, Huckleberry and Firehouse. */
internal val RollerPaints =
    listOf(
        RollerPaint("#2b5fc7", "#8fb0ff"),
        RollerPaint("#cf5521", "#ff9363"),
        RollerPaint("#22784a", "#6fd39a"),
        RollerPaint("#7a3d9e", "#c79cf2"),
        RollerPaint("#c62f2b", "#ff8079"),
    )
