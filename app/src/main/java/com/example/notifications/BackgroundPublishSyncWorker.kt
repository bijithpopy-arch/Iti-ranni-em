package com.example.notifications

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
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

object BackgroundSyncConfig {
    private const val PREFS_NAME = "iti_ranni_bg_sync_prefs"
    private const val KEY_ROLE = "bg_role"
    private const val KEY_ROLL = "bg_roll"
    private const val KEY_PIN = "bg_pin"

    fun save(context: Context, role: UserRole, roll: String, pin: String) {
        if (pin.isBlank()) return
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
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
            val creds = BackgroundSyncCredentials(role, roll, pin)
            if (creds.isValid) return creds
        }
        val saved = SessionPreferences.load(appCtx)
        return if (saved.hasCredentials) BackgroundSyncCredentials(saved.role, saved.roll, saved.pin) else null
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

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
                UserRole.TRAINEE -> when (val res = apiClient.fetchTraineeData(creds.roll, creds.pin)) {
                    is ApiResult.Success -> {
                        AttendanceNotificationHelper.evaluateTraineePublishedUpdates(ctx, res.data, seedBaselineOnly = false)
                        Result.success()
                    }
                    is ApiResult.Error -> retryOrComplete()
                }
                UserRole.PARENT -> when (val res = apiClient.fetchParentData(creds.roll, creds.pin)) {
                    is ApiResult.Success -> {
                        AttendanceNotificationHelper.evaluateParentPublishedUpdates(ctx, res.data, seedBaselineOnly = false)
                        Result.success()
                    }
                    is ApiResult.Error -> retryOrComplete()
                }
                UserRole.TRADE_INSTRUCTOR, UserRole.PRINCIPAL -> when (val res = apiClient.fetchStaffData(creds.pin)) {
                    is ApiResult.Success -> {
                        AttendanceNotificationHelper.evaluateStaffPublishedUpdates(ctx, res.data, creds.role, seedBaselineOnly = false)
                        Result.success()
                    }
                    is ApiResult.Error -> retryOrComplete()
                }
            }
        } catch (_: Exception) {
            retryOrComplete()
        }
    }

    private fun retryOrComplete(): Result = if (runAttemptCount < 2) Result.retry() else Result.success()
}

object BackgroundSyncScheduler {
    private const val PERIODIC_WORK_NAME = "iti_ranni_published_sync_periodic"

    fun ensureScheduled(context: Context) {
        val appCtx = context.applicationContext
        AttendanceNotificationHelper.ensureChannels(appCtx)
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val req = PeriodicWorkRequestBuilder<BackgroundPublishSyncWorker>(15L, TimeUnit.MINUTES, 5L, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1L, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(appCtx).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(PERIODIC_WORK_NAME)
    }
}
