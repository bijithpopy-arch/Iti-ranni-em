package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AttendanceMath
import com.example.data.PrincipalTab
import com.example.data.ShopFloorEntry
import com.example.data.StaffAttendanceRecord
import com.example.ui.components.CheckUpdateIconButton
import com.example.ui.components.ExternalUrlHelper
import com.example.ui.components.PinnedPrincipalBroadcastBanner
import com.example.ui.components.TricolourRibbon
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState
import kotlin.math.roundToInt

/**
 * Dedicated Principal Executive Oversight Dashboard (Read-Only Analytics,
 * Ranked <80% Exam Risk Watchlist, Shop Floor Audit Log, Approvals & Pinned Broadcasts).
 */
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
    val context = LocalContext.current
    val staffData = uiState.staffData
    val allRecords = staffData?.allAttendance.orEmpty()
    val pendingCount = staffData?.pendingLeaves?.size ?: 0
    val activeHeadcount = allRecords.count {
        val s = it.todayStatus.uppercase()
        s == "PRESENT" || s.contains("HALF") || s.contains("OUT-PASS") || s == "LATE" || s == "ON-DUTY"
    }
    val overallTradePct = remember(allRecords) {
        if (allRecords.isEmpty()) 0
        else allRecords.map { it.pct.coerceIn(0, 100) }.average().roundToInt().coerceIn(0, 100)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.cardSurface)
                        .statusBarsPadding()
                ) {
                    TricolourRibbon(height = 3.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.violetContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = colors.violetAccent,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "PRINCIPAL EXECUTIVE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = JetBrainsMonoFamily,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = colors.violetAccent
                                        )
                                    }
                                }
                                Text(
                                    text = "Health: $overallTradePct% • $activeHeadcount/${allRecords.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = if (overallTradePct >= 80) colors.emeraldAccent else colors.amberAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Govt. ITI Ranni • Oversight",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            CheckUpdateIconButton(
                                isChecking = uiState.isCheckingUpdate,
                                hasUpdate = uiState.availableUpdate != null,
                                onClick = onCheckUpdate
                            )
                            IconButton(
                                onClick = onRefresh,
                                enabled = !uiState.isRefreshingStaff,
                                modifier = Modifier.testTag("principal_refresh_button")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_refresh), tint = colors.skyAccent)
                            }
                            IconButton(
                                onClick = onLogout,
                                modifier = Modifier.testTag("principal_logout_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = stringResource(R.string.action_logout), tint = colors.redAccent)
                            }
                        }
                    }

                    if (uiState.isRefreshingStaff) {
                        LinearProgressIndicator(
                            color = colors.violetAccent,
                            trackColor = colors.cardElevated,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.borderSubtle)
                        )
                    }
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = colors.cardSurface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        PrincipalTab.entries.forEach { tab ->
                            val selected = uiState.selectedPrincipalTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onTabSelected(tab) },
                                icon = { PrincipalTabIcon(tab = tab, selected = selected, pendingCount = pendingCount) },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.violetAccent,
                                    selectedTextColor = colors.violetAccent,
                                    indicatorColor = colors.violetContainer,
                                    unselectedIconColor = colors.textMuted,
                                    unselectedTextColor = colors.textMuted
                                ),
                                modifier = Modifier.testTag("principal_nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = colors.cardSurface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        PrincipalTab.entries.forEach { tab ->
                            val selected = uiState.selectedPrincipalTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onTabSelected(tab) },
                                icon = { PrincipalTabIcon(tab = tab, selected = selected, pendingCount = pendingCount) },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = colors.violetAccent,
                                    selectedTextColor = colors.violetAccent,
                                    indicatorColor = colors.violetContainer,
                                    unselectedIconColor = colors.textMuted,
                                    unselectedTextColor = colors.textMuted
                                )
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (uiState.staffBannerError != null) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            InlineAlertCard(message = uiState.staffBannerError, isError = true)
                        }
                    }

                    when (uiState.selectedPrincipalTab) {
                        PrincipalTab.SCORECARD -> PrincipalScorecardTab(
                            uiState = uiState,
                            overallTradePct = overallTradePct,
                            activeHeadcount = activeHeadcount,
                            onExportPdf = {
                                onExportPdf { url -> ExternalUrlHelper.openUrl(context, url) }
                            }
                        )

                        PrincipalTab.SHOP_FLOOR_AUDIT -> PrincipalShopFloorAuditTab(uiState = uiState)

                        PrincipalTab.LEAVE_APPROVALS -> PendingApprovalsTab(
                            uiState = uiState,
                            onApproveAndSign = onApproveAndSign,
                            onRejectLeave = onRejectLeave,
                            onDismissApprovalFeedback = onDismissApprovalFeedback
                        )

                        PrincipalTab.BROADCASTS -> PrincipalBroadcastTab(
                            uiState = uiState,
                            onPublishPinnedBroadcast = onPublishPinnedBroadcast
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrincipalTabIcon(
    tab: PrincipalTab,
    selected: Boolean,
    pendingCount: Int
) {
    val colors = LocalItiColors.current
    val icon: ImageVector = when (tab) {
        PrincipalTab.SCORECARD -> if (selected) Icons.Rounded.Assessment else Icons.Outlined.Assessment
        PrincipalTab.SHOP_FLOOR_AUDIT -> if (selected) Icons.Rounded.FactCheck else Icons.Outlined.FactCheck
        PrincipalTab.LEAVE_APPROVALS -> if (selected) Icons.Rounded.PendingActions else Icons.Outlined.PendingActions
        PrincipalTab.BROADCASTS -> if (selected) Icons.Rounded.Campaign else Icons.Outlined.Campaign
    }
    if (tab == PrincipalTab.LEAVE_APPROVALS && pendingCount > 0) {
        BadgedBox(
            badge = {
                Badge(containerColor = colors.amberAccent, contentColor = MaterialTheme.colorScheme.onTertiary) {
                    Text(pendingCount.toString())
                }
            }
        ) {
            Icon(icon, contentDescription = tab.label, modifier = Modifier.size(22.dp))
        }
    } else {
        Icon(icon, contentDescription = tab.label, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun PrincipalScorecardTab(
    uiState: AppUiState,
    overallTradePct: Int,
    activeHeadcount: Int,
    onExportPdf: () -> Unit
) {
    val colors = LocalItiColors.current
    val context = LocalContext.current
    val allRecords = uiState.staffData?.allAttendance.orEmpty()
    val riskWatchlist = remember(allRecords) {
        allRecords
            .filter { it.pct < AttendanceMath.EXAM_THRESHOLD_PCT }
            .sortedBy { it.pct }
    }
    val eligibleCount = (allRecords.size - riskWatchlist.size).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("principal_scorecard_tab"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Executive Trade Health Scorecard Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.5.dp, colors.violetAccent.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Executive Trade Health Scorecard",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Electronics Mechanic (2025–2027) • Official DGT 80% Oversight",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ScorecardKpiTile(
                            label = "TRADE AVG",
                            value = "$overallTradePct%",
                            accent = if (overallTradePct >= 80) colors.emeraldAccent else colors.amberAccent,
                            modifier = Modifier.weight(1f)
                        )
                        ScorecardKpiTile(
                            label = "ACTIVE TODAY",
                            value = "$activeHeadcount/${allRecords.size}",
                            accent = colors.skyAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ScorecardKpiTile(
                            label = "ELIGIBLE >=80%",
                            value = eligibleCount.toString(),
                            accent = colors.emeraldAccent,
                            modifier = Modifier.weight(1f)
                        )
                        ScorecardKpiTile(
                            label = "AT RISK <80%",
                            value = riskWatchlist.size.toString(),
                            accent = if (riskWatchlist.isNotEmpty()) colors.redAccent else colors.emeraldAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 1-Tap Official Register Export PDF
                    Button(
                        onClick = onExportPdf,
                        enabled = !uiState.isExportingPdf,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.violetAccent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("principal_export_register_button")
                    ) {
                        if (uiState.isExportingPdf) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Official Register...")
                        } else {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "1-Tap Official ITI Register Export (PDF)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    if (!uiState.exportedPdfUrl.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = { ExternalUrlHelper.openUrl(context, uiState.exportedPdfUrl) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.emeraldAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.emeraldAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Generated Official Register PDF")
                        }
                    }
                    if (uiState.exportPdfError != null) {
                        InlineAlertCard(message = uiState.exportPdfError, isError = true)
                    }
                }
            }
        }

        // Ranked <80% Exam-Eligibility Risk Watchlist
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = colors.redAccent, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Ranked <80% Exam-Eligibility Watchlist",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${riskWatchlist.size} trainee(s)",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.redAccent
                )
            }
        }

        if (riskWatchlist.isEmpty()) {
            item {
                InlineAlertCard(
                    message = "All trainees currently meet or exceed the 80% DGT exam attendance threshold.",
                    isError = false
                )
            }
        } else {
            items(riskWatchlist, key = { "risk_${it.roll}" }) { rec ->
                PrincipalRiskWatchlistCard(record = rec)
            }
        }
    }
}

