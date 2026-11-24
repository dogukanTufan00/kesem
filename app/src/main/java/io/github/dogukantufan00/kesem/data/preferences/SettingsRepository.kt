package io.github.dogukantufan00.kesem.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.dogukantufan00.kesem.data.backup.BackupSettings
import io.github.dogukantufan00.kesem.model.AppLockConfig
import io.github.dogukantufan00.kesem.model.ThemeMode
import io.github.dogukantufan00.kesem.util.PinHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.kesemDataStore: DataStore<Preferences> by preferencesDataStore(name = "kesem_prefs")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val MONTH_START_DAY = intPreferencesKey("month_start_day")
        val HIDE_BALANCES = booleanPreferencesKey("hide_balances")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PIN_LOCK_ENABLED = booleanPreferencesKey("pin_lock_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val WARM_PALETTE_APPLIED = booleanPreferencesKey("warm_palette_v1_applied")
    }

    val monthStartDay: Flow<Int> = context.kesemDataStore.data.map { prefs ->
        (prefs[Keys.MONTH_START_DAY] ?: 1).coerceIn(1, 28)
    }

    val hideBalances: Flow<Boolean> = context.kesemDataStore.data.map { prefs ->
        prefs[Keys.HIDE_BALANCES] ?: false
    }

    val themeMode: Flow<ThemeMode> = context.kesemDataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
    }

    val appLockConfig: Flow<AppLockConfig> = context.kesemDataStore.data.map { prefs ->
        val pinHash = prefs[Keys.PIN_HASH] ?: ""
        val pinSalt = prefs[Keys.PIN_SALT] ?: ""
        val pinEnabled = (prefs[Keys.PIN_LOCK_ENABLED] ?: false) &&
            pinHash.isNotEmpty() &&
            pinSalt.isNotEmpty()
        AppLockConfig(
            isPinEnabled = pinEnabled,
            isBiometricEnabled = pinEnabled && (prefs[Keys.BIOMETRIC_ENABLED] ?: false),
            pinHash = pinHash,
            pinSalt = pinSalt
        )
    }

    suspend fun setMonthStartDay(day: Int) {
        val clamped = day.coerceIn(1, 28)
        context.kesemDataStore.edit { prefs ->
            prefs[Keys.MONTH_START_DAY] = clamped
        }
    }

    suspend fun toggleHideBalances() {
        context.kesemDataStore.edit { prefs ->
            val current = prefs[Keys.HIDE_BALANCES] ?: false
            prefs[Keys.HIDE_BALANCES] = !current
        }
    }

    suspend fun setHideBalances(hide: Boolean) {
        context.kesemDataStore.edit { prefs ->
            prefs[Keys.HIDE_BALANCES] = hide
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.kesemDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun enablePinLock(pin: String) {
        require(pin.length == 4 && pin.all { it.isDigit() }) { "PIN 4 haneli rakam olmalı" }
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hashPin(pin, salt)
        context.kesemDataStore.edit { prefs ->
            prefs[Keys.PIN_SALT] = salt
            prefs[Keys.PIN_HASH] = hash
            prefs[Keys.PIN_LOCK_ENABLED] = true
        }
    }

    suspend fun changePin(newPin: String) {
        enablePinLock(newPin)
    }

    suspend fun disablePinLock() {
        context.kesemDataStore.edit { prefs ->
            prefs[Keys.PIN_LOCK_ENABLED] = false
            prefs[Keys.BIOMETRIC_ENABLED] = false
            prefs.remove(Keys.PIN_HASH)
            prefs.remove(Keys.PIN_SALT)
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.kesemDataStore.edit { prefs ->
            val hasPin = (prefs[Keys.PIN_LOCK_ENABLED] ?: false) &&
                !(prefs[Keys.PIN_HASH].isNullOrBlank())
            prefs[Keys.BIOMETRIC_ENABLED] = enabled && hasPin
        }
    }

    suspend fun getBackupSettingsOnce(): BackupSettings {
        val prefs = context.kesemDataStore.data.first()
        val rawTheme = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        return BackupSettings(
            monthStartDay = (prefs[Keys.MONTH_START_DAY] ?: 1).coerceIn(1, 28),
            hideBalances = prefs[Keys.HIDE_BALANCES] ?: false,
            themeMode = runCatching { ThemeMode.valueOf(rawTheme) }.getOrDefault(ThemeMode.SYSTEM)
        )
    }

    suspend fun restoreBackupSettings(settings: BackupSettings) {
        context.kesemDataStore.edit { prefs ->
            prefs[Keys.MONTH_START_DAY] = settings.monthStartDay.coerceIn(1, 28)
            prefs[Keys.HIDE_BALANCES] = settings.hideBalances
            prefs[Keys.THEME_MODE] = settings.themeMode.name
        }
    }

    suspend fun isWarmPaletteApplied(): Boolean {
        val prefs = context.kesemDataStore.data.first()
        return prefs[Keys.WARM_PALETTE_APPLIED] ?: false
    }

    suspend fun markWarmPaletteApplied() {
        context.kesemDataStore.edit { prefs ->
            prefs[Keys.WARM_PALETTE_APPLIED] = true
        }
    }
}
