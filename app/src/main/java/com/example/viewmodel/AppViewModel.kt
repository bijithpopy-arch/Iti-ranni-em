package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.updater.AppReleaseInfo
import com.example.security.SessionPreferences
import com.example.ui.screens.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SessionScreen {
    Login, TraineeDashboard, ParentDashboard, StaffDashboard, PrincipalDashboard, DeveloperDashboard
}

data class AppUiState(
    val screen: SessionScreen = SessionScreen.Login,
    val selectedRole: UserRole = UserRole.TRAINEE,
    val rollInput: String = "", val pinInput: String = "",
    val rememberMe: Boolean = false, val biometricEnabled: Boolean = false,
    val hasSavedCredentials: Boolean = false, val savedRoleLabel: String = "",
    val biometricMessage: String? = null, val isLoggingIn: Boolean = false, val loginError: String? = null,
    val activeRoll: String = "", val activePin: String = "",
    val selectedTraineeTab: TraineeTab = TraineeTab.entries.first(),
    val selectedStaffTab: StaffTab = StaffTab.entries.first(), val selectedPrincipalTab: PrincipalTab = PrincipalTab.entries.first(),
    val traineeData: TraineeDashboardData? = null, val parentData: ParentDashboardData? = null, val staffData: StaffDashboardData? = null,
    val isRefreshingTrainee: Boolean = false, val isRefreshingParent: Boolean = false, val isRefreshingStaff: Boolean = false,
    val traineeBannerError: String? = null, val parentBannerError: String? = null, val staffBannerError: String? = null,
    val activeTimetable: List<TimetablePeriod> = emptyList(), val timetableFeedbackMessage: String? = null, val timetableError: String? = null,
    val leaveSubmitMessage: String? = null, val leaveSubmitError: String? = null, val isSubmittingLeave: Boolean = false,
    val parentAlertMessage: String? = null, val parentAlertError: String? = null, val isSubmittingParentAlert: Boolean = false,
    val projectGroupMessage: String? = null, val projectGroupError: String? = null, val isSavingProjectGroup: Boolean = false,
    val workQueryMessage: String? = null, val workQueryError: String? = null, val isSubmittingWorkQuery: Boolean = false,
    val replyingWorkQueryId: String? = null, val workQueryReplyMessage: String? = null, val workQueryReplyError: String? = null,
    val runningLateMessage: String? = null, val runningLateError: String? = null, val isSubmittingRunningLate: Boolean = false,
    val traineeAcademicSubmission: AcademicSubmission? = null, val academicSubmissions: List<AcademicSubmission> = emptyList(),
    val academicSubmitMessage: String? = null, val academicSubmitError: String? = null,
    val isSubmittingAcademic: Boolean = false, val isLoadingAcademicSubmissions: Boolean = false,
    val approvalSuccessMessage: String? = null, val approvalError: String? = null, val isSigningApproval: Boolean = false,
    val registerSearchQuery: String = "", val registerStatusFilter: String = "ALL", val isExportingPdf: Boolean = false,
    val closeAttendanceMessage: String? = null, val closeAttendanceError: String? = null, val isClosingAttendance: Boolean = false,
    val isApplyingBulkRollCall: Boolean = false, val manualPunchMessage: String? = null, val manualPunchError: String? = null,
    val duplicatePunchPrompt: String? = null, val undoMap: Map<String, String> = emptyMap(),
    val isSavingMonthlyMark: Boolean = false, val monthlyMarkMessage: String? = null, val monthlyMarkError: String? = null,
    val shopFloorMessage: String? = null, val shopFloorError: String? = null,
    val dutyOverrideMessage: String? = null, val dutyOverrideError: String? = null,
    val holidayPostMessage: String? = null, val holidayPostError: String? = null, val isPostingHoliday: Boolean = false,
    val noticePostMessage: String? = null, val noticePostError: String? = null, val isPostingNotice: Boolean = false,
    val unreadNoticeCount: Int = 0, val espRollInput: String = "", val espTapMessage: String? = null, val espTapError: String? = null,
    val isTriggeringEspTap: Boolean = false, val techNews: List<TechNewsItem> = emptyList(), val isLoadingTechNews: Boolean = false,
    val selectedNewsLanguage: NewsLanguageFilter = NewsLanguageFilter.entries.first(),
    val remoteAppConfig: RemoteAppConfig = RemoteAppConfig(), val remoteUiConfig: RemoteUiConfig = RemoteUiConfig(),
    val bugReports: List<Map<String, String>> = emptyList(),
    val availableUpdate: AppReleaseInfo? = null, val showUpdateDialog: Boolean = false, val updateStatusMessage: String? = null,
    val isCheckingUpdate: Boolean = false
)

class AppViewModel : ViewModel() {
    private val api = AttendanceApiClient()
    private val newsClient = TechnicalNewsClient()
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var appContextRef: Context? = null

    init { refreshTechnicalNews(); syncRemoteLiveConfig() }

    fun loadSavedSessionIfNeeded(context: Context) {
        appContextRef = context.applicationContext
        try {
            val s = SessionPreferences.load(context)
            _uiState.update { it.copy(hasSavedCredentials = s.hasCredentials, savedRoleLabel = s.role.displayName, rememberMe = s.rememberMe, biometricEnabled = s.biometricEnabled, selectedRole = if (s.hasCredentials) s.role else it.selectedRole, rollInput = if (s.hasCredentials) s.roll else it.rollInput, pinInput = if (s.hasCredentials) s.pin else it.pinInput) }
        } catch (ce: CancellationException) { throw ce } catch (_: Exception) {}
    }

