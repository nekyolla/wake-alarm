package com.kindness.wakealarm.ui.keywords

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kindness.wakealarm.R
import com.kindness.wakealarm.data.PresetKeywords
import com.kindness.wakealarm.ui.components.Banner
import com.kindness.wakealarm.ui.components.BannerTone
import com.kindness.wakealarm.ui.components.GroupCard
import com.kindness.wakealarm.ui.components.GroupDivider
import com.kindness.wakealarm.ui.components.KeywordPill
import com.kindness.wakealarm.ui.components.SectionHeader
import com.kindness.wakealarm.ui.components.WakeTopBar
import com.kindness.wakealarm.ui.theme.statusColors
import com.kindness.wakealarm.util.KeywordMatcher
import com.kindness.wakealarm.util.KeywordValidator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KeywordListScreen(
    onBack: () -> Unit,
    viewModel: KeywordViewModel = viewModel()
) {
    val activePresets by viewModel.activePresets.collectAsStateWithLifecycle()
    val customKeywords by viewModel.customKeywords.collectAsStateWithLifecycle()
    val activeKeywords by viewModel.activeKeywords.collectAsStateWithLifecycle()
    val threshold by viewModel.threshold.collectAsStateWithLifecycle()

    var newKeywordText by rememberSaveable { mutableStateOf("") }
    var inputError by remember { mutableStateOf<KeywordValidator.Result?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val undoLabel = stringResource(R.string.action_undo)
    val removedTemplate = stringResource(R.string.keyword_removed)

    fun submit() {
        val result = viewModel.addCustomKeyword(newKeywordText)
        if (result is KeywordValidator.Result.Valid) {
            newKeywordText = ""
            inputError = null
        } else {
            inputError = result
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { WakeTopBar(stringResource(R.string.keywords_title), onBack = onBack, scrollBehavior = scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val totalActive = activeKeywords.size
            if (totalActive < threshold) {
                Banner(
                    tone = BannerTone.Alarm,
                    icon = Icons.Outlined.NotificationsOff,
                    title = stringResource(R.string.home_keywords_insufficient_title),
                    body = stringResource(R.string.home_keywords_insufficient_body, totalActive, threshold)
                )
            } else {
                Banner(
                    tone = BannerTone.Info,
                    icon = Icons.Outlined.Info,
                    title = pluralStringResource(R.plurals.keywords_summary_title, totalActive, totalActive),
                    body = stringResource(R.string.keywords_summary_body, threshold)
                )
            }

            MessageTester(keywords = activeKeywords, threshold = threshold)

            SectionHeader(
                stringResource(R.string.keywords_custom_section),
                supporting = stringResource(R.string.keywords_custom_supporting)
            )
            Row(verticalAlignment = Alignment.Top) {
                OutlinedTextField(
                    value = newKeywordText,
                    onValueChange = {
                        newKeywordText = it
                        inputError = null
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.keywords_input_label)) },
                    placeholder = { Text(stringResource(R.string.keywords_input_placeholder)) },
                    singleLine = true,
                    isError = inputError != null,
                    supportingText = {
                        val error = inputError
                        Text(
                            when (error) {
                                KeywordValidator.Result.Empty -> stringResource(R.string.keyword_error_empty)
                                KeywordValidator.Result.TooShort ->
                                    stringResource(R.string.keyword_error_short, KeywordValidator.MIN_LENGTH)
                                KeywordValidator.Result.TooLong ->
                                    stringResource(R.string.keyword_error_long, KeywordValidator.MAX_LENGTH)
                                KeywordValidator.Result.Duplicate -> stringResource(R.string.keyword_error_duplicate)
                                else -> stringResource(R.string.keywords_input_hint)
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { submit() },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(56.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_keyword))
                }
            }

            if (customKeywords.isEmpty()) {
                Text(
                    stringResource(R.string.keywords_custom_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    customKeywords.sorted().forEach { keyword ->
                        val removeDescription = stringResource(R.string.cd_remove_keyword, keyword)
                        InputChip(
                            selected = true,
                            onClick = {
                                viewModel.removeCustomKeyword(keyword)
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = snackbarHostState.showSnackbar(
                                        message = removedTemplate.format(keyword),
                                        actionLabel = undoLabel,
                                        withDismissAction = true
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreCustomKeyword(keyword)
                                    }
                                }
                            },
                            label = { Text(keyword) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = removeDescription,
                                    modifier = Modifier.size(InputChipDefaults.AvatarSize)
                                )
                            },
                            modifier = Modifier.heightIn(min = 40.dp)
                        )
                    }
                }
            }

            SectionHeader(
                stringResource(R.string.keywords_preset_section),
                supporting = stringResource(R.string.keywords_preset_supporting)
            )
            GroupCard {
                PresetKeywords.all.forEachIndexed { index, preset ->
                    val checked = preset.keyword in activePresets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = { viewModel.togglePreset(preset.keyword) }
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(preset.keyword, style = MaterialTheme.typography.titleMedium)
                            Text(
                                preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (index != PresetKeywords.all.lastIndex) GroupDivider()
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Lets the user type a sample message and see immediately whether it would ring the alarm.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageTester(keywords: List<String>, threshold: Int) {
    var sample by rememberSaveable { mutableStateOf("") }
    val result = remember(sample, keywords, threshold) { KeywordMatcher.match(sample, keywords, threshold) }
    val status = MaterialTheme.statusColors

    GroupCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.tester_title), style = MaterialTheme.typography.titleMedium)
            }
            Text(
                stringResource(R.string.tester_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = sample,
                onValueChange = { sample = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.tester_placeholder)) },
                minLines = 2
            )
            if (sample.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                ) {
                    Icon(
                        if (result.isTriggered) Icons.Outlined.CheckCircle else Icons.Outlined.NotificationsOff,
                        contentDescription = null,
                        tint = if (result.isTriggered) status.alarm else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (result.isTriggered) {
                            stringResource(R.string.tester_would_ring, result.matchCount, threshold)
                        } else {
                            stringResource(R.string.tester_would_not_ring, result.matchCount, threshold)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = if (result.isTriggered) status.alarm else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (result.matchedKeywords.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        result.matchedKeywords.forEach { kw ->
                            KeywordPill(
                                text = kw,
                                container = MaterialTheme.colorScheme.primaryContainer,
                                content = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
