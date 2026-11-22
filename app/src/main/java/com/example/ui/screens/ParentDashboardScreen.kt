package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.TricolourRibbon
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

@Composable
fun ParentDashboardScreen(
    uiState: AppUiState,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onReasonChanged: (String) -> Unit,
    onReportBunking: () -> Unit,
    onSubmitLeaveReason: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onLogout() }

    val colors = LocalItiColors.current
    val data = uiState.traineeData
    val todayStatus = data?.attendance?.todayStatus?.uppercase().orEmpty()
    val isAbsent = todayStatus == "ABSENT" || todayStatus.isBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        TricolourRibbon(height = 3.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.cardSurface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Parent Portal • Roll #${data?.roll ?: uiState.activeRoll}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = data?.name ?: "Trainee",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }

            Row {
                IconButton(onClick = onRefresh, enabled = !uiState.isRefreshingTrainee) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = colors.skyAccent
                    )
                }
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = "Logout",
                        tint = colors.redAccent
                    )
                }
            }
        }

        if (uiState.isRefreshingTrainee) {
            LinearProgressIndicator(
                color = colors.skyAccent,
                trackColor = colors.cardElevated,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.borderSubtle)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isAbsent) colors.redContainer else colors.emeraldContainer,
                border = BorderStroke(
                    1.dp,
                    if (isAbsent) colors.redAccent.copy(alpha = 0.6f) else colors.emeraldAccent.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "TODAY'S STATUS: ${todayStatus.ifBlank { "ABSENT" }}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isAbsent) colors.redAccent else colors.emeraldAccent
                    )
                    Text(
                        text = "In: ${data?.attendance?.inT ?: "-"}  •  Out: ${data?.attendance?.outT ?: "-"}  •  Attendance: ${data?.attendance?.pct ?: 0}%",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (uiState.parentAlertSuccessMessage != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.emeraldContainer,
                    border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.65f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = colors.emeraldAccent
                            )
                            Text(
                                text = uiState.parentAlertSuccessMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onDismissFeedback) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss",
                                tint = colors.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (uiState.parentAlertError != null) {
                InlineAlertCard(message = uiState.parentAlertError, isError = true)
            }

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
                        text = "Report Bunking / Left Home",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Use this if your ward left home for ITI today but is not marked present.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                    Button(
                        onClick = onReportBunking,
                        enabled = !uiState.isSubmittingParentAlert,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.redAccent,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (uiState.isSubmittingParentAlert) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sending...")
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Report Bunking / Left Home",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

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
                        text = "Send Leave Reason (Manglish / Malayalam)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = uiState.parentLeaveReasonInput,
                        onValueChange = onReasonChanged,
                        label = { Text("Leave Reason") },
                        placeholder = { Text("e.g., Pani aanu / Hospital-il poyi") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.skyAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedLabelColor = colors.skyAccent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = onSubmitLeaveReason,
                        enabled = !uiState.isSubmittingParentAlert,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.skyAccent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Leave Reason",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
