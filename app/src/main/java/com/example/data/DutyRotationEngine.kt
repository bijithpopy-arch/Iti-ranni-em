package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Rich daily duty schedule representation for UI and PDF consumption.
 * Note: DutyPairSlot remains canonically declared in Models.kt (never redeclared here).
 */
data class DutyScheduleDay(
    val dateLabel: String,
    val dateKey: String,
    val dayNumber: Int,
    val firstRoll: Int,
    val secondRoll: Int,
    val rolls: List<Int>,
    val dutyItems: List<String>,
    val isToday: Boolean,
    val isWeekendToday: Boolean,
    val isOverridden: Boolean,
    val label: String,
    val dutyNote: String = ""
)

/**
 * Deterministic Monday–Friday Infinite Duty Rotation Engine for the 23-trainee batch (Rolls 1–23).
 *
 * Rules:
 * - Strictly 23 trainees (Rolls 1..23); zero references to Roll 33.
 * - Regular duties (Eggs, Milk, Tool Crib, Safety Marshal, etc.) run Monday–Friday only;
 *   Saturday and Sunday are skipped completely.
 * - Anchored to 24-11-2026 (IST) = Roll #14 & Roll #15, advancing by 2 rolls per working day
 *   and wrapping cleanly from Roll #23 back to Roll #01 (22 & 23 -> 1 & 2).
 * - Supports Instructor live overrides via [DutyOverrideEntry]:
 *     1. Override Today Only (reanchorFromHere = false): swaps only that date's pair.
 *     2. Re-anchor Rotation From Here (reanchorFromHere = true): shifts subsequent working days
 *        forward consecutively from the newly selected pair.
 */
object DutyRotationEngine {

    const val DEFAULT_TOTAL_ROLLS = 23
    const val TOTAL_TRAINEES = 23

    val DEFAULT_DUTY_ITEMS = listOf(
        "Eggs",
        "Milk",
        "Tool Crib",
        "Safety Marshal",
        "Workshop & Bench Setup"
    )

    // Fixed anchor date in IST: 24-11-2026 (Tuesday) = Roll #14 & Roll #15
    private const val ANCHOR_YEAR = 2026
    private const val ANCHOR_MONTH_ZERO_BASED = Calendar.NOVEMBER
    private const val ANCHOR_DAY = 24
    private const val ANCHOR_FIRST_ROLL = 14

    private val istTimeZone: TimeZone
        get() = TimeZone.getTimeZone("Asia/Kolkata")

    fun buildRollPairs(totalRolls: Int = DEFAULT_TOTAL_ROLLS): List<List<Int>> {
        val count = totalRolls.coerceIn(2, TOTAL_TRAINEES)
        val pairs = mutableListOf<List<Int>>()
        var step = 0
        val slots = (count + 1) / 2
        while (step < slots) {
            val first = ((ANCHOR_FIRST_ROLL - 1 + 2 * step) % count) + 1
            val second = (first % count) + 1
            pairs.add(listOf(first, second))
            step++
        }
        return pairs
    }

    fun isWeekendToday(): Boolean {
        val dow = Calendar.getInstance(istTimeZone).get(Calendar.DAY_OF_WEEK)
        return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
    }

    fun todayDutyDateKey(): String {
        val todayCal = normalizedIstCalendar()
        val firstDutyCal = nextWeekdayCalendar(todayCal, includeTodayIfWeekday = true)
        return formatDateKey(firstDutyCal)
    }

