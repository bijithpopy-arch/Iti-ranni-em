package com.example.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val versionName: String,
    val releaseUrl: String,
    val notes: String
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

object AppUpdater {
    private const val GITHUB_LATEST_API = "https://api.github.com/repos/bijithpopy/Iti-ranni-em/releases/latest"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(GITHUB_LATEST_API)
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.code == 404) return@withContext UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME)
                if (!resp.isSuccessful) return@withContext UpdateCheckResult.Error("GitHub returned HTTP ${resp.code}")
                val json = JSONObject(resp.body?.string().orEmpty())
                val tag = json.optString("tag_name", "").removePrefix("v").trim()
                val htmlUrl = json.optString("html_url", "https://github.com")
                val notes = json.optString("body", "Latest release build available.").take(300)
                if (tag.isNotBlank() && tag != BuildConfig.VERSION_NAME) {
                    UpdateCheckResult.UpdateAvailable(AppUpdateInfo(tag, htmlUrl, notes))
                } else {
                    UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME)
                }
            }
        } catch (_: Exception) {
            UpdateCheckResult.UpToDate(BuildConfig.VERSION_NAME)
        }
    }

    fun openReleasePage(context: Context, releaseUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
