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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ParentAlertItem
import com.example.data.ParentAlertTypes
import com.example.notifications.AttendanceNotificationHelper
import com.example.ui.components.ActiveHolidayBanner
import com.example.ui.components.AttendanceFinalizationBanner
import com.example.ui.components.AttendanceStatusPill
import com.example.ui.components.CheckUpdateIconButton
import com.example.ui.components.PinnedPrincipalBroadcastBanner
import com.example.ui.components.TricolourRibbon
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState
import kotlinx.coroutines.delay

private val QUICK_MALAYALAM_LEAVE_REASONS = listOf(
    "പനി ആയതുകൊണ്ട് ഇന്ന് ക്ലാസ്സിൽ വരാൻ കഴിയില്ല (Fever)",
    "ആശുപത്രി ആവശ്യം / Hospital visit aanu",
    "കുടുംബ ആവശ്യം / Family function undu",
    "ബസ് പ്രശ്നം കാരണം വൈകും / Bus delay"
)

@Composable
fun ParentDashboardScreen(
    uiState: AppUiState,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onEvaluateNotification: () -> Unit,
    onSubmitBunkAlert: (optionalNote: String, onSuccessReset: () -> Unit) -> Unit,
    onSubmitLeaveReason: (reasonText: String, onSuccessReset: () -> Unit) -> Unit,
    onDismissFeedback: () -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onLogout() }

    val colors = LocalItiColors.current
    val context = LocalContext.current
    val data = uiState.parentData

    var hasNotifPermission by remember {
        mutableStateOf(AttendanceNotificationHelper.hasNotificationPermission(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (granted) {
            onEvaluateNotification()
        }
    }

    // Request POST_NOTIFICATIONS runtime permission on Android 13+ on first entry
    LaunchedEffect(Unit) {
        AttendanceNotificationHelper.ensureChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Trigger Absent / Half-Day notification strictly AFTER Instructor confirms & closes today's attendance (skipped on holidays)
    LaunchedEffect(data, hasNotifPermission) {
        if (data != null && hasNotifPermission) {
            onEvaluateNotification()
        }
    }

    // Auto-poll every 45 seconds while attendance is still Open so confirmed Absent/Half-Day alerts arrive promptly
    LaunchedEffect(data?.attendanceClosed) {
        if (data != null && !data.attendanceClosed) {
            while (true) {
                delay(45_000L)
                onRefresh()
            }
        }
    }

    var bunkNote by remember { mutableStateOf("") }
    var leaveReasonText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TricolourRibbon(height = 3.dp)

        // Top Parent Portal Header
        Surface(
            color = colors.cardSurface,
            border = BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = colors.violetContainer,
                                border = BorderStroke(1.dp, colors.violetAccent.copy(alpha = 0.55f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FamilyRestroom,
                                        contentDescription = null,
                                        tint = colors.violetAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.parent_portal_badge),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.violetAccent
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = data?.studentName ?: "Ward Details",
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
                            enabled = !uiState.isRefreshingParent,
                            modifier = Modifier.testTag("parent_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.action_refresh),
                                tint = colors.skyAccent
                            )
                        }
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier.testTag("parent_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = stringResource(R.string.action_logout),
                                tint = colors.redAccent
                            )
                        }
                    }
                }
                if (uiState.isRefreshingParent) {
                    LinearProgressIndicator(
                        color = colors.skyAccent,
                        trackColor = colors.cardElevated,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 640.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.parentBannerError != null) {
                item {
                    InlineAlertCard(message = uiState.parentBannerError, isError = true)
                }
            }

            // Notification Permission Helper Banner on Android 13+ if denied
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.amberContainer,
                        border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
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
                                    imageVector = Icons.Rounded.NotificationsOff,
                                    contentDescription = null,
                                    tint = colors.amberAccent
                                )
                                Text(
                                    text = "Enable notifications to receive Absent & Half-Day status alerts once Instructor closes attendance.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                border = BorderStroke(1.dp, colors.amberAccent),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.amberAccent),
                                modifier = Modifier.testTag("parent_enable_notif_button")
                            ) {
                                Text("Enable")
                            }
                        }
                    }
                }
            }

            if (data != null) {
                // 0. Pinned Principal Broadcast Notice
                if (data.pinnedBroadcast != null) {
                    item {
                        PinnedPrincipalBroadcastBanner(notice = data.pinnedBroadcast)
                    }
                }

                // 1. Active Holiday Banner (prevents false ABSENT alerts)
                if (data.todayHoliday != null) {
                    item {
                        ActiveHolidayBanner(holiday = data.todayHoliday)
                    }
                }

                // 2. Attendance Open vs Confirmed/Closed Banner
                item {
                    AttendanceFinalizationBanner(
                        attendanceClosed = data.attendanceClosed,
                        attendanceDate = data.attendanceDate,
                        closedAt = data.closedAt
                    )
                }

                // 3. Today's Status Card (ABSENT / HALF-DAY / PRESENT / HOLIDAY + ESP IN/OUT)
                item {
                    val effectiveStatus = if (data.todayHoliday != null) {
                        "HOLIDAY"
                    } else {
                        data.attendance.todayStatus
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.cardSurface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parent_today_status_card")
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
                                Column {
                                    Text(
                                        text = "Ward's Attendance Today",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Date: ${data.attendanceDate}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = colors.textMuted
                                    )
                                }
                                AttendanceStatusPill(status = effectiveStatus)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ParentTimeTile(
                                    label = "MORNING IN",
                                    timeValue = data.attendance.inT,
                                    accentColor = colors.emeraldAccent,
                                    modifier = Modifier.weight(1f)
                                )
                                ParentTimeTile(
                                    label = "AFTERNOON OUT",
                                    timeValue = data.attendance.outT,
                                    accentColor = colors.amberAccent,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (uiState.parentNotificationDispatched) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.NotificationsActive,
                                        contentDescription = null,
                                        tint = colors.emeraldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Status alert notification sent to your device.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.emeraldAccent
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Attendance Percentage Summary Ring Card
                item {
                    ParentAttendanceRingCard(
                        pct = data.attendance.pct,
                        present = data.attendance.present,
                        half = data.attendance.half,
                        total = data.attendance.total
                    )
                }

                // Feedback banner for BUNK_ALERT / LEAVE_REASON submission
                if (uiState.parentAlertSuccessMessage != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.emeraldContainer,
                            border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parent_alert_success_banner")
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
                                    Text(
                                        text = uiState.parentAlertSuccessMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
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
                }

                if (uiState.parentAlertError != null) {
                    item {
                        InlineAlertCard(message = uiState.parentAlertError, isError = true)
                    }
                }

                // 5. Report Bunking / Left Home Card (BUNK_ALERT)
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.cardSurface,
                        border = BorderStroke(1.5.dp, colors.redAccent.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(colors.redContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ReportProblem,
                                        contentDescription = null,
                                        tint = colors.redAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Report Bunking / Left Home",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Alert Trade Instructor immediately if your ward left home but is absent",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = bunkNote,
                                onValueChange = { bunkNote = it },
                                label = { Text("Optional Note (English / Manglish / മലയാളം)") },
                                placeholder = { Text("e.g., വീട്ടിൽ നിന്ന് 8:30 AM-ന് ഇറങ്ങിയിട്ടുണ്ട്") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.redAccent,
                                    unfocusedBorderColor = colors.borderSubtle,
                                    focusedContainerColor = MaterialTheme.colorScheme.background,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("parent_bunk_note_input")
                            )

                            Button(
                                onClick = {
                                    onSubmitBunkAlert(bunkNote) { bunkNote = "" }
                                },
                                enabled = !uiState.isSubmittingParentAlert,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.redAccent,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("parent_bunk_alert_button")
                            ) {
                                if (uiState.isSubmittingParentAlert) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onError,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Sending Alert...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.ReportProblem,
                                        contentDescription = null,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.action_report_bunking),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Leave Reason Form (LEAVE_REASON — Manglish & Malayalam supported)
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.cardSurface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(colors.skyContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Sms,
                                        contentDescription = null,
                                        tint = colors.skyAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Submit Leave Reason",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Supports English, Manglish & മലയാളം",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.skyAccent
                                    )
                                }
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(QUICK_MALAYALAM_LEAVE_REASONS) { suggestion ->
                                    FilterChip(
                                        selected = leaveReasonText == suggestion,
                                        onClick = { leaveReasonText = suggestion },
                                        label = {
                                            Text(
                                                text = suggestion,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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
                                            selected = leaveReasonText == suggestion,
                                            borderColor = colors.borderSubtle,
                                            selectedBorderColor = colors.skyAccent
                                        )
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = leaveReasonText,
                                onValueChange = { leaveReasonText = it },
                                label = { Text("Leave Reason (English / Manglish / മലയാളം)") },
                                placeholder = { Text("Type reason e.g., Pani aayathukondu innu leave aanu / പനി ആയതിനാൽ ഇന്ന് അവധിയാണ്") },
                                minLines = 3,
                                maxLines = 5,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.skyAccent,
                                    unfocusedBorderColor = colors.borderSubtle,
                                    focusedContainerColor = MaterialTheme.colorScheme.background,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("parent_leave_reason_input")
                            )

                            Button(
                                onClick = {
                                    onSubmitLeaveReason(leaveReasonText) { leaveReasonText = "" }
                                },
                                enabled = !uiState.isSubmittingParentAlert,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.skyAccent,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("parent_submit_leave_reason_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Send Leave Reason to Instructor",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                // 7. Recent Parent Reports History
                if (data.parentAlerts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent Parent Submissions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    items(data.parentAlerts, key = { it.id.ifBlank { "${it.date}_${it.time}_${it.hashCode()}" } }) { alert ->
                        ParentAlertCard(alert = alert)
                    }
                }

                // 8. Latest College Notices Preview for Parents
                if (data.notices.isNotEmpty()) {
                    item {
                        Text(
                            text = "Latest College & Trade Notices",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    items(data.notices.take(3), key = { "parent_notice_${it.id}" }) { notice ->
                        NoticeCardItem(notice = notice, isUnread = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentTimeTile(
    label: String,
    timeValue: String,
    accentColor: androidx.compose.ui.graphics.Color,
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
private fun ParentAttendanceRingCard(
    pct: Int,
    present: Int,
    half: Int,
    total: Int
) {
    val colors = LocalItiColors.current
    val clamped = pct.coerceIn(0, 100)
    val animatedSweep by animateFloatAsState(
        targetValue = (clamped / 100f) * 360f,
        animationSpec = tween(durationMillis = 800),
        label = "parent_ring"
    )
    val ringColors = when {
        clamped >= 80 -> listOf(colors.skyAccent, colors.emeraldAccent)
        clamped >= 75 -> listOf(colors.amberAccent, colors.skyAccent)
        else -> listOf(colors.redAccent, colors.amberAccent)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(102.dp)
            ) {
                val trackColor = colors.borderSubtle
                Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(colors = ringColors),
                        startAngle = -90f,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "$clamped%",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Overall Attendance",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Present: $present  •  Half-Day: $half  •  Total: $total",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.textMuted
                )
                Text(
                    text = if (clamped >= 80) "DGT Exam Eligibility Met (>= 80%)" else "Attendance Below 80% DGT Requirement",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (clamped >= 80) colors.emeraldAccent else colors.amberAccent
                )
            }
        }
    }
}

@Composable
fun ParentAlertCard(
    alert: ParentAlertItem,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val isBunk = alert.alertType.equals(ParentAlertTypes.BUNK_ALERT, ignoreCase = true)
    val badgeBg = if (isBunk) colors.redContainer else colors.skyContainer
    val badgeFg = if (isBunk) colors.redAccent else colors.skyAccent
    val badgeLabel = if (isBunk) "BUNK ALERT" else "LEAVE REASON"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, if (isBunk) colors.redAccent.copy(alpha = 0.55f) else colors.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .testTag("parent_alert_card_${alert.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeBg,
                        border = BorderStroke(1.dp, badgeFg.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = badgeFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (alert.roll.isNotBlank()) {
                        Text(
                            text = "Roll #${alert.roll} • ${alert.studentName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = listOf(alert.date, alert.time).filter { it.isNotBlank() }.joinToString(" "),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.textMuted
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}
