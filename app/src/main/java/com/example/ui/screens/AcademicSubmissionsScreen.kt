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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ForwardToInbox
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Verified
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AcademicSubmission
import com.example.data.AcademicSubmissionStatuses
import com.example.data.IstDateTimeFormatter
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

private val THEORY_STATUS_OPTIONS = listOf("Completed", "In Progress", "Pending")
private val LAB_STATUS_OPTIONS = listOf("Signed by Instructor", "Submitted for Sign", "Incomplete")
private val INSTRUCTOR_SUBMISSION_FILTERS = listOf("All", "Pending", "Verified", "Forwarded")

/**
 * TRAINEE VIEW: Submit & Track Theory Notes & Lab Log Book status.
 */
@Composable
fun TraineeAcademicSubmissionsTab(
    uiState: AppUiState,
    onSubmitRecord: (
        theoryTopic: String,
        theoryStatus: String,
        theoryUpdatedDate: String,
        labJobName: String,
        labStatus: String,
        labSignedDate: String,
        remarks: String
    ) -> Unit,
    onRefreshSubmissions: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val data = uiState.traineeData
    val existing = data?.myAcademicSubmission

    var theoryTopic by remember(existing?.id, existing?.theoryTopic) {
        mutableStateOf(existing?.theoryTopic.orEmpty())
    }
    var theoryStatus by remember(existing?.id, existing?.theoryStatus) {
        mutableStateOf(existing?.theoryStatus?.takeIf { it in THEORY_STATUS_OPTIONS } ?: "In Progress")
    }
    var theoryUpdatedDate by remember(existing?.id, existing?.theoryUpdatedDate) {
        mutableStateOf(
            existing?.theoryUpdatedDate?.takeIf { it.isNotBlank() && it != "-" }
                ?: IstDateTimeFormatter.nowIstDateString()
        )
    }
    var labJobName by remember(existing?.id, existing?.labJobName) {
        mutableStateOf(existing?.labJobName.orEmpty())
    }
    var labStatus by remember(existing?.id, existing?.labStatus) {
        mutableStateOf(existing?.labStatus?.takeIf { it in LAB_STATUS_OPTIONS } ?: "Submitted for Sign")
    }
    var labSignedDate by remember(existing?.id, existing?.labSignedDate) {
        mutableStateOf(
            existing?.labSignedDate?.takeIf { it.isNotBlank() && it != "-" }
                ?: if (labStatus == "Signed by Instructor") IstDateTimeFormatter.nowIstDateString() else ""
        )
    }
    var remarks by remember(existing?.id, existing?.remarks) {
        mutableStateOf(existing?.remarks.orEmpty())
    }

    LaunchedEffect(Unit) {
        onRefreshSubmissions()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("trainee_academic_submissions_tab"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Auto-filled Trainee Identity
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.2.dp, colors.skyAccent.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(colors.skyContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                                contentDescription = null,
                                tint = colors.skyAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Theory Notes & Lab Log Book",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Roll #${(data?.roll ?: uiState.activeRoll).padStart(2, '0')} • ${data?.name ?: "Trainee"}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                color = colors.skyAccent
                            )
                        }
                    }
                    IconButton(
                        onClick = onRefreshSubmissions,
                        modifier = Modifier.testTag("trainee_academic_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh submission status",
                            tint = colors.skyAccent
                        )
                    }
                }
            }
        }

        // Live Submission Status Card (PENDING Amber -> VERIFIED Blue -> SENT TO PRINCIPAL Green)
        if (existing != null) {
            item {
                TraineeSubmissionStatusCard(submission = existing)
            }
        }

        // Submit / Update Form Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Section A: Current Theory Notes
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = null,
                            tint = colors.skyAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "1. Current Theory Notes",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = theoryTopic,
                        onValueChange = { theoryTopic = it },
                        label = { Text("Module / Topic Title") },
                        placeholder = { Text("e.g., Module 4: SMPS & Voltage Regulators") },
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
                            .testTag("academic_theory_topic_input")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(THEORY_STATUS_OPTIONS) { option ->
                            val selected = theoryStatus == option
                            FilterChip(
                                selected = selected,
                                onClick = { theoryStatus = option },
                                label = { Text(option, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.skyContainer,
                                    selectedLabelColor = colors.skyAccent,
                                    containerColor = MaterialTheme.colorScheme.background,
                                    labelColor = colors.textMuted
                                ),
                                modifier = Modifier.testTag("theory_status_chip_${option.lowercase().replace(" ", "_")}")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = theoryUpdatedDate,
                        onValueChange = { theoryUpdatedDate = it },
                        label = { Text("Last Updated Date (dd-MM-yyyy)") },
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
                            .testTag("academic_theory_date_input")
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.borderSubtle)
                    )

                    // Section B: Current Lab Log Book
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Science,
                            contentDescription = null,
                            tint = colors.emeraldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "2. Current Lab Log Book",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = labJobName,
                        onValueChange = { labJobName = it },
                        label = { Text("Experiment / Job Name & No.") },
                        placeholder = { Text("e.g., Job #14 — Bridge Rectifier & Filter Testing") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.emeraldAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("academic_lab_job_input")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(LAB_STATUS_OPTIONS) { option ->
                            val selected = labStatus == option
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    labStatus = option
                                    if (option == "Signed by Instructor" && labSignedDate.isBlank()) {
                                        labSignedDate = IstDateTimeFormatter.nowIstDateString()
                                    }
                                },
                                label = { Text(option, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.emeraldContainer,
                                    selectedLabelColor = colors.emeraldAccent,
                                    containerColor = MaterialTheme.colorScheme.background,
                                    labelColor = colors.textMuted
                                ),
                                modifier = Modifier.testTag("lab_status_chip_${option.lowercase().replace(" ", "_")}")
                            )
                        }
                    }

                    if (labStatus == "Signed by Instructor") {
                        OutlinedTextField(
                            value = labSignedDate,
                            onValueChange = { labSignedDate = it },
                            label = { Text("Signed Date (dd-MM-yyyy)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.emeraldAccent,
                                unfocusedBorderColor = colors.borderSubtle,
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("academic_lab_signed_date_input")
                        )
                    }

                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Remarks / Page Count (Optional)") },
                        placeholder = { Text("e.g., 42 pages completed, circuit diagrams neat") },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.skyAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("academic_remarks_input")
                    )

                    AnimatedVisibility(visible = uiState.academicActionMessage != null) {
                        InlineAlertCard(message = uiState.academicActionMessage.orEmpty(), isError = false)
                    }
                    AnimatedVisibility(visible = uiState.academicActionError != null) {
                        InlineAlertCard(message = uiState.academicActionError.orEmpty(), isError = true)
                    }

                    Button(
                        onClick = {
                            onDismissFeedback()
                            onSubmitRecord(
                                theoryTopic,
                                theoryStatus,
                                theoryUpdatedDate,
                                labJobName,
                                labStatus,
                                if (labStatus == "Signed by Instructor") labSignedDate else "",
                                remarks
                            )
                        },
                        enabled = !uiState.isSubmittingAcademicRecord,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.skyAccent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_academic_record_button")
                    ) {
                        if (uiState.isSubmittingAcademicRecord) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving Submission...")
                        } else {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (existing != null) "Update Notes & Log Book Status" else "Submit for Instructor Verification",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TraineeSubmissionStatusCard(submission: AcademicSubmission) {
    val colors = LocalItiColors.current
    val (badgeLabel, badgeFg, badgeBg, icon) = when (submission.verificationStatus.uppercase()) {
        AcademicSubmissionStatuses.FORWARDED_TO_PRINCIPAL -> Quad(
            "SENT TO PRINCIPAL",
            colors.emeraldAccent,
            colors.emeraldContainer,
            Icons.Rounded.CheckCircle
        )
        AcademicSubmissionStatuses.VERIFIED -> Quad(
            "VERIFIED BY INSTRUCTOR",
            colors.skyAccent,
            colors.skyContainer,
            Icons.Rounded.Verified
        )
        else -> Quad(
            "PENDING VERIFICATION",
            colors.amberAccent,
            colors.amberContainer,
            Icons.Rounded.HourglassTop
        )
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.5.dp, badgeFg.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trainee_submission_status_card")
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
                    text = "Live Verification Status",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeFg.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(icon, contentDescription = null, tint = badgeFg, modifier = Modifier.size(14.dp))
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = badgeFg
                        )
                    }
                }
            }

            if (submission.submittedAt.isNotBlank() && submission.submittedAt != "-") {
                Text(
                    text = "Submitted: ${IstDateTimeFormatter.formatDateTime(submission.submittedAt)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.textMuted
                )
            }
            if (submission.verifiedAt.isNotBlank() && submission.verifiedAt != "-") {
                Text(
                    text = "Verified by ${submission.verifiedBy.ifBlank { "Trade Instructor (EM)" }} at ${IstDateTimeFormatter.formatDateTime(submission.verifiedAt)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.skyAccent
                )
            }
            if (submission.forwardedAt.isNotBlank() && submission.forwardedAt != "-") {
                Text(
                    text = "Forwarded to Principal at ${IstDateTimeFormatter.formatDateTime(submission.forwardedAt)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.emeraldAccent
                )
            }
            if (submission.instructorRemark.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Instructor Remark: ${submission.instructorRemark}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

/**
 * TRADE INSTRUCTOR VIEW: Verify & Sign all 23 trainees' Theory Notes & Lab Log Books
 * and forward individual or all verified records to the Principal.
 */
@Composable
fun InstructorAcademicSubmissionsTab(
    uiState: AppUiState,
    onRefreshSubmissions: () -> Unit,
    onVerifySubmission: (roll: String, submissionId: String, remark: String) -> Unit,
    onForwardSingleToPrincipal: (roll: String) -> Unit,
    onForwardAllVerifiedToPrincipal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    var selectedFilter by remember { mutableStateOf("All") }
    var verifyDialogTarget by remember { mutableStateOf<AcademicSubmission?>(null) }
    var verifyRemarkInput by remember { mutableStateOf("") }

    val attendance = uiState.staffData?.allAttendance.orEmpty()
    val rawSubs = uiState.staffData?.academicSubmissions.orEmpty()
    val subsByRoll = remember(rawSubs) { rawSubs.associateBy { it.roll.trim() } }

    // Merge with full 23-trainee roster so every trainee is visible
    val mergedRoster = remember(attendance, subsByRoll) {
        if (attendance.isNotEmpty()) {
            attendance.map { att ->
                subsByRoll[att.roll.trim()] ?: AcademicSubmission(
                    roll = att.roll,
                    studentName = att.name,
                    verificationStatus = AcademicSubmissionStatuses.NOT_SUBMITTED
                )
            }
        } else {
            rawSubs
        }
    }

    val pendingCount = mergedRoster.count {
        it.verificationStatus.equals(AcademicSubmissionStatuses.PENDING_VERIFICATION, ignoreCase = true)
    }
    val verifiedOnlyCount = mergedRoster.count {
        it.verificationStatus.equals(AcademicSubmissionStatuses.VERIFIED, ignoreCase = true) && !it.sentToPrincipal
    }
    val forwardedCount = mergedRoster.count { it.isForwarded }

    val filteredList = remember(mergedRoster, selectedFilter) {
        when (selectedFilter) {
            "Pending" -> mergedRoster.filter {
                it.verificationStatus.equals(AcademicSubmissionStatuses.PENDING_VERIFICATION, true) ||
                    it.verificationStatus.equals(AcademicSubmissionStatuses.NOT_SUBMITTED, true)
            }
            "Verified" -> mergedRoster.filter { it.isVerifiedOrForwarded }
            "Forwarded" -> mergedRoster.filter { it.isForwarded }
            else -> mergedRoster
        }
    }

    LaunchedEffect(Unit) {
        onRefreshSubmissions()
    }

    // Verify & Sign Confirmation Dialog with optional Instructor Remark
    verifyDialogTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { verifyDialogTarget = null },
            containerColor = colors.cardSurface,
            title = {
                Text(
                    text = "Verify & Sign • Roll #${target.roll.padStart(2, '0')}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${target.studentName}\nTheory: ${target.theoryTopic.ifBlank { "-" }} (${target.theoryStatus})\nLab Log: ${target.labJobName.ifBlank { "-" }} (${target.labStatus})",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                    OutlinedTextField(
                        value = verifyRemarkInput,
                        onValueChange = { verifyRemarkInput = it },
                        label = { Text("Short Instructor Remark (Optional)") },
                        placeholder = { Text("e.g., Verified up to Job #14, neat diagrams") },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verify_submission_remark_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val t = target
                        val rem = verifyRemarkInput.trim()
                        verifyDialogTarget = null
                        verifyRemarkInput = ""
                        onVerifySubmission(t.roll, t.id, rem)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.skyAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("confirm_verify_sign_button")
                ) {
                    Text("Verify & Sign")
                }
            },
            dismissButton = {
                TextButton(onClick = { verifyDialogTarget = null }) {
                    Text("Cancel", color = colors.textMuted)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("instructor_academic_submissions_tab"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + Summary KPI + Bulk Send All Verified to Principal
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.2.dp, colors.skyAccent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Theory Notes & Lab Log Verification",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Verify trainee submissions & forward signed records to Principal",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                        IconButton(onClick = onRefreshSubmissions) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = colors.skyAccent)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AcademicKpiChip("Pending", pendingCount.toString(), colors.amberAccent, Modifier.weight(1f))
                        AcademicKpiChip("Verified", (verifiedOnlyCount + forwardedCount).toString(), colors.skyAccent, Modifier.weight(1f))
                        AcademicKpiChip("Sent", forwardedCount.toString(), colors.emeraldAccent, Modifier.weight(1f))
                    }

                    Button(
                        onClick = onForwardAllVerifiedToPrincipal,
                        enabled = !uiState.isUpdatingAcademicSubmission && verifiedOnlyCount > 0,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.emeraldAccent,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("bulk_forward_verified_button")
                    ) {
                        Icon(Icons.Rounded.ForwardToInbox, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (verifiedOnlyCount > 0) {
                                "Send All Verified to Principal ($verifiedOnlyCount ready)"
                            } else {
                                "All Verified Records Sent to Principal"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    AnimatedVisibility(visible = uiState.academicActionMessage != null) {
                        InlineAlertCard(message = uiState.academicActionMessage.orEmpty(), isError = false)
                    }
                    AnimatedVisibility(visible = uiState.academicActionError != null) {
                        InlineAlertCard(message = uiState.academicActionError.orEmpty(), isError = true)
                    }
                }
            }
        }

        // Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(INSTRUCTOR_SUBMISSION_FILTERS) { filter ->
                    val selected = selectedFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.skyContainer,
                            selectedLabelColor = colors.skyAccent,
                            containerColor = colors.cardSurface,
                            labelColor = colors.textMuted
                        ),
                        modifier = Modifier.testTag("instructor_academic_filter_${filter.lowercase()}")
                    )
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                InlineAlertCard(message = "No submissions match the '$selectedFilter' filter.", isError = false)
            }
        } else {
            items(filteredList, key = { "sub_${it.roll}_${it.id}" }) { sub ->
                InstructorAcademicSubmissionCard(
                    submission = sub,
                    isBusy = uiState.isUpdatingAcademicSubmission,
                    onVerifyClick = {
                        verifyRemarkInput = sub.instructorRemark
                        verifyDialogTarget = sub
                    },
                    onSendToPrincipalClick = { onForwardSingleToPrincipal(sub.roll) }
                )
            }
        }
    }
}

