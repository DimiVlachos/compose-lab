package dev.dimvlachos.lab.core.presentation.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
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

    @Test
    fun theThemedTokensChangeWithThePalette() {
        for (token in ThemedTokens) {
            if (token in SameInBoth) continue
            assertNotEquals(token.get(AppColors.Dark), token.get(AppColors.Light), token.name)
        }
        for (token in SameInBoth) {
            assertEquals(token.get(AppColors.Dark), token.get(AppColors.Light), token.name)
        }
    }

    // A new colour must be sorted into the art or the themed tokens before this passes.
    @Test
    fun everyTokenIsArtOrThemed() {
        val names = PropertyName.findAll(AppColors().toString()).map { it.groupValues[1] }.toList()
        assertEquals(TokenCount + 1, names.size, "isDark and $TokenCount colours")
        assertEquals(TokenCount, ArtTokens.size + ThemedTokens.size)
        val sorted = (ArtTokens + ThemedTokens).map { it.name }.toSet() + "isDark"
        assertEquals(names.toSet(), sorted)
    }

    private companion object {
        const val TokenCount = 67

        val PropertyName = Regex("""(\w+)=""")

        val ThemedTokens =
            listOf(
                AppColors::background,
                AppColors::surface,
                AppColors::bar,
                AppColors::textPrimary,
                AppColors::textMuted,
                AppColors::accent,
                AppColors::accentSoft,
                AppColors::onAccent,
                AppColors::topBar,
                AppColors::topBarShadow,
                AppColors::chipBorder,
                AppColors::selection,
                AppColors::sendDisc,
                AppColors::table,
                AppColors::tableLit,
                AppColors::touch,
                AppColors::bubbleTheirs,
                AppColors::tableSteel,
                AppColors::tableSteelDeep,
                AppColors::stripRail,
                AppColors::stripWell,
                AppColors::filing,
                AppColors::cardShadow,
                AppColors::fanScrim,
                AppColors::waterCrest,
                AppColors::rodBlank,
                AppColors::rodReel,
                AppColors::fishingLine,
            )

        // Themed, but the same in both: white on the accent, and a black shadow under the bar.
        val SameInBoth = listOf(AppColors::onAccent, AppColors::topBarShadow)

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
