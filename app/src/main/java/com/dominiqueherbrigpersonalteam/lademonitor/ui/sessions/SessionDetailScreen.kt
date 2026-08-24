package com.dominiqueherbrigpersonalteam.lademonitor.ui.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.ChargingLocation
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.ChargingSession
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.ChargingSessionPayload
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.Provider
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.Vehicle
import com.dominiqueherbrigpersonalteam.lademonitor.data.repo.AppRepository
import com.dominiqueherbrigpersonalteam.lademonitor.ui.common.Fmt
import com.dominiqueherbrigpersonalteam.lademonitor.ui.common.FullScreenModal
import com.dominiqueherbrigpersonalteam.lademonitor.ui.common.SectionCard
import com.dominiqueherbrigpersonalteam.lademonitor.ui.map.StaticMap
import com.dominiqueherbrigpersonalteam.lademonitor.ui.theme.Green
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    session: ChargingSession,
    vehicles: List<Vehicle>,
    providers: List<Provider>,
    locations: List<ChargingLocation>,
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showEdit by remember { mutableStateOf(false) }
    var isConfirming by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val vehicleName = vehicles.firstOrNull { it.id == session.vehicleId }?.name
    val providerName = providers.firstOrNull { it.id == session.providerId }?.name
    val locationName = locations.firstOrNull { it.id == session.locationId }?.name

    val socText = when {
        session.socStart != null && session.socEnd != null -> "${session.socStart} → ${session.socEnd} %"
        session.socStart != null -> "ab ${session.socStart} %"
        session.socEnd != null -> "bis ${session.socEnd} %"
        else -> "–"
    }

    fun confirm() {
        scope.launch {
            isConfirming = true; errorMessage = null
            try {
                AppRepository.updateSession(session.id, ChargingSessionPayload(needsReview = false))
                onChanged()
            } catch (e: Exception) { errorMessage = e.localizedMessage }
            isConfirming = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ladevorgang") },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Schließen") } },
                actions = {
                    if (session.needsReview) {
                        IconButton(onClick = { confirm() }, enabled = !isConfirming) {
                            if (isConfirming) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Filled.Check, contentDescription = "Bestätigen", tint = Green)
                        }
                    }
                    IconButton(onClick = { showEdit = true }) { Icon(Icons.Filled.Edit, contentDescription = "Bearbeiten") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val lat = session.latitude
            val lon = session.longitude
            if (lat != null && lon != null) {
                StaticMap(
                    lat = lat, lon = lon,
                    label = locationName ?: session.geocodedPlace ?: "Ladevorgang",
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp))
                )
            }

            SectionCard {
                CardTitle("Fahrzeug & Zeit")
                LabeledRow("Fahrzeug", vehicleName ?: "–")
                LabeledRow("Start", Fmt.dateTimeFull(session.startTime))
                session.chargingTypeValue?.let { LabeledRow("Lade-Art", it.raw) }
            }

            SectionCard {
                CardTitle("Ort & Anbieter")
                LabeledRow("Anbieter", providerName ?: "–")
                LabeledRow("Ort", session.geocodedPlace ?: locationName ?: "–")
            }

            SectionCard {
                CardTitle("Akkustand & Energie")
                if (session.socStart != null || session.socEnd != null) LabeledRow("SoC", socText)
                session.energyKwh?.let {
                    LabeledRow("kWh", Fmt.n("%.2f kWh", it) + if (session.energyIsEstimated) " (geschätzt)" else "")
                }
                session.odometerKm?.let { LabeledRow("Kilometerstand", Fmt.km(it)) }
                session.consumptionKwhPer100km?.let { LabeledRow("Verbrauch", Fmt.n("%.1f kWh/100km", it)) }
            }

            if (session.priceTotal != null || session.pricePerKwh != null) {
                SectionCard {
                    CardTitle("Preis")
                    session.priceTotal?.let { LabeledRow("Gesamt", Fmt.n("%.2f €", it)) }
                    session.pricePerKwh?.let { LabeledRow("Pro kWh", Fmt.n("%.4f €", it)) }
                }
            }

            SectionCard {
                CardTitle("Quelle")
                LabeledRow("Erfasst als", session.sourceValue.displayName)
                LabeledRow("Status", if (session.needsReview) "Zu prüfen" else "Geprüft")
            }

            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }

    if (showEdit) {
        FullScreenModal(onDismiss = { showEdit = false }) {
            AddEditSessionScreen(
                vehicles = vehicles, providers = providers, locations = locations, session = session,
                onDismiss = { showEdit = false },
                onSaved = { showEdit = false; onChanged() }
            )
        }
    }
}

@Composable
private fun CardTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
