package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class OfflineQueuedAction(
    val id: String,
    val type: String,
    val summary: String,
    val createdAt: String
)

object OfflineSyncQueue {
    private const val PREFS_NAME = "iti_ranni_offline_queue"
    private const val KEY_QUEUE = "queued_actions"

    fun enqueue(context: Context, type: String, summary: String) {
        val current = getAll(context).toMutableList()
        current.add(
            OfflineQueuedAction(
                id = "OQ-${System.currentTimeMillis()}",
                type = type,
                summary = summary,
                createdAt = AttendanceApiClient.todayIstDateString()
            )
        )
        save(context, current)
    }

    fun count(context: Context): Int = getAll(context).size

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_QUEUE)
            .apply()
    }

    fun getAll(context: Context): List<OfflineQueuedAction> {
        val raw = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_QUEUE, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(
                        OfflineQueuedAction(
                            id = o.optString("id"),
                            type = o.optString("type"),
                            summary = o.optString("summary"),
                            createdAt = o.optString("createdAt")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun save(context: Context, items: List<OfflineQueuedAction>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("type", item.type)
                    put("summary", item.summary)
                    put("createdAt", item.createdAt)
                }
            )
        }
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_QUEUE, arr.toString())
            .apply()
    }
}
