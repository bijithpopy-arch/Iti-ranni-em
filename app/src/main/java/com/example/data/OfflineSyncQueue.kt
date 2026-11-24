package com.example.data

import android.content.Context
import com.example.ui.screens.ExamMarkRow
import org.json.JSONArray
import org.json.JSONObject

data class QueuedActionItem(
    val id: String,
    val actionType: String,
    val createdAtMillis: Long,
    val payloadJson: String
)

object OfflineSyncQueue {
    private const val PREFS_NAME = "emech_offline_queue_prefs"
    private const val KEY_QUEUE = "pending_actions_json"
    private const val KEY_MARKS_DRAFT = "offline_marks_draft_json"

    fun enqueue(context: Context, actionType: String, payload: Map<String, String>): QueuedActionItem {
        val item = QueuedActionItem(
            id = "Q-${System.currentTimeMillis()}-${(100..999).random()}",
            actionType = actionType,
            createdAtMillis = System.currentTimeMillis(),
            payloadJson = JSONObject(payload).toString()
        )
        val current = getAll(context).toMutableList()
        current.add(item)
        saveList(context, current)
        return item
    }

    fun getAll(context: Context): List<QueuedActionItem> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_QUEUE, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        QueuedActionItem(
                            id = obj.optString("id"),
                            actionType = obj.optString("actionType"),
                            createdAtMillis = obj.optLong("createdAtMillis", 0L),
                            payloadJson = obj.optString("payloadJson", "{}")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun count(context: Context): Int = getAll(context).size

    fun removeById(context: Context, id: String) {
        val remaining = getAll(context).filterNot { it.id == id }
        saveList(context, remaining)
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_QUEUE)
            .apply()
    }

    private fun saveList(context: Context, items: List<QueuedActionItem>) {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("actionType", item.actionType)
                    put("createdAtMillis", item.createdAtMillis)
                    put("payloadJson", item.payloadJson)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_QUEUE, arr.toString())
            .apply()
    }

    fun saveMarksDraft(context: Context, monthLabel: String, rows: List<ExamMarkRow>) {
        val arr = JSONArray()
        rows.forEach { row ->
            arr.put(
                JSONObject().apply {
                    put("roll", row.roll)
                    put("name", row.name)
                    put("theoryMarks", row.theoryMarks)
                    put("labMarks", row.labMarks)
                    put("remarks", row.remarks)
                    put("published", row.published)
                }
            )
        }
        val wrapper = JSONObject().apply {
            put("monthLabel", monthLabel)
            put("savedAtMillis", System.currentTimeMillis())
            put("rows", arr)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MARKS_DRAFT, wrapper.toString())
            .apply()
    }

    fun loadMarksDraft(context: Context): Pair<String, List<ExamMarkRow>>? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MARKS_DRAFT, null) ?: return null
        return try {
            val wrapper = JSONObject(raw)
            val monthLabel = wrapper.optString("monthLabel", "Monthly Exam")
            val arr = wrapper.optJSONArray("rows") ?: JSONArray()
            val rows = buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(
                        ExamMarkRow(
                            roll = o.optInt("roll", i + 1),
                            name = o.optString("name", "Trainee #${i + 1}"),
                            theoryMarks = o.optString("theoryMarks", ""),
                            labMarks = o.optString("labMarks", ""),
                            remarks = o.optString("remarks", ""),
                            published = o.optBoolean("published", false)
                        )
                    )
                }
            }
            monthLabel to rows
        } catch (_: Exception) {
            null
        }
    }
}
