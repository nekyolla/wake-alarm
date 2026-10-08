package com.kindness.wakealarm.ui.components

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kindness.wakealarm.R
import com.kindness.wakealarm.receiver.PersistentToggleReceiver
import com.kindness.wakealarm.ui.theme.Gold
import com.kindness.wakealarm.ui.theme.GoldContainer
import com.kindness.wakealarm.ui.theme.Ivory
import com.kindness.wakealarm.ui.theme.LineStrong
import com.kindness.wakealarm.ui.theme.PanelLow
import com.kindness.wakealarm.ui.theme.Spacing
import com.kindness.wakealarm.ui.theme.TouchTarget
import com.kindness.wakealarm.util.AppLocale

/**
 * Segmented choice between following the system language, Indonesian and English.
 */
@Composable
fun LanguageSelector(
    current: AppLocale.Option,
    onSelect: (AppLocale.Option) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHaptics()
    Row(
        modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        AppLocale.Option.entries.forEach { option ->
            val selected = option == current
            val label = stringResource(
                when (option) {
                    AppLocale.Option.SYSTEM -> R.string.language_system
                    AppLocale.Option.INDONESIAN -> R.string.language_indonesian
                    AppLocale.Option.ENGLISH -> R.string.language_english
                }
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) Gold else Ivory,
                textAlign = TextAlign.Center,
                // One line each, so the three segments always have the same height
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = TouchTarget.min)
                    .clip(MaterialTheme.shapes.small)
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = {
                            if (!selected) {
                                haptics.tap()
                                onSelect(option)
                            }
                        }
                    )
                    .hsrPanel(
                        MaterialTheme.shapes.small,
                        container = if (selected) GoldContainer.copy(alpha = 0.8f) else PanelLow.copy(alpha = 0.8f),
                        accent = if (selected) Gold else LineStrong,
                        ornament = false
                    )
                    .padding(horizontal = Spacing.sm, vertical = 14.dp)
            )
        }
    }
}

/**
 * Applies a language choice from anywhere in the UI. Android 13+ recreates the screen itself;
 * older versions need an explicit recreate so the wrapped context picks up the new locale.
 */
fun applyLanguage(context: Context, option: AppLocale.Option) {
    AppLocale.set(context, option)
    PersistentToggleReceiver.notifyMasterSwitchChanged(context.applicationContext)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) (context as? Activity)?.recreate()
}
