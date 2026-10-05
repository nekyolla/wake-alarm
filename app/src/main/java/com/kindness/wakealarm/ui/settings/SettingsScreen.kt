package com.kindness.wakealarm.ui.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kindness.wakealarm.R
import com.kindness.wakealarm.data.SettingsRepository
import com.kindness.wakealarm.ui.components.FooterTagline
import com.kindness.wakealarm.ui.components.GroupCard
import com.kindness.wakealarm.ui.components.GroupDivider
import com.kindness.wakealarm.ui.components.IconBadge
import com.kindness.wakealarm.ui.components.NavRow
import com.kindness.wakealarm.ui.components.SectionHeader
import com.kindness.wakealarm.ui.components.SwitchRow
import com.kindness.wakealarm.ui.components.WakeTopBar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onReplayOnboarding: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val threshold by viewModel.threshold.collectAsStateWithLifecycle()
    val volumeRampUp by viewModel.volumeRampUp.collectAsStateWithLifecycle()
    val vibration by viewModel.vibration.collectAsStateWithLifecycle()
    val customRingtoneUri by viewModel.customRingtoneUri.collectAsStateWithLifecycle()
    val customRingtoneTitle by viewModel.customRingtoneTitle.collectAsStateWithLifecycle()
    val isPlayingPreview by viewModel.isPlayingPreview.collectAsStateWithLifecycle()
    val isAlarmActive by viewModel.isAlarmActive.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val fallbackAudioTitle = stringResource(R.string.ringtone_custom_fallback)
    val fallbackSystemTitle = stringResource(R.string.ringtone_system_fallback)

    // Audio/MP3 file picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            // Keep read access across reboots so the alarm can still play this file later
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
        }
        var fileName: String? = null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) fileName = cursor.getString(0)
        }
        viewModel.setCustomRingtone(uri.toString(), fileName ?: fallbackAudioTitle)
    }

    // System ringtone picker
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.let {
            IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } ?: return@rememberLauncherForActivityResult
        val title = RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: fallbackSystemTitle
        viewModel.setCustomRingtone(uri.toString(), title)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopPreview() }
    }

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) {
            null
        } ?: "—"
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { WakeTopBar(stringResource(R.string.settings_title), onBack = onBack, scrollBehavior = scrollBehavior) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(stringResource(R.string.settings_section_sensitivity))
            ThresholdCard(threshold = threshold, onThresholdChange = viewModel::setThreshold)

            SectionHeader(stringResource(R.string.settings_section_sound))
            GroupCard {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(
                        Icons.Outlined.MusicNote,
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.settings_ringtone_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            customRingtoneTitle ?: stringResource(R.string.ringtone_default),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Outlined.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_pick_file))
                    }
                    FilledTonalButton(
                        onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                customRingtoneUri?.let {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                                }
                            }
                            ringtonePickerLauncher.launch(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Outlined.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_pick_system))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.togglePreview() },
                        enabled = !isAlarmActive,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            if (isPlayingPreview) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(if (isPlayingPreview) R.string.action_stop_preview else R.string.action_preview)
                        )
                    }
                    if (customRingtoneTitle != null) {
                        TextButton(onClick = { viewModel.resetToDefaultRingtone() }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_reset_default))
                        }
                    }
                }
                Text(
                    stringResource(R.string.settings_preview_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }

            GroupCard {
                SwitchRow(
                    title = stringResource(R.string.settings_ramp_title),
                    subtitle = stringResource(R.string.settings_ramp_desc),
                    checked = volumeRampUp,
                    onCheckedChange = viewModel::setVolumeRampUp,
                    icon = Icons.AutoMirrored.Outlined.TrendingUp
                )
                GroupDivider()
                SwitchRow(
                    title = stringResource(R.string.settings_vibrate_title),
                    subtitle = stringResource(R.string.settings_vibrate_desc),
                    checked = vibration,
                    onCheckedChange = viewModel::setVibration,
                    icon = Icons.Outlined.Vibration
                )
            }

            SectionHeader(stringResource(R.string.settings_section_about))
            GroupCard {
                NavRow(
                    icon = Icons.Outlined.School,
                    title = stringResource(R.string.settings_replay_onboarding),
                    subtitle = stringResource(R.string.settings_replay_onboarding_desc),
                    onClick = onReplayOnboarding
                )
                GroupDivider()
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    IconBadge(
                        Icons.Outlined.Lock,
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.privacy_full),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                GroupDivider()
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        Icons.Outlined.Info,
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(stringResource(R.string.settings_version), style = MaterialTheme.typography.titleMedium)
                        Text(
                            versionName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            FooterTagline()
        }
    }
}

@Composable
private fun ThresholdCard(threshold: Int, onThresholdChange: (Int) -> Unit) {
    // Local slider state so dragging is smooth; persisted when the drag ends
    var sliderValue by remember { mutableFloatStateOf(threshold.toFloat()) }
    LaunchedEffect(threshold) { sliderValue = threshold.toFloat() }
    val current = sliderValue.roundToInt()
    val stateText = stringResource(R.string.settings_threshold_value, current)

    GroupCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.settings_threshold_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(stateText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onThresholdChange(sliderValue.roundToInt()) },
                valueRange = SettingsRepository.MIN_THRESHOLD.toFloat()..SettingsRepository.MAX_THRESHOLD.toFloat(),
                steps = SettingsRepository.MAX_THRESHOLD - SettingsRepository.MIN_THRESHOLD - 1,
                modifier = Modifier.semantics { stateDescription = stateText }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.settings_threshold_sensitive),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.settings_threshold_strict),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(
                    when (current) {
                        1 -> R.string.settings_threshold_hint_1
                        2 -> R.string.settings_threshold_hint_2
                        else -> R.string.settings_threshold_hint_many
                    },
                    current
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
