package com.kindness.wakealarm.ui.home

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kindness.wakealarm.R
import com.kindness.wakealarm.service.MasterSwitchTileService
import com.kindness.wakealarm.ui.components.Banner
import com.kindness.wakealarm.ui.components.BannerTone
import com.kindness.wakealarm.ui.components.FooterTagline
import com.kindness.wakealarm.ui.components.GroupCard
import com.kindness.wakealarm.ui.components.GroupDivider
import com.kindness.wakealarm.ui.components.NavRow
import com.kindness.wakealarm.ui.components.OnResume
import com.kindness.wakealarm.ui.components.SectionHeader
import com.kindness.wakealarm.ui.components.StatTile
import com.kindness.wakealarm.ui.components.WakeTopBar
import com.kindness.wakealarm.ui.theme.statusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToKeywords: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val masterSwitch by viewModel.masterSwitchState.collectAsStateWithLifecycle()
    val keywordsCount by viewModel.activeKeywordsCount.collectAsStateWithLifecycle()
    val threshold by viewModel.threshold.collectAsStateWithLifecycle()
    val ringtoneTitle by viewModel.customRingtoneTitle.collectAsStateWithLifecycle()
    val permissionStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()
    val isAlarmActive by viewModel.isAlarmActive.collectAsStateWithLifecycle()
    val listenerConnected by viewModel.isListenerConnected.collectAsStateWithLifecycle()
    val lastAlarm by viewModel.lastAlarm.collectAsStateWithLifecycle()

    OnResume { viewModel.refreshPermissions() }

    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val keywordsInsufficient = keywordsCount < threshold

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            WakeTopBar(title = stringResource(R.string.app_name), scrollBehavior = scrollBehavior) {
                IconButton(onClick = onNavigateToHistory) {
                    Icon(Icons.Filled.History, contentDescription = stringResource(R.string.history_title))
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            AnimatedVisibility(visible = isAlarmActive) {
                ActiveAlarmCard(onStop = { viewModel.stopActiveAlarm() })
            }

            if (!permissionStatus.requiredGranted) {
                Banner(
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
            } else if (!listenerConnected) {
                Banner(
                    tone = BannerTone.Warning,
                    icon = Icons.Outlined.LinkOff,
                    title = stringResource(R.string.home_listener_disconnected_title),
                    body = stringResource(R.string.home_listener_disconnected_body),
                    actionLabel = stringResource(R.string.action_reconnect),
                    onAction = { viewModel.reconnectListener() }
                )
            }

            if (keywordsInsufficient) {
                Banner(
                    tone = BannerTone.Alarm,
                    icon = Icons.Outlined.Key,
                    title = stringResource(R.string.home_keywords_insufficient_title),
                    body = stringResource(R.string.home_keywords_insufficient_body, keywordsCount, threshold),
                    actionLabel = stringResource(R.string.action_manage_keywords),
                    onAction = onNavigateToKeywords
                )
            }

            StandbyCard(
                enabled = masterSwitch,
                ready = permissionStatus.requiredGranted && !keywordsInsufficient,
                onToggle = { viewModel.toggleMasterSwitch() }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(
                    label = stringResource(R.string.stat_active_keywords),
                    value = keywordsCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = stringResource(R.string.stat_threshold),
                    value = "≥ $threshold",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = stringResource(R.string.stat_last_alarm),
                    value = lastAlarm?.let {
                        DateUtils.getRelativeTimeSpanString(
                            it.timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE
                        ).toString()
                    } ?: "—",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { viewModel.startTestAlarm() },
                    enabled = !isAlarmActive,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Outlined.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_test_alarm))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    OutlinedButton(
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
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Outlined.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_add_tile))
                    }
                }
            }

            SectionHeader(stringResource(R.string.home_section_configure))
            GroupCard {
                NavRow(
                    icon = Icons.Outlined.Key,
                    title = stringResource(R.string.keywords_title),
                    subtitle = pluralStringResource(R.plurals.home_keywords_subtitle, keywordsCount, keywordsCount, threshold),
                    onClick = onNavigateToKeywords
                )
                GroupDivider()
                NavRow(
                    icon = Icons.Outlined.Tune,
                    title = stringResource(R.string.settings_title),
                    subtitle = ringtoneTitle ?: stringResource(R.string.ringtone_default),
                    onClick = onNavigateToSettings
                )
                GroupDivider()
                NavRow(
                    icon = Icons.Outlined.Shield,
                    title = stringResource(R.string.permissions_title),
                    subtitle = stringResource(
                        R.string.home_permissions_subtitle,
                        permissionStatus.grantedCount,
                        permissionStatus.totalCount
                    ),
                    onClick = onNavigateToPermissions,
                    trailing = {
                        if (!permissionStatus.requiredGranted) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.statusColors.warning)
                            )
                        }
                    }
                )
            }

            Row(
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.privacy_short),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FooterTagline()
        }
    }
}

@Composable
private fun ActiveAlarmCard(onStop: () -> Unit) {
    val status = MaterialTheme.statusColors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = status.alarmContainer)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = status.alarm)
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.home_alarm_ringing_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = status.alarm
                )
            }
            Text(
                stringResource(R.string.home_alarm_ringing_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Button(
                onClick = onStop,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = status.alarm,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(stringResource(R.string.action_stop_alarm), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * The Master Switch, presented as one large toggle. The whole card is the touch target.
 */
@Composable
private fun StandbyCard(enabled: Boolean, ready: Boolean, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (enabled) scheme.primaryContainer else scheme.surfaceContainerHigh,
        animationSpec = tween(300),
        label = "standby_container"
    )
    val content = if (enabled) scheme.onPrimaryContainer else scheme.onSurface
    val stateText = stringResource(if (enabled) R.string.standby_state_on else R.string.standby_state_off)
    val cardDescription = stringResource(R.string.standby_title)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = { onToggle() })
            .semantics(mergeDescendants = true) {
                contentDescription = cardDescription
                stateDescription = stateText
            },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (enabled) scheme.primary else scheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (enabled) Icons.Filled.Alarm else Icons.Filled.AlarmOff,
                        contentDescription = null,
                        tint = if (enabled) scheme.onPrimary else scheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.standby_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = content.copy(alpha = 0.8f)
                    )
                    Text(stateText, style = MaterialTheme.typography.headlineMedium, color = content)
                }
                Switch(checked = enabled, onCheckedChange = null)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(
                    when {
                        enabled && ready -> R.string.standby_desc_on
                        enabled -> R.string.standby_desc_on_not_ready
                        else -> R.string.standby_desc_off
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = content.copy(alpha = 0.85f)
            )
        }
    }
}
