package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.OfflineSyncQueue
import com.example.data.StaffTab
import com.example.data.TimetablePeriod
import com.example.pdf.FullReportExportDialog
import com.example.ui.components.CheckUpdateIconButton
import com.example.ui.components.EspStatusBadge
import com.example.ui.components.ExternalUrlHelper
import com.example.ui.components.InlineAlertCard
import com.example.ui.components.TricolourRibbon
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

@Composable
fun StaffDashboardScreen(
    uiState: AppUiState,
    onTabSelected: (StaffTab) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onCheckUpdate: () -> Unit,
    onApproveAndSign: (reqId: String, signature: String) -> Unit,
    onRejectLeave: (reqId: String, reason: String) -> Unit,
    onDismissApprovalFeedback: () -> Unit,
    onRegisterSearchChanged: (String) -> Unit,
    onRegisterStatusFilterChanged: (String) -> Unit,
    onExportPdf: (onPdfReady: (String) -> Unit) -> Unit,
    onConfirmCloseAttendance: () -> Unit,
    onDismissCloseFeedback: () -> Unit,
    onBulkMorningRollCall: () -> Unit,
    onManualPunch: (roll: String, status: String) -> Unit,
    onConfirmDuplicatePunch: () -> Unit,
    onDismissDuplicatePunch: () -> Unit,
    onUndoAttendance: (roll: String) -> Unit,
    onResetAttendanceToAbsent: (roll: String, studentName: String) -> Unit,
    onRefreshAcademicSubmissions: () -> Unit,
    onVerifyAcademicSubmission: (roll: String, submissionId: String, remark: String) -> Unit,
    onForwardSingleAcademicToPrincipal: (roll: String) -> Unit,
    onForwardAllAcademicToPrincipal: () -> Unit,
    onSetLateGrace: (roll: String, mode: String, cutoff: String, note: String) -> Unit,
    onSendInstantParentAlert: (roll: String, name: String, alertType: String, msg: String) -> Unit,
    onToggleEarlyClassLogout: (Boolean) -> Unit,
    onToggleShopFloorReleaseNow: (Boolean) -> Unit,
    onUpdateShopFloorMember: (roll: String, operation: String, sessionType: String, endTime: String) -> Unit,
    onRecordShopFloorCardLogout: (roll: String) -> Unit,
    onVerifyShopFloorLogout: (roll: String, date: String) -> Unit,
    onSignOffDuty: (date: String, pairLabel: String, note: String, onReset: () -> Unit) -> Unit,
    onLogToolCrib: (itemName: String, conditionStatus: String, notes: String, onReset: () -> Unit) -> Unit,
    onSaveMarksDraftOffline: (monthLabel: String, rows: List<ExamMarkRow>) -> Unit,
    onPublishSignedMarksTable: (
        monthLabel: String,
        examDate: String,
        theoryMax: Int,
        labMax: Int,
        rows: List<ExamMarkRow>,
        signedBy: String,
        signatureData: String
    ) -> Unit,
    onDismissMonthlyMarkFeedback: () -> Unit,
    onReplyWorkMarkQuery: (queryId: String, status: String, reply: String, onSuccessReset: () -> Unit) -> Unit,
    onDismissWorkQueryReplyFeedback: () -> Unit,
    onSaveDutyOverride: (dateKey: String, firstRoll: Int, secondRoll: Int, dutyNote: String, reanchorFromHere: Boolean, resetToAuto: Boolean) -> Unit,
    onDismissDutyOverrideFeedback: () -> Unit,
    onPublishTimetable: (periods: List<TimetablePeriod>, allowTraineeEdit: Boolean) -> Unit,
    onDismissTimetableFeedback: () -> Unit,
    onEvaluateInstructorNotifications: () -> Unit,
    onPostNotice: (title: String, category: String, body: String, onReset: () -> Unit) -> Unit,
    onPostHoliday: (date: String, reason: String, onReset: () -> Unit) -> Unit,
    onToggleProjectGroupsAccess: (Boolean) -> Unit,
    onRefreshProjectGroups: () -> Unit,
    onEspRollChanged: (String) -> Unit,
    onTriggerManualTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        if (uiState.selectedStaffTab != StaffTab.ATTENDANCE_REGISTER) {
            onTabSelected(StaffTab.ATTENDANCE_REGISTER)
        } else {
            onLogout()
        }
    }

    LaunchedEffect(uiState.staffData?.runningLateAlerts, uiState.staffData?.workMarkQueries) {
        if (uiState.staffData != null) onEvaluateInstructorNotifications()
    }

    val colors = LocalItiColors.current
    val context = LocalContext.current
    val pendingCount = uiState.staffData?.pendingLeaves?.size ?: 0
    val alertsCount = (uiState.staffData?.parentAlerts?.size ?: 0) + (uiState.staffData?.runningLateAlerts?.size ?: 0)
    val shopFloorCount = uiState.staffData?.shopFloorList?.size ?: 0
    val allAttendance = uiState.staffData?.allAttendance.orEmpty()
    val presentTodayCount = allAttendance.count {
        val status = it.todayStatus.uppercase()
        status == "PRESENT" || status.contains("HALF") || status.contains("OUT-PASS") || status == "LATE" || status == "ON-DUTY"
    }
    val attendanceClosed = uiState.staffData?.attendanceClosed ?: false
    var showFullPdfDialog by remember { mutableStateOf(false) }
    val pendingOfflineCount = remember(context, uiState.selectedStaffTab) {
        OfflineSyncQueue.count(context)
    }

    if (showFullPdfDialog && uiState.staffData != null) {
        FullReportExportDialog(
            staffData = uiState.staffData,
            roleLabel = "Trade Instructor (EM)",
            onDismiss = { showFullPdfDialog = false }
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                StaffHeaderBar(
                    presentToday = presentTodayCount,
                    totalTrainees = allAttendance.size,
                    shopFloorCount = shopFloorCount,
                    attendanceClosed = attendanceClosed,
                    isRefreshing = uiState.isRefreshingStaff,
                    isCheckingUpdate = uiState.isCheckingUpdate,
                    hasUpdate = uiState.availableUpdate != null,
                    onExportFullPdf = { showFullPdfDialog = true },
                    onCheckUpdate = onCheckUpdate,
                    onRefresh = onRefresh,
                    onLogout = onLogout
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = colors.cardSurface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        StaffTab.entries.forEach { tab ->
                            val selected = uiState.selectedStaffTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onTabSelected(tab) },
                                icon = { StaffTabIcon(tab, selected, pendingCount + alertsCount, shopFloorCount) },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.skyAccent,
                                    selectedTextColor = colors.skyAccent,
                                    indicatorColor = colors.skyContainer,
                                    unselectedIconColor = colors.textMuted,
                                    unselectedTextColor = colors.textMuted
                                ),
                                modifier = Modifier.testTag("staff_nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = colors.cardSurface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        StaffTab.entries.forEach { tab ->
                            val selected = uiState.selectedStaffTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onTabSelected(tab) },
                                icon = { StaffTabIcon(tab, selected, pendingCount + alertsCount, shopFloorCount) },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = colors.skyAccent,
                                    selectedTextColor = colors.skyAccent,
                                    indicatorColor = colors.skyContainer,
                                    unselectedIconColor = colors.textMuted,
                                    unselectedTextColor = colors.textMuted
                                ),
                                modifier = Modifier.testTag("staff_rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (pendingOfflineCount > 0) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            InlineAlertCard("Offline queue: $pendingOfflineCount draft(s) saved locally.", false)
                        }
                    }
                    if (uiState.staffBannerError != null) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            InlineAlertCard(uiState.staffBannerError, true)
                        }
                    }

                    when (uiState.selectedStaffTab) {
                                StaffTab.ALERTS -> {
                                    var instructorAlerts by remember { mutableStateOf(defaultInstructorAlerts()) }
                                    InstructorAlertHubPanel(
                                        alerts = instructorAlerts,
                                        onUpdateAlerts = { instructorAlerts = it },
                                        isInstructor = true,
                                        cardSurface = colors.cardSurface,
                                        borderSubtle = colors.borderSubtle,
                                        textMuted = colors.textMuted
                                    )
                                }
                        StaffTab.ATTENDANCE_REGISTER -> AttendanceRegisterTab(
                            uiState = uiState,
                            onSearchChanged = onRegisterSearchChanged,
                            onStatusFilterChanged = onRegisterStatusFilterChanged,
                            onExportPdf = { onExportPdf { url -> ExternalUrlHelper.openUrl(context, url) } },
                            onConfirmCloseAttendance = onConfirmCloseAttendance,
                            onDismissCloseFeedback = onDismissCloseFeedback,
                            onBulkMorningRollCall = onBulkMorningRollCall,
                            onManualPunch = onManualPunch,
                            onConfirmDuplicatePunch = onConfirmDuplicatePunch,
                            onDismissDuplicatePunch = onDismissDuplicatePunch,
                            onUndoAttendance = onUndoAttendance,
                            onResetAttendanceToAbsent = onResetAttendanceToAbsent,
                            onSetLateGrace = onSetLateGrace,
                            onSendInstantParentAlert = onSendInstantParentAlert
                        )
                        StaffTab.APPROVALS_ALERTS -> ApprovalsAndAlertsTab(
                            uiState = uiState,
                            onApproveAndSign = onApproveAndSign,
                            onRejectLeave = onRejectLeave,
                            onDismissApprovalFeedback = onDismissApprovalFeedback,
                            onRefreshAcademicSubmissions = onRefreshAcademicSubmissions,
                            onVerifyAcademicSubmission = onVerifyAcademicSubmission,
                            onForwardSingleAcademicToPrincipal = onForwardSingleAcademicToPrincipal,
                            onForwardAllAcademicToPrincipal = onForwardAllAcademicToPrincipal
                        )

                        StaffTab.MARKS_REGISTER -> InstructorMarksRegisterTablePanel(
                            uiState = uiState,
                            onSaveDraftOffline = onSaveMarksDraftOffline,
                            onPublishSignedMarksTable = onPublishSignedMarksTable,
                            onDismissFeedback = onDismissMonthlyMarkFeedback,
                            onReplyWorkMarkQuery = onReplyWorkMarkQuery,
                            onDismissWorkQueryReplyFeedback = onDismissWorkQueryReplyFeedback
                        )

                        StaffTab.SHOP_FLOOR -> ShopFloorControlTab(
                            uiState = uiState,
                            onToggleEarlyClassLogout = onToggleEarlyClassLogout,
                            onToggleShopFloorReleaseNow = onToggleShopFloorReleaseNow,
                            onAddOrRemoveShopFloor = onUpdateShopFloorMember,
                            onRecordCardLogout = onRecordShopFloorCardLogout,
                            onVerifyLogout = onVerifyShopFloorLogout
                        )

                        StaffTab.CAMPUS_HUB -> CampusManagementTab(
                            uiState = uiState,
                            onPostNotice = onPostNotice,
                            onPostHoliday = onPostHoliday,
                            onToggleProjectGroupsAccess = onToggleProjectGroupsAccess,
                            onRefreshProjectGroups = onRefreshProjectGroups,
                            onSignOffDuty = onSignOffDuty,
                            onLogToolCrib = onLogToolCrib,
                            onSaveDutyOverride = onSaveDutyOverride,
                            onDismissDutyOverrideFeedback = onDismissDutyOverrideFeedback,
                            onPublishTimetable = onPublishTimetable,
                            onDismissTimetableFeedback = onDismissTimetableFeedback,
                            onEspRollChanged = onEspRollChanged,
                            onTriggerManualTap = onTriggerManualTap
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffHeaderBar(
    presentToday: Int,
    totalTrainees: Int,
    shopFloorCount: Int,
    attendanceClosed: Boolean,
    isRefreshing: Boolean,
    isCheckingUpdate: Boolean,
    hasUpdate: Boolean,
    onExportFullPdf: () -> Unit,
    onCheckUpdate: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    val colors = LocalItiColors.current
    Column(modifier = Modifier.fillMaxWidth().background(colors.cardSurface).statusBarsPadding()) {
        TricolourRibbon(height = 3.dp)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Trade Instructor Workshop",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                EspStatusBadge(label = stringResource(R.string.esp_badge_live))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onExportFullPdf, modifier = Modifier.testTag("staff_header_pdf_button")) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Export Full PDF Report", tint = colors.emeraldAccent)
                }
                CheckUpdateIconButton(isCheckingUpdate, hasUpdate, onCheckUpdate)
                IconButton(onClick = onRefresh, enabled = !isRefreshing, modifier = Modifier.testTag("staff_refresh_button")) {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_refresh), tint = colors.skyAccent)
                }
                IconButton(onClick = onLogout, modifier = Modifier.testTag("staff_logout_button")) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = stringResource(R.string.action_logout), tint = colors.redAccent)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiMiniBadge("Active", "$presentToday/$totalTrainees", colors.emeraldAccent, Modifier.weight(1f))
            KpiMiniBadge("Register", if (attendanceClosed) "CLOSED" else "OPEN", if (attendanceClosed) colors.emeraldAccent else colors.amberAccent, Modifier.weight(1f))
            KpiMiniBadge("ShopFlr", shopFloorCount.toString(), if (shopFloorCount > 0) colors.amberAccent else colors.emeraldAccent, Modifier.weight(1f))
        }

        if (isRefreshing) {
            LinearProgressIndicator(color = colors.skyAccent, trackColor = colors.cardElevated, modifier = Modifier.fillMaxWidth())
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
        }
    }
}

