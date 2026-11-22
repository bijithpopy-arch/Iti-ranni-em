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
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceApiClient
import com.example.data.ParentAlertTypes
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

private val PARENT_ALERT_FILTERS = listOf("ALL", ParentAlertTypes.BUNK_ALERT, ParentAlertTypes.LEAVE_REASON)
private val NOTICE_CATEGORIES = listOf("General", "Exam", "Workshop", "Urgent", "DGT / NCVT", "Holiday")

private enum class CampusSubSection(val label: String) {
    DUTY_TOOLS("Duty/Tools"),
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
    val allAlerts = uiState.staffData?.parentAlerts.orEmpty()
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredAlerts = remember(allAlerts, selectedFilter) {
        if (selectedFilter == "ALL") {
            allAlerts
        } else {
            allAlerts.filter { it.alertType.equals(selectedFilter, ignoreCase = true) }
        }
    }

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
                        ParentAlertTypes.BUNK_ALERT -> "Bunk Alerts"
                        ParentAlertTypes.LEAVE_REASON -> "Leave Reasons"
                        else -> "All (${allAlerts.size})"
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { selectedFilter = filter },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (filter == ParentAlertTypes.BUNK_ALERT) colors.redContainer else colors.skyContainer,
                            selectedLabelColor = if (filter == ParentAlertTypes.BUNK_ALERT) colors.redAccent else colors.skyAccent,
                            containerColor = colors.cardSurface,
                            labelColor = colors.textMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = colors.borderSubtle,
                            selectedBorderColor = if (filter == ParentAlertTypes.BUNK_ALERT) colors.redAccent else colors.skyAccent
                        )
                    )
                }
            }
        }

        if (filteredAlerts.isEmpty()) {
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
                            text = "No Parent Alerts",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Bunking reports (BUNK_ALERT) and English/Manglish/Malayalam leave reasons sent by parents appear here in real time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }
            }
        } else {
            items(filteredAlerts, key = { it.id.ifBlank { "${it.roll}_${it.date}_${it.hashCode()}" } }) { alert ->
                ParentAlertCard(alert = alert)
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
    onEspRollChanged: (String) -> Unit,
    onTriggerManualTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    var activeSection by remember { mutableStateOf(CampusSubSection.DUTY_TOOLS) }

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
        // Sub-section segmented bar
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CampusSubSection.entries.forEach { section ->
                        val selected = activeSection == section
                        Surface(
                            onClick = { activeSection = section },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) colors.skyAccent else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("campus_sub_${section.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
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
        }

        when (activeSection) {
            CampusSubSection.DUTY_TOOLS -> {
                item {
                    DutyAndToolCribPanel(
                        uiState = uiState,
                        onSignOffDuty = onSignOffDuty,
                        onLogToolCrib = onLogToolCrib
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
