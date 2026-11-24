package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AccessTimeFilled
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsBus
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
import androidx.compose.ui.unit.dp
import com.example.data.RunningLateAlert
import com.example.ui.components.WhatsAppBotHelper
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors

private val RUNNING_LATE_ETA_PRESETS = listOf(
    "09:20 AM (~15m)",
    "09:30 AM (~25m)",
    "09:45 AM (~40m)",
    "10:00 AM+"
)

private val RUNNING_LATE_REASONS = listOf(
    "Bus / Transport Delay",
    "Rain / Road Issue",
    "Family / Health",
    "Other"
)

@Composable
fun TraineeRunningLateCard(
    roll: String,
    studentName: String,
    recentAlerts: List<RunningLateAlert>,
    isSubmitting: Boolean,
    successMessage: String?,
    errorMessage: String?,
    onSubmitRunningLate: (
        expectedArrival: String,
        reasonCategory: String,
        details: String,
        onSuccessReset: () -> Unit
    ) -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val context = LocalContext.current

    var expectedArrival by remember { mutableStateOf(RUNNING_LATE_ETA_PRESETS.first()) }
    var reasonCategory by remember { mutableStateOf(RUNNING_LATE_REASONS.first()) }
    var details by remember { mutableStateOf("") }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.5.dp, colors.amberAccent.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("trainee_running_late_card")
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
                        imageVector = Icons.Rounded.DirectionsBus,
                        contentDescription = null,
                        tint = colors.amberAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Running Late Alert",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Notify Trade Instructor immediately with your expected arrival & reason",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }
            }

            Text(
                text = "Expected Arrival / Delay Window",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RUNNING_LATE_ETA_PRESETS) { preset ->
                    FilterChip(
                        selected = expectedArrival == preset,
                        onClick = { expectedArrival = preset },
                        label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.amberContainer,
                            selectedLabelColor = colors.amberAccent,
                            containerColor = MaterialTheme.colorScheme.background,
                            labelColor = colors.textMuted
                        )
                    )
                }
            }

            OutlinedTextField(
                value = expectedArrival,
                onValueChange = { expectedArrival = it },
                label = { Text("Expected Arrival Time / Delay") },
                leadingIcon = {
                    Icon(Icons.Rounded.AccessTimeFilled, contentDescription = null, tint = colors.amberAccent)
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
                    .testTag("running_late_eta_input")
            )

            Text(
                text = "Reason",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RUNNING_LATE_REASONS) { reason ->
                    FilterChip(
                        selected = reasonCategory == reason,
                        onClick = { reasonCategory = reason },
                        label = { Text(reason, style = MaterialTheme.typography.labelSmall) },
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
                value = details,
                onValueChange = { details = it },
                label = { Text("Optional Note (Route / Details)") },
                placeholder = { Text("e.g., KSRTC bus delayed near Ranni bridge") },
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
                    .testTag("running_late_details_input")
            )

            AnimatedVisibility(visible = successMessage != null) {
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
                            text = successMessage.orEmpty(),
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

            AnimatedVisibility(visible = errorMessage != null) {
                InlineAlertCard(message = errorMessage.orEmpty(), isError = true)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onSubmitRunningLate(expectedArrival, reasonCategory, details) {
                            details = ""
                        }
                    },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.amberAccent,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("send_running_late_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onTertiary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sending...")
                    } else {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Alert Instructor", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }

                OutlinedButton(
                    onClick = {
                        val waMsg = "ITI Ranni EM • Running Late Alert\n" +
                            "Trainee: $studentName (Roll #$roll)\n" +
                            "Expected Arrival: $expectedArrival\n" +
                            "Reason: $reasonCategory" +
                            if (details.isNotBlank()) "\nNote: $details" else ""
                        WhatsAppBotHelper.openWhatsAppBot(context, waMsg)
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colors.emeraldAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.emeraldAccent),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("running_late_whatsapp_button")
                ) {
                    Icon(Icons.Rounded.Chat, contentDescription = "Send via WhatsApp Bot", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WA Bot", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }

            if (recentAlerts.isNotEmpty()) {
                val latest = recentAlerts.first()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Latest Alert Sent • ${listOf(latest.date, latest.time).filter { it.isNotBlank() }.joinToString(" ")}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                            color = colors.amberAccent
                        )
                        Text(
                            text = "ETA: ${latest.expectedArrival} • ${latest.reasonCategory}" +
                                if (latest.details.isNotBlank()) " — ${latest.details}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
