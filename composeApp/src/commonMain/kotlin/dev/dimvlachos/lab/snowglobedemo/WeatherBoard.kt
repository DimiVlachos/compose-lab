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
internal class WeatherBoard(private val initial: Forecast = OiaForecast) {
    var days by mutableStateOf(initial.days)
        private set

    var updated by mutableStateOf(initial.updated)
        private set

    var selected by mutableIntStateOf(OpeningDay)
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

    // Each refresh's token; a reset moves it on, so a refresh that lands afterwards is dropped.
    private var generation = 0

    /** Starts a refresh and returns its token, or null if one is already pending. */
    fun startRefresh(): Int? {
        if (refreshPending) return null
        refreshPending = true
        return ++generation
    }

    /** Lands the refresh [token] started; one from before a [reset] is ignored. */
    fun finishRefresh(token: Int?, result: Result<Forecast>) {
        if (token == null || token != generation || !refreshPending) return
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
        generation++
        days = initial.days
        updated = initial.updated
        selected = OpeningDay
        refreshPending = false
        failed = false
        lastChange = null
    }
}

@Composable internal fun rememberWeatherBoard(): WeatherBoard = remember { WeatherBoard() }
