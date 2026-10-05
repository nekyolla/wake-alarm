package com.kindness.wakealarm.service

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.kindness.wakealarm.R
import com.kindness.wakealarm.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Quick Settings tile that toggles the Master Switch with one tap, like the flashlight tile.
 */
class MasterSwitchTileService : TileService() {

    companion object {
        fun requestUpdate(context: Context) {
            try {
                TileService.requestListeningState(context, ComponentName(context, MasterSwitchTileService::class.java))
            } catch (_: Exception) {
                // Tile not added or not supported — nothing to refresh
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { render(settingsRepository.masterSwitchFlow.first()) }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val enabled = settingsRepository.toggleMasterSwitch()
            render(enabled)
            WaNotificationListenerService.updatePersistentNotification()
        }
    }

    private fun render(enabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.icon = Icon.createWithResource(this, if (enabled) R.drawable.ic_stat_alarm else R.drawable.ic_stat_alarm_off)
        tile.contentDescription = getString(if (enabled) R.string.tile_on else R.string.tile_off)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(if (enabled) R.string.tile_on else R.string.tile_off)
        }
        tile.updateTile()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
