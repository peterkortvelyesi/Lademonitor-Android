package com.dominiqueherbrigpersonalteam.lademonitor.data.repo

import com.dominiqueherbrigpersonalteam.lademonitor.data.model.ChargingSession
import com.dominiqueherbrigpersonalteam.lademonitor.data.model.ConsumptionMethod
import kotlin.math.roundToLong

/**
 * Local, simplified port of the server's `consumption.py`, matching the iOS `LocalConsumptionCalculator`.
 *
 * Base formula: consumedEnergy = energy_kwh(N) - capacity * (socEnd(N) - socEnd(N-1)) / 100,
 * consumption = consumedEnergy / kmDelta * 100.
 *
 * Deliberate simplification vs. the server: the exact "full_charge_interval" calibration is not
 * ported — local sessions fall back to soc_corrected/naive. On sync the server recomputes anyway,
 * so the local value is only an approximation for display.
 */
object LocalConsumptionCalculator {

    data class Result(
        val value: Double? = null,
        val method: String? = null,
        /** km delta to the predecessor — only used to weight the monthly average, not shown. */
        val km: Double? = null
    )

    fun compute(sessions: List<ChargingSession>, batteryCapacityKwh: Double?): Map<String, Result> {
        val sorted = sessions.sortedBy { it.startTime }
        val result = HashMap<String, Result>()

        for ((index, session) in sorted.withIndex()) {
            if (index == 0) {
                result[session.id] = Result(null, ConsumptionMethod.UNAVAILABLE.raw, null)
                continue
            }
            val previous = sorted[index - 1]
            val odometerNow = session.odometerKm
            val odometerPrevious = previous.odometerKm
            val energyKwh = session.energyKwh
            if (odometerNow == null || odometerPrevious == null ||
                odometerNow <= odometerPrevious || energyKwh == null
            ) {
                result[session.id] = Result(null, ConsumptionMethod.UNAVAILABLE.raw, null)
                continue
            }

            val kmDriven = (odometerNow - odometerPrevious).toDouble()
            var consumedEnergy = energyKwh
            val method: ConsumptionMethod
            val socEndNow = session.socEnd
            val socEndPrevious = previous.socEnd
            if (batteryCapacityKwh != null && socEndNow != null && socEndPrevious != null) {
                val socDelta = (socEndNow - socEndPrevious).toDouble()
                consumedEnergy = energyKwh - batteryCapacityKwh * socDelta / 100.0
                method = if (session.energyIsEstimated) ConsumptionMethod.ESTIMATED_ENERGY
                else ConsumptionMethod.SOC_CORRECTED
            } else {
                method = if (session.energyIsEstimated) ConsumptionMethod.ESTIMATED_ENERGY
                else ConsumptionMethod.NAIVE
            }

            val value = (consumedEnergy / kmDriven * 100 * 10).roundToLong() / 10.0
            result[session.id] = Result(value, method.raw, kmDriven)
        }
        return result
    }
}
