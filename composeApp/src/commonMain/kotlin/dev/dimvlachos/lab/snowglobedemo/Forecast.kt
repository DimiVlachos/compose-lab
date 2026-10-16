package dev.dimvlachos.lab.snowglobedemo

import dev.dimvlachos.lab.core.presentation.components.snowglobe.GlobeScene
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.snowglobe_breezy
import dev.dimvlachos.lab.resources.snowglobe_label_fri
import dev.dimvlachos.lab.resources.snowglobe_label_sat
import dev.dimvlachos.lab.resources.snowglobe_label_sun
import dev.dimvlachos.lab.resources.snowglobe_label_thu
import dev.dimvlachos.lab.resources.snowglobe_label_today
import dev.dimvlachos.lab.resources.snowglobe_meltemi
import dev.dimvlachos.lab.resources.snowglobe_note_calm_sea
import dev.dimvlachos.lab.resources.snowglobe_note_meltemi
import dev.dimvlachos.lab.resources.snowglobe_note_north
import dev.dimvlachos.lab.resources.snowglobe_note_rain
import dev.dimvlachos.lab.resources.snowglobe_note_rain_low
import dev.dimvlachos.lab.resources.snowglobe_note_uv
import dev.dimvlachos.lab.resources.snowglobe_short_fri
import dev.dimvlachos.lab.resources.snowglobe_short_sat
import dev.dimvlachos.lab.resources.snowglobe_short_sun
import dev.dimvlachos.lab.resources.snowglobe_short_thu
import dev.dimvlachos.lab.resources.snowglobe_short_today
import dev.dimvlachos.lab.resources.snowglobe_shower
import dev.dimvlachos.lab.resources.snowglobe_sunny
import dev.dimvlachos.lab.resources.snowglobe_updated_now
import dev.dimvlachos.lab.resources.snowglobe_updated_stale
import org.jetbrains.compose.resources.StringResource

/**
 * One day of the forecast: its card's [short] name and the header's [label], the [scene] in the
 * globe, and the numbers in °C.
 */
internal class DayForecast(
    val id: String,
    val short: StringResource,
    val label: StringResource,
    val scene: GlobeScene,
    val high: Int,
    val low: Int,
    val conditions: StringResource,
    val note: StringResource,
)

/** Five days, and the line saying how fresh they are. */
internal class Forecast(val days: List<DayForecast>, val updated: StringResource)

private val today =
    DayForecast(
        "today",
        Res.string.snowglobe_short_today,
        Res.string.snowglobe_label_today,
        GlobeScene.Sunny,
        26,
        19,
        Res.string.snowglobe_sunny,
        Res.string.snowglobe_note_uv,
    )
private val thursdaySunny =
    DayForecast(
        "thu",
        Res.string.snowglobe_short_thu,
        Res.string.snowglobe_label_thu,
        GlobeScene.Sunny,
        25,
        18,
        Res.string.snowglobe_sunny,
        Res.string.snowglobe_note_rain_low,
    )
private val thursdayShower =
    DayForecast(
        "thu",
        Res.string.snowglobe_short_thu,
        Res.string.snowglobe_label_thu,
        GlobeScene.Shower,
        22,
        18,
        Res.string.snowglobe_shower,
        Res.string.snowglobe_note_rain,
    )
private val friday =
    DayForecast(
        "fri",
        Res.string.snowglobe_short_fri,
        Res.string.snowglobe_label_fri,
        GlobeScene.Windy,
        23,
        18,
        Res.string.snowglobe_meltemi,
        Res.string.snowglobe_note_meltemi,
    )
private val saturday =
    DayForecast(
        "sat",
        Res.string.snowglobe_short_sat,
        Res.string.snowglobe_label_sat,
        GlobeScene.Sunny,
        25,
        19,
        Res.string.snowglobe_sunny,
        Res.string.snowglobe_note_calm_sea,
    )
private val sunday =
    DayForecast(
        "sun",
        Res.string.snowglobe_short_sun,
        Res.string.snowglobe_label_sun,
        GlobeScene.Windy,
        24,
        18,
        Res.string.snowglobe_breezy,
        Res.string.snowglobe_note_north,
    )

/** Oia as the screen opens, three hours stale: tomorrow still says sunny. */
internal val OiaForecast =
    Forecast(
        listOf(today, thursdaySunny, friday, saturday, sunday),
        Res.string.snowglobe_updated_stale,
    )

/** Oia after a refresh: tomorrow has turned to a light shower. */
internal val OiaRefreshed =
    Forecast(
        listOf(today, thursdayShower, friday, saturday, sunday),
        Res.string.snowglobe_updated_now,
    )

/** The day the screen opens on: tomorrow, whose forecast a shake will freshen. */
internal const val OpeningDay = 1
