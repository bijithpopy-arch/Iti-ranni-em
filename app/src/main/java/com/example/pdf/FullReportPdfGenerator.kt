package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.AcademicSubmission
import com.example.data.AcademicSubmissionStatuses
import com.example.data.AttendanceMath
import com.example.data.DutyRotationEngine
import com.example.data.IstDateTimeFormatter
import com.example.data.StaffDashboardData
import com.example.ui.theme.LocalItiColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class PdfReportSection(val title: String, val subtitle: String) {
    ATTENDANCE("1. Daily & Cumulative Attendance", "All 23 trainees: Status, IN/OUT (IST), P/H/Total & %"),
    ACADEMIC_SUBMISSIONS("2. Theory Notes & Lab Log Book", "Verification & signed status for all 23 trainees"),
    LEAVE_REQUESTS("3. Leave & Out-Pass Requests", "Pending/recent requests, dates, reasons & status"),
    DUTY_AND_SHOP_FLOOR("4. Duty Rotation & Shop Floor", "Weekday duties, makeup sessions & card logouts"),
    NOTICE_BOARD("5. Published Notice Board Archive", "Date, category, title, author & full body"),
    PARENT_ALERTS("6. Parent Alerts Log", "Parent & running-late alerts if records exist")
}

object FullReportPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 28f
    private const val MARGIN_RIGHT = 28f
    private const val MARGIN_TOP = 32f
    private const val MARGIN_BOTTOM = 44f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    private val colorNavy = Color.rgb(15, 35, 66)
    private val colorAccent = Color.rgb(2, 132, 199)
    private val colorHeaderBg = Color.rgb(226, 232, 240)
    private val colorZebra = Color.rgb(248, 250, 252)
    private val colorBorder = Color.rgb(203, 213, 225)
    private val colorTextDark = Color.rgb(15, 23, 42)
    private val colorTextMuted = Color.rgb(71, 85, 105)
    private val colorGreen = Color.rgb(5, 150, 105)
    private val colorAmber = Color.rgb(180, 83, 9)
    private val colorRed = Color.rgb(185, 28, 28)

    suspend fun generateReportPdf(
        context: Context,
        staffData: StaffDashboardData?,
        sections: Set<PdfReportSection>,
        generatedByRole: String
    ): File = withContext(Dispatchers.IO) {
        val selected = if (sections.isEmpty()) PdfReportSection.entries.toSet() else sections
        val generatedAtIst = IstDateTimeFormatter.nowIstDateTimeString()
        val reportDate = IstDateTimeFormatter.formatDateOnly(
            staffData?.attendanceDate,
            IstDateTimeFormatter.nowIstDateString()
        )

        val document = PdfDocument()
        val writer = MultiPagePdfWriter(
            document = document,
            reportDate = reportDate,
            generatedAtIst = generatedAtIst,
            generatedByRole = generatedByRole
        )

        try {
            writer.startFirstPage()

            if (PdfReportSection.ATTENDANCE in selected) {
                writeAttendanceSection(writer, staffData)
            }
            if (PdfReportSection.ACADEMIC_SUBMISSIONS in selected) {
                writeAcademicSubmissionsSection(writer, staffData)
            }
            if (PdfReportSection.LEAVE_REQUESTS in selected) {
                writeLeaveRequestsSection(writer, staffData)
            }
            if (PdfReportSection.DUTY_AND_SHOP_FLOOR in selected) {
                writeDutyAndShopFloorSection(writer, staffData)
            }
            if (PdfReportSection.NOTICE_BOARD in selected) {
                writeNoticeBoardSection(writer, staffData)
            }
            if (PdfReportSection.PARENT_ALERTS in selected) {
                writeParentAlertsSection(writer, staffData)
            }

            writer.drawSignatureBlock()
            writer.finishAllPages()

            val safeStamp = generatedAtIst
                .replace(":", "-")
                .replace(" ", "_")
                .replace("/", "-")
            val outFile = File(context.cacheDir, "ITI_Ranni_EM_Full_Report_$safeStamp.pdf")
            FileOutputStream(outFile).use { fos ->
                document.writeTo(fos)
            }
            outFile
        } finally {
            document.close()
        }
    }

    fun launchShareOrViewChooser(context: Context, pdfFile: File, viewDirectly: Boolean = false) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            if (viewDirectly) {
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(viewIntent, "Open / Print ITI Ranni PDF Report"))
            } else {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "Govt. ITI Ranni (EM) Official Report — ${IstDateTimeFormatter.nowIstDateString()}"
                    )
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Attached is the official Govt. ITI Ranni — Electronics Mechanic (2025–2027) report generated at ${IstDateTimeFormatter.nowIstDateTimeString()} IST."
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Share, Save or Print PDF Report").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "PDF saved to cache (${pdfFile.name}), but no PDF viewer/share app was available.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun writeAttendanceSection(writer: MultiPagePdfWriter, data: StaffDashboardData?) {
        val records = data?.allAttendance.orEmpty()
        val presentToday = records.count {
            val s = it.todayStatus.uppercase()
            s == "PRESENT" || s == "LATE" || s == "ON-DUTY" || s.contains("OUT-PASS")
        }
        val halfToday = records.count { it.todayStatus.uppercase().contains("HALF") }
        val absentToday = (records.size - presentToday - halfToday).coerceAtLeast(0)
        val below80 = records.count { it.pct < 80 }

        writer.drawSectionHeader(
            title = "SECTION 1: DAILY & CUMULATIVE ATTENDANCE REGISTER",
            meta = "Total: ${records.size}  |  Present: $presentToday  |  Half-Day: $halfToday  |  Absent: $absentToday  |  <80% Risk: $below80"
        )

        val headers = listOf("Roll", "Trainee Name", "Today", "IN (IST)", "OUT (IST)", "P", "H", "Tot", "%")
        val widths = floatArrayOf(30f, 155f, 70f, 64f, 64f, 34f, 32f, 38f, 52f)
        writer.drawTableHeader(headers, widths)

        if (records.isEmpty()) {
            writer.drawEmptyRow("No attendance records available in current session.")
            return
        }

        records.forEachIndexed { idx, rec ->
            val inFormatted = IstDateTimeFormatter.formatTimeOnly(rec.inT, "-")
            val outFormatted = IstDateTimeFormatter.formatTimeOnly(rec.outT, "-")
            val pctColor = when {
                rec.pct >= 80 -> colorGreen
                rec.pct >= 75 -> colorAmber
                else -> colorRed
            }
            writer.drawTableRow(
                cells = listOf(
                    rec.roll.padStart(2, '0'),
                    rec.name,
                    rec.todayStatus.uppercase(),
                    inFormatted,
                    outFormatted,
                    rec.present.toString(),
                    rec.half.toString(),
                    rec.total.toString(),
                    "${rec.pct.coerceIn(0, 100)}%"
                ),
                widths = widths,
                isZebra = idx % 2 == 1,
                accentColumnIndex = 8,
                accentColor = pctColor
            )
        }
        writer.addSpacing(14f)
    }

    private fun writeAcademicSubmissionsSection(writer: MultiPagePdfWriter, data: StaffDashboardData?) {
        val attendance = data?.allAttendance.orEmpty()
        val subsByRoll = data?.academicSubmissions.orEmpty().associateBy { it.roll.trim() }

        // Build full 23-trainee merged roster so every trainee is represented in the official report
        val merged: List<AcademicSubmission> = if (attendance.isNotEmpty()) {
            attendance.map { att ->
                subsByRoll[att.roll.trim()] ?: AcademicSubmission(
                    roll = att.roll,
                    studentName = att.name,
                    theoryTopic = "Not submitted",
                    theoryStatus = "Pending",
                    labJobName = "Not submitted",
                    labStatus = "Incomplete",
                    verificationStatus = AcademicSubmissionStatuses.NOT_SUBMITTED
                )
            }
        } else {
            data?.academicSubmissions.orEmpty()
        }

        val totalCount = merged.size.coerceAtLeast(23)
        val verifiedCount = merged.count { it.isVerifiedOrForwarded }
        val signedLabCount = merged.count { it.labStatus.equals("Signed by Instructor", ignoreCase = true) }
        val forwardedCount = merged.count { it.isForwarded }

        writer.drawSectionHeader(
            title = "SECTION 2: THEORY NOTES & LAB LOG BOOK VERIFICATION REPORT",
            meta = "$verifiedCount/$totalCount Verified  |  $signedLabCount/$totalCount Lab Logs Signed  |  $forwardedCount Forwarded to Principal"
        )

        val headers = listOf("Roll", "Trainee Name", "Theory Notes & Status", "Lab Log & Status", "Verification", "Verified At (IST)")
        val widths = floatArrayOf(28f, 92f, 126f, 126f, 80f, 87f)
        writer.drawTableHeader(headers, widths)

        if (merged.isEmpty()) {
            writer.drawEmptyRow("No Theory Notes / Lab Log Book submissions recorded yet.")
            return
        }

        merged.forEachIndexed { idx, sub ->
            val theoryCell = if (sub.theoryTopic.isBlank() || sub.verificationStatus == AcademicSubmissionStatuses.NOT_SUBMITTED) {
                "Not submitted"
            } else {
                "${sub.theoryTopic} [${sub.theoryStatus}]"
            }
            val labCell = if (sub.labJobName.isBlank() || sub.verificationStatus == AcademicSubmissionStatuses.NOT_SUBMITTED) {
                "Not submitted"
            } else {
                "${sub.labJobName} [${sub.labStatus}]"
            }
            val verLabel = when (sub.verificationStatus.uppercase()) {
                AcademicSubmissionStatuses.FORWARDED_TO_PRINCIPAL -> "SENT TO PRINCIPAL"
                AcademicSubmissionStatuses.VERIFIED -> "VERIFIED"
                AcademicSubmissionStatuses.NOT_SUBMITTED -> "NOT SUBMITTED"
                else -> "PENDING"
            }
            val verColor = when (sub.verificationStatus.uppercase()) {
                AcademicSubmissionStatuses.FORWARDED_TO_PRINCIPAL -> colorGreen
                AcademicSubmissionStatuses.VERIFIED -> colorAccent
                AcademicSubmissionStatuses.NOT_SUBMITTED -> colorTextMuted
                else -> colorAmber
            }
            val verifiedTime = IstDateTimeFormatter.formatDateTime(sub.verifiedAt, "-")

            writer.drawTableRow(
                cells = listOf(
                    sub.roll.padStart(2, '0'),
                    sub.studentName,
                    theoryCell,
                    labCell,
                    verLabel,
                    verifiedTime
                ),
                widths = widths,
                isZebra = idx % 2 == 1,
                accentColumnIndex = 4,
                accentColor = verColor
            )
        }
        writer.addSpacing(14f)
    }

    private fun writeLeaveRequestsSection(writer: MultiPagePdfWriter, data: StaffDashboardData?) {
        val leaves = data?.pendingLeaves.orEmpty()
        writer.drawSectionHeader(
            title = "SECTION 3: LEAVE & OUT-PASS REQUESTS LOG",
            meta = "Active / Pending Requests: ${leaves.size}"
        )

        val headers = listOf("Req ID", "Roll", "Trainee Name", "Date (IST)", "Type / Window", "Reason")
        val widths = floatArrayOf(64f, 30f, 100f, 68f, 118f, 159f)
        writer.drawTableHeader(headers, widths)

        if (leaves.isEmpty()) {
            writer.drawEmptyRow("No pending leave or out-pass requests at this time.")
            return
        }

        leaves.forEachIndexed { idx, lv ->
            writer.drawTableRow(
                cells = listOf(
                    lv.reqId.ifBlank { "-" },
                    lv.roll.padStart(2, '0'),
                    lv.name,
                    IstDateTimeFormatter.formatDateOnly(lv.date, "-"),
                    "${lv.type} (${lv.timeWindow})",
                    lv.reason
                ),
                widths = widths,
                isZebra = idx % 2 == 1
            )
        }
        writer.addSpacing(14f)
    }

    private fun writeDutyAndShopFloorSection(writer: MultiPagePdfWriter, data: StaffDashboardData?) {
        val dutySignOff = data?.todayDutySignOff
        val todayDutySlot = DutyRotationEngine.generateRotationSchedule().firstOrNull { it.isToday }
            ?: DutyRotationEngine.generateRotationSchedule().firstOrNull()
        val shopFloor = data?.shopFloorList.orEmpty()

        val dutySummary = buildString {
            if (todayDutySlot != null) {
                append("Today Duty Pair: Roll ${todayDutySlot.rolls.joinToString(" & ")} (${todayDutySlot.dateLabel})")
            } else {
                append("Weekday Duty Rotation: Mon–Fri Cycle")
            }
            if (dutySignOff != null) {
                val signedTime = IstDateTimeFormatter.formatTimeOnly(dutySignOff.signedAt, "")
                append("  |  Sign-Off: ${if (dutySignOff.signedOff) "SIGNED ($signedTime)" else "PENDING"}")
            }
        }

        writer.drawSectionHeader(
            title = "SECTION 4: DUTY ROTATION & SHOP FLOOR ROSTER",
            meta = dutySummary
        )

        val headers = listOf("Roll", "Trainee Name", "Att %", "Session Type", "Status", "Card Out (IST)", "Hours")
        val widths = floatArrayOf(30f, 130f, 40f, 92f, 80f, 87f, 80f)
        writer.drawTableHeader(headers, widths)

        if (shopFloor.isEmpty()) {
            writer.drawEmptyRow("No trainees assigned to Shop Floor makeup sessions today.")
            return
        }

        shopFloor.forEachIndexed { idx, sf ->
            val cardOut = IstDateTimeFormatter.formatTimeOnly(sf.checkOutCardScanTime, "-")
            val hrs = AttendanceMath.formatShopFloorHours(sf.durationMinutes)
            val statusLabel = if (sf.verifiedByInstructor) "VERIFIED" else sf.status.uppercase()
            writer.drawTableRow(
                cells = listOf(
                    sf.roll.padStart(2, '0'),
                    sf.name,
                    "${sf.attendancePct}%",
                    if (sf.sessionType == "SATURDAY") "Saturday" else "Post-3PM (${sf.scheduledEndTime})",
                    statusLabel,
                    cardOut,
                    hrs
                ),
                widths = widths,
                isZebra = idx % 2 == 1
            )
        }
        writer.addSpacing(14f)
    }

    private fun writeNoticeBoardSection(writer: MultiPagePdfWriter, data: StaffDashboardData?) {
        val notices = data?.notices.orEmpty()
        writer.drawSectionHeader(
            title = "SECTION 5: PUBLISHED NOTICE BOARD ARCHIVE",
            meta = "Total Published Notices: ${notices.size}"
        )

        val headers = listOf("Date (IST)", "Category", "Title", "Message", "Author")
        val widths = floatArrayOf(82f, 68f, 118f, 195f, 76f)
        writer.drawTableHeader(headers, widths)

        if (notices.isEmpty()) {
            writer.drawEmptyRow("No published notices in archive.")
            return
        }

        notices.forEachIndexed { idx, n ->
            writer.drawTableRow(
                cells = listOf(
                    IstDateTimeFormatter.formatDateTime(n.date, "-"),
                    n.category,
                    if (n.isPinned) "[PINNED] ${n.title}" else n.title,
                    n.body,
                    n.postedBy
                ),
                widths = widths,
                isZebra = idx % 2 == 1
            )
        }
        writer.addSpacing(14f)
    }

    private fun writeParentAlertsSection(writer: MultiPagePdfWriter, data: StaffDashboardData?) {
        val parentAlerts = data?.parentAlerts.orEmpty()
        val runningLate = data?.runningLateAlerts.orEmpty()
        if (parentAlerts.isEmpty() && runningLate.isEmpty()) return

        writer.drawSectionHeader(
            title = "SECTION 6: PARENT ALERTS & RUNNING-LATE LOG",
            meta = "Parent Alerts: ${parentAlerts.size}  |  Running Late Alerts: ${runningLate.size}"
        )

        val headers = listOf("Timestamp (IST)", "Roll", "Trainee Name", "Alert Type", "Message / Details")
        val widths = floatArrayOf(96f, 30f, 98f, 94f, 221f)
        writer.drawTableHeader(headers, widths)

        var rowIndex = 0
        runningLate.forEach { rl ->
            val ts = IstDateTimeFormatter.combineDateAndTime(rl.date, rl.time, "-")
            val msg = "ETA: ${rl.expectedArrival} (${rl.reasonCategory})" +
                if (rl.details.isNotBlank()) " — ${rl.details}" else ""
            writer.drawTableRow(
                cells = listOf(ts, rl.roll.padStart(2, '0'), rl.studentName, "RUNNING LATE", msg),
                widths = widths,
                isZebra = rowIndex++ % 2 == 1,
                accentColumnIndex = 3,
                accentColor = colorAmber
            )
        }

        parentAlerts.forEach { pa ->
            val ts = IstDateTimeFormatter.combineDateAndTime(pa.date, pa.time, "-")
            writer.drawTableRow(
                cells = listOf(ts, pa.roll.padStart(2, '0'), pa.studentName, pa.alertType.replace("_", " "), pa.message),
                widths = widths,
                isZebra = rowIndex++ % 2 == 1
            )
        }
        writer.addSpacing(14f)
    }

    /**
     * Stateful paginated A4 canvas writer using Android PdfDocument.
     */
    private class MultiPagePdfWriter(
        private val document: PdfDocument,
        private val reportDate: String,
        private val generatedAtIst: String,
        private val generatedByRole: String
    ) {
        private val finishedPages = mutableListOf<PdfDocument.Page>()
        private var currentPage: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var pageNumber = 0
        private var cursorY = MARGIN_TOP

        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 13.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        private val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorNavy
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        private val sectionMetaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextMuted
            textSize = 7.8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        private val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorNavy
            textSize = 7.6f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextDark
            textSize = 7.4f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = colorBorder
            strokeWidth = 0.6f
        }
        private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextMuted
            textSize = 7.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        fun startFirstPage() {
            startNewPage(isCoverHeader = true)
        }

        private fun startNewPage(isCoverHeader: Boolean = false) {
            currentPage?.let {
                drawPageFooter(it.canvas, pageNumber)
                document.finishPage(it)
            }
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(info)
            currentPage = page
            canvas = page.canvas
            cursorY = MARGIN_TOP

            if (isCoverHeader) {
                drawCoverBanner(page.canvas)
            } else {
                drawContinuationHeader(page.canvas)
            }
        }

        private fun drawCoverBanner(c: Canvas) {
            val bannerHeight = 58f
            fillPaint.color = colorNavy
            c.drawRoundRect(
                RectF(MARGIN_LEFT, cursorY, PAGE_WIDTH - MARGIN_RIGHT, cursorY + bannerHeight),
                8f,
                8f,
                fillPaint
            )
            // Accent stripe
            fillPaint.color = colorAccent
            c.drawRect(MARGIN_LEFT, cursorY + bannerHeight - 4f, PAGE_WIDTH - MARGIN_RIGHT, cursorY + bannerHeight, fillPaint)

            c.drawText(
                "GOVT. ITI RANNI — ELECTRONICS MECHANIC (2025–2027)",
                MARGIN_LEFT + 12f,
                cursorY + 21f,
                titlePaint
            )
            c.drawText(
                "Official Consolidated Register & Academic Verification Report",
                MARGIN_LEFT + 12f,
                cursorY + 35f,
                subtitlePaint
            )
            c.drawText(
                "Report Date: $reportDate   |   Generated: $generatedAtIst IST   |   Role: $generatedByRole",
                MARGIN_LEFT + 12f,
                cursorY + 48f,
                subtitlePaint
            )
            cursorY += bannerHeight + 14f
        }

        private fun drawContinuationHeader(c: Canvas) {
            c.drawText(
                "Govt. ITI Ranni — Electronics Mechanic (2025–2027) • Official Report ($reportDate)",
                MARGIN_LEFT,
                cursorY + 8f,
                sectionMetaPaint
            )
            c.drawLine(MARGIN_LEFT, cursorY + 13f, PAGE_WIDTH - MARGIN_RIGHT, cursorY + 13f, strokePaint)
            cursorY += 22f
        }

        private fun drawPageFooter(c: Canvas, pageNum: Int) {
            val y = PAGE_HEIGHT - 22f
            c.drawLine(MARGIN_LEFT, y - 10f, PAGE_WIDTH - MARGIN_RIGHT, y - 10f, strokePaint)
            c.drawText(
                "Govt. ITI Ranni (EM) • Generated $generatedAtIst IST",
                MARGIN_LEFT,
                y,
                footerPaint
            )
            val pageText = "Page $pageNum"
            val textWidth = footerPaint.measureText(pageText)
            c.drawText(pageText, PAGE_WIDTH - MARGIN_RIGHT - textWidth, y, footerPaint)
        }

        fun ensureSpace(neededHeight: Float) {
            if (cursorY + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                startNewPage(isCoverHeader = false)
            }
        }

        fun addSpacing(height: Float) {
            cursorY += height
        }

        fun drawSectionHeader(title: String, meta: String) {
            ensureSpace(34f)
            val c = canvas ?: return
            fillPaint.color = Color.rgb(241, 245, 249)
            c.drawRect(MARGIN_LEFT, cursorY, PAGE_WIDTH - MARGIN_RIGHT, cursorY + 26f, fillPaint)
            fillPaint.color = colorAccent
            c.drawRect(MARGIN_LEFT, cursorY, MARGIN_LEFT + 3.5f, cursorY + 26f, fillPaint)
            c.drawText(title, MARGIN_LEFT + 9f, cursorY + 11.5f, sectionTitlePaint)
            c.drawText(truncateText(meta, sectionMetaPaint, CONTENT_WIDTH - 14f), MARGIN_LEFT + 9f, cursorY + 22f, sectionMetaPaint)
            cursorY += 30f
        }

        fun drawTableHeader(headers: List<String>, widths: FloatArray) {
            ensureSpace(20f)
            val c = canvas ?: return
            val rowH = 18f
            fillPaint.color = colorHeaderBg
            c.drawRect(MARGIN_LEFT, cursorY, PAGE_WIDTH - MARGIN_RIGHT, cursorY + rowH, fillPaint)
            c.drawRect(MARGIN_LEFT, cursorY, PAGE_WIDTH - MARGIN_RIGHT, cursorY + rowH, strokePaint)

            var x = MARGIN_LEFT
            headers.forEachIndexed { idx, label ->
                val colW = widths.getOrElse(idx) { 60f }
                c.drawText(truncateText(label, tableHeaderPaint, colW - 6f), x + 3f, cursorY + 12f, tableHeaderPaint)
                if (idx > 0) {
                    c.drawLine(x, cursorY, x, cursorY + rowH, strokePaint)
                }
                x += colW
            }
            cursorY += rowH
        }

        fun drawEmptyRow(message: String) {
            ensureSpace(22f)
            val c = canvas ?: return
            val rowH = 20f
            c.drawRect(MARGIN_LEFT, cursorY, PAGE_WIDTH - MARGIN_RIGHT, cursorY + rowH, strokePaint)
            c.drawText(message, MARGIN_LEFT + 6f, cursorY + 13f, sectionMetaPaint)
            cursorY += rowH + 10f
        }

        fun drawTableRow(
            cells: List<String>,
            widths: FloatArray,
            isZebra: Boolean,
            accentColumnIndex: Int = -1,
            accentColor: Int = colorTextDark
        ) {
            // Wrap text up to 3 lines per cell so long topics/notices never clip awkwardly
            val wrappedCells = cells.mapIndexed { idx, text ->
                val colW = (widths.getOrElse(idx) { 60f } - 6f).coerceAtLeast(16f)
                wrapText(text.ifBlank { "-" }, cellPaint, colW, maxLines = 3)
            }
            val maxLines = wrappedCells.maxOfOrNull { it.size } ?: 1
            val lineHeight = 9.8f
            val rowH = (maxLines * lineHeight) + 7f

            ensureSpace(rowH + 4f)
            val c = canvas ?: return

            if (isZebra) {
                fillPaint.color = colorZebra
                c.drawRect(MARGIN_LEFT, cursorY, PAGE_WIDTH - MARGIN_RIGHT, cursorY + rowH, fillPaint)
            }
            c.drawRect(MARGIN_LEFT, cursorY, PAGE_WIDTH - MARGIN_RIGHT, cursorY + rowH, strokePaint)

            var x = MARGIN_LEFT
            wrappedCells.forEachIndexed { idx, lines ->
                val colW = widths.getOrElse(idx) { 60f }
                if (idx > 0) {
                    c.drawLine(x, cursorY, x, cursorY + rowH, strokePaint)
                }
                val paintToUse = if (idx == accentColumnIndex) {
                    Paint(cellPaint).apply {
                        color = accentColor
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    }
                } else {
                    cellPaint
                }
                var textY = cursorY + 10.5f
                lines.forEach { line ->
                    c.drawText(line, x + 3f, textY, paintToUse)
                    textY += lineHeight
                }
                x += colW
            }
            cursorY += rowH
        }

        fun drawSignatureBlock() {
            ensureSpace(84f)
            val c = canvas ?: return
            cursorY += 18f
            val leftX = MARGIN_LEFT + 16f
            val rightX = PAGE_WIDTH - MARGIN_RIGHT - 180f
            val lineY = cursorY + 34f

            c.drawLine(leftX, lineY, leftX + 164f, lineY, strokePaint)
            c.drawLine(rightX, lineY, rightX + 164f, lineY, strokePaint)

            c.drawText("Trade Instructor (Electronics Mechanic)", leftX, lineY + 12f, sectionTitlePaint)
            c.drawText("Govt. ITI Ranni • Date: $reportDate", leftX, lineY + 23f, sectionMetaPaint)

            c.drawText("Principal / Group Instructor", rightX, lineY + 12f, sectionTitlePaint)
            c.drawText("Govt. ITI Ranni • Official Seal", rightX, lineY + 23f, sectionMetaPaint)
            cursorY = lineY + 32f
        }

        fun finishAllPages() {
            currentPage?.let {
                drawPageFooter(it.canvas, pageNumber)
                document.finishPage(it)
            }
            currentPage = null
            canvas = null
        }

        private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
            if (paint.measureText(text) <= maxWidth) return text
            var end = text.length
            while (end > 1 && paint.measureText(text.substring(0, end) + "…") > maxWidth) {
                end--
            }
            return text.substring(0, end) + "…"
        }

        private fun wrapText(text: String, paint: Paint, maxWidth: Float, maxLines: Int): List<String> {
            val clean = text.replace("\n", " ").trim()
            if (clean.isEmpty()) return listOf("-")
            if (paint.measureText(clean) <= maxWidth) return listOf(clean)

            val words = clean.split(Regex("\\s+"))
            val lines = mutableListOf<String>()
            var current = StringBuilder()

            for (word in words) {
                val candidate = if (current.isEmpty()) word else "${current} $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    current.clear()
                    current.append(candidate)
                } else {
                    if (current.isNotEmpty()) {
                        lines.add(current.toString())
                        if (lines.size >= maxLines) break
                        current = StringBuilder(truncateText(word, paint, maxWidth))
                    } else {
                        lines.add(truncateText(word, paint, maxWidth))
                        if (lines.size >= maxLines) break
                    }
                }
            }
            if (lines.size < maxLines && current.isNotEmpty()) {
                lines.add(current.toString())
            }
            if (lines.size == maxLines && words.size > 1) {
                lines[maxLines - 1] = truncateText(lines[maxLines - 1], paint, maxWidth)
            }
            return lines.ifEmpty { listOf("-") }
        }
    }
}

