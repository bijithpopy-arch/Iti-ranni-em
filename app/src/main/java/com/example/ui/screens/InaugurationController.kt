package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Locale

data class InaugurationConfig(
    val title: String = "INAUGURATION OF DIGITAL ATTENDANCE SYSTEM ON 01/12/26",
    val subtitle: String = "GOVT. ITI RANNI • ELECTRONICS MECHANIC",
    val startWindow: String = "01-12-2026 10:30",
    val endWindow: String = "01-12-2026 23:59",
    val lockEnabled: Boolean = true,
    val devEarlyUnlock: Boolean = false,
    val fireworksSeconds: Int = 18,
    val devRoll: String = "12",
    val devName: String = "BIJITH S BINU"
)

object InaugurationController {
    private const val PREFS = "iti_inauguration_prefs_v2"
    private const val DEV_PIN = "9037"
    private val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US)

    var config by mutableStateOf(InaugurationConfig())
        private set

    var fireworksActive by mutableStateOf(false)
        private set

    var fireworksSeed by mutableStateOf(0)
        private set

    var devSessionAuthenticated by mutableStateOf(false)
        private set

    fun verifyDevPassword(pin: String): Boolean {
        val ok = pin.trim() == DEV_PIN
        if (ok) devSessionAuthenticated = true
        return ok
    }

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        config = InaugurationConfig(
            title = p.getString("title", "INAUGURATION OF DIGITAL ATTENDANCE SYSTEM ON 01/12/26")
                ?: "INAUGURATION OF DIGITAL ATTENDANCE SYSTEM ON 01/12/26",
            subtitle = p.getString("subtitle", "GOVT. ITI RANNI • ELECTRONICS MECHANIC")
                ?: "GOVT. ITI RANNI • ELECTRONICS MECHANIC",
            startWindow = p.getString("startWindow", "01-12-2026 10:30") ?: "01-12-2026 10:30",
            endWindow = p.getString("endWindow", "01-12-2026 23:59") ?: "01-12-2026 23:59",
            lockEnabled = p.getBoolean("lockEnabled", true),
            devEarlyUnlock = p.getBoolean("devEarlyUnlock", false),
            fireworksSeconds = p.getInt("fireworksSeconds", 18).coerceIn(5, 180),
            devRoll = p.getString("devRoll", "12") ?: "12",
            devName = p.getString("devName", "BIJITH S BINU") ?: "BIJITH S BINU"
        )
    }

    fun save(context: Context, next: InaugurationConfig) {
        val clean = next.copy(fireworksSeconds = next.fireworksSeconds.coerceIn(5, 180))
        config = clean
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("title", clean.title)
            .putString("subtitle", clean.subtitle)
            .putString("startWindow", clean.startWindow)
            .putString("endWindow", clean.endWindow)
            .putBoolean("lockEnabled", clean.lockEnabled)
            .putBoolean("devEarlyUnlock", clean.devEarlyUnlock)
            .putInt("fireworksSeconds", clean.fireworksSeconds)
            .putString("devRoll", clean.devRoll)
            .putString("devName", clean.devName)
            .apply()
    }

    fun openEarlyAcrossApps(context: Context, packageNames: List<String> = emptyList()) {
        save(context, config.copy(devEarlyUnlock = true))
        val pm = context.packageManager
        packageNames.forEach { pkg ->
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(launchIntent) }
            }
        }
    }

    fun triggerFireworks() {
        fireworksSeed += 1
        fireworksActive = true
    }

    fun dismissFireworks() {
        fireworksActive = false
    }

    fun startMillis(): Long? = try { sdf.parse(config.startWindow)?.time } catch (_: Exception) { null }
    fun endMillis(): Long? = try { sdf.parse(config.endWindow)?.time } catch (_: Exception) { null }

    fun isWithinInaugurationWindow(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val s = startMillis() ?: return true
        val e = endMillis() ?: return true
        return nowMillis in s..e
    }

    fun isAppLocked(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!config.lockEnabled) return false
        if (config.devEarlyUnlock) return false
        return !isWithinInaugurationWindow(nowMillis)
    }
}
