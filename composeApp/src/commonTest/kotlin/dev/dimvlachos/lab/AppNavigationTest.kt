package dev.dimvlachos.lab

import dev.dimvlachos.lab.catalog.Catalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppNavigationTest {
    private val morphApp = Catalog.find("morph.app")!!
    private val morphSection = Catalog.sections[1]

    @Test
    fun backStepsFromADemoToItsSectionThenHome() {
        val navigation = AppNavigation(initialDemo = null, record = false)
        navigation.openSection(morphSection)
        navigation.openDemo(morphApp)
        navigation.back()
        assertNull(navigation.demo)
        assertEquals(morphSection, navigation.section)
        navigation.back()
        assertNull(navigation.section)
    }

    @Test
    fun theHomeScreenLeavesBackToTheSystem() {
        assertFalse(AppNavigation(initialDemo = null, record = false).canGoBack)
    }

    @Test
    fun aDemoOpenedByIdGoesBackToItsSection() {
        val navigation = AppNavigation(initialDemo = morphApp, record = false)
        assertTrue(navigation.canGoBack)
        navigation.back()
        assertEquals(morphSection, navigation.section)
    }

    @Test
    fun theRecordedDemoCannotBeLeft() {
        val navigation = AppNavigation(initialDemo = morphApp, record = true)
        assertFalse(navigation.canGoBack)
        navigation.back()
        assertEquals(morphApp, navigation.demo)
    }
}
