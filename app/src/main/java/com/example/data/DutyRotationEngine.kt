package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Deterministic daily rotating Duty Chart engine.
 * Pairs trainees consecutively by Roll Number:
 *   Day 1: Roll 1 & 2
 *   Day 2: Roll 3 & 4
 *   ...
 *   Day 16: Roll 31 & 32
 *   Day 17: Roll 33 (or Roll 33 & 34 if 34 trainees)
 * Wraps cleanly back to Roll 1 & 2 on the following day.
 */
object DutyRotationEngine {

    const val DEFAULT_TOTAL_ROLLS = 33

    val DEFAULT_DUTY_ITEMS = listOf(
        "Milk",
        "Eggs",
        "Tea / Refreshments",
        "Workshop & Bench Setup"
    )

    // Fixed anchor date in IST (01-08-2025 = Batch 2025-2027 start) so rotation is consistent on every device
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
                // Clean wrap for odd last roll number
                pairs.add(listOf(roll))
            }
            roll += 2
        }
        return pairs
    }

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

        val millisPerDay = 24L * 60L * 60L * 1000L
        val daysElapsed = ((todayCal.timeInMillis - anchorCal.timeInMillis) / millisPerDay).toInt()
        val todayPairIndex = ((daysElapsed % pairs.size) + pairs.size) % pairs.size

        val displayFormatter = SimpleDateFormat("EEE, dd MMM", Locale.US).apply {
            timeZone = tz
        }

        return List(pairs.size) { offset ->
            val slotIndex = (todayPairIndex + offset) % pairs.size
            val slotCal = (todayCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, offset)
            }
            val dateLabel = when (offset) {
                0 -> "Today (${displayFormatter.format(slotCal.time)})"
                1 -> "Tomorrow (${displayFormatter.format(slotCal.time)})"
                else -> displayFormatter.format(slotCal.time)
            }
            DutyPairSlot(
                dayNumber = slotIndex + 1,
                rolls = pairs[slotIndex],
                dateLabel = dateLabel,
                isToday = offset == 0,
                dutyItems = items
            )
        }
    }

    fun daysUntilRollTurn(
        rollNumber: Int,
        schedule: List<DutyPairSlot>
    ): Int? {
        val idx = schedule.indexOfFirst { rollNumber in it.rolls }
        return if (idx >= 0) idx else null
    }
}
