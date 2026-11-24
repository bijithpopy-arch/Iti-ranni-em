package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApiResult
import com.example.data.AttendanceApiClient
import com.example.data.AttendanceUndoRecord
import com.example.data.DuplicatePunchPrompt
import com.example.data.EspTapResult
import com.example.data.OfflineSyncQueue
import com.example.data.ParentAlertTypes
import com.example.data.ParentDashboardData
import com.example.data.PrincipalTab
import com.example.data.StaffDashboardData
import com.example.data.StaffTab
import com.example.data.TechNewsItem
import com.example.data.TechnicalNewsClient
import com.example.data.TimetablePeriod
import com.example.data.TimetableStore
import com.example.data.TraineeDashboardData
import com.example.data.TraineeTab
import com.example.data.UserRole
import com.example.notifications.AttendanceNotificationHelper
import com.example.notifications.BackgroundSyncConfig
import com.example.notifications.BackgroundSyncScheduler
import com.example.security.SessionPreferences
import com.example.ui.screens.ExamMarkRow
import com.example.ui.screens.NewsLanguageFilter
import com.example.ui.screens.RemoteAppConfig
import com.example.ui.screens.RemoteUiConfig
import com.example.updater.AppUpdateInfo
import com.example.updater.AppUpdater
import com.example.updater.UpdateCheckResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SessionScreen {
    data object Login : SessionScreen
    data object TraineeDashboard : SessionScreen
    data object ParentDashboard : SessionScreen
    data object StaffDashboard : SessionScreen
    data object PrincipalDashboard : SessionScreen
    data object DeveloperDashboard : SessionScreen
}

data class AppUiState(
    val screen: SessionScreen = SessionScreen.Login,

    // Login & Remember-Me / Biometrics
    val selectedRole: UserRole = UserRole.TRAINEE,
    val rollInput: String = "",
    val pinInput: String = "",
    val rememberMe: Boolean = false,
    val biometricEnabled: Boolean = false,
    val hasSavedCredentials: Boolean = false,
    val savedRoleLabel: String = "",
    val biometricMessage: String? = null,
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,

    // Remote Developer Config, Live UI Config & Bug Reports
    val remoteAppConfig: RemoteAppConfig = RemoteAppConfig(),
    val remoteUiConfig: RemoteUiConfig = RemoteUiConfig(),
    val bugReports: List<Map<String, String>> = emptyList(),
    val developerFeedbackMessage: String? = null,

    // In-App Updater
    val isCheckingUpdate: Boolean = false,
    val availableUpdate: AppUpdateInfo? = null,
    val showUpdateDialog: Boolean = false,
    val updateStatusMessage: String? = null,

    // Active session credentials
    val activeRoll: String = "",
    val activePin: String = "",
    val activeStaffRole: UserRole = UserRole.TRADE_INSTRUCTOR,

    // Notifications
    val unreadNoticeCount: Int = 0,

    // Trainee state
    val traineeData: TraineeDashboardData? = null,
    val selectedTraineeTab: TraineeTab = TraineeTab.HOME,
    val isRefreshingTrainee: Boolean = false,
    val traineeBannerError: String? = null,
    val isSubmittingLeave: Boolean = false,
    val leaveSubmitReqId: String? = null,
    val leaveSubmitMessage: String? = null,
    val leaveSubmitError: String? = null,

    // Trainee project groups
    val isSavingProjectGroup: Boolean = false,
    val projectGroupSaveMessage: String? = null,
    val projectGroupSaveError: String? = null,

    // Work queries & Running late
    val isSubmittingWorkQuery: Boolean = false,
    val workQueryMessage: String? = null,
    val workQueryError: String? = null,
    val isSubmittingRunningLate: Boolean = false,
    val runningLateMessage: String? = null,
    val runningLateError: String? = null,

    // Tech News (EN + ML)
    val techNewsItems: List<TechNewsItem> = emptyList(),
    val isLoadingTechNews: Boolean = false,
    val newsLanguageFilter: NewsLanguageFilter = NewsLanguageFilter.BOTH,

    // Academics Log Book
    val isSubmittingAcademicRecord: Boolean = false,
    val isUpdatingAcademicSubmission: Boolean = false,
    val academicActionMessage: String? = null,
    val academicActionError: String? = null,

    // Parent state
    val parentData: ParentDashboardData? = null,
    val isRefreshingParent: Boolean = false,
    val parentBannerError: String? = null,
    val isSubmittingParentAlert: Boolean = false,
    val parentAlertSuccessMessage: String? = null,
    val parentAlertError: String? = null,
    val parentNotificationDispatched: Boolean = false,
    val parentMarksNotificationDispatched: Boolean = false,

    // Staff & Principal state
    val staffData: StaffDashboardData? = null,
    val selectedStaffTab: StaffTab = StaffTab.ATTENDANCE_REGISTER,
    val selectedPrincipalTab: PrincipalTab = PrincipalTab.SCORECARD,
    val isRefreshingStaff: Boolean = false,
    val staffBannerError: String? = null,

    // Register operations
    val isClosingAttendance: Boolean = false,
    val closeAttendanceMessage: String? = null,
    val closeAttendanceError: String? = null,
    val isRunningRollCallOp: Boolean = false,
    val rollCallFeedbackMessage: String? = null,
    val rollCallError: String? = null,

    // Manual punch & duplicate guard
    val manualPunchingRoll: String? = null,
    val undoingRoll: String? = null,
    val duplicatePunchPrompt: DuplicatePunchPrompt? = null,
    val recentUndoByRoll: Map<String, AttendanceUndoRecord> = emptyMap(),

    // Monthly Marks & Queries
    val isSavingMonthlyMark: Boolean = false,
    val monthlyMarkMessage: String? = null,
    val monthlyMarkError: String? = null,
    val replyingWorkQueryId: String? = null,
    val workQueryReplyMessage: String? = null,
    val workQueryReplyError: String? = null,

    // Shop Floor & Overrides
    val isUpdatingShopFloor: Boolean = false,
    val shopFloorActionMessage: String? = null,
    val shopFloorActionError: String? = null,

    // Duty Override
    val isSavingDutyOverride: Boolean = false,
    val dutyOverrideMessage: String? = null,
    val dutyOverrideError: String? = null,

    // Timetable
    val officialTimetableCache: List<TimetablePeriod> = TimetableStore.defaultWeeklyTimetable(),
    val traineeCustomTimetable: List<TimetablePeriod>? = null,
    val isSavingTimetable: Boolean = false,
    val timetableFeedbackMessage: String? = null,
    val timetableError: String? = null,

    // Notices & Holidays
    val isPostingHoliday: Boolean = false,
    val holidayPostMessage: String? = null,
    val holidayPostError: String? = null,
    val isPostingNotice: Boolean = false,
    val noticePostMessage: String? = null,
    val noticePostError: String? = null,

    // Approvals
    val signingReqId: String? = null,
    val approvalSuccessMessage: String? = null,
    val approvalError: String? = null,

    // Register search/filter & PDF
    val registerSearchQuery: String = "",
    val registerStatusFilter: String = "ALL",
    val isExportingPdf: Boolean = false,

    // ESP Terminal
    val espRollInput: String = "",
    val isTappingEsp: Boolean = false,
    val lastEspTap: EspTapResult? = null,
    val espTapHistory: List<EspTapResult> = emptyList(),
    val espTapError: String? = null
)

