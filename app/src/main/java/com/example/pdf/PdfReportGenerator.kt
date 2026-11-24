package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.AttendanceApiClient
import com.example.data.StaffDashboardData
import com.example.ui.screens.ExamMarkRow
import java.io.File
import java.io.FileOutputStream

object PdfReportGenerator {

    fun generateAndShareMarksPdf(
        context: Context,
        monthLabel: String,
        examDate: String,
        theoryMax: Int,
        labMax: Int,
        rows: List<ExamMarkRow>,
        signedBy: String,
        signatureData: String
    ): Result<File> {
        return runCatching {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = doc.startPage(pageInfo)
            val c: Canvas = page.canvas

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 15f
                isFakeBoldText = true
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(51, 65, 85)
                textSize = 10f
            }
            val hdrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 9.5f
                isFakeBoldText = true
            }
            val hdrBg = Paint().apply { color = Color.rgb(16, 36, 73) }
            val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 9f
            }
            val linePaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 0.8f
            }

            var y = 38f
            c.drawText("GOVT. ITI RANNI — ELECTRONICS MECHANIC (2025–27)", 32f, y, titlePaint)
            y += 16f
            c.drawText("Signed Marks Register: $monthLabel   |   Date: $examDate   |   Theory Max: $theoryMax   |   Practical Max: $labMax", 32f, y, subPaint)
            y += 18f

            // Table header
            c.drawRect(32f, y, 563f, y + 20f, hdrBg)
            c.drawText("Roll", 38f, y + 14f, hdrPaint)
            c.drawText("Trainee Name", 76f, y + 14f, hdrPaint)
            c.drawText("Theory/$theoryMax", 285f, y + 14f, hdrPaint)
            c.drawText("Lab/$labMax", 355f, y + 14f, hdrPaint)
            c.drawText("Total", 415f, y + 14f, hdrPaint)
            c.drawText("Remarks", 465f, y + 14f, hdrPaint)
            y += 20f

            rows.sortedBy { it.roll }.take(25).forEach { row ->
                val rowTop = y
                val rowBot = y + 21f
                c.drawLine(32f, rowBot, 563f, rowBot, linePaint)
                c.drawText(row.roll.toString().padStart(2, '0'), 38f, rowTop + 14f, cellPaint)
                c.drawText(row.name.take(30), 76f, rowTop + 14f, cellPaint)
                c.drawText(row.theoryMarks.ifBlank { "-" }, 295f, rowTop + 14f, cellPaint)
                c.drawText(row.labMarks.ifBlank { "-" }, 362f, rowTop + 14f, cellPaint)
                val totalMax = theoryMax + labMax
                val totVal = (row.theoryMarks.toIntOrNull() ?: 0) + (row.labMarks.toIntOrNull() ?: 0)
                c.drawText("$totVal/$totalMax", 415f, rowTop + 14f, cellPaint)
                c.drawText(row.remarks.take(18), 465f, rowTop + 14f, cellPaint)
                y = rowBot
            }

            y += 22f
            c.drawText("Digitally Signed By: ${signedBy.ifBlank { "Trade Instructor (EM)" }}   |   Timestamp: ${AttendanceApiClient.todayIstDateString()}", 32f, y, subPaint)
            y += 10f
            drawSignatureStrokesOnPdf(c, signatureData, 32f, y, 210f, 52f)

            doc.finishPage(page)
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val outFile = File(dir, "EM_Signed_Marks_${System.currentTimeMillis()}.pdf")
            FileOutputStream(outFile).use { doc.writeTo(it) }
            doc.close()
            sharePdfFile(context, outFile, "Export Digitally Signed Marks Sheet")
            outFile
        }
    }

    fun generateAndShareAttendancePdf(
        context: Context,
        staffData: StaffDashboardData,
        roleLabel: String
    ): Result<File> {
        return runCatching {
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = doc.startPage(pageInfo)
            val c: Canvas = page.canvas

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 15f
                isFakeBoldText = true
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(51, 65, 85)
                textSize = 10f
            }
            val hdrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 9.5f
                isFakeBoldText = true
            }
            val hdrBg = Paint().apply { color = Color.rgb(16, 36, 73) }
            val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 9f
            }
            val linePaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 0.8f
            }

            var y = 40f
            c.drawText("GOVT. ITI RANNI — DAILY ATTENDANCE REGISTER", 32f, y, titlePaint)
            y += 16f
            c.drawText("Trade: Electronics Mechanic (2025–27)   |   Date: ${staffData.attendanceDate}   |   Generated by: $roleLabel", 32f, y, subPaint)
            y += 20f

            c.drawRect(32f, y, 563f, y + 20f, hdrBg)
            c.drawText("Roll", 38f, y + 14f, hdrPaint)
            c.drawText("Trainee Name", 76f, y + 14f, hdrPaint)
            c.drawText("Status", 280f, y + 14f, hdrPaint)
            c.drawText("In", 360f, y + 14f, hdrPaint)
            c.drawText("Out", 430f, y + 14f, hdrPaint)
            c.drawText("Overall %", 500f, y + 14f, hdrPaint)
            y += 20f

            staffData.allAttendance.sortedBy { it.roll.toIntOrNull() ?: 99 }.take(25).forEach { rec ->
                val rowTop = y
                val rowBot = y + 22f
                c.drawLine(32f, rowBot, 563f, rowBot, linePaint)
                c.drawText(rec.roll.padStart(2, '0'), 38f, rowTop + 15f, cellPaint)
                c.drawText(rec.name.take(28), 76f, rowTop + 15f, cellPaint)
                c.drawText(rec.todayStatus, 280f, rowTop + 15f, cellPaint)
                c.drawText(rec.inT, 360f, rowTop + 15f, cellPaint)
                c.drawText(rec.outT, 430f, rowTop + 15f, cellPaint)
                c.drawText("${rec.pct}%", 505f, rowTop + 15f, cellPaint)
                y = rowBot
            }

            doc.finishPage(page)
            val dir = File(context.cacheDir, "reports").apply { mkdirs() }
            val outFile = File(dir, "EM_Attendance_${staffData.attendanceDate}.pdf")
            FileOutputStream(outFile).use { doc.writeTo(it) }
            doc.close()
            sharePdfFile(context, outFile, "Export Attendance Register PDF")
            outFile
        }
    }

    private fun drawSignatureStrokesOnPdf(
        canvas: Canvas,
        serialized: String,
        left: Float,
        top: Float,
        width: Float,
        height: Float
    ) {
        val boxPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(left, top, left + width, top + height, boxPaint)
        if (serialized.isBlank()) return
        val penPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        serialized.split("|").forEach { strokeStr ->
            val pts = strokeStr.split(";").mapNotNull { pair ->
                val xy = pair.split(",")
                val x = xy.getOrNull(0)?.toFloatOrNull()
                val y = xy.getOrNull(1)?.toFloatOrNull()
                if (x != null && y != null) (left + x * width) to (top + y * height) else null
            }
            if (pts.size >= 2) {
                val path = Path().apply {
                    moveTo(pts.first().first, pts.first().second)
                    pts.drop(1).forEach { lineTo(it.first, it.second) }
                }
                canvas.drawPath(path, penPaint)
            }
        }
    }

    private fun sharePdfFile(context: Context, file: File, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}

