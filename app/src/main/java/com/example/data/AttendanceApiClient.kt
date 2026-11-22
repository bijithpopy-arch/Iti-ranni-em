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
                "action" to "log_tool_crib",
                "pin" to pin.trim(),
                "item" to itemName.trim(),
                "status" to conditionStatus.trim(),
                "notes" to notes.trim()
            )
        ) { json ->
            val entry = ToolCribLogItem(
                id = json.optString("id", System.currentTimeMillis().toString()).trim(),
                roll = json.optSafeString("roll"),
                name = json.optString("name", "").trim(),
                tool = itemName.trim(),
                status = conditionStatus.trim(),
                time = json.optString("time", todayIstDateString()).trim()
            )
            ToolCribLogResult(
                entry = entry,
                message = json.optString("msg", "Tool crib log recorded.").ifBlank { "Tool crib log recorded." }
            )
        }
    }

    suspend fun sendInstructorParentAlert(
        pin: String,
        roll: String,
        alertType: String,
        message: String
    ): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf(
                "action" to "instructor_parent_alert",
                "pin" to pin.trim(),
                "roll" to roll.trim(),
                "alertType" to alertType.trim(),
                "message" to message.trim()
            )
        ) { json ->
            SimpleActionResult(
                message = json.optString("msg", "Parent alert dispatched for Roll #$roll.")
                    .ifBlank { "Parent alert dispatched for Roll #$roll." }
            )
        }
    }

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
                message = json.optString("msg", "Today's attendance confirmed and locked.")
                    .ifBlank { "Today's attendance confirmed and locked." }
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
            val holiday = HolidayItem(
                date = date.trim(),
                title = reason.trim()
            )
            HolidayPostResult(
                holiday = holiday,
                message = json.optString("msg", "Holiday announced for $date.")
                    .ifBlank { "Holiday announced for $date." }
            )
        }
    }

    suspend fun postNotice(
        pin: String,
        title: String,
        category: String,
        body: String,
        pinned: Boolean = false,
        postedBy: String = "Trade Instructor (EM)"
    ): ApiResult<NoticePostResult> {
        return executeGet(
            mapOf(
                "action" to "post_notice",
                "pin" to pin.trim(),
                "title" to title.trim(),
                "category" to category.trim(),
                "body" to body.trim(),
                "pinned" to pinned.toString(),
                "postedBy" to postedBy.trim()
            )
        ) { json ->
            val notice = NoticeItem(
                id = json.optString("id", System.currentTimeMillis().toString()).trim(),
                title = title.trim(),
                body = body.trim(),
                date = json.optString("date", todayIstDateString()).trim(),
                isPinned = pinned
            )
            NoticePostResult(
                notice = notice,
                message = json.optString("msg", "Notice published.").ifBlank { "Notice published." }
            )
        }
    }

    suspend fun setProjectGroupsAccess(
        pin: String,
        enabled: Boolean
    ): ApiResult<ProjectGroupsAccessResult> {
        return executeGet(
            mapOf(
                "action" to "set_project_groups_access",
                "pin" to pin.trim(),
                "enabled" to enabled.toString()
            )
        ) { json ->
            ProjectGroupsAccessResult(
                enabled = json.optBoolean("enabled", enabled),
                message = json.optString("msg", "Project groups access updated.")
                    .ifBlank { "Project groups access updated." }
            )
        }
    }

    suspend fun fetchProjectGroups(
        pin: String,
        roll: String = ""
    ): ApiResult<ProjectGroupsFetchResult> {
        return executeGet(
            mapOf(
                "action" to "fetch_project_groups",
                "pin" to pin.trim(),
                "roll" to roll.trim()
            )
        ) { json ->
            ProjectGroupsFetchResult(
                enabled = json.optBoolean("projectGroupsEnabled", false),
                groups = parseProjectGroups(json.optJSONArray("projectGroups"))
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
        return executeGet(
            mapOf(
                "action" to "save_project_group",
                "roll" to roll.trim(),
                "pin" to pin.trim(),
                "projectTitle" to projectTitle.trim(),
                "memberRolls" to memberRolls.joinToString(","),
                "leaderRoll" to leaderRoll.toString(),
                "leaderName" to leaderName.trim()
            )
        ) { json ->
            val groupObj = json.optJSONObject("group")
            val group = ProjectGroupItem(
                id = groupObj?.optString("id", "")?.trim().orEmpty(),
                title = groupObj?.optString("title", projectTitle)?.trim().orEmpty(),
                members = groupObj?.optString("members", memberRolls.joinToString(", "))?.trim().orEmpty(),
                status = groupObj?.optString("status", "SUBMITTED")?.trim().orEmpty()
            )
            ProjectGroupSaveResult(
                group = group,
                message = json.optString("msg", "Project group saved.").ifBlank { "Project group saved." }
            )
        }
    }

    suspend fun signByInstructor(
        pin: String,
        reqId: String,
        sirSig: String
    ): ApiResult<LeaveSignResult> {
        return executeGet(
            mapOf(
                "action" to "sign_by_instructor",
                "pin" to pin.trim(),
                "reqId" to reqId.trim(),
                "sirSig" to sirSig.trim()
            )
        ) { json ->
            LeaveSignResult(
                pdfUrl = json.optString("pdfUrl", json.optString("pdf", "")).trim(),
                message = json.optString("msg", "Approved and signed by Trade Instructor.")
                    .ifBlank { "Approved and signed by Trade Instructor." }
            )
        }
    }

    suspend fun signByPrincipal(
        pin: String,
        reqId: String,
        principalSig: String
    ): ApiResult<LeaveSignResult> {
        return executeGet(
            mapOf(
                "action" to "sign_by_principal",
                "pin" to pin.trim(),
                "reqId" to reqId.trim(),
                "principalSig" to principalSig.trim()
            )
        ) { json ->
            LeaveSignResult(
                pdfUrl = json.optString("pdfUrl", json.optString("pdf", "")).trim(),
                message = json.optString("msg", "Approved and signed by Principal.")
                    .ifBlank { "Approved and signed by Principal." }
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
                message = json.optString("msg", "Leave request rejected.")
                    .ifBlank { "Leave request rejected." }
            )
        }
    }

    suspend fun exportDailyAttendancePdf(pin: String): ApiResult<PdfExportResult> {
        return executeGet(
            mapOf(
                "action" to "export_daily_pdf",
                "pin" to pin.trim(),
                "date" to todayIstDateString()
            )
        ) { json ->
            PdfExportResult(
                pdfUrl = json.optString("pdfUrl", json.optString("url", "")).trim()
            )
        }
    }

    suspend fun triggerEspTap(roll: String): ApiResult<EspTapResult> {
        return executeGet(
            mapOf(
                "action" to "esp_tap",
                "key" to ESP_KEY,
                "roll" to roll.trim()
            )
        ) { json ->
            EspTapResult(
                roll = roll.trim(),
                mode = json.optString("mode", "IN").ifBlank { "IN" },
                name = json.optString("name", "Roll #$roll").ifBlank { "Roll #$roll" },
                time = json.optString("time", "-").ifBlank { "-" },
                message = json.optString("msg", "ID-card tap recorded.").trim()
            )
        }
    }

    private suspend fun <T> executeGet(
        params: Map<String, String>,
        parser: (JSONObject) -> T
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = BASE_URL.toHttpUrl().newBuilder()
            params.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }
            val request = Request.Builder().url(urlBuilder.build()).get().build()
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext ApiResult.Error("Server error (${response.code}).")
                }
                val json = JSONObject(raw)
                if (!json.optBoolean("ok", false)) {
                    val errMsg = json.optString("msg", json.optString("error", "Request not found"))
                        .ifBlank { "Request not found" }
                    return@withContext ApiResult.Error(errMsg)
                }
                ApiResult.Success(parser(json))
            }
        } catch (e: IOException) {
            ApiResult.Error("Network error: ${e.localizedMessage ?: "check connection"}")
        } catch (e: Exception) {
            ApiResult.Error("Response error: ${e.localizedMessage ?: "invalid data"}")
        }
    }

    private fun parseAttendanceSummary(obj: JSONObject?): AttendanceSummary {
        if (obj == null) return AttendanceSummary()
        val present = obj.optSafeInt("present").coerceAtLeast(0)
        val half = obj.optSafeInt("half").coerceAtLeast(0)
        val total = AttendanceMath.safeTotalDays(present, half, obj.optSafeInt("total"))
        val pct = AttendanceMath.computePercentage(present, half, total, obj.optSafeInt("pct"))
        return AttendanceSummary(
            todayStatus = obj.optString("todayStatus", "ABSENT").ifBlank { "ABSENT" },
            inT = obj.optString("inT", "-").ifBlank { "-" },
            outT = obj.optString("outT", "-").ifBlank { "-" },
            present = present,
            half = half,
            total = total,
            pct = pct
        )
    }

    private fun parseHolidays(arr: JSONArray?): List<HolidayItem> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(
                    HolidayItem(
                        date = item.optString("date", "").trim(),
                        title = item.optString("title", item.optString("name", "Holiday")).trim()
                    )
                )
            }
        }
    }

    private fun parseSingleHoliday(obj: JSONObject?): HolidayItem? {
        if (obj == null) return null
        val title = obj.optString("title", obj.optString("name", "")).trim()
        val date = obj.optString("date", "").trim()
        if (title.isBlank() && date.isBlank()) return null
        return HolidayItem(date = date, title = title.ifBlank { "Holiday" })
    }

    private fun parseNotices(arr: JSONArray?): List<NoticeItem> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(
                    NoticeItem(
                        id = item.optString("id", i.toString()).trim(),
                        title = item.optString("title", "Notice").trim(),
                        body = item.optString("body", item.optString("message", "")).trim(),
                        date = item.optString("date", "").trim(),
                        isPinned = item.optBoolean("isPinned", item.optBoolean("pinned", false))
                    )
                )
            }
        }
    }

    private fun parseSingleNoticeOrNull(obj: JSONObject?): NoticeItem? {
        if (obj == null) return null
        val title = obj.optString("title", "").trim()
        val body = obj.optString("body", obj.optString("message", "")).trim()
        if (title.isBlank() && body.isBlank()) return null
        return NoticeItem(
            id = obj.optString("id", "pinned").trim(),
            title = title.ifBlank { "Announcement" },
            body = body,
            date = obj.optString("date", "").trim(),
            isPinned = true
        )
    }

    private fun parseShopFloorList(arr: JSONArray?): List<ShopFloorEntry> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(
                    ShopFloorEntry(
                        id = item.optString("id", "").trim(),
                        date = item.optString("date", "").trim(),
                        roll = item.optSafeString("roll"),
                        name = item.optString("name", "Trainee").trim(),
                        attendancePct = item.optSafeInt("attendancePct"),
                        sessionType = item.optString("sessionType", "POST_3PM").trim(),
                        scheduledEndTime = item.optString("scheduledEndTime", "05:00 PM").trim(),
                        assignedReason = item.optString("assignedReason", "").trim(),
                        status = item.optString("status", "SCHEDULED").trim()
                    )
                )
            }
        }
    }

    private fun parseSingleShopFloorOrNull(obj: JSONObject?): ShopFloorEntry? {
        if (obj == null) return null
        val roll = obj.optSafeString("roll")
        if (roll.isBlank()) return null
        return ShopFloorEntry(
            id = obj.optString("id", "").trim(),
            date = obj.optString("date", "").trim(),
            roll = roll,
            name = obj.optString("name", "Trainee").trim(),
            attendancePct = obj.optSafeInt("attendancePct"),
            sessionType = obj.optString("sessionType", "POST_3PM").trim(),
            scheduledEndTime = obj.optString("scheduledEndTime", "05:00 PM").trim(),
            assignedReason = obj.optString("assignedReason", "").trim(),
            status = obj.optString("status", "SCHEDULED").trim()
        )
    }

    private fun mergeAutoFlaggedShopFloor(
        attendanceList: List<StaffAttendanceRecord>,
        serverList: List<ShopFloorEntry>,
        date: String
    ): List<ShopFloorEntry> {
        val byRoll = linkedMapOf<String, ShopFloorEntry>()
        serverList.forEach { byRoll[it.roll] = it }
        attendanceList.filter { it.pct < AttendanceMath.EXAM_THRESHOLD_PCT && it.total > 0 }.forEach { rec ->
            if (!byRoll.containsKey(rec.roll)) {
                byRoll[rec.roll] = ShopFloorEntry(
                    id = "AUTO-${rec.roll}",
                    date = date,
                    roll = rec.roll,
                    name = rec.name,
                    attendancePct = rec.pct,
                    sessionType = "POST_3PM",
                    scheduledEndTime = "05:00 PM",
                    assignedReason = "Auto-flagged (<80% attendance)",
                    status = "SCHEDULED"
                )
            }
        }
        return byRoll.values.toList()
    }

    private fun parseScanOverrides(obj: JSONObject?): ScanOverrideState {
        if (obj == null) return ScanOverrideState()
        return ScanOverrideState(
            earlyClassLogoutEnabled = obj.optBoolean("earlyClassLogoutEnabled", obj.optBoolean("allowEarlyLogout", false)),
            shopFloorReleaseNowEnabled = obj.optBoolean("shopFloorReleaseNowEnabled", obj.optBoolean("releaseShopFloorNow", false))
        )
    }

    private fun parseToolCribLogs(arr: JSONArray?): List<ToolCribLogItem> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(
                    ToolCribLogItem(
                        id = item.optString("id", "").trim(),
                        roll = item.optSafeString("roll"),
                        name = item.optString("name", "").trim(),
                        tool = item.optString("tool", item.optString("item", "")).trim(),
                        status = item.optString("status", "").trim(),
                        time = item.optString("time", "").trim()
                    )
                )
            }
        }
    }

    private fun parseDutySignOff(obj: JSONObject?): DutySignOffRecord? {
        if (obj == null) return null
        val signedOff = obj.optBoolean("signedOff", false)
        val signedAt = obj.optString("signedAt", "").trim()
        if (!signedOff && signedAt.isBlank()) return null
        return DutySignOffRecord(
            date = obj.optString("date", "").trim(),
            pairLabel = obj.optString("pairLabel", "").trim(),
            signedOff = signedOff,
            signedAt = signedAt,
            note = obj.optString("note", "").trim()
        )
    }

    private fun parseParentAlerts(arr: JSONArray?): List<ParentAlertItem> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                add(
                    ParentAlertItem(
                        id = item.optString("id", item.optString("alertId", "")).trim(),
                        roll = item.optSafeString("roll"),
           
   
