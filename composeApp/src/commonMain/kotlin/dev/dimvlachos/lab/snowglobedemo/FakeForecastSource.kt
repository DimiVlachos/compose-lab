package dev.dimvlachos.lab.snowglobedemo

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

/**
 * A forecast service that takes [delay] to answer: the refreshed Oia forecast, or a failure when
 * [failNext] is set, which it then clears.
 */
internal class FakeForecastSource(private val delay: Duration = 1200.milliseconds) {
    var failNext = false

    suspend fun refresh(): Result<Forecast> {
        delay(delay)
        if (failNext) {
            failNext = false
            return Result.failure(IllegalStateException("The forecast service didn't answer"))
        }
        return Result.success(OiaRefreshed)
    }

    fun reset() {
        failNext = false
    }
}