@Composable
private fun KpiMiniBadge(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    val colors = LocalItiColors.current
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, colors.borderSubtle), modifier = modifier) {
        Row(modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textMuted, maxLines = 1)
            Spacer(Modifier.width(4.dp))
            Text(value, style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold), color = accent, maxLines = 1)
        }
    }
}

@Composable
private fun StaffTabIcon(tab: StaffTab, selected: Boolean, approvalsAndAlertsCount: Int, shopFloorCount: Int) {
    val colors = LocalItiColors.current
    val icon: ImageVector = when (tab) {
        StaffTab.ATTENDANCE_REGISTER -> if (selected) Icons.Rounded.Groups else Icons.Outlined.Groups
        StaffTab.APPROVALS_ALERTS -> if (selected) Icons.Rounded.PendingActions else Icons.Outlined.PendingActions
        StaffTab.ALERTS -> if (selected) Icons.Rounded.Campaign else Icons.Outlined.Campaign
        StaffTab.MARKS_REGISTER -> if (selected) Icons.Rounded.Grade else Icons.Outlined.Grade
        StaffTab.SHOP_FLOOR -> if (selected) Icons.Rounded.Build else Icons.Outlined.Build
        StaffTab.CAMPUS_HUB -> if (selected) Icons.Rounded.Campaign else Icons.Outlined.Campaign
    }
    val badgeCount = when (tab) {
        StaffTab.APPROVALS_ALERTS -> approvalsAndAlertsCount
        StaffTab.SHOP_FLOOR -> shopFloorCount
        else -> 0
    }
    if (badgeCount > 0) {
        BadgedBox(badge = { Badge(containerColor = colors.amberAccent) { Text(badgeCount.toString()) } }) {
            Icon(icon, contentDescription = tab.label, modifier = Modifier.size(22.dp))
        }
    } else {
        Icon(icon, contentDescription = tab.label, modifier = Modifier.size(22.dp))
    }
}
