package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DutyRotationEngine
import com.example.data.DutyScheduleDay
import com.example.data.HolidayAnnouncement
import com.example.data.LeaveRequestItem
import com.example.data.MonthlyMarkRecord
import com.example.data.NoticeItem
import com.example.data.ProjectGroupEntry
import com.example.data.RunningLateAlert
import com.example.data.WorkMarkQueryItem
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors

@Composable
fun TraineeDutyRotationTab(
    myRollNumber: Int?,
    customDutyItems: List<String>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val schedule = remember(customDutyItems) {
        DutyRotationEngine.generateRotationSchedule(
            totalRolls = DutyRotationEngine.DEFAULT_TOTAL_ROLLS,
            customDutyItems = customDutyItems
        )
    }
    val todaySlot = remember(schedule) { schedule.firstOrNull { it.isToday } }
    val isWeekend = remember { DutyRotationEngine.isWeekendToday() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("trainee_duty_rotation_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
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
                                    imageVector = Icons.Rounded.EventRepeat,
                                    contentDescription = null,
                                    tint = colors.skyAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Egg, Milk & Tool Crib Duty",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "2 trainees per day • Rolls 1–${DutyRotationEngine.DEFAULT_TOTAL_ROLLS}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync")
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (todaySlot != null && !isWeekend) colors.skyContainer else colors.amberContainer,
                        border = BorderStroke(
                            1.dp,
                            if (todaySlot != null && !isWeekend) colors.skyAccent.copy(alpha = 0.5f)
                            else colors.amberAccent.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = when {
                                    isWeekend -> "Weekend Break Today"
                                    todaySlot != null -> "Today's Assigned Pair (${todaySlot.dateLabel})"
                                    else -> "Upcoming Duty Pair"
                                },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (todaySlot != null && !isWeekend) colors.skyAccent else colors.amberAccent
                            )
                            val activePairLabel = todaySlot?.label
                                ?: schedule.firstOrNull()?.label
                                ?: "Roll #14 & Roll #15"
                            Text(
                                text = activePairLabel,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mon–Fri Infinite Duty Rotation",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sat/Sun Skipped • 1–${DutyRotationEngine.DEFAULT_TOTAL_ROLLS}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.textMuted
                )
            }
        }

        items(schedule, key = { "${it.dayNumber}_${it.dateLabel}" }) { slot ->
            DutyScheduleRow(
                slot = slot,
                myRollNumber = myRollNumber
            )
        }
    }
}

