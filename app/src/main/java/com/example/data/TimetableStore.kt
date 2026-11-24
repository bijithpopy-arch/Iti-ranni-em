package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.TimeZone

/**
 * Local SharedPreferences cache & default EMech weekly schedule provider for the
 * Editable Weekly Timetable (Trainee Panel + Instructor Panel).
 */
object TimetableStore {

    private const val PREFS_NAME = "iti_ranni_em_timetable_prefs"
    private const val KEY_OFFICIAL_TIMETABLE = "official_timetable_json"
    private const val KEY_TRAINEE_EDIT_ENABLED = "trainee_timetable_edit_enabled"
    private const val KEY_CUSTOM_PREFIX = "trainee_custom_timetable_"

    val WEEKDAYS: List<Pair<String, String>> = listOf(
        "MON" to "Mon",
        "TUE" to "Tue",
        "WED" to "Wed",
        "THU" to "Thu",
        "FRI" to "Fri",
        "SAT" to "Sat"
    )

    val SESSION_TYPES: List<String> = listOf(
        "Practical",
        "Lunch",
        "1st Period",
        "2nd Period",
        "Theory",
        "Other"
    )

    /**
     * Pre-populated default Government ITI Ranni Electronics Mechanic weekly timetable:
     * - Mon–Fri:
     *   08:00 AM – 12:30 PM : Lab / Practical
     *   12:30 PM – 01:00 PM : Lunch
     *   01:00 PM – 02:00 PM : 1st Period (Trade Theory)
     *   02:00 PM – 03:00 PM : 2nd Period (WCS / ED / ES)
     * - Sat (Optional):
     *   08:30 AM – 01:00 PM : Optional Shop Floor / Makeup Practical
     */
    fun defaultWeeklyTimetable(): List<TimetablePeriod> {
        val list = mutableListOf<TimetablePeriod>()
        val monToFri = listOf(
            "MON" to ("Trade Theory — Semiconductor & Circuits" to "WCS — Workshop Calculation & Science"),
            "TUE" to ("Trade Theory — Digital Electronics" to "ED — Engineering Drawing"),
            "WED" to ("Trade Theory — Power Supplies & SMPS" to "ES — Employability Skills"),
            "THU" to ("Trade Theory — Microcontrollers & IoT" to "WCS — Electrical & Electronic Math"),
            "FRI" to ("Trade Theory — Communication & Testing" to "ED / ES — Revision & Assessment")
        )
        monToFri.forEach { (day, subjects) ->
            list.add(
                TimetablePeriod(
                    id = "${day}_1",
                    dayOfWeek = day,
                    startTime = "08:00 AM",
                    endTime = "12:30 PM",
                    subject = "Lab / Practical",
                    sessionType = "Practical",
                    roomOrNote = "EM Electronics Workshop & Lab",
                    updatedBy = "Default EMech Schedule"
                )
            )
            list.add(
                TimetablePeriod(
                    id = "${day}_2",
                    dayOfWeek = day,
                    startTime = "12:30 PM",
                    endTime = "01:00 PM",
                    subject = "Lunch",
                    sessionType = "Lunch",
                    roomOrNote = "Midday Break",
                    updatedBy = "Default EMech Schedule"
                )
            )
            list.add(
                TimetablePeriod(
                    id = "${day}_3",
                    dayOfWeek = day,
                    startTime = "01:00 PM",
                    endTime = "02:00 PM",
                    subject = "1st Period — ${subjects.first}",
                    sessionType = "1st Period",
                    roomOrNote = "EM Theory Classroom",
                    updatedBy = "Default EMech Schedule"
                )
            )
            list.add(
                TimetablePeriod(
                    id = "${day}_4",
                    dayOfWeek = day,
                    startTime = "02:00 PM",
                    endTime = "03:00 PM",
                    subject = "2nd Period — ${subjects.second}",
                    sessionType = "2nd Period",
                    roomOrNote = "EM Theory Classroom",
                    updatedBy = "Default EMech Schedule"
                )
            )
        }
        list.add(
            TimetablePeriod(
                id = "SAT_1",
                dayOfWeek = "SAT",
                startTime = "08:30 AM",
                endTime = "01:00 PM",
                subject = "Optional Shop Floor / Makeup Practical",
                sessionType = "Practical",
                roomOrNote = "EM Lab (As Scheduled)",
                updatedBy = "Default EMech Schedule"
            )
        )
        return list
    }

