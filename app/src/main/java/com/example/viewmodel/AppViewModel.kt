package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApiResult
import com.example.data.AttendanceApiClient
import com.example.data.EspTapResult
import com.example.data.ParentAlertTypes
import com.example.data.ParentDashboardData
import com.example.data.PrincipalTab
import com.example.data.StaffDashboardData
import com.example.data.StaffTab
import com.example.data.TraineeDashboardData
import com.example.data.TraineeTab
import com.example.data.UserRole
import com.example.notifications.AttendanceNotificationHelper
import com.example.security.SessionPreferences
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

    // Parent dashboard state
    val parentData: ParentDashboardData? = null,
    val isRefreshingParent: Boolean = false,
    val parentBannerError: String? = null,
    val isSubmittingParentAlert: Boolean = false,
    val parentAlertSuccessMessage: String? = null,
    val parentAlertError: String? = null,
    val parentNotificationDispatched: Boolean = false,

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

    // Shop Floor & Instructor Scan Override state
    val isUpdatingScanOverride: Boolean = false,
    val scanOverrideMessage: String? = null,
    val scanOverrideError: String? = null,
    val isUpdatingShopFloor: Boolean = false,
    val shopFloorActionMessage: String? = null,
    val shopFloorActionError: String? = null,

    // Duty Sign-off & Tool Crib state
    val isUpdatingOps: Boolean = false,
    val opsFeedbackMessage: String? = null,
    val opsError: String? = null,

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
)