@Composable
private fun DutyScheduleRow(
    slot: DutyScheduleDay,
    myRollNumber: Int?
) {
    val colors = LocalItiColors.current
    val containsMe = myRollNumber != null && myRollNumber in slot.rolls
    val pairLabel = slot.rolls.joinToString(" & ") { "Roll #${it.toString().padStart(2, '0')}" }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (slot.isToday) colors.skyContainer else colors.cardSurface,
        border = BorderStroke(
            width = if (slot.isToday || containsMe) 1.5.dp else 1.dp,
            color = when {
                slot.isToday -> colors.skyAccent
                containsMe -> colors.emeraldAccent.copy(alpha = 0.65f)
                else -> colors.borderSubtle
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = pairLabel,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = when {
                            slot.isToday -> colors.skyAccent
                            containsMe -> colors.emeraldAccent
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (slot.isToday) {
                        Surface(shape = RoundedCornerShape(50), color = colors.skyAccent) {
                            Text(
                                text = "TODAY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    } else if (containsMe) {
                        Surface(shape = RoundedCornerShape(50), color = colors.emeraldContainer) {
                            Text(
                                text = "YOUR TURN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.emeraldAccent,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = slot.dutyItems.joinToString(" • ").ifBlank { "Egg & Milk Duty" },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = slot.dateLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Day #${slot.dayNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
        }
    }
}
@Composable
fun TraineeNoticesAndHolidaysTab(
    notices: List<NoticeItem>,
    holidays: List<HolidayAnnouncement>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("trainee_notices_holidays_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = colors.skyAccent
                    )
                    Text(
                        text = "Notices & Holiday Announcements",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                OutlinedButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }
        }

        if (holidays.isNotEmpty()) {
            item {
                Text(
                    text = "Declared Holidays",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.amberAccent
                )
            }
            items(holidays) { holiday ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.amberContainer,
                    border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${holiday.date} • ${holiday.reason}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.amberAccent
                        )
                        Text(
                            text = "Posted by ${holiday.postedBy}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Trade & College Notices",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (notices.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No notices published yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(notices, key = { it.id }) { notice ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(
                        1.dp,
                        if (notice.isPinned) colors.skyAccent.copy(alpha = 0.65f) else colors.borderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
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
                                text = "[${notice.category}] ${notice.title}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = notice.date,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = colors.textMuted
                            )
                        }
                        if (notice.body.isNotBlank()) {
                            Text(
                                text = notice.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "By ${notice.postedBy}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TraineeLeaveAndLateTab(
    rollNumber: String,
    leaves: List<LeaveRequestItem>,
    runningLateAlerts: List<RunningLateAlert>,
    isSubmittingLeave: Boolean,
    isSubmittingLate: Boolean,
    onSubmitLeave: (type: String, timeWindow: String, reason: String) -> Unit,
    onSubmitRunningLate: (expectedArrival: String, reasonCategory: String, details: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    var leaveType by remember { mutableStateOf("Half-Day") }
    var timeWindow by remember { mutableStateOf("Afternoon") }
    var leaveReason by remember { mutableStateOf("") }

    var etaText by remember { mutableStateOf("08:30 AM") }
    var lateCategory by remember { mutableStateOf("KSRTC / Bus Delay") }
    var lateDetails by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("trainee_leave_late_tab"),
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditCalendar,
                            contentDescription = null,
                            tint = colors.skyAccent
                        )
                        Text(
                            text = "Leave / Permission Request (Roll #$rollNumber)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    OutlinedTextField(
                        value = leaveType,
                        onValueChange = { leaveType = it },
                        label = { Text("Type (Half-Day / Full-Day / Gate Pass)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = timeWindow,
                        onValueChange = { timeWindow = it },
                        label = { Text("Time Window / Session") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = leaveReason,
                        onValueChange = { leaveReason = it },
                        label = { Text("Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (leaveReason.isNotBlank()) {
                                onSubmitLeave(leaveType, timeWindow, leaveReason.trim())
                                leaveReason = ""
                            }
                        },
                        enabled = !isSubmittingLeave && leaveReason.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSubmittingLeave) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Submit Leave Request")
                        }
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.5f)),
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
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = colors.amberAccent
                        )
                        Text(
                            text = "Running Late Alert to Instructor",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    OutlinedTextField(
                        value = etaText,
                        onValueChange = { etaText = it },
                        label = { Text("Expected Arrival Time (ETA)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = lateCategory,
                        onValueChange = { lateCategory = it },
                        label = { Text("Reason Category") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = lateDetails,
                        onValueChange = { lateDetails = it },
                        label = { Text("Additional Details (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onSubmitRunningLate(etaText.trim(), lateCategory.trim(), lateDetails.trim())
                            lateDetails = ""
                        },
                        enabled = !isSubmittingLate,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.amberAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSubmittingLate) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Send Running Late Alert", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }

        if (leaves.isNotEmpty()) {
            item {
                Text(
                    text = "My Leave Requests",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(leaves, key = { it.id }) { item ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.type} • ${item.date}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = item.status,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = colors.skyAccent
                            )
                        }
                        Text(
                            text = item.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }
            }
        }

        if (runningLateAlerts.isNotEmpty()) {
            item {
                Text(
                    text = "Sent Running Late Alerts",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(runningLateAlerts, key = { it.id }) { rl ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${rl.date} (${rl.time}) • ETA ${rl.expectedArrival}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${rl.reasonCategory}${if (rl.details.isNotBlank()) " — ${rl.details}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun TraineeMarksAndQueryTab(
    rollNumber: String,
    monthlyMarks: List<MonthlyMarkRecord>,
    workMarkQueries: List<WorkMarkQueryItem>,
    isSubmittingQuery: Boolean,
    onSubmitQuery: (monthLabel: String, workReference: String, currentMarkNote: String, traineeNote: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    var monthLabel by remember { mutableStateOf("November 2026") }
    var workRef by remember { mutableStateOf("Trade Theory / Lab Work") }
    var currentNote by remember { mutableStateOf("") }
    var traineeNote by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("trainee_marks_query_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Published Monthly Marks (Roll #$rollNumber)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (monthlyMarks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No monthly marks published yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(monthlyMarks, key = { it.id }) { mark ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = mark.monthLabel,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${mark.percentage}%",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.emeraldAccent
                            )
                        }
                        Text(
                            text = "Theory: ${mark.theoryMarks}/${mark.theoryMax} • Lab: ${mark.labMarks}/${mark.labMax} • Total: ${mark.totalObtained}/${mark.totalMax}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (mark.remarks.isNotBlank()) {
                            Text(
                                text = "Remarks: ${mark.remarks}",
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
                shape = RoundedCornerShape(18.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.skyAccent.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Ask Work / Lab Mark Review",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = monthLabel,
                        onValueChange = { monthLabel = it },
                        label = { Text("Month / Exam Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = workRef,
                        onValueChange = { workRef = it },
                        label = { Text("Lab Job / Theory Topic Reference") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = currentNote,
                        onValueChange = { currentNote = it },
                        label = { Text("Current Mark Note (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = traineeNote,
                        onValueChange = { traineeNote = it },
                        label = { Text("Your Question / Note for Instructor") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (traineeNote.isNotBlank()) {
                                onSubmitQuery(
                                    monthLabel.trim(),
                                    workRef.trim(),
                                    currentNote.trim(),
                                    traineeNote.trim()
                                )
                                traineeNote = ""
                            }
                        },
                        enabled = !isSubmittingQuery && traineeNote.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSubmittingQuery) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Send Mark Query")
                        }
                    }
                }
            }
        }

        if (workMarkQueries.isNotEmpty()) {
            item {
                Text(
                    text = "My Mark Queries",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(workMarkQueries, key = { it.id }) { q ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = q.workReference.ifBlank { q.monthLabel },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = q.status,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = colors.skyAccent
                            )
                        }
                        Text(
                            text = q.traineeNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (q.instructorReply.isNotBlank()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
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
fun TraineeProjectGroupsTab(
    rollNumber: String,
    projectGroupsEnabled: Boolean,
    projectGroups: List<ProjectGroupEntry>,
    isSubmittingGroup: Boolean,
    onSaveProjectGroup: (projectTitle: String, leaderRoll: String, leaderName: String, members: String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    var title by remember { mutableStateOf("") }
    var leaderRoll by remember { mutableStateOf(rollNumber) }
    var leaderName by remember { mutableStateOf("") }
    var members by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("trainee_project_groups_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Project Groups",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }
        }

        if (!projectGroupsEnabled) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.amberContainer,
                    border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = colors.amberAccent
                        )
                        Text(
                            text = "Project Group submission is currently locked by the Trade Instructor.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.amberAccent
                        )
                    }
                }
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Register / Update Project Group",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Project Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = leaderRoll,
                            onValueChange = { leaderRoll = it },
                            label = { Text("Group Leader Roll #") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = leaderName,
                            onValueChange = { leaderName = it },
                            label = { Text("Group Leader Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = members,
                            onValueChange = { members = it },
                            label = { Text("Team Members (Rolls / Names)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onSaveProjectGroup(
                                        title.trim(),
                                        leaderRoll.trim().ifBlank { rollNumber },
                                        leaderName.trim(),
                                        members.trim()
                                    )
                                    title = ""
                                    members = ""
                                }
                            },
                            enabled = !isSubmittingGroup && title.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSubmittingGroup) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Project Group")
                            }
                        }
                    }
                }
            }
        }

        items(projectGroups, key = { it.groupId }) { group ->
            ProjectGroupSummaryCard(group = group)
        }
    }
}

@Composable
fun ProjectGroupSummaryCard(
    group: ProjectGroupEntry,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardSurface),
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier.fillMaxWidth()
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Groups,
                        contentDescription = null,
                        tint = colors.skyAccent
                    )
                    Text(
                        text = group.projectTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = group.groupId,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.textMuted
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = colors.emeraldAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Leader: Roll #${group.leaderRoll}${if (group.leaderName.isNotBlank()) " (${group.leaderName})" else ""}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (group.members.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = colors.skyAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Members: ${group.members}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }
            }
        }
    }
}
@Composable
fun DutyChartTab(
    myRollNumber: Int?,
    customDutyItems: List<String> = emptyList(),
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TraineeDutyRotationTab(
        myRollNumber = myRollNumber,
        customDutyItems = customDutyItems,
        onRefresh = onRefresh,
        modifier = modifier
    )
}
@Composable
fun ProjectGroupsTab(
    rollNumber: String,
    projectGroupsEnabled: Boolean,
    projectGroups: List<ProjectGroupEntry>,
    isSubmittingGroup: Boolean,
    onSaveProjectGroup: (projectTitle: String, leaderRoll: String, leaderName: String, members: String) -> Unit,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TraineeProjectGroupsTab(
        rollNumber = rollNumber,
        projectGroupsEnabled = projectGroupsEnabled,
        projectGroups = projectGroups,
        isSubmittingGroup = isSubmittingGroup,
        onSaveProjectGroup = onSaveProjectGroup,
        onRefresh = onRefresh,
        modifier = modifier
    )
}