@Composable
private fun ScorecardKpiTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                color = colors.textMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accent
            )
        }
    }
}

@Composable
private fun PrincipalRiskWatchlistCard(record: StaffAttendanceRecord) {
    val colors = LocalItiColors.current
    val advice = remember(record.present, record.half, record.total) {
        AttendanceMath.calculateSafeZone(record.present, record.half, record.total)
    }
    val severityAccent = if (record.pct < 70) colors.redAccent else colors.amberAccent

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, severityAccent.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("principal_risk_row_${record.roll}")
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Roll #${record.roll.padStart(2, '0')} • ${record.name}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "P: ${record.present} • H: ${record.half} • Total: ${record.total} • Today: ${record.todayStatus}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = colors.textMuted
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = severityAccent.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, severityAccent.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${record.pct}%",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = severityAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            Text(
                text = "Needs ${advice.daysCount} consecutive present day(s) to reach 80%.",
                style = MaterialTheme.typography.bodySmall,
                color = severityAccent
            )
        }
    }
}

@Composable
private fun PrincipalShopFloorAuditTab(uiState: AppUiState) {
    val colors = LocalItiColors.current
    val list = uiState.staffData?.shopFloorList.orEmpty()
    val overrides = uiState.staffData?.scanOverrides
    val completedCount = list.count { it.checkOutCardScanTime.isNotBlank() || it.status.equals("COMPLETED", ignoreCase = true) }
    val verifiedCount = list.count { it.verifiedByInstructor }
    val compliancePct = if (list.isEmpty()) 100 else ((completedCount.toDouble() / list.size) * 100).roundToInt().coerceIn(0, 100)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("principal_shop_floor_audit_tab"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Shop Floor & After-Hours Audit Log",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Read-only compliance monitor for Saturday & post-3:00 PM RFID card logout scans.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ScorecardKpiTile(
                            label = "CARD LOGOUTS",
                            value = "$completedCount/${list.size}",
                            accent = colors.skyAccent,
                            modifier = Modifier.weight(1f)
                        )
                        ScorecardKpiTile(
                            label = "COMPLIANCE",
                            value = "$compliancePct%",
                            accent = if (compliancePct >= 80) colors.emeraldAccent else colors.amberAccent,
                            modifier = Modifier.weight(1f)
                        )
                        ScorecardKpiTile(
                            label = "VERIFIED",
                            value = verifiedCount.toString(),
                            accent = colors.emeraldAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Read-only indicator of Instructor Scan Overrides
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (overrides?.earlyClassLogoutEnabled == true) colors.emeraldContainer else MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Early Class Out: ${if (overrides?.earlyClassLogoutEnabled == true) "ACTIVE" else "OFF"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = if (overrides?.earlyClassLogoutEnabled == true) colors.emeraldAccent else colors.textMuted,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (overrides?.shopFloorReleaseNowEnabled == true) colors.amberContainer else MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "SF Release Now: ${if (overrides?.shopFloorReleaseNowEnabled == true) "ACTIVE" else "OFF"}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = if (overrides?.shopFloorReleaseNowEnabled == true) colors.amberAccent else colors.textMuted,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (list.isEmpty()) {
            item {
                InlineAlertCard(message = "No active Shop Floor audit records for today.", isError = false)
            }
        } else {
            items(list, key = { "audit_${it.roll}_${it.date}_${it.id}" }) { entry ->
                PrincipalShopFloorAuditCard(entry = entry)
            }
        }
    }
}

