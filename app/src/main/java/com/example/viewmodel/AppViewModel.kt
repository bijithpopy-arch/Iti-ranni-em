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
        _uiState.update { it.copy(selectedRole = role, loginError = null) }
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
                    _uiState.update { it.copy(isLoggingIn = false, loginError = result.message) }
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
                    _uiState.update { it.copy(isLoggingIn = false, loginError = result.message) }
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
                           
