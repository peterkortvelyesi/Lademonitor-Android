package com.dominiqueherbrigpersonalteam.lademonitor.data.repo

import com.dominiqueherbrigpersonalteam.lademonitor.data.model.ChargingSession
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.ChargingType
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.MonthlyStat
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.Provider
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.ProviderStat
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.StatsSummary
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.Vehicle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

/**
 * Local port of the server's `routers/stats.py`, matching the iOS `LocalStatsCalculator`. Order,
 * rounding and sign conventions are kept identical so numbers don't visibly "jump" when switching
 * between local-only and server mode.
 */
object LocalStatsCalculator {

    private val monthFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM")

    private fun monthKey(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(monthFormatter)

    fun compute(
        sessions: List<ChargingSession>,
        vehicles: List<Vehicle>,
        providers: List<Provider>
    ): StatsSummary {
        val sorted = sessions.sortedBy { it.startTime }

        val totalSessions = sorted.size
        val totalKwh = round2(sorted.sumOf { it.energyKwh ?: 0.0 })
        val totalCost = round2(sorted.sumOf { it.priceTotal ?: 0.0 })
        val avgPrice = if (totalKwh > 0) round4(totalCost / totalKwh) else null

        val acKwh = sorted.filter { it.chargingTypeValue == ChargingType.AC }.sumOf { it.energyKwh ?: 0.0 }
        val dcKwh = sorted.filter { it.chargingTypeValue == ChargingType.DC }.sumOf { it.energyKwh ?: 0.0 }
        val typedKwh = acKwh + dcKwh
        val acShare = if (typedKwh > 0) round1(acKwh / typedKwh * 100) else null
        val dcShare = if (typedKwh > 0) round1(dcKwh / typedKwh * 100) else null

        val withOdo = sorted.filter { it.odometerKm != null }
        var consumption: Double? = null
        var pricePer100km: Double? = null
        var totalKmDriven: Int? = null
        if (withOdo.size >= 2) {
            val first = withOdo.first().odometerKm
            val last = withOdo.last().odometerKm
            if (first != null && last != null) {
                val kmDriven = last - first
                totalKmDriven = kmDriven
                if (kmDriven > 0) {
                    val kwhInRange = withOdo.drop(1).sumOf { it.energyKwh ?: 0.0 }
                    consumption = round1(kwhInRange / kmDriven.toDouble() * 100)
                    val costInRange = withOdo.drop(1).sumOf { it.priceTotal ?: 0.0 }
                    pricePer100km = round2(costInRange / kmDriven.toDouble() * 100)
                }
            }
        }

        // Provider split; sessions without provider -> "Ohne Anbieter".
        val providerKwh = HashMap<String, Double>()
        val providerCost = HashMap<String, Double>()
        for (session in sorted) {
            val name = providers.firstOrNull { it.id == session.providerId }?.name ?: "Ohne Anbieter"
            providerKwh[name] = (providerKwh[name] ?: 0.0) + (session.energyKwh ?: 0.0)
            providerCost[name] = (providerCost[name] ?: 0.0) + (session.priceTotal ?: 0.0)
        }
        val byProvider = providerKwh.keys
            .sortedByDescending { providerKwh[it]!! }
            .map { name ->
                ProviderStat(name, round2(providerKwh[name]!!), round2(providerCost[name]!!))
            }

        // Monthly consumption: km-weighted mean over per-vehicle consumption results.
        val consumptionByVehicle = HashMap<String, LocalConsumptionCalculator.Result>()
        for (vehicle in vehicles) {
            val vehicleSessions = sorted.filter { it.vehicleId == vehicle.id }
            consumptionByVehicle.putAll(
                LocalConsumptionCalculator.compute(vehicleSessions, vehicle.batteryCapacityKwh)
            )
        }

        val monthlyCost = HashMap<String, Double>()
        val monthlyKwh = HashMap<String, Double>()
        val monthlyCount = HashMap<String, Int>()
        val monthlyConsumptionNum = HashMap<String, Double>()
        val monthlyConsumptionKm = HashMap<String, Double>()
        for (session in sorted) {
            val key = monthKey(session.startTime)
            monthlyCost[key] = (monthlyCost[key] ?: 0.0) + (session.priceTotal ?: 0.0)
            monthlyKwh[key] = (monthlyKwh[key] ?: 0.0) + (session.energyKwh ?: 0.0)
            monthlyCount[key] = (monthlyCount[key] ?: 0) + 1
            val result = consumptionByVehicle[session.id]
            val value = result?.value
            val km = result?.km
            if (value != null && km != null && km > 0) {
                monthlyConsumptionNum[key] = (monthlyConsumptionNum[key] ?: 0.0) + value * km
                monthlyConsumptionKm[key] = (monthlyConsumptionKm[key] ?: 0.0) + km
            }
        }
        val monthly = monthlyCost.keys.sortedDescending().map { month ->
            val km = monthlyConsumptionKm[month] ?: 0.0
            MonthlyStat(
                month = month,
                totalCost = round2(monthlyCost[month] ?: 0.0),
                totalKwh = round2(monthlyKwh[month] ?: 0.0),
                sessionCount = monthlyCount[month] ?: 0,
                avgConsumptionKwhPer100km = if (km > 0) round1(monthlyConsumptionNum[month]!! / km) else null
            )
        }

        return StatsSummary(
            totalSessions = totalSessions,
            totalKwh = totalKwh,
            totalCost = totalCost,
            avgPricePerKwh = avgPrice,
            avgConsumptionKwhPer100km = consumption,
            pricePer100km = pricePer100km,
            acSharePct = acShare,
            dcSharePct = dcShare,
            acKwh = round2(acKwh),
            dcKwh = round2(dcKwh),
            totalKmDriven = totalKmDriven,
            byProvider = byProvider,
            monthly = monthly
        )
    }

    private fun round1(v: Double): Double = (v * 10).roundToLong() / 10.0
    private fun round2(v: Double): Double = (v * 100).roundToLong() / 100.0
    private fun round4(v: Double): Double = (v * 10000).roundToLong() / 10000.0
}
