package com.example.data

enum class UserRole(val displayName: String) {
    TRAINEE("Trainee"),
    PARENT("Parent"),
    TRADE_INSTRUCTOR("Trade Instructor"),
    PRINCIPAL("Principal")
}

enum class TraineeTab(val label: String) {
    HOME("Home"),
    OUT_PASS("Out-Pass"),
    MARKS("Marks"),
    ACADEMICS("Academics"),
    NOTICES("Notices & News")
}

enum class StaffTab(val label: String) {
    ATTENDANCE_REGISTER("Register"),
    APPROVALS_ALERTS("Approvals"),
    MARKS_REGISTER("Marks Table"),
    SHOP_FLOOR("Shop Floor"),
    CAMPUS_HUB("Campus")
}

enum class PrincipalTab(val label: String) {
    SCORECARD("Scorecard"),
    APPROVALS("Approvals"),
    ACADEMICS("Log Books"),
    BROADCAST("Broadcast")
}

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val message: String) : ApiResult<Nothing>
}

data class SimpleActionResult(val message: String)

data class AttendanceSummary(
    val todayStatus: String = "ABSENT",
    val inT: String = "-",
    val outT: String = "-",
    val present: Int = 0,
    val half: Int = 0,
    val total: Int = 0,
    val pct: Int = 100,
    val streakDays: Int = 0
)

data class TraineeLeaveItem(
    val id: String,
    val date: String,
    val type: String,
    val status: String,
    val pdf: String = ""
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
    val category: String = "General",
    val body: String = "",
    val postedBy: String = "Govt. ITI Ranni",
    val isPinned: Boolean = false
)

data class ScanOverrideState(
    val earlyClassLogoutEnabled: Boolean = false,
    val shopFloorReleaseNowEnabled: Boolean = false,
    val updatedAt: String = "",
    val updatedBy: String = ""
)

data class ShopFloorEntry(
    val id: String,
    val date: String,
    val roll: String,
    val name: String,
    val attendancePct: Int = 0,
    val sessionType: String = "POST_3PM",
    val scheduledEndTime: String = "05:00 PM",
    val assignedReason: String = "Auto (<80%)",
    val status: String = "SCHEDULED",
    val checkInTime: String = "03:00 PM",
    val checkOutCardScanTime: String = "",
    val durationMinutes: Int = 0,
    val verifiedByInstructor: Boolean = false,
    val isManualOverride: Boolean = false
)

object ParentAlertTypes {
    const val BUNK_ALERT = "BUNK_ALERT"
    const val LEAVE_REASON = "LEAVE_REASON"
    const val INSTRUCTOR_WARNING = "INSTRUCTOR_WARNING"
    const val MARKS_PUBLISHED = "MARKS_PUBLISHED"
    const val MARKS_UPDATED = "MARKS_UPDATED"
    const val TRAINEE_RUNNING_LATE = "TRAINEE_RUNNING_LATE"
}

data class ParentAlertItem(
    val id: String,
    val date: String,
    val time: String = "",
    val roll: String = "",
    val studentName: String = "Trainee",
    val alertType: String = ParentAlertTypes.LEAVE_REASON,
    val message: String = ""
)

data class ToolCribLogEntry(
    val id: String,
    val date: String,
    val time: String = "",
    val itemName: String,
    val conditionStatus: String = "DAMAGED",
    val notes: String = "",
    val reportedBy: String = "Trade Instructor"
)

data class DutySignOffRecord(
    val date: String,
    val pairLabel: String,
    val signedOff: Boolean = false,
    val signedAt: String = "",
    val note: String = ""
)

data class DutyOverrideEntry(
    val dateKey: String,
    val firstRoll: Int,
    val secondRoll: Int,
    val dutyNote: String = "",
    val reanchorFromHere: Boolean = false,
    val updatedBy: String = "Trade Instructor",
    val updatedAt: String = ""
)

data class DutyPairSlot(
    val dayNumber: Int = 1,
    val rolls: List<Int> = emptyList(),
    val dateLabel: String = "",
    val dateKey: String = "",
    val isToday: Boolean = false,
    val dutyItems: List<String> = emptyList(),
    val isOverridden: Boolean = false,
    val dutyNote: String = ""
)

data class MonthlyExamMark(
    val id: String,
    val monthLabel: String,
    val examDate: String = "",
    val roll: String,
    val studentName: String,
    val theoryMarks: Int,
    val theoryMax: Int = 50,
    val labMarks: Int,
    val labMax: Int = 100,
    val remarks: String = "",
    val published: Boolean = true,
    val isRevised: Boolean = false,
    val signedBy: String = "",
    val signedAt: String = "",
    val signatureData: String = "",
    val updatedAt: String = ""
) {
    val totalMarks: Int get() = theoryMarks + labMarks
    val totalMax: Int get() = (theoryMax + labMax).coerceAtLeast(1)
    val percentage: Int get() = ((totalMarks * 100f) / totalMax).toInt().coerceIn(0, 100)
}

data class WorkMarkQuery(
    val id: String,
    val date: String,
    val time: String = "",
    val roll: String,
    val studentName: String,
    val monthLabel: String = "",
    val workReference: String = "Workshop / Lab Practical",
    val currentMarkNote: String = "",
    val traineeNote: String = "",
    val status: String = "OPEN",
    val instructorReply: String = "",
    val updatedAt: String = ""
)

data class RunningLateAlert(
    val id: String,
    val date: String,
    val time: String = "",
    val roll: String,
    val studentName: String,
    val expectedArrival: String = "Soon",
    val reasonCategory: String = "Delay",
    val details: String = "",
    val status: String = "ACTIVE"
)

