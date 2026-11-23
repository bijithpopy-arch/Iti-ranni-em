package com.example.data

enum class UserRole(val displayName: String) {
    TRAINEE("Trainee"),
    PARENT("Parent"),
    TRADE_INSTRUCTOR("Trade Instructor"),
    PRINCIPAL("Principal")
}

enum class TraineeTab(val label: String) {
    HOME("Home"),
    NOTICES("Notices"),
    DUTY_CHART("Duty"),
    PROJECT_GROUPS("Projects")
}

/**
 * Trade Instructor workspace tabs (Daily Workshop & Operations)
 */
enum class StaffTab(val label: String) {
    ATTENDANCE_REGISTER("Register"),
    SHOP_FLOOR("Shop Floor"),
    PENDING_APPROVALS("Approvals"),
    PARENT_ALERTS("Alerts"),
    CAMPUS_HUB("Ops & Hub")
}

/**
 * Principal executive oversight tabs (Read-only Scorecard, Audit, Approvals, Broadcasts)
 */
enum class PrincipalTab(val label: String) {
    SCORECARD("Scorecard"),
    SHOP_FLOOR_AUDIT("Audit Log"),
    LEAVE_APPROVALS("Approvals"),
    BROADCASTS("Broadcasts")
}

object ParentAlertTypes {
    const val BUNK_ALERT = "BUNK_ALERT"
    const val LEAVE_REASON = "LEAVE_REASON"
    const val LOW_ATTENDANCE_WARNING = "LOW_ATTENDANCE_WARNING"
    const val ABSENCE_ALERT = "ABSENCE_ALERT"
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
    val dutyItems: List<String> = emptyList()
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
    val parentAlerts: List<ParentAlertItem>
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
    val projectGroups: List<ProjectGroup> = emptyList()
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

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}
