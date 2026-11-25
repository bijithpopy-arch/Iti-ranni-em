package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.StaffAttendanceRecord
import com.example.pdf.AttendancePdfExporter
import com.example.pdf.AttendancePdfMode
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PcbMaskBg = Color(0xFF060B09)
private val PcbCardBg = Color(0xFF0C1512)
private val PcbTrace = Color(0xFF142821)
private val CopperPad = Color(0xFFD97706)
private val CopperRim = Color(0xFFF59E0B)
private val ScopeGreen = Color(0xFF00FF88)
private val FaultRed = Color(0xFFFF3B5C)
private val SilkWhite = Color(0xFFE6F4EE)
private val SilkMuted = Color(0xFF7FA394)
private val MonoFont = FontFamily.Monospace

private fun pcbStyle(size: Int, weight: FontWeight = FontWeight.Normal, color: Color = SilkWhite, spacing: Float = 0.2f) =
    TextStyle(fontFamily = MonoFont, fontWeight = weight, fontSize = size.sp, color = color, letterSpacing = spacing.sp, fontFeatureSettings = "tnum")

private fun Modifier.pcbModule(accent: Color = CopperPad, fill: Color = PcbCardBg): Modifier =
    this.clip(RoundedCornerShape(8.dp)).background(fill).border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(8.dp))

