package com.example

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notifications.AttendanceNotificationHelper
import com.example.notifications.BackgroundSyncScheduler
import com.example.security.BiometricAuthHelper
import com.example.ui.components.AppUpdateDialog
import com.example.ui.screens.DeveloperMasterPanel
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ParentDashboardScreen
import com.example.ui.screens.PrincipalDashboardScreen
import com.example.ui.screens.RemoteAppConfig
import com.example.ui.screens.RemoteUiConfig
import com.example.ui.screens.ReportBugButtonAndDialog
import com.example.ui.screens.StaffDashboardScreen
import com.example.ui.screens.TraineeDashboardScreen
import com.example.ui.theme.ItiRanniTheme
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.SessionScreen

class MainActivity : FragmentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            AttendanceNotificationHelper.ensureChannels(this)
            BackgroundSyncScheduler.ensureScheduled(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        AttendanceNotificationHelper.ensureChannels(this)
        BackgroundSyncScheduler.ensureScheduled(this)
        requestNotificationPermissionIfNeeded()
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

    override fun onStop() {
        super.onStop()
        BackgroundSyncScheduler.ensureScheduled(this)
        BackgroundSyncScheduler.scheduleQuickBackgroundCheck(this, delayMinutes = 2L)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentDeepLinks(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !AttendanceNotificationHelper.hasNotificationPermission(this)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun handleIntentDeepLinks(intent: Intent?) {
        if (intent?.getBooleanExtra(AttendanceNotificationHelper.EXTRA_OPEN_NOTICES, false) == true) {
            appViewModel.handleOpenNoticesDeepLink()
            intent.removeExtra(AttendanceNotificationHelper.EXTRA_OPEN_NOTICES)
        }
        if (intent?.getBooleanExtra(AttendanceNotificationHelper.EXTRA_FROM_BACKGROUND_ALERT, false) == true) {
            appViewModel.handleNotificationTapRefresh()
            intent.removeExtra(AttendanceNotificationHelper.EXTRA_FROM_BACKGROUND_ALERT)
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

        val remoteAppConfig: RemoteAppConfig = uiState.remoteAppConfig
    val remoteUiConfig: RemoteUiConfig = uiState.remoteUiConfig
    val bugList: List> = uiState.bugReports

    val packageVersionName = remember(context) {
        try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            "${info.versionName ?: "1.0"} (${androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(info)})"
        } catch (_: Exception) {
            "1.0"
        }
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
                    onSubmitWorkMarkQuery = viewModel::submitTraineeWorkMarkQuery,
                    onDismissWorkQueryFeedback = viewModel::dismissWorkQueryFeedback,
                    onSubmitRunningLate = viewModel::submitTraineeRunningLateAlert,
                    onDismissRunningLateFeedback = viewModel::dismissRunningLateFeedback,
                    onSubmitAcademicRecord = viewModel::submitTraineeAcademicRecord,
                    onRefreshAcademicSubmission = viewModel::refreshTraineeAcademicSubmission,
                    onDismissAcademicFeedback = viewModel::dismissAcademicFeedback,
                    onRefreshTechNews = viewModel::refreshTechnicalNews,
                    onSaveLocalTimetable = { periods -> viewModel.saveTraineeLocalTimetable(context, periods) },
                    onResetTimetableToOfficial = { viewModel.resetTraineeTimetableToOfficial(context) },
                    onSubmitSharedTimetable = { periods -> viewModel.submitTraineeSharedTimetable(context, periods) },
                    onDismissTimetableFeedback = viewModel::dismissTimetableFeedback,
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
                    onManualPunch = { roll, status -> viewModel.requestManualAttendancePunch(roll, status) },
                    onConfirmDuplicatePunch = viewModel::confirmDuplicateManualPunchOverride,
                    onDismissDuplicatePunch = viewModel::dismissDuplicatePunchPrompt,
                    onUndoAttendance = viewModel::undoRecentAttendanceChange,
                    onResetAttendanceToAbsent = viewModel::resetTraineeAttendanceToAbsent,
                    onRefreshAcademicSubmissions = viewModel::refreshStaffAcademicSubmissions,
                    onVerifyAcademicSubmission = viewModel::verifyAcademicSubmission,
                    onForwardSingleAcademicToPrincipal = { roll ->
                        viewModel.forwardAcademicSubmissionToPrincipal(roll = roll, bulkAllVerified = false)
                    },
                    onForwardAllAcademicToPrincipal = {
                        viewModel.forwardAcademicSubmissionToPrincipal(roll = "", bulkAllVerified = true)
                    },
                    onSetLateGrace = viewModel::setTraineeLateGrace,
                    onDismissLateGraceFeedback = viewModel::dismissLateGraceFeedback,
                    onSendInstantParentAlert = viewModel::sendInstantParentAlert,
                    onToggleEarlyClassLogout = viewModel::toggleEarlyClassLogoutOverride,
                    onToggleShopFloorReleaseNow = viewModel::toggleShopFloorReleaseNowOverride,
                    onUpdateShopFloorMember = viewModel::updateShopFloorMember,
                    onRecordShopFloorCardLogout = viewModel::triggerShopFloorCardLogout,
                    onVerifyShopFloorLogout = viewModel::verifyShopFloorLogout,
                    onSignOffDuty = viewModel::signOffDailyDuty,
                    onLogToolCrib = viewModel::logToolCribIssue,
                    onSaveMonthlyMark = viewModel::saveMonthlyExamMark,
                    onDismissMonthlyMarkFeedback = viewModel::dismissMonthlyMarkFeedback,
                    onReplyWorkMarkQuery = viewModel::replyToWorkMarkQuery,
                    onDismissWorkQueryReplyFeedback = viewModel::dismissWorkQueryReplyFeedback,
                    onSaveDutyOverride = viewModel::saveDutyOverride,
                    onDismissDutyOverrideFeedback = viewModel::dismissDutyOverrideFeedback,
                    onPublishTimetable = { periods, allow -> viewModel.publishOfficialTimetable(context, periods, allow) },
                    onDismissTimetableFeedback = viewModel::dismissTimetableFeedback,
                    onEvaluateInstructorNotifications = { viewModel.evaluateInstructorUrgentNotifications(context) },
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
                    },
                    onResetAttendanceToAbsent = viewModel::resetTraineeAttendanceToAbsent,
                    onRefreshAcademicSubmissions = viewModel::refreshStaffAcademicSubmissions
                )

                SessionScreen.DeveloperDashboard -> DeveloperMasterPanel(
                    appConfig = remoteAppConfig,
                    uiConfig = remoteUiConfig,
                    bugList = bugList,
                    onSaveAppConfig = { /* hook to viewModel.updateRemoteAppConfig(it) */ },
                    onSaveUiConfig = { updates, screenName -> /* hook to viewModel.saveRemoteUiConfig(updates, screenName) */ },
                    onResolveBug = { bugId, reply -> /* hook to viewModel.resolveBugReport(bugId, reply) */ },
                    onEditAnySheetRow = { tabName, rowIndex, colName, newValue ->
                        /* hook to viewModel.editSheetCell(tabName, rowIndex, colName, newValue) */
                    }
                )
            }
        }

        // Global Report Bug button on every screen
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 8.dp, end = 12.dp)
        ) {
            ReportBugButtonAndDialog(
                screenName = uiState.screen.name,
                reportedBy = uiState.selectedRole.displayName,
                role = uiState.selectedRole.name,
                appVersion = packageVersionName,
                visible = remoteUiConfig.showReportBugButton,
                onSubmitBug = { screenName, reportedBy, role, version, description ->
                    /* hook to viewModel.submitBugReport(screenName, reportedBy, role, version, description) */
                }
            )
        }

        // Global In-App Self-Updater Dialog
        AppUpdateDialog(
            uiState = uiState,
            onDownloadAndInstall = { viewModel.startApkUpdateDownload(context) },
            onDismiss = viewModel::dismissUpdateDialog
        )
    }
}
