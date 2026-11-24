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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AttendanceMath
import com.example.data.AttendanceUndoRecord
import com.example.data.EspTapResult
import com.example.data.IstDateTimeFormatter
import com.example.data.PendingLeaveRequest
import com.example.data.ShopFloorEntry
import com.example.data.StaffAttendanceRecord
import com.example.data.UserRole
import com.example.pdf.FullReportExportDialog
import com.example.ui.components.ActiveHolidayBanner
import com.example.ui.components.AttendanceStatusPill
import com.example.ui.components.DigitalSignaturePad
import com.example.ui.components.EspStatusBadge
import com.example.ui.components.ExternalUrlHelper
import com.example.ui.components.WhatsAppBotHelper
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

@Composable
fun PendingApprovalsTab(
    uiState: AppUiState,
    onApproveAndSign: (reqId: String, signature: String) -> Unit,
    onRejectLeave: (reqId: String, reason: String) -> Unit = { _, _ -> },
    onDismissApprovalFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val context = LocalContext.current
    val pendingLeaves = uiState.staffData?.pendingLeaves.orEmpty()
    val signerDefaultName = if (uiState.activeStaffRole == UserRole.PRINCIPAL) {
        "Principal, Govt. ITI Ranni"
    } else {
        "Trade Instructor (EM)"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (uiState.approvalSuccessMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.emeraldContainer,
                    border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("approval_success_banner")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                    text = uiState.approvalSuccessMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = onDismissApprovalFeedback) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Dismiss",
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (!uiState.approvalPdfUrl.isNullOrBlank()) {
                            Button(
                                onClick = { ExternalUrlHelper.openUrl(context, uiState.approvalPdfUrl) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.emeraldAccent,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("open_official_signed_pdf_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Open Official Signed PDF",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.approvalError != null) {
            item {
                InlineAlertCard(message = uiState.approvalError, isError = true)
            }
        }

        if (pendingLeaves.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(colors.emeraldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TaskAlt,
                                contentDescription = null,
                                tint = colors.emeraldAccent,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(
                            text = "All Caught Up",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "No pending out-pass or half-day leave requests awaiting ${uiState.activeStaffRole.displayName} approval.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }
            }
        } else {
            items(pendingLeaves, key = { it.reqId.ifBlank { "${it.roll}_${it.date}_${it.hashCode()}" } }) { request ->
                PendingApprovalCard(
                    request = request,
                    defaultSignerName = signerDefaultName,
                    isSigning = uiState.signingReqId == request.reqId,
                    anySigningInProgress = uiState.signingReqId != null,
                    onApprove = { sig -> onApproveAndSign(request.reqId, sig) },
                    onReject = { reason -> onRejectLeave(request.reqId, reason) }
                )
            }
        }
    }
}

@Composable
private fun PendingApprovalCard(
    request: PendingLeaveRequest,
    defaultSignerName: String,
    isSigning: Boolean,
    anySigningInProgress: Boolean,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    val colors = LocalItiColors.current
    var expandedSignPad by remember(request.reqId) { mutableStateOf(false) }
    var signature by remember(request.reqId) { mutableStateOf("") }
    var resetCounter by remember(request.reqId) { mutableIntStateOf(0) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_request_card_${request.reqId}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        color = colors.skyContainer
                    ) {
                        Text(
                            text = "ROLL #${request.roll}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.skyAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = request.reqId,
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                        color = colors.textMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.amberContainer,
                    border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = request.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.amberAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = request.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccessTime,
                        contentDescription = null,
                        tint = colors.skyAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${request.date}  •  ${request.timeWindow}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = colors.skyAccent
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "REASON",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = colors.textMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = request.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (!expandedSignPad) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { expandedSignPad = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.skyAccent.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.skyAccent),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("open_sign_pad_${request.reqId}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Draw,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign & Approve")
                    }
                    OutlinedButton(
                        onClick = { onReject("Rejected by $defaultSignerName") },
                        enabled = !anySigningInProgress,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.redAccent.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.redAccent),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("reject_request_${request.reqId}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Reject Request",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject")
                    }
                }
            } else {
                DigitalSignaturePad(
                    defaultSignerName = defaultSignerName,
                    resetKey = resetCounter,
                    onSignatureChanged = { signature = it },
                    title = "Authorized Digital Signature",
                    tagPrefix = "staff_sig_${request.reqId}"
                )

                Button(
                    onClick = { onApprove(signature) },
                    enabled = !anySigningInProgress,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.emeraldAccent,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("approve_sign_button_${request.reqId}")
                ) {
                    if (isSigning) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onSecondary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Signing & Submitting...")
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Approve & Digitally Sign",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

private val STATUS_FILTERS = listOf("ALL", "PRESENT", "HALF-DAY", "ABSENT")

@Composable
fun AttendanceRegisterTab(
    uiState: AppUiState,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (String) -> Unit,
    onExportPdf: () -> Unit,
    onConfirmCloseAttendance: () -> Unit,
    onDismissCloseFeedback: () -> Unit,
    onBulkMorningRollCall: () -> Unit,
    onMarkException: (roll: String, exceptionType: String) -> Unit,
    onManualPunch: (roll: String, status: String) -> Unit = { r, s -> onMarkException(r, s) },
    onConfirmDuplicatePunch: () -> Unit = {},
    onDismissDuplicatePunch: () -> Unit = {},
    onUndoAttendance: (roll: String) -> Unit = {},
    onResetAttendanceToAbsent: (roll: String, studentName: String) -> Unit = { _, _ -> },
    onSetLateGrace: (roll: String, mode: String, cutoff: String, note: String) -> Unit,
    onDismissLateGraceFeedback: () -> Unit,
    onSendInstantParentAlert: (roll: String, name: String, alertType: String, msg: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val context = LocalContext.current
    val staffData = uiState.staffData
    val allRecords = staffData?.allAttendance.orEmpty()
    val attendanceClosed = staffData?.attendanceClosed ?: false
    val todayHoliday = staffData?.todayHoliday

    var showCloseConfirmDialog by remember { mutableStateOf(false) }
    var undoConfirmTarget by remember { mutableStateOf<StaffAttendanceRecord?>(null) }
    var showFullPdfDialog by remember { mutableStateOf(false) }

    val absentCount = remember(allRecords) {
        allRecords.count { it.todayStatus.uppercase().let { s -> s == "ABSENT" || s.isBlank() } }
    }
    val halfDayCount = remember(allRecords) {
        allRecords.count { it.todayStatus.uppercase().contains("HALF") }
    }
    val presentCount = remember(allRecords) {
        allRecords.count { it.todayStatus.uppercase() == "PRESENT" }
    }

    val filteredRecords = remember(allRecords, uiState.registerSearchQuery, uiState.registerStatusFilter) {
        val query = uiState.registerSearchQuery.trim().lowercase()
        allRecords.filter { record ->
            val matchesQuery = query.isEmpty() ||
                record.name.lowercase().contains(query) ||
                record.roll.lowercase().contains(query)
            val statusUpper = record.todayStatus.uppercase()
            val matchesStatus = when (uiState.registerStatusFilter) {
                "PRESENT" -> statusUpper == "PRESENT"
                "HALF-DAY" -> statusUpper.contains("HALF") || statusUpper.contains("OUT-PASS") || statusUpper.contains("OUTPASS")
                "ABSENT" -> statusUpper == "ABSENT" || statusUpper.isBlank()
                else -> true
            }
            matchesQuery && matchesStatus
        }
    }

    if (showCloseConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCloseConfirmDialog = false },
            containerColor = colors.cardSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = colors.textMuted,
            title = {
                Text(
                    text = "Confirm & Close Today's Attendance?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Date: ${staffData?.attendanceDate.orEmpty()}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                        color = colors.skyAccent
                    )
                    Text(
                        text = "Present: $presentCount  •  Half-Day: $halfDayCount  •  Absent: $absentCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (todayHoliday != null) {
                            "Note: Today is a declared holiday (${todayHoliday.reason}). ABSENT alerts will be skipped."
                        } else {
                            "Once closed, parent status-bar notifications will be enabled for trainees marked ABSENT ($absentCount) or HALF-DAY ($halfDayCount)."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCloseConfirmDialog = false
                        onConfirmCloseAttendance()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.emeraldAccent,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.testTag("confirm_close_dialog_ok")
                ) {
                    Text("Confirm & Close")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseConfirmDialog = false }) {
                    Text("Cancel", color = colors.textMuted)
                }
            }
        )
    }

    // Duplicate Same-Day Punch Guard Dialog ("Mark again (by mistake)" Override)
    val dupPrompt = uiState.duplicatePunchPrompt
    if (dupPrompt != null) {
        AlertDialog(
            onDismissRequest = onDismissDuplicatePunch,
            containerColor = colors.cardSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = colors.textMuted,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = colors.amberAccent,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Already Punched Today",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Roll #${dupPrompt.roll} (${dupPrompt.studentName}) already has an attendance entry today:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.amberContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Existing: ${dupPrompt.existingStatus} (IN: ${dupPrompt.existingInTime})\nRequested: ${dupPrompt.requestedStatus}",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = colors.amberAccent,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Text(
                        text = "One punch per trainee per day is enforced by default to prevent accidental duplicates. Tap below only if you intentionally want to override.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmDuplicatePunch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.amberAccent,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    modifier = Modifier.testTag("duplicate_punch_override_button")
                ) {
                    Text("Mark again (by mistake)")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDuplicatePunch,
                    modifier = Modifier.testTag("duplicate_punch_cancel_button")
                ) {
                    Text("Keep Existing", color = colors.textMuted)
                }
            }
        )
    }

    // Undo Attendance Confirmation Dialog ("Reset [Trainee Name] (Roll #[X]) to ABSENT for today?")
    val undoTarget = undoConfirmTarget
    if (undoTarget != null) {
        AlertDialog(
            onDismissRequest = { undoConfirmTarget = null },
            containerColor = colors.cardSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = colors.textMuted,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Undo,
                    contentDescription = null,
                    tint = colors.amberAccent,
                    modifier = Modifier.size(26.dp)
                )
            },
            title = {
                Text(
                    text = "Undo Attendance?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Reset ${undoTarget.name} (Roll #${undoTarget.roll}) to ABSENT for today?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = undoTarget
                        undoConfirmTarget = null
                        onResetAttendanceToAbsent(target.roll, target.name)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.redAccent,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_undo_attendance_ok")
                ) {
                    Text("Reset to ABSENT")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { undoConfirmTarget = null },
                    modifier = Modifier.testTag("confirm_undo_attendance_cancel")
                ) {
                    Text("Cancel", color = colors.textMuted)
                }
            }
        )
    }

    if (showFullPdfDialog && staffData != null) {
        FullReportExportDialog(
            staffData = staffData,
            roleLabel = "Trade Instructor (EM)",
            onDismiss = { showFullPdfDialog = false }
        )
    }

    val shopFloorByRoll = remember(staffData?.shopFloorList) {
        staffData?.shopFloorList.orEmpty().associateBy { it.roll.trim() }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (staffData?.pinnedBroadcast != null) {
            item {
                com.example.ui.components.PinnedPrincipalBroadcastBanner(notice = staffData.pinnedBroadcast)
            }
        }

        if (todayHoliday != null) {
            item {
                ActiveHolidayBanner(holiday = todayHoliday)
            }
        }

        // 1-Tap Bulk Morning Roll Call & Live Scanner Status Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.skyAccent.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bulk_roll_call_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Morning Roll Call & Live Scanner",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ESP8266 Sync: ${staffData?.lastScannerSyncTime?.ifBlank { "Online" } ?: "Online"} • Present: $presentCount • Absent: $absentCount",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = colors.skyAccent
                            )
                        }
                        Button(
                            onClick = onBulkMorningRollCall,
                            enabled = !uiState.isRunningRollCallOp && !attendanceClosed,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.skyAccent,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("bulk_morning_roll_call_button")
                        ) {
                            Text(
                                text = if (uiState.isRunningRollCallOp) "Working..." else "1-Tap Bulk Present",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    AnimatedVisibility(visible = uiState.rollCallFeedbackMessage != null) {
                        InlineAlertCard(message = uiState.rollCallFeedbackMessage.orEmpty(), isError = false)
                    }
                    AnimatedVisibility(visible = uiState.rollCallError != null) {
                        InlineAlertCard(message = uiState.rollCallError.orEmpty(), isError = true)
                    }

                    // Top Quick-Undo Action Bar for Most Recent Manual Punch / Status Change
                    val lastUndo = uiState.lastUndoRecord
                    AnimatedVisibility(visible = lastUndo != null) {
                        if (lastUndo != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.amberContainer,
                                border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("top_undo_attendance_bar")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Roll #${lastUndo.roll} (${lastUndo.studentName}) → ${lastUndo.newStatus}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Changed at ${lastUndo.changedAtLabel} • Restore to ${lastUndo.previousStatus}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.amberAccent
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onUndoAttendance(lastUndo.roll) },
                                        enabled = uiState.undoingRoll == null && !uiState.isRunningRollCallOp,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.amberAccent,
                                            contentColor = MaterialTheme.colorScheme.onTertiary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("top_undo_attendance_button")
                                    ) {
                                        Icon(Icons.Rounded.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (uiState.undoingRoll == lastUndo.roll) "..." else "Undo",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Late Grace Quick-Mark Table (Scrollable Trainee Table with 1-Tap 09:30 Grace, Exempt, Clear)
        item {
            LateGraceQuickMarkTableCard(
                uiState = uiState,
                records = allRecords,
                onSetLateGrace = onSetLateGrace,
                onDismissFeedback = onDismissLateGraceFeedback
            )
        }

        // Instructor Confirm & Close Today's Attendance Control Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.cardSurface,
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (attendanceClosed) colors.emeraldAccent.copy(alpha = 0.65f) else colors.amberAccent.copy(alpha = 0.65f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("close_attendance_control_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (attendanceClosed) colors.emeraldContainer else colors.amberContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (attendanceClosed) Icons.Rounded.Verified else Icons.Rounded.LockClock,
                                    contentDescription = null,
                                    tint = if (attendanceClosed) colors.emeraldAccent else colors.amberAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (attendanceClosed) {
                                        "Today's Attendance Confirmed & Closed"
                                    } else {
                                        "Today's Attendance: OPEN"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (attendanceClosed) colors.emeraldAccent else colors.amberAccent
                                )
                                val timeSuffix = if (!staffData?.closedAt.isNullOrBlank()) " at ${staffData?.closedAt}" else ""
                                Text(
                                    text = if (attendanceClosed) {
                                        "Finalized on ${staffData?.attendanceDate}$timeSuffix. Absent & Half-Day parent notifications unlocked."
                                    } else {
                                        "Absent: $absentCount • Half-Day: $halfDayCount • Notifications held until confirmed."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }

                    if (!attendanceClosed) {
                        Button(
                            onClick = { showCloseConfirmDialog = true },
                            enabled = !uiState.isClosingAttendance,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.emeraldAccent,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("confirm_close_attendance_button")
                        ) {
                            if (uiState.isClosingAttendance) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Confirming & Closing...")
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.action_confirm_close_attendance),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = uiState.closeAttendanceMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.emeraldContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = uiState.closeAttendanceMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.emeraldAccent,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onDismissCloseFeedback) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = colors.textMuted)
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = uiState.closeAttendanceError != null) {
                        InlineAlertCard(message = uiState.closeAttendanceError.orEmpty(), isError = true)
                    }
                }
            }
        }

        // Export Full Multi-Section PDF Report + Server Register PDF
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showFullPdfDialog = true },
                    enabled = staffData != null,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.skyAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("export_full_pdf_report_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Print / Export Full App PDF Report",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = onExportPdf,
                    enabled = !uiState.isExportingPdf,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.skyAccent.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.skyAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("export_attendance_pdf_button")
                ) {
                    if (uiState.isExportingPdf) {
                        CircularProgressIndicator(
                            color = colors.skyAccent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating Server Attendance Sheet PDF...")
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export Server Daily Attendance PDF",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }

        if (!uiState.exportedPdfUrl.isNullOrBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.emeraldContainer,
                    border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Daily Attendance PDF Ready",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.emeraldAccent
                        )
                        OutlinedButton(
                            onClick = { ExternalUrlHelper.openUrl(context, uiState.exportedPdfUrl) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, colors.emeraldAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.emeraldAccent),
                            modifier = Modifier.testTag("reopen_exported_pdf_button")
                        ) {
                            Text("Open PDF")
                        }
                    }
                }
            }
        }

        if (uiState.exportPdfError != null) {
            item {
                InlineAlertCard(message = uiState.exportPdfError, isError = true)
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = uiState.registerSearchQuery,
                onValueChange = onSearchChanged,
                placeholder = { Text("Search trainee by Name or Roll No...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = colors.skyAccent
                    )
                },
                trailingIcon = {
                    if (uiState.registerSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChanged("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = colors.textMuted
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.skyAccent,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedContainerColor = colors.cardSurface,
                    unfocusedContainerColor = colors.cardSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_search_input")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(STATUS_FILTERS) { filter ->
                    val selected = uiState.registerStatusFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { onStatusFilterChanged(filter) },
                        label = { Text(filter, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.skyContainer,
                            selectedLabelColor = colors.skyAccent,
                            containerColor = colors.cardSurface,
                            labelColor = colors.textMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = colors.borderSubtle,
                            selectedBorderColor = colors.skyAccent
                        )
                    )
                }
            }
        }

        if (filteredRecords.isEmpty()) {
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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No matching trainees found",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting your search text or status filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }
            }
        } else {
            items(filteredRecords, key = { "${it.roll}_${it.name}" }) { record ->
                AttendanceRegisterRowCard(
                    record = record,
                    shopFloorEntry = shopFloorByRoll[record.roll.trim()],
                    undoRecord = uiState.recentUndoByRoll[record.roll.trim()],
                    isBusy = uiState.isRunningRollCallOp || uiState.manualPunchingRoll == record.roll,
                    isUndoing = uiState.undoingRoll == record.roll,
                    onManualPunch = { status -> onManualPunch(record.roll, status) },
                    onRequestResetToAbsent = { undoConfirmTarget = record },
                    onSendInstantParentAlert = { alertType ->
                        onSendInstantParentAlert(record.roll, record.name, alertType, "")
                    }
                )
            }
        }
    }
}

