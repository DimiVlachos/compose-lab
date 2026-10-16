@file:OptIn(ExperimentalCoroutinesApi::class)

package dev.dimvlachos.lab.snowglobedemo

import dev.dimvlachos.lab.core.presentation.components.snowglobe.GlobeScene
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.snowglobe_updated_now
import dev.dimvlachos.lab.resources.snowglobe_updated_stale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class WeatherBoardTest {
    @Test
    fun itOpensOnTomorrowInOiaWithAStaleSunnyForecast() {
        val board = WeatherBoard()
        assertEquals(5, board.days.size)
        assertEquals(1, board.selected)
        assertEquals(GlobeScene.Sunny, board.days[1].scene)
        assertEquals(25, board.days[1].high)
        assertEquals(Res.string.snowglobe_updated_stale, board.updated)
    }

    @Test
    fun theDaysAreThePrototypes() {
        val days = WeatherBoard().days
        assertEquals(listOf(26, 25, 23, 25, 24), days.map { it.high })
        assertEquals(listOf(19, 18, 18, 19, 18), days.map { it.low })
        assertEquals(
            listOf(
                GlobeScene.Sunny,
                GlobeScene.Sunny,
                GlobeScene.Windy,
                GlobeScene.Sunny,
                GlobeScene.Windy,
            ),
            days.map { it.scene },
        )
    }

    @Test
    fun aRefreshTurnsThursdayIntoAShower() = runTest {
        val board = WeatherBoard()
        val token = board.startRefresh()
        assertTrue(token != null)
        assertTrue(board.refreshPending)
        board.finishRefresh(token, FakeForecastSource().refresh())
        assertFalse(board.refreshPending)
        assertEquals(GlobeScene.Shower, board.days[1].scene)
        assertEquals(22, board.days[1].high)
        assertEquals(Res.string.snowglobe_updated_now, board.updated)
        assertEquals("thu", board.lastChange?.id)
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
        assertTrue(board.startRefresh() != null)
        assertNull(board.startRefresh())
    }

    @Test
    fun aFailedRefreshKeepsTheOldDaysAndTheNextOneRetries() = runTest {
        val board = WeatherBoard()
        val source = FakeForecastSource().apply { failNext = true }
        board.finishRefresh(board.startRefresh(), source.refresh())
        assertTrue(board.failed)
        assertEquals(GlobeScene.Sunny, board.days[1].scene)
        assertNull(board.lastChange)
        board.finishRefresh(board.startRefresh(), source.refresh())
        assertFalse(board.failed)
        assertEquals(GlobeScene.Shower, board.days[1].scene)
    }

    @Test
    fun resetPutsTheBoardBackAsItBegan() = runTest {
        val board = WeatherBoard()
        board.finishRefresh(board.startRefresh(), FakeForecastSource().refresh())
        board.select(3)
        board.reset()
        assertEquals(1, board.selected)
        assertEquals(GlobeScene.Sunny, board.days[1].scene)
        assertEquals(Res.string.snowglobe_updated_stale, board.updated)
        assertFalse(board.refreshPending)
    }

    @Test
    fun aRefreshThatLandsAfterAResetIsDropped() = runTest {
        val board = WeatherBoard()
        val token = board.startRefresh()
        board.reset()
        board.finishRefresh(token, FakeForecastSource().refresh())
        assertEquals(GlobeScene.Sunny, board.days[1].scene)
        assertEquals(Res.string.snowglobe_updated_stale, board.updated)
        assertFalse(board.refreshPending)
        assertTrue(board.startRefresh() != null, "a new refresh can start after the reset")
    }
}
