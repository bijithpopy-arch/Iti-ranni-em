package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notifications.AttendanceNotificationHelper

@Composable
fun RingtonePickerCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var currentTitle by remember {
        mutableStateOf(AttendanceNotificationHelper.getSelectedRingtoneTitle(context))
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            AttendanceNotificationHelper.setSelectedSoundUri(context, pickedUri)
            currentTitle = AttendanceNotificationHelper.getSelectedRingtoneTitle(context)
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Alert Ringtone Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Active Sound: $currentTitle",
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select ITI Ranni EM Alert Tone")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                AttendanceNotificationHelper.getSelectedSoundUri(context)
                            )
                        }
                        ringtoneLauncher.launch(intent)
                    }
                ) {
                    Text("Pick Ringtone")
                }

                OutlinedButton(
                    onClick = {
                        AttendanceNotificationHelper.setSelectedSoundUri(context, null)
                        currentTitle = AttendanceNotificationHelper.getSelectedRingtoneTitle(context)
                    }
                ) {
                    Text("Reset Default")
                }

                OutlinedButton(
                    onClick = {
                        AttendanceNotificationHelper.showAlert(
                            context = context,
                            title = "ITI Ranni EM Test Alert",
                            message = "Custom ringtone check successful."
                        )
                    }
                ) {
                    Text("Test")
                }
            }
        }
    }
}