@Composable
private fun InstructorAcademicSubmissionCard(
    submission: AcademicSubmission,
    isBusy: Boolean,
    onVerifyClick: () -> Unit,
    onSendToPrincipalClick: () -> Unit
) {
    val colors = LocalItiColors.current
    val isNotSubmitted = submission.verificationStatus.equals(AcademicSubmissionStatuses.NOT_SUBMITTED, true)
    val isForwarded = submission.isForwarded
    val isVerified = submission.isVerifiedOrForwarded

    val (badgeLabel, badgeFg, badgeBg) = when {
        isForwarded -> Triple("SENT TO PRINCIPAL", colors.emeraldAccent, colors.emeraldContainer)
        isVerified -> Triple("VERIFIED", colors.skyAccent, colors.skyContainer)
        isNotSubmitted -> Triple("NOT SUBMITTED", colors.textMuted, MaterialTheme.colorScheme.background)
        else -> Triple("PENDING", colors.amberAccent, colors.amberContainer)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, if (isForwarded) colors.emeraldAccent.copy(alpha = 0.5f) else colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("instructor_submission_card_${submission.roll}")
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
                Text(
                    text = "Roll #${submission.roll.padStart(2, '0')} • ${submission.studentName}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeFg.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (isNotSubmitted) {
                Text(
                    text = "Trainee has not submitted Theory Notes or Lab Log Book status yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Theory Notes: ${submission.theoryTopic.ifBlank { "-" }} • [${submission.theoryStatus}]" +
                                if (submission.theoryUpdatedDate.isNotBlank() && submission.theoryUpdatedDate != "-") " (${submission.theoryUpdatedDate})" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Lab Log Book: ${submission.labJobName.ifBlank { "-" }} • [${submission.labStatus}]" +
                                if (submission.labSignedDate.isNotBlank() && submission.labSignedDate != "-") " (Signed ${submission.labSignedDate})" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.emeraldAccent
                        )
                        if (submission.remarks.isNotBlank()) {
                            Text(
                                text = "Trainee Note: ${submission.remarks}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                        if (submission.verifiedAt.isNotBlank() && submission.verifiedAt != "-") {
                            Text(
                                text = "Verified: ${IstDateTimeFormatter.formatDateTime(submission.verifiedAt)} by ${submission.verifiedBy.ifBlank { "Trade Instructor (EM)" }}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = colors.skyAccent
                            )
                        }
                        if (submission.instructorRemark.isNotBlank()) {
                            Text(
                                text = "Remark: ${submission.instructorRemark}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.skyAccent
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onVerifyClick,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isVerified) colors.skyContainer else colors.skyAccent,
                            contentColor = if (isVerified) colors.skyAccent else MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("verify_sign_button_${submission.roll}")
                    ) {
                        Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isVerified) "Re-Verify" else "Verify & Sign",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedButton(
                        onClick = onSendToPrincipalClick,
                        enabled = !isBusy && isVerified && !isForwarded,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isVerified && !isForwarded) colors.emeraldAccent else colors.borderSubtle
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isForwarded) colors.emeraldAccent else colors.emeraldAccent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("send_to_principal_button_${submission.roll}")
                    ) {
                        Icon(Icons.Rounded.ForwardToInbox, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isForwarded) "Sent ✓" else "Send Principal",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * PRINCIPAL VIEW: Read-only oversight of Theory Notes & Lab Log Book verification
 * with summary KPIs and toggle between Forwarded Records and All 23 Trainees.
 */
@Composable
fun PrincipalAcademicSubmissionsTab(
    uiState: AppUiState,
    onRefreshSubmissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    var showAllTrainees by remember { mutableStateOf(false) }

    val attendance = uiState.staffData?.allAttendance.orEmpty()
    val rawSubs = uiState.staffData?.academicSubmissions.orEmpty()
    val subsByRoll = remember(rawSubs) { rawSubs.associateBy { it.roll.trim() } }

    val mergedAll = remember(attendance, subsByRoll) {
        if (attendance.isNotEmpty()) {
            attendance.map { att ->
                subsByRoll[att.roll.trim()] ?: AcademicSubmission(
                    roll = att.roll,
                    studentName = att.name,
                    verificationStatus = AcademicSubmissionStatuses.NOT_SUBMITTED
                )
            }
        } else {
            rawSubs
        }
    }

    val totalCount = mergedAll.size.coerceAtLeast(23)
    val verifiedCount = mergedAll.count { it.isVerifiedOrForwarded }
    val labSignedCount = mergedAll.count { it.labStatus.equals("Signed by Instructor", ignoreCase = true) }
    val forwardedCount = mergedAll.count { it.isForwarded }

    val displayedList = remember(mergedAll, showAllTrainees) {
        if (showAllTrainees) mergedAll else mergedAll.filter { it.isForwarded }
    }

    LaunchedEffect(Unit) {
        onRefreshSubmissions()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("principal_academic_submissions_tab"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.5.dp, colors.violetAccent.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Theory Notes & Lab Log Book Oversight",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Read-only academic completion audit forwarded by Trade Instructor",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                        IconButton(onClick = onRefreshSubmissions) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = colors.violetAccent)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AcademicKpiChip("Verified", "$verifiedCount/$totalCount", colors.skyAccent, Modifier.weight(1f))
                        AcademicKpiChip("Lab Signed", "$labSignedCount/$totalCount", colors.emeraldAccent, Modifier.weight(1f))
                        AcademicKpiChip("Forwarded", "$forwardedCount/$totalCount", colors.violetAccent, Modifier.weight(1f))
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (showAllTrainees) "Showing All $totalCount Trainees" else "Showing Forwarded Records Only ($forwardedCount)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Toggle to inspect all 23 trainees including pending submissions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                            Switch(
                                checked = showAllTrainees,
                                onCheckedChange = { showAllTrainees = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = colors.violetAccent
                                ),
                                modifier = Modifier.testTag("principal_academic_show_all_switch")
                            )
                        }
                    }
                }
            }
        }

        if (displayedList.isEmpty()) {
            item {
                InlineAlertCard(
                    message = if (showAllTrainees) {
                        "No trainee records available yet."
                    } else {
                        "No records forwarded by Trade Instructor yet. Toggle 'Show All' above to inspect all 23 trainees."
                    },
                    isError = false
                )
            }
        } else {
            items(displayedList, key = { "prin_sub_${it.roll}_${it.id}" }) { sub ->
                PrincipalAcademicReadOnlyCard(submission = sub)
            }
        }
    }
}