    private fun normalizedIstCalendar(): Calendar {
        return Calendar.getInstance(istTimeZone).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun defaultAnchorCalendar(): Calendar {
        return Calendar.getInstance(istTimeZone).apply {
            set(ANCHOR_YEAR, ANCHOR_MONTH_ZERO_BASED, ANCHOR_DAY, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun parseDateKeyToCalendar(dateKey: String): Calendar? {
        val trimmed = dateKey.trim()
        if (trimmed.isBlank()) return null
        return try {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
                timeZone = istTimeZone
                isLenient = false
            }
            val parsed = sdf.parse(trimmed) ?: return null
            Calendar.getInstance(istTimeZone).apply {
                time = parsed
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun formatDateKey(cal: Calendar): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
            timeZone = istTimeZone
        }
        return sdf.format(cal.time)
    }

    /**
     * Exact count of Monday–Friday weekdays between [startCal] (exclusive) and [endCal] (inclusive).
     */
    private fun countWeekdaysBetween(startCal: Calendar, endCal: Calendar): Int {
        val millisPerDay = 24L * 60L * 60L * 1000L
        val diffMillis = endCal.timeInMillis - startCal.timeInMillis
        val totalDays = kotlin.math.round(diffMillis.toDouble() / millisPerDay.toDouble()).toInt()
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

    private fun normalizeRoll(roll: Int, totalRolls: Int): Int {
        val count = totalRolls.coerceIn(2, TOTAL_TRAINEES)
        val zeroBased = ((roll - 1) % count + count) % count
        return zeroBased + 1
    }

    /**
     * Computes the assigned (firstRoll, secondRoll) pair for [targetCal],
     * respecting both single-day overrides and re-anchor overrides in [overridesByDate].
     */
    fun pairForDate(
        targetCal: Calendar,
        totalRolls: Int = DEFAULT_TOTAL_ROLLS,
        overridesByDate: Map<String, DutyOverrideEntry> = emptyMap()
    ): Pair<Int, Int> {
        val count = totalRolls.coerceIn(2, TOTAL_TRAINEES)
        val effectiveCal = nextWeekdayCalendar(targetCal, includeTodayIfWeekday = true)
        val targetKey = formatDateKey(effectiveCal)

        // 1. Direct override on this exact date takes top precedence
        val exactOverride = overridesByDate[targetKey]
        if (exactOverride != null) {
            val r1 = exactOverride.firstRoll.coerceIn(1, count)
            val r2 = exactOverride.secondRoll.coerceIn(1, count)
            return r1 to r2
        }

        // 2. Check for the latest active re-anchor override strictly before targetCal
        var activeReanchorCal: Calendar? = null
        var activeReanchorEntry: DutyOverrideEntry? = null
        overridesByDate.values
            .filter { it.reanchorFromHere }
            .forEach { entry ->
                val ovCal = parseDateKeyToCalendar(entry.dateKey) ?: return@forEach
                if (ovCal.timeInMillis <= effectiveCal.timeInMillis) {
                    if (activeReanchorCal == null || ovCal.timeInMillis > activeReanchorCal!!.timeInMillis) {
                        activeReanchorCal = ovCal
                        activeReanchorEntry = entry
                    }
                }
            }

        if (activeReanchorCal != null && activeReanchorEntry != null) {
            val steps = countWeekdaysBetween(activeReanchorCal!!, effectiveCal)
            if (steps == 0) {
                return activeReanchorEntry!!.firstRoll.coerceIn(1, count) to
                    activeReanchorEntry!!.secondRoll.coerceIn(1, count)
            }
            // Resume consecutively forward from secondRoll + 1 on the next working day
            val nextStartZero = activeReanchorEntry!!.secondRoll.coerceIn(1, count) % count
            val firstZero = ((nextStartZero + 2 * (steps - 1)) % count + count) % count
            val firstRoll = firstZero + 1
            val secondRoll = (firstRoll % count) + 1
            return firstRoll to secondRoll
        }

        // 3. Default deterministic anchor: 24-11-2026 (IST) = Roll #14 & Roll #15
        val anchorCal = defaultAnchorCalendar()
        val weekdaysElapsed = countWeekdaysBetween(anchorCal, effectiveCal)
        val firstZero = (((ANCHOR_FIRST_ROLL - 1) + 2 * weekdaysElapsed) % count + count) % count
        val firstRoll = firstZero + 1
        val secondRoll = (firstRoll % count) + 1
        return normalizeRoll(firstRoll, count) to normalizeRoll(secondRoll, count)
    }

    /**
     * Returns full structured [DutyScheduleDay] details for a given calendar date.
     */
    fun dayScheduleForDate(
        targetCal: Calendar = normalizedIstCalendar(),
        totalRolls: Int = DEFAULT_TOTAL_ROLLS,
        customDutyItems: List<String> = emptyList(),
        overridesByDate: Map<String, DutyOverrideEntry> = emptyMap()
    ): DutyScheduleDay {
        val count = totalRolls.coerceIn(2, TOTAL_TRAINEES)
        val todayCal = normalizedIstCalendar()
        val isWeekend = isWeekendToday()
        val effectiveCal = nextWeekdayCalendar(targetCal, includeTodayIfWeekday = true)
        val dateKey = formatDateKey(effectiveCal)
        val exactOverride = overridesByDate[dateKey]
        val (firstRoll, secondRoll) = pairForDate(effectiveCal, count, overridesByDate)
        val rolls = if (firstRoll == secondRoll) listOf(firstRoll) else listOf(firstRoll, secondRoll)

        val displayFormatter = SimpleDateFormat("EEE, dd MMM", Locale.US).apply {
            timeZone = istTimeZone
        }
        val formattedDate = displayFormatter.format(effectiveCal.time)
        val isTargetToday = !isWeekend && formatDateKey(todayCal) == dateKey
        val dateLabel = when {
            isTargetToday -> "Today ($formattedDate)"
            isWeekend && formatDateKey(nextWeekdayCalendar(todayCal, true)) == dateKey ->
                "Next Duty: $formattedDate (Mon–Fri)"
            else -> formattedDate
        }

        val anchorCal = defaultAnchorCalendar()
        val cycleLen = (count + 1) / 2
        val elapsed = countWeekdaysBetween(anchorCal, effectiveCal)
        val dayNum = (((elapsed % cycleLen) + cycleLen) % cycleLen) + 1

        val baseItems = customDutyItems.ifEmpty { DEFAULT_DUTY_ITEMS }
        val note = exactOverride?.dutyNote?.trim().orEmpty()
        val resolvedItems = if (note.isNotBlank()) {
            val parts = note.split(",", "•", "/").map { it.trim() }.filter { it.isNotBlank() }
            if (parts.isNotEmpty()) parts else baseItems
        } else {
            baseItems
        }

        val label = rolls.joinToString(" & ") { "Roll #${it.toString().padStart(2, '0')}" }

        return DutyScheduleDay(
            dateLabel = dateLabel,
            dateKey = dateKey,
            dayNumber = dayNum,
            firstRoll = firstRoll,
            secondRoll = secondRoll,
            rolls = rolls,
            dutyItems = resolvedItems,
            isToday = isTargetToday,
            isWeekendToday = isWeekend,
            isOverridden = exactOverride != null,
            label = label,
            dutyNote = note
        )
    }

    /**
     * Generates a full Monday–Friday cycle of duty pair slots starting from today
     * (or the upcoming Monday if today is Saturday/Sunday), honoring Instructor overrides.
     * With 23 trainees and 2 rolls/day, 12 working days covers all 23 rolls cleanly.
     */
    fun generateRotationSchedule(
        totalRolls: Int = DEFAULT_TOTAL_ROLLS,
        customDutyItems: List<String> = emptyList(),
        overridesByDate: Map<String, DutyOverrideEntry> = emptyMap(),
        daysCount: Int = 12
    ): List<DutyPairSlot> {
        val count = totalRolls.coerceIn(2, TOTAL_TRAINEES)
        val slotsToGenerate = daysCount.coerceAtLeast((count + 1) / 2)
        val todayCal = normalizedIstCalendar()
        val isWeekend = isWeekendToday()
        val firstDutyCal = nextWeekdayCalendar(todayCal, includeTodayIfWeekday = true)

        val displayFormatter = SimpleDateFormat("EEE, dd MMM", Locale.US).apply {
            timeZone = istTimeZone
        }

        var cursorCal = firstDutyCal.clone() as Calendar
        return List(slotsToGenerate) { offset ->
            if (offset > 0) {
                cursorCal = nextWeekdayCalendar(cursorCal, includeTodayIfWeekday = false)
            }
            val day = dayScheduleForDate(
                targetCal = cursorCal,
                totalRolls = count,
                customDutyItems = customDutyItems,
                overridesByDate = overridesByDate
            )
            val formattedDate = displayFormatter.format(cursorCal.time)
            val dateLabel = when {
                offset == 0 && !isWeekend -> "Today ($formattedDate)"
                offset == 0 && isWeekend -> "Next Duty: $formattedDate (Mon–Fri)"
                offset == 1 && !isWeekend -> "Next Working Day ($formattedDate)"
                else -> formattedDate
            }
            DutyPairSlot(
                dayNumber = day.dayNumber,
                rolls = day.rolls,
                dateLabel = dateLabel,
                dateKey = day.dateKey,
                isToday = offset == 0 && !isWeekend,
                dutyItems = day.dutyItems,
                isOverridden = day.isOverridden,
                dutyNote = day.dutyNote
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
