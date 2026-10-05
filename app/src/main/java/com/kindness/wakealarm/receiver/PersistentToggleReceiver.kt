package com.kindness.wakealarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kindness.wakealarm.data.SettingsRepository
import com.kindness.wakealarm.service.MasterSwitchTileService
import com.kindness.wakealarm.service.WaNotificationListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for the toggle action button in the persistent notification.
 * Toggles the Master Switch state and updates the persistent notification and Quick Settings tile.
 */
class PersistentToggleReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TOGGLE = "com.kindness.wakealarm.TOGGLE_MASTER_SWITCH"

        /** Refresh every surface that mirrors the Master Switch. */
        fun notifyMasterSwitchChanged(context: Context) {
            WaNotificationListenerService.updatePersistentNotification()
            MasterSwitchTileService.requestUpdate(context)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_TOGGLE) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SettingsRepository(appContext).toggleMasterSwitch()
                notifyMasterSwitchChanged(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
