package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AcademicSubmissionStatuses
import com.example.data.AttendanceApiClient
import com.example.data.DutyRotationEngine
import com.example.data.LateGraceModes
import com.example.data.ParentAlertTypes
import com.example.data.PendingLeaveRequest
import com.example.data.StaffAttendanceRecord
import com.example.data.TimetablePeriod
import com.example.notifications.AttendanceNotificationHelper
import com.example.ui.components.AttendanceFinalizationBanner
import com.example.ui.components.AttendanceStatusPill
import com.example.ui.components.DigitalSignaturePad
import com.example.ui.components.InlineAlertCard
import com.example.ui.components.NoticeCardItem
import com.example.ui.components.WhatsAppBotHelper
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

private val REGISTER_FILTERS = listOf("ALL", "PRESENT", "HALF-DAY", "ABSENT", "OUT-PASS")
private val NOTICE_CATEGORIES = listOf("General", "Exam", "Workshop", "Urgent", "DGT / NCVT", "Holiday")

private enum class ApprovalsSubSection(val label: String) {
    OUT_PASS("Out-Pass Approvals"),
    ALERTS("Late & Parent Alerts"),
    LOG_BOOKS("Log Books Verify")
}

private enum class CampusSubSection(val label: String) {
    DUTY_TOOLS("Duty/Tools"),
    TIMETABLE("Timetable"),
    NOTICES("Notices"),
    HOLIDAYS("Holidays"),
    PROJECTS("Projects"),
    ESP("ESP Terminal")
}

