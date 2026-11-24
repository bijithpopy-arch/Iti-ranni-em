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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.TimetablePeriod
import com.example.data.TimetableStore
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors

/**
 * Compact Trainee Home card showing Today's current or next timetable slot
 * with one-tap jump to the full Timetable tab.
 */
@Composable
fun TodayTimetableQuickStrip(
    periods: List<TimetablePeriod>,
    hasCustomEdits: Boolean,
    onOpenTimetableTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val todayKey = remember { TimetableStore.currentIstDayKey() }
    val dayName = remember(todayKey) { TimetableStore.dayDisplayLabel(todayKey) }
    val todayPeriods = remember(periods, todayKey) {
        TimetableStore.sortPeriodsChronologically(
            periods.filter { it.dayOfWeek.equals(todayKey, ignoreCase = true) }
        )
    }
    val activeStatus = remember(todayPeriods) {
        TimetableStore.findActiveOrUpcomingSlot(todayPeriods)
    }

    Surface(
        onClick = onOpenTimetableTab,
        shape = RoundedCornerShape(20.dp),
        color = colors.cardSurface,
        border = BorderStroke(
            width = 1.5.dp,
            color = if (activeStatus?.isLiveNow == true) colors.emeraldAccent.copy(alpha = 0.65f)
            else colors.skyAccent.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("today_timetable_quick_strip")
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.skyContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarViewWeek,
                            contentDescription = null,
                            tint = colors.skyAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Today's Timetable ($dayName)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (hasCustomEdits) "My Custom Edits • ${todayPeriods.size} slots"
                            else "Official EMech Schedule • ${todayPeriods.size} slots",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (activeStatus != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (activeStatus.isLiveNow) colors.emeraldContainer else colors.skyContainer
                        ) {
                            Text(
                                text = if (activeStatus.isLiveNow) "LIVE NOW" else "UP NEXT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (activeStatus.isLiveNow) colors.emeraldAccent else colors.skyAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Open full timetable",
                        tint = colors.skyAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (activeStatus != null) {
                val p = activeStatus.period
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = p.subject,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = listOf(p.sessionType, p.roomOrNote)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${p.startTime} – ${p.endTime}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.skyAccent
                        )
                    }
                }
            } else {
                Text(
                    text = "Tap to view or customize your weekly Mon–Fri EMech schedule.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
        }
    }
}

/**
 * Trainee Editable Weekly Timetable Tab:
 * - Day selector (Mon–Fri + Sat) defaulting to current IST weekday
 * - Highlights active / upcoming slot
 * - Edit Timetable mode: Add, edit, delete slots; save locally ("My Custom Edits")
 *   or publish to shared Google Sheet if Instructor enabled trainee edits
 * - "Reset to Official" restores Instructor's official schedule cleanly
 */
@Composable
fun TraineeTimetableTab(
    officialPeriods: List<TimetablePeriod>,
    customPeriods: List<TimetablePeriod>?,
    allowSharedTraineeEdit: Boolean,
    isSaving: Boolean,
    feedbackMessage: String?,
    errorMessage: String?,
    onSaveLocalEdits: (List<TimetablePeriod>) -> Unit,
    onResetToOfficial: () -> Unit,
    onSubmitSharedEdits: (List<TimetablePeriod>) -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val todayKey = remember { TimetableStore.currentIstDayKey() }
    var selectedDay by remember { mutableStateOf(todayKey) }
    var isEditMode by remember { mutableStateOf(false) }

    val baseList = remember(officialPeriods, customPeriods) {
        val active = customPeriods ?: officialPeriods
        active.ifEmpty { TimetableStore.defaultWeeklyTimetable() }
    }
    val draftPeriods = remember { mutableStateListOf<TimetablePeriod>() }
    LaunchedEffect(baseList, isEditMode) {
        if (!isEditMode) {
            draftPeriods.clear()
            draftPeriods.addAll(baseList)
        }
    }

    var editingPeriod by remember { mutableStateOf<TimetablePeriod?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val displayedPeriods = if (isEditMode) draftPeriods.toList() else baseList
    val dayPeriods = remember(displayedPeriods, selectedDay) {
        TimetableStore.sortPeriodsChronologically(
            displayedPeriods.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }
        )
    }
    val activeSlot = remember(dayPeriods, selectedDay, todayKey) {
        if (selectedDay == todayKey) TimetableStore.findActiveOrUpcomingSlot(dayPeriods) else null
    }

    if (showAddDialog || editingPeriod != null) {
        TimetableSlotEditorDialog(
            dayKey = selectedDay,
            existing = editingPeriod,
            onDismiss = {
                showAddDialog = false
                editingPeriod = null
            },
            onSave = { updated ->
                val idx = draftPeriods.indexOfFirst { it.id == updated.id }
                if (idx >= 0) {
                    draftPeriods[idx] = updated
                } else {
                    draftPeriods.add(updated)
                }
                showAddDialog = false
                editingPeriod = null
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("trainee_timetable_tab"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header & Mode Controls Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.cardSurface,
                border = BorderStroke(
                    1.dp,
                    if (isEditMode) colors.amberAccent.copy(alpha = 0.65f) else colors.borderSubtle
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
                                text = "EMech Weekly Timetable",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when {
                                    isEditMode -> "Editing slots — save locally or share when allowed"
                                    customPeriods != null -> "Viewing My Custom Edits (cached offline)"
                                    else -> "Official Govt. ITI Ranni EMech Schedule"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (customPeriods != null) colors.amberAccent else colors.textMuted
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (customPeriods != null) colors.amberContainer else colors.emeraldContainer
                        ) {
                            Text(
                                text = if (customPeriods != null) "CUSTOM" else "OFFICIAL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (customPeriods != null) colors.amberAccent else colors.emeraldAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (!isEditMode) {
                                    draftPeriods.clear()
                                    draftPeriods.addAll(baseList)
                                }
                                isEditMode = !isEditMode
                                onDismissFeedback()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isEditMode) colors.amberAccent else colors.skyAccent),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isEditMode) colors.amberAccent else colors.skyAccent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("trainee_toggle_edit_timetable_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEditMode) "Exit Edit" else "Edit Timetable",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (customPeriods != null || isEditMode) {
                            OutlinedButton(
                                onClick = {
                                    isEditMode = false
                                    onResetToOfficial()
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, colors.borderSubtle),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textMuted),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("trainee_reset_timetable_button")
                            ) {
                                Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset Official", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    AnimatedVisibility(visible = feedbackMessage != null) {
                        InlineAlertCard(message = feedbackMessage.orEmpty(), isError = false)
                    }
                    AnimatedVisibility(visible = errorMessage != null) {
                        InlineAlertCard(message = errorMessage.orEmpty(), isError = true)
                    }
                }
            }
        }

        // Day Selector (Mon–Fri + Sat)
        item {
            DaySelectorRow(
                selectedDay = selectedDay,
                todayKey = todayKey,
                onSelectDay = { selectedDay = it }
            )
        }

        // Edit Mode Action Bar
        if (isEditMode) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.skyAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.skyAccent),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("trainee_add_slot_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Slot", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Button(
                            onClick = {
                                onSaveLocalEdits(draftPeriods.toList())
                                isEditMode = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.emeraldAccent,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("trainee_save_local_timetable_button")
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Local", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    if (allowSharedTraineeEdit) {
                        Button(
                            onClick = {
                                onSubmitSharedEdits(draftPeriods.toList())
                                isEditMode = false
                            },
                            enabled = !isSaving,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.skyAccent,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("trainee_submit_shared_timetable_button")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing Shared Timetable...")
                            } else {
                                Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Submit to Shared Sheet (Enabled by Instructor)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Day Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${TimetableStore.dayDisplayLabel(selectedDay)} Schedule",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${dayPeriods.size} slot(s)",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.textMuted
                )
            }
        }

        if (dayPeriods.isEmpty()) {
            item {
                EmptyDayTimetableCard(
                    dayName = TimetableStore.dayDisplayLabel(selectedDay),
                    canAdd = isEditMode,
                    onAdd = { showAddDialog = true }
                )
            }
        } else {
            items(dayPeriods, key = { "${it.dayOfWeek}_${it.id}" }) { period ->
                val isHighlighted = activeSlot?.period?.id == period.id
                TimetablePeriodCard(
                    period = period,
                    isLiveNow = isHighlighted && activeSlot?.isLiveNow == true,
                    isUpNext = isHighlighted && activeSlot?.isLiveNow == false,
                    editable = isEditMode,
                    onEdit = { editingPeriod = period },
                    onDelete = { draftPeriods.removeAll { it.id == period.id } }
                )
            }
        }
    }
}

