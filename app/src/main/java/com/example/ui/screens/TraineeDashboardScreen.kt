package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Grade
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AttendanceMath
import com.example.data.AttendanceSummary
import com.example.data.EmIstDateTimeFormatter
import com.example.data.TimetablePeriod
import com.example.data.TraineeLeaveItem
import com.example.data.TraineeTab
import com.example.notifications.AttendanceNotificationHelper
import com.example.ui.components.ActiveHolidayBanner
import com.example.ui.components.AttendanceFinalizationBanner
import com.example.ui.components.AttendanceStatusPill
import com.example.ui.components.CheckUpdateIconButton
import com.example.ui.components.DigitalSignaturePad
import com.example.ui.components.EspStatusBadge
import com.example.ui.components.ExternalUrlHelper
import com.example.ui.components.InlineAlertCard
import com.example.ui.components.LeaveStatusBadge
import com.example.ui.components.PinnedPrincipalBroadcastBanner
import com.example.ui.components.TricolourRibbon
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

private val LEAVE_TYPES = listOf("1-Hour Permission", "Half-Day Leave")
private val QUICK_TIME_WINDOWS = listOf("11:00 AM - 12:00 PM", "02:00 PM - 03:00 PM", "Afternoon Session", "Forenoon Session")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraineeDashboardScreen(
    uiState: AppUiState,
    onTabSelected: (TraineeTab) -> Unit,
    onSyncNotices: () -> Unit,
    onMarkNoticesSeen: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onSubmitLeave: (type: String, timeWindow: String, reason: String, studentSig: String, onSuccessReset: () -> Unit) -> Unit,
    onDismissFeedback: () -> Unit,
    onSaveProjectGroup: (title: String, members: List<Int>, leaderRoll: Int?, leaderName: String, onReset: () -> Unit) -> Unit,
    onRefreshProjectGroups: () -> Unit,
    onDismissProjectFeedback: () -> Unit,
    onSubmitWorkMarkQuery: (
        monthLabel: String,
        workReference: String,
        currentMarkNote: String,
        traineeNote: String,
        onSuccessReset: () -> Unit
    ) -> Unit,
    onDismissWorkQueryFeedback: () -> Unit,
    onSubmitRunningLate: (
        expectedArrival: String,
        reasonCategory: String,
        details: String,
        onSuccessReset: () -> Unit
    ) -> Unit,
    onDismissRunningLateFeedback: () -> Unit,
    onSubmitAcademicRecord: (
        theoryTopic: String,
        theoryStatus: String,
        theoryUpdatedDate: String,
        labJobName: String,
        labStatus: String,
        labSignedDate: String,
        remarks: String
    ) -> Unit,
    onRefreshAcademicSubmission: () -> Unit,
    onDismissAcademicFeedback: () -> Unit,
    onRefreshTechNews: () -> Unit,
    onSelectNewsLanguage: (NewsLanguageFilter) -> Unit,
    onSaveLocalTimetable: (List<TimetablePeriod>) -> Unit,
    onResetTimetableToOfficial: () -> Unit,
    onSubmitSharedTimetable: (List<TimetablePeriod>) -> Unit,
    onDismissTimetableFeedback: () -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        if (uiState.selectedTraineeTab != TraineeTab.HOME) {
            onTabSelected(TraineeTab.HOME)
        } else {
            onLogout()
        }
    }

    val colors = LocalItiColors.current
    val context = LocalContext.current
    val data = uiState.traineeData

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onSyncNotices()
    }

    LaunchedEffect(Unit) {
        AttendanceNotificationHelper.ensureChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !AttendanceNotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(data?.notices, uiState.selectedTraineeTab) {
        if (uiState.selectedTraineeTab == TraineeTab.NOTICES) {
            onMarkNoticesSeen()
        } else if (data != null) {
            onSyncNotices()
        }
    }

    var selectedType by remember { mutableStateOf(LEAVE_TYPES.first()) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var timeWindow by remember { mutableStateOf(QUICK_TIME_WINDOWS.first()) }
    var reason by remember { mutableStateOf("") }
    var signature by remember { mutableStateOf("") }
    var sigResetKey by remember { mutableIntStateOf(0) }

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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.cardSurface)
                        .statusBarsPadding()
                ) {
                    TricolourRibbon(height = 3.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(shape = RoundedCornerShape(6.dp), color = colors.skyContainer) {
                                    Text(
                                        text = "ROLL #${data?.roll ?: uiState.activeRoll}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = colors.skyAccent,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                EspStatusBadge(label = stringResource(R.string.esp_badge_synced))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Hello, ${data?.name ?: "Trainee"}",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            CheckUpdateIconButton(
                                isChecking = uiState.isCheckingUpdate,
                                hasUpdate = uiState.availableUpdate != null,
                                onClick = onCheckUpdate
                            )
                            IconButton(
                                onClick = onRefresh,
                                enabled = !uiState.isRefreshingTrainee,
                                modifier = Modifier.testTag("trainee_refresh_button")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_refresh), tint = colors.skyAccent)
                            }
                            IconButton(
                                onClick = onLogout,
                                modifier = Modifier.testTag("trainee_logout_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = stringResource(R.string.action_logout), tint = colors.redAccent)
                            }
                        }
                    }
                    if (uiState.isRefreshingTrainee) {
                        LinearProgressIndicator(color = colors.skyAccent, trackColor = colors.cardElevated, modifier = Modifier.fillMaxWidth())
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
                    }
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = colors.cardSurface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        TraineeTab.entries.forEach { tab ->
                            val selected = uiState.selectedTraineeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onTabSelected(tab) },
                                icon = { TraineeTabIcon(tab, selected, uiState.unreadNoticeCount) },
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
                                modifier = Modifier.testTag("trainee_nav_${tab.name.lowercase()}")
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
                    .imePadding()
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = colors.cardSurface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TraineeTab.entries.forEach { tab ->
                            val selected = uiState.selectedTraineeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onTabSelected(tab) },
                                icon = { TraineeTabIcon(tab, selected, uiState.unreadNoticeCount) },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = colors.skyAccent,
                                    selectedTextColor = colors.skyAccent,
                                    indicatorColor = colors.skyContainer,
                                    unselectedIconColor = colors.textMuted,
                                    unselectedTextColor = colors.textMuted
                                ),
                                modifier = Modifier.testTag("trainee_rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (uiState.selectedTraineeTab) {
                        TraineeTab.HOME -> {
                            LazyColumn(
                                modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                if (uiState.traineeBannerError != null) {
                                    item { InlineAlertCard(message = uiState.traineeBannerError, isError = true) }
                                }
                                if (data != null) {
                                    if (data.pinnedBroadcast != null) {
                                        item { PinnedPrincipalBroadcastBanner(notice = data.pinnedBroadcast) }
                                    }
                                    if (data.todayHoliday != null) {
                                        item { ActiveHolidayBanner(holiday = data.todayHoliday) }
                                    }
                                    item {
                                        AttendanceFinalizationBanner(
                                            attendanceClosed = data.attendanceClosed,
                                            attendanceDate = data.attendanceDate,
                                            closedAt = data.closedAt
                                        )
                                    }
                                    item {
                                        TraineeTodayStatusCard(
                                            attendance = data.attendance,
                                            isHolidayToday = data.todayHoliday != null
                                        )
                                    }
                                    item {
                                        TraineeAttendanceRingCard(attendance = data.attendance)
                                    }
                                    if (data.myShopFloorEntry != null) {
                                        item {
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = colors.amberContainer,
                                                border = BorderStroke(1.5.dp, colors.amberAccent.copy(alpha = 0.6f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Icon(Icons.Rounded.Build, contentDescription = null, tint = colors.amberAccent, modifier = Modifier.size(18.dp))
                                                        Text("Post-3PM Shop Floor Assigned", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colors.amberAccent)
                                                    }
                                                    Text(
                                                        text = "${data.myShopFloorEntry.assignedReason} • End: ${data.myShopFloorEntry.scheduledEndTime} • Status: ${data.myShopFloorEntry.status}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    item {
                                        TraineeRunningLateCard(
                                            isSubmitting = uiState.isSubmittingRunningLate,
                                            successMessage = uiState.runningLateMessage,
                                            errorMessage = uiState.runningLateError,
                                            onSubmitRunningLate = { eta, cat, details, reset ->
                                                onDismissRunningLateFeedback()
                                                onSubmitRunningLate(eta, cat, details, reset)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        TraineeTab.OUT_PASS -> {
                            LazyColumn(
                                modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().testTag("trainee_outpass_tab"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = colors.cardSurface,
                                        border = BorderStroke(1.dp, colors.borderSubtle),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(Icons.Rounded.EventNote, contentDescription = null, tint = colors.skyAccent)
                                                Text("Apply for Out-Pass / Half-Day Leave", style = MaterialTheme.typography.titleMedium)
                                            }

                                            ExposedDropdownMenuBox(expanded = typeMenuExpanded, onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }) {
                                                OutlinedTextField(
                                                    value = selectedType,
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("Request Type") },
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.menuAnchor().fillMaxWidth().testTag("leave_type_dropdown")
                                                )
                                                ExposedDropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                                                    LEAVE_TYPES.forEach { option ->
                                                        DropdownMenuItem(text = { Text(option) }, onClick = { selectedType = option; typeMenuExpanded = false })
                                                    }
                                                }
                                            }

                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(QUICK_TIME_WINDOWS) { preset ->
                                                    FilterChip(selected = timeWindow == preset, onClick = { timeWindow = preset }, label = { Text(preset, style = MaterialTheme.typography.labelSmall) })
                                                }
                                            }

                                            OutlinedTextField(
                                                value = timeWindow,
                                                onValueChange = { timeWindow = it },
                                                label = { Text("Time Window") },
                                                leadingIcon = { Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = colors.skyAccent) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth().testTag("leave_time_window_input")
                                            )

                                            OutlinedTextField(
                                                value = reason,
                                                onValueChange = { reason = it },
                                                label = { Text("Reason") },
                                                minLines = 2,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth().testTag("leave_reason_input")
                                            )

                                            DigitalSignaturePad(
                                                defaultSignerName = data?.name ?: "Trainee",
                                                resetKey = sigResetKey,
                                                onSignatureChanged = { signature = it },
                                                title = "Trainee Digital Signature",
                                                tagPrefix = "trainee_sig"
                                            )

                                            AnimatedVisibility(visible = uiState.leaveSubmitMessage != null) {
                                                InlineAlertCard(uiState.leaveSubmitMessage.orEmpty(), isError = false)
                                            }
                                            AnimatedVisibility(visible = uiState.leaveSubmitError != null) {
                                                InlineAlertCard(uiState.leaveSubmitError.orEmpty(), isError = true)
                                            }

                                            Button(
                                                onClick = {
                                                    onDismissFeedback()
                                                    onSubmitLeave(selectedType, timeWindow, reason, signature) {
                                                        reason = ""
                                                        signature = ""
                                                        sigResetKey++
                                                    }
                                                },
                                                enabled = !uiState.isSubmittingLeave && reason.isNotBlank() && signature.isNotBlank(),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("submit_leave_button")
                                            ) {
                                                if (uiState.isSubmittingLeave) {
                                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                                } else {
                                                    Text("Submit Signed Out-Pass Request", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                val leaves = data?.leaves.orEmpty()
                                item {
                                    Text("Out-Pass & Leave History (${leaves.size})", style = MaterialTheme.typography.titleMedium)
                                }
                                items(leaves, key = { "tr_lv_${it.id}_${it.date}" }) { leave ->
                                    TraineeLeaveHistoryCard(leave = leave)
                                }
                            }
                        }

                        TraineeTab.MARKS -> TraineeMarksTab(
                            uiState = uiState,
                            onSubmitWorkMarkQuery = onSubmitWorkMarkQuery,
                            onDismissWorkQueryFeedback = onDismissWorkQueryFeedback
                        )

                        TraineeTab.ACADEMICS -> TraineeAcademicsTab(
                            uiState = uiState,
                            onSubmitAcademicRecord = onSubmitAcademicRecord,
                            onRefreshAcademicSubmission = onRefreshAcademicSubmission,
                            onDismissAcademicFeedback = onDismissAcademicFeedback,
                            onSaveLocalTimetable = onSaveLocalTimetable,
                            onResetTimetableToOfficial = onResetTimetableToOfficial,
                            onSubmitSharedTimetable = onSubmitSharedTimetable,
                            onDismissTimetableFeedback = onDismissTimetableFeedback,
                            onSaveProjectGroup = onSaveProjectGroup,
                            onRefreshProjectGroups = onRefreshProjectGroups,
                            onDismissProjectFeedback = onDismissProjectFeedback
                        )

                        TraineeTab.NOTICES -> TraineeNoticesAndNewsTab(
                            uiState = uiState,
                            onRefreshTechNews = onRefreshTechNews,
                            onSelectNewsLanguage = onSelectNewsLanguage
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TraineeTodayStatusCard(
    attendance: AttendanceSummary,
    isHolidayToday: Boolean
) {
    val colors = LocalItiColors.current
    val effectiveStatus = if (isHolidayToday) "HOLIDAY" else attendance.todayStatus
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Today's RF Attendance", style = MaterialTheme.typography.titleMedium)
                AttendanceStatusPill(status = effectiveStatus)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("MORNING IN", style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily), color = colors.textMuted)
                        Text(EmIstDateTimeFormatter.formatTimeOnly(attendance.inT), style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold), color = colors.emeraldAccent)
                    }
                }
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("AFTERNOON OUT", style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily), color = colors.textMuted)
                        Text(EmIstDateTimeFormatter.formatTimeOnly(attendance.outT), style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold), color = colors.amberAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun TraineeAttendanceRingCard(attendance: AttendanceSummary) {
    val colors = LocalItiColors.current
    val clamped = attendance.pct.coerceIn(0, 100)
    val sweep by animateFloatAsState(targetValue = (clamped / 100f) * 360f, animationSpec = tween(750), label = "tr_ring")
    val ringColors = when {
        clamped >= 80 -> listOf(colors.skyAccent, colors.emeraldAccent)
        clamped >= 75 -> listOf(colors.amberAccent, colors.skyAccent)
        else -> listOf(colors.redAccent, colors.amberAccent)
    }
    val needed = AttendanceMath.sessionsNeededForTargetPct(attendance.present, attendance.half, attendance.total)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                val track = colors.borderSubtle
                Canvas(modifier = Modifier.fillMaxSize().padding(5.dp)) {
                    drawArc(color = track, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(brush = Brush.sweepGradient(ringColors), startAngle = -90f, sweepAngle = sweep, useCenter = false, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
                }
                Text("$clamped%", style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Overall Attendance & DGT Safe-Zone", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text("Present: ${attendance.present} • Half: ${attendance.half} • Total: ${attendance.total}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily), color = colors.textMuted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = colors.amberAccent, modifier = Modifier.size(16.dp))
                    Text("${attendance.streakDays}-day streak • ${if (needed == 0) "DGT Safe (>=80%)" else "Need $needed more full day(s) for 80%"}", style = MaterialTheme.typography.labelSmall, color = if (needed == 0) colors.emeraldAccent else colors.amberAccent)
                }
            }
        }
    }
}

@Composable
private fun TraineeRunningLateCard(
    isSubmitting: Boolean,
    successMessage: String?,
    errorMessage: String?,
    onSubmitRunningLate: (expectedArrival: String, reasonCategory: String, details: String, onReset: () -> Unit) -> Unit
) {
    val colors = LocalItiColors.current
    var eta by remember { mutableStateOf("09:30 AM") }
    var category by remember { mutableStateOf("Bus Delay") }
    var details by remember { mutableStateOf("") }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.DirectionsBus, contentDescription = null, tint = colors.amberAccent, modifier = Modifier.size(18.dp))
                Text("Running Late? Alert Instructor", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = eta, onValueChange = { eta = it }, label = { Text("Expected Arrival") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Reason") }, singleLine = true, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Optional Note") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (successMessage != null) InlineAlertCard(successMessage, false)
            if (errorMessage != null) InlineAlertCard(errorMessage, true)
            Button(
                onClick = { onSubmitRunningLate(eta, category, details) { details = "" } },
                enabled = !isSubmitting && eta.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.amberAccent, contentColor = MaterialTheme.colorScheme.onTertiary),
                modifier = Modifier.fillMaxWidth().testTag("trainee_running_late_button")
            ) {
                Text("Send Running Late Alert", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TraineeLeaveHistoryCard(leave: TraineeLeaveItem) {
    val colors = LocalItiColors.current
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${leave.type} • ${leave.date}", style = MaterialTheme.typography.titleSmall)
                LeaveStatusBadge(status = leave.status)
            }
            if (leave.pdf.isNotBlank()) {
                OutlinedButton(onClick = { ExternalUrlHelper.openUrl(context, leave.pdf) }) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PDF", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun TraineeTabIcon(
    tab: TraineeTab,
    selected: Boolean,
    unreadNotices: Int
) {
    val colors = LocalItiColors.current
    val icon: ImageVector = when (tab) {
        TraineeTab.HOME -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
        TraineeTab.OUT_PASS -> if (selected) Icons.Rounded.EventRepeat else Icons.Outlined.EventRepeat
        TraineeTab.MARKS -> if (selected) Icons.Rounded.Grade else Icons.Outlined.Grade
        TraineeTab.ACADEMICS -> if (selected) Icons.AutoMirrored.Rounded.MenuBook else Icons.AutoMirrored.Outlined.MenuBook
        TraineeTab.NOTICES -> if (selected) Icons.Rounded.Campaign else Icons.Outlined.Campaign
    }
    if (tab == TraineeTab.NOTICES && unreadNotices > 0) {
        BadgedBox(badge = { Badge(containerColor = colors.redAccent) { Text(unreadNotices.toString()) } }) {
            Icon(icon, contentDescription = tab.label, modifier = Modifier.size(22.dp))
        }
    } else {
        Icon(icon, contentDescription = tab.label, modifier = Modifier.size(22.dp))
    }
}