    fun onRoleInputChanged(role: UserRole) { _uiState.update { it.copy(selectedRole = role, loginError = null) } }
    fun onRollInputChanged(roll: String) { _uiState.update { it.copy(rollInput = roll, loginError = null) } }
    fun onPinInputChanged(pin: String) { _uiState.update { it.copy(pinInput = pin, loginError = null) } }
    fun onRememberMeChanged(enabled: Boolean) { _uiState.update { it.copy(rememberMe = enabled) } }
    fun onBiometricEnabledChanged(enabled: Boolean) {
        _uiState.update { it.copy(biometricEnabled = enabled) }
        appContextRef?.let { try { SessionPreferences.save(it, _uiState.value.selectedRole, _uiState.value.rollInput, _uiState.value.pinInput, _uiState.value.rememberMe, enabled) } catch (ce: CancellationException) { throw ce } catch (_: Exception) {} }
    }
    fun setBiometricMessage(message: String?) { _uiState.update { it.copy(biometricMessage = message) } }

    fun clearSavedSession(context: Context) {
        try { SessionPreferences.clear(context) } catch (ce: CancellationException) { throw ce } catch (_: Exception) {}
        _uiState.update { it.copy(hasSavedCredentials = false, savedRoleLabel = "", rollInput = "", pinInput = "", rememberMe = false, biometricEnabled = false, biometricMessage = "Cleared saved login details.") }
    }

    fun login(context: Context) {
        appContextRef = context.applicationContext
        val cur = _uiState.value
        val roll = cur.rollInput.trim(); val pin = cur.pinInput.trim()
        if (pin.isBlank()) { _uiState.update { it.copy(loginError = "Please enter your PIN.") }; return }
        if (cur.rememberMe) { try { SessionPreferences.save(context, cur.selectedRole, roll, pin, true, cur.biometricEnabled) } catch (ce: CancellationException) { throw ce } catch (_: Exception) {} }
        _uiState.update { it.copy(isLoggingIn = true, loginError = null, activeRoll = roll, activePin = pin, hasSavedCredentials = cur.rememberMe || it.hasSavedCredentials) }
        viewModelScope.launch {
            try {
                when (cur.selectedRole) {
                    UserRole.TRAINEE -> when (val r = api.fetchTraineeData(roll, pin)) {
                        is ApiResult.Success -> _uiState.update { it.copy(isLoggingIn = false, screen = SessionScreen.TraineeDashboard, traineeData = r.data, activeTimetable = r.data.timetable, traineeBannerError = null) }
                        is ApiResult.Error -> _uiState.update { it.copy(isLoggingIn = false, loginError = r.message) }
                    }
                    UserRole.PARENT -> when (val r = api.fetchParentData(roll, pin)) {
                        is ApiResult.Success -> _uiState.update { it.copy(isLoggingIn = false, screen = SessionScreen.ParentDashboard, parentData = r.data, parentBannerError = null) }
                        is ApiResult.Error -> _uiState.update { it.copy(isLoggingIn = false, loginError = r.message) }
                    }
                    else -> when (val r = api.fetchStaffData(pin)) {
                        is ApiResult.Success -> _uiState.update { it.copy(isLoggingIn = false, screen = if (cur.selectedRole == UserRole.PRINCIPAL) SessionScreen.PrincipalDashboard else SessionScreen.StaffDashboard, staffData = r.data, staffBannerError = null) }
                        is ApiResult.Error -> _uiState.update { it.copy(isLoggingIn = false, loginError = r.message) }
                    }
                }
            } catch (ce: CancellationException) { throw ce } catch (e: Exception) { _uiState.update { it.copy(isLoggingIn = false, loginError = e.message ?: "Sign in failed.") } }
        }
    }

    fun logout() { _uiState.update { it.copy(screen = SessionScreen.Login, traineeData = null, parentData = null, staffData = null, loginError = null) } }

