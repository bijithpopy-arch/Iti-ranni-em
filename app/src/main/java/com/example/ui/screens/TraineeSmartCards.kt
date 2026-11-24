package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceMath
import com.example.data.AttendanceSummary
import com.example.data.DutyOverrideEntry
import com.example.data.DutyRotationEngine
import com.example.data.ScanOverrideState
import com.example.data.ShopFloorEntry
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors

/**
 * Trainee Personal Attendance "Safe-Zone" Calculator & Consistency Streak Badge Card.
 */
@Composable
fun TraineeSafeZoneAndStreakCard(
    attendance: AttendanceSummary,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val safeZone = remember(attendance.present, attendance.half, attendance.total) {
        AttendanceMath.calculateSafeZone(attendance.present, attendance.half, attendance.total)
    }

    val accent = if (safeZone.isSafe) colors.emeraldAccent else colors.amberAccent
    val container = if (safeZone.isSafe) colors.emeraldContainer else colors.amberContainer

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("trainee_safe_zone_card")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(container),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (safeZone.isSafe) Icons.Rounded.Shield else Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Text(
                        text = "80% Safe-Zone Calculator",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Gamified Streak / Consistency Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (safeZone.isSafe) colors.skyContainer else colors.redContainer,
                    border = BorderStroke(
                        1.dp,
                        if (safeZone.isSafe) colors.skyAccent.copy(alpha = 0.5f) else colors.redAccent.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (safeZone.isSafe) Icons.Rounded.EmojiEvents else Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = if (safeZone.isSafe) colors.skyAccent else colors.redAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = safeZone.streakBadgeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (safeZone.isSafe) colors.skyAccent else colors.redAccent
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = container,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = safeZone.headline,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent
                    )
                    Text(
                        text = safeZone.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Trainee My Duty (Mon–Fri) & Live Shop Floor Status Badge Card.
 */
@Composable
fun TraineeDutyAndShopFloorCard(
    myRollNumber: Int?,
    customDutyItems: List<String>,
    myShopFloorEntry: ShopFloorEntry?,
    scanOverrides: ScanOverrideState,
    onOpenDutyTab: () -> Unit,
    dutyOverrides: Map<String, DutyOverrideEntry> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val schedule = remember(customDutyItems, dutyOverrides) {
        DutyRotationEngine.generateRotationSchedule(
            totalRolls = DutyRotationEngine.TOTAL_TRAINEES,
            customDutyItems = customDutyItems,
            overridesByDate = dutyOverrides
        )
    }
    val todaySlot = schedule.firstOrNull()
    val myTurnOffset = remember(myRollNumber, schedule) {
        myRollNumber?.let { DutyRotationEngine.workingDaysUntilRollTurn(it, schedule) }
    }
    val mySlot = remember(myTurnOffset, schedule) {
        myTurnOffset?.let { schedule.getOrNull(it) }
    }
    val isWeekend = remember { DutyRotationEngine.isWeekendToday() }
    val todayPairText = todaySlot?.rolls?.joinToString(" & ") {
        "Roll #${it.toString().padStart(2, '0')}"
    } ?: "Roll #14 & Roll #15"
    val showUpdatedBadge = todaySlot?.isOverridden == true || mySlot?.isOverridden == true

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .testTag("trainee_duty_shopfloor_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section 1: My Mon–Fri Duty Turn
            Surface(
                onClick = onOpenDutyTab,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(
                    1.dp,
                    if (showUpdatedBadge) colors.amberAccent.copy(alpha = 0.7f) else colors.skyAccent.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Mon–Fri Duty (Eggs / Milk / Tool Crib)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isWeekend) "Next Pair: $todayPairText" else "Today: $todayPairText",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.skyAccent
                            )
                            if (showUpdatedBadge) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = colors.amberContainer,
                                    border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.55f))
                                ) {
                                    Text(
                                        text = "Updated by Instructor",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.amberAccent,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        val turnText = when {
                            mySlot == null -> "Tap to view Monday–Friday Duty Rotation (Rolls 1–23)"
                            mySlot.isToday -> "YOU ARE ON DUTY TODAY (${mySlot.dateLabel})"
                            isWeekend -> "Weekend break • Next turn: ${mySlot.dateLabel}"
                            else -> "Your turn: ${mySlot.dateLabel} (in $myTurnOffset working day${if (myTurnOffset == 1) "" else "s"})"
                        }
                        Text(
                            text = turnText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (mySlot?.isToday == true) colors.emeraldAccent else colors.textMuted
                        )
                    }
                }
            }

            // Section 2: Live Shop Floor Status Badge & Card Scan Logout Confirmation
            val hasShopFloor = myShopFloorEntry != null
            val hasCheckedOut = !myShopFloorEntry?.checkOutCardScanTime.isNullOrBlank()
            val sfAccent = when {
                !hasShopFloor -> colors.emeraldAccent
                hasCheckedOut -> colors.emeraldAccent
                else -> colors.amberAccent
            }
            val sfBg = when {
                !hasShopFloor -> colors.emeraldContainer
                hasCheckedOut -> colors.emeraldContainer
                else -> colors.amberContainer
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = sfBg,
                border = BorderStroke(1.dp, sfAccent.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trainee_shopfloor_badge")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            Icon(
                                imageVector = if (hasCheckedOut || !hasShopFloor) Icons.Rounded.CheckCircle else Icons.Rounded.Build,
                                contentDescription = null,
                                tint = sfAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Shop Floor Status",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = sfAccent
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Text(
                                text = when {
                                    !hasShopFloor -> "CLEAR (>=80%)"
                                    hasCheckedOut -> "CARD LOGOUT DONE"
                                    myShopFloorEntry?.sessionType == "SATURDAY" -> "SATURDAY SESSION"
                                    else -> "AFTER 3:00 PM"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = sfAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (!hasShopFloor) {
                        Text(
                            text = "No Shop Floor makeup session assigned. Regular class hours: Mon–Fri 8:00 AM – 3:00 PM.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        val sessionLabel = if (myShopFloorEntry!!.sessionType == "SATURDAY") {
                            "Saturday Shop Floor Makeup"
                        } else {
                            "Post-3:00 PM Shop Floor (till ${myShopFloorEntry.scheduledEndTime})"
                        }
                        Text(
                            text = "$sessionLabel • Reason: ${myShopFloorEntry.assignedReason}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (hasCheckedOut) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Nfc,
                                    contentDescription = null,
                                    tint = colors.emeraldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                val durationPart = if (myShopFloorEntry.durationMinutes > 0) {
                                    " (${myShopFloorEntry.durationMinutes} min)"
                                } else ""
                                val verifiedPart = if (myShopFloorEntry.verifiedByInstructor) " • Verified by Instructor" else " • Awaiting verification"
                                Text(
                                    text = "ID Card Logout Recorded at ${myShopFloorEntry.checkOutCardScanTime}$durationPart$verifiedPart",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = colors.emeraldAccent
                                )
                            }
                        } else {
                            Text(
                                text = if (scanOverrides.shopFloorReleaseNowEnabled) {
                                    "Instructor enabled 'Release Shop Floor Now' — swipe your ID card on the RFID scanner now to complete logout!"
                                } else {
                                    "Scan your ID card on the RFID scanner when leaving after 3:00 PM so your logout timestamp is logged for the Instructor."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }

            if (scanOverrides.earlyClassLogoutEnabled) {
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.skyContainer,
                    border = BorderStroke(1.dp, colors.skyAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Early Class Dismissal Active — RFID card swipes are recording normal OUT timestamps now.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.skyAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
