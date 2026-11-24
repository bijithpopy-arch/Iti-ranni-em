package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.OfflineSyncQueue
import com.example.notifications.AttendanceNotificationHelper
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
import com.example.viewmodel.AppUiState
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.SessionScreen

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AttendanceNotificationHelper.ensureChannels(this)
        OfflineSyncQueue.getAll(this)

        setContent {
            ItiRanniTheme {
                AppRoot(
                    activity = this,
                    viewModel = viewModel
                )
            }
        }
    }
}

private object BiometricAuthHelper {
    fun isBiometricAvailable(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showFingerprintPrompt(
        activity: ComponentActivity,
        roleLabel: String,
        onAuthenticated: () -> Unit,
        onFallbackToPassword: (String?) -> Unit
    ) {
        val fragmentActivity = activity as? FragmentActivity
        if (fragmentActivity == null) {
            onFallbackToPassword(null)
            return
        }
        val executor = ContextCompat.getMainExecutor(fragmentActivity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onAuthenticated()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    onFallbackToPassword(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                // Allow retry until success or user cancellation
            }
        }
        val prompt = BiometricPrompt(fragmentActivity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock $roleLabel")
            .setSubtitle("Use fingerprint or biometric authentication")
            .setNegativeButtonText("Use PIN / Password")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()
        prompt.authenticate(info)
    }
}

private fun checkPostNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

@Composable
fun AppRoot(
    activity: ComponentActivity,
    viewModel: AppViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context: Context = LocalContext.current

    val notifState: MutableState<Boolean> = remember(context) {
        mutableStateOf(checkPostNotificationPermission(context))
    }
    var hasNotificationPermission: Boolean by notifState

    val notificationPermissionLauncher: ActivityResultLauncher<String> =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted: Boolean ->
            hasNotificationPermission = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            if (hasNotificationPermission) {
                AttendanceNotificationHelper.ensureChannels(context)
            }
        }

    LaunchedEffect(Unit) {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val biometricAvailable: Boolean = remember(context) {
        BiometricAuthHelper.isBiometricAvailable(context)
    }

    val triggerFingerprintUnlock: () -> Unit = remember(activity, viewModel) {
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

    LaunchedEffect(Unit) {
        val shouldAutoBiometric = viewModel.loadSavedSessionIfNeeded(context)
        if (biometricAvailable && shouldAutoBiometric) {
            triggerFingerprintUnlock()
        }
        val cfg = viewModel.uiState.value.remoteAppConfig
        if (cfg.updaterEnabled && cfg.releaseUpdateToUsers) {
            viewModel.checkForAppUpdate(silentIfUpToDate = true)
        }
    }

    val remoteAppConfig: RemoteAppConfig = uiState.remoteAppConfig
    val remoteUiConfig: RemoteUiConfig = uiState.remoteUiConfig
    val bugList = uiState.bugReports
    val packageVersionName: String = remember(context) {
        try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0L)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            info.versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }
    var showDeveloperPanel by remember { mutableStateOf(false) }
    var devTapCount by remember { mutableIntStateOf(0) }
    var showDevPinDialog by remember { mutableStateOf(false) }
    var devPinInput by remember { mutableStateOf("") }
    var devPinError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Notification permission is off. Enable alerts to hear custom tones.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        ) {
                            Text("Allow")
                        }
                    }
                }
            }
            if (remoteUiConfig.announcementBanner.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = remoteUiConfig.announcementBanner,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = remoteUiConfig.appTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            devTapCount++
                            if (devTapCount >= 5) {
                                devTapCount = 0
                                devPinInput = ""
                                devPinError = null
                                showDevPinDialog = true
                            }
                        }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportBugButtonAndDialog(
                        screenName = uiState.screen::class.simpleName ?: "Screen",
                        reportedBy = uiState.rollInput.ifBlank { uiState.selectedRole.displayName },
                        role = uiState.selectedRole.displayName,
                        appVersion = packageVersionName,
                        visible = remoteUiConfig.showReportBugButton && !remoteAppConfig.maintenanceMode,
                        onSubmitBug = { screenName, reportedBy, role, appVersion, description ->
                            viewModel.submitBugReport(
                                screenName = screenName,
                                reportedBy = reportedBy,
                                role = role,
                                appVersion = appVersion,
                                description = description
                            )
                        }
                    )

                    if (showDeveloperPanel) {
                        TextButton(onClick = { showDeveloperPanel = false }) {
                            Text("Close Dev")
                        }
                    }
                }
            }

            if (showDeveloperPanel) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    DeveloperMasterPanel(
                        appConfig = remoteAppConfig,
                        uiConfig = remoteUiConfig,
                        bugList = bugList,
                        onSaveAppConfig = viewModel::saveRemoteAppConfig,
                        onSaveUiConfig = viewModel::saveRemoteUiConfig,
                        onResolveBug = viewModel::resolveBugReport,
                        onEditAnySheetRow = viewModel::editSheetRow
                    )
                }
            }
            else if (remoteAppConfig.maintenanceMode && uiState.screen != SessionScreen.DeveloperDashboard) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFFD32F2F),
                        border = BorderStroke(2.dp, Color(0xFFB71C1C)),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 460.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                devTapCount++
                                if (devTapCount >= 5) {
                                    devTapCount = 0
                                    devPinInput = ""
                                    devPinError = null
                                    showDevPinDialog = true
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = "Maintenance Mode",
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "MAINTENANCE MODE",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = remoteAppConfig.maintenanceBanner.ifBlank {
                                    "App is currently under maintenance. Please check back shortly."
                                },
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    RenderSessionScreens(
                        uiState = uiState,
                        viewModel = viewModel,
                        biometricAvailable = biometricAvailable,
                        triggerFingerprintUnlock = triggerFingerprintUnlock
                    )
                }
            }
        }
    }

    if (showDevPinDialog) {
        AlertDialog(
            onDismissRequest = { showDevPinDialog = false },
            title = { Text("Enter Access Code") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = devPinInput,
                        onValueChange = {
                            devPinInput = it.filter { ch -> ch.isDigit() }.take(8)
                            devPinError = null
                        },
                        label = { Text("Code") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (devPinError != null) {
                        Text(
                            text = devPinError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (devPinInput.trim() == remoteAppConfig.developerPin) {
                        showDevPinDialog = false
                        showDeveloperPanel = true
                        devPinInput = ""
                        devPinError = null
                    } else {
                        devPinError = "Invalid code."
                    }
                }) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDevPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RenderSessionScreens(
    uiState: AppUiState,
    viewModel: AppViewModel,
    biometricAvailable: Boolean,
    triggerFingerprintUnlock: () -> Unit
) {
    val context: Context = LocalContext.current
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
                onRoleSelected = viewModel::onRoleInputChanged,
                onRollChanged = viewModel::onRollInputChanged,
                onPinChanged = viewModel::onPinInputChanged,
                onRememberMeChanged = viewModel::onRememberMeChanged,
                onBiometricEnabledChanged = viewModel::onBiometricEnabledChanged,
                onFingerprintUnlockClick = triggerFingerprintUnlock,
                onClearSavedLogin = { viewModel.clearSavedSession(context) },
                onCheckUpdateClick = {
                    val cfg = viewModel.uiState.value.remoteAppConfig
                    if (cfg.updaterEnabled) {
                        viewModel.checkForAppUpdate(silentIfUpToDate = false)
                    }
                },
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
                onDismissWorkQueryFeedback = viewModel::dismissTraineeWorkMarkQueryFeedback,
                onSubmitRunningLate = viewModel::submitTraineeRunningLateAlert,
                onDismissRunningLateFeedback = viewModel::dismissTraineeRunningLateAlertFeedback,
                onSubmitAcademicRecord = viewModel::submitTraineeAcademicRecord,
                onRefreshAcademicSubmission = viewModel::refreshTraineeAcademicSubmission,
                onDismissAcademicFeedback = viewModel::dismissTraineeAcademicFeedback,
                onRefreshTechNews = viewModel::refreshTechnicalNews,
                onSaveLocalTimetable = { periods -> viewModel.saveTraineeLocalTimetable(context, periods) },
                onResetTimetableToOfficial = { viewModel.resetTraineeTimetableToOfficial(context) },
                onSubmitSharedTimetable = { periods -> viewModel.submitTraineeSharedTimetable(context, periods) },
                onDismissTimetableFeedback = viewModel::dismissTimetableFeedback,
                onCheckUpdate = {
                    val cfg = viewModel.uiState.value.remoteAppConfig
                    if (cfg.updaterEnabled) {
                        viewModel.checkForAppUpdate(silentIfUpToDate = false)
                    }
                }
            )

            SessionScreen.ParentDashboard -> ParentDashboardScreen(
                uiState = uiState,
                onRefresh = viewModel::refreshParentDashboard,
                onLogout = viewModel::logout,
                onEvaluateNotification = { viewModel.evaluateParentNotification(context) },
                onSubmitBunkAlert = viewModel::submitParentBunkAlert,
                onSubmitLeaveReason = viewModel::submitParentLeaveReason,
                onDismissFeedback = viewModel::dismissParentAlertFeedback,
                onCheckUpdate = {
                    val cfg = viewModel.uiState.value.remoteAppConfig
                    if (cfg.updaterEnabled) {
                        viewModel.checkForAppUpdate(silentIfUpToDate = false)
                    }
                }
            )

            SessionScreen.StaffDashboard -> StaffDashboardScreen(
                uiState = uiState,
                onTabSelected = viewModel::onStaffTabSelected,
                onRefresh = viewModel::refreshStaffDashboard,
                onLogout = viewModel::logout,
                onCheckUpdate = {
                    val cfg = viewModel.uiState.value.remoteAppConfig
                    if (cfg.updaterEnabled) {
                        viewModel.checkForAppUpdate(silentIfUpToDate = false)
                    }
                },
                onApproveAndSign = viewModel::approveAndSignLeave,
                onRejectLeave = viewModel::rejectLeaveRequest,
                onDismissApprovalFeedback = viewModel::dismissApprovalFeedback,
                onRegisterSearchChanged = viewModel::onRegisterSearchChanged,
                onRegisterStatusFilterChanged = viewModel::onRegisterStatusFilterChanged,
                onExportPdf = { onPdfReady -> viewModel.exportAttendancePdf(onPdfReady) },
                onConfirmCloseAttendance = viewModel::confirmCloseAttendance,
                onDismissCloseFeedback = viewModel::dismissCloseAttendanceFeedback,
                onBulkMorningRollCall = { viewModel.bulkMorningRollCall() },
                onMarkException = viewModel::markAttendanceException,
                onManualPunch = { roll, status -> viewModel.manualAttendancePunch(roll, status) },
                onConfirmDuplicatePunch = viewModel::confirmDuplicatePunch,
                onDismissDuplicatePunch = viewModel::dismissDuplicatePunch,
                onUndoAttendance = viewModel::undoAttendanceAction,
                onResetAttendanceToAbsent = { roll, name -> viewModel.resetAttendanceToAbsent(roll, name) },
                onRefreshAcademicSubmissions = viewModel::refreshAcademicSubmissions,
                onVerifyAcademicSubmission = viewModel::verifyAcademicSubmission,
                onForwardSingleAcademicToPrincipal = viewModel::forwardSingleAcademicToPrincipal,
                onForwardAllAcademicToPrincipal = viewModel::forwardAllVerifiedAcademicToPrincipal,
                onSetLateGrace = { roll, mode, cutoff, note -> viewModel.setLateGraceForTrainee(roll, mode, cutoff, note) },
                onDismissLateGraceFeedback = viewModel::dismissLateGraceFeedback,
                onSendInstantParentAlert = viewModel::sendInstantParentAlert,
                onToggleEarlyClassLogout = viewModel::toggleEarlyClassLogout,
                onToggleShopFloorReleaseNow = viewModel::toggleShopFloorReleaseNow,
                onUpdateShopFloorMember = viewModel::updateShopFloorMember,
                onRecordShopFloorCardLogout = viewModel::recordShopFloorCardLogout,
                onVerifyShopFloorLogout = viewModel::verifyShopFloorLogout,
                onSignOffDuty = viewModel::signOffDuty,
                onLogToolCrib = viewModel::logToolCribItem,
                onSaveMonthlyMark = viewModel::saveMonthlyMark,
                onDismissMonthlyMarkFeedback = viewModel::dismissMonthlyMarkFeedback,
                onReplyWorkMarkQuery = viewModel::replyToWorkMarkQuery,
                onDismissWorkQueryReplyFeedback = viewModel::dismissWorkQueryReplyFeedback,
                onSaveDutyOverride = viewModel::saveDutyOverride,
                onDismissDutyOverrideFeedback = viewModel::dismissDutyOverrideFeedback,
                onPublishTimetable = { periods, allowTraineeEdit ->
                    viewModel.publishTimetable(context, periods, allowTraineeEdit)
                },
                onDismissTimetableFeedback = viewModel::dismissTimetableFeedback,
                onEvaluateInstructorNotifications = { viewModel.evaluateInstructorNotifications(context) },
                onPostNotice = { title, category, body, onReset ->
                    viewModel.postCampusNotice(title, category, body, onReset)
                },
                onPostHoliday = viewModel::postCampusHoliday,
                onToggleProjectGroupsAccess = viewModel::toggleProjectGroupsAccess,
                onRefreshProjectGroups = viewModel::refreshProjectGroupsForStaff,
                onEspRollChanged = viewModel::onEspRollChanged,
                onTriggerManualTap = viewModel::triggerManualEspTap
            )

            SessionScreen.PrincipalDashboard -> PrincipalDashboardScreen(
                uiState = uiState,
                onTabSelected = viewModel::onPrincipalTabSelected,
                onRefresh = viewModel::refreshPrincipalDashboard,
                onLogout = viewModel::logout,
                onCheckUpdate = {
                    val cfg = viewModel.uiState.value.remoteAppConfig
                    if (cfg.updaterEnabled) {
                        viewModel.checkForAppUpdate(silentIfUpToDate = false)
                    }
                },
                onExportPdf = { onPdfReady -> viewModel.exportAttendancePdf(onPdfReady) },
                onApproveAndSign = viewModel::approveAndSignLeave,
                onRejectLeave = viewModel::rejectLeaveRequest,
                onDismissApprovalFeedback = viewModel::dismissApprovalFeedback,
                onPublishPinnedBroadcast = { title, category, body, onReset ->
                    viewModel.postCampusNotice(title, category, body, pinned = true, onSuccessReset = onReset)
                },
                onResetAttendanceToAbsent = { roll, name -> viewModel.resetAttendanceToAbsent(roll, name) },
                onRefreshAcademicSubmissions = viewModel::refreshAcademicSubmissions
            )

            SessionScreen.DeveloperDashboard -> Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = viewModel::logout) {
                        Text("Exit Developer Console")
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    DeveloperMasterPanel(
                        appConfig = uiState.remoteAppConfig,
                        uiConfig = uiState.remoteUiConfig,
                        bugList = uiState.bugReports,
                        onSaveAppConfig = viewModel::saveRemoteAppConfig,
                        onSaveUiConfig = viewModel::saveRemoteUiConfig,
                        onResolveBug = viewModel::resolveBugReport,
                        onEditAnySheetRow = viewModel::editSheetRow
                    )
                }
            }
        }
    }
}
