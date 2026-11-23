package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RunningLateDialog(
    onDismiss: () -> Unit,
    onSubmit: (delayOrEta: String, reason: String) -> Unit
) {
    val presetReasons = listOf("Bus / Transport delay", "Rain / Road block", "Health / Family", "Other")
    var etaText by remember { mutableStateOf("15-20 mins / ~09:45 AM") }
    var selectedReason by remember { mutableStateOf(presetReasons.first()) }
    var customReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Running Late Alert") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = etaText,
                    onValueChange = { etaText = it },
                    label = { Text("Expected Arrival / Delay") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                presetReasons.forEach { reason ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (selectedReason == "Other") {
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        label = { Text("Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (selectedReason == "Other") customReason.ifBlank { "Other delay" } else selectedReason
                    onSubmit(etaText.trim(), finalReason.trim())
                }
            ) { Text("Send Alert") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PrivateWorkLabMarkQueryCard(
    isSubmitting: Boolean,
    statusMessage: String?,
    onSendQuery: (subject: String, question: String) -> Unit
) {
    var subject by remember { mutableStateOf("Lab / Work Mark Clarification") }
    var question by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Work / Lab Mark Query (Private to Trade Instructor)", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Topic / Lab") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Your mark query") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (question.isNotBlank()) {
                        onSendQuery(subject.trim(), question.trim())
                        question = ""
                    }
                },
                enabled = !isSubmitting && question.isNotBlank()
            ) {
                Text(if (isSubmitting) "Sending..." else "Send Private Query")
            }
            if (!statusMessage.isNullOrBlank()) {
                Text(statusMessage, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
