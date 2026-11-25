package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AlertKind(val tag: String) {
    LATE("LATE"),
    PARENTS_ALERT("PARENTS ALERT"),
    HOLIDAY_DOUBT("HOLIDAY DOUBT"),
    PARENT_DOUBT("PARENT DOUBT")
}

data class AlertReplyEntry(
    val sender: String,
    val message: String,
    val timestamp: String,
    val fromInstructor: Boolean
)

data class InstructorAlertItem(
    val id: String,
    val kind: AlertKind,
    val senderLabel: String,
    val subject: String,
    val body: String,
    val timestamp: String,
    val unread: Boolean = true,
    val allowOtherSideReply: Boolean = true,
    val thread: List<AlertReplyEntry> = emptyList()
)

private fun nowStamp(): String =
    SimpleDateFormat("dd MMM HH:mm", Locale.US).format(Date()).uppercase(Locale.US)

fun defaultInstructorAlerts(): List<InstructorAlertItem> = emptyList()

@Composable
fun InstructorAlertHubPanel(
    alerts: List<InstructorAlertItem>,
    onUpdateAlerts: (List<InstructorAlertItem>) -> Unit,
    isInstructor: Boolean = true,
    cardSurface: Color = MaterialTheme.colorScheme.surface,
    borderSubtle: Color = MaterialTheme.colorScheme.outlineVariant,
    textMuted: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    var activeAlertId by remember { mutableStateOf<String?>(null) }

    if (alerts.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No alerts.",
                style = MaterialTheme.typography.bodyMedium,
                color = textMuted
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(alerts, key = { it.id }) { alert ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardSurface,
                    border = BorderStroke(
                        1.dp,
                        if (alert.unread) MaterialTheme.colorScheme.primary else borderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onUpdateAlerts(
                                alerts.map { if (it.id == alert.id) it.copy(unread = false) else it }
                            )
                            activeAlertId = alert.id
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                alert.senderLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                alert.timestamp,
                                style = MaterialTheme.typography.labelSmall,
                                color = textMuted
                            )
                        }
                        Text(
                            alert.subject,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            alert.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = textMuted
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Messages: ${alert.thread.size} • Tap to view & reply",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                if (alert.allowOtherSideReply) "Other side can reply" else "Reply locked",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (alert.allowOtherSideReply) MaterialTheme.colorScheme.tertiary else textMuted
                            )
                        }
                    }
                }
            }
        }
    }

    val currentItem = alerts.find { it.id == activeAlertId }
    if (currentItem != null) {
        AlertThreadModal(
            item = currentItem,
            isInstructor = isInstructor,
            cardSurface = cardSurface,
            borderSubtle = borderSubtle,
            textMuted = textMuted,
            onDismiss = { activeAlertId = null },
            onDeleteAlert = {
                onUpdateAlerts(alerts.filterNot { it.id == currentItem.id })
                activeAlertId = null
            },
            onSendReply = { replyText ->
                if (replyText.isNotBlank() && (isInstructor || currentItem.allowOtherSideReply)) {
                    val sender = if (isInstructor) "Instructor" else currentItem.senderLabel
                    val entry = AlertReplyEntry(sender, replyText.trim(), nowStamp(), isInstructor)
                    onUpdateAlerts(
                        alerts.map {
                            if (it.id == currentItem.id) {
                                it.copy(unread = false, thread = it.thread + entry)
                            } else {
                                it
                            }
                        }
                    )
                }
            },
            onToggleOtherSideReply = { allowed ->
                if (isInstructor) {
                    onUpdateAlerts(
                        alerts.map {
                            if (it.id == currentItem.id) it.copy(allowOtherSideReply = allowed) else it
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun AlertThreadModal(
    item: InstructorAlertItem,
    isInstructor: Boolean,
    cardSurface: Color,
    borderSubtle: Color,
    textMuted: Color,
    onDismiss: () -> Unit,
    onDeleteAlert: () -> Unit,
    onSendReply: (String) -> Unit,
    onToggleOtherSideReply: (Boolean) -> Unit
) {
    var draft by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardSurface,
            border = BorderStroke(1.dp, borderSubtle),
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
                    Text(
                        item.senderLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        if (isInstructor) {
                            TextButton(onClick = onDeleteAlert) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }

                Text(
                    item.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    item.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = textMuted
                )

                HorizontalDivider(color = borderSubtle)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(item.thread) { msg ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (msg.fromInstructor) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            border = BorderStroke(1.dp, borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        msg.sender,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        msg.timestamp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textMuted
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    msg.message,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                if (isInstructor) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Allow other side to reply",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = item.allowOtherSideReply,
                            onCheckedChange = { onToggleOtherSideReply(it) }
                        )
                    }
                }

                if (isInstructor || item.allowOtherSideReply) {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text("Write reply...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Button(
                        onClick = {
                            if (draft.isNotBlank()) {
                                onSendReply(draft)
                                draft = ""
                            }
                        },
                        enabled = draft.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send Reply")
                    }
                } else {
                    Text(
                        "Instructor has locked replies for this alert.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textMuted
                    )
                }
            }
        }
    }
}
