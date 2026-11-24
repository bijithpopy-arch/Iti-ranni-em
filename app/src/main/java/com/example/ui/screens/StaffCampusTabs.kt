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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceApiClient
import com.example.data.ParentAlertTypes
import com.example.data.RunningLateAlert
import com.example.data.TimetablePeriod
import com.example.notifications.AttendanceNotificationHelper
import com.example.ui.components.WhatsAppBotHelper
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

private val PARENT_ALERT_FILTERS = listOf(
    "ALL",
    ParentAlertTypes.TRAINEE_RUNNING_LATE,
    ParentAlertTypes.BUNK_ALERT,
    ParentAlertTypes.LEAVE_REASON,
    ParentAlertTypes.MARKS_PUBLISHED
)
private val NOTICE_CATEGORIES = listOf("General", "Exam", "Workshop", "Urgent", "DGT / NCVT", "Holiday")

private enum class CampusSubSection(val label: String) {
    DUTY_TOOLS("Duty/Tools"),
    TIMETABLE("Timetable"),
    MARKS("Marks & Queries"),
    NOTICES("Notices"),
    HOLIDAYS("Holidays"),
    PROJECTS("Projects"),
    ESP("ESP")
}

@Composable
fun ParentAlertsTab(
    uiState: AppUiState,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val context = LocalContext.current
    val allParentAlerts = uiState.staffData?.parentAlerts.orEmpty()
    val runningLateAlerts = uiState.staffData?.runningLateAlerts.orEmpty()
    var selectedFilter by remember { mutableStateOf("ALL") }

    val showRunningLate = selectedFilter == "ALL" || selectedFilter == ParentAlertTypes.TRAINEE_RUNNING_LATE
    val filteredParentAlerts = remember(allParentAlerts, selectedFilter) {
        when (selectedFilter) {
            "ALL" -> allParentAlerts
            ParentAlertTypes.TRAINEE_RUNNING_LATE -> emptyList()
            ParentAlertTypes.MARKS_PUBLISHED -> allParentAlerts.filter {
                it.alertType.equals(ParentAlertTypes.MARKS_PUBLISHED, true) ||
                    it.alertType.equals(ParentAlertTypes.MARKS_UPDATED, true)
            }
            else -> allParentAlerts.filter { it.alertType.equals(selectedFilter, ignoreCase = true) }
        }
    }

    val totalCount = allParentAlerts.size + runningLateAlerts.size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("staff_parent_alerts_tab"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PARENT_ALERT_FILTERS) { filter ->
                    val selected = selectedFilter == filter
                    val label = when (filter) {
                        ParentAlertTypes.TRAINEE_RUNNING_LATE -> "Running Late (${runningLateAlerts.size})"
                        ParentAlertTypes.BUNK_ALERT -> "Bunk Alerts"
                        ParentAlertTypes.LEAVE_REASON -> "Leave Reasons"
                        ParentAlertTypes.MARKS_PUBLISHED -> "Marks Alerts"
                        else -> "All ($totalCount)"
                    }
                    val selBg = when (filter) {
                        ParentAlertTypes.TRAINEE_RUNNING_LATE -> colors.amberContainer
                        ParentAlertTypes.BUNK_ALERT -> colors.redContainer
                        ParentAlertTypes.MARKS_PUBLISHED -> colors.emeraldContainer
                        else -> colors.skyContainer
                    }
                    val selFg = when (filter) {
                        ParentAlertTypes.TRAINEE_RUNNING_LATE -> colors.amberAccent
                        ParentAlertTypes.BUNK_ALERT -> colors.redAccent
                        ParentAlertTypes.MARKS_PUBLISHED -> colors.emeraldAccent
                        else -> colors.skyAccent
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { selectedFilter = filter },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = selBg,
                            selectedLabelColor = selFg,
                            containerColor = colors.cardSurface,
                            labelColor = colors.textMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = colors.borderSubtle,
                            selectedBorderColor = selFg
                        )
                    )
                }
            }
        }

        if (showRunningLate && runningLateAlerts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trainee Running Late Alerts (${runningLateAlerts.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.amberAccent
                    )
                    Text(
                        text = "LIVE DELAY FEED",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = colors.textMuted
                    )
                }
            }
            items(runningLateAlerts, key = { "rl_${it.id}_${it.roll}_${it.time}" }) { lateAlert ->
                InstructorRunningLateAlertCard(
                    alert = lateAlert,
                    onWhatsAppReply = {
                        val waMsg = "Govt. ITI Ranni (EM) • Running Late Acknowledged\n" +
                            "Trainee: ${lateAlert.studentName} (Roll #${lateAlert.roll})\n" +
                            "Noted ETA: ${lateAlert.expectedArrival} (${lateAlert.reasonCategory}). Report to Trade Instructor on arrival."
                        WhatsAppBotHelper.openWhatsAppBot(context, waMsg)
                    }
                )
            }
        }

        if (filteredParentAlerts.isEmpty() && (!showRunningLate || runningLateAlerts.isEmpty())) {
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
                                .background(colors.skyContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FamilyRestroom,
                                contentDescription = null,
                                tint = colors.skyAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "No Alerts Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Trainee Running Late alerts, Parent bunk reports, leave reasons, and published mark alerts appear here in real time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }
            }
        } else {
            items(filteredParentAlerts, key = { it.id.ifBlank { "${it.roll}_${it.date}_${it.hashCode()}" } }) { alert ->
                ParentAlertCard(alert = alert)
            }
        }
    }
}

