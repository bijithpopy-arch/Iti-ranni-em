package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AttendanceMath
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

@Composable
fun ParentDashboardScreen(
    uiState: AppUiState,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onEvaluateNotification: () -> Unit,
    onSubmitBunkAlert: (details: String, onSuccessReset: () -> Unit) -> Unit,
    onSubmitLeaveReason: (reason: String, onSuccessReset: () -> Unit) -> Unit,
    onDismissFeedback: () -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onLogout() }

    val colors = LocalItiColors.current
    val data = uiState.parentData
    var bunkDetails by remember { mutableStateOf("") }
    var leaveReason by remember { mutableStateOf("") }

    LaunchedEffect(data?.attendanceDate, data?.monthlyMarks) {
        if (data != null) onEvaluateNotification()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(6.dp), color = colors.violetContainer) {
                                Text(
                                    text = "PARENT • ROLL #${data?.roll ?: uiState.activeRoll}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                                    color = colors.violetAccent,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = data?.studentName ?: "Ward Portal",
                            style = MaterialTheme.typography.titleLarge,
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
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.borderSubtle))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (uiState.parentBannerError != null) {
                    item { InlineAlertCard(uiState.parentBannerError, isError = true) }
                }

                if (data != null) {
                    if (data.pinnedBroadcast != null) {
                        item { PinnedPrincipalBroadcastBanner(data.pinnedBroadcast) }
                    }
                    if (data.todayHoliday != null) {
                        item { ActiveHolidayBanner(data.todayHoliday) }
                    }
                    item {
                        AttendanceFinalizationBanner(data.attendanceClosed, data.attendanceDate, data.closedAt)
                    }

                    item {
                        val att = data.attendance
                        val pctColor = if (att.pct >= AttendanceMath.EXAM_THRESHOLD_PCT) colors.emeraldAccent else colors.redAccent
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colors.cardSurface,
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Rounded.FamilyRestroom, contentDescription = null, tint = colors.violetAccent)
                                        Text("Ward Attendance (${data.attendanceDate})", style = MaterialTheme.typography.titleMedium)
                                    }
                                    AttendanceStatusPill(if (data.todayHoliday != null) "HOLIDAY" else att.todayStatus)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Check-In: ${att.inT} • Out: ${att.outT}", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                                    Text("${att.pct}% (${att.present}P/${att.half}H/${att.total}D)", style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold), color = pctColor)
                                }
                            }
                        }
                    }

                    if (data.wardShopFloorEntry != null) {
                        item {
                            val sf = data.wardShopFloorEntry
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.amberContainer,
                                border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Post-3PM Shop Floor Assignment", style = MaterialTheme.typography.titleSmall, color = colors.amberAccent)
                                    Text("${sf.assignedReason} • Scheduled till ${sf.scheduledEndTime} • Status: ${sf.status}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colors.cardSurface,
                            border = BorderStroke(1.dp, colors.redAccent.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = colors.redAccent)
                                    Text("Parent-to-Instructor Alerts", style = MaterialTheme.typography.titleMedium)
                                }
                                OutlinedTextField(
                                    value = bunkDetails,
                                    onValueChange = { bunkDetails = it },
                                    label = { Text("Report Urgent Bunk / Off-Campus Concern") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        onDismissFeedback()
                                        onSubmitBunkAlert(bunkDetails) { bunkDetails = "" }
                                    },
                                    enabled = !uiState.isSubmittingParentAlert,
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.redAccent, contentColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (uiState.isSubmittingParentAlert) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                    } else {
                                        Text(stringResource(R.string.action_report_bunking), fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedTextField(
                                    value = leaveReason,
                                    onValueChange = { leaveReason = it },
                                    label = { Text("Submit Official Leave / Absence Reason") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedButton(
                                    onClick = {
                                        onDismissFeedback()
                                        onSubmitLeaveReason(leaveReason) { leaveReason = "" }
                                    },
                                    enabled = !uiState.isSubmittingParentAlert && leaveReason.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Send Leave Reason to Instructor")
                                }

                                AnimatedVisibility(visible = uiState.parentAlertMessage != null) {
                                    InlineAlertCard(uiState.parentAlertMessage.orEmpty(), isError = false)
                                }
                                AnimatedVisibility(visible = uiState.parentAlertError != null) {
                                    InlineAlertCard(uiState.parentAlertError.orEmpty(), isError = true)
                                }
                            }
                        }
                    }

                    if (data.monthlyMarks.isNotEmpty()) {
                        item { Text("Digitally Signed Exam Marks", style = MaterialTheme.typography.titleMedium) }
                        items(data.monthlyMarks, key = { "pr_mk_${it.id}_${it.monthLabel}" }) { mark ->
                            TraineeSignedMarkCard(mark = mark)
                        }
                    }

                    if (data.parentAlerts.isNotEmpty()) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = colors.amberAccent)
                                Text("Recent Alert Log (${data.parentAlerts.size})", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        items(data.parentAlerts, key = { "pr_al_${it.id}" }) { al ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.cardSurface,
                                border = BorderStroke(1.dp, colors.borderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("${al.alertType} • ${al.date} ${al.time}", style = MaterialTheme.typography.labelSmall, color = colors.amberAccent)
                                    Text(al.message, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    if (data.notices.isNotEmpty()) {
                        item { Text("Campus Notices", style = MaterialTheme.typography.titleMedium) }
                        items(data.notices, key = { "pr_ntc_${it.id}" }) { notice ->
                            NoticeCardItem(notice = notice, isUnread = false)
                        }
                    }
                }
            }
        }
    }
}
