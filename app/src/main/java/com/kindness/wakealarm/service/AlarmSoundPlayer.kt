package com.kindness.wakealarm.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Manages alarm sound playback and vibration.
 * Supports custom audio / MP3 URI or default system alarm ringtone.
 * Configured with USAGE_ALARM to override silent/DND mode.
 *
 * Each instance is independent: the real alarm (owned by [AlarmForegroundService]) and the
 * settings preview never stop each other. When [Options.forceMaxVolume] is used, the user's
 * original STREAM_ALARM volume is restored on [stop].
 */
class AlarmSoundPlayer(context: Context) {

    data class Options(
        val customUri: String? = null,
        val enableRampUp: Boolean = true,
        val vibrate: Boolean = true,
        val forceMaxVolume: Boolean = true
    )

    companion object {
        private const val TAG = "AlarmSoundPlayer"
        private const val RAMP_START = 0.3f
        private const val RAMP_STEP = 0.05f
        private const val RAMP_INTERVAL_MS = 350L
    }

    private val context = context.applicationContext
    private val audioManager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val alarmAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var focusRequest: AudioFocusRequest? = null
    private var rampUpJob: Job? = null
    private var originalAlarmVolume: Int? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Start playing the alarm sound (and vibrating, if enabled). Restarts if already playing.
     */
    fun start(options: Options = Options()) {
        stop()

        if (options.forceMaxVolume) {
            // Force STREAM_ALARM to 100% so silent/vibrate/DND or a low alarm volume can't mute it
            originalAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
            } catch (e: SecurityException) {
                Log.w(TAG, "Unable to change alarm volume", e)
                originalAlarmVolume = null
            }
        }

        requestAudioFocus()

        val targetUri = if (!options.customUri.isNullOrBlank()) {
            Uri.parse(options.customUri)
        } else {
            defaultAlarmUri()
        }
        if (targetUri != null) playAudio(targetUri, options.enableRampUp)

        if (options.vibrate) startVibration()
    }

    private fun defaultAlarmUri(): Uri? =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

    private fun playAudio(uri: Uri, enableRampUp: Boolean) {
        val initialVolume = if (enableRampUp) RAMP_START else 1.0f

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(alarmAttributes)
                // Keeps the CPU awake for exactly as long as the sound is playing
                setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(context, uri)
                isLooping = true
                setVolume(initialVolume, initialVolume)
                prepare()
                start()
            }

            if (enableRampUp) {
                startVolumeRampUp()
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer failed for URI: $uri, falling back to Ringtone", e)
            mediaPlayer?.release()
            mediaPlayer = null
            // A custom file may have been deleted or lost its permission: fall back to the system alarm
            if (!playFallbackRingtone(uri)) {
                defaultAlarmUri()?.let { playFallbackRingtone(it) }
            }
        }
    }

    private fun playFallbackRingtone(uri: Uri): Boolean {
        return try {
            ringtone = RingtoneManager.getRingtone(context, uri)?.apply {
                audioAttributes = alarmAttributes
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isLooping = true
                }
                play()
            }
            ringtone != null
        } catch (e: Exception) {
            Log.e(TAG, "Fallback ringtone also failed", e)
            false
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 500, 300, 500, 300, 800)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(pattern, 0),
                android.os.VibrationAttributes.createForUsage(android.os.VibrationAttributes.USAGE_ALARM)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0), alarmAttributes)
        }
    }

    private fun requestAudioFocus() {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(alarmAttributes)
            .build()
        audioManager.requestAudioFocus(request)
        focusRequest = request
    }

    private fun startVolumeRampUp() {
        rampUpJob?.cancel()
        rampUpJob = scope.launch {
            // Ramp up volume from 0.3f to 1.0f over ~5 seconds (14 steps of 350ms)
            var currentVol = RAMP_START
            while (currentVol < 1.0f) {
                delay(RAMP_INTERVAL_MS)
                currentVol = (currentVol + RAMP_STEP).coerceAtMost(1.0f)
                mediaPlayer?.setVolume(currentVol, currentVol)
            }
            mediaPlayer?.setVolume(1.0f, 1.0f)
        }
    }

    /**
     * Stop the alarm sound, ramp-up coroutine, and vibration, and restore the user's alarm volume.
     */
    fun stop() {
        rampUpJob?.cancel()
        rampUpJob = null

        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping MediaPlayer", e)
            }
        }
        mediaPlayer = null

        ringtone?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping Ringtone", e)
            }
        }
        ringtone = null

        vibrator?.cancel()
        vibrator = null

        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null

        originalAlarmVolume?.let { volume ->
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0)
            } catch (e: SecurityException) {
                Log.w(TAG, "Unable to restore alarm volume", e)
            }
        }
        originalAlarmVolume = null
    }

    /**
     * Check if the alarm is currently playing.
     */
    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true || ringtone?.isPlaying == true
}
