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
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.outlined.MarkChatUnread
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kindness.wakealarm.R
import com.kindness.wakealarm.service.AlarmForegroundService
import com.kindness.wakealarm.service.AlarmForegroundService.AlarmRequest
import com.kindness.wakealarm.ui.components.Companion3D
import com.kindness.wakealarm.ui.components.CompanionMood
import com.kindness.wakealarm.ui.components.HsrButton
import com.kindness.wakealarm.ui.components.HsrButtonStyle
import com.kindness.wakealarm.ui.components.KeywordPill
import com.kindness.wakealarm.ui.components.StarfieldBackground
import com.kindness.wakealarm.ui.components.hsrPanel
import com.kindness.wakealarm.ui.components.rememberHaptics
import com.kindness.wakealarm.ui.theme.AlarmAccent
import com.kindness.wakealarm.ui.theme.Crimson
import com.kindness.wakealarm.ui.theme.CrimsonDeep
import com.kindness.wakealarm.ui.theme.Gold
import com.kindness.wakealarm.ui.theme.GoldSoft
import com.kindness.wakealarm.ui.theme.Ivory
import com.kindness.wakealarm.ui.theme.Mist
import com.kindness.wakealarm.ui.theme.OnCrimsonContainer
import com.kindness.wakealarm.ui.theme.OnGold
import com.kindness.wakealarm.ui.theme.OnVioletContainer
import com.kindness.wakealarm.ui.theme.PanelLow
import com.kindness.wakealarm.ui.theme.Spacing
import com.kindness.wakealarm.ui.theme.VioletContainer
import com.kindness.wakealarm.ui.theme.WakeAlarmTheme
import com.kindness.wakealarm.ui.theme.WakeType
import com.kindness.wakealarm.util.AppLocale
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

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
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
            WakeAlarmTheme {
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
    val extraMatches by AlarmForegroundService.extraMatchesFlow.collectAsStateWithLifecycle()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    val timeText = DateFormat.getTimeFormat(context).format(now)
    val isTest = request?.isTest == true

    StarfieldBackground(Modifier.fillMaxSize(), nebula = 2.6f, nebulaColor = AlarmAccent) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                stringResource(if (isTest) R.string.alarm_screen_title_test else R.string.alarm_screen_title).uppercase(),
                style = WakeType.eyebrow,
                color = if (isTest) GoldSoft else AlarmAccent,
                modifier = Modifier.semantics { heading() }
            )
            Text(timeText, style = MaterialTheme.typography.displayLarge, color = Ivory)

            Companion3D(size = 176.dp, mood = CompanionMood.Alarm)

            if (!request?.sender.isNullOrBlank()) {
                Text(
                    stringResource(R.string.alarm_screen_from, request!!.sender),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Ivory,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (request != null) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .hsrPanel(MaterialTheme.shapes.large, PanelLow.copy(alpha = 0.78f), AlarmAccent)
                        .padding(Spacing.gutter),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    if (request.message.isNotBlank()) {
                        Text(
                            request.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Ivory,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (request.matchedKeywords.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            request.matchedKeywords.forEach { kw ->
                                KeywordPill(kw.uppercase(), container = Crimson.copy(alpha = 0.3f), content = OnCrimsonContainer)
                            }
                        }
                    }
                }
            }

            if (extraMatches > 0) {
                Row(
                    Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(VioletContainer.copy(alpha = 0.85f))
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                        .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.MarkChatUnread, contentDescription = null, tint = OnVioletContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        pluralStringResource(R.plurals.alarm_extra_matches, extraMatches, extraMatches),
                        style = MaterialTheme.typography.labelMedium,
                        color = OnVioletContainer
                    )
                }
            }

            Spacer(Modifier.weight(1f, fill = false))
            Spacer(Modifier.height(Spacing.sm))

            SlideToStop(label = stringResource(R.string.alarm_slide_to_stop), onComplete = onDismiss)

            if (request?.chatIntent != null) {
                HsrButton(
                    text = stringResource(R.string.alarm_stop_and_open_chat),
                    onClick = onOpenChat,
                    style = HsrButtonStyle.Secondary,
                    icon = Icons.AutoMirrored.Filled.Chat,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(
                stringResource(R.string.tagline),
                style = MaterialTheme.typography.labelMedium,
                color = Mist.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Drag the gold thumb to the end to stop. Prevents pocket/half-asleep accidental dismissal while
 * remaining one deliberate gesture; ticks at each quarter so the hand feels the progress.
 * Accessibility services get a plain click action.
 */
@Composable
private fun SlideToStop(label: String, onComplete: () -> Unit) {
    val density = LocalDensity.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val thumbSize = 64.dp
    val padding = 6.dp
    var trackWidthPx by remember { mutableIntStateOf(0) }
    val maxOffset = with(density) { (trackWidthPx - (thumbSize + padding * 2).toPx()).coerceAtLeast(0f) }
    val offset = remember { Animatable(0f) }
    var completed by remember { mutableStateOf(false) }
    var lastQuarter by remember { mutableIntStateOf(0) }
    val progress = if (maxOffset > 0f) offset.value / maxOffset else 0f
    val trackShape = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp)

    fun complete() {
        if (completed) return
        completed = true
        haptics.confirm()
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(thumbSize + padding * 2)
            .clip(trackShape)
            .background(Brush.horizontalGradient(listOf(CrimsonDeep.copy(alpha = 0.85f), Crimson.copy(alpha = 0.55f))), trackShape)
            .border(1.dp, Brush.linearGradient(listOf(Gold.copy(alpha = 0.7f), Gold.copy(alpha = 0.15f))), trackShape)
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
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = thumbSize + padding * 2, end = Spacing.lg)
                .alpha((1f - progress * 1.4f).coerceIn(0f, 1f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelLarge, color = Color.White)
            Spacer(Modifier.width(Spacing.xs))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .padding(padding)
                .size(thumbSize)
                .clip(MaterialTheme.shapes.small)
                .background(Brush.verticalGradient(listOf(Color(0xFFF6DC9C), Gold, Color(0xFFD9B055))))
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = !completed,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            offset.snapTo((offset.value + delta).coerceIn(0f, maxOffset))
                            val quarter = if (maxOffset > 0f) (offset.value / maxOffset * 4f).toInt() else 0
                            if (quarter != lastQuarter) {
                                lastQuarter = quarter
                                haptics.tick()
                            }
                        }
                    },
                    onDragStopped = {
                        if (offset.value >= maxOffset * 0.8f) {
                            offset.animateTo(maxOffset)
                            complete()
                        } else {
                            lastQuarter = 0
                            offset.animateTo(0f)
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.AlarmOff, contentDescription = null, tint = OnGold, modifier = Modifier.size(28.dp))
        }
    }
}
