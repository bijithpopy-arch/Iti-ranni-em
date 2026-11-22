package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApiResult
import com.example.data.AttendanceApiClient
import com.example.data.HolidayItem
import com.example.data.NoticeItem
import com.example.data.ParentDashboardData
import com.example.data.ProjectGroupItem
import com.example.data.StaffDashboardData
import com.example.data.TraineeDashboardData
import com.example.data.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val apiClient = AttendanceApiClient()

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val prefs by lazy {
        getApplication<Application>().getSharedPreferences("iti_ranni_session", Context.MODE_PRIVATE)
    }

    fun handleOpenNoticesDeepLink() {
        _uiState.update { it.copy(openNoticesPending = true) }
    }

    suspend fun loadSavedSessionIfNeeded(context: Context): Boolean {
        val savedRoleName = prefs.getString("saved_role", null) ?: return false
        val savedRole = runCatching { UserRole.valueOf(savedRoleName) }.getOrNull() ?: return false
        val savedRoll = prefs.getString("saved_roll", "").orEmpty()
        val savedPin = prefs.getString("saved_pin", "").orEmpty()
        val bioEnabled = prefs.getBoolean("biometric_enabled", false)

        _uiState.update {
            it.copy(
                selectedRole = savedRole,
                rollInput = savedRoll,
                pinInput = savedPin,
                rememberMe = true,
                biometricEnabled = bioEnabled,
                hasSavedCredentials = savedPin.isNotBlank(),
                savedRoleLabel = savedRole.displayName
            )
        }
        return bioEnabled && savedPin.isNotBlank()
    }

    fun clearSavedSession(context: Context) {
        prefs.edit().clear().apply()
        _uiState.update {
            it.copy(
                hasSavedCredentials = false,
                rememberMe = false,
                biometricEnabled = false,
                savedRoleLabel = "",
                pinInput = "",
                biometricMessage = null
            )
        }
    }

    fun setBiometricMessage(message: String) {
        _uiState.update { it.copy(biometricMessage = message) }
    }

    fun onRoleSelected(role: UserRole) {
        _uiState.update {
            it.copy(
                selectedRole = role,
                loginError = null,
                biometricMessage = null
            )
        }
    }

    fun onRollInputChanged(roll: String) {
        _uiState.update { it.copy(rollInput = roll, loginError = null) }
    }

    fun onPinInputChanged(pin: String) {
        _uiState.update { it.copy(pinInput = pin, loginError = null) }
    }

    fun onRememberMeChanged(remember: Boolean) {
        _uiState.update { it.copy(rememberMe = remember) }
    }

    fun onBiometricEnabledChanged(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        _uiState.update { it.copy(biometricEnabled = enabled) }
    }

    private fun persistSessionAfterSuccess(
        context: Context?,
        role: UserRole,
        roll: String,
        pin: String
    ) {
        val remember = _uiState.value.rememberMe
        val bio = _uiState.value.biometricEnabled
        if (remember) {
            prefs.edit()
                .putString("saved_role", role.name)
                .putString("saved_roll", roll)
                .putString("saved_pin", pin)
                .putBoolean("biometric_enabled", bio)
                .apply()
        }
        _uiState.update {
            it.copy(
                hasSavedCredentials = remember,
                savedRoleLabel = if (remember) role.displayName else it.savedRoleLabel
            )
        }
    }

    fun login(context: Context? = null) {
        val state = _uiState.value
        val role = state.selectedRole
        val roll = state.rollInput.trim()
        val pin = state.pinInput.trim()

        if (role != UserRole.STAFF && role != UserRole.PRINCIPAL && roll.isBlank()) {
            _uiState.update { it.copy(loginError = "Please enter roll number.") }
            return
        }
        if (pin.isBlank()) {
            _uiState.update { it.copy(loginError = "Please enter PIN.") }
            return
        }

        when (role) {
            UserRole.TRAINEE -> performTraineeLogin(roll, pin, context)
            UserRole.PARENT -> performParentLogin(roll, pin, context)
            UserRole.STAFF, UserRole.PRINCIPAL -> performStaffLogin(role, pin, context)
        }
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
                            noticePostError = null
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoggingIn = false, loginError = result.message) }
                }
            }
        }
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
        fun onTraineeTabSelected(tab: TraineeTab, context: Context? = null) {
        _uiState.update { it.copy(selectedTraineeTab = tab) }
        if (tab == TraineeTab.NOTICES && context != null) {
            markNoticesSeen(context)
        }
    }

    fun syncNoticeStateAndPopup(context: Context? = null) {
        refreshTraineeDashboard()
    }

    fun markNoticesSeen(context: Context? = null) {
        val currentNotices = _uiState.value.traineeData?.notices.orEmpty()
        val seenIds = currentNotices.map { it.id }.toSet()
        _uiState.update { it.copy(seenNoticeIds = it.seenNoticeIds + seenIds) }
    }

    fun refreshTraineeDashboard() {
        val state = _uiState.value
        if (state.activeRoll.isBlank() || state.activePin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, traineeBannerError = null) }
            when (val result = apiClient.fetchTraineeData(state.activeRoll, state.activePin)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isRefreshing = false, traineeData = result.data) }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isRefreshing = false, traineeBannerError = result.message) }
                }
            }
        }
    }

    fun submitLeaveRequest(type: String, timeWindow: String, reason: String, studentSig: String) {
        val state = _uiState.value
        if (state.activeRoll.isBlank() || state.activePin.isBlank()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmittingLeave = true,
                    leaveSubmitReqId = null,
                    leaveSubmitMessage = null,
                    leaveSubmitError = null
                )
            }
            when (val result = apiClient.submitLeave(state.activeRoll, state.activePin, type, timeWindow, reason, studentSig)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingLeave = false,
                            leaveSubmitReqId = result.data.reqId,
                            leaveSubmitMessage = result.data.message
                        )
                    }
                    refreshTraineeDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isSubmittingLeave = false, leaveSubmitError = result.message) }
                }
            }
        }
    }

    fun dismissLeaveFeedback() {
        _uiState.update {
            it.copy(leaveSubmitReqId = null, leaveSubmitMessage = null, leaveSubmitError = null)
        }
    }

    fun saveProjectGroup(projectTitle: String, memberRolls: List<Int>, leaderRoll: Int, leaderName: String) {
        val state = _uiState.value
        if (state.activeRoll.isBlank() || state.activePin.isBlank()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSavingProjectGroup = true,
                    projectGroupSaveMessage = null,
                    projectGroupSaveError = null
                )
            }
            when (val result = apiClient.saveProjectGroup(state.activeRoll, state.activePin, projectTitle, memberRolls, leaderRoll, leaderName)) {
                is ApiResult.Success -> {
                    _uiState.update { current ->
                        val updatedGroups = current.traineeData?.projectGroups.orEmpty() + result.data.group
                        val updatedTrainee = current.traineeData?.copy(projectGroups = updatedGroups)
                        current.copy(
                            isSavingProjectGroup = false,
                            traineeData = updatedTrainee,
                            projectGroupSaveMessage = result.data.message
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isSavingProjectGroup = false, projectGroupSaveError = result.message) }
                }
            }
        }
    }

    fun refreshProjectGroupsForTrainee() {
        val state = _uiState.value
        if (state.activePin.isBlank()) return
        viewModelScope.launch {
            when (val result = apiClient.fetchProjectGroups(state.activePin, state.activeRoll)) {
                is ApiResult.Success -> {
                    _uiState.update { current ->
                        val updated = current.traineeData?.copy(
                            projectGroupsEnabled = result.data.enabled,
                            projectGroups = result.data.groups
                        )
                        current.copy(traineeData = updated)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(projectGroupSaveError = result.message) }
                }
            }
        }
    }

    fun dismissProjectGroupFeedback() {
        _uiState.update { it.copy(projectGroupSaveMessage = null, projectGroupSaveError = null) }
    }

    fun refreshParentDashboard() {
        val state = _uiState.value
        if (state.activeRoll.isBlank() || state.activePin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, parentBannerError = null) }
            when (val result = apiClient.fetchParentData(state.activeRoll, state.activePin)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isRefreshing = false, parentData = result.data) }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isRefreshing = false, parentBannerError = result.message) }
                }
            }
        }
    }

    fun evaluateParentNotification(context: Context? = null) {
        _uiState.update { it.copy(parentNotificationDispatched = true) }
    }

    fun submitParentBunkAlert(message: String) {
        submitParentAlertInternal(alertType = "BUNK_ALERT", message = message)
    }

    fun submitParentLeaveReason(reason: String) {
        submitParentAlertInternal(alertType = "LEAVE_REASON", message = reason)
    }

    private fun submitParentAlertInternal(alertType: String, message: String) {
        val state = _uiState.value
        if (state.activeRoll.isBlank() || state.activePin.isBlank()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmittingParentAlert = true,
                    parentAlertSuccessMessage = null,
                    parentAlertError = null
                )
            }
            when (val result = apiClient.submitParentAlert(state.activeRoll, state.activePin, alertType, message)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingParentAlert = false,
                            parentAlertSuccessMessage = result.data.message
                        )
                    }
                    refreshParentDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isSubmittingParentAlert = false, parentAlertError = result.message) }
                }
            }
        }
    }

    fun dismissParentAlertFeedback() {
        _uiState.update { it.copy(parentAlertSuccessMessage = null, parentAlertError = null) }
    }

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
            _uiState.update { it.copy(isRefreshing = true, staffBannerError = null) }
            when (val result = apiClient.fetchStaffData(pin)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isRefreshing = false, staffData = result.data) }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isRefreshing = false, staffBannerError = result.message) }
                }
            }
        }
    }

    fun approveAndSignLeave(reqId: String, signature: String) {
        val state = _uiState.value
        if (state.activePin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isApprovalActionInProgress = true, approvalMessage = null, approvalError = null) }
            val result = if (state.activeStaffRole == UserRole.PRINCIPAL) {
                apiClient.signByPrincipal(state.activePin, reqId, signature)
            } else {
                apiClient.signByInstructor(state.activePin, reqId, signature)
            }
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isApprovalActionInProgress = false,
                            approvalMessage = result.data.message,
                            lastSignedPdfUrl = result.data.pdfUrl.ifBlank { null }
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isApprovalActionInProgress = false, approvalError = result.message) }
                }
            }
        }
    }

    fun rejectLeaveRequest(reqId: String, reason: String) {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isApprovalActionInProgress = true, approvalMessage = null, approvalError = null) }
            when (val result = apiClient.rejectLeaveRequest(pin, reqId, reason)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isApprovalActionInProgress = false, approvalMessage = result.data.message)
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isApprovalActionInProgress = false, approvalError = result.message) }
                }
            }
        }
    }

    fun dismissApprovalFeedback() {
        _uiState.update {
            it.copy(approvalMessage = null, approvalError = null, lastSignedPdfUrl = null)
        }
    }
        fun onRegisterSearchChanged(query: String) {
        _uiState.update { it.copy(registerSearchQuery = query) }
    }

    fun onRegisterStatusFilterChanged(filter: String) {
        _uiState.update { it.copy(registerStatusFilter = filter) }
    }

    fun exportDailyAttendancePdf() {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingPdf = true, exportedPdfUrl = null, opsError = null) }
            when (val result = apiClient.exportDailyAttendancePdf(pin)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isExportingPdf = false,
                            exportedPdfUrl = result.data.pdfUrl.ifBlank { null },
                            opsFeedbackMessage = "Daily PDF report ready."
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isExportingPdf = false, opsError = result.message) }
                }
            }
        }
    }

    fun confirmAndCloseTodayAttendance() {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isClosingAttendance = true,
                    closeAttendanceMessage = null,
                    closeAttendanceError = null
                )
            }
            when (val result = apiClient.confirmCloseAttendance(pin)) {
                is ApiResult.Success -> {
                    _uiState.update { current ->
                        val updatedStaff = current.staffData?.copy(
                            attendanceClosed = result.data.attendanceClosed,
                            closedAt = result.data.closedAt
                        )
                        current.copy(
                            isClosingAttendance = false,
                            staffData = updatedStaff,
                            closeAttendanceMessage = result.data.message
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isClosingAttendance = false, closeAttendanceError = result.message) }
                }
            }
        }
    }

    fun dismissCloseAttendanceFeedback() {
        _uiState.update { it.copy(closeAttendanceMessage = null, closeAttendanceError = null) }
    }

    fun runBulkMorningRollCall(status: String = "PRESENT") {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningRollCall = true, rollCallFeedbackMessage = null, rollCallError = null) }
            when (val result = apiClient.bulkMorningRollCall(pin, status)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isRunningRollCall = false, rollCallFeedbackMessage = result.data.message)
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isRunningRollCall = false, rollCallError = result.message) }
                }
            }
        }
    }

    fun markTraineeException(roll: String, exceptionType: String) {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningRollCall = true, rollCallFeedbackMessage = null, rollCallError = null) }
            when (val result = apiClient.markAttendanceException(pin, roll, exceptionType)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isRunningRollCall = false, rollCallFeedbackMessage = result.data.message)
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isRunningRollCall = false, rollCallError = result.message) }
                }
            }
        }
    }

    fun setTraineeLateGrace(roll: String, mode: String, cutoff: String = "09:30", note: String = "") {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningRollCall = true, rollCallFeedbackMessage = null, rollCallError = null) }
            when (val result = apiClient.setLateGrace(pin, roll, mode, cutoff, note)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isRunningRollCall = false, rollCallFeedbackMessage = result.data.message)
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isRunningRollCall = false, rollCallError = result.message) }
                }
            }
        }
    }

    fun sendInstantParentAlert(roll: String, alertType: String, message: String) {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingParentAlert = true, opsFeedbackMessage = null, opsError = null) }
            when (val result = apiClient.sendInstructorParentAlert(pin, roll, alertType, message)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isSendingParentAlert = false, opsFeedbackMessage = result.data.message)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isSendingParentAlert = false, opsError = result.message) }
                }
            }
        }
    }

    fun toggleEarlyClassLogoutOverride(enabled: Boolean) {
        val current = _uiState.value.staffData?.scanOverrides
        val releaseNow = current?.shopFloorReleaseNowEnabled ?: false
        updateScanOverrideInternal(enabled, releaseNow)
    }

    fun toggleShopFloorReleaseNowOverride(enabled: Boolean) {
        val current = _uiState.value.staffData?.scanOverrides
        val earlyLogout = current?.earlyClassLogoutEnabled ?: false
        updateScanOverrideInternal(earlyLogout, enabled)
    }

    private fun updateScanOverrideInternal(earlyLogout: Boolean, releaseNow: Boolean) {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingScanOverride = true, scanOverrideMessage = null, scanOverrideError = null) }
            when (val result = apiClient.setScanOverride(pin, earlyLogout, releaseNow)) {
                is ApiResult.Success -> {
                    _uiState.update { current ->
                        val updatedStaff = current.staffData?.copy(scanOverrides = result.data.state)
                        current.copy(
                            isUpdatingScanOverride = false,
                            staffData = updatedStaff,
                            scanOverrideMessage = result.data.message
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isUpdatingScanOverride = false, scanOverrideError = result.message) }
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
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingShopFloor = true, shopFloorActionMessage = null, shopFloorActionError = null) }
            when (val result = apiClient.setShopFloorMember(pin, roll, operation, sessionType, scheduledEndTime)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isUpdatingShopFloor = false, shopFloorActionMessage = result.data.message)
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isUpdatingShopFloor = false, shopFloorActionError = result.message) }
                }
            }
        }
    }

    fun triggerShopFloorCardLogout(roll: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingShopFloor = true, shopFloorActionMessage = null, shopFloorActionError = null) }
            when (val result = apiClient.triggerShopFloorCardLogout(roll)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingShopFloor = false,
                            shopFloorActionMessage = result.data.message.ifBlank {
                                "${result.data.name} (${result.data.mode}) @ ${result.data.time}"
                            }
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isUpdatingShopFloor = false, shopFloorActionError = result.message) }
                }
            }
        }
    }

    fun verifyShopFloorLogout(roll: String) {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingShopFloor = true, shopFloorActionMessage = null, shopFloorActionError = null) }
            when (val result = apiClient.verifyShopFloorLogout(pin, roll)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isUpdatingShopFloor = false, shopFloorActionMessage = result.data.message)
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isUpdatingShopFloor = false, shopFloorActionError = result.message) }
                }
            }
        }
    }

    fun signOffDailyDuty(date: String, pairLabel: String, note: String) {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningOffDuty = true, opsFeedbackMessage = null, opsError = null) }
            when (val result = apiClient.signOffDailyDuty(pin, date, pairLabel, note)) {
                is ApiResult.Success -> {
                    _uiState.update { current ->
                        val updatedStaff = current.staffData?.copy(todayDutySignOff = result.data.record)
                        current.copy(
                            isSigningOffDuty = false,
                            staffData = updatedStaff,
                            opsFeedbackMessage = result.data.message
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isSigningOffDuty = false, opsError = result.message) }
                }
            }
        }
    }

    fun logToolCribIssue(itemName: String, conditionStatus: String, notes: String) {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingToolCrib = true, opsFeedbackMessage = null, opsError = null) }
            when (val result = apiClient.logToolCribIssue(pin, itemName, conditionStatus, notes)) {
                is ApiResult.Success -> {
                    _uiState.update { current ->
                        val updatedLogs = listOf(result.data.entry) + current.staffData?.toolCribLogs.orEmpty()
                        val updatedStaff = current.staffData?.copy(toolCribLogs = updatedLogs)
                        current.copy(
                            isLoggingToolCrib = false,
                            staffData = updatedStaff,
                            opsFeedbackMessage = result.data.message
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoggingToolCrib = false, opsError = result.message) }
                }
            }
        }
    }

    fun postTradeNotice(
        title: String,
        category: String,
        body: String,
        pinned: Boolean = false,
        onSuccessReset: (() -> Unit)? = null
    ) {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPostingNotice = true, noticePostMessage = null, noticePostError = null) }
            when (val result = apiClient.postNotice(pin, title, category, body, pinned)) {
                is ApiResult.Success -> {
                    _uiState.update { current ->
                        val newNotices = listOf(result.data.notice) + current.staffData?.notices.orEmpty()
                        val newPinned = if (pinned) result.data.notice else current.staffData?.pinnedBroadcast
                        val updatedStaff = current.staffData?.copy(notices = newNotices, pinnedBroadcast = newPinned)
                        current.copy(
                            isPostingNotice = false,
                            staffData = updatedStaff,
                            noticePostMessage = result.data.message
                        )
                    }
                    onSuccessReset?.invoke()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isPostingNotice = false, noticePostError = result.message) }
                }
            }
        }
    }

    fun postHolidayAnnouncement(date: String, reason: String) {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPostingHoliday = true, holidayPostMessage = null, holidayPostError = null) }
            when (val result = apiClient.postHoliday(pin, date, reason)) {
                is ApiResult.Success -> {
                    _uiState.update { current ->
                        val updatedHolidays = current.staffData?.holidays.orEmpty() + result.data.holiday
                        val updatedStaff = current.staffData?.copy(holidays = updatedHolidays)
                        current.copy(
                            isPostingHoliday = false,
                            staffData = updatedStaff,
                            holidayPostMessage = result.data.message
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isPostingHoliday = false, holidayPostError = result.message) }
                }
            }
        }
    }

    fun toggleProjectGroupsAccess(enabled: Boolean) {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingProjectGroupsAccess = true, opsFeedbackMessage = null, opsError = null) }
            when (val result = apiClient.setProjectGroupsAccess(pin, enabled)) {
                is ApiResult.Success -> {
                    _uiState.update { current ->
                        val updatedStaff = current.staffData?.copy(projectGroupsEnabled = result.data.enabled)
                        current.copy(
                            isUpdatingProjectGroupsAccess = false,
                            staffData = updatedStaff,
                            opsFeedbackMessage = result.data.message
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isUpdatingProjectGroupsAccess = false, opsError = result.message) }
                }
            }
        }
    }

    fun refreshProjectGroupsForStaff() {
        val pin = _uiState.value.activePin
        if (pin.isBlank()) return
        viewModelScope.launch {
            when (val result = apiClient.fetchProjectGroups(pin)) {
                is ApiResult.Success -> {
                    _uiState.update { current ->
                        val updatedStaff = current.staffData?.copy(
                            projectGroupsEnabled = result.data.enabled,
                            projectGroups = result.data.groups
                        )
                        current.copy(staffData = updatedStaff)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(opsError = result.message) }
                }
            }
        }
    }

    fun onEspRollInputChanged(roll: String) {
        _uiState.update { it.copy(espRollInput = roll, espTapError = null) }
    }

    fun triggerManualEspTap() {
        val roll = _uiState.value.espRollInput.trim()
        if (roll.isBlank()) {
            _uiState.update { it.copy(espTapError = "Enter a roll number first.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isTriggeringEspTap = true, espTapMessage = null, espTapError = null) }
            when (val result = apiClient.triggerEspTap(roll)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isTriggeringEspTap = false,
                            espTapMessage = result.data.message.ifBlank {
                                "${result.data.name} (${result.data.mode}) @ ${result.data.time}"
                            }
                        )
                    }
                    refreshStaffDashboard()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isTriggeringEspTap = false, espTapError = result.message) }
                }
            }
        }
    }

    fun checkForAppUpdate(silentIfUpToDate: Boolean = false) {
        if (!silentIfUpToDate) {
            _uiState.update { it.copy(showUpdateDialog = true) }
        }
    }

    fun startApkUpdateDownload(context: Context? = null) {
        val apkUrl = _uiState.value.updateApkUrl.orEmpty().ifBlank { AttendanceApiClient.BASE_URL }
        val targetContext = context ?: getApplication<Application>()
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            targetContext.startActivity(intent)
        }
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }
}