@Composable
private fun PrincipalShopFloorAuditCard(entry: ShopFloorEntry) {
    val colors = LocalItiColors.current
    val hasOut = entry.checkOutCardScanTime.isNotBlank()
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Roll #${entry.roll.padStart(2, '0')} • ${entry.name}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (entry.verifiedByInstructor) "VERIFIED" else if (hasOut) "SCANNED OUT" else "PENDING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (entry.verifiedByInstructor) colors.emeraldAccent else if (hasOut) colors.skyAccent else colors.amberAccent
                )
            }
            Text(
                text = "Attendance: ${entry.attendancePct}% • ${if (entry.sessionType == "SATURDAY") "Saturday" else "Post-3:00 PM (till ${entry.scheduledEndTime})"}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.Nfc, contentDescription = null, tint = if (hasOut) colors.emeraldAccent else colors.textMuted, modifier = Modifier.size(15.dp))
                Text(
                    text = if (hasOut) {
                        "Card Logout: ${entry.checkOutCardScanTime} (${entry.durationMinutes} min)"
                    } else {
                        "No card-scan logout recorded yet"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = if (hasOut) colors.emeraldAccent else colors.textMuted
                )
            }
        }
    }
}

@Composable
private fun PrincipalBroadcastTab(
    uiState: AppUiState,
    onPublishPinnedBroadcast: (title: String, category: String, body: String, onReset: () -> Unit) -> Unit
) {
    val colors = LocalItiColors.current
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    val pinned = uiState.staffData?.pinnedBroadcast
    val notices = uiState.staffData?.notices.orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("principal_broadcast_tab"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (pinned != null) {
            item {
                PinnedPrincipalBroadcastBanner(notice = pinned)
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.cardSurface,
                border = BorderStroke(1.5.dp, colors.violetAccent.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.violetContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.PushPin, contentDescription = null, tint = colors.violetAccent, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "Post Official Principal Broadcast",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pinned automatically to the top of Trainee, Parent & Instructor dashboards",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Official Broadcast Headline") },
                        placeholder = { Text("e.g., DGT NCVT Exam Eligibility & Shop Floor Circular") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.violetAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("principal_broadcast_title_input")
                    )

                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("Official Circular Body (English / Manglish / മലയാളം)") },
                        placeholder = { Text("Enter message to pin across all dashboards...") },
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.violetAccent,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("principal_broadcast_body_input")
                    )

                    AnimatedVisibility(visible = uiState.noticePostMessage != null) {
                        InlineAlertCard(message = uiState.noticePostMessage.orEmpty(), isError = false)
                    }
                    AnimatedVisibility(visible = uiState.noticePostError != null) {
                        InlineAlertCard(message = uiState.noticePostError.orEmpty(), isError = true)
                    }

                    Button(
                        onClick = {
                            onPublishPinnedBroadcast(title, "Principal Broadcast", body) {
                                title = ""
                                body = ""
                            }
                        },
                        enabled = !uiState.isPostingNotice,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.violetAccent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("principal_publish_broadcast_button")
                    ) {
                        if (uiState.isPostingNotice) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Publishing Broadcast...")
                        } else {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pin & Broadcast to All Dashboards", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        if (notices.isNotEmpty()) {
            item {
                Text(
                    text = "All Institutional Notices (${notices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(notices, key = { "prin_notice_${it.id}" }) { notice ->
                NoticeCardItem(notice = notice, isUnread = false)
            }
        }
    }
}
