package com.example.data

import kotlin.math.ceil
import kotlin.math.roundToInt

object AttendanceMath {
    const val EXAM_THRESHOLD_PCT = 80

    fun safeTotalDays(present: Int, half: Int, rawTotal: Int): Int {
        return maxOf(rawTotal, present + half, 0)
    }

    fun computePercentage(present: Int, half: Int, rawTotal: Int, rawPct: Int): Int {
        val total = safeTotalDays(present, half, rawTotal)
        if (total <= 0) return rawPct.coerceIn(0, 100).let { if (it == 0) 100 else it }
        val computed = (((present + half * 0.5) / total.toDouble()) * 100.0).roundToInt()
        return computed.coerceIn(0, 100)
    }

    fun sessionsNeededForTargetPct(
        present: Int,
        half: Int,
        total: Int,
        targetPct: Double = 80.0
    ): Int {
        val safeTotal = safeTotalDays(present, half, total)
        if (safeTotal == 0) return 0
        val currentUnits = present + half * 0.5
        val targetFraction = (targetPct / 100.0).coerceIn(0.01, 0.99)
        if (currentUnits / safeTotal >= targetFraction) return 0
        val needed = ceil((targetFraction * safeTotal - currentUnits) / (1.0 - targetFraction))
        return needed.toInt().coerceAtLeast(0)
    }

    fun formatShopFloorHours(durationMinutes: Int): String {
        val safeMins = durationMinutes.coerceAtLeast(0)
        val hrs = safeMins / 60
        val mins = safeMins % 60
        return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
    }
}
