package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Engineering
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.data.UserRole
import com.example.ui.components.EspStatusBadge
import com.example.ui.components.ExternalUrlHelper
import com.example.ui.components.IndianFlagEmblem
import com.example.ui.components.MottoMarqueeBanner
import com.example.ui.components.TricolourRibbon
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import com.example.viewmodel.AppUiState

private data class OfficialPortal(
    val title: String,
    val subtitle: String,
    val url: String,
    val icon: ImageVector,
    val testTag: String
)

private val OFFICIAL_PORTALS = listOf(
    OfficialPortal(
        title = "Skill India Digital Hub (SIDH)",
        subtitle = "skillindiadigital.gov.in",
        url = "https://www.skillindiadigital.gov.in",
        icon = Icons.Rounded.VerifiedUser,
        testTag = "portal_sidh_button"
    ),
    OfficialPortal(
        title = "NCVT MIS / DGT Portal",
        subtitle = "ncvtmis.gov.in",
        url = "https://www.ncvtmis.gov.in",
        icon = Icons.Rounded.School,
        testTag = "portal_ncvt_button"
    ),
    OfficialPortal(
        title = "Kerala Industrial Training Dept (DET Kerala)",
        subtitle = "det.kerala.gov.in",
        url = "https://det.kerala.gov.in",
        icon = Icons.Rounded.Public,
        testTag = "portal_det_kerala_button"
    )
)

