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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AttendanceApiClient
import com.example.data.AttendanceMath
import com.example.data.DutyRotationEngine
import com.example.data.ShopFloorEntry
import com.example.data.StaffAttendanceRecord
import com.example.data.ToolCribLogEntry
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

private val TOOL_CRIB_PRESETS = listOf(
    "Digital Multimeter",
    "Soldering Station",
    "Breadboard Kit",
    "CRO / Oscilloscope Probe",
    "DC Power Supply",
    "Wire Stripper / Pliers"
)

private val TOOL_CONDITIONS = listOf("DAMAGED", "MISSING", "OK / RETURNED")

@Composable
fun ShopFloorControlTab(
    uiState: AppUiState,
    onToggleEarlyClassLogout: (Boolean) -> Unit,
    onToggleShopFloorReleaseNow: (Boolean) -> Unit,
    onAddOrRemoveShopFloor: (roll: String, operation: String, sessionType: String, endTime: String) -> Unit,
    onRecordCardLogout: (roll: String) -> Unit,
    onVerifyLogout: (roll: String, date: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val staffData = uiState.staffData
    val overrides = staffData?.scanOverrides
    val shopFloorList = staffData?.shopFloorList.orEmpty()

    var manualRoll by remember { mutableStateOf("") }
    var selectedSessionType by remember { mutableStateOf("POST_3PM") }
    var selectedEndTime by remember { mutableStateOf("05:00 PM") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("shop_floor_control_tab"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Instructor-Triggered RFID Scan Override Controls
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.5.dp, colors.skyAccent.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scan_override_control_card")
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
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = colors.skyAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "RFID Scanner Live Overrides",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Authorize early dismissal or early Shop Floor release without fixed clock locks",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    }

                    // Toggle 1: Allow Early Class Logout Now
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(
                            1.dp,
                            if (overrides?.earlyClassLogoutEnabled == true) colors.emeraldAccent else colors.borderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.override_early_class_title),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (overrides?.earlyClassLogoutEnabled == true) colors.emeraldAccent else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Early dismissal (e.g., 1:00 PM): RFID card swipes record normal OUT in Attendance_Log immediately — never blocked or logged as Shop Floor.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = overrides?.earlyClassLogoutEnabled == true,
                                onCheckedChange = onToggleEarlyClassLogout,
                                enabled = !uiState.isUpdatingScanOverride,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                                    checkedTrackColor = colors.emeraldAccent
                                ),
                                modifier = Modifier.testTag("toggle_early_class_logout")
                            )
                        }
                    }

                    // Toggle 2: Release Shop Floor Now
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(
                            1.dp,
                            if (overrides?.shopFloorReleaseNowEnabled == true) colors.amberAccent else colors.borderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.override_shop_floor_release_title),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (overrides?.shopFloorReleaseNowEnabled == true) colors.amberAccent else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Early Shop Floor dismissal (e.g., 4:00 PM / 4:15 PM): ID-card swipes immediately log Check_Out_Card_Scan_Time and mark Completed.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = overrides?.shopFloorReleaseNowEnabled == true,
                                onCheckedChange = onToggleShopFloorReleaseNow,
                                enabled = !uiState.isUpdatingScanOverride,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                                    checkedTrackColor = colors.amberAccent
                                ),
                                modifier = Modifier.testTag("toggle_shop_floor_release_now")
                            )
                        }
                    }

                    AnimatedVisibility(visible = uiState.scanOverrideMessage != null) {
                        InlineAlertCard(message = uiState.scanOverrideMessage.orEmpty(), isError = false)
                    }
                    AnimatedVisibility(visible = uiState.scanOverrideError != null) {
                        InlineAlertCard(message = uiState.scanOverrideError.orEmpty(), isError = true)
                    }
                }
            }
        }

        // 2. Manual Add / Schedule Shop Floor Trainee Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Build, contentDescription = null, tint = colors.amberAccent, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Assign Trainee to Shop Floor",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("POST_3PM" to "After 3:00 PM", "SATURDAY" to "Saturday").forEach { (key, label) ->
                            val selected = selectedSessionType == key
                            FilterChip(
                                selected = selected,
                                onClick = { selectedSessionType = key },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.amberContainer,
                                    selectedLabelColor = colors.amberAccent,
                                    containerColor = MaterialTheme.colorScheme.background,
                                    labelColor = colors.textMuted
                                )
                            )
                        }
                        listOf("04:00 PM", "05:00 PM").forEach { end ->
                            val selected = selectedEndTime == end
                            FilterChip(
                                selected = selected,
                                onClick = { selectedEndTime = end },
                                label = { Text("Till $end", style = MaterialTheme.typography.labelSmall) },
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualRoll,
                            onValueChange = { manualRoll = it.filter { ch -> ch.isDigit() }.take(2) },
                            label = { Text("Roll No (1–33)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.amberAccent,
                                unfocusedBorderColor = colors.borderSubtle,
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shop_floor_add_roll_input")
                        )
                        Button(
                            onClick = {
                                if (manualRoll.isNotBlank()) {
                                    onAddOrRemoveShopFloor(manualRoll, "ADD", selectedSessionType, selectedEndTime)
                                    manualRoll = ""
                                }
                            },
                            enabled = !uiState.isUpdatingShopFloor && manualRoll.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.amberAccent,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("shop_floor_add_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Assign")
                        }
                    }

                    AnimatedVisibility(visible = uiState.shopFloorActionMessage != null) {
                        InlineAlertCard(message = uiState.shopFloorActionMessage.orEmpty(), isError = false)
                    }
                    AnimatedVisibility(visible = uiState.shopFloorActionError != null) {
                        InlineAlertCard(message = uiState.shopFloorActionError.orEmpty(), isError = true)
                    }
                }
            }
        }

        // 3. Roster Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shop Floor Roster (${shopFloorList.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Auto <80% + Manual",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.amberAccent
                )
            }
        }

        if (shopFloorList.isEmpty()) {
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.emeraldAccent, modifier = Modifier.size(34.dp))
                        Text(
                            text = "All Trainees Above 80%",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "No trainees are currently auto-flagged for Shop Floor. You can still assign any trainee above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }
            }
        } else {
            val attendanceByRoll = staffData?.allAttendance.orEmpty().associateBy { it.roll.trim() }
            items(shopFloorList, key = { "${it.roll}_${it.date}_${it.id}" }) { entry ->
                InstructorShopFloorRowCard(
                    entry = entry,
                    attendanceRecord = attendanceByRoll[entry.roll.trim()],
                    isBusy = uiState.isUpdatingShopFloor,
                    onRecordCardLogout = { onRecordCardLogout(entry.roll) },
                    onVerifyLogout = { onVerifyLogout(entry.roll, entry.date.ifBlank { AttendanceApiClient.todayIstDateString() }) },
                    onRemove = { onAddOrRemoveShopFloor(entry.roll, "REMOVE", entry.sessionType, entry.scheduledEndTime) }
                )
            }
        }
    }
}

