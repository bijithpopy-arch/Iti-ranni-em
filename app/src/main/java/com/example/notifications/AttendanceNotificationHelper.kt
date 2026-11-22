package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.NoticeItem
import com.example.data.ParentDashboardData

object AttendanceNotificationHelper {

    const val EXTRA_OPEN_NOTICES = "extra_open_notices"

    private const val ATTENDANCE_CHANNEL_ID = "iti_ranni_attendance_alerts"
    private const val ATTENDANCE_CHANNEL_NAME = "Attendance Status Alerts"
    private const val ATTENDANCE_CHANNEL_DESC =
        "Notifies parents when today's attendance is confirmed & closed by the Trade Instructor and the ward is ABSENT or HALF-DAY."

    private const val NOTICES_CHANNEL_ID = "iti_ranni_trade_notices"
    private const val NOTICES_CHANNEL_NAME = "College & Trade Notices"
    private const val NOTICES_CHANNEL_DESC =
        "High-priority notices and holiday announcements published by Govt. ITI Ranni."

    private const val PREFS_NAME = "iti_ranni_notification_state"
    private const val KEY_LAST_ATTENDANCE_SIG = "last_notified_attendance_sig"
    private const val KEY_LAST_SEEN_NOTICE_ID = "last_seen_notice_id"
    private const val KEY_LAST_NOTIFIED_NOTICE_ID = "last_notified_notice_id"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            if (manager.getNotificationChannel(ATTENDANCE_CHANNEL_ID) == null) {
                val attendanceChannel = NotificationChannel(
                    ATTENDANCE_CHANNEL_ID,
                    ATTENDANCE_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = ATTENDANCE_CHANNEL_DESC
                    enableVibration(true)
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
     * Strict Notification Trigger Rule for Parents:
     * 1. MUST ONLY fire AFTER the Trade Instructor has confirmed & closed today's attendance (`attendanceClosed == true`).
     * 2. MUST SKIP `ABSENT` alerts if today is a declared Holiday (`todayHoliday != null` or `todayStatus == "HOLIDAY"`).
     * 3. MUST ONLY fire when ward's status is ABSENT or HALF-DAY.
     * 4. De-duplicates per (roll, date, normalizedStatus) so manual refreshes don't spam repeated alerts.
     */
    fun notifyParentIfConfirmedAndEligible(
        context: Context,
        data: ParentDashboardData
    ): Boolean {
        if (!data.attendanceClosed) return false

        val normalizedStatus = normalizeAlertStatus(data.attendance.todayStatus) ?: return false

        // Skip false ABSENT alerts on declared holiday dates
        val isHolidayToday = data.todayHoliday != null ||
            data.attendance.todayStatus.trim().equals("HOLIDAY", ignoreCase = true)
        if (isHolidayToday && normalizedStatus == "ABSENT") {
            return false
        }

        if (!hasNotificationPermission(context)) return false

        val effectiveDate = data.attendanceDate.ifBlank { "TODAY" }
        val signature = "${data.roll}|$effectiveDate|$normalizedStatus"
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

        val title = if (normalizedStatus == "ABSENT") {
            "Govt. ITI Ranni • Ward Marked ABSENT"
        } else {
            "Govt. ITI Ranni • Ward Marked HALF-DAY"
        }

        val timeNote = if (data.closedAt.isNotBlank()) " (Confirmed at ${data.closedAt})" else ""
        val body = "${data.studentName} (Roll #${data.roll}) is marked $normalizedStatus for $effectiveDate$timeNote."

        val notification = NotificationCompat.Builder(context, ATTENDANCE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = ("attendance_${data.roll}_$effectiveDate").hashCode()
        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            prefs.edit().putString(KEY_LAST_ATTENDANCE_SIG, signature).apply()
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

    fun normalizeAlertStatus(rawStatus: String): String? {
        val upper = rawStatus.trim().uppercase()
        return when {
            upper == "ABSENT" || upper == "A" -> "ABSENT"
            upper.contains("HALF") || upper == "HD" -> "HALF-DAY"
            else -> null
        }
    }
}
