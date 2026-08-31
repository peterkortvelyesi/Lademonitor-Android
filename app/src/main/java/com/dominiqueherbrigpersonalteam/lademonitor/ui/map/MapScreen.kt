package com.dominiqueherbrigpersonalteam.lademonitor.ui.map

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dominiqueherbrigpersonalteam.lademonitor.R
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.ChargingLocation
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.ChargingSession
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.Provider
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.Vehicle
import com.dominiqueherbrigpersonalteam.lademonitor.data.repo.AppRepository
import com.dominiqueherbrigpersonalteam.lademonitor.data.settings.AppSettings
import com.dominiqueherbrigpersonalteam.lademonitor.ui.common.EmptyState
import com.dominiqueherbrigpersonalteam.lademonitor.ui.common.ErrorState
import com.dominiqueherbrigpersonalteam.lademonitor.ui.common.FullScreenModal
import com.dominiqueherbrigpersonalteam.lademonitor.ui.filter.FilterIconButton
import com.dominiqueherbrigpersonalteam.lademonitor.ui.filter.FilterSheet
import com.dominiqueherbrigpersonalteam.lademonitor.ui.filter.SessionFilter
import com.dominiqueherbrigpersonalteam.lademonitor.ui.sessions.SessionDetailScreen
import com.dominiqueherbrigpersonalteam.lademonitor.ui.settings.AddEditLocationModal
import com.dominiqueherbrigpersonalteam.lademonitor.ui.theme.Blue
import com.dominiqueherbrigpersonalteam.lademonitor.ui.theme.Orange
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val scope = rememberCoroutineScope()
    val dateRange by SessionFilter.dateRange.collectAsStateWithLifecycle()

    var locations by remember { mutableStateOf<List<ChargingLocation>>(emptyList()) }
    var sessions by remember { mutableStateOf<List<ChargingSession>>(emptyList()) }
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var providers by remember { mutableStateOf<List<Provider>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showLocations by remember { mutableStateOf(true) }
    var showSessions by remember { mutableStateOf(true) }
    var showFilter by remember { mutableStateOf(false) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var didFit by remember { mutableStateOf(false) }

    var locationToEdit by remember { mutableStateOf<ChargingLocation?>(null) }
    var sessionToPreview by remember { mutableStateOf<ChargingSession?>(null) }

    val sessionPins = sessions.filter { it.latitude != null && it.longitude != null }
    val serverUrlRequiredMessage = stringResource(R.string.error_server_url_required)

    suspend fun load() {
        if (!AppSettings.isReadyForDataAccess) {
            errorMessage = serverUrlRequiredMessage
            return
        }
        isLoading = true; errorMessage = null
        try {
            coroutineScope {
                val l = async { AppRepository.fetchLocations() }
                val s = async { AppRepository.fetchSessions(dateRange = dateRange) }
                val v = async { AppRepository.fetchVehicles() }
                val p = async { AppRepository.fetchProviders() }
                locations = l.await(); sessions = s.await(); vehicles = v.await(); providers = p.await()
            }
            didFit = false
        } catch (e: Exception) {
            errorMessage = e.localizedMessage
        }
        isLoading = false
    }

    LaunchedEffect(dateRange) { load() }

    // Rebuild overlays whenever the data or toggles change.
    LaunchedEffect(mapView, locations, sessions, showLocations, showSessions) {
        val map = mapView ?: return@LaunchedEffect
        map.overlays.clear()
        if (showLocations) {
            locations.forEach { loc ->
                val center = GeoPoint(loc.latitude, loc.longitude)
                val circle = Polygon(map).apply {
                    points = Polygon.pointsAsCircle(center, loc.radiusM.toDouble())
                    fillPaint.color = AndroidColor.argb(30, 10, 132, 255)
                    outlinePaint.color = AndroidColor.argb(128, 10, 132, 255)
                    outlinePaint.strokeWidth = 2f
                }
                map.overlays.add(circle)
                map.overlays.add(Marker(map).apply {
                    position = center
                    title = loc.name
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ -> locationToEdit = loc; true }
                })
            }
        }
        if (showSessions) {
            sessionPins.forEach { session ->
                val point = GeoPoint(session.latitude!!, session.longitude!!)
                map.overlays.add(Marker(map).apply {
                    position = point
                    title = (if (session.needsReview) "⚠ " else "") +
                        com.dominiqueherbrigpersonalteam.lademonitor.ui.common.Fmt.dateTimeShort(session.startTime)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ -> sessionToPreview = session; true }
                })
            }
        }
        map.invalidate()

        if (!didFit) {
            val points = locations.map { GeoPoint(it.latitude, it.longitude) } +
                sessionPins.map { GeoPoint(it.latitude!!, it.longitude!!) }
            if (points.isNotEmpty()) {
                val lats = points.map { it.latitude }
                val lons = points.map { it.longitude }
                val box = BoundingBox(lats.max(), lons.max(), lats.min(), lons.min())
                map.post { runCatching { map.zoomToBoundingBox(box.increaseByScale(1.4f), false, 48) } }
                didFit = true
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.tab_map)) }, actions = { FilterIconButton(onClick = { showFilter = true }) })
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                errorMessage != null -> ErrorState(errorMessage!!, onRetry = { scope.launch { load() } })
                locations.isEmpty() && sessionPins.isEmpty() && !isLoading ->
                    EmptyState(stringResource(R.string.map_empty_state))
                else -> {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            MapView(context).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(6.0)
                                mapView = this
                            }
                        },
                        onRelease = { it.onDetach() }
                    )
                    // Legend / toggles
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LegendToggle(stringResource(R.string.map_legend_locations), Icons.Filled.EvStation, Blue, showLocations) { showLocations = !showLocations }
                        LegendToggle(stringResource(R.string.dashboard_stat_sessions), Icons.Filled.Bolt, Color.Gray, showSessions) { showSessions = !showSessions }
                    }
                }
            }
        }
    }

    locationToEdit?.let { loc ->
        AddEditLocationModal(location = loc, providers = providers, onDismiss = { locationToEdit = null }) {
            locationToEdit = null; scope.launch { load() }
        }
    }

    sessionToPreview?.let { s ->
        FullScreenModal(onDismiss = { sessionToPreview = null }) {
            SessionDetailScreen(
                session = s, vehicles = vehicles, providers = providers, locations = locations,
                onDismiss = { sessionToPreview = null },
                onChanged = { sessionToPreview = null; scope.launch { load() } }
            )
        }
    }

    if (showFilter) FilterSheet(onDismiss = { showFilter = false })
}

@Composable
private fun LegendToggle(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isOn: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (isOn) color else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Box(Modifier.size(0.dp).clip(CircleShape))
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = if (isOn) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textDecoration = if (isOn) TextDecoration.None else TextDecoration.LineThrough
        )
    }
}
