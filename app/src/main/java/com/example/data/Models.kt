package com.example.data

enum class UserRole(val displayName: String) {
    TRAINEE("Trainee"),
    PARENT("Parent"),
    TRADE_INSTRUCTOR("Trade Instructor"),
    PRINCIPAL("Principal")
}

enum class TraineeTab(val label: String) {
    HOME("Home"),
    ACADEMIC_RECORDS("LogBook"),
    MARKS("Marks"),
    NOTICES("Notices"),
    DUTY_CHART("Duty"),
    PROJECT_GROUPS("Projects")
}

/**
 * Trade Instructor workspace tabs (Daily Workshop & Operations)
 */
enum class StaffTab(val label: String) {
    ATTENDANCE_REGISTER("Register"),
    ACADEMIC_RECORDS("LogBook"),
    SHOP_FLOOR("ShopFlr"),
    PENDING_APPROVALS("Approve"),
    PARENT_ALERTS("Alerts"),
    CAMPUS_HUB("Hub")
}

/**
 * Principal executive oversight tabs (Scorecard, Log Book, Audit, Approvals, Broadcasts)
 */
enum class PrincipalTab(val label: String) {
    SCORECARD("Scorecard"),
    ACADEMIC_RECORDS("LogBook"),
    SHOP_FLOOR_AUDIT("Audit"),
    LEAVE_APPROVALS("Approve"),
    BROADCASTS("Broadcast")
}

object ParentAlertTypes {
    const val BUNK_ALERT = "BUNK_ALERT"
    const val LEAVE_REASON = "LEAVE_REASON"
    const val LOW_ATTENDANCE_WARNING = "LOW_ATTENDANCE_WARNING"
    const val ABSENCE_ALERT = "ABSENCE_ALERT"
    const val MARKS_PUBLISHED = "MARKS_PUBLISHED"
    const val MARKS_UPDATED = "MARKS_UPDATED"
    const val TRAINEE_RUNNING_LATE = "TRAINEE_RUNNING_LATE"
}

object LateGraceModes {
    const val CUSTOM_CUTOFF = "CUSTOM_CUTOFF"
    const val EXEMPT_TODAY = "EXEMPT_TODAY"
    const val CLEAR = "CLEAR"
}

data class AttendanceSummary(
    val todayStatus: String,
    val inT: String,
    val outT: String,
    val present: Int,
    val half: Int,
    val total: Int,
    val pct: Int,
    val streakDays: Int = 0
)

data class TraineeLeaveItem(
    val id: String,
    val date: String,
    val type: String,
    val status: String,
    val pdf: String
)

data class HolidayAnnouncement(
    val id: String,
    val date: String,
    val reason: String,
    val postedBy: String = "Trade Instructor",
    val postedAt: String = ""
)

data class NoticeItem(
    val id: String,
    val title: String,
    val date: String,
    val category: String,
    val body: String,
    val postedBy: String = "Govt. ITI Ranni",
    val isPinned: Boolean = false
)

data class ProjectGroup(
    val groupId: String,
    val projectTitle: String,
    val leaderRoll: String,
    val leaderName: String,
    val memberRolls: List<String>,
    val submittedByRoll: String = "",
    val createdAt: String = ""
)

data class DutyPairSlot(
    val dayNumber: Int,
    val rolls: List<Int>,
    val dateLabel: String,
    val dateKey: String = "",
    val isToday: Boolean,
    val dutyItems: List<String>
)

/**
 * Shop Floor makeup session record (auto-flagged <80% or manually assigned by Instructor)
 * for Saturday or post-3:00 PM sessions with RFID ID-card logout tracking.
 */
data class ShopFloorEntry(
    val id: String = "",
    val date: String = "",
    val roll: String,
    val name: String,
    val attendancePct: Int,
    val sessionType: String = "POST_3PM", // "POST_3PM" or "SATURDAY"
    val scheduledEndTime: String = "05:00 PM", // "04:00 PM" or "05:00 PM"
    val assignedReason: String = "Auto (<80%)",
    val status: String = "SCHEDULED", // "SCHEDULED", "IN_SESSION", "COMPLETED"
    val checkInTime: String = "03:00 PM",
    val checkOutCardScanTime: String = "",
    val durationMinutes: Int = 0,
    val verifiedByInstructor: Boolean = false,
    val isManualOverride: Boolean = false
)

/**
 * Instructor-triggered real-time RFID/ID-card scan overrides stored in Google Sheets / ScriptProperties.
 */
data class ScanOverrideState(
    val earlyClassLogoutEnabled: Boolean = false,
    val shopFloorReleaseNowEnabled: Boolean = false,
    val updatedAt: String = "",
    val updatedBy: String = ""
)

data class ToolCribLogEntry(
    val id: String,
    val date: String,
    val time: String,
    val itemName: String,
    val conditionStatus: String, // "MISSING", "DAMAGED", "OK / RETURNED"
    val notes: String,
    val reportedBy: String = "Trade Instructor"
)

