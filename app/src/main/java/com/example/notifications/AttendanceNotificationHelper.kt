package com.example.notifications

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.ApiResult
import com.example.data.AttendanceApiClient
import com.example.data.UserRole
import com.example.security.SessionPreferences
import java.util.concurrent.TimeUnit

data class BackgroundSyncCredentials(
    val role: UserRole,
    val roll: String,
    val pin: String
) {
    val isValid: Boolean
        get() = pin.isNotBlank() && (
            role == UserRole.TRADE_INSTRUCTOR ||
                role == UserRole.PRINCIPAL ||
                roll.isNotBlank()
            )
}

/**
 * Persists the active user's lightweight polling profile so WorkManager can continue
 * checking the Google Apps Script endpoint even after the app is closed or swiped away.
 */
object BackgroundSyncConfig {
    private const val PREFS_NAME = "iti_ranni_bg_sync_prefs"
    private const val KEY_ROLE = "bg_role"
    private const val KEY_ROLL = "bg_roll"
    private const val KEY_PIN = "bg_pin"

    fun save(context: Context, role: UserRole, roll: String, pin: String) {
        if (pin.isBlank()) return
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ROLE, role.name)
            .putString(KEY_ROLL, roll.trim())
            .putString(KEY_PIN, pin.trim())
            .apply()
    }

    fun load(context: Context): BackgroundSyncCredentials? {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val roleName = prefs.getString(KEY_ROLE, null)
        val roll = prefs.getString(KEY_ROLL, "").orEmpty().trim()
        val pin = prefs.getString(KEY_PIN, "").orEmpty().trim()
        if (roleName != null && pin.isNotBlank()) {
            val role = UserRole.entries.firstOrNull { it.name == roleName } ?: UserRole.TRAINEE
            val creds = BackgroundSyncCredentials(role = role, roll = roll, pin = pin)
            if (creds.isValid) return creds
        }

        // Fallback to saved Remember-Me credentials if present
        val saved = SessionPreferences.load(appCtx)
        if (saved.hasCredentials) {
            return BackgroundSyncCredentials(
                role = saved.role,
                roll = saved.roll.trim(),
                pin = saved.pin.trim()
            )
        }
        return null
    }

    fun clear(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}

/**
 * WorkManager CoroutineWorker that runs even when the app is closed or in the background.
 * Polls the existing Google Apps Script Web App (/exec) endpoint for newly published
 * notices, broadcasts, leave approvals/signed PDFs, holidays, marks, and attendance reports,
 * firing loud IMPORTANCE_HIGH alarm notifications without duplicates.
 */
class BackgroundPublishSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        AttendanceNotificationHelper.ensureChannels(ctx)

        val creds = BackgroundSyncConfig.load(ctx) ?: return Result.success()
        if (!creds.isValid) return Result.success()

        val apiClient = AttendanceApiClient()
        return try {
            when (creds.role) {
                UserRole.TRAINEE -> {
                    when (val res = apiClient.fetchTraineeData(creds.roll, creds.pin)) {
                        is ApiResult.Success -> {
                            AttendanceNotificationHelper.evaluateTraineePublishedUpdates(
                                context = ctx,
                                data = res.data,
                                seedBaselineOnly = false
                            )
                            Result.success()
                        }
                        is ApiResult.Error -> retryOrComplete()
                    }
                }

                UserRole.PARENT -> {
                    when (val res = apiClient.fetchParentData(creds.roll, creds.pin)) {
                        is ApiResult.Success -> {
                            AttendanceNotificationHelper.evaluateParentPublishedUpdates(
                                context = ctx,
                                data = res.data,
                                seedBaselineOnly = false
                            )
                            Result.success()
                        }
                        is ApiResult.Error -> retryOrComplete()
                    }
                }

                UserRole.TRADE_INSTRUCTOR, UserRole.PRINCIPAL -> {
                    when (val res = apiClient.fetchStaffData(creds.pin)) {
                        is ApiResult.Success -> {
                            AttendanceNotificationHelper.evaluateStaffPublishedUpdates(
                                context = ctx,
                                data = res.data,
                                activeRole = creds.role,
                                seedBaselineOnly = false
                            )
                            Result.success()
                        }
                        is ApiResult.Error -> retryOrComplete()
                    }
                }
            }
        } catch (_: Exception) {
            retryOrComplete()
        }
    }

    private fun retryOrComplete(): Result {
        return if (runAttemptCount < 2) Result.retry() else Result.success()
    }
}

object BackgroundSyncScheduler {
    private const val PERIODIC_WORK_NAME = "iti_ranni_published_sync_periodic"
    private const val QUICK_CHECK_WORK_NAME = "iti_ranni_published_sync_quick"

    private fun connectedConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    /**
     * Schedules the persistent periodic background sync (every 15 minutes, Android's minimum
     * periodic interval) that continues operating even when the app is closed or after reboot.
     */
    fun ensureScheduled(context: Context) {
        val appCtx = context.applicationContext
        AttendanceNotificationHelper.ensureChannels(appCtx)

        val periodicRequest = PeriodicWorkRequestBuilder<BackgroundPublishSyncWorker>(
            15L,
            TimeUnit.MINUTES,
            5L,
            TimeUnit.MINUTES
        )
            .setConstraints(connectedConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1L, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(appCtx).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    /**
     * Enqueues a rapid follow-up background check (e.g., when the app moves to background
     * or after device reboot) so newly posted notices or approvals trigger loud alerts quickly.
     */
    fun scheduleQuickBackgroundCheck(context: Context, delayMinutes: Long = 2L) {
        val appCtx = context.applicationContext
        if (BackgroundSyncConfig.load(appCtx) == null) return

        val builder = OneTimeWorkRequestBuilder<BackgroundPublishSyncWorker>()
            .setConstraints(connectedConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)

        if (delayMinutes > 0L) {
            builder.setInitialDelay(delayMinutes, TimeUnit.MINUTES)
        }

        WorkManager.getInstance(appCtx).enqueueUniqueWork(
            QUICK_CHECK_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            builder.build()
        )
    }

    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context.applicationContext)
        wm.cancelUniqueWork(PERIODIC_WORK_NAME)
        wm.cancelUniqueWork(QUICK_CHECK_WORK_NAME)
    }
}
