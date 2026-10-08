package com.kindness.wakealarm.ui.home

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kindness.wakealarm.R
import com.kindness.wakealarm.service.MasterSwitchTileService
import com.kindness.wakealarm.ui.components.Banner
import com.kindness.wakealarm.ui.components.BannerTone
import com.kindness.wakealarm.ui.components.Companion3D
import com.kindness.wakealarm.ui.components.FooterTagline
import com.kindness.wakealarm.ui.components.HsrButton
import com.kindness.wakealarm.ui.components.HsrButtonStyle
import com.kindness.wakealarm.ui.components.OnResume
import com.kindness.wakealarm.ui.components.RarityStars
import com.kindness.wakealarm.ui.components.StatTile
import com.kindness.wakealarm.ui.components.WakeSnackbarHost
import com.kindness.wakealarm.ui.components.WakeTopBar
import com.kindness.wakealarm.ui.components.hsrPanel
import com.kindness.wakealarm.ui.components.readableWidth
import com.kindness.wakealarm.ui.components.rememberHaptics
import com.kindness.wakealarm.ui.theme.Gold
import com.kindness.wakealarm.ui.theme.GoldSoft
import com.kindness.wakealarm.ui.theme.LineStrong
import com.kindness.wakealarm.ui.theme.Mist
import com.kindness.wakealarm.ui.theme.PanelLow
import com.kindness.wakealarm.ui.theme.Spacing
import com.kindness.wakealarm.ui.theme.TouchTarget
import com.kindness.wakealarm.ui.theme.WakeType
import com.kindness.wakealarm.ui.theme.statusColors
import kotlinx.coroutines.launch
import java.util.Calendar

