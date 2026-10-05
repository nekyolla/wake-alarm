package com.kindness.wakealarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kindness.wakealarm.MainActivity
import com.kindness.wakealarm.R
import com.kindness.wakealarm.data.KeywordRepository
import com.kindness.wakealarm.data.SettingsRepository
import com.kindness.wakealarm.receiver.PersistentToggleReceiver
import com.kindness.wakealarm.util.KeywordMatcher
import com.kindness.wakealarm.util.MessageDeduplicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Core service: listens to all incoming notifications and filters for WhatsApp.
 * When keyword match is found and Master Switch is ON, triggers the alarm.
 * Also manages the persistent notification with the Master Switch toggle.
 */
class WaNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "WaNotifListener"
        const val CHANNEL_ID_PERSISTENT = "persistent_toggle_channel"
        const val NOTIFICATION_ID_PERSISTENT = 1001

        /** WhatsApp Messenger and WhatsApp Business. */
        val WHATSAPP_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")

        private var instance: WaNotificationListenerService? = null
        private val _isConnectedFlow = MutableStateFlow(false)
        val isConnectedFlow: StateFlow<Boolean> = _isConnectedFlow.asStateFlow()

        fun isRunning(): Boolean = _isConnectedFlow.value

        /**
         * Request the persistent notification to be updated (called from receiver/viewmodel/tile).
         */
        fun updatePersistentNotification() {
            instance?.showPersistentNotification()
        }

        /**
         * Ask the system to re-bind the listener. OEM battery managers (notably Xiaomi) sometimes
         * leave the permission granted but the listener disconnected.
         */
        fun requestRebind(context: Context) {
            try {
                NotificationListenerService.requestRebind(ComponentName(context, WaNotificationListenerService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "requestRebind failed", e)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val deduplicator = MessageDeduplicator()
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var keywordRepository: KeywordRepository

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsRepository = SettingsRepository(applicationContext)
        keywordRepository = KeywordRepository(applicationContext)
        createPersistentNotificationChannel()
        Log.d(TAG, "NotificationListenerService created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
        instance = this
        _isConnectedFlow.value = true
        showPersistentNotification()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
        _isConnectedFlow.value = false
    }

    override fun onDestroy() {
        serviceScope.cancel()
        instance = null
        _isConnectedFlow.value = false
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName !in WHATSAPP_PACKAGES) return

        val notification = sbn.notification
        // Group summaries ("5 messages from 3 chats") repeat the per-chat notifications
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extracted = extractMessages(notification)
        // Mark as seen synchronously, regardless of Master Switch state, so a bubble is
        // evaluated exactly once even when WhatsApp re-posts the notification.
        val fresh = deduplicator.takeNew(sbn.key, extracted.messages)
        if (fresh.isEmpty()) return

        serviceScope.launch {
            try {
                if (!settingsRepository.masterSwitchFlow.first()) {
                    Log.d(TAG, "Master switch OFF, skipping alarm trigger")
                    return@launch
                }
                if (AlarmForegroundService.isRunning()) {
                    Log.d(TAG, "Alarm already running, skipping")
                    return@launch
                }

                val keywords = keywordRepository.allActiveKeywordsFlow.first()
                val threshold = settingsRepository.thresholdFlow.first()

                // Evaluate each new bubble on its own, newest first
                for (message in fresh.asReversed()) {
                    val result = KeywordMatcher.match(message.text, keywords, threshold = threshold)
                    Log.d(TAG, "Bubble -> ${result.matchCount}/$threshold matches (${result.matchedKeywords})")
                    if (result.isTriggered) {
                        Log.i(TAG, "ALARM TRIGGERED. Matched: ${result.matchedKeywords}")
                        triggerAlarm(
                            AlarmForegroundService.AlarmRequest(
                                matchedKeywords = result.matchedKeywords,
                                message = message.text,
                                sender = extracted.sender,
                                chatIntent = notification.contentIntent
                            )
                        )
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
            }
        }
    }

    private data class Extracted(val sender: String, val messages: List<MessageDeduplicator.Message>)

    /**
     * Pull individual message bubbles out of a WhatsApp notification.
     * MessagingStyle is preferred (it carries per-message timestamps); plain text extras are
     * only used when MessagingStyle is absent, so the same bubble is never counted twice.
     */
    private fun extractMessages(notification: Notification): Extracted {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()

        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        if (style != null && style.messages.isNotEmpty()) {
            val userName = style.user.name?.toString()
            val incoming = style.messages.filter { msg ->
                // Skip replies the user sent from the notification itself
                val name = msg.person?.name?.toString()
                name == null || name != userName
            }
            val latest = incoming.lastOrNull()
            val person = latest?.person?.name?.toString()
            val conversation = style.conversationTitle?.toString()
            val sender = when {
                !conversation.isNullOrBlank() && !person.isNullOrBlank() -> "$person · $conversation"
                !person.isNullOrBlank() -> person
                else -> title
            }
            return Extracted(
                sender = sender,
                messages = incoming.mapNotNull { msg ->
                    val text = msg.text?.toString()?.trim().orEmpty()
                    if (text.isBlank()) null else MessageDeduplicator.Message(text, msg.timestamp)
                }
            )
        }

        val texts = mutableListOf<String>()
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { line ->
            val s = line.toString().trim()
            if (s.isNotBlank() && s !in texts) texts.add(s)
        }
        val single = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        if (single.isNotBlank() && single !in texts) texts.add(single)

        return Extracted(title, texts.map { MessageDeduplicator.Message(it, null) })
    }

    private fun triggerAlarm(request: AlarmForegroundService.AlarmRequest) {
        if (AlarmForegroundService.start(this, request)) return

        // The system refused a background foreground-service start. Fall back to a full-screen
        // notification: opening the alarm screen starts the service from the foreground.
        AlarmForegroundService.createChannel(this)
        getSystemService(NotificationManager::class.java)
            .notify(AlarmForegroundService.NOTIFICATION_ID_ALARM, AlarmForegroundService.buildAlarmNotification(this, request, startServiceOnOpen = true))
    }

    // --- Persistent Notification with Master Switch Toggle ---

    private fun createPersistentNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID_PERSISTENT,
            getString(R.string.channel_status_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_status_desc)
            setShowBadge(false)
        }

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    fun showPersistentNotification() {
        serviceScope.launch {
            try {
                val masterEnabled = settingsRepository.masterSwitchFlow.first()
                val notification = buildPersistentNotification(masterEnabled)
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIFICATION_ID_PERSISTENT, notification)
            } catch (e: Exception) {
                Log.e(TAG, "Error showing persistent notification", e)
            }
        }
    }

    private fun buildPersistentNotification(masterEnabled: Boolean): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, PersistentToggleReceiver::class.java).apply {
            action = PersistentToggleReceiver.ACTION_TOGGLE
        }
        val togglePendingIntent = PendingIntent.getBroadcast(
            this, 0, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = getString(if (masterEnabled) R.string.status_notif_on else R.string.status_notif_off)
        val toggleLabel = getString(if (masterEnabled) R.string.action_pause else R.string.action_resume)
        val icon = if (masterEnabled) R.drawable.ic_stat_alarm else R.drawable.ic_stat_alarm_off

        return NotificationCompat.Builder(this, CHANNEL_ID_PERSISTENT)
            .setSmallIcon(icon)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(statusText)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .addAction(icon, toggleLabel, togglePendingIntent)
            .build()
    }
}
