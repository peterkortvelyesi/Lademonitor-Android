package com.dominiqueherbrigpersonalteam.lademonitor.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dominiqueherbrigpersonalteam.lademonitor.data.location.CurrentLocationProvider
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.ChargingLocation
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.GeocodeResult
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.LocationPayload
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.Provider
import com.dominiqueherbrigpersonalteam.lademonitor.data.repo.AppRepository
import com.dominiqueherbrigpersonalteam.lademonitor.data.settings.AppSettings
import com.dominiqueherbrigpersonalteam.lademonitor.ui.common.Fmt
import com.dominiqueherbrigpersonalteam.lademonitor.ui.common.FullScreenModal
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationsSettingsScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var locations by remember { mutableStateOf<List<ChargingLocation>>(emptyList()) }
    var providers by remember { mutableStateOf<List<Provider>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ChargingLocation?>(null) }
    var pendingDelete by remember { mutableStateOf<ChargingLocation?>(null) }

    suspend fun load() {
        if (!AppSettings.isReadyForDataAccess) { errorMessage = "Bitte zuerst die Server-Adresse eintragen."; return }
        try {
            coroutineScope {
                val l = async { AppRepository.fetchLocations() }
                val p = async { AppRepository.fetchProviders() }
                locations = l.await(); providers = p.await()
            }
            errorMessage = null
        } catch (e: Exception) { if (locations.isEmpty()) errorMessage = e.localizedMessage }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Ladeorte") },
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
            actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "Neu") } }
        )
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            errorMessage?.let { item { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            items(locations, key = { it.id }) { location ->
                Row(Modifier.fillMaxWidth().clickable { editing = location }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(location.name, style = MaterialTheme.typography.bodyLarge)
                        val providerName = providers.firstOrNull { it.id == location.defaultProviderId }?.name
                        val detail = listOfNotNull(
                            Fmt.n("%.5f", location.latitude) + ", " + Fmt.n("%.5f", location.longitude),
                            "Radius ${location.radiusM} m",
                            providerName
                        ).joinToString(" · ")
                        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { pendingDelete = location }) { Icon(Icons.Filled.Delete, "Löschen", tint = MaterialTheme.colorScheme.error) }
                }
                HorizontalDivider()
            }
        }
    }

    if (showAdd) AddEditLocationModal(null, providers, onDismiss = { showAdd = false }) { showAdd = false; scope.launch { load() } }
    editing?.let { l -> AddEditLocationModal(l, providers, onDismiss = { editing = null }) { editing = null; scope.launch { load() } } }

    pendingDelete?.let { l ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Ladeort löschen?") },
            text = { Text("„${l.name}“ wird gelöscht.") },
            confirmButton = {
                TextButton(onClick = {
                    val target = l; pendingDelete = null
                    scope.launch { runCatching { AppRepository.deleteLocation(target.id) }; load() }
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Abbrechen") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditLocationModal(
    location: ChargingLocation?,
    providers: List<Provider>,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEditing = location != null

    var providerList by remember { mutableStateOf(providers) }
    var name by remember { mutableStateOf(location?.name ?: "") }
    var latitude by remember { mutableStateOf(location?.let { Fmt.n("%.6f", it.latitude) } ?: "") }
    var longitude by remember { mutableStateOf(location?.let { Fmt.n("%.6f", it.longitude) } ?: "") }
    var radius by remember { mutableStateOf(location?.radiusM?.toString() ?: "100") }
    var defaultProviderId by remember { mutableStateOf(location?.defaultProviderId) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddProvider by remember { mutableStateOf(false) }

    var addressQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GeocodeResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchMessage by remember { mutableStateOf<String?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var providerMenu by remember { mutableStateOf(false) }

    val parsedLat = latitude.replace(",", ".").toDoubleOrNull()
    val parsedLon = longitude.replace(",", ".").toDoubleOrNull()
    val canSave = name.trim().isNotEmpty() && parsedLat != null && parsedLon != null

    fun fetchLocation() {
        scope.launch {
            isLocating = true; errorMessage = null
            try {
                val c = CurrentLocationProvider.requestCurrentLocation(context)
                latitude = Fmt.n("%.6f", c.latitude); longitude = Fmt.n("%.6f", c.longitude)
            } catch (e: Exception) { errorMessage = e.localizedMessage }
            isLocating = false
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) fetchLocation() else errorMessage = CurrentLocationProvider.LocationException.Denied.message
    }

    fun search() {
        val q = addressQuery.trim(); if (q.isEmpty()) return
        scope.launch {
            isSearching = true; searchMessage = null
            try {
                val results = AppRepository.forwardGeocode(q)
                searchResults = results
                if (results.isEmpty()) searchMessage = "Keine Treffer. Bitte die Koordinaten unten manuell eintragen."
            } catch (e: Exception) { searchResults = emptyList(); searchMessage = "Suche fehlgeschlagen." }
            isSearching = false
        }
    }

    fun save() {
        scope.launch {
            isSaving = true; errorMessage = null
            val payload = LocationPayload(
                name = name.trim(),
                latitude = parsedLat,
                longitude = parsedLon,
                radiusM = radius.toIntOrNull() ?: 100,
                defaultProviderId = defaultProviderId
            )
            try {
                if (location != null) AppRepository.updateLocation(location.id, payload)
                else AppRepository.createLocation(payload)
                onSaved()
            } catch (e: Exception) { errorMessage = e.localizedMessage }
            isSaving = false
        }
    }

    FullScreenModal(onDismiss = onDismiss) {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Ladeort bearbeiten" else "Neuer Ladeort") },
                navigationIcon = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
                actions = { TextButton(onClick = { save() }, enabled = !isSaving && canSave) { Text(if (isSaving) "Speichert…" else "Speichern") } }
            )
        }) { padding ->
            Column(Modifier.padding(padding).padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = addressQuery, onValueChange = { addressQuery = it }, label = { Text("Adresse suchen") }, singleLine = true, modifier = Modifier.weight(1f))
                    if (isSearching) CircularProgressIndicator(Modifier.padding(8.dp).height(24.dp), strokeWidth = 2.dp)
                    else IconButton(onClick = { search() }, enabled = addressQuery.isNotBlank()) { Icon(Icons.Filled.Search, "Suchen") }
                }
                searchMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                searchResults.forEach { r ->
                    TextButton(onClick = {
                        latitude = Fmt.n("%.6f", r.latitude); longitude = Fmt.n("%.6f", r.longitude)
                        addressQuery = r.displayName; searchResults = emptyList(); searchMessage = null
                    }, modifier = Modifier.fillMaxWidth()) { Text(r.displayName, Modifier.fillMaxWidth()) }
                }

                OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }, enabled = !isLocating, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null)
                    Text("  Aktueller Standort")
                    if (isLocating) { Spacer(Modifier.weight(1f)); CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp) }
                }

                OutlinedTextField(value = latitude, onValueChange = { latitude = it }, label = { Text("Breite (lat)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = longitude, onValueChange = { longitude = it }, label = { Text("Länge (lon)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = radius, onValueChange = { radius = it.filter { c -> c.isDigit() } }, label = { Text("Radius (m)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Text("Der Radius bestimmt, wie nah ein Ladevorgang sein muss, um automatisch diesem Ort zugeordnet zu werden.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Text("Standard-Anbieter", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                Box {
                    OutlinedButton(onClick = { providerMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(providerList.firstOrNull { it.id == defaultProviderId }?.name ?: "– keiner –")
                    }
                    DropdownMenu(expanded = providerMenu, onDismissRequest = { providerMenu = false }) {
                        DropdownMenuItem(text = { Text("– keiner –") }, onClick = { defaultProviderId = null; providerMenu = false })
                        providerList.forEach { p ->
                            DropdownMenuItem(text = { Text(p.name) }, onClick = { defaultProviderId = p.id; providerMenu = false })
                        }
                        DropdownMenuItem(text = { Text("Neuer Anbieter…") }, onClick = { providerMenu = false; showAddProvider = true })
                    }
                }

                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (showAddProvider) {
        AddEditProviderModal(provider = null, onDismiss = { showAddProvider = false }) { newProvider ->
            providerList = providerList + newProvider
            defaultProviderId = newProvider.id
            showAddProvider = false
        }
    }
}
