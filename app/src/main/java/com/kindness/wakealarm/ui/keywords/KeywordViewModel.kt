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

    /**
     * Toggles a preset.
     * @return true when this turns a preset off and leaves fewer active keywords than the
     * threshold, i.e. the alarm could no longer ring. The screen warns and offers undo.
     */
    fun togglePreset(keyword: String): Boolean {
        val turningOff = keyword in activePresets.value
        viewModelScope.launch {
            repository.togglePreset(keyword)
        }
        return turningOff && activeKeywords.value.size - 1 < threshold.value
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

    /**
     * @return true when removing it leaves fewer active keywords than the threshold.
     */
    fun removeCustomKeyword(keyword: String): Boolean {
        viewModelScope.launch {
            repository.removeCustomKeyword(keyword)
        }
        return activeKeywords.value.size - 1 < threshold.value
    }

    fun restoreCustomKeyword(keyword: String) {
        viewModelScope.launch {
            repository.addCustomKeyword(keyword)
        }
    }
}