data class DutySignOffRecord(
    val date: String,
    val pairLabel: String,
    val signedOff: Boolean,
    val signedAt: String = "",
    val note: String = ""
)

data class MonthlyExamMark(
    val id: String = "",
    val monthLabel: String,
    val examDate: String = "",
    val roll: String,
    val studentName: String = "",
    val theoryMarks: Int = 0,
    val theoryMax: Int = 50,
    val labMarks: Int = 0,
    val labMax: Int = 50,
    val remarks: String = "",
    val published: Boolean = true,
    val isRevised: Boolean = false,
    val updatedAt: String = ""
) {
    val totalObtained: Int get() = (theoryMarks.coerceAtLeast(0) + labMarks.coerceAtLeast(0))
    val totalMax: Int get() = (theoryMax.coerceAtLeast(0) + labMax.coerceAtLeast(0)).coerceAtLeast(1)
    val percentage: Int get() = ((totalObtained.toDouble() / totalMax.toDouble()) * 100.0).toInt().coerceIn(0, 100)
}

data class WorkMarkQuery(
    val id: String = "",
    val date: String = "",
    val time: String = "",
    val roll: String,
    val studentName: String = "",
    val monthLabel: String = "",
    val workReference: String,
    val currentMarkNote: String = "",
    val traineeNote: String,
    val status: String = "OPEN", // "OPEN", "REVIEWED", "RESOLVED"
    val instructorReply: String = "",
    val updatedAt: String = ""
)

data class RunningLateAlert(
    val id: String = "",
    val date: String = "",
    val time: String = "",
    val roll: String,
    val studentName: String = "",
    val expectedArrival: String,
    val reasonCategory: String,
    val details: String = "",
    val status: String = "ACTIVE"
)

object AcademicSubmissionStatuses {
    const val NOT_SUBMITTED = "NOT_SUBMITTED"
    const val PENDING_VERIFICATION = "PENDING_VERIFICATION"
    const val VERIFIED = "VERIFIED"
    const val FORWARDED_TO_PRINCIPAL = "FORWARDED_TO_PRINCIPAL"
}

data class AcademicSubmission(
    val id: String = "",
    val roll: String,
    val studentName: String = "",
    val theoryTopic: String = "",
    val theoryStatus: String = "Pending", // "Completed" / "In Progress" / "Pending"
    val theoryUpdatedDate: String = "",
    val labJobName: String = "",
    val labStatus: String = "Incomplete", // "Signed by Instructor" / "Submitted for Sign" / "Incomplete"
    val labSignedDate: String = "",
    val remarks: String = "",
    val verificationStatus: String = AcademicSubmissionStatuses.PENDING_VERIFICATION,
    val verifiedBy: String = "",
    val verifiedAt: String = "",
    val instructorRemark: String = "",
    val sentToPrincipal: Boolean = false,
    val forwardedAt: String = "",
    val submittedAt: String = ""
) {
    val isVerifiedOrForwarded: Boolean
        get() = verificationStatus.equals(AcademicSubmissionStatuses.VERIFIED, ignoreCase = true) ||
            verificationStatus.equals(AcademicSubmissionStatuses.FORWARDED_TO_PRINCIPAL, ignoreCase = true) ||
            sentToPrincipal

    val isForwarded: Boolean
        get() = sentToPrincipal ||
            verificationStatus.equals(AcademicSubmissionStatuses.FORWARDED_TO_PRINCIPAL, ignoreCase = true)
}

data class TraineeDashboardData(
    val roll: String,
    val name: String,
    val attendance: AttendanceSummary,
    val leaves: List<TraineeLeaveItem>,
    val attendanceClosed: Boolean = false,
    val attendanceDate: String = "",
    val closedAt: String = "",
    val todayHoliday: HolidayAnnouncement? = null,
    val holidays: List<HolidayAnnouncement> = emptyList(),
    val notices: List<NoticeItem> = emptyList(),
    val pinnedBroadcast: NoticeItem? = null,
    val myShopFloorEntry: ShopFloorEntry? = null,
    val scanOverrides: ScanOverrideState = ScanOverrideState(),
    val projectGroupsEnabled: Boolean = false,
    val projectGroups: List<ProjectGroup> = emptyList(),
    val dutyItems: List<String> = emptyList(),
    val monthlyMarks: List<MonthlyExamMark> = emptyList(),
    val myWorkMarkQueries: List<WorkMarkQuery> = emptyList(),
    val myRunningLateAlerts: List<RunningLateAlert> = emptyList(),
    val myAcademicSubmission: AcademicSubmission? = null
)

data class ParentAlertItem(
    val id: String,
    val date: String,
    val time: String,
    val roll: String,
    val studentName: String,
    val alertType: String,
    val message: String
)

