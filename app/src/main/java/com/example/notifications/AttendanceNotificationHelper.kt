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
import com.example.data.NoticeItem
import com.example.data.ParentDashboardData
import com.example.data.StaffDashboardData

object AttendanceNotificationHelper {

    const val EXTRA_OPEN_NOTICES = "extra_open_notices"

    private const val ATTENDANCE_CHANNEL_ID = "iti_ranni_attendance_alerts_v2"
    private const val ATTENDANCE_CHANNEL_NAME = "Attendance Status Alerts (Loud)"
    private const val ATTENDANCE_CHANNEL_DESC =
        "High-priority loud alerts when today's attendance is confirmed & closed by the Trade Instructor."

    private const val NOTICES_CHANNEL_ID = "iti_ranni_trade_notices_v2"
    private const val NOTICES_CHANNEL_NAME = "College & Trade Notices"
    private const val NOTICES_CHANNEL_DESC =
        "High-priority notices and holiday announcements published by Govt. ITI Ranni."

    private const val MARKS_CHANNEL_ID = "iti_ranni_exam_marks_v2"
    private const val MARKS_CHANNEL_NAME = "Monthly Theory & Lab Marks (Loud)"
    private const val MARKS_CHANNEL_DESC =
        "Loud notification alerts when the Trade Instructor publishes or edits monthly Theory & Lab marks."

    private const val LOUD_URGENT_CHANNEL_ID = "iti_ranni_loud_urgent_alerts_v2"
    private const val LOUD_URGENT_CHANNEL_NAME = "Urgent Running Late & Mark Queries (Loud)"
    private const val LOUD_URGENT_CHANNEL_DESC =
        "Distinct loud alarm-tone alerts for Trainee Running Late notices and private Work / Lab Mark queries."

    private val LOUD_VIBRATION_PATTERN = longArrayOf(0L, 420L, 160L, 420L, 160L, 520L)

