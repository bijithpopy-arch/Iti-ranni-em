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
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.ParentAlertTypes
import com.example.data.ParentDashboardData
import com.example.data.StaffDashboardData
import com.example.data.TraineeDashboardData
import com.example.data.UserRole

object AttendanceNotificationHelper {
    const val CHANNEL_ID_ALERTS_V2 = "emech_urgent_alerts_v2"
    const val CHANNEL_ID_SYNC_V2 = "emech_sync_alerts_v2"

    private const val PREFS_RINGTONE = "emech_ringtone_prefs"
    private const val KEY_CUSTOM_URI = "custom_ringtone_uri"

    private const val PREFS_STATE = "emech_notification_state_prefs"
    private const val KEY_LAST_SEEN_NOTICE_ID = "last_seen_notice_id"
    private const val KEY_LAST_NOTIFIED_NOTICE_ID = "last_notified_notice_id"
    private const val KEY_TRAINEE_SIG = "trainee_eval_sig"
    private const val KEY_PARENT_SIG = "parent_eval_sig"
    private const val KEY_PARENT_ATTENDANCE_SENT = "parent_attendance_sent"
    private const val KEY_PARENT_MARKS_SIG = "parent_marks_sig"
    private const val KEY_STAFF_SIG = "staff_eval_sig"
    private const val KEY_INSTRUCTOR_ALERT_SIG = "instructor_urgent_sig"

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

    fun defaultCustomSoundUri(context: Context): Uri =
        Uri.parse("android.resource://${context.packageName}/${R.raw.custom_alert}")

    fun getSelectedSoundUri(context: Context): Uri {
        val saved = context.getSharedPreferences(PREFS_RINGTONE, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_URI, null)
        return if (!saved.isNullOrBlank()) Uri.parse(saved) else defaultCustomSoundUri(context)
    }

    fun getSelectedRingtoneTitle(context: Context): String {
        val uri = getSelectedSoundUri(context)
        if (uri.scheme == "android.resource") return "Built-in Custom Alert (custom_alert.mp3)"
        return try {
            RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Custom Phone Ringtone"
        } catch (_: Exception) {
            "Custom Phone Ringtone"
        }
    }

    fun setSelectedSoundUri(context: Context, uri: Uri?) {
        context.getSharedPreferences(PREFS_RINGTONE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_URI, uri?.toString())
            .apply()
        recreateChannelsWithNewSound(context)
    }

    fun recreateChannelsWithNewSound(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.deleteNotificationChannel(CHANNEL_ID_ALERTS_V2)
        manager.deleteNotificationChannel(CHANNEL_ID_SYNC_V2)
        ensureChannels(context)
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = getSelectedSoundUri(context)

        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val alertChannel = NotificationChannel(
            CHANNEL_ID_ALERTS_V2,
            "ITI Ranni EM Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Attendance, exam marks, and urgent alerts"
            setSound(soundUri, audioAttrs)
            enableVibration(true)
        }

        val syncChannel = NotificationChannel(
            CHANNEL_ID_SYNC_V2,
            "Offline Sync & Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Background queue sync and app update notifications"
            setSound(soundUri, audioAttrs)
        }

        manager.createNotificationChannel(alertChannel)
        manager.createNotificationChannel(syncChannel)
    }

