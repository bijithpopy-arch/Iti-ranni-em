package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

data class DutyScheduleDay(
    val dateLabel: String,
    val dateKey: String = dateLabel,
    val dayNumber: Int = 1,
    val firstRoll: Int,
    val secondRoll: Int,
    val rolls: List<Int> = listOf(firstRoll, secondRoll),
    val dutyItems: List<String> = emptyList(),
    val isToday: Boolean,
    val isWeekendToday: Boolean,
    val label: String
)

object DutyRotationEngine {

    const val DEFAULT_TOTAL_ROLLS = 23
    const val TOTAL_TRAINEES = 23
    const val BASE_DATE_DD_MM_YYYY = "24-11-2026"
    const val BASE_START_ROLL = 14
    private const val MS_PER_DAY = 86_400_000.0

    private val istTimeZone: TimeZone
        get() = TimeZone.getTimeZone("Asia/Kolkata")

    private fun dateFormatter(): SimpleDateFormat {
        return SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
            timeZone = istTimeZone
            isLenient = false
        }
    }

    private fun istCalendar(dateDdMmYyyy: String? = null): Calendar {
        val cal = Calendar.getInstance(istTimeZone, Locale.US)
        if (!dateDdMmYyyy.isNullOrBlank()) {
            dateFormatter().parse(dateDdMmYyyy.trim())?.let { cal.time = it }
        }
        return cal
    }

    fun isWeekendToday(): Boolean {
        val day = Calendar.getInstance(istTimeZone, Locale.US).get(Calendar.DAY_OF_WEEK)
        return day == Calendar.SUNDAY || day == Calendar.SATURDAY
    }

    fun wrapRoll(rollNumber: Int, totalRolls: Int = DEFAULT_TOTAL_ROLLS): Int {
        val size = totalRolls.coerceAtLeast(1)
        val mod = ((rollNumber - 1) % size + size) % size
        return mod + 1
    }

    fun dayOffsetFromBase(dateDdMmYyyy: String): Int {
        val sdf = dateFormatter()
        val baseDate = sdf.parse(BASE_DATE_DD_MM_YYYY) ?: return 0
        val targetDate = sdf.parse(dateDdMmYyyy.trim()) ?: return 0
        return (((targetDate.time - baseDate.time) / MS_PER_DAY).roundToInt()).coerceAtLeast(0)
    }

    fun pairForDate(
        dateDdMmYyyy: String = AttendanceApiClient.todayIstDateString(),
        totalRolls: Int = DEFAULT_TOTAL_ROLLS
    ): Pair<Int, Int> {
        val offset = dayOffsetFromBase(dateDdMmYyyy)
        val first = wrapRoll(BASE_START_ROLL + (offset * 2), totalRolls)
        val second = wrapRoll(BASE_START_ROLL + (offset * 2) + 1, totalRolls)
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
        totalRolls: Int = DEFAULT_TOTAL_ROLLS,
        nameByRoll: Map<String, String> = emptyMap(),
        customDutyItems: List<String> = emptyList(),
        dayNumber: Int = 1
    ): DutyScheduleDay {
        val cleanDate = dateDdMmYyyy.trim()
        val todayStr = AttendanceApiClient.todayIstDateString()
        val cal = istCalendar(cleanDate)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val isWeekend = dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY
        val pair = pairForDate(cleanDate, totalRolls)
        val pairLabel = formatPairLabel(pair, nameByRoll)
        val matchingCustom = customDutyItems.filter { it.contains(cleanDate, ignoreCase = true) }
        val effectiveItems = if (matchingCustom.isNotEmpty()) {
            matchingCustom
        } else {
            listOf("Egg & Milk Duty: $pairLabel")
        }
        val label = matchingCustom.firstOrNull() ?: pairLabel
        return DutyScheduleDay(
            dateLabel = cleanDate,
            dateKey = cleanDate,
            dayNumber = dayNumber,
            firstRoll = pair.first,
            secondRoll = pair.second,
            rolls = listOf(pair.first, pair.second),
            dutyItems = effectiveItems,
            isToday = cleanDate.equals(todayStr, ignoreCase = true),
            isWeekendToday = isWeekend,
            label = label
        )
    }

    fun generateRotationSchedule(
        totalRolls: Int = DEFAULT_TOTAL_ROLLS,
        customDutyItems: List<String> = emptyList(),
        daysCount: Int = 14,
        startDateDdMmYyyy: String = AttendanceApiClient.todayIstDateString(),
        nameByRoll: Map<String, String> = emptyMap()
    ): List<DutyScheduleDay> {
        val count = daysCount.coerceAtLeast(1)
        val sdf = dateFormatter()
        val baseCal = istCalendar(startDateDdMmYyyy)
        return buildList {
            for (i in 0 until count) {
                val itemCal = baseCal.clone() as Calendar
                itemCal.add(Calendar.DAY_OF_YEAR, i)
                val labelDate = sdf.format(itemCal.time)
                add(
                    dayScheduleForDate(
                        dateDdMmYyyy = labelDate,
                        totalRolls = totalRolls,
                        nameByRoll = nameByRoll,
                        customDutyItems = customDutyItems,
                        dayNumber = i + 1
                    )
                )
            }
        }
    }

    fun workingDaysUntilRollTurn(
        rollNumber: Int,
        schedule: List<DutyScheduleDay>
    ): Int? {
        if (rollNumber <= 0 || schedule.isEmpty()) return null
        val index = schedule.indexOfFirst { day ->
            !day.isWeekendToday && (day.firstRoll == rollNumber || day.secondRoll == rollNumber || rollNumber in day.rolls)
        }
        return if (index >= 0) index else null
    }
}
