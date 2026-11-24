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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Translate
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AcademicSubmissionStatuses
import com.example.data.AttendanceApiClient
import com.example.data.DutyRotationEngine
import com.example.data.MonthlyExamMark
import com.example.data.TimetablePeriod
import com.example.ui.components.DigitalSignaturePreview
import com.example.ui.components.ExternalUrlHelper
import com.example.ui.components.InlineAlertCard
import com.example.ui.components.NoticeCardItem
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

enum class NewsLanguageFilter(val label: String) {
    BOTH("Both (EN + ML)"),
    ENGLISH("English"),
    MALAYALAM("മലയാളം")
}

private enum class AcademicsSubSection(val label: String) {
    LOG_BOOK("Log Book"),
    TIMETABLE("Timetable"),
    DUTY_TURN("Duty Turn"),
    PROJECTS("Project Groups")
}

@Composable
fun TraineeMarksTab(
    uiState: AppUiState,
    onSubmitWorkMarkQuery: (
        monthLabel: String,
        workReference: String,
        currentMarkNote: String,
        traineeNote: String,
        onSuccessReset: () -> Unit
    ) -> Unit,
    onDismissWorkQueryFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val data = uiState.traineeData
    val marks = data?.monthlyMarks.orEmpty()
    val queries = data?.myWorkMarkQueries.orEmpty()

    var monthLabel by remember { mutableStateOf(marks.firstOrNull()?.monthLabel ?: "Nov 2026 Evaluation") }
    var workRef by remember { mutableStateOf("") }
    var currentNote by remember { mutableStateOf("") }
    var traineeNote by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .widthIn(max = 640.dp)
            .fillMaxSize()
            .testTag("trainee_marks_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Grade, contentDescription = null, tint = colors.amberAccent)
                Column {
                    Text(
                        text = "Monthly Theory & Practical Marks",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Published & digitally signed by Trade Instructor",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }
            }
        }

        if (marks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No exam marks published yet for Roll #${uiState.activeRoll}. Marks signed by your Trade Instructor will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(marks, key = { "tr_mark_${it.id}_${it.monthLabel}" }) { mark ->
                TraineeSignedMarkCard(mark = mark)
            }
        }

        // Work / Lab Mark Query Form
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.skyAccent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.QuestionAnswer, contentDescription = null, tint = colors.skyAccent, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Ask Instructor About Practical / Theory Marks",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    OutlinedTextField(
                        value = monthLabel,
                        onValueChange = { monthLabel = it },
                        label = { Text("Exam / Evaluation Month") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = workRef,
                        onValueChange = { workRef = it },
                        label = { Text("Job / Practical Name or Theory Paper") },
                        placeholder = { Text("e.g., SMD Soldering Job #4") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("trainee_query_ref_input")
                    )
                    OutlinedTextField(
                        value = currentNote,
                        onValueChange = { currentNote = it },
                        label = { Text("Shown Mark (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = traineeNote,
                        onValueChange = { traineeNote = it },
                        label = { Text("Your Doubt / Query") },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("trainee_query_note_input")
                    )

                    AnimatedVisibility(visible = uiState.workQueryMessage != null) {
                        InlineAlertCard(message = uiState.workQueryMessage.orEmpty(), isError = false)
                    }
                    AnimatedVisibility(visible = uiState.workQueryError != null) {
                        InlineAlertCard(message = uiState.workQueryError.orEmpty(), isError = true)
                    }

                    Button(
                        onClick = {
                            onDismissWorkQueryFeedback()
                            onSubmitWorkMarkQuery(monthLabel, workRef, currentNote, traineeNote) {
                                workRef = ""
                                currentNote = ""
                                traineeNote = ""
                            }
                        },
                        enabled = !uiState.isSubmittingWorkQuery && workRef.isNotBlank() && traineeNote.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("trainee_submit_query_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Send Query to Instructor")
                    }
                }
            }
        }

        if (queries.isNotEmpty()) {
            item {
                Text(
                    text = "My Mark Queries (${queries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(queries, key = { "tr_q_${it.id}" }) { q ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(q.workReference, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = q.status,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                                color = if (q.status == "REVISED") colors.emeraldAccent else colors.amberAccent
                            )
                        }
                        Text(q.traineeNote, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                        if (q.instructorReply.isNotBlank()) {
                            Text(
                                text = "Instructor Reply: ${q.instructorReply}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.emeraldAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TraineeSignedMarkCard(mark: MonthlyExamMark) {
    val colors = LocalItiColors.current
    val pctColor = when {
        mark.percentage >= 60 -> colors.emeraldAccent
        mark.percentage >= 40 -> colors.amberAccent
        else -> colors.redAccent
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.5.dp, pctColor.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = mark.monthLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (mark.examDate.isNotBlank()) {
                        Text(
                            text = "Exam Date: ${mark.examDate}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = colors.textMuted
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = pctColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "${mark.totalMarks}/${mark.totalMax} (${mark.percentage}%)",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                        color = pctColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("THEORY", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                        Text(
                            "${mark.theoryMarks} / ${mark.theoryMax}",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                            color = colors.skyAccent
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("PRACTICAL / LAB", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                        Text(
                            "${mark.labMarks} / ${mark.labMax}",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                            color = colors.emeraldAccent
                        )
                    }
                }
            }

            if (mark.remarks.isNotBlank()) {
                Text(
                    text = "Remarks: ${mark.remarks}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (mark.signedBy.isNotBlank() || mark.signatureData.isNotBlank()) {
                DigitalSignaturePreview(
                    signedBy = mark.signedBy.ifBlank { "Trade Instructor (EM)" },
                    signedAt = mark.signedAt,
                    signatureData = mark.signatureData
                )
            }
        }
    }
}

@Composable
fun TraineeAcademicsTab(
    uiState: AppUiState,
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
    onSaveLocalTimetable: (List<TimetablePeriod>) -> Unit,
    onResetTimetableToOfficial: () -> Unit,
    onSubmitSharedTimetable: (List<TimetablePeriod>) -> Unit,
    onDismissTimetableFeedback: () -> Unit,
    onSaveProjectGroup: (title: String, members: List<Int>, leaderRoll: Int?, leaderName: String, onReset: () -> Unit) -> Unit,
    onRefreshProjectGroups: () -> Unit,
    onDismissProjectFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val data = uiState.traineeData
    var activeSub by remember { mutableStateOf(AcademicsSubSection.LOG_BOOK) }

    LazyColumn(
        modifier = modifier
            .widthIn(max = 640.dp)
            .fillMaxSize()
            .testTag("trainee_academics_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AcademicsSubSection.entries) { sub ->
                    val selected = activeSub == sub
                    FilterChip(
                        selected = selected,
                        onClick = { activeSub = sub },
                        label = { Text(sub.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.skyAccent,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = colors.cardSurface,
                            labelColor = colors.textMuted
                        )
                    )
                }
            }
        }

        when (activeSub) {
            AcademicsSubSection.LOG_BOOK -> {
                item {
                    val sub = data?.myAcademicSubmission
                    var theoryTopic by remember(sub?.theoryTopic) { mutableStateOf(sub?.theoryTopic.orEmpty()) }
                    var theoryStatus by remember(sub?.theoryStatus) { mutableStateOf(sub?.theoryStatus ?: "Completed") }
                    var labJobName by remember(sub?.labJobName) { mutableStateOf(sub?.labJobName.orEmpty()) }
                    var labStatus by remember(sub?.labStatus) { mutableStateOf(sub?.labStatus ?: "Completed") }
                    var remarks by remember(sub?.remarks) { mutableStateOf(sub?.remarks.orEmpty()) }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.cardSurface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = colors.skyAccent)
                                    Text("Theory & Lab Log Book", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(onClick = onRefreshAcademicSubmission) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh status", tint = colors.skyAccent)
                                }
                            }

                            if (sub != null) {
                                val stColor = when (sub.verificationStatus) {
                                    AcademicSubmissionStatuses.FORWARDED_TO_PRINCIPAL -> colors.emeraldAccent
                                    AcademicSubmissionStatuses.VERIFIED -> colors.skyAccent
                                    else -> colors.amberAccent
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = stColor.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, stColor.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Verification: ${sub.verificationStatus}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                                            color = stColor
                                        )
                                        if (sub.instructorRemark.isNotBlank()) {
                                            Text("Instructor Note: ${sub.instructorRemark}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = theoryTopic,
                                onValueChange = { theoryTopic = it },
                                label = { Text("Theory Module / Notes Topic") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Completed", "In Progress").forEach { st ->
                                    FilterChip(
                                        selected = theoryStatus == st,
                                        onClick = { theoryStatus = st },
                                        label = { Text("Theory: $st") }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = labJobName,
                                onValueChange = { labJobName = it },
                                label = { Text("Workshop / Practical Job Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Completed", "Pending Sign").forEach { st ->
                                    FilterChip(
                                        selected = labStatus == st,
                                        onClick = { labStatus = st },
                                        label = { Text("Lab: $st") }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = remarks,
                                onValueChange = { remarks = it },
                                label = { Text("Remarks / Record Book Notes") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (uiState.academicActionMessage != null) {
                                InlineAlertCard(uiState.academicActionMessage, isError = false)
                            }
                            if (uiState.academicActionError != null) {
                                InlineAlertCard(uiState.academicActionError, isError = true)
                            }

                            Button(
                                onClick = {
                                    onDismissAcademicFeedback()
                                    val today = AttendanceApiClient.todayIstDateString()
                                    onSubmitAcademicRecord(theoryTopic, theoryStatus, today, labJobName, labStatus, today, remarks)
                                },
                                enabled = !uiState.isSubmittingAcademicRecord && (theoryTopic.isNotBlank() || labJobName.isNotBlank()),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Submit Record for Instructor Verification")
                            }
                        }
                    }
                }
            }

            AcademicsSubSection.TIMETABLE -> {
                val periods = uiState.traineeCustomTimetable
                    ?: data?.timetable?.ifEmpty { uiState.officialTimetableCache }
                    ?: uiState.officialTimetableCache
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = colors.skyAccent)
                            Text("Weekly Trade Timetable", style = MaterialTheme.typography.titleMedium)
                        }
                        if (uiState.traineeCustomTimetable != null) {
                            OutlinedButton(onClick = onResetTimetableToOfficial) {
                                Text("Reset Official", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                if (uiState.timetableFeedbackMessage != null) {
                    item { InlineAlertCard(uiState.timetableFeedbackMessage, isError = false) }
                }
                if (data?.traineeTimetableEditEnabled == true) {
                    item {
                        val editable = remember(periods) { mutableStateListOf<TimetablePeriod>().apply { addAll(periods) } }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            editable.forEachIndexed { idx, p ->
                                OutlinedTextField(
                                    value = p.subject,
                                    onValueChange = { editable[idx] = p.copy(subject = it) },
                                    label = { Text("${p.day} • ${p.slot}") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onDismissTimetableFeedback(); onSaveLocalTimetable(editable.toList()) }, modifier = Modifier.weight(1f)) {
                                    Text("Save Personal")
                                }
                                Button(onClick = { onDismissTimetableFeedback(); onSubmitSharedTimetable(editable.toList()) }, modifier = Modifier.weight(1f)) {
                                    Text("Submit Shared")
                                }
                            }
                        }
                    }
                } else {
                    items(periods) { p ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.cardSurface,
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${p.day} • ${p.slot}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily), color = colors.skyAccent)
                                    Text(p.subject, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                                }
                                Text(p.roomOrLab, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                            }
                        }
                    }
                }
            }

            AcademicsSubSection.DUTY_TURN -> {
                val todayDuty = DutyRotationEngine.assignmentForDate(
                    overrides = data?.dutyOverrides.orEmpty(),
                    holidays = data?.holidays.orEmpty()
                )
                val myRoll = data?.roll?.toIntOrNull()
                val isMyTurn = myRoll != null && (myRoll == todayDuty.firstRoll || myRoll == todayDuty.secondRoll)
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isMyTurn) colors.amberContainer else colors.cardSurface,
                        border = BorderStroke(1.5.dp, if (isMyTurn) colors.amberAccent else colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.CleaningServices, contentDescription = null, tint = colors.amberAccent)
                                Text("Today's Workshop Duty (${todayDuty.dateKey})", style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                text = todayDuty.pairLabel,
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                                color = if (isMyTurn) colors.amberAccent else colors.skyAccent
                            )
                            Text(
                                text = if (isMyTurn) "Your Roll #${myRoll} is assigned for today's Workshop & Tool Crib Cleaning Duty!" else todayDuty.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                val customDuty = data?.dutyItems.orEmpty()
                if (customDuty.isNotEmpty()) {
                    items(customDuty) { line ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.cardSurface,
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(line, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }

            AcademicsSubSection.PROJECTS -> {
                val enabled = data?.projectGroupsEnabled ?: false
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.cardSurface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(
                                        imageVector = if (enabled) Icons.Rounded.Groups else Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = if (enabled) colors.emeraldAccent else colors.amberAccent
                                    )
                                    Text("Electronics Project Groups", style = MaterialTheme.typography.titleMedium)
                                }
                                IconButton(onClick = onRefreshProjectGroups) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh groups", tint = colors.skyAccent)
                                }
                            }
                            if (!enabled) {
                                Text(
                                    text = "Project group registration is currently locked by the Trade Instructor.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            } else {
                                var title by remember { mutableStateOf("") }
                                var membersInput by remember { mutableStateOf("") }
                                var leaderName by remember { mutableStateOf(data?.name.orEmpty()) }
                                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Project Title") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = membersInput, onValueChange = { membersInput = it }, label = { Text("Member Roll Numbers (e.g. 1, 5, 12)") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = leaderName, onValueChange = { leaderName = it }, label = { Text("Group Leader Name") }, modifier = Modifier.fillMaxWidth())
                                if (uiState.projectGroupSaveMessage != null) InlineAlertCard(uiState.projectGroupSaveMessage, false)
                                if (uiState.projectGroupSaveError != null) InlineAlertCard(uiState.projectGroupSaveError, true)
                                Button(
                                    onClick = {
                                        onDismissProjectFeedback()
                                        val rolls = membersInput.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..23 }
                                        onSaveProjectGroup(title, rolls, data?.roll?.toIntOrNull(), leaderName) {
                                            title = ""
                                            membersInput = ""
                                        }
                                    },
                                    enabled = !uiState.isSavingProjectGroup && title.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Register Project Group") }
                            }
                        }
                    }
                }
                items(data?.projectGroups.orEmpty(), key = { "pg_${it.groupId}" }) { pg ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.cardSurface,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(pg.projectTitle, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("Leader: ${pg.leaderName} (Roll #${pg.leaderRoll}) • Members: ${pg.memberRolls.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TraineeNoticesAndNewsTab(
    uiState: AppUiState,
    onRefreshTechNews: () -> Unit,
    onSelectNewsLanguage: (NewsLanguageFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val context = LocalContext.current
    val notices = uiState.traineeData?.notices.orEmpty()
    val newsItems = uiState.techNewsItems
    val selectedLang = uiState.newsLanguageFilter

    LazyColumn(
        modifier = modifier
            .widthIn(max = 640.dp)
            .fillMaxSize()
            .testTag("trainee_notices_news_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Campaign, contentDescription = null, tint = colors.skyAccent)
                Text("College & Trade Notices (${notices.size})", style = MaterialTheme.typography.titleLarge)
            }
        }

        if (notices.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("No active notices.", style = MaterialTheme.typography.bodySmall, color = colors.textMuted, modifier = Modifier.padding(14.dp))
                }
            }
        } else {
            items(notices, key = { "tr_ntc_${it.id}" }) { notice ->
                NoticeCardItem(notice = notice, isUnread = false)
            }
        }

        // Bilingual Tech News Section (English + മലയാളം)
        item {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Newspaper, contentDescription = null, tint = colors.emeraldAccent)
                    Column {
                        Text("Electronics Tech News", style = MaterialTheme.typography.titleLarge)
                        Text("Bilingual Feed (English & മലയാളം)", style = MaterialTheme.typography.bodySmall, color = colors.emeraldAccent)
                    }
                }
                IconButton(onClick = onRefreshTechNews, enabled = !uiState.isLoadingTechNews) {
                    if (uiState.isLoadingTechNews) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh tech news", tint = colors.emeraldAccent)
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(NewsLanguageFilter.entries) { filter ->
                    val selected = selectedLang == filter
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectNewsLanguage(filter) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Translate, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.emeraldContainer,
                            selectedLabelColor = colors.emeraldAccent,
                            containerColor = colors.cardSurface,
                            labelColor = colors.textMuted
                        ),
                        modifier = Modifier.testTag("news_lang_chip_${filter.name.lowercase()}")
                    )
                }
            }
        }

        items(newsItems, key = { "news_${it.id}" }) { item ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "${item.category.uppercase()} • ${item.source}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                            color = colors.emeraldAccent
                        )
                        if (item.publishedDate.isNotBlank()) {
                            Text(item.publishedDate, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                        }
                    }

                    if (selectedLang == NewsLanguageFilter.ENGLISH || selectedLang == NewsLanguageFilter.BOTH) {
                        Text(
                            text = item.titleEn,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.summaryEn,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }

                    if (selectedLang == NewsLanguageFilter.MALAYALAM || selectedLang == NewsLanguageFilter.BOTH) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.skyContainer.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = item.titleMl,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.skyAccent
                                )
                                Text(
                                    text = item.summaryMl,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (item.url.isNotBlank()) {
                        OutlinedButton(
                            onClick = { ExternalUrlHelper.openUrl(context, item.url) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Read Full Article", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
