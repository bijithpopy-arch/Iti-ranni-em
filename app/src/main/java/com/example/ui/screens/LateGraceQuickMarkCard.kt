package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTimeFilled
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.LateGraceModes
import com.example.data.StaffAttendanceRecord
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

private val LATE_GRACE_FILTERS = listOf("ALL", "LATE / HALF", "GRACE ACTIVE")

/**
 * Interactive scrollable Late Grace Quick-Mark Table for the Trade Instructor.
 * Displays Roll #, Name, Today's IN Time, Current Status, and Late Grace status with
 * direct one-tap chips for 09:30 Grace, Exempt, and Clear, plus instant row highlighting.
 */
@Composable
fun LateGraceQuickMarkTableCard(
    uiState: AppUiState,
    records: List<StaffAttendanceRecord>,
    onSetLateGrace: (roll: String, mode: String, cutoff: String, note: String) -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    var expanded by remember { mutableStateOf(true) }
    var activeFilter by remember { mutableStateOf("ALL") }

    val lateOrHalfCount = remember(records) {
        records.count { isLateOrHalfRecord(it) }
    }
    val graceActiveCount = remember(records) {
        records.count { it.hasActiveLateGrace }
    }

    // Sort highlighted rows (LATE/HALF or Grace Active) gently or keep clean Roll # order
    val visibleRecords = remember(records, activeFilter) {
        val base = when (activeFilter) {
            "LATE / HALF" -> records.filter { isLateOrHalfRecord(it) }
            "GRACE ACTIVE" -> records.filter { it.hasActiveLateGrace }
            else -> records
        }
        base.sortedBy { it.roll.toIntOrNull() ?: Int.MAX_VALUE }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardSurface,
        border = BorderStroke(
            width = 1.2.dp,
            color = if (lateOrHalfCount > 0) colors.amberAccent.copy(alpha = 0.65f) else colors.skyAccent.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("late_grace_quick_mark_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp),
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
                            .background(if (lateOrHalfCount > 0) colors.amberContainer else colors.skyContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccessTimeFilled,
                            contentDescription = null,
                            tint = if (lateOrHalfCount > 0) colors.amberAccent else colors.skyAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.late_grace_table_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.late_grace_table_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.testTag("toggle_late_grace_table_button")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "Collapse Late Grace Table" else "Expand Late Grace Table",
                        tint = colors.skyAccent
                    )
                }
            }

            // Summary KPI badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GraceSummaryPill(
                    label = "Late/Half",
                    count = lateOrHalfCount,
                    accent = colors.amberAccent,
                    container = colors.amberContainer,
                    modifier = Modifier.weight(1f)
                )
                GraceSummaryPill(
                    label = "Grace Active",
                    count = graceActiveCount,
                    accent = colors.emeraldAccent,
                    container = colors.emeraldContainer,
                    modifier = Modifier.weight(1f)
                )
                GraceSummaryPill(
                    label = "Total",
                    count = records.size,
                    accent = colors.skyAccent,
                    container = colors.skyContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            // Confirmation / Error Banner
            AnimatedVisibility(visible = uiState.lateGraceMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.emeraldContainer,
                    border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("late_grace_confirmation_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = colors.emeraldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = uiState.lateGraceMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = colors.emeraldAccent
                            )
                        }
                        IconButton(onClick = onDismissFeedback) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss late grace notification",
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = uiState.lateGraceError != null) {
                InlineAlertCard(message = uiState.lateGraceError.orEmpty(), isError = true)
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Quick table filter chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(LATE_GRACE_FILTERS) { filter ->
                            val selected = activeFilter == filter
                            FilterChip(
                                selected = selected,
                                onClick = { activeFilter = filter },
                                label = {
                                    Text(
                                        text = filter,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                },
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
                                ),
                                modifier = Modifier.testTag("late_grace_filter_${filter.lowercase().replace(" ", "_")}")
                            )
                        }
                    }

                    // Table Container
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Sticky Column Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.cardElevated)
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ROLL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colors.textMuted,
                                    modifier = Modifier.width(42.dp)
                                )
                                Text(
                                    text = "TRAINEE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colors.textMuted,
                                    modifier = Modifier.weight(1.25f)
                                )
                                Text(
                                    text = "IN TIME",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colors.textMuted,
                                    modifier = Modifier.width(68.dp)
                                )
                                Text(
                                    text = "STATUS / GRACE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colors.textMuted,
                                    modifier = Modifier.width(102.dp)
                                )
                            }
                            HorizontalDivider(color = colors.borderSubtle)

                            if (visibleRecords.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (activeFilter) {
                                            "LATE / HALF" -> "No trainees are currently marked LATE or HALF-DAY."
                                            "GRACE ACTIVE" -> "No trainees have Late Grace active today."
                                            else -> "No trainee attendance records loaded yet."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 380.dp)
                                        .verticalScroll(rememberScrollState())
                                        .testTag("late_grace_scrollable_table")
                                ) {
                                    visibleRecords.forEachIndexed { index, record ->
                                        LateGraceTableRow(
                                            record = record,
                                            isUpdatingThisRow = uiState.updatingLateGraceRoll == record.roll,
                                            isAnyUpdating = uiState.updatingLateGraceRoll != null,
                                            onSetLateGrace = { mode, cutoff, note ->
                                                onSetLateGrace(record.roll, mode, cutoff, note)
                                            }
                                        )
                                        if (index < visibleRecords.lastIndex) {
                                            HorizontalDivider(color = colors.borderSubtle.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LateGraceTableRow(
    record: StaffAttendanceRecord,
    isUpdatingThisRow: Boolean,
    isAnyUpdating: Boolean,
    onSetLateGrace: (mode: String, cutoff: String, note: String) -> Unit
) {
    val colors = LocalItiColors.current
    val isLateOrHalf = isLateOrHalfRecord(record)
    val hasGrace = record.hasActiveLateGrace
    val is0930Active = record.lateGraceMode.equals(LateGraceModes.CUSTOM_CUTOFF, ignoreCase = true)
    val isExemptActive = record.lateGraceMode.equals(LateGraceModes.EXEMPT_TODAY, ignoreCase = true)

    val rowBackground = when {
        hasGrace -> colors.emeraldContainer.copy(alpha = 0.28f)
        isLateOrHalf -> colors.amberContainer.copy(alpha = 0.34f)
        else -> Color.Transparent
    }
    val leftAccentColor = when {
        hasGrace -> colors.emeraldAccent
        isLateOrHalf -> colors.amberAccent
        else -> Color.Transparent
    }

    // Tapping the row directly toggles 09:30 Grace (or clears if already active)
    val onRowDirectTap = {
        if (!isAnyUpdating) {
            if (hasGrace) {
                onSetLateGrace(LateGraceModes.CLEAR, "", "Late grace cleared")
            } else {
                onSetLateGrace(LateGraceModes.CUSTOM_CUTOFF, "09:30", "09:30 grace granted via quick-tap")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .clickable(enabled = !isAnyUpdating, onClick = onRowDirectTap)
            .padding(start = 0.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
            .testTag("late_grace_row_${record.roll}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left highlight strip for LATE / Active Grace
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(leftAccentColor)
            )
            Spacer(modifier = Modifier.width(6.dp))

            // Roll #
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = when {
                    hasGrace -> colors.emeraldContainer
                    isLateOrHalf -> colors.amberContainer
                    else -> colors.skyContainer
                },
                modifier = Modifier.width(36.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "#${record.roll}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = when {
                            hasGrace -> colors.emeraldAccent
                            isLateOrHalf -> colors.amberAccent
                            else -> colors.skyAccent
                        },
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))

            // Name
            Column(modifier = Modifier.weight(1.25f)) {
                Text(
                    text = record.name,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (hasGrace) {
                    val graceBadgeText = if (isExemptActive) {
                        "EXEMPT TODAY"
                    } else {
                        "GRACE ${record.lateCutoff.ifBlank { "09:30" }}"
                    }
                    Text(
                        text = graceBadgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.emeraldAccent,
                        maxLines = 1
                    )
                }
            }

            // Today's IN Time
            Text(
                text = record.inT.ifBlank { "-" },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = if (isLateOrHalf) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isLateOrHalf) colors.amberAccent else colors.textMuted,
                maxLines = 1,
                modifier = Modifier.width(68.dp)
            )

            // Current Status Pill + Grace indicator
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.width(102.dp)
            ) {
                val statusUpper = record.todayStatus.trim().uppercase().ifBlank { "ABSENT" }
                val (statusBg, statusFg) = when {
                    statusUpper == "PRESENT" || statusUpper == "ON-DUTY" -> colors.emeraldContainer to colors.emeraldAccent
                    statusUpper == "LATE" || statusUpper.contains("HALF") -> colors.amberContainer to colors.amberAccent
                    else -> colors.redContainer to colors.redAccent
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusBg,
                    border = BorderStroke(1.dp, statusFg.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = statusUpper,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = statusFg,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = when {
                        isExemptActive -> "Exempt"
                        is0930Active -> "Grace ${record.lateCutoff.ifBlank { "09:30" }}"
                        else -> "No Grace"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasGrace) colors.emeraldAccent else colors.textMuted,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Direct One-Tap Action Chips Row (09:30 Grace | Exempt | Clear)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isUpdatingThisRow) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    CircularProgressIndicator(
                        color = colors.skyAccent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Updating Roll #${record.roll}...",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.skyAccent
                    )
                }
            } else {
                LateGraceActionChip(
                    label = stringResource(R.string.late_grace_chip_0930),
                    icon = Icons.Rounded.Schedule,
                    active = is0930Active,
                    accent = colors.skyAccent,
                    activeContainer = colors.skyContainer,
                    enabled = !isAnyUpdating,
                    onClick = {
                        onSetLateGrace(
                            LateGraceModes.CUSTOM_CUTOFF,
                            "09:30",
                            "09:30 late grace granted"
                        )
                    },
                    modifier = Modifier.testTag("late_grace_0930_${record.roll}")
                )

                LateGraceActionChip(
                    label = stringResource(R.string.late_grace_chip_exempt),
                    icon = Icons.Rounded.VerifiedUser,
                    active = isExemptActive,
                    accent = colors.emeraldAccent,
                    activeContainer = colors.emeraldContainer,
                    enabled = !isAnyUpdating,
                    onClick = {
                        onSetLateGrace(
                            LateGraceModes.EXEMPT_TODAY,
                            "",
                            "Exempt from late penalty today"
                        )
                    },
                    modifier = Modifier.testTag("late_grace_exempt_${record.roll}")
                )

                if (hasGrace) {
                    LateGraceActionChip(
                        label = stringResource(R.string.late_grace_chip_clear),
                        icon = Icons.Rounded.Close,
                        active = false,
                        accent = colors.redAccent,
                        activeContainer = colors.redContainer,
                        enabled = !isAnyUpdating,
                        onClick = {
                            onSetLateGrace(
                                LateGraceModes.CLEAR,
                                "",
                                "Late grace cleared"
                            )
                        },
                        modifier = Modifier.testTag("late_grace_clear_${record.roll}")
                    )
                }
            }
        }
    }
}

@Composable
private fun LateGraceActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    accent: Color,
    activeContainer: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (active) activeContainer else colors.cardSurface,
        border = BorderStroke(
            width = if (active) 1.5.dp else 1.dp,
            color = if (active) accent else accent.copy(alpha = 0.55f)
        ),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(9.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GraceSummaryPill(
    label: String,
    count: Int,
    accent: Color,
    container: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = container.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accent
            )
        }
    }
}

private fun isLateOrHalfRecord(record: StaffAttendanceRecord): Boolean {
    val st = record.todayStatus.trim().uppercase()
    return st == "LATE" || st.contains("HALF")
}
