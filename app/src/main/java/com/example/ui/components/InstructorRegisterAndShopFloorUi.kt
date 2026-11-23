package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceApiClient

data class PunchHistoryItem(
    val roll: String,
    val name: String,
    val date: String,
    val previousStatus: String,
    val newStatus: String
)

@Composable
fun InstructorTraineeRegisterRow(
    roll: String,
    name: String,
    date: String,
    currentStatus: String,
    present: Int,
    half: Int,
    total: Int,
    pct: Int,
    shopFloorStatus: String,
    shopFloorHours: Double,
    alreadyPunchedToday: Boolean,
    canUndo: Boolean,
    onApplyPunch: (roll: String, status: String, allowByMistake: Boolean) -> Unit,
    onUndoLast: () -> Unit
) {
    var pendingStatus by remember { mutableStateOf<String?>(null) }
    var showByMistakeDialog by remember { mutableStateOf(false) }

    val neededFor80 = AttendanceApiClient.sessionsNeededForTargetPct(
        present = present,
        half = half,
        total = total,
        targetPct = 80.0
    )

    fun handlePunchClick(targetStatus: String) {
        if (alreadyPunchedToday) {
            pendingStatus = targetStatus
            showByMistakeDialog = true
        } else {
            onApplyPunch(roll, targetStatus, false)
        }
    }

    if (showByMistakeDialog && pendingStatus != null) {
        AlertDialog(
            onDismissRequest = { showByMistakeDialog = false },
            title = { Text("Already Punched Today") },
            text = {
                Text("Roll #$roll ($name) is already punched for $date. Only one punch per day is allowed unless marked again by mistake.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val chosen = pendingStatus ?: return@Button
                        showByMistakeDialog = false
                        pendingStatus = null
                        onApplyPunch(roll, chosen, true)
                    }
                ) { Text("Mark again (by mistake)") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showByMistakeDialog = false
                        pendingStatus = null
                    }
                ) { Text("Cancel") }
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("#$roll • $name ($pct%)", style = MaterialTheme.typography.titleSmall)
                    Text("Today: $currentStatus", style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = { handlePunchClick("PRESENT") }) { Text("P") }
                    OutlinedButton(onClick = { handlePunchClick("LATE") }) { Text("Late") }
                    if (canUndo) {
                        TextButton(onClick = onUndoLast) { Text("Undo") }
                    }
                }
            }
            Text(
                "Shop Floor: $shopFloorStatus • ${shopFloorHours}h • " +
                    if (neededFor80 == 0) "80% met" else "Needs $neededFor80 class(es) for 80%",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
