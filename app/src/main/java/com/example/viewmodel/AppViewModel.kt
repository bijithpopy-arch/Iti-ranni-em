package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AcademicSubmission
import com.example.data.ApiResult
import com.example.data.AttendanceApiClient
import com.example.data.AttendanceUndoRecord
import com.example.data.DuplicatePunchPrompt
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
import com.example.updater.AppReleaseInfo
import com.example.updater.AppUpdater
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
    val selectedRole: UserRole = UserRole.TRAINEE,
    val rollInput: String = "",
    val pinInput: String = "",
    val activeRoll: String = "",
    val activePin: String = "",
    val rememberMe: Boolean = false,
    val biometricEnabled: Boolean = false,
    val hasSavedCredentials: Boolean = false,
    val savedRoleLabel: String = "",
    val biometricMessage: String? = null,
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,

    val remoteAppConfig: RemoteAppConfig = RemoteAppConfig(),
    val remoteUiConfig: RemoteUiConfig = RemoteUiConfig(),
    val bugReports: List<Map<String, String>> = emptyList(),

    val isCheckingUpdate: Boolean = false,
    val availableUpdate: AppReleaseInfo? = null,
    val showUpdateDialog: Boolean = false,
    val updateStatusMessage: String? = null,

    val selectedTraineeTab: TraineeTab = TraineeTab.HOME,
    val traineeData: TraineeDashboardData? = null,
    val isRefreshingTrainee: Boolean = false,
    val traineeBannerError: String? = null,
    val unreadNoticeCount: Int = 0,

    val isSubmittingLeave: Boolean = false,
    val leaveSubmitMessage: String? = null,
    val leaveSubmitError: String? = null,

    val isSavingProjectGroup: Boolean = false,
    val projectGroupMessage: String? = null,
    val projectGroupError: String? = null,

    val isSubmittingWorkQuery: Boolean = false,
    val workQueryMessage: String? = null,
    val workQueryError: String? = null,

    val isSubmittingRunningLate: Boolean = false,
    val runningLateMessage: String? = null,
    val runningLateError: String? = null,

    val traineeAcademicSubmission: AcademicSubmission? = null,
    val isSubmittingAcademic: Boolean = false,
    val academicSubmitMessage: String? = null,
    val academicSubmitError: String? = null,

    val techNews: List<TechNewsItem> = emptyList(),
    val isLoadingTechNews: Boolean = false,
    val selectedNewsLanguage: NewsLanguageFilter = NewsLanguageFilter.BOTH,

    val activeTimetable: List<TimetablePeriod> = TimetableStore.defaultWeeklyTimetable(),
    val timetableFeedbackMessage: String? = null,
    val timetableError: String? = null,

    val parentData: ParentDashboardData? = null,
    val isRefreshingParent: Boolean = false,
    val isSubmittingParentAlert: Boolean = false,
    val parentAlertMessage: String? = null,
    val parentAlertError: String? = null,
    val parentBannerError: String? = null,

    val selectedStaffTab: StaffTab = StaffTab.ATTENDANCE_REGISTER,
    val selectedPrincipalTab: PrincipalTab = PrincipalTab.SCORECARD,
    val staffData: StaffDashboardData? = null,
    val isRefreshingStaff: Boolean = false,
    val staffBannerError: String? = null,

    val registerSearchQuery: String = "",
    val registerStatusFilter: String = "ALL",
    val isExportingPdf: Boolean = false,
    val isClosingAttendance: Boolean = false,
    val closeAttendanceMessage: String? = null,
    val closeAttendanceError: String? = null,

    val isApplyingBulkRollCall: Boolean = false,
    val manualPunchMessage: String? = null,
    val manualPunchError: String? = null,
    val duplicatePunchPrompt: DuplicatePunchPrompt? = null,
    val undoMap: Map<String, AttendanceUndoRecord> = emptyMap(),

    val isSigningApproval: Boolean = false,
    val approvalSuccessMessage: String? = null,
    val approvalError: String? = null,

    val academicSubmissions: List<AcademicSubmission> = emptyList(),
    val isLoadingAcademicSubmissions: Boolean = false,

    val shopFloorMessage: String? = null,
    val shopFloorError: String? = null,

    val isSavingMonthlyMark: Boolean = false,
    val monthlyMarkMessage: String? = null,
    val monthlyMarkError: String? = null,

    val replyingWorkQueryId: String? = null,
    val workQueryReplyMessage: String? = null,
    val workQueryReplyError: String? = null,

    val dutyOverrideMessage: String? = null,
    val dutyOverrideError: String? = null,

    val isPostingNotice: Boolean = false,
    val noticePostMessage: String? = null,
    val noticePostError: String? = null,

    val isPostingHoliday: Boolean = false,
    val holidayPostMessage: String? = null,
    val holidayPostError: String? = null,

    val espRollInput: String = "",
    val isTriggeringEspTap: Boolean = false,
    val espTapMessage: String? = null,
    val espTapError: String? = null
)