@Composable
fun FullReportExportDialog(
    staffData: StaffDashboardData,
    roleLabel: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var statusMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Official PDF Report", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Generate an A4 PDF directly on your device for ${staffData.attendanceDate} (${staffData.allAttendance.size} trainees).",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = {
                        val res = PdfReportGenerator.generateAndShareAttendancePdf(context, staffData, roleLabel)
                        statusMsg = res.fold(
                            onSuccess = { "Attendance PDF ready to share or print." },
                            onFailure = { it.message ?: "Failed to generate PDF." }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Generate Daily Attendance PDF") }

                OutlinedButton(
                    onClick = {
                        val rows = staffData.allAttendance.sortedBy { it.roll.toIntOrNull() ?: 99 }.map { rec ->
                            val latestMark = staffData.monthlyMarks.firstOrNull { it.roll == rec.roll }
                            ExamMarkRow(
                                roll = rec.roll.toIntOrNull() ?: 0,
                                name = rec.name,
                                theoryMarks = latestMark?.theoryMarks?.toString().orEmpty(),
                                labMarks = latestMark?.labMarks?.toString().orEmpty(),
                                remarks = latestMark?.remarks.orEmpty(),
                                published = latestMark?.published ?: false
                            )
                        }
                        val signedMark = staffData.monthlyMarks.firstOrNull { it.signedBy.isNotBlank() }
                        val res = PdfReportGenerator.generateAndShareMarksPdf(
                            context = context,
                            monthLabel = signedMark?.monthLabel ?: "Monthly Evaluation",
                            examDate = signedMark?.examDate?.ifBlank { staffData.attendanceDate } ?: staffData.attendanceDate,
                            theoryMax = signedMark?.theoryMax ?: 50,
                            labMax = signedMark?.labMax ?: 100,
                            rows = rows,
                            signedBy = signedMark?.signedBy ?: roleLabel,
                            signatureData = signedMark?.signatureData.orEmpty()
                        )
                        statusMsg = res.fold(
                            onSuccess = { "Signed Marks Register PDF generated." },
                            onFailure = { it.message ?: "Failed to generate Marks PDF." }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Generate Signed Marks Register PDF") }

                if (statusMsg != null) {
                    Text(statusMsg.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