object AcademicSubmissionStatuses {
    const val PENDING_VERIFICATION = "PENDING_VERIFICATION"
    const val VERIFIED = "VERIFIED"
    const val FORWARDED_TO_PRINCIPAL = "FORWARDED_TO_PRINCIPAL"
}

data class AcademicSubmission(
    val id: String,
    val roll: String,
    val studentName: String,
    val theoryTopic: String = "",
    val theoryStatus: String = "Pending",
    val theoryUpdatedDate: String = "",
    val labJobName: String = "",
    val labStatus: String = "Incomplete",
    val labSignedDate: String = "",
    val remarks: String = "",
    val verificationStatus: String = AcademicSubmissionStatuses.PENDING_VERIFICATION,
    val verifiedBy: String = "",
    val verifiedAt: String = "",
    val instructorRemark: String = "",
    val sentToPrincipal: Boolean = false,
    val forwardedAt: String = "",
    val submittedAt: String = ""
)

data class ProjectGroup(
    val groupId: String,
    val projectTitle: String,
    val leaderRoll: String = "",
    val leaderName: String = "",
    val memberRolls: List<String> = emptyList(),
    val submittedByRoll: String = "",
    val createdAt: String = ""
)

data class ProjectGroupsPayload(
    val enabled: Boolean = false,
    val groups: List<ProjectGroup> = emptyList()
)

data class TechNewsItem(
    val id: String,
    val titleEn: String,
    val titleMl: String,
    val summaryEn: String,
    val summaryMl: String,
    val category: String = "Electronics",
    val source: String = "IEEE / Industry",
    val publishedDate: String = "",
    val url: String = ""
)

data class TimetablePeriod(
    val day: String,
    val slot: String,
    val subject: String,
    val roomOrLab: String = "EM Lab"
)

object LateGraceModes {
    const val CUSTOM_CUTOFF = "CUSTOM_CUTOFF"
    const val EXEMPT_TODAY = "EXEMPT_TODAY"
    const val CLEAR = "CLEAR"
}

data class StaffAttendanceRecord(
    val roll: String,
    val name: String,
    val todayStatus: String = "ABSENT",
    val inT: String = "-",
    val outT: String = "-",
    val present: Int = 0,
    val half: Int = 0,
    val total: Int = 0,
    val pct: Int = 100,
    val lateGraceMode: String = "",
    val lateCutoff: String = "",
    val lateGraceNote: String = ""
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

data class AttendanceUndoRecord(
    val roll: String,
    val studentName: String,
    val previousStatus: String,
    val previousInT: String,
    val previousOutT: String,
    val newStatus: String,
    val date: String
)

data class DuplicatePunchPrompt(
    val roll: String,
    val studentName: String,
    val currentStatus: String,
    val requestedStatus: String
)

data class EspTapResult(
    val roll: String,
    val mode: String,
    val name: String,
    val time: String,
    val message: String = ""
)

data class PdfExportResult(val pdfUrl: String, val message: String)
data class LeaveSubmitResult(val reqId: String, val message: String)
data class ParentAlertSubmitResult(val alertId: String, val message: String)
data class HolidayPostResult(val holiday: HolidayAnnouncement, val message: String)
data class NoticePostResult(val notice: NoticeItem, val message: String)
data class ProjectGroupSaveResult(val group: ProjectGroup, val message: String)
data class ProjectAccessResult(val enabled: Boolean, val message: String)
data class SignApprovalResult(val reqId: String, val message: String, val pdfUrl: String = "")
data class CloseAttendanceResult(
    val attendanceClosed: Boolean,
    val date: String,
    val closedAt: String,
    val absentCount: Int,
    val halfDayCount: Int,
    val message: String
)
data class ScanOverrideResult(val state: ScanOverrideState, val message: String)
data class ToolCribLogResult(val entry: ToolCribLogEntry, val message: String)
data class DutySignOffResult(val record: DutySignOffRecord, val message: String)

data class TraineeDashboardData(
    val roll: String,
    val name: String,
    val attendance: AttendanceSummary,
    val leaves: List<TraineeLeaveItem> = emptyList(),
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
    val myAcademicSubmission: AcademicSubmission? = null,
    val dutyOverrides: Map<String, DutyOverrideEntry> = emptyMap(),
    val timetable: List<TimetablePeriod> = emptyList(),
    val traineeTimetableEditEnabled: Boolean = false,
    val publishedReportUrl: String = "",
    val publishedReportAt: String = ""
)

data class ParentDashboardData(
    val roll: String,
    val studentName: String,
    val attendance: AttendanceSummary,
    val attendanceClosed: Boolean = false,
    val attendanceDate: String = "",
    val closedAt: String = "",
    val todayHoliday: HolidayAnnouncement? = null,
    val holidays: List<HolidayAnnouncement> = emptyList(),
    val notices: List<NoticeItem> = emptyList(),
    val pinnedBroadcast: NoticeItem? = null,
    val wardShopFloorEntry: ShopFloorEntry? = null,
    val parentAlerts: List<ParentAlertItem> = emptyList(),
    val monthlyMarks: List<MonthlyExamMark> = emptyList(),
    val publishedReportUrl: String = "",
    val publishedReportAt: String = ""
)

data class StaffDashboardData(
    val allAttendance: List<StaffAttendanceRecord> = emptyList(),
    val pendingLeaves: List<PendingLeaveRequest> = emptyList(),
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
    val academicSubmissions: List<AcademicSubmission> = emptyList(),
    val dutyOverrides: Map<String, DutyOverrideEntry> = emptyMap(),
    val timetable: List<TimetablePeriod> = emptyList(),
    val traineeTimetableEditEnabled: Boolean = false,
    val publishedReportUrl: String = "",
    val publishedReportAt: String = ""
)
