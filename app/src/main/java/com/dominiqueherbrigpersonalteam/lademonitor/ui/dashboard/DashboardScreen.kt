package com.dominiqueherbrigpersonalteam.lademonitor.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dominiqueherbrigpersonalteam.lademonitor.LademonitorApp
import com.dominiqueherbrigpersonalteam.lademonitor.R
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.StatsSummary
import com.dominiqueherbrigpersonalteam.lademonitor.data.repo.AppRepository
import com.dominiqueherbrigpersonalteam.lademonitor.data.settings.AppSettings
import com.dominiqueherbrigpersonalteam.lademonitor.ui.common.ErrorState
import com.dominiqueherbrigpersonalteam.lademonitor.ui.common.Fmt
import com.dominiqueherbrigpersonalteam.lademonitor.ui.common.SectionCard
import com.dominiqueherbrigpersonalteam.lademonitor.ui.filter.FilterIconButton
import com.dominiqueherbrigpersonalteam.lademonitor.ui.filter.FilterSheet
import com.dominiqueherbrigpersonalteam.lademonitor.ui.filter.SessionFilter
import com.dominiqueherbrigpersonalteam.lademonitor.ui.theme.Blue
import com.dominiqueherbrigpersonalteam.lademonitor.ui.theme.Green
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val scope = rememberCoroutineScope()
    val dateRange by SessionFilter.dateRange.collectAsStateWithLifecycle()

    var stats by remember { mutableStateOf<StatsSummary?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    val serverUrlRequiredMessage = stringResource(R.string.error_server_url_required)

    suspend fun load() {
        if (!AppSettings.isReadyForDataAccess) {
            errorMessage = serverUrlRequiredMessage
            return
        }
        isLoading = true
        try {
            stats = AppRepository.fetchStatsSummary(dateRange = dateRange)
            errorMessage = null
        } catch (e: Exception) {
            if (stats == null) errorMessage = e.localizedMessage
        }
        isLoading = false
    }

    LaunchedEffect(dateRange) { load() }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.tab_dashboard)) },
            actions = {
                IconButton(onClick = { scope.launch { load() } }) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
                }
                FilterIconButton(onClick = { showFilter = true })
            }
        )
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val current = stats
            when {
                errorMessage != null && current == null ->
                    ErrorState(errorMessage!!, onRetry = { scope.launch { load() } })
                current != null -> DashboardContent(current)
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (showFilter) FilterSheet(onDismiss = { showFilter = false })
}

@Composable
private fun DashboardContent(stats: StatsSummary) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Stat grid
        val cards = buildList {
            add(stringResource(R.string.dashboard_stat_sessions) to "${stats.totalSessions}")
            add(stringResource(R.string.dashboard_stat_total_kwh) to Fmt.n("%.1f kWh", stats.totalKwh))
            add(stringResource(R.string.dashboard_stat_total_cost) to Fmt.n("%.2f €", stats.totalCost))
            add(stringResource(R.string.dashboard_stat_avg_price) to (stats.avgPricePerKwh?.let { Fmt.n("%.3f €", it) } ?: "–"))
            add(stringResource(R.string.dashboard_stat_avg_consumption) to (stats.avgConsumptionKwhPer100km?.let { Fmt.n("%.1f kWh", it) } ?: "–"))
            add(stringResource(R.string.dashboard_stat_price_per_100km) to (stats.pricePer100km?.let { Fmt.n("%.2f €", it) } ?: "–"))
            add(stringResource(R.string.dashboard_stat_km_driven) to (stats.totalKmDriven?.let { Fmt.km(it) } ?: "–"))
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            cards.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (label, value) ->
                        StatCard(label, value, Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        val acKwh = stats.acKwh
        val dcKwh = stats.dcKwh
        if (acKwh != null && dcKwh != null && acKwh + dcKwh > 0) {
            Column {
                SectionHeader(stringResource(R.string.dashboard_section_ac_dc))
                SectionCard { AcDcBar(acKwh, dcKwh) }
            }
        }

        if (stats.byProvider.isNotEmpty()) {
            val entries = providerEntries(stats)
            SectionCard {
                SectionHeader(stringResource(R.string.dashboard_section_kwh_by_provider))
                DonutChart(entries.map { PieEntry(it.name, it.kwh, it.color) }, unit = "kWh")
            }
            Spacer(Modifier.height(4.dp))
            SectionCard {
                SectionHeader(stringResource(R.string.dashboard_section_paid_by_provider))
                DonutChart(entries.map { PieEntry(it.name, it.cost, it.color) }, unit = "€")
            }
        }

        val consumptionPoints = stats.monthly
            .reversed()
            .mapNotNull { m -> m.avgConsumptionKwhPer100km?.let { m.shortMonth to it } }
        if (consumptionPoints.isNotEmpty()) {
            Column {
                SectionHeader(stringResource(R.string.dashboard_stat_avg_consumption))
                SectionCard { VerticalBarChart(consumptionPoints) }
            }
        }

        if (stats.monthly.isNotEmpty()) {
            Column {
                SectionHeader(stringResource(R.string.dashboard_section_cost_by_month))
                SectionCard {
                    HorizontalBarChart(stats.monthly.map { it.displayMonth to it.totalCost }, Blue, "€")
                }
            }
            Column {
                SectionHeader(stringResource(R.string.dashboard_section_kwh_by_month))
                SectionCard {
                    HorizontalBarChart(stats.monthly.map { it.displayMonth to it.totalKwh }, Green, " kWh")
                }
            }
        }
    }
}

private data class ProviderEntry(val name: String, val kwh: Double, val cost: Double, val color: androidx.compose.ui.graphics.Color)

/** Port of the iOS provider grouping: top 5 named providers + "Other" (overflow + "no provider"). */
private fun providerEntries(stats: StatsSummary): List<ProviderEntry> {
    val context = LademonitorApp.appContext
    // The "no provider" placeholder may come back from a German-only server (untranslated) or
    // from the local calculator (already localized), so match either form.
    val noProviderLabel = context.getString(R.string.stats_no_provider)
    fun isNoProvider(name: String) = name == noProviderLabel || name == "Ohne Anbieter"

    val named = stats.byProvider.filterNot { isNoProvider(it.providerName) }
    val noName = stats.byProvider.filter { isNoProvider(it.providerName) }
    val top = named.take(5)
    val overflow = named.drop(5)
    val otherItems = overflow + noName

    val result = top.mapIndexed { i, p ->
        ProviderEntry(p.providerName, p.totalKwh, p.totalCost, ChartPalette[i % ChartPalette.size])
    }.toMutableList()

    if (otherItems.isNotEmpty()) {
        result.add(
            ProviderEntry(
                context.getString(R.string.dashboard_provider_other),
                otherItems.sumOf { it.totalKwh },
                otherItems.sumOf { it.totalCost },
                androidx.compose.ui.graphics.Color.Gray
            )
        )
    }
    return result
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}
