package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Automatically restores periodic WorkManager background polling after device reboot
 * or app update so loud Instructor/Principal alerts work even before the app is reopened.
 */
class BackgroundSyncBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            AttendanceNotificationHelper.ensureChannels(context)
            if (BackgroundSyncConfig.load(context) != null) {
                BackgroundSyncScheduler.ensureScheduled(context)
                BackgroundSyncScheduler.scheduleQuickBackgroundCheck(context, delayMinutes = 1L)
            }
        }
    }
}
