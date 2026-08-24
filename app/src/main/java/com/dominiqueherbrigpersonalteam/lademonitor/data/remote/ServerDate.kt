package com.dominiqueherbrigpersonalteam.lademonitor.data.remote

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.ToJson
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Marks a Long field as an epoch-millis timestamp encoded on the wire as a server datetime string. */
@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class ServerDate

/**
 * Kotlin port of the custom date (de)coding in the iOS `APIClient`.
 *
 * The backend emits either ISO datetimes with an offset/`Z`, or naive
 * "YYYY-MM-DDTHH:MM:SS[.ffffff]" strings without a zone. A naive value is a wall-clock time and
 * is interpreted in the *device's current time zone* (so 00:00 stays 00:00, not shifted to UTC).
 * On the way out, timestamps are always written as naive local strings — matching the decoder and
 * the way the app displays them.
 */
object ServerDateAdapter {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    private val naiveFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    @FromJson
    @ServerDate
    fun fromJson(raw: String): Long {
        // 1) Explicit offset / Z (optionally with fractional seconds).
        runCatching { return OffsetDateTime.parse(raw).toInstant().toEpochMilli() }
        runCatching { return Instant.parse(raw).toEpochMilli() }
        // 2) Naive local wall-clock time (ISO_LOCAL_DATE_TIME accepts optional fractional seconds).
        runCatching {
            return LocalDateTime.parse(raw).atZone(zone).toInstant().toEpochMilli()
        }
        // 3) Last resort: date only.
        runCatching {
            return LocalDateTime.parse(raw + "T00:00:00").atZone(zone).toInstant().toEpochMilli()
        }
        throw IllegalArgumentException("Unbekanntes Datumsformat: $raw")
    }

    @ToJson
    fun toJson(@ServerDate value: Long): String =
        Instant.ofEpochMilli(value).atZone(zone).toLocalDateTime().format(naiveFormatter)
}
