package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Deterministic Monday–Friday Infinite Duty Rotation Engine.
 *
 * Rules:
 * - Regular classes and duties (Eggs, Milk, Tool Crib, Safety Marshal, etc.) run
 *   strictly Monday through Friday only — Saturday and Sunday are completely excluded.
 * - Trainee pairs rotate consecutively by Roll Number (1&2, 3&4, ..., 31&32, 33) and
 *   cycle infinitely across weeks using modulo arithmetic over Mon–Fri working days.
 * - On Saturday or Sunday, standard weekday duties pause and the next scheduled pair
 *   is automatically shown for the upcoming Monday.
 */
object DutyRotationEngine {

    const val DEFAULT_TOTAL_ROLLS = 33

    val DEFAULT_DUTY_ITEMS = listOf(
        "Eggs",
        "Milk",
        "Tool Crib",
        "Safety Marshal",
        "Workshop & Bench Setup"
    )

    // Fixed anchor date in IST (01-08-2025 = Friday, Batch 2025-2027 start)
    private const val ANCHOR_YEAR = 2025
    private const val ANCHOR_MONTH_ZERO_BASED = Calendar.AUGUST
    private const val ANCHOR_DAY = 1

    fun buildRollPairs(totalRolls: Int = DEFAULT_TOTAL_ROLLS): List<List<Int>> {
        val count = totalRolls.coerceAtLeast(2)
        val pairs = mutableListOf<List<Int>>()
        var roll = 1
        while (roll <= count) {
            if (roll + 1 <= count) {
                pairs.add(listOf(roll, roll + 1))
            } else {
                pairs.add(listOf(roll))
            }
            roll += 2
        }
        return pairs
    }

    fun isWeekendToday(): Boolean {
        val tz = TimeZone.getTimeZone("Asia/Kolkata")
        val dow = Calendar.getInstance(tz).get(Calendar.DAY_OF_WEEK)
        return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
    }

    /**
     * Exact count of Monday–Friday weekdays between [startCal] (exclusive) and [endCal] (inclusive).
     * Works forward or backward in O(1) time without loops.
     */
    private fun countWeekdaysBetween(startCal: Calendar, endCal: Calendar): Int {
        val millisPerDay = 24L * 60L * 60L * 1000L
        val totalDays = ((endCal.timeInMillis - startCal.timeInMillis) / millisPerDay).toInt()
        if (totalDays == 0) return 0

        val step = if (totalDays > 0) 1 else -1
        val absDays = kotlin.math.abs(totalDays)
        val fullWeeks = absDays / 7
        val remainder = absDays % 7
        var weekdays = fullWeeks * 5

        val probe = startCal.clone() as Calendar
        for (i in 1..remainder) {
            probe.add(Calendar.DAY_OF_MONTH, step)
            val dow = probe.get(Calendar.DAY_OF_WEEK)
            if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) {
                weekdays++
            }
        }
        return weekdays * step
    }

    private fun nextWeekdayCalendar(from: Calendar, includeTodayIfWeekday: Boolean): Calendar {
        val cal = from.clone() as Calendar
        if (!includeTodayIfWeekday) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        while (
            cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
            cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        ) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal
    }

    /**
     * Generates a full cycle of Monday–Friday duty pair slots starting from today
     * (or the upcoming Monday if today is Saturday/Sunday).
     * Never includes Saturday or Sunday in the generated duty rotation.
     */
    fun generateRotationSchedule(
        totalRolls: Int = DEFAULT_TOTAL_ROLLS,
        customDutyItems: List<String> = emptyList()
    ): List<DutyPairSlot> {
        val pairs = buildRollPairs(totalRolls)
        val items = customDutyItems.ifEmpty { DEFAULT_DUTY_ITEMS }
        val tz = TimeZone.getTimeZone("Asia/Kolkata")

        val anchorCal = Calendar.getInstance(tz).apply {
            set(ANCHOR_YEAR, ANCHOR_MONTH_ZERO_BASED, ANCHOR_DAY, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayCal = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayDow = todayCal.get(Calendar.DAY_OF_WEEK)
        val isWeekend = todayDow == Calendar.SATURDAY || todayDow == Calendar.SUNDAY

        // Anchor the first active weekday slot (today if Mon–Fri, else next Monday)
        val firstDutyCal = nextWeekdayCalendar(todayCal, includeTodayIfWeekday = true)
        val weekdaysElapsed = countWeekdaysBetween(anchorCal, firstDutyCal)
        val startPairIndex = ((weekdaysElapsed % pairs.size) + pairs.size) % pairs.size

        val displayFormatter = SimpleDateFormat("EEE, dd MMM", Locale.US).apply {
            timeZone = tz
        }
        val isoFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
            timeZone = tz
        }

        var cursorCal = firstDutyCal.clone() as Calendar
        return List(pairs.size) { offset ->
            if (offset > 0) {
                cursorCal = nextWeekdayCalendar(cursorCal, includeTodayIfWeekday = false)
            }
            val slotIndex = (startPairIndex + offset) % pairs.size
            val formattedDate = displayFormatter.format(cursorCal.time)
            val dateLabel = when {
                offset == 0 && !isWeekend -> "Today ($formattedDate)"
                offset == 0 && isWeekend -> "Next Duty: $formattedDate (Mon–Fri)"
                offset == 1 && !isWeekend -> "Next Working Day ($formattedDate)"
                else -> formattedDate
            }
            DutyPairSlot(
                dayNumber = slotIndex + 1,
                rolls = pairs[slotIndex],
                dateLabel = dateLabel,
                dateKey = isoFormatter.format(cursorCal.time),
                isToday = offset == 0 && !isWeekend,
                dutyItems = items
            )
        }
    }

    fun workingDaysUntilRollTurn(
        rollNumber: Int,
        schedule: List<DutyPairSlot>
    ): Int? {
        val idx = schedule.indexOfFirst { rollNumber in it.rolls }
        return if (idx >= 0) idx else null
    }
}