@Composable
private fun AttendanceRegisterRowCard(
    record: StaffAttendanceRecord,
    shopFloorEntry: ShopFloorEntry? = null,
    undoRecord: AttendanceUndoRecord? = null,
    isBusy: Boolean,
    isUndoing: Boolean = false,
    onManualPunch: (String) -> Unit,
    onRequestResetToAbsent: () -> Unit = {},
    onSendInstantParentAlert: (String) -> Unit
) {
    val colors = LocalItiColors.current
    val context = LocalContext.current
    val clampedPct = record.pct.coerceIn(0, 100)
    val pctColor = when {
        clampedPct >= 80 -> colors.emeraldAccent
        clampedPct >= 75 -> colors.amberAccent
        else -> colors.redAccent
    }
    val formattedInT = IstDateTimeFormatter.formatTimeOnly(record.inT)
    val formattedOutT = IstDateTimeFormatter.formatTimeOnly(record.outT)
    val canUndoToday = record.hasPunchedToday ||
        (formattedInT != "-") ||
        (formattedOutT != "-")
    val isAbsent = record.todayStatus.equals("ABSENT", ignoreCase = true) || record.todayStatus.isBlank()
    val isLowAttendance = clampedPct < 80
    val neededClasses = record.sessionsNeededFor80Pct

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(
            1.dp,
            if (undoRecord != null) colors.amberAccent.copy(alpha = 0.55f) else colors.borderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("register_row_${record.roll}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Roll + Name + Quick 1-Tap Front-Panel Punch Button + Percentage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.skyContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = record.roll.padStart(2, '0'),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.skyAccent
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = record.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "P: ${record.present} • H: ${record.half} • Total: ${record.total}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = colors.textMuted
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quick 1-Tap Front-Panel Manual Punch right beside Trainee Name
                    Button(
                        onClick = { onManualPunch("PRESENT") },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (record.hasPunchedToday) colors.emeraldContainer else colors.emeraldAccent,
                            contentColor = if (record.hasPunchedToday) colors.emeraldAccent else MaterialTheme.colorScheme.onSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("front_punch_present_${record.roll}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = "Quick manual punch present",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (record.hasPunchedToday) "Punched" else "Punch",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = pctColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, pctColor.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "${clampedPct}%",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = pctColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Status Pill + IST IN/OUT times + Compact Undo button for any punched row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttendanceStatusPill(status = record.todayStatus)
                    if (canUndoToday) {
                        OutlinedButton(
                            onClick = onRequestResetToAbsent,
                            enabled = !isBusy && !isUndoing,
                            shape = RoundedCornerShape(9.dp),
                            border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = colors.amberContainer,
                                contentColor = colors.amberAccent
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("row_undo_button_${record.roll}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Undo,
                                contentDescription = "Undo Attendance",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isUndoing) "..." else "Undo",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "IN: $formattedInT",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = colors.emeraldAccent
                    )
                    Text(
                        text = "OUT: $formattedOutT",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = colors.amberAccent
                    )
                }
            }

            // Shop Floor Status/Hours alongside Needed Classes for 80% Attendance
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_shop_floor_80_strip_${record.roll}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val sfLabel = if (shopFloorEntry != null) {
                        val hrs = AttendanceMath.formatShopFloorHours(shopFloorEntry.durationMinutes)
                        "Shop Floor: ${shopFloorEntry.status} ($hrs)"
                    } else {
                        "Shop Floor: None (0.0 hrs)"
                    }
                    Text(
                        text = sfLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = if (shopFloorEntry != null) colors.skyAccent else colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (neededClasses > 0) "Need +$neededClasses for 80%" else "80% Met ✓",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (neededClasses > 0) colors.amberAccent else colors.emeraldAccent
                    )
                }
            }

            // Quick Manual Status Chips (LATE / HALF-DAY / ABSENT / ON-DUTY) + Parent Alert + WhatsApp
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(listOf("LATE", "HALF-DAY", "ABSENT", "ON-DUTY")) { exType ->
                    val isCurrent = record.todayStatus.equals(exType, ignoreCase = true)
                    FilterChip(
                        selected = isCurrent,
                        enabled = !isBusy,
                        onClick = { onManualPunch(exType) },
                        label = { Text(exType, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (exType == "ABSENT") colors.redContainer else colors.amberContainer,
                            selectedLabelColor = if (exType == "ABSENT") colors.redAccent else colors.amberAccent,
                            containerColor = MaterialTheme.colorScheme.background,
                            labelColor = colors.textMuted
                        ),
                        modifier = Modifier.testTag("quick_ex_${record.roll}_${exType.lowercase()}")
                    )
                }

                if (isAbsent || isLowAttendance) {
                    item {
                        val alertType = if (isLowAttendance && !isAbsent) {
                            com.example.data.ParentAlertTypes.LOW_ATTENDANCE_WARNING
                        } else {
                            com.example.data.ParentAlertTypes.ABSENCE_ALERT
                        }
                        val label = if (alertType == com.example.data.ParentAlertTypes.LOW_ATTENDANCE_WARNING) {
                            "Alert Parent (<80%)"
                        } else {
                            "Alert Parent (Absent)"
                        }
                        FilterChip(
                            selected = true,
                            enabled = !isBusy,
                            onClick = { onSendInstantParentAlert(alertType) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.redContainer,
                                selectedLabelColor = colors.redAccent
                            ),
                            modifier = Modifier.testTag("instant_parent_alert_${record.roll}")
                        )
                    }
                }

                item {
                    FilterChip(
                        selected = false,
                        enabled = !isBusy,
                        onClick = {
                            val waMsg = "Govt. ITI Ranni (EM) • Attendance Update\n" +
                                "Trainee: ${record.name} (Roll #${record.roll})\n" +
                                "Today Status: ${record.todayStatus} (IN: ${record.inT})\n" +
                                "Cumulative Attendance: $clampedPct% (P:${record.present}, H:${record.half}, Total:${record.total})" +
                                if (neededClasses > 0) "\nSessions needed to reach 80%: $neededClasses" else ""
                            WhatsAppBotHelper.openWhatsAppBot(context, waMsg)
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Chat, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.emeraldAccent)
                        },
                        label = { Text("WhatsApp", style = MaterialTheme.typography.labelSmall, color = colors.emeraldAccent) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = colors.emeraldContainer,
                            labelColor = colors.emeraldAccent
                        ),
                        modifier = Modifier.testTag("row_whatsapp_button_${record.roll}")
                    )
                }
            }
        }
    }
}

