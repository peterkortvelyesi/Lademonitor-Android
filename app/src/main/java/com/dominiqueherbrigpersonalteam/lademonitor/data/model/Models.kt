package com.dominiqueherbrigpersonalteam.lademonitor.data.model

import androidx.annotation.StringRes
import com.dominiqueherbrigpersonalteam.lademonitor.R
import com.dominiqueherbrigpersonalteam.lademonitor.data.remote.ServerDate
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Kotlin/Moshi port of the iOS `Models.swift` Codable structs. These types are shared by
 * the REST layer (server mode) and the UI, exactly like the iOS Codable structs.
 *
 * Timestamps are carried as epoch-millis [Long]; the [com.dominiqueherbrigpersonalteam.lademonitor.data.remote.ServerDate]
 * qualifier tells Moshi to (de)serialise them against the server's naive/ISO datetime
 * strings the same way the iOS APIClient does.
 */

enum class ChargingType(val raw: String) {
    AC("AC"),
    DC("DC");

    companion object {
        fun from(raw: String?): ChargingType? = entries.firstOrNull { it.raw == raw }
    }
}

enum class SessionSource(val raw: String, @param:StringRes val labelRes: Int) {
    MANUAL("manual", R.string.session_source_manual),
    AUTOMATIC("automatic", R.string.session_source_automatic),
    IMPORT("import", R.string.session_source_import);

    companion object {
        fun from(raw: String?): SessionSource = entries.firstOrNull { it.raw == raw } ?: MANUAL
    }
}

/**
 * How the server (or [com.dominiqueherbrigpersonalteam.lademonitor.data.repo.LocalConsumptionCalculator])
 * computed the kWh/100km for a session. Drives the marker + explanation text in the list,
 * mirroring `ConsumptionMethod` in the iOS app.
 */
enum class ConsumptionMethod(
    val raw: String,
    val marker: String,
    @param:StringRes val shortLabelRes: Int,
    @param:StringRes val explanationRes: Int
) {
    FULL_CHARGE_INTERVAL(
        "full_charge_interval", "🎯", R.string.consumption_method_full_charge_interval_label,
        R.string.consumption_method_full_charge_interval_explanation
    ),
    SOC_CORRECTED(
        "soc_corrected", "✓", R.string.consumption_method_soc_corrected_label,
        R.string.consumption_method_soc_corrected_explanation
    ),
    NAIVE(
        "naive", "~", R.string.consumption_method_naive_label,
        R.string.consumption_method_naive_explanation
    ),
    ESTIMATED_ENERGY(
        "estimated_energy", "~", R.string.consumption_method_estimated_energy_label,
        R.string.consumption_method_estimated_energy_explanation
    ),
    UNAVAILABLE(
        "unavailable", "", R.string.consumption_method_unavailable_label,
        R.string.consumption_method_unavailable_explanation
    );

    companion object {
        fun from(raw: String?): ConsumptionMethod? = entries.firstOrNull { it.raw == raw }
    }
}

@JsonClass(generateAdapter = true)
data class Vehicle(
    val id: String,
    @Json(name = "external_id") val externalId: String,
    val name: String,
    val brand: String? = null,
    val model: String? = null,
    @Json(name = "battery_capacity_kwh") val batteryCapacityKwh: Double? = null,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class Provider(
    val id: String,
    val name: String,
    @Json(name = "last_price_ac_per_kwh") val lastPriceAcPerKwh: Double? = null,
    @Json(name = "last_price_dc_per_kwh") val lastPriceDcPerKwh: Double? = null,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class ChargingLocation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    @Json(name = "radius_m") val radiusM: Int,
    @Json(name = "default_provider_id") val defaultProviderId: String? = null
)

@JsonClass(generateAdapter = true)
data class ChargingSession(
    val id: String,
    @Json(name = "vehicle_id") val vehicleId: String,
    @Json(name = "provider_id") val providerId: String? = null,
    @Json(name = "location_id") val locationId: String? = null,
    @ServerDate @Json(name = "start_time") val startTime: Long,
    @ServerDate @Json(name = "end_time") val endTime: Long? = null,
    @Json(name = "charging_type") val chargingType: String? = null,
    @Json(name = "soc_start") val socStart: Int? = null,
    @Json(name = "soc_end") val socEnd: Int? = null,
    @Json(name = "energy_kwh") val energyKwh: Double? = null,
    @Json(name = "energy_is_estimated") val energyIsEstimated: Boolean = false,
    @Json(name = "odometer_km") val odometerKm: Int? = null,
    @Json(name = "price_total") val priceTotal: Double? = null,
    @Json(name = "price_per_kwh") val pricePerKwh: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "geocoded_place") val geocodedPlace: String? = null,
    @Json(name = "consumption_kwh_per_100km") val consumptionKwhPer100km: Double? = null,
    @Json(name = "consumption_method") val consumptionMethod: String? = null,
    val notes: String? = null,
    val source: String = "manual",
    @Json(name = "needs_review") val needsReview: Boolean = false,
    @Json(name = "external_session_id") val externalSessionId: String? = null
) {
    val chargingTypeValue: ChargingType? get() = ChargingType.from(chargingType)
    val sourceValue: SessionSource get() = SessionSource.from(source)
    val consumptionMethodValue: ConsumptionMethod? get() = ConsumptionMethod.from(consumptionMethod)
}

