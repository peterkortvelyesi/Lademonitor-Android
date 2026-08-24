package com.dominiqueherbrigpersonalteam.lademonitor.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.Vehicle
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.VehiclePayload
import com.dominiqueherbrigpersonalteam.lademonitor.data.repo.AppRepository
import com.dominiqueherbrigpersonalteam.lademonitor.data.settings.AppSettings
import com.dominiqueherbrigpersonalteam.lademonitor.ui.common.Fmt
import com.dominiqueherbrigpersonalteam.lademonitor.ui.common.FullScreenModal
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesSettingsScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var vehicles by remember { mutableStateOf<List<Vehicle>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Vehicle?>(null) }
    var pendingDelete by remember { mutableStateOf<Vehicle?>(null) }

    suspend fun load() {
        if (!AppSettings.isReadyForDataAccess) { errorMessage = "Bitte zuerst die Server-Adresse eintragen."; return }
        try { vehicles = AppRepository.fetchVehicles(); errorMessage = null }
        catch (e: Exception) { if (vehicles.isEmpty()) errorMessage = e.localizedMessage }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Fahrzeuge") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
            actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "Neu") } }
        )
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            errorMessage?.let { item { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            items(vehicles, key = { it.id }) { vehicle ->
                Row(
                    Modifier.fillMaxWidth().clickable { editing = vehicle }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(vehicle.name, style = MaterialTheme.typography.bodyLarge)
                            if (!vehicle.isActive) {
                                Spacer(Modifier.width(6.dp))
                                Box(Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("inaktiv", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        val subtitle = listOfNotNull(vehicle.brand, vehicle.model).joinToString(" · ")
                        if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    vehicle.batteryCapacityKwh?.let {
                        Text(Fmt.n("%.0f kWh", it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { pendingDelete = vehicle }) { Icon(Icons.Filled.Delete, "Löschen", tint = MaterialTheme.colorScheme.error) }
                }
                HorizontalDivider()
            }
        }
    }

    if (showAdd) AddEditVehicleModal(null, onDismiss = { showAdd = false }) { showAdd = false; scope.launch { load() } }
    editing?.let { v -> AddEditVehicleModal(v, onDismiss = { editing = null }) { editing = null; scope.launch { load() } } }

    pendingDelete?.let { v ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Fahrzeug löschen?") },
            text = { Text("Achtung: Dabei werden auch ALLE zugehörigen Ladevorgänge dieses Fahrzeugs unwiderruflich gelöscht.") },
            confirmButton = {
                TextButton(onClick = {
                    val target = v; pendingDelete = null
                    scope.launch { runCatching { AppRepository.deleteVehicle(target.id) }; load() }
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Abbrechen") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVehicleModal(vehicle: Vehicle?, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    val isEditing = vehicle != null
    var externalId by remember { mutableStateOf(vehicle?.externalId ?: "") }
    var name by remember { mutableStateOf(vehicle?.name ?: "") }
    var brand by remember { mutableStateOf(vehicle?.brand ?: "") }
    var model by remember { mutableStateOf(vehicle?.model ?: "") }
    var battery by remember { mutableStateOf(vehicle?.batteryCapacityKwh?.let { Fmt.n("%.1f", it) } ?: "") }
    var isActive by remember { mutableStateOf(vehicle?.isActive ?: true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val canSave = name.trim().isNotEmpty() && (isEditing || externalId.trim().isNotEmpty())

    fun save() {
        scope.launch {
            isSaving = true; errorMessage = null
            val payload = VehiclePayload(
                externalId = if (isEditing) null else externalId.trim(),
                name = name.trim(),
                brand = brand.trim().ifEmpty { null },
                model = model.trim().ifEmpty { null },
                batteryCapacityKwh = battery.replace(",", ".").toDoubleOrNull(),
                isActive = isActive
            )
            try {
                if (vehicle != null) AppRepository.updateVehicle(vehicle.id, payload)
                else AppRepository.createVehicle(payload)
                onSaved()
            } catch (e: Exception) { errorMessage = e.localizedMessage }
            isSaving = false
        }
    }

    FullScreenModal(onDismiss = onDismiss) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Fahrzeug bearbeiten" else "Neues Fahrzeug") },
                navigationIcon = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
                actions = { TextButton(onClick = { save() }, enabled = !isSaving && canSave) { Text(if (isSaving) "Speichert…" else "Speichern") } }
            )
        }) { padding ->
            Column(Modifier.padding(padding).padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = externalId, onValueChange = { externalId = it }, label = { Text("Externe ID (z.B. enyaq)") },
                    singleLine = true, enabled = !isEditing, modifier = Modifier.fillMaxWidth()
                )
                if (isEditing) Text("Die externe ID ist nach dem Anlegen nicht mehr änderbar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Marke (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Modell (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = battery, onValueChange = { battery = it }, label = { Text("Akkukapazität (kWh, optional)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Aktiv", Modifier.weight(1f))
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
