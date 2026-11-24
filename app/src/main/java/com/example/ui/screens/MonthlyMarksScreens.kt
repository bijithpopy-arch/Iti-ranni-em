package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.OfflineSyncQueue

@Composable
fun MonthlyMarksScreen(
    traineeNamesByRoll: Map<Int, String> = emptyMap(),
    onPublishRow: (monthLabel: String, row: ExamMarkRow) -> Unit = { _, _ -> },
    onExportPdf: (monthLabel: String, rows: List<ExamMarkRow>) -> Unit = { _, _ -> }
) {
    val context: Context = LocalContext.current
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val initialRows = remember(traineeNamesByRoll) {
        val draft = OfflineSyncQueue.loadMarksDraft(context)
        if (draft != null && draft.second.isNotEmpty()) {
            draft.second.sortedBy { it.roll }
        } else {
            (1..23).map { roll ->
                ExamMarkRow(
                    roll = roll,
                    name = traineeNamesByRoll[roll]?.takeIf { it.isNotBlank() } ?: "Trainee Roll $roll",
                    theoryMarks = "",
                    labMarks = ""
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        statusMessage?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            InstructorExamMarksPanel(
                initialRows = initialRows,
                onSaveDraftOffline = { monthLabel, rows ->
                    OfflineSyncQueue.saveMarksDraft(context, monthLabel, rows)
                    OfflineSyncQueue.enqueue(
                        context = context,
                        actionType = "marks_draft",
                        payload = mapOf(
                            "monthLabel" to monthLabel,
                            "studentCount" to rows.size.toString()
                        )
                    )
                    statusMessage = "Saved offline draft for $monthLabel (${rows.size} trainees)."
                },
                onPublishMarks = { monthLabel, rows ->
                    OfflineSyncQueue.saveMarksDraft(context, monthLabel, rows)
                    rows.forEach { row ->
                        onPublishRow(monthLabel, row)
                    }
                    statusMessage = "Published & notified marks for $monthLabel (Theory /50 + Lab /100)."
                },
                onExportMarksPdf = { monthLabel, rows ->
                    onExportPdf(monthLabel, rows)
                    statusMessage = "Exported marks sheet PDF for $monthLabel."
                }
            )
        }
    }
}