// MARK: - Payloads (create/update). Null fields are omitted by Moshi, matching the iOS
// JSONEncoder behaviour where nil optionals are dropped.

@JsonClass(generateAdapter = true)
data class VehiclePayload(
    @Json(name = "external_id") val externalId: String? = null,
    val name: String? = null,
    val brand: String? = null,
    val model: String? = null,
    @Json(name = "battery_capacity_kwh") val batteryCapacityKwh: Double? = null,
    @Json(name = "is_active") val isActive: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class ProviderPayload(
    val name: String? = null,
    @Json(name = "last_price_ac_per_kwh") val lastPriceAcPerKwh: Double? = null,
    @Json(name = "last_price_dc_per_kwh") val lastPriceDcPerKwh: Double? = null,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class LocationPayload(
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "radius_m") val radiusM: Int? = null,
    @Json(name = "default_provider_id") val defaultProviderId: String? = null
)

@JsonClass(generateAdapter = true)
data class ChargingSessionPayload(
    @Json(name = "vehicle_id") val vehicleId: String? = null,
    @Json(name = "provider_id") val providerId: String? = null,
    @ServerDate @Json(name = "start_time") val startTime: Long? = null,
    @Json(name = "charging_type") val chargingType: String? = null,
    @Json(name = "soc_start") val socStart: Int? = null,
    @Json(name = "soc_end") val socEnd: Int? = null,
    @Json(name = "energy_kwh") val energyKwh: Double? = null,
    @Json(name = "price_per_kwh") val pricePerKwh: Double? = null,
    @Json(name = "price_total") val priceTotal: Double? = null,
    @Json(name = "odometer_km") val odometerKm: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "geocoded_place") val geocodedPlace: String? = null,
    val notes: String? = null,
    @Json(name = "needs_review") val needsReview: Boolean? = null
)

// MARK: - Auth

@JsonClass(generateAdapter = true)
data class AuthUser(
    val id: String,
    val username: String,
    @Json(name = "is_admin") val isAdmin: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val token: String,
    val user: AuthUser
)

@JsonClass(generateAdapter = true)
data class AuthCredentials(
    val username: String,
    val password: String
)

// MARK: - Geocoding

@JsonClass(generateAdapter = true)
data class GeocodeResult(
    @Json(name = "display_name") val displayName: String,
    val latitude: Double,
    val longitude: Double
)

// MARK: - Stats

@JsonClass(generateAdapter = true)
data class ProviderStat(
    @Json(name = "provider_name") val providerName: String,
    @Json(name = "total_kwh") val totalKwh: Double,
    @Json(name = "total_cost") val totalCost: Double
)

@JsonClass(generateAdapter = true)
data class MonthlyStat(
    val month: String,
    @Json(name = "total_cost") val totalCost: Double,
    @Json(name = "total_kwh") val totalKwh: Double,
    @Json(name = "session_count") val sessionCount: Int,
    @Json(name = "avg_consumption_kwh_per_100km") val avgConsumptionKwhPer100km: Double? = null
) {
    /**
     * "YYYY-MM" -> "August 2026" (or "August 2026" -> "August 2026" in English), matching the
     * iOS displayMonth. Uses the device's current locale so it follows the system language
     * automatically, the same way the rest of the app's localized strings do.
     */
    val displayMonth: String
        get() {
            val parsed = runCatching { java.time.YearMonth.parse(month) }.getOrNull() ?: return month
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.getDefault())
            return parsed.format(formatter)
        }

    /** "YYYY-MM" -> "Aug '26" for chart axes, localized to the device's current locale. */
    val shortMonth: String
        get() {
            val parsed = runCatching { java.time.YearMonth.parse(month) }.getOrNull() ?: return month
            val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM", java.util.Locale.getDefault())
            val yearSuffix = (parsed.year % 100).toString().padStart(2, '0')
            return "${parsed.format(formatter)} '$yearSuffix"
        }
}

@JsonClass(generateAdapter = true)
data class StatsSummary(
    @Json(name = "total_sessions") val totalSessions: Int,
    @Json(name = "total_kwh") val totalKwh: Double,
    @Json(name = "total_cost") val totalCost: Double,
    @Json(name = "avg_price_per_kwh") val avgPricePerKwh: Double? = null,
    @Json(name = "avg_consumption_kwh_per_100km") val avgConsumptionKwhPer100km: Double? = null,
    @Json(name = "price_per_100km") val pricePer100km: Double? = null,
    @Json(name = "ac_share_pct") val acSharePct: Double? = null,
    @Json(name = "dc_share_pct") val dcSharePct: Double? = null,
    @Json(name = "ac_kwh") val acKwh: Double? = null,
    @Json(name = "dc_kwh") val dcKwh: Double? = null,
    @Json(name = "total_km_driven") val totalKmDriven: Int? = null,
    @Json(name = "by_provider") val byProvider: List<ProviderStat> = emptyList(),
    val monthly: List<MonthlyStat> = emptyList()
)
