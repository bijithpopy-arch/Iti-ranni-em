package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.updater.AppReleaseInfo
import android.os.Bundle
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : FragmentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, ex ->
            try {
                val sw = StringWriter()
                ex.printStackTrace(PrintWriter(sw))
                val outDir = getExternalFilesDir(null) ?: filesDir
                File(outDir, "crash_trace.txt").writeText("Thread: ${t.name}\n$sw")
            } catch (_: Exception) {}
            prevHandler?.uncaughtException(t, ex)
        }
        try { enableEdgeToEdge() } catch (_: Exception) {}
        try { AttendanceNotificationHelper.ensureChannels(this) } catch (_: Exception) {}
        try { OfflineSyncQueue.getAll(this) } catch (_: Exception) {}

        setContent {
        val appCtx = androidx.compose.ui.platform.LocalContext.current
        androidx.compose.runtime.LaunchedEffect(Unit) { com.example.ui.screens.InaugurationController.load(appCtx) }
        var rootDevOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
            ItiRanniTheme {
                AppRoot(activity = this@MainActivity, viewModel = viewModel)
            }
            if (com.example.ui.screens.InaugurationController.isAppLocked()) {
                com.example.ui.screens.InaugurationLockGate { rootDevOpen = true }
            }
            if (rootDevOpen) {
                com.example.ui.screens.DevInaugurationConsoleDialog { rootDevOpen = false }
            }
            com.example.ui.screens.InaugurationFireworksOverlay()
        }
    }
}

private object BiometricAuthHelper {
    fun isBiometricAvailable(context: Context): Boolean = try {
        val manager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    } catch (_: Exception) { false }

    fun showFingerprintPrompt(
        activity: FragmentActivity,
        roleLabel: String,
        onAuthenticated: () -> Unit,
        onFallbackToPassword: (String?) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onAuthenticated()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    onFallbackToPassword(errString.toString())
                }
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
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
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true
}

