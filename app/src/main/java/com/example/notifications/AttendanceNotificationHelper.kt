package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.NoticeItem
import com.example.data.ParentDashboardData
import com.example.data.StaffDashboardData
import com.example.data.TraineeDashboardData
import com.example.data.UserRole

object AttendanceNotificationHelper {
    private const val PREFS_NAME = "iti_ranni_notifications_prefs"
    private const val KEY_LAST_SEEN_NOTICE_ID = "last_seen_notice_id"
    private const val KEY_NOTIFIED_NOTICE_ID = "last_notified_notice_id"
    private const val KEY_SELECTED_SOUND_URI = "selected_sound_uri"
    private const val KEY_LAST_MARKS_SIG = "last_marks_sig"
    private const val KEY_LAST_PARENT_ATT_DATE = "last_parent_att_date"
    private const val KEY_LAST_URGENT_KEY = "last_urgent_key"

    const val CHANNEL_NOTICES = "iti_notices_v2"
    const val CHANNEL_ALERTS = "iti_alerts_v2"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun defaultCustomSoundUri(): Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    fun getSelectedSoundUri(context: Context): Uri {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_SOUND_URI, null)
        return if (!raw.isNullOrBlank()) Uri.parse(raw) else defaultCustomSoundUri()
    }

    fun getSelectedRingtoneTitle(context: Context): String {
        val uri = getSelectedSoundUri(context)
        return try {
            RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "System Default"
        } catch (_: Exception) {
            "System Default"
        }
    }

    fun setSelectedSoundUri(context: Context, uri: Uri?) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_SOUND_URI, uri?.toString().orEmpty())
            .apply()
        recreateChannelsWithNewSound(context)
    }

    fun recreateChannelsWithNewSound(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.deleteNotificationChannel(CHANNEL_NOTICES)
            nm.deleteNotificationChannel(CHANNEL_ALERTS)
        }
        ensureChannels(context)
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = getSelectedSoundUri(context)
        val audioAttr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        if (nm.getNotificationChannel(CHANNEL_NOTICES) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_NOTICES, "Campus Notices & Broadcasts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "College notices, exam schedules, holidays & marks updates"
                    enableVibration(true)
                    setSound(soundUri, audioAttr)
                }
            )
        }
        if (nm.getNotificationChannel(CHANNEL_ALERTS) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ALERTS, "Attendance & Urgent Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Absentee alerts, mark review queries & running late notifications"
                    enableVibration(true)
                    setSound(soundUri, audioAttr)
                }
            )
        }
    }

    fun playLoudAlertTone(context: Context) {
        try {
            val uri = getSelectedSoundUri(context)
            RingtoneManager.getRingtone(context, uri)?.play()
        } catch (_: Exception) {
        }
    }

    fun getLastSeenNoticeId(context: Context): String? =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_SEEN_NOTICE_ID, null)

    fun countUnreadNotices(context: Context, notices: List<NoticeItem>): Int {
        if (notices.isEmpty()) return 0
        val lastSeen = getLastSeenNoticeId(context) ?: return notices.size
        val idx = notices.indexOfFirst { it.id == lastSeen }
        return if (idx < 0) notices.size else idx
    }

    fun markNoticesSeen(context: Context, latestNoticeId: String?) {
        if (latestNoticeId.isNullOrBlank()) return
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_SEEN_NOTICE_ID, latestNoticeId)
            .putString(KEY_NOTIFIED_NOTICE_ID, latestNoticeId)
            .apply()
    }

    fun notifyNewNoticeIfNeeded(context: Context, notices: List<NoticeItem>) {
        val newest = notices.firstOrNull() ?: return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_NOTIFIED_NOTICE_ID, null) == newest.id) return
        prefs.edit().putString(KEY_NOTIFIED_NOTICE_ID, newest.id).apply()
        showAlert(
            context = context,
            channelId = CHANNEL_NOTICES,
            notifId = newest.id.hashCode(),
            title = if (newest.isPinned) "Pinned Broadcast: ${newest.title}" else "New Notice: ${newest.title}",
            body = newest.body.ifBlank { "Tap to read on Govt. ITI Ranni portal." }
        )
    }

    fun evaluateTraineePublishedUpdates(context: Context, data: TraineeDashboardData, seedBaselineOnly: Boolean) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val newestNotice = data.notices.firstOrNull()
        val marksSig = data.monthlyMarks.joinToString("|") { "${it.id}:${it.theoryMarks}:${it.labMarks}:${it.signedAt}" }
        if (seedBaselineOnly) {
            prefs.edit().apply {
                if (newestNotice != null && getLastSeenNoticeId(context) == null) {
                    putString(KEY_NOTIFIED_NOTICE_ID, newestNotice.id)
                }
                putString(KEY_LAST_MARKS_SIG, marksSig)
            }.apply()
            return
        }
        notifyNewNoticeIfNeeded(context, data.notices)
        val prevMarksSig = prefs.getString(KEY_LAST_MARKS_SIG, "")
        if (marksSig.isNotBlank() && marksSig != prevMarksSig) {
            prefs.edit().putString(KEY_LAST_MARKS_SIG, marksSig).apply()
            val latest = data.monthlyMarks.firstOrNull()
            showAlert(
                context = context,
                channelId = CHANNEL_NOTICES,
                notifId = 4102,
                title = "Exam Marks Published (${latest?.monthLabel ?: "Monthly Exam"})",
                body = if (latest != null) "Theory ${latest.theoryMarks}/${latest.theoryMax}, Practical ${latest.labMarks}/${latest.labMax}" else "Updated marks available."
            )
        }
    }

    fun evaluateParentPublishedUpdates(context: Context, data: ParentDashboardData, seedBaselineOnly: Boolean) {
        if (seedBaselineOnly) return
        notifyParentIfConfirmedAndEligible(context, data)
        notifyParentMarksUpdatedIfNeeded(context, data)
    }

    fun notifyParentIfConfirmedAndEligible(context: Context, data: ParentDashboardData): Boolean {
        if (!data.attendanceClosed || data.todayHoliday != null) return false
        val status = data.attendance.todayStatus.uppercase()
        if (status != "ABSENT" && !status.contains("HALF")) return false
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "${data.attendanceDate}:$status"
        if (prefs.getString(KEY_LAST_PARENT_ATT_DATE, null) == key) return false
        prefs.edit().putString(KEY_LAST_PARENT_ATT_DATE, key).apply()
        showAlert(
            context = context,
            channelId = CHANNEL_ALERTS,
            notifId = 5101,
            title = "Attendance Alert: ${data.studentName} ($status)",
            body = "Daily attendance closed at ${data.closedAt.ifBlank { "campus" }}. Status today: $status."
        )
        return true
    }

    fun notifyParentMarksUpdatedIfNeeded(context: Context, data: ParentDashboardData): Boolean {
        val sig = data.monthlyMarks.joinToString("|") { "${it.id}:${it.theoryMarks}:${it.labMarks}:${it.signedAt}" }
        if (sig.isBlank()) return false
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prev = prefs.getString("${KEY_LAST_MARKS_SIG}_parent", "")
        if (sig == prev) return false
        prefs.edit().putString("${KEY_LAST_MARKS_SIG}_parent", sig).apply()
        val latest = data.monthlyMarks.firstOrNull()
        showAlert(
            context = context,
            channelId = CHANNEL_NOTICES,
            notifId = 5102,
            title = "Ward Exam Marks Published (${latest?.monthLabel ?: "Results"})",
            body = if (latest != null) "${data.studentName}: Theory ${latest.theoryMarks}/${latest.theoryMax}, Lab ${latest.labMarks}/${latest.labMax}" else "New marks published."
        )
        return true
    }

    fun evaluateStaffPublishedUpdates(context: Context, data: StaffDashboardData, activeRole: UserRole, seedBaselineOnly: Boolean) {
        if (seedBaselineOnly) return
        notifyInstructorUrgentAlertsIfNeeded(context, data)
    }

    fun notifyInstructorUrgentAlertsIfNeeded(context: Context, data: StaffDashboardData) {
        val latestLate = data.runningLateAlerts.firstOrNull()
        val latestQuery = data.workMarkQueries.firstOrNull { it.status == "OPEN" }
        val key = "${latestLate?.id.orEmpty()}:${latestQuery?.id.orEmpty()}"
        if (key == ":") return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST_URGENT_KEY, null) == key) return
        prefs.edit().putString(KEY_LAST_URGENT_KEY, key).apply()
        if (latestLate != null) {
            showAlert(
                context = context,
                channelId = CHANNEL_ALERTS,
                notifId = 6101,
                title = "Running Late: Roll #${latestLate.roll} ${latestLate.studentName}",
                body = "Expected ${latestLate.expectedArrival} (${latestLate.reasonCategory})"
            )
        }
    }

    fun showAlert(context: Context, channelId: String, notifId: Int, title: String, body: String) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notifId, notif)
        } catch (_: SecurityException) {
        }
    }
}
