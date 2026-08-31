package com.dominiqueherbrigpersonalteam.lademonitor.data.remote

import com.dominiqueherbrigpersonalteam.lademonitor.LademonitorApp
import com.dominiqueherbrigpersonalteam.lademonitor.R
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.AuthCredentials
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.AuthResponse
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.AuthUser
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.ChargingLocation
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.ChargingSession
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.ChargingSessionPayload
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.GeocodeResult
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.LocationPayload
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.Provider
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.ProviderPayload
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.StatsSummary
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.Vehicle
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.VehiclePayload
import com.dominiqueherbrigpersonalteam.lademonitor.data.session.SessionManager
import com.dominiqueherbrigpersonalteam.lademonitor.data.settings.AppSettings
import com.dominiqueherbrigpersonalteam.lademonitor.data.settings.TokenStore
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/**
 * Hand-rolled REST client, a close port of the iOS `APIClient`: it attaches the bearer token to
 * authenticated calls, extracts `{"detail": ...}` error messages, and on a 401 for an
 * authenticated request invalidates the session (kicking the user back to login).
 *
 * All work happens on [Dispatchers.IO]; callers just `await` the suspend functions.
 */
object ApiClient {

    private val jsonMedia = "application/json".toMediaType()

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun listType(elem: Type): Type = Types.newParameterizedType(List::class.java, elem)

    // MARK: - Request plumbing

    private fun buildRequest(
        path: String,
        method: String,
        body: String?,
        authenticated: Boolean
    ): Request {
        val base = AppSettings.serverUrl() ?: throw ApiException.NotConfigured
        val url = (base + path).toHttpUrlOrNull() ?: throw ApiException.InvalidResponse
        val builder = Request.Builder().url(url).header("Content-Type", "application/json")
        if (authenticated) {
            TokenStore.readToken()?.let { builder.header("Authorization", "Bearer $it") }
        }
        val requestBody = when (method) {
            "GET", "DELETE" -> if (body != null) body.toRequestBody(jsonMedia) else null
            else -> (body ?: "").toRequestBody(jsonMedia)
        }
        builder.method(method, requestBody)
        return builder.build()
    }

