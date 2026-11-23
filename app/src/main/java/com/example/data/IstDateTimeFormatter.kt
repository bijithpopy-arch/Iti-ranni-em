package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * Centralized IST (Asia/Kolkata) date and time sanitizer & formatter.
 * Fixes Google Sheets serialized time strings such as "1899-12-30T03:45:12.000Z"
 * and ISO-8601 UTC timestamps so raw "1899" or "T...Z" strings never appear
 * anywhere in the UI or exported PDFs.
 */
object IstDateTimeFormatter {

    private val istZone: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    private val utcZone: TimeZone = TimeZone.getTimeZone("UTC")

    private val isoMillisRegex = Regex(
        """^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?(?:\.(\d+))?(Z|[+-]\d{2}:?\d{2})?$""",
        RegexOption.IGNORE_CASE
    )
    private val time24Regex = Regex("""^(\d{1,2}):(\d{2})(?::(\d{2}))?$""")
    private val time12Regex = Regex("""^(\d{1,2}):(\d{2})(?::(\d{2}))?\s*(AM|PM)$""", RegexOption.IGNORE_CASE)
    private val dateYmdRegex = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")
    private val dateDmyRegex = Regex("""^(\d{1,2})[-/](\d{1,2})[-/](\d{4})$""")

    fun nowIstDateString(): String = formatWithIst(Date(), "dd-MM-yyyy")

    fun nowIstTimeString(): String = formatWithIst(Date(), "hh:mm a")

    fun nowIstDateTimeString(): String = formatWithIst(Date(), "dd-MM-yyyy hh:mm a")

    /**
     * Strictly formats IN/OUT times in IST as "hh:mm a" (e.g. "09:15 AM").
     * Safely parses Google Sheets 1899-12-30T...Z strings, ISO-8601 timestamps,
     * 24-hour times, and decimal fractional day values.
     */
    fun formatTimeOnly(raw: String?, emptyFallback: String = "-"): String {
        val cleaned = raw?.trim().orEmpty()
        if (cleaned.isEmpty() || cleaned == "-" || cleaned == "--" || cleaned.equals("null", ignoreCase = true)) {
            return emptyFallback
        }

        // 1. ISO-8601 timestamp (including Google Sheets 1899-12-30T03:45:12.000Z)
        parseIsoDate(cleaned)?.let { parsedDate ->
            return formatWithIst(parsedDate, "hh:mm a")
        }

        // 2. Already 12-hour format like "9:15 am" or "09:15:00 PM"
        time12Regex.matchEntire(cleaned)?.let { m ->
            val hour = m.groupValues[1].toIntOrNull()?.coerceIn(1, 12) ?: return emptyFallback
            val minute = m.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: return emptyFallback
            val amPm = m.groupValues[4].uppercase(Locale.US)
            return String.format(Locale.US, "%02d:%02d %s", hour, minute, amPm)
        }

        // 3. 24-hour format like "09:15" or "15:05:30"
        time24Regex.matchEntire(cleaned)?.let { m ->
            val h24 = m.groupValues[1].toIntOrNull() ?: return emptyFallback
            val min = m.groupValues[2].toIntOrNull() ?: return emptyFallback
            if (h24 in 0..23 && min in 0..59) {
                val cal = Calendar.getInstance(istZone).apply {
                    set(Calendar.HOUR_OF_DAY, h24)
                    set(Calendar.MINUTE, min)
                    set(Calendar.SECOND, 0)
                }
                return formatWithIst(cal.time, "hh:mm a")
            }
        }

        // 4. Google Sheets fractional day serial (e.g. 0.3854166 => ~09:15 AM)
        cleaned.toDoubleOrNull()?.let { serial ->
            if (serial in 0.0..1.0) {
                val totalMinutes = (serial * 24.0 * 60.0).roundToInt().coerceIn(0, 1439)
                val cal = Calendar.getInstance(istZone).apply {
                    set(Calendar.HOUR_OF_DAY, totalMinutes / 60)
                    set(Calendar.MINUTE, totalMinutes % 60)
                    set(Calendar.SECOND, 0)
                }
                return formatWithIst(cal.time, "hh:mm a")
            }
        }

        // 5. Try Java Date.toString() format from Apps Script (e.g. "Sat Dec 30 1899 09:15:00 GMT+0530 ...")
        parseJsDateString(cleaned)?.let { parsedDate ->
            return formatWithIst(parsedDate, "hh:mm a")
        }

        // 6. Final safety guard: never leak raw 1899 or T...Z strings
        if (containsRawEpochOrIsoArtifact(cleaned)) {
            extractEmbeddedTimeFallback(cleaned)?.let { return it }
            return emptyFallback
        }

        return cleaned
    }

