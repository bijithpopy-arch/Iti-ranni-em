package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class AttendanceApiClient {

    companion object {
        const val BASE_URL =
            "https://script.google.com/macros/s/AKfycbyHV65ckL_FdhXCd27TTCxJbgMY5tc0d8Pvifxe60o7aLMe0TMvjzuknotjEvwoDOzRyw/exec"
        const val ESP_KEY = "EMECH_KEY_2026"

        fun todayIstDateString(): String {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            return sdf.format(Date())
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun fetchTraineeData(roll: String, pin: String): ApiResult<TraineeDashboardData> {
        return executeGet(
            mapOf(
                "action" to "trainee_data",
                "roll" to roll.trim(),
                "pin" to pin.trim()
            )
        ) { json ->
            val name = json.optString("name", "Trainee #$roll").ifBlank { "Trainee #$roll" }
            val attendance = parseAttendanceSummary(json.optJSONObject("attendance"))

            val leavesArray = json.optJSONArray("leaves") ?: JSONArray()
            val leaves = buildList {
                for (i in 0 until leavesArray.length()) {
                    val item = leavesArray.optJSONObject(i) ?: continue
                    add(
                        TraineeLeaveItem(
                            id = item.optString("id", item.optString("reqId", "")),
                            date = item.optString("date", "-"),
                            type = item.optString("type", "Out-Pass"),
                            status = item.optString("status", "PENDING_SIR"),
                            pdf = item.optString("pdf", item.optString("pdfUrl", "")).trim()
                        )
                    )
                }
            }

            val attendanceDate = json.optString("attendanceDate", json.optString("date", todayIstDateString()))
                .ifBlank { todayIstDateString() }
            val holidays = parseHolidays(json.optJSONArray("holidays"))
            val todayHoliday = parseSingleHoliday(json.optJSONObject("todayHoliday"))
                ?: holidays.firstOrNull { it.date.equals(attendanceDate, ignoreCase = true) }
            val notices = parseNotices(json.optJSONArray("notices"))
            val pinnedBroadcast = parseSingleNoticeOrNull(json.optJSONObject("pinnedBroadcast"))
                ?: notices.firstOrNull { it.isPinned }
            val myShopFloor = parseSingleShopFloorOrNull(json.optJSONObject("myShopFloor"))
                ?: if (attendance.pct < AttendanceMath.EXAM_THRESHOLD_PCT && attendance.total > 0) {
                    ShopFloorEntry(
                        id = "AUTO-${roll.trim()}",
                        date = attendanceDate,
                        roll = roll.trim(),
                        name = name,
                        attendancePct = attendance.pct,
                        sessionType = "POST_3PM",
                        scheduledEndTime = "05:00 PM",
                        assignedReason = "Auto-flagged (<80% attendance)",
                        status = "SCHEDULED"
                    )
                } else {
                    null
                }

            TraineeDashboardData(
                roll = roll.trim(),
                name = name,
                attendance = attendance,
                leaves = leaves,
                attendanceClosed = json.optAttendanceClosed(),
                attendanceDate = attendanceDate,
                closedAt = json.optString("closedAt", "").trim(),
                todayHoliday = todayHoliday,
                holidays = holidays,
                notices = notices,
                pinnedBroadcast = pinnedBroadcast,
                myShopFloorEntry = myShopFloor,
                scanOverrides = parseScanOverrides(json.optJSONObject("scanOverrides")),
                projectGroupsEnabled = json.optBoolean("projectGroupsEnabled", false),
                projectGroups = parseProjectGroups(json.optJSONArray("projectGroups")),
                dutyItems = parseStringList(json.optJSONArray("dutyItems"))
            )
        }
    }

    suspend fun fetchParentData(roll: String, pin: String): ApiResult<ParentDashboardData> {
        return executeGet(
            mapOf(
                "action" to "parent_data",
                "roll" to roll.trim(),
                "pin" to pin.trim()
            )
        ) { json ->
            val studentName = json.optString("name", json.optString("studentName", "Trainee #$roll"))
                .ifBlank { "Trainee #$roll" }
            val resolvedRoll = json.optSafeString("roll", roll.trim())
            val attendance = parseAttendanceSummary(json.optJSONObject("attendance"))
            val attendanceDate = json.optString("attendanceDate", json.optString("date", todayIstDateString()))
                .ifBlank { todayIstDateString() }
            val holidays = parseHolidays(json.optJSONArray("holidays"))
            val todayHoliday = parseSingleHoliday(json.optJSONObject("todayHoliday"))
                ?: holidays.firstOrNull { it.date.equals(attendanceDate, ignoreCase = true) }
            val notices = parseNotices(json.optJSONArray("notices"))
            val pinnedBroadcast = parseSingleNoticeOrNull(json.optJSONObject("pinnedBroadcast"))
                ?: notices.firstOrNull { it.isPinned }
            val wardShopFloor = parseSingleShopFloorOrNull(json.optJSONObject("wardShopFloor"))
                ?: parseSingleShopFloorOrNull(json.optJSONObject("myShopFloor"))

            ParentDashboardData(
                roll = resolvedRoll,
                studentName = studentName,
                attendance = attendance,
                attendanceClosed = json.optAttendanceClosed(),
                attendanceDate = attendanceDate,
                closedAt = json.optString("closedAt", "").trim(),
                todayHoliday = todayHoliday,
                holidays = holidays,
                notices = notices,
                pinnedBroadcast = pinnedBroadcast,
                wardShopFloorEntry = wardShopFloor,
                parentAlerts = parseParentAlerts(json.optJSONArray("parentAlerts"))
            )
        }
    }

    suspend fun submitParentAlert(
        roll: String,
        pin: String,
        alertType: String,
        message: String
    ): ApiResult<ParentAlertSubmitResult> {
        return executeGet(
            mapOf(
                "action" to "parent_submit_alert",
                "roll" to roll.trim(),
                "pin" to pin.trim(),
                "alertType" to alertType.trim(),
                "message" to message.trim()
            )
        ) { json ->
            val alertId = json.optString("alertId", json.optString("id", "")).trim()
            val msg = json.optString("msg", "Alert sent to Trade Instructor.").ifBlank {
                "Alert sent to Trade Instructor."
            }
            ParentAlertSubmitResult(alertId = alertId, message = msg)
        }
    }

    suspend fun submitLeave(
        roll: String,
        pin: String,
        type: String,
        timeWindow: String,
        reason: String,
        studentSig: String
    ): ApiResult<LeaveSubmitResult> {
        return executeGet(
            mapOf(
                "action" to "submit_leave",
                "roll" to roll.trim(),
                "pin" to pin.trim(),
                "type" to type.trim(),
                "timeWindow" to timeWindow.trim(),
                "reason" to reason.trim(),
                "studentSig" to studentSig.trim()
            )
        ) { json ->
            val reqId = json.optString("reqId", json.optString("id", "")).trim()
            val msg = json.optString("msg", "Out-pass request submitted successfully.").ifBlank {
                "Out-pass request submitted successfully."
            }
            LeaveSubmitResult(reqId = reqId, message = msg)
        }
    }

    suspend fun fetchStaffData(pin: String): ApiResult<StaffDashboardData> {
        return executeGet(
            mapOf(
                "action" to "staff_data",
                "pin" to pin.trim()
            )
        ) { json ->
            val attArray = json.optJSONArray("allAttendance") ?: JSONArray()
            var maxRecordedTotal = 0
            for (i in 0 until attArray.length()) {
                val item = attArray.optJSONObject(i) ?: continue
                val p = item.optSafeInt("present")
                val h = item.optSafeInt("half")
                val t = AttendanceMath.safeTotalDays(p, h, item.optSafeInt("total"))
                if (t > maxRecordedTotal) maxRecordedTotal = t
            }

            val attendanceList = buildList {
                for (i in 0 until attArray.length()) {
                    val item = attArray.optJSONObject(i) ?: continue
                    val present = item.optSafeInt("present").coerceAtLeast(0)
                    val half = item.optSafeInt("half").coerceAtLeast(0)
                    val rawTotal = item.optSafeInt("total")
                    val safeTotal = maxOf(
                        AttendanceMath.safeTotalDays(present, half, rawTotal),
                        if (rawTotal <= 1 && maxRecordedTotal > 1) maxRecordedTotal else 0
                    )
                    val clampedPct = AttendanceMath.computePercentage(
                        present = present,
                        half = half,
                        rawTotal = safeTotal,
                        rawPct = item.optSafeInt("pct")
                    )
                    add(
                        StaffAttendanceRecord(
                            roll = item.optSafeString("roll"),
                            name = item.optString("name", "Unknown Trainee"),
                            todayStatus = item.optString("todayStatus", "ABSENT").ifBlank { "ABSENT" },
                            inT = item.optString("inT", "-").ifBlank { "-" },
                            outT = item.optString("outT", "-").ifBlank { "-" },
                            present = present,
                            half = half,
                            total = safeTotal,
                            pct = clampedPct
                        )
                    )
                }
            }

            val pendingArray = json.optJSONArray("pendingLeaves") ?: JSONArray()
            val pendingList = buildList {
                for (i in 0 until pendingArray.length()) {
                    val item = pendingArray.optJSONObject(i) ?: continue
                    add(
                        PendingLeaveRequest(
                            reqId = item.optString("reqId", item.optString("id", "")),
                            date = item.optString("date", "-"),
                            roll = item.optSafeString("roll"),
                            name = item.optString("name", "Trainee"),
                            type = item.optString("type", "Out-Pass"),
                            timeWindow = item.optString("timeWindow", "-"),
                            reason = item.optString("reason", "-")
                        )
                    )
                }
            }

            val attendanceDate = json.optString("attendanceDate", json.optString("date", todayIstDateString()))
                .ifBlank { todayIstDateString() }
            val holidays = parseHolidays(json.optJSONArray("holidays"))
            val todayHoliday = parseSingleHoliday(json.optJSONObject("todayHoliday"))
                ?: holidays.firstOrNull { it.date.equals(attendanceDate, ignoreCase = true) }
            val notices = parseNotices(json.optJSONArray("notices"))
            val pinnedBroadcast = parseSingleNoticeOrNull(json.optJSONObject("pinnedBroadcast"))
                ?: notices.firstOrNull { it.isPinned }

            val serverShopFloor = parseShopFloorList(json.optJSONArray("shopFloorList"))
            val mergedShopFloor = mergeAutoFlaggedShopFloor(
                attendanceList = attendanceList,
                serverList = serverShopFloor,
                date = attendanceDate
            )

            StaffDashboardData(
                allAttendance = attendanceList,
                pendingLeaves = pendingList,
                parentAlerts = parseParentAlerts(json.optJSONArray("parentAlerts")),
                attendanceClosed = json.optAttendanceClosed(),
                attendanceDate = attendanceDate,
                closedAt = json.optString("closedAt", "").trim(),
                todayHoliday = todayHoliday,
                holidays = holidays,
                notices = notices,
                pinnedBroadcast = pinnedBroadcast,
                shopFloorList = mergedShopFloor,
                scanOverrides = parseScanOverrides(json.optJSONObject("scanOverrides")),
                toolCribLogs = parseToolCribLogs(json.optJSONArray("toolCribLogs")),
                todayDutySignOff = parseDutySignOff(json.optJSONObject("todayDutySignOff")),
                lastScannerSyncTime = json.optString("lastScannerSync", "").trim(),
                projectGroupsEnabled = json.optBoolean("projectGroupsEnabled", false),
                projectGroups = parseProjectGroups(json.optJSONArray("projectGroups"))
            )
        }
    }

    suspend fun setScanOverride(
        pin: String,
        earlyClassLogoutEnabled: Boolean,
        shopFloorReleaseNowEnabled: Boolean
    ): ApiResult<ScanOverrideResult> {
        return executeGet(
            mapOf(
                "action" to "set_scan_override",
                "pin" to pin.trim(),
                "earlyClassLogout" to earlyClassLogoutEnabled.toString(),
                "shopFloorReleaseNow" to shopFloorReleaseNowEnabled.toString()
            )
        ) { json ->
            val state = parseScanOverrides(json.optJSONObject("scanOverrides"))
                .copy(
                    earlyClassLogoutEnabled = json.optBoolean("earlyClassLogoutEnabled", earlyClassLogoutEnabled),
                    shopFloorReleaseNowEnabled = json.optBoolean("shopFloorReleaseNowEnabled", shopFloorReleaseNowEnabled)
                )
            ScanOverrideResult(
                state = state,
                message = json.optString("msg", "Scanner override settings updated.")
                    .ifBlank { "Scanner override settings updated." }
            )
        }
    }

    suspend fun setShopFloorMember(
        pin: String,
        roll: String,
        operation: String,
        sessionType: String = "POST_3PM",
        scheduledEndTime: String = "05:00 PM"
    ): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf(
                "action" to "set_shop_floor_member",
                "pin" to pin.trim(),
                "roll" to roll.trim(),
                "operation" to operation.trim().uppercase(),
                "sessionType" to sessionType.trim().uppercase(),
                "endTime" to scheduledEndTime.trim()
            )
        ) { json ->
            SimpleActionResult(
                message = json.optString("msg", "Shop Floor roster updated for Roll #$roll.")
                    .ifBlank { "Shop Floor roster updated for Roll #$roll." }
            )
        }
    }

    suspend fun triggerShopFloorCardLogout(roll: String): ApiResult<EspTapResult> {
        return executeGet(
            mapOf(
                "action" to "shop_floor_card_logout",
                "key" to ESP_KEY,
                "roll" to roll.trim()
            )
        ) { json ->
            EspTapResult(
                roll = roll.trim(),
                mode = json.optString("mode", "SHOP_FLOOR_OUT").ifBlank { "SHOP_FLOOR_OUT" },
                name = json.optString("name", "Roll #$roll").ifBlank { "Roll #$roll" },
                time = json.optString("checkOutCardScanTime", json.optString("time", "-")).ifBlank { "-" },
                message = json.optString("msg", "Shop Floor ID-card logout recorded.").trim()
            )
        }
    }

    suspend fun verifyShopFloorLogout(
        pin: String,
        roll: String,
        date: String = todayIstDateString()
    ): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf(
                "action" to "verify_shop_floor_logout",
                "pin" to pin.trim(),
                "roll" to roll.trim(),
                "date" to date.trim()
            )
        ) { json ->
            SimpleActionResult(
                message = json.optString("msg", "Shop Floor logout verified for Roll #$roll.")
                    .ifBlank { "Shop Floor logout verified for Roll #$roll." }
            )
        }
    }

    suspend fun bulkMorningRollCall(
        pin: String,
        status: String = "PRESENT"
    ): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf(
                "action" to "bulk_roll_call",
                "pin" to pin.trim(),
                "status" to status.trim().uppercase(),
                "date" to todayIstDateString()
            )
        ) { json ->
            SimpleActionResult(
                message = json.optString("msg", "Bulk morning roll call applied.")
                    .ifBlank { "Bulk morning roll call applied." }
            )
        }
    }

    suspend fun markAttendanceException(
        pin: String,
        roll: String,
        exceptionType: String
    ): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf(
                "action" to "mark_exception",
                "pin" to pin.trim(),
                "roll" to roll.trim(),
                "exceptionType" to exceptionType.trim().uppercase(),
                "date" to todayIstDateString()
            )
        ) { json ->
            SimpleActionResult(
                message = json.optString("msg", "Updated Roll #$roll to $exceptionType.")
                    .ifBlank { "Updated Roll #$roll to $exceptionType." }
            )
        }
    }

    suspend fun setLateGrace(
        pin: String,
        roll: String,
        mode: String,
        cutoff: String = "09:30",
        note: String = ""
    ): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf(
                "action" to "set_late_grace",
                "pin" to pin.trim(),
                "roll" to roll.trim(),
                "mode" to mode.trim().uppercase(),
                "cutoff" to cutoff.trim(),
                "note" to note.trim()
            )
        ) { json ->
            SimpleActionResult(
                message = json.optString("msg", "Late grace updated for Roll #$roll.")
                    .ifBlank { "Late grace updated for Roll #$roll." }
            )
        }
    }

    suspend fun signOffDailyDuty(
        pin: String,
        date: String,
        pairLabel: String,
        note: String
    ): ApiResult<DutySignOffResult> {
        return executeGet(
            mapOf(
                "action" to "sign_off_duty",
                "pin" to pin.trim(),
                "date" to date.trim(),
                "pairLabel" to pairLabel.trim(),
                "note" to note.trim()
            )
        ) { json ->
            val rec = DutySignOffRecord(
                date = date,
                pairLabel = pairLabel,
                signedOff = true,
   
