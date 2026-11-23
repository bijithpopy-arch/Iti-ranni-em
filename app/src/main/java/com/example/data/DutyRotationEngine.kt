package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

data class DutyScheduleDay(
    val dateLabel: String,
    val firstRoll: Int,
    val secondRoll: Int,
    val isToday: Boolean,
    val isWeekendToday: Boolean,
    val label: String
)

object DutyRotationEngine {

    const val TOTAL_TRAINEES = 23
    const val BASE_DATE_DD_MM_YYYY = "24-11-2026"
    const val BASE_START_ROLL = 14
    private const val MS_PER_DAY = 86_400_000.0

    private fun istCalendar(dateDdMmYyyy: String? = null): Calendar {
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        val cal = Calendar.getInstance(tz, Locale.US)
        if (!dateDdMmYyyy.isNullOrBlank()) {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
                timeZone = tz
                isLenient = false
            }
            sdf.parse(dateDdMmYyyy.trim())?.let { cal.time = it }
        }
        return cal
    }

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
        return (((targetDate.time - baseDate.time) / MS_PER_DAY).roundToInt()).coerceAtLeast(0)
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
        val n1 = nameByRoll[pair.first.toString()]?.takeIf { it.isNotBlank() }
        val n2 = nameByRoll[pair.second.toString()]?.takeIf { it.isNotBlank() }
        val left = if (n1 != null) "Roll #${pair.first} ($n1)" else "Roll #${pair.first}"
        val right = if (n2 != null) "Roll #${pair.second} ($n2)" else "Roll #${pair.second}"
        return "$left & $right"
    }

    fun dutyItemsForDate(
        dateDdMmYyyy: String = AttendanceApiClient.todayIstDateString(),
        nameByRoll: Map<String, String> = emptyMap()
    ): List<String> {
        val pair = pairForDate(dateDdMmYyyy)
        return listOf("Egg & Milk Duty: ${formatPairLabel(pair, nameByRoll)}")
    }

    fun dayScheduleForDate(
        dateDdMmYyyy: String,
        nameByRoll: Map<String, String> = emptyMap()
    ): DutyScheduleDay {
        val cleanDate = dateDdMmYyyy.trim()
        val todayStr = AttendanceApiClient.todayIstDateString()
        val cal = istCalendar(cleanDate)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val isSunday = dayOfWeek == Calendar.SUNDAY
        val pair = pairForDate(cleanDate)
        return DutyScheduleDay(
            dateLabel = cleanDate,
            firstRoll = pair.first,
            secondRoll = pair.second,
            isToday = cleanDate.equals(todayStr, ignoreCase = true),
            isWeekendToday = isSunday,
            label = formatPairLabel(pair, nameByRoll)
        )
    }

    fun generateRotationSchedule(
        daysCount: Int = 14,
        startDateDdMmYyyy: String = AttendanceApiClient.todayIstDateString(),
        nameByRoll: Map<String, String> = emptyMap()
    ): List<DutyScheduleDay> {
        val count = daysCount.coerceAtLeast(1)
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
            timeZone = tz
            isLenient = false
        }
        val baseCal = istCalendar(startDateDdMmYyyy)
        return buildList {
            for (i in 0 until count) {
                val itemCal = baseCal.clone() as Calendar
                itemCal.add(Calendar.DAY_OF_YEAR, i)
                val labelDate = sdf.format(itemCal.time)
                add(dayScheduleForDate(labelDate, nameByRoll))
            }
        }
    }
}