@Composable
private fun PrincipalAcademicReadOnlyCard(submission: AcademicSubmission) {
    val colors = LocalItiColors.current
    val isNotSubmitted = submission.verificationStatus.equals(AcademicSubmissionStatuses.NOT_SUBMITTED, true)
    val badgeColor = when {
        submission.isForwarded -> colors.emeraldAccent
        submission.isVerifiedOrForwarded -> colors.skyAccent
        isNotSubmitted -> colors.textMuted
        else -> colors.amberAccent
    }
    val badgeText = when {
        submission.isForwarded -> "FORWARDED ✓"
        submission.isVerifiedOrForwarded -> "VERIFIED"
        isNotSubmitted -> "NOT SUBMITTED"
        else -> "PENDING"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("principal_submission_card_${submission.roll}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Roll #${submission.roll.padStart(2, '0')} • ${submission.studentName}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = badgeColor
                )
            }

            if (isNotSubmitted) {
                Text(
                    text = "Awaiting trainee submission.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            } else {
                Text(
                    text = "Theory Notes: ${submission.theoryTopic.ifBlank { "-" }} (${submission.theoryStatus})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Lab Log Book: ${submission.labJobName.ifBlank { "-" }} (${submission.labStatus})",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (submission.labStatus.equals("Signed by Instructor", true)) colors.emeraldAccent else colors.amberAccent
                )
                if (submission.verifiedAt.isNotBlank() && submission.verifiedAt != "-") {
                    Text(
                        text = "Instructor Verified: ${IstDateTimeFormatter.formatDateTime(submission.verifiedAt)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = colors.skyAccent
                    )
                }
                if (submission.instructorRemark.isNotBlank()) {
                    Text(
                        text = "Instructor Remark: ${submission.instructorRemark}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun AcademicKpiChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                color = colors.textMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accent
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
