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
            // Determine cohort max total days so a corrupt Total: 1 never skews percentage to 3500%
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
                    val lateGraceMode = item.optString(
                        "lateGraceMode",
                        item.optString("late_grace_mode", "")
                    ).trim().uppercase()
                    val lateCutoff = item.optString(
                        "lateCutoff",
                        item.optString("late_cutoff", "")
                    ).trim()
                    val lateGraceNote = item.optString(
                        "lateGraceNote",
                        item.optString("late_grace_note", "")
                    ).trim()
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
                            pct = clampedPct,
                            lateGraceMode = lateGraceMode,
                            lateCutoff = lateCutoff,
                            lateGraceNote = lateGraceNote
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

    // ---------- Shop Floor & Instructor Scan Override Endpoints ----------

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
        operation: String, // "ADD" or "REMOVE"
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

    // ---------- Bulk Roll Call, Quick Exceptions, Duty & Tool Crib, Instant Parent Alert ----------

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
        exceptionType: String // "LATE", "HALF-DAY", "ON-DUTY", "PRESENT", "ABSENT"
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
        cutoff: String = "",
        note: String = "",
        date: String = todayIstDateString()
    ): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf(
                "action" to "set_late_grace",
                "pin" to pin.trim(),
                "roll" to roll.trim(),
                "mode" to mode.trim().uppercase(),
                "cutoff" to cutoff.trim(),
                "note" to note.trim(),
                "date" to date.trim()
            )
        ) { json ->
            val fallback = when (mode.trim().uppercase()) {
                LateGraceModes.CUSTOM_CUTOFF -> "Late grace (${cutoff.ifBlank { "09:30" }}) saved for Roll #$roll."
                LateGraceModes.EXEMPT_TODAY -> "Late exemption saved for Roll #$roll."
                else -> "Late grace cleared for Roll #$roll."
            }
            SimpleActionResult(
                message = json.optString("msg", fallback).ifBlank { fallback }
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
                signedAt = json.optString("signedAt", "").trim(),
                note = note.trim()
            )
            DutySignOffResult(
                record = rec,
                message = json.optString("msg", "Duty verified & signed off for $pairLabel.")
                    .ifBlank { "Duty verified & signed off for $pairLabel." }
            )
        }
    }

    suspend fun logToolCribIssue(
        pin: String,
        itemName: String,
        conditionStatus: String,
        notes: String
    ): ApiResult<ToolCribLogResult> {
        return executeGet(
            mapOf(
                "action" to "log_tool_crib_issue",
                "pin" to pin.trim(),
                "itemName" to itemName.trim(),
                "conditionStatus" to conditionStatus.trim().uppercase(),
                "notes" to notes.trim()
            )
        ) { json ->
            val entry = ToolCribLogEntry(
                id = json.optSafeString("id", "TC-${System.currentTimeMillis()}"),
                date = json.optString("date", todayIstDateString()),
                time = json.optString("time", ""),
                itemName = itemName.trim(),
                conditionStatus = conditionStatus.trim().uppercase(),
                notes = notes.trim(),
                reportedBy = json.optString("reportedBy", "Trade Instructor")
            )
            ToolCribLogResult(
                entry = entry,
                message = json.optString("msg", "Tool Crib / Lab equipment log recorded.")
                    .ifBlank { "Tool Crib / Lab equipment log recorded." }
            )
        }
    }

    suspend fun sendInstructorParentAlert(
        pin: String,
        roll: String,
        alertType: String,
        message: String
    ): ApiResult<ParentAlertSubmitResult> {
        return executeGet(
            mapOf(
                "action" to "send_instructor_parent_alert",
                "pin" to pin.trim(),
                "roll" to roll.trim(),
                "alertType" to alertType.trim().uppercase(),
                "message" to message.trim()
            )
        ) { json ->
            ParentAlertSubmitResult(
                alertId = json.optSafeString("alertId", json.optSafeString("id", "")),
                message = json.optString("msg", "Parent warning alert logged for Roll #$roll.")
                    .ifBlank { "Parent warning alert logged for Roll #$roll." }
            )
        }
    }

    suspend fun rejectLeaveRequest(
        pin: String,
        reqId: String,
        reason: String
    ): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf(
                "action" to "reject_leave",
                "pin" to pin.trim(),
                "reqId" to reqId.trim(),
                "reason" to reason.trim()
            )
        ) { json ->
            SimpleActionResult(
                message = json.optString("msg", "Request $reqId rejected.")
                    .ifBlank { "Request $reqId rejected." }
            )
        }
    }

    // ---------- Existing Core Actions ----------

    suspend fun confirmCloseAttendance(
        pin: String,
        date: String = todayIstDateString()
    ): ApiResult<CloseAttendanceResult> {
        return executeGet(
            mapOf(
                "action" to "confirm_close_attendance",
                "pin" to pin.trim(),
                "date" to date.trim()
            )
        ) { json ->
            CloseAttendanceResult(
                attendanceClosed = json.optBoolean("attendanceClosed", true),
                date = json.optString("date", date).ifBlank { date },
                closedAt = json.optString("closedAt", "").trim(),
                absentCount = json.optSafeInt("absentCount"),
                halfDayCount = json.optSafeInt("halfDayCount"),
                message = json.optString(
                    "msg",
                    "Today's attendance confirmed & closed. Parent alerts for Absent & Half-Day are now active."
                ).ifBlank {
                    "Today's attendance confirmed & closed. Parent alerts for Absent & Half-Day are now active."
                }
            )
        }
    }

    suspend fun postHoliday(
        pin: String,
        date: String,
        reason: String
    ): ApiResult<HolidayPostResult> {
        return executeGet(
            mapOf(
                "action" to "post_holiday",
                "pin" to pin.trim(),
                "date" to date.trim(),
                "reason" to reason.trim()
            )
        ) { json ->
            val holidayObj = json.optJSONObject("holiday")
            val holiday = parseSingleHoliday(holidayObj) ?: HolidayAnnouncement(
                id = json.optSafeString("id", "HOL-$date"),
                date = date.trim(),
                reason = reason.trim(),
                postedBy = json.optString("postedBy", "Trade Instructor"),
                postedAt = json.optString("postedAt", "")
            )
            HolidayPostResult(
                holiday = holiday,
                message = json.optString("msg", "Holiday announcement published for $date.")
                    .ifBlank { "Holiday announcement published for $date." }
            )
        }
    }

    suspend fun postNotice(
        pin: String,
        title: String,
        category: String,
        body: String,
        date: String = todayIstDateString(),
        pinned: Boolean = false,
        postedBy: String = "Trade Instructor"
    ): ApiResult<NoticePostResult> {
        return executeGet(
            mapOf(
                "action" to "post_notice",
                "pin" to pin.trim(),
                "title" to title.trim(),
                "category" to category.trim(),
                "body" to body.trim(),
                "date" to date.trim(),
                "pinned" to pinned.toString(),
                "postedBy" to postedBy.trim()
            )
        ) { json ->
            val noticeObj = json.optJSONObject("notice")
            val notice = if (noticeObj != null) {
                parseSingleNotice(noticeObj)
            } else {
                NoticeItem(
                    id = json.optSafeString("noticeId", json.optSafeString("id", "NTC-${System.currentTimeMillis()}")),
                    title = title.trim(),
                    date = date.trim(),
                    category = category.trim(),
                    body = body.trim(),
                    postedBy = json.optString("postedBy", postedBy),
                    isPinned = pinned
                )
            }
            NoticePostResult(
                notice = notice,
                message = json.optString("msg", if (pinned) "Pinned Principal Broadcast published to all dashboards." else "Notice published to students & parents.")
                    .ifBlank { "Notice published." }
            )
        }
    }

    suspend fun setProjectGroupsAccess(
        pin: String,
        enabled: Boolean
    ): ApiResult<ProjectAccessResult> {
        return executeGet(
            mapOf(
                "action" to "set_project_groups_access",
                "pin" to pin.trim(),
                "enabled" to enabled.toString()
            )
        ) { json ->
            val resolvedEnabled = json.optBoolean("projectGroupsEnabled", json.optBoolean("enabled", enabled))
            ProjectAccessResult(
                enabled = resolvedEnabled,
                message = json.optString(
                    "msg",
                    if (resolvedEnabled) "Project Groups tab enabled for trainees." else "Project Groups tab locked for trainees."
                ).ifBlank {
                    if (resolvedEnabled) "Project Groups tab enabled for trainees." else "Project Groups tab locked for trainees."
                }
            )
        }
    }

    suspend fun fetchProjectGroups(
        pin: String,
        roll: String = ""
    ): ApiResult<ProjectGroupsPayload> {
        val params = mutableMapOf(
            "action" to "project_groups_data",
            "pin" to pin.trim()
        )
        if (roll.isNotBlank()) {
            params["roll"] = roll.trim()
        }
        return executeGet(params) { json ->
            ProjectGroupsPayload(
                enabled = json.optBoolean("projectGroupsEnabled", json.optBoolean("enabled", false)),
                groups = parseProjectGroups(json.optJSONArray("projectGroups") ?: json.optJSONArray("groups"))
            )
        }
    }

    suspend fun saveProjectGroup(
        roll: String,
        pin: String,
        projectTitle: String,
        memberRolls: List<Int>,
        leaderRoll: Int,
        leaderName: String
    ): ApiResult<ProjectGroupSaveResult> {
        val csvMembers = memberRolls.distinct().sorted().joinToString(",")
        return executeGet(
            mapOf(
                "action" to "save_project_group",
                "roll" to roll.trim(),
                "pin" to pin.trim(),
                "projectTitle" to projectTitle.trim(),
                "members" to csvMembers,
                "leaderRoll" to leaderRoll.toString(),
                "leaderName" to leaderName.trim()
            )
        ) { json ->
            val groupObj = json.optJSONObject("group")
            val group = if (groupObj != null) {
                parseSingleProjectGroup(groupObj)
            } else {
                ProjectGroup(
                    groupId = json.optSafeString("groupId", json.optSafeString("id", "PG-${System.currentTimeMillis()}")),
                    projectTitle = projectTitle.trim(),
                    leaderRoll = leaderRoll.toString(),
                    leaderName = leaderName.trim(),
                    memberRolls = memberRolls.distinct().sorted().map { it.toString() },
                    submittedByRoll = roll.trim(),
                    createdAt = todayIstDateString()
                )
            }
            ProjectGroupSaveResult(
                group = group,
                message = json.optString("msg", "Project group saved successfully.")
                    .ifBlank { "Project group saved successfully." }
            )
        }
    }

    suspend fun signByInstructor(
        pin: String,
        reqId: String,
        sirSig: String
    ): ApiResult<SignApprovalResult> {
        return executeGet(
            mapOf(
                "action" to "sign_sir",
                "pin" to pin.trim(),
                "reqId" to reqId.trim(),
                "sirSig" to sirSig.trim()
            )
        ) { json ->
            SignApprovalResult(
                reqId = reqId,
                message = json.optString("msg", "Approved by Trade Instructor.").ifBlank {
                    "Approved by Trade Instructor."
                },
                pdfUrl = json.optString("pdfUrl", json.optString("pdf", "")).trim()
            )
        }
    }

    suspend fun signByPrincipal(
        pin: String,
        reqId: String,
        principalSig: String
    ): ApiResult<SignApprovalResult> {
        return executeGet(
            mapOf(
                "action" to "sign_principal",
                "pin" to pin.trim(),
                "reqId" to reqId.trim(),
                "principalSig" to principalSig.trim()
            )
        ) { json ->
            SignApprovalResult(
                reqId = reqId,
                message = json.optString("msg", "Approved & Signed by Principal.").ifBlank {
                    "Approved & Signed by Principal."
                },
                pdfUrl = json.optString("pdfUrl", json.optString("pdf", "")).trim()
            )
        }
    }

    suspend fun exportDailyAttendancePdf(pin: String): ApiResult<PdfExportResult> {
        return executeGet(
            mapOf(
                "action" to "print_attendance",
                "pin" to pin.trim()
            )
        ) { json ->
            val pdfUrl = json.optString("pdfUrl", json.optString("pdf", "")).trim()
            val msg = json.optString("msg", "Daily Attendance PDF generated.").ifBlank {
                "Daily Attendance PDF generated."
            }
            PdfExportResult(pdfUrl = pdfUrl, message = msg)
        }
    }

    suspend fun triggerEspTap(roll: String): ApiResult<EspTapResult> {
        return executeGet(
            mapOf(
                "action" to "tap",
                "key" to ESP_KEY,
                "roll" to roll.trim()
            )
        ) { json ->
            EspTapResult(
                roll = roll.trim(),
                mode = json.optString("mode", "PRESENT").ifBlank { "PRESENT" },
                name = json.optString("name", "Roll #$roll").ifBlank { "Roll #$roll" },
                time = json.optString("time", "-").ifBlank { "-" },
                message = json.optString("msg", "").trim()
            )
        }
    }

    // ---------- Parsers with Strict AttendanceMath Guard ----------

    private fun parseAttendanceSummary(attObj: JSONObject?): AttendanceSummary {
        val obj = attObj ?: JSONObject()
        val present = obj.optSafeInt("present").coerceAtLeast(0)
        val half = obj.optSafeInt("half").coerceAtLeast(0)
        val rawTotal = obj.optSafeInt("total")
        val safeTotal = AttendanceMath.safeTotalDays(present, half, rawTotal)
        val clampedPct = AttendanceMath.computePercentage(
            present = present,
            half = half,
            rawTotal = safeTotal,
            rawPct = obj.optSafeInt("pct")
        )
        return AttendanceSummary(
            todayStatus = obj.optString("todayStatus", "ABSENT").ifBlank { "ABSENT" },
            inT = obj.optString("inT", "-").ifBlank { "-" },
            outT = obj.optString("outT", "-").ifBlank { "-" },
            present = present,
            half = half,
            total = safeTotal,
            pct = clampedPct,
            streakDays = obj.optSafeInt("streakDays", present.coerceAtMost(15))
        )
    }

    private fun mergeAutoFlaggedShopFloor(
        attendanceList: List<StaffAttendanceRecord>,
        serverList: List<ShopFloorEntry>,
        date: String
    ): List<ShopFloorEntry> {
        val byRoll = LinkedHashMap<String, ShopFloorEntry>()
        serverList.forEach { entry ->
            byRoll[entry.roll] = entry
        }
        attendanceList.forEach { rec ->
            if (rec.pct < AttendanceMath.EXAM_THRESHOLD_PCT && rec.total > 0 && !byRoll.containsKey(rec.roll)) {
                byRoll[rec.roll] = ShopFloorEntry(
                    id = "AUTO-${rec.roll}-$date",
                    date = date,
                    roll = rec.roll,
                    name = rec.name,
                    attendancePct = rec.pct,
                    sessionType = "POST_3PM",
                    scheduledEndTime = "05:00 PM",
                    assignedReason = "Auto (<80%)",
                    status = "SCHEDULED",
                    isManualOverride = false
                )
            }
        }
        return byRoll.values.sortedBy { it.attendancePct }
    }

    private fun parseSingleShopFloorOrNull(obj: JSONObject?): ShopFloorEntry? {
        if (obj == null) return null
        val roll = obj.optSafeString("roll")
        if (roll.isBlank()) return null
        return ShopFloorEntry(
            id = obj.optSafeString("id", "SF-$roll"),
            date = obj.optString("date", todayIstDateString()).ifBlank { todayIstDateString() },
            roll = roll,
            name = obj.optString("name", "Roll #$roll").ifBlank { "Roll #$roll" },
            attendancePct = obj.optSafeInt("attendancePct", obj.optSafeInt("pct")).coerceIn(0, 100),
            sessionType = obj.optString("sessionType", "POST_3PM").ifBlank { "POST_3PM" },
            scheduledEndTime = obj.optString("scheduledEndTime", obj.optString("endTime", "05:00 PM")).ifBlank { "05:00 PM" },
            assignedReason = obj.optString("assignedReason", obj.optString("reason", "Auto (<80%)")).ifBlank { "Auto (<80%)" },
            status = obj.optString("status", "SCHEDULED").ifBlank { "SCHEDULED" },
            checkInTime = obj.optString("checkInTime", "03:00 PM").ifBlank { "03:00 PM" },
            checkOutCardScanTime = obj.optString("checkOutCardScanTime", obj.optString("Check_Out_Card_Scan_Time", "")).trim(),
            durationMinutes = obj.optSafeInt("durationMinutes"),
            verifiedByInstructor = obj.optBoolean("verifiedByInstructor", false),
            isManualOverride = obj.optBoolean("isManualOverride", false)
        )
    }

    private fun parseShopFloorList(array: JSONArray?): List<ShopFloorEntry> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val parsed = parseSingleShopFloorOrNull(array.optJSONObject(i)) ?: continue
                add(parsed)
            }
        }
    }

    private fun parseScanOverrides(obj: JSONObject?): ScanOverrideState {
        if (obj == null) return ScanOverrideState()
        return ScanOverrideState(
            earlyClassLogoutEnabled = obj.optBoolean("earlyClassLogoutEnabled", obj.optBoolean("earlyClassLogout", false)),
            shopFloorReleaseNowEnabled = obj.optBoolean("shopFloorReleaseNowEnabled", obj.optBoolean("shopFloorReleaseNow", false)),
            updatedAt = obj.optString("updatedAt", "").trim(),
            updatedBy = obj.optString("updatedBy", "").trim()
        )
    }

    private fun parseToolCribLogs(array: JSONArray?): List<ToolCribLogEntry> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val name = item.optString("itemName", "").trim()
                if (name.isBlank()) continue
                add(
                    ToolCribLogEntry(
                        id = item.optSafeString("id", "TC-$i"),
                        date = item.optString("date", todayIstDateString()),
                        time = item.optString("time", ""),
                        itemName = name,
                        conditionStatus = item.optString("conditionStatus", "DAMAGED").ifBlank { "DAMAGED" },
                        notes = item.optString("notes", "").trim(),
                        reportedBy = item.optString("reportedBy", "Trade Instructor")
                    )
                )
            }
        }
    }

    private fun parseDutySignOff(obj: JSONObject?): DutySignOffRecord? {
        if (obj == null) return null
        val date = obj.optString("date", "").trim()
        val pair = obj.optString("pairLabel", "").trim()
        if (date.isBlank() && pair.isBlank()) return null
        return DutySignOffRecord(
            date = date.ifBlank { todayIstDateString() },
            pairLabel = pair,
            signedOff = obj.optBoolean("signedOff", false),
            signedAt = obj.optString("signedAt", "").trim(),
            note = obj.optString("note", "").trim()
        )
    }

    private fun parseParentAlerts(array: JSONArray?): List<ParentAlertItem> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(
                    ParentAlertItem(
                        id = item.optSafeString("id", item.optSafeString("alertId", "PA-$i")),
                        date = item.optString("date", "-").ifBlank { "-" },
                        time = item.optString("time", "").trim(),
                        roll = item.optSafeString("roll"),
                        studentName = item.optString("studentName", item.optString("name", "Trainee"))
                            .ifBlank { "Trainee" },
                        alertType = item.optString("alertType", item.optString("type", ParentAlertTypes.LEAVE_REASON))
                            .ifBlank { ParentAlertTypes.LEAVE_REASON },
                        message = item.optString("message", item.optString("reason", "")).trim()
                    )
                )
            }
        }
    }

    private fun parseSingleHoliday(obj: JSONObject?): HolidayAnnouncement? {
        if (obj == null) return null
        val date = obj.optString("date", "").trim()
        val reason = obj.optString("reason", "").trim()
        if (date.isBlank() && reason.isBlank()) return null
        return HolidayAnnouncement(
            id = obj.optSafeString("id", "HOL-$date"),
            date = date.ifBlank { todayIstDateString() },
            reason = reason.ifBlank { "Declared Holiday" },
            postedBy = obj.optString("postedBy", "Trade Instructor").ifBlank { "Trade Instructor" },
            postedAt = obj.optString("postedAt", "").trim()
        )
    }

    private fun parseHolidays(array: JSONArray?): List<HolidayAnnouncement> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val parsed = parseSingleHoliday(array.optJSONObject(i)) ?: continue
                add(parsed)
            }
        }
    }

    private fun parseSingleNoticeOrNull(obj: JSONObject?): NoticeItem? {
        if (obj == null) return null
        val title = obj.optString("title", "").trim()
        val body = obj.optString("body", obj.optString("message", "")).trim()
        if (title.isBlank() && body.isBlank()) return null
        return parseSingleNotice(obj)
    }

    private fun parseSingleNotice(item: JSONObject): NoticeItem {
        return NoticeItem(
            id = item.optSafeString("id", item.optSafeString("noticeId", "NTC-${item.hashCode()}")),
            title = item.optString("title", "Trade Notice").ifBlank { "Trade Notice" },
            date = item.optString("date", todayIstDateString()).ifBlank { todayIstDateString() },
            category = item.optString("category", item.optString("tag", "General")).ifBlank { "General" },
            body = item.optString("body", item.optString("message", "")).trim(),
            postedBy = item.optString("postedBy", "Govt. ITI Ranni").ifBlank { "Govt. ITI Ranni" },
            isPinned = item.optBoolean("isPinned", item.optBoolean("pinned", false))
        )
    }

    private fun parseNotices(array: JSONArray?): List<NoticeItem> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(parseSingleNotice(obj))
            }
        }
    }

    private fun parseSingleProjectGroup(item: JSONObject): ProjectGroup {
        val membersRaw = item.opt("members")
        val memberList = when (membersRaw) {
            is JSONArray -> buildList {
                for (i in 0 until membersRaw.length()) {
                    val v = membersRaw.opt(i)?.toString()?.trim().orEmpty()
                    if (v.isNotBlank()) add(v)
                }
            }
            is String -> membersRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
            else -> emptyList()
        }
        return ProjectGroup(
            groupId = item.optSafeString("groupId", item.optSafeString("id", "PG-${item.hashCode()}")),
            projectTitle = item.optString("projectTitle", item.optString("title", "Electronics Project"))
                .ifBlank { "Electronics Project" },
            leaderRoll = item.optSafeString("leaderRoll"),
            leaderName = item.optString("leaderName", "").trim(),
            memberRolls = memberList,
            submittedByRoll = item.optSafeString("submittedByRoll"),
            createdAt = item.optString("createdAt", item.optString("date", "")).trim()
        )
    }

    private fun parseProjectGroups(array: JSONArray?): List<ProjectGroup> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(parseSingleProjectGroup(obj))
            }
        }
    }

    private fun parseStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val s = array.optString(i, "").trim()
                if (s.isNotBlank()) add(s)
            }
        }
    }

    private suspend fun <T> executeGet(
        params: Map<String, String>,
        parser: (JSONObject) -> T
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = BASE_URL.toHttpUrl().newBuilder()
            params.forEach { (key, value) ->
                urlBuilder.addQueryParameter(key, value)
            }
            val request = Request.Builder()
                .url(urlBuilder.build())
                .get()
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string()?.trim().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext ApiResult.Error(
                        "Server responded with HTTP ${response.code}. Please try again."
                    )
                }
                if (rawBody.isEmpty()) {
                    return@withContext ApiResult.Error(
                        "Empty response received from Attendance Server."
                    )
                }
                if (!rawBody.startsWith("{")) {
                    return@withContext ApiResult.Error(
                        "Unexpected response format from server. Please check network or script deployment."
                    )
                }

                val json = JSONObject(rawBody)
                val ok = json.optBoolean("ok", false)
                if (!ok) {
                    val errorMsg = json.optString("msg", "")
                        .ifBlank { json.optString("error", "") }
                        .ifBlank { "Request could not be completed. Please verify credentials." }
                    return@withContext ApiResult.Error(errorMsg)
                }

                ApiResult.Success(parser(json))
            }
        } catch (e: IOException) {
            ApiResult.Error("Network error: Unable to reach Govt. ITI Ranni server. Check internet connection.")
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "An unexpected error occurred while communicating with the server.")
        }
    }

    private fun JSONObject.optAttendanceClosed(): Boolean {
        return when {
            has("attendanceClosed") -> optBoolean("attendanceClosed", false)
            has("isClosed") -> optBoolean("isClosed", false)
            has("todayClosed") -> optBoolean("todayClosed", false)
            has("closed") -> optBoolean("closed", false)
            else -> false
        }
    }

    private fun JSONObject.optSafeInt(key: String, default: Int = 0): Int {
        if (!has(key) || isNull(key)) return default
        val raw = opt(key)
        return when (raw) {
            is Number -> raw.toDouble().roundToInt()
            is String -> raw.trim().toDoubleOrNull()?.roundToInt() ?: default
            else -> default
        }
    }

    private fun JSONObject.optSafeString(key: String, default: String = ""): String {
        if (!has(key) || isNull(key)) return default
        val raw = opt(key)
        return raw?.toString()?.trim()?.ifBlank { default } ?: default
    }
}
