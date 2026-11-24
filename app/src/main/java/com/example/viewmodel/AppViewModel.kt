package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApiResult
import com.example.data.AttendanceApiClient
import com.example.data.AttendanceUndoRecord
import com.example.data.DuplicatePunchPrompt
import com.example.data.DutyOverrideEntry
import com.example.data.DutyRotationEngine
import com.example.data.EspTapResult
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

    // Login & Remember-Me / Fingerprint Unlock state
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

    // Remote Developer Config, Live UI Config & Bug Reports state
    val remoteAppConfig: RemoteAppConfig = RemoteAppConfig(),
    val remoteUiConfig: RemoteUiConfig = RemoteUiConfig(),
    val bugReports: List<Map<String, String>> = emptyList(),
    val isUpdatingDeveloperConfig: Boolean = false,
    val developerFeedbackMessage: String? = null,
    val developerError: String? = null,

    // In-App Self-Updater state
    val isCheckingUpdate: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val availableUpdate: AppUpdateInfo? = null,
    val showUpdateDialog: Boolean = false,
    val updateStatusMessage: String? = null,
    val updateError: String? = null,

    // Active session credentials
    val activeRoll: String = "",
    val activePin: String = "",
    val activeStaffRole: UserRole = UserRole.TRADE_INSTRUCTOR,

    // Notification / deep-link state
    val openNoticesPending: Boolean = false,
    val unreadNoticeCount: Int = 0,
    val lastSeenNoticeId: String? = null,

    // Trainee dashboard state
    val traineeData: TraineeDashboardData? = null,
    val selectedTraineeTab: TraineeTab = TraineeTab.HOME,
    val isRefreshingTrainee: Boolean = false,
    val traineeBannerError: String? = null,
    val isSubmittingLeave: Boolean = false,
    val leaveSubmitReqId: String? = null,
    val leaveSubmitMessage: String? = null,
    val leaveSubmitError: String? = null,

    // Trainee project group submission state
    val isSavingProjectGroup: Boolean = false,
    val projectGroupSaveMessage: String? = null,
    val projectGroupSaveError: String? = null,

    // Trainee Work/Lab Mark Query & Running Late Alert state
    val isSubmittingWorkQuery: Boolean = false,
    val workQueryMessage: String? = null,
    val workQueryError: String? = null,
    val isSubmittingRunningLate: Boolean = false,
    val runningLateMessage: String? = null,
    val runningLateError: String? = null,

    // Trainee Live Technical News Feed state
    val techNewsItems: List<TechNewsItem> = emptyList(),
    val isLoadingTechNews: Boolean = false,
    val techNewsError: String? = null,

    // Theory Notes & Lab Log Book Submission & Verification state
    val isSubmittingAcademicRecord: Boolean = false,
    val isUpdatingAcademicSubmission: Boolean = false,
    val academicActionMessage: String? = null,
    val academicActionError: String? = null,

    // Parent dashboard state
    val parentData: ParentDashboardData? = null,
    val isRefreshingParent: Boolean = false,
    val parentBannerError: String? = null,
    val isSubmittingParentAlert: Boolean = false,
    val parentAlertSuccessMessage: String? = null,
    val parentAlertError: String? = null,
    val parentNotificationDispatched: Boolean = false,
    val parentMarksNotificationDispatched: Boolean = false,

    // Staff & Principal dashboard state
    val staffData: StaffDashboardData? = null,
    val selectedStaffTab: StaffTab = StaffTab.ATTENDANCE_REGISTER,
    val selectedPrincipalTab: PrincipalTab = PrincipalTab.SCORECARD,
    val isRefreshingStaff: Boolean = false,
    val staffBannerError: String? = null,

    // Staff Confirm & Close Attendance + Bulk Roll Call + Exceptions
    val isClosingAttendance: Boolean = false,
    val closeAttendanceMessage: String? = null,
    val closeAttendanceError: String? = null,
    val isRunningRollCallOp: Boolean = false,
    val rollCallFeedbackMessage: String? = null,
    val rollCallError: String? = null,

    // Staff Late Grace Quick-Mark Table state
    val updatingLateGraceRoll: String? = null,
    val lateGraceMessage: String? = null,
    val lateGraceError: String? = null,

    // Staff Front-Panel Manual Punch, Duplicate Guard & Undo state
    val manualPunchingRoll: String? = null,
    val undoingRoll: String? = null,
    val duplicatePunchPrompt: DuplicatePunchPrompt? = null,
    val recentUndoByRoll: Map<String, AttendanceUndoRecord> = emptyMap(),
    val lastUndoRecord: AttendanceUndoRecord? = null,

    // Staff Monthly Marks & Work/Lab Query Review state
    val isSavingMonthlyMark: Boolean = false,
    val monthlyMarkMessage: String? = null,
    val monthlyMarkError: String? = null,
    val replyingWorkQueryId: String? = null,
    val workQueryReplyMessage: String? = null,
    val workQueryReplyError: String? = null,

    // Shop Floor & Instructor Scan Override state
    val isUpdatingScanOverride: Boolean = false,
    val scanOverrideMessage: String? = null,
    val scanOverrideError: String? = null,
    val isUpdatingShopFloor: Boolean = false,
    val shopFloorActionMessage: String? = null,
    val shopFloorActionError: String? = null,

    // Duty Sign-off, Live Duty Override & Tool Crib state
    val isUpdatingOps: Boolean = false,
    val opsFeedbackMessage: String? = null,
    val opsError: String? = null,
    val isSavingDutyOverride: Boolean = false,
    val dutyOverrideMessage: String? = null,
    val dutyOverrideError: String? = null,

    // Editable Weekly Timetable state (Official Sheet + Trainee Local Cache)
    val officialTimetableCache: List<TimetablePeriod> = TimetableStore.defaultWeeklyTimetable(),
    val traineeCustomTimetable: List<TimetablePeriod>? = null,
    val isSavingTimetable: Boolean = false,
    val timetableFeedbackMessage: String? = null,
    val timetableError: String? = null,

    // Staff Holiday & Notice publishing state
    val isPostingHoliday: Boolean = false,
    val holidayPostMessage: String? = null,
    val holidayPostError: String? = null,
    val isPostingNotice: Boolean = false,
    val noticePostMessage: String? = null,
    val noticePostError: String? = null,

    // Staff Project Groups toggle state
    val isTogglingProjectAccess: Boolean = false,
    val projectAccessMessage: String? = null,
    val projectAccessError: String? = null,

    // Staff & Principal leave approval / rejection state
    val signingReqId: String? = null,
    val approvalSuccessMessage: String? = null,
    val approvalPdfUrl: String? = null,
    val approvalError: String? = null,

    // Staff attendance register state
    val registerSearchQuery: String = "",
    val registerStatusFilter: String = "ALL",
    val isExportingPdf: Boolean = false,
    val exportedPdfUrl: String? = null,
    val exportPdfError: String? = null,

    // Staff ESP terminal state
    val espRollInput: String = "",
    val isTappingEsp: Boolean = false,
    val lastEspTap: EspTapResult? = null,
    val espTapHistory: List<EspTapResult> = emptyList(),
    val espTapError: String? = null
)class AppViewModel(
    private val apiClient: AttendanceApiClient = AttendanceApiClient(),
    private val newsClient: TechnicalNewsClient = TechnicalNewsClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var sessionLoadedOnce = false

    // ---------- Session Persistence & Biometrics ----------

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

    fun onRememberMeChanged(enabled: Boolean) {
        _uiState.update {
            it.copy(
                rememberMe = enabled,
                biometricEnabled = if (!enabled) false else it.biometricEnabled
            )
        }
    }

    fun onBiometricEnabledChanged(enabled: Boolean) {
        _uiState.update {
            it.copy(
                biometricEnabled = enabled,
                rememberMe = if (enabled) true else it.rememberMe
            )
        }
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
                biometricMessage = "Saved login credentials cleared from this device."
            )
        }
    }

    // ---------- Developer Master Console, Live UI Config & Bug Reports ----------

    fun openDeveloperConsole(pin: String) {
        if (pin.trim() != _uiState.value.remoteAppConfig.developerPin) {
            _uiState.update { it.copy(loginError = "Invalid Developer PIN.") }
            return
        }
        _uiState.update {
            it.copy(screen = SessionScreen.DeveloperDashboard, loginError = null)
        }
    }

    fun saveRemoteAppConfig(updates: Map<String, String>) {
        val currentCfg = _uiState.value.remoteAppConfig
        val merged = mutableMapOf(
            "developerPin" to currentCfg.developerPin,
            "updaterEnabled" to currentCfg.updaterEnabled.toString(),
            "releaseUpdateToUsers" to currentCfg.releaseUpdateToUsers.toString(),
            "instructorAccessEnabled" to currentCfg.instructorAccessEnabled.toString(),
            "principalAccessEnabled" to currentCfg.principalAccessEnabled.toString(),
            "maintenanceMode" to currentCfg.maintenanceMode.toString(),
            "maintenanceBanner" to currentCfg.maintenanceBanner,
            "examMarksEnabled" to currentCfg.examMarksEnabled.toString(),
            "shopFloorEnabled" to currentCfg.shopFloorEnabled.toString(),
            "undoEnabled" to currentCfg.undoEnabled.toString(),
            "alertRepliesEnabled" to currentCfg.alertRepliesEnabled.toString()
        ).apply { putAll(updates) }
        _uiState.update {
            it.copy(
                remoteAppConfig = RemoteAppConfig.fromMap(merged),
                developerFeedbackMessage = "Updated remote app settings."
            )
        }
    }

    fun updateRemoteAppConfig(updates: Map<String, String>) = saveRemoteAppConfig(updates)

    fun saveRemoteUiConfig(updates: Map<String, String>, screen: String = "Global") {
        val merged = _uiState.value.remoteUiConfig.rawOverrides.toMutableMap().apply { putAll(updates) }
        _uiState.update {
            it.copy(
                remoteUiConfig = RemoteUiConfig.fromMap(merged),
                developerFeedbackMessage = "Live UI settings updated ($screen)."
            )
        }
    }

    fun submitBugReport(
        screenName: String,
        reportedBy: String,
        role: String,
        appVersion: String,
        description: String
    ) {
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
        _uiState.update { state ->
            state.copy(
                bugReports = state.bugReports.map {
                    if (it["BugId"] == bugId) it + mapOf("Status" to "FIXED", "DevReply" to reply) else it
                }
            )
        }
    }

    fun editSheetRow(tabName: String, rowIndex: Int, colName: String, newValue: String) {
        _uiState.update {
            it.copy(developerFeedbackMessage = "Queued edit on $tabName row $rowIndex ($colName = $newValue).")
        }
    }

    fun editSheetCell(tabName: String, rowIndex: Int, colName: String, newValue: String) =
        editSheetRow(tabName, rowIndex, colName, newValue)

    // ---------- In-App Self-Updater ----------

    fun checkForAppUpdate(silentIfUpToDate: Boolean = false) {
        if (!_uiState.value.remoteAppConfig.updaterEnabled) return
        if (_uiState.value.isCheckingUpdate) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCheckingUpdate = true,
                    updateError = null,
                    updateStatusMessage = if (!silentIfUpToDate) "Checking GitHub Releases..." else null
                )
            }
            when (val result = AppUpdater.checkForUpdate()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            availableUpdate = result.info,
                            showUpdateDialog = true,
                            updateStatusMessage = "Update v${result.info.versionName} available!",
                            updateError = null
                        )
                    }
                }

                is UpdateCheckResult.UpToDate -> {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            availableUpdate = null,
                            showUpdateDialog = !silentIfUpToDate,
                            updateStatusMessage = if (silentIfUpToDate) null else "You are on the latest version (v${result.currentVersion}).",
                            updateError = null
                        )
                    }
                }

                is UpdateCheckResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            showUpdateDialog = !silentIfUpToDate,
                            updateStatusMessage = null,
                            updateError = if (silentIfUpToDate) null else result.message
                        )
                    }
                }
            }
        }
    }

    fun startApkUpdateDownload(context: Context) {
        val info = _uiState.value.availableUpdate ?: return
        if (!AppUpdater.canRequestPackageInstalls(context)) {
            _uiState.update {
                it.copy(
                    updateStatusMessage = "Please enable 'Allow from this source' for Govt. ITI Ranni, then tap Install Update again."
                )
            }
            AppUpdater.openUnknownSourcesSettings(context)
            return
        }

        AppUpdater.downloadAndInstallApk(
            context = context,
            updateInfo = info,
            onStarted = {
                _uiState.update {
                    it.copy(
                        isDownloadingUpdate = true,
                        updateStatusMessage = "Downloading v${info.versionName} via DownloadManager...",
                        updateError = null
                    )
                }
            },
            onComplete = { success, msg ->
                _uiState.update {
                    it.copy(
                        isDownloadingUpdate = false,
                        updateStatusMessage = if (success) msg else null,
                        updateError = if (success) null else msg
                    )
                }
            }
        )
    }

    fun dismissUpdateDialog() {
        _uiState.update {
            it.copy(
                showUpdateDialog = false,
                updateStatusMessage = null,
                updateError = null
            )
        }
    }

    // ---------- Deep Links & Login ----------

    fun handleOpenNoticesDeepLink() {
        _uiState.update { state ->
            when (state.screen) {
                SessionScreen.TraineeDashboard -> state.copy(
                    selectedTraineeTab = TraineeTab.NOTICES,
                    openNoticesPending = false
                )
                SessionScreen.Login -> state.copy(
                    selectedRole = UserRole.TRAINEE,
                    openNoticesPending = true
                )
                else -> state.copy(openNoticesPending = false)
            }
        }
    }

    fun handleNotificationTapRefresh() {
        when (_uiState.value.screen) {
            SessionScreen.TraineeDashboard -> refreshTraineeDashboard()
            SessionScreen.ParentDashboard -> refreshParentDashboard()
            SessionScreen.StaffDashboard, SessionScreen.PrincipalDashboard -> refreshStaffDashboard()
            SessionScreen.DeveloperDashboard, SessionScreen.Login -> Unit
        }
    }

    fun onRoleSelected(role: UserRole) {
        _uiState.update {
            it.copy(
                selectedRole = role,
                loginError = null
            )
        }
    }

    fun onRoleInputChanged(role: UserRole) = onRoleSelected(role)

    fun onRollInputChanged(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(2)
        _uiState.update { it.copy(rollInput = digitsOnly, loginError = null) }
    }

    fun onPinInputChanged(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(12)
        _uiState.update { it.copy(pinInput = digitsOnly, loginError = null) }
    }

    fun login(context: Context? = null) {
        val current = _uiState.value
        if (current.isLoggingIn) return
        if (current.pinInput.trim() == current.remoteAppConfig.developerPin) {
            openDeveloperConsole(current.pinInput.trim())
            return
        }
        if (current.remoteAppConfig.maintenanceMode) {
            _uiState.update {
                it.copy(loginError = current.remoteAppConfig.maintenanceBanner.ifBlank { "App is under maintenance." })
            }
            return
        }

        when (current.selectedRole) {
            UserRole.TRAINEE -> {
                val rollNum = current.rollInput.toIntOrNull()
                if (rollNum == null || rollNum !in 1..DutyRotationEngine.TOTAL_TRAINEES) {
                    _uiState.update { it.copy(loginError = "Please enter a valid Roll Number between 1 and 23.") }
                    return
                }
                if (current.pinInput.isBlank()) {
                    _uiState.update { it.copy(loginError = "Please enter your Trainee PIN.") }
                    return
                }
                performTraineeLogin(rollNum.toString(), current.pinInput.trim(), context)
            }

            UserRole.PARENT -> {
                val rollNum = current.rollInput.toIntOrNull()
                if (rollNum == null || rollNum !in 1..DutyRotationEngine.TOTAL_TRAINEES) {
                    _uiState.update { it.copy(loginError = "Please enter your ward's Roll Number (1–23).") }
                    return
                }
                if (current.pinInput.isBlank()) {
                    _uiState.update { it.copy(loginError = "Please enter your Parent PIN.") }
                    return
                }
                performParentLogin(rollNum.toString(), current.pinInput.trim(), context)
            }

            UserRole.TRADE_INSTRUCTOR, UserRole.PRINCIPAL -> {
                if (current.pinInput.isBlank()) {
                    _uiState.update { it.copy(loginError = "Please enter your Staff PIN.") }
                    return
                }
                if (current.selectedRole == UserRole.TRADE_INSTRUCTOR && !current.remoteAppConfig.instructorAccessEnabled) {
                    _uiState.update { it.copy(loginError = "Instructor (2025) admin access is currently shut down.") }
                    return
                }
                if (current.selectedRole == UserRole.PRINCIPAL && !current.remoteAppConfig.principalAccessEnabled) {
                    _uiState.update { it.copy(loginError = "Principal (9999) admin access is currently shut down.") }
                    return
                }
                performStaffLogin(current.selectedRole, current.pinInput.trim(), context)
            }
        }
    }

    private fun persistSessionAfterSuccess(
        context: Context?,
        role: UserRole,
        roll: String,
        pin: String
    ) {
        if (context == null) return
        val state = _uiState.value
        SessionPreferences.save(
            context = context,
            rememberMe = state.rememberMe,
            biometricEnabled = state.biometricEnabled,
            role = role,
            roll = roll,
            pin = pin
        )
        BackgroundSyncConfig.save(context, role, roll, pin)
        BackgroundSyncScheduler.ensureScheduled(context)
    }

    private fun performTraineeLogin(roll: String, pin: String, context: Context?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, loginError = null, biometricMessage = null) }
            when (val result = apiClient.fetchTraineeData(roll = roll, pin = pin)) {
                is ApiResult.Success -> {
                    persistSessionAfterSuccess(context, UserRole.TRAINEE, roll, pin)
                    if (context != null) {
                        AttendanceNotificationHelper.evaluateTraineePublishedUpdates(
                            context = context,
                            data = result.data,
                            seedBaselineOnly = true
                        )
                    }
                    val officialList = if (result.data.timetable.isNotEmpty()) {
                        if (context != null) {
                            TimetableStore.saveOfficialCache(context, result.data.timetable, result.data.traineeTimetableEditEnabled)
                        }
                        result.data.timetable
                    } else if (context != null) {
                        TimetableStore.loadOfficialCache(context)
                    } else {
                        TimetableStore.defaultWeeklyTimetable()
                    }
                    val customLocal = if (context != null) {
                        TimetableStore.loadTraineeCustomEdits(context, roll)
                    } else null
                    _uiState.update { state ->
                        val initialTab = if (state.openNoticesPending) TraineeTab.NOTICES else TraineeTab.HOME
                        state.copy(
                            isLoggingIn = false,
                            screen = SessionScreen.TraineeDashboard,
                            selectedTraineeTab = initialTab,
                            openNoticesPending = false,
                            activeRoll = roll,
                            activePin = pin,
                            hasSavedCredentials = state.rememberMe,
                            traineeData = result.data,
                            officialTimetableCache = officialList,
                            traineeCustomTimetable = customLocal,
                            traineeBannerError = null,
                            leaveSubmitReqId = null,
                            leaveSubmitMessage = null,
                            leaveSubmitError = null,
                            projectGroupSaveMessage = null,
                            projectGroupSaveError = null,
                            timetableFeedbackMessage = null,
                            timetableError = null
                        )
                    }
                }

                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isLoggingIn = false, loginError = result.message)
                    }
                }
            }
        }
    }

    private fun performParentLogin(roll: String, pin: String, context: Context?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, loginError = null, biometricMessage = null) }
            when (val result = apiClient.fetchParentData(roll = roll, pin = pin)) {
                is ApiResult.Success -> {
                    persistSessionAfterSuccess(context, UserRole.PARENT, roll, pin)
                    if (context != null) {
                        AttendanceNotificationHelper.evaluateParentPublishedUpdates(
                            context = context,
                            data = result.data,
                            seedBaselineOnly = true
                        )
                    }
                    _uiState.update { state ->
                        state.copy(
                            isLoggingIn = false,
                            screen = SessionScreen.ParentDashboard,
                            activeRoll = roll,
                            activePin = pin,
                            hasSavedCredentials = state.rememberMe,
                            parentData = result.data,
                            parentBannerError = null,
                            parentAlertSuccessMessage = null,
                            parentAlertError = null,
                            parentNotificationDispatched = false,
                            parentMarksNotificationDispatched = false
                        )
                    }
                }

                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isLoggingIn = false, loginError = result.message)
                    }
                }
            }
        }
    }

    private fun performStaffLogin(role: UserRole, pin: String, context: Context?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, loginError = 
                    // ---------- Trainee Operations ----------

    fun onTraineeTabSelected(tab: TraineeTab, context: Context) {
        _uiState.update { it.copy(selectedTraineeTab = tab) }
        if (tab == TraineeTab.NOTICES) {
            markNoticesSeen(context)
            if (_uiState.value.techNewsItems.isEmpty() && !_uiState.value.isLoadingTechNews) {
                refreshTechnicalNews()
            }
        } else if (tab == TraineeTab.PROJECT_GROUPS) {
            refreshProjectGroupsForTrainee()
        } else if (tab == TraineeTab.ACADEMIC_RECORDS) {
            refreshTraineeAcademicSubmission()
        }
    }

    fun refreshTechnicalNews() {
        if (_uiState.value.isLoadingTechNews) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTechNews = true, techNewsError = null) }
            when (val result = newsClient.fetchLiveNews()) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingTechNews = false,
                            techNewsItems = result.data,
                            techNewsError = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingTechNews = false,
                            techNewsError = result.message
                        )
                    }
                }
            }
        }
    }

    fun syncNoticeStateAndPopup(context: Context) {
        val traineeData = _uiState.value.traineeData
        val notices = traineeData?.notices.orEmpty()
        val lastSeen = AttendanceNotificationHelper.getLastSeenNoticeId(context)
        val unread = AttendanceNotificationHelper.countUnreadNotices(context, notices)
        _uiState.update {
            it.copy(unreadNoticeCount = unread, lastSeenNoticeId = lastSeen)
        }
        if (notices.isNotEmpty() && _uiState.value.selectedTraineeTab != TraineeTab.NOTICES) {
            AttendanceNotificationHelper.notifyNewNoticeIfNeeded(
                context = context,
                notices = notices,
                pinnedBroadcast = traineeData?.pinnedBroadcast
            )
        }
    }

    fun markNoticesSeen(context: Context) {
        val latestId = _uiState.value.traineeData?.notices?.firstOrNull()?.id
        AttendanceNotificationHelper.markNoticesSeen(context, latestId)
        _uiState.update {
            it.copy(unreadNoticeCount = 0, lastSeenNoticeId = latestId ?: it.lastSeenNoticeId)
        }
    }

    fun refreshTraineeDashboard() {
        val current = _uiState.value
        if (current.isRefreshingTrainee || current.activeRoll.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingTrainee = true, traineeBannerError = null) }
            when (val result = apiClient.fetchTraineeData(current.activeRoll, current.activePin)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isRefreshingTrainee = false, traineeData = result.data, traineeBannerError = null)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isRefreshingTrainee = false, traineeBannerError = result.message)
                    }
                }
            }
        }
    }

    fun refreshProjectGroupsForTrainee() {
        val current = _uiState.value
        if (current.activeRoll.isBlank()) return
        viewModelScope.launch {
            when (val result = apiClient.fetchProjectGroups(pin = current.activePin, roll = current.activeRoll)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val existing = state.traineeData ?: return@update state
                        state.copy(
                            traineeData = existing.copy(
                                projectGroupsEnabled = result.data.enabled,
                                projectGroups = result.data.groups
                            )
                        )
                    }
                }
                is ApiResult.Error -> Unit
            }
        }
    }

    fun saveProjectGroup(
        projectTitle: String,
        memberRolls: List<Int>,
        leaderRoll: Int?,
        leaderName: String,
        onSuccessReset: () -> Unit
    ) {
        val current = _uiState.value
        if (current.isSavingProjectGroup) return
        val enabled = current.traineeData?.projectGroupsEnabled ?: false
        if (!enabled) {
            _uiState.update {
                it.copy(projectGroupSaveError = "Project Groups submission is currently locked by the Trade Instructor.")
            }
            return
        }
        val distinctMembers = memberRolls.distinct().filter { it in 1..DutyRotationEngine.TOTAL_TRAINEES }.sorted()
        if (projectTitle.isBlank()) {
            _uiState.update { it.copy(projectGroupSaveError = "Please enter your Project Title.") }
            return
        }
        if (distinctMembers.isEmpty() || distinctMembers.size > 4) {
            _uiState.update { it.copy(projectGroupSaveError = "Select between 1 and 4 trainee members (maximum 4 total).") }
            return
        }
        if (leaderRoll == null || leaderRoll !in distinctMembers) {
            _uiState.update { it.copy(projectGroupSaveError = "Select exactly 1 Group Leader from the chosen members.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSavingProjectGroup = true, projectGroupSaveMessage = null, projectGroupSaveError = null)
            }
            val resolvedLeaderName = leaderName.trim().ifBlank { "Roll #$leaderRoll" }
            when (
                val result = apiClient.saveProjectGroup(
                    roll = current.activeRoll,
                    pin = current.activePin,
                    projectTitle = projectTitle,
                    memberRolls = distinctMembers,
                    leaderRoll = leaderRoll,
                    leaderName = resolvedLeaderName
                )
            ) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update { state ->
                        val updatedTrainee = state.traineeData?.let { td ->
                            val updatedList = listOf(result.data.group) +
                                td.projectGroups.filterNot { it.groupId == result.data.group.groupId }
                            td.copy(projectGroups = updatedList)
                        }
                        state.copy(
                            isSavingProjectGroup = false,
                            traineeData = updatedTrainee,
                            projectGroupSaveMessage = result.data.message,
                            projectGroupSaveError = null
                        )
                    }
                    refreshProjectGroupsForTrainee()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isSavingProjectGroup = false, projectGroupSaveError = result.message)
                    }
                }
            }
        }
    }

    fun dismissProjectGroupFeedback() {
        _uiState.update { it.copy(projectGroupSaveMessage = null, projectGroupSaveError = null) }
    }

    fun submitLeaveRequest(
        type: String,
        timeWindow: String,
        reason: String,
        studentSig: String,
        onSuccessReset: () -> Unit
    ) {
        val current = _uiState.value
        if (current.isSubmittingLeave) return
        if (timeWindow.isBlank()) {
            _uiState.update { it.copy(leaveSubmitError = "Please specify a time window (e.g., 11:00 AM - 12:00 PM).") }
            return
        }
        if (reason.isBlank()) {
            _uiState.update { it.copy(leaveSubmitError = "Please enter the reason for your out-pass or leave.") }
            return
        }
        if (studentSig.isBlank()) {
            _uiState.update { it.copy(leaveSubmitError = "Please provide your digital signature before submitting.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSubmittingLeave = true, leaveSubmitError = null, leaveSubmitMessage = null, leaveSubmitReqId = null)
            }
            when (
                val result = apiClient.submitLeave(
                    roll = current.activeRoll,
                    pin = current.activePin,
                    type = type,
                    timeWindow = timeWindow,
                    reason = reason,
                    studentSig = studentSig
                )
            ) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update {
                        it.copy(
                            isSubmittingLeave = false,
                            leaveSubmitReqId = result.data.reqId.ifBlank { null },
                            leaveSubmitMessage = result.data.message,
                            leaveSubmitError = null
                        )
                    }
                    refreshTraineeDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isSubmittingLeave = false, leaveSubmitError = result.message)
                    }
                }
            }
        }
    }

    fun dismissLeaveFeedback() {
        _uiState.update {
            it.copy(leaveSubmitReqId = null, leaveSubmitMessage = null, leaveSubmitError = null)
        }
    }

    fun submitTraineeWorkMarkQuery(
        monthLabel: String,
        workReference: String,
        currentMarkNote: String,
        traineeNote: String,
        onSuccessReset: () -> Unit
    ) {
        val current = _uiState.value
        if (current.isSubmittingWorkQuery || current.activeRoll.isBlank()) return
        if (workReference.isBlank()) {
            _uiState.update { it.copy(workQueryError = "Please enter the Lab / Workshop task reference.") }
            return
        }
        if (traineeNote.isBlank()) {
            _uiState.update { it.copy(workQueryError = "Please describe your mark query or review request.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSubmittingWorkQuery = true, workQueryMessage = null, workQueryError = null)
            }
            when (
                val result = apiClient.submitWorkMarkQuery(
                    roll = current.activeRoll,
                    pin = current.activePin,
                    monthLabel = monthLabel.ifBlank { "Current Month" },
                    workReference = workReference,
                    currentMarkNote = currentMarkNote,
                    traineeNote = traineeNote
                )
            ) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update {
                        it.copy(
                            isSubmittingWorkQuery = false,
                            workQueryMessage = result.data.message,
                            workQueryError = null
                        )
                    }
                    refreshTraineeDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isSubmittingWorkQuery = false, workQueryError = result.message)
                    }
                }
            }
        }
    }

    fun dismissWorkQueryFeedback() {
        _uiState.update { it.copy(workQueryMessage = null, workQueryError = null) }
    }

    fun dismissTraineeWorkMarkQueryFeedback() = dismissWorkQueryFeedback()

    fun submitTraineeRunningLateAlert(
        expectedArrival: String,
        reasonCategory: String,
        details: String,
        onSuccessReset: () -> Unit
    ) {
        val current = _uiState.value
        if (current.isSubmittingRunningLate || current.activeRoll.isBlank()) return
        if (expectedArrival.isBlank()) {
            _uiState.update { it.copy(runningLateError = "Please select or enter your expected arrival time.") }
            return
        }
        if (reasonCategory.isBlank()) {
            _uiState.update { it.copy(runningLateError = "Please choose a reason for being late.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSubmittingRunningLate = true, runningLateMessage = null, runningLateError = null)
            }
            when (
                val result = apiClient.submitRunningLateAlert(
                    roll = current.activeRoll,
                    pin = current.activePin,
                    expectedArrival = expectedArrival,
                    reasonCategory = reasonCategory,
                    details = details
                )
            ) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update {
                        it.copy(
                            isSubmittingRunningLate = false,
                            runningLateMessage = result.data.message,
                            runningLateError = null
                        )
                    }
                    refreshTraineeDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isSubmittingRunningLate = false, runningLateError = result.message)
                    }
                }
            }
        }
    }

    fun dismissRunningLateFeedback() {
        _uiState.update { it.copy(runningLateMessage = null, runningLateError = null) }
    }

    fun dismissTraineeRunningLateAlertFeedback() = dismissRunningLateFeedback()

    // ---------- Parent Operations ----------

    fun refreshParentDashboard() {
        val current = _uiState.value
        if (current.isRefreshingParent || current.activeRoll.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingParent = true, parentBannerError = null) }
            when (val result = apiClient.fetchParentData(current.activeRoll, current.activePin)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isRefreshingParent = false, parentData = result.data, parentBannerError = null)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isRefreshingParent = false, parentBannerError = result.message)
                    }
                }
            }
        }
    }

    fun evaluateParentNotification(context: Context) {
        val data = _uiState.value.parentData ?: return
        val attendanceFired = AttendanceNotificationHelper.notifyParentIfConfirmedAndEligible(context, data)
        val marksFired = AttendanceNotificationHelper.notifyParentMarksUpdatedIfNeeded(context, data)
        if (attendanceFired || marksFired) {
            _uiState.update {
                it.copy(
                    parentNotificationDispatched = it.parentNotificationDispatched || attendanceFired,
                    parentMarksNotificationDispatched = it.parentMarksNotificationDispatched || marksFired
                )
            }
        }
    }

    fun submitParentBunkAlert(optionalNote: String, onSuccessReset: () -> Unit) {
        val note = optionalNote.trim().ifBlank {
            "Left home for ITI — not reached / possible bunking (വീട്ടിൽ നിന്ന് ഇറങ്ങിയിട്ടുണ്ട്)"
        }
        submitParentAlertInternal(ParentAlertTypes.BUNK_ALERT, note, onSuccessReset)
    }

    fun submitParentLeaveReason(reasonText: String, onSuccessReset: () -> Unit) {
        if (reasonText.isBlank()) {
            _uiState.update {
                it.copy(parentAlertError = "Please type a leave reason in English, Manglish, or മലയാളം.")
            }
            return
        }
        submitParentAlertInternal(ParentAlertTypes.LEAVE_REASON, reasonText.trim(), onSuccessReset)
    }

    private fun submitParentAlertInternal(
        alertType: String,
        message: String,
        onSuccessReset: () -> Unit
    ) {
        val current = _uiState.value
        if (current.isSubmittingParentAlert) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSubmittingParentAlert = true, parentAlertSuccessMessage = null, parentAlertError = null)
            }
            when (
                val result = apiClient.submitParentAlert(
                    roll = current.activeRoll,
                    pin = current.activePin,
                    alertType = alertType,
                    message = message
                )
            ) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update {
                        it.copy(
                            isSubmittingParentAlert = false,
                            parentAlertSuccessMessage = result.data.message,
                            parentAlertError = null
                        )
                    }
                    refreshParentDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isSubmittingParentAlert = false, parentAlertError = result.message)
                    }
                }
            }
        }
    }

    fun dismissParentAlertFeedback() {
        _uiState.update { it.copy(parentAlertSuccessMessage = null, parentAlertError = null) }
    }

    // ---------- Staff & Principal Navigation & Refresh ----------

    fun onStaffTabSelected(tab: StaffTab) {
        _uiState.update { it.copy(selectedStaffTab = tab) }
        if (tab == StaffTab.ACADEMIC_RECORDS) {
            refreshStaffAcademicSubmissions()
        }
    }

    fun onPrincipalTabSelected(tab: PrincipalTab) {
        _uiState.update { it.copy(selectedPrincipalTab = tab) }
        if (tab == PrincipalTab.ACADEMIC_RECORDS) {
            refreshStaffAcademicSubmissions()
        }
    }

    fun refreshStaffDashboard() {
        val current = _uiState.value
        if (current.isRefreshingStaff || current.activePin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingStaff = true, staffBannerError = null) }
            when (val result = apiClient.fetchStaffData(current.activePin)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isRefreshingStaff = false, staffData = result.data, staffBannerError = null)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isRefreshingStaff = false, staffBannerError = result.message)
                    }
                }
            }
        }
    }

    fun refreshPrincipalDashboard() = refreshStaffDashboard()

    fun evaluateInstructorUrgentNotifications(context: Context) {
        val data = _uiState.value.staffData ?: return
        AttendanceNotificationHelper.notifyInstructorUrgentAlertsIfNeeded(context, data)
    }

    fun evaluateInstructorNotifications(context: Context) = evaluateInstructorUrgentNotifications(context)
    // ---------- Instructor Scan Overrides & Shop Floor ----------

    fun toggleEarlyClassLogoutOverride(enabled: Boolean) {
        val current = _uiState.value
        if (current.isUpdatingScanOverride) return
        val currentShopRelease = current.staffData?.scanOverrides?.shopFloorReleaseNowEnabled ?: false
        updateScanOverrides(earlyClassLogout = enabled, shopFloorReleaseNow = currentShopRelease)
    }

    fun toggleEarlyClassLogout(enabled: Boolean) = toggleEarlyClassLogoutOverride(enabled)

    fun toggleShopFloorReleaseNowOverride(enabled: Boolean) {
        val current = _uiState.value
        if (current.isUpdatingScanOverride) return
        val currentEarlyClass = current.staffData?.scanOverrides?.earlyClassLogoutEnabled ?: false
        updateScanOverrides(earlyClassLogout = currentEarlyClass, shopFloorReleaseNow = enabled)
    }

    fun toggleShopFloorReleaseNow(enabled: Boolean) = toggleShopFloorReleaseNowOverride(enabled)

    private fun updateScanOverrides(earlyClassLogout: Boolean, shopFloorReleaseNow: Boolean) {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update {
                it.copy(isUpdatingScanOverride = true, scanOverrideMessage = null, scanOverrideError = null)
            }
            when (
                val result = apiClient.setScanOverride(
                    pin = current.activePin,
                    earlyClassLogoutEnabled = earlyClassLogout,
                    shopFloorReleaseNowEnabled = shopFloorReleaseNow
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isUpdatingScanOverride = false,
                            staffData = state.staffData?.copy(scanOverrides = result.data.state),
                            scanOverrideMessage = result.data.message,
                            scanOverrideError = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isUpdatingScanOverride = false, scanOverrideError = result.message)
                    }
                }
            }
        }
    }

    fun updateShopFloorMember(
        roll: String,
        operation: String,
        sessionType: String = "POST_3PM",
        scheduledEndTime: String = "05:00 PM"
    ) {
        val current = _uiState.value
        if (current.isUpdatingShopFloor) return
        val cleanRoll = roll.filter { it.isDigit() }
        val rollNum = cleanRoll.toIntOrNull()
        if (rollNum == null || rollNum !in 1..DutyRotationEngine.TOTAL_TRAINEES) {
            _uiState.update { it.copy(shopFloorActionError = "Enter a valid Roll Number (1–23).") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isUpdatingShopFloor = true, shopFloorActionMessage = null, shopFloorActionError = null)
            }
            when (
                val result = apiClient.setShopFloorMember(
                    pin = current.activePin,
                    roll = cleanRoll,
                    operation = operation,
                    sessionType = sessionType,
                    scheduledEndTime = scheduledEndTime
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingShopFloor = false,
                            shopFloorActionMessage = result.data.message,
                            shopFloorActionError = null
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isUpdatingShopFloor = false, shopFloorActionError = result.message)
                    }
                }
            }
        }
    }

    fun triggerShopFloorCardLogout(roll: String) {
        val current = _uiState.value
        if (current.isUpdatingShopFloor) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isUpdatingShopFloor = true, shopFloorActionMessage = null, shopFloorActionError = null)
            }
            when (val result = apiClient.triggerShopFloorCardLogout(roll)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingShopFloor = false,
                            shopFloorActionMessage = "${result.data.name} card logout recorded at ${result.data.time}.",
                            shopFloorActionError = null
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isUpdatingShopFloor = false, shopFloorActionError = result.message)
                    }
                }
            }
        }
    }

    fun recordShopFloorCardLogout(roll: String) = triggerShopFloorCardLogout(roll)

    fun verifyShopFloorLogout(roll: String, date: String) {
        val current = _uiState.value
        if (current.isUpdatingShopFloor) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isUpdatingShopFloor = true, shopFloorActionMessage = null, shopFloorActionError = null)
            }
            when (val result = apiClient.verifyShopFloorLogout(current.activePin, roll, date)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingShopFloor = false,
                            shopFloorActionMessage = result.data.message,
                            shopFloorActionError = null
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isUpdatingShopFloor = false, shopFloorActionError = result.message)
                    }
                }
            }
        }
    }

    // ---------- Bulk Morning Roll Call, Quick Exceptions & Undo ----------

    fun runBulkMorningRollCall(status: String = "PRESENT") {
        val current = _uiState.value
        if (current.isRunningRollCallOp) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isRunningRollCallOp = true, rollCallFeedbackMessage = null, rollCallError = null)
            }
            when (val result = apiClient.bulkMorningRollCall(current.activePin, status)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRunningRollCallOp = false,
                            rollCallFeedbackMessage = result.data.message,
                            rollCallError = null
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isRunningRollCallOp = false, rollCallError = result.message)
                    }
                }
            }
        }
    }

    fun bulkMorningRollCall() = runBulkMorningRollCall("PRESENT")

    fun bulkMorningRollCall(status: String) = runBulkMorningRollCall(status)

    fun markTraineeException(roll: String, exceptionType: String) {
        requestManualAttendancePunch(roll = roll, status = exceptionType, bypassDuplicateGuard = false)
    }

    fun markAttendanceException(roll: String, exceptionType: String) =
        markTraineeException(roll, exceptionType)

    fun requestManualAttendancePunch(
        roll: String,
        status: String,
        bypassDuplicateGuard: Boolean = false
    ) {
        val current = _uiState.value
        if (current.isRunningRollCallOp || current.manualPunchingRoll != null || current.activePin.isBlank()) return
        val cleanRoll = roll.trim()
        val normalizedStatus = status.trim().uppercase()
        val existing = current.staffData?.allAttendance?.firstOrNull {
            it.roll.equals(cleanRoll, ignoreCase = true)
        }

        if (!bypassDuplicateGuard && existing != null && existing.hasPunchedToday && normalizedStatus != "ABSENT") {
            _uiState.update {
                it.copy(
                    duplicatePunchPrompt = DuplicatePunchPrompt(
                        roll = existing.roll,
                        studentName = existing.name,
                        existingStatus = existing.todayStatus.ifBlank { "PRESENT" },
                        existingInTime = existing.inT.ifBlank { "-" },
                        requestedStatus = normalizedStatus
                    )
                )
            }
            return
        }

        executeManualAttendancePunch(
            roll = cleanRoll,
            studentName = existing?.name ?: "Roll #$cleanRoll",
            previousStatus = existing?.todayStatus?.ifBlank { "ABSENT" } ?: "ABSENT",
            previousInT = existing?.inT ?: "-",
            previousOutT = existing?.outT ?: "-",
            requestedStatus = normalizedStatus,
            allowDuplicateOverride = bypassDuplicateGuard
        )
    }

    fun manualAttendancePunch(roll: String, status: String) =
        requestManualAttendancePunch(roll, status, false)

    fun manualAttendancePunch(roll: String, status: String, bypassDuplicateGuard: Boolean) =
        requestManualAttendancePunch(roll, status, bypassDuplicateGuard)

    fun confirmDuplicateManualPunchOverride() {
        val prompt = _uiState.value.duplicatePunchPrompt ?: return
        _uiState.update { it.copy(duplicatePunchPrompt = null) }
        requestManualAttendancePunch(
            roll = prompt.roll,
            status = prompt.requestedStatus,
            bypassDuplicateGuard = true
        )
    }

    fun confirmDuplicatePunch() = confirmDuplicateManualPunchOverride()

    fun dismissDuplicatePunchPrompt() {
        _uiState.update { it.copy(duplicatePunchPrompt = null) }
    }

    fun dismissDuplicatePunch() = dismissDuplicatePunchPrompt()

    private fun executeManualAttendancePunch(
        roll: String,
        studentName: String,
        previousStatus: String,
        previousInT: String,
        previousOutT: String,
        requestedStatus: String,
        allowDuplicateOverride: Boolean
    ) {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRunningRollCallOp = true,
                    manualPunchingRoll = roll,
                    rollCallFeedbackMessage = null,
                    rollCallError = null
                )
            }
            val result = apiClient.submitManualAttendancePunch(
                pin = current.activePin,
                roll = roll,
                status = requestedStatus,
                allowDuplicateOverride = allowDuplicateOverride,
                note = if (allowDuplicateOverride) "Instructor override (by mistake)" else "Front-panel quick punch"
            )
            when (result) {
                is ApiResult.Success -> {
                    val timeLabel = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
                    }.format(java.util.Date())
                    val undoEntry = AttendanceUndoRecord(
                        roll = roll,
                        studentName = studentName,
                        previousStatus = previousStatus,
                        previousInT = previousInT,
                        previousOutT = previousOutT,
                        newStatus = requestedStatus,
                        changedAtLabel = timeLabel
                    )
                    _uiState.update { state ->
                        val updatedMap = state.recentUndoByRoll + (roll to undoEntry)
                        val optimisticStaff = state.staffData?.let { sd ->
                            sd.copy(
                                allAttendance = sd.allAttendance.map { rec ->
                                    if (rec.roll.equals(roll, ignoreCase = true)) {
                                        rec.copy(
                                            todayStatus = requestedStatus,
                                            inT = if (requestedStatus == "ABSENT") "-" else rec.inT.takeIf { it.isNotBlank() && it != "-" } ?: timeLabel
                                        )
                                    } else {
                                        rec
                                    }
                                }
                            )
                        }
                        state.copy(
                            isRunningRollCallOp = false,
                            manualPunchingRoll = null,
                            staffData = optimisticStaff,
                            recentUndoByRoll = updatedMap,
                            lastUndoRecord = undoEntry,
                            rollCallFeedbackMessage = result.data.message,
                            rollCallError = null
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isRunningRollCallOp = false,
                            manualPunchingRoll = null,
                            rollCallError = result.message
                        )
                    }
                }
            }
        }
    }

    fun undoRecentAttendanceChange(roll: String) {
        val current = _uiState.value
        if (current.undoingRoll != null || current.isRunningRollCallOp || current.activePin.isBlank()) return
        val undoRecord = current.recentUndoByRoll[roll] ?: current.lastUndoRecord?.takeIf { it.roll == roll } ?: return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    undoingRoll = roll,
                    isRunningRollCallOp = true,
                    rollCallFeedbackMessage = null,
                    rollCallError = null
                )
            }
            when (
                val result = apiClient.undoAttendanceAction(
                    pin = current.activePin,
                    roll = undoRecord.roll,
                    restoreStatus = undoRecord.previousStatus,
                    restoreInT = undoRecord.previousInT,
                    restoreOutT = undoRecord.previousOutT
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val remaining = state.recentUndoByRoll - undoRecord.roll
                        val optimisticStaff = state.staffData?.let { sd ->
                            sd.copy(
                                allAttendance = sd.allAttendance.map { rec ->
                                    if (rec.roll.equals(undoRecord.roll, ignoreCase = true)) {
                                        rec.copy(
                                            todayStatus = undoRecord.previousStatus,
                                            inT = undoRecord.previousInT,
                                            outT = undoRecord.previousOutT
                                        )
                                    } else {
                                        rec
                                    }
                                }
                            )
                        }
                        state.copy(
                            undoingRoll = null,
                            isRunningRollCallOp = false,
                            staffData = optimisticStaff,
                            recentUndoByRoll = remaining,
                            lastUndoRecord = if (state.lastUndoRecord?.roll == undoRecord.roll) {
                                remaining.values.lastOrNull()
                            } else {
                                state.lastUndoRecord
                            },
                            rollCallFeedbackMessage = "Undo complete: Roll #${undoRecord.roll} restored to ${undoRecord.previousStatus}.",
                            rollCallError = null
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            undoingRoll = null,
                            isRunningRollCallOp = false,
                            rollCallError = "Undo failed: ${result.message}"
                        )
                    }
                }
            }
        }
    }

    fun undoAttendanceAction(roll: String) = undoRecentAttendanceChange(roll)

    fun resetTraineeAttendanceToAbsent(roll: String, studentName: String) {
        val current = _uiState.value
        if (current.undoingRoll != null || current.isRunningRollCallOp || current.activePin.isBlank()) return
        val cleanRoll = roll.trim()
        if (cleanRoll.isBlank()) return
        val targetDate = current.staffData?.attendanceDate
            ?.takeIf { it.isNotBlank() }
            ?: AttendanceApiClient.todayIstDateString()

        viewModelScope.launch {
            _uiState.update { state ->
                val optimisticStaff = state.staffData?.let { sd ->
                    sd.copy(
                        allAttendance = sd.allAttendance.map { rec ->
                            if (rec.roll.trim().equals(cleanRoll, ignoreCase = true)) {
                                rec.copy(todayStatus = "ABSENT", inT = "-", outT = "-")
                            } else {
                                rec
                            }
                        }
                    )
                }
                state.copy(
                    undoingRoll = cleanRoll,
                    isRunningRollCallOp = true,
                    staffData = optimisticStaff,
                    rollCallFeedbackMessage = null,
                    rollCallError = null
                )
            }
            when (
                val result = apiClient.undoAttendanceAction(
                    pin = current.activePin,
                    roll = cleanRoll,
                    date = targetDate,
                    restoreStatus = "ABSENT",
                    restoreInT = "-",
                    restoreOutT = "-"
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val remaining = state.recentUndoByRoll - cleanRoll
                        state.copy(
                            undoingRoll = null,
                            isRunningRollCallOp = false,
                            recentUndoByRoll = remaining,
                            lastUndoRecord = if (state.lastUndoRecord?.roll == cleanRoll) {
                                remaining.values.lastOrNull()
                            } else {
                                state.lastUndoRecord
                            },
                            rollCallFeedbackMessage = "Reset $studentName (Roll #$cleanRoll) to ABSENT for today.",
                            rollCallError = null
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
             // ---------- Theory Notes & Lab Log Book Submission & Verification ----------

    fun submitTraineeAcademicRecord(
        theoryTopic: String,
        theoryStatus: String,
        theoryUpdatedDate: String,
        labJobName: String,
        labStatus: String,
        labSignedDate: String,
        remarks: String
    ) {
        val current = _uiState.value
        if (current.isSubmittingAcademicRecord || current.activeRoll.isBlank()) return
        if (theoryTopic.isBlank() && labJobName.isBlank()) {
            _uiState.update {
                it.copy(academicActionError = "Please enter at least a Theory Notes topic or Lab Log Book job/experiment.")
            }
            return
        }
        val studentName = current.traineeData?.name.orEmpty().ifBlank { "Roll #${current.activeRoll}" }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmittingAcademicRecord = true,
                    academicActionMessage = null,
                    academicActionError = null
                )
            }
            when (
                val result = apiClient.submitAcademicRecord(
                    roll = current.activeRoll,
                    pin = current.activePin,
                    studentName = studentName,
                    theoryTopic = theoryTopic,
                    theoryStatus = theoryStatus,
                    theoryUpdatedDate = theoryUpdatedDate,
                    labJobName = labJobName,
                    labStatus = labStatus,
                    labSignedDate = labSignedDate,
                    remarks = remarks
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAcademicRecord = false,
                            academicActionMessage = result.data.message,
                            academicActionError = null
                        )
                    }
                    refreshTraineeAcademicSubmission()
                    refreshTraineeDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingAcademicRecord = false,
                            academicActionError = result.message
                        )
                    }
                }
            }
        }
    }

    fun refreshTraineeAcademicSubmission() {
        val current = _uiState.value
        if (current.activeRoll.isBlank()) return
        viewModelScope.launch {
            when (val result = apiClient.fetchAcademicSubmissions(pin = current.activePin, roll = current.activeRoll)) {
                is ApiResult.Success -> {
                    val mine = result.data.firstOrNull()
                    if (mine != null) {
                        _uiState.update { state ->
                            state.copy(traineeData = state.traineeData?.copy(myAcademicSubmission = mine))
                        }
                    }
                }
                is ApiResult.Error -> Unit
            }
        }
    }

    fun refreshStaffAcademicSubmissions() {
        val current = _uiState.value
        if (current.activePin.isBlank()) return
        viewModelScope.launch {
            when (val result = apiClient.fetchAcademicSubmissions(pin = current.activePin)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val existing = state.staffData ?: return@update state
                        state.copy(staffData = existing.copy(academicSubmissions = result.data))
                    }
                }
                is ApiResult.Error -> Unit
            }
        }
    }

    fun refreshAcademicSubmissions() = refreshStaffAcademicSubmissions()

    fun verifyAcademicSubmission(
        roll: String,
        submissionId: String,
        instructorRemark: String
    ) {
        val current = _uiState.value
        if (current.isUpdatingAcademicSubmission || current.activePin.isBlank()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUpdatingAcademicSubmission = true,
                    academicActionMessage = null,
                    academicActionError = null
                )
            }
            when (
                val result = apiClient.verifyAcademicSubmission(
                    pin = current.activePin,
                    roll = roll,
                    submissionId = submissionId,
                    instructorRemark = instructorRemark,
                    verifiedBy = "Trade Instructor (EM)"
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingAcademicSubmission = false,
                            academicActionMessage = result.data.message,
                            academicActionError = null
                        )
                    }
                    refreshStaffAcademicSubmissions()
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingAcademicSubmission = false,
                            academicActionError = result.message
                        )
                    }
                }
            }
        }
    }

    fun forwardAcademicSubmissionToPrincipal(
        roll: String = "",
        bulkAllVerified: Boolean = false
    ) {
        val current = _uiState.value
        if (current.isUpdatingAcademicSubmission || current.activePin.isBlank()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUpdatingAcademicSubmission = true,
                    academicActionMessage = null,
                    academicActionError = null
                )
            }
            when (
                val result = apiClient.forwardAcademicSubmissionToPrincipal(
                    pin = current.activePin,
                    roll = roll,
                    bulkAllVerified = bulkAllVerified
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingAcademicSubmission = false,
                            academicActionMessage = result.data.message,
                            academicActionError = null
                        )
                    }
                    refreshStaffAcademicSubmissions()
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingAcademicSubmission = false,
                            academicActionError = result.message
                        )
                    }
                }
            }
        }
    }

    fun forwardSingleAcademicToPrincipal(roll: String) =
        forwardAcademicSubmissionToPrincipal(roll = roll, bulkAllVerified = false)

    fun forwardAllVerifiedAcademicToPrincipal() =
        forwardAcademicSubmissionToPrincipal(roll = "", bulkAllVerified = true)

    fun dismissAcademicFeedback() {
        _uiState.update { it.copy(academicActionMessage = null, academicActionError = null) }
    }

    fun dismissTraineeAcademicFeedback() = dismissAcademicFeedback()

    fun setTraineeLateGrace(
        roll: String,
        mode: String,
        cutoff: String = "",
        note: String = ""
    ) {
        val current = _uiState.value
        if (current.updatingLateGraceRoll != null || current.activePin.isBlank()) return
        val cleanRoll = roll.trim()
        if (cleanRoll.isBlank()) return
        val normalizedMode = mode.trim().uppercase()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    updatingLateGraceRoll = cleanRoll,
                    lateGraceMessage = null,
                    lateGraceError = null
                )
            }
            when (
                val result = apiClient.setLateGrace(
                    pin = current.activePin,
                    roll = cleanRoll,
                    mode = normalizedMode,
                    cutoff = cutoff,
                    note = note
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            updatingLateGraceRoll = null,
                            lateGraceMessage = result.data.message,
                            lateGraceError = null
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            updatingLateGraceRoll = null,
                            lateGraceError = result.message
                        )
                    }
                }
            }
        }
    }

    fun setLateGraceForTrainee(roll: String, mode: String, cutoff: String = "", note: String = "") =
        setTraineeLateGrace(roll, mode, cutoff, note)

    fun setLateGraceForTrainee(roll: String, mode: String) =
        setTraineeLateGrace(roll, mode)

    fun dismissLateGraceFeedback() {
        _uiState.update { it.copy(lateGraceMessage = null, lateGraceError = null) }
    }

    fun saveMonthlyExamMark(
        markId: String,
        roll: String,
        monthLabel: String,
        examDate: String,
        theoryMarks: Int,
        theoryMax: Int,
        labMarks: Int,
        labMax: Int,
        remarks: String,
        publish: Boolean,
        notifyParent: Boolean,
        onSuccessReset: () -> Unit
    ) {
        val current = _uiState.value
        if (current.isSavingMonthlyMark || current.activePin.isBlank()) return
        val cleanRoll = roll.filter { it.isDigit() }
        val rollNum = cleanRoll.toIntOrNull()
        if (rollNum == null || rollNum !in 1..DutyRotationEngine.TOTAL_TRAINEES) {
            _uiState.update { it.copy(monthlyMarkError = "Select a trainee Roll Number (1–23).") }
            return
        }
        if (monthLabel.isBlank()) {
            _uiState.update { it.copy(monthlyMarkError = "Enter a monthly exam label (e.g., Nov 2026).") }
            return
        }
        if (theoryMax <= 0 || labMax <= 0) {
            _uiState.update { it.copy(monthlyMarkError = "Max marks for Theory and Lab must be greater than 0.") }
            return
        }
        if (theoryMarks !in 0..theoryMax || labMarks !in 0..labMax) {
            _uiState.update { it.copy(monthlyMarkError = "Entered marks cannot exceed the max marks (Theory 0..$theoryMax, Lab 0..$labMax).") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSavingMonthlyMark = true, monthlyMarkMessage = null, monthlyMarkError = null)
            }
            when (
                val result = apiClient.saveMonthlyMark(
                    pin = current.activePin,
                    markId = markId,
                    roll = cleanRoll,
                    monthLabel = monthLabel,
                    examDate = examDate.ifBlank { AttendanceApiClient.todayIstDateString() },
                    theoryMarks = theoryMarks,
                    theoryMax = theoryMax,
                    labMarks = labMarks,
                    labMax = labMax,
                    remarks = remarks,
                    publish = publish,
                    notifyParent = notifyParent
                )
            ) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update {
                        it.copy(
                            isSavingMonthlyMark = false,
                            monthlyMarkMessage = result.data.message,
                            monthlyMarkError = null
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isSavingMonthlyMark = false, monthlyMarkError = result.message)
                    }
                }
            }
        }
    }

    fun saveMonthlyMark(
        markId: String,
        roll: String,
        monthLabel: String,
        examDate: String,
        theoryMarks: Int,
        theoryMax: Int,
        labMarks: Int,
        labMax: Int,
        remarks: String,
        publish: Boolean,
        notifyParent: Boolean,
        onSuccessReset: () -> Unit
    ) = saveMonthlyExamMark(
        markId, roll, monthLabel, examDate, theoryMarks, theoryMax,
        labMarks, labMax, remarks, publish, notifyParent, onSuccessReset
    )

    fun dismissMonthlyMarkFeedback() {
        _uiState.update { it.copy(monthlyMarkMessage = null, monthlyMarkError = null) }
    }

    fun replyToWorkMarkQuery(
        queryId: String,
        status: String,
        reply: String,
        onSuccessReset: () -> Unit = {}
    ) {
        val current = _uiState.value
        if (current.replyingWorkQueryId != null || current.activePin.isBlank()) return
        if (reply.isBlank()) {
            _uiState.update { it.copy(workQueryReplyError = "Please enter an instructor note or resolution reply.") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(replyingWorkQueryId = queryId, workQueryReplyMessage = null, workQueryReplyError = null)
            }
            when (val result = apiClient.replyWorkMarkQuery(current.activePin, queryId, status, reply)) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update {
                        it.copy(
                            replyingWorkQueryId = null,
                            workQueryReplyMessage = result.data.message,
                            workQueryReplyError = null
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(replyingWorkQueryId = null, workQueryReplyError = result.message)
                    }
                }
            }
        }
    }

    fun dismissWorkQueryReplyFeedback() {
        _uiState.update { it.copy(workQueryReplyMessage = null, workQueryReplyError = null) }
    }

    fun sendInstantParentAlert(roll: String, studentName: String, alertType: String, customMessage: String) {
        val current = _uiState.value
        if (current.isRunningRollCallOp) return
        val message = customMessage.ifBlank {
            if (alertType == ParentAlertTypes.LOW_ATTENDANCE_WARNING) {
                "Low Attendance Warning (<80%): $studentName (Roll #$roll) must attend Shop Floor makeup sessions to meet DGT exam eligibility."
            } else {
                "Absence Alert: $studentName (Roll #$roll) is marked ABSENT today at Govt. ITI Ranni (Electronics Mechanic)."
            }
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(isRunningRollCallOp = true, rollCallFeedbackMessage = null, rollCallError = null)
            }
            when (
                val result = apiClient.sendInstructorParentAlert(
                    pin = current.activePin,
                    roll = roll,
                    alertType = alertType,
                    message = message
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRunningRollCallOp = false,
                            rollCallFeedbackMessage = result.data.message,
                            rollCallError = null
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isRunningRollCallOp = false, rollCallError = result.message)
                    }
                }
            }
        }
    }

    // ---------- Duty Sign-Off & Tool Crib Equipment Log ----------

    fun signOffDailyDuty(date: String, pairLabel: String, note: String, onSuccessReset: () -> Unit) {
        val current = _uiState.value
        if (current.isUpdatingOps) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingOps = true, opsFeedbackMessage = null, opsError = null) }
            when (val result = apiClient.signOffDailyDuty(current.activePin, date, pairLabel, note)) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update { state ->
                        state.copy(
                            isUpdatingOps = false,
                            staffData = state.staffData?.copy(todayDutySignOff = result.data.record),
                            opsFeedbackMessage = result.data.message,
                            opsError = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isUpdatingOps = false, opsError = result.message) }
                }
            }
        }
    }

    fun signOffDuty(date: String, pairLabel: String, note: String, onSuccessReset: () -> Unit) =
        signOffDailyDuty(date, pairLabel, note, onSuccessReset)

    fun logToolCribIssue(
        itemName: String,
        conditionStatus: String,
        notes: String,
        onSuccessReset: () -> Unit
    ) {
        val current = _uiState.value
        if (current.isUpdatingOps) return
        if (itemName.isBlank()) {
            _uiState.update { it.copy(opsError = "Enter equipment or kit name (e.g., Digital Multimeter #4, Soldering Iron).") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingOps = true, opsFeedbackMessage = null, opsError = null) }
            when (
                val result = apiClient.logToolCribIssue(
                    pin = current.activePin,
                    itemName = itemName,
                    conditionStatus = conditionStatus,
                    notes = notes
                )
            ) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update { state ->
                        val updatedLogs = listOf(result.data.entry) + state.staffData?.toolCribLogs.orEmpty()
                        state.copy(
                            isUpdatingOps = false,
                            staffData = state.staffData?.copy(toolCribLogs = updatedLogs),
                            opsFeedbackMessage = result.data.message,
                            opsError = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isUpdatingOps = false, opsError = result.message) }
                }
            }
        }
    }

    fun logToolCribItem(itemName: String, conditionStatus: String, notes: String, onSuccessReset: () -> Unit) =
        logToolCribIssue(itemName, conditionStatus, notes, onSuccessReset)

    // ---------- Confirm & Close, Holidays, Notices, Project Groups, Approvals ----------

    fun confirmAndCloseTodayAttendance() {
        val current = _uiState.value
        if (current.isClosingAttendance) return
        val targetDate =
                    
