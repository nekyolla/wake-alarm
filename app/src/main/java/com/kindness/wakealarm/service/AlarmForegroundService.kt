package com.kindness.wakealarm.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.IntentCompat
import com.kindness.wakealarm.R
import com.kindness.wakealarm.data.AlarmEvent
import com.kindness.wakealarm.data.AlarmHistoryRepository
import com.kindness.wakealarm.data.SettingsRepository
import com.kindness.wakealarm.ui.alarm.AlarmTriggerActivity
import com.kindness.wakealarm.ui.theme.NotificationAccent
import com.kindness.wakealarm.util.AppLocale
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
 * Foreground service that manages the alarm when keywords are matched.
 * Creates a high-importance notification with full-screen intent to show
 * the alarm activity over the lockscreen.
 */
class AlarmForegroundService : Service() {

    /**
     * Everything the alarm UI needs to describe what triggered it.
     * [chatIntent] is WhatsApp's own content intent, used to jump straight into the chat.
     */
    data class AlarmRequest(
        val matchedKeywords: List<String>,
        val message: String,
        val sender: String,
        val chatIntent: PendingIntent? = null,
        val isTest: Boolean = false
    ) {
        fun writeTo(intent: Intent): Intent = intent.apply {
            putExtra(EXTRA_MATCHED_KEYWORDS, matchedKeywords.toTypedArray())
            putExtra(EXTRA_NOTIFICATION_TEXT, message)
            putExtra(EXTRA_SENDER, sender)
            putExtra(EXTRA_CHAT_INTENT, chatIntent)
            putExtra(EXTRA_IS_TEST, isTest)
        }

        companion object {
            fun from(intent: Intent?): AlarmRequest? {
                intent ?: return null
                val keywords = intent.getStringArrayExtra(EXTRA_MATCHED_KEYWORDS) ?: return null
                return AlarmRequest(
                    matchedKeywords = keywords.toList(),
                    message = intent.getStringExtra(EXTRA_NOTIFICATION_TEXT).orEmpty(),
                    sender = intent.getStringExtra(EXTRA_SENDER).orEmpty(),
                    chatIntent = IntentCompat.getParcelableExtra(intent, EXTRA_CHAT_INTENT, PendingIntent::class.java),
                    isTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)
                )
            }
        }
    }

    companion object {
        private const val TAG = "AlarmService"
        const val CHANNEL_ID_ALARM = "alarm_channel"
        const val NOTIFICATION_ID_ALARM = 2001
        const val ACTION_STOP_ALARM = "com.kindness.wakealarm.ACTION_STOP_ALARM"
        const val EXTRA_MATCHED_KEYWORDS = "matched_keywords"
        const val EXTRA_NOTIFICATION_TEXT = "notification_text"
        const val EXTRA_SENDER = "sender"
        const val EXTRA_CHAT_INTENT = "chat_intent"
        const val EXTRA_IS_TEST = "is_test"
        private const val EXTRA_GENERATION = "generation"

        // Safety net only: MediaPlayer holds its own wake lock while it plays
        private const val WAKE_LOCK_TIMEOUT_MS = 30 * 60 * 1000L

        private var instance: AlarmForegroundService? = null

        // Every start() gets a new generation; stop() cancels everything up to the latest one.
        // This covers a stop that lands after start() but before onStartCommand() runs.
        private val startGeneration = java.util.concurrent.atomic.AtomicLong(0)
        @Volatile private var cancelledGeneration = 0L
        private val _isAlarmRunningFlow = MutableStateFlow(false)
        val isAlarmRunningFlow: StateFlow<Boolean> = _isAlarmRunningFlow.asStateFlow()

        /** Urgent messages that arrived while this alarm was already ringing (recorded, not rung). */
        private val _extraMatchesFlow = MutableStateFlow(0)
        val extraMatchesFlow: StateFlow<Int> = _extraMatchesFlow.asStateFlow()

        fun isRunning(): Boolean = _isAlarmRunningFlow.value

        fun reportExtraMatch() {
            if (_isAlarmRunningFlow.value) _extraMatchesFlow.value += 1
        }

        /**
         * Start the alarm. The running flag flips immediately so a second notification arriving
         * before the service is created can't start a second alarm.
         *
         * @return false if the system refused to start the foreground service.
         */
        fun start(context: Context, request: AlarmRequest): Boolean {
            _isAlarmRunningFlow.value = true
            _extraMatchesFlow.value = 0
            val intent = request.writeTo(Intent(context, AlarmForegroundService::class.java))
                .putExtra(EXTRA_GENERATION, startGeneration.incrementAndGet())
            return try {
                context.startForegroundService(intent)
                true
            } catch (e: Exception) {
                // e.g. ForegroundServiceStartNotAllowedException on Android 12+
                Log.e(TAG, "Unable to start alarm service", e)
                _isAlarmRunningFlow.value = false
                false
            }
        }

        fun stop() {
            cancelledGeneration = startGeneration.get()
            _isAlarmRunningFlow.value = false
            _extraMatchesFlow.value = 0
            instance?.shutdown()
        }

        fun createChannel(context: Context) {
            val res = AppLocale.wrap(context)
            val channel = NotificationChannel(
                CHANNEL_ID_ALARM,
                res.getString(R.string.channel_alarm_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = res.getString(R.string.channel_alarm_desc)
                enableVibration(false) // Vibration handled by AlarmSoundPlayer
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null) // Sound handled by AlarmSoundPlayer
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        /**
         * @param startServiceOnOpen true for the fallback notification posted when the service
         * couldn't start from the background; opening the alarm screen then starts it.
         */
        fun buildAlarmNotification(
            context: Context,
            request: AlarmRequest,
            startServiceOnOpen: Boolean = false
        ): Notification {
            val res = AppLocale.wrap(context)
            val fullScreenIntent = request.writeTo(Intent(context, AlarmTriggerActivity::class.java)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(AlarmTriggerActivity.EXTRA_START_SERVICE, startServiceOnOpen)
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context, 0, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val dismissPendingIntent = PendingIntent.getService(
                context, 1,
                Intent(context, AlarmForegroundService::class.java).setAction(ACTION_STOP_ALARM),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = if (request.sender.isNotBlank()) {
                res.getString(R.string.alarm_notif_title_sender, request.sender)
            } else {
                res.getString(R.string.alarm_notif_title)
            }

            return NotificationCompat.Builder(context, CHANNEL_ID_ALARM)
                .setSmallIcon(R.drawable.ic_stat_alarm)
                .setColor(NotificationAccent)
                .setContentTitle(title)
                .setContentText(
                    res.getString(R.string.alarm_notif_keywords, request.matchedKeywords.joinToString(", "))
                )
                .setStyle(NotificationCompat.BigTextStyle().bigText(request.message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(fullScreenPendingIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                // In case an OEM lets the user swipe the ongoing notification away
                .setDeleteIntent(dismissPendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(R.drawable.ic_stat_alarm_off, res.getString(R.string.action_stop_alarm), dismissPendingIntent)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build()
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var historyRepository: AlarmHistoryRepository
    private var soundPlayer: AlarmSoundPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsRepository = SettingsRepository(applicationContext)
        historyRepository = AlarmHistoryRepository(applicationContext)
        createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALARM) {
            _isAlarmRunningFlow.value = false
            _extraMatchesFlow.value = 0
            shutdown()
            return START_NOT_STICKY
        }

        val request = AlarmRequest.from(intent)
            ?: AlarmRequest(emptyList(), "", "")
        // startForegroundService() obliges us to call startForeground() even if we stop right away
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID_ALARM,
            buildAlarmNotification(this, request),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        )

        val generation = intent?.getLongExtra(EXTRA_GENERATION, 0L) ?: 0L
        if (request.matchedKeywords.isEmpty() || generation <= cancelledGeneration) {
            shutdown()
            return START_NOT_STICKY
        }
        _isAlarmRunningFlow.value = true

        if (wakeLock?.isHeld != true) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeAlarm::AlarmWakeLock").apply {
                acquire(WAKE_LOCK_TIMEOUT_MS)
            }
        }

        // The full-screen intent only shows a heads-up while the phone is unlocked and in use.
        // With "display over other apps" granted we're allowed to open the alarm screen directly.
        if (Settings.canDrawOverlays(this)) {
            try {
                startActivity(
                    request.writeTo(Intent(this, AlarmTriggerActivity::class.java))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Direct activity launch failed, relying on full-screen intent", e)
            }
        }

        serviceScope.launch {
            val options = AlarmSoundPlayer.Options(
                customUri = settingsRepository.customRingtoneUriFlow.first(),
                enableRampUp = settingsRepository.volumeRampUpFlow.first(),
                vibrate = settingsRepository.vibrationFlow.first(),
                forceMaxVolume = true
            )
            // A stop may have arrived while settings were loading
            if (!_isAlarmRunningFlow.value) return@launch
            soundPlayer?.stop()
            soundPlayer = AlarmSoundPlayer(this@AlarmForegroundService).apply { start(options) }

            if (!request.isTest) {
                historyRepository.add(
                    AlarmEvent(
                        timestamp = System.currentTimeMillis(),
                        sender = request.sender,
                        message = request.message,
                        keywords = request.matchedKeywords
                    )
                )
            }
        }

        return START_NOT_STICKY
    }

    private fun shutdown() {
        soundPlayer?.stop()
        soundPlayer = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        // Also clears the fallback notification, which isn't owned by the foreground service
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID_ALARM)
        stopSelf()
    }

    override fun onDestroy() {
        _isAlarmRunningFlow.value = false
        _extraMatchesFlow.value = 0
        serviceScope.cancel()
        soundPlayer?.stop()
        soundPlayer = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