@Composable
private fun InstructorRunningLateAlertCard(
    alert: RunningLateAlert,
    onWhatsAppReply: () -> Unit
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.5.dp, colors.amberAccent.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("instructor_running_late_card_${alert.id}")
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.amberContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DirectionsBus,
                                contentDescription = null,
                                tint = colors.amberAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "RUNNING LATE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.amberAccent
                            )
                        }
                    }
                    Text(
                        text = "Roll #${alert.roll} • ${alert.studentName}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Expected Arrival: ${alert.expectedArrival}  •  Reason: ${alert.reasonCategory}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.amberAccent
                    )
                    if (alert.details.isNotBlank()) {
                        Text(
                            text = alert.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onWhatsAppReply,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, colors.emeraldAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.emeraldAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Rounded.Chat, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Acknowledge via WhatsApp", style = MaterialTheme.typography.labelSmall)
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
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onDismissMonthlyMarkFeedback: () -> Unit = {},
    onReplyWorkMarkQuery: (queryId: String, status: String, reply: String, onSuccessReset: () -> Unit) -> Unit = { _, _, _, _ -> },
    onDismissWorkQueryReplyFeedback: () -> Unit = {},
    onSaveDutyOverride: (
        dateKey: String,
        firstRoll: Int,
        secondRoll: Int,
        dutyNote: String,
        reanchorFromHere: Boolean,
        resetToAuto: Boolean
    ) -> Unit = { _, _, _, _, _, _ -> },
    onDismissDutyOverrideFeedback: () -> Unit = {},
    onPublishTimetable: (periods: List<TimetablePeriod>, allowTraineeEdit: Boolean) -> Unit = { _, _ -> },
    onDismissTimetableFeedback: () -> Unit = {},
    onEspRollChanged: (String) -> Unit,
    onTriggerManualTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val context = LocalContext.current
    var activeSection by remember { mutableStateOf(CampusSubSection.DUTY_TOOLS) }
    var botPhoneInput by remember { mutableStateOf(WhatsAppBotHelper.getBotPhoneNumber(context)) }
    var botSavedBanner by remember { mutableStateOf<String?>(null) }

    // Notice form state
    var noticeTitle by remember { mutableStateOf("") }
    var noticeCategory by remember { mutableStateOf(NOTICE_CATEGORIES.first()) }
    var noticeBody by remember { mutableStateOf("") }

    // Holiday form state
    var holidayDate by remember { mutableStateOf(AttendanceApiClient.todayIstDateString()) }
    var holidayReason by remember { mutableStateOf("") }

    val staffData = uiState.staffData

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("staff_campus_hub_tab"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Sub-section scrollable pill bar
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(CampusSubSection.entries) { section ->
                    val selected = activeSection == section
                    Surface(
                        onClick = { activeSection = section },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) colors.skyAccent else colors.cardSurface,
                        border = BorderStroke(1.dp, if (selected) colors.skyAccent else colors.borderSubtle),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("campus_sub_${section.name.lowercase()}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = section.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else colors.textMuted
                            )
                        }
                    }
                }
            }
        }

        when (activeSection) {
            CampusSubSection.DUTY_TOOLS -> {
                item {
                    // WhatsApp Bot Link Number & Loud Alert Tone Tester Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.cardSurface,
                        border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("whatsapp_bot_config_card")
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
                                    Text(
                                        text = "WhatsApp Bot & Loud Alert Sound",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Configure wa.me bot number for marks/alerts & test loud alarm tone",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted
                                    )
                                }
                                IconButton(
                                    onClick = { AttendanceNotificationHelper.playLoudAlertTone(context) },
                                    modifier = Modifier.testTag("test_loud_alert_sound_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VolumeUp,
                                        contentDescription = "Test loud alert sound",
                                        tint = colors.amberAccent
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = botPhoneInput,
                                    onValueChange = { botPhoneInput = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) },
                                    label = { Text("WhatsApp Bot Number (with country code)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.emeraldAccent,
                                        unfocusedBorderColor = colors.borderSubtle,
                                        focusedContainerColor = MaterialTheme.colorScheme.background,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("whatsapp_bot_number_input")
                                )
                                Button(
                                    onClick = {
                                        val saved = WhatsAppBotHelper.setBotPhoneNumber(context, botPhoneInput)
                                        botPhoneInput = saved
                                        botSavedBanner = "WhatsApp Bot saved: wa.me/$saved"
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.emeraldAccent,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    ),
                                    modifier = Modifier
                                        .height(52.dp)
                                        .testTag("save_whatsapp_bot_button")
                                ) {
                                    Text("Save", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                            if (botSavedBanner != null) {
                                InlineAlertCard(message = botSavedBanner.orEmpty(), isError = false)
                            }
                        }
                    }
                }
                item {
                    DutyAndToolCribPanel(
                        uiState = uiState,
                        onSignOffDuty = onSignOffDuty,
                        onLogToolCrib = onLogToolCrib,
                        onSaveDutyOverride = onSaveDutyOverride,
                        onDismissDutyOverrideFeedback = onDismissDutyOverrideFeedback
                    )
                }
            }

            CampusSubSection.TIMETABLE -> {
                item {
                    InstructorTimetableEditorTab(
                        officialPeriods = staffData?.timetable?.ifEmpty { uiState.officialTimetableCache }
                            ?: uiState.officialTimetableCache,
                        traineeEditEnabled = staffData?.traineeTimetableEditEnabled ?: false,
                        isSaving = uiState.isSavingTimetable,
                        feedbackMessage = uiState.timetableFeedbackMessage,
                        errorMessage = uiState.timetableError,
                        onPublishTimetable = onPublishTimetable,
                        onDismissFeedback = onDismissTimetableFeedback
                    )
                }
            }

            CampusSubSection.MARKS -> {
                item {
                    InstructorMarksAndQueriesPanel(
                        uiState = uiState,
                        onSaveMonthlyMark = onSaveMonthlyMark,
                        onDismissMonthlyMarkFeedback = onDismissMonthlyMarkFeedback,
                        onReplyWorkMarkQuery = onReplyWorkMarkQuery,
                        onDismissWorkQueryReplyFeedback = onDismissWorkQueryReplyFeedback
                    )
                }
            }

            CampusSubSection.ESP -> {
                item {
                    Box(modifier = Modifier.height(520.dp)) {
                        EspTerminalTab(
                            uiState = uiState,
                            onRollChanged = onEspRollChanged,
                            onTriggerManualTap = onTriggerManualTap
                        )
                    }
                }
            }

            CampusSubSection.NOTICES -> {
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
                                        imageVector = Icons.Rounded.Campaign,
                                        contentDescription = null,
                                        tint = colors.skyAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Publish College / Trade Notice",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Triggers a high-priority popup on Trainee devices",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted
                                    )
                                }
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(NOTICE_CATEGORIES) { cat ->
                                    val selected = noticeCategory == cat
                                    FilterChip(
                                        selected = selected,
                                        onClick = { noticeCategory = cat },
                                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = colors.skyContainer,
                                            selectedLabelColor = colors.skyAccent,
                                            containerColor = MaterialTheme.colorScheme.background,
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

                            OutlinedTextField(
                                value = noticeTitle,
                                onValueChange = { noticeTitle = it },
                                label = { Text("Notice Title") },
                                placeholder = { Text("e.g., Practical Exam Schedule / പ്രാക്ടിക്കൽ പരീക്ഷ") },
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
                                    .testTag("staff_notice_title_input")
                            )

                            OutlinedTextField(
                                value = noticeBody,
                                onValueChange = { noticeBody = it },
                                label = { Text("Message Body (English / Manglish / മലയാളം)") },
                                placeholder = { Text("Enter full circular details...") },
                                minLines = 3,
                                maxLines = 6,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.skyAccent,
                                    unfocusedBorderColor = colors.borderSubtle,
                                    focusedContainerColor = MaterialTheme.colorScheme.background,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("staff_notice_body_input")
                            )

                            AnimatedVisibility(visible = uiState.noticePostMessage != null) {
                                InlineAlertCard(message = uiState.noticePostMessage.orEmpty(), isError = false)
                            }
                            AnimatedVisibility(visible = uiState.noticePostError != null) {
                                InlineAlertCard(message = uiState.noticePostError.orEmpty(), isError = true)
                            }

                            Button(
                                onClick = {
                                    onPostNotice(noticeTitle, noticeCategory, noticeBody) {
                                        noticeTitle = ""
                                        noticeBody = ""
                                    }
                                },
                                enabled = !uiState.isPostingNotice,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.skyAccent,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("staff_publish_notice_button")
                            ) {
                                if (uiState.isPostingNotice) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Publishing Notice...")
                                } else {
                                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Publish Notice",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                val postedNotices = staffData?.notices.orEmpty()
                if (postedNotices.isNotEmpty()) {
                    item {
                        Text(
                            text = "Published Notices (${postedNotices.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    items(postedNotices, key = { "staff_notice_${it.id}" }) { notice ->
                        NoticeCardItem(notice = notice, isUnread = false)
                    }
                }
            }

            CampusSubSection.HOLIDAYS -> {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.cardSurface,
                        border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.5f)),
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
                                        .background(colors.amberContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Celebration,
                                        contentDescription = null,
                                        tint = colors.amberAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Declare Holiday",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Shows Holiday banner on dashboards & prevents false ABSENT alerts",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = holidayDate,
                                onValueChange = { holidayDate = it },
                                label = { Text("Holiday Date (DD-MM-YYYY)") },
                                placeholder = { Text("22-11-2026") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = colors.amberAccent)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.amberAccent,
                                    unfocusedBorderColor = colors.borderSubtle,
                                    focusedContainerColor = MaterialTheme.colorScheme.background,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("staff_holiday_date_input")
                            )

                            OutlinedTextField(
                                value = holidayReason,
                                onValueChange = { holidayReason = it },
                                label = { Text("Holiday Reason (English / Manglish / മലയാളം)") },
                                placeholder = { Text("e.g., മഴ കാരണം കളക്ടർ അവധി പ്രഖ്യാപിച്ചു / Heavy Rain District Holiday") },
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.amberAccent,
                                    unfocusedBorderColor = colors.borderSubtle,
                                    focusedContainerColor = MaterialTheme.colorScheme.background,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("staff_holiday_reason_input")
                            )

                            AnimatedVisibility(visible = uiState.holidayPostMessage != null) {
                                InlineAlertCard(message = uiState.holidayPostMessage.orEmpty(), isError = false)
                            }
                            AnimatedVisibility(visible = uiState.holidayPostError != null) {
                                InlineAlertCard(message = uiState.holidayPostError.orEmpty(), isError = true)
                            }

                            Button(
                                onClick = {
                                    onPostHoliday(holidayDate, holidayReason) {
                                        holidayReason = ""
                                    }
                                },
                                enabled = !uiState.isPostingHoliday,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.amberAccent,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("staff_post_holiday_button")
                            ) {
                                if (uiState.isPostingHoliday) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onTertiary,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Posting Holiday...")
                                } else {
                                    Icon(Icons.Rounded.Celebration, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Publish Holiday Announcement",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                val holidays = staffData?.holidays.orEmpty()
                if (holidays.isNotEmpty()) {
                    item {
                        Text(
                            text = "Declared Holidays (${holidays.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    items(holidays, key = { "${it.id}_${it.date}" }) { holiday ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.cardSurface,
                            border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.amberContainer
                                ) {
                                    Text(
                                        text = holiday.date,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = colors.amberAccent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                Text(
                                    text = holiday.reason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            CampusSubSection.PROJECTS -> {
                val enabled = staffData?.projectGroupsEnabled ?: false
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.cardSurface,
                        border = BorderStroke(
                            1.dp,
                            if (enabled) colors.emeraldAccent.copy(alpha = 0.6f) else colors.borderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (enabled) colors.emeraldContainer else colors.amberContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (enabled) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                            contentDescription = null,
                                            tint = if (enabled) colors.emeraldAccent else colors.amberAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Trainee Project Groups Access",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (enabled) {
                                                "UNLOCKED — Trainees can form groups (max 4 + 1 Leader)"
                                            } else {
                                                "LOCKED — Hidden from trainee submissions"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (enabled) colors.emeraldAccent else colors.amberAccent
                                        )
                                    }
                                }

                                Switch(
                                    checked = enabled,
                                    onCheckedChange = { onToggleProjectGroupsAccess(it) },
                                    enabled = !uiState.isTogglingProjectAccess,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                                        checkedTrackColor = colors.emeraldAccent,
                                        uncheckedThumbColor = colors.textMuted,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.background
                                    ),
                                    modifier = Modifier.testTag("staff_project_access_switch")
                                )
                            }

                            AnimatedVisibility(visible = uiState.projectAccessMessage != null) {
                                InlineAlertCard(message = uiState.projectAccessMessage.orEmpty(), isError = false)
                            }
                            AnimatedVisibility(visible = uiState.projectAccessError != null) {
                                InlineAlertCard(message = uiState.projectAccessError.orEmpty(), isError = true)
                            }
                        }
                    }
                }

                val groups = staffData?.projectGroups.orEmpty()
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Created Project Groups (${groups.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = onRefreshProjectGroups,
                            modifier = Modifier.testTag("staff_refresh_projects_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh groups", tint = colors.skyAccent)
                        }
                    }
                }

                if (groups.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
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
                                Icon(Icons.Rounded.Groups, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(34.dp))
                                Text(
                                    text = "No Project Groups Submitted Yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Enable access above so trainees can register their 4-member project teams.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }
                } else {
                    items(groups, key = { "staff_pg_${it.groupId}" }) { group ->
                        ProjectGroupSummaryCard(group = group)
                    }
                }
            }
        }
    }
}
private fun readQueryField(obj: Any, vararg getterNames: String): String {
    for (name in getterNames) {
        val value = runCatching { obj.javaClass.getMethod(name).invoke(obj)?.toString() }.getOrNull()
        if (!value.isNullOrBlank()) return value
    }
    return ""
}

