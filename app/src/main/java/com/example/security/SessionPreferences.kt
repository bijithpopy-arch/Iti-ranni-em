package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.UserRole

data class SavedLoginSession(
    val rememberMe: Boolean = false,
    val biometricEnabled: Boolean = false,
    val role: UserRole = UserRole.TRAINEE,
    val roll: String = "",
    val pin: String = ""
) {
    val hasCredentials: Boolean
        get() = rememberMe && pin.isNotBlank() && (
            (role == UserRole.TRADE_INSTRUCTOR || role == UserRole.PRINCIPAL) || roll.isNotBlank()
        )
}

object SessionPreferences {
    private const val PREFS_NAME = "iti_ranni_remember_login"
    private const val KEY_REMEMBER_ME = "remember_me"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_ROLE = "saved_role"
    private const val KEY_ROLL = "saved_roll"
    private const val KEY_PIN = "saved_pin"

    fun load(context: Context): SavedLoginSession {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val roleName = prefs.getString(KEY_ROLE, UserRole.TRAINEE.name) ?: UserRole.TRAINEE.name
        val role = UserRole.entries.firstOrNull { it.name == roleName } ?: UserRole.TRAINEE
        return SavedLoginSession(
            rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false),
            biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false),
            role = role,
            roll = prefs.getString(KEY_ROLL, "").orEmpty(),
            pin = prefs.getString(KEY_PIN, "").orEmpty()
        )
    }

    fun save(
        context: Context,
        rememberMe: Boolean,
        biometricEnabled: Boolean,
        role: UserRole,
        roll: String,
        pin: String
    ) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        if (!rememberMe) {
            editor
                .putBoolean(KEY_REMEMBER_ME, false)
                .putBoolean(KEY_BIOMETRIC_ENABLED, false)
                .remove(KEY_ROLL)
                .remove(KEY_PIN)
                .apply()
        } else {
            editor
                .putBoolean(KEY_REMEMBER_ME, true)
                .putBoolean(KEY_BIOMETRIC_ENABLED, biometricEnabled)
                .putString(KEY_ROLE, role.name)
                .putString(KEY_ROLL, roll.trim())
                .putString(KEY_PIN, pin.trim())
                .apply()
        }
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

object BiometricAuthHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val status = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showFingerprintPrompt(
        activity: FragmentActivity,
        roleLabel: String,
        onAuthenticated: () -> Unit,
        onFallbackToPassword: (String?) -> Unit
    ) {
        if (!isBiometricAvailable(activity)) {
            onFallbackToPassword("Fingerprint sensor not enrolled. Use saved PIN login.")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onAuthenticated()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                val isUserCancel = errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED
                onFallbackToPassword(if (isUserCancel) null else errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // System keeps prompt open for retry
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Govt. ITI Ranni")
            .setSubtitle("Verify fingerprint to sign in as $roleLabel")
            .setNegativeButtonText("Use PIN Instead")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}
