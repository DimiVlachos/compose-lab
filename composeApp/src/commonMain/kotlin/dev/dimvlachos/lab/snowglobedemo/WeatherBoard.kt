package dev.dimvlachos.lab.snowglobedemo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * What the weather screen shows: the [days], which one is [selected], when the forecast was
 * [updated], and whether a refresh is [refreshPending] or the last one [failed]. [lastChange] is
 * the day a refresh changed, to announce.
 */
@Stable
internal class WeatherBoard(private val initial: Forecast = WellfleetForecast) {
    var days by mutableStateOf(initial.days)
        private set

    var updated by mutableStateOf(initial.updated)
        private set

    var selected by mutableIntStateOf(0)
        private set

    var refreshPending by mutableStateOf(false)
        private set

    var failed by mutableStateOf(false)
        private set

    var lastChange by mutableStateOf<DayForecast?>(null)
        private set

    val selectedDay: DayForecast
        get() = days[selected]

    /** Selects day [index]; returns which way that moved, or 0 if it didn't. */
    fun select(index: Int): Int {
        if (index !in days.indices || index == selected) return 0
        val direction = if (index > selected) 1 else -1
        selected = index
        return direction
    }

    /** Starts a refresh, unless one is already pending. */
    fun startRefresh(): Boolean {
        if (refreshPending) return false
        refreshPending = true
        return true
    }

    fun finishRefresh(result: Result<Forecast>) {
        refreshPending = false
        result
            .onSuccess { forecast ->
                lastChange = forecast.days.firstOrNull { new -> days.none { it === new } }
                days = forecast.days
                updated = forecast.updated
                failed = false
            }
            .onFailure {
                failed = true
                lastChange = null
            }
    }

    fun reset() {
        days = initial.days
        updated = initial.updated
        selected = 0
        refreshPending = false
        failed = false
        lastChange = null
    }
}

@Composable internal fun rememberWeatherBoard(): WeatherBoard = remember { WeatherBoard() }