/**
 * Trade Instructor Full CRUD Weekly Timetable Editor Tab:
 * - Day selector (Mon–Fri + Sat)
 * - Add / Edit / Delete any slot (start/end time, subject, session type, room/lab note)
 * - Toggle "Allow Trainees to Edit Shared Timetable"
 * - "Publish Timetable" syncs directly to Google Sheets (`action = "saveTimetable"`)
 */
@Composable
fun InstructorTimetableEditorTab(
    officialPeriods: List<TimetablePeriod>,
    traineeEditEnabled: Boolean,
    isSaving: Boolean,
    feedbackMessage: String?,
    errorMessage: String?,
    onPublishTimetable: (periods: List<TimetablePeriod>, allowTraineeEdit: Boolean) -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val todayKey = remember { TimetableStore.currentIstDayKey() }
    var selectedDay by remember { mutableStateOf(todayKey) }
    var allowTraineeEdit by remember(traineeEditEnabled) { mutableStateOf(traineeEditEnabled) }

    val draftPeriods = remember { mutableStateListOf<TimetablePeriod>() }
    LaunchedEffect(officialPeriods) {
        draftPeriods.clear()
        draftPeriods.addAll(officialPeriods.ifEmpty { TimetableStore.defaultWeeklyTimetable() })
    }

    var editingPeriod by remember { mutableStateOf<TimetablePeriod?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val dayPeriods = remember(draftPeriods.toList(), selectedDay) {
        TimetableStore.sortPeriodsChronologically(
            draftPeriods.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }
        )
    }

    if (showAddDialog || editingPeriod != null) {
        TimetableSlotEditorDialog(
            dayKey = selectedDay,
            existing = editingPeriod,
            onDismiss = {
                showAddDialog = false
                editingPeriod = null
            },
            onSave = { updated ->
                val withAuthor = updated.copy(updatedBy = "Trade Instructor")
                val idx = draftPeriods.indexOfFirst { it.id == withAuthor.id }
                if (idx >= 0) {
                    draftPeriods[idx] = withAuthor
                } else {
                    draftPeriods.add(withAuthor)
                }
                showAddDialog = false
                editingPeriod = null
                onDismissFeedback()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("instructor_timetable_editor_tab"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Editor Header & Trainee Permission Switch
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.cardSurface,
            border = BorderStroke(1.5.dp, colors.skyAccent.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
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
                            .background(colors.skyContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarViewWeek,
                            contentDescription = null,
                            tint = colors.skyAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Official Weekly Timetable Editor",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Edit Mon–Fri / Sat slots & publish live to all 23 trainees",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }

                // Permission Switch: Allow Trainees to Edit Shared Timetable
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(
                        1.dp,
                        if (allowTraineeEdit) colors.emeraldAccent else colors.borderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Allow Trainees to Edit Shared Timetable",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (allowTraineeEdit) colors.emeraldAccent else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (allowTraineeEdit) {
                                    "Enabled — Trainees can submit timetable edits to Google Sheets"
                                } else {
                                    "Disabled — Trainees can view official schedule & save local edits only"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = allowTraineeEdit,
                            onCheckedChange = { allowTraineeEdit = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                                checkedTrackColor = colors.emeraldAccent
                            ),
                            modifier = Modifier.testTag("toggle_allow_trainee_timetable_edit")
                        )
                    }
                }

                DaySelectorRow(
                    selectedDay = selectedDay,
                    todayKey = todayKey,
                    onSelectDay = { selectedDay = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.skyAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.skyAccent),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("instructor_add_timetable_slot_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add Slot", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }

                    OutlinedButton(
                        onClick = {
                            draftPeriods.clear()
                            draftPeriods.addAll(TimetableStore.defaultWeeklyTimetable())
                            onDismissFeedback()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textMuted),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("instructor_reset_default_timetable_button")
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("EMech Default", style = MaterialTheme.typography.labelMedium)
                    }
                }

                AnimatedVisibility(visible = feedbackMessage != null) {
                    InlineAlertCard(message = feedbackMessage.orEmpty(), isError = false)
                }
                AnimatedVisibility(visible = errorMessage != null) {
                    InlineAlertCard(message = errorMessage.orEmpty(), isError = true)
                }

                Button(
                    onClick = { onPublishTimetable(draftPeriods.toList(), allowTraineeEdit) },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.emeraldAccent,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("instructor_publish_timetable_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onSecondary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Publishing Timetable...")
                    } else {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Publish Timetable (${draftPeriods.size} Slots)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Day Slots List
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${TimetableStore.dayDisplayLabel(selectedDay)} Slots (${dayPeriods.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Tap Edit or Delete on any slot",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
        }

        if (dayPeriods.isEmpty()) {
            EmptyDayTimetableCard(
                dayName = TimetableStore.dayDisplayLabel(selectedDay),
                canAdd = true,
                onAdd = { showAddDialog = true }
            )
        } else {
            dayPeriods.forEach { period ->
                TimetablePeriodCard(
                    period = period,
                    isLiveNow = false,
                    isUpNext = false,
                    editable = true,
                    onEdit = { editingPeriod = period },
                    onDelete = { draftPeriods.removeAll { it.id == period.id } }
                )
            }
        }
    }
}

@Composable
private fun DaySelectorRow(
    selectedDay: String,
    todayKey: String,
    onSelectDay: (String) -> Unit
) {
    val colors = LocalItiColors.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(TimetableStore.WEEKDAYS) { (key, label) ->
            val selected = selectedDay == key
            val isToday = key == todayKey
            FilterChip(
                selected = selected,
                onClick = { onSelectDay(key) },
                label = {
                    Text(
                        text = if (isToday) "$label •" else label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.skyAccent,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = colors.cardSurface,
                    labelColor = colors.textMuted
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = if (isToday) colors.skyAccent.copy(alpha = 0.6f) else colors.borderSubtle,
                    selectedBorderColor = colors.skyAccent
                ),
                modifier = Modifier.testTag("timetable_day_${key.lowercase()}")
            )
        }
    }
}

@Composable
private fun TimetablePeriodCard(
    period: TimetablePeriod,
    isLiveNow: Boolean,
    isUpNext: Boolean,
    editable: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalItiColors.current
    val typeAccent: Color
    val typeBg: Color
    when (period.sessionType.lowercase()) {
        "practical" -> {
            typeAccent = colors.skyAccent
            typeBg = colors.skyContainer
        }
        "lunch", "break" -> {
            typeAccent = colors.amberAccent
            typeBg = colors.amberContainer
        }
        "1st period", "2nd period", "theory" -> {
            typeAccent = colors.emeraldAccent
            typeBg = colors.emeraldContainer
        }
        else -> {
            typeAccent = colors.textMuted
            typeBg = MaterialTheme.colorScheme.background
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(
            width = if (isLiveNow || isUpNext) 1.5.dp else 1.dp,
            color = when {
                isLiveNow -> colors.emeraldAccent
                isUpNext -> colors.skyAccent.copy(alpha = 0.7f)
                else -> colors.borderSubtle
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timetable_period_${period.id}")
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = typeAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${period.startTime} – ${period.endTime}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = typeAccent
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isLiveNow || isUpNext) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isLiveNow) colors.emeraldContainer else colors.skyContainer
                        ) {
                            Text(
                                text = if (isLiveNow) "LIVE NOW" else "UP NEXT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isLiveNow) colors.emeraldAccent else colors.skyAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = typeBg,
                        border = BorderStroke(1.dp, typeAccent.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = period.sessionType.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = typeAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = period.subject,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (period.roomOrNote.isNotBlank()) {
                        Text(
                            text = period.roomOrNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }

                if (editable) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.testTag("edit_slot_${period.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit slot",
                                tint = colors.skyAccent,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("delete_slot_${period.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete slot",
                                tint = colors.redAccent,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDayTimetableCard(
    dayName: String,
    canAdd: Boolean,
    onAdd: () -> Unit
) {
    val colors = LocalItiColors.current
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
            Icon(
                imageVector = Icons.Rounded.CalendarViewWeek,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "No slots scheduled for $dayName",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (canAdd) {
                OutlinedButton(
                    onClick = onAdd,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, colors.skyAccent)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Slot for $dayName")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimetableSlotEditorDialog(
    dayKey: String,
    existing: TimetablePeriod?,
    onDismiss: () -> Unit,
    onSave: (TimetablePeriod) -> Unit
) {
    val colors = LocalItiColors.current
    var startTime by remember(existing) { mutableStateOf(existing?.startTime ?: "01:00 PM") }
    var endTime by remember(existing) { mutableStateOf(existing?.endTime ?: "02:00 PM") }
    var subject by remember(existing) { mutableStateOf(existing?.subject ?: "") }
    var sessionType by remember(existing) { mutableStateOf(existing?.sessionType ?: "1st Period") }
    var roomOrNote by remember(existing) { mutableStateOf(existing?.roomOrNote ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardSurface,
        title = {
            Text(
                text = if (existing != null) "Edit ${TimetableStore.dayDisplayLabel(dayKey)} Slot"
                else "Add ${TimetableStore.dayDisplayLabel(dayKey)} Slot",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        placeholder = { Text("08:00 AM") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.skyAccent,
                            unfocusedBorderColor = colors.borderSubtle
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        placeholder = { Text("12:30 PM") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.skyAccent,
                            unfocusedBorderColor = colors.borderSubtle
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject / Session Title") },
                    placeholder = { Text("e.g., Lab / Practical, 1st Period — Trade Theory") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.skyAccent,
                        unfocusedBorderColor = colors.borderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Session Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TimetableStore.SESSION_TYPES.forEach { type ->
                        val selected = sessionType.equals(type, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { sessionType = type },
                            label = { Text(type, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.skyContainer,
                                selectedLabelColor = colors.skyAccent
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = roomOrNote,
                    onValueChange = { roomOrNote = it },
                    label = { Text("Room / Lab Note (Optional)") },
                    placeholder = { Text("e.g., EM Electronics Lab / Classroom") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.skyAccent,
                        unfocusedBorderColor = colors.borderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanSubject = subject.trim().ifBlank { sessionType }
                    val cleanStart = startTime.trim().ifBlank { "08:00 AM" }
                    val cleanEnd = endTime.trim().ifBlank { "03:00 PM" }
                    onSave(
                        TimetablePeriod(
                            id = existing?.id ?: "${dayKey}_${System.currentTimeMillis()}",
                            dayOfWeek = existing?.dayOfWeek ?: dayKey,
                            startTime = cleanStart,
                            endTime = cleanEnd,
                            subject = cleanSubject,
                            sessionType = sessionType,
                            roomOrNote = roomOrNote.trim(),
                            updatedBy = existing?.updatedBy.orEmpty()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.skyAccent,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Save Slot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textMuted)
            }
        }
    )
}
