package com.example.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

object DutyRotationEngine {

    const val TOTAL_TRAINEES = 23
    const val BASE_DATE_DD_MM_YYYY = "24-11-2026"
    const val BASE_START_ROLL = 14
    private const val MS_PER_DAY = 86_400_000.0

    fun wrapRoll(rollNumber: Int): Int {
        val mod = ((rollNumber - 1) % TOTAL_TRAINEES + TOTAL_TRAINEES) % TOTAL_TRAINEES
        return mod + 1
    }

    fun dayOffsetFromBase(dateDdMmYyyy: String): Int {
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
            timeZone = tz
            isLenient = false
        }
        val baseDate = sdf.parse(BASE_DATE_DD_MM_YYYY) ?: return 0
        val targetDate = sdf.parse(dateDdMmYyyy.trim()) ?: return 0
        val diffDays = ((targetDate.time - baseDate.time) / MS_PER_DAY).roundToInt()
        return diffDays.coerceAtLeast(0)
    }

    fun pairForDate(dateDdMmYyyy: String = AttendanceApiClient.todayIstDateString()): Pair<Int, Int> {
        val offset = dayOffsetFromBase(dateDdMmYyyy)
        val first = wrapRoll(BASE_START_ROLL + (offset * 2))
        val second = wrapRoll(BASE_START_ROLL + (offset * 2) + 1)
        return first to second
    }

    fun formatPairLabel(
        pair: Pair<Int, Int>,
        nameByRoll: Map<String, String> = emptyMap()
    ): String {
        val firstName = nameByRoll[pair.first.toString()]?.takeIf { it.isNotBlank() }
        val secondName = nameByRoll[pair.second.toString()]?.takeIf { it.isNotBlank() }
        val left = if (firstName != null) "Roll #${pair.first} ($firstName)" else "Roll #${pair.first}"
        val right = if (secondName != null) "Roll #${pair.second} ($secondName)" else "Roll #${pair.second}"
        return "$left & $right"
    }

    fun dutyItemsForDate(
        dateDdMmYyyy: String = AttendanceApiClient.todayIstDateString(),
        nameByRoll: Map<String, String> = emptyMap()
    ): List<String> {
        val pair = pairForDate(dateDdMmYyyy)
        return listOf("Egg & Milk Duty: ${formatPairLabel(pair, nameByRoll)}")
    }
}
