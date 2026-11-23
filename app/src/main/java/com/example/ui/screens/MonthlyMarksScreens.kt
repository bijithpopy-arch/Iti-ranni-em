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
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.School
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.sp
import com.example.data.AttendanceApiClient
import com.example.data.MonthlyExamMark
import com.example.data.ParentAlertTypes
import com.example.data.WorkMarkQuery
import com.example.ui.components.WhatsAppBotHelper
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

private val QUICK_MONTH_LABELS = listOf("Nov 2026", "Oct 2026", "Dec 2026", "Jan 2027", "Mid-Term")

/**
 * Trainee-only Monthly Theory & Lab Marks + Private "Work / Lab Mark" Query tab.
 * Queries are strictly scoped to the signed-in trainee and the Trade Instructor.
 */
@Composable
fun TraineeMarksAndQueriesTab(
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
    val context = LocalContext.current
    val data = uiState.traineeData
    val marks = remember(data?.monthlyMarks) {
        data?.monthlyMarks.orEmpty().filter { it.published }
    }
    val myRoll = (data?.roll ?: uiState.activeRoll).trim()
    val myQueries = remember(data?.myWorkMarkQueries, myRoll) {
        data?.myWorkMarkQueries.orEmpty().filter { it.roll.equals(myRoll, ignoreCase = true) }
    }

    var monthLabel by remember { mutableStateOf(marks.firstOrNull()?.monthLabel ?: QUICK_MONTH_LABELS.first()) }
    var workReference by remember { mutableStateOf("") }
    var currentMarkNote by remember { mutableStateOf("") }
    var traineeNote by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("trainee_marks_tab"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.skyAccent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.skyContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Grade,
                            contentDescription = null,
                            tint = colors.skyAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Monthly Theory & Lab Marks",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Published by Trade Instructor • ${marks.size} exam record(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }
            }
        }

        // 2. Marks List
        if (marks.isEmpty()) {
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = "No Monthly Marks Published Yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Once your Trade Instructor publishes monthly Theory & Practical/Lab exam marks, they will appear here automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }
            }
        } else {
            items(marks, key = { it.id.ifBlank { "${it.roll}_${it.monthLabel}_${it.examDate}" } }) { mark ->
                MonthlyMarkDisplayCard(
                    mark = mark,
                    showWhatsAppShare = true,
                    onWhatsAppClick = {
                        val msg = "ITI Ranni EM • Monthly Marks (${mark.monthLabel})\n" +
                            "Trainee: ${mark.studentName} (Roll #${mark.roll})\n" +
                            "Trade Theory: ${mark.theoryMarks}/${mark.theoryMax}\n" +
                            "Trade Practical/Lab: ${mark.labMarks}/${mark.labMax}\n" +
                            "Total: ${mark.totalObtained}/${mark.totalMax} (${mark.percentage}%)" +
                            if (mark.remarks.isNotBlank()) "\nRemarks: ${mark.remarks}" else ""
                        WhatsAppBotHelper.openWhatsAppBot(context, msg)
                    }
                )
            }
        }

        // 3. Trainee-Only Private "Work / Lab Mark" Query Form
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.5.dp, colors.violetAccent.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trainee_work_mark_query_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
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
                                .background(colors.violetContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HelpOutline,
                                contentDescription = null,
                                tint = colors.violetAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Work / Lab Mark Query (Private)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = colors.violetAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Visible only to you & Trade Instructor — hidden from other trainees",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.violetAccent
                                )
                            }
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(QUICK_MONTH_LABELS) { label ->
                            FilterChip(
                                selected = monthLabel == label,
                                onClick = { monthLabel = label },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.violetContainer,
                                    selectedLabelColor = colors.violetAccent,
                                    containerColor = MaterialTheme.colorScheme.background,
                                    labelColor = colors.textMuted
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = workReference,
                        onValueChange = { workReference = it },
                        label = { Text("Lab / Workshop Job Reference") },
                        placeholder = { Text("e.g., PCB Soldering Practical #3, CRO Lab Test") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.violetAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("work_query_reference_input")
                    )

                    OutlinedTextField(
                        value = currentMarkNote,
                        onValueChange = { currentMarkNote = it },
                        label = { Text("Current Mark / Score Noted (Optional)") },
                        placeholder = { Text("e.g., Lab 34/50 — submitted record on Friday") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.violetAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("work_query_current_mark_input")
                    )

                    OutlinedTextField(
                        value = traineeNote,
                        onValueChange = { traineeNote = it },
                        label = { Text("Query / Review Request to Instructor") },
                        placeholder = { Text("Describe which practical work or mark needs review...") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.violetAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("work_query_note_input")
                    )

                    AnimatedVisibility(visible = uiState.workQueryMessage != null) {
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
                                    text = uiState.workQueryMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.emeraldAccent,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = onDismissWorkQueryFeedback) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = colors.textMuted)
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = uiState.workQueryError != null) {
                        InlineAlertCard(message = uiState.workQueryError.orEmpty(), isError = true)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSubmitWorkMarkQuery(
                                    monthLabel,
                                    workReference,
                                    currentMarkNote,
                                    traineeNote
                                ) {
                                    workReference = ""
                                    currentMarkNote = ""
                                    traineeNote = ""
                                }
                            },
                            enabled = !uiState.isSubmittingWorkQuery,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.violetAccent,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("submit_work_mark_query_button")
                        ) {
                            if (uiState.isSubmittingWorkQuery) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sending...")
                            } else {
                                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Query", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val waMsg = "ITI Ranni EM • Work / Lab Mark Query\n" +
                                    "Roll #$myRoll (${data?.name ?: "Trainee"})\n" +
                                    "Exam/Month: $monthLabel\n" +
                                    "Work Ref: ${workReference.ifBlank { "Workshop Practical" }}\n" +
                                    "Current Mark: ${currentMarkNote.ifBlank { "N/A" }}\n" +
                                    "Query: ${traineeNote.ifBlank { "Requesting review of my work/lab mark." }}"
                                WhatsAppBotHelper.openWhatsAppBot(context, waMsg)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.emeraldAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.emeraldAccent),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("work_query_whatsapp_button")
                        ) {
                            Icon(Icons.Rounded.Chat, contentDescription = "Send via WhatsApp Bot", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WA Bot", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        // 4. My Private Query History
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Private Mark Queries (${myQueries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "PRIVATE • ROLL #$myRoll",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.violetAccent
                )
            }
        }

        if (myQueries.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No private work/lab mark queries submitted yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(myQueries, key = { it.id.ifBlank { "${it.roll}_${it.date}_${it.hashCode()}" } }) { q ->
                PrivateWorkQueryCard(query = q)
            }
        }
    }
}

