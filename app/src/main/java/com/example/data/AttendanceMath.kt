package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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

object IstDateTimeFormatter {
    private val istTz = TimeZone.getTimeZone("Asia/Kolkata")

    fun formatDateOnly(raw: String, fallback: String = "-"): String {
        val clean = raw.trim()
        if (clean.isBlank() || clean == "-") return fallback
        if (Regex("\\d{2}-\\d{2}-\\d{4}").matches(clean)) return clean
        return try {
            val iso = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = istTz }
            val parsed = iso.parse(clean.take(10))
            if (parsed != null) {
                SimpleDateFormat("dd-MM-yyyy", Locale.US).apply { timeZone = istTz }.format(parsed)
            } else clean
        } catch (_: Exception) {
            clean
        }
    }

    fun formatTimeOnly(raw: String, fallback: String = "-"): String {
        val clean = raw.trim()
        if (clean.isBlank() || clean == "-") return fallback
        if (clean.contains("AM", ignoreCase = true) || clean.contains("PM", ignoreCase = true)) return clean
        return try {
            if (Regex("\\d{2}:\\d{2}.*").matches(clean)) {
                val parser = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = istTz }
                val dt = parser.parse(clean.take(5))
                if (dt != null) SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = istTz }.format(dt)
                else clean
            } else clean
        } catch (_: Exception) {
            clean
        }
    }

    fun formatDateTime(raw: String, fallback: String = "-"): String {
        val clean = raw.trim()
        if (clean.isBlank() || clean == "-") return fallback
        return clean
    }
}

data class DailyDutyAssignment(
    val dateKey: String,
    val firstRoll: Int,
    val secondRoll: Int,
    val note: String = "",
    val isOverride: Boolean = false
) {
    val pairLabel: String
        get() = "Roll #${firstRoll.toString().padStart(2, '0')} & Roll #${secondRoll.toString().padStart(2, '0')}"
}

object DutyRotationEngine {
    const val TOTAL_TRAINEES = 23

    fun todayDutyDateKey(): String {
        return SimpleDateFormat("dd-MM-yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }.format(Date())
    }

    fun assignmentForDate(
        dateKey: String = todayDutyDateKey(),
        overrides: Map<String, DutyOverrideEntry> = emptyMap(),
        holidays: List<HolidayAnnouncement> = emptyList()
    ): DailyDutyAssignment {
        overrides[dateKey]?.let { ov ->
            return DailyDutyAssignment(
                dateKey = dateKey,
                firstRoll = ov.firstRoll.coerceIn(1, TOTAL_TRAINEES),
                secondRoll = ov.secondRoll.coerceIn(1, TOTAL_TRAINEES),
                note = ov.dutyNote,
                isOverride = true
            )
        }
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val dayIndex = (cal.get(Calendar.DAY_OF_YEAR) - 1).coerceAtLeast(0)
        val first = ((dayIndex * 2) % TOTAL_TRAINEES) + 1
        val second = ((dayIndex * 2 + 1) % TOTAL_TRAINEES) + 1
        val isHoliday = holidays.any { it.date.equals(dateKey, ignoreCase = true) }
        return DailyDutyAssignment(
            dateKey = dateKey,
            firstRoll = first,
            secondRoll = second,
            note = if (isHoliday) "Holiday - No workshop cleaning duty" else "Workshop Bench & Tool Crib Duty",
            isOverride = false
        )
    }
}

object TimetableStore {
    private const val PREFS_NAME = "iti_ranni_timetable_prefs"
    private const val KEY_OFFICIAL = "official_tt"
    private const val KEY_TRAINEE_EDIT = "trainee_tt_edit"

    fun defaultWeeklyTimetable(): List<TimetablePeriod> = listOf(
        TimetablePeriod("Monday", "09:00 AM - 10:30 AM", "Trade Theory (Semiconductors & Amplifiers)", "Classroom 1"),
        TimetablePeriod("Monday", "10:45 AM - 01:00 PM", "Trade Practical (PCB Soldering & Testing)", "EM Lab"),
        TimetablePeriod("Monday", "02:00 PM - 04:30 PM", "Workshop Calculation & Science", "EM Lab"),
        TimetablePeriod("Tuesday", "09:00 AM - 12:30 PM", "Power Electronics Practical (MOSFET/SCR)", "EM Lab"),
        TimetablePeriod("Tuesday", "01:30 PM - 04:30 PM", "Engineering Drawing & CAD Schematics", "Drawing Hall"),
        TimetablePeriod("Wednesday", "09:00 AM - 11:00 AM", "Digital Electronics & Logic Gates", "Classroom 1"),
        TimetablePeriod("Wednesday", "11:15 AM - 04:30 PM", "Microcontroller & Sensor Lab (ESP/Arduino)", "EM Lab"),
        TimetablePeriod("Thursday", "09:00 AM - 12:30 PM", "SMPS, UPS & Inverter Troubleshooting", "EM Lab"),
        TimetablePeriod("Thursday", "01:30 PM - 04:30 PM", "Employability Skills & Soft Skills", "Classroom 1"),
        TimetablePeriod("Friday", "09:00 AM - 01:00 PM", "Oscilloscope & Signal Generator Lab", "EM Lab"),
        TimetablePeriod("Friday", "02:00 PM - 04:30 PM", "Project Work & Trade Test Revision", "EM Lab")
    )

    fun parseJsonArray(array: JSONArray?): List<TimetablePeriod> {
        if (array == null || array.length() == 0) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val day = obj.optString("day", "").trim()
                val slot = obj.optString("slot", "").trim()
                val subject = obj.optString("subject", "").trim()
                val room = obj.optString("roomOrLab", "EM Lab").trim()
                if (day.isNotBlank() && subject.isNotBlank()) {
                    add(TimetablePeriod(day, slot.ifBlank { "Session" }, subject, room))
                }
            }
        }
    }

    fun toJsonString(periods: List<TimetablePeriod>): String {
        val arr = JSONArray()
        periods.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("day", p.day)
                    put("slot", p.slot)
                    put("subject", p.subject)
                    put("roomOrLab", p.roomOrLab)
                }
            )
        }
        return arr.toString()
    }

    fun saveOfficialCache(context: Context, periods: List<TimetablePeriod>, allowEdit: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_OFFICIAL, toJsonString(periods))
            .putBoolean(KEY_TRAINEE_EDIT, allowEdit)
            .apply()
    }

    fun loadOfficialCache(context: Context): List<TimetablePeriod> {
        val raw = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_OFFICIAL, null) ?: return defaultWeeklyTimetable()
        return try {
            parseJsonArray(JSONArray(raw)).ifEmpty { defaultWeeklyTimetable() }
        } catch (_: Exception) {
            defaultWeeklyTimetable()
        }
    }

    fun saveTraineeCustomEdits(context: Context, roll: String, periods: List<TimetablePeriod>) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString("custom_tt_$roll", toJsonString(periods))
            .apply()
    }

    fun loadTraineeCustomEdits(context: Context, roll: String): List<TimetablePeriod>? {
        val raw = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("custom_tt_$roll", null) ?: return null
        return try {
            parseJsonArray(JSONArray(raw)).ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    fun clearTraineeCustomEdits(context: Context, roll: String) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove("custom_tt_$roll")
            .apply()
    }
}
