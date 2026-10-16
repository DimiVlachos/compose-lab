package dev.dimvlachos.lab.snowglobedemo

import dev.dimvlachos.lab.core.presentation.components.snowglobe.GlobeScene
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.snowglobe_breezy
import dev.dimvlachos.lab.resources.snowglobe_day_fri
import dev.dimvlachos.lab.resources.snowglobe_day_mon
import dev.dimvlachos.lab.resources.snowglobe_day_sat
import dev.dimvlachos.lab.resources.snowglobe_day_sun
import dev.dimvlachos.lab.resources.snowglobe_day_today
import dev.dimvlachos.lab.resources.snowglobe_note_calm
import dev.dimvlachos.lab.resources.snowglobe_note_clear
import dev.dimvlachos.lab.resources.snowglobe_note_gusts
import dev.dimvlachos.lab.resources.snowglobe_note_rain
import dev.dimvlachos.lab.resources.snowglobe_shower
import dev.dimvlachos.lab.resources.snowglobe_sunny
import org.jetbrains.compose.resources.StringResource

/** One day of the forecast: its tab's [day], the [scene] in the globe, and the numbers in °F. */
internal class DayForecast(
    val id: String,
    val day: StringResource,
    val scene: GlobeScene,
    val high: Int,
    val low: Int,
    val conditions: StringResource,
    val note: StringResource,
)

/** Five days, and when they were [updated], as the clock read then. */
internal class Forecast(val days: List<DayForecast>, val updated: String)

private val today =
    DayForecast(
        "today",
        Res.string.snowglobe_day_today,
        GlobeScene.Sunny,
        68,
        55,
        Res.string.snowglobe_sunny,
        Res.string.snowglobe_note_calm,
    )
private val tomorrowSunny =
    DayForecast(
        "tomorrow",
        Res.string.snowglobe_day_fri,
        GlobeScene.Sunny,
        70,
        57,
        Res.string.snowglobe_sunny,
        Res.string.snowglobe_note_clear,
    )
private val tomorrowShower =
    DayForecast(
        "tomorrow",
        Res.string.snowglobe_day_fri,
        GlobeScene.Shower,
        64,
        56,
        Res.string.snowglobe_shower,
        Res.string.snowglobe_note_rain,
    )
private val saturday =
    DayForecast(
        "sat",
        Res.string.snowglobe_day_sat,
        GlobeScene.Windy,
        61,
        50,
        Res.string.snowglobe_breezy,
        Res.string.snowglobe_note_gusts,
    )
private val sunday =
    DayForecast(
        "sun",
        Res.string.snowglobe_day_sun,
        GlobeScene.Shower,
        59,
        51,
        Res.string.snowglobe_shower,
        Res.string.snowglobe_note_rain,
    )
private val monday =
    DayForecast(
        "mon",
        Res.string.snowglobe_day_mon,
        GlobeScene.Sunny,
        66,
        53,
        Res.string.snowglobe_sunny,
        Res.string.snowglobe_note_calm,
    )

/** Wellfleet as the screen opens. */
internal val WellfleetForecast =
    Forecast(listOf(today, tomorrowSunny, saturday, sunday, monday), "6:42 PM")

/** Wellfleet after a refresh: tomorrow has turned to a light shower. */
internal val WellfleetRefreshed =
    Forecast(listOf(today, tomorrowShower, saturday, sunday, monday), "6:48 PM")
