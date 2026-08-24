package com.dominiqueherbrigpersonalteam.lademonitor.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dominiqueherbrigpersonalteam.lademonitor.data.session.SessionManager
import com.dominiqueherbrigpersonalteam.lademonitor.data.settings.AppMode
import com.dominiqueherbrigpersonalteam.lademonitor.data.settings.AppSettings
import com.dominiqueherbrigpersonalteam.lademonitor.ui.auth.AuthScreen
import com.dominiqueherbrigpersonalteam.lademonitor.ui.auth.ModeSelectionScreen
import com.dominiqueherbrigpersonalteam.lademonitor.ui.dashboard.DashboardScreen
import com.dominiqueherbrigpersonalteam.lademonitor.ui.map.MapScreen
import com.dominiqueherbrigpersonalteam.lademonitor.ui.sessions.SessionsListScreen
import com.dominiqueherbrigpersonalteam.lademonitor.ui.settings.ConnectionSettingsScreen
import com.dominiqueherbrigpersonalteam.lademonitor.ui.settings.LocationsSettingsScreen
import com.dominiqueherbrigpersonalteam.lademonitor.ui.settings.ProvidersSettingsScreen
import com.dominiqueherbrigpersonalteam.lademonitor.ui.settings.SettingsScreen
import com.dominiqueherbrigpersonalteam.lademonitor.ui.settings.VehiclesSettingsScreen

@Composable
fun LademonitorRoot() {
    val appMode by AppSettings.appMode.collectAsStateWithLifecycle()
    val isAuthenticated by SessionManager.isAuthenticated.collectAsStateWithLifecycle()

    when (appMode) {
        AppMode.UNDECIDED -> ModeSelectionScreen()
        AppMode.LOCAL_ONLY -> MainScaffold()
        AppMode.SERVER -> {
            if (isAuthenticated) {
                LaunchedEffect(Unit) { SessionManager.refreshCurrentUserIfNeeded() }
                MainScaffold()
            } else {
                AuthScreen()
            }
        }
    }
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Dashboard", Icons.Filled.BarChart),
    SESSIONS("sessions", "Ladevorgänge", Icons.Filled.Bolt),
    MAP("map", "Karte", Icons.Filled.Map),
    SETTINGS("settings", "Einstellungen", Icons.Filled.Settings)
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.DASHBOARD.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Tab.DASHBOARD.route) { DashboardScreen() }
            composable(Tab.SESSIONS.route) { SessionsListScreen() }
            composable(Tab.MAP.route) { MapScreen() }
            composable(Tab.SETTINGS.route) { SettingsScreen(navController) }
            composable("settings/vehicles") { VehiclesSettingsScreen(navController) }
            composable("settings/providers") { ProvidersSettingsScreen(navController) }
            composable("settings/locations") { LocationsSettingsScreen(navController) }
            composable("settings/connection") { ConnectionSettingsScreen(navController) }
        }
    }
}
