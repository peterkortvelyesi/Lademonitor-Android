package com.dominiqueherbrigpersonalteam.lademonitor.ui.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** German-locale number/date formatting helpers, matching the iOS formatters. */
object Fmt {
    private val de = Locale.GERMANY
    private val zone: ZoneId get() = ZoneId.systemDefault()

    private val dateTimeMedium: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(de)
    private val dateTimeFull: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT).withLocale(de)
    private val dateTimeShort: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT).withLocale(de)

    fun dateTimeMedium(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(dateTimeMedium)

    fun dateTimeFull(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(dateTimeFull)

    fun dateTimeShort(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(dateTimeShort)

    fun km(value: Int): String = "%,d km".format(de, value)

    fun n(format: String, value: Double): String = String.format(de, format, value)

    fun n(format: String, value: Int): String = String.format(de, format, value)

    /** "vor 5 Minuten" style relative time for the last-sync line. */
    fun relative(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = (now - epochMillis).coerceAtLeast(0)
        val minutes = diff / 60_000
        val hours = diff / 3_600_000
        val days = diff / 86_400_000
        return when {
            minutes < 1 -> "gerade eben"
            minutes < 60 -> "vor $minutes Min."
            hours < 24 -> "vor $hours Std."
            else -> "vor $days Tg."
        }
    }
}
