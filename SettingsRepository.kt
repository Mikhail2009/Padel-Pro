package com.padelpro.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.padelpro.app.domain.model.DeuceMode
import com.padelpro.app.domain.model.MatchFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "padel_pro_settings")

data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val autoSideSwap: Boolean = true,
    val defaultFormat: MatchFormat = MatchFormat.DOUBLES,
    val defaultDeuceMode: DeuceMode = DeuceMode.GOLDEN_POINT,
    val contentVersion: Int = 1,
    val backupLastExportedAt: Long? = null
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val AUTO_SIDE_SWAP = booleanPreferencesKey("auto_side_swap")
        val DEFAULT_FORMAT = stringPreferencesKey("default_format")
        val DEFAULT_DEUCE_MODE = stringPreferencesKey("default_deuce_mode")
        val CONTENT_VERSION = intPreferencesKey("content_version")
        val BACKUP_LAST_EXPORTED_AT = longPreferencesKey("backup_last_exported_at")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val formatStr = prefs[Keys.DEFAULT_FORMAT] ?: MatchFormat.DOUBLES.name
        val deuceStr = prefs[Keys.DEFAULT_DEUCE_MODE] ?: DeuceMode.GOLDEN_POINT.name
        AppSettings(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
            autoSideSwap = prefs[Keys.AUTO_SIDE_SWAP] ?: true,
            defaultFormat = runCatching { MatchFormat.valueOf(formatStr) }.getOrDefault(MatchFormat.DOUBLES),
            defaultDeuceMode = runCatching { DeuceMode.valueOf(deuceStr) }.getOrDefault(DeuceMode.GOLDEN_POINT),
            contentVersion = prefs[Keys.CONTENT_VERSION] ?: 1,
            backupLastExportedAt = prefs[Keys.BACKUP_LAST_EXPORTED_AT]
        )
    }

    val autoSideSwap: Flow<Boolean> = settings.map { it.autoSideSwap }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setAutoSideSwap(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SIDE_SWAP] = enabled }
    }

    suspend fun setDefaultFormat(format: MatchFormat) {
        context.dataStore.edit { it[Keys.DEFAULT_FORMAT] = format.name }
    }

    suspend fun setDefaultDeuceMode(mode: DeuceMode) {
        context.dataStore.edit { it[Keys.DEFAULT_DEUCE_MODE] = mode.name }
    }

    suspend fun setBackupLastExportedAt(timestampMillis: Long) {
        context.dataStore.edit { it[Keys.BACKUP_LAST_EXPORTED_AT] = timestampMillis }
    }

    suspend fun applySettings(newSettings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = newSettings.onboardingCompleted
            prefs[Keys.HAPTICS_ENABLED] = newSettings.hapticsEnabled
            prefs[Keys.KEEP_SCREEN_ON] = newSettings.keepScreenOn
            prefs[Keys.AUTO_SIDE_SWAP] = newSettings.autoSideSwap
            prefs[Keys.DEFAULT_FORMAT] = newSettings.defaultFormat.name
            prefs[Keys.DEFAULT_DEUCE_MODE] = newSettings.defaultDeuceMode.name
            prefs[Keys.CONTENT_VERSION] = newSettings.contentVersion
            val exportedAt = newSettings.backupLastExportedAt
            if (exportedAt != null) {
                prefs[Keys.BACKUP_LAST_EXPORTED_AT] = exportedAt
            } else {
                prefs.remove(Keys.BACKUP_LAST_EXPORTED_AT)
            }
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
