package com.kindness.wakealarm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kindness.wakealarm.util.KeywordValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.keywordDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "keywords"
)

/**
 * Repository for keyword management.
 * Stores active preset keywords and custom user keywords in DataStore.
 */
class KeywordRepository(private val context: Context) {

    companion object {
        private val ACTIVE_PRESETS_KEY = stringSetPreferencesKey("active_presets")
        private val CUSTOM_KEYWORDS_KEY = stringSetPreferencesKey("custom_keywords")
    }

    /**
     * Flow of active preset keyword strings.
     * On first launch, defaults to PresetKeywords.defaultEnabled.
     */
    val activePresetsFlow: Flow<Set<String>> = context.keywordDataStore.data
        .map { preferences ->
            preferences[ACTIVE_PRESETS_KEY] ?: PresetKeywords.defaultEnabled
        }

    /**
     * Flow of custom (user-added) keyword strings.
     */
    val customKeywordsFlow: Flow<Set<String>> = context.keywordDataStore.data
        .map { preferences ->
            preferences[CUSTOM_KEYWORDS_KEY] ?: emptySet()
        }

    /**
     * Combined flow of all active keywords (presets + custom).
     */
    val allActiveKeywordsFlow: Flow<List<String>> = context.keywordDataStore.data
        .map { preferences ->
            val presets = preferences[ACTIVE_PRESETS_KEY] ?: PresetKeywords.defaultEnabled
            val custom = preferences[CUSTOM_KEYWORDS_KEY] ?: emptySet()
            (presets + custom).toList()
        }

    /**
     * Toggle a preset keyword on/off.
     */
    suspend fun togglePreset(keyword: String) {
        context.keywordDataStore.edit { preferences ->
            val current = preferences[ACTIVE_PRESETS_KEY]?.toMutableSet()
                ?: PresetKeywords.defaultEnabled.toMutableSet()
            if (current.contains(keyword)) {
                current.remove(keyword)
            } else {
                current.add(keyword)
            }
            preferences[ACTIVE_PRESETS_KEY] = current
        }
    }

    /**
     * Add a custom keyword.
     */
    suspend fun addCustomKeyword(keyword: String) {
        val trimmed = KeywordValidator.normalize(keyword)
        if (trimmed.isBlank()) return

        context.keywordDataStore.edit { preferences ->
            val current = preferences[CUSTOM_KEYWORDS_KEY]?.toMutableSet() ?: mutableSetOf()
            current.add(trimmed)
            preferences[CUSTOM_KEYWORDS_KEY] = current
        }
    }

    /**
     * Remove a custom keyword.
     */
    suspend fun removeCustomKeyword(keyword: String) {
        context.keywordDataStore.edit { preferences ->
            val current = preferences[CUSTOM_KEYWORDS_KEY]?.toMutableSet() ?: mutableSetOf()
            current.remove(keyword)
            preferences[CUSTOM_KEYWORDS_KEY] = current
        }
    }
}
