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
        const val SHEET_NAME = "EMech_2025_27_Attendance"

        fun todayIstDateString(): String {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            return sdf.format(Date())
        }

        fun sessionsNeededForTargetPct(
            present: Int,
            half: Int,
            total: Int,
            targetPct: Double = 80.0
        ): Int = AttendanceMath.sessionsNeededForTargetPct(present, half, total, targetPct)

        fun formatShopFloorHours(durationMinutes: Int): String =
            AttendanceMath.formatShopFloorHours(durationMinutes)
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
                            date = IstDateTimeFormatter.formatDateOnly(item.optString("date", "-")),
                            type = item.optString("type", "Out-Pass"),
                            status = item.optString("status", "PENDING_SIR"),
                            pdf = item.optString("pdf", item.optString("pdfUrl", "")).trim()
                        )
                    )
                }
            }
            val attendanceDate = IstDateTimeFormatter.formatDateOnly(
                json.optString("attendanceDate", json.optString("date", todayIstDateString())),
                todayIstDateString()
            )
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
                } else null
            val mySubmission = parseSingleAcademicSubmissionOrNull(json.optJSONObject("myAcademicSubmission"))
                ?: parseAcademicSubmissions(json.optJSONArray("academicSubmissions"))
                    .firstOrNull { it.roll.equals(roll.trim(), ignoreCase = true) }

            TraineeDashboardData(
                roll = roll.trim(),
                name = name,
                attendance = attendance,
                leaves = leaves,
                attendanceClosed = json.optAttendanceClosed(),
                attendanceDate = attendanceDate,
                closedAt = IstDateTimeFormatter.formatTimeOnly(json.optString("closedAt", ""), ""),
                todayHoliday = todayHoliday,
                holidays = holidays,
                notices = notices,
                pinnedBroadcast = pinnedBroadcast,
                myShopFloorEntry = myShopFloor,
                scanOverrides = parseScanOverrides(json.optJSONObject("scanOverrides")),
                projectGroupsEnabled = json.optBoolean("projectGroupsEnabled", false),
                projectGroups = parseProjectGroups(json.optJSONArray("projectGroups")),
                dutyItems = parseStringList(json.optJSONArray("dutyItems")),
                monthlyMarks = parseMonthlyMarks(json.optJSONArray("monthlyMarks")),
                myWorkMarkQueries = parseWorkMarkQueries(
                    json.optJSONArray("myWorkMarkQueries") ?: json.optJSONArray("workMarkQueries")
                ).filter { it.roll.equals(roll.trim(), ignoreCase = true) },
                myRunningLateAlerts = parseRunningLateAlerts(
                    json.optJSONArray("myRunningLateAlerts") ?: json.optJSONArray("runningLateAlerts")
                ).filter { it.roll.equals(roll.trim(), ignoreCase = true) },
                myAcademicSubmission = mySubmission,
                dutyOverrides = parseDutyOverrides(json.opt("dutyOverrides")),
                timetable = TimetableStore.parseJsonArray(json.optJSONArray("timetable")),
                traineeTimetableEditEnabled = json.optBoolean("traineeTimetableEditEnabled", false),
                publishedReportUrl = json.optString("publishedReportUrl", json.optString("attendancePdfUrl", "")).trim(),
                publishedReportAt = IstDateTimeFormatter.formatDateTime(json.optString("publishedReportAt", ""), "")
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
            val attendanceDate = IstDateTimeFormatter.formatDateOnly(
                json.optString("attendanceDate", json.optString("date", todayIstDateString())),
                todayIstDateString()
            )
            val holidays = parseHolidays(json.optJSONArray("holidays"))
            val notices = parseNotices(json.optJSONArray("notices"))
            ParentDashboardData(
                roll = resolvedRoll,
                studentName = studentName,
                attendance = attendance,
                attendanceClosed = json.optAttendanceClosed(),
                attendanceDate = attendanceDate,
                closedAt = IstDateTimeFormatter.formatTimeOnly(json.optString("closedAt", ""), ""),
                todayHoliday = parseSingleHoliday(json.optJSONObject("todayHoliday"))
                    ?: holidays.firstOrNull { it.date.equals(attendanceDate, ignoreCase = true) },
                holidays = holidays,
                notices = notices,
                pinnedBroadcast = parseSingleNoticeOrNull(json.optJSONObject("pinnedBroadcast"))
                    ?: notices.firstOrNull { it.isPinned },
                wardShopFloorEntry = parseSingleShopFloorOrNull(json.optJSONObject("wardShopFloor"))
                    ?: parseSingleShopFloorOrNull(json.optJSONObject("myShopFloor")),
                parentAlerts = parseParentAlerts(json.optJSONArray("parentAlerts")),
                monthlyMarks = parseMonthlyMarks(json.optJSONArray("monthlyMarks")),
                publishedReportUrl = json.optString("publishedReportUrl", "").trim(),
                publishedReportAt = IstDateTimeFormatter.formatDateTime(json.optString("publishedReportAt", ""), "")
            )
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
                val t = AttendanceMath.safeTotalDays(
                    item.optSafeInt("present"),
                    item.optSafeInt("half"),
                    item.optSafeInt("total")
                )
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
                    val pct = AttendanceMath.computePercentage(present, half, safeTotal, item.optSafeInt("pct"))
                    add(
                        StaffAttendanceRecord(
                            roll = item.optSafeString("roll"),
                            name = item.optString("name", "Trainee"),
                            todayStatus = item.optString("todayStatus", "ABSENT").ifBlank { "ABSENT" },
                            inT = IstDateTimeFormatter.formatTimeOnly(item.optString("inT", "-")),
                            outT = IstDateTimeFormatter.formatTimeOnly(item.optString("outT", "-")),
                            present = present,
                            half = half,
                            total = safeTotal,
                            pct = pct,
                            lateGraceMode = item.optString("lateGraceMode", "").trim().uppercase(),
                            lateCutoff = IstDateTimeFormatter.formatTimeOnly(item.optString("lateCutoff", ""), ""),
                            lateGraceNote = item.optString("lateGraceNote", "").trim()
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
                            date = IstDateTimeFormatter.formatDateOnly(item.optString("date", "-")),
                            roll = item.optSafeString("roll"),
                            name = item.optString("name", "Trainee"),
                            type = item.optString("type", "Out-Pass"),
                            timeWindow = item.optString("timeWindow", "-"),
                            reason = item.optString("reason", "-")
                        )
                    )
                }
            }
            val attendanceDate = IstDateTimeFormatter.formatDateOnly(
                json.optString("attendanceDate", json.optString("date", todayIstDateString())),
                todayIstDateString()
            )
            val holidays = parseHolidays(json.optJSONArray("holidays"))
            val notices = parseNotices(json.optJSONArray("notices"))
            val serverShopFloor = parseShopFloorList(json.optJSONArray("shopFloorList"))
            StaffDashboardData(
                allAttendance = attendanceList,
                pendingLeaves = pendingList,
                parentAlerts = parseParentAlerts(json.optJSONArray("parentAlerts")),
                attendanceClosed = json.optAttendanceClosed(),
                attendanceDate = attendanceDate,
                closedAt = IstDateTimeFormatter.formatTimeOnly(json.optString("closedAt", ""), ""),
                todayHoliday = parseSingleHoliday(json.optJSONObject("todayHoliday"))
                    ?: holidays.firstOrNull { it.date.equals(attendanceDate, ignoreCase = true) },
                holidays = holidays,
                notices = notices,
                pinnedBroadcast = parseSingleNoticeOrNull(json.optJSONObject("pinnedBroadcast"))
                    ?: notices.firstOrNull { it.isPinned },
                shopFloorList = mergeAutoFlaggedShopFloor(attendanceList, serverShopFloor, attendanceDate),
                scanOverrides = parseScanOverrides(json.optJSONObject("scanOverrides")),
                toolCribLogs = parseToolCribLogs(json.optJSONArray("toolCribLogs")),
                todayDutySignOff = parseDutySignOff(json.optJSONObject("todayDutySignOff")),
                lastScannerSyncTime = IstDateTimeFormatter.formatDateTime(json.optString("lastScannerSync", ""), ""),
                projectGroupsEnabled = json.optBoolean("projectGroupsEnabled", false),
                projectGroups = parseProjectGroups(json.optJSONArray("projectGroups")),
                monthlyMarks = parseMonthlyMarks(json.optJSONArray("monthlyMarks")),
                workMarkQueries = parseWorkMarkQueries(
                    json.optJSONArray("workMarkQueries") ?: json.optJSONArray("labMarkQueries")
                ),
                runningLateAlerts = parseRunningLateAlerts(json.optJSONArray("runningLateAlerts")),
                academicSubmissions = parseAcademicSubmissions(
                    json.optJSONArray("academicSubmissions") ?: json.optJSONArray("submissions")
                ),
                dutyOverrides = parseDutyOverrides(json.opt("dutyOverrides")),
                timetable = TimetableStore.parseJsonArray(json.optJSONArray("timetable")),
                traineeTimetableEditEnabled = json.optBoolean("traineeTimetableEditEnabled", false),
                publishedReportUrl = json.optString("publishedReportUrl", "").trim(),
                publishedReportAt = IstDateTimeFormatter.formatDateTime(json.optString("publishedReportAt", ""), "")
            )
        }
    }

    suspend fun submitParentAlert(roll: String, pin: String, alertType: String, message: String): ApiResult<ParentAlertSubmitResult> {
        return executeGet(
            mapOf("action" to "parent_submit_alert", "roll" to roll.trim(), "pin" to pin.trim(), "alertType" to alertType.trim(), "message" to message.trim())
        ) { json ->
            ParentAlertSubmitResult(
                alertId = json.optSafeString("alertId", json.optSafeString("id", "")),
                message = json.optString("msg", "Alert sent to Trade Instructor.").ifBlank { "Alert sent to Trade Instructor." }
            )
        }
    }

    suspend fun submitLeave(roll: String, pin: String, type: String, timeWindow: String, reason: String, studentSig: String): ApiResult<LeaveSubmitResult> {
        return executeGet(
            mapOf("action" to "submit_leave", "roll" to roll.trim(), "pin" to pin.trim(), "type" to type.trim(), "timeWindow" to timeWindow.trim(), "reason" to reason.trim(), "studentSig" to studentSig.trim())
        ) { json ->
            LeaveSubmitResult(
                reqId = json.optSafeString("reqId", json.optSafeString("id", "")),
                message = json.optString("msg", "Out-pass request submitted.").ifBlank { "Out-pass request submitted." }
            )
        }
    }

    suspend fun setScanOverride(pin: String, earlyClassLogoutEnabled: Boolean, shopFloorReleaseNowEnabled: Boolean): ApiResult<ScanOverrideResult> {
        return executeGet(
            mapOf("action" to "set_scan_override", "pin" to pin.trim(), "earlyClassLogout" to earlyClassLogoutEnabled.toString(), "shopFloorReleaseNow" to shopFloorReleaseNowEnabled.toString())
        ) { json ->
            ScanOverrideResult(
                state = parseScanOverrides(json.optJSONObject("scanOverrides")).copy(
                    earlyClassLogoutEnabled = json.optBoolean("earlyClassLogoutEnabled", earlyClassLogoutEnabled),
                    shopFloorReleaseNowEnabled = json.optBoolean("shopFloorReleaseNowEnabled", shopFloorReleaseNowEnabled)
                ),
                message = json.optString("msg", "Scanner override settings updated.").ifBlank { "Scanner override settings updated." }
            )
        }
    }

    suspend fun setShopFloorMember(pin: String, roll: String, operation: String, sessionType: String = "POST_3PM", scheduledEndTime: String = "05:00 PM"): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf("action" to "set_shop_floor_member", "pin" to pin.trim(), "roll" to roll.trim(), "operation" to operation.trim().uppercase(), "sessionType" to sessionType.trim().uppercase(), "endTime" to scheduledEndTime.trim())
        ) { json -> SimpleActionResult(json.optString("msg", "Shop Floor roster updated.").ifBlank { "Shop Floor roster updated." }) }
    }

    suspend fun triggerShopFloorCardLogout(roll: String): ApiResult<EspTapResult> {
        return executeGet(
            mapOf("action" to "shop_floor_card_logout", "roll" to roll.trim())
        ) { json ->
            EspTapResult(
                roll = roll.trim(),
                mode = json.optString("mode", "SHOP_FLOOR_OUT").ifBlank { "SHOP_FLOOR_OUT" },
                name = json.optString("name", "Roll #$roll").ifBlank { "Roll #$roll" },
                time = json.optString("checkOutCardScanTime", json.optString("time", "-")),
                message = json.optString("msg", "Shop Floor ID-card logout recorded.").trim()
            )
        }
    }

    suspend fun verifyShopFloorLogout(pin: String, roll: String, date: String = todayIstDateString()): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf("action" to "verify_shop_floor_logout", "pin" to pin.trim(), "roll" to roll.trim(), "date" to date.trim())
        ) { json -> SimpleActionResult(json.optString("msg", "Verified Roll #$roll.").ifBlank { "Verified Roll #$roll." }) }
    }

    suspend fun bulkMorningRollCall(pin: String, status: String = "PRESENT"): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf("action" to "bulk_roll_call", "pin" to pin.trim(), "status" to status.trim().uppercase(), "date" to todayIstDateString())
        ) { json -> SimpleActionResult(json.optString("msg", "Bulk morning roll call applied.").ifBlank { "Bulk morning roll call applied." }) }
    }

    suspend fun markAttendanceException(pin: String, roll: String, exceptionType: String): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf("action" to "mark_exception", "pin" to pin.trim(), "roll" to roll.trim(), "exceptionType" to exceptionType.trim().uppercase(), "date" to todayIstDateString())
        ) { json -> SimpleActionResult(json.optString("msg", "Updated Roll #$roll.").ifBlank { "Updated Roll #$roll." }) }
    }

    suspend fun setLateGrace(pin: String, roll: String, mode: String, cutoff: String = "", note: String = "", date: String = todayIstDateString()): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf("action" to "set_late_grace", "pin" to pin.trim(), "roll" to roll.trim(), "mode" to mode.trim().uppercase(), "cutoff" to cutoff.trim(), "note" to note.trim(), "date" to date.trim())
        ) { json -> SimpleActionResult(json.optString("msg", "Late grace updated.").ifBlank { "Late grace updated." }) }
    }

    suspend fun saveMonthlyMark(
        pin: String,
        markId: String = "",
        roll: String,
        monthLabel: String,
        examDate: String = todayIstDateString(),
        theoryMarks: Int,
        theoryMax: Int = 50,
        labMarks: Int,
        labMax: Int = 100,
        remarks: String = "",
        publish: Boolean = true,
        notifyParent: Boolean = true,
        signedBy: String = "",
        signatureData: String = ""
    ): ApiResult<SimpleActionResult> {
        val params = mutableMapOf(
            "action" to "save_monthly_mark",
            "pin" to pin.trim(),
            "markId" to markId.trim(),
            "roll" to roll.trim(),
            "monthLabel" to monthLabel.trim(),
            "examDate" to examDate.trim(),
            "theoryMarks" to theoryMarks.coerceAtLeast(0).toString(),
            "theoryMax" to theoryMax.coerceAtLeast(1).toString(),
            "labMarks" to labMarks.coerceAtLeast(0).toString(),
            "labMax" to labMax.coerceAtLeast(1).toString(),
            "remarks" to remarks.trim(),
            "publish" to publish.toString(),
            "notifyParent" to notifyParent.toString()
        )
        if (signedBy.isNotBlank()) params["signedBy"] = signedBy.trim()
        if (signatureData.isNotBlank()) params["signatureData"] = signatureData.trim()
        return executeGet(params) { json ->
            SimpleActionResult(json.optString("msg", "Saved marks for Roll #$roll.").ifBlank { "Saved marks for Roll #$roll." })
        }
    }

    suspend fun submitWorkMarkQuery(roll: String, pin: String, monthLabel: String, workReference: String, currentMarkNote: String, traineeNote: String): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf("action" to "submit_work_mark_query", "roll" to roll.trim(), "pin" to pin.trim(), "monthLabel" to monthLabel.trim(), "workReference" to workReference.trim(), "currentMarkNote" to currentMarkNote.trim(), "traineeNote" to traineeNote.trim())
        ) { json -> SimpleActionResult(json.optString("msg", "Query sent to Trade Instructor.").ifBlank { "Query sent to Trade Instructor." }) }
    }

    suspend fun replyWorkMarkQuery(pin: String, queryId: String, status: String, reply: String): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf("action" to "reply_work_mark_query", "pin" to pin.trim(), "queryId" to queryId.trim(), "status" to status.trim().uppercase(), "reply" to reply.trim())
        ) { json -> SimpleActionResult(json.optString("msg", "Reply saved.").ifBlank { "Reply saved." }) }
    }

    suspend fun submitRunningLateAlert(roll: String, pin: String, expectedArrival: String, reasonCategory: String, details: String): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf("action" to "submit_running_late", "roll" to roll.trim(), "pin" to pin.trim(), "expectedArrival" to expectedArrival.trim(), "reasonCategory" to reasonCategory.trim(), "details" to details.trim())
        ) { json -> SimpleActionResult(json.optString("msg", "Running Late alert sent.").ifBlank { "Running Late alert sent." }) }
    }

    suspend fun submitManualAttendancePunch(pin: String, roll: String, status: String, allowDuplicateOverride: Boolean = false, note: String = ""): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf("action" to "manual_punch", "pin" to pin.trim(), "roll" to roll.trim(), "status" to status.trim().uppercase(), "date" to todayIstDateString(), "allowDuplicate" to allowDuplicateOverride.toString(), "note" to note.trim())
        ) { json -> SimpleActionResult(json.optString("msg", "Marked Roll #$roll as $status.").ifBlank { "Marked Roll #$roll as $status." }) }
    }

    suspend fun undoAttendanceAction(pin: String, roll: String, date: String = todayIstDateString(), restoreStatus: String = "ABSENT", restoreInT: String = "-", restoreOutT: String = "-"): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf("action" to "undo_attendance", "pin" to pin.trim(), "roll" to roll.trim(), "rollNo" to roll.trim(), "date" to date.trim(), "restoreStatus" to restoreStatus.trim().uppercase(), "restoreInT" to restoreInT.trim(), "restoreOutT" to restoreOutT.trim())
        ) { json -> SimpleActionResult(json.optString("msg", "Reset Roll #$roll.").ifBlank { "Reset Roll #$roll." }) }
    }

    suspend fun submitAcademicRecord(roll: String, pin: String, studentName: String, theoryTopic: String, theoryStatus: String, theoryUpdatedDate: String, labJobName: String, labStatus: String, labSignedDate: String, remarks: String): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf("action" to "submit_record", "roll" to roll.trim(), "rollNo" to roll.trim(), "pin" to pin.trim(), "studentName" to studentName.trim(), "theoryTopic" to theoryTopic.trim(), "theoryStatus" to theoryStatus.trim(), "theoryUpdatedDate" to theoryUpdatedDate.trim(), "labJobName" to labJobName.trim(), "labStatus" to labStatus.trim(), "labSignedDate" to labSignedDate.trim(), "remarks" to remarks.trim())
        ) { json -> SimpleActionResult(json.optString("msg", "Academic record submitted.").ifBlank { "Academic record submitted." }) }
    }

    suspend fun fetchAcademicSubmissions(pin: String, roll: String = ""): ApiResult<List<AcademicSubmission>> {
        val params = mutableMapOf("action" to "get_submissions", "pin" to pin.trim())
        if (roll.isNotBlank()) { params["roll"] = roll.trim(); params["rollNo"] = roll.trim() }
        return executeGet(params) { json ->
            val list = parseAcademicSubmissions(json.optJSONArray("submissions") ?: json.optJSONArray("academicSubmissions"))
            if (roll.isNotBlank()) list.filter { it.roll.equals(roll.trim(), ignoreCase = true) } else list
        }
    }

    suspend fun verifyAcademicSubmission(pin: String, roll: String, submissionId: String = "", instructorRemark: String = "", verifiedBy: String = "Trade Instructor (EM)"): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf("action" to "verify_submission", "pin" to pin.trim(), "roll" to roll.trim(), "rollNo" to roll.trim(), "submissionId" to submissionId.trim(), "instructorRemark" to instructorRemark.trim(), "verifiedBy" to verifiedBy.trim())
        ) { json -> SimpleActionResult(json.optString("msg", "Verified Roll #$roll.").ifBlank { "Verified Roll #$roll." }) }
    }

    suspend fun forwardAcademicSubmissionToPrincipal(pin: String, roll: String = "", bulkAllVerified: Boolean = false): ApiResult<SimpleActionResult> {
        val params = mutableMapOf("action" to "forward_to_principal", "pin" to pin.trim(), "bulk" to bulkAllVerified.toString())
        if (roll.isNotBlank()) { params["roll"] = roll.trim(); params["rollNo"] = roll.trim() }
        return executeGet(params) { json -> SimpleActionResult(json.optString("msg", "Forwarded to Principal.").ifBlank { "Forwarded to Principal." }) }
    }

    suspend fun signOffDailyDuty(pin: String, date: String, pairLabel: String, note: String): ApiResult<DutySignOffResult> {
        return executeGet(
            mapOf("action" to "sign_off_duty", "pin" to pin.trim(), "date" to date.trim(), "pairLabel" to pairLabel.trim(), "note" to note.trim())
        ) { json ->
            DutySignOffResult(
                record = DutySignOffRecord(date = date, pairLabel = pairLabel, signedOff = true, signedAt = json.optString("signedAt", ""), note = note.trim()),
                message = json.optString("msg", "Duty signed off.").ifBlank { "Duty signed off." }
            )
        }
    }

    suspend fun saveDutyOverride(pin: String, dateKey: String, firstRoll: Int, secondRoll: Int, dutyNote: String = "", reanchorFromHere: Boolean = false, resetToAuto: Boolean = false): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf("action" to "saveDutyOverride", "pin" to pin.trim(), "dateKey" to dateKey.trim(), "firstRoll" to firstRoll.toString(), "secondRoll" to secondRoll.toString(), "dutyNote" to dutyNote.trim(), "reanchorFromHere" to reanchorFromHere.toString(), "resetToAuto" to resetToAuto.toString())
        ) { json -> SimpleActionResult(json.optString("msg", "Duty charge updated.").ifBlank { "Duty charge updated." }) }
    }

    suspend fun saveTimetable(pin: String, roll: String = "", periods: List<TimetablePeriod>, traineeTimetableEditEnabled: Boolean? = null, isTraineeEdit: Boolean = false): ApiResult<SimpleActionResult> {
        val params = mutableMapOf("action" to "saveTimetable", "pin" to pin.trim(), "periodsJson" to TimetableStore.toJsonString(periods), "isTraineeEdit" to isTraineeEdit.toString())
        if (roll.isNotBlank()) params["roll"] = roll.trim()
        if (traineeTimetableEditEnabled != null) params["traineeTimetableEditEnabled"] = traineeTimetableEditEnabled.toString()
        return executeGet(params) { json -> SimpleActionResult(json.optString("msg", "Timetable saved.").ifBlank { "Timetable saved." }) }
    }

    suspend fun logToolCribIssue(pin: String, itemName: String, conditionStatus: String, notes: String): ApiResult<ToolCribLogResult> {
        return executeGet(
            mapOf("action" to "log_tool_crib_issue", "pin" to pin.trim(), "itemName" to itemName.trim(), "conditionStatus" to conditionStatus.trim().uppercase(), "notes" to notes.trim())
        ) { json ->
            ToolCribLogResult(
                entry = ToolCribLogEntry(id = json.optSafeString("id", "TC-${System.currentTimeMillis()}"), date = json.optString("date", todayIstDateString()), time = json.optString("time", ""), itemName = itemName.trim(), conditionStatus = conditionStatus.trim().uppercase(), notes = notes.trim()),
                message = json.optString("msg", "Tool Crib log recorded.").ifBlank { "Tool Crib log recorded." }
            )
        }
    }

    suspend fun sendInstructorParentAlert(pin: String, roll: String, alertType: String, message: String): ApiResult<ParentAlertSubmitResult> {
        return executeGet(
            mapOf("action" to "send_instructor_parent_alert", "pin" to pin.trim(), "roll" to roll.trim(), "alertType" to alertType.trim().uppercase(), "message" to message.trim())
        ) { json ->
            ParentAlertSubmitResult(alertId = json.optSafeString("alertId", ""), message = json.optString("msg", "Alert logged for Roll #$roll.").ifBlank { "Alert logged for Roll #$roll." })
        }
    }

    suspend fun rejectLeaveRequest(pin: String, reqId: String, reason: String): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf("action" to "reject_leave", "pin" to pin.trim(), "reqId" to reqId.trim(), "reason" to reason.trim())
        ) { json -> SimpleActionResult(json.optString("msg", "Request $reqId rejected.").ifBlank { "Request $reqId rejected." }) }
    }

    suspend fun confirmCloseAttendance(pin: String, date: String = todayIstDateString()): ApiResult<CloseAttendanceResult> {
        return executeGet(
            mapOf("action" to "confirm_close_attendance", "pin" to pin.trim(), "date" to date.trim())
        ) { json ->
            CloseAttendanceResult(
                attendanceClosed = json.optBoolean("attendanceClosed", true),
                date = json.optString("date", date).ifBlank { date },
                closedAt = json.optString("closedAt", "").trim(),
                absentCount = json.optSafeInt("absentCount"),
                halfDayCount = json.optSafeInt("halfDayCount"),
                message = json.optString("msg", "Attendance closed for today.").ifBlank { "Attendance closed for today." }
            )
        }
    }

    suspend fun postHoliday(pin: String, date: String, reason: String): ApiResult<HolidayPostResult> {
        return executeGet(
            mapOf("action" to "post_holiday", "pin" to pin.trim(), "date" to date.trim(), "reason" to reason.trim())
        ) { json ->
            val holiday = parseSingleHoliday(json.optJSONObject("holiday")) ?: HolidayAnnouncement(id = "HOL-$date", date = date.trim(), reason = reason.trim())
            HolidayPostResult(holiday = holiday, message = json.optString("msg", "Holiday published.").ifBlank { "Holiday published." })
        }
    }

    suspend fun postNotice(pin: String, title: String, category: String, body: String, date: String = todayIstDateString(), pinned: Boolean = false, postedBy: String = "Trade Instructor"): ApiResult<NoticePostResult> {
        return executeGet(
            mapOf("action" to "post_notice", "pin" to pin.trim(), "title" to title.trim(), "category" to category.trim(), "body" to body.trim(), "date" to date.trim(), "pinned" to pinned.toString(), "postedBy" to postedBy.trim())
        ) { json ->
            val noticeObj = json.optJSONObject("notice")
            val notice = if (noticeObj != null) parseSingleNotice(noticeObj) else NoticeItem(id = "NTC-${System.currentTimeMillis()}", title = title.trim(), date = date.trim(), category = category.trim(), body = body.trim(), postedBy = postedBy, isPinned = pinned)
            NoticePostResult(notice = notice, message = json.optString("msg", "Notice published.").ifBlank { "Notice published." })
        }
    }

    suspend fun setProjectGroupsAccess(pin: String, enabled: Boolean): ApiResult<ProjectAccessResult> {
        return executeGet(
            mapOf("action" to "set_project_groups_access", "pin" to pin.trim(), "enabled" to enabled.toString())
        ) { json ->
            val resolved = json.optBoolean("projectGroupsEnabled", json.optBoolean("enabled", enabled))
            ProjectAccessResult(enabled = resolved, message = json.optString("msg", if (resolved) "Project Groups enabled." else "Project Groups locked."))
        }
    }

    suspend fun fetchProjectGroups(pin: String, roll: String = ""): ApiResult<ProjectGroupsPayload> {
        val params = mutableMapOf("action" to "project_groups_data", "pin" to pin.trim())
        if (roll.isNotBlank()) params["roll"] = roll.trim()
        return executeGet(params) { json ->
            ProjectGroupsPayload(
                enabled = json.optBoolean("projectGroupsEnabled", json.optBoolean("enabled", false)),
                groups = parseProjectGroups(json.optJSONArray("projectGroups") ?: json.optJSONArray("groups"))
            )
        }
    }

    suspend fun saveProjectGroup(roll: String, pin: String, projectTitle: String, memberRolls: List<Int>, leaderRoll: Int, leaderName: String): ApiResult<ProjectGroupSaveResult> {
        return executeGet(
            mapOf("action" to "save_project_group", "roll" to roll.trim(), "pin" to pin.trim(), "projectTitle" to projectTitle.trim(), "members" to memberRolls.distinct().sorted().joinToString(","), "leaderRoll" to leaderRoll.toString(), "leaderName" to leaderName.trim())
        ) { json ->
            val group = json.optJSONObject("group")?.let { parseSingleProjectGroup(it) } ?: ProjectGroup(
                groupId = json.optSafeString("groupId", "PG-${System.currentTimeMillis()}"),
                projectTitle = projectTitle.trim(),
                leaderRoll = leaderRoll.toString(),
                leaderName = leaderName.trim(),
                memberRolls = memberRolls.distinct().sorted().map { it.toString() },
                submittedByRoll = roll.trim(),
                createdAt = todayIstDateString()
            )
            ProjectGroupSaveResult(group = group, message = json.optString("msg", "Project group saved.").ifBlank { "Project group saved." })
        }
    }

    suspend fun signByInstructor(pin: String, reqId: String, sirSig: String): ApiResult<SignApprovalResult> {
        return executeGet(
            mapOf("action" to "sign_sir", "pin" to pin.trim(), "reqId" to reqId.trim(), "sirSig" to sirSig.trim())
        ) { json ->
            SignApprovalResult(reqId = reqId, message = json.optString("msg", "Approved by Trade Instructor."), pdfUrl = json.optString("pdfUrl", json.optString("pdf", "")).trim())
        }
    }

    suspend fun signByPrincipal(pin: String, reqId: String, principalSig: String): ApiResult<SignApprovalResult> {
        return executeGet(
            mapOf("action" to "sign_principal", "pin" to pin.trim(), "reqId" to reqId.trim(), "principalSig" to principalSig.trim())
        ) { json ->
            SignApprovalResult(reqId = reqId, message = json.optString("msg", "Approved & Signed by Principal."), pdfUrl = json.optString("pdfUrl", json.optString("pdf", "")).trim())
        }
    }

    suspend fun exportDailyAttendancePdf(pin: String): ApiResult<PdfExportResult> {
        return executeGet(mapOf("action" to "print_attendance", "pin" to pin.trim())) { json ->
            PdfExportResult(pdfUrl = json.optString("pdfUrl", json.optString("pdf", "")).trim(), message = json.optString("msg", "Daily Attendance PDF generated."))
        }
    }

    suspend fun triggerEspTap(roll: String): ApiResult<EspTapResult> {
        return executeGet(mapOf("action" to "tap", "roll" to roll.trim())) { json ->
            EspTapResult(
                roll = roll.trim(),
                mode = json.optString("mode", "PRESENT").ifBlank { "PRESENT" },
                name = json.optString("name", "Roll #$roll").ifBlank { "Roll #$roll" },
                time = IstDateTimeFormatter.formatTimeOnly(json.optString("time", "-")),
                message = json.optString("msg", json.optString("message", "")).trim()
            )
        }
    }

    suspend fun fetchLiveConfig(): ApiResult<Pair<Map<String, String>, Map<String, String>>> {
        return executeGet(mapOf("action" to "get_live_config")) { json ->
            val appMap = jsonObjectToStringMap(json.optJSONObject("appConfig"))
            val uiMap = jsonObjectToStringMap(json.optJSONObject("uiConfig"))
            appMap to uiMap
        }
    }

    suspend fun saveRemoteAppConfig(pin: String, updates: Map<String, String>): ApiResult<SimpleActionResult> {
        val params = mutableMapOf("action" to "save_app_config", "pin" to pin.trim())
        updates.forEach { (k, v) -> params[k] = v }
        return executeGet(params) { json ->
            SimpleActionResult(json.optString("msg", "App config pushed live.").ifBlank { "App config pushed live." })
        }
    }

    suspend fun saveRemoteUiConfig(pin: String, updates: Map<String, String>, scope: String): ApiResult<SimpleActionResult> {
        val params = mutableMapOf("action" to "save_ui_config", "pin" to pin.trim(), "scope" to scope.trim())
        updates.forEach { (k, v) -> params[k] = v }
        return executeGet(params) { json ->
            SimpleActionResult(json.optString("msg", "Live UI config updated.").ifBlank { "Live UI config updated." })
        }
    }

    suspend fun submitBugReport(
        screenName: String,
        reportedBy: String,
        role: String,
        appVersion: String,
        description: String
    ): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf(
                "action" to "submit_bug",
                "screenName" to screenName.trim(),
                "reportedBy" to reportedBy.trim(),
                "role" to role.trim(),
                "appVersion" to appVersion.trim(),
                "description" to description.trim()
            )
        ) { json ->
            SimpleActionResult(json.optString("msg", "Bug report submitted.").ifBlank { "Bug report submitted." })
        }
    }

    suspend fun fetchBugReports(pin: String): ApiResult<List<Map<String, String>>> {
        return executeGet(mapOf("action" to "get_bugs", "pin" to pin.trim())) { json ->
            val arr = json.optJSONArray("bugs") ?: JSONArray()
            buildList {
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { add(jsonObjectToStringMap(it)) }
                }
            }
        }
    }

    suspend fun resolveBugReport(pin: String, bugId: String, reply: String): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf("action" to "resolve_bug", "pin" to pin.trim(), "bugId" to bugId.trim(), "reply" to reply.trim())
        ) { json ->
            SimpleActionResult(json.optString("msg", "Bug marked resolved.").ifBlank { "Bug marked resolved." })
        }
    }

    suspend fun editSheetRow(
        pin: String,
        tabName: String,
        rowIndex: Int,
        colName: String,
        newValue: String
    ): ApiResult<SimpleActionResult> {
        return executeGet(
            mapOf(
                "action" to "edit_sheet_row",
                "pin" to pin.trim(),
                "tabName" to tabName.trim(),
                "rowIndex" to rowIndex.toString(),
                "colName" to colName.trim(),
                "newValue" to newValue
            )
        ) { json ->
            SimpleActionResult(json.optString("msg", "Sheet cell updated in $SHEET_NAME.").ifBlank { "Sheet cell updated." })
        }
    }

    private fun jsonObjectToStringMap(obj: JSONObject?): Map<String, String> {
        if (obj == null) return emptyMap()
        val out = linkedMapOf<String, String>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            out[k] = obj.opt(k)?.toString().orEmpty()
        }
        return out
    }

    private fun parseAttendanceSummary(attObj: JSONObject?): AttendanceSummary {
        val obj = attObj ?: JSONObject()
        val present = obj.optSafeInt("present").coerceAtLeast(0)
        val half = obj.optSafeInt("half").coerceAtLeast(0)
        val safeTotal = AttendanceMath.safeTotalDays(present, half, obj.optSafeInt("total"))
        return AttendanceSummary(
            todayStatus = obj.optString("todayStatus", "ABSENT").ifBlank { "ABSENT" },
            inT = IstDateTimeFormatter.formatTimeOnly(obj.optString("inT", "-")),
            outT = IstDateTimeFormatter.formatTimeOnly(obj.optString("outT", "-")),
            present = present,
            half = half,
            total = safeTotal,
            pct = AttendanceMath.computePercentage(present, half, safeTotal, obj.optSafeInt("pct")),
            streakDays = obj.optSafeInt("streakDays", present.coerceAtMost(15))
        )
    }

    private fun mergeAutoFlaggedShopFloor(attendanceList: List<StaffAttendanceRecord>, serverList: List<ShopFloorEntry>, date: String): List<ShopFloorEntry> {
        val byRoll = LinkedHashMap<String, ShopFloorEntry>()
        serverList.forEach { byRoll[it.roll] = it }
        attendanceList.forEach { rec ->
            if (rec.pct < AttendanceMath.EXAM_THRESHOLD_PCT && rec.total > 0 && !byRoll.containsKey(rec.roll)) {
                byRoll[rec.roll] = ShopFloorEntry(id = "AUTO-${rec.roll}-$date", date = date, roll = rec.roll, name = rec.name, attendancePct = rec.pct, assignedReason = "Auto (<80%)")
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
            date = IstDateTimeFormatter.formatDateOnly(obj.optString("date", todayIstDateString()), todayIstDateString()),
            roll = roll,
            name = obj.optString("name", "Roll #$roll").ifBlank { "Roll #$roll" },
            attendancePct = obj.optSafeInt("attendancePct", obj.optSafeInt("pct")).coerceIn(0, 100),
            sessionType = obj.optString("sessionType", "POST_3PM").ifBlank { "POST_3PM" },
            scheduledEndTime = IstDateTimeFormatter.formatTimeOnly(obj.optString("scheduledEndTime", "05:00 PM"), "05:00 PM"),
            assignedReason = obj.optString("assignedReason", "Auto (<80%)"),
            status = obj.optString("status", "SCHEDULED").ifBlank { "SCHEDULED" },
            checkInTime = IstDateTimeFormatter.formatTimeOnly(obj.optString("checkInTime", "03:00 PM"), "03:00 PM"),
            checkOutCardScanTime = IstDateTimeFormatter.formatTimeOnly(obj.optString("checkOutCardScanTime", ""), ""),
            durationMinutes = obj.optSafeInt("durationMinutes"),
            verifiedByInstructor = obj.optBoolean("verifiedByInstructor", false),
            isManualOverride = obj.optBoolean("isManualOverride", false)
        )
    }

    private fun parseShopFloorList(array: JSONArray?): List<ShopFloorEntry> {
        if (array == null) return emptyList()
        return buildList { for (i in 0 until array.length()) parseSingleShopFloorOrNull(array.optJSONObject(i))?.let(::add) }
    }

    private fun parseScanOverrides(obj: JSONObject?): ScanOverrideState {
        if (obj == null) return ScanOverrideState()
        return ScanOverrideState(
            earlyClassLogoutEnabled = obj.optBoolean("earlyClassLogoutEnabled", obj.optBoolean("earlyClassLogout", false)),
            shopFloorReleaseNowEnabled = obj.optBoolean("shopFloorReleaseNowEnabled", obj.optBoolean("shopFloorReleaseNow", false)),
            updatedAt = IstDateTimeFormatter.formatDateTime(obj.optString("updatedAt", ""), ""),
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
                add(ToolCribLogEntry(id = item.optSafeString("id", "TC-$i"), date = IstDateTimeFormatter.formatDateOnly(item.optString("date", todayIstDateString()), todayIstDateString()), time = IstDateTimeFormatter.formatTimeOnly(item.optString("time", ""), ""), itemName = name, conditionStatus = item.optString("conditionStatus", "DAMAGED"), notes = item.optString("notes", "").trim(), reportedBy = item.optString("reportedBy", "Trade Instructor")))
            }
        }
    }

    private fun parseDutySignOff(obj: JSONObject?): DutySignOffRecord? {
        if (obj == null) return null
        val date = IstDateTimeFormatter.formatDateOnly(obj.optString("date", ""), "")
        val pair = obj.optString("pairLabel", "").trim()
        if (date.isBlank() && pair.isBlank()) return null
        return DutySignOffRecord(date = date.ifBlank { todayIstDateString() }, pairLabel = pair, signedOff = obj.optBoolean("signedOff", false), signedAt = IstDateTimeFormatter.formatTimeOnly(obj.optString("signedAt", ""), ""), note = obj.optString("note", "").trim())
    }

    private fun parseParentAlerts(array: JSONArray?): List<ParentAlertItem> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(ParentAlertItem(id = item.optSafeString("id", "PA-$i"), date = IstDateTimeFormatter.formatDateOnly(item.optString("date", "-")), time = IstDateTimeFormatter.formatTimeOnly(item.optString("time", ""), ""), roll = item.optSafeString("roll"), studentName = item.optString("studentName", "Trainee"), alertType = item.optString("alertType", ParentAlertTypes.LEAVE_REASON), message = item.optString("message", "").trim()))
            }
        }
    }

    private fun parseSingleHoliday(obj: JSONObject?): HolidayAnnouncement? {
        if (obj == null) return null
        val date = IstDateTimeFormatter.formatDateOnly(obj.optString("date", ""), "")
        val reason = obj.optString("reason", "").trim()
        if (date.isBlank() && reason.isBlank()) return null
        return HolidayAnnouncement(id = obj.optSafeString("id", "HOL-$date"), date = date.ifBlank { todayIstDateString() }, reason = reason.ifBlank { "Declared Holiday" }, postedBy = obj.optString("postedBy", "Trade Instructor"), postedAt = obj.optString("postedAt", ""))
    }

    private fun parseHolidays(array: JSONArray?): List<HolidayAnnouncement> {
        if (array == null) return emptyList()
        return buildList { for (i in 0 until array.length()) parseSingleHoliday(array.optJSONObject(i))?.let(::add) }
    }

    private fun parseSingleNoticeOrNull(obj: JSONObject?): NoticeItem? {
        if (obj == null) return null
        if (obj.optString("title", "").isBlank() && obj.optString("body", "").isBlank()) return null
        return parseSingleNotice(obj)
    }

    private fun parseSingleNotice(item: JSONObject): NoticeItem {
        return NoticeItem(
            id = item.optSafeString("id", item.optSafeString("noticeId", "NTC-${item.hashCode()}")),
            title = item.optString("title", "Trade Notice").ifBlank { "Trade Notice" },
            date = IstDateTimeFormatter.formatDateOnly(item.optString("date", todayIstDateString()), todayIstDateString()),
            category = item.optString("category", "General").ifBlank { "General" },
            body = item.optString("body", item.optString("message", "")).trim(),
            postedBy = item.optString("postedBy", "Govt. ITI Ranni").ifBlank { "Govt. ITI Ranni" },
            isPinned = item.optBoolean("isPinned", item.optBoolean("pinned", false))
        )
    }

    private fun parseNotices(array: JSONArray?): List<NoticeItem> {
        if (array == null) return emptyList()
        return buildList { for (i in 0 until array.length()) array.optJSONObject(i)?.let { add(parseSingleNotice(it)) } }
    }

    private fun parseSingleProjectGroup(item: JSONObject): ProjectGroup {
        val raw = item.opt("members")
        val list = when (raw) {
            is JSONArray -> buildList { for (i in 0 until raw.length()) raw.opt(i)?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let(::add) }
            is String -> raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
            else -> emptyList()
        }
        return ProjectGroup(groupId = item.optSafeString("groupId", "PG"), projectTitle = item.optString("projectTitle", "Electronics Project"), leaderRoll = item.optSafeString("leaderRoll"), leaderName = item.optString("leaderName", ""), memberRolls = list, submittedByRoll = item.optSafeString("submittedByRoll"), createdAt = item.optString("createdAt", ""))
    }

    private fun parseProjectGroups(array: JSONArray?): List<ProjectGroup> {
        if (array == null) return emptyList()
        return buildList { for (i in 0 until array.length()) array.optJSONObject(i)?.let { add(parseSingleProjectGroup(it)) } }
    }

    private fun parseStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList { for (i in 0 until array.length()) array.optString(i, "").trim().takeIf { it.isNotBlank() }?.let(::add) }
    }

    private fun parseMonthlyMarks(array: JSONArray?): List<MonthlyExamMark> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val roll = item.optSafeString("roll")
                val monthLabel = item.optString("monthLabel", item.optString("month", "")).trim()
                if (roll.isBlank() && monthLabel.isBlank()) continue
                add(
                    MonthlyExamMark(
                        id = item.optSafeString("id", "MK-$roll-$i"),
                        monthLabel = monthLabel.ifBlank { "Monthly Exam" },
                        examDate = IstDateTimeFormatter.formatDateOnly(item.optString("examDate", ""), ""),
                        roll = roll,
                        studentName = item.optString("studentName", "Trainee #$roll").ifBlank { "Trainee #$roll" },
                        theoryMarks = item.optSafeInt("theoryMarks", item.optSafeInt("theory", 0)).coerceAtLeast(0),
                        theoryMax = item.optSafeInt("theoryMax", 50).coerceAtLeast(1),
                        labMarks = item.optSafeInt("labMarks", item.optSafeInt("lab", 0)).coerceAtLeast(0),
                        labMax = item.optSafeInt("labMax", 100).coerceAtLeast(1),
                        remarks = item.optString("remarks", "").trim(),
                        published = item.optBoolean("published", true),
                        isRevised = item.optBoolean("isRevised", false),
                        signedBy = item.optString("signedBy", "").trim(),
                        signedAt = item.optString("signedAt", "").trim(),
                        signatureData = item.optString("signatureData", "").trim(),
                        updatedAt = item.optString("updatedAt", "")
                    )
                )
            }
        }
    }

    private fun parseWorkMarkQueries(array: JSONArray?): List<WorkMarkQuery> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val ref = item.optString("workReference", "").trim()
                val note = item.optString("traineeNote", "").trim()
                if (ref.isBlank() && note.isBlank()) continue
                add(WorkMarkQuery(id = item.optSafeString("id", "WMQ-$i"), date = IstDateTimeFormatter.formatDateOnly(item.optString("date", todayIstDateString()), todayIstDateString()), time = item.optString("time", ""), roll = item.optSafeString("roll"), studentName = item.optString("studentName", "Trainee"), monthLabel = item.optString("monthLabel", ""), workReference = ref.ifBlank { "Lab Practical" }, currentMarkNote = item.optString("currentMarkNote", ""), traineeNote = note, status = item.optString("status", "OPEN").uppercase(), instructorReply = item.optString("instructorReply", ""), updatedAt = item.optString("updatedAt", "")))
            }
        }
    }

    private fun parseRunningLateAlerts(array: JSONArray?): List<RunningLateAlert> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val roll = item.optSafeString("roll")
                if (roll.isBlank()) continue
                add(RunningLateAlert(id = item.optSafeString("id", "RL-$roll-$i"), date = IstDateTimeFormatter.formatDateOnly(item.optString("date", todayIstDateString()), todayIstDateString()), time = item.optString("time", ""), roll = roll, studentName = item.optString("studentName", "Trainee #$roll"), expectedArrival = item.optString("expectedArrival", "Soon"), reasonCategory = item.optString("reasonCategory", "Delay"), details = item.optString("details", ""), status = item.optString("status", "ACTIVE").uppercase()))
            }
        }
    }

    private fun parseSingleAcademicSubmissionOrNull(item: JSONObject?): AcademicSubmission? {
        if (item == null) return null
        val roll = item.optSafeString("roll", item.optSafeString("rollNo"))
        if (roll.isBlank()) return null
        val sent = item.optBoolean("sentToPrincipal", false)
        val rawStatus = item.optString("verificationStatus", "").trim().uppercase()
            .ifBlank { if (sent) AcademicSubmissionStatuses.FORWARDED_TO_PRINCIPAL else AcademicSubmissionStatuses.PENDING_VERIFICATION }
        return AcademicSubmission(
            id = item.optSafeString("id", "SUB-$roll"),
            roll = roll,
            studentName = item.optString("studentName", "Trainee #$roll").ifBlank { "Trainee #$roll" },
            theoryTopic = item.optString("theoryTopic", "").trim(),
            theoryStatus = item.optString("theoryStatus", "Pending"),
            theoryUpdatedDate = item.optString("theoryUpdatedDate", ""),
            labJobName = item.optString("labJobName", "").trim(),
            labStatus = item.optString("labStatus", "Incomplete"),
            labSignedDate = item.optString("labSignedDate", ""),
            remarks = item.optString("remarks", "").trim(),
            verificationStatus = rawStatus,
            verifiedBy = item.optString("verifiedBy", ""),
            verifiedAt = item.optString("verifiedAt", ""),
            instructorRemark = item.optString("instructorRemark", ""),
            sentToPrincipal = sent || rawStatus == AcademicSubmissionStatuses.FORWARDED_TO_PRINCIPAL,
            forwardedAt = item.optString("forwardedAt", ""),
            submittedAt = item.optString("submittedAt", "")
        )
    }

    private fun parseAcademicSubmissions(array: JSONArray?): List<AcademicSubmission> {
        if (array == null) return emptyList()
        return buildList { for (i in 0 until array.length()) parseSingleAcademicSubmissionOrNull(array.optJSONObject(i))?.let(::add) }
    }

    private fun parseDutyOverrides(raw: Any?): Map<String, DutyOverrideEntry> {
        if (raw == null) return emptyMap()
        val result = linkedMapOf<String, DutyOverrideEntry>()
        when (raw) {
            is JSONObject -> {
                val keys = raw.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    raw.optJSONObject(k)?.let { obj ->
                        parseSingleDutyOverride(obj, k)?.let { result[it.dateKey] = it }
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until raw.length()) {
                    raw.optJSONObject(i)?.let { obj ->
                        parseSingleDutyOverride(obj, "")?.let { result[it.dateKey] = it }
                    }
                }
            }
        }
        return result
    }

    private fun parseSingleDutyOverride(obj: JSONObject, fallbackDateKey: String): DutyOverrideEntry? {
        val dateKey = IstDateTimeFormatter.formatDateOnly(obj.optString("dateKey", fallbackDateKey), fallbackDateKey).trim()
        if (dateKey.isBlank()) return null
        val r1 = obj.optSafeInt("firstRoll")
        val r2 = obj.optSafeInt("secondRoll")
        if (r1 !in 1..DutyRotationEngine.TOTAL_TRAINEES || r2 !in 1..DutyRotationEngine.TOTAL_TRAINEES) return null
        return DutyOverrideEntry(dateKey = dateKey, firstRoll = r1, secondRoll = r2, dutyNote = obj.optString("dutyNote", ""), reanchorFromHere = obj.optBoolean("reanchorFromHere", false), updatedBy = obj.optString("updatedBy", "Trade Instructor"), updatedAt = obj.optString("updatedAt", ""))
    }

    private suspend fun <T> executeGet(
        params: Map<String, String>,
        parser: (JSONObject) -> T
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = BASE_URL.toHttpUrl().newBuilder()
            urlBuilder.addQueryParameter("key", ESP_KEY)
            urlBuilder.addQueryParameter("sheetName", SHEET_NAME)
            params.forEach { (key, value) ->
                if (!key.equals("key", ignoreCase = true) && !key.equals("sheetName", ignoreCase = true)) {
                    urlBuilder.addQueryParameter(key, value)
                }
            }
            val request = Request.Builder()
                .url(urlBuilder.build())
                .get()
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string()?.trim().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext ApiResult.Error("Server responded with HTTP ${response.code}. Please try again.")
                }
                if (rawBody.isEmpty() || !rawBody.startsWith("{")) {
                    return@withContext ApiResult.Error("Unexpected response from Attendance Server. Check deployment.")
                }
                val json = JSONObject(rawBody)
                val ok = json.optBoolean("ok", json.optString("status", "").equals("OK", ignoreCase = true))
                if (!ok) {
                    val errorMsg = json.optString("msg", "")
                        .ifBlank { json.optString("message", "") }
                        .ifBlank { json.optString("error", "") }
                        .ifBlank { "Request could not be completed. Please verify credentials." }
                    return@withContext ApiResult.Error(errorMsg)
                }
                ApiResult.Success(parser(json))
            }
        } catch (_: IOException) {
            ApiResult.Error("Network error: Unable to reach Govt. ITI Ranni server.")
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Unexpected server communication error.")
        }
    }

    private fun JSONObject.optAttendanceClosed(): Boolean = when {
        has("attendanceClosed") -> optBoolean("attendanceClosed", false)
        has("isClosed") -> optBoolean("isClosed", false)
        has("todayClosed") -> optBoolean("todayClosed", false)
        has("closed") -> optBoolean("closed", false)
        else -> false
    }

    private fun JSONObject.optSafeInt(key: String, default: Int = 0): Int {
        if (!has(key) || isNull(key)) return default
        return when (val raw = opt(key)) {
            is Number -> raw.toDouble().roundToInt()
            is String -> raw.trim().toDoubleOrNull()?.roundToInt() ?: default
            else -> default
        }
    }

    private fun JSONObject.optSafeString(key: String, default: String = ""): String {
        if (!has(key) || isNull(key)) return default
        return opt(key)?.toString()?.trim()?.ifBlank { default } ?: default
    }
}
