package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalItiColors

enum class SignatureMode { DRAW, TYPE }

@Composable
fun DigitalSignaturePad(
    defaultSignerName: String,
    resetKey: Int,
    onSignatureChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Digital Signature",
    tagPrefix: String = "sig"
) {
    val colors = LocalItiColors.current
    var mode by remember { mutableStateOf(SignatureMode.DRAW) }
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var typedSignature by remember { mutableStateOf("") }

    LaunchedEffect(resetKey) {
        strokes.clear()
        currentStroke = emptyList()
        typedSignature = ""
        onSignatureChanged("")
    }

    val hasDrawing = strokes.isNotEmpty() || currentStroke.size > 1

    fun emitDrawnSignature() {
        val signer = defaultSignerName.trim().ifBlank { "Verified Signer" }
        onSignatureChanged("$signer (Digitally Signed)")
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Draw / Type Mode Toggle
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, colors.borderSubtle)
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    SignatureModeChip(
                        label = "Draw",
                        icon = Icons.Rounded.Draw,
                        selected = mode == SignatureMode.DRAW,
                        onClick = {
                            mode = SignatureMode.DRAW
                            if (hasDrawing) emitDrawnSignature() else onSignatureChanged("")
                        },
                        modifier = Modifier.testTag("${tagPrefix}_mode_draw")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    SignatureModeChip(
                        label = "Type",
                        icon = Icons.Rounded.Keyboard,
                        selected = mode == SignatureMode.TYPE,
                        onClick = {
                            mode = SignatureMode.TYPE
                            onSignatureChanged(typedSignature.trim())
                        },
                        modifier = Modifier.testTag("${tagPrefix}_mode_type")
                    )
                }
            }
        }

        if (mode == SignatureMode.DRAW) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(
                        width = 1.dp,
                        color = if (hasDrawing) colors.emeraldAccent.copy(alpha = 0.65f) else colors.borderSubtle,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .testTag("${tagPrefix}_canvas")
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val points = mutableListOf(down.position)
                            currentStroke = points.toList()
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    break
                                }
                                change.consume()
                                points.add(change.position)
                                currentStroke = points.toList()
                            }
                            if (points.isNotEmpty()) {
                                strokes.add(points.toList())
                                currentStroke = emptyList()
                                emitDrawnSignature()
                            }
                        }
                    }
            ) {
                val inkColor = colors.skyAccent
                val guideColor = colors.borderSubtle

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Dotted signature baseline
                    val baselineY = size.height * 0.76f
                    drawLine(
                        color = guideColor,
                        start = Offset(24.dp.toPx(), baselineY),
                        end = Offset(size.width - 24.dp.toPx(), baselineY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    fun drawStroke(pts: List<Offset>) {
                        if (pts.size == 1) {
                            drawCircle(color = inkColor, radius = 2.dp.toPx(), center = pts.first())
                        } else if (pts.size > 1) {
                            val path = Path().apply {
                                moveTo(pts.first().x, pts.first().y)
                                for (i in 1 until pts.size) {
                                    lineTo(pts[i].x, pts[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = inkColor,
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    strokes.forEach { drawStroke(it) }
                    if (currentStroke.isNotEmpty()) {
                        drawStroke(currentStroke)
                    }
                }

                if (!hasDrawing) {
                    Text(
                        text = "Sign here with your finger",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 14.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasDrawing) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = colors.emeraldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Captured: $defaultSignerName",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.emeraldAccent
                            )
                        } else {
                            Text(
                                text = "Signer: $defaultSignerName",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                    }

                    if (hasDrawing) {
                        TextButton(
                            onClick = {
                                strokes.clear()
                                currentStroke = emptyList()
                                onSignatureChanged("")
                            },
                            modifier = Modifier.testTag("${tagPrefix}_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CleaningServices,
                                contentDescription = "Clear signature",
                                tint = colors.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = typedSignature,
                onValueChange = {
                    typedSignature = it
                    onSignatureChanged(it.trim())
                },
                label = { Text("Type Full Name / Initials to Sign") },
                placeholder = { Text(defaultSignerName) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${tagPrefix}_typed_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.skyAccent,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                )
            )

            if (typedSignature.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, colors.emeraldAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "SIGNATURE PREVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = typedSignature,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.8.sp
                            ),
                            color = colors.skyAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SignatureModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalItiColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) colors.skyAccent.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent,
        border = if (selected) BorderStroke(1.dp, colors.skyAccent.copy(alpha = 0.6f)) else null,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.skyAccent else colors.textMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) colors.skyAccent else colors.textMuted
            )
        }
    }
}