@Composable
private fun InstructorMarksAndQueriesPanel(
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
    var rollInput by remember { mutableStateOf("1") }
    var monthLabel by remember { mutableStateOf("Nov 2026") }
    var examDate by remember { mutableStateOf(AttendanceApiClient.todayIstDateString()) }
    var theoryText by remember { mutableStateOf("") }
    var labText by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var publishToPortal by remember { mutableStateOf(true) }
    var notifyParent by remember { mutableStateOf(true) }

    val traineeName = remember(rollInput, uiState.staffData?.allAttendance) {
        val clean = rollInput.trim()
        uiState.staffData?.allAttendance?.firstOrNull { it.roll.trim() == clean }?.name.orEmpty()
    }
    val queries = uiState.staffData?.workMarkQueries.orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                Text(
                    text = "Monthly Theory & Lab Exam Marks",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (traineeName.isNotBlank()) "Selected: Roll #$rollInput • $traineeName" else "Enter trainee Roll Number (1–23) and marks",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (traineeName.isNotBlank()) colors.emeraldAccent else colors.textMuted
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = rollInput,
                        onValueChange = { rollInput = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Roll (1–23)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.skyAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier.weight(0.9f)
                    )
                    OutlinedTextField(
                        value = monthLabel,
                        onValueChange = { monthLabel = it },
                        label = { Text("Exam Month") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.skyAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier.weight(1.3f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = theoryText,
                        onValueChange = { theoryText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Theory / 50") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.skyAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = labText,
                        onValueChange = { labText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Lab / 100") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.skyAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Instructor Remarks (Optional)") },
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Publish to Trainee/Parent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = publishToPortal, onCheckedChange = { publishToPortal = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Send Parent Notification", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = notifyParent, onCheckedChange = { notifyParent = it })
                }

                AnimatedVisibility(visible = uiState.monthlyMarkMessage != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InlineAlertCard(message = uiState.monthlyMarkMessage.orEmpty(), isError = false)
                        androidx.compose.material3.TextButton(onClick = onDismissMonthlyMarkFeedback) {
                            Text("Dismiss")
                        }
                    }
                }
                AnimatedVisibility(visible = uiState.monthlyMarkError != null) {
                    InlineAlertCard(message = uiState.monthlyMarkError.orEmpty(), isError = true)
                }

                Button(
                    onClick = {
                        onSaveMonthlyMark(
                            "",
                            rollInput,
                            monthLabel,
                            examDate,
                            theoryText.toIntOrNull() ?: 0,
                            50,
                            labText.toIntOrNull() ?: 0,
                            100,
                            remarks,
                            publishToPortal,
                            notifyParent
                        ) {
                            theoryText = ""
                            labText = ""
                            remarks = ""
                        }
                    },
                    enabled = !uiState.isSavingMonthlyMark,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.emeraldAccent,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (uiState.isSavingMonthlyMark) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving Mark...")
                    } else {
                        Text("Save & Publish Exam Mark", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.cardSurface,
            border = BorderStroke(1.dp, colors.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Trainee Work / Lab Mark Queries (${queries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                AnimatedVisibility(visible = uiState.workQueryReplyMessage != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InlineAlertCard(message = uiState.workQueryReplyMessage.orEmpty(), isError = false)
                        androidx.compose.material3.TextButton(onClick = onDismissWorkQueryReplyFeedback) {
                            Text("Dismiss")
                        }
                    }
                }
                AnimatedVisibility(visible = uiState.workQueryReplyError != null) {
                    InlineAlertCard(message = uiState.workQueryReplyError.orEmpty(), isError = true)
                }

                if (queries.isEmpty()) {
                    Text(
                        text = "No trainee work/lab mark review queries submitted yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                } else {
                    queries.forEach { q ->
                        val qId = readQueryField(q, "getId", "getQueryId")
                        val qRoll = readQueryField(q, "getRoll")
                        val qName = readQueryField(q, "getStudentName", "getName")
                        val qRef = readQueryField(q, "getWorkReference", "getMonthLabel")
                        val qNote = readQueryField(q, "getTraineeNote", "getNote")
                        val qStatus = readQueryField(q, "getStatus").ifBlank { "OPEN" }
                        val existingReply = readQueryField(q, "getInstructorReply", "getReply")
                        var replyText by remember(qId) { mutableStateOf(existingReply) }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Roll #$qRoll ${if (qName.isNotBlank()) "• $qName" else ""} [$qStatus]",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.skyAccent
                                )
                                if (qRef.isNotBlank()) {
                                    Text(text = qRef, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                                if (qNote.isNotBlank()) {
                                    Text(text = qNote, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                                }
                                OutlinedTextField(
                                    value = replyText,
                                    onValueChange = { replyText = it },
                                    label = { Text("Instructor Reply") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        onReplyWorkMarkQuery(qId, "RESOLVED", replyText) {}
                                    },
                                    enabled = uiState.replyingWorkQueryId != qId && qId.isNotBlank(),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (uiState.replyingWorkQueryId == qId) "Sending..." else "Resolve & Send Reply")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
