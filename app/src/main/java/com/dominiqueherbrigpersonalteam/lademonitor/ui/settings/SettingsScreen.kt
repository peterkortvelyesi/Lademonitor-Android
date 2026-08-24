package com.dominiqueherbrigpersonalteam.lademonitor.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.dominiqueherbrigpersonalteam.lademonitor.data.repo.LocalDataStore
import com.dominiqueherbrigpersonalteam.lademonitor.data.repo.SyncService
import com.dominiqueherbrigpersonalteam.lademonitor.data.session.SessionManager
import com.dominiqueherbrigpersonalteam.lademonitor.data.settings.AppMode
import com.dominiqueherbrigpersonalteam.lademonitor.data.settings.AppSettings
import com.dominiqueherbrigpersonalteam.lademonitor.ui.common.Fmt
import com.dominiqueherbrigpersonalteam.lademonitor.ui.common.SectionCard
import androidx.compose.material3.AlertDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val appMode by AppSettings.appMode.collectAsStateWithLifecycle()
    val user by SessionManager.currentUser.collectAsStateWithLifecycle()
    val isSyncing by SyncService.isSyncing.collectAsStateWithLifecycle()
    val lastSyncDate by SyncService.lastSyncDate.collectAsStateWithLifecycle()
    val lastSyncError by SyncService.lastSyncError.collectAsStateWithLifecycle()

    var isLoggingOut by remember { mutableStateOf(false) }
    var showServerSwitch by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var resetError by remember { mutableStateOf<String?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Einstellungen") }) }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode
            SectionCard {
                Header("Modus")
                LabeledRow("Aktuell", if (appMode == AppMode.LOCAL_ONLY) "Nur lokal" else "Server")
                if (appMode == AppMode.LOCAL_ONLY) {
                    ActionText("Zu Server wechseln") { showServerSwitch = true }
                    Text(
                        "Alle Daten liegen ausschließlich auf diesem Gerät.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    ActionText("Zu \"Nur lokal\" wechseln") { AppSettings.setAppMode(AppMode.LOCAL_ONLY) }
                }
            }

            if (appMode == AppMode.SERVER) {
                SectionCard {
                    Header("Konto")
                    user?.let { LabeledRow("Angemeldet als", it.username) }
                    if (isLoggingOut) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("Abmelden…", color = MaterialTheme.colorScheme.error)
                            CircularProgressIndicator(Modifier.padding(start = 8.dp).height(18.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        ActionText("Abmelden", color = MaterialTheme.colorScheme.error) {
                            scope.launch { isLoggingOut = true; SessionManager.logout(); isLoggingOut = false }
                        }
                    }
                }

                SectionCard {
                    Header("Verbindung")
                    NavRow("Server & Verbindung") { navController.navigate("settings/connection") }
                }

                SectionCard {
                    Header("Synchronisierung")
                    if (lastSyncDate != null) LabeledRow("Zuletzt synchronisiert", Fmt.relative(lastSyncDate!!))
                    else Text("Noch nicht synchronisiert", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    lastSyncError?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    if (isSyncing) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("Synchronisiert…")
                            CircularProgressIndicator(Modifier.padding(start = 8.dp).height(18.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        ActionText("Jetzt synchronisieren") { scope.launch { SyncService.syncNow() } }
                    }
                }
            }

            SectionCard {
                Header("Verwaltung")
                NavRow("Ladeorte") { navController.navigate("settings/locations") }
                HorizontalDivider()
                NavRow("Fahrzeuge") { navController.navigate("settings/vehicles") }
                HorizontalDivider()
                NavRow("Anbieter") { navController.navigate("settings/providers") }
            }

            SectionCard {
                ActionText("Alle lokalen Daten zurücksetzen", color = MaterialTheme.colorScheme.error) {
                    showReset = true
                }
                resetError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                Text(
                    if (appMode == AppMode.SERVER)
                        "Löscht Fahrzeuge, Anbieter, Ladeorte und Ladevorgänge auf diesem Gerät. Bereits synchronisierte Daten bleiben auf dem Server und werden danach automatisch zurückgeholt – noch nicht hochgeladene Änderungen gehen verloren."
                    else
                        "Löscht Fahrzeuge, Anbieter, Ladeorte und Ladevorgänge unwiderruflich von diesem Gerät. Es gibt keine weitere Kopie.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    if (showServerSwitch) {
        AlertDialog(
            onDismissRequest = { showServerSwitch = false },
            title = { Text("Zu Server wechseln?") },
            text = { Text("Nach der Anmeldung werden deine bisherigen lokalen Daten automatisch zum Server hochgeladen. Du kannst jederzeit zurück zu \"Nur lokal\" wechseln.") },
            confirmButton = { TextButton(onClick = { showServerSwitch = false; AppSettings.setAppMode(AppMode.SERVER) }) { Text("Wechseln") } },
            dismissButton = { TextButton(onClick = { showServerSwitch = false }) { Text("Abbrechen") } }
        )
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Alle lokalen Daten löschen?") },
            text = { Text("Das kann nicht rückgängig gemacht werden.") },
            confirmButton = {
                TextButton(onClick = {
                    showReset = false
                    scope.launch {
                        resetError = null
                        try {
                            LocalDataStore.resetAllData()
                            if (appMode == AppMode.SERVER) SyncService.syncNow()
                        } catch (e: Exception) { resetError = e.localizedMessage }
                    }
                }) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Abbrechen") } }
        )
    }
}

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActionText(text: String, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary, onClick: () -> Unit) {
    Text(
        text, color = color,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp)
    )
}

@Composable
private fun NavRow(text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
