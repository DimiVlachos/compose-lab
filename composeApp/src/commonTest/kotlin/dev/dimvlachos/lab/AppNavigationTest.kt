package dev.dimvlachos.lab

import dev.dimvlachos.lab.catalog.Catalog
import dev.dimvlachos.lab.catalog.CatalogEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppNavigationTest {
    private val morphApp = Catalog.find("morph.app")!!

    @Test
    fun backFromADemoReturnsHome() {
        val navigation = AppNavigation(initialDemo = null, record = false)
        navigation.openDemo(morphApp)
        assertTrue(navigation.canGoBack)
        navigation.back()
        assertNull(navigation.demo)
    }

    @Test
    fun theHomeScreenLeavesBackToTheSystem() {
        assertFalse(AppNavigation(initialDemo = null, record = false).canGoBack)
    }

    @Test
    fun aDemoOpenedByIdGoesBackHome() {
        val navigation = AppNavigation(initialDemo = morphApp, record = false)
        navigation.back()
        assertNull(navigation.demo)
    }

    @Test
    fun theRecordedDemoCannotBeLeft() {
        val navigation = AppNavigation(initialDemo = morphApp, record = true)
        assertFalse(navigation.canGoBack)
        navigation.back()
        assertEquals(morphApp, navigation.demo)
    }

    private val folder = Catalog.entries.filterIsInstance<CatalogEntry.Group>().single()

    @Test
    fun backFromAVersionReturnsToItsFolderThenHome() {
        val navigation = AppNavigation(initialDemo = null, record = false)
        navigation.openGroup(folder)
        assertTrue(navigation.canGoBack)
        navigation.openDemo(folder.demos.first())
        navigation.back()
        assertNull(navigation.demo)
        assertEquals(folder, navigation.group)
        navigation.back()
        assertNull(navigation.group)
        assertFalse(navigation.canGoBack)
    }

    @Test
    fun aVersionRecordedByIdOpensWithoutItsFolder() {
        val bathroom = Catalog.find("fog.mirror.bathroom")!!
        val navigation = AppNavigation(initialDemo = bathroom, record = true)
        assertNull(navigation.group)
        assertFalse(navigation.canGoBack)
    }

    @Test
    fun eachScreenKnowsTheOneBackLeadsTo() {
        val navigation = AppNavigation(initialDemo = null, record = false)
        assertEquals(AppScreen.Home, navigation.screen)
        assertNull(navigation.backScreen)
        navigation.openGroup(folder)
        assertEquals(AppScreen.Folder(folder), navigation.screen)
        assertEquals(AppScreen.Home, navigation.backScreen)
        val version = folder.demos.first()
        navigation.openDemo(version)
        assertEquals(AppScreen.Open(version, folder), navigation.screen)
        assertEquals(AppScreen.Folder(folder), navigation.backScreen)
    }

    @Test
    fun theRecordedDemoHasNothingBehindIt() {
        assertNull(AppNavigation(initialDemo = morphApp, record = true).backScreen)
    }

    @Test
    fun goingDeeperPushesAndGoingBackPops() {
        val version = folder.demos.first()
        val home = AppScreen.Home
        val inFolder = AppScreen.Folder(folder)
        val fromFolder = AppScreen.Open(version, folder)
        val fromHome = AppScreen.Open(morphApp, null)
        assertTrue(inFolder.pushesOver(home))
        assertTrue(fromFolder.pushesOver(inFolder))
        assertTrue(fromHome.pushesOver(home))
        assertFalse(home.pushesOver(inFolder))
        assertFalse(inFolder.pushesOver(fromFolder))
        assertFalse(home.pushesOver(fromHome))
    }
}
