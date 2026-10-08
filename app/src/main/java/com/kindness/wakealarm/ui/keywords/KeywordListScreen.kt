package com.kindness.wakealarm.ui.keywords

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
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
import com.kindness.wakealarm.ui.components.WakeSnackbarHost
import com.kindness.wakealarm.ui.components.WakeTopBar
import com.kindness.wakealarm.ui.components.rememberHaptics
import com.kindness.wakealarm.ui.theme.Gold
import com.kindness.wakealarm.ui.theme.GoldSoft
import com.kindness.wakealarm.ui.theme.Mist
import com.kindness.wakealarm.ui.theme.OnGold
import com.kindness.wakealarm.ui.theme.PanelLow
import com.kindness.wakealarm.ui.theme.Spacing
import com.kindness.wakealarm.ui.theme.Stroke
import com.kindness.wakealarm.ui.theme.TouchTarget
import com.kindness.wakealarm.ui.theme.statusColors
import com.kindness.wakealarm.util.KeywordMatcher
import com.kindness.wakealarm.util.KeywordValidator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KeywordListScreen(viewModel: KeywordViewModel = viewModel()) {
    val activePresets by viewModel.activePresets.collectAsStateWithLifecycle()
    val customKeywords by viewModel.customKeywords.collectAsStateWithLifecycle()
    val activeKeywords by viewModel.activeKeywords.collectAsStateWithLifecycle()
    val threshold by viewModel.threshold.collectAsStateWithLifecycle()

    var newKeywordText by rememberSaveable { mutableStateOf("") }
    var inputError by remember { mutableStateOf<KeywordValidator.Result?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val undoLabel = stringResource(R.string.action_undo)
    val removedTemplate = stringResource(R.string.keyword_removed)
    val addedTemplate = stringResource(R.string.keyword_added)
    val belowThresholdTemplate = stringResource(R.string.snack_below_threshold)

    fun snack(message: String, undo: (() -> Unit)? = null) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = if (undo != null) undoLabel else null,
                withDismissAction = undo != null
            )
            if (result == SnackbarResult.ActionPerformed) undo?.invoke()
        }
    }

    fun submit() {
        val result = viewModel.addCustomKeyword(newKeywordText)
        if (result is KeywordValidator.Result.Valid) {
            newKeywordText = ""
            inputError = null
            haptics.confirm()
            snack(addedTemplate.format(result.normalized))
        } else {
            inputError = result
            haptics.reject()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { WakeTopBar(stringResource(R.string.keywords_title), scrollBehavior = scrollBehavior) },
        snackbarHost = { WakeSnackbarHost(snackbarHostState) },
        bottomBar = {
            AddKeywordBar(
                text = newKeywordText,
                onTextChange = {
                    newKeywordText = it
                    inputError = null
                },
                error = inputError,
                onSubmit = ::submit
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
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
            if (customKeywords.isEmpty()) {
                Text(
                    stringResource(R.string.keywords_custom_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Mist,
                    modifier = Modifier.padding(vertical = Spacing.xs)
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    customKeywords.sorted().forEach { keyword ->
                        val removeDescription = stringResource(R.string.cd_remove_keyword, keyword)
                        InputChip(
                            selected = true,
                            onClick = {
                                val belowThreshold = viewModel.removeCustomKeyword(keyword)
                                haptics.tap()
                                val message = if (belowThreshold) {
                                    belowThresholdTemplate.format(totalActive - 1, threshold)
                                } else {
                                    removedTemplate.format(keyword)
                                }
                                snack(message) { viewModel.restoreCustomKeyword(keyword) }
                            },
                            label = { Text(keyword) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = removeDescription,
                                    modifier = Modifier.size(InputChipDefaults.AvatarSize)
                                )
                            },
                            modifier = Modifier.heightIn(min = TouchTarget.min)
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
                            .heightIn(min = TouchTarget.comfortable)
                            .toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = {
                                    haptics.tap()
                                    if (viewModel.togglePreset(preset.keyword)) {
                                        haptics.reject()
                                        snack(belowThresholdTemplate.format(totalActive - 1, threshold)) {
                                            viewModel.togglePreset(preset.keyword)
                                        }
                                    }
                                }
                            )
                            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Spacer(Modifier.width(Spacing.lg))
                        Column(Modifier.weight(1f)) {
                            Text(preset.keyword, style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(preset.description),
                                style = MaterialTheme.typography.bodySmall,
                                color = Mist
                            )
                        }
                    }
                    if (index != PresetKeywords.all.lastIndex) GroupDivider()
                }
            }

            Spacer(Modifier.height(Spacing.xl))
        }
    }
}

/**
 * Pinned to the bottom (thumb zone) and lifted above the keyboard while typing.
 */
@Composable
private fun AddKeywordBar(
    text: String,
    onTextChange: (String) -> Unit,
    error: KeywordValidator.Result?,
    onSubmit: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(PanelLow.copy(alpha = 0.97f))
            .imePadding()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(Stroke.hairline)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, GoldSoft.copy(alpha = 0.45f), Color.Transparent)))
        )
        Row(
            Modifier.padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.keywords_input_label)) },
                placeholder = { Text(stringResource(R.string.keywords_input_placeholder)) },
                singleLine = true,
                isError = error != null,
                supportingText = {
                    Text(
                        when (error) {
                            KeywordValidator.Result.Empty -> stringResource(R.string.keyword_error_empty)
                            KeywordValidator.Result.TooShort ->
                                stringResource(R.string.keyword_error_short, KeywordValidator.MIN_LENGTH)
                            KeywordValidator.Result.TooLong ->
                                stringResource(R.string.keyword_error_long, KeywordValidator.MAX_LENGTH)
                            KeywordValidator.Result.NoLetterOrDigit -> stringResource(R.string.keyword_error_no_letters)
                            KeywordValidator.Result.Duplicate -> stringResource(R.string.keyword_error_duplicate)
                            is KeywordValidator.Result.Valid, null -> stringResource(R.string.keywords_input_hint)
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() })
            )
            Spacer(Modifier.width(Spacing.sm))
            FilledIconButton(
                onClick = onSubmit,
                shape = MaterialTheme.shapes.small,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Gold, contentColor = OnGold),
                modifier = Modifier
                    .padding(top = Spacing.sm)
                    .size(TouchTarget.comfortable)
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_keyword))
            }
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
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Science, contentDescription = null, tint = Gold)
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(R.string.tester_title), style = MaterialTheme.typography.titleMedium)
            }
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
                        tint = if (result.isTriggered) status.alarm else Mist,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(Spacing.sm))
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
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
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
