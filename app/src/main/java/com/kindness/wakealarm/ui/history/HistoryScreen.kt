package com.kindness.wakealarm.ui.history

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kindness.wakealarm.R
import com.kindness.wakealarm.data.AlarmEvent
import com.kindness.wakealarm.data.AlarmHistoryRepository
import com.kindness.wakealarm.ui.components.DiamondGlyph
import com.kindness.wakealarm.ui.components.KeywordPill
import com.kindness.wakealarm.ui.components.WakeTopBar
import com.kindness.wakealarm.ui.components.hsrPanel
import com.kindness.wakealarm.ui.theme.Gold
import com.kindness.wakealarm.ui.theme.GoldSoft
import com.kindness.wakealarm.ui.theme.Mist
import com.kindness.wakealarm.ui.theme.OnVioletContainer
import com.kindness.wakealarm.ui.theme.Spacing
import com.kindness.wakealarm.ui.theme.Violet
import com.kindness.wakealarm.ui.theme.VioletContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AlarmHistoryRepository(application)

    val events: StateFlow<List<AlarmEvent>?> = repository.eventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun clear() {
        viewModelScope.launch { repository.clear() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            WakeTopBar(stringResource(R.string.history_title), scrollBehavior = scrollBehavior) {
                if (!events.isNullOrEmpty()) {
                    IconButton(onClick = { confirmClear = true }) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = stringResource(R.string.history_clear))
                    }
                }
            }
        }
    ) { padding ->
        val list = events
        when {
            list == null -> Unit // still loading — avoid flashing the empty state
            list.isEmpty() -> EmptyHistory(Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.xs, bottom = Spacing.xxl)
            ) {
                item {
                    Text(
                        stringResource(R.string.history_caption, AlarmHistoryRepository.MAX_EVENTS),
                        style = MaterialTheme.typography.bodySmall,
                        color = Mist,
                        modifier = Modifier.padding(bottom = Spacing.md)
                    )
                }
                itemsIndexed(list, key = { index, event -> "$index-${event.timestamp}" }) { index, event ->
                    HistoryItem(event, isLast = index == list.lastIndex)
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
            title = { Text(stringResource(R.string.history_clear_confirm_title)) },
            text = { Text(stringResource(R.string.history_clear_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clear()
                    confirmClear = false
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

/** One entry on the timeline: a diamond node and line on the left, the message panel on the right. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryItem(event: AlarmEvent, isLast: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    val whenText = remember(event.timestamp, locale) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(event.timestamp))
    }
    Row(
        Modifier.drawBehind {
            // Timeline line from below this node down to the next one
            if (!isLast) {
                val x = 10.dp.toPx()
                drawLine(
                    Brush.verticalGradient(listOf(GoldSoft.copy(alpha = 0.5f), GoldSoft.copy(alpha = 0.1f))),
                    start = Offset(x, (Spacing.lg + 14.dp).toPx()),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    ) {
        Column(Modifier.width(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(Spacing.lg))
            DiamondGlyph(if (event.whileRinging) Violet else Gold, 12.dp)
        }
        Spacer(Modifier.width(Spacing.md))
        Column(
            Modifier
                .weight(1f)
                .padding(bottom = Spacing.md)
                .hsrPanel(MaterialTheme.shapes.large, ornament = false)
                .semantics(mergeDescendants = true) {}
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    whenText,
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldSoft,
                    modifier = Modifier.weight(1f)
                )
                if (event.whileRinging) {
                    KeywordPill(
                        text = stringResource(R.string.history_while_ringing),
                        container = VioletContainer,
                        content = OnVioletContainer
                    )
                }
            }
            if (event.sender.isNotBlank()) {
                Text(event.sender, style = MaterialTheme.typography.titleMedium)
            }
            Text(event.message, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
            if (event.keywords.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = Spacing.xs)
                ) {
                    event.keywords.forEach {
                        KeywordPill(
                            text = it,
                            container = MaterialTheme.colorScheme.secondaryContainer,
                            content = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.History,
            contentDescription = null,
            tint = Mist,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(stringResource(R.string.history_empty_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.history_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = Mist,
            textAlign = TextAlign.Center
        )
    }
}
