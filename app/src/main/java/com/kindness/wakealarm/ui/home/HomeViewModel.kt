package com.kindness.wakealarm.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kindness.wakealarm.R
import com.kindness.wakealarm.data.AlarmEvent
import com.kindness.wakealarm.data.AlarmHistoryRepository
import com.kindness.wakealarm.data.KeywordRepository
import com.kindness.wakealarm.data.SettingsRepository
import com.kindness.wakealarm.receiver.PersistentToggleReceiver
import com.kindness.wakealarm.service.AlarmForegroundService
import com.kindness.wakealarm.service.WaNotificationListenerService
import com.kindness.wakealarm.util.AppLocale
import com.kindness.wakealarm.util.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val keywordRepository = KeywordRepository(application)
    private val historyRepository = AlarmHistoryRepository(application)

    val masterSwitchState: StateFlow<Boolean> = settingsRepository.masterSwitchFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** null until the first read from disk, so no "can't ring" warning flashes on launch. */
    val activeKeywordsCount: StateFlow<Int?> = keywordRepository.allActiveKeywordsFlow
        .map<List<String>, Int?> { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** null until loaded, like [activeKeywordsCount]. */
    val threshold: StateFlow<Int?> = settingsRepository.thresholdFlow
        .map<Int, Int?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastAlarm: StateFlow<AlarmEvent?> = historyRepository.eventsFlow
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _permissionStatus = MutableStateFlow(PermissionHelper.checkAll(application))
    val permissionStatus: StateFlow<PermissionHelper.PermissionStatus> = _permissionStatus.asStateFlow()

    val isAlarmActive: StateFlow<Boolean> = AlarmForegroundService.isAlarmRunningFlow

    val isListenerConnected: StateFlow<Boolean> = WaNotificationListenerService.isConnectedFlow

    fun setMasterSwitch(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMasterSwitch(enabled)
            PersistentToggleReceiver.notifyMasterSwitchChanged(getApplication())
        }
    }

    fun stopActiveAlarm() {
        AlarmForegroundService.stop()
    }

    /** @return false when the system refused to start the alarm. */
    fun startTestAlarm(): Boolean {
        val app = getApplication<Application>()
        val res = AppLocale.wrap(app)
        return AlarmForegroundService.start(
            app,
            AlarmForegroundService.AlarmRequest(
                matchedKeywords = res.getString(R.string.test_alarm_keywords).split(",").map { it.trim() },
                message = res.getString(R.string.test_alarm_message),
                sender = res.getString(R.string.app_name),
                isTest = true
            )
        )
    }

    fun reconnectListener() {
        WaNotificationListenerService.requestRebind(getApplication())
    }

    fun refreshPermissions() {
        val status = PermissionHelper.checkAll(getApplication())
        _permissionStatus.value = status
        // Permission granted but listener not bound (common after OEM battery kills): ask to rebind
        if (status.notificationListener && !WaNotificationListenerService.isRunning()) {
            reconnectListener()
        }
    }
}
