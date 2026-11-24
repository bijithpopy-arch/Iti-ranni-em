package com.example.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppReleaseInfo(
    val versionName: String,
    val releaseUrl: String,
    val notes: String
)

object AppUpdater {
    private const val GITHUB_LATEST_RELEASE_URL =
        "https://api.github.com/repos/bijithpopy/ITI-Ranni-EM/releases/latest"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(currentVersionName: String): AppReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) return@withContext null
                val json = JSONObject(body)
                val tag = json.optString("tag_name", "").removePrefix("v").trim()
                val htmlUrl = json.optString("html_url", "").trim()
                val notes = json.optString("body", "").trim()
                if (tag.isBlank() || htmlUrl.isBlank()) return@withContext null
                if (isNewerVersion(tag, currentVersionName)) {
                    AppReleaseInfo(versionName = tag, releaseUrl = htmlUrl, notes = notes)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun openReleasePage(context: Context, releaseUrl: String) {
        val clean = releaseUrl.trim().ifBlank { "https://github.com/bijithpopy/ITI-Ranni-EM/releases" }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(clean)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val rParts = remote.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val cParts = current.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