class AppViewModel(
    private val apiClient: AttendanceApiClient = AttendanceApiClient()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var sessionLoadedOnce = false

    // ---------- Session Persistence & Biometrics ----------

    fun loadSavedSessionIfNeeded(context: Context): Boolean {
        if (sessionLoadedOnce) return _uiState.value.hasSavedCredentials && _uiState.value.biometricEnabled
        sessionLoadedOnce = true
        val saved = SessionPreferences.load(context)
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

    // ---------- In-App Self-Updater ----------

    fun checkForAppUpdate(silentIfUpToDate: Boolean = false) {
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

    fun onRoleSelected(role: UserRole) {
        _uiState.update {
            it.copy(
                selectedRole = role,
                loginError = null
            )
        }
    }

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

        when (current.selectedRole) {
            UserRole.TRAINEE -> {
                val rollNum = current.rollInput.toIntOrNull()
                if (rollNum == null || rollNum !in 1..33) {
                    _uiState.update { it.copy(loginError = "Please enter a valid Roll Number between 1 and 33.") }
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
                if (rollNum == null || rollNum !in 1..33) {
                    _uiState.update { it.copy(loginError = "Please enter your ward's Roll Number (1–33).") }
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
    }

    private fun performTraineeLogin(roll: String, pin: String, context: Context?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, loginError = null, biometricMessage = null) }
            when (val result = apiClient.fetchTraineeData(roll = roll, pin = pin)) {
                is ApiResult.Success -> {
                    persistSessionAfterSuccess(context, UserRole.TRAINEE, roll, pin)
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
                            traineeBannerError = null,
                            leaveSubmitReqId = null,
                            leaveSubmitMessage = null,
                            leaveSubmitError = null,
                            projectGroupSaveMessage = null,
                            projectGroupSaveError = null
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
                            parentNotificationDispatched = false
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
            _uiState.update { it.copy(isLoggingIn = true, loginError = null, biometricMessage = null) }
            when (val result = apiClient.fetchStaffData(pin = pin)) {
                is ApiResult.Success -> {
                    persistSessionAfterSuccess(context, role, "", pin)
                    val targetScreen = if (role == UserRole.PRINCIPAL) {
                        SessionScreen.PrincipalDashboard
                    } else {
                        SessionScreen.StaffDashboard
                    }
                    _uiState.update { state ->
                        state.copy(
                            isLoggingIn = false,
                            screen = targetScreen,
                            activeStaffRole = role,
                            activePin = pin,
                            hasSavedCredentials = state.rememberMe,
                            staffData = result.data,
                            selectedStaffTab = StaffTab.ATTENDANCE_REGISTER,
                            selectedPrincipalTab = PrincipalTab.SCORECARD,
                            staffBannerError = null,
                            closeAttendanceMessage = null,
                            closeAttendanceError = null,
                            rollCallFeedbackMessage = null,
                            rollCallError = null,
                            updatingLateGraceRoll = null,
                            lateGraceMessage = null,
                            lateGraceError = null,
                            scanOverrideMessage = null,
                            scanOverrideError = null,
                            shopFloorActionMessage = null,
                            shopFloorActionError = null,
                            opsFeedbackMessage = null,
                            opsError = null,
                            holidayPostMessage = null,
                            holidayPostError = null,
                            noticePostMessage = null,
                            noticePostError = null,
                            projectAccessMessage = null,
                            projectAccessError = null,
                            approvalSuccessMessage = null,
                            approvalPdfUrl = null,
                            approvalError = null,
                            exportedPdfUrl = null,
                            exportPdfError = null,
                            lastEspTap = null,
                            espTapError = null
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

    // ---------- Trainee Operations ----------

    fun onTraineeTabSelected(tab: TraineeTab, context: Context) {
        _uiState.update { it.copy(selectedTraineeTab = tab) }
        if (tab == TraineeTab.NOTICES) {
            markNoticesSeen(context)
        } else if (tab == TraineeTab.PROJECT_GROUPS) {
            refreshProjectGroupsForTrainee()
        }
    }

    fun syncNoticeStateAndPopup(context: Context) {
        val notices = _uiState.value.traineeData?.notices.orEmpty()
        val lastSeen = AttendanceNotificationHelper.getLastSeenNoticeId(context)
        val unread = AttendanceNotificationHelper.countUnreadNotices(context, notices)
        _uiState.update {
            it.copy(unreadNoticeCount = unread, lastSeenNoticeId = lastSeen)
        }
        if (notices.isNotEmpty() && _uiState.value.selectedTraineeTab != TraineeTab.NOTICES) {
            AttendanceNotificationHelper.notifyNewNoticeIfNeeded(context, notices)
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
        val distinctMembers = memberRolls.distinct().filter { it in 1..33 }.sorted()
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
        val fired = AttendanceNotificationHelper.notifyParentIfConfirmedAndEligible(context, data)
        if (fired) {
            _uiState.update { it.copy(parentNotificationDispatched = true) }
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
    }

    fun onPrincipalTabSelected(tab: PrincipalTab) {
        _uiState.update { it.copy(selectedPrincipalTab = tab) }
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

    // ---------- Instructor Scan Overrides (Early Dismissal & Shop Floor Release) ----------

    fun toggleEarlyClassLogoutOverride(enabled: Boolean) {
        val current = _uiState.value
        if (current.isUpdatingScanOverride) return
        val currentShopRelease = current.staffData?.scanOverrides?.shopFloorReleaseNowEnabled ?: false
        updateScanOverrides(earlyClassLogout = enabled, shopFloorReleaseNow = currentShopRelease)
    }

    fun toggleShopFloorReleaseNowOverride(enabled: Boolean) {
        val current = _uiState.value
        if (current.isUpdatingScanOverride) return
        val currentEarlyClass = current.staffData?.scanOverrides?.earlyClassLogoutEnabled ?: false
        updateScanOverrides(earlyClassLogout = currentEarlyClass, shopFloorReleaseNow = enabled)
    }

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

    // ---------- Shop Floor Roster Management & Card Logout Verification ----------

    fun updateShopFloorMember(
        roll: String,
        operation: String, // "ADD" or "REMOVE"
        sessionType: String = "POST_3PM",
        scheduledEndTime: String = "05:00 PM"
    ) {
        val current = _uiState.value
        if (current.isUpdatingShopFloor) return
        val cleanRoll = roll.filter { it.isDigit() }
        if (cleanRoll.isBlank()) {
            _uiState.update { it.copy(shopFloorActionError = "Enter a valid Roll Number (1–33).") }
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

    // ---------- Bulk Morning Roll Call, Quick Exceptions & Instant Parent Alert ----------

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

    fun markTraineeException(roll: String, exceptionType: String) {
        val current = _uiState.value
        if (current.isRunningRollCallOp) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isRunningRollCallOp = true, rollCallFeedbackMessage = null, rollCallError = null)
            }
            when (val result = apiClient.markAttendanceException(current.activePin, roll, exceptionType)) {
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

    fun dismissLateGraceFeedback() {
        _uiState.update { it.copy(lateGraceMessage = null, lateGraceError = null) }
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

    // ---------- Confirm & Close, Holidays, Notices, Project Groups, Approvals ----------

    fun confirmAndCloseTodayAttendance() {
        val current = _uiState.value
        if (current.isClosingAttendance) return
        val targetDate = current.staffData?.attendanceDate
            ?.takeIf { it.isNotBlank() }
            ?: AttendanceApiClient.todayIstDateString()

        viewModelScope.launch {
            _uiState.update {
                it.copy(isClosingAttendance = true, closeAttendanceMessage = null, closeAttendanceError = null)
            }
            when (val result = apiClient.confirmCloseAttendance(pin = current.activePin, date = targetDate)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val updatedStaff = state.staffData?.copy(
                            attendanceClosed = result.data.attendanceClosed,
                            attendanceDate = result.data.date,
                            closedAt = result.data.closedAt
                        )
                        state.copy(
                            isClosingAttendance = false,
                            staffData = updatedStaff,
                            closeAttendanceMessage = result.data.message,
                            closeAttendanceError = null
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isClosingAttendance = false, closeAttendanceError = result.message)
                    }
                }
            }
        }
    }

    fun dismissCloseAttendanceFeedback() {
        _uiState.update { it.copy(closeAttendanceMessage = null, closeAttendanceError = null) }
    }

    fun postHolidayAnnouncement(date: String, reason: String, onSuccessReset: () -> Unit) {
        val current = _uiState.value
        if (current.isPostingHoliday) return
        val cleanDate = date.trim().ifBlank { AttendanceApiClient.todayIstDateString() }
        if (reason.isBlank()) {
            _uiState.update { it.copy(holidayPostError = "Enter a holiday reason (English, Manglish, or മലയാളം).") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isPostingHoliday = true, holidayPostMessage = null, holidayPostError = null)
            }
            when (val result = apiClient.postHoliday(pin = current.activePin, date = cleanDate, reason = reason)) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update { state ->
                        val updatedStaff = state.staffData?.let { sd ->
                            val isToday = result.data.holiday.date.equals(
                                sd.attendanceDate.ifBlank { AttendanceApiClient.todayIstDateString() },
                                ignoreCase = true
                            )
                            sd.copy(
                                todayHoliday = if (isToday) result.data.holiday else sd.todayHoliday,
                                holidays = listOf(result.data.holiday) +
                                    sd.holidays.filterNot { it.date == result.data.holiday.date }
                            )
                        }
                        state.copy(
                            isPostingHoliday = false,
                            staffData = updatedStaff,
                            holidayPostMessage = result.data.message,
                            holidayPostError = null
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isPostingHoliday = false, holidayPostError = result.message)
                    }
                }
            }
        }
    }

    fun postTradeNotice(
        title: String,
        category: String,
        body: String,
        pinned: Boolean = false,
        onSuccessReset: () -> Unit
    ) {
        val current = _uiState.value
        if (current.isPostingNotice) return
        if (title.isBlank()) {
            _uiState.update { it.copy(noticePostError = "Please enter a notice title.") }
            return
        }
        if (body.isBlank()) {
            _uiState.update { it.copy(noticePostError = "Please enter the notice message body.") }
            return
        }

        val author = if (current.activeStaffRole == UserRole.PRINCIPAL) {
            "Principal, Govt. ITI Ranni"
        } else {
            "Trade Instructor (EM)"
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isPostingNotice = true, noticePostMessage = null, noticePostError = null)
            }
            when (
                val result = apiClient.postNotice(
                    pin = current.activePin,
                    title = title,
                    category = category,
                    body = body,
                    pinned = pinned,
                    postedBy = author
                )
            ) {
                is ApiResult.Success -> {
                    onSuccessReset()
                    _uiState.update { state ->
                        val updatedStaff = state.staffData?.let { sd ->
                            sd.copy(
                                notices = listOf(result.data.notice) +
                                    sd.notices.filterNot { it.id == result.data.notice.id },
                                pinnedBroadcast = if (pinned) result.data.notice else sd.pinnedBroadcast
                            )
                        }
                        state.copy(
                            isPostingNotice = false,
                            staffData = updatedStaff,
                            noticePostMessage = result.data.message,
                            noticePostError = null
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isPostingNotice = false, noticePostError = result.message)
                    }
                }
            }
        }
    }

    fun toggleProjectGroupsAccess(enabled: Boolean) {
        val current = _uiState.value
        if (current.isTogglingProjectAccess) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isTogglingProjectAccess = true, projectAccessMessage = null, projectAccessError = null)
            }
            when (val result = apiClient.setProjectGroupsAccess(pin = current.activePin, enabled = enabled)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isTogglingProjectAccess = false,
                            staffData = state.staffData?.copy(projectGroupsEnabled = result.data.enabled),
                            projectAccessMessage = result.data.message,
                            projectAccessError = null
                        )
                    }
                    refreshProjectGroupsForStaff()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isTogglingProjectAccess = false, projectAccessError = result.message)
                    }
                }
            }
        }
    }

    fun refreshProjectGroupsForStaff() {
        val current = _uiState.value
        if (current.activePin.isBlank()) return
        viewModelScope.launch {
            when (val result = apiClient.fetchProjectGroups(pin = current.activePin)) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        val existing = state.staffData ?: return@update state
                        state.copy(
                            staffData = existing.copy(
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

    fun approveAndSignLeave(reqId: String, signature: String) {
        val current = _uiState.value
        if (current.signingReqId != null) return
        if (signature.isBlank()) {
            _uiState.update { it.copy(approvalError = "Please draw or type your digital signature before approving.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(signingReqId = reqId, approvalError = null, approvalSuccessMessage = null, approvalPdfUrl = null)
            }

            val result = if (current.activeStaffRole == UserRole.PRINCIPAL) {
                apiClient.signByPrincipal(pin = current.activePin, reqId = reqId, principalSig = signature)
            } else {
                apiClient.signByInstructor(pin = current.activePin, reqId = reqId, sirSig = signature)
            }

            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            signingReqId = null,
                            approvalSuccessMessage = result.data.message,
                            approvalPdfUrl = result.data.pdfUrl.ifBlank { null },
                            approvalError = null
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(signingReqId = null, approvalError = result.message)
                    }
                }
            }
        }
    }

    fun rejectLeaveRequest(reqId: String, reason: String) {
        val current = _uiState.value
        if (current.signingReqId != null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(signingReqId = reqId, approvalError = null, approvalSuccessMessage = null)
            }
            when (val result = apiClient.rejectLeaveRequest(current.activePin, reqId, reason.ifBlank { "Not approved by Principal" })) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(signingReqId = null, approvalSuccessMessage = result.data.message, approvalError = null)
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(signingReqId = null, approvalError = result.message)
                    }
                }
            }
        }
    }

    fun dismissApprovalFeedback() {
        _uiState.update {
            it.copy(approvalSuccessMessage = null, approvalPdfUrl = null, approvalError = null)
        }
    }

    fun onRegisterSearchChanged(query: String) {
        _uiState.update { it.copy(registerSearchQuery = query) }
    }

    fun onRegisterStatusFilterChanged(filter: String) {
        _uiState.update { it.copy(registerStatusFilter = filter) }
    }

    fun exportDailyAttendancePdf(onPdfReady: (String) -> Unit) {
        val current = _uiState.value
        if (current.isExportingPdf) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isExportingPdf = true, exportPdfError = null, exportedPdfUrl = null)
            }
            when (val result = apiClient.exportDailyAttendancePdf(current.activePin)) {
                is ApiResult.Success -> {
                    val url = result.data.pdfUrl
                    _uiState.update {
                        it.copy(
                            isExportingPdf = false,
                            exportedPdfUrl = url.ifBlank { null },
                            exportPdfError = if (url.isBlank()) "Server did not return a PDF link." else null
                        )
                    }
                    if (url.isNotBlank()) onPdfReady(url)
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isExportingPdf = false, exportPdfError = result.message)
                    }
                }
            }
        }
    }

    fun onEspRollInputChanged(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(2)
        _uiState.update { it.copy(espRollInput = digitsOnly, espTapError = null) }
    }

    fun triggerManualEspTap() {
        val current = _uiState.value
        if (current.isTappingEsp) return
        val rollNum = current.espRollInput.toIntOrNull()
        if (rollNum == null || rollNum !in 1..33) {
            _uiState.update { it.copy(espTapError = "Enter a valid trainee Roll Number (1–33).") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTappingEsp = true, espTapError = null) }
            when (val result = apiClient.triggerEspTap(rollNum.toString())) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isTappingEsp = false,
                            lastEspTap = result.data,
                            espTapHistory = listOf(result.data) + state.espTapHistory.take(9),
                            espTapError = null
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isTappingEsp = false, espTapError = result.message)
                    }
                }
            }
        }
    }

    fun logout() {
        val prev = _uiState.value
        _uiState.update {
            AppUiState(
                selectedRole = prev.selectedRole,
                rollInput = if (prev.rememberMe) prev.rollInput else "",
                pinInput = if (prev.rememberMe) prev.pinInput else "",
                rememberMe = prev.rememberMe,
                biometricEnabled = prev.biometricEnabled,
                hasSavedCredentials = prev.hasSavedCredentials,
                savedRoleLabel = prev.savedRoleLabel
            )
        }
    }
}
