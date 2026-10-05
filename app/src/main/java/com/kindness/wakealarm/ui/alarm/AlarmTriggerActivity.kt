package com.kindness.wakealarm.ui.alarm

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kindness.wakealarm.R
import com.kindness.wakealarm.service.AlarmForegroundService
import com.kindness.wakealarm.service.AlarmForegroundService.AlarmRequest
import com.kindness.wakealarm.ui.components.KeywordPill
import com.kindness.wakealarm.ui.theme.AlarmAccent
import com.kindness.wakealarm.ui.theme.AlarmScreenBottom
import com.kindness.wakealarm.ui.theme.AlarmScreenMid
import com.kindness.wakealarm.ui.theme.AlarmScreenTop
import com.kindness.wakealarm.ui.theme.WakeAlarmTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Full-screen alarm shown over the lockscreen. Closes itself as soon as the alarm stops,
 * no matter where it was stopped from (this screen, the notification, or the app).
 */
class AlarmTriggerActivity : ComponentActivity() {

    companion object {
        /** Set only by the fallback notification, when the service couldn't be started from the background. */
        const val EXTRA_START_SERVICE = "start_service_on_open"
    }

    private var request by mutableStateOf<AlarmRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        showOverLockscreen()

        request = AlarmRequest.from(intent)
        if (!ensureAlarmRunning(intent, savedInstanceState == null)) {
            finish()
            return
        }

        // Back must not silence the alarm by accident; it has to be stopped deliberately
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AlarmForegroundService.isAlarmRunningFlow.collect { running ->
                    if (!running) finishAndRemoveTask()
                }
            }
        }

        setContent {
            WakeAlarmTheme(darkTheme = true) {
                AlarmScreen(
                    request = request,
                    onDismiss = ::dismissAlarm,
                    onOpenChat = ::dismissAndOpenChat
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AlarmRequest.from(intent)?.let { request = it }
        ensureAlarmRunning(intent, firstLaunch = true)
    }

    /**
     * @return false when this screen is stale (the alarm was already stopped) and should close.
     */
    private fun ensureAlarmRunning(intent: Intent?, firstLaunch: Boolean): Boolean {
        if (AlarmForegroundService.isRunning()) return true
        val pending = AlarmRequest.from(intent)
        if (firstLaunch && pending != null && intent?.getBooleanExtra(EXTRA_START_SERVICE, false) == true) {
            // We're in the foreground now, so starting the foreground service is allowed
            return AlarmForegroundService.start(this, pending)
        }
        return false
    }

    private fun showOverLockscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        // Keep screen on while alarm is active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun dismissAlarm() {
        AlarmForegroundService.stop()
        finishAndRemoveTask()
    }

    private fun dismissAndOpenChat() {
        val chatIntent = request?.chatIntent
        AlarmForegroundService.stop()
        // Unlock first so the chat isn't hidden behind the keyguard
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            km.requestDismissKeyguard(this, null)
        }
        if (chatIntent != null) {
            try {
                val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                        .toBundle()
                } else {
                    null
                }
                chatIntent.send(this, 0, null, null, null, null, options)
            } catch (_: PendingIntent.CanceledException) {
                // WhatsApp already cancelled this notification (e.g. read on another device)
            }
        }
        finishAndRemoveTask()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlarmScreen(
    request: AlarmRequest?,
    onDismiss: () -> Unit,
    onOpenChat: () -> Unit
) {
    val context = LocalContext.current
    val pulse = rememberInfiniteTransition(label = "alarm_pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_scale"
    )
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_alpha"
    )

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    val timeText = DateFormat.getTimeFormat(context).format(now)

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AlarmScreenTop, AlarmScreenMid, AlarmScreenBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                timeText,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )

            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(120.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(AlarmAccent.copy(alpha = glowAlpha))
                )
                Box(
                    Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFFFF6B81), Color(0xFFD50032)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(if (request?.isTest == true) R.string.alarm_screen_title_test else R.string.alarm_screen_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
                if (!request?.sender.isNullOrBlank()) {
                    Text(
                        stringResource(R.string.alarm_screen_from, request!!.sender),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (request != null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(Color.White.copy(alpha = 0.10f))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (request.message.isNotBlank()) {
                        Text(
                            request.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (request.matchedKeywords.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            request.matchedKeywords.forEach { kw ->
                                KeywordPill(kw.uppercase(), container = AlarmAccent.copy(alpha = 0.3f), content = Color(0xFFFFE3E7))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f, fill = false))

            SlideToStop(label = stringResource(R.string.alarm_slide_to_stop), onComplete = onDismiss)

            if (request?.chatIntent != null) {
                OutlinedButton(
                    onClick = onOpenChat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        stringResource(R.string.alarm_stop_and_open_chat),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Text(
                stringResource(R.string.tagline),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Drag the thumb to the end to stop. Prevents pocket/half-asleep accidental dismissal while
 * remaining one deliberate gesture. Accessibility services get a plain click action.
 */
@Composable
private fun SlideToStop(label: String, onComplete: () -> Unit) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val thumbSize = 64.dp
    val padding = 6.dp
    var trackWidthPx by remember { mutableIntStateOf(0) }
    val maxOffset = with(density) { (trackWidthPx - (thumbSize + padding * 2).toPx()).coerceAtLeast(0f) }
    val offset = remember { Animatable(0f) }
    var completed by remember { mutableStateOf(false) }
    val progress = if (maxOffset > 0f) offset.value / maxOffset else 0f

    fun complete() {
        if (completed) return
        completed = true
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(thumbSize + padding * 2)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .onSizeChanged { trackWidthPx = it.width }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = label
                onClick(label) {
                    complete()
                    true
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = thumbSize + padding)
                .alpha((1f - progress * 1.4f).coerceIn(0f, 1f))
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .padding(padding)
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White)
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = !completed,
                    state = rememberDraggableState { delta ->
                        scope.launch { offset.snapTo((offset.value + delta).coerceIn(0f, maxOffset)) }
                    },
                    onDragStopped = {
                        if (offset.value >= maxOffset * 0.8f) {
                            offset.animateTo(maxOffset)
                            complete()
                        } else {
                            offset.animateTo(0f)
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.AlarmOff, contentDescription = null, tint = Color(0xFFC4002B), modifier = Modifier.size(28.dp))
        }
    }
}
