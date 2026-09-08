package com.shohan.khatago.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "khatago_settings")

/** Settings snapshot included in a backup file. */
@Serializable
data class AppSettingsSnapshot(
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val remindersEnabled: Boolean = true,
    val reminderDaysBefore: Int = 2,
    val pinSalt: String? = null,
    val pinHash: String? = null
)

/** Live app settings. */
data class AppSettings(
    val appLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val remindersEnabled: Boolean = true,
    val reminderDaysBefore: Int = 2,
    val hasPin: Boolean = false,
    val lastBackupAt: Long? = null
)

/**
 * App preferences stored locally with DataStore.
 *
 * The PIN is never stored: only a SHA-256 hash with a per-install random salt is
 * kept, and the lock can additionally be opened with the device biometric.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val APP_LOCK = booleanPreferencesKey("app_lock_enabled")
        val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        val REMINDERS = booleanPreferencesKey("reminders_enabled")
        val REMINDER_DAYS = intPreferencesKey("reminder_days_before")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val LAST_BACKUP = longPreferencesKey("last_backup_at")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            appLockEnabled = prefs[Keys.APP_LOCK] ?: false,
            biometricEnabled = prefs[Keys.BIOMETRIC] ?: false,
            remindersEnabled = prefs[Keys.REMINDERS] ?: true,
            reminderDaysBefore = prefs[Keys.REMINDER_DAYS] ?: 2,
            hasPin = !prefs[Keys.PIN_HASH].isNullOrEmpty(),
            lastBackupAt = prefs[Keys.LAST_BACKUP]
        )
    }

    suspend fun snapshot(): AppSettingsSnapshot {
        val prefs = context.settingsDataStore.data.first()
        return AppSettingsSnapshot(
            appLockEnabled = prefs[Keys.APP_LOCK] ?: false,
            biometricEnabled = prefs[Keys.BIOMETRIC] ?: false,
            remindersEnabled = prefs[Keys.REMINDERS] ?: true,
            reminderDaysBefore = prefs[Keys.REMINDER_DAYS] ?: 2,
            pinSalt = prefs[Keys.PIN_SALT],
            pinHash = prefs[Keys.PIN_HASH]
        )
    }

    suspend fun restore(snapshot: AppSettingsSnapshot) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.APP_LOCK] = snapshot.appLockEnabled
            prefs[Keys.BIOMETRIC] = snapshot.biometricEnabled
            prefs[Keys.REMINDERS] = snapshot.remindersEnabled
            prefs[Keys.REMINDER_DAYS] = snapshot.reminderDaysBefore
            if (snapshot.pinSalt != null && snapshot.pinHash != null) {
                prefs[Keys.PIN_SALT] = snapshot.pinSalt
                prefs[Keys.PIN_HASH] = snapshot.pinHash
            } else {
                prefs.remove(Keys.PIN_SALT)
                prefs.remove(Keys.PIN_HASH)
            }
        }
    }

    suspend fun setAppLock(enabled: Boolean, pin: String? = null) {
        context.settingsDataStore.edit { prefs ->
            if (enabled && pin != null) {
                val salt = generateSalt()
                prefs[Keys.PIN_SALT] = salt
                prefs[Keys.PIN_HASH] = hashPin(pin, salt)
            }
            prefs[Keys.APP_LOCK] = enabled
            if (!enabled) {
                prefs.remove(Keys.PIN_SALT)
                prefs.remove(Keys.PIN_HASH)
                prefs[Keys.BIOMETRIC] = false
            }
        }
    }

    suspend fun setBiometric(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC] = enabled && (prefs[Keys.APP_LOCK] ?: false)
        }
    }

    suspend fun setReminders(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.REMINDERS] = enabled }
    }

    suspend fun setReminderDaysBefore(days: Int) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.REMINDER_DAYS] = days.coerceIn(0, 14) }
    }

    suspend fun markBackupCreated(atMillis: Long = System.currentTimeMillis()) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.LAST_BACKUP] = atMillis }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.settingsDataStore.data.first()
        val salt = prefs[Keys.PIN_SALT] ?: return false
        val stored = prefs[Keys.PIN_HASH] ?: return false
        return stored == hashPin(pin, salt)
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { it.toString(16).padStart(2, '0') }
    }

    /** SHA-256(salt + pin). Fast, salted, and never reversible to the PIN itself. */
    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { it.toString(16).padStart(2, '0') }
    }
}