data class ParentDashboardData(
    val roll: String,
    val studentName: String,
    val attendance: AttendanceSummary,
    val attendanceClosed: Boolean,
    val attendanceDate: String,
    val closedAt: String,
    val todayHoliday: HolidayAnnouncement? = null,
    val holidays: List<HolidayAnnouncement> = emptyList(),
    val notices: List<NoticeItem> = emptyList(),
    val pinnedBroadcast: NoticeItem? = null,
    val wardShopFloorEntry: ShopFloorEntry? = null,
    val parentAlerts: List<ParentAlertItem>,
    val monthlyMarks: List<MonthlyExamMark> = emptyList()
)

data class ParentAlertSubmitResult(
    val alertId: String,
    val message: String
)

data class CloseAttendanceResult(
    val attendanceClosed: Boolean,
    val date: String,
    val closedAt: String,
    val absentCount: Int,
    val halfDayCount: Int,
    val message: String
)

data class HolidayPostResult(
    val holiday: HolidayAnnouncement,
    val message: String
)

data class NoticePostResult(
    val notice: NoticeItem,
    val message: String
)

data class ProjectAccessResult(
    val enabled: Boolean,
    val message: String
)

data class ProjectGroupSaveResult(
    val group: ProjectGroup,
    val message: String
)

data class ProjectGroupsPayload(
    val enabled: Boolean,
    val groups: List<ProjectGroup>
)

data class StaffAttendanceRecord(
    val roll: String,
    val name: String,
    val todayStatus: String,
    val inT: String,
    val outT: String,
    val present: Int,
    val half: Int,
    val total: Int,
    val pct: Int,
    val lateGraceMode: String = "",
    val lateCutoff: String = "",
    val lateGraceNote: String = ""
) {
    val hasActiveLateGrace: Boolean
        get() {
            val mode = lateGraceMode.trim().uppercase()
            return mode == LateGraceModes.CUSTOM_CUTOFF || mode == LateGraceModes.EXEMPT_TODAY
        }

    val sessionsNeededFor80Pct: Int
        get() = AttendanceMath.sessionsNeededForTargetPct(
            present = present,
            half = half,
            total = total,
            targetPct = 80.0
        )

    val hasPunchedToday: Boolean
        get() = AttendanceMath.hasTodayPunch(todayStatus = todayStatus, inT = inT)
}

data class PendingLeaveRequest(
    val reqId: String,
    val date: String,
    val roll: String,
    val name: String,
    val type: String,
    val timeWindow: String,
    val reason: String
)

data class StaffDashboardData(
    val allAttendance: List<StaffAttendanceRecord>,
    val pendingLeaves: List<PendingLeaveRequest>,
    val parentAlerts: List<ParentAlertItem> = emptyList(),
    val attendanceClosed: Boolean = false,
    val attendanceDate: String = "",
    val closedAt: String = "",
    val todayHoliday: HolidayAnnouncement? = null,
    val holidays: List<HolidayAnnouncement> = emptyList(),
    val notices: List<NoticeItem> = emptyList(),
    val pinnedBroadcast: NoticeItem? = null,
    val shopFloorList: List<ShopFloorEntry> = emptyList(),
    val scanOverrides: ScanOverrideState = ScanOverrideState(),
    val toolCribLogs: List<ToolCribLogEntry> = emptyList(),
    val todayDutySignOff: DutySignOffRecord? = null,
    val lastScannerSyncTime: String = "",
    val projectGroupsEnabled: Boolean = false,
    val projectGroups: List<ProjectGroup> = emptyList(),
    val monthlyMarks: List<MonthlyExamMark> = emptyList(),
    val workMarkQueries: List<WorkMarkQuery> = emptyList(),
    val runningLateAlerts: List<RunningLateAlert> = emptyList(),
    val academicSubmissions: List<AcademicSubmission> = emptyList()
)

data class LeaveSubmitResult(
    val reqId: String,
    val message: String
)

data class SignApprovalResult(
    val reqId: String,
    val message: String,
    val pdfUrl: String
)

data class PdfExportResult(
    val pdfUrl: String,
    val message: String
)

data class EspTapResult(
    val roll: String,
    val mode: String,
    val name: String,
    val time: String,
    val message: String
)

data class SimpleActionResult(
    val message: String
)

data class ScanOverrideResult(
    val state: ScanOverrideState,
    val message: String
)

data class ToolCribLogResult(
    val entry: ToolCribLogEntry,
    val message: String
)

data class DutySignOffResult(
    val record: DutySignOffRecord,
    val message: String
)

data class AttendanceUndoRecord(
    val roll: String,
    val studentName: String,
    val previousStatus: String,
    val previousInT: String,
    val previousOutT: String,
    val newStatus: String,
    val changedAtLabel: String
)

data class DuplicatePunchPrompt(
    val roll: String,
    val studentName: String,
    val existingStatus: String,
    val existingInTime: String,
    val requestedStatus: String
)

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}
