package com.dominiqueherbrigpersonalteam.lademonitor.ui.filter

import androidx.annotation.StringRes
import com.dominiqueherbrigpersonalteam.lademonitor.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Prebuilt date ranges for the global filter (port of the iOS `FilterPreset`). Each computes a
 * concrete epoch-millis range on selection; the end is always end-of-today so sessions logged today
 * are never cut off by a mid-day time boundary.
 */
enum class FilterPreset(@param:StringRes val titleRes: Int) {
    LAST_7_DAYS(R.string.filter_preset_last_7_days),
    LAST_30_DAYS(R.string.filter_preset_last_30_days),
    LAST_90_DAYS(R.string.filter_preset_last_90_days),
    LAST_MONTH(R.string.filter_preset_last_month),
    MONTH_TO_DATE(R.string.filter_preset_month_to_date),
    YEAR_TO_DATE(R.string.filter_preset_year_to_date),
    LAST_YEAR(R.string.filter_preset_last_year);

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
