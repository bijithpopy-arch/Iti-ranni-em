package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AttendanceApiClient
import com.example.data.HolidayAnnouncement
import com.example.data.NoticeItem
import com.example.data.ParentDashboardData
import com.example.data.StaffDashboardData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AttendanceNotificationHelper {

    const val EXTRA_OPEN_NOTICES = "extra_open_notices"

    private const val ATTENDANCE_CHANNEL_ID = "iti_ranni_attendance_alerts_v3"
    private const val ATTENDANCE_CHANNEL_NAME = "Attendance Status Alerts (Loud)"
    private const val ATTENDANCE_CHANNEL_DESC =
        "High-priority loud alerts when today's attendance is confirmed & closed by the Trade Instructor."

    private const val NOTICES_CHANNEL_ID = "iti_ranni_trade_notices_v3"
    private const val NOTICES_CHANNEL_NAME = "Instructor Notices & Holidays (Loud)"
    private const val NOTICES_CHANNEL_DESC =
        "Top-banner and lock-screen loud alerts for every notice or holiday sent by your instructor."

    private const val MARKS_CHANNEL_ID = "iti_ranni_exam_marks_v3"
    private const val MARKS_CHANNEL_NAME = "Monthly Theory & Lab Marks (Loud)"
    private const val MARKS_CHANNEL_DESC =
        "Loud notification alerts when the Trade Instructor publishes or edits monthly Theory & Lab marks."

    private const val LOUD_URGENT_CHANNEL_ID = "iti_ranni_loud_urgent_alerts_v3"
    private const val LOUD_URGENT_CHANNEL_NAME = "Urgent Running Late & Mark Queries (Loud)"
    private const val LOUD_URGENT_CHANNEL_DESC =
        "Distinct loud alarm-tone alerts for Trainee Running Late notices and private Work / Lab Mark queries."

    private val LOUD_VIBRATION_PATTERN = longArrayOf(0L, 450L, 150L, 450L, 150L, 550L)

    private const val PREFS_NAME = "iti_ranni_notification_state"
    private const val KEY_LAST_ATTENDANCE_SIG = "last_notified_attendance_sig"
    private const val KEY_LAST_SEEN_NOTICE_ID = "last_seen_notice_id"
    private const val KEY_LAST_NOTIFIED_NOTICE_ID = "last_notified_notice_id"
    private const val KEY_LAST_NOTIFIED_HOLIDAY_SIG = "last_notified_holiday_sig"
    private const val KEY_LAST_MARKS_SIG = "last_notified_marks_sig"
    private const val KEY_LAST_LATE_ALERT_SIG = "last_notified_running_late_sig"
    private const val KEY_LAST_WORK_QUERY_SIG = "last_notified_work_query_sig"

    private val backgroundHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    private fun resolveLoudAlertSoundUri(context: Context): Uri {
        val customResId = context.resources.getIdentifier("iti_loud_alert", "raw", context.packageName)
        if (customResId != 0) {
            return Uri.parse("android.resource://${context.packageName}/$customResId")
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: Uri.EMPTY
    }

    private fun loudAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }

    /**
     * Plays a loud beep/alarm tone + vibration with fallback to ToneGenerator on STREAM_ALARM.
     */
    fun playLoudAlertTone(context: Context) {
        var playedRingtone = false
        try {
            val uri = resolveLoudAlertSoundUri(context)
            if (uri != Uri.EMPTY) {
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
                if (ringtone != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ringtone.audioAttributes = loudAudioAttributes()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ringtone.volume = 1.0f
                    }
                    ringtone.play()
                    playedRingtone = true
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            if (ringtone.isPlaying) ringtone.stop()
                        } catch (_: Exception) {
                        }
                    }, 1800L)
                }
            }
        } catch (_: Exception) {
        }

        if (!playedRingtone) {
            playFallbackToneBeep()
        } else {
            // Crisp loud beep burst alongside alarm tone so lock-screen alerts stand out
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                playFallbackToneBeep()
            }, 250L)
        }

        triggerUrgentVibration(context)
    }

    private fun playFallbackToneBeep() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 850)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    tg.release()
                } catch (_: Exception) {
                }
            }, 1050L)
        } catch (_: Exception) {
        }
    }

    private fun triggerUrgentVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(LOUD_VIBRATION_PATTERN, -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(LOUD_VIBRATION_PATTERN, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(LOUD_VIBRATION_PATTERN, -1)
                }
            }
        } catch (_: Exception) {
        }
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val loudSoundUri = resolveLoudAlertSoundUri(context)
            val alarmAttrs = loudAudioAttributes()

            val channels = listOf(
                Triple(ATTENDANCE_CHANNEL_ID, ATTENDANCE_CHANNEL_NAME, ATTENDANCE_CHANNEL_DESC),
                Triple(NOTICES_CHANNEL_ID, NOTICES_CHANNEL_NAME, NOTICES_CHANNEL_DESC),
                Triple(MARKS_CHANNEL_ID, MARKS_CHANNEL_NAME, MARKS_CHANNEL_DESC),
                Triple(LOUD_URGENT_CHANNEL_ID, LOUD_URGENT_CHANNEL_NAME, LOUD_URGENT_CHANNEL_DESC)
            )

            for ((id, name, desc) in channels) {
                if (manager.getNotificationChannel(id) == null) {
                    manager.createNotificationChannel(
                        NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                            description = desc
                            enableVibration(true)
                            vibrationPattern = LOUD_VIBRATION_PATTERN
                            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                            setSound(loudSoundUri, alarmAttrs)
                        }
                    )
                }
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun buildLoudBannerNotification(
        context: Context,
        channelId: String,
        title: String,
        body: String,
        pendingIntent: PendingIntent
    ) = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setSound(resolveLoudAlertSoundUri(context), AudioManager.STREAM_ALARM)
        .setVibrate(LOUD_VIBRATION_PATTERN)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    fun notifyParentIfConfirmedAndEligible(
        context: Context,
        data: ParentDashboardData
    ): Boolean {
        if (data.todayHoliday != null) return false
        if (!data.attendanceClosed) return false

        val normalizedStatus = normalizeAlertStatus(data.attendance.todayStatus) ?: return false
        if (!hasNotificationPermission(context)) return false

        val dateKey = data.attendanceDate.ifBlank { "today" }
        val signature = "${data.roll}|$dateKey|$normalizedStatus"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_ATTENDANCE_SIG, null) == signature) {
            return false
        }

        ensureChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            data.roll.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Govt. ITI Ranni • Attendance Confirmed"
        val body = if (normalizedStatus == "ABSENT") {
            "Roll #${data.roll} (${data.studentName}) is confirmed ABSENT on $dateKey. Tap to send a leave reason or report bunking."
        } else {
            "Roll #${data.roll} (${data.studentName}) is confirmed HALF-DAY on $dateKey (IN: ${data.attendance.inT}, OUT: ${data.attendance.outT})."
        }

        val notification = buildLoudBannerNotification(
            context = context,
            channelId = ATTENDANCE_CHANNEL_ID,
            title = title,
            body = body,
            pendingIntent = pendingIntent
        )

        val notificationId = (data.roll + "_" + dateKey).hashCode()
        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            prefs.edit().putString(KEY_LAST_ATTENDANCE_SIG, signature).apply()
            playLoudAlertTone(context)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun getLastSeenNoticeId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_SEEN_NOTICE_ID, null)
    }

    fun markNoticesSeen(context: Context, latestNoticeId: String?) {
        if (latestNoticeId.isNullOrBlank()) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_SEEN_NOTICE_ID, latestNoticeId)
            .putString(KEY_LAST_NOTIFIED_NOTICE_ID, latestNoticeId)
            .apply()
    }

    fun countUnreadNotices(context: Context, notices: List<NoticeItem>): Int {
        if (notices.isEmpty()) return 0
        val lastSeenId = getLastSeenNoticeId(context) ?: return notices.size
        val index = notices.indexOfFirst { it.id == lastSeenId }
        return if (index < 0) notices.size else index
    }

    /**
     * Fires a top-banner + lock-screen notification with loud beep for EVERY new instructor notice.
     */
    fun notifyNewNoticeIfNeeded(
        context: Context,
        notices: List<NoticeItem>
    ): Boolean {
        val latest = notices.firstOrNull() ?: return false
        if (latest.id.isBlank()) return false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSeen = prefs.getString(KEY_LAST_SEEN_NOTICE_ID, null)
        val lastNotified = prefs.getString(KEY_LAST_NOTIFIED_NOTICE_ID, null)
        if (latest.id == lastSeen || latest.id == lastNotified) {
            return false
        }

        if (!hasNotificationPermission(context)) return false

        ensureChannels(context)

        val openNoticesIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_NOTICES, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            ("notice_${latest.id}").hashCode(),
            openNoticesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tagPrefix = latest.category.takeIf { it.isNotBlank() }?.let { "[$it] " } ?: ""
        val title = "$tagPrefix${latest.title}"
        val body = latest.body.ifBlank { "Tap to read the full notice from ${latest.postedBy}." }

        val notification = buildLoudBannerNotification(
            context = context,
            channelId = NOTICES_CHANNEL_ID,
            title = title,
            body = body,
            pendingIntent = pendingIntent
        )

        return try {
            NotificationManagerCompat.from(context).notify(latest.id.hashCode(), notification)
            prefs.edit().putString(KEY_LAST_NOTIFIED_NOTICE_ID, latest.id).apply()
            playLoudAlertTone(context)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun notifyHolidayIfNeeded(
        context: Context,
        holiday: HolidayAnnouncement?,
        holidays: List<HolidayAnnouncement> = emptyList()
    ): Boolean {
        val target = holiday ?: holidays.lastOrNull() ?: return false
        val signature = "${target.id}|${target.date}|${target.reason}"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_NOTIFIED_HOLIDAY_SIG, null) == signature) {
            return false
        }
        if (!hasNotificationPermission(context)) return false

        ensureChannels(context)

        val openNoticesIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_NOTICES, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            ("holiday_$signature").hashCode(),
            openNoticesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "[Holiday] Govt. ITI Ranni • ${target.date}"
        val body = "${target.reason} (Posted by ${target.postedBy})."

        val notification = buildLoudBannerNotification(
            context = context,
            channelId = NOTICES_CHANNEL_ID,
            title = title,
            body = body,
            pendingIntent = pendingIntent
        )

        return try {
            NotificationManagerCompat.from(context).notify(signature.hashCode(), notification)
            prefs.edit().putString(KEY_LAST_NOTIFIED_HOLIDAY_SIG, signature).apply()
            playLoudAlertTone(context)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    suspend fun syncNotificationsFromRemote(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${AttendanceApiClient.BASE_URL}?action=project_groups_data")
                .get()
                .build()
            backgroundHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string()?.trim().orEmpty()
                if (!response.isSuccessful || !body.startsWith("{")) return@withContext false
                val json = JSONObject(body)
                val notices = parseSimpleNotices(json.optJSONArray("notices"))
                val holidays = parseSimpleHolidays(json.optJSONArray("holidays"))
                val nFired = notifyNewNoticeIfNeeded(context, notices)
                val hFired = notifyHolidayIfNeeded(context, holidays.lastOrNull(), holidays)
                nFired || hFired
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun parseSimpleNotices(array: JSONArray?): List<NoticeItem> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(
                    NoticeItem(
                        id = obj.optString("id", "NTC-$i"),
                        title = obj.optString("title", "Trade Notice"),
                        date = obj.optString("date", ""),
                        category = obj.optString("category", "General"),
                        body = obj.optString("body", ""),
                        postedBy = obj.optString("postedBy", "Govt. ITI Ranni"),
                        isPinned = obj.optBoolean("isPinned", false)
                    )
                )
            }
        }
    }

    private fun parseSimpleHolidays(array: JSONArray?): List<HolidayAnnouncement> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(
                    HolidayAnnouncement(
                        id = obj.optString("id", "HOL-$i"),
                        date = obj.optString("date", ""),
                        reason = obj.optString("reason", "Holiday"),
                        postedBy = obj.optString("postedBy", "Trade Instructor"),
                        postedAt = obj.optString("postedAt", "")
                    )
                )
            }
        }
    }

    fun notifyParentMarksUpdatedIfNeeded(
        context: Context,
        data: ParentDashboardData
    ): Boolean {
        val latest = data.monthlyMarks.firstOrNull { it.published } ?: return false
        if (!hasNotificationPermission(context)) return false

        val signature = "${data.roll}|${latest.id}|${latest.monthLabel}|${latest.theoryMarks}/${latest.theoryMax}|${latest.labMarks}/${latest.labMax}|${latest.updatedAt}"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_MARKS_SIG, null) == signature)
                {
            return false
        }

        ensureChannels(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            ("marks_${data.roll}_${latest.id}").hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (latest.isRevised) {
            "Govt. ITI Ranni • Monthly Marks Updated (${latest.monthLabel})"
        } else {
            "Govt. ITI Ranni • Monthly Marks Published (${latest.monthLabel})"
        }
        val body = "${data.studentName} (Roll #${data.roll}) — Theory: ${latest.theoryMarks}/${latest.theoryMax}, Lab: ${latest.labMarks}/${latest.labMax} (Total: ${latest.totalObtained}/${latest.totalMax}, ${latest.percentage}%)."

        val notification = buildLoudBannerNotification(
            context = context,
            channelId = MARKS_CHANNEL_ID,
            title = title,
            body = body,
            pendingIntent = pendingIntent
        )

        val notificationId = ("marks_${data.roll}_${latest.monthLabel}").hashCode()
        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            prefs.edit().putString(KEY_LAST_MARKS_SIG, signature).apply()
            playLoudAlertTone(context)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun notifyInstructorUrgentAlertsIfNeeded(
        context: Context,
        data: StaffDashboardData
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ensureChannels(context)
        var fired = false

        val latestLate = data.runningLateAlerts.firstOrNull()
        if (latestLate != null) {
            val lateSig = "${latestLate.id}|${latestLate.roll}|${latestLate.date}|${latestLate.time}|${latestLate.expectedArrival}"
            if (prefs.getString(KEY_LAST_LATE_ALERT_SIG, null) != lateSig) {
                prefs.edit().putString(KEY_LAST_LATE_ALERT_SIG, lateSig).apply()
                if (hasNotificationPermission(context)) {
                    val openAppIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pi = PendingIntent.getActivity(
                        context,
                        ("late_${latestLate.id}").hashCode(),
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val title = "Running Late Alert • Roll #${latestLate.roll} (${latestLate.studentName})"
                    val body = "ETA: ${latestLate.expectedArrival} • Reason: ${latestLate.reasonCategory}" +
                        if (latestLate.details.isNotBlank()) " — ${latestLate.details}" else ""
                    val notification = buildLoudBannerNotification(
                        context = context,
                        channelId = LOUD_URGENT_CHANNEL_ID,
                        title = title,
                        body = body,
                        pendingIntent = pi
                    )
                    try {
                        NotificationManagerCompat.from(context).notify(("late_${latestLate.id}").hashCode(), notification)
                    } catch (_: SecurityException) {
                    }
                }
                fired = true
            }
        }

        val latestOpenQuery = data.workMarkQueries.firstOrNull { it.status.equals("OPEN", ignoreCase = true) }
        if (latestOpenQuery != null) {
            val querySig = "${latestOpenQuery.id}|${latestOpenQuery.roll}|${latestOpenQuery.date}|${latestOpenQuery.workReference}"
            if (prefs.getString(KEY_LAST_WORK_QUERY_SIG, null) != querySig) {
                prefs.edit().putString(KEY_LAST_WORK_QUERY_SIG, querySig).apply()
                if (hasNotificationPermission(context)) {
                    val openAppIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pi = PendingIntent.getActivity(
                        context,
                        ("wmq_${latestOpenQuery.id}").hashCode(),
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val title = "Work / Lab Mark Query • Roll #${latestOpenQuery.roll}"
                    val body = "${latestOpenQuery.studentName}: ${latestOpenQuery.workReference} — ${latestOpenQuery.traineeNote}"
                    val notification = buildLoudBannerNotification(
                        context = context,
                        channelId = LOUD_URGENT_CHANNEL_ID,
                        title = title,
                        body = body,
                        pendingIntent = pi
                    )
                    try {
                        NotificationManagerCompat.from(context).notify(("wmq_${latestOpenQuery.id}").hashCode(), notification)
                    } catch (_: SecurityException) {
                    }
                }
                fired = true
            }
        }

        if (fired) {
            playLoudAlertTone(context)
        }
        return fired
    }

    fun normalizeAlertStatus(rawStatus: String): String? {
        val upper = rawStatus.trim().uppercase()
        return when {
            upper == "ABSENT" || upper == "A" -> "ABSENT"
            upper.contains("HALF") || upper == "HD" -> "HALF-DAY"
            else -> null
        }
    }
}