/** What keeps the alarm from being fully ready, most severe first. */
private enum class Issue { Permissions, Listener, Keywords }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToKeywords: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val masterSwitch by viewModel.masterSwitchState.collectAsStateWithLifecycle()
    val keywordsCount by viewModel.activeKeywordsCount.collectAsStateWithLifecycle()
    val threshold by viewModel.threshold.collectAsStateWithLifecycle()
    val permissionStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()
    val isAlarmActive by viewModel.isAlarmActive.collectAsStateWithLifecycle()
    val listenerConnected by viewModel.isListenerConnected.collectAsStateWithLifecycle()
    val lastAlarm by viewModel.lastAlarm.collectAsStateWithLifecycle()

    OnResume { viewModel.refreshPermissions() }

    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var confirmNotReady by rememberSaveable { mutableStateOf(false) }

    // null while the first disk read is in flight: show placeholders, not a false warning
    val count = keywordsCount
    val minimum = threshold
    val keywordsShort = count != null && minimum != null && count < minimum

    val issues = buildList {
        if (!permissionStatus.requiredGranted) add(Issue.Permissions)
        else if (!listenerConnected) add(Issue.Listener)
        if (keywordsShort) add(Issue.Keywords)
    }

    val snackStandbyOn = stringResource(R.string.snack_standby_on)
    val snackStandbyOff = stringResource(R.string.snack_standby_off)
    val snackTestFailed = stringResource(R.string.snack_test_failed)

    fun fix(issue: Issue) = when (issue) {
        Issue.Permissions -> onNavigateToPermissions()
        Issue.Listener -> viewModel.reconnectListener()
        Issue.Keywords -> onNavigateToKeywords()
    }

    fun setStandby(enabled: Boolean) {
        viewModel.setMasterSwitch(enabled)
        haptics.confirm()
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(if (enabled) snackStandbyOn else snackStandbyOff)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            WakeTopBar(
                title = stringResource(R.string.app_name),
                eyebrow = stringResource(greetingRes()),
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { WakeSnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter)
                .readableWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Spacer(Modifier.height(Spacing.xs))

            AnimatedVisibility(visible = isAlarmActive) {
                ActiveAlarmCard(onStop = { viewModel.stopActiveAlarm() })
            }

            // Only the most important problem gets a banner; the hero shows the full status
            when (issues.firstOrNull()) {
                Issue.Permissions -> Banner(
                    tone = BannerTone.Warning,
                    icon = Icons.Outlined.ReportProblem,
                    title = pluralStringResource(
                        R.plurals.home_setup_incomplete_title,
                        permissionStatus.missingRequiredCount,
                        permissionStatus.missingRequiredCount
                    ),
                    body = stringResource(R.string.home_setup_incomplete_body),
                    actionLabel = stringResource(R.string.action_fix_now),
                    onAction = onNavigateToPermissions
                )
                Issue.Listener -> Banner(
                    tone = BannerTone.Warning,
                    icon = Icons.Outlined.LinkOff,
                    title = stringResource(R.string.home_listener_disconnected_title),
                    body = stringResource(R.string.home_listener_disconnected_body),
                    actionLabel = stringResource(R.string.action_reconnect),
                    onAction = { viewModel.reconnectListener() }
                )
                Issue.Keywords -> Banner(
                    tone = BannerTone.Alarm,
                    icon = Icons.Outlined.Key,
                    title = stringResource(R.string.home_keywords_insufficient_title),
                    body = stringResource(R.string.home_keywords_insufficient_body, count ?: 0, minimum ?: 0),
                    actionLabel = stringResource(R.string.action_manage_keywords),
                    onAction = onNavigateToKeywords
                )
                null -> Unit
            }

            StandbyHero(
                enabled = masterSwitch,
                issues = issues,
                onFix = ::fix,
                onToggle = {
                    when {
                        masterSwitch -> setStandby(false)
                        issues.isNotEmpty() -> {
                            haptics.reject()
                            confirmNotReady = true
                        }
                        else -> setStandby(true)
                    }
                }
            )

            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                StatTile(
                    label = stringResource(R.string.stat_active_keywords),
                    value = count?.toString() ?: "—",
                    valueColor = if (keywordsShort) MaterialTheme.statusColors.alarm else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                val thresholdDescription = stringResource(R.string.stat_threshold_cd, minimum ?: 0)
                StatTile(
                    label = stringResource(R.string.stat_threshold),
                    value = minimum?.let { "≥$it" } ?: "—",
                    footer = { RarityStars(minimum ?: 0, size = 11.dp) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clearAndSetSemantics { contentDescription = thresholdDescription }
                )
                StatTile(
                    label = stringResource(R.string.stat_last_alarm),
                    value = lastAlarm?.let { shortRelativeTime(it.timestamp) } ?: "—",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(role = Role.Button, onClick = onNavigateToHistory)
                )
            }

            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                HsrButton(
                    text = stringResource(R.string.action_test_alarm),
                    onClick = {
                        if (!viewModel.startTestAlarm()) {
                            haptics.reject()
                            scope.launch { snackbarHostState.showSnackbar(snackTestFailed) }
                        }
                    },
                    style = HsrButtonStyle.Secondary,
                    icon = Icons.Outlined.NotificationsActive,
                    enabled = !isAlarmActive,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    HsrButton(
                        text = stringResource(R.string.action_add_tile),
                        onClick = {
                            val sbm = context.getSystemService(StatusBarManager::class.java)
                            sbm.requestAddTileService(
                                ComponentName(context, MasterSwitchTileService::class.java),
                                context.getString(R.string.tile_label),
                                Icon.createWithResource(context, R.drawable.ic_stat_alarm),
                                context.mainExecutor
                            ) { result ->
                                if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                                    Toast.makeText(context, R.string.tile_already_added, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        style = HsrButtonStyle.Secondary,
                        icon = Icons.Outlined.Dashboard,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = Spacing.sm, start = Spacing.xs, end = Spacing.xs),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = Mist, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(R.string.privacy_short), style = MaterialTheme.typography.bodySmall, color = Mist)
            }

            FooterTagline()
        }
    }

    if (confirmNotReady) {
        NotReadyDialog(
            issues = issues,
            onFix = {
                confirmNotReady = false
                issues.firstOrNull()?.let(::fix)
            },
            onEnableAnyway = {
                confirmNotReady = false
                setStandby(true)
            },
            onDismiss = { confirmNotReady = false }
        )
    }
}

private fun greetingRes(): Int = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 4..10 -> R.string.greeting_morning
    in 11..14 -> R.string.greeting_afternoon
    in 15..17 -> R.string.greeting_evening
    else -> R.string.greeting_night
}

/**
 * Compact "59m / 3h / 2d"-style age that fits a stat tile, in the app's language
 * (DateUtils would use the system language and is too long for the tile).
 */
@Composable
private fun shortRelativeTime(timestamp: Long): String {
    val minutes = ((System.currentTimeMillis() - timestamp) / 60_000L).coerceAtLeast(0L).toInt()
    return when {
        minutes < 1 -> stringResource(R.string.time_short_now)
        minutes < 60 -> stringResource(R.string.time_short_minutes, minutes)
        minutes < 24 * 60 -> stringResource(R.string.time_short_hours, minutes / 60)
        else -> stringResource(R.string.time_short_days, minutes / (24 * 60))
    }
}

@Composable
private fun ActiveAlarmCard(onStop: () -> Unit) {
    val status = MaterialTheme.statusColors
    Column(
        Modifier
            .fillMaxWidth()
            .hsrPanel(MaterialTheme.shapes.large, status.alarmContainer.copy(alpha = 0.92f), status.alarm)
            .padding(Spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = status.alarm)
            Spacer(Modifier.width(Spacing.md))
            Text(stringResource(R.string.home_alarm_ringing_title), style = MaterialTheme.typography.titleLarge, color = status.alarm)
        }
        Text(stringResource(R.string.home_alarm_ringing_body), style = MaterialTheme.typography.bodyMedium)
        HsrButton(
            text = stringResource(R.string.action_stop_alarm),
            onClick = onStop,
            style = HsrButtonStyle.Danger,
            icon = Icons.Filled.AlarmOff,
            minHeight = TouchTarget.hero,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * The Master Switch as the screen's hero: companion, big state, readiness checks and one large
 * toggle button low in the card, within easy thumb reach.
 */
@Composable
private fun StandbyHero(
    enabled: Boolean,
    issues: List<Issue>,
    onFix: (Issue) -> Unit,
    onToggle: () -> Unit
) {
    val ready = issues.isEmpty()
    val stateColor by animateColorAsState(if (enabled) Gold else Mist, label = "standby_state")
    val stateText = stringResource(if (enabled) R.string.standby_state_on else R.string.standby_state_off)

    Column(
        Modifier
            .fillMaxWidth()
            .hsrPanel(
                MaterialTheme.shapes.extraLarge,
                container = PanelLow.copy(alpha = 0.85f),
                accent = if (enabled) Gold else LineStrong
            )
            .padding(horizontal = Spacing.gutter, vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Smaller on short screens so the standby button stays in view without scrolling
        val screenHeight = LocalConfiguration.current.screenHeightDp
        Companion3D(
            size = when {
                screenHeight < 700 -> 104.dp
                screenHeight < 800 -> 136.dp
                else -> 168.dp
            }
        )
        Text(stringResource(R.string.standby_title).uppercase(), style = WakeType.eyebrow, color = GoldSoft)
        Text(
            stateText.uppercase(),
            style = MaterialTheme.typography.displaySmall,
            color = stateColor,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(
                when {
                    enabled && ready -> R.string.standby_desc_on
                    enabled -> R.string.standby_desc_on_not_ready
                    else -> R.string.standby_desc_off
                }
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = Mist,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.lg))
        ReadinessRow(issues = issues, onFix = onFix)
        Spacer(Modifier.height(Spacing.lg))
        HsrButton(
            text = stringResource(if (enabled) R.string.action_standby_off else R.string.action_standby_on),
            onClick = onToggle,
            style = if (enabled) HsrButtonStyle.Secondary else HsrButtonStyle.Primary,
            icon = if (enabled) Icons.Filled.AlarmOff else Icons.Filled.Alarm,
            minHeight = TouchTarget.hero,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Three checks at a glance; a failing one can be tapped to fix it. */
@Composable
private fun ReadinessRow(issues: List<Issue>, onFix: (Issue) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        listOf(
            Issue.Permissions to R.string.readiness_permissions,
            Issue.Listener to R.string.readiness_listener,
            Issue.Keywords to R.string.readiness_keywords
        ).forEach { (issue, labelRes) ->
            val label = stringResource(labelRes)
            val ok = issue !in issues
            val description = stringResource(if (ok) R.string.readiness_ok_cd else R.string.readiness_fix_cd, label)
            val status = MaterialTheme.statusColors
            val tint = if (ok) status.success else status.warning
            // Icon above the label so "Kata kunci" fits on narrow phones and at large font sizes
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = TouchTarget.min)
                    .clip(MaterialTheme.shapes.small)
                    .then(if (ok) Modifier else Modifier.clickable { onFix(issue) })
                    .hsrPanel(
                        MaterialTheme.shapes.small,
                        container = (if (ok) status.successContainer else status.warningContainer).copy(alpha = 0.6f),
                        accent = tint,
                        ornament = false
                    )
                    .padding(horizontal = Spacing.xs, vertical = Spacing.sm)
                    .clearAndSetSemantics {
                        contentDescription = description
                        if (!ok) role = Role.Button
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    if (ok) Icons.Filled.CheckCircle else Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NotReadyDialog(
    issues: List<Issue>,
    onFix: () -> Unit,
    onEnableAnyway: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.ReportProblem, contentDescription = null, tint = MaterialTheme.statusColors.warning) },
        title = { Text(stringResource(R.string.not_ready_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(stringResource(R.string.not_ready_body))
                issues.forEach { issue ->
                    Text(
                        "• " + stringResource(
                            when (issue) {
                                Issue.Permissions -> R.string.not_ready_permissions
                                Issue.Listener -> R.string.not_ready_listener
                                Issue.Keywords -> R.string.not_ready_keywords
                            }
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onFix) { Text(stringResource(R.string.action_fix_now)) }
        },
        dismissButton = {
            TextButton(onClick = onEnableAnyway) { Text(stringResource(R.string.action_enable_anyway)) }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
