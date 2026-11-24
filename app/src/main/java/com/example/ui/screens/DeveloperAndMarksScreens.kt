package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceApiClient
import com.example.data.DutyRotationEngine
import com.example.data.WorkMarkQuery
import com.example.pdf.PdfReportGenerator
import com.example.ui.components.DigitalSignaturePad
import com.example.ui.components.DigitalSignaturePreview
import com.example.ui.components.InlineAlertCard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

@Composable
fun ReportBugButtonAndDialog(
    screenName: String,
    reportedBy: String,
    role: String,
    appVersion: String,
    visible: Boolean = true,
    onSubmitBug: (screenName: String, reportedBy: String, role: String, appVersion: String, description: String) -> Unit
) {
    if (!visible) return
    var showDialog by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    OutlinedButton(
        onClick = { showDialog = true },
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.testTag("report_bug_button")
    ) {
        Text("Report Bug", style = MaterialTheme.typography.labelMedium)
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Report Bug ($screenName)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Screen: $screenName • Role: $role • Build: $appVersion",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Describe what went wrong") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bug_description_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = description.isNotBlank(),
                    onClick = {
                        onSubmitBug(screenName, reportedBy, role, appVersion, description.trim())
                        description = ""
                        showDialog = false
                    },
                    modifier = Modifier.testTag("bug_submit_button")
                ) { Text("Send to Developer") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun DeveloperMasterPanel(
    appConfig: RemoteAppConfig,
    uiConfig: RemoteUiConfig,
    bugList: List<Map<String, String>>,
    onSaveAppConfig: (Map<String, String>) -> Unit,
    onSaveUiConfig: (Map<String, String>, String) -> Unit,
    onResolveBug: (bugId: String, reply: String) -> Unit,
    onEditAnySheetRow: (tabName: String, rowIndex: Int, colName: String, newValue: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Kill-Switches", "Live UI Editor", "Bug Inbox (${bugList.size})", "Sheet Editor")

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Developer Master Console", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        ScrollableTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, title ->
                Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title) })
            }
        }
        Spacer(Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                var updater by remember(appConfig) { mutableStateOf(appConfig.updaterEnabled) }
                var releaseToUsers by remember(appConfig) { mutableStateOf(appConfig.releaseUpdateToUsers) }
                var instAccess by remember(appConfig) { mutableStateOf(appConfig.instructorAccessEnabled) }
                var prinAccess by remember(appConfig) { mutableStateOf(appConfig.principalAccessEnabled) }
                var maintMode by remember(appConfig) { mutableStateOf(appConfig.maintenanceMode) }
                var banner by remember(appConfig) { mutableStateOf(appConfig.maintenanceBanner) }
                var newDevPin by remember(appConfig) { mutableStateOf(appConfig.developerPin) }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        SwitchRow("Release Update Popup to Users", releaseToUsers) { releaseToUsers = it }
                        SwitchRow("In-App Updater Enabled", updater) { updater = it }
                        SwitchRow("Instructor (2025) Admin Access", instAccess) { instAccess = it }
                        SwitchRow("Principal (9999) Admin Access", prinAccess) { prinAccess = it }
                        SwitchRow("Full Maintenance Lock Mode", maintMode) { maintMode = it }
                    }
                    item {
                        OutlinedTextField(
                            value = banner,
                            onValueChange = { banner = it },
                            label = { Text("Custom Red Maintenance Banner Message") },
                            placeholder = { Text("Enter custom word or notice shown on center red card") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dev_maintenance_banner_input")
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = newDevPin,
                            onValueChange = { newDevPin = it },
                            label = { Text("Developer Secret PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dev_pin_input")
                        )
                    }
                    item {
                        Button(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dev_save_config_button"),
                            onClick = {
                                onSaveAppConfig(
                                    mapOf(
                                        "updaterEnabled" to updater.toString(),
                                        "releaseUpdateToUsers" to releaseToUsers.toString(),
                                        "instructorAccessEnabled" to instAccess.toString(),
                                        "principalAccessEnabled" to prinAccess.toString(),
                                        "maintenanceMode" to maintMode.toString(),
                                        "maintenanceBanner" to banner,
                                        "developerPin" to newDevPin
                                    )
                                )
                            }
                        ) { Text("Push Kill-Switch & Config Live") }
                    }
                }
            }

            1 -> {
                var title by remember(uiConfig) { mutableStateOf(uiConfig.appTitle) }
                var traineeTab by remember(uiConfig) { mutableStateOf(uiConfig.traineeTabTitle) }
                var staffTab by remember(uiConfig) { mutableStateOf(uiConfig.staffTabTitle) }
                var marksTab by remember(uiConfig) { mutableStateOf(uiConfig.examMarksTabTitle) }
                var shopTab by remember(uiConfig) { mutableStateOf(uiConfig.shopFloorTabTitle) }
                var annBanner by remember(uiConfig) { mutableStateOf(uiConfig.announcementBanner) }
                var showBugBtn by remember(uiConfig) { mutableStateOf(uiConfig.showReportBugButton) }
                var showUndoBtn by remember(uiConfig) { mutableStateOf(uiConfig.showUndoButton) }
                var customKey by remember { mutableStateOf("") }
                var customVal by remember { mutableStateOf("") }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("App Header Title") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = traineeTab, onValueChange = { traineeTab = it }, label = { Text("Trainee Portal Label") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = staffTab, onValueChange = { staffTab = it }, label = { Text("Staff Console Label") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = marksTab, onValueChange = { marksTab = it }, label = { Text("Exam Marks Tab Label") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = shopTab, onValueChange = { shopTab = it }, label = { Text("Shop Floor Tab Label") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = annBanner, onValueChange = { annBanner = it }, label = { Text("Live Global Banner Text") }, modifier = Modifier.fillMaxWidth())
                        SwitchRow("Show Report Bug Button on Panels", showBugBtn) { showBugBtn = it }
                        SwitchRow("Show Undo Button on Actions", showUndoBtn) { showUndoBtn = it }
                    }
                    item {
                        Text("Custom UI Key / Override", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = customKey, onValueChange = { customKey = it }, label = { Text("UI Key") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = customVal, onValueChange = { customVal = it }, label = { Text("Value") }, modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val updates = mutableMapOf(
                                    "appTitle" to title,
                                    "traineeTabTitle" to traineeTab,
                                    "staffTabTitle" to staffTab,
                                    "examMarksTabTitle" to marksTab,
                                    "shopFloorTabTitle" to shopTab,
                                    "announcementBanner" to annBanner,
                                    "showReportBugButton" to showBugBtn.toString(),
                                    "showUndoButton" to showUndoBtn.toString()
                                )
                                if (customKey.isNotBlank()) updates[customKey.trim()] = customVal
                                onSaveUiConfig(updates, "Global")
                            }
                        ) { Text("Save & Push Live UI Changes") }
                    }
                }
            }

            2 -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(bugList) { _, bug ->
                        var reply by remember { mutableStateOf(bug["DevReply"] ?: "") }
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${bug["BugId"]} • ${bug["ScreenName"]} (${bug["Status"]})", fontWeight = FontWeight.Bold)
                                Text("From: ${bug["ReportedBy"]} (${bug["Role"]}) • Build ${bug["AppVersion"]}", style = MaterialTheme.typography.bodySmall)
                                Text(bug["Description"] ?: "")
                                OutlinedTextField(
                                    value = reply,
                                    onValueChange = { reply = it },
                                    label = { Text("Developer Reply") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(onClick = { onResolveBug(bug["BugId"] ?: "", reply) }) {
                                    Text("Mark Fixed & Save Reply")
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                var targetTab by remember { mutableStateOf("Notices") }
                var rowNum by remember { mutableStateOf("2") }
                var colName by remember { mutableStateOf("Status") }
                var cellVal by remember { mutableStateOf("") }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Direct Live Sheet Cell Override", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = targetTab, onValueChange = { targetTab = it }, label = { Text("Sheet Tab (e.g. Leave_Requests, Shop_Floor, Notices)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = rowNum, onValueChange = { rowNum = it }, label = { Text("Row Number (2+)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = colName, onValueChange = { colName = it }, label = { Text("Column Header Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = cellVal, onValueChange = { cellVal = it }, label = { Text("New Value") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            rowNum.toIntOrNull()?.let { r -> onEditAnySheetRow(targetTab.trim(), r, colName.trim(), cellVal) }
                        }
                    ) { Text("Update Sheet Cell Immediately") }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * MAJOR FEATURE: Full-class Roll #01–#23 Practical & Theory Marks Register Table
 * with inline mark editing, live total & percentage calculation, finger-drawn
 * Digital Signature Pad, Google Sheets batch publish, PDF export, and Query Review.
 */
@Composable
fun InstructorMarksRegisterTablePanel(
    uiState: AppUiState,
    onSaveDraftOffline: (monthLabel: String, rows: List<ExamMarkRow>) -> Unit,
    onPublishSignedMarksTable: (
        monthLabel: String,
        examDate: String,
        theoryMax: Int,
        labMax: Int,
        rows: List<ExamMarkRow>,
        signedBy: String,
        signatureData: String
    ) -> Unit,
    onDismissFeedback: () -> Unit,
    onReplyWorkMarkQuery: (queryId: String, status: String, reply: String, onReset: () -> Unit) -> Unit,
    onDismissWorkQueryReplyFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val context = LocalContext.current
    val staffData = uiState.staffData

    var monthLabel by remember { mutableStateOf("Nov 2026 Monthly Evaluation") }
    var examDate by remember { mutableStateOf(AttendanceApiClient.todayIstDateString()) }
    var theoryMaxStr by remember { mutableStateOf("50") }
    var labMaxStr by remember { mutableStateOf("100") }
    var signedBy by remember { mutableStateOf("Trade Instructor (EM)") }
    var signatureData by remember { mutableStateOf("") }
    var sigResetKey by remember { mutableIntStateOf(0) }
    var localPdfFeedback by remember { mutableStateOf<String?>(null) }
    var validationWarning by remember { mutableStateOf<String?>(null) }

    val theoryMax = (theoryMaxStr.toIntOrNull() ?: 50).coerceIn(1, 500)
    val labMax = (labMaxStr.toIntOrNull() ?: 100).coerceIn(1, 500)

    val attendanceByRoll = remember(staffData?.allAttendance) {
        staffData?.allAttendance.orEmpty().associateBy { it.roll.toIntOrNull() ?: -1 }
    }
    val existingMarksByRoll = remember(staffData?.monthlyMarks) {
        staffData?.monthlyMarks.orEmpty().associateBy { it.roll.toIntOrNull() ?: -1 }
    }

    val rows = remember(staffData?.allAttendance, staffData?.monthlyMarks) {
        mutableStateListOf<ExamMarkRow>().apply {
            for (roll in 1..DutyRotationEngine.TOTAL_TRAINEES) {
                val att = attendanceByRoll[roll]
                val mark = existingMarksByRoll[roll]
                val resolvedName = att?.name?.takeIf { it.isNotBlank() }
                    ?: mark?.studentName?.takeIf { it.isNotBlank() }
                    ?: "Trainee #${roll.toString().padStart(2, '0')}"
                add(
                    ExamMarkRow(
                        roll = roll,
                        name = resolvedName,
                        theoryMarks = mark?.theoryMarks?.toString().orEmpty(),
                        labMarks = mark?.labMarks?.toString().orEmpty(),
                        remarks = mark?.remarks.orEmpty(),
                        published = mark?.published ?: false
                    )
                )
            }
        }
    }

    val enteredCount = rows.count { it.theoryMarks.isNotBlank() || it.labMarks.isNotBlank() }
    val lastSignedMark = staffData?.monthlyMarks?.firstOrNull { it.signedBy.isNotBlank() || it.signatureData.isNotBlank() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("instructor_marks_register_table"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.skyAccent.copy(alpha = 0.45f)),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Class Marks Register (Roll #01–#23)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Enter Theory & Practical marks for every trainee, digitally sign & publish",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.emeraldContainer,
                            border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "$enteredCount/23 Entered",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.emeraldAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = monthLabel,
                        onValueChange = { monthLabel = it },
                        label = { Text("Exam / Month Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.skyAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("marks_table_month_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = examDate,
                            onValueChange = { examDate = it },
                            label = { Text("Exam Date") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = theoryMaxStr,
                            onValueChange = { theoryMaxStr = it.filter { ch -> ch.isDigit() }.take(3) },
                            label = { Text("Theory Max") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = labMaxStr,
                            onValueChange = { labMaxStr = it.filter { ch -> ch.isDigit() }.take(3) },
                            label = { Text("Practical Max") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Table Header Row
        item {
            Surface(
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                color = colors.skyContainer,
                border = BorderStroke(1.dp, colors.skyAccent.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Roll",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                        color = colors.skyAccent,
                        modifier = Modifier.width(34.dp)
                    )
                    Text(
                        text = "Trainee Name",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.skyAccent,
                        modifier = Modifier.weight(1.45f)
                    )
                    Text(
                        text = "Theory /$theoryMax",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                        color = colors.skyAccent,
                        modifier = Modifier.weight(0.95f)
                    )
                    Text(
                        text = "Pract /$labMax",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                        color = colors.skyAccent,
                        modifier = Modifier.weight(0.95f)
                    )
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
                        color = colors.skyAccent,
                        modifier = Modifier.width(56.dp)
                    )
                }
            }
        }

        // 23-Trainee Table Rows
        itemsIndexed(rows, key = { _, row -> "mark_row_${row.roll}" }) { index, item ->
            val totalVal = item.total(theoryMax, labMax)
            val totalMax = theoryMax + labMax
            val pct = ((totalVal * 100f) / totalMax.coerceAtLeast(1)).toInt().coerceIn(0, 100)
            val pctColor = when {
                item.theoryMarks.isBlank() && item.labMarks.isBlank() -> colors.textMuted
                pct >= 60 -> colors.emeraldAccent
                pct >= 40 -> colors.amberAccent
                else -> colors.redAccent
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (index % 2 == 0) colors.cardSurface else colors.cardElevated.copy(alpha = 0.65f),
                border = BorderStroke(
                    1.dp,
                    if (item.published) colors.emeraldAccent.copy(alpha = 0.45f) else colors.borderSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("marks_table_row_${item.roll}")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.skyContainer,
                            modifier = Modifier.width(34.dp)
                        ) {
                            Text(
                                text = item.roll.toString().padStart(2, '0'),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.skyAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1.45f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.published) {
                                Text(
                                    text = "Published",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = colors.emeraldAccent
                                )
                            }
                        }

                        OutlinedTextField(
                            value = item.theoryMarks,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }.take(3)
                                val clamped = digits.toIntOrNull()?.coerceAtMost(theoryMax)?.toString().orEmpty()
                                rows[index] = item.copy(theoryMarks = clamped)
                            },
                            placeholder = { Text("0-$theoryMax", style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .weight(0.95f)
                                .testTag("marks_theory_input_${item.roll}")
                        )

                        OutlinedTextField(
                            value = item.labMarks,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }.take(3)
                                val clamped = digits.toIntOrNull()?.coerceAtMost(labMax)?.toString().orEmpty()
                                rows[index] = item.copy(labMarks = clamped)
                            },
                            placeholder = { Text("0-$labMax", style = MaterialTheme.typography.labelSmall) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .weight(0.95f)
                                .testTag("marks_lab_input_${item.roll}")
                        )

                        Column(
                            modifier = Modifier.width(56.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "$totalVal/$totalMax",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = pctColor
                            )
                            Text(
                                text = "$pct%",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = pctColor
                            )
                        }
                    }

                    OutlinedTextField(
                        value = item.remarks,
                        onValueChange = { rows[index] = item.copy(remarks = it) },
                        placeholder = { Text("Optional remark (e.g., Good practical soldering / Absent)", style = MaterialTheme.typography.labelSmall) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("marks_remark_input_${item.roll}")
                    )
                }
            }
        }

        // Instructor Digital Signature Pad & Publish Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.5.dp, colors.emeraldAccent.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = colors.emeraldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Instructor Digital Signature & Authentication",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = signedBy,
                        onValueChange = { signedBy = it },
                        label = { Text("Instructor Signatory Title / Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("marks_signed_by_input")
                    )

                    DigitalSignaturePad(
                        defaultSignerName = signedBy,
                        resetKey = sigResetKey,
                        onSignatureChanged = { signatureData = it },
                        title = "Sign Register Sheet Below",
                        tagPrefix = "instructor_marks_sig"
                    )

                    if (lastSignedMark != null && signatureData.isBlank()) {
                        DigitalSignaturePreview(
                            signedBy = lastSignedMark.signedBy,
                            signedAt = lastSignedMark.signedAt,
                            signatureData = lastSignedMark.signatureData
                        )
                    }

                    AnimatedVisibility(visible = validationWarning != null) {
                        InlineAlertCard(message = validationWarning.orEmpty(), isError = true)
                    }
                    AnimatedVisibility(visible = uiState.monthlyMarkMessage != null) {
                        InlineAlertCard(message = uiState.monthlyMarkMessage.orEmpty(), isError = false)
                    }
                    AnimatedVisibility(visible = uiState.monthlyMarkError != null) {
                        InlineAlertCard(message = uiState.monthlyMarkError.orEmpty(), isError = true)
                    }
                    AnimatedVisibility(visible = localPdfFeedback != null) {
                        InlineAlertCard(message = localPdfFeedback.orEmpty(), isError = false)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSaveDraftOffline(monthLabel, rows.toList()) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("marks_save_draft_button")
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Save Draft", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = {
                                val res = PdfReportGenerator.generateAndShareMarksPdf(
                                    context = context,
                                    monthLabel = monthLabel,
                                    examDate = examDate,
                                    theoryMax = theoryMax,
                                    labMax = labMax,
                                    rows = rows.toList(),
                                    signedBy = signedBy,
                                    signatureData = signatureData.ifBlank { lastSignedMark?.signatureData.orEmpty() }
                                )
                                localPdfFeedback = res.fold(
                                    onSuccess = { "Signed Marks Register PDF exported." },
                                    onFailure = { it.message ?: "PDF export failed." }
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.skyAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.skyAccent),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("marks_export_pdf_button")
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Signed PDF", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Button(
                        onClick = {
                            onDismissFeedback()
                            val filledRows = rows.filter { it.theoryMarks.isNotBlank() || it.labMarks.isNotBlank() }
                            if (filledRows.isEmpty()) {
                                validationWarning = "Enter Theory or Practical marks for at least one trainee first."
                                return@Button
                            }
                            if (signatureData.isBlank()) {
                                validationWarning = "Please draw your digital signature above before signing & publishing."
                                return@Button
                            }
                            validationWarning = null
                            onPublishSignedMarksTable(
                                monthLabel.trim(),
                                examDate.trim(),
                                theoryMax,
                                labMax,
                                filledRows,
                                signedBy.trim().ifBlank { "Trade Instructor (EM)" },
                                signatureData
                            )
                        },
                        enabled = !uiState.isSavingMonthlyMark,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.emeraldAccent,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("marks_sign_and_publish_all_button")
                    ) {
                        if (uiState.isSavingMonthlyMark) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onSecondary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Signing & Publishing Marks (${enteredCount})...")
                        } else {
                            Icon(Icons.Rounded.Publish, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Digitally Sign & Publish All ($enteredCount Entered)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Work / Lab Mark Queries Review Section
        val queries = staffData?.workMarkQueries.orEmpty()
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.QuestionAnswer, contentDescription = null, tint = colors.amberAccent, modifier = Modifier.size(18.dp))
                Text(
                    text = "Trainee Work / Lab Mark Queries (${queries.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (uiState.workQueryReplyMessage != null) {
            item { InlineAlertCard(message = uiState.workQueryReplyMessage, isError = false) }
        }
        if (uiState.workQueryReplyError != null) {
            item { InlineAlertCard(message = uiState.workQueryReplyError, isError = true) }
        }

        if (queries.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No open work/practical mark queries from trainees.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(queries, key = { "wmq_${it.id}_${it.roll}" }) { query ->
                WorkMarkQueryReviewCard(
                    query = query,
                    isReplying = uiState.replyingWorkQueryId == query.id,
                    onReply = { status, replyText, reset ->
                        onDismissWorkQueryReplyFeedback()
                        onReplyWorkMarkQuery(query.id, status, replyText, reset)
                    }
                )
            }
        }
    }
}

@Composable
private fun WorkMarkQueryReviewCard(
    query: WorkMarkQuery,
    isReplying: Boolean,
    onReply: (status: String, replyText: String, onReset: () -> Unit) -> Unit
) {
    val colors = LocalItiColors.current
    var replyInput by remember(query.id) { mutableStateOf(query.instructorReply) }
    val statusColor = when (query.status.uppercase()) {
        "REVISED" -> colors.emeraldAccent
        "RESOLVED" -> colors.skyAccent
        else -> colors.amberAccent
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Roll #${query.roll} • ${query.studentName}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = query.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text = "${query.workReference}${if (query.monthLabel.isNotBlank()) " (${query.monthLabel})" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.skyAccent
            )
            if (query.traineeNote.isNotBlank()) {
                Text(
                    text = "Trainee Note: ${query.traineeNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            OutlinedTextField(
                value = replyInput,
                onValueChange = { replyInput = it },
                label = { Text("Instructor Reply / Revision Note") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onReply("RESOLVED", replyInput) {} },
                    enabled = !isReplying && replyInput.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Mark Checked", style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = { onReply("REVISED", replyInput) {} },
                    enabled = !isReplying && replyInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.emeraldAccent, contentColor = MaterialTheme.colorScheme.onSecondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.FactCheck, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Mark Revised", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
