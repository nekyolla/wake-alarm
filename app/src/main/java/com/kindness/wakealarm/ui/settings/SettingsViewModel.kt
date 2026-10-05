package com.kindness.wakealarm.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kindness.wakealarm.data.SettingsRepository
import com.kindness.wakealarm.service.AlarmForegroundService
import com.kindness.wakealarm.service.AlarmSoundPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val previewPlayer = AlarmSoundPlayer(application)

    val threshold: StateFlow<Int> = settingsRepository.thresholdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_THRESHOLD)

    val volumeRampUp: StateFlow<Boolean> = settingsRepository.volumeRampUpFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val vibration: StateFlow<Boolean> = settingsRepository.vibrationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val customRingtoneUri: StateFlow<String?> = settingsRepository.customRingtoneUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val customRingtoneTitle: StateFlow<String?> = settingsRepository.customRingtoneTitleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isAlarmActive: StateFlow<Boolean> = AlarmForegroundService.isAlarmRunningFlow

    private val _isPlayingPreview = MutableStateFlow(false)
    val isPlayingPreview: StateFlow<Boolean> = _isPlayingPreview.asStateFlow()

    init {
        // A real alarm always wins over the preview
        viewModelScope.launch {
            AlarmForegroundService.isAlarmRunningFlow.collect { running ->
                if (running) stopPreview()
            }
        }
    }

    fun setThreshold(value: Int) {
        viewModelScope.launch {
            settingsRepository.setThreshold(value)
        }
    }

    fun setVolumeRampUp(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVolumeRampUp(enabled)
        }
    }

    fun setVibration(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVibration(enabled)
        }
    }

    fun setCustomRingtone(uri: String?, title: String?) {
        stopPreview()
        viewModelScope.launch {
            settingsRepository.setCustomRingtone(uri, title)
        }
    }

    fun resetToDefaultRingtone() = setCustomRingtone(null, null)

    fun togglePreview() {
        if (_isPlayingPreview.value) {
            stopPreview()
        } else if (!AlarmForegroundService.isRunning()) {
            _isPlayingPreview.value = true
            // Preview plays at the user's current alarm volume rather than forcing 100%
            previewPlayer.start(
                AlarmSoundPlayer.Options(
                    customUri = customRingtoneUri.value,
                    enableRampUp = volumeRampUp.value,
                    vibrate = false,
                    forceMaxVolume = false
                )
            )
        }
    }

    fun stopPreview() {
        previewPlayer.stop()
        _isPlayingPreview.value = false
    }

    override fun onCleared() {
        previewPlayer.stop()
        super.onCleared()
    }
}
