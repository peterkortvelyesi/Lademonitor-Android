package com.dominiqueherbrigpersonalteam.lademonitor.data.repo

import com.dominiqueherbrigpersonalteam.lademonitor.data.model.GeocodeResult
import com.dominiqueherbrigpersonalteam.lademonitor.data.remote.Json
import com.squareup.moshi.Json as MoshiJson
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Replacement for the server's forward-geocode proxy in local-only mode, mirroring the iOS
 * `LocalGeocoder`. iOS uses Apple's on-device MKLocalSearch; the keyless Android equivalent queries
 * OpenStreetMap Nominatim directly — the same OSM source the server proxies to. Nominatim's usage
 * policy requires an identifying User-Agent, set below.
 */
object LocalGeocoder {

    @JsonClass(generateAdapter = true)
    data class NominatimResult(
        @MoshiJson(name = "display_name") val displayName: String?,
        val lat: String?,
        val lon: String?
    )

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun search(query: String): List<GeocodeResult> = withContext(Dispatchers.IO) {
        val url = "https://nominatim.openstreetmap.org/search".toHttpUrlOrNull()
            ?.newBuilder()
            ?.addQueryParameter("q", query)
            ?.addQueryParameter("format", "json")
            ?.addQueryParameter("limit", "10")
            ?.build() ?: return@withContext emptyList()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Lademonitor-Android")
            .header("Accept-Language", "de")
            .get()
            .build()

        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext emptyList()
            val raw = resp.body?.string().orEmpty()
            val type = Types.newParameterizedType(List::class.java, NominatimResult::class.java)
            val results = Json.moshi.adapter<List<NominatimResult>>(type).fromJson(raw) ?: emptyList()
            results.mapNotNull { r ->
                val lat = r.lat?.toDoubleOrNull()
                val lon = r.lon?.toDoubleOrNull()
                if (lat == null || lon == null) return@mapNotNull null
                GeocodeResult(
                    displayName = r.displayName ?: "Unbekannter Ort",
                    latitude = lat,
                    longitude = lon
                )
            }
        }
    }
}
