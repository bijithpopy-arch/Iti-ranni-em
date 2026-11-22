package com.example.data

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

data class SafeZoneAdvice(
    val isSafe: Boolean,
    val daysCount: Int,
    val headline: String,
    val detail: String,
    val streakBadgeLabel: String
)

/**
 * Single source of truth for accurate cumulative attendance math across the entire app.
 * Prevents the "3500% / Total: 1" division bug and strictly clamps percentages to [0, 100].
 */
object AttendanceMath {

    const val EXAM_THRESHOLD_PCT = 80

    fun safeTotalDays(present: Int, half: Int, rawTotal: Int): Int {
        val safePresent = present.coerceAtLeast(0)
        val safeHalf = half.coerceAtLeast(0)
        val minimumRecorded = safePresent + safeHalf
        return maxOf(rawTotal.coerceAtLeast(0), minimumRecorded)
    }

    /**
     * Computes cumulative percentage accurately:
     *   35 / 46 = 76%
     *   32 / 46 = 70%
     *   42 / 46 = 91%
     *   39 / 46 = 85%
     * Always clamped strictly between 0 and 100.
     */
    fun computePercentage(present: Int, half: Int, rawTotal: Int, rawPct: Int? = null): Int {
        val p = present.coerceAtLeast(0)
        val h = half.coerceAtLeast(0)
        val total = safeTotalDays(p, h, rawTotal)
        if (total <= 0) {
            return (rawPct ?: 0).coerceIn(0, 100)
        }
        val earned = p + (h * 0.5)
        val computed = ((earned / total.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        // Guard against corrupt backend values like 3500% or division by Total: 1
        return computed
    }

    /**
     * Calculates trainee "Safe-Zone" guidance relative to the 80% ITI exam eligibility rule.
     */
    fun calculateSafeZone(present: Int, half: Int, rawTotal: Int): SafeZoneAdvice {
        val p = present.coerceAtLeast(0)
        val h = half.coerceAtLeast(0)
        val total = safeTotalDays(p, h, rawTotal)
        val earned = p + (h * 0.5)
        val pct = computePercentage(p, h, total)

        val streakLabel = when {
            pct >= 92 -> "Gold Consistency (${pct}%)"
            pct >= 85 -> "Silver Streak (${pct}%)"
            pct >= 80 -> "Exam Eligible (${pct}%)"
            pct >= 75 -> "Shop Floor Warning (${pct}%)"
            else -> "Critical Makeup Required (${pct}%)"
        }

        if (total == 0) {
            return SafeZoneAdvice(
                isSafe = true,
                daysCount = 0,
                headline = "No working days recorded yet",
                detail = "Swipe your ESP8266 ID card daily to build your attendance streak.",
                streakBadgeLabel = "New Session"
            )
        }

        return if (pct < EXAM_THRESHOLD_PCT) {
            // Solve (earned + x) / (total + x) >= 0.80  =>  0.20*x >= 0.80*total - earned
            val needed = ceil(((0.80 * total) - earned) / 0.20).toInt().coerceAtLeast(1)
            SafeZoneAdvice(
                isSafe = false,
                daysCount = needed,
                headline = "You need $needed consecutive present day${if (needed == 1) "" else "s"} to reach 80%",
                detail = "Currently $pct% ($p full, $h half out of $total days). Attend Saturday / post-3:00 PM Shop Floor sessions to recover faster.",
                streakBadgeLabel = streakLabel
            )
        } else {
            // Solve earned / (total + x) >= 0.80  =>  x <= (earned / 0.80) - total
            val affordable = floor((earned / 0.80) - total).toInt().coerceAtLeast(0)
            SafeZoneAdvice(
                isSafe = true,
                daysCount = affordable,
                headline = if (affordable > 0) {
                    "You can afford $affordable leave${if (affordable == 1) "" else "s"} and stay above 80%"
                } else {
                    "Right on the 80% line — avoid missing class"
                },
                detail = "Safe Zone active ($pct% across $total working days). Keep swiping on time!",
                streakBadgeLabel = streakLabel
            )
        }
    }
}
