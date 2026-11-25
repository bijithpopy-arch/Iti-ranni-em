package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val DevBg = Color(0xFF050A08)
private val DevPanel = Color(0xFF0B1612)
private val DevTrace = Color(0xFF173027)
private val DevGreen = Color(0xFF00FF88)
private val DevAmber = Color(0xFFF59E0B)
private val DevRed = Color(0xFFFF3B5C)
private val DevCyan = Color(0xFF38BDF8)
private val DevWhite = Color(0xFFE8F6F0)
private val DevMuted = Color(0xFF7FA394)
private val DevMono = FontFamily.Monospace

private fun devStyle(size: Int, weight: FontWeight = FontWeight.Normal, color: Color = DevWhite) =
    TextStyle(fontFamily = DevMono, fontWeight = weight, fontSize = size.sp, color = color, fontFeatureSettings = "tnum")

private fun formatCountdown(remainingMillis: Long): String {
    val totalSec = (remainingMillis / 1000L).coerceAtLeast(0L)
    val days = totalSec / 86400L
    val hours = (totalSec % 86400L) / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%02dD : %02dH : %02dM : %02dS", days, hours, mins, secs)
}

@Composable
fun InaugurationFireworksOverlay() {
    if (!InaugurationController.fireworksActive) return
    val cfg = InaugurationController.config
    val seed = InaugurationController.fireworksSeed
    val durationMs = (cfg.fireworksSeconds.coerceIn(5, 180)) * 1000L
    val progress = remember(seed, durationMs) { Animatable(0f) }
    var elapsedSec by remember(seed) { mutableIntStateOf(0) }

    LaunchedEffect(seed, durationMs) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = durationMs.toInt(), easing = LinearEasing))
        InaugurationController.dismissFireworks()
    }

    LaunchedEffect(seed, durationMs) {
        val start = System.currentTimeMillis()
        while (InaugurationController.fireworksActive) {
            elapsedSec = (((System.currentTimeMillis() - start) / 1000L).toInt()).coerceAtMost(cfg.fireworksSeconds)
            delay(250L)
        }
    }

    val bursts = remember(seed) {
        val rnd = Random(seed * 9973 + 17)
        val palette = listOf(DevGreen, DevAmber, DevCyan, DevRed, Color(0xFFE879F9), Color(0xFFFACC15))
        List(16) { idx ->
            Triple(
                Offset(0.16f + rnd.nextFloat() * 0.68f, 0.14f + rnd.nextFloat() * 0.45f),
                idx / 16f,
                palette[idx % palette.size]
            )
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xEE040807))
            .clickable { InaugurationController.dismissFireworks() },
        contentAlignment = Alignment.Center
    ) {
        val t = progress.value
        Canvas(Modifier.fillMaxSize()) {
            bursts.forEachIndexed { bIdx, (normCenter, phaseStart, col) ->
                val raw = (t - phaseStart) * 5.5f
                val localT = (raw - raw.toInt().toFloat()).coerceIn(0f, 1f)
                val cx = normCenter.x * size.width
                val cy = normCenter.y * size.height
                val maxR = size.minDimension * 0.36f * localT
                val alpha = (1f - localT).coerceIn(0f, 1f)
                drawCircle(col.copy(alpha = alpha * 0.35f), radius = maxR * 0.65f, center = Offset(cx, cy), style = Stroke(width = 2.dp.toPx()))
                val spokes = 28
                for (i in 0 until spokes) {
                    val angle = (2.0 * Math.PI * i / spokes) + (bIdx * 0.25)
                    val r = maxR * (0.45f + 0.55f * ((i % 3) / 2f))
                    val px = cx + (cos(angle) * r).toFloat()
                    val py = cy + (sin(angle) * r).toFloat() + (localT * localT * 38.dp.toPx())
                    drawCircle(col.copy(alpha = alpha), radius = (3.5f * alpha + 1f).dp.toPx(), center = Offset(px, py))
                }
            }
        }
        Column(
            Modifier
                .padding(22.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DevPanel.copy(alpha = 0.92f))
                .border(1.5.dp, DevGreen, RoundedCornerShape(14.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("★ OFFICIAL LAUNCH  •  ${elapsedSec}S / ${cfg.fireworksSeconds}S ★", style = devStyle(10, FontWeight.ExtraBold, DevAmber))
            Spacer(Modifier.height(10.dp))
            Text(cfg.title.uppercase(), style = devStyle(20, FontWeight.ExtraBold, DevGreen), textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(cfg.subtitle.uppercase(), style = devStyle(12, FontWeight.SemiBold, DevWhite), textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Text("TAP ANYWHERE TO DISMISS", style = devStyle(10, FontWeight.Bold, DevMuted))
        }
    }
}

@Composable
fun InaugurationLockGate(onOpenDevConsole: () -> Unit) {
    val cfg = InaugurationController.config
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var autoTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(cfg.startWindow) {
        autoTriggered = false
        while (true) {
            nowMs = System.currentTimeMillis()
            val target = InaugurationController.startMillis()
            if (target != null && nowMs >= target && !autoTriggered && cfg.lockEnabled && !cfg.devEarlyUnlock) {
                autoTriggered = true
                InaugurationController.triggerFireworks()
            }
            delay(500L)
        }
    }

    val targetMs = InaugurationController.startMillis()
    val remMs = if (targetMs != null) (targetMs - nowMs).coerceAtLeast(0L) else 0L

    Box(Modifier.fillMaxSize().background(DevBg).padding(20.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DevPanel)
                .border(1.2.dp, DevAmber, RoundedCornerShape(12.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.clip(CircleShape).background(DevRed.copy(alpha = 0.2f)).border(1.dp, DevRed, CircleShape).padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text("APP LOCKED // INAUGURATION COUNTDOWN", style = devStyle(10, FontWeight.ExtraBold, DevRed))
            }
            Text(cfg.title.uppercase(), style = devStyle(17, FontWeight.ExtraBold, DevWhite), textAlign = TextAlign.Center)
            Text(cfg.subtitle.uppercase(), style = devStyle(11, FontWeight.Medium, DevMuted), textAlign = TextAlign.Center)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(DevBg).border(1.dp, DevGreen.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LAUNCH COUNTDOWN", style = devStyle(9, FontWeight.Bold, DevAmber))
                Spacer(Modifier.height(6.dp))
                Text(formatCountdown(remMs), style = devStyle(17, FontWeight.ExtraBold, DevGreen))
                Spacer(Modifier.height(4.dp))
                Text("WINDOW: ${cfg.startWindow}  ->  ${cfg.endWindow}", style = devStyle(9, FontWeight.Normal, DevMuted))
            }
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(DevBg)
                    .border(1.dp, DevCyan, CircleShape)
                    .clickable(onClick = onOpenDevConsole)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text("DEV LOGIN (ROLL ${cfg.devRoll} // ${cfg.devName})", style = devStyle(10, FontWeight.ExtraBold, DevCyan))
            }
        }
    }
}

@Composable
fun DevInaugurationConsoleDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val cur = InaugurationController.config
    var unlocked by remember { mutableStateOf(InaugurationController.devSessionAuthenticated) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf(cur.title) }
    var subtitle by remember { mutableStateOf(cur.subtitle) }
    var startWin by remember { mutableStateOf(cur.startWindow) }
    var endWin by remember { mutableStateOf(cur.endWindow) }
    var fwSecStr by remember { mutableStateOf(cur.fireworksSeconds.toString()) }
    var lockOn by remember { mutableStateOf(cur.lockEnabled) }
    var earlyUnlock by remember { mutableStateOf(cur.devEarlyUnlock) }

    Dialog(onDismissRequest = onDismiss) {
        if (!unlocked) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DevPanel)
                    .border(1.2.dp, DevCyan, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("DEV AUTH // ROLL ${cur.devRoll} (${cur.devName})", style = devStyle(11, FontWeight.ExtraBold, DevCyan))
                Text("ENTER DEV PASSCODE TO UNLOCK CONSOLE", style = devStyle(10, FontWeight.Normal, DevMuted))
                DevField("PASSCODE", pinInput) { pinInput = it; pinError = false }
                if (pinError) {
                    Text("INVALID PASSCODE", style = devStyle(10, FontWeight.Bold, DevRed))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    DevPill("CANCEL", false, DevMuted, Modifier.weight(1f), onDismiss)
                    Spacer(Modifier.width(8.dp))
                    DevPill("UNLOCK DEV", true, DevCyan, Modifier.weight(1f)) {
                        if (InaugurationController.verifyDevPassword(pinInput)) {
                            unlocked = true
                        } else {
                            pinError = true
                        }
                    }
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DevPanel)
                    .border(1.2.dp, DevGreen, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("DEV BENCH // ${cur.devName} (#${cur.devRoll})", style = devStyle(11, FontWeight.ExtraBold, DevGreen))
                    Box(Modifier.clip(CircleShape).background(if (lockOn && !earlyUnlock) DevRed.copy(alpha = 0.2f) else DevGreen.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(if (lockOn && !earlyUnlock) "LOCKED" else "ACTIVE", style = devStyle(9, FontWeight.Bold, if (lockOn && !earlyUnlock) DevRed else DevGreen))
                    }
                }
                DevField("INAUGURATION HEADLINE", title) { title = it }
                DevField("SUBTITLE / LAB TAG", subtitle) { subtitle = it }
                DevField("START TIME (DD-MM-YYYY HH:MM)", startWin) { startWin = it }
                DevField("END TIME (DD-MM-YYYY HH:MM)", endWin) { endWin = it }
                DevField("FIREWORKS DURATION (SEC, 5-180)", fwSecStr) { fwSecStr = it }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DevPill(if (lockOn) "LOCK: ON" else "LOCK: OFF", lockOn, DevRed, Modifier.weight(1f)) { lockOn = !lockOn }
                    DevPill(if (earlyUnlock) "EARLY UNLOCK: ON" else "EARLY UNLOCK: OFF", earlyUnlock, DevCyan, Modifier.weight(1f)) { earlyUnlock = !earlyUnlock }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DevPill("FIREWORKS", true, DevAmber, Modifier.weight(1f)) {
                        val sec = fwSecStr.toIntOrNull() ?: cur.fireworksSeconds
                        InaugurationController.save(context, cur.copy(title = title, subtitle = subtitle, startWindow = startWin, endWindow = endWin, fireworksSeconds = sec, lockEnabled = lockOn, devEarlyUnlock = earlyUnlock))
                        onDismiss()
                        InaugurationController.triggerFireworks()
                    }
                    DevPill("SAVE & APPLY", true, DevGreen, Modifier.weight(1f)) {
                        val sec = fwSecStr.toIntOrNull() ?: cur.fireworksSeconds
                        InaugurationController.save(context, cur.copy(title = title, subtitle = subtitle, startWindow = startWin, endWindow = endWin, fireworksSeconds = sec, lockEnabled = lockOn, devEarlyUnlock = earlyUnlock))
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun DevField(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, style = devStyle(9, FontWeight.Bold, DevMuted))
        Spacer(Modifier.height(3.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = devStyle(12, FontWeight.Bold, DevWhite),
            cursorBrush = SolidColor(DevGreen),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(DevBg).border(1.dp, DevTrace, RoundedCornerShape(6.dp)).padding(8.dp)
        )
    }
}

@Composable
private fun DevPill(label: String, active: Boolean, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(CircleShape).background(if (active) color.copy(alpha = 0.22f) else DevBg).border(1.dp, if (active) color else DevTrace, CircleShape).clickable(onClick = onClick).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = devStyle(9, FontWeight.ExtraBold, if (active) color else DevWhite), maxLines = 1)
    }
}