@Composable
fun LoginScreen(
    uiState: AppUiState,
    biometricAvailable: Boolean,
    onRoleSelected: (UserRole) -> Unit,
    onRollChanged: (String) -> Unit,
    onPinChanged: (String) -> Unit,
    onRememberMeChanged: (Boolean) -> Unit,
    onBiometricEnabledChanged: (Boolean) -> Unit,
    onFingerprintUnlockClick: () -> Unit,
    onClearSavedLogin: () -> Unit,
    onCheckUpdateClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val context = LocalContext.current
    var showPin by remember { mutableStateOf(false) }

    val needsRollNumber = uiState.selectedRole == UserRole.TRAINEE || uiState.selectedRole == UserRole.PARENT

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_electronics_banner_1795563711290),
                contentDescription = stringResource(R.string.cd_circuit_banner),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.28f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TricolourRibbon(height = 4.dp)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InstitutionalHeaderCard()

                MottoMarqueeBanner(
                    text = stringResource(R.string.motto_marquee),
                    modifier = Modifier.testTag("motto_marquee_banner")
                )

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Attendance, Marks & Out-Pass Portal",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Select your role and sign in securely",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }

                        AnimatedVisibility(visible = uiState.hasSavedCredentials && uiState.biometricEnabled && biometricAvailable) {
                            Surface(
                                onClick = onFingerprintUnlockClick,
                                shape = RoundedCornerShape(14.dp),
                                color = colors.emeraldContainer,
                                border = BorderStroke(1.5.dp, colors.emeraldAccent.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("biometric_quick_unlock_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(colors.emeraldAccent.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Fingerprint,
                                            contentDescription = "Unlock with Fingerprint",
                                            tint = colors.emeraldAccent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.label_fingerprint_unlock),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = colors.emeraldAccent
                                        )
                                        Text(
                                            text = "Saved: ${uiState.savedRoleLabel.ifBlank { uiState.selectedRole.displayName }} • Tap to verify",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        RoleGridSelector(
                            selectedRole = uiState.selectedRole,
                            onRoleSelected = onRoleSelected
                        )

                        AnimatedVisibility(
                            visible = needsRollNumber,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            var rollMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = uiState.rollInput,
                                    onValueChange = { raw ->
                                        val digits = raw.filter { it.isDigit() }.take(2)
                                        val num = digits.toIntOrNull()
                                        if (digits.isEmpty() || (num != null && num <= 23)) {
                                            onRollChanged(digits)
                                        }
                                    },
                                    label = {
                                        Text(
                                            if (uiState.selectedRole == UserRole.PARENT) {
                                                "Ward's Roll Number (1–23)"
                                            } else {
                                                stringResource(R.string.label_roll_number)
                                            }
                                        )
                                    },
                                    placeholder = { Text("Enter 1 to 23 or pick Roll #01–#23") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Badge,
                                            contentDescription = null,
                                            tint = colors.skyAccent
                                        )
                                    },
                                    trailingIcon = {
                                        TextButton(
                                            onClick = { rollMenuExpanded = true },
                                            modifier = Modifier.testTag("login_roll_picker_button")
                                        ) {
                                            Text(
                                                text = "01–23 ▾",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = JetBrainsMonoFamily,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = colors.skyAccent
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.skyAccent,
                                        unfocusedBorderColor = colors.borderSubtle,
                                        focusedContainerColor = MaterialTheme.colorScheme.background,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                        focusedLabelColor = colors.skyAccent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("login_roll_input")
                                )

                                DropdownMenu(
                                    expanded = rollMenuExpanded,
                                    onDismissRequest = { rollMenuExpanded = false },
                                    modifier = Modifier
                                        .background(colors.cardElevated)
                                        .height(280.dp)
                                ) {
                                    (1..23).forEach { roll ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "Roll #${roll.toString().padStart(2, '0')}",
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontFamily = JetBrainsMonoFamily
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            onClick = {
                                                onRollChanged(roll.toString())
                                                rollMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        val pinLabel = when (uiState.selectedRole) {
                            UserRole.TRAINEE -> stringResource(R.string.label_pin)
                            UserRole.PARENT -> stringResource(R.string.label_parent_pin)
                            UserRole.TRADE_INSTRUCTOR, UserRole.PRINCIPAL -> stringResource(R.string.label_staff_pin)
                        }

                        OutlinedTextField(
                            value = uiState.pinInput,
                            onValueChange = onPinChanged,
                            label = { Text(pinLabel) },
                            placeholder = {
                                Text(
                                    when (uiState.selectedRole) {
                                        UserRole.TRAINEE -> "Enter your numeric PIN"
                                        UserRole.PARENT -> "Enter Parent numeric PIN"
                                        else -> "Enter authorized ${uiState.selectedRole.displayName} PIN"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = colors.skyAccent
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showPin = !showPin },
                                    modifier = Modifier.testTag("toggle_pin_visibility")
                                ) {
                                    Icon(
                                        imageVector = if (showPin) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = stringResource(R.string.cd_toggle_pin),
                                        tint = colors.textMuted
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { onLoginClick() }
                            ),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.skyAccent,
                                unfocusedBorderColor = colors.borderSubtle,
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                focusedLabelColor = colors.skyAccent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_pin_input")
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = uiState.rememberMe,
                                            onCheckedChange = onRememberMeChanged,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = colors.skyAccent,
                                                checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            modifier = Modifier.testTag("remember_me_checkbox")
                                        )
                                        Text(
                                            text = stringResource(R.string.label_remember_me),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (uiState.hasSavedCredentials) {
                                        TextButton(
                                            onClick = onClearSavedLogin,
                                            modifier = Modifier.testTag("clear_saved_login_button")
                                        ) {
                                            Text(
                                                text = "Forget",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.redAccent
                                            )
                                        }
                                    }
                                }

                                if (biometricAvailable) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 10.dp, end = 6.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Fingerprint,
                                                contentDescription = null,
                                                tint = if (uiState.biometricEnabled) colors.emeraldAccent else colors.textMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Enable Fingerprint Unlock on app open",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textMuted
                                            )
                                        }
                                        Switch(
                                            checked = uiState.biometricEnabled,
                                            onCheckedChange = onBiometricEnabledChanged,
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                                                checkedTrackColor = colors.emeraldAccent
                                            ),
                                            modifier = Modifier.testTag("fingerprint_unlock_switch")
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = uiState.biometricMessage != null) {
                            Text(
                                text = uiState.biometricMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.amberAccent,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = uiState.loginError != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colors.redContainer,
                                border = BorderStroke(1.dp, colors.redAccent.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_error_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = colors.redAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = uiState.loginError.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.redAccent
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onLoginClick,
                            enabled = !uiState.isLoggingIn,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.skyAccent,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = colors.skyAccent.copy(alpha = 0.45f),
                                disabledContentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("login_submit_button")
                        ) {
                            if (uiState.isLoggingIn) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Verifying with Server...",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Login as ${uiState.selectedRole.displayName}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                Surface(
                    onClick = onCheckUpdateClick,
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardSurface,
                    border = BorderStroke(1.dp, colors.skyAccent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_check_update_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = colors.skyAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.action_check_update),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Installed v${BuildConfig.VERSION_NAME} • GitHub Releases",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = colors.textMuted
                                )
                            }
                        }
                        if (uiState.isCheckingUpdate) {
                            CircularProgressIndicator(
                                color = colors.skyAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.portal_links_header).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            letterSpacing = 1.sp
                        ),
                        color = colors.textMuted,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    OFFICIAL_PORTALS.forEach { portal ->
                        OfficialPortalPill(
                            portal = portal,
                            onClick = { ExternalUrlHelper.openUrl(context, portal.url) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun InstitutionalHeaderCard() {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.cardSurface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IndianFlagEmblem(
                        contentDescriptionText = stringResource(R.string.cd_indian_flag),
                        width = 48.dp,
                        height = 32.dp
                    )
                    Image(
                        painter = painterResource(id = R.drawable.img_iti_skill_badge_1795543228780),
                        contentDescription = stringResource(R.string.cd_iti_badge),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, colors.skyAccent.copy(alpha = 0.65f), CircleShape)
                    )
                }

                EspStatusBadge(label = "ESP8266 Live")
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.institution_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        letterSpacing = 0.8.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.institution_subtitle),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.skyAccent
                )
            }
        }
    }
}

@Composable
private fun RoleGridSelector(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit
) {
    val rows = remember { UserRole.entries.chunked(2) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowRoles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowRoles.forEach { role ->
                    RoleChipButton(
                        role = role,
                        selected = role == selectedRole,
                        onClick = { onRoleSelected(role) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RoleChipButton(
    role: UserRole,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val icon = when (role) {
        UserRole.TRAINEE -> Icons.Rounded.Person
        UserRole.PARENT -> Icons.Rounded.FamilyRestroom
        UserRole.TRADE_INSTRUCTOR -> Icons.Rounded.Engineering
        UserRole.PRINCIPAL -> Icons.Rounded.AdminPanelSettings
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.skyAccent else MaterialTheme.colorScheme.background,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) colors.skyAccent else colors.borderSubtle
        ),
        modifier = modifier
            .height(48.dp)
            .testTag("role_segment_${role.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else colors.textMuted,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = role.displayName,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) MaterialTheme.colorScheme.onPrimary else colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OfficialPortalPill(
    portal: OfficialPortal,
    onClick: () -> Unit
) {
    val colors = LocalItiColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = colors.cardSurface,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(portal.testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.skyContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = portal.icon,
                        contentDescription = null,
                        tint = colors.skyAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = portal.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = portal.subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = stringResource(R.string.cd_open_external),
                tint = colors.skyAccent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
