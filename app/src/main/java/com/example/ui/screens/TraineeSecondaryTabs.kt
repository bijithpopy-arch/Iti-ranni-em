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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceApiClient
import com.example.data.DutyRotationEngine
import com.example.data.MonthlyExamMark
import com.example.data.TechNewsItem
import com.example.data.TimetablePeriod
import com.example.data.TimetableStore
import com.example.ui.components.ActiveHolidayBanner
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

    var monthLabel by remember { mutableStateOf(marks.firstOrNull()?.monthLabel ?: "Monthly Evaluation") }
    var workReference by remember { mutableStateOf("") }
    var currentMarkNote by remember { mutableStateOf("") }
    var traineeNote by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.widthIn(max = 640.dp).fillMaxSize().testTag("trainee_marks_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Digitally Signed Exam & Practical Marks", style = MaterialTheme.typography.titleMedium)
        }

        if (marks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No marks published yet by your Trade Instructor.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(marks, key = { "tr_mk_${it.id}_${it.monthLabel}_${it.roll}" }) { mark ->
                TraineeSignedMarkCard(mark = mark)
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.QuestionAnswer, contentDescription = null, tint = colors.skyAccent)
                        Text("Ask Trade Instructor About a Mark", style = MaterialTheme.typography.titleSmall)
                    }
                    OutlinedTextField(
                        value = monthLabel,
                        onValueChange = { monthLabel = it },
                        label = { Text("Exam / Month Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = workReference,
                        onValueChange = { workReference = it },
                        label = { Text("Practical Job / Theory Paper Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = currentMarkNote,
                        onValueChange = { currentMarkNote = it },
                        label = { Text("Shown Mark (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = traineeNote,
                        onValueChange = { traineeNote = it },
                        label = { Text("Question / Clarification Note") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    AnimatedVisibility(visible = uiState.workQueryMessage != null) {
                        InlineAlertCard(uiState.workQueryMessage.orEmpty(), isError = false)
                    }
                    AnimatedVisibility(visible = uiState.workQueryError != null) {
                        InlineAlertCard(uiState.workQueryError.orEmpty(), isError = true)
                    }

                    Button(
                        onClick = {
                            onDismissWorkQueryFeedback()
                            onSubmitWorkMarkQuery(monthLabel, workReference, currentMarkNote, traineeNote) {
                                workReference = ""
                                currentMarkNote = ""
                                traineeNote = ""
                            }
                        },
                        enabled = !uiState.isSubmittingWorkQuery && traineeNote.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (uiState.isSubmittingWorkQuery) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Send Mark Query", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (queries.isNotEmpty()) {
            item { Text("Your Mark Queries (${queries.size})", style = MaterialTheme.typography.titleMedium) }
            items(queries, key = { "tr_mq_${it.id}" }) { q ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${q.workReference} • ${q.status}", style = MaterialTheme.typography.titleSmall, color = colors.skyAccent)
                        Text(q.traineeNote, style = MaterialTheme.typography.bodySmall)
                        if (q.instructorReply.isNotBlank()) {
                            Text("Instructor Reply: ${q.instructorReply}", style = MaterialTheme.typography.bodySmall, color = colors.emeraldAccent)
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
    val accent = if (mark.percentage >= 60) colors.emeraldAccent else if (mark.percentage >= 40) colors.amberAccent else colors.redAccent
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(mark.monthLabel, style = MaterialTheme.typography.titleMedium)
                    if (mark.examDate.isNotBlank()) {
                        Text("Date: ${mark.examDate}", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                    }
                }
                Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.16f)) {
                    Text(
                        text = "${mark.totalMarks}/${mark.totalMax} (${mark.percentage}%)",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = colors.cardElevated, modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Theory", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                        Text("${mark.theoryMarks} / ${mark.theoryMax}", style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily))
                    }
                }
                Surface(shape = RoundedCornerShape(12.dp), color = colors.cardElevated, modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Practical / Lab", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                        Text("${mark.labMarks} / ${mark.labMax}", style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily))
                    }
                }
            }

            if (mark.remarks.isNotBlank()) {
                Text("Remarks: ${mark.remarks}", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
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
    val activeSub = uiState.traineeAcademicSubmission ?: data?.myAcademicSubmission

    var theoryTopic by remember(activeSub) { mutableStateOf(activeSub?.theoryTopic.orEmpty()) }
    var theoryStatus by remember(activeSub) { mutableStateOf(activeSub?.theoryStatus ?: "Completed") }
    var theoryUpdatedDate by remember(activeSub) { mutableStateOf(activeSub?.theoryUpdatedDate?.ifBlank { AttendanceApiClient.todayIstDateString() } ?: AttendanceApiClient.todayIstDateString()) }
    var labJobName by remember(activeSub) { mutableStateOf(activeSub?.labJobName.orEmpty()) }
    var labStatus by remember(activeSub) { mutableStateOf(activeSub?.labStatus ?: "Completed") }
    var labSignedDate by remember(activeSub) { mutableStateOf(activeSub?.labSignedDate?.ifBlank { AttendanceApiClient.todayIstDateString() } ?: AttendanceApiClient.todayIstDateString()) }
    var remarks by remember(activeSub) { mutableStateOf(activeSub?.remarks.orEmpty()) }

    val baseTimetable = remember(uiState.activeTimetable) {
        uiState.activeTimetable.ifEmpty { TimetableStore.defaultWeeklyTimetable() }
    }
    val editablePeriods = remember(baseTimetable) {
        mutableStateListOf<TimetablePeriod>().apply { addAll(baseTimetable) }
    }
    var editTimetable by remember { mutableStateOf(false) }

    var projectTitle by remember { mutableStateOf("") }
    var memberRollsText by remember { mutableStateOf(uiState.activeRoll) }
    var leaderRollText by remember { mutableStateOf(uiState.activeRoll) }
    var leaderName by remember { mutableStateOf(data?.name.orEmpty()) }

    val dutyToday = remember(data?.dutyOverrides, data?.holidays) {
        DutyRotationEngine.assignmentForDate(
            overrides = data?.dutyOverrides.orEmpty(),
            holidays = data?.holidays.orEmpty()
        )
    }

    LazyColumn(
        modifier = modifier.widthIn(max = 640.dp).fillMaxSize().testTag("trainee_academics_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.CleaningServices, contentDescription = null, tint = colors.amberAccent)
                        Text("Today's Workshop Duty Turn (${dutyToday.dateKey})", style = MaterialTheme.typography.titleSmall)
                    }
                    Text(dutyToday.pairLabel, style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily), color = colors.skyAccent)
                    Text(dutyToday.note, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = colors.skyAccent)
                            Text("Weekly Trade Timetable", style = MaterialTheme.typography.titleSmall)
                        }
                        TextButton(onClick = { editTimetable = !editTimetable }) {
                            Text(if (editTimetable) "Done" else "Customize")
                        }
                    }

                    editablePeriods.forEachIndexed { idx, period ->
                        Surface(shape = RoundedCornerShape(10.dp), color = colors.cardElevated, modifier = Modifier.fillMaxWidth()) {
                            if (!editTimetable) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("${period.day} • ${period.slot}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily), color = colors.skyAccent)
                                    Text(period.subject, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(period.roomOrLab, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                                }
                            } else {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${period.day} (${period.slot})", style = MaterialTheme.typography.labelSmall, color = colors.skyAccent)
                                    OutlinedTextField(
                                        value = period.subject,
                                        onValueChange = { editablePeriods[idx] = period.copy(subject = it) },
                                        label = { Text("Subject") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    if (editTimetable) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onDismissTimetableFeedback(); onSaveLocalTimetable(editablePeriods.toList()) }, modifier = Modifier.weight(1f)) {
                                Text("Save Locally")
                            }
                            OutlinedButton(onClick = { onDismissTimetableFeedback(); onResetTimetableToOfficial() }, modifier = Modifier.weight(1f)) {
                                Text("Reset Official")
                            }
                        }
                        if (data?.traineeTimetableEditEnabled == true) {
                            Button(onClick = { onDismissTimetableFeedback(); onSubmitSharedTimetable(editablePeriods.toList()) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Publish to Shared Class Timetable")
                            }
                        }
                    }
                    if (uiState.timetableFeedbackMessage != null) {
                        InlineAlertCard(uiState.timetableFeedbackMessage, isError = false)
                    }
                    if (uiState.timetableError != null) {
                        InlineAlertCard(uiState.timetableError, isError = true)
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.AutoStories, contentDescription = null, tint = colors.emeraldAccent)
                            Text("Trade Log Book & Record Submission", style = MaterialTheme.typography.titleSmall)
                        }
                        IconButton(onClick = onRefreshAcademicSubmission, enabled = !uiState.isLoadingAcademicSubmissions) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Log Book", tint = colors.skyAccent)
                        }
                    }
                    if (activeSub != null) {
                        Text(
                            text = "Status: ${activeSub.verificationStatus}${if (activeSub.instructorRemark.isNotBlank()) " • Note: ${activeSub.instructorRemark}" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.emeraldAccent
                        )
                    }
                    OutlinedTextField(value = theoryTopic, onValueChange = { theoryTopic = it }, label = { Text("Theory Chapter / Topic") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = theoryStatus, onValueChange = { theoryStatus = it }, label = { Text("Theory Notebook Status") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = labJobName, onValueChange = { labJobName = it }, label = { Text("Practical Job / Circuit Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = labStatus, onValueChange = { labStatus = it }, label = { Text("Practical Record Status") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("Remarks") }, modifier = Modifier.fillMaxWidth())

                    AnimatedVisibility(visible = uiState.academicSubmitMessage != null) {
                        InlineAlertCard(uiState.academicSubmitMessage.orEmpty(), isError = false)
                    }
                    AnimatedVisibility(visible = uiState.academicSubmitError != null) {
                        InlineAlertCard(uiState.academicSubmitError.orEmpty(), isError = true)
                    }

                    Button(
                        onClick = {
                            onDismissAcademicFeedback()
                            onSubmitAcademicRecord(theoryTopic, theoryStatus, theoryUpdatedDate, labJobName, labStatus, labSignedDate, remarks)
                        },
                        enabled = !uiState.isSubmittingAcademic && (theoryTopic.isNotBlank() || labJobName.isNotBlank()),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Submit Log Book Update", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Groups, contentDescription = null, tint = colors.violetAccent)
                            Text("Final Trade Project Groups", style = MaterialTheme.typography.titleSmall)
                        }
                        IconButton(onClick = onRefreshProjectGroups) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Groups", tint = colors.skyAccent)
                        }
                    }

                    val groups = data?.projectGroups.orEmpty()
                    if (groups.isEmpty()) {
                        Text("No project groups registered yet.", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                    } else {
                        groups.forEach { grp ->
                            Surface(shape = RoundedCornerShape(10.dp), color = colors.cardElevated, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(grp.projectTitle, style = MaterialTheme.typography.titleSmall)
                                    Text("Leader: #${grp.leaderRoll} ${grp.leaderName} • Members: ${grp.memberRolls.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                                }
                            }
                        }
                    }

                    if (data?.projectGroupsEnabled == true) {
                        HorizontalDivider(color = colors.borderSubtle)
                        OutlinedTextField(value = projectTitle, onValueChange = { projectTitle = it }, label = { Text("Project Title") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = memberRollsText, onValueChange = { memberRollsText = it }, label = { Text("Member Roll Numbers (e.g. 1, 5, 12)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = leaderRollText, onValueChange = { leaderRollText = it }, label = { Text("Leader Roll") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = leaderName, onValueChange = { leaderName = it }, label = { Text("Leader Name") }, modifier = Modifier.fillMaxWidth())
                        if (uiState.projectGroupMessage != null) InlineAlertCard(uiState.projectGroupMessage, isError = false)
                        if (uiState.projectGroupError != null) InlineAlertCard(uiState.projectGroupError, isError = true)
                        Button(
                            onClick = {
                                onDismissProjectFeedback()
                                val rolls = memberRollsText.split(",", " ").mapNotNull { it.trim().toIntOrNull() }
                                onSaveProjectGroup(projectTitle, rolls, leaderRollText.toIntOrNull(), leaderName) {
                                    projectTitle = ""
                                }
                            },
                            enabled = !uiState.isSavingProjectGroup && projectTitle.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save Project Group") }
                    } else {
                        Text("Project group editing is currently locked by Trade Instructor.", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
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
    val data = uiState.traineeData
    val holidays = data?.holidays.orEmpty()
    val notices = data?.notices.orEmpty()
    val newsList = uiState.techNews

    LazyColumn(
        modifier = modifier.widthIn(max = 640.dp).fillMaxSize().testTag("trainee_notices_news_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (holidays.isNotEmpty()) {
            item { Text("Upcoming / Declared Holidays", style = MaterialTheme.typography.titleMedium) }
            items(holidays, key = { "tr_hol_${it.id}_${it.date}" }) { h ->
                ActiveHolidayBanner(holiday = h)
            }
        }

        item { Text("Campus Notices (${notices.size})", style = MaterialTheme.typography.titleMedium) }
        if (notices.isEmpty()) {
            item {
                Text("No notices posted yet.", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
        } else {
            items(notices, key = { "tr_ntc_${it.id}_${it.date}" }) { notice ->
                NoticeCardItem(notice = notice, isUnread = false)
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Newspaper, contentDescription = null, tint = colors.skyAccent)
                    Text("Electronics Tech News (EN / ML)", style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = onRefreshTechNews, enabled = !uiState.isLoadingTechNews) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Tech News", tint = colors.skyAccent)
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(NewsLanguageFilter.entries) { filter ->
                    FilterChip(
                        selected = uiState.selectedNewsLanguage == filter,
                        onClick = { onSelectNewsLanguage(filter) },
                        label = { Text(filter.label) },
                        modifier = Modifier.testTag("news_lang_${filter.name.lowercase()}")
                    )
                }
            }
        }

        items(newsList, key = { "news_${it.id}" }) { news ->
            TechNewsCard(
                item = news,
                language = uiState.selectedNewsLanguage,
                onOpenUrl = { url -> ExternalUrlHelper.openUrl(context, url) }
            )
        }
    }
}

@Composable
private fun TechNewsCard(
    item: TechNewsItem,
    language: NewsLanguageFilter,
    onOpenUrl: (String) -> Unit
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${item.category} • ${item.source}", style = MaterialTheme.typography.labelSmall, color = colors.skyAccent)
                if (item.publishedDate.isNotBlank()) {
                    Text(item.publishedDate, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                }
            }
            if (language == NewsLanguageFilter.BOTH || language == NewsLanguageFilter.ENGLISH) {
                Text(item.titleEn, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(item.summaryEn, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            if (language == NewsLanguageFilter.BOTH || language == NewsLanguageFilter.MALAYALAM) {
                Text(item.titleMl, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.emeraldAccent)
                Text(item.summaryMl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
            if (item.url.isNotBlank()) {
                TextButton(onClick = { onOpenUrl(item.url) }, contentPadding = PaddingValues(0.dp)) {
                    Text("Read Full Article", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.size(4.dp))
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