    /**
     * Strictly formats calendar dates in IST as "dd-MM-yyyy".
     */
    fun formatDateOnly(raw: String?, emptyFallback: String = "-"): String {
        val cleaned = raw?.trim().orEmpty()
        if (cleaned.isEmpty() || cleaned == "-" || cleaned == "--" || cleaned.equals("null", ignoreCase = true)) {
            return emptyFallback
        }

        // Reject 1899 epoch sentinel as a calendar date
        if (cleaned.startsWith("1899-") || cleaned.contains(" 1899 ")) {
            return emptyFallback
        }

        parseIsoDate(cleaned)?.let { parsedDate ->
            if (isEpochSentinelDate(parsedDate)) return emptyFallback
            return formatWithIst(parsedDate, "dd-MM-yyyy")
        }

        dateDmyRegex.matchEntire(cleaned)?.let { m ->
            val d = m.groupValues[1].toIntOrNull() ?: return emptyFallback
            val mo = m.groupValues[2].toIntOrNull() ?: return emptyFallback
            val y = m.groupValues[3].toIntOrNull() ?: return emptyFallback
            if (y <= 1900) return emptyFallback
            return String.format(Locale.US, "%02d-%02d-%04d", d.coerceIn(1, 31), mo.coerceIn(1, 12), y)
        }

        dateYmdRegex.matchEntire(cleaned)?.let { m ->
            val y = m.groupValues[1].toIntOrNull() ?: return emptyFallback
            val mo = m.groupValues[2].toIntOrNull() ?: return emptyFallback
            val d = m.groupValues[3].toIntOrNull() ?: return emptyFallback
            if (y <= 1900) return emptyFallback
            return String.format(Locale.US, "%02d-%02d-%04d", d.coerceIn(1, 31), mo.coerceIn(1, 12), y)
        }

        parseJsDateString(cleaned)?.let { parsedDate ->
            if (isEpochSentinelDate(parsedDate)) return emptyFallback
            return formatWithIst(parsedDate, "dd-MM-yyyy")
        }

        if (containsRawEpochOrIsoArtifact(cleaned)) {
            return emptyFallback
        }

        return cleaned
    }

    /**
     * Strictly formats full log / notice / submission timestamps in IST as "dd-MM-yyyy hh:mm a".
     * If the raw input is an 1899 time-only sentinel, falls back to "hh:mm a".
     */
    fun formatDateTime(raw: String?, emptyFallback: String = "-"): String {
        val cleaned = raw?.trim().orEmpty()
        if (cleaned.isEmpty() || cleaned == "-" || cleaned == "--" || cleaned.equals("null", ignoreCase = true)) {
            return emptyFallback
        }

        parseIsoDate(cleaned)?.let { parsedDate ->
            return if (isEpochSentinelDate(parsedDate)) {
                formatWithIst(parsedDate, "hh:mm a")
            } else {
                formatWithIst(parsedDate, "dd-MM-yyyy hh:mm a")
            }
        }

        parseJsDateString(cleaned)?.let { parsedDate ->
            return if (isEpochSentinelDate(parsedDate)) {
                formatWithIst(parsedDate, "hh:mm a")
            } else {
                formatWithIst(parsedDate, "dd-MM-yyyy hh:mm a")
            }
        }

        // Handle "dd-MM-yyyy hh:mm a" or "dd-MM-yyyy 1899-12-30T..."
        val parts = cleaned.split(" ", limit = 2)
        if (parts.size == 2) {
            val dPart = parts[0].trim()
            val tPart = parts[1].trim()
            if (dPart.startsWith("1899-") || tPart.contains("1899-") || tPart.contains("T")) {
                val safeDate = formatDateOnly(dPart, "")
                val safeTime = formatTimeOnly(tPart, "")
                val joined = listOf(safeDate, safeTime).filter { it.isNotBlank() }.joinToString(" ")
                if (joined.isNotBlank()) return joined
            }
        }

        // Date-only string like yyyy-MM-dd
        dateYmdRegex.matchEntire(cleaned)?.let {
            return formatDateOnly(cleaned, emptyFallback)
        }

        // Time-only string
        if (time24Regex.matches(cleaned) || time12Regex.matches(cleaned)) {
            return formatTimeOnly(cleaned, emptyFallback)
        }

        if (containsRawEpochOrIsoArtifact(cleaned)) {
            extractEmbeddedTimeFallback(cleaned)?.let { return it }
            return emptyFallback
        }

        return cleaned
    }