/**
 * Interactive dialog allowing Trade Instructor or Principal to export either
 * the Complete All-in-One 6-Section PDF Report OR individually chosen sections.
 */
@Composable
fun FullReportExportDialog(
    staffData: StaffDashboardData?,
    roleLabel: String,
    onDismiss: () -> Unit
) {
    val colors = LocalItiColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedSections by remember { mutableStateOf(PdfReportSection.entries.toSet()) }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val allSelected = selectedSections.size == PdfReportSection.entries.size

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        containerColor = colors.cardSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PictureAsPdf,
                    contentDescription = null,
                    tint = colors.skyAccent,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text(
                        text = "Export Full PDF Report",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Multi-page A4 report with IST times & signature lines",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Report Sections",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = {
                            selectedSections = if (allSelected) {
                                setOf(PdfReportSection.ATTENDANCE)
                            } else {
                                PdfReportSection.entries.toSet()
                            }
                        }
                    ) {
                        Text(
                            text = if (allSelected) "Select 1" else "Select All 6",
                            color = colors.skyAccent,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                PdfReportSection.entries.forEach { section ->
                    val checked = section in selectedSections
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (checked) colors.skyContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.background,
                        border = BorderStroke(
                            1.dp,
                            if (checked) colors.skyAccent.copy(alpha = 0.55f) else colors.borderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isGenerating) {
                                selectedSections = if (checked && selectedSections.size > 1) {
                                    selectedSections - section
                                } else {
                                    selectedSections + section
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    if (!isGenerating) {
                                        selectedSections = if (isChecked) {
                                            selectedSections + section
                                        } else if (selectedSections.size > 1) {
                                            selectedSections - section
                                        } else {
                                            selectedSections
                                        }
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colors.skyAccent,
                                    uncheckedColor = colors.textMuted
                                )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = section.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.redAccent
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (isGenerating) return@OutlinedButton
                        scope.launch {
                            isGenerating = true
                            errorMessage = null
                            try {
                                val file = FullReportPdfGenerator.generateReportPdf(
                                    context = context,
                                    staffData = staffData,
                                    sections = selectedSections,
                                    generatedByRole = roleLabel
                                )
                                isGenerating = false
                                onDismiss()
                                FullReportPdfGenerator.launchShareOrViewChooser(context, file, viewDirectly = true)
                            } catch (e: Exception) {
                                isGenerating = false
                                errorMessage = "Failed to generate PDF: ${e.message ?: "Unknown error"}"
                            }
                        }
                    },
                    enabled = !isGenerating,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, colors.skyAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.skyAccent),
                    modifier = Modifier.testTag("pdf_dialog_view_button")
                ) {
                    Text("View / Print")
                }

                Button(
                    onClick = {
                        if (isGenerating) return@Button
                        scope.launch {
                            isGenerating = true
                            errorMessage = null
                            try {
                                val file = FullReportPdfGenerator.generateReportPdf(
                                    context = context,
                                    staffData = staffData,
                                    sections = selectedSections,
                                    generatedByRole = roleLabel
                                )
                                isGenerating = false
                                onDismiss()
                                FullReportPdfGenerator.launchShareOrViewChooser(context, file, viewDirectly = false)
                            } catch (e: Exception) {
                                isGenerating = false
                                errorMessage = "Failed to generate PDF: ${e.message ?: "Unknown error"}"
                            }
                        }
                    },
                    enabled = !isGenerating,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.skyAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("pdf_dialog_share_button")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Building...")
                    } else {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share / Save")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isGenerating) {
                Text("Cancel", color = colors.textMuted)
            }
        }
    )
}