class AppViewModel(
    private val apiClient: AttendanceApiClient = AttendanceApiClient(),
    private val newsClient: TechnicalNewsClient = TechnicalNewsClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AppUiState(techNewsItems = newsClient.curatedBilingualBriefs())
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var sessionLoadedOnce = false

    fun loadSavedSessionIfNeeded(context: Context): Boolean {
        if (sessionLoadedOnce) return _uiState.value.hasSavedCredentials && _uiState.value.biometricEnabled
        sessionLoadedOnce = true
        val saved = SessionPreferences.load(context)
        if (saved.hasCredentials) {
            BackgroundSyncConfig.save(context, saved.role, saved.roll, saved.pin)
            BackgroundSyncScheduler.ensureScheduled(context)
        }
        _uiState.update { state ->
            state.copy(
                selectedRole = if (saved.rememberMe) saved.role else state.selectedRole,
                rollInput = if (saved.rememberMe) saved.roll else state.rollInput,
                pinInput = if (saved.rememberMe) saved.pin else state.pinInput,
                rememberMe = saved.rememberMe,
                biometricEnabled = saved.biometricEnabled,
                hasSavedCredentials = saved.hasCredentials,
                savedRoleLabel = if (saved.hasCredentials) {
                    if (saved.roll.isNotBlank()) "${saved.role.displayName} (Roll #${saved.roll})"
                    else saved.role.displayName
                } else ""
            )
        }
        return saved.hasCredentials && saved.biometricEnabled
    }

    fun onRoleInputChanged(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role, loginError = null) }
    }

    fun onRollInputChanged(roll: String) {
        _uiState.update { it.copy(rollInput = roll, loginError = null) }
    }

    fun onPinInputChanged(pin: String) {
        _uiState.update { it.copy(pinInput = pin, loginError = null) }
    }

    fun onRememberMeChanged(enabled: Boolean) {
        _uiState.update { it.copy(rememberMe = enabled, biometricEnabled = if (!enabled) false else it.biometricEnabled) }
    }

    fun onBiometricEnabledChanged(enabled: Boolean) {
        _uiState.update { it.copy(biometricEnabled = enabled, rememberMe = if (enabled) true else it.rememberMe) }
    }

    fun setBiometricMessage(message: String?) {
        _uiState.update { it.copy(biometricMessage = message) }
    }

    fun clearSavedSession(context: Context) {
        SessionPreferences.clearAll(context)
        BackgroundSyncConfig.clear(context)
        BackgroundSyncScheduler.cancelAll(context)
        _uiState.update {
            it.copy(
                rememberMe = false,
                biometricEnabled = false,
                hasSavedCredentials = false,
                savedRoleLabel = "",
                rollInput = "",
                pinInput = "",
                biometricMessage = "Saved login credentials cleared."
            )
        }
    }

    // ---------- Hidden Developer Mode (9037) ----------

    fun openDeveloperConsole(pin: String): Boolean {
        val expected = _uiState.value.remoteAppConfig.developerPin
        if (pin.trim() != expected) return false
        _uiState.update { it.copy(screen = SessionScreen.DeveloperDashboard, loginError = null) }
        return true
    }

    fun saveRemoteAppConfig(updates: Map<String, String>) {
        val cur = _uiState.value.remoteAppConfig
        val merged = mutableMapOf(
            "developerPin" to cur.developerPin,
            "updaterEnabled" to cur.updaterEnabled.toString(),
            "releaseUpdateToUsers" to cur.releaseUpdateToUsers.toString(),
            "instructorAccessEnabled" to cur.instructorAccessEnabled.toString(),
            "principalAccessEnabled" to cur.principalAccessEnabled.toString(),
            "maintenanceMode" to cur.maintenanceMode.toString(),
            "maintenanceBanner" to cur.maintenanceBanner,
            "examMarksEnabled" to cur.examMarksEnabled.toString(),
            "shopFloorEnabled" to cur.shopFloorEnabled.toString(),
            "undoEnabled" to cur.undoEnabled.toString(),
            "alertRepliesEnabled" to cur.alertRepliesEnabled.toString()
        ).apply { putAll(updates) }
        _uiState.update { it.copy(remoteAppConfig = RemoteAppConfig.fromMap(merged), developerFeedbackMessage = "Configuration saved.") }
    }

    fun saveRemoteUiConfig(updates: Map<String, String>, screen: String = "Global") {
        val merged = _uiState.value.remoteUiConfig.rawOverrides.toMutableMap().apply { putAll(updates) }
        _uiState.update { it.copy(remoteUiConfig = RemoteUiConfig.fromMap(merged), developerFeedbackMessage = "Live UI updated ($screen).") }
    }

    fun submitBugReport(screenName: String, reportedBy: String, role: String, appVersion: String, description: String) {
        val report = mapOf(
            "BugId" to "BUG-${System.currentTimeMillis()}",
            "ScreenName" to screenName,
            "ReportedBy" to reportedBy,
            "Role" to role,
            "AppVersion" to appVersion,
            "Description" to description,
            "Status" to "OPEN",
            "DevReply" to ""
        )
        _uiState.update { it.copy(bugReports = listOf(report) + it.bugReports) }
    }

    fun resolveBugReport(bugId: String, reply: String) {
        _uiState.update { s ->
            s.copy(bugReports = s.bugReports.map { if (it["BugId"] == bugId) it + mapOf("Status" to "FIXED", "DevReply" to reply) else it })
        }
    }

    fun editSheetRow(tabName: String, rowIndex: Int, colName: String, newValue: String) {
        _uiState.update { it.copy(developerFeedbackMessage = "Queued override on $tabName[$rowIndex].$colName = $newValue") }
    }

    // ---------- App Updater ----------

    fun checkForAppUpdate(silentIfUpToDate: Boolean = false) {
        if (!_uiState.value.remoteAppConfig.updaterEnabled || _uiState.value.isCheckingUpdate) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true, updateStatusMessage = if (!silentIfUpToDate) "Checking GitHub Releases..." else null) }
            when (val res = AppUpdater.checkForUpdate()) {
                is UpdateCheckResult.UpdateAvailable -> _uiState.update {
                    it.copy(isCheckingUpdate = false, availableUpdate = res.info, showUpdateDialog = true, updateStatusMessage = "Update v${res.info.versionName} ready.")
                }
                is UpdateCheckResult.UpToDate -> _uiState.update {
                    it.copy(isCheckingUpdate = false, availableUpdate = null, showUpdateDialog = !silentIfUpToDate, updateStatusMessage = if (silentIfUpToDate) null else "Up to date (v${res.currentVersion}).")
                }
                is UpdateCheckResult.Error -> _uiState.update {
                    it.copy(isCheckingUpdate = false, showUpdateDialog = !silentIfUpToDate, updateStatusMessage = res.message)
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun openUpdateReleasePage(context: Context) {
        val url = _uiState.value.availableUpdate?.releaseUrl ?: return
        AppUpdater.openReleasePage(context, url)
    }

    // ---------- Login & Logout ----------

    fun login(context: Context) {
        val state = _uiState.value
        val pin = state.pinInput.trim()
        val roll = state.rollInput.trim()

        if (pin == state.remoteAppConfig.developerPin) {
            _uiState.update { it.copy(screen = SessionScreen.DeveloperDashboard, loginError = null) }
            return
        }

        if (pin.isBlank()) {
            _uiState.update { it.copy(loginError = "Please enter your numeric PIN.") }
            return
        }

        if (state.selectedRole == UserRole.TRAINEE || state.selectedRole == UserRole.PARENT) {
            val rollNum = roll.toIntOrNull()
            if (rollNum == null || rollNum !in 1..23) {
                _uiState.update { it.copy(loginError = "Enter a valid Roll Number (1 to 23).") }
                return
            }
        }

        if (state.selectedRole == UserRole.TRADE_INSTRUCTOR && !state.remoteAppConfig.instructorAccessEnabled) {
            _uiState.update { it.copy(loginError = "Instructor portal access is temporarily paused.") }
            return
        }
        if (state.selectedRole == UserRole.PRINCIPAL && !state.remoteAppConfig.principalAccessEnabled) {
            _uiState.update { it.copy(loginError = "Principal portal access is temporarily paused.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, loginError = null) }
            when (state.selectedRole) {
                UserRole.TRAINEE -> {
                    when (val res = apiClient.fetchTraineeData(roll, pin)) {
                        is ApiResult.Success -> {
                            persistSessionIfRemembered(context, state.selectedRole, roll, pin)
                            AttendanceNotificationHelper.evaluateTraineePublishedUpdates(context, res.data, true)
                            _uiState.update {
                                it.copy(
                                    isLoggingIn = false,
                                    screen = SessionScreen.TraineeDashboard,
                                    activeRoll = res.data.roll,
                                    activePin = pin,
                                    traineeData = res.data,
                                    traineeCustomTimetable = TimetableStore.loadTraineeCustomEdits(context, res.data.roll),
                                    unreadNoticeCount = AttendanceNotificationHelper.countUnreadNotices(context, res.data.notices)
                                )
                            }
                            refreshTechnicalNews()
                        }
                        is ApiResult.Error -> _uiState.update { it.copy(isLoggingIn = false, loginError = res.message) }
                    }
                }

                UserRole.PARENT -> {
                    when (val res = apiClient.fetchParentData(roll, pin)) {
                        is ApiResult.Success -> {
                            persistSessionIfRemembered(context, state.selectedRole, roll, pin)
                            _uiState.update {
                                it.copy(
                                    isLoggingIn = false,
                                    screen = SessionScreen.ParentDashboard,
                                    activeRoll = res.data.roll,
                                    activePin = pin,
                                    parentData = res.data
                                )
                            }
                        }
                        is ApiResult.Error -> _uiState.update { it.copy(isLoggingIn = false, loginError = res.message) }
                    }
                }

                UserRole.TRADE_INSTRUCTOR, UserRole.PRINCIPAL -> {
                    when (val res = apiClient.fetchStaffData(pin)) {
                        is ApiResult.Success -> {
                            persistSessionIfRemembered(context, state.selectedRole, "", pin)
                            val targetScreen = if (state.selectedRole == UserRole.PRINCIPAL) {
                                SessionScreen.PrincipalDashboard
                            } else {
                                SessionScreen.StaffDashboard
                            }
                            _uiState.update {
                                it.copy(
                                    isLoggingIn = false,
                                    screen = targetScreen,
                                    activePin = pin,
                                    activeStaffRole = state.selectedRole,
                                    staffData = res.data
                                )
                            }
                        }
                        is ApiResult.Error -> _uiState.update { it.copy(isLoggingIn = false, loginError = res.message) }
                    }
                }
            }
        }
    }

    private fun persistSessionIfRemembered(context: Context, role: UserRole, roll: String, pin: String) {
        val st = _uiState.value
        SessionPreferences.save(context, st.rememberMe, st.biometricEnabled, role, roll, pin)
        if (st.rememberMe) {
            BackgroundSyncConfig.save(context, role, roll, pin)
            BackgroundSyncScheduler.ensureScheduled(context)
        }
    }

    fun logout() {
        _uiState.update {
            it.copy(
                screen = SessionScreen.Login,
                traineeData = null,
                parentData = null,
                staffData = null,
                activePin = "",
                activeRoll = "",
                loginError = null
            )
        }
    }

    // ---------- Trainee Actions ----------

    fun onTraineeTabSelected(tab: TraineeTab, context: Context) {
        _uiState.update { it.copy(selectedTraineeTab = tab) }
        if (tab == TraineeTab.NOTICES) {
            markNoticesSeen(context)
            refreshTechnicalNews()
        }
    }

    fun syncNoticeStateAndPopup(context: Context) {
        val data = _uiState.value.traineeData ?: return
        AttendanceNotificationHelper.evaluateTraineePublishedUpdates(context, data, false)
        _uiState.update { it.copy(unreadNoticeCount = AttendanceNotificationHelper.countUnreadNotices(context, data.notices)) }
    }

    fun markNoticesSeen(context: Context) {
        val data = _uiState.value.traineeData ?: return
        AttendanceNotificationHelper.markNoticesSeen(context, data.notices.firstOrNull()?.id)
        _uiState.update { it.copy(unreadNoticeCount = 0) }
    }

    fun refreshTraineeDashboard() {
        val s = _uiState.value
        if (s.activeRoll.isBlank() || s.activePin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingTrainee = true, traineeBannerError = null) }
            when (val res = apiClient.fetchTraineeData(s.activeRoll, s.activePin)) {
                is ApiResult.Success -> _uiState.update { it.copy(isRefreshingTrainee = false, traineeData = res.data) }
                is ApiResult.Error -> _uiState.update { it.copy(isRefreshingTrainee = false, traineeBannerError = res.message) }
            }
        }
    }

    fun submitLeaveRequest(type: String, timeWindow: String, reason: String, studentSig: String, onSuccessReset: () -> Unit) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingLeave = true, leaveSubmitError = null, leaveSubmitMessage = null) }
            when (val res = apiClient.submitLeave(s.activeRoll, s.activePin, type, timeWindow, reason, studentSig)) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update { it.copy(isSubmittingLeave = false, leaveSubmitReqId = res.data.reqId, leaveSubmitMessage = res.data.message) }
                    refreshTraineeDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSubmittingLeave = false, leaveSubmitError = res.message) }
            }
        }
    }

    fun dismissLeaveFeedback() {
        _uiState.update { it.copy(leaveSubmitMessage = null, leaveSubmitError = null) }
    }

    fun submitTraineeWorkMarkQuery(monthLabel: String, workReference: String, currentMarkNote: String, traineeNote: String, onSuccessReset: () -> Unit) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingWorkQuery = true, workQueryError = null, workQueryMessage = null) }
            when (val res = apiClient.submitWorkMarkQuery(s.activeRoll, s.activePin, monthLabel, workReference, currentMarkNote, traineeNote)) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update { it.copy(isSubmittingWorkQuery = false, workQueryMessage = res.data.message) }
                    refreshTraineeDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSubmittingWorkQuery = false, workQueryError = res.message) }
            }
        }
    }

    fun dismissTraineeWorkMarkQueryFeedback() {
        _uiState.update { it.copy(workQueryMessage = null, workQueryError = null) }
    }

    fun submitTraineeRunningLateAlert(expectedArrival: String, reasonCategory: String, details: String, onSuccessReset: () -> Unit) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingRunningLate = true, runningLateMessage = null, runningLateError = null) }
            when (val res = apiClient.submitRunningLateAlert(s.activeRoll, s.activePin, expectedArrival, reasonCategory, details)) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update { it.copy(isSubmittingRunningLate = false, runningLateMessage = res.data.message) }
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSubmittingRunningLate = false, runningLateError = res.message) }
            }
        }
    }

    fun dismissTraineeRunningLateAlertFeedback() {
        _uiState.update { it.copy(runningLateMessage = null, runningLateError = null) }
    }

    fun submitTraineeAcademicRecord(theoryTopic: String, theoryStatus: String, theoryUpdatedDate: String, labJobName: String, labStatus: String, labSignedDate: String, remarks: String) {
        val s = _uiState.value
        val name = s.traineeData?.name ?: "Trainee #${s.activeRoll}"
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAcademicRecord = true, academicActionMessage = null, academicActionError = null) }
            when (val res = apiClient.submitAcademicRecord(s.activeRoll, s.activePin, name, theoryTopic, theoryStatus, theoryUpdatedDate, labJobName, labStatus, labSignedDate, remarks)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmittingAcademicRecord = false, academicActionMessage = res.data.message) }
                    refreshTraineeDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSubmittingAcademicRecord = false, academicActionError = res.message) }
            }
        }
    }

    fun refreshTraineeAcademicSubmission() = refreshTraineeDashboard()

    fun dismissTraineeAcademicFeedback() {
        _uiState.update { it.copy(academicActionMessage = null, academicActionError = null) }
    }

    fun saveProjectGroup(title: String, members: List<Int>, leaderRoll: Int?, leaderName: String, onReset: () -> Unit) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProjectGroup = true, projectGroupSaveMessage = null, projectGroupSaveError = null) }
            when (val res = apiClient.saveProjectGroup(s.activeRoll, s.activePin, title, members, leaderRoll ?: 1, leaderName)) {
                is ApiResult.Success -> {
                    onReset()
                    _uiState.update { it.copy(isSavingProjectGroup = false, projectGroupSaveMessage = res.data.message) }
                    refreshTraineeDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSavingProjectGroup = false, projectGroupSaveError = res.message) }
            }
        }
    }

    fun refreshProjectGroupsForTrainee() = refreshTraineeDashboard()

    fun dismissProjectGroupFeedback() {
        _uiState.update { it.copy(projectGroupSaveMessage = null, projectGroupSaveError = null) }
    }

    fun saveTraineeLocalTimetable(context: Context, periods: List<TimetablePeriod>) {
        val roll = _uiState.value.activeRoll
        TimetableStore.saveTraineeCustomEdits(context, roll, periods)
        _uiState.update { it.copy(traineeCustomTimetable = periods, timetableFeedbackMessage = "Saved personal timetable on this device.") }
    }

    fun resetTraineeTimetableToOfficial(context: Context) {
        TimetableStore.clearTraineeCustomEdits(context, _uiState.value.activeRoll)
        _uiState.update { it.copy(traineeCustomTimetable = null, timetableFeedbackMessage = "Reset to official timetable.") }
    }

    fun submitTraineeSharedTimetable(context: Context, periods: List<TimetablePeriod>) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingTimetable = true, timetableFeedbackMessage = null, timetableError = null) }
            when (val res = apiClient.saveTimetable(s.activePin, s.activeRoll, periods, isTraineeEdit = true)) {
                is ApiResult.Success -> {
                    TimetableStore.saveOfficialCache(context, periods, true)
                    _uiState.update { it.copy(isSavingTimetable = false, timetableFeedbackMessage = res.data.message) }
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSavingTimetable = false, timetableError = res.message) }
            }
        }
    }

    fun dismissTimetableFeedback() {
        _uiState.update { it.copy(timetableFeedbackMessage = null, timetableError = null) }
    }

    fun refreshTechnicalNews() {
        if (_uiState.value.isLoadingTechNews) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTechNews = true) }
            val items = newsClient.fetchElectronicsNews()
            _uiState.update { it.copy(isLoadingTechNews = false, techNewsItems = items) }
        }
    }

    fun onSelectNewsLanguage(filter: NewsLanguageFilter) {
        _uiState.update { it.copy(newsLanguageFilter = filter) }
    }

    // ---------- Parent Actions ----------

    fun refreshParentDashboard() {
        val s = _uiState.value
        if (s.activeRoll.isBlank() || s.activePin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingParent = true, parentBannerError = null) }
            when (val res = apiClient.fetchParentData(s.activeRoll, s.activePin)) {
                is ApiResult.Success -> _uiState.update { it.copy(isRefreshingParent = false, parentData = res.data) }
                is ApiResult.Error -> _uiState.update { it.copy(isRefreshingParent = false, parentBannerError = res.message) }
            }
        }
    }

    fun evaluateParentNotification(context: Context) {
        val data = _uiState.value.parentData ?: return
        val attAlerted = AttendanceNotificationHelper.notifyParentIfConfirmedAndEligible(context, data)
        val mkAlerted = AttendanceNotificationHelper.notifyParentMarksUpdatedIfNeeded(context, data)
        _uiState.update {
            it.copy(
                parentNotificationDispatched = it.parentNotificationDispatched || attAlerted,
                parentMarksNotificationDispatched = it.parentMarksNotificationDispatched || mkAlerted
            )
        }
    }

    fun submitParentBunkAlert(optionalNote: String, onSuccessReset: () -> Unit) {
        val msg = optionalNote.ifBlank { "Parent reports ward left home on time for ITI." }
        submitParentAlertInternal(ParentAlertTypes.BUNK_ALERT, msg, onSuccessReset)
    }

    fun submitParentLeaveReason(reasonText: String, onSuccessReset: () -> Unit) {
        submitParentAlertInternal(ParentAlertTypes.LEAVE_REASON, reasonText, onSuccessReset)
    }

    private fun submitParentAlertInternal(type: String, message: String, onSuccessReset: () -> Unit) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingParentAlert = true, parentAlertSuccessMessage = null, parentAlertError = null) }
            when (val res = apiClient.submitParentAlert(s.activeRoll, s.activePin, type, message)) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update { it.copy(isSubmittingParentAlert = false, parentAlertSuccessMessage = res.data.message) }
                    refreshParentDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSubmittingParentAlert = false, parentAlertError = res.message) }
            }
        }
    }

    fun dismissParentAlertFeedback() {
        _uiState.update { it.copy(parentAlertSuccessMessage = null, parentAlertError = null) }
    }

    // ---------- Staff & Principal Actions ----------

    fun onStaffTabSelected(tab: StaffTab) {
        _uiState.update { it.copy(selectedStaffTab = tab) }
    }

    fun onPrincipalTabSelected(tab: PrincipalTab) {
        _uiState.update { it.copy(selectedPrincipalTab = tab) }
    }

    fun refreshStaffDashboard() {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingStaff = true, staffBannerError = null) }
            when (val res = apiClient.fetchStaffData(pin)) {
                is ApiResult.Success -> _uiState.update { it.copy(isRefreshingStaff = false, staffData = res.data) }
                is ApiResult.Error -> _uiState.update { it.copy(isRefreshingStaff = false, staffBannerError = res.message) }
            }
        }
    }

    fun refreshPrincipalDashboard() = refreshStaffDashboard()

    fun evaluateInstructorNotifications(context: Context) {
        val data = _uiState.value.staffData ?: return
        AttendanceNotificationHelper.evaluateStaffPublishedUpdates(context, data, _uiState.value.activeStaffRole, false)
    }

    fun onRegisterSearchChanged(q: String) {
        _uiState.update { it.copy(registerSearchQuery = q) }
    }

    fun onRegisterStatusFilterChanged(filter: String) {
        _uiState.update { it.copy(registerStatusFilter = filter) }
    }

    fun exportAttendancePdf(onPdfReady: (String) -> Unit) {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingPdf = true) }
            when (val res = apiClient.exportDailyAttendancePdf(pin)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isExportingPdf = false) }
                    if (res.data.pdfUrl.isNotBlank()) onPdfReady(res.data.pdfUrl)
                }
                is ApiResult.Error -> _uiState.update { it.copy(isExportingPdf = false, staffBannerError = res.message) }
            }
        }
    }

    fun confirmCloseAttendance() {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            _uiState.update { it.copy(isClosingAttendance = true, closeAttendanceMessage = null, closeAttendanceError = null) }
            when (val res = apiClient.confirmCloseAttendance(pin)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isClosingAttendance = false, closeAttendanceMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isClosingAttendance = false, closeAttendanceError = res.message) }
            }
        }
    }

    fun dismissCloseAttendanceFeedback() {
        _uiState.update { it.copy(closeAttendanceMessage = null, closeAttendanceError = null) }
    }

    fun bulkMorningRollCall() {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningRollCallOp = true, rollCallFeedbackMessage = null, rollCallError = null) }
            when (val res = apiClient.bulkMorningRollCall(pin)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isRunningRollCallOp = false, rollCallFeedbackMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isRunningRollCallOp = false, rollCallError = res.message) }
            }
        }
    }

    fun manualAttendancePunch(roll: String, status: String, overrideDuplicate: Boolean = false) {
        val s = _uiState.value
        val existing = s.staffData?.allAttendance?.firstOrNull { it.roll == roll }
        if (!overrideDuplicate && existing != null && existing.todayStatus.equals(status, true)) {
            _uiState.update {
                it.copy(duplicatePunchPrompt = DuplicatePunchPrompt(roll, existing.name, existing.todayStatus, status))
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(manualPunchingRoll = roll, duplicatePunchPrompt = null) }
            when (val res = apiClient.submitManualAttendancePunch(s.activePin, roll, status, overrideDuplicate)) {
                is ApiResult.Success -> {
                    val undoRec = existing?.let {
                        AttendanceUndoRecord(roll, it.name, it.todayStatus, it.inT, it.outT, status, s.staffData?.attendanceDate ?: "")
                    }
                    _uiState.update { st ->
                        st.copy(
                            manualPunchingRoll = null,
                            rollCallFeedbackMessage = res.data.message,
                            recentUndoByRoll = if (undoRec != null) st.recentUndoByRoll + (roll to undoRec) else st.recentUndoByRoll
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(manualPunchingRoll = null, rollCallError = res.message) }
            }
        }
    }

    fun confirmDuplicatePunch() {
        val dup = _uiState.value.duplicatePunchPrompt ?: return
        manualAttendancePunch(dup.roll, dup.requestedStatus, overrideDuplicate = true)
    }

    fun dismissDuplicatePunch() {
        _uiState.update { it.copy(duplicatePunchPrompt = null) }
    }

    fun undoAttendanceAction(roll: String) {
        val s = _uiState.value
        val rec = s.recentUndoByRoll[roll] ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(undoingRoll = roll) }
            when (val res = apiClient.undoAttendanceAction(s.activePin, roll, rec.date, rec.previousStatus, rec.previousInT, rec.previousOutT)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(undoingRoll = null, recentUndoByRoll = it.recentUndoByRoll - roll, rollCallFeedbackMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(undoingRoll = null, rollCallError = res.message) }
            }
        }
    }

    fun resetAttendanceToAbsent(roll: String, studentName: String) {
        val s = _uiState.value
        viewModelScope.launch {
            when (val res = apiClient.undoAttendanceAction(s.activePin, roll, s.staffData?.attendanceDate ?: AttendanceApiClient.todayIstDateString(), "ABSENT", "-", "-")) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(rollCallFeedbackMessage = "Reset $studentName to ABSENT.") }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(rollCallError = res.message) }
            }
        }
    }

    fun setLateGraceForTrainee(roll: String, mode: String, cutoff: String, note: String) {
        viewModelScope.launch {
            when (val res = apiClient.setLateGrace(_uiState.value.activePin, roll, mode, cutoff, note)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(rollCallFeedbackMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(rollCallError = res.message) }
            }
        }
    }

    fun sendInstantParentAlert(roll: String, name: String, alertType: String, msg: String) {
        viewModelScope.launch {
            when (val res = apiClient.sendInstructorParentAlert(_uiState.value.activePin, roll, alertType, msg)) {
                is ApiResult.Success -> _uiState.update { it.copy(rollCallFeedbackMessage = res.data.message) }
                is ApiResult.Error -> _uiState.update { it.copy(rollCallError = res.message) }
            }
        }
    }

    fun approveAndSignLeave(reqId: String, signature: String) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(signingReqId = reqId, approvalError = null, approvalSuccessMessage = null) }
            val res = if (s.activeStaffRole == UserRole.PRINCIPAL) {
                apiClient.signByPrincipal(s.activePin, reqId, signature)
            } else {
                apiClient.signByInstructor(s.activePin, reqId, signature)
            }
            when (res) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(signingReqId = null, approvalSuccessMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(signingReqId = null, approvalError = res.message) }
            }
        }
    }

    fun rejectLeaveRequest(reqId: String, reason: String) {
        viewModelScope.launch {
            when (val res = apiClient.rejectLeaveRequest(_uiState.value.activePin, reqId, reason)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(approvalSuccessMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(approvalError = res.message) }
            }
        }
    }

    fun dismissApprovalFeedback() {
        _uiState.update { it.copy(approvalSuccessMessage = null, approvalError = null) }
    }

    fun refreshAcademicSubmissions() = refreshStaffDashboard()

    fun verifyAcademicSubmission(roll: String, submissionId: String, remark: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingAcademicSubmission = true) }
            when (val res = apiClient.verifyAcademicSubmission(_uiState.value.activePin, roll, submissionId, remark)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isUpdatingAcademicSubmission = false, approvalSuccessMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isUpdatingAcademicSubmission = false, approvalError = res.message) }
            }
        }
    }

    fun forwardSingleAcademicToPrincipal(roll: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingAcademicSubmission = true) }
            when (val res = apiClient.forwardAcademicSubmissionToPrincipal(_uiState.value.activePin, roll, false)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isUpdatingAcademicSubmission = false, approvalSuccessMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isUpdatingAcademicSubmission = false, approvalError = res.message) }
            }
        }
    }

    fun forwardAllVerifiedAcademicToPrincipal() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingAcademicSubmission = true) }
            when (val res = apiClient.forwardAcademicSubmissionToPrincipal(_uiState.value.activePin, "", true)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isUpdatingAcademicSubmission = false, approvalSuccessMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isUpdatingAcademicSubmission = false, approvalError = res.message) }
            }
        }
    }

    // ---------- Whole-Class Marks Register Table + Digital Signature ----------

    fun saveMarksDraftOffline(context: Context, monthLabel: String, rows: List<ExamMarkRow>) {
        val entered = rows.count { it.theoryMarks.isNotBlank() || it.labMarks.isNotBlank() }
        OfflineSyncQueue.enqueue(context, "MARKS_DRAFT", "$monthLabel ($entered trainee marks drafted)")
        _uiState.update { it.copy(monthlyMarkMessage = "Saved $entered mark rows as offline draft on device.") }
    }

    fun publishSignedMarksTable(
        monthLabel: String,
        examDate: String,
        theoryMax: Int,
        labMax: Int,
        rows: List<ExamMarkRow>,
        signedBy: String,
        signatureData: String
    ) {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingMonthlyMark = true, monthlyMarkMessage = null, monthlyMarkError = null) }
            var savedCount = 0
            var firstError: String? = null
            for (row in rows) {
                val res = apiClient.saveMonthlyMark(
                    pin = pin,
                    roll = row.roll.toString(),
                    monthLabel = monthLabel,
                    examDate = examDate,
                    theoryMarks = row.theoryClamped(theoryMax),
                    theoryMax = theoryMax,
                    labMarks = row.labClamped(labMax),
                    labMax = labMax,
                    remarks = row.remarks,
                    publish = true,
                    notifyParent = true,
                    signedBy = signedBy,
                    signatureData = signatureData
                )
                if (res is ApiResult.Success) {
                    savedCount++
                } else if (res is ApiResult.Error && firstError == null) {
                    firstError = "Roll #${row.roll}: ${res.message}"
                }
            }
            if (savedCount > 0) {
                _uiState.update {
                    it.copy(
                        isSavingMonthlyMark = false,
                        monthlyMarkMessage = "Digitally signed & published marks for $savedCount trainee(s) ($monthLabel)." +
                            if (firstError != null) " Partial error: $firstError" else ""
                    )
                }
                refreshStaffDashboard()
            } else {
                _uiState.update {
                    it.copy(isSavingMonthlyMark = false, monthlyMarkError = firstError ?: "Failed to publish marks.")
                }
            }
        }
    }

    fun dismissMonthlyMarkFeedback() {
        _uiState.update { it.copy(monthlyMarkMessage = null, monthlyMarkError = null) }
    }

    fun replyToWorkMarkQuery(queryId: String, status: String, reply: String, onSuccessReset: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(replyingWorkQueryId = queryId, workQueryReplyMessage = null, workQueryReplyError = null) }
            when (val res = apiClient.replyWorkMarkQuery(_uiState.value.activePin, queryId, status, reply)) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update { it.copy(replyingWorkQueryId = null, workQueryReplyMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(replyingWorkQueryId = null, workQueryReplyError = res.message) }
            }
        }
    }

    fun dismissWorkQueryReplyFeedback() {
        _uiState.update { it.copy(workQueryReplyMessage = null, workQueryReplyError = null) }
    }

    // ---------- Shop Floor, Duty, Timetable, Notices, Holidays, Projects, ESP ----------

    fun toggleEarlyClassLogout(enabled: Boolean) {
        val cur = _uiState.value.staffData?.scanOverrides?.shopFloorReleaseNowEnabled ?: false
        viewModelScope.launch {
            when (val res = apiClient.setScanOverride(_uiState.value.activePin, enabled, cur)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(shopFloorActionMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(shopFloorActionError = res.message) }
            }
        }
    }

    fun toggleShopFloorReleaseNow(enabled: Boolean) {
        val cur = _uiState.value.staffData?.scanOverrides?.earlyClassLogoutEnabled ?: false
        viewModelScope.launch {
            when (val res = apiClient.setScanOverride(_uiState.value.activePin, cur, enabled)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(shopFloorActionMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(shopFloorActionError = res.message) }
            }
        }
    }

    fun updateShopFloorMember(roll: String, operation: String, sessionType: String, endTime: String) {
        viewModelScope.launch {
            when (val res = apiClient.setShopFloorMember(_uiState.value.activePin, roll, operation, sessionType, endTime)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(shopFloorActionMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(shopFloorActionError = res.message) }
            }
        }
    }

    fun recordShopFloorCardLogout(roll: String) {
        viewModelScope.launch {
            when (val res = apiClient.triggerShopFloorCardLogout(roll)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(shopFloorActionMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(shopFloorActionError = res.message) }
            }
        }
    }

    fun verifyShopFloorLogout(roll: String, date: String) {
        viewModelScope.launch {
            when (val res = apiClient.verifyShopFloorLogout(_uiState.value.activePin, roll, date)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(shopFloorActionMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(shopFloorActionError = res.message) }
            }
        }
    }

    fun signOffDuty(date: String, pairLabel: String, note: String, onReset: () -> Unit) {
        viewModelScope.launch {
            when (val res = apiClient.signOffDailyDuty(_uiState.value.activePin, date, pairLabel, note)) {
                is ApiResult.Success -> {
                    onReset()
                    _uiState.update { it.copy(dutyOverrideMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(dutyOverrideError = res.message) }
            }
        }
    }

    fun logToolCribItem(itemName: String, conditionStatus: String, notes: String, onReset: () -> Unit) {
        viewModelScope.launch {
            when (val res = apiClient.logToolCribIssue(_uiState.value.activePin, itemName, conditionStatus, notes)) {
                is ApiResult.Success -> {
                    onReset()
                    _uiState.update { it.copy(dutyOverrideMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(dutyOverrideError = res.message) }
            }
        }
    }

    fun saveDutyOverride(dateKey: String, firstRoll: Int, secondRoll: Int, dutyNote: String, reanchorFromHere: Boolean, resetToAuto: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingDutyOverride = true, dutyOverrideMessage = null, dutyOverrideError = null) }
            when (val res = apiClient.saveDutyOverride(_uiState.value.activePin, dateKey, firstRoll, secondRoll, dutyNote, reanchorFromHere, resetToAuto)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSavingDutyOverride = false, dutyOverrideMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSavingDutyOverride = false, dutyOverrideError = res.message) }
            }
        }
    }

    fun dismissDutyOverrideFeedback() {
        _uiState.update { it.copy(dutyOverrideMessage = null, dutyOverrideError = null) }
    }

    fun publishTimetable(context: Context, periods: List<TimetablePeriod>, allowTraineeEdit: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingTimetable = true, timetableFeedbackMessage = null, timetableError = null) }
            when (val res = apiClient.saveTimetable(_uiState.value.activePin, "", periods, allowTraineeEdit, false)) {
                is ApiResult.Success -> {
                    TimetableStore.saveOfficialCache(context, periods, allowTraineeEdit)
                    _uiState.update { it.copy(isSavingTimetable = false, officialTimetableCache = periods, timetableFeedbackMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSavingTimetable = false, timetableError = res.message) }
            }
        }
    }

    fun postCampusNotice(title: String, category: String, body: String, pinned: Boolean = false, onSuccessReset: () -> Unit) {
        val postedBy = if (_uiState.value.activeStaffRole == UserRole.PRINCIPAL) "Principal" else "Trade Instructor"
        viewModelScope.launch {
            _uiState.update { it.copy(isPostingNotice = true, noticePostMessage = null, noticePostError = null) }
            when (val res = apiClient.postNotice(_uiState.value.activePin, title, category, body, AttendanceApiClient.todayIstDateString(), pinned, postedBy)) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update { it.copy(isPostingNotice = false, noticePostMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isPostingNotice = false, noticePostError = res.message) }
            }
        }
    }

    fun postCampusHoliday(date: String, reason: String, onReset: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPostingHoliday = true, holidayPostMessage = null, holidayPostError = null) }
            when (val res = apiClient.postHoliday(_uiState.value.activePin, date, reason)) {
                is ApiResult.Success -> {
                    onReset()
                    _uiState.update { it.copy(isPostingHoliday = false, holidayPostMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isPostingHoliday = false, holidayPostError = res.message) }
            }
        }
    }

    fun toggleProjectGroupsAccess(enabled: Boolean) {
        viewModelScope.launch {
            when (val res = apiClient.setProjectGroupsAccess(_uiState.value.activePin, enabled)) {
                is ApiResult.Success -> refreshStaffDashboard()
                is ApiResult.Error -> _uiState.update { it.copy(staffBannerError = res.message) }
            }
        }
    }

    fun refreshProjectGroupsForStaff() = refreshStaffDashboard()

    fun onEspRollChanged(roll: String) {
        _uiState.update { it.copy(espRollInput = roll.filter(Char::isDigit).take(2), espTapError = null) }
    }

    fun triggerManualEspTap() {
        val roll = _uiState.value.espRollInput.trim()
        val rollNum = roll.toIntOrNull()
        if (rollNum == null || rollNum !in 1..23) {
            _uiState.update { it.copy(espTapError = "Enter a valid Roll Number (1 to 23).") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isTappingEsp = true, espTapError = null) }
            when (val res = apiClient.triggerEspTap(roll)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isTappingEsp = false, lastEspTap = res.data, espTapHistory = listOf(res.data) + it.espTapHistory) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isTappingEsp = false, espTapError = res.message) }
            }
        }
    }
}
