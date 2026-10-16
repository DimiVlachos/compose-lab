@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.snowglobedemo

import dev.dimvlachos.lab.core.presentation.components.snowglobe.GlobeScene
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class WeatherBoardTest {
    @Test
    fun itStartsOnTodayWithFiveDaysAndTomorrowSunny() {
        val board = WeatherBoard()
        assertEquals(5, board.days.size)
        assertEquals(0, board.selected)
        assertEquals(GlobeScene.Sunny, board.days[1].scene)
        assertEquals("6:42 PM", board.updated)
    }

    @Test
    fun aRefreshTurnsTomorrowIntoAShower() = runTest {
        val board = WeatherBoard()
        val source = FakeForecastSource()
        assertTrue(board.startRefresh())
        assertTrue(board.refreshPending)
        board.finishRefresh(source.refresh())
        assertFalse(board.refreshPending)
        assertEquals(GlobeScene.Shower, board.days[1].scene)
        assertEquals("6:48 PM", board.updated)
        assertEquals("tomorrow", board.lastChange?.id)
    }

    @Test
    fun selectingADayDoesNotRefresh() {
        val board = WeatherBoard()
        assertEquals(1, board.select(2))
        assertEquals(2, board.selected)
        assertFalse(board.refreshPending)
        assertEquals(-1, board.select(0))
        assertEquals(0, board.select(0))
        assertEquals(0, board.select(9), "out of range is ignored")
    }

    @Test
    fun aSecondRefreshWhilePendingIsIgnored() {
        val board = WeatherBoard()
        assertTrue(board.startRefresh())
        assertFalse(board.startRefresh())
    }

    @Test
    fun aFailedRefreshKeepsTheOldDaysAndTheNextOneRetries() = runTest {
        val board = WeatherBoard()
        val source = FakeForecastSource().apply { failNext = true }
        board.startRefresh()
        board.finishRefresh(source.refresh())
        assertTrue(board.failed)
        assertEquals(GlobeScene.Sunny, board.days[1].scene)
        assertNull(board.lastChange)
        assertTrue(board.startRefresh())
        board.finishRefresh(source.refresh())
        assertFalse(board.failed)
        assertEquals(GlobeScene.Shower, board.days[1].scene)
    }

    @Test
    fun resetPutsTheBoardBackAsItBegan() = runTest {
        val board = WeatherBoard()
        board.startRefresh()
        board.finishRefresh(FakeForecastSource().refresh())
        board.select(3)
        board.reset()
        assertEquals(0, board.selected)
        assertEquals(GlobeScene.Sunny, board.days[1].scene)
        assertFalse(board.refreshPending)
    }
}
