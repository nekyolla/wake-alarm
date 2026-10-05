package com.kindness.wakealarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings"
)

/**
 * Repository for app settings, primarily the Master Switch state.
 * Uses DataStore Preferences as the single source of truth.
 */
class SettingsRepository(private val context: Context) {

    companion object {
        const val MIN_THRESHOLD = 1
        const val MAX_THRESHOLD = 5
        const val DEFAULT_THRESHOLD = 2

        private val MASTER_SWITCH_KEY = booleanPreferencesKey("master_switch_enabled")
        private val KEYWORD_THRESHOLD_KEY = intPreferencesKey("keyword_threshold")
        private val VOLUME_RAMP_UP_KEY = booleanPreferencesKey("volume_ramp_up_enabled")
        private val VIBRATION_KEY = booleanPreferencesKey("vibration_enabled")
        private val CUSTOM_RINGTONE_URI_KEY = stringPreferencesKey("custom_ringtone_uri")
        private val CUSTOM_RINGTONE_TITLE_KEY = stringPreferencesKey("custom_ringtone_title")
        private val ONBOARDING_DONE_KEY = booleanPreferencesKey("onboarding_completed")
    }

    /**
     * Observe the Master Switch state as a Flow.
     * Defaults to false (OFF) on first install.
     */
    val masterSwitchFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[MASTER_SWITCH_KEY] ?: false
        }

    /**
     * Observe the keyword threshold (1..5). Default: 2.
     */
    val thresholdFlow: Flow<Int> = context.settingsDataStore.data
        .map { preferences ->
            preferences[KEYWORD_THRESHOLD_KEY] ?: DEFAULT_THRESHOLD
        }

    /**
     * Observe volume ramp-up setting. Default: true.
     */
    val volumeRampUpFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[VOLUME_RAMP_UP_KEY] ?: true
        }

    /**
     * Observe vibration setting. Default: true.
     */
    val vibrationFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[VIBRATION_KEY] ?: true
        }

    /**
     * Observe custom ringtone URI (null = default alarm sound).
     */
    val customRingtoneUriFlow: Flow<String?> = context.settingsDataStore.data
        .map { preferences ->
            preferences[CUSTOM_RINGTONE_URI_KEY]
        }

    /**
     * Observe custom ringtone title (null = default alarm sound).
     */
    val customRingtoneTitleFlow: Flow<String?> = context.settingsDataStore.data
        .map { preferences ->
            preferences[CUSTOM_RINGTONE_TITLE_KEY]
        }

    /**
     * Observe whether the first-run onboarding has been completed.
     */
    val onboardingCompletedFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[ONBOARDING_DONE_KEY] ?: false
        }

    /**
     * Set the Master Switch state.
     */
    suspend fun setMasterSwitch(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[MASTER_SWITCH_KEY] = enabled
        }
    }

    /**
     * Toggle the Master Switch and return the new state.
     */
    suspend fun toggleMasterSwitch(): Boolean {
        var newState = false
        context.settingsDataStore.edit { preferences ->
            val current = preferences[MASTER_SWITCH_KEY] ?: false
            newState = !current
            preferences[MASTER_SWITCH_KEY] = newState
        }
        return newState
    }

    /**
     * Set keyword match threshold (1..5).
     */
    suspend fun setThreshold(threshold: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEYWORD_THRESHOLD_KEY] = threshold.coerceIn(MIN_THRESHOLD, MAX_THRESHOLD)
        }
    }

    /**
     * Set volume ramp-up enabled/disabled.
     */
    suspend fun setVolumeRampUp(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[VOLUME_RAMP_UP_KEY] = enabled
        }
    }

    /**
     * Set vibration enabled/disabled.
     */
    suspend fun setVibration(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[VIBRATION_KEY] = enabled
        }
    }

    /**
     * Set custom ringtone URI and display title. Pass null to reset to default.
     */
    suspend fun setCustomRingtone(uri: String?, title: String?) {
        context.settingsDataStore.edit { preferences ->
            if (uri != null && title != null) {
                preferences[CUSTOM_RINGTONE_URI_KEY] = uri
                preferences[CUSTOM_RINGTONE_TITLE_KEY] = title
            } else {
                preferences.remove(CUSTOM_RINGTONE_URI_KEY)
                preferences.remove(CUSTOM_RINGTONE_TITLE_KEY)
            }
        }
    }

    /**
     * Mark the first-run onboarding as completed.
     */
    suspend fun setOnboardingCompleted() {
        context.settingsDataStore.edit { preferences ->
            preferences[ONBOARDING_DONE_KEY] = true
        }
    }
}