@Composable
private fun InstructorShopFloorRowCard(
    entry: ShopFloorEntry,
    attendanceRecord: StaffAttendanceRecord? = null,
    isBusy: Boolean,
    onRecordCardLogout: () -> Unit,
    onVerifyLogout: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = LocalItiColors.current
    val hasCardOut = entry.checkOutCardScanTime.isNotBlank()
    val neededFor80 = attendanceRecord?.sessionsNeededFor80Pct ?: 0
    val formattedHours = AttendanceMath.formatShopFloorHours(entry.durationMinutes)
    val sessionStatusLabel = entry.status.trim().uppercase().ifBlank { "SCHEDULED" }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(
            1.dp,
            when {
                entry.verifiedByInstructor -> colors.emeraldAccent.copy(alpha = 0.6f)
                hasCardOut -> colors.skyAccent.copy(alpha = 0.6f)
                else -> colors.amberAccent.copy(alpha = 0.45f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_floor_card_${entry.roll}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.amberContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = entry.roll.padStart(2, '0'),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.amberAccent
                        )
                    }
                    Column {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${entry.attendancePct}% • ${if (entry.sessionType == "SATURDAY") "Saturday" else "Post-3:00 PM (till ${entry.scheduledEndTime})"}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = colors.textMuted
                        )
                    }
                }

                IconButton(
                    onClick = onRemove,
                    enabled = !isBusy,
                    modifier = Modifier.testTag("remove_shop_floor_${entry.roll}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Remove from Shop Floor",
                        tint = colors.redAccent
                    )
                }
            }

            // Shop Floor Status/Hours + Needed Classes for 80% Attendance Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.skyContainer,
                    border = BorderStroke(1.dp, colors.skyAccent.copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                        Text(
                            text = "SHOP FLOOR: $sessionStatusLabel",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.skyAccent
                        )
                        Text(
                            text = "Hours Logged: $formattedHours",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                val isEligible = neededFor80 == 0 && (attendanceRecord?.pct ?: entry.attendancePct) >= 80
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isEligible) colors.emeraldContainer else colors.amberContainer,
                    border = BorderStroke(
                        1.dp,
                        if (isEligible) colors.emeraldAccent.copy(alpha = 0.45f) else colors.amberAccent.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                        Text(
                            text = if (isEligible) "80% ELIGIBLE" else "NEED +$neededFor80 CLASSES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isEligible) colors.emeraldAccent else colors.amberAccent
                        )
                        val breakdown = if (attendanceRecord != null) {
                            "P:${attendanceRecord.present} H:${attendanceRecord.half} T:${attendanceRecord.total}"
                        } else {
                            "To reach 80% threshold"
                        }
                        Text(
                            text = breakdown,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Card Scan Logout Status Bar
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Nfc,
                            contentDescription = null,
                            tint = if (hasCardOut) colors.emeraldAccent else colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        val outText = if (hasCardOut) {
                            val dur = if (entry.durationMinutes > 0) " (${entry.durationMinutes}m)" else ""
                            "Card Logout: ${entry.checkOutCardScanTime}$dur"
                        } else {
                            "Awaiting post-3:00 PM RFID Card Logout"
                        }
                        Text(
                            text = outText,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = if (hasCardOut) colors.emeraldAccent else colors.textMuted
                        )
                    }
                    if (entry.verifiedByInstructor) {
                        Text(
                            text = "VERIFIED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.emeraldAccent
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!hasCardOut) {
                    OutlinedButton(
                        onClick = onRecordCardLogout,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, colors.skyAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.skyAccent),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Rounded.Sensors, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log Card Out", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (hasCardOut && !entry.verifiedByInstructor) {
                    Button(
                        onClick = onVerifyLogout,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.emeraldAccent,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("verify_shop_floor_${entry.roll}")
                    ) {
                        Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify Logout", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

/**
 * Instructor Duty Sign-Off & Tool Crib Equipment Damage/Missing Log Panel.
 */
@Composable
fun DutyAndToolCribPanel(
    uiState: AppUiState,
    onSignOffDuty: (date: String, pairLabel: String, note: String, onReset: () -> Unit) -> Unit,
    onLogToolCrib: (itemName: String, conditionStatus: String, notes: String, onReset: () -> Unit) -> Unit
) {
    val colors = LocalItiColors.current
    val schedule = remember { DutyRotationEngine.generateRotationSchedule() }
    val todaySlot = schedule.firstOrNull()
    val isWeekend = remember { DutyRotationEngine.isWeekendToday() }
    val pairLabel = todaySlot?.rolls?.joinToString(" & ") { "Roll #${it.toString().padStart(2, '0')}" } ?: "Roll #01 & Roll #02"
    val todaySignOff = uiState.staffData?.todayDutySignOff

    var dutyNote by remember { mutableStateOf("") }
    var equipName by remember { mutableStateOf("") }
    var equipCondition by remember { mutableStateOf("DAMAGED") }
    var equipNotes by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Mon–Fri Daily Duty Sign-Off Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.cardSurface,
            border = BorderStroke(
                1.dp,
                if (todaySignOff?.signedOff == true) colors.emeraldAccent.copy(alpha = 0.6f) else colors.skyAccent.copy(alpha = 0.5f)
            ),
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
                        Icon(Icons.Rounded.EventRepeat, contentDescription = null, tint = colors.skyAccent, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "Mon–Fri Duty Sign-Off (Eggs / Milk / Safety)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isWeekend) "Weekend — Next Pair: $pairLabel (${todaySlot?.dateLabel})" else "Today's Pair: $pairLabel • ${todaySlot?.dateLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.skyAccent
                        )
                    }
                }

                if (todaySignOff?.signedOff == true) {
                    InlineAlertCard(
                        message = "Signed off for ${todaySignOff.pairLabel} (${todaySignOff.date}) ${if (todaySignOff.signedAt.isNotBlank()) "at ${todaySignOff.signedAt}" else ""}",
                        isError = false
                    )
                } else {
                    OutlinedTextField(
                        value = dutyNote,
                        onValueChange = { dutyNote = it },
                        label = { Text("Verification Note (Optional)") },
                        placeholder = { Text("e.g., Eggs & Milk delivered, Lab benches clean") },
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
                    Button(
                        onClick = {
                            onSignOffDuty(
                                todaySlot?.dateKey?.ifBlank { AttendanceApiClient.todayIstDateString() }
                                    ?: AttendanceApiClient.todayIstDateString(),
                                pairLabel,
                                dutyNote
                            ) { dutyNote = "" }
                        },
                        enabled = !uiState.isUpdatingOps,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.emeraldAccent,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sign_off_duty_button")
                    ) {
                        Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify & Sign Off Today's Duty", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // 2. Tool Crib & Electronics Lab Equipment Log Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.cardSurface,
            border = BorderStroke(1.dp, colors.borderSubtle),
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
                            .background(colors.amberContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Handyman, contentDescription = null, tint = colors.amberAccent, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "Tool Crib & Lab Equipment Log",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Log missing or damaged soldering kits, multimeters & electronics lab gear",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TOOL_CRIB_PRESETS) { preset ->
                        FilterChip(
                            selected = equipName == preset,
                            onClick = { equipName = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.skyContainer,
                                selectedLabelColor = colors.skyAccent,
                                containerColor = MaterialTheme.colorScheme.background,
                                labelColor = colors.textMuted
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = equipName,
                    onValueChange = { equipName = it },
                    label = { Text("Equipment / Kit Name") },
                    placeholder = { Text("e.g., Digital Multimeter #4 / Soldering Iron") },
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
                        .testTag("tool_crib_item_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TOOL_CONDITIONS.forEach { cond ->
                        val selected = equipCondition == cond
                        FilterChip(
                            selected = selected,
                            onClick = { equipCondition = cond },
                            label = { Text(cond, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (cond == "OK / RETURNED") colors.emeraldContainer else colors.redContainer,
                                selectedLabelColor = if (cond == "OK / RETURNED") colors.emeraldAccent else colors.redAccent,
                                containerColor = MaterialTheme.colorScheme.background,
                                labelColor = colors.textMuted
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = equipNotes,
                    onValueChange = { equipNotes = it },
                    label = { Text("Bench / Roll No / Details") },
                    placeholder = { Text("e.g., Fuse blown on Bench 3 — Roll #12") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.amberAccent,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = uiState.opsFeedbackMessage != null) {
                    InlineAlertCard(message = uiState.opsFeedbackMessage.orEmpty(), isError = false)
                }
                AnimatedVisibility(visible = uiState.opsError != null) {
                    InlineAlertCard(message = uiState.opsError.orEmpty(), isError = true)
                }

                Button(
                    onClick = {
                        onLogToolCrib(equipName, equipCondition, equipNotes) {
                            equipName = ""
                            equipNotes = ""
                        }
                    },
                    enabled = !uiState.isUpdatingOps,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.amberAccent,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_tool_crib_button")
                ) {
                    Text("Record Equipment Log", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }

                val recentLogs = uiState.staffData?.toolCribLogs.orEmpty()
                if (recentLogs.isNotEmpty()) {
                    Text(
                        text = "Recent Tool Crib Entries (${recentLogs.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    recentLogs.take(5).forEach { log ->
                        ToolCribLogRow(log = log)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCribLogRow(log: ToolCribLogEntry) {
    val colors = LocalItiColors.current
    val isOk = log.conditionStatus.contains("OK")
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.itemName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = log.conditionStatus,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isOk) colors.emeraldAccent else colors.redAccent
                )
            }
            if (log.notes.isNotBlank()) {
                Text(text = log.notes, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            Text(
                text = "${log.date} ${log.time}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                color = colors.textMuted
            )
        }
    }
}
