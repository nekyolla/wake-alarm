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
import androidx.core.app.Person
import com.kindness.wakealarm.MainActivity
import com.kindness.wakealarm.R
import com.kindness.wakealarm.data.AlarmEvent
import com.kindness.wakealarm.data.AlarmHistoryRepository
import com.kindness.wakealarm.data.KeywordRepository
import com.kindness.wakealarm.data.SettingsRepository
import com.kindness.wakealarm.receiver.PersistentToggleReceiver
import com.kindness.wakealarm.ui.theme.NotificationAccent
import com.kindness.wakealarm.util.AppLocale
import com.kindness.wakealarm.util.KeywordMatcher
import com.kindness.wakealarm.util.MessageDeduplicator
import com.kindness.wakealarm.util.TriggerPlanner
import com.kindness.wakealarm.util.WhatsAppMessageParser
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
    private lateinit var historyRepository: AlarmHistoryRepository

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsRepository = SettingsRepository(applicationContext)
        keywordRepository = KeywordRepository(applicationContext)
        historyRepository = AlarmHistoryRepository(applicationContext)
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

        // Mark as seen synchronously, regardless of Master Switch state, so a bubble is
        // evaluated exactly once even when WhatsApp re-posts the notification.
        val fresh = deduplicator.takeNew(sbn.key, extractMessages(notification))
        if (fresh.isEmpty()) return

        serviceScope.launch {
            try {
                if (!settingsRepository.masterSwitchFlow.first()) {
                    Log.d(TAG, "Master switch OFF, skipping alarm trigger")
                    return@launch
                }

                val keywords = keywordRepository.allActiveKeywordsFlow.first()
                val threshold = settingsRepository.thresholdFlow.first()

                // Evaluate each new bubble on its own, newest first
                val triggered = fresh.asReversed().mapNotNull { message ->
                    val result = KeywordMatcher.match(message.text, keywords, threshold = threshold)
                    Log.d(TAG, "Bubble -> ${result.matchCount}/$threshold matches (${result.matchedKeywords})")
                    if (result.isTriggered) message to result.matchedKeywords else null
                }

                val plan = TriggerPlanner.plan(triggered, AlarmForegroundService.isRunning())
                plan.ring?.let { (message, matched) ->
                    Log.i(TAG, "ALARM TRIGGERED. Matched: $matched")
                    triggerAlarm(
                        AlarmForegroundService.AlarmRequest(
                            matchedKeywords = matched,
                            message = message.text,
                            sender = message.sender,
                            chatIntent = notification.contentIntent
                        )
                    )
                }
                // Only one alarm rings at a time; the others are kept so none is lost
                plan.recordOnly.forEach { (message, matched) ->
                    historyRepository.add(
                        AlarmEvent(
                            timestamp = System.currentTimeMillis(),
                            sender = message.sender,
                            message = message.text,
                            keywords = matched,
                            whileRinging = true
                        )
                    )
                    AlarmForegroundService.reportExtraMatch()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
            }
        }
    }

    /**
     * Pull individual message bubbles out of a WhatsApp notification.
     * MessagingStyle is preferred (it carries per-message timestamps and senders); plain text
     * extras are only used when MessagingStyle is absent, so the same bubble is never counted twice.
     */
    private fun extractMessages(notification: Notification): List<MessageDeduplicator.Message> {
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()

        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        if (style != null && style.messages.isNotEmpty()) {
            return WhatsAppMessageParser.parseMessagingStyle(
                messages = style.messages.map { msg ->
                    WhatsAppMessageParser.RawMessage(msg.text?.toString(), msg.timestamp, msg.person?.toSender())
                },
                user = style.user.toSender(),
                conversationTitle = style.conversationTitle?.toString(),
                fallbackTitle = title
            )
        }

        return WhatsAppMessageParser.parsePlainText(
            title = title,
            lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.map { it.toString() }.orEmpty(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        )
    }

    private fun Person.toSender() = WhatsAppMessageParser.Sender(name = name?.toString(), key = key)

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
        val res = AppLocale.wrap(this)
        val channel = NotificationChannel(
            CHANNEL_ID_PERSISTENT,
            res.getString(R.string.channel_status_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = res.getString(R.string.channel_status_desc)
            setShowBadge(false)
        }

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    fun showPersistentNotification() {
        serviceScope.launch {
            try {
                val masterEnabled = settingsRepository.masterSwitchFlow.first()
                // Re-create the channel so its name follows a language change
                createPersistentNotificationChannel()
                val notification = buildPersistentNotification(masterEnabled)
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(NOTIFICATION_ID_PERSISTENT, notification)
            } catch (e: Exception) {
                Log.e(TAG, "Error showing persistent notification", e)
            }
        }
    }

    private fun buildPersistentNotification(masterEnabled: Boolean): Notification {
        val res = AppLocale.wrap(this)
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

        val statusText = res.getString(if (masterEnabled) R.string.status_notif_on else R.string.status_notif_off)
        val toggleLabel = res.getString(if (masterEnabled) R.string.action_pause else R.string.action_resume)
        val icon = if (masterEnabled) R.drawable.ic_stat_alarm else R.drawable.ic_stat_alarm_off

        return NotificationCompat.Builder(this, CHANNEL_ID_PERSISTENT)
            .setSmallIcon(icon)
            .setColor(NotificationAccent)
            .setContentTitle(res.getString(R.string.app_name))
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
