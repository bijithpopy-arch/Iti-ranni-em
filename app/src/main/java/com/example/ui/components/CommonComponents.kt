package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HolidayAnnouncement
import com.example.data.NoticeItem
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import java.net.URLEncoder
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TricolourRibbon(
    height: Dp = 3.dp,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Box(modifier = Modifier.weight(1f).height(height).background(colors.saffron))
        Box(modifier = Modifier.weight(1f).height(height).background(Color.White))
        Box(modifier = Modifier.weight(1f).height(height).background(colors.flagGreen))
    }
}

@Composable
fun IndianFlagEmblem(
    contentDescriptionText: String,
    width: Dp = 44.dp,
    height: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Canvas(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(4.dp))
            .semantics { contentDescription = contentDescriptionText }
    ) {
        val bandH = size.height / 3f
        drawRect(color = colors.saffron, topLeft = Offset.Zero, size = Size(size.width, bandH))
        drawRect(color = Color.White, topLeft = Offset(0f, bandH), size = Size(size.width, bandH))
        drawRect(color = colors.flagGreen, topLeft = Offset(0f, bandH * 2f), size = Size(size.width, bandH))
        val navy = Color(0xFF000080)
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = bandH * 0.42f
        drawCircle(color = navy, radius = r, center = center, style = Stroke(width = 1.4.dp.toPx()))
        for (i in 0 until 24) {
            val angle = (i * 15.0) * Math.PI / 180.0
            val end = Offset(
                center.x + (r * cos(angle)).toFloat(),
                center.y + (r * sin(angle)).toFloat()
            )
            drawLine(color = navy, start = center, end = end, strokeWidth = 0.7.dp.toPx())
        }
    }
}

@Composable
fun MottoMarqueeBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.skyContainer.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, colors.skyAccent.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            ),
            color = colors.skyAccent,
            maxLines = 1,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .basicMarquee()
        )
    }
}

@Composable
fun EspStatusBadge(
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(50),
        color = colors.emeraldContainer,
        border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Sensors,
                contentDescription = null,
                tint = colors.emeraldAccent,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = colors.emeraldAccent
            )
        }
    }
}

@Composable
fun AttendanceStatusPill(
    status: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val upper = status.uppercase()
    val (bg, fg) = when {
        upper == "PRESENT" -> colors.emeraldContainer to colors.emeraldAccent
        upper.contains("HALF") || upper == "LATE" -> colors.amberContainer to colors.amberAccent
        upper.contains("OUT-PASS") || upper == "ON-DUTY" -> colors.skyContainer to colors.skyAccent
        upper == "HOLIDAY" -> colors.violetContainer to colors.violetAccent
        else -> colors.redContainer to colors.redAccent
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Text(
            text = upper,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            ),
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun LeaveStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val upper = status.uppercase()
    val (bg, fg, label) = when {
        upper == "APPROVED" -> Triple(colors.emeraldContainer, colors.emeraldAccent, "APPROVED")
        upper == "PENDING_PRINCIPAL" -> Triple(colors.skyContainer, colors.skyAccent, "WAIT PRINCIPAL")
        upper == "REJECTED" -> Triple(colors.redContainer, colors.redAccent, "REJECTED")
        else -> Triple(colors.amberContainer, colors.amberAccent, "WAIT INSTRUCTOR")
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            ),
            color = fg,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

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
        modifier = modifier.testTag("check_update_icon_button")
    ) {
        if (isChecking) {
            CircularProgressIndicator(
                color = colors.skyAccent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
        } else if (hasUpdate) {
            BadgedBox(
                badge = {
                    Badge(containerColor = colors.amberAccent)
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdate,
                    contentDescription = "Update available",
                    tint = colors.amberAccent
                )
            }
        } else {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = "Check for updates",
                tint = colors.textMuted
            )
        }
    }
}

