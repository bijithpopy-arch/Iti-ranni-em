package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object AttendanceNotificationHelper {
    const val CHANNEL_ID_ALERTS_V2 = "emech_urgent_alerts_v2"
    const val CHANNEL_ID_SYNC_V2 = "emech_sync_alerts_v2"
    private const val PREFS_RINGTONE = "emech_ringtone_prefs"
    private const val KEY_CUSTOM_URI = "custom_ringtone_uri"

    fun defaultCustomSoundUri(context: Context): Uri =
        Uri.parse("android.resource://${context.packageName}/${R.raw.custom_alert}")

    fun getSelectedSoundUri(context: Context): Uri {
        val saved = context.getSharedPreferences(PREFS_RINGTONE, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_URI, null)
        return if (!saved.isNullOrBlank()) Uri.parse(saved) else defaultCustomSoundUri(context)
    }

    fun setSelectedSoundUri(context: Context, uri: Uri?) {
        context.getSharedPreferences(PREFS_RINGTONE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_URI, uri?.toString())
            .apply()
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
        manager.notify(notificationId, notification)
    }
}
