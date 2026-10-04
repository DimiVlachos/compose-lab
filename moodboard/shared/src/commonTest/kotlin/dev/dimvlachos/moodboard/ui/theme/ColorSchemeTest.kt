package dev.dimvlachos.moodboard.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class ColorSchemeTest {
    @Test
    fun screensShareTheBarsAndListsSurface() {
        // One colour behind every screen, bars and list rows: no bands between them.
        assertEquals(BrandLightColors.surface, BrandLightColors.background)
        assertEquals(BrandDarkColors.surface, BrandDarkColors.background)
    }

    @Test
    fun theBrandBlueIsTheLightPrimaryContainer() {
        // Fidelity keeps the seed itself in the scheme, where Android's filled components use it.
        assertEquals(Brand.Seed, BrandLightColors.primaryContainer)
    }
}
