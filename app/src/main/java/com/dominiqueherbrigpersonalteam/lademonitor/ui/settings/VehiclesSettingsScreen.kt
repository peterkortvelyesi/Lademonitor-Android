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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dominiqueherbrigpersonalteam.lademonitor.R
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
    val serverAddressRequiredMessage = stringResource(R.string.error_server_address_required)

    suspend fun load() {
        if (!AppSettings.isReadyForDataAccess) { errorMessage = serverAddressRequiredMessage; return }
        try { vehicles = AppRepository.fetchVehicles(); errorMessage = null }
        catch (e: Exception) { if (vehicles.isEmpty()) errorMessage = e.localizedMessage }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_nav_vehicles)) },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } },
            actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, stringResource(R.string.sessions_add_content_description)) } }
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
                                    Text(stringResource(R.string.vehicle_inactive_badge), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        val subtitle = listOfNotNull(vehicle.brand, vehicle.model).joinToString(" · ")
                        if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    vehicle.batteryCapacityKwh?.let {
                        Text(Fmt.n("%.0f kWh", it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { pendingDelete = vehicle }) { Icon(Icons.Filled.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error) }
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
            title = { Text(stringResource(R.string.vehicle_delete_confirm_title)) },
            text = { Text(stringResource(R.string.vehicle_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val target = v; pendingDelete = null
                    scope.launch { runCatching { AppRepository.deleteVehicle(target.id) }; load() }
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) } }
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
                title = { Text(if (isEditing) stringResource(R.string.vehicle_edit_title) else stringResource(R.string.vehicle_add_title)) },
                navigationIcon = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
                actions = { TextButton(onClick = { save() }, enabled = !isSaving && canSave) { Text(if (isSaving) stringResource(R.string.action_saving) else stringResource(R.string.action_save)) } }
            )
        }) { padding ->
            Column(Modifier.padding(padding).padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = externalId, onValueChange = { externalId = it }, label = { Text(stringResource(R.string.vehicle_field_external_id)) },
                    singleLine = true, enabled = !isEditing, modifier = Modifier.fillMaxWidth()
                )
                if (isEditing) Text(stringResource(R.string.vehicle_external_id_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.field_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text(stringResource(R.string.vehicle_field_brand)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text(stringResource(R.string.vehicle_field_model)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = battery, onValueChange = { battery = it }, label = { Text(stringResource(R.string.vehicle_field_battery_capacity)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.field_active), Modifier.weight(1f))
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