class AppViewModel(
    private val api: AttendanceApiClient = AttendanceApiClient(),
    private val newsClient: TechnicalNewsClient = TechnicalNewsClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AppUiState(techNews = newsClient.curatedElectronicsMechanicNews())
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var appContextRef: Context? = null

    init {
        refreshTechnicalNews()
        syncRemoteLiveConfig()
    }

    fun loadSavedSessionIfNeeded(context: Context): Boolean {
        appContextRef = context.applicationContext
        val localAppMap = SessionPreferences.loadLocalRemoteAppConfig(context)
        val localUiMap = SessionPreferences.loadLocalRemoteUiConfig(context)
        if (localAppMap.isNotEmpty() || localUiMap.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    remoteAppConfig = if (localAppMap.isNotEmpty()) RemoteAppConfig.fromMap(localAppMap) else it.remoteAppConfig,
                    remoteUiConfig = if (localUiMap.isNotEmpty()) RemoteUiConfig.fromMap(localUiMap) else it.remoteUiConfig
                )
            }
        }
        val saved = SessionPreferences.load(context)
        val hasCreds = saved.hasCredentials && saved.rememberMe
        _uiState.update {
            it.copy(
                selectedRole = saved.role,
                rollInput = if (saved.rememberMe) saved.roll else "",
                pinInput = if (saved.rememberMe) saved.pin else "",
                rememberMe = saved.rememberMe,
                biometricEnabled = saved.biometricEnabled,
                hasSavedCredentials = hasCreds,
                savedRoleLabel = saved.role.displayName
            )
        }
        return hasCreds && saved.biometricEnabled
    }

    fun clearSavedSession(context: Context) {
        SessionPreferences.clear(context)
        BackgroundSyncConfig.clear(context)
        BackgroundSyncScheduler.cancelAll(context)
        _uiState.update {
            it.copy(
                rollInput = "",
                pinInput = "",
                rememberMe = false,
                biometricEnabled = false,
                hasSavedCredentials = false,
                savedRoleLabel = ""
            )
        }
    }

    fun setBiometricMessage(message: String) {
        _uiState.update { it.copy(biometricMessage = message) }
    }

    fun onRoleInputChanged(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role, loginError = null) }
    }

    fun onRollInputChanged(roll: String) {
        _uiState.update { it.copy(rollInput = roll.filter(Char::isDigit).take(2), loginError = null) }
    }

    fun onPinInputChanged(pin: String) {
        _uiState.update { it.copy(pinInput = pin.trim().take(12), loginError = null) }
    }

    fun onRememberMeChanged(remember: Boolean) {
        _uiState.update { it.copy(rememberMe = remember) }
    }

    fun onBiometricEnabledChanged(enabled: Boolean) {
        _uiState.update { it.copy(biometricEnabled = enabled) }
    }

    fun login(context: Context) {
        appContextRef = context.applicationContext
        val state = _uiState.value
        val role = state.selectedRole
        val roll = state.rollInput.trim()
        val pin = state.pinInput.trim()

        // Secret Developer PIN shortcut (9037)
        if (pin == state.remoteAppConfig.developerPin) {
            _uiState.update {
                it.copy(
                    screen = SessionScreen.DeveloperDashboard,
                    activePin = pin,
                    loginError = null,
                    isLoggingIn = false
                )
            }
            refreshBugReports(pin)
            return
        }

        if ((role == UserRole.TRAINEE || role == UserRole.PARENT) && roll.isBlank()) {
            _uiState.update { it.copy(loginError = "Please enter Roll Number (1–23).") }
            return
        }
        if (pin.isBlank()) {
            _uiState.update { it.copy(loginError = "Please enter your PIN.") }
            return
        }
        if (role == UserRole.TRADE_INSTRUCTOR && !state.remoteAppConfig.instructorAccessEnabled) {
            _uiState.update { it.copy(loginError = "Instructor access is temporarily disabled.") }
            return
        }
        if (role == UserRole.PRINCIPAL && !state.remoteAppConfig.principalAccessEnabled) {
            _uiState.update { it.copy(loginError = "Principal access is temporarily disabled.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, loginError = null) }
            when (role) {
                UserRole.TRAINEE -> {
                    when (val res = api.fetchTraineeData(roll, pin)) {
                        is ApiResult.Success -> {
                            persistSessionIfNeeded(context, role, roll, pin, state.rememberMe, state.biometricEnabled)
                            val tt = TimetableStore.loadTraineeCustomEdits(context, roll)
                                ?: res.data.timetable.ifEmpty { TimetableStore.loadOfficialCache(context) }
                            val unseen = computeUnreadNotices(context, res.data.notices.map { it.id })
                            _uiState.update {
                                it.copy(
                                    isLoggingIn = false,
                                    screen = SessionScreen.TraineeDashboard,
                                    activeRoll = roll,
                                    activePin = pin,
                                    traineeData = res.data,
                                    traineeAcademicSubmission = res.data.myAcademicSubmission,
                                    activeTimetable = tt,
                                    unreadNoticeCount = unseen,
                                    traineeBannerError = null
                                )
                            }
                            AttendanceNotificationHelper.evaluateTraineePublishedUpdates(context, res.data, seedBaselineOnly = true)
                        }
                        is ApiResult.Error -> _uiState.update { it.copy(isLoggingIn = false, loginError = res.message) }
                    }
                }

                UserRole.PARENT -> {
                    when (val res = api.fetchParentData(roll, pin)) {
                        is ApiResult.Success -> {
                            persistSessionIfNeeded(context, role, roll, pin, state.rememberMe, state.biometricEnabled)
                            _uiState.update {
                                it.copy(
                                    isLoggingIn = false,
                                    screen = SessionScreen.ParentDashboard,
                                    activeRoll = roll,
                                    activePin = pin,
                                    parentData = res.data,
                                    parentBannerError = null
                                )
                            }
                            AttendanceNotificationHelper.evaluateParentPublishedUpdates(context, res.data, seedBaselineOnly = true)
                        }
                        is ApiResult.Error -> _uiState.update { it.copy(isLoggingIn = false, loginError = res.message) }
                    }
                }

                UserRole.TRADE_INSTRUCTOR, UserRole.PRINCIPAL -> {
                    when (val res = api.fetchStaffData(pin)) {
                        is ApiResult.Success -> {
                            persistSessionIfNeeded(context, role, "", pin, state.rememberMe, state.biometricEnabled)
                            val tt = res.data.timetable.ifEmpty { TimetableStore.loadOfficialCache(context) }
                            val targetScreen = if (role == UserRole.PRINCIPAL) SessionScreen.PrincipalDashboard else SessionScreen.StaffDashboard
                            _uiState.update {
                                it.copy(
                                    isLoggingIn = false,
                                    screen = targetScreen,
                                    activeRoll = "",
                                    activePin = pin,
                                    staffData = res.data,
                                    academicSubmissions = res.data.academicSubmissions,
                                    activeTimetable = tt,
                                    staffBannerError = null
                                )
                            }
                            AttendanceNotificationHelper.evaluateStaffPublishedUpdates(context, res.data, role, seedBaselineOnly = true)
                        }
                        is ApiResult.Error -> _uiState.update { it.copy(isLoggingIn = false, loginError = res.message) }
                    }
                }
            }
        }
    }

    private fun persistSessionIfNeeded(
        context: Context,
        role: UserRole,
        roll: String,
        pin: String,
        remember: Boolean,
        biometric: Boolean
    ) {
        if (remember) {
            SessionPreferences.save(context, role, roll, pin, rememberMe = true, biometricEnabled = biometric)
        } else {
            SessionPreferences.clear(context)
        }
        BackgroundSyncConfig.save(context, role, roll, pin)
        BackgroundSyncScheduler.ensureScheduled(context)
    }

    fun logout() {
        _uiState.update {
            it.copy(
                screen = SessionScreen.Login,
                activeRoll = "",
                activePin = "",
                traineeData = null,
                parentData = null,
                staffData = null,
                loginError = null
            )
        }
    }

    fun checkForAppUpdate(silentIfUpToDate: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true) }
            val info = AppUpdater.checkForUpdate(BuildConfig.VERSION_NAME)
            _uiState.update {
                it.copy(
                    isCheckingUpdate = false,
                    availableUpdate = info,
                    showUpdateDialog = info != null || !silentIfUpToDate,
                    updateStatusMessage = if (info == null) "You are on the latest version (v${BuildConfig.VERSION_NAME})." else null
                )
            }
        }
    }

    fun openUpdateReleasePage(context: Context) {
        AppUpdater.openReleasePage(context, _uiState.value.availableUpdate?.releaseUrl.orEmpty())
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun onTraineeTabSelected(tab: TraineeTab, context: Context) {
        _uiState.update { it.copy(selectedTraineeTab = tab) }
        if (tab == TraineeTab.NOTICES) markNoticesSeen(context)
    }

    fun syncNoticeStateAndPopup(context: Context) {
        val data = _uiState.value.traineeData ?: return
        val unseen = computeUnreadNotices(context, data.notices.map { it.id })
        _uiState.update { it.copy(unreadNoticeCount = unseen) }
        AttendanceNotificationHelper.evaluateTraineePublishedUpdates(context, data, seedBaselineOnly = false)
    }

    fun markNoticesSeen(context: Context) {
        val latestId = _uiState.value.traineeData?.notices?.firstOrNull()?.id ?: return
        AttendanceNotificationHelper.markNoticesSeen(context, latestId)
        _uiState.update { it.copy(unreadNoticeCount = 0) }
    }

    private fun computeUnreadNotices(context: Context, noticeIds: List<String>): Int {
        val lastSeen = AttendanceNotificationHelper.getLastSeenNoticeId(context)
        if (noticeIds.isEmpty()) return 0
        if (lastSeen.isNullOrBlank()) return noticeIds.size.coerceAtMost(9)
        val idx = noticeIds.indexOf(lastSeen)
        return if (idx == -1) noticeIds.size.coerceAtMost(9) else idx
    }

    fun refreshTraineeDashboard() {
        val state = _uiState.value
        if (state.activeRoll.isBlank() || state.activePin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingTrainee = true, traineeBannerError = null) }
            when (val res = api.fetchTraineeData(state.activeRoll, state.activePin)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isRefreshingTrainee = false,
                        traineeData = res.data,
                        traineeAcademicSubmission = res.data.myAcademicSubmission ?: it.traineeAcademicSubmission
                    )
                }
                is ApiResult.Error -> _uiState.update { it.copy(isRefreshingTrainee = false, traineeBannerError = res.message) }
            }
        }
    }

    fun submitLeaveRequest(type: String, timeWindow: String, reason: String, studentSig: String, onSuccessReset: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingLeave = true, leaveSubmitMessage = null, leaveSubmitError = null) }
            when (val res = api.submitLeave(state.activeRoll, state.activePin, type, timeWindow, reason, studentSig)) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update { it.copy(isSubmittingLeave = false, leaveSubmitMessage = res.data.message) }
                    refreshTraineeDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSubmittingLeave = false, leaveSubmitError = res.message) }
            }
        }
    }

    fun dismissLeaveFeedback() = _uiState.update { it.copy(leaveSubmitMessage = null, leaveSubmitError = null) }

    fun saveProjectGroup(title: String, members: List<Int>, leaderRoll: Int?, leaderName: String, onReset: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProjectGroup = true, projectGroupMessage = null, projectGroupError = null) }
            val resolvedLeader = leaderRoll ?: state.activeRoll.toIntOrNull() ?: 1
            when (val res = api.saveProjectGroup(state.activeRoll, state.activePin, title, members, resolvedLeader, leaderName)) {
                is ApiResult.Success -> {
                    onReset()
                    _uiState.update { it.copy(isSavingProjectGroup = false, projectGroupMessage = res.data.message) }
                    refreshTraineeDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSavingProjectGroup = false, projectGroupError = res.message) }
            }
        }
    }

    fun refreshProjectGroupsForTrainee() = refreshTraineeDashboard()
    fun dismissProjectGroupFeedback() = _uiState.update { it.copy(projectGroupMessage = null, projectGroupError = null) }

    fun submitTraineeWorkMarkQuery(monthLabel: String, workReference: String, currentMarkNote: String, traineeNote: String, onSuccessReset: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingWorkQuery = true, workQueryMessage = null, workQueryError = null) }
            when (val res = api.submitWorkMarkQuery(state.activeRoll, state.activePin, monthLabel, workReference, currentMarkNote, traineeNote)) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update { it.copy(isSubmittingWorkQuery = false, workQueryMessage = res.data.message) }
                    refreshTraineeDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSubmittingWorkQuery = false, workQueryError = res.message) }
            }
        }
    }

    fun dismissTraineeWorkMarkQueryFeedback() = _uiState.update { it.copy(workQueryMessage = null, workQueryError = null) }

    fun submitTraineeRunningLateAlert(expectedArrival: String, reasonCategory: String, details: String, onSuccessReset: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingRunningLate = true, runningLateMessage = null, runningLateError = null) }
            when (val res = api.submitRunningLateAlert(state.activeRoll, state.activePin, expectedArrival, reasonCategory, details)) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update { it.copy(isSubmittingRunningLate = false, runningLateMessage = res.data.message) }
                    refreshTraineeDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSubmittingRunningLate = false, runningLateError = res.message) }
            }
        }
    }

    fun dismissTraineeRunningLateAlertFeedback() = _uiState.update { it.copy(runningLateMessage = null, runningLateError = null) }

    fun submitTraineeAcademicRecord(theoryTopic: String, theoryStatus: String, theoryUpdatedDate: String, labJobName: String, labStatus: String, labSignedDate: String, remarks: String) {
        val state = _uiState.value
        val name = state.traineeData?.name ?: "Trainee #${state.activeRoll}"
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingAcademic = true, academicSubmitMessage = null, academicSubmitError = null) }
            when (val res = api.submitAcademicRecord(state.activeRoll, state.activePin, name, theoryTopic, theoryStatus, theoryUpdatedDate, labJobName, labStatus, labSignedDate, remarks)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmittingAcademic = false, academicSubmitMessage = res.data.message) }
                    refreshTraineeAcademicSubmission()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSubmittingAcademic = false, academicSubmitError = res.message) }
            }
        }
    }

    fun refreshTraineeAcademicSubmission() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAcademicSubmissions = true) }
            when (val res = api.fetchAcademicSubmissions(state.activePin, state.activeRoll)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoadingAcademicSubmissions = false, traineeAcademicSubmission = res.data.firstOrNull())
                }
                is ApiResult.Error -> _uiState.update { it.copy(isLoadingAcademicSubmissions = false) }
            }
        }
    }

    fun dismissTraineeAcademicFeedback() = _uiState.update { it.copy(academicSubmitMessage = null, academicSubmitError = null) }

    fun refreshTechnicalNews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTechNews = true) }
            val items = newsClient.fetchLiveNews()
            _uiState.update { it.copy(isLoadingTechNews = false, techNews = items) }
        }
    }

    fun onSelectNewsLanguage(filter: NewsLanguageFilter) {
        _uiState.update { it.copy(selectedNewsLanguage = filter) }
    }

    fun saveTraineeLocalTimetable(context: Context, periods: List<TimetablePeriod>) {
        val roll = _uiState.value.activeRoll
        TimetableStore.saveTraineeCustomEdits(context, roll, periods)
        _uiState.update { it.copy(activeTimetable = periods, timetableFeedbackMessage = "Saved personal timetable on device.") }
    }

    fun resetTraineeTimetableToOfficial(context: Context) {
        val roll = _uiState.value.activeRoll
        TimetableStore.clearTraineeCustomEdits(context, roll)
        val official = _uiState.value.traineeData?.timetable?.ifEmpty { TimetableStore.loadOfficialCache(context) }
            ?: TimetableStore.loadOfficialCache(context)
        _uiState.update { it.copy(activeTimetable = official, timetableFeedbackMessage = "Reset to official trade timetable.") }
    }

    fun submitTraineeSharedTimetable(context: Context, periods: List<TimetablePeriod>) {
        val state = _uiState.value
        viewModelScope.launch {
            when (val res = api.saveTimetable(state.activePin, state.activeRoll, periods, isTraineeEdit = true)) {
                is ApiResult.Success -> {
                    TimetableStore.saveOfficialCache(context, periods, true)
                    _uiState.update { it.copy(activeTimetable = periods, timetableFeedbackMessage = res.data.message) }
                }
                is ApiResult.Error -> _uiState.update { it.copy(timetableError = res.message) }
            }
        }
    }

    fun dismissTimetableFeedback() = _uiState.update { it.copy(timetableFeedbackMessage = null, timetableError = null) }

    fun refreshParentDashboard() {
        val state = _uiState.value
        if (state.activeRoll.isBlank() || state.activePin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingParent = true, parentBannerError = null) }
            when (val res = api.fetchParentData(state.activeRoll, state.activePin)) {
                is ApiResult.Success -> _uiState.update { it.copy(isRefreshingParent = false, parentData = res.data) }
                is ApiResult.Error -> _uiState.update { it.copy(isRefreshingParent = false, parentBannerError = res.message) }
            }
        }
    }

    fun evaluateParentNotification(context: Context) {
        val data = _uiState.value.parentData ?: return
        AttendanceNotificationHelper.evaluateParentPublishedUpdates(context, data, seedBaselineOnly = false)
    }

    fun submitParentBunkAlert(details: String, onSuccessReset: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingParentAlert = true, parentAlertMessage = null, parentAlertError = null) }
            val msg = details.trim().ifBlank { "Parent reported urgent off-campus / bunking concern for Roll #${state.activeRoll}." }
            when (val res = api.submitParentAlert(state.activeRoll, state.activePin, ParentAlertTypes.BUNK_ALERT, msg)) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update { it.copy(isSubmittingParentAlert = false, parentAlertMessage = res.data.message) }
                    refreshParentDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSubmittingParentAlert = false, parentAlertError = res.message) }
            }
        }
    }

    fun submitParentLeaveReason(reason: String, onSuccessReset: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingParentAlert = true, parentAlertMessage = null, parentAlertError = null) }
            when (val res = api.submitParentAlert(state.activeRoll, state.activePin, ParentAlertTypes.LEAVE_REASON, reason)) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update { it.copy(isSubmittingParentAlert = false, parentAlertMessage = res.data.message) }
                    refreshParentDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSubmittingParentAlert = false, parentAlertError = res.message) }
            }
        }
    }

    fun dismissParentAlertFeedback() = _uiState.update { it.copy(parentAlertMessage = null, parentAlertError = null) }

    fun onStaffTabSelected(tab: StaffTab) = _uiState.update { it.copy(selectedStaffTab = tab) }
    fun onPrincipalTabSelected(tab: PrincipalTab) = _uiState.update { it.copy(selectedPrincipalTab = tab) }

    fun refreshStaffDashboard() {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingStaff = true, staffBannerError = null) }
            when (val res = api.fetchStaffData(pin)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isRefreshingStaff = false,
                        staffData = res.data,
                        academicSubmissions = res.data.academicSubmissions
                    )
                }
                is ApiResult.Error -> _uiState.update { it.copy(isRefreshingStaff = false, staffBannerError = res.message) }
            }
        }
    }

    fun refreshPrincipalDashboard() = refreshStaffDashboard()

    fun evaluateInstructorNotifications(context: Context) {
        val data = _uiState.value.staffData ?: return
        AttendanceNotificationHelper.evaluateStaffPublishedUpdates(context, data, UserRole.TRADE_INSTRUCTOR, seedBaselineOnly = false)
    }

    fun approveAndSignLeave(reqId: String, signature: String) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningApproval = true, approvalSuccessMessage = null, approvalError = null) }
            val result = if (state.screen == SessionScreen.PrincipalDashboard) {
                api.signByPrincipal(state.activePin, reqId, signature)
            } else {
                api.signByInstructor(state.activePin, reqId, signature)
            }
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSigningApproval = false, approvalSuccessMessage = result.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSigningApproval = false, approvalError = result.message) }
            }
        }
    }

    fun rejectLeaveRequest(reqId: String, reason: String) {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningApproval = true, approvalSuccessMessage = null, approvalError = null) }
            when (val res = api.rejectLeaveRequest(pin, reqId, reason)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSigningApproval = false, approvalSuccessMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSigningApproval = false, approvalError = res.message) }
            }
        }
    }

    fun dismissApprovalFeedback() = _uiState.update { it.copy(approvalSuccessMessage = null, approvalError = null) }

    fun onRegisterSearchChanged(q: String) = _uiState.update { it.copy(registerSearchQuery = q) }
    fun onRegisterStatusFilterChanged(f: String) = _uiState.update { it.copy(registerStatusFilter = f) }

    fun exportAttendancePdf(onPdfReady: (String) -> Unit) {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingPdf = true) }
            when (val res = api.exportDailyAttendancePdf(pin)) {
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
            when (val res = api.confirmCloseAttendance(pin)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isClosingAttendance = false, closeAttendanceMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isClosingAttendance = false, closeAttendanceError = res.message) }
            }
        }
    }

    fun dismissCloseAttendanceFeedback() = _uiState.update { it.copy(closeAttendanceMessage = null, closeAttendanceError = null) }

    fun bulkMorningRollCall() {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingBulkRollCall = true, manualPunchMessage = null, manualPunchError = null) }
            when (val res = api.bulkMorningRollCall(pin)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isApplyingBulkRollCall = false, manualPunchMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isApplyingBulkRollCall = false, manualPunchError = res.message) }
            }
        }
    }

    fun manualAttendancePunch(roll: String, status: String) {
        val rec = _uiState.value.staffData?.allAttendance?.firstOrNull { it.roll == roll }
        if (rec != null && !rec.todayStatus.equals("ABSENT", true) && !rec.todayStatus.equals(status, true)) {
            _uiState.update {
                it.copy(
                    duplicatePunchPrompt = DuplicatePunchPrompt(
                        roll = roll,
                        studentName = rec.name,
                        currentStatus = rec.todayStatus,
                        requestedStatus = status
                    )
                )
            }
            return
        }
        executeManualPunch(roll, status, allowDuplicate = false)
    }

    fun confirmDuplicatePunch() {
        val prompt = _uiState.value.duplicatePunchPrompt ?: return
        _uiState.update { it.copy(duplicatePunchPrompt = null) }
        executeManualPunch(prompt.roll, prompt.requestedStatus, allowDuplicate = true)
    }

    fun dismissDuplicatePunch() = _uiState.update { it.copy(duplicatePunchPrompt = null) }

    private fun executeManualPunch(roll: String, status: String, allowDuplicate: Boolean) {
        val state = _uiState.value
        val prev = state.staffData?.allAttendance?.firstOrNull { it.roll == roll }
        viewModelScope.launch {
            when (val res = api.submitManualAttendancePunch(state.activePin, roll, status, allowDuplicate)) {
                is ApiResult.Success -> {
                    val undo = if (prev != null) {
                        AttendanceUndoRecord(
                            roll = roll,
                            studentName = prev.name,
                            previousStatus = prev.todayStatus,
                            previousInT = prev.inT,
                            previousOutT = prev.outT,
                            newStatus = status,
                            date = state.staffData.attendanceDate
                        )
                    } else null
                    _uiState.update { st ->
                        st.copy(
                            manualPunchMessage = res.data.message,
                            manualPunchError = null,
                            undoMap = if (undo != null) st.undoMap + (roll to undo) else st.undoMap
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(manualPunchError = res.message) }
            }
        }
    }

    fun undoAttendanceAction(roll: String) {
        val state = _uiState.value
        val undo = state.undoMap[roll] ?: return
        viewModelScope.launch {
            when (val res = api.undoAttendanceAction(state.activePin, roll, undo.date, undo.previousStatus, undo.previousInT, undo.previousOutT)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(manualPunchMessage = res.data.message, undoMap = it.undoMap - roll) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(manualPunchError = res.message) }
            }
        }
    }

    fun resetAttendanceToAbsent(roll: String, studentName: String) {
        val state = _uiState.value
        viewModelScope.launch {
            when (val res = api.undoAttendanceAction(state.activePin, roll, state.staffData?.attendanceDate ?: AttendanceApiClient.todayIstDateString(), "ABSENT", "-", "-")) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(manualPunchMessage = "Marked $studentName (#$roll) ABSENT.") }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(manualPunchError = res.message) }
            }
        }
    }

    fun refreshAcademicSubmissions() {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAcademicSubmissions = true) }
            when (val res = api.fetchAcademicSubmissions(pin)) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoadingAcademicSubmissions = false, academicSubmissions = res.data) }
                is ApiResult.Error -> _uiState.update { it.copy(isLoadingAcademicSubmissions = false) }
            }
        }
    }

    fun verifyAcademicSubmission(roll: String, submissionId: String, remark: String) {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            when (val res = api.verifyAcademicSubmission(pin, roll, submissionId, remark)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(approvalSuccessMessage = res.data.message) }
                    refreshAcademicSubmissions()
                }
                is ApiResult.Error -> _uiState.update { it.copy(approvalError = res.message) }
            }
        }
    }

    fun forwardSingleAcademicToPrincipal(roll: String) {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            when (val res = api.forwardAcademicSubmissionToPrincipal(pin, roll = roll, bulkAllVerified = false)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(approvalSuccessMessage = res.data.message) }
                    refreshAcademicSubmissions()
                }
                is ApiResult.Error -> _uiState.update { it.copy(approvalError = res.message) }
            }
        }
    }

    fun forwardAllVerifiedAcademicToPrincipal() {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            when (val res = api.forwardAcademicSubmissionToPrincipal(pin, roll = "", bulkAllVerified = true)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(approvalSuccessMessage = res.data.message) }
                    refreshAcademicSubmissions()
                }
                is ApiResult.Error -> _uiState.update { it.copy(approvalError = res.message) }
            }
        }
    }

    fun setLateGraceForTrainee(roll: String, mode: String, cutoff: String, note: String) {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            when (val res = api.setLateGrace(pin, roll, mode, cutoff, note)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(manualPunchMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(manualPunchError = res.message) }
            }
        }
    }

    fun sendInstantParentAlert(roll: String, name: String, alertType: String, msg: String) {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            when (val res = api.sendInstructorParentAlert(pin, roll, alertType, msg)) {
                is ApiResult.Success -> _uiState.update { it.copy(manualPunchMessage = res.data.message) }
                is ApiResult.Error -> _uiState.update { it.copy(manualPunchError = res.message) }
            }
        }
    }

    fun toggleEarlyClassLogout(enabled: Boolean) {
        val state = _uiState.value
        val releaseNow = state.staffData?.scanOverrides?.shopFloorReleaseNowEnabled ?: false
        viewModelScope.launch {
            when (val res = api.setScanOverride(state.activePin, enabled, releaseNow)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(shopFloorMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(shopFloorError = res.message) }
            }
        }
    }

    fun toggleShopFloorReleaseNow(enabled: Boolean) {
        val state = _uiState.value
        val early = state.staffData?.scanOverrides?.earlyClassLogoutEnabled ?: false
        viewModelScope.launch {
            when (val res = api.setScanOverride(state.activePin, early, enabled)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(shopFloorMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(shopFloorError = res.message) }
            }
        }
    }

    fun updateShopFloorMember(roll: String, operation: String, sessionType: String, endTime: String) {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            when (val res = api.setShopFloorMember(pin, roll, operation, sessionType, endTime)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(shopFloorMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(shopFloorError = res.message) }
            }
        }
    }

    fun recordShopFloorCardLogout(roll: String) {
        viewModelScope.launch {
            when (val res = api.triggerShopFloorCardLogout(roll)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(shopFloorMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(shopFloorError = res.message) }
            }
        }
    }

    fun verifyShopFloorLogout(roll: String, date: String) {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            when (val res = api.verifyShopFloorLogout(pin, roll, date)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(shopFloorMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(shopFloorError = res.message) }
            }
        }
    }

    fun signOffDuty(date: String, pairLabel: String, note: String, onReset: () -> Unit) {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            when (val res = api.signOffDailyDuty(pin, date, pairLabel, note)) {
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
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            when (val res = api.logToolCribIssue(pin, itemName, conditionStatus, notes)) {
                is ApiResult.Success -> {
                    onReset()
                    _uiState.update { it.copy(staffBannerError = null, noticePostMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(staffBannerError = res.message) }
            }
        }
    }

    fun saveMarksDraftOffline(context: Context, monthLabel: String, rows: List<ExamMarkRow>) {
        val count = rows.count { it.theoryMarks.isNotBlank() || it.labMarks.isNotBlank() }
        OfflineSyncQueue.enqueue(context, "MARKS_DRAFT", "$monthLabel ($count graded rows)")
        _uiState.update { it.copy(monthlyMarkMessage = "Saved $count marks row(s) offline for $monthLabel.", monthlyMarkError = null) }
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
        val gradedRows = rows.filter { it.theoryMarks.isNotBlank() || it.labMarks.isNotBlank() }
        if (gradedRows.isEmpty()) {
            _uiState.update { it.copy(monthlyMarkError = "Enter at least one trainee's marks before publishing.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingMonthlyMark = true, monthlyMarkMessage = null, monthlyMarkError = null) }
            var successCount = 0
            var lastError: String? = null
            gradedRows.forEach { row ->
                when (
                    val res = api.saveMonthlyMark(
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
                ) {
                    is ApiResult.Success -> successCount++
                    is ApiResult.Error -> lastError = res.message
                }
            }
            if (successCount > 0) {
                appContextRef?.let { OfflineSyncQueue.clear(it) }
                _uiState.update {
                    it.copy(
                        isSavingMonthlyMark = false,
                        monthlyMarkMessage = "Digitally signed & published $successCount/${gradedRows.size} trainee marks to ${AttendanceApiClient.SHEET_NAME}.",
                        monthlyMarkError = lastError
                    )
                }
                refreshStaffDashboard()
            } else {
                _uiState.update {
                    it.copy(
                        isSavingMonthlyMark = false,
                        monthlyMarkError = lastError ?: "Failed to publish marks to server."
                    )
                }
            }
        }
    }

    fun dismissMonthlyMarkFeedback() = _uiState.update { it.copy(monthlyMarkMessage = null, monthlyMarkError = null) }

    fun replyToWorkMarkQuery(queryId: String, status: String, reply: String, onSuccessReset: () -> Unit) {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            _uiState.update { it.copy(replyingWorkQueryId = queryId, workQueryReplyMessage = null, workQueryReplyError = null) }
            when (val res = api.replyWorkMarkQuery(pin, queryId, status, reply)) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update { it.copy(replyingWorkQueryId = null, workQueryReplyMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(replyingWorkQueryId = null, workQueryReplyError = res.message) }
            }
        }
    }

    fun dismissWorkQueryReplyFeedback() = _uiState.update { it.copy(workQueryReplyMessage = null, workQueryReplyError = null) }

    fun saveDutyOverride(dateKey: String, firstRoll: Int, secondRoll: Int, dutyNote: String, reanchorFromHere: Boolean, resetToAuto: Boolean) {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            when (val res = api.saveDutyOverride(pin, dateKey, firstRoll, secondRoll, dutyNote, reanchorFromHere, resetToAuto)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(dutyOverrideMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(dutyOverrideError = res.message) }
            }
        }
    }

    fun dismissDutyOverrideFeedback() = _uiState.update { it.copy(dutyOverrideMessage = null, dutyOverrideError = null) }

    fun publishTimetable(context: Context, periods: List<TimetablePeriod>, allowTraineeEdit: Boolean) {
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            when (val res = api.saveTimetable(pin, "", periods, traineeTimetableEditEnabled = allowTraineeEdit, isTraineeEdit = false)) {
                is ApiResult.Success -> {
                    TimetableStore.saveOfficialCache(context, periods, allowTraineeEdit)
                    _uiState.update { it.copy(activeTimetable = periods, timetableFeedbackMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(timetableError = res.message) }
            }
        }
    }

    fun postCampusNotice(title: String, category: String, body: String, pinned: Boolean, onSuccessReset: () -> Unit) {
        val pin = _uiState.value.activePin
        val postedBy = if (_uiState.value.screen == SessionScreen.PrincipalDashboard) "Principal" else "Trade Instructor"
        viewModelScope.launch {
            _uiState.update { it.copy(isPostingNotice = true, noticePostMessage = null, noticePostError = null) }
            when (val res = api.postNotice(pin, title, category, body, pinned = pinned, postedBy = postedBy)) {
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
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            _uiState.update { it.copy(isPostingHoliday = true, holidayPostMessage = null, holidayPostError = null) }
            when (val res = api.postHoliday(pin, date, reason)) {
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
        val pin = _uiState.value.activePin
        viewModelScope.launch {
            when (val res = api.setProjectGroupsAccess(pin, enabled)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(projectGroupMessage = res.data.message) }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(projectGroupError = res.message) }
            }
        }
    }

    fun refreshProjectGroupsForStaff() = refreshStaffDashboard()

    fun onEspRollChanged(roll: String) = _uiState.update { it.copy(espRollInput = roll.filter(Char::isDigit).take(2)) }

    fun triggerManualEspTap() {
        val roll = _uiState.value.espRollInput.trim()
        if (roll.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isTriggeringEspTap = true, espTapMessage = null, espTapError = null) }
            when (val res = api.triggerEspTap(roll)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isTriggeringEspTap = false, espTapMessage = "${res.data.name}: ${res.data.mode} at ${res.data.time}") }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isTriggeringEspTap = false, espTapError = res.message) }
            }
        }
    }

    // =====================================================================
    // Developer Master Console & Live Control
    // =====================================================================
    private fun syncRemoteLiveConfig() {
        viewModelScope.launch {
            when (val res = api.fetchLiveConfig()) {
                is ApiResult.Success -> {
                    val (appMap, uiMap) = res.data
                    _uiState.update {
                        it.copy(
                            remoteAppConfig = if (appMap.isNotEmpty()) RemoteAppConfig.fromMap(appMap) else it.remoteAppConfig,
                            remoteUiConfig = if (uiMap.isNotEmpty()) RemoteUiConfig.fromMap(uiMap) else it.remoteUiConfig
                        )
                    }
                }
                is ApiResult.Error -> Unit
            }
        }
    }

    fun saveRemoteAppConfig(updates: Map<String, String>) {
        val updated = RemoteAppConfig.fromMap(updates)
        _uiState.update { it.copy(remoteAppConfig = updated) }
        appContextRef?.let { SessionPreferences.saveLocalRemoteAppConfig(it, updates) }
        val pin = _uiState.value.activePin.ifBlank { updated.developerPin }
        viewModelScope.launch {
            api.saveRemoteAppConfig(pin, updates)
        }
    }

    fun saveRemoteUiConfig(updates: Map<String, String>, scope: String) {
        val merged = _uiState.value.remoteUiConfig.rawOverrides + updates
        val updated = RemoteUiConfig.fromMap(merged)
        _uiState.update { it.copy(remoteUiConfig = updated) }
        appContextRef?.let { SessionPreferences.saveLocalRemoteUiConfig(it, merged) }
        val pin = _uiState.value.activePin.ifBlank { _uiState.value.remoteAppConfig.developerPin }
        viewModelScope.launch {
            api.saveRemoteUiConfig(pin, updates, scope)
        }
    }

    fun submitBugReport(screenName: String, reportedBy: String, role: String, appVersion: String, description: String) {
        val localBug = mapOf(
            "BugId" to "BUG-${System.currentTimeMillis()}",
            "ScreenName" to screenName,
            "ReportedBy" to reportedBy,
            "Role" to role,
            "AppVersion" to appVersion,
            "Description" to description,
            "Status" to "OPEN"
        )
        _uiState.update { it.copy(bugReports = listOf(localBug) + it.bugReports) }
        viewModelScope.launch {
            api.submitBugReport(screenName, reportedBy, role, appVersion, description)
        }
    }

    private fun refreshBugReports(pin: String) {
        viewModelScope.launch {
            when (val res = api.fetchBugReports(pin)) {
                is ApiResult.Success -> if (res.data.isNotEmpty()) {
                    _uiState.update { it.copy(bugReports = res.data) }
                }
                is ApiResult.Error -> Unit
            }
        }
    }

    fun resolveBugReport(bugId: String, reply: String) {
        _uiState.update { st ->
            st.copy(
                bugReports = st.bugReports.map { b ->
                    if (b["BugId"] == bugId) b + mapOf("Status" to "FIXED", "DevReply" to reply) else b
                }
            )
        }
        val pin = _uiState.value.activePin.ifBlank { _uiState.value.remoteAppConfig.developerPin }
        viewModelScope.launch {
            api.resolveBugReport(pin, bugId, reply)
        }
    }

    fun editSheetRow(tabName: String, rowIndex: Int, colName: String, newValue: String) {
        val pin = _uiState.value.activePin.ifBlank { _uiState.value.remoteAppConfig.developerPin }
        viewModelScope.launch {
            when (val res = api.editSheetRow(pin, tabName, rowIndex, colName, newValue)) {
                is ApiResult.Success -> _uiState.update { it.copy(staffBannerError = null, noticePostMessage = res.data.message) }
                is ApiResult.Error -> _uiState.update { it.copy(staffBannerError = res.message) }
            }
        }
    }
}