    fun showAlert(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt()
    ) {
        ensureChannels(context)
        if (!hasNotificationPermission(context)) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = getSelectedSoundUri(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS_V2)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            manager.notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked at runtime
        }
    }

    // ---------- Notice Read/Unread Helpers ----------

    private fun extractStringProp(obj: Any?, getterName: String): String? {
        if (obj == null) return null
        return runCatching {
            obj.javaClass.getMethod(getterName).invoke(obj)?.toString()
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun extractNoticeId(notice: Any?): String? =
        extractStringProp(notice, "getId") ?: notice?.toString()?.takeIf { it.isNotBlank() }

    fun getLastSeenNoticeId(context: Context): String? =
        context.applicationContext
            .getSharedPreferences(PREFS_STATE, Context.MODE_PRIVATE)
            .getString(KEY_LAST_SEEN_NOTICE_ID, null)

    fun countUnreadNotices(context: Context, notices: List<Any>): Int {
        if (notices.isEmpty()) return 0
        val lastSeen = getLastSeenNoticeId(context) ?: return notices.size
        val idx = notices.indexOfFirst { extractNoticeId(it) == lastSeen }
        return if (idx < 0) notices.size else idx
    }

    fun markNoticesSeen(context: Context, latestId: String?) {
        if (latestId.isNullOrBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_STATE, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LAST_SEEN_NOTICE_ID, latestId)
            .putString(KEY_LAST_NOTIFIED_NOTICE_ID, latestId)
            .apply()
    }

    fun notifyNewNoticeIfNeeded(
        context: Context,
        notices: List<Any>,
        pinnedBroadcast: Any? = null
    ) {
        val target = pinnedBroadcast ?: notices.firstOrNull() ?: return
        val targetId = extractNoticeId(target) ?: return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_STATE, Context.MODE_PRIVATE)
        val lastSeen = prefs.getString(KEY_LAST_SEEN_NOTICE_ID, null)
        val lastNotified = prefs.getString(KEY_LAST_NOTIFIED_NOTICE_ID, null)
        if (targetId == lastSeen || targetId == lastNotified) return

        val title = extractStringProp(target, "getTitle") ?: "New College Notice"
        val body = extractStringProp(target, "getBody")
            ?: extractStringProp(target, "getMessage")
            ?: "Open Govt. ITI Ranni (EM) to read the latest announcement."

        prefs.edit().putString(KEY_LAST_NOTIFIED_NOTICE_ID, targetId).apply()
        showAlert(context, title = title, message = body, notificationId = 2001)
    }

    // ---------- Published Update Evaluators ----------

    fun evaluateTraineePublishedUpdates(
        context: Context,
        data: TraineeDashboardData,
        seedBaselineOnly: Boolean = false
    ) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_STATE, Context.MODE_PRIVATE)
        val latestNoticeId = data.pinnedBroadcast?.id ?: data.notices.firstOrNull()?.id.orEmpty()
        val holidayDate = data.todayHoliday?.date.orEmpty()
        val leavesSig = data.leaves.joinToString("|") { "${it.id}:${it.status}" }
        val sig = "${data.roll}|${data.attendanceDate}|${data.attendanceClosed}|${data.attendance.todayStatus}|$latestNoticeId|$holidayDate|$leavesSig"

        val prevSig = prefs.getString(KEY_TRAINEE_SIG, null)
        prefs.edit().putString(KEY_TRAINEE_SIG, sig).apply()

        if (seedBaselineOnly || prevSig == null || prevSig == sig) return

        val latestLeave = data.leaves.firstOrNull()
        val msg = when {
            latestNoticeId.isNotBlank() && !prevSig.contains(latestNoticeId) ->
                "A new notice or broadcast was published for Roll #${data.roll}."
            data.todayHoliday != null ->
                "Holiday update posted for ${data.todayHoliday.date}."
            latestLeave != null ->
                "Leave/Out-Pass ${latestLeave.id} status: ${latestLeave.status}."
            else ->
                "Attendance updated for ${data.attendanceDate}: ${data.attendance.todayStatus}."
        }
        showAlert(context, title = "ITI Ranni EM • Update", message = msg, notificationId = 2101)
    }

    fun evaluateParentPublishedUpdates(
        context: Context,
        data: ParentDashboardData,
        seedBaselineOnly: Boolean = false
    ) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_STATE, Context.MODE_PRIVATE)
        val latestNoticeId = data.pinnedBroadcast?.id ?: data.notices.firstOrNull()?.id.orEmpty()
        val sig = "${data.roll}|${data.attendanceDate}|${data.attendanceClosed}|${data.attendance.todayStatus}|$latestNoticeId|${data.monthlyMarks.size}|${data.parentAlerts.size}"

        val prevSig = prefs.getString(KEY_PARENT_SIG, null)
        prefs.edit().putString(KEY_PARENT_SIG, sig).apply()

        if (seedBaselineOnly) {
            val marksSig = "${data.roll}|${data.monthlyMarks.size}|${data.parentAlerts.count { it.alertType.contains("MARKS", true) }}"
            prefs.edit().putString(KEY_PARENT_MARKS_SIG, marksSig).apply()
            return
        }

        notifyParentIfConfirmedAndEligible(context, data)
        notifyParentMarksUpdatedIfNeeded(context, data)

        if (prevSig != null && prevSig != sig && latestNoticeId.isNotBlank() && !prevSig.contains(latestNoticeId)) {
            showAlert(
                context,
                title = "College Notice • Govt. ITI Ranni",
                message = "New notice or broadcast published for ${data.studentName} (Roll #${data.roll}).",
                notificationId = 2201
            )
        }
    }

    fun notifyParentIfConfirmedAndEligible(context: Context, data: ParentDashboardData): Boolean {
        if (!hasNotificationPermission(context)) return false
        if (data.todayHoliday != null || !data.attendanceClosed) return false

        val statusUpper = data.attendance.todayStatus.trim().uppercase()
        val isAlertStatus = statusUpper == "ABSENT" || statusUpper.contains("HALF") || statusUpper == "LATE"
        if (!isAlertStatus) return false

        val sentKey = "${data.roll}|${data.attendanceDate}|$statusUpper"
        val prefs = context.applicationContext.getSharedPreferences(PREFS_STATE, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_PARENT_ATTENDANCE_SENT, null) == sentKey) return false

        prefs.edit().putString(KEY_PARENT_ATTENDANCE_SENT, sentKey).apply()
        showAlert(
            context,
            title = "Attendance Alert: ${data.studentName}",
            message = "Confirmed register status for ${data.attendanceDate}: $statusUpper (Roll #${data.roll}).",
            notificationId = 2202
        )
        return true
    }

    fun notifyParentMarksUpdatedIfNeeded(context: Context, data: ParentDashboardData): Boolean {
        if (!hasNotificationPermission(context)) return false
        val marksAlertsCount = data.parentAlerts.count {
            it.alertType.equals(ParentAlertTypes.MARKS_PUBLISHED, true) ||
                it.alertType.equals(ParentAlertTypes.MARKS_UPDATED, true)
        }
        val sig = "${data.roll}|${data.monthlyMarks.size}|$marksAlertsCount"
        val prefs = context.applicationContext.getSharedPreferences(PREFS_STATE, Context.MODE_PRIVATE)
        val prev = prefs.getString(KEY_PARENT_MARKS_SIG, null)
        if (prev == sig) return false

        prefs.edit().putString(KEY_PARENT_MARKS_SIG, sig).apply()
        if (prev == null && marksAlertsCount == 0) return false
        if (data.monthlyMarks.isEmpty() && marksAlertsCount == 0) return false

        showAlert(
            context,
            title = "Monthly Exam Marks Updated",
            message = "New monthly theory/lab marks are available for ${data.studentName} (Roll #${data.roll}).",
            notificationId = 2203
        )
        return true
    }

    fun evaluateStaffPublishedUpdates(
        context: Context,
        data: StaffDashboardData,
        activeRole: UserRole,
        seedBaselineOnly: Boolean = false
    ) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_STATE, Context.MODE_PRIVATE)
        val sig = "${activeRole.name}|${data.pendingLeaves.size}|${data.parentAlerts.size}|${data.runningLateAlerts.size}|${data.workMarkQueries.size}"
        val prev = prefs.getString(KEY_STAFF_SIG, null)
        prefs.edit().putString(KEY_STAFF_SIG, sig).apply()

        if (seedBaselineOnly || prev == null || prev == sig) return

        val summary = buildString {
            if (data.pendingLeaves.isNotEmpty()) append("Pending leaves: ${data.pendingLeaves.size}. ")
            if (data.runningLateAlerts.isNotEmpty()) append("Running late alerts: ${data.runningLateAlerts.size}. ")
            if (data.workMarkQueries.isNotEmpty()) append("Mark queries: ${data.workMarkQueries.size}.")
        }.ifBlank { "New staff dashboard update available." }

        showAlert(
            context,
            title = "${activeRole.displayName} • New Updates",
            message = summary.trim(),
            notificationId = 2301
        )
    }

    fun notifyInstructorUrgentAlertsIfNeeded(context: Context, data: StaffDashboardData) {
        if (!hasNotificationPermission(context)) return
        val urgentAlerts = data.parentAlerts.filter {
            val t = it.alertType.uppercase()
            t == ParentAlertTypes.BUNK_ALERT ||
                t == ParentAlertTypes.LEAVE_REASON ||
                t == ParentAlertTypes.TRAINEE_RUNNING_LATE
        }
        val latestUrgentId = urgentAlerts.firstOrNull()?.id.orEmpty()
        val sig = "$latestUrgentId|${data.runningLateAlerts.size}|${data.workMarkQueries.size}"
        val prefs = context.applicationContext.getSharedPreferences(PREFS_STATE, Context.MODE_PRIVATE)
        val prev = prefs.getString(KEY_INSTRUCTOR_ALERT_SIG, null)
        if (prev == sig) return

        prefs.edit().putString(KEY_INSTRUCTOR_ALERT_SIG, sig).apply()
        if (urgentAlerts.isEmpty() && data.runningLateAlerts.isEmpty()) return

        val latest = urgentAlerts.firstOrNull()
        val msg = if (latest != null) {
            "Roll #${latest.roll} ${latest.studentName}: ${latest.message}"
        } else {
            "${data.runningLateAlerts.size} running-late / parent alert(s) received."
        }
        showAlert(
            context,
            title = "Urgent Instructor Alert",
            message = msg,
            notificationId = 2302
        )
    }
}
