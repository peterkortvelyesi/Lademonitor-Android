package com.dominiqueherbrigpersonalteam.lademonitor.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.dominiqueherbrigpersonalteam.lademonitor.LademonitorApp
import com.dominiqueherbrigpersonalteam.lademonitor.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * One-shot current-location lookup via Android's [LocationManager] (no Google Play Services needed),
 * the analogue of the iOS `CurrentLocationProvider`. The Compose layer requests the runtime
 * permission first; this throws [LocationException.Denied] if it is still missing.
 */
object CurrentLocationProvider {

    sealed class LocationException(message: String) : Exception(message) {
        object Denied : LocationException(
            LademonitorApp.appContext.getString(R.string.location_error_permission_denied)
        )
        object Unavailable : LocationException(
            LademonitorApp.appContext.getString(R.string.location_error_unavailable)
        )
    }

    data class Coordinate(val latitude: Double, val longitude: Double)

    private fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun requestCurrentLocation(context: Context): Coordinate {
        if (!hasPermission(context)) throw LocationException.Denied
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: throw LocationException.Unavailable

        // A very recent last-known fix is good enough and instant.
        recentLastKnown(lm)?.let { return it.toCoordinate() }

        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> throw LocationException.Unavailable
        }

        val location = withTimeoutOrNull(15_000L) {
            suspendCancellableCoroutine<Location?> { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        lm.removeUpdates(this)
                        if (cont.isActive) cont.resume(location)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(p: String?, s: Int, e: android.os.Bundle?) {}
                    override fun onProviderDisabled(p: String) {}
                    override fun onProviderEnabled(p: String) {}
                }
                cont.invokeOnCancellation { lm.removeUpdates(listener) }
                try {
                    @Suppress("MissingPermission")
                    lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                } catch (_: SecurityException) {
                    lm.removeUpdates(listener)
                    if (cont.isActive) cont.resume(null)
                }
            }
        }

        return (location ?: lastKnown(lm))?.toCoordinate() ?: throw LocationException.Unavailable
    }

    private fun recentLastKnown(lm: LocationManager): Location? {
        val loc = lastKnown(lm) ?: return null
        val ageMs = System.currentTimeMillis() - loc.time
        return if (ageMs in 0..120_000) loc else null
    }

    private fun lastKnown(lm: LocationManager): Location? {
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        return providers.mapNotNull { p ->
            runCatching {
                @Suppress("MissingPermission")
                lm.getLastKnownLocation(p)
            }.getOrNull()
        }.maxByOrNull { it.time }
    }

    private fun Location.toCoordinate() = Coordinate(latitude, longitude)
}
