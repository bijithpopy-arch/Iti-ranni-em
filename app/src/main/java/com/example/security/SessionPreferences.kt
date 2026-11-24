package com.example.security

import android.content.Context
import com.example.data.UserRole

data class SavedSession(
    val rememberMe: Boolean = false,
    val biometricEnabled: Boolean = false,
    val role: UserRole = UserRole.TRAINEE,
    val roll: String = "",
    val pin: String = ""
) {
    val hasCredentials: Boolean
        get() = rememberMe && pin.isNotBlank() && (
            role == UserRole.TRADE_INSTRUCTOR ||
                role == UserRole.PRINCIPAL ||
                roll.isNotBlank()
            )
}

object SessionPreferences {
    private const val PREFS_NAME = "iti_ranni_session_prefs"
    private const val KEY_REMEMBER = "remember_me"
    private const val KEY_BIOMETRIC = "biometric_enabled"
    private const val KEY_ROLE = "saved_role"
    private const val KEY_ROLL = "saved_roll"
    private const val KEY_PIN = "saved_pin"

    fun save(
        context: Context,
        rememberMe: Boolean,
        biometricEnabled: Boolean,
        role: UserRole,
        roll: String,
        pin: String
    ) {
        val editor = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        if (!rememberMe) {
            editor.clear().apply()
            return
        }
        editor.putBoolean(KEY_REMEMBER, true)
            .putBoolean(KEY_BIOMETRIC, biometricEnabled)
            .putString(KEY_ROLE, role.name)
            .putString(KEY_ROLL, roll.trim())
            .putString(KEY_PIN, pin.trim())
            .apply()
    }

    fun load(context: Context): SavedSession {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val remember = prefs.getBoolean(KEY_REMEMBER, false)
        val biometric = prefs.getBoolean(KEY_BIOMETRIC, false)
        val roleStr = prefs.getString(KEY_ROLE, UserRole.TRAINEE.name)
        val role = UserRole.entries.firstOrNull { it.name == roleStr } ?: UserRole.TRAINEE
        val roll = prefs.getString(KEY_ROLL, "").orEmpty()
        val pin = prefs.getString(KEY_PIN, "").orEmpty()
        return SavedSession(remember, biometric, role, roll, pin)
    }

    fun clearAll(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