    fun currentIstDayKey(): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "MON"
            Calendar.TUESDAY -> "TUE"
            Calendar.WEDNESDAY -> "WED"
            Calendar.THURSDAY -> "THU"
            Calendar.FRIDAY -> "FRI"
            Calendar.SATURDAY -> "SAT"
            else -> "MON"
        }
    }

    fun dayDisplayLabel(dayKey: String): String {
        return when (dayKey.trim().uppercase()) {
            "MON" -> "Monday"
            "TUE" -> "Tuesday"
            "WED" -> "Wednesday"
            "THU" -> "Thursday"
            "FRI" -> "Friday"
            "SAT" -> "Saturday"
            else -> "Monday"
        }
    }

    /**
     * Parses a time string like "08:00 AM", "12:30 PM", "13:00" into minutes since midnight.
     */
    fun parseTimeToMinutes(raw: String): Int {
        val s = raw.trim().uppercase()
        val match = Regex("^(\\d{1,2}):(\\d{2})(?:\\s*(AM|PM))?$").find(s) ?: return -1
        var hh = match.groupValues[1].toIntOrNull() ?: return -1
        val mm = match.groupValues[2].toIntOrNull() ?: return -1
        val ampm = match.groupValues[3]
        if (mm !in 0..59) return -1
        if (ampm == "PM" && hh < 12) hh += 12
        if (ampm == "AM" && hh == 12) hh = 0
        return hh * 60 + mm
    }

    fun sortPeriodsChronologically(periods: List<TimetablePeriod>): List<TimetablePeriod> {
        return periods.sortedWith(
            compareBy<TimetablePeriod> {
                val mins = parseTimeToMinutes(it.startTime)
                if (mins >= 0) mins else Int.MAX_VALUE
            }.thenBy { it.id }
        )
    }

    data class ActiveSlotStatus(
        val period: TimetablePeriod,
        val isLiveNow: Boolean
    )

    /**
     * Finds the currently running slot or the next upcoming slot for today in IST.
     */
    fun findActiveOrUpcomingSlot(dayPeriods: List<TimetablePeriod>): ActiveSlotStatus? {
        val sorted = sortPeriodsChronologically(dayPeriods)
        if (sorted.isEmpty()) return null
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
        val nowMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        sorted.forEach { slot ->
            val start = parseTimeToMinutes(slot.startTime)
            val end = parseTimeToMinutes(slot.endTime)
            if (start >= 0 && end > start && nowMins in start until end) {
                return ActiveSlotStatus(period = slot, isLiveNow = true)
            }
        }
        val upcoming = sorted.firstOrNull { slot ->
            val start = parseTimeToMinutes(slot.startTime)
            start >= 0 && start > nowMins
        }
        return if (upcoming != null) {
            ActiveSlotStatus(period = upcoming, isLiveNow = false)
        } else {
            ActiveSlotStatus(period = sorted.first(), isLiveNow = false)
        }
    }

    fun saveOfficialCache(
        context: Context,
        periods: List<TimetablePeriod>,
        traineeEditEnabled: Boolean
    ) {
        if (periods.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_OFFICIAL_TIMETABLE, toJsonString(periods))
            .putBoolean(KEY_TRAINEE_EDIT_ENABLED, traineeEditEnabled)
            .apply()
    }

    fun loadOfficialCache(context: Context): List<TimetablePeriod> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_OFFICIAL_TIMETABLE, null)
        val parsed = fromJsonString(raw)
        return parsed.ifEmpty { defaultWeeklyTimetable() }
    }

    fun saveTraineeCustomEdits(context: Context, roll: String, periods: List<TimetablePeriod>) {
        val key = KEY_CUSTOM_PREFIX + roll.trim().ifBlank { "default" }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(key, toJsonString(periods)).apply()
    }

    fun loadTraineeCustomEdits(context: Context, roll: String): List<TimetablePeriod>? {
        val key = KEY_CUSTOM_PREFIX + roll.trim().ifBlank { "default" }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(key, null) ?: return null
        val parsed = fromJsonString(raw)
        return parsed.ifEmpty { null }
    }

    fun clearTraineeCustomEdits(context: Context, roll: String) {
        val key = KEY_CUSTOM_PREFIX + roll.trim().ifBlank { "default" }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(key).apply()
    }

    fun toJsonString(periods: List<TimetablePeriod>): String {
        val arr = JSONArray()
        periods.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("d", p.dayOfWeek.trim().uppercase())
                    put("s", p.startTime.trim())
                    put("e", p.endTime.trim())
                    put("t", p.subject.trim())
                    put("y", p.sessionType.trim())
                    if (p.roomOrNote.isNotBlank()) put("r", p.roomOrNote.trim())
                    if (p.updatedBy.isNotBlank()) put("u", p.updatedBy.trim())
                }
            )
        }
        return arr.toString()
    }

    fun fromJsonString(raw: String?): List<TimetablePeriod> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            parseJsonArray(arr)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseJsonArray(arr: JSONArray?): List<TimetablePeriod> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val day = obj.optString("dayOfWeek", obj.optString("d", "MON")).trim().uppercase()
                val start = obj.optString("startTime", obj.optString("s", "")).trim()
                val end = obj.optString("endTime", obj.optString("e", "")).trim()
                val subject = obj.optString("subject", obj.optString("t", "")).trim()
                if (start.isBlank() && subject.isBlank()) continue
                add(
                    TimetablePeriod(
                        id = obj.optString("id", "${day}_${i + 1}").ifBlank { "${day}_${i + 1}" },
                        dayOfWeek = day.ifBlank { "MON" },
                        startTime = IstDateTimeFormatter.formatTimeOnly(start, start),
                        endTime = IstDateTimeFormatter.formatTimeOnly(end, end),
                        subject = subject.ifBlank { "Class Period" },
                        sessionType = obj.optString("sessionType", obj.optString("y", "Theory"))
                            .ifBlank { "Theory" },
                        roomOrNote = obj.optString("roomOrNote", obj.optString("r", "")).trim(),
                        updatedBy = obj.optString("updatedBy", obj.optString("u", "")).trim()
                    )
                )
            }
        }
    }
}
