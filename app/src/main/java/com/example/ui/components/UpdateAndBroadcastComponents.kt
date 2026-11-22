package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.NoticeItem
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

@Composable
fun CheckUpdateIconButton(
    isChecking: Boolean,
    hasUpdate: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    IconButton(
        onClick = onClick,
        enabled = !isChecking,
        modifier = modifier.testTag("check_app_update_button")
    ) {
        if (isChecking) {
            CircularProgressIndicator(
                color = colors.skyAccent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = stringResource(R.string.action_check_update),
                tint = if (hasUpdate) colors.emeraldAccent else colors.textMuted
            )
        }
    }
}

@Composable
fun AppUpdateDialog(
    uiState: AppUiState,
    onDownloadAndInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!uiState.showUpdateDialog) return

    val colors = LocalItiColors.current
    val updateInfo = uiState.availableUpdate

    AlertDialog(
        onDismissRequest = { if (!uiState.isDownloadingUpdate) onDismiss() },
        containerColor = colors.cardSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = colors.textMuted,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            updateInfo != null -> colors.emeraldContainer
                            uiState.updateError != null -> colors.redContainer
                            else -> colors.skyContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        updateInfo != null -> Icons.Rounded.SystemUpdate
                        uiState.updateError != null -> Icons.Rounded.ErrorOutline
                        else -> Icons.Rounded.CheckCircle
                    },
                    contentDescription = null,
                    tint = when {
                        updateInfo != null -> colors.emeraldAccent
                        uiState.updateError != null -> colors.redAccent
                        else -> colors.skyAccent
                    },
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = when {
                    updateInfo != null -> "New Update Available (v${updateInfo.versionName})"
                    uiState.updateError != null -> "Update Check Failed"
                    else -> "App Is Up To Date"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Installed: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.skyAccent
                )
                if (updateInfo != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "WHAT'S NEW IN BUILD ${updateInfo.versionCode}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = colors.textMuted
                            )
                            Text(
                                text = updateInfo.changelog,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                if (!uiState.updateStatusMessage.isNullOrBlank()) {
                    Text(
                        text = uiState.updateStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.emeraldAccent
                    )
                }
                if (!uiState.updateError.isNullOrBlank()) {
                    Text(
                        text = uiState.updateError,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.redAccent
                    )
                }
            }
        },
        confirmButton = {
            if (updateInfo != null) {
                Button(
                    onClick = onDownloadAndInstall,
                    enabled = !uiState.isDownloadingUpdate,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.emeraldAccent,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.testTag("install_app_update_button")
                ) {
                    if (uiState.isDownloadingUpdate) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onSecondary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Downloading...")
                    } else {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download & Install")
                    }
                }
            } else {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.skyAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            if (updateInfo != null && !uiState.isDownloadingUpdate) {
                TextButton(onClick = onDismiss) {
                    Text("Later", color = colors.textMuted)
                }
            }
        }
    )
}

@Composable
fun PinnedPrincipalBroadcastBanner(
    notice: NoticeItem,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.violetContainer,
        border = BorderStroke(1.5.dp, colors.violetAccent.copy(alpha = 0.7f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pinned_principal_broadcast_banner")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.violetAccent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Campaign,
                    contentDescription = "Principal Broadcast",
                    tint = colors.violetAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = null,
                        tint = colors.violetAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "PRINCIPAL BROADCAST • ${notice.date}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.violetAccent
                    )
                }
                Text(
                    text = notice.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = notice.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Issued by ${notice.postedBy}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
        }
    }
}
