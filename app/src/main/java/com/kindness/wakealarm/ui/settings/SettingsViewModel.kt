package com.kindness.wakealarm.ui.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kindness.wakealarm.R
import com.kindness.wakealarm.data.KeywordRepository
import com.kindness.wakealarm.data.SettingsRepository
import com.kindness.wakealarm.service.AlarmForegroundService
import com.kindness.wakealarm.service.AlarmSoundPlayer
import com.kindness.wakealarm.util.AppLocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val keywordRepository = KeywordRepository(application)
    private val previewPlayer = AlarmSoundPlayer(application)

    val threshold: StateFlow<Int> = settingsRepository.thresholdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_THRESHOLD)

    val activeKeywordsCount: StateFlow<Int> = keywordRepository.allActiveKeywordsFlow
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Int.MAX_VALUE)

    val volumeRampUp: StateFlow<Boolean> = settingsRepository.volumeRampUpFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val vibration: StateFlow<Boolean> = settingsRepository.vibrationFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val customRingtoneUri: StateFlow<String?> = settingsRepository.customRingtoneUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val customRingtoneTitle: StateFlow<String?> = settingsRepository.customRingtoneTitleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val recheck = MutableStateFlow(0)

    /**
     * True when a custom sound is set but can no longer be opened (file deleted, permission lost).
     * The alarm then falls back to the system sound; the screen says so before it matters.
     */
    val ringtoneUnavailable: StateFlow<Boolean> = combine(settingsRepository.customRingtoneUriFlow, recheck) { uri, _ -> uri }
        .map { uri -> uri != null && !canOpen(uri) }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isAlarmActive: StateFlow<Boolean> = AlarmForegroundService.isAlarmRunningFlow

    private val _isPlayingPreview = MutableStateFlow(false)
    val isPlayingPreview: StateFlow<Boolean> = _isPlayingPreview.asStateFlow()

    private val _language = MutableStateFlow(AppLocale.current(application))
    val language: StateFlow<AppLocale.Option> = _language.asStateFlow()

    /** One-off messages (string resource ids) for the snackbar. */
    private val _messages = Channel<Int>(Channel.BUFFERED)
    val messages: Flow<Int> = _messages.receiveAsFlow()

    init {
        // A real alarm always wins over the preview
        viewModelScope.launch {
            AlarmForegroundService.isAlarmRunningFlow.collect { running ->
                if (running) stopPreview()
            }
        }
    }

    fun recheckRingtone() {
        recheck.value += 1
        _language.value = AppLocale.current(getApplication())
    }

    private fun canOpen(uri: String): Boolean = try {
        getApplication<Application>().contentResolver.openAssetFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false
    } catch (e: Exception) {
        false
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
        releaseOldGrant(previous = customRingtoneUri.value, next = uri)
        viewModelScope.launch {
            settingsRepository.setCustomRingtone(uri, title)
            if (uri != null) _messages.send(R.string.snack_ringtone_saved)
        }
    }

    fun resetToDefaultRingtone() = setCustomRingtone(null, null)

    /** Don't keep permanent read access to audio files the user no longer uses as the alarm sound. */
    private fun releaseOldGrant(previous: String?, next: String?) {
        if (previous == null || previous == next) return
        try {
            getApplication<Application>().contentResolver.releasePersistableUriPermission(
                Uri.parse(previous),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            // Not a persisted grant (e.g. a system ringtone): nothing to release
        }
    }

    fun togglePreview() {
        if (_isPlayingPreview.value) {
            stopPreview()
        } else if (!AlarmForegroundService.isRunning()) {
            _isPlayingPreview.value = true
            viewModelScope.launch {
                // Preview plays at the user's current alarm volume rather than forcing 100%
                val source = previewPlayer.start(
                    AlarmSoundPlayer.Options(
                        customUri = customRingtoneUri.value,
                        enableRampUp = volumeRampUp.value,
                        vibrate = false,
                        forceMaxVolume = false
                    )
                )
                when (source) {
                    null, AlarmSoundPlayer.Source.REQUESTED -> Unit // null: stopped while loading
                    AlarmSoundPlayer.Source.FALLBACK -> _messages.send(R.string.snack_ringtone_fallback)
                    AlarmSoundPlayer.Source.NONE -> {
                        stopPreview()
                        _messages.send(R.string.snack_ringtone_none)
                    }
                }
            }
        }
    }

    fun stopPreview() {
        previewPlayer.stop()
        _isPlayingPreview.value = false
    }

    /** Called after the screen applied a new language (see applyLanguage). */
    fun onLanguageChanged(option: AppLocale.Option) {
        _language.value = option
    }

    override fun onCleared() {
        previewPlayer.stop()
        super.onCleared()
    }
}