@Composable
fun ActiveHolidayBanner(
    holiday: HolidayAnnouncement,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.violetContainer,
        border = BorderStroke(1.5.dp, colors.violetAccent.copy(alpha = 0.65f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Celebration,
                contentDescription = null,
                tint = colors.violetAccent,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Holiday Declared (${holiday.date})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.violetAccent
                )
                Text(
                    text = holiday.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun AttendanceFinalizationBanner(
    attendanceClosed: Boolean,
    attendanceDate: String,
    closedAt: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (attendanceClosed) colors.emeraldContainer else colors.amberContainer,
        border = BorderStroke(
            1.dp,
            (if (attendanceClosed) colors.emeraldAccent else colors.amberAccent).copy(alpha = 0.55f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (attendanceClosed) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                contentDescription = null,
                tint = if (attendanceClosed) colors.emeraldAccent else colors.amberAccent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (attendanceClosed) {
                    "Register Closed ($attendanceDate)${if (closedAt.isNotBlank()) " at $closedAt" else ""} • Final Confirmed Status"
                } else {
                    "Register Open ($attendanceDate) • Live ESP8266 RF Scans Active"
                },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PinnedPrincipalBroadcastBanner(
    notice: NoticeItem,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.amberContainer,
        border = BorderStroke(1.5.dp, colors.amberAccent.copy(alpha = 0.7f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = null,
                    tint = colors.amberAccent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "PINNED BROADCAST • ${notice.postedBy.uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.amberAccent
                )
            }
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (notice.body.isNotBlank()) {
                Text(
                    text = notice.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun InlineAlertCard(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val bg = if (isError) colors.redContainer else colors.emeraldContainer
    val fg = if (isError) colors.redAccent else colors.emeraldAccent
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.55f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.Info,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun NoticeCardItem(
    notice: NoticeItem,
    isUnread: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardSurface,
        border = BorderStroke(
            width = if (isUnread || notice.isPinned) 1.5.dp else 1.dp,
            color = when {
                notice.isPinned -> colors.amberAccent.copy(alpha = 0.7f)
                isUnread -> colors.skyAccent.copy(alpha = 0.7f)
                else -> colors.borderSubtle
            }
        ),
        modifier = modifier.fillMaxWidth()
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Campaign,
                        contentDescription = null,
                        tint = if (notice.isPinned) colors.amberAccent else colors.skyAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = notice.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (notice.isPinned) colors.amberAccent else colors.skyAccent
                    )
                }
                Text(
                    text = notice.date,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = colors.textMuted
                )
            }
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (notice.body.isNotBlank()) {
                Text(
                    text = notice.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Posted by: ${notice.postedBy}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
        }
    }
}

/**
 * Finger-drawn Canvas digital signature pad that serializes strokes as normalized
 * coordinates `x,y;x,y|...` (compatible with PdfReportGenerator & DigitalSignaturePreview),
 * OR falls back cleanly to an authenticated digital text stamp if typed.
 */
@Composable
fun DigitalSignaturePad(
    defaultSignerName: String,
    resetKey: Int = 0,
    onSignatureChanged: (String) -> Unit,
    title: String = "Digital Signature",
    tagPrefix: String = "sig",
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val strokes = remember(resetKey) { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember(resetKey) { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
    var signerLabel by remember(resetKey, defaultSignerName) { mutableStateOf(defaultSignerName) }

    fun emitSerialized() {
        val w = canvasSize.width.coerceAtLeast(1).toFloat()
        val h = canvasSize.height.coerceAtLeast(1).toFloat()
        val serialized = strokes.filter { it.size >= 2 }.joinToString("|") { pts ->
            pts.joinToString(";") { pt ->
                val nx = (pt.x / w).coerceIn(0f, 1f)
                val ny = (pt.y / h).coerceIn(0f, 1f)
                "${"%.3f".format(nx)},${"%.3f".format(ny)}"
            }
        }
        onSignatureChanged(serialized)
    }

    LaunchedEffect(resetKey) {
        onSignatureChanged("")
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, if (strokes.isNotEmpty()) colors.emeraldAccent.copy(alpha = 0.7f) else colors.borderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Draw,
                        contentDescription = null,
                        tint = if (strokes.isNotEmpty()) colors.emeraldAccent else colors.skyAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TextButton(
                    onClick = {
                        strokes.clear()
                        currentStroke = emptyList()
                        onSignatureChanged("")
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("${tagPrefix}_clear_button")
                ) {
                    Icon(Icons.Rounded.Clear, contentDescription = "Clear signature", modifier = Modifier.size(14.dp), tint = colors.redAccent)
                    Spacer(Modifier.width(4.dp))
                    Text("Clear", style = MaterialTheme.typography.labelSmall, color = colors.redAccent)
                }
            }

            OutlinedTextField(
                value = signerLabel,
                onValueChange = { signerLabel = it },
                label = { Text("Authorized Signatory Name") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.skyAccent,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedContainerColor = colors.cardSurface,
                    unfocusedContainerColor = colors.cardSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${tagPrefix}_signer_input")
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(resetKey) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentStroke = listOf(offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentStroke = currentStroke + change.position
                            },
                            onDragEnd = {
                                if (currentStroke.size >= 2) {
                                    strokes.add(currentStroke)
                                }
                                currentStroke = emptyList()
                                emitSerialized()
                            },
                            onDragCancel = {
                                currentStroke = emptyList()
                            }
                        )
                    }
                    .testTag("${tagPrefix}_canvas")
            ) {
                if (strokes.isEmpty() && currentStroke.isEmpty()) {
                    Text(
                        text = "Sign with your finger here",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val guideY = size.height * 0.78f
                    drawLine(
                        color = Color(0xFFCBD5E1),
                        start = Offset(16.dp.toPx(), guideY),
                        end = Offset(size.width - 16.dp.toPx(), guideY),
                        strokeWidth = 1.dp.toPx()
                    )
                    (strokes + listOf(currentStroke)).forEach { pts ->
                        if (pts.size >= 2) {
                            val p = Path().apply {
                                moveTo(pts.first().x, pts.first().y)
                                pts.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(
                                path = p,
                                color = Color(0xFF0F172A),
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (strokes.isNotEmpty()) Icons.Rounded.Verified else Icons.Rounded.Info,
                    contentDescription = null,
                    tint = if (strokes.isNotEmpty()) colors.emeraldAccent else colors.textMuted,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (strokes.isNotEmpty()) {
                        "Digital signature captured for $signerLabel (${strokes.size} stroke(s))"
                    } else {
                        "Draw your signature above to digitally authenticate"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (strokes.isNotEmpty()) colors.emeraldAccent else colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DigitalSignaturePreview(
    signedBy: String,
    signedAt: String = "",
    signatureData: String = "",
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.emeraldContainer.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.55f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = colors.emeraldAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "DIGITALLY SIGNED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.emeraldAccent
                    )
                }
                Text(
                    text = signedBy.ifBlank { "Trade Instructor (EM)" },
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (signedAt.isNotBlank()) {
                    Text(
                        text = "Stamped: $signedAt",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = colors.textMuted
                    )
                }
            }

            if (signatureData.isNotBlank() && signatureData.contains(",")) {
                Box(
                    modifier = Modifier
                        .size(width = 112.dp, height = 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF8FAFC))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                        signatureData.split("|").forEach { strokeStr ->
                            val pts = strokeStr.split(";").mapNotNull { pair ->
                                val xy = pair.split(",")
                                val x = xy.getOrNull(0)?.toFloatOrNull()
                                val y = xy.getOrNull(1)?.toFloatOrNull()
                                if (x != null && y != null) Offset(x * size.width, y * size.height) else null
                            }
                            if (pts.size >= 2) {
                                val p = Path().apply {
                                    moveTo(pts.first().x, pts.first().y)
                                    pts.drop(1).forEach { lineTo(it.x, it.y) }
                                }
                                drawPath(
                                    path = p,
                                    color = Color(0xFF0F172A),
                                    style = Stroke(
                                        width = 1.8.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

object ExternalUrlHelper {
    fun openUrl(context: Context, url: String) {
        val clean = url.trim()
        if (clean.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(clean)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}

object WhatsAppBotHelper {
    private const val PREFS_NAME = "iti_ranni_wa_prefs"
    private const val KEY_BOT_PHONE = "wa_bot_phone"
    private const val DEFAULT_BOT_PHONE = "919447000000"

    fun getBotPhoneNumber(context: Context): String =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BOT_PHONE, DEFAULT_BOT_PHONE).orEmpty().ifBlank { DEFAULT_BOT_PHONE }

    fun setBotPhoneNumber(context: Context, rawPhone: String): String {
        val cleaned = rawPhone.filter { it.isDigit() }.ifBlank { DEFAULT_BOT_PHONE }
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_BOT_PHONE, cleaned)
            .apply()
        return cleaned
    }

    fun openWhatsAppBot(context: Context, message: String) {
        val phone = getBotPhoneNumber(context)
        val encoded = URLEncoder.encode(message, "UTF-8")
        ExternalUrlHelper.openUrl(context, "https://wa.me/$phone?text=$encoded")
    }
}
