package com.kindness.wakealarm.ui.keywords

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kindness.wakealarm.data.KeywordRepository
import com.kindness.wakealarm.data.PresetKeywords
import com.kindness.wakealarm.data.SettingsRepository
import com.kindness.wakealarm.util.KeywordValidator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KeywordViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KeywordRepository(application)
    private val settingsRepository = SettingsRepository(application)

    val activePresets: StateFlow<Set<String>> = repository.activePresetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PresetKeywords.defaultEnabled)

    val customKeywords: StateFlow<Set<String>> = repository.customKeywordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val activeKeywords: StateFlow<List<String>> = repository.allActiveKeywordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PresetKeywords.defaultEnabled.toList())

    val threshold: StateFlow<Int> = settingsRepository.thresholdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_THRESHOLD)

    fun togglePreset(keyword: String) {
        viewModelScope.launch {
            repository.togglePreset(keyword)
        }
    }

    /**
     * Validates and stores a custom keyword. Presets count as existing so they can't be duplicated.
     */
    fun addCustomKeyword(input: String): KeywordValidator.Result {
        val existing = customKeywords.value + PresetKeywords.all.map { it.keyword }
        val result = KeywordValidator.validate(input, existing)
        if (result is KeywordValidator.Result.Valid) {
            viewModelScope.launch {
                repository.addCustomKeyword(result.normalized)
            }
        }
        return result
    }

    fun removeCustomKeyword(keyword: String) {
        viewModelScope.launch {
            repository.removeCustomKeyword(keyword)
        }
    }

    fun restoreCustomKeyword(keyword: String) {
        viewModelScope.launch {
            repository.addCustomKeyword(keyword)
        }
    }
}
