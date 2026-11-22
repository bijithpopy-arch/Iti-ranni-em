package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HolidayAnnouncement
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalItiColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object ExternalUrlHelper {
    fun openUrl(context: Context, url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            Toast.makeText(context, "Document URL not available.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(trimmed)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No browser found to open link.", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun TricolourRibbon(
    modifier: Modifier = Modifier,
    height: Dp = 4.dp
) {
    val colors = LocalItiColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(height)
                .background(colors.saffron)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(height)
                .background(colors.tricolourWhite)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(height)
                .background(colors.indiaGreen)
        )
    }
}

@Composable
fun IndianFlagEmblem(
    contentDescriptionText: String,
    modifier: Modifier = Modifier,
    width: Dp = 48.dp,
    height: Dp = 32.dp
) {
    val colors = LocalItiColors.current
    Canvas(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, colors.borderSubtle, RoundedCornerShape(6.dp))
            .semantics { contentDescription = contentDescriptionText }
    ) {
        val bandHeight = size.height / 3f
        drawRect(
            color = colors.saffron,
            topLeft = Offset(0f, 0f),
            size = Size(size.width, bandHeight)
        )
        drawRect(
            color = colors.tricolourWhite,
            topLeft = Offset(0f, bandHeight),
            size = Size(size.width, bandHeight)
        )
        drawRect(
            color = colors.indiaGreen,
            topLeft = Offset(0f, bandHeight * 2f),
            size = Size(size.width, bandHeight)
        )

        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = bandHeight * 0.41f
        drawCircle(
            color = colors.ashokaNavy,
            radius = radius,
            center = center,
            style = Stroke(width = size.height * 0.032f)
        )
        drawCircle(
            color = colors.ashokaNavy,
            radius = radius * 0.16f,
            center = center
        )
        for (i in 0 until 24) {
            val angleRad = (i * 15.0) * (PI / 180.0)
            val endX = center.x + (radius * cos(angleRad)).toFloat()
            val endY = center.y + (radius * sin(angleRad)).toFloat()
            drawLine(
                color = colors.ashokaNavy,
                start = center,
                end = Offset(endX, endY),
                strokeWidth = size.height * 0.018f
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MottoMarqueeBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        colors.saffron.copy(alpha = 0.14f),
                        colors.skyAccent.copy(alpha = 0.12f),
                        colors.indiaGreen.copy(alpha = 0.14f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        colors.saffron.copy(alpha = 0.45f),
                        colors.skyAccent.copy(alpha = 0.35f),
                        colors.indiaGreen.copy(alpha = 0.45f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "$text     •     $text",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    velocity = 44.dp,
                    initialDelayMillis = 0,
                    repeatDelayMillis = 0
                )
        )
    }
}

@Composable
fun EspStatusBadge(
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "esp_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = colors.emeraldContainer,
        border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.emeraldAccent.copy(alpha = pulseAlpha))
            )
            Icon(
                imageVector = Icons.Rounded.Memory,
                contentDescription = null,
                tint = colors.emeraldAccent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.5.sp
                ),
                color = colors.emeraldAccent,
                maxLines = 1
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
        shape = RoundedCornerShape(18.dp),
        color = colors.amberContainer,
        border = BorderStroke(1.5.dp, colors.amberAccent.copy(alpha = 0.7f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_holiday_banner")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.amberAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Celebration,
                    contentDescription = "Holiday Announcement",
                    tint = colors.amberAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "DECLARED HOLIDAY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.amberAccent
                    )
                    Text(
                        text = "• ${holiday.date}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = holiday.reason,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "ABSENT alerts are automatically paused for this holiday date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
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
    val bg = if (attendanceClosed) colors.emeraldContainer else colors.skyContainer
    val fg = if (attendanceClosed) colors.emeraldAccent else colors.skyAccent
    val title = if (attendanceClosed) {
        "Today's Attendance Confirmed & Closed"
    } else {
        "Today's Attendance is Still Open"
    }
    val subtitle = if (attendanceClosed) {
        val timePart = if (closedAt.isNotBlank()) " at $closedAt" else ""
        "Finalized by Trade Instructor on $attendanceDate$timePart."
    } else {
        "Register is in progress ($attendanceDate). Absent / Half-Day alerts trigger only after Instructor closes attendance."
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (attendanceClosed) "attendance_closed_banner" else "attendance_open_banner")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (attendanceClosed) Icons.Rounded.Verified else Icons.Rounded.LockClock,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = fg
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
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
        upper == "HOLIDAY" -> colors.amberContainer to colors.amberAccent
        upper.contains("HALF") || upper.contains("OUT-PASS") || upper.contains("OUTPASS") || upper.contains("PERM") ->
            colors.amberContainer to colors.amberAccent
        upper.contains("ALREADY") -> colors.skyContainer to colors.skyAccent
        else -> colors.redContainer to colors.redAccent
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.ifBlank { "ABSENT" },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = fg,
                maxLines = 1
            )
        }
    }
}

@Composable
fun LeaveStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    val upper = status.uppercase()
    val (bg, fg, display) = when {
        upper == "APPROVED" -> Triple(colors.emeraldContainer, colors.emeraldAccent, "APPROVED")
        upper == "PENDING_PRINCIPAL" -> Triple(colors.violetContainer, colors.violetAccent, "PENDING PRINCIPAL")
        upper == "PENDING_SIR" -> Triple(colors.amberContainer, colors.amberAccent, "PENDING INSTRUCTOR")
        upper.contains("REJECT") || upper.contains("DENY") -> Triple(colors.redContainer, colors.redAccent, status)
        else -> Triple(colors.skyContainer, colors.skyAccent, status.replace("_", " "))
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.45f))
    ) {
        Text(
            text = display,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}