/**
 * Parent Portal section displaying monthly Theory & Lab marks + loud notification banner when updated.
 */
@Composable
fun ParentMonthlyMarksSection(
    studentName: String,
    roll: String,
    marks: List<MonthlyExamMark>,
    hasMarksAlertNotification: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val context = LocalContext.current
    val publishedMarks = remember(marks) { marks.filter { it.published } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("parent_monthly_marks_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Monthly Theory & Lab Marks",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${publishedMarks.size} exam(s)",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = colors.skyAccent
            )
        }

        if (hasMarksAlertNotification && publishedMarks.isNotEmpty()) {
            val latest = publishedMarks.first()
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (latest.isRevised) colors.violetContainer else colors.emeraldContainer,
                border = BorderStroke(
                    1.dp,
                    if (latest.isRevised) colors.violetAccent.copy(alpha = 0.6f) else colors.emeraldAccent.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = if (latest.isRevised) colors.violetAccent else colors.emeraldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (latest.isRevised) {
                            "Marks Updated: ${latest.monthLabel} marks for $studentName were revised by Trade Instructor."
                        } else {
                            "New Marks Published: ${latest.monthLabel} Theory & Lab marks are now available."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (publishedMarks.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No monthly Theory & Practical/Lab exam marks published yet for $studentName (Roll #$roll).",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            publishedMarks.forEach { mark ->
                MonthlyMarkDisplayCard(
                    mark = mark,
                    showWhatsAppShare = true,
                    onWhatsAppClick = {
                        val msg = "ITI Ranni EM • Parent Marks Enquiry\n" +
                            "Ward: $studentName (Roll #$roll)\n" +
                            "Exam: ${mark.monthLabel}\n" +
                            "Theory: ${mark.theoryMarks}/${mark.theoryMax}, Lab: ${mark.labMarks}/${mark.labMax} (${mark.percentage}%)"
                        WhatsAppBotHelper.openWhatsAppBot(context, msg)
                    }
                )
            }
        }
    }
}

/**
 * Trade Instructor panel inside Ops & Hub -> "Marks":
 * 1. Enter / Edit / Publish Monthly Theory & Lab marks (with automatic Parent Notification + WhatsApp Bot trigger)
 * 2. Searchable list of saved marks with 1-tap Edit
 * 3. Private Work / Lab Mark Queries Inbox with inline reply & status update (OPEN / REVIEWED / RESOLVED)
 */
@Composable
fun InstructorMarksAndQueriesPanel(
    uiState: AppUiState,
    onSaveMonthlyMark: (
        markId: String,
        roll: String,
        monthLabel: String,
        examDate: String,
        theoryMarks: Int,
        theoryMax: Int,
        labMarks: Int,
        labMax: Int,
        remarks: String,
        publish: Boolean,
        notifyParent: Boolean,
        onSuccessReset: () -> Unit
    ) -> Unit,
    onDismissMonthlyMarkFeedback: () -> Unit,
    onReplyWorkMarkQuery: (queryId: String, status: String, reply: String, onSuccessReset: () -> Unit) -> Unit,
    onDismissWorkQueryReplyFeedback: () -> Unit
) {
    val colors = LocalItiColors.current
    val context = LocalContext.current
    val staffData = uiState.staffData
    val allMarks = staffData?.monthlyMarks.orEmpty()
    val allQueries = staffData?.workMarkQueries.orEmpty()
    val trainees = staffData?.allAttendance.orEmpty()

    var editingMarkId by remember { mutableStateOf("") }
    var rollInput by remember { mutableStateOf("") }
    var monthLabel by remember { mutableStateOf(QUICK_MONTH_LABELS.first()) }
    var examDate by remember { mutableStateOf(AttendanceApiClient.todayIstDateString()) }
    var theoryMarksText by remember { mutableStateOf("") }
    var theoryMaxText by remember { mutableStateOf("50") }
    var labMarksText by remember { mutableStateOf("") }
    var labMaxText by remember { mutableStateOf("50") }
    var remarks by remember { mutableStateOf("") }
    var publishToAll by remember { mutableStateOf(true) }
    var notifyParent by remember { mutableStateOf(true) }
    var marksFilterQuery by remember { mutableStateOf("") }

    val selectedTraineeName = remember(rollInput, trainees) {
        trainees.firstOrNull { it.roll.equals(rollInput.trim(), ignoreCase = true) }?.name.orEmpty()
    }

    val filteredMarks = remember(allMarks, marksFilterQuery) {
        val q = marksFilterQuery.trim().lowercase()
        if (q.isBlank()) allMarks
        else allMarks.filter {
            it.roll.lowercase().contains(q) ||
                it.studentName.lowercase().contains(q) ||
                it.monthLabel.lowercase().contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("instructor_marks_queries_panel"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Enter / Edit Monthly Theory & Lab Marks Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.cardSurface,
            border = BorderStroke(
                1.5.dp,
                if (editingMarkId.isNotBlank()) colors.amberAccent.copy(alpha = 0.7f) else colors.skyAccent.copy(alpha = 0.55f)
            ),
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
                            text = if (editingMarkId.isNotBlank()) "Edit Monthly Exam Marks" else "Enter Monthly Theory & Lab Marks",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (editingMarkId.isNotBlank()) colors.amberAccent else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (editingMarkId.isNotBlank()) {
                                "Editing saved record ($editingMarkId) • Parent gets revised alert"
                            } else {
                                "Publish & notify Parent automatically on save"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                    if (editingMarkId.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                editingMarkId = ""
                                theoryMarksText = ""
                                labMarksText = ""
                                remarks = ""
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, colors.amberAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.amberAccent)
                        ) {
                            Text("Cancel Edit", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Month presets
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(QUICK_MONTH_LABELS) { label ->
                        FilterChip(
                            selected = monthLabel == label,
                            onClick = { monthLabel = label },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.skyContainer,
                                selectedLabelColor = colors.skyAccent,
                                containerColor = MaterialTheme.colorScheme.background,
                                labelColor = colors.textMuted
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = rollInput,
                        onValueChange = { rollInput = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Roll (1–33)") },
                        supportingText = if (selectedTraineeName.isNotBlank()) {
                            { Text(selectedTraineeName, color = colors.skyAccent) }
                        } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.skyAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("marks_roll_input")
                    )

                    OutlinedTextField(
                        value = monthLabel,
                        onValueChange = { monthLabel = it },
                        label = { Text("Exam / Month") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.skyAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("marks_month_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = theoryMarksText,
                        onValueChange = { theoryMarksText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Theory Marks") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.skyAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("marks_theory_input")
                    )
                    OutlinedTextField(
                        value = theoryMaxText,
                        onValueChange = { theoryMaxText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Theory Max") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.skyAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .weight(0.75f)
                            .testTag("marks_theory_max_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = labMarksText,
                        onValueChange = { labMarksText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Lab / Practical") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.emeraldAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("marks_lab_input")
                    )
                    OutlinedTextField(
                        value = labMaxText,
                        onValueChange = { labMaxText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Lab Max") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.emeraldAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .weight(0.75f)
                            .testTag("marks_lab_max_input")
                    )
                }

                OutlinedTextField(
                    value = examDate,
                    onValueChange = { examDate = it },
                    label = { Text("Exam Date (DD-MM-YYYY)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.skyAccent,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Instructor Remarks (Optional)") },
                    placeholder = { Text("e.g., Good oscilloscope reading, improve theory revision") },
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
                        .testTag("marks_remarks_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Publish Live", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = publishToAll,
                            onCheckedChange = { publishToAll = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = colors.emeraldAccent)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Notify Parent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = notifyParent,
                            onCheckedChange = { notifyParent = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = colors.skyAccent)
                        )
                    }
                }

                AnimatedVisibility(visible = uiState.monthlyMarkMessage != null) {
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
                                text = uiState.monthlyMarkMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.emeraldAccent,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onDismissMonthlyMarkFeedback) {
                                Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = colors.textMuted)
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = uiState.monthlyMarkError != null) {
                    InlineAlertCard(message = uiState.monthlyMarkError.orEmpty(), isError = true)
                }

                Button(
                    onClick = {
                        onSaveMonthlyMark(
                            editingMarkId,
                            rollInput,
                            monthLabel,
                            examDate,
                            theoryMarksText.toIntOrNull() ?: 0,
                            theoryMaxText.toIntOrNull() ?: 50,
                            labMarksText.toIntOrNull() ?: 0,
                            labMaxText.toIntOrNull() ?: 50,
                            remarks,
                            publishToAll,
                            notifyParent
                        ) {
                            editingMarkId = ""
                            theoryMarksText = ""
                            labMarksText = ""
                            remarks = ""
                        }
                    },
                    enabled = !uiState.isSavingMonthlyMark,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (editingMarkId.isNotBlank()) colors.amberAccent else colors.skyAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_monthly_mark_button")
                ) {
                    if (uiState.isSavingMonthlyMark) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving Marks...")
                    } else {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (editingMarkId.isNotBlank()) "Update Marks & Notify Parent" else "Publish Marks & Notify Parent",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // 2. Previously Saved / Published Marks List (1-Tap Edit)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.cardSurface,
            border = BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Saved Monthly Marks (${filteredMarks.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = marksFilterQuery,
                    onValueChange = { marksFilterQuery = it },
                    placeholder = { Text("Filter by Roll #, Name, or Month...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.skyAccent,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (filteredMarks.isEmpty()) {
                    Text(
                        text = "No monthly marks entered yet. Use the form above to publish marks for any trainee.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                } else {
                    filteredMarks.forEach { mark ->
                        MonthlyMarkDisplayCard(
                            mark = mark,
                            showEditAction = true,
                            showWhatsAppShare = true,
                            onEditClick = {
                                editingMarkId = mark.id
                                rollInput = mark.roll
                                monthLabel = mark.monthLabel
                                examDate = mark.examDate.ifBlank { AttendanceApiClient.todayIstDateString() }
                                theoryMarksText = mark.theoryMarks.toString()
                                theoryMaxText = mark.theoryMax.toString()
                                labMarksText = mark.labMarks.toString()
                                labMaxText = mark.labMax.toString()
                                remarks = mark.remarks
                                publishToAll = mark.published
                            },
                            onWhatsAppClick = {
                                val statusWord = if (mark.isRevised) "Updated" else "Published"
                                val msg = "Govt. ITI Ranni (EM) • Monthly Marks $statusWord (${mark.monthLabel})\n" +
                                    "Trainee: ${mark.studentName} (Roll #${mark.roll})\n" +
                                    "Trade Theory: ${mark.theoryMarks}/${mark.theoryMax}\n" +
                                    "Trade Practical/Lab: ${mark.labMarks}/${mark.labMax}\n" +
                                    "Total: ${mark.totalObtained}/${mark.totalMax} (${mark.percentage}%)" +
                                    if (mark.remarks.isNotBlank()) "\nInstructor Remarks: ${mark.remarks}" else ""
                                WhatsAppBotHelper.openWhatsAppBot(context, msg)
                            }
                        )
                    }
                }
            }
        }

        // 3. Private Work / Lab Mark Queries Inbox
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.cardSurface,
            border = BorderStroke(1.dp, colors.violetAccent.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("instructor_work_queries_inbox")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trainee Work / Lab Mark Queries (${allQueries.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val openCount = allQueries.count { it.status.equals("OPEN", ignoreCase = true) }
                    if (openCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.amberContainer
                        ) {
                            Text(
                                text = "$openCount OPEN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.amberAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = uiState.workQueryReplyMessage != null) {
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
                                text = uiState.workQueryReplyMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.emeraldAccent,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onDismissWorkQueryReplyFeedback) {
                                Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = colors.textMuted)
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = uiState.workQueryReplyError != null) {
                    InlineAlertCard(message = uiState.workQueryReplyError.orEmpty(), isError = true)
                }

                if (allQueries.isEmpty()) {
                    Text(
                        text = "No trainee Work / Lab Mark review queries right now.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                } else {
                    allQueries.forEach { query ->
                        InstructorWorkQueryReplyCard(
                            query = query,
                            isReplying = uiState.replyingWorkQueryId == query.id,
                            onReply = { status, replyText, reset ->
                                onReplyWorkMarkQuery(query.id, status, replyText, reset)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyMarkDisplayCard(
    mark: MonthlyExamMark,
    showEditAction: Boolean = false,
    showWhatsAppShare: Boolean = false,
    onEditClick: () -> Unit = {},
    onWhatsAppClick: () -> Unit = {}
) {
    val colors = LocalItiColors.current
    val pct = mark.percentage.coerceIn(0, 100)
    val accent = when {
        pct >= 75 -> colors.emeraldAccent
        pct >= 50 -> colors.skyAccent
        else -> colors.amberAccent
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, if (mark.isRevised) colors.violetAccent.copy(alpha = 0.55f) else colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("monthly_mark_card_${mark.roll}_${mark.monthLabel}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = mark.monthLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (mark.isRevised) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.violetContainer
                            ) {
                                Text(
                                    text = "UPDATED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colors.violetAccent,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Roll #${mark.roll} • ${mark.studentName.ifBlank { "Trainee" }}" +
                            if (mark.examDate.isNotBlank()) " • ${mark.examDate}" else "",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MarkScorePill(
                    label = "Trade Theory",
                    score = "${mark.theoryMarks}/${mark.theoryMax}",
                    accent = colors.skyAccent,
                    modifier = Modifier.weight(1f)
                )
                MarkScorePill(
                    label = "Practical / Lab",
                    score = "${mark.labMarks}/${mark.labMax}",
                    accent = colors.emeraldAccent,
                    modifier = Modifier.weight(1f)
                )
                MarkScorePill(
                    label = "Total",
                    score = "${mark.totalObtained}/${mark.totalMax}",
                    accent = accent,
                    modifier = Modifier.weight(0.9f)
                )
            }

            LinearProgressIndicator(
                progress = { pct / 100f },
                color = accent,
                trackColor = colors.borderSubtle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
            )

            if (mark.remarks.isNotBlank()) {
                Text(
                    text = "Remarks: ${mark.remarks}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }

            if (showEditAction || showWhatsAppShare) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showEditAction) {
                        OutlinedButton(
                            onClick = onEditClick,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, colors.amberAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.amberAccent),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("edit_mark_button_${mark.id}")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Marks", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    if (showWhatsAppShare) {
                        OutlinedButton(
                            onClick = onWhatsAppClick,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, colors.emeraldAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.emeraldAccent),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("wa_mark_button_${mark.id}")
                        ) {
                            Icon(Icons.Rounded.Chat, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp Bot", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkScorePill(
    label: String,
    score: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = colors.textMuted,
                maxLines = 1
            )
            Text(
                text = score,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accent
            )
        }
    }
}

@Composable
private fun PrivateWorkQueryCard(query: WorkMarkQuery) {
    val colors = LocalItiColors.current
    val statusUpper = query.status.uppercase()
    val (bg, fg) = when (statusUpper) {
        "RESOLVED" -> colors.emeraldContainer to colors.emeraldAccent
        "REVIEWED" -> colors.skyContainer to colors.skyAccent
        else -> colors.amberContainer to colors.amberAccent
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("private_work_query_${query.id}")
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
                    text = query.workReference,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(shape = RoundedCornerShape(8.dp), color = bg) {
                    Text(
                        text = statusUpper,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = fg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text = listOf(query.monthLabel, query.date, query.time).filter { it.isNotBlank() }.joinToString(" • "),
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                color = colors.textMuted
            )
            if (query.currentMarkNote.isNotBlank()) {
                Text(
                    text = "Noted Mark: ${query.currentMarkNote}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.skyAccent
                )
            }
            Text(
                text = query.traineeNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (query.instructorReply.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.emeraldContainer,
                    border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Instructor Reply: ${query.instructorReply}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.emeraldAccent,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InstructorWorkQueryReplyCard(
    query: WorkMarkQuery,
    isReplying: Boolean,
    onReply: (status: String, reply: String, onReset: () -> Unit) -> Unit
) {
    val colors = LocalItiColors.current
    val context = LocalContext.current
    var selectedStatus by remember(query.id, query.status) {
        mutableStateOf(if (query.status.equals("RESOLVED", ignoreCase = true)) "RESOLVED" else "REVIEWED")
    }
    var replyText by remember(query.id) { mutableStateOf(query.instructorReply) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("instructor_query_card_${query.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Roll #${query.roll} • ${query.studentName}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = query.status.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (query.status.equals("RESOLVED", true)) colors.emeraldAccent else colors.amberAccent
                )
            }
            Text(
                text = "${query.workReference} (${query.monthLabel.ifBlank { query.date }})",
                style = MaterialTheme.typography.labelMedium,
                color = colors.violetAccent
            )
            if (query.currentMarkNote.isNotBlank()) {
                Text(
                    text = "Current Mark Noted: ${query.currentMarkNote}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
            Text(
                text = query.traineeNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("REVIEWED", "RESOLVED").forEach { st ->
                    FilterChip(
                        selected = selectedStatus == st,
                        onClick = { selectedStatus = st },
                        label = { Text(st, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (st == "RESOLVED") colors.emeraldContainer else colors.skyContainer,
                            selectedLabelColor = if (st == "RESOLVED") colors.emeraldAccent else colors.skyAccent
                        )
                    )
                }
            }

            OutlinedTextField(
                value = replyText,
                onValueChange = { replyText = it },
                label = { Text("Instructor Reply / Resolution") },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.emeraldAccent,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedContainerColor = colors.cardSurface,
                    unfocusedContainerColor = colors.cardSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onReply(selectedStatus, replyText) {} },
                    enabled = !isReplying && replyText.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.emeraldAccent,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("reply_query_button_${query.id}")
                ) {
                    Text(
                        text = if (isReplying) "Saving..." else "Save Reply",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = {
                        val waMsg = "ITI Ranni EM • Work/Lab Mark Query Reply\n" +
                            "Trainee: Roll #${query.roll} (${query.studentName})\n" +
                            "Work Ref: ${query.workReference}\n" +
                            "Status: $selectedStatus\n" +
                            "Instructor Note: ${replyText.ifBlank { "Reviewed by Trade Instructor." }}"
                        WhatsAppBotHelper.openWhatsAppBot(context, waMsg)
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, colors.emeraldAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.emeraldAccent),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Rounded.Chat, contentDescription = "Reply via WhatsApp", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