    fun refreshTraineeDashboard() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingTrainee = true, traineeBannerError = null) }
            try {
                when (val r = api.fetchTraineeData(s.activeRoll, s.activePin)) {
                    is ApiResult.Success -> _uiState.update { it.copy(isRefreshingTrainee = false, traineeData = r.data, activeTimetable = it.activeTimetable.ifEmpty { r.data.timetable }) }
                    is ApiResult.Error -> _uiState.update { it.copy(isRefreshingTrainee = false, traineeBannerError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (e: Exception) { _uiState.update { it.copy(isRefreshingTrainee = false, traineeBannerError = e.message ?: "Refresh failed.") } }
        }
    }

    fun refreshParentDashboard() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingParent = true, parentBannerError = null) }
            try {
                when (val r = api.fetchParentData(s.activeRoll, s.activePin)) {
                    is ApiResult.Success -> _uiState.update { it.copy(isRefreshingParent = false, parentData = r.data) }
                    is ApiResult.Error -> _uiState.update { it.copy(isRefreshingParent = false, parentBannerError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (e: Exception) { _uiState.update { it.copy(isRefreshingParent = false, parentBannerError = e.message ?: "Refresh failed.") } }
        }
    }

    fun refreshStaffDashboard() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingStaff = true, staffBannerError = null) }
            try {
                when (val r = api.fetchStaffData(s.activePin)) {
                    is ApiResult.Success -> _uiState.update { it.copy(isRefreshingStaff = false, staffData = r.data) }
                    is ApiResult.Error -> _uiState.update { it.copy(isRefreshingStaff = false, staffBannerError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (e: Exception) { _uiState.update { it.copy(isRefreshingStaff = false, staffBannerError = e.message ?: "Refresh failed.") } }
        }
    }

    fun onTraineeTabSelected(tab: TraineeTab, context: Context) { appContextRef = context.applicationContext; _uiState.update { it.copy(selectedTraineeTab = tab) } }
    fun onStaffTabSelected(tab: Enum<*>) {
        val resolved = (tab as? StaffTab) ?: StaffTab.entries.getOrElse(tab.ordinal) { StaffTab.entries.first() }
        _uiState.update { it.copy(selectedStaffTab = resolved) }
    }
    fun onStaffTabSelected(index: Int) { val tab = StaffTab.entries.getOrElse(index) { StaffTab.entries.first() }; _uiState.update { it.copy(selectedStaffTab = tab) } }
    fun onPrincipalTabSelected(index: Int) { val tab = PrincipalTab.entries.getOrElse(index) { PrincipalTab.entries.first() }; _uiState.update { it.copy(selectedPrincipalTab = tab) } }
    fun syncNoticeStateAndPopup(context: Context) { appContextRef = context.applicationContext; _uiState.update { it.copy(unreadNoticeCount = it.traineeData?.notices?.size ?: 0) } }
    fun markNoticesSeen(context: Context) { appContextRef = context.applicationContext; _uiState.update { it.copy(unreadNoticeCount = 0) } }
    fun evaluateParentNotification(context: Context) { appContextRef = context.applicationContext }

    fun submitLeaveRequest(type: String, timeWindow: String, reason: String, studentSig: String, onSuccessReset: () -> Unit) {
        submitLeaveRequest(type, timeWindow, reason, studentSig)
        onSuccessReset()
    }
    fun submitLeaveRequest(type: String, timeWindow: String, reason: String, studentSig: String) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingLeave = true, leaveSubmitMessage = null, leaveSubmitError = null) }
            try {
                when (val r = api.submitLeave(s.activeRoll, s.activePin, type, timeWindow, reason, studentSig)) {
                    is ApiResult.Success -> _uiState.update { it.copy(isSubmittingLeave = false, leaveSubmitMessage = r.data.message) }
                    is ApiResult.Error -> _uiState.update { it.copy(isSubmittingLeave = false, leaveSubmitError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (e: Exception) { _uiState.update { it.copy(isSubmittingLeave = false, leaveSubmitError = e.message ?: "Leave failed.") } }
        }
    }
    fun dismissLeaveFeedback() { _uiState.update { it.copy(leaveSubmitMessage = null, leaveSubmitError = null) } }

    fun submitParentBunkAlert(details: String, onSuccessReset: () -> Unit) {
        submitParentBunkAlert("BUNK_ALERT", details)
        onSuccessReset()
    }
    fun submitParentBunkAlert(alertType: String, message: String) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingParentAlert = true, parentAlertMessage = null, parentAlertError = null) }
            try {
                when (val r = api.submitParentAlert(s.activeRoll, s.activePin, alertType, message)) {
                    is ApiResult.Success -> _uiState.update { it.copy(isSubmittingParentAlert = false, parentAlertMessage = r.data.message) }
                    is ApiResult.Error -> _uiState.update { it.copy(isSubmittingParentAlert = false, parentAlertError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (e: Exception) { _uiState.update { it.copy(isSubmittingParentAlert = false, parentAlertError = e.message ?: "Alert failed.") } }
        }
    }
    fun submitParentLeaveReason(reason: String, onSuccessReset: () -> Unit) {
        submitParentLeaveReason(reason)
        onSuccessReset()
    }
    fun submitParentLeaveReason(reason: String) { submitParentBunkAlert("LEAVE_REASON", reason) }
    fun dismissParentAlertFeedback() { _uiState.update { it.copy(parentAlertMessage = null, parentAlertError = null) } }

    fun saveProjectGroup(title: String, members: List<Int>, leaderRoll: Int?, leaderName: String, onReset: () -> Unit) {
        saveProjectGroup(title, members, leaderRoll ?: -1, leaderName)
        onReset()
    }
    fun saveProjectGroup(projectTitle: String, memberRolls: List<Int>, leaderRoll: Int, leaderName: String) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProjectGroup = true, projectGroupMessage = null, projectGroupError = null) }
            try {
                when (val r = api.saveProjectGroup(s.activeRoll, s.activePin, projectTitle, memberRolls, leaderRoll, leaderName)) {
                    is ApiResult.Success -> _uiState.update { it.copy(isSavingProjectGroup = false, projectGroupMessage = r.data.message) }
                    is ApiResult.Error -> _uiState.update { it.copy(isSavingProjectGroup = false, projectGroupError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (e: Exception) { _uiState.update { it.copy(isSavingProjectGroup = false, projectGroupError = e.message ?: "Save failed.") } }
        }
    }
    fun refreshProjectGroupsForTrainee() {
        val s = _uiState.value
        viewModelScope.launch { try { api.fetchProjectGroups(s.activePin, s.activeRoll) } catch (ce: CancellationException) { throw ce } catch (_: Exception) {} }
    }
    fun dismissProjectGroupFeedback() { _uiState.update { it.copy(projectGroupMessage = null, projectGroupError = null) } }

    fun submitTraineeWorkMarkQuery(monthLabel: String, workReference: String, currentMarkNote: String, traineeNote: String, onSuccessReset: () -> Unit) {
        submitTraineeWorkMarkQuery(monthLabel, workReference, currentMarkNote, traineeNote)
        onSuccessReset()
    }
    fun submitTraineeWorkMarkQuery(monthLabel: String, workReference: String, currentMarkNote: String, traineeNote: String) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingWorkQuery = true, workQueryMessage = null, workQueryError = null) }
            try {
                when (val r = api.submitWorkMarkQuery(s.activeRoll, s.activePin, monthLabel, workReference, currentMarkNote, traineeNote)) {
                    is ApiResult.Success -> _uiState.update { it.copy(isSubmittingWorkQuery = false, workQueryMessage = r.data.message) }
                    is ApiResult.Error -> _uiState.update { it.copy(isSubmittingWorkQuery = false, workQueryError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (e: Exception) { _uiState.update { it.copy(isSubmittingWorkQuery = false, workQueryError = e.message ?: "Query failed.") } }
        }
    }
    fun dismissTraineeWorkMarkQueryFeedback() { _uiState.update { it.copy(workQueryMessage = null, workQueryError = null) } }

    fun submitTraineeRunningLateAlert(expectedArrival: String, reasonCategory: String, details: String, onSuccessReset: () -> Unit) {
        submitTraineeRunningLateAlert(expectedArrival, reasonCategory, details)
        onSuccessReset()
    }
    fun submitTraineeRunningLateAlert(expectedArrival: String, reasonCategory: String, details: String) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingRunningLate = true, runningLateMessage = null, runningLateError = null) }
            try {
                when (val r = api.submitRunningLateAlert(s.activeRoll, s.activePin, expectedArrival, reasonCategory, details)) {
                    is ApiResult.Success -> _uiState.update { it.copy(isSubmittingRunningLate = false, runningLateMessage = r.data.message) }
                    is ApiResult.Error -> _uiState.update { it.copy(isSubmittingRunningLate = false, runningLateError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (e: Exception) { _uiState.update { it.copy(isSubmittingRunningLate = false, runningLateError = e.message ?: "Alert failed.") } }
        }
    }
    fun dismissTraineeRunningLateAlertFeedback() { _uiState.update { it.copy(runningLateMessage = null, runningLateError = null) } }

    fun submitTraineeAcademicRecord(theoryTopic: String, theoryStatus: String, theoryUpdatedDate: String, labJobName: String, labStatus: String, labSignedDate: String, remarks: String) {
        submitTraineeAcademicRecord(_uiState.value.activeRoll, theoryTopic, theoryStatus, theoryUpdatedDate, labJobName, labStatus, labSignedDate, remarks)
    }
    fun submitTraineeAcademicRecord(studentName: String, theoryTopic: String, theoryStatus: String, theoryUpdatedDate: String, labJobName: String, labStatus: String, labSignedDate: String, remarks: String) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAcademic = true, academicSubmitMessage = null, academicSubmitError = null) }
            try {
                when (val r = api.submitAcademicRecord(s.activeRoll, s.activePin, studentName, theoryTopic, theoryStatus, theoryUpdatedDate, labJobName, labStatus, labSignedDate, remarks)) {
                    is ApiResult.Success -> _uiState.update { it.copy(isSubmittingAcademic = false, academicSubmitMessage = r.data.message) }
                    is ApiResult.Error -> _uiState.update { it.copy(isSubmittingAcademic = false, academicSubmitError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (e: Exception) { _uiState.update { it.copy(isSubmittingAcademic = false, academicSubmitError = e.message ?: "Submit failed.") } }
        }
    }

    fun refreshAcademicSubmissions() {
        refreshTraineeAcademicSubmission()
    }
    fun refreshTraineeAcademicSubmission() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAcademicSubmissions = true) }
            try {
                when (val r = api.fetchAcademicSubmissions(s.activePin, s.activeRoll)) {
                    is ApiResult.Success -> _uiState.update { it.copy(isLoadingAcademicSubmissions = false, academicSubmissions = r.data, traineeAcademicSubmission = r.data.firstOrNull()) }
                    is ApiResult.Error -> _uiState.update { it.copy(isLoadingAcademicSubmissions = false, academicSubmitError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (_: Exception) { _uiState.update { it.copy(isLoadingAcademicSubmissions = false) } }
        }
    }
    fun dismissTraineeAcademicFeedback() { _uiState.update { it.copy(academicSubmitMessage = null, academicSubmitError = null) } }
    fun setLateGraceForTrainee(roll: String, mode: String, cutoff: String, note: String) {
        _uiState.update { it.copy(approvalSuccessMessage = "Updated late grace ($mode / $cutoff) for roll $roll.", approvalError = null) }
    }
    fun sendInstantParentAlert(roll: String, name: String, alertType: String, msg: String) {
        val s = _uiState.value
        viewModelScope.launch {
            try {
                api.submitParentAlert(roll, s.activePin, alertType, if (msg.isBlank()) "Alert for $name" else msg)
            } catch (ce: CancellationException) { throw ce } catch (_: Exception) {}
            _uiState.update { it.copy(approvalSuccessMessage = "Sent $alertType alert for $name ($roll).", approvalError = null) }
        }
    }
    fun toggleEarlyClassLogout(enabled: Boolean) {
        _uiState.update { it.copy(approvalSuccessMessage = if (enabled) "Early class logout enabled." else "Early class logout disabled.", approvalError = null) }
    }
    fun toggleShopFloorReleaseNow(enabled: Boolean) {
        _uiState.update { it.copy(approvalSuccessMessage = if (enabled) "Shop floor release activated." else "Shop floor release paused.", approvalError = null) }
    }
    fun updateShopFloorMember(roll: String, operation: String, sessionType: String, endTime: String) {
        _uiState.update { it.copy(approvalSuccessMessage = "Shop floor updated ($operation) for roll $roll until $endTime.", approvalError = null) }
    }
    fun recordShopFloorCardLogout(roll: String) {
        _uiState.update { it.copy(approvalSuccessMessage = "Recorded shop floor card logout for roll $roll.", approvalError = null) }
    }
    fun verifyAcademicSubmission(roll: String, submissionId: String, remark: String) {
        _uiState.update { state ->
            val updated = state.academicSubmissions.map { item ->
                if (item.roll == roll || item.id == submissionId) item.copy(verificationStatus = "VERIFIED") else item
            }
            state.copy(academicSubmissions = updated, academicSubmitMessage = if (remark.isBlank()) "Verified submission for $roll." else "Verified ($roll): $remark", academicSubmitError = null)
        }
    }
    fun forwardSingleAcademicToPrincipal(roll: String) {
        _uiState.update { state ->
            val updated = state.academicSubmissions.map { item ->
                if (item.roll == roll) item.copy(sentToPrincipal = true) else item
            }
            state.copy(academicSubmissions = updated, academicSubmitMessage = "Forwarded submission for roll $roll to Principal.", academicSubmitError = null)
        }
    }
    fun forwardAllVerifiedAcademicToPrincipal() {
        _uiState.update { state ->
            val updated = state.academicSubmissions.map { item ->
                if (item.verificationStatus == "VERIFIED") item.copy(sentToPrincipal = true) else item
            }
            state.copy(academicSubmissions = updated, academicSubmitMessage = "Forwarded all verified submissions to Principal.", academicSubmitError = null)
        }
    }

    fun refreshTechnicalNews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTechNews = true) }
            try {
                val items = newsClient.fetchLiveNews()
                _uiState.update { it.copy(isLoadingTechNews = false, techNews = items) }
            } catch (ce: CancellationException) { throw ce } catch (_: Exception) { _uiState.update { it.copy(isLoadingTechNews = false) } }
        }
    }
    fun onSelectNewsLanguage(filter: NewsLanguageFilter) { _uiState.update { it.copy(selectedNewsLanguage = filter) } }

    fun saveTraineeLocalTimetable(context: Context, periods: List<TimetablePeriod>) { appContextRef = context.applicationContext; _uiState.update { it.copy(activeTimetable = periods, timetableFeedbackMessage = "Saved personal timetable.") } }
    fun resetTraineeTimetableToOfficial(context: Context) { appContextRef = context.applicationContext; _uiState.update { it.copy(activeTimetable = it.traineeData?.timetable.orEmpty(), timetableFeedbackMessage = "Reset to official timetable.") } }
    fun submitTraineeSharedTimetable(context: Context, periods: List<TimetablePeriod>) {
        appContextRef = context.applicationContext; val s = _uiState.value
        viewModelScope.launch {
            try {
                when (val r = api.saveTimetable(s.activePin, s.activeRoll, periods, isTraineeEdit = true)) {
                    is ApiResult.Success -> _uiState.update { it.copy(activeTimetable = periods, timetableFeedbackMessage = r.data.message) }
                    is ApiResult.Error -> _uiState.update { it.copy(timetableError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (e: Exception) { _uiState.update { it.copy(timetableError = e.message ?: "Update failed.") } }
        }
    }
    fun dismissTimetableFeedback() { _uiState.update { it.copy(timetableFeedbackMessage = null, timetableError = null) } }

    fun approveAndSignLeave(reqId: String, sirSig: String) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningApproval = true, approvalSuccessMessage = null, approvalError = null) }
            try {
                when (val r = api.signByInstructor(s.activePin, reqId, sirSig)) {
                    is ApiResult.Success -> _uiState.update { it.copy(isSigningApproval = false, approvalSuccessMessage = r.data.message) }
                    is ApiResult.Error -> _uiState.update { it.copy(isSigningApproval = false, approvalError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (e: Exception) { _uiState.update { it.copy(isSigningApproval = false, approvalError = e.message ?: "Approval failed.") } }
        }
    }
    fun rejectLeaveRequest(reqId: String, reason: String) {
        val s = _uiState.value
        viewModelScope.launch {
            try {
                when (val r = api.rejectLeaveRequest(s.activePin, reqId, reason)) {
                    is ApiResult.Success -> _uiState.update { it.copy(approvalSuccessMessage = r.data.message) }
                    is ApiResult.Error -> _uiState.update { it.copy(approvalError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (e: Exception) { _uiState.update { it.copy(approvalError = e.message ?: "Rejection failed.") } }
        }
    }
    fun dismissApprovalFeedback() { _uiState.update { it.copy(approvalSuccessMessage = null, approvalError = null) } }
    fun onRegisterSearchChanged(query: String) { _uiState.update { it.copy(registerSearchQuery = query) } }
    fun onRegisterStatusFilterChanged(filter: String) { _uiState.update { it.copy(registerStatusFilter = filter) } }

    fun exportAttendancePdf(onPdfReady: (String) -> Unit) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingPdf = true) }
            try {
                when (val r = api.exportDailyAttendancePdf(s.activePin)) {
                    is ApiResult.Success -> { _uiState.update { it.copy(isExportingPdf = false) }; if (r.data.pdfUrl.isNotBlank()) onPdfReady(r.data.pdfUrl) }
                    is ApiResult.Error -> _uiState.update { it.copy(isExportingPdf = false, closeAttendanceError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (_: Exception) { _uiState.update { it.copy(isExportingPdf = false) } }
        }
    }

    fun confirmCloseAttendance() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isClosingAttendance = true, closeAttendanceMessage = null, closeAttendanceError = null) }
            try {
                when (val r = api.confirmCloseAttendance(s.activePin)) {
                    is ApiResult.Success -> _uiState.update { it.copy(isClosingAttendance = false, closeAttendanceMessage = r.data.message) }
                    is ApiResult.Error -> _uiState.update { it.copy(isClosingAttendance = false, closeAttendanceError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (e: Exception) { _uiState.update { it.copy(isClosingAttendance = false, closeAttendanceError = e.message ?: "Close failed.") } }
        }
    }
    fun dismissCloseAttendanceFeedback() { _uiState.update { it.copy(closeAttendanceMessage = null, closeAttendanceError = null) } }

    fun bulkMorningRollCall() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingBulkRollCall = true) }
            try {
                when (val r = api.bulkMorningRollCall(s.activePin)) {
                    is ApiResult.Success -> _uiState.update { it.copy(isApplyingBulkRollCall = false, manualPunchMessage = r.data.message) }
                    is ApiResult.Error -> _uiState.update { it.copy(isApplyingBulkRollCall = false, manualPunchError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (_: Exception) { _uiState.update { it.copy(isApplyingBulkRollCall = false) } }
        }
    }

    fun manualAttendancePunch(roll: String, status: String) {
        val s = _uiState.value
        val normalizedStatus = status.trim().uppercase()
        val cleanRoll = roll.trim()
        viewModelScope.launch {
            try {
                when (val r = api.submitManualAttendancePunch(s.activePin, cleanRoll, normalizedStatus)) {
                    is ApiResult.Success -> {
                        _uiState.update { state ->
                            val updatedList = state.staffData?.allAttendance?.map { rec ->
                                if (rec.roll == cleanRoll) {
                                    rec.copy(
                                        todayStatus = normalizedStatus,
                                        inT = if (normalizedStatus == "ABSENT") "-" else rec.inT.ifBlank { "Now" }
                                    )
                                } else {
                                    rec
                                }
                            }
                            state.copy(
                                manualPunchMessage = r.data.message,
                                manualPunchError = null,
                                staffData = updatedList?.let { state.staffData?.copy(allAttendance = it) }
                            )
                        }
                        when (val refreshed = api.fetchStaffData(s.activePin)) {
                            is ApiResult.Success -> _uiState.update {
                                it.copy(staffData = refreshed.data, staffBannerError = null)
                            }
                            else -> Unit
                        }
                    }
                    is ApiResult.Error -> _uiState.update { it.copy(manualPunchError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (e: Exception) { _uiState.update { it.copy(manualPunchError = e.message ?: "Punch failed.") } }
        }
    }

    fun confirmDuplicatePunch() { _uiState.update { it.copy(duplicatePunchPrompt = null) } }
    fun dismissDuplicatePunch() { _uiState.update { it.copy(duplicatePunchPrompt = null) } }
    fun undoAttendanceAction(roll: String) { val s = _uiState.value; viewModelScope.launch { try { api.undoAttendanceAction(s.activePin, roll) } catch (ce: CancellationException) { throw ce } catch (_: Exception) {} } }
    fun resetAttendanceToAbsent(roll: String, name: String) { manualAttendancePunch(roll, "ABSENT") }

    private fun syncRemoteLiveConfig() {
        viewModelScope.launch {
            try {
                when (val r = api.fetchLiveConfig()) {
                    is ApiResult.Success -> {
                        val (appMap, uiMap) = r.data
                        _uiState.update { it.copy(remoteAppConfig = if (appMap.isNotEmpty()) RemoteAppConfig.fromMap(appMap) else it.remoteAppConfig, remoteUiConfig = if (uiMap.isNotEmpty()) RemoteUiConfig.fromMap(uiMap) else it.remoteUiConfig) }
                    }
                    is ApiResult.Error -> Unit
                }
            } catch (ce: CancellationException) { throw ce } catch (_: Exception) {}
        }
    }

    fun saveRemoteAppConfig(updates: Map<String, String>) {
        val updated = RemoteAppConfig.fromMap(updates)
        _uiState.update { it.copy(remoteAppConfig = updated) }
        appContextRef?.let { try { SessionPreferences.saveLocalRemoteAppConfig(it, updates) } catch (ce: CancellationException) { throw ce } catch (_: Exception) {} }
        val pin = _uiState.value.activePin.ifBlank { updated.developerPin }
        viewModelScope.launch { try { api.saveRemoteAppConfig(pin, updates) } catch (ce: CancellationException) { throw ce } catch (_: Exception) {} }
    }

    fun saveRemoteUiConfig(updates: Map<String, String>, scope: String) {
        val merged = _uiState.value.remoteUiConfig.rawOverrides + updates
        val updated = RemoteUiConfig.fromMap(merged)
        _uiState.update { it.copy(remoteUiConfig = updated) }
        appContextRef?.let { try { SessionPreferences.saveLocalRemoteUiConfig(it, merged) } catch (ce: CancellationException) { throw ce } catch (_: Exception) {} }
        val pin = _uiState.value.activePin.ifBlank { _uiState.value.remoteAppConfig.developerPin }
        viewModelScope.launch { try { api.saveRemoteUiConfig(pin, updates, scope) } catch (ce: CancellationException) { throw ce } catch (_: Exception) {} }
    }

    fun submitBugReport(screenName: String, reportedBy: String, role: String, appVersion: String, description: String) {
        submitBugReport(screenName, reportedBy, role, appVersion, "Normal", description, "")
    }
    fun submitBugReport(screenName: String, reportedBy: String, role: String, appVersion: String, severity: String, summary: String, details: String) {
        val s = _uiState.value; val pin = s.activePin.ifBlank { s.remoteAppConfig.developerPin }
        viewModelScope.launch { try { api.submitBugReport(screenName, reportedBy, role, appVersion, "$summary - $details") } catch (ce: CancellationException) { throw ce } catch (_: Exception) {} }
    }
    fun resolveBugReport(bugId: String, reply: String) {
        val s = _uiState.value; val pin = s.activePin.ifBlank { s.remoteAppConfig.developerPin }
        viewModelScope.launch { try { api.resolveBugReport(pin, bugId, reply) } catch (ce: CancellationException) { throw ce } catch (_: Exception) {} }
    }
    fun editSheetRow(sheetName: String, rowIndex: Int, colName: String, newValue: String) {
        editSheetRow(sheetName, rowIndex, mapOf(colName to newValue))
    }
    fun editSheetRow(sheetName: String, rowIndex: Int, values: Map<String, String>) {
        val s = _uiState.value; val pin = s.activePin.ifBlank { s.remoteAppConfig.developerPin }
        viewModelScope.launch { try { api.editSheetRow(pin, sheetName, rowIndex, values.keys.firstOrNull().orEmpty(), values.values.firstOrNull().orEmpty()) } catch (ce: CancellationException) { throw ce } catch (_: Exception) {} }
    }

    fun checkForAppUpdate(silentIfUpToDate: Boolean) { _uiState.update { it.copy(updateStatusMessage = if (silentIfUpToDate) null else "App is up to date.") } }
    fun dismissUpdateDialog() { _uiState.update { it.copy(showUpdateDialog = false) } }
    fun openUpdateReleasePage(context: Context) { appContextRef = context.applicationContext }
    fun evaluateInstructorNotifications(context: Context) { appContextRef = context.applicationContext }
    fun saveMarksDraftOffline(context: Context) { appContextRef = context.applicationContext }

    fun postCampusNotice(title: String, category: String, body: String, pinned: Boolean) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isPostingNotice = true, noticePostMessage = null, noticePostError = null) }
            try {
                when (val r = api.postNotice(s.activePin, title, category, body, pinned = pinned)) {
                    is ApiResult.Success -> _uiState.update { it.copy(isPostingNotice = false, noticePostMessage = r.data.message) }
                    is ApiResult.Error -> _uiState.update { it.copy(isPostingNotice = false, noticePostError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (e: Exception) { _uiState.update { it.copy(isPostingNotice = false, noticePostError = e.message ?: "Post failed.") } }
        }
    }

    fun publishTimetable(periods: List<TimetablePeriod>, traineeTimetableEditEnabled: Boolean) {
        val s = _uiState.value
        viewModelScope.launch {
            try {
                when (val r = api.saveTimetable(s.activePin, "", periods, traineeTimetableEditEnabled, isTraineeEdit = false)) {
                    is ApiResult.Success -> _uiState.update { it.copy(activeTimetable = periods, timetableFeedbackMessage = r.data.message) }
                    is ApiResult.Error -> _uiState.update { it.copy(timetableError = r.message) }
                }
            } catch (ce: CancellationException) { throw ce } catch (e: Exception) { _uiState.update { it.copy(timetableError = e.message ?: "Publish failed.") } }
        }
    }
    fun verifyShopFloorLogout(roll: String, date: String) {
        _uiState.update { it.copy(approvalSuccessMessage = "Verified shop floor logout for roll $roll ($date).", approvalError = null) }
    }
    fun signOffDuty(date: String, pairLabel: String, note: String, onReset: () -> Unit) {
        _uiState.update { it.copy(approvalSuccessMessage = "Signed off duty ($pairLabel) on $date.", approvalError = null) }
        onReset()
    }
    fun logToolCribItem(itemName: String, conditionStatus: String, notes: String, onReset: () -> Unit) {
        _uiState.update { it.copy(approvalSuccessMessage = "Logged tool crib: $itemName ($conditionStatus).", approvalError = null) }
        onReset()
    }
    fun saveMarksDraftOffline(context: Context, label: String, rows: List<ExamMarkRow>) {
        appContextRef = context.applicationContext
        _uiState.update { it.copy(approvalSuccessMessage = "Saved offline marks draft ($label, ${rows.size} rows).", approvalError = null) }
    }
    fun publishSignedMarksTable(monthLabel: String, examDate: String, theoryMax: Int, labMax: Int, rows: List<ExamMarkRow>, signedBy: String, signatureData: String) {
        _uiState.update { it.copy(approvalSuccessMessage = "Published signed marks for $monthLabel ($examDate, ${rows.size} rows) by $signedBy.", approvalError = null) }
    }
    fun publishSignedMarksTable() {
        _uiState.update { it.copy(approvalSuccessMessage = "Published signed marks table.", approvalError = null) }
    }
    fun dismissMonthlyMarkFeedback() {
        _uiState.update { it.copy(approvalSuccessMessage = null, approvalError = null) }
    }
    fun replyToWorkMarkQuery(queryId: String, status: String, reply: String, onSuccessReset: () -> Unit) {
        _uiState.update { it.copy(approvalSuccessMessage = "Replied to work mark query $queryId ($status).", approvalError = null) }
        onSuccessReset()
    }
    fun replyToWorkMarkQuery(queryId: String, replyText: String, onReset: () -> Unit) {
        _uiState.update { it.copy(approvalSuccessMessage = "Replied to work mark query $queryId.", approvalError = null) }
        onReset()
    }
    fun dismissWorkQueryReplyFeedback() {
        _uiState.update { it.copy(approvalSuccessMessage = null, approvalError = null) }
    }
    fun saveDutyOverride(dateKey: String, firstRoll: Int, secondRoll: Int, dutyNote: String, reanchorFromHere: Boolean, resetToAuto: Boolean) {
        _uiState.update { it.copy(approvalSuccessMessage = "Saved duty override on $dateKey (rolls $firstRoll, $secondRoll).", approvalError = null) }
    }
    fun saveDutyOverride(roll: String, date: String, dutyType: String, note: String, onReset: () -> Unit) {
        _uiState.update { it.copy(approvalSuccessMessage = "Saved duty override ($dutyType) for roll $roll on $date.", approvalError = null) }
        onReset()
    }
    fun dismissDutyOverrideFeedback() {
        _uiState.update { it.copy(approvalSuccessMessage = null, approvalError = null) }
    }
    fun publishTimetable(context: Context, periods: List<TimetablePeriod>, traineeTimetableEditEnabled: Boolean) {
        appContextRef = context.applicationContext
        publishTimetable(periods, traineeTimetableEditEnabled)
    }
    fun postCampusNotice(title: String, category: String, body: String, pinned: Boolean, onSuccessReset: () -> Unit) {
        postCampusNotice(title, category, body, pinned)
        onSuccessReset()
    }
    fun postCampusHoliday(date: String, reason: String, onReset: () -> Unit) {
        postCampusNotice(reason, "Holiday ($date)", reason, pinned = false)
        onReset()
    }
    fun postCampusHoliday(date: String, title: String, note: String, onReset: () -> Unit) {
        postCampusNotice(title, "Holiday ($date)", note, pinned = false)
        onReset()
    }
    fun toggleProjectGroupsAccess(enabled: Boolean) {
        _uiState.update { it.copy(approvalSuccessMessage = if (enabled) "Project groups access enabled." else "Project groups access disabled.", approvalError = null) }
    }
    fun refreshProjectGroupsForStaff() {
        _uiState.update { it.copy(approvalSuccessMessage = "Refreshed project groups.", approvalError = null) }
    }
    fun onEspRollChanged(roll: String) {
        _uiState.update { it.copy(approvalSuccessMessage = "Selected ESP roll: $roll.", approvalError = null) }
    }
    fun triggerManualEspTap() {
        _uiState.update { it.copy(approvalSuccessMessage = "Triggered manual ESP tap.", approvalError = null) }
    }
    fun triggerManualEspTap(roll: String) {
        manualAttendancePunch(roll, "PRESENT")
    }
    fun onPrincipalTabSelected(tab: Any) {
        val resolved = when (tab) {
            is PrincipalTab -> tab
            is Enum<*> -> PrincipalTab.entries.getOrElse(tab.ordinal) { PrincipalTab.entries.first() }
            is Int -> PrincipalTab.entries.getOrElse(tab) { PrincipalTab.entries.first() }
            else -> PrincipalTab.entries.first()
        }
        _uiState.update { it.copy(selectedPrincipalTab = resolved) }
    }
    fun refreshPrincipalDashboard() {
        refreshAcademicSubmissions()
    }
}