    /**
     * Combines separate date and time fields safely into "dd-MM-yyyy hh:mm a" (or single non-blank part).
     */
    fun combineDateAndTime(dateRaw: String?, timeRaw: String?, emptyFallback: String = "-"): String {
        val safeDate = formatDateOnly(dateRaw, "")
        val safeTime = formatTimeOnly(timeRaw, "")
        return when {
            safeDate.isNotBlank() && safeTime.isNotBlank() -> "$safeDate $safeTime"
            safeDate.isNotBlank() -> safeDate
            safeTime.isNotBlank() -> safeTime
            else -> emptyFallback
        }
    }

    private fun parseIsoDate(raw: String): Date? {
        val match = isoMillisRegex.matchEntire(raw) ?: return null
        val year = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val day = match.groupValues[3].toIntOrNull() ?: return null
        val hour = match.groupValues[4].toIntOrNull() ?: return null
        val minute = match.groupValues[5].toIntOrNull() ?: return null
        val second = match.groupValues[6].toIntOrNull() ?: 0
        val tzToken = match.groupValues[8]

        val tz = when {
            tzToken.isEmpty() -> istZone
            tzToken.equals("Z", ignoreCase = true) -> utcZone
            else -> {
                val normalized = if (tzToken.contains(":")) "GMT$tzToken"
                else "GMT${tzToken.substring(0, 3)}:${tzToken.substring(3)}"
                TimeZone.getTimeZone(normalized)
            }
        }

        val cal = Calendar.getInstance(tz).apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 31))
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, second.coerceIn(0, 59))
            set(Calendar.MILLISECOND, 0)
        }
        return cal.time
    }

    private fun parseJsDateString(raw: String): Date? {
        if (!raw.contains("GMT", ignoreCase = true) && !raw.contains("IST", ignoreCase = true)) {
            return null
        }
        val patterns = listOf(
            "EEE MMM dd yyyy HH:mm:ss 'GMT'Z",
            "EEE MMM dd yyyy HH:mm:ss z",
            "MMM dd yyyy HH:mm:ss 'GMT'Z"
        )
        // Strip parenthetical timezone name e.g. " (India Standard Time)"
        val stripped = raw.substringBefore(" (").trim()
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = true
                }
                val parsed = sdf.parse(stripped)
                if (parsed != null) return parsed
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return null
    }

    private fun isEpochSentinelDate(date: Date): Boolean {
        val cal = Calendar.getInstance(istZone).apply { time = date }
        return cal.get(Calendar.YEAR) <= 1900
    }

    private fun containsRawEpochOrIsoArtifact(text: String): Boolean {
        return text.contains("1899") ||
            (text.contains("T") && text.endsWith("Z", ignoreCase = true)) ||
            text.contains(".000Z", ignoreCase = true)
    }

    private fun extractEmbeddedTimeFallback(text: String): String? {
        val match = Regex("""(\d{2}):(\d{2})(?::\d{2})?""").find(text) ?: return null
        val h = match.groupValues[1].toIntOrNull() ?: return null
        val m = match.groupValues[2].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        val cal = Calendar.getInstance(istZone).apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
        }
        return formatWithIst(cal.time, "hh:mm a")
    }

    private fun formatWithIst(date: Date, pattern: String): String {
        val sdf = SimpleDateFormat(pattern, Locale.US)
        sdf.timeZone = istZone
        return sdf.format(date)
    }
}