@Composable
fun InstructorGlassScreen1(records: List<StaffAttendanceRecord>, onManualPunch: (roll: String, status: String) -> Unit, onUndoAttendance: (roll: String) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("ALL") }
    var isMl by remember { mutableStateOf(false) }
    val overrides = remember { mutableStateMapOf<String, Pair<Double, Double>>() }
    var editTarget by remember { mutableStateOf<StaffAttendanceRecord?>(null) }
    var editAllOpen by remember { mutableStateOf(false) }
    var pdfModalOpen by remember { mutableStateOf(false) }
    var devConsoleOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { InaugurationController.load(context) }
    var lastRoll by remember { mutableStateOf<String?>(null) }
    var timeStr by remember { mutableStateOf(currentTimeText()) }
    var dateStr by remember { mutableStateOf(currentDateText()) }

    LaunchedEffect(Unit) { while (true) { timeStr = currentTimeText(); dateStr = currentDateText(); delay(1000L) } }

    val presentCount = records.count { val s = it.todayStatus.uppercase(Locale.US); s == "PRESENT" || s.contains("HALF") || s == "LATE" || s == "ON-DUTY" }
    val shortCount = records.count { val ov = overrides[it.roll]; val p = ov?.first ?: (it.present + (it.half * 0.5)); val t = ov?.second ?: it.total.toDouble(); t > 0 && (p / t) * 100.0 < 80.0 }

    val filtered = remember(records, query, filterMode, overrides.toMap()) {
        records.filter { rec ->
            val matchesQuery = query.isBlank() || rec.name.contains(query, true) || rec.roll.contains(query)
            val st = rec.todayStatus.uppercase(Locale.US)
            val ov = overrides[rec.roll]
            val p = ov?.first ?: (rec.present + (rec.half * 0.5))
            val t = ov?.second ?: rec.total.toDouble()
            val pct = if (t > 0.0) (p / t) * 100.0 else rec.pct.toDouble()
            val matchesFilter = when (filterMode) {
                "HIGH" -> st == "PRESENT" || st.contains("HALF") || st == "LATE" || st == "ON-DUTY"
                "LOW" -> st == "ABSENT"
                "FAULT" -> pct < 80.0
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    Box(Modifier.fillMaxSize().background(PcbMaskBg)) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 36.dp.toPx()
            var y = step
            while (y < size.height) {
                val path = Path().apply {
                    moveTo(0f, y); lineTo(size.width * 0.28f, y); lineTo(size.width * 0.36f, y - 14.dp.toPx()); lineTo(size.width, y - 14.dp.toPx())
                }
                drawPath(path, PcbTrace, style = Stroke(width = 1.4.dp.toPx()))
                drawCircle(PcbTrace, 2.6.dp.toPx(), Offset(size.width * 0.28f, y))
                y += step
            }
        }
        LazyColumn(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Column(Modifier.fillMaxWidth().pcbModule(CopperRim).padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(ScopeGreen))
                                Spacer(Modifier.width(6.dp))
                                Text("U1 // EM-23  •  $dateStr", style = pcbStyle(10, FontWeight.Bold, SilkMuted, 0.3f), maxLines = 1)
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(if (isMl) "ഇലക്ട്രോണിക്സ് ഹാജർ" else "EM BENCH REGISTER", style = pcbStyle(15, FontWeight.ExtraBold, SilkWhite, 0.3f))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Box(Modifier.clip(CircleShape).background(PcbMaskBg).border(1.dp, ScopeGreen.copy(alpha = 0.7f), CircleShape).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(timeStr, style = pcbStyle(12, FontWeight.ExtraBold, ScopeGreen, 0.4f))
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                OvalKey(if (isMl) "EN" else "ML", false) { isMl = !isMl }
                                OvalKey("ALL", false) { editAllOpen = true }
                                OvalKey("UNDO", false) { lastRoll?.let(onUndoAttendance) ?: records.firstOrNull()?.let { onUndoAttendance(it.roll) } }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScopeChannel("CH1 // PRESENT", "$presentCount / ${records.size}", ScopeGreen, Modifier.weight(1f))
                        ScopeChannel("CH2 // FAULT <80%", "$shortCount", if (shortCount > 0) FaultRed else ScopeGreen, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OvalKey("ALL (${records.size})", filterMode == "ALL", ScopeGreen) { filterMode = "ALL" }
                        OvalKey("PRESENT", filterMode == "HIGH", ScopeGreen) { filterMode = "HIGH" }
                        OvalKey("ABSENT", filterMode == "LOW", FaultRed) { filterMode = "LOW" }
                        OvalKey("<80%", filterMode == "FAULT", CopperRim) { filterMode = "FAULT" }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().pcbModule(PcbTrace).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("PROBE> ", style = pcbStyle(11, FontWeight.Bold, ScopeGreen))
                    BasicTextField(value = query, onValueChange = { query = it }, singleLine = true, textStyle = pcbStyle(13, FontWeight.SemiBold, SilkWhite), cursorBrush = SolidColor(ScopeGreen), modifier = Modifier.weight(1f),
                        decorationBox = { inner -> if (query.isEmpty()) Text(if (isMl) "പേര് അല്ലെങ്കിൽ റോൾ..." else "PIN OR TRAINEE NAME...", style = pcbStyle(12, FontWeight.Normal, SilkMuted)); inner() })
                    if (query.isNotEmpty()) Text("RST", style = pcbStyle(11, FontWeight.Bold, CopperRim), modifier = Modifier.clickable { query = "" })
                }
            }
            items(filtered, key = { it.roll }) { rec ->
                val ov = overrides[rec.roll]
                val pres = ov?.first ?: (rec.present + (rec.half * 0.5))
                val tot = ov?.second ?: rec.total.toDouble()
                val pct = if (tot > 0.0) ((pres / tot) * 100.0).coerceIn(0.0, 100.0) else rec.pct.toDouble()
                val (zoneTag, zoneColor) = when {
                    pct >= 83.0 -> "SAFE" to ScopeGreen
                    pct >= 80.0 -> "WARN" to CopperRim
                    else -> "SHOP FLOOR" to FaultRed
                }
                val st = rec.todayStatus.uppercase(Locale.US)
                val ledColor = when {
                    st == "PRESENT" -> ScopeGreen
                    st.contains("HALF") -> CopperRim
                    st == "ABSENT" -> FaultRed
                    else -> SilkMuted
                }
                Column(Modifier.fillMaxWidth().pcbModule(zoneColor, PcbCardBg).padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(ledColor))
                            Spacer(Modifier.width(6.dp))
                            Text("P${rec.roll.padStart(2, '0')}", style = pcbStyle(14, FontWeight.ExtraBold, zoneColor))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(rec.name.uppercase(Locale.US), style = pcbStyle(13, FontWeight.Bold, SilkWhite), maxLines = 1)
                            Spacer(Modifier.height(2.dp))
                            Text(String.format(Locale.US, "%.1f/%.0f  •  %.1f%%  •  %s", pres, tot, pct, st), style = pcbStyle(10, FontWeight.SemiBold, SilkMuted), maxLines = 1)
                        }
                        Box(Modifier.clip(CircleShape).background(zoneColor.copy(alpha = 0.16f)).border(1.dp, zoneColor.copy(alpha = 0.7f), CircleShape).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(zoneTag, style = pcbStyle(9, FontWeight.ExtraBold, zoneColor))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(PcbTrace)) {
                        Box(Modifier.fillMaxWidth((pct / 100.0).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(zoneColor))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        FullOvalButton("PRESENT", st == "PRESENT", ScopeGreen, Modifier.weight(1.15f)) { lastRoll = rec.roll; onManualPunch(rec.roll, "PRESENT") }
                        FullOvalButton("HALF DAY", st.contains("HALF"), CopperRim, Modifier.weight(1.2f)) { lastRoll = rec.roll; onManualPunch(rec.roll, "HALF-DAY") }
                        FullOvalButton("ABSENT", st == "ABSENT", FaultRed, Modifier.weight(1.05f)) { lastRoll = rec.roll; onManualPunch(rec.roll, "ABSENT") }
                        FullOvalButton("EDIT", false, SilkWhite, Modifier.weight(0.85f)) { editTarget = rec }
                    }
                }
            }
        }
                if (pdfModalOpen) {
                    var customRangeOpen by remember { mutableStateOf(false) }
                    val defaultDate = remember { SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date()) }
                    var fromDateStr by remember { mutableStateOf(defaultDate) }
                    var toDateStr by remember { mutableStateOf(defaultDate) }
                    Dialog(onDismissRequest = { pdfModalOpen = false }) {
                        Column(Modifier.fillMaxWidth().pcbModule(CopperRim).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("EXPORT REGISTER PDF", style = pcbStyle(12, FontWeight.Bold, SilkWhite))
                            Text("OUTPUT -> DOWNLOADS FOLDER", style = pcbStyle(10, FontWeight.Normal, SilkMuted))
                            listOf(
                                AttendancePdfMode.TODAY to "TODAY'S ATTENDANCE",
                                AttendancePdfMode.TILL_DATE to "TILL DATE (CUMULATIVE)",
                                AttendancePdfMode.MONTH to "CURRENT MONTH SHEET"
                            ).forEach { (mode, label) ->
                                Box(Modifier.fillMaxWidth().clip(CircleShape).background(PcbMaskBg).border(1.dp, PcbTrace, CircleShape).clickable {
                                    AttendancePdfExporter.export(context, records, overrides, mode)
                                    pdfModalOpen = false
                                }.padding(horizontal = 14.dp, vertical = 11.dp)) { Text(label, style = pcbStyle(12, FontWeight.Bold, ScopeGreen)) }
                            }
                            Box(Modifier.fillMaxWidth().clip(CircleShape).background(PcbMaskBg).border(1.dp, if (customRangeOpen) CopperRim else PcbTrace, CircleShape).clickable {
                                customRangeOpen = !customRangeOpen
                            }.padding(horizontal = 14.dp, vertical = 11.dp)) { Text("CUSTOM DATE RANGE (FROM - TO)", style = pcbStyle(12, FontWeight.Bold, CopperRim)) }
                            if (customRangeOpen) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text("FROM (DD-MM-YYYY)", style = pcbStyle(9, FontWeight.Bold, SilkMuted)); Spacer(Modifier.height(4.dp))
                                        BasicTextField(value = fromDateStr, onValueChange = { fromDateStr = it }, singleLine = true, textStyle = pcbStyle(12, FontWeight.Bold, SilkWhite), cursorBrush = SolidColor(ScopeGreen), modifier = Modifier.fillMaxWidth().pcbModule(PcbTrace, PcbMaskBg).padding(9.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text("TO (DD-MM-YYYY)", style = pcbStyle(9, FontWeight.Bold, SilkMuted)); Spacer(Modifier.height(4.dp))
                                        BasicTextField(value = toDateStr, onValueChange = { toDateStr = it }, singleLine = true, textStyle = pcbStyle(12, FontWeight.Bold, SilkWhite), cursorBrush = SolidColor(ScopeGreen), modifier = Modifier.fillMaxWidth().pcbModule(PcbTrace, PcbMaskBg).padding(9.dp))
                                    }
                                }
                                Box(Modifier.fillMaxWidth().clip(CircleShape).background(ScopeGreen.copy(alpha = 0.22f)).border(1.dp, ScopeGreen, CircleShape).clickable {
                                    AttendancePdfExporter.export(context, records, overrides, AttendancePdfMode.CUSTOM_RANGE, fromDateStr, toDateStr)
                                    pdfModalOpen = false
                                }.padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.Center) { Text("EXPORT RANGE PDF", style = pcbStyle(11, FontWeight.ExtraBold, ScopeGreen)) }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { OvalKey("CLOSE", false) { pdfModalOpen = false } }
                        }
                    }
                }
        editTarget?.let { target ->
            val ov = overrides[target.roll] ?: ((target.present + (target.half * 0.5)) to target.total.toDouble())
            PcbEditDialog("PIN ${target.roll} // ${target.name.uppercase(Locale.US)}", ov.first.toString(), ov.second.toString(), { editTarget = null }) { p, t -> overrides[target.roll] = p to t; editTarget = null }
        }
    }
}

