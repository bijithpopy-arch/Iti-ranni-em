package com.example.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * In-App Self-Updater reading `version.json` and downloading `app-debug.apk` from GitHub Releases
 * using DownloadManager + FileProvider to launch the system package installer in place.
 */
object AppUpdater {

    // GitHub repository for automated APK self-updates
    const val GITHUB_REPO_SLUG = "bijithpopy-arch/Iti-ranni-em"
    const val VERSION_JSON_URL =
        "https://github.com/$GITHUB_REPO_SLUG/releases/download/latest/version.json"
    const val FALLBACK_APK_URL =
        "https://github.com/$GITHUB_REPO_SLUG/releases/download/latest/app-debug.apk"

    private const val APK_FILE_NAME = "iti-ranni-em-update.apk"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(
        versionJsonUrl: String = VERSION_JSON_URL
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val separator = if ("?" in versionJsonUrl) "&" else "?"
            val requestUrl = "$versionJsonUrl${separator}t=${System.currentTimeMillis()}"

            val request = Request.Builder()
                .url(requestUrl)
                .get()
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateCheckResult.Error(
                        "Could not check releases (HTTP ${response.code}). Ensure GitHub Release 'latest' has version.json."
                    )
                }
                val body = response.body?.string()?.trim().orEmpty()
                if (!body.startsWith("{")) {
                    return@withContext UpdateCheckResult.Error("Invalid update manifest received.")
                }
                val json = JSONObject(body)
                val remoteCode = json.optInt("versionCode", 0)
                val remoteName = json.optString("versionName", "1.0").ifBlank { "1.0" }
                val apkUrl = json.optString("apkUrl", FALLBACK_APK_URL).ifBlank { FALLBACK_APK_URL }
                val changelog = json.optString(
                    "changelog",
                    "Bug fixes, attendance accuracy improvements, and Shop Floor updates."
                )

                if (remoteCode > BuildConfig.VERSION_CODE) {
                    UpdateCheckResult.UpdateAvailable(
                        AppUpdateInfo(
                            versionCode = remoteCode,
                            versionName = remoteName,
                            apkUrl = apkUrl,
                            changelog = changelog
                        )
                    )
                } else {
                    UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME)
                }
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(
                e.localizedMessage ?: "Unable to reach GitHub Releases. Check your internet connection."
            )
        }
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openUnknownSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Downloads the APK via DownloadManager into app-scoped Downloads directory
     * and invokes [onComplete] when ready to install via FileProvider.
     */
    fun downloadAndInstallApk(
        context: Context,
        updateInfo: AppUpdateInfo,
        onStarted: () -> Unit,
        onComplete: (Boolean, String) -> Unit
    ) {
        val appContext = context.applicationContext
        val downloadDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (downloadDir == null) {
            onComplete(false, "External downloads directory unavailable.")
            return
        }

        val targetFile = File(downloadDir, APK_FILE_NAME)
        if (targetFile.exists()) {
            targetFile.delete()
        }

        val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (downloadManager == null) {
            onComplete(false, "DownloadManager service unavailable.")
            return
        }

        val request = DownloadManager.Request(Uri.parse(updateInfo.apkUrl))
            .setTitle("Govt. ITI Ranni EM v${updateInfo.versionName}")
            .setDescription("Downloading app update...")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                appContext,
                Environment.DIRECTORY_DOWNLOADS,
                APK_FILE_NAME
            )

        val downloadId = downloadManager.enqueue(request)
        onStarted()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val completedId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (completedId != downloadId) return
                try {
                    appContext.unregisterReceiver(this)
                } catch (_: Exception) {
                }

                val query = DownloadManager.Query().setFilterById(downloadId)
                var success = false
                downloadManager.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (statusIdx >= 0 && cursor.getInt(statusIdx) == DownloadManager.STATUS_SUCCESSFUL) {
                            success = true
                        }
                    }
                }

                if (success && targetFile.exists()) {
                    val launched = launchApkInstaller(appContext, targetFile)
                    if (launched) {
                        onComplete(true, "Installer launched for v${updateInfo.versionName}.")
                    } else {
                        onComplete(false, "Unable to launch package installer. Allow 'Install unknown apps' in Settings.")
                    }
                } else {
                    onComplete(false, "APK download failed. Please check GitHub Release asset.")
                }
            }
        }

        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    fun launchApkInstaller(context: Context, apkFile: File): Boolean {
        return try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
