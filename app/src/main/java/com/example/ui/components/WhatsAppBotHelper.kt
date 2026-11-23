package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Launches pre-filled WhatsApp Bot messages using https://wa.me/<number>?text=<encoded_message>
 * via Intent.ACTION_VIEW with configurable bot phone number and graceful fallback handling.
 */
object WhatsAppBotHelper {

    private const val PREFS_NAME = "iti_ranni_whatsapp_bot"
    private const val KEY_BOT_NUMBER = "bot_phone_number"
    private const val DEFAULT_BOT_NUMBER = "919447000000"

    fun getBotPhoneNumber(context: Context): String {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BOT_NUMBER, DEFAULT_BOT_NUMBER)
            .orEmpty()
        return sanitizePhoneDigits(saved).ifBlank { DEFAULT_BOT_NUMBER }
    }

    fun setBotPhoneNumber(context: Context, phoneNumber: String): String {
        val clean = sanitizePhoneDigits(phoneNumber).ifBlank { DEFAULT_BOT_NUMBER }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BOT_NUMBER, clean)
            .apply()
        return clean
    }

    fun sanitizePhoneDigits(raw: String): String {
        return raw.filter { it.isDigit() }
    }

    fun buildWhatsAppUrl(phoneNumber: String, message: String): String {
        val cleanDigits = sanitizePhoneDigits(phoneNumber).ifBlank { DEFAULT_BOT_NUMBER }
        val encodedText = URLEncoder.encode(message.trim(), StandardCharsets.UTF_8.name())
            .replace("+", "%20")
        return "https://wa.me/$cleanDigits?text=$encodedText"
    }

    /**
     * Opens WhatsApp (or browser fallback) with a pre-filled bot message.
     * Shows a safe user-facing Toast message if WhatsApp or a web handler is unavailable.
     */
    fun openWhatsAppBot(
        context: Context,
        message: String,
        overridePhoneNumber: String? = null
    ): Boolean {
        val targetNumber = overridePhoneNumber?.takeIf { sanitizePhoneDigits(it).isNotBlank() }
            ?: getBotPhoneNumber(context)
        val url = buildWhatsAppUrl(targetNumber, message)
        val uri = Uri.parse(url)

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "WhatsApp or browser not found on this device. Bot link: wa.me/$targetNumber",
                Toast.LENGTH_LONG
            ).show()
            false
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Unable to open WhatsApp right now. Please check WhatsApp installation.",
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }
}