@Composable
fun AppRoot(
    activity: FragmentActivity,
    viewModel: AppViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context: Context = LocalContext.current

    var hasNotificationPermission by remember(context) {
        mutableStateOf(checkPostNotificationPermission(context))
    }

    val notificationPermissionLauncher: ActivityResultLauncher<String> =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasNotificationPermission = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            if (hasNotificationPermission) AttendanceNotificationHelper.ensureChannels(context)
        }

    LaunchedEffect(Unit) {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // REMOVED_CRASH_LINE // notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val biometricAvailable = remember(context) { BiometricAuthHelper.isBiometricAvailable(context) }

    val triggerFingerprintUnlock: () -> Unit = remember(activity, viewModel) {
        {
            val state = viewModel.uiState.value
            val label = state.savedRoleLabel.ifBlank { state.selectedRole.displayName }
            BiometricAuthHelper.showFingerprintPrompt(
                activity = activity,
                roleLabel = label,
                onAuthenticated = { viewModel.login(context) },
                onFallbackToPassword = { msg -> if (!msg.isNullOrBlank()) viewModel.setBiometricMessage(msg) }
            )
        }
    }

    LaunchedEffect(Unit) {
        try {
            viewModel.loadSavedSessionIfNeeded(context)
            val savedSession = viewModel.uiState.value
            if (biometricAvailable && savedSession.biometricEnabled && savedSession.hasSavedCredentials) triggerFingerprintUnlock()
            val cfg = viewModel.uiState.value.remoteAppConfig
            if (cfg.updaterEnabled && cfg.releaseUpdateToUsers) viewModel.checkForAppUpdate(silentIfUpToDate = true)
        } catch (_: Exception) {}
        val savedSession = viewModel.uiState.value
        if (biometricAvailable && savedSession.biometricEnabled && savedSession.hasSavedCredentials) triggerFingerprintUnlock()
    }

    val remoteAppConfig: RemoteAppConfig = uiState.remoteAppConfig
    val remoteUiConfig: RemoteUiConfig = uiState.remoteUiConfig
    val bugList = uiState.bugReports
    val packageVersionName = BuildConfig.VERSION_NAME

    // Hidden Developer Mode unlock state (secret 5-tap + 9037 PIN dialog — no visible Dev button)
    var showDeveloperPanel by remember { mutableStateOf(false) }
    var secretTapCount by remember { mutableIntStateOf(0) }
    var showDevPinDialog by remember { mutableStateOf(false) }
    var devPinInput by remember { mutableStateOf("") }
    var devPinError by remember { mutableStateOf<String?>(null) }

    if (showDevPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showDevPinDialog = false
                devPinInput = ""
                devPinError = null
            },
            title = { Text("Developer Access", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = devPinInput,
                        onValueChange = { devPinInput = it.filter(Char::isDigit).take(6); devPinError = null },
                        label = { Text("Enter Secret Code") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth().testTag("dev_secret_pin_input")
                    )
                    if (devPinError != null) {
                        Text(devPinError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (devPinInput.trim() == remoteAppConfig.developerPin) {
                            showDeveloperPanel = true
                            showDevPinDialog = false
                            devPinInput = ""
                            devPinError = null
                        } else {
                            devPinError = "Invalid Developer Code."
                        }
                    },
                    modifier = Modifier.testTag("dev_secret_unlock_button")
                ) { Text("Unlock") }
            },
            dismissButton = {
                TextButton(onClick = { showDevPinDialog = false; devPinInput = ""; devPinError = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showUpdateDialog) {
        val upd = uiState.availableUpdate
        AlertDialog(
            onDismissRequest = viewModel::dismissUpdateDialog,
            title = {
                Text(if (upd != null) "Update v${(upd as? AppReleaseInfo)?.versionName.orEmpty()} Available" else "App Update Status", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = if (upd != null) upd.notes.ifBlank { "A newer release build is available on GitHub Releases." }
                    else uiState.updateStatusMessage ?: "You are running the latest version.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                if (upd != null) {
                    Button(onClick = { viewModel.openUpdateReleasePage(context); viewModel.dismissUpdateDialog() }) {
                        Text("Download Update")
                    }
                } else {
                    TextButton(onClick = viewModel::dismissUpdateDialog) { Text("OK") }
                }
            },
            dismissButton = {
                if (upd != null) TextButton(onClick = viewModel::dismissUpdateDialog) { Text("Later") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (remoteUiConfig.announcementBanner.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
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
                                if (showDeveloperPanel) {
                                    showDeveloperPanel = false
                                } else {
                                    secretTapCount++
                                    if (secretTapCount >= 5) {
                                        secretTapCount = 0
                                        showDevPinDialog = true
                                    }
                                }
                            }
                        )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportBugButtonAndDialog(
                        screenName = uiState.screen::class.simpleName ?: "Screen",
                        reportedBy = uiState.rollInput.ifBlank { uiState.selectedRole.displayName },
                        role = uiState.selectedRole.displayName,
                        appVersion = packageVersionName,
                        visible = remoteUiConfig.showReportBugButton,
                        onSubmitBug = { sName, by, r, ver, desc -> viewModel.submitBugReport(sName, by, r, ver, desc) }
                    )
                    if (showDeveloperPanel) {
                        TextButton(onClick = { showDeveloperPanel = false }) {
                            Text("Close Dev")
                        }
                    }
                }
            }

            if (showDeveloperPanel) {
                Box(modifier = Modifier.weight(1f)) {
                    DeveloperMasterPanel(
                        appConfig = remoteAppConfig,
                        uiConfig = remoteUiConfig,
                        bugList = bugList,
                        onSaveAppConfig = viewModel::saveRemoteAppConfig,
                        onSaveUiConfig = viewModel::saveRemoteUiConfig,
                        onResolveBug = viewModel::resolveBugReport,
                        onEditAnySheetRow = { tab, row, col, valStr -> viewModel.editSheetRow(tab, row, col, valStr) }
                    )
                }
            } else if (remoteAppConfig.maintenanceMode && uiState.screen != SessionScreen.DeveloperDashboard) {
                // Big Red Center Maintenance Block
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF140707))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color(0xFFD32F2F),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(3.dp, Color(0xFFFFCDD2)),
                        shadowElevation = 14.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 460.dp)
                            .testTag("maintenance_center_red_block")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = "Maintenance Lock",
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "MAINTENANCE MODE",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    secretTapCount++
                                    if (secretTapCount >= 5) {
                                        secretTapCount = 0
                                        showDevPinDialog = true
                                    }
                                }
                            )
                            Text(
                                text = remoteAppConfig.maintenanceBanner.ifBlank {
                                    "The app is currently under scheduled maintenance. Please check back shortly."
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                    }
                Box(modifier = Modifier.weight(1f)) {
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
                    if (viewModel.uiState.value.remoteAppConfig.updaterEnabled) {
                        viewModel.checkForAppUpdate(silentIfUpToDate = false)
                    }
                },
                onLoginClick = { viewModel.login(context) }
            )

            SessionScreen.TraineeDashboard -> {
                val isOwnerDev = uiState.rollInput.trim() == "12" || uiState.rollInput.trim().equals("BIJITH S BINU", ignoreCase = true)
                Column(modifier = Modifier.fillMaxSize()) {
                    var showFullConsole by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    if (isOwnerDev) {
                        var devExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                        var editTitle by androidx.compose.runtime.remember(uiState.remoteUiConfig.appTitle) { androidx.compose.runtime.mutableStateOf<String>(uiState.remoteUiConfig.appTitle) }
                        var editAnnouncement by androidx.compose.runtime.remember(uiState.remoteUiConfig.announcementBanner) { androidx.compose.runtime.mutableStateOf<String>(uiState.remoteUiConfig.announcementBanner) }
                        var editMaintBanner by androidx.compose.runtime.remember(uiState.remoteAppConfig.maintenanceBanner) { androidx.compose.runtime.mutableStateOf<String>(uiState.remoteAppConfig.maintenanceBanner) }
                        var editPin by androidx.compose.runtime.remember(uiState.remoteAppConfig.developerPin) { androidx.compose.runtime.mutableStateOf<String>(uiState.remoteAppConfig.developerPin) }
                        androidx.compose.material3.Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("DEV MODE // ROLL 12 (BIJITH S BINU)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = {
                                        com.example.ui.screens.InaugurationController.triggerFireworks()
                                    }) {
                                        Text("Fireworks", maxLines = 1, softWrap = false)
                                    }
                                    TextButton(onClick = { showFullConsole = !showFullConsole }) {
                                        Text(if (showFullConsole) "Close Master" else "Full Console", maxLines = 1, softWrap = false)
                                    }
                                    TextButton(onClick = { devExpanded = !devExpanded }) {
                                        Text(if (devExpanded) "Hide" else "Quick Edit", maxLines = 1, softWrap = false)
                                    }
                                }
                                if (devExpanded) {
                                    OutlinedTextField(
                                        value = editTitle,
                                        onValueChange = { editTitle = it },
                                        label = { Text("App Title (Live)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = editAnnouncement,
                                        onValueChange = { editAnnouncement = it },
                                        label = { Text("Announcement Banner (All Users)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = editMaintBanner,
                                        onValueChange = { editMaintBanner = it },
                                        label = { Text("Maintenance Banner") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = editPin,
                                        onValueChange = { editPin = it.filter(Char::isDigit).take(6) },
                                        label = { Text("Dev PIN") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                viewModel.saveRemoteUiConfig(
                                                    mapOf(
                                                        "appTitle" to editTitle.trim(),
                                                        "announcementBanner" to editAnnouncement.trim()
                                                    )
                                                    ,
                                                    "global"
                                                )
                                                viewModel.saveRemoteAppConfig(
                                                    mapOf(
                                                        "developerPin" to editPin.trim().ifBlank { "9037" },
                                                        "maintenanceBanner" to editMaintBanner.trim()
                                                    )
                                                )
                                            }
                                        ) {
                                            Text("Publish Live")
                                        }
                                        OutlinedButton(
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                val nextLock = (!uiState.remoteAppConfig.maintenanceMode).toString()
                                                viewModel.saveRemoteAppConfig(
                                                    mapOf(
                                                        "maintenanceMode" to nextLock,
                                                        "developerPin" to editPin.trim().ifBlank { "9037" },
                                                        "maintenanceBanner" to editMaintBanner.trim()
                                                    )
                                                )
                                            }
                                        ) {
                                            Text(if (uiState.remoteAppConfig.maintenanceMode) "Unlock App" else "Lock App")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (showFullConsole) {
                            DeveloperMasterPanel(
                                appConfig = uiState.remoteAppConfig,
                                uiConfig = uiState.remoteUiConfig,
                                bugList = uiState.bugReports,
                                onSaveAppConfig = viewModel::saveRemoteAppConfig,
                                onSaveUiConfig = viewModel::saveRemoteUiConfig,
                                onResolveBug = viewModel::resolveBugReport,
                                onEditAnySheetRow = { tab, row, col, valStr -> viewModel.editSheetRow(tab, row, col, valStr) }
                            )
                        } else {
                        TraineeDashboardScreen(
                uiState = uiState,
                onTabSelected = { tab -> viewModel.onTraineeTabSelected(tab, context) },
                onSyncNotices = { viewModel.syncNoticeStateAndPopup(context) },
                onMarkNoticesSeen = { viewModel.markNoticesSeen(context) },
                onRefresh = viewModel::refreshTraineeDashboard,
                            onLogout = viewModel::logout,
                onSubmitLeave = { type, window, reason, sig, onReset -> viewModel.submitLeaveRequest(type, window, reason, sig, onReset) },
                onDismissFeedback = viewModel::dismissLeaveFeedback,
                onSaveProjectGroup = { title, members, leaderRoll, leaderName, onReset -> viewModel.saveProjectGroup(title, members, leaderRoll, leaderName, onReset) },
                onRefreshProjectGroups = viewModel::refreshProjectGroupsForTrainee,
                onDismissProjectFeedback = viewModel::dismissProjectGroupFeedback,
                onSubmitWorkMarkQuery = { m, w, c, t, onReset -> viewModel.submitTraineeWorkMarkQuery(m, w, c, t, onReset) },
                onDismissWorkQueryFeedback = viewModel::dismissTraineeWorkMarkQueryFeedback,
                onSubmitRunningLate = { eta, cat, det, onReset -> viewModel.submitTraineeRunningLateAlert(eta, cat, det, onReset) },
                onDismissRunningLateFeedback = viewModel::dismissTraineeRunningLateAlertFeedback,
                onSubmitAcademicRecord = { tTopic, tStat, tDate, lJob, lStat, lDate, rem -> viewModel.submitTraineeAcademicRecord(tTopic, tStat, tDate, lJob, lStat, lDate, rem) },
                onRefreshAcademicSubmission = viewModel::refreshTraineeAcademicSubmission,
                onDismissAcademicFeedback = viewModel::dismissTraineeAcademicFeedback,
                onRefreshTechNews = viewModel::refreshTechnicalNews,
                onSelectNewsLanguage = viewModel::onSelectNewsLanguage,
                onSaveLocalTimetable = { periods -> viewModel.saveTraineeLocalTimetable(context, periods) },
                onResetTimetableToOfficial = { viewModel.resetTraineeTimetableToOfficial(context) },
                onSubmitSharedTimetable = { periods -> viewModel.submitTraineeSharedTimetable(context, periods) },
                onDismissTimetableFeedback = viewModel::dismissTimetableFeedback,
                onCheckUpdate = {
                    if (viewModel.uiState.value.remoteAppConfig.updaterEnabled) {
                        viewModel.checkForAppUpdate(silentIfUpToDate = false)
                    }
                }
                        )
                    }
                }
                }
            }


            SessionScreen.ParentDashboard -> ParentDashboardScreen(
                uiState = uiState,
                onRefresh = viewModel::refreshParentDashboard,
                onLogout = viewModel::logout,
                onEvaluateNotification = { viewModel.evaluateParentNotification(context) },
                onSubmitBunkAlert = { details, onReset -> viewModel.submitParentBunkAlert(details, onReset) },
                onSubmitLeaveReason = { reason, onReset -> viewModel.submitParentLeaveReason(reason, onReset) },
                onDismissFeedback = viewModel::dismissParentAlertFeedback,
                onCheckUpdate = {
                    if (viewModel.uiState.value.remoteAppConfig.updaterEnabled) {
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
                    if (viewModel.uiState.value.remoteAppConfig.updaterEnabled) {
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
                onManualPunch = { roll, status -> viewModel.manualAttendancePunch(roll, status) },
                onConfirmDuplicatePunch = viewModel::confirmDuplicatePunch,
                onDismissDuplicatePunch = viewModel::dismissDuplicatePunch,
                onUndoAttendance = viewModel::undoAttendanceAction,
                onResetAttendanceToAbsent = { roll, name -> viewModel.resetAttendanceToAbsent(roll, name) },
                onRefreshAcademicSubmissions = viewModel::refreshAcademicSubmissions,
                onVerifyAcademicSubmission = viewModel::verifyAcademicSubmission,
                onForwardSingleAcademicToPrincipal = viewModel::forwardSingleAcademicToPrincipal,
                onForwardAllAcademicToPrincipal = viewModel::forwardAllVerifiedAcademicToPrincipal,
                onSetLateGrace = viewModel::setLateGraceForTrainee,
                onSendInstantParentAlert = viewModel::sendInstantParentAlert,
                onToggleEarlyClassLogout = viewModel::toggleEarlyClassLogout,
                onToggleShopFloorReleaseNow = viewModel::toggleShopFloorReleaseNow,
                onUpdateShopFloorMember = viewModel::updateShopFloorMember,
                onRecordShopFloorCardLogout = viewModel::recordShopFloorCardLogout,
                onVerifyShopFloorLogout = viewModel::verifyShopFloorLogout,
                onSignOffDuty = viewModel::signOffDuty,
                onLogToolCrib = viewModel::logToolCribItem,
                onSaveMarksDraftOffline = { label, rows -> viewModel.saveMarksDraftOffline(context, label, rows) },
                onPublishSignedMarksTable = viewModel::publishSignedMarksTable,
                onDismissMonthlyMarkFeedback = viewModel::dismissMonthlyMarkFeedback,
                onReplyWorkMarkQuery = viewModel::replyToWorkMarkQuery,
                onDismissWorkQueryReplyFeedback = viewModel::dismissWorkQueryReplyFeedback,
                onSaveDutyOverride = viewModel::saveDutyOverride,
                onDismissDutyOverrideFeedback = viewModel::dismissDutyOverrideFeedback,
                onPublishTimetable = { periods, allowTraineeEdit -> viewModel.publishTimetable(context, periods, allowTraineeEdit) },
                onDismissTimetableFeedback = viewModel::dismissTimetableFeedback,
                onEvaluateInstructorNotifications = { viewModel.evaluateInstructorNotifications(context) },
                onPostNotice = { title, category, body, onReset -> viewModel.postCampusNotice(title, category, body, false, onReset) },
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
                    if (viewModel.uiState.value.remoteAppConfig.updaterEnabled) {
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
                onRefreshAcademicSubmissions = viewModel::refreshAcademicSubmissions
            )

            SessionScreen.DeveloperDashboard -> Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = viewModel::logout) {
                        Text("Exit Developer Console")
                    }
                }
                    DeveloperMasterPanel(
                        appConfig = uiState.remoteAppConfig,
                        uiConfig = uiState.remoteUiConfig,
                                bugList = uiState.bugReports,
                        onSaveAppConfig = viewModel::saveRemoteAppConfig,
                        onSaveUiConfig = viewModel::saveRemoteUiConfig,
                        onResolveBug = viewModel::resolveBugReport,
                        onEditAnySheetRow = { tab, row, col, valStr -> viewModel.editSheetRow(tab, row, col, valStr) }
                    )
            }
            else -> Unit
        }
    }

}