@Composable
fun AttendanceRegisterTab(
    uiState: AppUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (String) -> Unit,
    onExportPdf: () -> Unit,
    onConfirmCloseAttendance: () -> Unit,
    onDismissCloseFeedback: () -> Unit,
    onBulkMorningRollCall: () -> Unit,
    onManualPunch: (roll: String, status: String) -> Unit,
    onConfirmDuplicatePunch: () -> Unit,
    onDismissDuplicatePunch: () -> Unit,
    onUndoAttendance: (roll: String) -> Unit,
    onResetAttendanceToAbsent: (roll: String, studentName: String) -> Unit,
    onSetLateGrace: (roll: String, mode: String, cutoff: String, note: String) -> Unit,
    onSendInstantParentAlert: (roll: String, name: String, alertType: String, msg: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val staffData = uiState.staffData
    val list = staffData?.allAttendance.orEmpty()

    val filtered = remember(list, uiState.registerSearchQuery, uiState.registerStatusFilter) {
        list.filter { rec ->
            val matchQuery = uiState.registerSearchQuery.isBlank() ||
                rec.name.contains(uiState.registerSearchQuery, ignoreCase = true) ||
                rec.roll.contains(uiState.registerSearchQuery, ignoreCase = true)
            val matchFilter = uiState.registerStatusFilter == "ALL" ||
                rec.todayStatus.contains(uiState.registerStatusFilter, ignoreCase = true)
            matchQuery && matchFilter
        }.sortedBy { it.roll.toIntOrNull() ?: 99 }
    }

    var warningTarget by remember { mutableStateOf<StaffAttendanceRecord?>(null) }
    var warningMsg by remember { mutableStateOf("") }

    if (uiState.duplicatePunchPrompt != null) {
        val dup = uiState.duplicatePunchPrompt
        AlertDialog(
            onDismissRequest = onDismissDuplicatePunch,
            title = { Text("Duplicate Punch Guard") },
            text = {
                Text("Roll #${dup.roll} (${dup.studentName}) is already marked ${dup.currentStatus}. Override with ${dup.requestedStatus}?")
            },
            confirmButton = {
                Button(onClick = onConfirmDuplicatePunch) { Text("Override") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDuplicatePunch) { Text("Cancel") }
            }
        )
    }

    if (warningTarget != null) {
        val t = warningTarget!!
        AlertDialog(
            onDismissRequest = { warningTarget = null },
            title = { Text("Send Parent Alert • Roll #${t.roll}") },
            text = {
                OutlinedTextField(
                    value = warningMsg,
                    onValueChange = { warningMsg = it },
                    label = { Text("Warning / Attendance Message") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = warningMsg.isNotBlank(),
                    onClick = {
                        onSendInstantParentAlert(t.roll, t.name, ParentAlertTypes.INSTRUCTOR_WARNING, warningMsg)
                        warningMsg = ""
                        warningTarget = null
                    }
                ) { Text("Send Alert") }
            },
            dismissButton = {
                TextButton(onClick = { warningTarget = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("staff_attendance_register_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (staffData != null) {
            item {
                AttendanceFinalizationBanner(
                    attendanceClosed = staffData.attendanceClosed,
                    attendanceDate = staffData.attendanceDate,
                    closedAt = staffData.closedAt
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onBulkMorningRollCall,
                    enabled = !uiState.isRunningRollCallOp,
                    modifier = Modifier.weight(1f).testTag("bulk_roll_call_button")
                ) {
                    Text("Bulk Present", style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = { onDismissCloseFeedback(); onConfirmCloseAttendance() },
                    enabled = !uiState.isClosingAttendance && !(staffData?.attendanceClosed ?: false),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.emeraldAccent, contentColor = MaterialTheme.colorScheme.onSecondary),
                    modifier = Modifier.weight(1.2f).testTag("close_attendance_button")
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (staffData?.attendanceClosed == true) "Closed" else "Close Day", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onExportPdf,
                    enabled = !uiState.isExportingPdf,
                    modifier = Modifier.weight(0.8f).testTag("register_export_pdf_button")
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                }
            }
        }

        if (uiState.closeAttendanceMessage != null) item { InlineAlertCard(uiState.closeAttendanceMessage, false) }
        if (uiState.closeAttendanceError != null) item { InlineAlertCard(uiState.closeAttendanceError, true) }
        if (uiState.rollCallFeedbackMessage != null) item { InlineAlertCard(uiState.rollCallFeedbackMessage, false) }
        if (uiState.rollCallError != null) item { InlineAlertCard(uiState.rollCallError, true) }

        item {
            OutlinedTextField(
                value = uiState.registerSearchQuery,
                onValueChange = onSearchChanged,
                label = { Text("Search Roll # or Trainee Name") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.skyAccent) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("register_search_input")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(REGISTER_FILTERS) { filter ->
                    FilterChip(
                        selected = uiState.registerStatusFilter == filter,
                        onClick = { onStatusFilterChanged(filter) },
                        label = { Text(filter, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        items(filtered, key = { "reg_${it.roll}" }) { rec ->
            val hasUndo = uiState.recentUndoByRoll.containsKey(rec.roll)
            val pctColor = when {
                rec.pct >= 80 -> colors.emeraldAccent
                rec.pct >= 75 -> colors.amberAccent
                else -> colors.redAccent
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Roll #${rec.roll.padStart(2, '0')} • ${rec.name}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "In: ${rec.inT} • Out: ${rec.outT} • ${rec.pct}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = pctColor
                            )
                        }
                        AttendanceStatusPill(status = rec.todayStatus)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("PRESENT" to "P", "HALF-DAY" to "HD", "LATE" to "L", "ON-DUTY" to "OD").forEach { (status, short) ->
                            OutlinedButton(
                                onClick = { onManualPunch(rec.roll, status) },
                                enabled = uiState.manualPunchingRoll != rec.roll,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Text(short, style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold))
                            }
                        }
                        if (hasUndo && uiState.remoteUiConfig.showUndoButton) {
                            IconButton(onClick = { onUndoAttendance(rec.roll) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.Undo, contentDescription = "Undo", tint = colors.amberAccent, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            IconButton(onClick = { onResetAttendanceToAbsent(rec.roll, rec.name) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "Mark Absent", tint = colors.redAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(onClick = { warningTarget = rec; warningMsg = "Attendance follow-up needed for ${rec.name}." }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = "Warn Parent", tint = colors.skyAccent, modifier = Modifier.size(18.dp))
                        }
                    }

                    if (rec.lateGraceMode.isNotBlank()) {
                        Text("Late Grace: ${rec.lateGraceMode} (${rec.lateCutoff})", style = MaterialTheme.typography.labelSmall, color = colors.amberAccent)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onSetLateGrace(rec.roll, LateGraceModes.EXEMPT_TODAY, "", "Instructor granted today") }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                                Text("Grant Late Grace", style = MaterialTheme.typography.labelSmall, color = colors.skyAccent)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApprovalsAndAlertsTab(
    uiState: AppUiState,
    onApproveAndSign: (reqId: String, signature: String) -> Unit,
    onRejectLeave: (reqId: String, reason: String) -> Unit,
    onDismissApprovalFeedback: () -> Unit,
    onRefreshAcademicSubmissions: () -> Unit,
    onVerifyAcademicSubmission: (roll: String, submissionId: String, remark: String) -> Unit,
    onForwardSingleAcademicToPrincipal: (roll: String) -> Unit,
    onForwardAllAcademicToPrincipal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val context = LocalContext.current
    var activeSub by remember { mutableStateOf(ApprovalsSubSection.OUT_PASS) }

    val pendingLeaves = uiState.staffData?.pendingLeaves.orEmpty()
    val runningLate = uiState.staffData?.runningLateAlerts.orEmpty()
    val parentAlerts = uiState.staffData?.parentAlerts.orEmpty()
    val submissions = uiState.staffData?.academicSubmissions.orEmpty()

    var approvingLeave by remember { mutableStateOf<PendingLeaveRequest?>(null) }
    var rejectingLeave by remember { mutableStateOf<PendingLeaveRequest?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    var sigData by remember { mutableStateOf("") }
    var sigKey by remember { mutableIntStateOf(0) }

    if (approvingLeave != null) {
        val req = approvingLeave!!
        AlertDialog(
            onDismissRequest = { approvingLeave = null },
            title = { Text("Approve & Digitally Sign Out-Pass") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${req.name} (Roll #${req.roll}) • ${req.type}\nWindow: ${req.timeWindow}\nReason: ${req.reason}", style = MaterialTheme.typography.bodySmall)
                    DigitalSignaturePad(defaultSignerName = "Trade Instructor (EM)", resetKey = sigKey, onSignatureChanged = { sigData = it }, tagPrefix = "staff_leave_sig")
                }
            },
            confirmButton = {
                Button(enabled = sigData.isNotBlank(), onClick = {
                    onDismissApprovalFeedback()
                    onApproveAndSign(req.reqId, sigData)
                    approvingLeave = null
                    sigData = ""
                    sigKey++
                }) { Text("Sign & Forward") }
            },
            dismissButton = { TextButton(onClick = { approvingLeave = null }) { Text("Cancel") } }
        )
    }

    if (rejectingLeave != null) {
        val req = rejectingLeave!!
        AlertDialog(
            onDismissRequest = { rejectingLeave = null },
            title = { Text("Reject Leave Request") },
            text = { OutlinedTextField(value = rejectReason, onValueChange = { rejectReason = it }, label = { Text("Rejection Reason") }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(enabled = rejectReason.isNotBlank(), onClick = {
                    onDismissApprovalFeedback()
                    onRejectLeave(req.reqId, rejectReason)
                    rejectReason = ""
                    rejectingLeave = null
                }, colors = ButtonDefaults.buttonColors(containerColor = colors.redAccent)) { Text("Reject") }
            },
            dismissButton = { TextButton(onClick = { rejectingLeave = null }) { Text("Cancel") } }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("staff_approvals_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ApprovalsSubSection.entries) { sub ->
                    FilterChip(
                        selected = activeSub == sub,
                        onClick = { activeSub = sub },
                        label = {
                            val badge = when (sub) {
                                ApprovalsSubSection.OUT_PASS -> pendingLeaves.size
                                ApprovalsSubSection.ALERTS -> runningLate.size + parentAlerts.size
                                ApprovalsSubSection.LOG_BOOKS -> submissions.size
                            }
                            Text("${sub.label} ($badge)")
                        }
                    )
                }
            }
        }

        if (uiState.approvalSuccessMessage != null) item { InlineAlertCard(uiState.approvalSuccessMessage, false) }
        if (uiState.approvalError != null) item { InlineAlertCard(uiState.approvalError, true) }

        when (activeSub) {
            ApprovalsSubSection.OUT_PASS -> {
                if (pendingLeaves.isEmpty()) {
                    item { InlineAlertCard("No pending out-pass or leave approvals.", false) }
                } else {
                    items(pendingLeaves, key = { "st_lv_${it.reqId}" }) { req ->
                        Surface(shape = RoundedCornerShape(16.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Roll #${req.roll} • ${req.name}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text("${req.type} • ${req.timeWindow} • ${req.date}", style = MaterialTheme.typography.labelMedium, color = colors.skyAccent)
                                Text(req.reason, style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { rejectingLeave = req }, modifier = Modifier.weight(1f)) { Text("Reject", color = colors.redAccent) }
                                    Button(onClick = { approvingLeave = req }, modifier = Modifier.weight(1.2f)) { Text("Sign & Approve") }
                                }
                            }
                        }
                    }
                }
            }

            ApprovalsSubSection.ALERTS -> {
                items(runningLate, key = { "st_rl_${it.id}_${it.roll}" }) { late ->
                    Surface(shape = RoundedCornerShape(16.dp), color = colors.cardSurface, border = BorderStroke(1.5.dp, colors.amberAccent.copy(alpha = 0.6f)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.DirectionsBus, contentDescription = null, tint = colors.amberAccent, modifier = Modifier.size(16.dp))
                                Text("RUNNING LATE • Roll #${late.roll} ${late.studentName}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Text("ETA: ${late.expectedArrival} • ${late.reasonCategory}", style = MaterialTheme.typography.labelMedium, color = colors.amberAccent)
                            if (late.details.isNotBlank()) Text(late.details, style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = {
                                WhatsAppBotHelper.openWhatsAppBot(context, "Govt. ITI Ranni (EM) • Noted delay for ${late.studentName} (Roll #${late.roll}). Report to Instructor on arrival.")
                            }) {
                                Icon(Icons.Rounded.Chat, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Acknowledge via WhatsApp", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                items(parentAlerts, key = { "st_pa_${it.id}_${it.roll}" }) { alert ->
                    ParentAlertCard(alert = alert)
                }
                if (runningLate.isEmpty() && parentAlerts.isEmpty()) {
                    item { InlineAlertCard("No active Running Late or Parent alerts.", false) }
                }
            }

            ApprovalsSubSection.LOG_BOOKS -> {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = onRefreshAcademicSubmissions) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Refresh Records")
                        }
                        Button(onClick = onForwardAllAcademicToPrincipal, enabled = !uiState.isUpdatingAcademicSubmission) {
                            Text("Forward All Verified")
                        }
                    }
                }
                items(submissions, key = { "st_sub_${it.id}_${it.roll}" }) { sub ->
                    var remark by remember(sub.id) { mutableStateOf(sub.instructorRemark) }
                    Surface(shape = RoundedCornerShape(16.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Roll #${sub.roll} • ${sub.studentName} (${sub.verificationStatus})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Theory: ${sub.theoryTopic} (${sub.theoryStatus}) • Lab: ${sub.labJobName} (${sub.labStatus})", style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text("Instructor Remark") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onVerifyAcademicSubmission(sub.roll, sub.id, remark) }, modifier = Modifier.weight(1f)) { Text("Verify") }
                                Button(onClick = { onForwardSingleAcademicToPrincipal(sub.roll) }, modifier = Modifier.weight(1f)) { Text("Send Principal") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShopFloorControlTab(
    uiState: AppUiState,
    onToggleEarlyClassLogout: (Boolean) -> Unit,
    onToggleShopFloorReleaseNow: (Boolean) -> Unit,
    onAddOrRemoveShopFloor: (roll: String, operation: String, sessionType: String, endTime: String) -> Unit,
    onRecordCardLogout: (roll: String) -> Unit,
    onVerifyLogout: (roll: String, date: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val data = uiState.staffData
    val overrides = data?.scanOverrides
    var manualRoll by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("staff_shop_floor_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ESP8266 Scanner Overrides", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Allow Early Class Logout", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Switch(checked = overrides?.earlyClassLogoutEnabled ?: false, onCheckedChange = onToggleEarlyClassLogout)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Release Shop Floor Now", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Switch(checked = overrides?.shopFloorReleaseNowEnabled ?: false, onCheckedChange = onToggleShopFloorReleaseNow)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = manualRoll, onValueChange = { manualRoll = it.filter(Char::isDigit).take(2) }, label = { Text("Roll #1-23") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                        Button(enabled = manualRoll.toIntOrNull() in 1..23, onClick = { onAddOrRemoveShopFloor(manualRoll, "ADD", "POST_3PM", "05:00 PM"); manualRoll = "" }) { Text("Add") }
                    }
                }
            }
        }

        if (uiState.shopFloorActionMessage != null) item { InlineAlertCard(uiState.shopFloorActionMessage, false) }
        if (uiState.shopFloorActionError != null) item { InlineAlertCard(uiState.shopFloorActionError, true) }

        val list = data?.shopFloorList.orEmpty()
        item { Text("Post-3PM Shop Floor Roster (${list.size})", style = MaterialTheme.typography.titleMedium) }
        items(list, key = { "sf_${it.id}_${it.roll}" }) { entry ->
            Surface(shape = RoundedCornerShape(16.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Roll #${entry.roll} • ${entry.name}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text("${entry.attendancePct}%", style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily), color = colors.amberAccent)
                    }
                    Text("${entry.assignedReason} • End: ${entry.scheduledEndTime} • ${entry.status}", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onRecordCardLogout(entry.roll) }, modifier = Modifier.weight(1f)) { Text("Card Out", style = MaterialTheme.typography.labelSmall) }
                        Button(onClick = { onVerifyLogout(entry.roll, entry.date) }, enabled = !entry.verifiedByInstructor, modifier = Modifier.weight(1f)) {
                            Text(if (entry.verifiedByInstructor) "Verified" else "Verify", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { onAddOrRemoveShopFloor(entry.roll, "REMOVE", entry.sessionType, entry.scheduledEndTime) }) {
                            Text("Remove", color = colors.redAccent, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CampusManagementTab(
    uiState: AppUiState,
    onPostNotice: (title: String, category: String, body: String, onReset: () -> Unit) -> Unit,
    onPostHoliday: (date: String, reason: String, onReset: () -> Unit) -> Unit,
    onToggleProjectGroupsAccess: (Boolean) -> Unit,
    onRefreshProjectGroups: () -> Unit,
    onSignOffDuty: (date: String, pairLabel: String, note: String, onReset: () -> Unit) -> Unit,
    onLogToolCrib: (itemName: String, conditionStatus: String, notes: String, onReset: () -> Unit) -> Unit,
    onSaveDutyOverride: (dateKey: String, firstRoll: Int, secondRoll: Int, dutyNote: String, reanchorFromHere: Boolean, resetToAuto: Boolean) -> Unit,
    onDismissDutyOverrideFeedback: () -> Unit,
    onPublishTimetable: (periods: List<TimetablePeriod>, allowTraineeEdit: Boolean) -> Unit,
    onDismissTimetableFeedback: () -> Unit,
    onEspRollChanged: (String) -> Unit,
    onTriggerManualTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val context = LocalContext.current
    var activeSection by remember { mutableStateOf(CampusSubSection.DUTY_TOOLS) }
    var botPhoneInput by remember { mutableStateOf(WhatsAppBotHelper.getBotPhoneNumber(context)) }
    var botSavedBanner by remember { mutableStateOf<String?>(null) }

    var noticeTitle by remember { mutableStateOf("") }
    var noticeCategory by remember { mutableStateOf(NOTICE_CATEGORIES.first()) }
    var noticeBody by remember { mutableStateOf("") }
    var holidayDate by remember { mutableStateOf(AttendanceApiClient.todayIstDateString()) }
    var holidayReason by remember { mutableStateOf("") }

    val staffData = uiState.staffData

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("staff_campus_hub_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CampusSubSection.entries) { section ->
                    val selected = activeSection == section
                    FilterChip(
                        selected = selected,
                        onClick = { activeSection = section },
                        label = { Text(section.label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.skyAccent, selectedLabelColor = MaterialTheme.colorScheme.onPrimary)
                    )
                }
            }
        }

        when (activeSection) {
            CampusSubSection.DUTY_TOOLS -> {
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.45f)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("WhatsApp Bot & Loud Alert Tone", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                IconButton(onClick = { AttendanceNotificationHelper.playLoudAlertTone(context) }) {
                                    Icon(Icons.Rounded.VolumeUp, contentDescription = "Test alert tone", tint = colors.amberAccent)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(value = botPhoneInput, onValueChange = { botPhoneInput = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) }, label = { Text("WhatsApp Bot Number") }, singleLine = true, modifier = Modifier.weight(1f))
                                Button(onClick = { botPhoneInput = WhatsAppBotHelper.setBotPhoneNumber(context, botPhoneInput); botSavedBanner = "Saved wa.me/$botPhoneInput" }) { Text("Save") }
                            }
                            if (botSavedBanner != null) InlineAlertCard(botSavedBanner.orEmpty(), false)
                        }
                    }
                }
                item {
                    val todayDuty = DutyRotationEngine.assignmentForDate(overrides = staffData?.dutyOverrides.orEmpty(), holidays = staffData?.holidays.orEmpty())
                    var dutyNote by remember { mutableStateOf("") }
                    var r1 by remember { mutableStateOf(todayDuty.firstRoll.toString()) }
                    var r2 by remember { mutableStateOf(todayDuty.secondRoll.toString()) }
                    Surface(shape = RoundedCornerShape(18.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.CleaningServices, contentDescription = null, tint = colors.amberAccent)
                                Text("Today's Duty: ${todayDuty.pairLabel}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            OutlinedTextField(value = dutyNote, onValueChange = { dutyNote = it }, label = { Text("Sign-off Note") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Button(onClick = { onSignOffDuty(todayDuty.dateKey, todayDuty.pairLabel, dutyNote) { dutyNote = "" } }, modifier = Modifier.fillMaxWidth()) { Text("Sign Off Today's Cleaning Duty") }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = r1, onValueChange = { r1 = it.filter(Char::isDigit).take(2) }, label = { Text("Roll 1") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = r2, onValueChange = { r2 = it.filter(Char::isDigit).take(2) }, label = { Text("Roll 2") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                            if (uiState.dutyOverrideMessage != null) InlineAlertCard(uiState.dutyOverrideMessage, false)
                            OutlinedButton(onClick = {
                                onDismissDutyOverrideFeedback()
                                val a = (r1.toIntOrNull() ?: todayDuty.firstRoll).coerceIn(1, 23)
                                val b = (r2.toIntOrNull() ?: todayDuty.secondRoll).coerceIn(1, 23)
                                onSaveDutyOverride(todayDuty.dateKey, a, b, "Instructor Override", false, false)
                            }, modifier = Modifier.fillMaxWidth()) { Text("Override Today's Duty Pair") }
                        }
                    }
                }
                item {
                    var itemName by remember { mutableStateOf("") }
                    var condition by remember { mutableStateOf("DAMAGED") }
                    var notes by remember { mutableStateOf("") }
                    Surface(shape = RoundedCornerShape(18.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Build, contentDescription = null, tint = colors.skyAccent)
                                Text("Tool Crib Issue Log", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("Tool / Equipment Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("DAMAGED", "MISSING", "REPAIRED").forEach { st ->
                                    FilterChip(selected = condition == st, onClick = { condition = st }, label = { Text(st, style = MaterialTheme.typography.labelSmall) })
                                }
                            }
                            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Details") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Button(enabled = itemName.isNotBlank(), onClick = { onLogToolCrib(itemName, condition, notes) { itemName = ""; notes = "" } }, modifier = Modifier.fillMaxWidth()) { Text("Log Tool Issue") }
                        }
                    }
                }
            }

            CampusSubSection.TIMETABLE -> {
                item {
                    val base = staffData?.timetable?.ifEmpty { uiState.officialTimetableCache } ?: uiState.officialTimetableCache
                    val editable = remember(base) { mutableStateListOf<TimetablePeriod>().apply { addAll(base) } }
                    var allowEdit by remember(staffData?.traineeTimetableEditEnabled) { mutableStateOf(staffData?.traineeTimetableEditEnabled ?: false) }
                    Surface(shape = RoundedCornerShape(18.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = colors.skyAccent)
                                    Text("Weekly Timetable Editor", style = MaterialTheme.typography.titleMedium)
                                }
                                Switch(checked = allowEdit, onCheckedChange = { allowEdit = it })
                            }
                            editable.forEachIndexed { idx, p ->
                                OutlinedTextField(value = p.subject, onValueChange = { editable[idx] = p.copy(subject = it) }, label = { Text("${p.day} • ${p.slot}") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            }
                            if (uiState.timetableFeedbackMessage != null) InlineAlertCard(uiState.timetableFeedbackMessage, false)
                            Button(onClick = { onDismissTimetableFeedback(); onPublishTimetable(editable.toList(), allowEdit) }, enabled = !uiState.isSavingTimetable, modifier = Modifier.fillMaxWidth()) {
                                Text("Publish Timetable Live")
                            }
                        }
                    }
                }
            }

            CampusSubSection.NOTICES -> {
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Campaign, contentDescription = null, tint = colors.skyAccent)
                                Text("Publish Trade Notice", style = MaterialTheme.typography.titleMedium)
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(NOTICE_CATEGORIES) { cat ->
                                    FilterChip(selected = noticeCategory == cat, onClick = { noticeCategory = cat }, label = { Text(cat) })
                                }
                            }
                            OutlinedTextField(value = noticeTitle, onValueChange = { noticeTitle = it }, label = { Text("Notice Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = noticeBody, onValueChange = { noticeBody = it }, label = { Text("Body (English / Manglish / മലയാളം)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                            if (uiState.noticePostMessage != null) InlineAlertCard(uiState.noticePostMessage, false)
                            if (uiState.noticePostError != null) InlineAlertCard(uiState.noticePostError, true)
                            Button(onClick = { onPostNotice(noticeTitle, noticeCategory, noticeBody) { noticeTitle = ""; noticeBody = "" } }, enabled = !uiState.isPostingNotice && noticeTitle.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Publish Notice")
                            }
                        }
                    }
                }
                items(staffData?.notices.orEmpty(), key = { "hub_ntc_${it.id}" }) { notice -> NoticeCardItem(notice, false) }
            }

            CampusSubSection.HOLIDAYS -> {
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Celebration, contentDescription = null, tint = colors.amberAccent)
                                Text("Declare Holiday", style = MaterialTheme.typography.titleMedium)
                            }
                            OutlinedTextField(value = holidayDate, onValueChange = { holidayDate = it }, label = { Text("Holiday Date (DD-MM-YYYY)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = holidayReason, onValueChange = { holidayReason = it }, label = { Text("Holiday Reason (English / മലയാളം)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                            if (uiState.holidayPostMessage != null) InlineAlertCard(uiState.holidayPostMessage, false)
                            if (uiState.holidayPostError != null) InlineAlertCard(uiState.holidayPostError, true)
                            Button(onClick = { onPostHoliday(holidayDate, holidayReason) { holidayReason = "" } }, enabled = !uiState.isPostingHoliday && holidayReason.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = colors.amberAccent), modifier = Modifier.fillMaxWidth()) {
                                Text("Publish Holiday")
                            }
                        }
                    }
                }
            }

            CampusSubSection.PROJECTS -> {
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Enable Trainee Project Group Entry", style = MaterialTheme.typography.titleSmall)
                                Switch(checked = staffData?.projectGroupsEnabled ?: false, onCheckedChange = onToggleProjectGroupsAccess)
                            }
                            OutlinedButton(onClick = onRefreshProjectGroups, modifier = Modifier.fillMaxWidth()) { Text("Refresh Project Groups") }
                        }
                    }
                }
                items(staffData?.projectGroups.orEmpty(), key = { "st_pg_${it.groupId}" }) { pg ->
                    Surface(shape = RoundedCornerShape(14.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(pg.projectTitle, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Leader: ${pg.leaderName} (#${pg.leaderRoll}) • Members: ${pg.memberRolls.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                        }
                    }
                }
            }

            CampusSubSection.ESP -> {
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Sensors, contentDescription = null, tint = colors.emeraldAccent)
                                Text("ESP8266 RF Card Scan Terminal", style = MaterialTheme.typography.titleMedium)
                            }
                            OutlinedTextField(value = uiState.espRollInput, onValueChange = onEspRollChanged, label = { Text("Enter Trainee Roll #1-23") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth().testTag("esp_roll_input"))
                            Button(onClick = onTriggerManualTap, enabled = !uiState.isTappingEsp, colors = ButtonDefaults.buttonColors(containerColor = colors.emeraldAccent), modifier = Modifier.fillMaxWidth().testTag("esp_trigger_tap_button")) {
                                if (uiState.isTappingEsp) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Trigger RF Tap on Sheet")
                            }
                            if (uiState.espTapError != null) InlineAlertCard(uiState.espTapError, true)
                            if (uiState.lastEspTap != null) {
                                InlineAlertCard("Tapped Roll #${uiState.lastEspTap.roll} (${uiState.lastEspTap.name}): ${uiState.lastEspTap.mode} at ${uiState.lastEspTap.time}", false)
                            }
                        }
                    }
                }
                items(uiState.espTapHistory, key = { "esp_${it.roll}_${it.time}_${it.hashCode()}" }) { tap ->
                    Surface(shape = RoundedCornerShape(12.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                        Text("Roll #${tap.roll} • ${tap.name} -> ${tap.mode} (${tap.time})", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
    }
}
