package com.dominiqueherbrigpersonalteam.lademonitor.ui.filter

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Prebuilt date ranges for the global filter (port of the iOS `FilterPreset`). Each computes a
 * concrete epoch-millis range on selection; the end is always end-of-today so sessions logged today
 * are never cut off by a mid-day time boundary.
 */
enum class FilterPreset(val title: String) {
    LAST_7_DAYS("Letzte 7 Tage"),
    LAST_30_DAYS("Letzte 30 Tage"),
    LAST_90_DAYS("Letzte 90 Tage"),
    LAST_MONTH("Letzter Monat"),
    MONTH_TO_DATE("Monat bis jetzt"),
    YEAR_TO_DATE("Jahr bis jetzt"),
    LAST_YEAR("Letztes Jahr");

    fun range(zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): LongRange {
        fun startMillis(date: LocalDate): Long =
            date.atStartOfDay(zone).toInstant().toEpochMilli()

        fun endOfDayMillis(date: LocalDate): Long =
            date.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()

        val endOfToday = endOfDayMillis(today)
        return when (this) {
            LAST_7_DAYS -> startMillis(today.minusDays(6))..endOfToday
            LAST_30_DAYS -> startMillis(today.minusDays(29))..endOfToday
            LAST_90_DAYS -> startMillis(today.minusDays(89))..endOfToday
            LAST_MONTH -> {
                val firstOfThisMonth = today.withDayOfMonth(1)
                val firstOfLastMonth = firstOfThisMonth.minusMonths(1)
                startMillis(firstOfLastMonth)..(startMillis(firstOfThisMonth) - 1000)
            }
            MONTH_TO_DATE -> startMillis(today.withDayOfMonth(1))..endOfToday
            YEAR_TO_DATE -> startMillis(today.withDayOfYear(1))..endOfToday
            LAST_YEAR -> {
                val firstOfThisYear = today.withDayOfYear(1)
                val firstOfLastYear = firstOfThisYear.minusYears(1)
                startMillis(firstOfLastYear)..(startMillis(firstOfThisYear) - 1000)
            }
        }
    }
}
