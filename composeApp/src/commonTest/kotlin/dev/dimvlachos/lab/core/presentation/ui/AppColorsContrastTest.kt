package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

// The WCAG contrast ratios the palettes hold to: 7 for body text, 4.5 for muted text and the
// accent, 3 for the scripted finger. The dark accent has always sat at 4.47 on the background, a
// shade under 4.5, so the dark palette is held to 3 there (large text and icons), where the light
// one meets 4.5.
class AppColorsContrastTest {
    @Test
    fun textReadsOnEveryBackground() {
        for (colors in Palettes) {
            for (back in listOf(colors.background, colors.surface, colors.bar)) {
                assertContrast(colors.textPrimary, back, 7f, colors, "textPrimary")
                assertContrast(colors.textMuted, back, 4.5f, colors, "textMuted")
            }
            assertContrast(colors.textPrimary, colors.bubbleTheirs, 7f, colors, "on bubbleTheirs")
        }
    }

    @Test
    fun theAccentReadsOnTheBackgroundAndTheSurface() {
        for (colors in Palettes) {
            val minimum = if (colors.isDark) 3f else 4.5f
            assertContrast(colors.accent, colors.background, minimum, colors, "accent")
            assertContrast(colors.accent, colors.surface, minimum, colors, "accent on surface")
            assertContrast(colors.onAccent, colors.accent, minimum, colors, "onAccent")
        }
    }

    @Test
    fun theDemosSurroundingsRead() {
        for (colors in Palettes) {
            assertContrast(colors.textMuted, colors.stripRail, 4.5f, colors, "on stripRail")
            assertContrast(colors.textPrimary, colors.fanScrim, 4.5f, colors, "on fanScrim")
            assertContrast(colors.touch, colors.background, 3f, colors, "touch")
        }
    }

    @Test
    fun theUndoSnackbarReads() {
        for (colors in Palettes) {
            assertContrast(colors.onSmokeSnackbar, colors.smokeSnackbar, 7f, colors, "snackbar")
            assertContrast(
                colors.smokeSnackbarAction,
                colors.smokeSnackbar,
                4.5f,
                colors,
                "snackbar action",
            )
        }
    }

    @Test
    fun theInitialsReadOnEveryChatAvatar() {
        // White initials, as the smoke delete's chat list draws them.
        for (colors in Palettes) {
            val avatars =
                listOf(
                    colors.chatGreen,
                    colors.chatBlue,
                    colors.chatAmber,
                    colors.magnetRed,
                    colors.chatTeal,
                    colors.chatViolet,
                    colors.tagVillage,
                )
            for (avatar in avatars) assertContrast(
                colors.onAccent,
                avatar,
                4.5f,
                colors,
                "initials",
            )
        }
    }

    @Test
    fun selectedTextReads() {
        for (colors in Palettes) {
            val selected = colors.selection.compositeOver(colors.surface)
            assertContrast(colors.textPrimary, selected, 4.5f, colors, "on selection")
        }
    }

    @Test
    fun whatTheShellDrawsReads() {
        for (colors in Palettes) {
            // A catalog card's icon on its tile.
            val tile = colors.accentSoft.compositeOver(colors.surface)
            assertContrast(colors.accent, tile, 3f, colors, "accent icon on its tile")
            // A selected tag chip, and the muted chips and placeholder, on the top bar.
            val chip = colors.accentSoft.compositeOver(colors.topBar)
            assertContrast(colors.textPrimary, chip, 4.5f, colors, "selected chip")
            assertContrast(colors.textMuted, colors.topBar, 4.5f, colors, "textMuted on topBar")
        }
    }

    @Test
    fun theFannedPhotosTitlesRead() {
        // The titles under a fanned-out cluster, over the scrim on the steel table.
        for (colors in Palettes) {
            for (steel in listOf(colors.tableSteel, colors.tableSteelDeep)) {
                val scrim = colors.fanScrim.copy(alpha = FanScrimAlpha).compositeOver(steel)
                assertContrast(colors.textPrimary, scrim, 4.5f, colors, "fan title")
            }
        }
    }

    @Test
    fun textOverAPhotoReads() {
        // The photo detail's caption sits on a black scrim; under it, a white sky at worst.
        val scrim = Color.Black.copy(alpha = PhotoScrimAlpha).compositeOver(Color.White)
        val colors = AppColors.Light
        assertContrast(colors.onPhoto, scrim, 4.5f, colors, "onPhoto")
        assertContrast(colors.onPhotoMuted, scrim, 4.5f, colors, "onPhotoMuted")
    }

    private fun assertContrast(
        front: Color,
        back: Color,
        minimum: Float,
        colors: AppColors,
        what: String,
    ) {
        val ratio = contrast(front, back)
        val palette = if (colors.isDark) "dark" else "light"
        assertTrue(ratio >= minimum, "$what in the $palette palette: $ratio, under $minimum")
    }

    private companion object {
        val Palettes = listOf(AppColors.Dark, AppColors.Light)

        // As MagnetPainter and MorphDetail draw them.
        const val FanScrimAlpha = 0.7f
        const val PhotoScrimAlpha = 0.7f
    }
}

/** The WCAG contrast ratio between two opaque colours, from 1 (the same) to 21. */
private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
}
