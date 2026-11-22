package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.DutyPairSlot
import com.example.data.DutyRotationEngine
import com.example.data.ProjectGroup
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DutyChartTab(
    myRollNumber: Int?,
    customDutyItems: List<String>,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val schedule = remember(customDutyItems) {
        DutyRotationEngine.generateRotationSchedule(
            totalRolls = DutyRotationEngine.DEFAULT_TOTAL_ROLLS,
            customDutyItems = customDutyItems
        )
    }
    val todaySlot = schedule.firstOrNull()
    val isWeekend = remember { DutyRotationEngine.isWeekendToday() }
    val daysUntilMyTurn = remember(myRollNumber, schedule) {
        myRollNumber?.let { DutyRotationEngine.workingDaysUntilRollTurn(it, schedule) }
    }
    val amOnDutyToday = !isWeekend && myRollNumber != null && todaySlot?.rolls?.contains(myRollNumber) == true

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("duty_chart_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Today's On-Duty Spotlight Card
        if (todaySlot != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(
                        width = 1.5.dp,
                        color = if (amOnDutyToday) colors.emeraldAccent else colors.skyAccent.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("today_duty_spotlight_card")
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (amOnDutyToday) colors.emeraldContainer else colors.skyContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.LocalCafe,
                                        contentDescription = null,
                                        tint = if (amOnDutyToday) colors.emeraldAccent else colors.skyAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (isWeekend) "Next Mon–Fri On-Duty Pair" else "Today's On-Duty Pair",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = todaySlot.dateLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = colors.skyAccent
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (amOnDutyToday) colors.emeraldContainer else colors.skyContainer
                            ) {
                                Text(
                                    text = if (amOnDutyToday) "YOUR TURN!" else "DAY ${todaySlot.dayNumber}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (amOnDutyToday) colors.emeraldAccent else colors.skyAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Highlighted Roll Pair Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            todaySlot.rolls.forEach { roll ->
                                val isMe = roll == myRollNumber
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isMe) colors.emeraldContainer else MaterialTheme.colorScheme.background,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isMe) colors.emeraldAccent else colors.skyAccent.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "ROLL #${roll.toString().padStart(2, '0')}",
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontFamily = JetBrainsMonoFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (isMe) colors.emeraldAccent else colors.skyAccent
                                        )
                                        Text(
                                            text = if (isMe) "You are on duty" else "On Duty Today",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textMuted
                                        )
                                    }
                                }
                            }
                        }

                        // Duty Items
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "TODAY'S DUTY ITEMS",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = colors.textMuted
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                todaySlot.dutyItems.forEach { itemName ->
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = colors.amberContainer,
                                        border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.45f))
                                    ) {
                                        Text(
                                            text = itemName,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = colors.amberAccent,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Upcoming personal countdown if not today
                        if (daysUntilMyTurn != null && daysUntilMyTurn > 0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.background,
                                border = BorderStroke(1.dp, colors.borderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = colors.skyAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Your next Mon–Fri turn (Roll #$myRollNumber) is in $daysUntilMyTurn working day(s).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
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
    slot: DutyPairSlot,
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
                        color = if (slot.isToday) colors.skyAccent else MaterialTheme.colorScheme.onSurface
                    )
                    if (containsMe) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.emeraldContainer
                        ) {
                            Text(
                                text = "YOU",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.emeraldAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = slot.dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, colors.borderSubtle)
            ) {
                Text(
                    text = "Cycle Day ${slot.dayNumber}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.textMuted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectGroupsTab(
    myRollNumber: Int?,
    myName: String,
    isEnabled: Boolean,
    groups: List<ProjectGroup>,
    isSaving: Boolean,
    saveSuccessMessage: String?,
    saveErrorMessage: String?,
    onSaveGroup: (title: String, memberRolls: List<Int>, leaderRoll: Int?, leaderName: String, onReset: () -> Unit) -> Unit,
    onRefreshGroups: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current

    var projectTitle by remember { mutableStateOf("") }
    var rollToAdd by remember { mutableStateOf("") }
    val selectedMembers = remember(myRollNumber) {
        mutableStateListOf<Int>().apply {
            if (myRollNumber != null && myRollNumber in 1..33) add(myRollNumber)
        }
    }
    var selectedLeaderRoll by remember(myRollNumber) { mutableStateOf(myRollNumber) }
    var leaderName by remember(myName) { mutableStateOf(myName) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("project_groups_tab"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!isEnabled) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("project_groups_locked_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(colors.amberContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Project Groups Locked",
                                tint = colors.amberAccent,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(
                            text = "Project Groups Locked",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "This section is gated by your Trade Instructor. Once the Instructor enables Project Groups access, you can form a team of up to 4 trainees with 1 Group Leader.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                        OutlinedButton(
                            onClick = onRefreshGroups,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.skyAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.skyAccent),
                            modifier = Modifier.testTag("project_groups_check_access_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check Access Status")
                        }
                    }
                }
            }
        } else {
            // Group Creation / Submission Card
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colors.emeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Workspaces,
                                    contentDescription = null,
                                    tint = colors.emeraldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Register Project Group",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Max 4 members total • Select exactly 1 Leader",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.emeraldAccent
                                )
                            }
                        }

                        OutlinedTextField(
                            value = projectTitle,
                            onValueChange = { projectTitle = it },
                            label = { Text("Project Title") },
                            placeholder = { Text("e.g., ESP8266 Smart Energy Meter") },
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
                                .testTag("project_title_input")
                        )

                        // Add Member Roll (Max 4)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Team Members (${selectedMembers.size}/4)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (selectedMembers.size >= 4) "Limit reached" else "Add Roll 1–33",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = if (selectedMembers.size >= 4) colors.amberAccent else colors.textMuted
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = rollToAdd,
                                    onValueChange = { rollToAdd = it.filter { ch -> ch.isDigit() }.take(2) },
                                    label = { Text("Member Roll No") },
                                    placeholder = { Text("1–33") },
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
                                        .testTag("project_member_roll_input")
                                )

                                Button(
                                    onClick = {
                                        val r = rollToAdd.toIntOrNull()
                                        if (r != null && r in 1..33 && r !in selectedMembers && selectedMembers.size < 4) {
                                            selectedMembers.add(r)
                                            if (selectedLeaderRoll == null) {
                                                selectedLeaderRoll = r
                                            }
                                            rollToAdd = ""
                                        }
                                    },
                                    enabled = selectedMembers.size < 4 && (rollToAdd.toIntOrNull() in 1..33),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.skyAccent,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("project_add_member_button")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Add member")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add")
                                }
                            }

                            if (selectedMembers.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    selectedMembers.sorted().forEach { memberRoll ->
                                        val isLeader = selectedLeaderRoll == memberRoll
                                        InputChip(
                                            selected = isLeader,
                                            onClick = { selectedLeaderRoll = memberRoll },
                                            label = {
                                                Text(
                                                    text = if (isLeader) "Roll #$memberRoll (Leader)" else "Roll #$memberRoll",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily)
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (isLeader) Icons.Rounded.Star else Icons.Rounded.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            trailingIcon = {
                                                IconButton(
                                                    onClick = {
                                                        selectedMembers.remove(memberRoll)
                                                        if (selectedLeaderRoll == memberRoll) {
                                                            selectedLeaderRoll = selectedMembers.firstOrNull()
                                                        }
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = "Remove Roll #$memberRoll",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            },
                                            colors = InputChipDefaults.inputChipColors(
                                                selectedContainerColor = colors.amberContainer,
                                                selectedLabelColor = colors.amberAccent,
                                                selectedLeadingIconColor = colors.amberAccent,
                                                containerColor = MaterialTheme.colorScheme.background,
                                                labelColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                                Text(
                                    text = "Tap any member chip above to assign the 1 Group Leader.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                        }

                        // Leader Name confirmation
                        OutlinedTextField(
                            value = leaderName,
                            onValueChange = { leaderName = it },
                            label = {
                                Text(
                                    if (selectedLeaderRoll != null) {
                                        "Leader Name (Roll #$selectedLeaderRoll)"
                                    } else {
                                        "Leader Name"
                                    }
                                )
                            },
                            placeholder = { Text("Enter Leader's full name") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = colors.amberAccent
                                )
                            },
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
                                .testTag("project_leader_name_input")
                        )

                        AnimatedVisibility(visible = saveSuccessMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.emeraldContainer,
                                border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = saveSuccessMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.emeraldAccent,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = onDismissFeedback) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = colors.textMuted)
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = saveErrorMessage != null) {
                            InlineAlertCard(message = saveErrorMessage.orEmpty(), isError = true)
                        }

                        Button(
                            onClick = {
                                onSaveGroup(
                                    projectTitle,
                                    selectedMembers.toList(),
                                    selectedLeaderRoll,
                                    leaderName
                                ) {
                                    projectTitle = ""
                                }
                            },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.emeraldAccent,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_project_group_button")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Saving Project Group...")
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Submit Project Group",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // Submitted Groups Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Submitted Class Project Groups",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${groups.size} group(s)",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = colors.textMuted
                    )
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
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Groups, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(34.dp))
                            Text(
                                text = "No Project Groups Registered Yet",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Form your team of up to 4 trainees and select 1 leader above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            } else {
                items(groups, key = { it.groupId.ifBlank { "${it.projectTitle}_${it.leaderRoll}" } }) { group ->
                    ProjectGroupSummaryCard(group = group)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectGroupSummaryCard(
    group: ProjectGroup,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .testTag("project_group_card_${group.groupId}")
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
                    text = group.groupId,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.skyAccent
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.emeraldContainer
                ) {
                    Text(
                        text = "${group.memberRolls.size}/4 MEMBERS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.emeraldAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = group.projectTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Leader pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.amberContainer,
                border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = colors.amberAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    val leaderDisplay = if (group.leaderName.isNotBlank()) {
                        "Leader: ${group.leaderName} (Roll #${group.leaderRoll})"
                    } else {
                        "Leader: Roll #${group.leaderRoll}"
                    }
                    Text(
                        text = leaderDisplay,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.amberAccent
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                group.memberRolls.forEach { roll ->
                    val isLeader = roll == group.leaderRoll
                    FilterChip(
                        selected = isLeader,
                        onClick = {},
                        label = {
                            Text(
                                text = "Roll #$roll",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.skyContainer,
                            selectedLabelColor = colors.skyAccent,
                            containerColor = MaterialTheme.colorScheme.background,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isLeader,
                            borderColor = colors.borderSubtle,
                            selectedBorderColor = colors.skyAccent
                        )
                    )
                }
            }
        }
    }
}
