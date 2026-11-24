package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PendingLeaveRequest
import com.example.data.PrincipalTab
import com.example.pdf.FullReportExportDialog
import com.example.ui.components.AttendanceFinalizationBanner
import com.example.ui.components.AttendanceStatusPill
import com.example.ui.components.CheckUpdateIconButton
import com.example.ui.components.DigitalSignaturePad
import com.example.ui.components.DigitalSignaturePreview
import com.example.ui.components.InlineAlertCard
import com.example.ui.components.NoticeCardItem
import com.example.ui.components.TricolourRibbon
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

@Composable
fun PrincipalDashboardScreen(
    uiState: AppUiState,
    onTabSelected: (PrincipalTab) -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onCheckUpdate: () -> Unit,
    onExportPdf: (onPdfReady: (String) -> Unit) -> Unit,
    onApproveAndSign: (reqId: String, signature: String) -> Unit,
    onRejectLeave: (reqId: String, reason: String) -> Unit,
    onDismissApprovalFeedback: () -> Unit,
    onPublishPinnedBroadcast: (title: String, category: String, body: String, onReset: () -> Unit) -> Unit,
    onRefreshAcademicSubmissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        if (uiState.selectedPrincipalTab != PrincipalTab.SCORECARD) {
            onTabSelected(PrincipalTab.SCORECARD)
        } else {
            onLogout()
        }
    }

    val colors = LocalItiColors.current
    val data = uiState.staffData
    val pendingLeaves = data?.pendingLeaves.orEmpty()
    val allAtt = data?.allAttendance.orEmpty()
    val presentCount = allAtt.count { it.todayStatus.equals("PRESENT", true) }
    val below80Count = allAtt.count { it.total > 0 && it.pct < 80 }
    val avgPct = if (allAtt.isNotEmpty()) allAtt.map { it.pct }.average().toInt() else 0

    var showPdfDialog by remember { mutableStateOf(false) }
    var approvingReq by remember { mutableStateOf<PendingLeaveRequest?>(null) }
    var rejectingReq by remember { mutableStateOf<PendingLeaveRequest?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    var sigData by remember { mutableStateOf("") }
    var sigKey by remember { mutableIntStateOf(0) }

    if (showPdfDialog && data != null) {
        FullReportExportDialog(staffData = data, roleLabel = "Principal (Govt. ITI Ranni)", onDismiss = { showPdfDialog = false })
    }

    if (approvingReq != null) {
        val req = approvingReq!!
        AlertDialog(
            onDismissRequest = { approvingReq = null },
            title = { Text("Principal Final Approval & Signature") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${req.name} (Roll #${req.roll}) • ${req.type}\nWindow: ${req.timeWindow}\nReason: ${req.reason}", style = MaterialTheme.typography.bodySmall)
                    DigitalSignaturePad(defaultSignerName = "Principal (Govt. ITI Ranni)", resetKey = sigKey, onSignatureChanged = { sigData = it }, tagPrefix = "principal_leave_sig")
                }
            },
            confirmButton = {
                Button(enabled = sigData.isNotBlank(), onClick = {
                    onDismissApprovalFeedback()
                    onApproveAndSign(req.reqId, sigData)
                    approvingReq = null
                    sigData = ""
                    sigKey++
                }) { Text("Counter-Sign & Approve") }
            },
            dismissButton = { TextButton(onClick = { approvingReq = null }) { Text("Cancel") } }
        )
    }

    if (rejectingReq != null) {
        val req = rejectingReq!!
        AlertDialog(
            onDismissRequest = { rejectingReq = null },
            title = { Text("Reject Leave Request") },
            text = { OutlinedTextField(value = rejectReason, onValueChange = { rejectReason = it }, label = { Text("Reason") }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(enabled = rejectReason.isNotBlank(), onClick = {
                    onDismissApprovalFeedback()
                    onRejectLeave(req.reqId, rejectReason)
                    rejectReason = ""
                    rejectingReq = null
                }, colors = ButtonDefaults.buttonColors(containerColor = colors.redAccent)) { Text("Reject") }
            },
            dismissButton = { TextButton(onClick = { rejectingReq = null }) { Text("Cancel") } }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Column(modifier = Modifier.fillMaxWidth().background(colors.cardSurface).statusBarsPadding()) {
                    TricolourRibbon(height = 3.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, tint = colors.violetAccent, modifier = Modifier.size(28.dp))
                            Column {
                                Text("Principal Oversight", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Govt. ITI Ranni • Electronics Mechanic", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                            }
                        }
                        Row {
                            IconButton(onClick = { showPdfDialog = true }) {
                                Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Export Reports", tint = colors.emeraldAccent)
                            }
                            CheckUpdateIconButton(uiState.isCheckingUpdate, uiState.availableUpdate != null, onCheckUpdate)
                            IconButton(onClick = onRefresh, enabled = !uiState.isRefreshingStaff) {
                                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_refresh), tint = colors.skyAccent)
                            }
                            IconButton(onClick = onLogout) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = stringResource(R.string.action_logout), tint = colors.redAccent)
                            }
                        }
                    }
                    if (uiState.isRefreshingStaff) {
                        LinearProgressIndicator(color = colors.violetAccent, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(containerColor = colors.cardSurface, windowInsets = WindowInsets.navigationBars) {
                        PrincipalTab.entries.forEach { tab ->
                            val selected = uiState.selectedPrincipalTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onTabSelected(tab) },
                                icon = { PrincipalTabIcon(tab, selected, pendingLeaves.size) },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = colors.violetAccent, selectedTextColor = colors.violetAccent, indicatorColor = colors.violetContainer),
                                modifier = Modifier.testTag("principal_nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (useRail) {
                    NavigationRail(containerColor = colors.cardSurface, modifier = Modifier.fillMaxHeight()) {
                        Spacer(Modifier.height(12.dp))
                        PrincipalTab.entries.forEach { tab ->
                            val selected = uiState.selectedPrincipalTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onTabSelected(tab) },
                                icon = { PrincipalTabIcon(tab, selected, pendingLeaves.size) },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (uiState.selectedPrincipalTab) {
                        PrincipalTab.SCORECARD -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (data != null) {
                                item { AttendanceFinalizationBanner(data.attendanceClosed, data.attendanceDate, data.closedAt) }
                            }
                            item {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ScoreMetricTile("Avg Trade %", "$avgPct%", colors.skyAccent, Modifier.weight(1f))
                                    ScoreMetricTile("Present Today", "$presentCount/${allAtt.size}", colors.emeraldAccent, Modifier.weight(1f))
                                    ScoreMetricTile("Below 80%", "$below80Count", colors.amberAccent, Modifier.weight(1f))
                                }
                            }
                            items(allAtt.sortedBy { it.roll.toIntOrNull() ?: 99 }, key = { "pr_${it.roll}" }) { rec ->
                                Surface(shape = RoundedCornerShape(14.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Roll #${rec.roll.padStart(2, '0')} • ${rec.name}", style = MaterialTheme.typography.titleSmall)
                                            Text("In: ${rec.inT} • Out: ${rec.outT} • Overall: ${rec.pct}%", style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily), color = colors.textMuted)
                                        }
                                        AttendanceStatusPill(rec.todayStatus)
                                    }
                                }
                            }
                        }

                        PrincipalTab.APPROVALS -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (uiState.approvalSuccessMessage != null) item { InlineAlertCard(uiState.approvalSuccessMessage, false) }
                            if (uiState.approvalError != null) item { InlineAlertCard(uiState.approvalError, true) }
                            if (pendingLeaves.isEmpty()) {
                                item { InlineAlertCard("No out-pass requests awaiting Principal counter-signature.", false) }
                            } else {
                                items(pendingLeaves, key = { "pr_lv_${it.reqId}" }) { req ->
                                    Surface(shape = RoundedCornerShape(16.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.violetAccent.copy(alpha = 0.45f)), modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Roll #${req.roll} • ${req.name}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                            Text("${req.type} • ${req.timeWindow} • ${req.date}", style = MaterialTheme.typography.labelMedium, color = colors.skyAccent)
                                            Text(req.reason, style = MaterialTheme.typography.bodySmall)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(onClick = { rejectingReq = req }, modifier = Modifier.weight(1f)) { Text("Reject", color = colors.redAccent) }
                                                Button(onClick = { approvingReq = req }, modifier = Modifier.weight(1.2f)) { Text("Counter-Sign") }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        PrincipalTab.ACADEMICS -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Log Books & Signed Marks", style = MaterialTheme.typography.titleMedium)
                                    OutlinedButton(onClick = onRefreshAcademicSubmissions) { Text("Refresh") }
                                }
                            }
                            val signedMark = data?.monthlyMarks?.firstOrNull { it.signedBy.isNotBlank() || it.signatureData.isNotBlank() }
                            if (signedMark != null) {
                                item {
                                    DigitalSignaturePreview(
                                        signedBy = "Marks Register Signed By: ${signedMark.signedBy}",
                                        signedAt = signedMark.signedAt,
                                        signatureData = signedMark.signatureData
                                    )
                                }
                            }
                            items(data?.academicSubmissions.orEmpty(), key = { "pr_sub_${it.id}_${it.roll}" }) { sub ->
                                Surface(shape = RoundedCornerShape(14.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Roll #${sub.roll} • ${sub.studentName} (${sub.verificationStatus})", style = MaterialTheme.typography.titleSmall)
                                        Text("Theory: ${sub.theoryTopic} • Lab: ${sub.labJobName}", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                                    }
                                }
                            }
                        }

                        PrincipalTab.BROADCAST -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item {
                                var title by remember { mutableStateOf("") }
                                var body by remember { mutableStateOf("") }
                                Surface(shape = RoundedCornerShape(18.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.55f)), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Publish Pinned Principal Broadcast", style = MaterialTheme.typography.titleMedium)
                                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Broadcast Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                        OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Broadcast Message") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                                        if (uiState.noticePostMessage != null) InlineAlertCard(uiState.noticePostMessage, false)
                                        if (uiState.noticePostError != null) InlineAlertCard(uiState.noticePostError, true)
                                        Button(onClick = { onPublishPinnedBroadcast(title, "Principal Broadcast", body) { title = ""; body = "" } }, enabled = !uiState.isPostingNotice && title.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Pin & Broadcast Now")
                                        }
                                    }
                                }
                            }
                            items(data?.notices.orEmpty(), key = { "pr_ntc_${it.id}" }) { notice -> NoticeCardItem(notice, false) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreMetricTile(label: String, value: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val colors = LocalItiColors.current
    Surface(shape = RoundedCornerShape(14.dp), color = colors.cardSurface, border = BorderStroke(1.dp, colors.borderSubtle), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold), color = accent)
        }
    }
}

@Composable
private fun PrincipalTabIcon(tab: PrincipalTab, selected: Boolean, pendingCount: Int) {
    val colors = LocalItiColors.current
    val icon: ImageVector = when (tab) {
        PrincipalTab.SCORECARD -> if (selected) Icons.Rounded.Analytics else Icons.Outlined.Analytics
        PrincipalTab.APPROVALS -> if (selected) Icons.Rounded.VerifiedUser else Icons.Outlined.VerifiedUser
        PrincipalTab.ACADEMICS -> if (selected) Icons.AutoMirrored.Rounded.MenuBook else Icons.AutoMirrored.Outlined.MenuBook
        PrincipalTab.BROADCAST -> if (selected) Icons.Rounded.Campaign else Icons.Outlined.Campaign
    }
    if (tab == PrincipalTab.APPROVALS && pendingCount > 0) {
        BadgedBox(badge = { Badge(containerColor = colors.amberAccent) { Text(pendingCount.toString()) } }) {
            Icon(icon, contentDescription = tab.label, modifier = Modifier.size(22.dp))
        }
    } else {
        Icon(icon, contentDescription = tab.label, modifier = Modifier.size(22.dp))
    }
}