    private suspend fun <T> send(
        path: String,
        method: String = "GET",
        body: String? = null,
        authenticated: Boolean = true,
        type: Type
    ): T = withContext(Dispatchers.IO) {
        val request = buildRequest(path, method, body, authenticated)
        val response = try {
            http.newCall(request).execute()
        } catch (e: Exception) {
            throw ApiException.Network(e)
        }
        response.use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                if (resp.code == 401 && request.header("Authorization") != null) {
                    SessionManager.invalidateSession()
                }
                throw ApiException.Server(resp.code, messageFrom(raw))
            }
            try {
                @Suppress("UNCHECKED_CAST")
                Json.moshi.adapter<Any>(type).fromJson(raw) as T
            } catch (e: Exception) {
                throw ApiException.Decoding(e)
            }
        }
    }

    private suspend fun sendNoContent(
        path: String,
        method: String,
        body: String? = null,
        authenticated: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val request = buildRequest(path, method, body, authenticated)
        val response = try {
            http.newCall(request).execute()
        } catch (e: Exception) {
            throw ApiException.Network(e)
        }
        response.use { resp ->
            if (!resp.isSuccessful) {
                val raw = resp.body?.string().orEmpty()
                if (resp.code == 401 && request.header("Authorization") != null) {
                    SessionManager.invalidateSession()
                }
                throw ApiException.Server(resp.code, messageFrom(raw))
            }
        }
    }

    private fun messageFrom(raw: String): String {
        if (raw.isBlank()) return LademonitorApp.appContext.getString(R.string.api_error_unknown)
        return try {
            val map = Json.moshi.adapter<Map<String, Any>>(
                Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            ).fromJson(raw)
            (map?.get("detail") as? String)?.takeIf { it.isNotEmpty() } ?: raw
        } catch (_: Exception) {
            raw
        }
    }

    private inline fun <reified T> encode(value: T): String =
        Json.moshi.adapter(T::class.java).toJson(value)

    // MARK: - Auth

    suspend fun register(username: String, password: String): AuthResponse =
        send(
            "/api/auth/register", "POST",
            encode(AuthCredentials(username, password)),
            authenticated = false, type = AuthResponse::class.java
        )

    suspend fun login(username: String, password: String): AuthResponse =
        send(
            "/api/auth/login", "POST",
            encode(AuthCredentials(username, password)),
            authenticated = false, type = AuthResponse::class.java
        )

    suspend fun logout() = sendNoContent("/api/auth/logout", "POST")

    suspend fun fetchMe(): AuthUser = send("/api/auth/me", type = AuthUser::class.java)

    suspend fun checkHealth(): Boolean {
        val map: Map<String, Any> = send(
            "/health", authenticated = false,
            type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        )
        return map["status"] == "ok"
    }

    // MARK: - Vehicles

    suspend fun fetchVehicles(): List<Vehicle> =
        send("/api/vehicles", type = listType(Vehicle::class.java))

    suspend fun createVehicle(payload: VehiclePayload): Vehicle =
        send("/api/vehicles", "POST", encode(payload), type = Vehicle::class.java)

    suspend fun updateVehicle(id: String, payload: VehiclePayload): Vehicle =
        send("/api/vehicles/$id", "PATCH", encode(payload), type = Vehicle::class.java)

    suspend fun deleteVehicle(id: String) = sendNoContent("/api/vehicles/$id", "DELETE")

    // MARK: - Providers

    suspend fun fetchProviders(): List<Provider> =
        send("/api/providers", type = listType(Provider::class.java))

    suspend fun createProvider(payload: ProviderPayload): Provider =
        send("/api/providers", "POST", encode(payload), type = Provider::class.java)

    suspend fun updateProvider(id: String, payload: ProviderPayload): Provider =
        send("/api/providers/$id", "PATCH", encode(payload), type = Provider::class.java)

    suspend fun deleteProvider(id: String) = sendNoContent("/api/providers/$id", "DELETE")

    // MARK: - Locations

    suspend fun fetchLocations(): List<ChargingLocation> =
        send("/api/locations", type = listType(ChargingLocation::class.java))

    suspend fun createLocation(payload: LocationPayload): ChargingLocation =
        send("/api/locations", "POST", encode(payload), type = ChargingLocation::class.java)

    suspend fun updateLocation(id: String, payload: LocationPayload): ChargingLocation =
        send("/api/locations/$id", "PATCH", encode(payload), type = ChargingLocation::class.java)

    suspend fun deleteLocation(id: String) = sendNoContent("/api/locations/$id", "DELETE")

    // MARK: - Geocoding (server proxy to Nominatim)

    suspend fun forwardGeocode(query: String): List<GeocodeResult> {
        val base = AppSettings.serverUrl() ?: throw ApiException.NotConfigured
        val url = "$base/api/geocode/forward".toHttpUrlOrNull()
            ?.newBuilder()?.addQueryParameter("query", query)?.build()
            ?: throw ApiException.InvalidResponse
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).header("Content-Type", "application/json").get().build()
            val response = try {
                http.newCall(request).execute()
            } catch (e: Exception) {
                throw ApiException.Network(e)
            }
            response.use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw ApiException.Server(resp.code, messageFrom(raw))
                try {
                    Json.moshi.adapter<List<GeocodeResult>>(listType(GeocodeResult::class.java))
                        .fromJson(raw) ?: emptyList()
                } catch (e: Exception) {
                    throw ApiException.Decoding(e)
                }
            }
        }
    }

    // MARK: - Sessions

    suspend fun fetchSessions(
        vehicleId: String? = null,
        needsReview: Boolean? = null
    ): List<ChargingSession> {
        val query = buildList {
            vehicleId?.let { add("vehicle_id=$it") }
            needsReview?.let { add("needs_review=$it") }
        }
        val path = "/api/sessions" + if (query.isNotEmpty()) "?" + query.joinToString("&") else ""
        return send(path, type = listType(ChargingSession::class.java))
    }

    suspend fun createSession(payload: ChargingSessionPayload): ChargingSession =
        send("/api/sessions", "POST", encode(payload), type = ChargingSession::class.java)

    suspend fun updateSession(id: String, payload: ChargingSessionPayload): ChargingSession =
        send("/api/sessions/$id", "PATCH", encode(payload), type = ChargingSession::class.java)

    suspend fun deleteSession(id: String) = sendNoContent("/api/sessions/$id", "DELETE")

    // MARK: - Stats (server-side; the app normally computes stats locally)

    suspend fun fetchStatsSummary(vehicleId: String? = null): StatsSummary {
        val path = "/api/stats/summary" + if (vehicleId != null) "?vehicle_id=$vehicleId" else ""
        return send(path, type = StatsSummary::class.java)
    }
}
