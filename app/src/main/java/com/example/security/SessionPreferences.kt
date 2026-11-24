package com.example.security

import android.content.Context
import com.example.data.UserRole
import org.json.JSONObject

data class SavedSessionCredentials(
    val role: UserRole = UserRole.TRAINEE,
    val roll: String = "",
    val pin: String = "",
    val rememberMe: Boolean = false,
    val biometricEnabled: Boolean = false
) {
    val hasCredentials: Boolean
        get() = pin.isNotBlank() && (
            role == UserRole.TRADE_INSTRUCTOR ||
                role == UserRole.PRINCIPAL ||
                roll.isNotBlank()
            )
}

object SessionPreferences {
    private const val PREFS_NAME = "iti_ranni_session_prefs"
    private const val KEY_ROLE = "role"
    private const val KEY_ROLL = "roll"
    private const val KEY_PIN = "pin"
    private const val KEY_REMEMBER = "remember_me"
    private const val KEY_BIOMETRIC = "biometric_enabled"
    private const val KEY_APP_CONFIG_JSON = "remote_app_config_json"
    private const val KEY_UI_CONFIG_JSON = "remote_ui_config_json"

    fun save(
        context: Context,
        role: UserRole,
        roll: String,
        pin: String,
        rememberMe: Boolean,
        biometricEnabled: Boolean
    ) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ROLE, role.name)
            .putString(KEY_ROLL, roll.trim())
            .putString(KEY_PIN, pin.trim())
            .putBoolean(KEY_REMEMBER, rememberMe)
            .putBoolean(KEY_BIOMETRIC, biometricEnabled)
            .apply()
    }

    fun load(context: Context): SavedSessionCredentials {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val roleName = prefs.getString(KEY_ROLE, UserRole.TRAINEE.name)
        val role = UserRole.entries.firstOrNull { it.name == roleName } ?: UserRole.TRAINEE
        return SavedSessionCredentials(
            role = role,
            roll = prefs.getString(KEY_ROLL, "").orEmpty(),
            pin = prefs.getString(KEY_PIN, "").orEmpty(),
            rememberMe = prefs.getBoolean(KEY_REMEMBER, false),
            biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC, false)
        )
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_ROLE)
            .remove(KEY_ROLL)
            .remove(KEY_PIN)
            .remove(KEY_REMEMBER)
            .remove(KEY_BIOMETRIC)
            .apply()
    }

    fun saveLocalRemoteAppConfig(context: Context, map: Map<String, String>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_APP_CONFIG_JSON, obj.toString())
            .apply()
    }

    fun loadLocalRemoteAppConfig(context: Context): Map<String, String> {
        val raw = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APP_CONFIG_JSON, null) ?: return emptyMap()
        return jsonToMap(raw)
    }

    fun saveLocalRemoteUiConfig(context: Context, map: Map<String, String>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_UI_CONFIG_JSON, obj.toString())
            .apply()
    }

    fun loadLocalRemoteUiConfig(context: Context): Map<String, String> {
        val raw = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_UI_CONFIG_JSON, null) ?: return emptyMap()
        return jsonToMap(raw)
    }

    private fun jsonToMap(raw: String): Map<String, String> {
        return try {
            val obj = JSONObject(raw)
            val map = linkedMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optString(k, "")
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
