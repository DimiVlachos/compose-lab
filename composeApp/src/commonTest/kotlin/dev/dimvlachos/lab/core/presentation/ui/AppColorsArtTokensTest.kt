package dev.dimvlachos.lab.core.presentation.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppColorsArtTokensTest {
    @Test
    fun theDarkPaletteIsTheDefaults() {
        assertEquals(AppColors(), AppColors.Dark)
        assertTrue(AppColors.Dark.isDark)
        assertFalse(AppColors.Light.isDark)
    }

    @Test
    fun theArtIsTheSameInBothPalettes() {
        for (token in ArtTokens) {
            assertEquals(token.get(AppColors.Dark), token.get(AppColors.Light), token.name)
        }
    }

    private companion object {
        val ArtTokens =
            listOf(
                AppColors::pageShade,
                AppColors::pageGlare,
                AppColors::bookGutter,
                AppColors::bookCrease,
                AppColors::thread,
                AppColors::threadTwist,
                AppColors::pageEdge,
                AppColors::pageEdgeLine,
                AppColors::paperBack,
                AppColors::paper,
                AppColors::lampShade,
                AppColors::lampShadeSheen,
                AppColors::lampBrass,
                AppColors::lampCord,
                AppColors::bulbOff,
                AppColors::bulbLit,
                AppColors::lampLight,
                AppColors::cardPaper,
                AppColors::clipMetal,
                AppColors::clipSheen,
                AppColors::magnetRed,
                AppColors::magnetSteel,
                AppColors::magnetSheen,
                AppColors::badge,
                AppColors::onBadge,
                AppColors::tagSunset,
                AppColors::tagSea,
                AppColors::tagBoats,
                AppColors::tagCliffs,
                AppColors::tagVillage,
                AppColors::waterTop,
                AppColors::waterDeep,
                AppColors::waterLight,
                AppColors::rodCork,
                AppColors::bobberRed,
                AppColors::bobberWhite,
                AppColors::mirrorGlass,
                AppColors::onPhoto,
                AppColors::onPhotoMuted,
            )
    }
}
