package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AcademicSubmissionStatuses
import com.example.data.AttendanceApiClient
import com.example.data.AttendanceMath
import com.example.data.DutyRotationEngine
import com.example.data.LateGraceModes
import com.example.data.ParentAlertTypes
import com.example.data.PendingLeaveRequest
import com.example.data.StaffAttendanceRecord
import com.example.data.TimetablePeriod
import com.example.data.TimetableStore
import com.example.ui.components.AttendanceStatusPill
import com.example.ui.components.DigitalSignaturePad
import com.example.ui.components.InlineAlertCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

private val STATUS_FILTERS = listOf("ALL", "PRESENT", "ABSENT", "HALF", "BELOW 80%")

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
    val data = uiState.staffData
    val query = uiState.registerSearchQuery.trim()
    val filter = uiState.registerStatusFilter

    var graceTarget by remember { mutableStateOf<StaffAttendanceRecord?>(null) }
    var graceCutoff by remember { mutableStateOf("09:45 AM") }
    var graceNote by remember { mutableStateOf("") }
    var alertTarget by remember { mutableStateOf<StaffAttendanceRecord?>(null) }
    var alertMsg by remember { mutableStateOf("") }

    if (uiState.duplicatePunchPrompt != null) {
        val dup = uiState.duplicatePunchPrompt
        AlertDialog(
            onDismissRequest = onDismissDuplicatePunch,
            title = { Text("Override Existing Status?") },
            text = {
                Text("${dup.studentName} (Roll #${dup.roll}) is already marked ${dup.currentStatus}. Override with ${dup.requestedStatus}?")
            },
            confirmButton = {
                Button(onClick = onConfirmDuplicatePunch) { Text("Override") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDuplicatePunch) { Text("Cancel") }
            }
        )
    }

    if (graceTarget != null) {
        val target = graceTarget!!
        AlertDialog(
            onDismissRequest = { graceTarget = null },
            title = { Text("Late Grace: ${target.name} (#${target.roll})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = graceCutoff, onValueChange = { graceCutoff = it }, label = { Text("Custom Cutoff (e.g. 09:45 AM)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = graceNote, onValueChange = { graceNote = it }, label = { Text("Note (e.g. Bus Delay)") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onSetLateGrace(target.roll, LateGraceModes.EXEMPT_TODAY, "", graceNote); graceTarget = null }, modifier = Modifier.weight(1f)) {
                            Text("Exempt Today")
                        }
                        OutlinedButton(onClick = { onSetLateGrace(target.roll, LateGraceModes.CLEAR, "", ""); graceTarget = null }, modifier = Modifier.weight(1f)) {
                            Text("Clear")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onSetLateGrace(target.roll, LateGraceModes.CUSTOM_CUTOFF, graceCutoff, graceNote); graceTarget = null }) {
                    Text("Apply Cutoff")
                }
            },
            dismissButton = { TextButton(onClick = { graceTarget = null }) { Text("Close") } }
        )
    }

    if (alertTarget != null) {
        val target = alertTarget!!
        AlertDialog(
            onDismissRequest = { alertTarget = null },
            title = { Text("Notify Parent of #${target.roll}") },
            text = {
                OutlinedTextField(
                    value = alertMsg,
                    onValueChange = { alertMsg = it },
                    label = { Text("Urgent Message to Parent") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = alertMsg.isNotBlank(),
                    onClick = {
                        onSendInstantParentAlert(target.roll, target.name, ParentAlertTypes.INSTRUCTOR_WARNING, alertMsg)
                        alertMsg = ""
                        alertTarget = null
                    }
                ) { Text("Send Alert") }
            },
            dismissButton = { TextButton(onClick = { alertTarget = null }) { Text("Cancel") } }
        )
    }

    val filtered = remember(data?.allAttendance, query, filter) {
        data?.allAttendance.orEmpty().filter { rec ->
            val matchesQuery = query.isBlank() ||
                rec.name.contains(query, ignoreCase = true) ||
                rec.roll.contains(query, ignoreCase = true)
            val upper = rec.todayStatus.uppercase()
            val matchesFilter = when (filter) {
                "PRESENT" -> upper == "PRESENT" || upper == "LATE" || upper == "ON-DUTY"
                "ABSENT" -> upper == "ABSENT"
                "HALF" -> upper.contains("HALF") || upper.contains("OUT-PASS")
                "BELOW 80%" -> rec.total > 0 && rec.pct < AttendanceMath.EXAM_THRESHOLD_PCT
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("staff_register_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.widthIn(max = 720.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onBulkMorningRollCall,
                        enabled = !uiState.isApplyingBulkRollCall,
                        modifier = Modifier.weight(1f).testTag("bulk_roll_call_button")
                    ) {
                        Text(if (uiState.isApplyingBulkRollCall) "Applying..." else "Bulk Present", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onDismissCloseFeedback(); onConfirmCloseAttendance() },
                        enabled = !uiState.isClosingAttendance,
                        modifier = Modifier.weight(1f).testTag("close_attendance_button")
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (data?.attendanceClosed == true) "Closed" else "Close Day", maxLines = 1)
                    }
                    OutlinedButton(onClick = onExportPdf, enabled = !uiState.isExportingPdf) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Export Server PDF", modifier = Modifier.size(16.dp))
                    }
                }

                if (uiState.closeAttendanceMessage != null) InlineAlertCard(uiState.closeAttendanceMessage, isError = false)
                if (uiState.closeAttendanceError != null) InlineAlertCard(uiState.closeAttendanceError, isError = true)
                if (uiState.manualPunchMessage != null) InlineAlertCard(uiState.manualPunchMessage, isError = false)
                if (uiState.manualPunchError != null) InlineAlertCard(uiState.manualPunchError, isError = true)

                OutlinedTextField(
                    value = uiState.registerSearchQuery,
                    onValueChange = onSearchChanged,
                    label = { Text("Search Trainee Name or Roll #") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("register_search_input")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(STATUS_FILTERS) { st ->
                        FilterChip(
                            selected = uiState.registerStatusFilter == st,
                            onClick = { onStatusFilterChanged(st) },
                            label = { Text(st) }
                        )
                    }
                }
            }
        }

        items(filtered, key = { "st_reg_${it.roll}" }) { rec ->
            val pctColor = if (rec.pct >= AttendanceMath.EXAM_THRESHOLD_PCT) colors.emeraldAccent else colors.redAccent
            val undoEntry = uiState.undoMap[rec.roll]
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("#${rec.roll.padStart(2, '0')} ${rec.name}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "In: ${rec.inT} • Out: ${rec.outT} • ${rec.present}P/${rec.half}H/${rec.total}D",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = colors.textMuted
                            )
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AttendanceStatusPill(rec.todayStatus)
                            Text("${rec.pct}%", style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold), color = pctColor)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { onManualPunch(rec.roll, "PRESENT") }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                            Text("P", style = MaterialTheme.typography.labelMedium, color = colors.emeraldAccent)
                        }
                        OutlinedButton(onClick = { onManualPunch(rec.roll, "HALF-DAY") }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                            Text("HD", style = MaterialTheme.typography.labelMedium, color = colors.amberAccent)
                        }
                        OutlinedButton(onClick = { onResetAttendanceToAbsent(rec.roll, rec.name) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                            Text("A", style = MaterialTheme.typography.labelMedium, color = colors.redAccent)
                        }
                        OutlinedButton(onClick = { graceTarget = rec }, modifier = Modifier.weight(1.2f), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                            Text("Grace", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(onClick = { alertTarget = rec }, modifier = Modifier.weight(1.2f), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)) {
                            Text("Alert", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (undoEntry != null && uiState.remoteAppConfig.undoEnabled && uiState.remoteUiConfig.showUndoButton) {
                        TextButton(onClick = { onUndoAttendance(rec.roll) }, contentPadding = PaddingValues(0.dp)) {
                            Icon(Icons.Rounded.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Undo (Restore ${undoEntry.previousStatus})", style = MaterialTheme.typography.labelSmall)
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
    val data = uiState.staffData
    var approvingReq by remember { mutableStateOf<PendingLeaveRequest?>(null) }
    var rejectingReq by remember { mutableStateOf<PendingLeaveRequest?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    var sigData by remember { mutableStateOf("") }
    var sigResetKey by remember { mutableIntStateOf(0) }

    if (approvingReq != null) {
        val req = approvingReq!!
        AlertDialog(
            onDismissRequest = { approvingReq = null },
            title = { Text("Sign & Approve Leave (${req.reqId})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${req.name} (#${req.roll}) • ${req.type} • ${req.timeWindow}", style = MaterialTheme.typography.bodySmall)
                    DigitalSignaturePad(
                        defaultSignerName = "Trade Instructor (EM)",
                        resetKey = sigResetKey,
                        onSignatureChanged = { sigData = it },
                        title = "Instructor Signature",
                        tagPrefix = "sir_sig"
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = sigData.isNotBlank() && !uiState.isSigningApproval,
                    onClick = {
                        onDismissApprovalFeedback()
                        onApproveAndSign(req.reqId, sigData)
                        approvingReq = null
                        sigData = ""
                        sigResetKey++
                    }
                ) { Text("Approve & Forward") }
            },
            dismissButton = { TextButton(onClick = { approvingReq = null }) { Text("Cancel") } }
        )
    }

    if (rejectingReq != null) {
        val req = rejectingReq!!
        AlertDialog(
            onDismissRequest = { rejectingReq = null },
            title = { Text("Reject Leave (${req.reqId})") },
            text = {
                OutlinedTextField(value = rejectReason, onValueChange = { rejectReason = it }, label = { Text("Rejection Reason") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(
                    enabled = rejectReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.redAccent),
                    onClick = {
                        onDismissApprovalFeedback()
                        onRejectLeave(req.reqId, rejectReason)
                        rejectReason = ""
                        rejectingReq = null
                    }
                ) { Text("Reject") }
            },
            dismissButton = { TextButton(onClick = { rejectingReq = null }) { Text("Cancel") } }
        )
    }

    val pending = data?.pendingLeaves.orEmpty()
    val lateAlerts = data?.runningLateAlerts.orEmpty()
    val parentAlerts = data?.parentAlerts.orEmpty()
    val submissions = uiState.academicSubmissions.ifEmpty { data?.academicSubmissions.orEmpty() }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("staff_approvals_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (uiState.approvalSuccessMessage != null) item { InlineAlertCard(uiState.approvalSuccessMessage, isError = false) }
        if (uiState.approvalError != null) item { InlineAlertCard(uiState.approvalError, isError = true) }

        item { Text("Pending Out-Pass / Leave Requests (${pending.size})", style = MaterialTheme.typography.titleMedium) }
        if (pending.isEmpty()) {
            item { Text("No pending leave requests.", style = MaterialTheme.typography.bodySmall, color = colors.textMuted) }
        } else {
            items(pending, key = { "st_lv_${it.reqId}" }) { req ->
                Surface(shape = RoundedCornerShape(16.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("#${req.roll} ${req.name} • ${req.type}", style = MaterialTheme.typography.titleSmall)
                        Text("${req.date} • ${req.timeWindow} • ${req.reason}", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { approvingReq = req }, modifier = Modifier.weight(1f)) { Text("Sign & Approve") }
                            OutlinedButton(onClick = { rejectingReq = req }, modifier = Modifier.weight(1f)) { Text("Reject", color = colors.redAccent) }
                        }
                    }
                }
            }
        }

        if (lateAlerts.isNotEmpty()) {
            item { Text("Running Late Alerts (${lateAlerts.size})", style = MaterialTheme.typography.titleMedium) }
            items(lateAlerts, key = { "st_rl_${it.id}" }) { rl ->
                Surface(shape = RoundedCornerShape(14.dp), color = colors.amberContainer, border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("#${rl.roll} ${rl.studentName} • ETA ${rl.expectedArrival}", style = MaterialTheme.typography.titleSmall, color = colors.amberAccent)
                        Text("${rl.reasonCategory}: ${rl.details}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (parentAlerts.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = colors.redAccent)
                    Text("Parent Alerts (${parentAlerts.size})", style = MaterialTheme.typography.titleMedium)
                }
            }
            items(parentAlerts, key = { "st_pa_${it.id}" }) { al ->
                Surface(shape = RoundedCornerShape(14.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("#${al.roll} ${al.studentName} • ${al.alertType}", style = MaterialTheme.typography.titleSmall, color = colors.redAccent)
                        Text("${al.date} ${al.time}: ${al.message}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            HorizontalDivider(color = colors.borderSubtle)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Trainee Log Books (${submissions.size})", style = MaterialTheme.typography.titleMedium)
                Row {
                    IconButton(onClick = onRefreshAcademicSubmissions) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Log Books", tint = colors.skyAccent)
                    }
                    TextButton(onClick = onForwardAllAcademicToPrincipal) { Text("Forward All Verified") }
                }
            }
        }

        items(submissions, key = { "st_ac_${it.id}_${it.roll}" }) { sub ->
            var remark by remember(sub.id) { mutableStateOf(sub.instructorRemark) }
            Surface(shape = RoundedCornerShape(14.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("#${sub.roll} ${sub.studentName} • ${sub.verificationStatus}", style = MaterialTheme.typography.titleSmall)
                    Text("Theory: ${sub.theoryTopic} (${sub.theoryStatus}) • Lab: ${sub.labJobName} (${sub.labStatus})", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                    if (sub.verificationStatus == AcademicSubmissionStatuses.PENDING_VERIFICATION) {
                        OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text("Verification Remark") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onVerifyAcademicSubmission(sub.roll, sub.id, remark) }, modifier = Modifier.weight(1f)) { Text("Verify") }
                            OutlinedButton(onClick = { onForwardSingleAcademicToPrincipal(sub.roll) }, modifier = Modifier.weight(1f)) { Text("Forward") }
                        }
                    } else if (!sub.sentToPrincipal) {
                        OutlinedButton(onClick = { onForwardSingleAcademicToPrincipal(sub.roll) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Forward Verified Log Book to Principal")
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
    var endTime by remember { mutableStateOf("05:00 PM") }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("staff_shop_floor_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Build, contentDescription = null, tint = colors.amberAccent)
                        Text(uiState.remoteUiConfig.shopFloorTabTitle, style = MaterialTheme.typography.titleMedium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Allow Early Class Logout Override", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Switch(checked = overrides?.earlyClassLogoutEnabled ?: false, onCheckedChange = onToggleEarlyClassLogout)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Release Post-3PM Shop Floor Now", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Switch(checked = overrides?.shopFloorReleaseNowEnabled ?: false, onCheckedChange = onToggleShopFloorReleaseNow)
                    }
                    if (uiState.shopFloorMessage != null) InlineAlertCard(uiState.shopFloorMessage, isError = false)
                    if (uiState.shopFloorError != null) InlineAlertCard(uiState.shopFloorError, isError = true)
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(16.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add Trainee to Post-3PM Remedial", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = manualRoll, onValueChange = { manualRoll = it.filter(Char::isDigit).take(2) }, label = { Text("Roll #") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("End Time") }, modifier = Modifier.weight(1.4f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = manualRoll.isNotBlank(), onClick = { onAddOrRemoveShopFloor(manualRoll, "ADD", "POST_3PM", endTime); manualRoll = "" }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Assign")
                        }
                        OutlinedButton(enabled = manualRoll.isNotBlank(), onClick = { onAddOrRemoveShopFloor(manualRoll, "REMOVE", "POST_3PM", endTime); manualRoll = "" }, modifier = Modifier.weight(1f)) {
                            Text("Remove")
                        }
                    }
                }
            }
        }

        val list = data?.shopFloorList.orEmpty()
        item { Text("Assigned Shop Floor Trainees (${list.size})", style = MaterialTheme.typography.titleMedium) }
        items(list, key = { "st_sf_${it.id}_${it.roll}" }) { sf ->
            Surface(shape = RoundedCornerShape(14.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("#${sf.roll} ${sf.name} (${sf.attendancePct}%)", style = MaterialTheme.typography.titleSmall)
                    Text("${sf.assignedReason} • End: ${sf.scheduledEndTime} • Status: ${sf.status}", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onRecordCardLogout(sf.roll) }, modifier = Modifier.weight(1f)) { Text("Card Out") }
                        Button(onClick = { onVerifyLogout(sf.roll, sf.date) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Verify")
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
    val data = uiState.staffData

    var noticeTitle by remember { mutableStateOf("") }
    var noticeCat by remember { mutableStateOf("General") }
    var noticeBody by remember { mutableStateOf("") }

    var holidayDate by remember { mutableStateOf(AttendanceApiClient.todayIstDateString()) }
    var holidayReason by remember { mutableStateOf("") }

    val dutyToday = remember(data?.dutyOverrides, data?.holidays) {
        DutyRotationEngine.assignmentForDate(overrides = data?.dutyOverrides.orEmpty(), holidays = data?.holidays.orEmpty())
    }
    var dutyNote by remember { mutableStateOf("") }
    var ovRoll1 by remember { mutableStateOf(dutyToday.firstRoll.toString()) }
    var ovRoll2 by remember { mutableStateOf(dutyToday.secondRoll.toString()) }

    var toolItem by remember { mutableStateOf("") }
    var toolCondition by remember { mutableStateOf("DAMAGED") }
    var toolNotes by remember { mutableStateOf("") }

    val timetablePeriods = remember(uiState.activeTimetable) {
        mutableStateListOf<TimetablePeriod>().apply { addAll(uiState.activeTimetable.ifEmpty { TimetableStore.defaultWeeklyTimetable() }) }
    }
    var allowTraineeTtEdit by remember(data?.traineeTimetableEditEnabled) { mutableStateOf(data?.traineeTimetableEditEnabled ?: false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("staff_campus_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(shape = RoundedCornerShape(18.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Sensors, contentDescription = null, tint = colors.emeraldAccent)
                        Text("ESP8266 Manual Card Tap Simulator", style = MaterialTheme.typography.titleSmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = uiState.espRollInput,
                            onValueChange = onEspRollChanged,
                            label = { Text("Roll (1-23)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = onTriggerManualTap, enabled = !uiState.isTriggeringEspTap) {
                            Text(if (uiState.isTriggeringEspTap) "Tapping..." else "Tap Card")
                        }
                    }
                    if (uiState.espTapMessage != null) InlineAlertCard(uiState.espTapMessage, isError = false)
                    if (uiState.espTapError != null) InlineAlertCard(uiState.espTapError, isError = true)
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(18.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Campaign, contentDescription = null, tint = colors.skyAccent)
                        Text("Publish Campus Notice", style = MaterialTheme.typography.titleSmall)
                    }
                    OutlinedTextField(value = noticeTitle, onValueChange = { noticeTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = noticeCat, onValueChange = { noticeCat = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = noticeBody, onValueChange = { noticeBody = it }, label = { Text("Notice Details") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    if (uiState.noticePostMessage != null) InlineAlertCard(uiState.noticePostMessage, isError = false)
                    if (uiState.noticePostError != null) InlineAlertCard(uiState.noticePostError, isError = true)
                    Button(
                        enabled = !uiState.isPostingNotice && noticeTitle.isNotBlank() && noticeBody.isNotBlank(),
                        onClick = { onPostNotice(noticeTitle, noticeCat, noticeBody) { noticeTitle = ""; noticeBody = "" } },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Post Notice") }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(18.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.EventBusy, contentDescription = null, tint = colors.violetAccent)
                        Text("Declare Official Holiday", style = MaterialTheme.typography.titleSmall)
                    }
                    OutlinedTextField(value = holidayDate, onValueChange = { holidayDate = it }, label = { Text("Date (dd-MM-yyyy)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = holidayReason, onValueChange = { holidayReason = it }, label = { Text("Reason") }, modifier = Modifier.fillMaxWidth())
                    if (uiState.holidayPostMessage != null) InlineAlertCard(uiState.holidayPostMessage, isError = false)
                    if (uiState.holidayPostError != null) InlineAlertCard(uiState.holidayPostError, isError = true)
                    Button(
                        enabled = !uiState.isPostingHoliday && holidayReason.isNotBlank(),
                        onClick = { onPostHoliday(holidayDate, holidayReason) { holidayReason = "" } },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Publish Holiday") }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(18.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Daily Duty Rotation (${dutyToday.dateKey}): ${dutyToday.pairLabel}", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(value = dutyNote, onValueChange = { dutyNote = it }, label = { Text("Duty Sign-Off Note") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = { onSignOffDuty(dutyToday.dateKey, dutyToday.pairLabel, dutyNote) { dutyNote = "" } },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sign Off Today's Duty") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = ovRoll1, onValueChange = { ovRoll1 = it.filter(Char::isDigit).take(2) }, label = { Text("Roll 1") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = ovRoll2, onValueChange = { ovRoll2 = it.filter(Char::isDigit).take(2) }, label = { Text("Roll 2") }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onDismissDutyOverrideFeedback()
                                onSaveDutyOverride(dutyToday.dateKey, ovRoll1.toIntOrNull() ?: 1, ovRoll2.toIntOrNull() ?: 2, dutyNote, false, false)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Override Pair") }
                        OutlinedButton(
                            onClick = {
                                onDismissDutyOverrideFeedback()
                                onSaveDutyOverride(dutyToday.dateKey, 1, 2, "", false, true)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Reset Auto") }
                    }
                    if (uiState.dutyOverrideMessage != null) InlineAlertCard(uiState.dutyOverrideMessage, isError = false)
                    if (uiState.dutyOverrideError != null) InlineAlertCard(uiState.dutyOverrideError, isError = true)
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(18.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Groups, contentDescription = null, tint = colors.skyAccent)
                            Text("Project Groups & Timetable", style = MaterialTheme.typography.titleSmall)
                        }
                        IconButton(onClick = onRefreshProjectGroups) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Groups") }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Allow Trainees to Form Project Groups", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Switch(checked = data?.projectGroupsEnabled ?: false, onCheckedChange = onToggleProjectGroupsAccess)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Allow Trainees to Edit Shared Timetable", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Switch(checked = allowTraineeTtEdit, onCheckedChange = { allowTraineeTtEdit = it })
                    }
                    Button(onClick = { onDismissTimetableFeedback(); onPublishTimetable(timetablePeriods.toList(), allowTraineeTtEdit) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Publish Official Timetable Settings")
                    }
                    if (uiState.timetableFeedbackMessage != null) InlineAlertCard(uiState.timetableFeedbackMessage, isError = false)
                    if (uiState.timetableError != null) InlineAlertCard(uiState.timetableError, isError = true)
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(18.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tool Crib Equipment Log", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(value = toolItem, onValueChange = { toolItem = it }, label = { Text("Instrument / Tool Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = toolCondition, onValueChange = { toolCondition = it }, label = { Text("Condition (DAMAGED / MISSING / CALIBRATION)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = toolNotes, onValueChange = { toolNotes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        enabled = toolItem.isNotBlank(),
                        onClick = { onLogToolCrib(toolItem, toolCondition, toolNotes) { toolItem = ""; toolNotes = "" } },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Log Tool Crib Entry") }
                }
            }
        }
    }
}