private fun currentTimeText(): String = SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date()).uppercase(Locale.US)
private fun currentDateText(): String = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date()).uppercase(Locale.US)

@Composable
private fun ScopeChannel(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier.pcbModule(PcbTrace, PcbMaskBg).padding(horizontal = 10.dp, vertical = 7.dp)) {
        Text(label, style = pcbStyle(9, FontWeight.Bold, SilkMuted))
        Text(value, style = pcbStyle(16, FontWeight.ExtraBold, accent))
    }
}

@Composable
private fun OvalKey(label: String, active: Boolean, activeColor: Color = CopperRim, onClick: () -> Unit) {
    Box(Modifier.clip(CircleShape).background(if (active) activeColor.copy(alpha = 0.22f) else PcbMaskBg).border(1.dp, if (active) activeColor else PcbTrace, CircleShape).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(label, style = pcbStyle(10, FontWeight.Bold, if (active) activeColor else SilkWhite), maxLines = 1)
    }
}

@Composable
private fun FullOvalButton(label: String, active: Boolean, activeColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.clip(CircleShape).background(if (active) activeColor.copy(alpha = 0.24f) else PcbMaskBg).border(1.1.dp, if (active) activeColor else PcbTrace, CircleShape).clickable(onClick = onClick).padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
        Text(label, style = pcbStyle(10, FontWeight.ExtraBold, if (active) activeColor else SilkWhite, 0.1f), maxLines = 1)
    }
}

