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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Workspaces
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
import androidx.compose.ui.graphics.Color
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
import com.example.data.AttendanceSummary
import com.example.data.TraineeLeaveItem
import com.example.data.TraineeTab
import com.example.notifications.AttendanceNotificationHelper
import com.example.ui.components.ActiveHolidayBanner
import com.example.ui.components.AttendanceFinalizationBanner
import com.example.ui.components.AttendanceStatusPill
import com.example.ui.components.DigitalSignaturePad
import com.example.ui.components.EspStatusBadge
import com.example.ui.components.ExternalUrlHelper
import com.example.ui.components.LeaveStatusBadge
import com.example.ui.components.TricolourRibbon
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

private val LEAVE_TYPES = listOf("1-Hour Permission", "Half-Day Leave")
private val QUICK_TIME_WINDOWS = listOf(
    "11:00 AM - 12:00 PM",
    "02:00 PM - 03:00 PM",
    "Afternoon Session",
    "Forenoon Session"
)

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
    var timeWindow by remember { mutableStateOf("11:00 AM - 12:00 PM") }
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
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.skyContainer
                                ) {
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

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = onRefresh,
                                enabled = !uiState.isRefreshingTrainee,
                                modifier = Modifier.testTag("trainee_refresh_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.action_refresh),
                                    tint = colors.skyAccent
                                )
                            }
                            IconButton(
                                onClick = onLogout,
                                modifier = Modifier.testTag("trainee_logout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = stringResource(R.string.action_logout),
                                    tint = colors.redAccent
                                )
                            }
                        }
                    }
                    if (uiState.isRefreshingTrainee) {
                        LinearProgressIndicator(
                            color = colors.skyAccent,
                            trackColor = colors.cardElevated,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.borderSubtle)
                        )
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
                                icon = {
                                    TraineeTabIcon(
                                        tab = tab,
                                        selected = selected,
                                        unreadNotices = uiState.unreadNoticeCount,
                                        projectsLocked = !(data?.projectGroupsEnabled ?: false)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
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
                                icon = {
                                    TraineeTabIcon(
                                        tab = tab,
                                        selected = selected,
                                        unreadNotices = uiState.unreadNoticeCount,
                                        projectsLocked = !(data?.projectGroupsEnabled ?: false)
                                    )
                                },
                                label = {
                                    Text(text = tab.label, style = MaterialTheme.typography.labelSmall)
                                },
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
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (uiState.selectedTraineeTab) {
                        TraineeTab.HOME -> {
                            LazyColumn(
                                modifier = Modifier
                                    .widthIn(max = 640.dp)
                                    .fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (uiState.traineeBannerError != null) {
                                    item {
                                        InlineAlertCard(message = uiState.traineeBannerError, isError = true)
                                    }
                                }

                                if (data != null) {
                                    // Active Holiday Banner
                                    if (data.todayHoliday != null) {
                                        item {
                                            ActiveHolidayBanner(holiday = data.todayHoliday)
                                        }
                                    }

                                    // Attendance Finalization Banner
                                    item {
                                        AttendanceFinalizationBanner(
                                            attendanceClosed = data.attendanceClosed,
                                            attendanceDate = data.attendanceDate,
                                            closedAt = data.closedAt
                                        )
                                    }

                                    // 1. Today's Status Card
                                    item {
                                        TodayStatusCard(
                                            attendance = data.attendance,
                                            isHolidayToday = data.todayHoliday != null
                                        )
                                    }

                                    // 2. Attendance Percentage Ring & Breakdown Card
                                    item {
                                        AttendancePercentageCard(attendance = data.attendance)
                                    }

                                    // 3. Apply for Out-Pass / Half-Day Leave Card
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = colors.cardSurface,
                                            border = BorderStroke(1.dp, colors.borderSubtle),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(18.dp),
                                                verticalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(colors.skyContainer),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.EventNote,
                                                            contentDescription = null,
                                                            tint = colors.skyAccent,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Column {
                                                        Text(
                                                            text = "Apply for Out-Pass / Half-Day Leave",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "Digitally signed workflow via Instructor & Principal",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = colors.textMuted
                                                        )
                                                    }
                                                }

                                                ExposedDropdownMenuBox(
                                                    expanded = typeMenuExpanded,
                                                    onExpandedChange = { typeMenuExpanded = !typeMenuExpanded }
                                                ) {
                                                    OutlinedTextField(
                                                        value = selectedType,
                                                        onValueChange = {},
                                                        readOnly = true,
                                                        label = { Text("Request Type") },
                                                        trailingIcon = {
                                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded)
                                                        },
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = colors.skyAccent,
                                                            unfocusedBorderColor = colors.borderSubtle,
                                                            focusedContainerColor = MaterialTheme.colorScheme.background,
                                                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                                                        ),
                                                        modifier = Modifier
                                                            .menuAnchor()
                                                            .fillMaxWidth()
                                                            .testTag("leave_type_dropdown")
                                                    )
                                                    ExposedDropdownMenu(
                                                        expanded = typeMenuExpanded,
                                                        onDismissRequest = { typeMenuExpanded = false },
                                                        modifier = Modifier.background(colors.cardElevated)
                                                    ) {
                                                        LEAVE_TYPES.forEach { option ->
                                                            DropdownMenuItem(
                                                                text = { Text(option, color = MaterialTheme.colorScheme.onSurface) },
                                                                onClick = {
                                                                    selectedType = option
                                                                    if (option == "Half-Day Leave" && timeWindow == "11:00 AM - 12:00 PM") {
                                                                        timeWindow = "Afternoon Session"
                                                                    } else if (option == "1-Hour Permission" && timeWindow == "Afternoon Session") {
                                                                        timeWindow = "11:00 AM - 12:00 PM"
                                                                    }
                                                                    typeMenuExpanded = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }

                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        items(QUICK_TIME_WINDOWS) { preset ->
                                                            FilterChip(
                                                                selected = timeWindow == preset,
                                                                onClick = { timeWindow = preset },
                                                                label = {
                                                                    Text(
                                                                        text = preset,
                                                                        style = MaterialTheme.typography.labelSmall
                                                                    )
                                                                },
                                                                colors = FilterChipDefaults.filterChipColors(
                                                                    selectedContainerColor = colors.skyContainer,
                                                                    selectedLabelColor = colors.skyAccent,
                                                                    containerColor = MaterialTheme.colorScheme.background,
                                                                    labelColor = colors.textMuted
                                                                ),
                                                                border = FilterChipDefaults.filterChipBorder(
                                                                    enabled = true,
                                                                    selected = timeWindow == preset,
                                                                    borderColor = colors.borderSubtle,
                                                                    selectedBorderColor = colors.skyAccent
                                                                )
                                                            )
                                                        }
                                                    }

                                                    OutlinedTextField(
                                                        value = timeWindow,
                                                        onValueChange = { timeWindow = it },
                                                        label = { Text("Time Window") },
                                                        placeholder = { Text("e.g., 11:00 AM - 12:00 PM or Afternoon Session") },
                                                        leadingIcon = {
                                                            Icon(
                                                                imageVector = Icons.Rounded.AccessTime,
                                                                contentDescription = null,
                                                                tint = colors.skyAccent
                                                            )
                                                        },
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = colors.skyAccent,
                                                            unfocusedBorderColor = colors.borderSubtle,
                                                            focusedContainerColor = MaterialTheme.colorScheme.background,
                                                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                                                        ),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("leave_time_window_input")
                                                    )
                                                }

                                                OutlinedTextField(
                                                    value = reason,
                                                    onValueChange = { reason = it },
                                                    label = { Text("Reason") },
                                                    placeholder = { Text("Enter valid reason for permission / leave") },
                                                    minLines = 2,
                                                    maxLines = 4,
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = colors.skyAccent,
                                                        unfocusedBorderColor = colors.borderSubtle,
                                                        focusedContainerColor = MaterialTheme.colorScheme.background,
                                                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("leave_reason_input")
                                                )

                                                DigitalSignaturePad(
                                                    defaultSignerName = data.name,
                                                    resetKey = sigResetKey,
                                                    onSignatureChanged = { signature = it },
                                                    title = "Trainee Digital Signature",
                                                    tagPrefix = "trainee_sig"
                                                )

                                                AnimatedVisibility(visible = uiState.leaveSubmitMessage != null) {
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = colors.emeraldContainer,
                                                        border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.6f)),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("leave_submit_success_banner")
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(12.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.weight(1f),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.CheckCircle,
                                                                    contentDescription = null,
                                                                    tint = colors.emeraldAccent
                                                                )
                                                                Column {
                                                                    if (!uiState.leaveSubmitReqId.isNullOrBlank()) {
                                                                        Text(
                                                                            text = "Request ID: ${uiState.leaveSubmitReqId}",
                                                                            style = MaterialTheme.typography.labelLarge.copy(
                                                                                fontFamily = JetBrainsMonoFamily,
                                                                                fontWeight = FontWeight.Bold
                                                                            ),
                                                                            color = colors.emeraldAccent
                                                                        )
                                                                    }
                                                                    Text(
                                                                        text = uiState.leaveSubmitMessage.orEmpty(),
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                }
                                                            }
                                                            IconButton(onClick = onDismissFeedback) {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.Close,
                                                                    contentDescription = "Dismiss",
                                                                    tint = colors.textMuted,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                AnimatedVisibility(visible = uiState.leaveSubmitError != null) {
                                                    InlineAlertCard(
                                                        message = uiState.leaveSubmitError.orEmpty(),
                                                        isError = true
                                                    )
                                                }

                                                Button(
                                                    onClick = {
                                                        onSubmitLeave(selectedType, timeWindow, reason, signature) {
                                                            reason = ""
                                                            signature = ""
                                                            sigResetKey++
                                                        }
                                                    },
                                                    enabled = !uiState.isSubmittingLeave,
                                                    shape = RoundedCornerShape(14.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = colors.emeraldAccent,
                                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(52.dp)
                                                        .testTag("submit_leave_button")
                                                ) {
                                                    if (uiState.isSubmittingLeave) {
                                                        CircularProgressIndicator(
                                                            color = MaterialTheme.colorScheme.onSecondary,
                                                            strokeWidth = 2.5.dp,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text("Submitting Request...")
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Rounded.Send,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "Submit Signed Out-Pass Request",
                                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 4. My Leave & Pass History
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "My Leave & Pass History",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${data.leaves.size} record(s)",
                                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                                color = colors.textMuted
                                            )
                                        }
                                    }

                                    if (data.leaves.isEmpty()) {
                                        item {
                                            Surface(
                                                shape = RoundedCornerShape(18.dp),
                                                color = colors.cardSurface,
                                                border = BorderStroke(1.dp, colors.borderSubtle),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(28.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Description,
                                                        contentDescription = null,
                                                        tint = colors.textMuted,
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                    Text(
                                                        text = "No Out-Pass or Leave Requests Yet",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Submitted requests and approved signed PDFs will appear here.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = colors.textMuted
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        items(
                                            data.leaves,
                                            key = { it.id.ifBlank { "${it.date}_${it.type}_${it.hashCode()}" } }
                                        ) { item ->
                                            TraineeLeaveHistoryCard(
                                                item = item,
                                                onOpenPdf = { url -> ExternalUrlHelper.openUrl(context, url) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        TraineeTab.NOTICES -> NoticesScreen(
                            notices = data?.notices.orEmpty(),
                            todayHoliday = data?.todayHoliday,
                            lastSeenNoticeId = uiState.lastSeenNoticeId,
                            onRefresh = onRefresh,
                            modifier = Modifier.widthIn(max = 640.dp)
                        )

                        TraineeTab.DUTY_CHART -> DutyChartTab(
                            myRollNumber = (data?.roll ?: uiState.activeRoll).toIntOrNull(),
                            customDutyItems = data?.dutyItems.orEmpty(),
                            modifier = Modifier.widthIn(max = 640.dp)
                        )

                        TraineeTab.PROJECT_GROUPS -> ProjectGroupsTab(
                            myRollNumber = (data?.roll ?: uiState.activeRoll).toIntOrNull(),
                            myName = data?.name.orEmpty(),
                            isEnabled = data?.projectGroupsEnabled ?: false,
                            groups = data?.projectGroups.orEmpty(),
                            isSaving = uiState.isSavingProjectGroup,
                            saveSuccessMessage = uiState.projectGroupSaveMessage,
                            saveErrorMessage = uiState.projectGroupSaveError,
                            onSaveGroup = onSaveProjectGroup,
                            onRefreshGroups = onRefreshProjectGroups,
                            onDismissFeedback = onDismissProjectFeedback,
                            modifier = Modifier.widthIn(max = 640.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TraineeTabIcon(
    tab: TraineeTab,
    selected: Boolean,
    unreadNotices: Int,
    projectsLocked: Boolean
) {
    val colors = LocalItiColors.current
    val icon: ImageVector = when (tab) {
        TraineeTab.HOME -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
        TraineeTab.NOTICES -> if (selected) Icons.Rounded.Campaign else Icons.Outlined.Campaign
        TraineeTab.DUTY_CHART -> if (selected) Icons.Rounded.EventRepeat else Icons.Outlined.EventRepeat
        TraineeTab.PROJECT_GROUPS -> if (projectsLocked) {
            if (selected) Icons.Rounded.Lock else Icons.Outlined.Lock
        } else {
            if (selected) Icons.Rounded.Workspaces else Icons.Outlined.Workspaces
        }
    }

    if (tab == TraineeTab.NOTICES && unreadNotices > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = colors.redAccent,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Text(unreadNotices.toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = tab.label, modifier = Modifier.size(22.dp))
        }
    } else {
        Icon(imageVector = icon, contentDescription = tab.label, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun TodayStatusCard(
    attendance: AttendanceSummary,
    isHolidayToday: Boolean
) {
    val colors = LocalItiColors.current
    val statusToShow = if (isHolidayToday) "HOLIDAY" else attendance.todayStatus
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_status_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Today's Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.esp_badge_live),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = colors.emeraldAccent
                    )
                }
                AttendanceStatusPill(status = statusToShow)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EspTimeTile(
                    label = "ESP IN TIME",
                    timeValue = attendance.inT,
                    accentColor = colors.emeraldAccent,
                    modifier = Modifier.weight(1f)
                )
                EspTimeTile(
                    label = "ESP OUT TIME",
                    timeValue = attendance.outT,
                    accentColor = colors.amberAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EspTimeTile(
    label: String,
    timeValue: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sensors,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.textMuted
                )
            }
            Text(
                text = timeValue.ifBlank { "-" },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AttendancePercentageCard(attendance: AttendanceSummary) {
    val colors = LocalItiColors.current
    val pctClamped = attendance.pct.coerceIn(0, 100)
    val animatedSweep by animateFloatAsState(
        targetValue = (pctClamped / 100f) * 360f,
        animationSpec = tween(durationMillis = 850),
        label = "attendance_ring_sweep"
    )

    val ringColors = when {
        pctClamped >= 80 -> listOf(colors.skyAccent, colors.emeraldAccent)
        pctClamped >= 75 -> listOf(colors.amberAccent, colors.skyAccent)
        else -> listOf(colors.redAccent, colors.amberAccent)
    }

    val statusHint = when {
        pctClamped >= 80 -> "On Track (DGT >= 80% Eligibility)"
        pctClamped >= 75 -> "Borderline — Maintain Daily Swipes"
        else -> "Below 75% — Attention Required"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attendance_percentage_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(116.dp)
                ) {
                    val trackColor = colors.borderSubtle
                    Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(colors = ringColors),
                            startAngle = -90f,
                            sweepAngle = animatedSweep,
                            useCenter = false,
                            style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${attendance.pct}%",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "OVERALL",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                            color = colors.textMuted
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Attendance Summary",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = statusHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (pctClamped >= 80) colors.emeraldAccent else if (pctClamped >= 75) colors.amberAccent else colors.redAccent
                    )
                    LinearProgressIndicator(
                        progress = { pctClamped / 100f },
                        color = ringColors.first(),
                        trackColor = colors.borderSubtle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCounterTile(
                    label = "Present",
                    value = attendance.present.toString(),
                    accent = colors.emeraldAccent,
                    modifier = Modifier.weight(1f)
                )
                MetricCounterTile(
                    label = "Half-Day",
                    value = attendance.half.toString(),
                    accent = colors.amberAccent,
                    modifier = Modifier.weight(1f)
                )
                MetricCounterTile(
                    label = "Total Days",
                    value = attendance.total.toString(),
                    accent = colors.skyAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricCounterTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TraineeLeaveHistoryCard(
    item: TraineeLeaveItem,
    onOpenPdf: (String) -> Unit
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leave_history_card_${item.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.id.ifBlank { "REQ" },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.skyAccent
                )
                LeaveStatusBadge(status = item.status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.type,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.textMuted
                )
            }

            if (item.pdf.isNotBlank()) {
                Button(
                    onClick = { onOpenPdf(item.pdf) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.emeraldContainer,
                        contentColor = colors.emeraldAccent
                    ),
                    border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("view_pdf_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "View Signed PDF",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
internal fun InlineAlertCard(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val bg = if (isError) colors.redContainer else colors.emeraldContainer
    val fg = if (isError) colors.redAccent else colors.emeraldAccent
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = fg
            )
        }
    }
}
