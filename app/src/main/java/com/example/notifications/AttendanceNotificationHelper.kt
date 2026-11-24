package com.example.notifications

import android.Manifest
import android.app.Notification
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
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.HolidayAnnouncement
import com.example.data.NoticeItem
import com.example.data.ParentDashboardData
import com.example.data.StaffDashboardData
import com.example.data.TraineeDashboardData
import com.example.data.UserRole

object AttendanceNotificationHelper {

    const val EXTRA_OPEN_NOTICES = "extra_open_notices"
    const val EXTRA_FROM_BACKGROUND_ALERT = "extra_from_background_alert"

    private const val ATTENDANCE_CHANNEL_ID = "iti_ranni_attendance_alerts_v3"
    private const val ATTENDANCE_CHANNEL_NAME = "Attendance Status Alerts (Loud)"
    private const val ATTENDANCE_CHANNEL_DESC =
        "High-priority loud alerts when today's attendance is confirmed & closed by the Trade Instructor."

    private const val NOTICES_CHANNEL_ID = "iti_ranni_trade_notices_v3"
    private const val NOTICES_CHANNEL_NAME = "College & Trade Notices (Loud)"
    private const val NOTICES_CHANNEL_DESC =
        "High-priority loud alarm alerts for notices, broadcasts, and holiday announcements posted by the Trade Instructor or Principal."

    private const val PUBLISHED_ALERTS_CHANNEL_ID = "iti_ranni_published_alerts_v3"
    private const val PUBLISHED_ALERTS_CHANNEL_NAME = "Published Approvals, PDFs & Reports (Loud)"
    private const val PUBLISHED_ALERTS_CHANNEL_DESC =
        "High-priority loud alerts for leave approvals, signed PDFs, published attendance reports, timetable, and duty updates."

    private const val MARKS_CHANNEL_ID = "iti_ranni_exam_marks_v3"
    private const val MARKS_CHANNEL_NAME = "Monthly Theory & Lab Marks (Loud)"
    private const val MARKS_CHANNEL_DESC =
        "Loud notification alerts when the Trade Instructor publishes or edits monthly Theory & Lab marks."

    private const val LOUD_URGENT_CHANNEL_ID = "iti_ranni_loud_urgent_alerts_v3"
    private const val LOUD_URGENT_CHANNEL_NAME = "Urgent Running Late & Mark Queries (Loud)"
    private const val LOUD_URGENT_CHANNEL_DESC =
        "Distinct loud alarm-tone alerts for Trainee Running Late notices and private Work / Lab Mark queries."

    private val LOUD_VIBRATION_PATTERN = longArrayOf(0L, 500L, 180L, 500L, 180L, 650L)

    private const val PREFS_NAME = "iti_ranni_notification_state"
    private const val KEY_LAST_ATTENDANCE_SIG = "last_notified_attendance_sig"
    private const val KEY_LAST_SEEN_NOTICE_ID = "last_seen_notice_id"
    private const val KEY_LAST_NOTIFIED_NOTICE_ID = "last_notified_notice_id"
    private const val KEY_NOTIFIED_NOTICE_KEYS = "notified_notice_keys_v1"
    private const val KEY_LAST_MARKS_SIG = "last_notified_marks_sig"
    private const val KEY_LAST_TRAINEE_MARKS_SIG = "last_notified_trainee_marks_sig"
    private const val KEY_LAST_LATE_ALERT_SIG = "last_notified_running_late_sig"
    private const val KEY_LAST_WORK_QUERY_SIG = "last_notified_work_query_sig"

    // Background published-item deduplication keys
    private const val KEY_BASELINE_SEEDED_PREFIX = "bg_baseline_seeded_"
    private const val KEY_SEEN_LEAVE_SIGS = "seen_leave_sigs_v1"
    private const val KEY_SEEN_HOLIDAY_KEYS = "seen_holiday_keys_v1"
    private const val KEY_LAST_CLOSED_REPORT_SIG = "last_closed_report_sig_v1"
    private const val KEY_LAST_PUBLISHED_PDF_SIG = "last_published_pdf_sig_v1"
    private const val KEY_LAST_DUTY_OVERRIDE_SIG = "last_duty_override_sig_v1"
    private const val KEY_LAST_TIMETABLE_SIG = "last_timetable_sig_v1"
    private const val KEY_SEEN_PENDING_LEAVE_IDS = "seen_pending_leave_ids_v1"
    private const val KEY_SEEN_PARENT_ALERT_IDS = "seen_parent_alert_ids_v1"

    private fun resolveLoudAlertSoundUri(context: Context): Uri {
        val customResId = context.resources.getIdentifier("iti_loud_alert", "raw", context.packageName)
        if (customResId != 0) {
            return Uri.parse("android.resource://${context.packageName}/$customResId")
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: Uri.EMPTY
    }

    private fun loudAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }

