package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notifications.AttendanceNotificationHelper
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ParentDashboardScreen
import com.example.ui.screens.StaffDashboardScreen
import com.example.ui.screens.TraineeDashboardScreen
import com.example.ui.theme.ItiRanniTheme
import com.example.viewmodel.AppViewModel
import com.example.viewmodel.SessionScreen

class MainActivity : ComponentActivity() {

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
                ItiAttendanceApp(viewModel = appViewModel)
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
    viewModel: AppViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Crossfade(
        targetState = uiState.screen,
        animationSpec = tween(durationMillis = 240),
        label = "session_screen_transition",
        modifier = Modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            SessionScreen.Login -> LoginScreen(
                uiState = uiState,
                onRoleSelected = viewModel::onRoleSelected,
                onRollChanged = viewModel::onRollInputChanged,
                onPinChanged = viewModel::onPinInputChanged,
                onLoginClick = viewModel::login
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
                onDismissProjectFeedback = viewModel::dismissProjectGroupFeedback
            )

            SessionScreen.ParentDashboard -> ParentDashboardScreen(
                uiState = uiState,
                onRefresh = viewModel::refreshParentDashboard,
                onLogout = viewModel::logout,
                onEvaluateNotification = { viewModel.evaluateParentNotification(context) },
                onSubmitBunkAlert = viewModel::submitParentBunkAlert,
                onSubmitLeaveReason = viewModel::submitParentLeaveReason,
                onDismissFeedback = viewModel::dismissParentAlertFeedback
            )

            SessionScreen.StaffDashboard -> StaffDashboardScreen(
                uiState = uiState,
                onTabSelected = viewModel::onStaffTabSelected,
                onRefresh = viewModel::refreshStaffDashboard,
                onLogout = viewModel::logout,
                onApproveAndSign = viewModel::approveAndSignLeave,
                onDismissApprovalFeedback = viewModel::dismissApprovalFeedback,
                onRegisterSearchChanged = viewModel::onRegisterSearchChanged,
                onRegisterStatusFilterChanged = viewModel::onRegisterStatusFilterChanged,
                onExportPdf = viewModel::exportDailyAttendancePdf,
                onConfirmCloseAttendance = viewModel::confirmAndCloseTodayAttendance,
                onDismissCloseFeedback = viewModel::dismissCloseAttendanceFeedback,
                onPostNotice = viewModel::postTradeNotice,
                onPostHoliday = viewModel::postHolidayAnnouncement,
                onToggleProjectGroupsAccess = viewModel::toggleProjectGroupsAccess,
                onRefreshProjectGroups = viewModel::refreshProjectGroupsForStaff,
                onEspRollChanged = viewModel::onEspRollInputChanged,
                onTriggerManualTap = viewModel::triggerManualEspTap
            )
        }
    }
}
