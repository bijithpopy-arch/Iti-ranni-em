package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notifications.AttendanceNotificationHelper
import com.example.security.BiometricAuthHelper
import com.example.ui.components.AppUpdateDialog
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ParentDashboardScreen
import com.example.ui.screens.PrincipalDashboardScreen
import com.example.ui.screens.StaffDashboardScreen
import com.example.ui.screens.TraineeDashboardScreen
import com.example.ui.theme.ItiRanniTheme
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.SessionScreen

class MainActivity : FragmentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        AttendanceNotificationHelper.ensureChannels(this)
        handleIntentDeepLinks(intent)

        setContent {
            ItiRanniTheme {
                ItiAttendanceApp(
                    activity = this,
                    viewModel = appViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentDeepLinks(intent)
    }

    private fun handleIntentDeepLinks(intent: Intent?) {
        if (intent?.getBooleanExtra(AttendanceNotificationHelper.EXTRA_OPEN_NOTICES, false) == true) {
            appViewModel.handleOpenNoticesDeepLink()
            intent.removeExtra(AttendanceNotificationHelper.EXTRA_OPEN_NOTICES)
        }
    }
}

@Composable
fun ItiAttendanceApp(
    activity: FragmentActivity,
    viewModel: AppViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val biometricAvailable = remember { BiometricAuthHelper.isBiometricAvailable(context) }

    val triggerFingerprintUnlock = remember(activity, viewModel) {
        {
            val state = viewModel.uiState.value
            val label = state.savedRoleLabel.ifBlank { state.selectedRole.displayName }
            BiometricAuthHelper.showFingerprintPrompt(
                activity = activity,
                roleLabel = label,
                onAuthenticated = { viewModel.login(context) },
                onFallbackToPassword = { msg ->
                    if (!msg.isNullOrBlank()) {
                        viewModel.setBiometricMessage(msg)
                    }
                }
            )
        }
    }

    // Load Remember-Me session & trigger Fingerprint Unlock on app open + silent GitHub Releases update check
    LaunchedEffect(Unit) {
        val shouldAutoPromptBiometric = viewModel.loadSavedSessionIfNeeded(context)
        if (shouldAutoPromptBiometric && biometricAvailable) {
            triggerFingerprintUnlock()
        }
        viewModel.checkForAppUpdate(silentIfUpToDate = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            targetState = uiState.screen,
            animationSpec = tween(durationMillis = 240),
            label = "session_screen_transition",
            modifier = Modifier.fillMaxSize()
        ) { screen ->
            when (screen) {
                SessionScreen.Login -> LoginScreen(
                    uiState = uiState,
                    biometricAvailable = biometricAvailable,
                    onRoleSelected = viewModel::onRoleSelected,
                    onRollChanged = viewModel::onRollInputChanged,
                    onPinChanged = viewModel::onPinInputChanged,
                    onRememberMeChanged = viewModel::onRememberMeChanged,
                    onBiometricEnabledChanged = viewModel::onBiometricEnabledChanged,
                    onFingerprintUnlockClick = triggerFingerprintUnlock,
                    onClearSavedLogin = { viewModel.clearSavedSession(context) },
                    onCheckUpdateClick = { viewModel.checkForAppUpdate(silentIfUpToDate = false) },
                    onLoginClick = { viewModel.login(context) }
                )

                SessionScreen.TraineeDashboard -> TraineeDashboardScreen(
                    uiState = uiState,
                    onTabSelected = { tab -> viewModel.onTraineeTabSelected(tab, context) },
                    onSyncNotices = { viewModel.syncNoticeStateAndPopup(context) },
                    onMarkNoticesSeen = { viewModel.markNoticesSeen(context) },
                    onRefresh = viewModel::refreshTraineeDashboard,
                    onLogout = viewModel::logout,
                    onSubmitLeave = viewModel::submitLeaveRequest,
                    onDismissFeedback = viewModel::dismissLeaveFeedback,
                    onSaveProjectGroup = viewModel::saveProjectGroup,
                    onRefreshProjectGroups = viewModel::refreshProjectGroupsForTrainee,
                    onDismissProjectFeedback = viewModel::dismissProjectGroupFeedback,
                    onCheckUpdate = { viewModel.checkForAppUpdate(silentIfUpToDate = false) }
                )

                SessionScreen.ParentDashboard -> ParentDashboardScreen(
                    uiState = uiState,
                    onRefresh = viewModel::refreshParentDashboard,
                    onLogout = viewModel::logout,
                    onEvaluateNotification = { viewModel.evaluateParentNotification(context) },
                    onSubmitBunkAlert = viewModel::submitParentBunkAlert,
                    onSubmitLeaveReason = viewModel::submitParentLeaveReason,
                    onDismissFeedback = viewModel::dismissParentAlertFeedback,
                    onCheckUpdate = { viewModel.checkForAppUpdate(silentIfUpToDate = false) }
                )

                SessionScreen.StaffDashboard -> StaffDashboardScreen(
                    uiState = uiState,
                    onTabSelected = viewModel::onStaffTabSelected,
                    onRefresh = viewModel::refreshStaffDashboard,
                    onLogout = viewModel::logout,
                    onCheckUpdate = { viewModel.checkForAppUpdate(silentIfUpToDate = false) },
                    onApproveAndSign = viewModel::approveAndSignLeave,
                    onRejectLeave = viewModel::rejectLeaveRequest,
                    onDismissApprovalFeedback = viewModel::dismissApprovalFeedback,
                    onRegisterSearchChanged = viewModel::onRegisterSearchChanged,
                    onRegisterStatusFilterChanged = viewModel::onRegisterStatusFilterChanged,
                    onExportPdf = viewModel::exportDailyAttendancePdf,
                    onConfirmCloseAttendance = viewModel::confirmAndCloseTodayAttendance,
                    onDismissCloseFeedback = viewModel::dismissCloseAttendanceFeedback,
                    onBulkMorningRollCall = { viewModel.runBulkMorningRollCall("PRESENT") },
                    onMarkException = viewModel::markTraineeException,
                    onSendInstantParentAlert = viewModel::sendInstantParentAlert,
                    onToggleEarlyClassLogout = viewModel::toggleEarlyClassLogoutOverride,
                    onToggleShopFloorReleaseNow = viewModel::toggleShopFloorReleaseNowOverride,
                    onUpdateShopFloorMember = viewModel::updateShopFloorMember,
                    onRecordShopFloorCardLogout = viewModel::triggerShopFloorCardLogout,
                    onVerifyShopFloorLogout = viewModel::verifyShopFloorLogout,
                    onSignOffDuty = viewModel::signOffDailyDuty,
                    onLogToolCrib = viewModel::logToolCribIssue,
                    onPostNotice = { title, cat, body, onReset ->
                        viewModel.postTradeNotice(title, cat, body, pinned = false, onSuccessReset = onReset)
                    },
                    onPostHoliday = viewModel::postHolidayAnnouncement,
                    onToggleProjectGroupsAccess = viewModel::toggleProjectGroupsAccess,
                    onRefreshProjectGroups = viewModel::refreshProjectGroupsForStaff,
                    onEspRollChanged = viewModel::onEspRollInputChanged,
                    onTriggerManualTap = viewModel::triggerManualEspTap
                )

                SessionScreen.PrincipalDashboard -> PrincipalDashboardScreen(
                    uiState = uiState,
                    onTabSelected = viewModel::onPrincipalTabSelected,
                    onRefresh = viewModel::refreshStaffDashboard,
                    onLogout = viewModel::logout,
                    onCheckUpdate = { viewModel.checkForAppUpdate(silentIfUpToDate = false) },
                    onExportPdf = viewModel::exportDailyAttendancePdf,
                    onApproveAndSign = viewModel::approveAndSignLeave,
                    onRejectLeave = viewModel::rejectLeaveRequest,
                    onDismissApprovalFeedback = viewModel::dismissApprovalFeedback,
                    onPublishPinnedBroadcast = { title, cat, body, onReset ->
                        viewModel.postTradeNotice(title, cat, body, pinned = true, onSuccessReset = onReset)
                    }
                )
            }
        }

        // Global In-App Self-Updater Dialog
        AppUpdateDialog(
            uiState = uiState,
            onDownloadAndInstall = { viewModel.startApkUpdateDownload(context) },
            onDismiss = viewModel::dismissUpdateDialog
        )
    }
}