    /**
     * Plays a distinct, loud alert tone + strong haptic vibration with safe multi-tier fallback
     * (custom raw resource -> system alarm tone -> ringtone -> notification tone -> ToneGenerator beep).
     * Thread-safe when called from WorkManager background workers.
     */
    fun playLoudAlertTone(context: Context) {
        val appContext = context.applicationContext
        try {
            val uri = resolveLoudAlertSoundUri(appContext)
            if (uri != Uri.EMPTY) {
                val ringtone = RingtoneManager.getRingtone(appContext, uri)
                if (ringtone != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ringtone.audioAttributes = loudAudioAttributes()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ringtone.volume = 1.0f
                    }
                    ringtone.play()
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            if (ringtone.isPlaying) ringtone.stop()
                        } catch (_: Exception) {
                        }
                    }, 2600L)
                } else {
                    playFallbackToneBeep()
                }
            } else {
                playFallbackToneBeep()
            }
        } catch (_: Exception) {
            playFallbackToneBeep()
        }

        triggerUrgentVibration(appContext)
    }

    private fun playFallbackToneBeep() {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1100)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    tg.release()
                } catch (_: Exception) {
                }
            }, 1300L)
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
            // Ignore vibration failures on devices without vibrator hardware
        }
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val loudSoundUri = resolveLoudAlertSoundUri(context)
            val alarmAttrs = loudAudioAttributes()

            createLoudHighImportanceChannel(
                manager = manager,
                id = ATTENDANCE_CHANNEL_ID,
                name = ATTENDANCE_CHANNEL_NAME,
                desc = ATTENDANCE_CHANNEL_DESC,
                soundUri = loudSoundUri,
                audioAttrs = alarmAttrs
            )
            createLoudHighImportanceChannel(
                manager = manager,
                id = NOTICES_CHANNEL_ID,
                name = NOTICES_CHANNEL_NAME,
                desc = NOTICES_CHANNEL_DESC,
                soundUri = loudSoundUri,
                audioAttrs = alarmAttrs
            )
            createLoudHighImportanceChannel(
                manager = manager,
                id = PUBLISHED_ALERTS_CHANNEL_ID,
                name = PUBLISHED_ALERTS_CHANNEL_NAME,
                desc = PUBLISHED_ALERTS_CHANNEL_DESC,
                soundUri = loudSoundUri,
                audioAttrs = alarmAttrs
            )
            createLoudHighImportanceChannel(
                manager = manager,
                id = MARKS_CHANNEL_ID,
                name = MARKS_CHANNEL_NAME,
                desc = MARKS_CHANNEL_DESC,
                soundUri = loudSoundUri,
                audioAttrs = alarmAttrs
            )
            createLoudHighImportanceChannel(
                manager = manager,
                id = LOUD_URGENT_CHANNEL_ID,
                name = LOUD_URGENT_CHANNEL_NAME,
                desc = LOUD_URGENT_CHANNEL_DESC,
                soundUri = loudSoundUri,
                audioAttrs = alarmAttrs
            )
        }
    }

    private fun createLoudHighImportanceChannel(
        manager: NotificationManager,
        id: String,
        name: String,
        desc: String,
        soundUri: Uri,
        audioAttrs: AudioAttributes
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(id) == null) {
                val channel = NotificationChannel(
                    id,
                    name,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = desc
                    enableLights(true)
                    enableVibration(true)
                    vibrationPattern = LOUD_VIBRATION_PATTERN
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(soundUri, audioAttrs)
                    setShowBadge(true)
                    try {
                        setBypassDnd(true)
                    } catch (_: Exception) {
                        // Best-effort; requires system DND policy grant on some OEMs
                    }
                }
                manager.createNotificationChannel(channel)
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

    private fun buildOpenAppPendingIntent(
        context: Context,
        requestCode: Int,
        openNotices: Boolean = false
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_FROM_BACKGROUND_ALERT, true)
            if (openNotices) {
                putExtra(EXTRA_OPEN_NOTICES, true)
            }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun postLoudNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        openNotices: Boolean = false,
        playToneImmediately: Boolean = true
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)

        val pendingIntent = buildOpenAppPendingIntent(
            context = context,
            requestCode = notificationId,
            openNotices = openNotices
        )
        val loudSound = resolveLoudAlertSoundUri(context)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(loudSound, AudioManager.STREAM_ALARM)
            .setVibrate(LOUD_VIBRATION_PATTERN)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            if (playToneImmediately) {
                playLoudAlertTone(context)
            }
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun noticeStableKey(notice: NoticeItem): String {
        return notice.id.trim().ifBlank {
            "${notice.date.trim()}|${notice.title.trim()}|${notice.postedBy.trim()}"
        }
    }

    private fun readStringSet(context: Context, key: String): MutableSet<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    private fun saveStringSet(context: Context, key: String, values: Set<String>) {
        // Keep bounded to avoid unbounded SharedPreferences growth while preserving recent history
        val trimmed = if (values.size > 120) values.toList().takeLast(120).toSet() else values
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(key, trimmed)
            .apply()
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

        val title = "Govt. ITI Ranni • Attendance Confirmed"
        val body = if (normalizedStatus == "ABSENT") {
            "Roll #${data.roll} (${data.studentName}) is confirmed ABSENT on $dateKey. Tap to send a leave reason or report bunking."
        } else {
            "Roll #${data.roll} (${data.studentName}) is confirmed HALF-DAY on $dateKey (IN: ${data.attendance.inT}, OUT: ${data.attendance.outT})."
        }

        val notificationId = (data.roll + "_" + dateKey).hashCode()
        val posted = postLoudNotification(
            context = context,
            channelId = ATTENDANCE_CHANNEL_ID,
            notificationId = notificationId,
            title = title,
            body = body,
            openNotices = false,
            playToneImmediately = true
        )
        if (posted) {
            prefs.edit().putString(KEY_LAST_ATTENDANCE_SIG, signature).apply()
        }
        return posted
    }

    fun getLastSeenNoticeId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_SEEN_NOTICE_ID, null)
    }

    fun markNoticesSeen(context: Context, latestNoticeId: String?) {
        if (latestNoticeId.isNullOrBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val seenKeys = readStringSet(context, KEY_NOTIFIED_NOTICE_KEYS)
        seenKeys.add(latestNoticeId.trim())
        prefs.edit()
            .putString(KEY_LAST_SEEN_NOTICE_ID, latestNoticeId)
            .putString(KEY_LAST_NOTIFIED_NOTICE_ID, latestNoticeId)
            .putStringSet(KEY_NOTIFIED_NOTICE_KEYS, seenKeys)
            .apply()
    }

    fun countUnreadNotices(context: Context, notices: List<NoticeItem>): Int {
        if (notices.isEmpty()) return 0
        val lastSeenId = getLastSeenNoticeId(context) ?: return notices.size
        val index = notices.indexOfFirst { it.id == lastSeenId }
        return if (index < 0) notices.size else index
    }

    /**
     * Triggers a loud, high-importance status-bar popup ONCE when a brand-new notice
     * or pinned broadcast from the Trade Instructor or Principal arrives.
     * Tapping the notification opens MainActivity directly on the Notices page.
     */
    fun notifyNewNoticeIfNeeded(
        context: Context,
        notices: List<NoticeItem>,
        pinnedBroadcast: NoticeItem? = null
    ): Boolean {
        val ordered = buildList {
            if (pinnedBroadcast != null) add(pinnedBroadcast)
            notices.forEach { n ->
                if (none { noticeStableKey(it) == noticeStableKey(n) }) add(n)
            }
        }
        val latest = ordered.firstOrNull() ?: return false
        val latestKey = noticeStableKey(latest)
        if (latestKey.isBlank()) return false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSeen = prefs.getString(KEY_LAST_SEEN_NOTICE_ID, null)
        val lastNotified = prefs.getString(KEY_LAST_NOTIFIED_NOTICE_ID, null)
        val notifiedSet = readStringSet(context, KEY_NOTIFIED_NOTICE_KEYS)

        // Collect all brand-new notices not yet notified/seen
        val newNotices = if (notifiedSet.isEmpty() && lastSeen == null && lastNotified == null) {
            // Very first run: only notify for the single latest notice so we don't spam historical notices
            listOf(latest)
        } else {
            ordered.filter { item ->
                val k = noticeStableKey(item)
                k != lastSeen && k != lastNotified && !notifiedSet.contains(k)
            }.take(3)
        }

        if (newNotices.isEmpty()) return false
        if (!hasNotificationPermission(context)) return false

        var anyPosted = false
        newNotices.forEachIndexed { idx, notice ->
            val sender = notice.postedBy.trim().ifBlank { "Govt. ITI Ranni" }
            val prefix = when {
                notice.isPinned -> "PRINCIPAL BROADCAST • "
                notice.category.isNotBlank() -> "[${notice.category}] "
                else -> "New Notice • "
            }
            val title = "$prefix${notice.title}"
            val bodyText = buildString {
                append("By $sender")
                if (notice.date.isNotBlank()) append(" (${notice.date})")
                if (notice.body.isNotBlank()) {
                    append(": ")
                    append(notice.body)
                }
            }

            val posted = postLoudNotification(
                context = context,
                channelId = NOTICES_CHANNEL_ID,
                notificationId = ("notice_${noticeStableKey(notice)}").hashCode(),
                title = title,
                body = bodyText,
                openNotices = true,
                playToneImmediately = (idx == 0)
            )
            if (posted) {
                anyPosted = true
            }
        }

        if (anyPosted) {
            ordered.forEach { notifiedSet.add(noticeStableKey(it)) }
            prefs.edit()
                .putString(KEY_LAST_NOTIFIED_NOTICE_ID, latest.id.ifBlank { latestKey })
                .apply()
            saveStringSet(context, KEY_NOTIFIED_NOTICE_KEYS, notifiedSet)
        }
        return anyPosted
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

        val title = if (latest.isRevised) {
            "Govt. ITI Ranni • Monthly Marks Updated (${latest.monthLabel})"
        } else {
            "Govt. ITI Ranni • Monthly Marks Published (${latest.monthLabel})"
        }
        val body = "${data.studentName} (Roll #${data.roll}) — Theory: ${latest.theoryMarks}/${latest.theoryMax}, Lab: ${latest.labMarks}/${latest.labMax} (Total: ${latest.totalObtained}/${latest.totalMax}, ${latest.percentage}%)."

        val notificationId = ("marks_${data.roll}_${latest.monthLabel}").hashCode()
        val posted = postLoudNotification(
            context = context,
            channelId = MARKS_CHANNEL_ID,
            notificationId = notificationId,
            title = title,
            body = body,
            openNotices = false,
            playToneImmediately = true
        )
        if (posted) {
            prefs.edit().putString(KEY_LAST_MARKS_SIG, signature).apply()
        }
        return posted
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
        var fired = false

        val latestLate = data.runningLateAlerts.firstOrNull()
        if (latestLate != null) {
            val lateSig = "${latestLate.id}|${latestLate.roll}|${latestLate.date}|${latestLate.time}|${latestLate.expectedArrival}"
            if (prefs.getString(KEY_LAST_LATE_ALERT_SIG, null) != lateSig) {
                prefs.edit().putString(KEY_LAST_LATE_ALERT_SIG, lateSig).apply()
                val title = "Running Late Alert • Roll #${latestLate.roll} (${latestLate.studentName})"
                val body = "ETA: ${latestLate.expectedArrival} • Reason: ${latestLate.reasonCategory}" +
                    if (latestLate.details.isNotBlank()) " — ${latestLate.details}" else ""
                postLoudNotification(
                    context = context,
                    channelId = LOUD_URGENT_CHANNEL_ID,
                    notificationId = ("late_${latestLate.id}").hashCode(),
                    title = title,
                    body = body,
                    openNotices = false,
                    playToneImmediately = false
                )
                fired = true
            }
        }

        val latestOpenQuery = data.workMarkQueries.firstOrNull { it.status.equals("OPEN", ignoreCase = true) }
        if (latestOpenQuery != null) {
            val querySig = "${latestOpenQuery.id}|${latestOpenQuery.roll}|${latestOpenQuery.date}|${latestOpenQuery.workReference}"
            if (prefs.getString(KEY_LAST_WORK_QUERY_SIG, null) != querySig) {
                prefs.edit().putString(KEY_LAST_WORK_QUERY_SIG, querySig).apply()
                val title = "Work / Lab Mark Query • Roll #${latestOpenQuery.roll}"
                val body = "${latestOpenQuery.studentName}: ${latestOpenQuery.workReference} — ${latestOpenQuery.traineeNote}"
                postLoudNotification(
                    context = context,
                    channelId = LOUD_URGENT_CHANNEL_ID,
                    notificationId = ("wmq_${latestOpenQuery.id}").hashCode(),
                    title = title,
                    body = body,
                    openNotices = false,
                    playToneImmediately = false
                )
                fired = true
            }
        }

        if (fired) {
            playLoudAlertTone(context)
        }
        return fired
    }

    // =========================================================================
    // BACKGROUND & FOREGROUND PUBLISHED UPDATES DETECTOR (INSTRUCTOR & PRINCIPAL)
    // =========================================================================

    /**
     * Evaluates ALL newly published items from the Trade Instructor (PIN 2025) or
     * Principal (PIN 9999 / 8492) for a Trainee account:
     * - New Notices & Pinned Broadcasts
     * - Leave Approvals / Rejections & newly attached Signed PDF links
     * - Daily Attendance Confirmed/Closed & Published Attendance Report PDFs
     * - Newly Declared Holidays
     * - Published / Revised Monthly Theory & Lab Marks
     * - Instructor Duty Charge Overrides & Official Timetable Publishes
     *
     * When [seedBaselineOnly] is true on the very first run for this account,
     * historical records (old leaves, old holidays, etc.) are recorded silently
     * without spamming, while future background/foreground diffs alert loudly immediately.
     */
    fun evaluateTraineePublishedUpdates(
        context: Context,
        data: TraineeDashboardData,
        seedBaselineOnly: Boolean = false
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val baselineKey = "${KEY_BASELINE_SEEDED_PREFIX}TRAINEE_${data.roll}"
        val isFirstSeed = !prefs.getBoolean(baselineKey, false)
        var anyAlerted = false

        // 1. Notices & Pinned Broadcasts (always check unless explicitly suppressed)
        if (!seedBaselineOnly) {
            if (notifyNewNoticeIfNeeded(context, data.notices, data.pinnedBroadcast)) {
                anyAlerted = true
            }
        } else if (isFirstSeed) {
            // Seed older notices beyond the very latest so an unread latest notice still alerts once
            val existingKeys = readStringSet(context, KEY_NOTIFIED_NOTICE_KEYS)
            data.notices.drop(1).forEach { existingKeys.add(noticeStableKey(it)) }
            saveStringSet(context, KEY_NOTIFIED_NOTICE_KEYS, existingKeys)
        }

        // 2. Leave Approvals / Rejections / Signed PDF Links
        val seenLeaves = readStringSet(context, KEY_SEEN_LEAVE_SIGS)
        val currentLeaveSigs = data.leaves.map { leave ->
            "${data.roll}|${leave.id}|${leave.status.uppercase()}|${leave.pdf.trim()}"
        }
        if (isFirstSeed && seedBaselineOnly) {
            seenLeaves.addAll(currentLeaveSigs)
            saveStringSet(context, KEY_SEEN_LEAVE_SIGS, seenLeaves)
        } else {
            var leavesUpdated = false
            data.leaves.forEach { leave ->
                val sig = "${data.roll}|${leave.id}|${leave.status.uppercase()}|${leave.pdf.trim()}"
                if (!seenLeaves.contains(sig)) {
                    val statusUpper = leave.status.trim().uppercase()
                    val isInstructorOrPrincipalUpdate =
                        leave.pdf.isNotBlank() ||
                            statusUpper.contains("APPROVED") ||
                            statusUpper.contains("REJECTED") ||
                            statusUpper.contains("SIGNED") ||
                            statusUpper == "PENDING_PRINCIPAL"
                    if (isInstructorOrPrincipalUpdate && !seedBaselineOnly) {
                        val hasPdf = leave.pdf.isNotBlank()
                        val title = when {
                            hasPdf -> "Signed Out-Pass PDF Published • ${leave.id.ifBlank { leave.type }}"
                            statusUpper.contains("REJECT") -> "Leave Request Rejected • ${leave.type}"
                            statusUpper == "PENDING_PRINCIPAL" -> "Instructor Approved Leave • Forwarded to Principal"
                            else -> "Leave Approved • ${leave.type}"
                        }
                        val body = buildString {
                            append("Roll #${data.roll} (${data.name}) • ${leave.type} (${leave.date}) — Status: ${leave.status}.")
                            if (hasPdf) append(" Tap to open the app and view your signed PDF.")
                        }
                        val posted = postLoudNotification(
                            context = context,
                            channelId = PUBLISHED_ALERTS_CHANNEL_ID,
                            notificationId = ("leave_${leave.id}_${statusUpper}").hashCode(),
                            title = title,
                            body = body,
                            openNotices = false,
                            playToneImmediately = !anyAlerted
                        )
                        if (posted) {
                            anyAlerted = true
                            seenLeaves.add(sig)
                            leavesUpdated = true
                        }
                    } else {
                        seenLeaves.add(sig)
                        leavesUpdated = true
                    }
                }
            }
            if (leavesUpdated) {
                saveStringSet(context, KEY_SEEN_LEAVE_SIGS, seenLeaves)
            }
        }

        // 3. Holidays declared by Instructor or Principal
        if (evaluateHolidays(context, data.holidays, data.todayHoliday, isFirstSeed && seedBaselineOnly, !anyAlerted)) {
            anyAlerted = true
        }

        // 4. Published Attendance Report PDF or Attendance Confirmed & Closed
        if (evaluateAttendanceReportOrClose(
                context = context,
                accountTag = "TRAINEE_${data.roll}",
                attendanceClosed = data.attendanceClosed,
                attendanceDate = data.attendanceDate,
                closedAt = data.closedAt,
                statusLine = "Today's status: ${data.attendance.todayStatus} (Overall: ${data.attendance.pct}%)",
                publishedReportUrl = data.publishedReportUrl,
                publishedReportAt = data.publishedReportAt,
                seedOnly = isFirstSeed && seedBaselineOnly,
                playTone = !anyAlerted
            )
        ) {
            anyAlerted = true
        }

        // 5. Published / Revised Monthly Marks for Trainee
        val latestMark = data.monthlyMarks.firstOrNull { it.published }
        if (latestMark != null) {
            val markSig = "${data.roll}|${latestMark.id}|${latestMark.monthLabel}|${latestMark.theoryMarks}/${latestMark.theoryMax}|${latestMark.labMarks}/${latestMark.labMax}|${latestMark.updatedAt}"
            val lastMarkSig = prefs.getString(KEY_LAST_TRAINEE_MARKS_SIG, null)
            if (isFirstSeed && seedBaselineOnly) {
                prefs.edit().putString(KEY_LAST_TRAINEE_MARKS_SIG, markSig).apply()
            } else if (lastMarkSig != markSig && !seedBaselineOnly) {
                val title = if (latestMark.isRevised) {
                    "Monthly Marks Updated • ${latestMark.monthLabel}"
                } else {
                    "Monthly Marks Published • ${latestMark.monthLabel}"
                }
                val body = "Theory: ${latestMark.theoryMarks}/${latestMark.theoryMax}, Lab: ${latestMark.labMarks}/${latestMark.labMax} (${latestMark.percentage}%). Tap to view."
                val posted = postLoudNotification(
                    context = context,
                    channelId = MARKS_CHANNEL_ID,
                    notificationId = ("tmarks_${data.roll}_${latestMark.monthLabel}").hashCode(),
                    title = title,
                    body = body,
                    openNotices = false,
                    playToneImmediately = !anyAlerted
                )
                if (posted) {
                    anyAlerted = true
                    prefs.edit().putString(KEY_LAST_TRAINEE_MARKS_SIG, markSig).apply()
                }
            }
        }

        // 6. Instructor Duty Charge Override & Official Timetable updates
        val dutySig = data.dutyOverrides.entries
            .sortedBy { it.key }
            .joinToString(";") { (k, v) -> "$k:${v.firstRoll},${v.secondRoll},${v.updatedAt}" }
        if (dutySig.isNotBlank()) {
            val prevDutySig = prefs.getString(KEY_LAST_DUTY_OVERRIDE_SIG, null)
            if (isFirstSeed && seedBaselineOnly) {
                prefs.edit().putString(KEY_LAST_DUTY_OVERRIDE_SIG, dutySig).apply()
            } else if (prevDutySig != null && prevDutySig != dutySig && !seedBaselineOnly) {
                val latestEntry = data.dutyOverrides.values.lastOrNull()
                val title = "Duty Rotation Updated by Instructor"
                val body = if (latestEntry != null) {
                    "Roll #${latestEntry.firstRoll} & Roll #${latestEntry.secondRoll} assigned for ${latestEntry.dateKey}. Tap to check duty schedule."
                } else {
                    "Milk, Egg & Tool Crib duty charge was updated by the Trade Instructor."
                }
                val posted = postLoudNotification(
                    context = context,
                    channelId = PUBLISHED_ALERTS_CHANNEL_ID,
                    notificationId = ("duty_override_$dutySig").hashCode(),
                    title = title,
                    body = body,
                    openNotices = false,
                    playToneImmediately = !anyAlerted
                )
                if (posted) {
                    anyAlerted = true
                    prefs.edit().putString(KEY_LAST_DUTY_OVERRIDE_SIG, dutySig).apply()
                }
            } else if (prevDutySig == null) {
                prefs.edit().putString(KEY_LAST_DUTY_OVERRIDE_SIG, dutySig).apply()
            }
        }

        val timetableSig = data.timetable.joinToString(";") {
            "${it.dayOfWeek}|${it.startTime}|${it.endTime}|${it.subject}|${it.sessionType}"
        }
        if (timetableSig.isNotBlank()) {
            val prevTtSig = prefs.getString(KEY_LAST_TIMETABLE_SIG, null)
            if (isFirstSeed && seedBaselineOnly) {
                prefs.edit().putString(KEY_LAST_TIMETABLE_SIG, timetableSig).apply()
            } else if (prevTtSig != null && prevTtSig != timetableSig && !seedBaselineOnly) {
                val posted = postLoudNotification(
                    context = context,
                    channelId = PUBLISHED_ALERTS_CHANNEL_ID,
                    notificationId = ("timetable_update_$timetableSig").hashCode(),
                    title = "Official Weekly Timetable Updated",
                    body = "The Trade Instructor published an updated EMech weekly timetable. Tap to view.",
                    openNotices = false,
                    playToneImmediately = !anyAlerted
                )
                if (posted) {
                    anyAlerted = true
                    prefs.edit().putString(KEY_LAST_TIMETABLE_SIG, timetableSig).apply()
                }
            } else if (prevTtSig == null) {
                prefs.edit().putString(KEY_LAST_TIMETABLE_SIG, timetableSig).apply()
            }
        }

        if (isFirstSeed) {
            prefs.edit().putBoolean(baselineKey, true).apply()
        }
        return anyAlerted
    }

    /**
     * Evaluates all newly published items for a Parent account (background or foreground).
     */
    fun evaluateParentPublishedUpdates(
        context: Context,
        data: ParentDashboardData,
        seedBaselineOnly: Boolean = false
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val baselineKey = "${KEY_BASELINE_SEEDED_PREFIX}PARENT_${data.roll}"
        val isFirstSeed = !prefs.getBoolean(baselineKey, false)
        var anyAlerted = false

        if (!seedBaselineOnly) {
            if (notifyParentIfConfirmedAndEligible(context, data)) anyAlerted = true
            if (notifyParentMarksUpdatedIfNeeded(context, data)) anyAlerted = true
            if (notifyNewNoticeIfNeeded(context, data.notices, data.pinnedBroadcast)) anyAlerted = true
        } else if (isFirstSeed) {
            val existingKeys = readStringSet(context, KEY_NOTIFIED_NOTICE_KEYS)
            data.notices.drop(1).forEach { existingKeys.add(noticeStableKey(it)) }
            saveStringSet(context, KEY_NOTIFIED_NOTICE_KEYS, existingKeys)
        }

        if (evaluateHolidays(context, data.holidays, data.todayHoliday, isFirstSeed && seedBaselineOnly, !anyAlerted)) {
            anyAlerted = true
        }

        if (evaluateAttendanceReportOrClose(
                context = context,
                accountTag = "PARENT_${data.roll}",
                attendanceClosed = false, // Parent confirmed ABSENT/HALF-DAY already handled by notifyParentIfConfirmedAndEligible
                attendanceDate = data.attendanceDate,
                closedAt = data.closedAt,
                statusLine = "Ward: ${data.studentName} (Roll #${data.roll})",
                publishedReportUrl = data.publishedReportUrl,
                publishedReportAt = data.publishedReportAt,
                seedOnly = isFirstSeed && seedBaselineOnly,
                playTone = !anyAlerted
            )
        ) {
            anyAlerted = true
        }

        if (isFirstSeed) {
            prefs.edit().putBoolean(baselineKey, true).apply()
        }
        return anyAlerted
    }

    /**
     * Evaluates all newly published items for Trade Instructor (PIN 2025) or Principal (PIN 9999 / 8492).
     */
    fun evaluateStaffPublishedUpdates(
        context: Context,
        data: StaffDashboardData,
        activeRole: UserRole,
        seedBaselineOnly: Boolean = false
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val baselineKey = "${KEY_BASELINE_SEEDED_PREFIX}${activeRole.name}"
        val isFirstSeed = !prefs.getBoolean(baselineKey, false)
        var anyAlerted = false

        // 1. Notices & Pinned Broadcasts (e.g., Principal broadcasts -> Instructor; Instructor notices -> Principal)
        if (!seedBaselineOnly) {
            if (notifyNewNoticeIfNeeded(context, data.notices, data.pinnedBroadcast)) {
                anyAlerted = true
            }
        } else if (isFirstSeed) {
            val existingKeys = readStringSet(context, KEY_NOTIFIED_NOTICE_KEYS)
            data.notices.forEach { existingKeys.add(noticeStableKey(it)) }
            saveStringSet(context, KEY_NOTIFIED_NOTICE_KEYS, existingKeys)
        }

        // 2. Urgent Trainee Running Late + Work/Lab Mark Queries (for Trade Instructor)
        if (activeRole == UserRole.TRADE_INSTRUCTOR) {
            if (isFirstSeed && seedBaselineOnly) {
                data.runningLateAlerts.firstOrNull()?.let { latestLate ->
                    val lateSig = "${latestLate.id}|${latestLate.roll}|${latestLate.date}|${latestLate.time}|${latestLate.expectedArrival}"
                    prefs.edit().putString(KEY_LAST_LATE_ALERT_SIG, lateSig).apply()
                }
                data.workMarkQueries.firstOrNull { it.status.equals("OPEN", ignoreCase = true) }?.let { q ->
                    val qSig = "${q.id}|${q.roll}|${q.date}|${q.workReference}"
                    prefs.edit().putString(KEY_LAST_WORK_QUERY_SIG, qSig).apply()
                }
            } else if (!seedBaselineOnly) {
                if (notifyInstructorUrgentAlertsIfNeeded(context, data)) {
                    anyAlerted = true
                }
            }
        }

        // 3. New Pending Leave Requests awaiting approval/signature
        val seenPending = readStringSet(context, KEY_SEEN_PENDING_LEAVE_IDS)
        val currentPendingIds = data.pendingLeaves.map { "${activeRole.name}|${it.reqId}" }
        if (isFirstSeed && seedBaselineOnly) {
            seenPending.addAll(currentPendingIds)
            saveStringSet(context, KEY_SEEN_PENDING_LEAVE_IDS, seenPending)
        } else if (!seedBaselineOnly) {
            var pendingUpdated = false
            data.pendingLeaves.forEach { leave ->
                val key = "${activeRole.name}|${leave.reqId}"
                if (!seenPending.contains(key)) {
                    val title = if (activeRole == UserRole.PRINCIPAL) {
                        "Leave Ready for Principal Approval • Roll #${leave.roll}"
                    } else {
                        "New Out-Pass / Leave Request • Roll #${leave.roll}"
                    }
                    val body = "${leave.name} requested ${leave.type} (${leave.timeWindow}): ${leave.reason}"
                    val posted = postLoudNotification(
                        context = context,
                        channelId = PUBLISHED_ALERTS_CHANNEL_ID,
                        notificationId = ("pending_$key").hashCode(),
                        title = title,
                        body = body,
                        openNotices = false,
                        playToneImmediately = !anyAlerted
                    )
                    if (posted) {
                        anyAlerted = true
                        seenPending.add(key)
                        pendingUpdated = true
                    }
                }
            }
            if (pendingUpdated) {
                saveStringSet(context, KEY_SEEN_PENDING_LEAVE_IDS, seenPending)
            }
        }

        // 4. New Parent Alerts (bunking / leave reason)
        val seenParentAlerts = readStringSet(context, KEY_SEEN_PARENT_ALERT_IDS)
        if (isFirstSeed && seedBaselineOnly) {
            data.parentAlerts.forEach { seenParentAlerts.add(it.id.ifBlank { "${it.roll}|${it.date}|${it.time}" }) }
            saveStringSet(context, KEY_SEEN_PARENT_ALERT_IDS, seenParentAlerts)
        } else if (!seedBaselineOnly) {
            var alertsUpdated = false
            data.parentAlerts.take(3).forEach { alert ->
                val key = alert.id.ifBlank { "${alert.roll}|${alert.date}|${alert.time}" }
                if (!seenParentAlerts.contains(key)) {
                    val posted = postLoudNotification(
                        context = context,
                        channelId = LOUD_URGENT_CHANNEL_ID,
                        notificationId = ("palert_$key").hashCode(),
                        title = "Parent Alert (${alert.alertType}) • Roll #${alert.roll}",
                        body = "${alert.studentName}: ${alert.message}",
                        openNotices = false,
                        playToneImmediately = !anyAlerted
                    )
                    if (posted) {
                        anyAlerted = true
                        seenParentAlerts.add(key)
                        alertsUpdated = true
                    }
                }
            }
            if (alertsUpdated) {
                saveStringSet(context, KEY_SEEN_PARENT_ALERT_IDS, seenParentAlerts)
            }
        }

        // 5. Holidays & Published Attendance Reports
        if (evaluateHolidays(context, data.holidays, data.todayHoliday, isFirstSeed && seedBaselineOnly, !anyAlerted)) {
            anyAlerted = true
        }
        if (evaluateAttendanceReportOrClose(
                context = context,
                accountTag = activeRole.name,
                attendanceClosed = data.attendanceClosed,
                attendanceDate = data.attendanceDate,
                closedAt = data.closedAt,
                statusLine = "Daily register closed for ${data.attendanceDate}",
                publishedReportUrl = data.publishedReportUrl,
                publishedReportAt = data.publishedReportAt,
                seedOnly = isFirstSeed && seedBaselineOnly,
                playTone = !anyAlerted
            )
        ) {
            anyAlerted = true
        }

        if (isFirstSeed) {
            prefs.edit().putBoolean(baselineKey, true).apply()
        }
        return anyAlerted
    }

    private fun evaluateHolidays(
        context: Context,
        holidays: List<HolidayAnnouncement>,
        todayHoliday: HolidayAnnouncement?,
        seedOnly: Boolean,
        playTone: Boolean
    ): Boolean {
        val allHolidays = buildList {
            if (todayHoliday != null) add(todayHoliday)
            holidays.forEach { h ->
                if (none { it.date == h.date && it.reason == h.reason }) add(h)
            }
        }
        if (allHolidays.isEmpty()) return false

        val seen = readStringSet(context, KEY_SEEN_HOLIDAY_KEYS)
        if (seedOnly) {
            allHolidays.forEach { h -> seen.add(h.id.ifBlank { "${h.date}|${h.reason}" }) }
            saveStringSet(context, KEY_SEEN_HOLIDAY_KEYS, seen)
            return false
        }

        var fired = false
        var changed = false
        allHolidays.take(2).forEach { holiday ->
            val key = holiday.id.ifBlank { "${holiday.date}|${holiday.reason}" }
            if (!seen.contains(key)) {
                val posted = postLoudNotification(
                    context = context,
                    channelId = NOTICES_CHANNEL_ID,
                    notificationId = ("holiday_$key").hashCode(),
                    title = "Holiday Announced • ${holiday.date}",
                    body = "${holiday.reason} (Posted by ${holiday.postedBy.ifBlank { "Govt. ITI Ranni" }})",
                    openNotices = true,
                    playToneImmediately = playTone && !fired
                )
                if (posted) {
                    fired = true
                    seen.add(key)
                    changed = true
                }
            }
        }
        if (changed) {
            saveStringSet(context, KEY_SEEN_HOLIDAY_KEYS, seen)
        }
        return fired
    }

    private fun evaluateAttendanceReportOrClose(
        context: Context,
        accountTag: String,
        attendanceClosed: Boolean,
        attendanceDate: String,
        closedAt: String,
        statusLine: String,
        publishedReportUrl: String,
        publishedReportAt: String,
        seedOnly: Boolean,
        playTone: Boolean
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var fired = false

        // A. Newly published Attendance Report PDF URL/timestamp
        if (publishedReportUrl.isNotBlank()) {
            val pdfSig = "$accountTag|$publishedReportUrl|$publishedReportAt"
            val prevPdfSig = prefs.getString(KEY_LAST_PUBLISHED_PDF_SIG, null)
            if (seedOnly) {
                prefs.edit().putString(KEY_LAST_PUBLISHED_PDF_SIG, pdfSig).apply()
            } else if (prevPdfSig != pdfSig) {
                val posted = postLoudNotification(
                    context = context,
                    channelId = PUBLISHED_ALERTS_CHANNEL_ID,
                    notificationId = ("att_pdf_$pdfSig").hashCode(),
                    title = "Attendance Report Published • Govt. ITI Ranni",
                    body = buildString {
                        append("A new official attendance PDF report has been published")
                        if (publishedReportAt.isNotBlank()) append(" ($publishedReportAt)")
                        append(". Tap to open the app.")
                    },
                    openNotices = false,
                    playToneImmediately = playTone
                )
                if (posted) {
                    fired = true
                    prefs.edit().putString(KEY_LAST_PUBLISHED_PDF_SIG, pdfSig).apply()
                }
            }
        }

        // B. Daily Attendance Confirmed & Closed by Trade Instructor
        if (attendanceClosed && attendanceDate.isNotBlank()) {
            val closeSig = "$accountTag|$attendanceDate|$closedAt"
            val prevCloseSig = prefs.getString(KEY_LAST_CLOSED_REPORT_SIG, null)
            if (seedOnly) {
                prefs.edit().putString(KEY_LAST_CLOSED_REPORT_SIG, closeSig).apply()
            } else if (prevCloseSig != closeSig) {
                val posted = postLoudNotification(
                    context = context,
                    channelId = ATTENDANCE_CHANNEL_ID,
                    notificationId = ("att_close_$closeSig").hashCode(),
                    title = "Attendance Finalized • $attendanceDate",
                    body = "$statusLine${if (closedAt.isNotBlank()) " • Finalized at $closedAt" else ""}. Tap to view.",
                    openNotices = false,
                    playToneImmediately = playTone && !fired
                )
                if (posted) {
                    fired = true
                    prefs.edit().putString(KEY_LAST_CLOSED_REPORT_SIG, closeSig).apply()
                }
            }
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
