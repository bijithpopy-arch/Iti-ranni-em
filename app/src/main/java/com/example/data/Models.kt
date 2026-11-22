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

enum class StaffTab(val label: String) {
    PENDING_APPROVALS("Approvals"),
    ATTENDANCE_REGISTER("Register"),
    PARENT_ALERTS("Alerts"),
    CAMPUS_HUB("Campus"),
    ESP_TERMINAL("ESP")
}

object ParentAlertTypes {
    const val BUNK_ALERT = "BUNK_ALERT"
    const val LEAVE_REASON = "LEAVE_REASON"
}

data class AttendanceSummary(
    val todayStatus: String,
    val inT: String,
    val outT: String,
    val present: Int,
    val half: Int,
    val total: Int,
    val pct: Int
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
    val postedBy: String = "Govt. ITI Ranni"
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
    val isToday: Boolean,
    val dutyItems: List<String>
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
    val pct: Int
)

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

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}