@Composable
fun EspTerminalTab(
    uiState: AppUiState,
    onRollChanged: (String) -> Unit,
    onTriggerManualTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.emeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Memory,
                                    contentDescription = null,
                                    tint = colors.emeraldAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "ESP8266 + IR Sensor Bridge",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tape-Strip Optical ID Card Terminal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                        }
                        EspStatusBadge(label = "ONLINE")
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ENDPOINT: GET /exec?action=tap&key=EMECH_KEY_2026&roll=<ROLL>",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = colors.skyAccent
                            )
                            Text(
                                text = "• 1st Swipe -> Logs Morning IN Time (PRESENT)\n• 2nd Swipe -> Logs Afternoon OUT Time (HALF-DAY)",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
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
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Nfc,
                            contentDescription = null,
                            tint = colors.skyAccent
                        )
                        Column {
                            Text(
                                text = "Manual / Backup Card Punch",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Trigger a live punch if the physical IR scanner is offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    }

                    OutlinedTextField(
                        value = uiState.espRollInput,
                        onValueChange = onRollChanged,
                        label = { Text("Trainee Roll Number (1–23)") },
                        placeholder = { Text("e.g., 12") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Badge,
                                contentDescription = null,
                                tint = colors.skyAccent
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onTriggerManualTap() }),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.skyAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("esp_roll_input")
                    )

                    AnimatedVisibility(visible = uiState.espTapError != null) {
                        InlineAlertCard(
                            message = uiState.espTapError.orEmpty(),
                            isError = true
                        )
                    }

                    Button(
                        onClick = onTriggerManualTap,
                        enabled = !uiState.isTappingEsp,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.emeraldAccent,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("esp_manual_tap_button")
                    ) {
                        if (uiState.isTappingEsp) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onSecondary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Sending ESP8266 Punch...")
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.TouchApp,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Simulate / Manual ESP Tap",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        if (uiState.lastEspTap != null) {
            item {
                EspTapReadoutCard(
                    result = uiState.lastEspTap,
                    isLatest = true
                )
            }
        }

        if (uiState.espTapHistory.size > 1) {
            item {
                Text(
                    text = "RECENT MANUAL PUNCHES",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(uiState.espTapHistory.drop(1)) { historyItem ->
                EspTapReadoutCard(result = historyItem, isLatest = false)
            }
        }
    }
}

@Composable
private fun EspTapReadoutCard(
    result: EspTapResult,
    isLatest: Boolean
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isLatest) MaterialTheme.colorScheme.background else colors.cardSurface,
        border = BorderStroke(
            width = if (isLatest) 1.5.dp else 1.dp,
            color = if (isLatest) colors.skyAccent else colors.borderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (isLatest) "esp_latest_readout_card" else "esp_history_card_${result.roll}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isLatest) "LIVE ESP8266 RESPONSE" else "LOGGED PUNCH",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = if (isLatest) colors.skyAccent else colors.textMuted
                )
                AttendanceStatusPill(status = result.mode)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Roll #${result.roll}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = colors.textMuted
                    )
                }
                Text(
                    text = result.time,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.emeraldAccent
                )
            }
        }
    }
}
