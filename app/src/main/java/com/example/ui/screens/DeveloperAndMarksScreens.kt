package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

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
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
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
                        modifier = Modifier.fillMaxWidth()
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
                    }
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
    bugList: List>,
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
                var instAccess by remember(appConfig) { mutableStateOf(appConfig.instructorAccessEnabled) }
                var prinAccess by remember(appConfig) { mutableStateOf(appConfig.principalAccessEnabled) }
                var maintMode by remember(appConfig) { mutableStateOf(appConfig.maintenanceMode) }
                var banner by remember(appConfig) { mutableStateOf(appConfig.maintenanceBanner) }
                var newDevPin by remember(appConfig) { mutableStateOf(appConfig.developerPin) }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        SwitchRow("In-App Updater Enabled", updater) { updater = it }
                        SwitchRow("Instructor (2025) Admin Access", instAccess) { instAccess = it }
                        SwitchRow("Principal (9999) Admin Access", prinAccess) { prinAccess = it }
                        SwitchRow("Full Maintenance Lock Mode", maintMode) { maintMode = it }
                    }
                    item {
                        OutlinedTextField(
                            value = banner,
                            onValueChange = { banner = it },
                            label = { Text("Lock / Maintenance Banner Message") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = newDevPin,
                            onValueChange = { newDevPin = it },
                            label = { Text("Developer PIN") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onSaveAppConfig(
                                    mapOf(
                                        "updaterEnabled" to updater.toString(),
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

@Composable
fun InstructorExamMarksPanel(
    initialRows: List<ExamMarkRow>,
    onSaveDraftOffline: (monthLabel: String, rows: List<ExamMarkRow>) -> Unit,
    onPublishMarks: (monthLabel: String, rows: List<ExamMarkRow>) -> Unit,
    onExportMarksPdf: (monthLabel: String, rows: List<ExamMarkRow>) -> Unit
) {
    var monthLabel by remember { mutableStateOf("Nov 2026 Monthly Exam") }
    val rows = remember(initialRows) {
        mutableStateListOf<ExamMarkRow>().apply { addAll(initialRows.sortedBy { it.roll }) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Exam Marks Entry (Roll Order)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = monthLabel,
            onValueChange = { monthLabel = it },
            label = { Text("Exam / Month Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Roll & Trainee", modifier = Modifier.weight(1.6f), fontWeight = FontWeight.Bold)
            Text("Theory /50", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Lab /100", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Total", modifier = Modifier.weight(0.7f), fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(rows, key = { _, item -> item.roll }) { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "${item.roll}. ${item.name}",
                        modifier = Modifier.weight(1.6f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = item.theoryMarks,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() }.take(2)
                            val clamped = (clean.toIntOrNull()?.coerceAtMost(50)?.toString()) ?: ""
                            rows[index] = item.copy(theoryMarks = clamped)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = item.labMarks,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() }.take(3)
                            val clamped = (clean.toIntOrNull()?.coerceAtMost(100)?.toString()) ?: ""
                            rows[index] = item.copy(labMarks = clamped)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${item.totalOutOf150}/150",
                        modifier = Modifier.weight(0.7f),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onSaveDraftOffline(monthLabel, rows.toList()) },
                modifier = Modifier.weight(1f)
            ) { Text("Save Offline") }
            Button(
                onClick = { onPublishMarks(monthLabel, rows.toList()) },
                modifier = Modifier.weight(1.2f)
            ) { Text("Publish & Notify") }
            OutlinedButton(
                onClick = { onExportMarksPdf(monthLabel, rows.toList()) },
                modifier = Modifier.weight(0.9f)
            ) { Text("PDF") }
        }
    }
}
