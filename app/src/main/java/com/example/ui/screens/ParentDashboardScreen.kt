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
import androidx.compose.material.icons.rounded.Grade
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.IstDateTimeFormatter
import com.example.data.ParentAlertItem
import com.example.data.ParentAlertTypes
import com.example.notifications.AttendanceNotificationHelper
import com.example.ui.components.ActiveHolidayBanner
import com.example.ui.components.AttendanceFinalizationBanner
import com.example.ui.components.AttendanceStatusPill
import com.example.ui.components.CheckUpdateIconButton
import com.example.ui.components.InlineAlertCard
import com.example.ui.components.NoticeCardItem
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
        if (granted) onEvaluateNotification()
    }

    LaunchedEffect(Unit) {
        AttendanceNotificationHelper.ensureChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(data, hasNotifPermission) {
        if (data != null && hasNotifPermission) onEvaluateNotification()
    }

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

        Surface(
            color = colors.cardSurface,
            border = BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(6.dp), color = colors.skyContainer) {
                                Text(
                                    text = "ROLL #${data?.roll ?: uiState.activeRoll}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
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
                                    Icon(Icons.Rounded.FamilyRestroom, contentDescription = null, tint = colors.violetAccent, modifier = Modifier.size(13.dp))
                                    Text(stringResource(R.string.parent_portal_badge), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = colors.violetAccent)
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
                        CheckUpdateIconButton(uiState.isCheckingUpdate, uiState.availableUpdate != null, onCheckUpdate)
                        IconButton(onClick = onRefresh, enabled = !uiState.isRefreshingParent, modifier = Modifier.testTag("parent_refresh_button")) {
                            Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_refresh), tint = colors.skyAccent)
                        }
                        IconButton(onClick = onLogout, modifier = Modifier.testTag("parent_logout_button")) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = stringResource(R.string.action_logout), tint = colors.redAccent)
                        }
                    }
                }
                if (uiState.isRefreshingParent) {
                    LinearProgressIndicator(color = colors.skyAccent, trackColor = colors.cardElevated, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).widthIn(max = 640.dp).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.parentBannerError != null) {
                item { InlineAlertCard(message = uiState.parentBannerError, isError = true) }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.amberContainer,
                        border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Rounded.NotificationsOff, contentDescription = null, tint = colors.amberAccent)
                                Text("Enable notifications for Absent & Half-Day alerts.", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Enable") }
                        }
                    }
                }
            }

            if (data != null) {
                if (data.pinnedBroadcast != null) item { PinnedPrincipalBroadcastBanner(notice = data.pinnedBroadcast) }
                if (data.todayHoliday != null) item { ActiveHolidayBanner(holiday = data.todayHoliday) }
                item {
                    AttendanceFinalizationBanner(data.attendanceClosed, data.attendanceDate, data.closedAt)
                }

                item {
                    val effectiveStatus = if (data.todayHoliday != null) "HOLIDAY" else data.attendance.todayStatus
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.cardSurface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth().testTag("parent_today_status_card")
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Ward's Attendance Today", style = MaterialTheme.typography.titleMedium)
                                    Text("Date: ${data.attendanceDate}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily), color = colors.textMuted)
                                }
                                AttendanceStatusPill(status = effectiveStatus)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ParentTimeTile("MORNING IN", IstDateTimeFormatter.formatTimeOnly(data.attendance.inT), colors.emeraldAccent, Modifier.weight(1f))
                                ParentTimeTile("AFTERNOON OUT", IstDateTimeFormatter.formatTimeOnly(data.attendance.outT), colors.amberAccent, Modifier.weight(1f))
                            }
                            if (uiState.parentNotificationDispatched) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = colors.emeraldAccent, modifier = Modifier.size(16.dp))
                                    Text("Status alert sent to your device.", style = MaterialTheme.typography.labelSmall, color = colors.emeraldAccent)
                                }
                            }
                        }
                    }
                }

                item {
                    ParentAttendanceRingCard(data.attendance.pct, data.attendance.present, data.attendance.half, data.attendance.total)
                }

                // Digitally Signed Exam Marks for Ward
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Grade, contentDescription = null, tint = colors.amberAccent)
                        Text("Ward's Digitally Signed Exam Marks", style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (data.monthlyMarks.isEmpty()) {
                    item {
                        Surface(shape = RoundedCornerShape(14.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                            Text("No exam marks published yet.", style = MaterialTheme.typography.bodySmall, color = colors.textMuted, modifier = Modifier.padding(14.dp))
                        }
                    }
                } else {
                    items(data.monthlyMarks, key = { "par_mk_${it.id}_${it.monthLabel}" }) { mark ->
                        TraineeSignedMarkCard(mark = mark)
                    }
                }

                if (uiState.parentAlertSuccessMessage != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.emeraldContainer,
                            border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.emeraldAccent)
                                    Text(uiState.parentAlertSuccessMessage, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = onDismissFeedback) { Icon(Icons.Rounded.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp)) }
                            }
                        }
                    }
                }

                if (uiState.parentAlertError != null) {
                    item { InlineAlertCard(message = uiState.parentAlertError, isError = true) }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.cardSurface,
                        border = BorderStroke(1.5.dp, colors.redAccent.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(colors.redContainer), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.ReportProblem, contentDescription = null, tint = colors.redAccent, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text("Report Bunking / Left Home", style = MaterialTheme.typography.titleMedium)
                                    Text("Alert Trade Instructor immediately if ward left home but is absent", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                                }
                            }
                            OutlinedTextField(
                                value = bunkNote,
                                onValueChange = { bunkNote = it },
                                label = { Text("Optional Note (English / Manglish / മലയാളം)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("parent_bunk_note_input")
                            )
                            Button(
                                onClick = { onSubmitBunkAlert(bunkNote) { bunkNote = "" } },
                                enabled = !uiState.isSubmittingParentAlert,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.redAccent, contentColor = MaterialTheme.colorScheme.onError),
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("parent_bunk_alert_button")
                            ) {
                                if (uiState.isSubmittingParentAlert) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onError, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                } else {
                                    Text(stringResource(R.string.action_report_bunking), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.cardSurface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(colors.skyContainer), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Sms, contentDescription = null, tint = colors.skyAccent, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text("Submit Leave Reason", style = MaterialTheme.typography.titleMedium)
                                    Text("Supports English, Manglish & മലയാളം", style = MaterialTheme.typography.bodySmall, color = colors.skyAccent)
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(QUICK_MALAYALAM_LEAVE_REASONS) { suggestion ->
                                    FilterChip(
                                        selected = leaveReasonText == suggestion,
                                        onClick = { leaveReasonText = suggestion },
                                        label = { Text(suggestion, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = leaveReasonText,
                                onValueChange = { leaveReasonText = it },
                                label = { Text("Leave Reason") },
                                minLines = 2,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("parent_leave_reason_input")
                            )
                            Button(
                                onClick = { onSubmitLeaveReason(leaveReasonText) { leaveReasonText = "" } },
                                enabled = !uiState.isSubmittingParentAlert && leaveReasonText.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("parent_submit_leave_reason_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Send Leave Reason to Instructor", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (data.parentAlerts.isNotEmpty()) {
                    item { Text("Recent Parent Submissions", style = MaterialTheme.typography.titleMedium) }
                    items(data.parentAlerts, key = { it.id.ifBlank { "${it.date}_${it.time}_${it.hashCode()}" } }) { alert ->
                        ParentAlertCard(alert = alert)
                    }
                }

                if (data.notices.isNotEmpty()) {
                    item { Text("Latest College & Trade Notices", style = MaterialTheme.typography.titleMedium) }
                    items(data.notices.take(3), key = { "parent_notice_${it.id}" }) { notice ->
                        NoticeCardItem(notice = notice, isUnread = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentTimeTile(label: String, timeValue: String, accentColor: Color, modifier: Modifier = Modifier) {
    val colors = LocalItiColors.current
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.background, border = BorderStroke(1.dp, colors.borderSubtle), modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.Sensors, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                Text(label, style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily), color = colors.textMuted)
            }
            Text(timeValue.ifBlank { "-" }, style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun ParentAttendanceRingCard(pct: Int, present: Int, half: Int, total: Int) {
    val colors = LocalItiColors.current
    val clamped = pct.coerceIn(0, 100)
    val animatedSweep by animateFloatAsState(targetValue = (clamped / 100f) * 360f, animationSpec = tween(800), label = "parent_ring")
    val ringColors = when {
        clamped >= 80 -> listOf(colors.skyAccent, colors.emeraldAccent)
        clamped >= 75 -> listOf(colors.amberAccent, colors.skyAccent)
        else -> listOf(colors.redAccent, colors.amberAccent)
    }
    Surface(shape = RoundedCornerShape(20.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(98.dp)) {
                val trackColor = colors.borderSubtle
                Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(brush = Brush.sweepGradient(ringColors), startAngle = -90f, sweepAngle = animatedSweep, useCenter = false, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
                }
                Text("$clamped%", style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Overall Attendance", style = MaterialTheme.typography.titleMedium)
                Text("Present: $present • Half-Day: $half • Total: $total", style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily), color = colors.textMuted)
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
fun ParentAlertCard(alert: ParentAlertItem, modifier: Modifier = Modifier) {
    val colors = LocalItiColors.current
    val typeUpper = alert.alertType.uppercase()
    val accent = when {
        typeUpper == ParentAlertTypes.BUNK_ALERT -> colors.redAccent
        typeUpper.contains("MARKS") -> colors.emeraldAccent
        else -> colors.skyAccent
    }
    Surface(shape = RoundedCornerShape(16.dp), color = colors.cardSurface, border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)), modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Roll #${alert.roll} • ${alert.studentName}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text(typeUpper.replace("_", " "), style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold), color = accent)
            }
            Text(alert.message, style = MaterialTheme.typography.bodySmall)
            Text(listOf(alert.date, alert.time).filter { it.isNotBlank() }.joinToString(" • "), style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily), color = colors.textMuted)
        }
    }
}
