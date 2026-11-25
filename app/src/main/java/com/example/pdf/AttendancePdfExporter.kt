package com.example.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import com.example.data.StaffAttendanceRecord
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AttendancePdfMode(val title: String, val tag: String) {
    TODAY("TODAY'S ATTENDANCE", "TODAY"),
    TILL_DATE("CUMULATIVE (TILL DATE)", "TILL_DATE"),
    MONTH("MONTHLY REGISTER", "MONTH"),
    CUSTOM_RANGE("CUSTOM DATE RANGE", "CUSTOM")
}

data class PdfPrintOptions(
    val showRoll: Boolean = true,
    val showName: Boolean = true,
    val showToday: Boolean = true,
    val showIn: Boolean = true,
    val showOut: Boolean = true,
    val showPresent: Boolean = true,
    val showHalf: Boolean = true,
    val showTotal: Boolean = true,
    val showPct: Boolean = true,
    val showStatus: Boolean = true,
    val showSignatures: Boolean = true,
    val requestedBy: String = "Instructor / Principal"
)

object AttendancePdfExporter {
    fun export(
        context: Context,
        records: List<StaffAttendanceRecord>,
        overrides: Map<String, Pair<Double, Double>>,
        mode: AttendancePdfMode,
        fromDate: String = "",
        toDate: String = "",
        printOptions: PdfPrintOptions = PdfPrintOptions()
    ) {
        val now = Date()
        val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(now)
        val cleanFrom = fromDate.trim().ifEmpty { dateStr }
        val cleanTo = toDate.trim().ifEmpty { dateStr }
        val safeFrom = cleanFrom.replace("/", "-").replace(" ", "_")
        val safeTo = cleanTo.replace("/", "-").replace(" ", "_")

        val fileStamp = when (mode) {
            AttendancePdfMode.TODAY -> "Today_$dateStr"
            AttendancePdfMode.TILL_DATE -> "TillDate_$dateStr"
            AttendancePdfMode.MONTH -> "Month_${SimpleDateFormat("MMM-yyyy", Locale.US).format(now)}"
            AttendancePdfMode.CUSTOM_RANGE -> "Range_${safeFrom}_to_${safeTo}"
        }
        val modeSuffix = when (mode) {
            AttendancePdfMode.TODAY -> " [TODAY]"
            AttendancePdfMode.TILL_DATE -> " [TILL DATE]"
            AttendancePdfMode.MONTH -> " [${SimpleDateFormat("MMM yyyy", Locale.US).format(now).uppercase(Locale.US)}]"
            AttendancePdfMode.CUSTOM_RANGE -> " [$cleanFrom TO $cleanTo]"
        }
        val dateSubLine = when (mode) {
            AttendancePdfMode.CUSTOM_RANGE -> "Period: $cleanFrom to $cleanTo  |  By: ${printOptions.requestedBy}  |  Generated: $dateStr"
            else -> "Date: $dateStr  |  By: ${printOptions.requestedBy}"
        }

        data class ActiveCol(val key: String, val label: String, val widthRatio: Float)

        val activeCols = buildList {
            if (printOptions.showRoll) add(ActiveCol("roll", "Roll", 0.06f))
            if (printOptions.showName) add(ActiveCol("name", "Trainee Name", 0.28f))
            if (printOptions.showToday) add(ActiveCol("today", "Today", 0.10f))
            if (printOptions.showIn) add(ActiveCol("in", "IN", 0.07f))
            if (printOptions.showOut) add(ActiveCol("out", "OUT", 0.07f))
            if (printOptions.showPresent) add(ActiveCol("pres", "Pres", 0.08f))
            if (printOptions.showHalf) add(ActiveCol("half", "Half", 0.07f))
            if (printOptions.showTotal) add(ActiveCol("tot", "Tot", 0.07f))
            if (printOptions.showPct) add(ActiveCol("pct", "%", 0.07f))
            if (printOptions.showStatus) add(ActiveCol("status", "Status", 0.14f))
        }.ifEmpty {
            listOf(ActiveCol("name", "Trainee Name", 0.75f), ActiveCol("pct", "%", 0.25f))
        }

        val totalRatio = activeCols.sumOf { it.widthRatio.toDouble() }.toFloat().coerceAtLeast(0.1f)
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val left = 24f
        val right = 571f
        val tableWidth = right - left
        val rowH = 21f

        val colX = FloatArray(activeCols.size + 1)
        colX[0] = left
        var cursorX = left
        activeCols.forEachIndexed { idx, col ->
            cursorX += (col.widthRatio / totalRatio) * tableWidth
            colX[idx + 1] = cursorX
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 0.8f
        }

        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var c = page.canvas
        var y = 34f

        fun drawHeaderAndTableTop() {
            c.drawText(
                "GOVT. ITI RANNI - ELECTRONICS MECHANIC (2025-2027) Attendance Register$modeSuffix",
                left,
                y,
                titlePaint
            )
            y += 15f
            c.drawText(dateSubLine, left, y, subPaint)
            y += 12f
            c.drawLine(left, y, right, y, gridPaint)
            y += 14f
            activeCols.forEachIndexed { i, col ->
                c.drawText(col.label, colX[i] + 3f, y, headPaint)
            }
            y += 6f
            c.drawLine(left, y, right, y, gridPaint)
        }

        drawHeaderAndTableTop()
        records.forEach { rec ->
            if (y + rowH > pageHeight - 75f) {
                doc.finishPage(page)
                pageNum += 1
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                c = page.canvas
                y = 34f
                drawHeaderAndTableTop()
            }
            val ov = overrides[rec.roll]
            val effPres = ov?.first ?: (rec.present + (rec.half * 0.5))
            val effTot = ov?.second ?: rec.total.toDouble()
            val pctVal = if (effTot > 0.0) ((effPres / effTot) * 100.0).coerceIn(0.0, 100.0) else rec.pct.toDouble()
            val pctInt = kotlin.math.round(pctVal).toInt()
            val (statusLabel, statusColor) = when {
                pctVal >= 83.0 -> "SAFE" to Color.parseColor("#15803D")
                pctVal >= 80.0 -> "WARN" to Color.parseColor("#B45309")
                else -> "SHOP FLOOR" to Color.parseColor("#B91C1C")
            }
            val topY = y
            val botY = y + rowH
            val midTextY = topY + 13.5f
            val presStr = if (effPres % 1.0 == 0.0) effPres.toInt().toString() else String.format(Locale.US, "%.1f", effPres)
            val totStr = if (effTot % 1.0 == 0.0) effTot.toInt().toString() else String.format(Locale.US, "%.1f", effTot)
            val todayStr = when (mode) {
                AttendancePdfMode.MONTH -> "MONTH"
                AttendancePdfMode.CUSTOM_RANGE -> "RANGE"
                else -> rec.todayStatus.uppercase(Locale.US)
            }

            activeCols.forEachIndexed { idx, col ->
                when (col.key) {
                    "roll" -> c.drawText(rec.roll, colX[idx] + 3f, midTextY, cellPaint)
                    "name" -> c.drawText(rec.name.take(26), colX[idx] + 3f, midTextY, cellPaint)
                    "today" -> c.drawText(todayStr.take(8), colX[idx] + 3f, midTextY, cellPaint)
                    "in" -> c.drawText(rec.inT.take(5), colX[idx] + 3f, midTextY, cellPaint)
                    "out" -> c.drawText(rec.outT.take(5), colX[idx] + 3f, midTextY, cellPaint)
                    "pres" -> c.drawText(presStr, colX[idx] + 3f, midTextY, cellPaint)
                    "half" -> c.drawText(rec.half.toString(), colX[idx] + 3f, midTextY, cellPaint)
                    "tot" -> c.drawText(totStr, colX[idx] + 3f, midTextY, cellPaint)
                    "pct" -> c.drawText("$pctInt%", colX[idx] + 3f, midTextY, cellPaint)
                    "status" -> {
                        tagPaint.color = statusColor
                        c.drawText(statusLabel, colX[idx] + 4f, midTextY, tagPaint)
                    }
                }
            }
            c.drawLine(left, botY, right, botY, gridPaint)
            for (x in colX) c.drawLine(x, topY, x, botY, gridPaint)
            y = botY
        }

        if (printOptions.showSignatures) {
            y += 28f
            if (y + 24f > pageHeight - 20f) {
                doc.finishPage(page)
                pageNum += 1
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
                c = page.canvas
                y = 60f
            }
            c.drawText("Trade Instructor Signature:", left, y, subPaint)
            c.drawText("Principal Signature:", left + 260f, y, subPaint)
        }

        doc.finishPage(page)

        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val outFile = File(dir, "EMech_Attendance_Report_$fileStamp.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        Toast.makeText(context, "SAVED: ${outFile.name}", Toast.LENGTH_LONG).show()
    }
}