@Composable
private fun PcbEditDialog(title: String, initPres: String, initTot: String, onDismiss: () -> Unit, onSave: (Double, Double) -> Unit) {
    var pStr by remember { mutableStateOf(initPres) }; var tStr by remember { mutableStateOf(initTot) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().pcbModule(CopperRim).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = pcbStyle(12, FontWeight.Bold, SilkWhite))
            PcbInput("PRESENT DAYS", pStr) { pStr = it }; PcbInput("TOTAL DAYS", tStr) { tStr = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OvalKey("CANCEL", false, onClick = onDismiss); Spacer(Modifier.width(8.dp))
                OvalKey("APPLY", true, ScopeGreen) { val p = pStr.toDoubleOrNull() ?: 0.0; val t = tStr.toDoubleOrNull() ?: 0.0; if (t >= 0) onSave(p, t) }
            }
        }
    }
}

@Composable
private fun PcbInput(label: String, value: String, onChange: (String) -> Unit) {
    Column {
        Text(label, style = pcbStyle(9, FontWeight.Bold, SilkMuted)); Spacer(Modifier.height(4.dp))
        BasicTextField(value = value, onValueChange = onChange, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textStyle = pcbStyle(14, FontWeight.Bold, SilkWhite), cursorBrush = SolidColor(ScopeGreen), modifier = Modifier.fillMaxWidth().pcbModule(PcbTrace, PcbMaskBg).padding(10.dp))
    }
}