    private const val PREFS_NAME = "iti_ranni_notification_state"
    private const val KEY_LAST_ATTENDANCE_SIG = "last_notified_attendance_sig"
    private const val KEY_LAST_SEEN_NOTICE_ID = "last_seen_notice_id"
    private const val KEY_LAST_NOTIFIED_NOTICE_ID = "last_notified_notice_id"
    private const val KEY_LAST_MARKS_SIG = "last_notified_marks_sig"
    private const val KEY_LAST_LATE_ALERT_SIG = "last_notified_running_late_sig"
    private const val KEY_LAST_WORK_QUERY_SIG = "last_notified_work_query_sig"

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
     * Plays a distinct, loud alert tone + haptic vibration with safe multi-tier fallback
     * (custom raw resource -> system alarm tone -> notification tone -> ToneGenerator beep).
     */
    fun playLoudAlertTone(context: Context) {
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
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            if (ringtone.isPlaying) ringtone.stop()
                        } catch (_: Exception) {
                        }
                    }, 2200L)
                } else {
                    playFallbackToneBeep()
                }
            } else {
                playFallbackToneBeep()
            }
        } catch (_: Exception) {
            playFallbackToneBeep()
        }

        triggerUrgentVibration(context)
    }

    private fun playFallbackToneBeep() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 900)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    tg.release()
                } catch (_: Exception) {
                }
            }, 1100L)
        } catch (_: Exception) {
            // Safe no-op on devices without audio hardware
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
            // Ignore vibration failures on devices without vibrator permission/hardware
        }
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val loudSoundUri = resolveLoudAlertSoundUri(context)
            val alarmAttrs = loudAudioAttributes()

            if (manager.getNotificationChannel(ATTENDANCE_CHANNEL_ID) == null) {
                val attendanceChannel = NotificationChannel(
                    ATTENDANCE_CHANNEL_ID,
                    ATTENDANCE_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = ATTENDANCE_CHANNEL_DESC
                    enableVibration(true)
                    vibrationPattern = LOUD_VIBRATION_PATTERN
                    setSound(loudSoundUri, alarmAttrs)
                }
                manager.createNotificationChannel(attendanceChannel)
            }

            if (manager.getNotificationChannel(NOTICES_CHANNEL_ID) == null) {
                val noticesChannel = NotificationChannel(
                    NOTICES_CHANNEL_ID,
                    NOTICES_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = NOTICES_CHANNEL_DESC
                    enableVibration(true)
                }
                manager.createNotificationChannel(noticesChannel)
            }

            if (manager.getNotificationChannel(MARKS_CHANNEL_ID) == null) {
                val marksChannel = NotificationChannel(
                    MARKS_CHANNEL_ID,
                    MARKS_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = MARKS_CHANNEL_DESC
                    enableVibration(true)
                    vibrationPattern = LOUD_VIBRATION_PATTERN
                    setSound(loudSoundUri, alarmAttrs)
                }
                manager.createNotificationChannel(marksChannel)
            }

            if (manager.getNotificationChannel(LOUD_URGENT_CHANNEL_ID) == null) {
                val urgentChannel = NotificationChannel(
                    LOUD_URGENT_CHANNEL_ID,
                    LOUD_URGENT_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = LOUD_URGENT_CHANNEL_DESC
                    enableVibration(true)
                    vibrationPattern = LOUD_VIBRATION_PATTERN
                    setSound(loudSoundUri, alarmAttrs)
                }
                manager.createNotificationChannel(urgentChannel)
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

    /**
     * Triggers an Android status-bar notification ONLY when:
     * 1. Today is NOT a declared holiday.
     * 2. Instructor has confirmed & closed today's attendance (attendanceClosed == true).
     * 3. Ward's final status is ABSENT or HALF-DAY (never PRESENT).
     * 4. Notification has not already been sent for this (roll, date, status).
     */
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

        val loudSound = resolveLoudAlertSoundUri(context)
        val notification = NotificationCompat.Builder(context, ATTENDANCE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSound(loudSound, AudioManager.STREAM_ALARM)
            .setVibrate(LOUD_VIBRATION_PATTERN)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

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
     * Triggers a high-priority status-bar popup ONCE when a brand-new notice arrives.
     * Tapping the notification opens MainActivity directly on the Notices page.
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
        val body = latest.body.ifBlank { "Tap to read the full notice from Govt. ITI Ranni." }

        val notification = NotificationCompat.Builder(context, NOTICES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(latest.id.hashCode(), notification)
            prefs.edit().putString(KEY_LAST_NOTIFIED_NOTICE_ID, latest.id).apply()
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Triggers a loud status-bar notification on the Parent device whenever
     * the Trade Instructor publishes or edits/revises monthly Theory & Lab marks for their ward.
     */
    fun notifyParentMarksUpdatedIfNeeded(
        context: Context,
        data: ParentDashboardData
    ): Boolean {
        val latest = data.monthlyMarks.firstOrNull { it.published } ?: return false
        if (!hasNotificationPermission(context)) return false

        val signature = "${data.roll}|${latest.id}|${latest.monthLabel}|${latest.theoryMarks}/${latest.theoryMax}|${latest.labMarks}/${latest.labMax}|${latest.updatedAt}"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_MARKS_SIG, null) == signature) {
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

        val loudSound = resolveLoudAlertSoundUri(context)
        val notification = NotificationCompat.Builder(context, MARKS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setSound(loudSound, AudioManager.STREAM_ALARM)
            .setVibrate(LOUD_VIBRATION_PATTERN)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

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

    /**
     * Triggers a distinct loud notification + alarm tone for the Trade Instructor when
     * a new Trainee "Running Late" alert or private "Work / Lab Mark" query arrives.
     */
    fun notifyInstructorUrgentAlertsIfNeeded(
        context: Context,
        data: StaffDashboardData
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ensureChannels(context)
        val loudSound = resolveLoudAlertSoundUri(context)
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
                    val notification = NotificationCompat.Builder(context, LOUD_URGENT_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setSound(loudSound, AudioManager.STREAM_ALARM)
                        .setVibrate(LOUD_VIBRATION_PATTERN)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setAutoCancel(true)
                        .setContentIntent(pi)
                        .build()
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
                    val notification = NotificationCompat.Builder(context, LOUD_URGENT_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setSound(loudSound, AudioManager.STREAM_ALARM)
                        .setVibrate(LOUD_VIBRATION_PATTERN)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setAutoCancel(true)
                        .setContentIntent(pi)
                        .build()
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
