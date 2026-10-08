package com.kindness.wakealarm.ui.components

import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kindness.wakealarm.R
import com.kindness.wakealarm.ui.theme.Crimson
import com.kindness.wakealarm.ui.theme.CrimsonDeep
import com.kindness.wakealarm.ui.theme.CrimsonLight
import com.kindness.wakealarm.ui.theme.Gold
import com.kindness.wakealarm.ui.theme.GoldSoft
import com.kindness.wakealarm.ui.theme.Ivory
import com.kindness.wakealarm.ui.theme.LineSubtle
import com.kindness.wakealarm.ui.theme.Mist
import com.kindness.wakealarm.ui.theme.MistDim
import com.kindness.wakealarm.ui.theme.OnGold
import com.kindness.wakealarm.ui.theme.PanelHigh
import com.kindness.wakealarm.ui.theme.PanelHighest
import com.kindness.wakealarm.ui.theme.PanelLow
import com.kindness.wakealarm.ui.theme.SpaceDeep
import com.kindness.wakealarm.ui.theme.Spacing
import com.kindness.wakealarm.ui.theme.Stroke
import com.kindness.wakealarm.ui.theme.TouchTarget
import com.kindness.wakealarm.ui.theme.WakeType
import com.kindness.wakealarm.ui.theme.statusColors

// ---------------------------------------------------------------------------------------------
// Lifecycle, motion and haptics helpers
// ---------------------------------------------------------------------------------------------

/** Runs [onResume] every time the hosting screen comes back to the foreground. */
@Composable
fun OnResume(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(onResume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) latest()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/** True when the user turned animations off in the system settings; decorative motion then stops. */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Haptic feedback with one vocabulary across the app. */
class Haptics(private val view: View) {
    fun tap() {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun confirm() {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
        )
    }

    fun reject() {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
        )
    }

    fun tick() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}

// ---------------------------------------------------------------------------------------------
// Panels and ornaments
// ---------------------------------------------------------------------------------------------

/**
 * The HSR "glass panel": translucent fill, a hairline border that glows [accent] at the top-left
 * and fades out, and small corner brackets on the two square corners.
 */
fun Modifier.hsrPanel(
    shape: Shape,
    container: Color = PanelLow.copy(alpha = 0.92f),
    accent: Color = GoldSoft,
    ornament: Boolean = true
): Modifier {
    val bordered = this
        .clip(shape)
        .background(container, shape)
        .border(
            Stroke.hairline,
            Brush.linearGradient(listOf(accent.copy(alpha = 0.55f), LineSubtle.copy(alpha = 0.8f), LineSubtle.copy(alpha = 0.3f))),
            shape
        )
    if (!ornament) return bordered
    return bordered.drawWithContent {
        drawContent()
        val len = 12.dp.toPx()
        val w = Stroke.ornament.toPx()
        // Top-left bracket
        drawLine(accent, Offset(0f, w / 2), Offset(len, w / 2), w)
        drawLine(accent, Offset(w / 2, 0f), Offset(w / 2, len), w)
        // Bottom-right bracket, dimmer
        val dim = accent.copy(alpha = 0.45f)
        drawLine(dim, Offset(size.width - len, size.height - w / 2), Offset(size.width, size.height - w / 2), w)
        drawLine(dim, Offset(size.width - w / 2, size.height - len), Offset(size.width - w / 2, size.height), w)
    }
}

/** A small diamond, the app's recurring ornament (from the card suits on the mascot's crown). */
@Composable
fun DiamondGlyph(color: Color, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .background(color, CutCornerShape(50))
    )
}

/** Rarity-style stars, e.g. the keyword threshold shown as ★★☆☆☆. Decorative: callers describe the value. */
@Composable
fun RarityStars(count: Int, modifier: Modifier = Modifier, max: Int = 5, size: Dp = 16.dp) {
    Row(modifier.clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(max) { index ->
            val filled = index < count
            Icon(
                if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (filled) Gold else MistDim,
                modifier = Modifier.size(size)
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Bars
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakeTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    eyebrow: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Column {
                if (eyebrow != null) {
                    Text(eyebrow.uppercase(), style = WakeType.eyebrow, color = GoldSoft, maxLines = 1)
                }
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = SpaceDeep.copy(alpha = 0.94f),
            titleContentColor = Ivory,
            navigationIconContentColor = Ivory,
            actionIconContentColor = Ivory
        )
    )
}

/** One destination in [WakeBottomBar]. */
data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

/**
 * Bottom navigation in the thumb zone: glass bar, gold selection with a diamond marker.
 */
@Composable
fun WakeBottomBar(tabs: List<BottomTab>, selectedRoute: String?, onSelect: (String) -> Unit) {
    val haptics = rememberHaptics()
    Column(
        Modifier
            .fillMaxWidth()
            .background(PanelLow.copy(alpha = 0.97f))
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(Stroke.hairline)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, GoldSoft.copy(alpha = 0.6f), Color.Transparent)))
        )
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(TouchTarget.hero)
                .selectableGroup()
        ) {
            tabs.forEach { tab ->
                val selected = tab.route == selectedRoute
                val tint by animateColorAsState(if (selected) Gold else Mist, label = "tab_tint")
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = {
                                if (!selected) haptics.tap()
                                onSelect(tab.route)
                            }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    DiamondGlyph(color = if (selected) Gold else Color.Transparent, size = 6.dp)
                    Spacer(Modifier.height(Spacing.xs))
                    Icon(
                        if (selected) tab.selectedIcon else tab.icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(tab.label, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
                }
            }
        }
    }
}

/** Snackbars styled as small gold-accented panels. */
@Composable
fun WakeSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = MaterialTheme.shapes.small,
            containerColor = PanelHighest,
            contentColor = Ivory,
            actionColor = Gold,
            dismissActionContentColor = Mist
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Headers, groups and rows
// ---------------------------------------------------------------------------------------------

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, supporting: String? = null) {
    Column(modifier.padding(top = Spacing.md, bottom = Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DiamondGlyph(Gold, 8.dp)
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = text.uppercase(),
                style = WakeType.eyebrow,
                color = GoldSoft,
                modifier = Modifier.clearAndSetSemantics {
                    heading()
                    contentDescription = text
                }
            )
            Spacer(Modifier.width(Spacing.md))
            Box(
                Modifier
                    .weight(1f)
                    .height(Stroke.hairline)
                    .background(Brush.horizontalGradient(listOf(GoldSoft.copy(alpha = 0.5f), Color.Transparent)))
            )
        }
        if (supporting != null) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = Mist,
                modifier = Modifier.padding(start = Spacing.lg, top = Spacing.xs)
            )
        }
    }
}

/** A glass panel that groups related rows, separated by [GroupDivider]s. */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .hsrPanel(MaterialTheme.shapes.large),
        content = content
    )
}

@Composable
fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        color = LineSubtle.copy(alpha = 0.8f)
    )
}

/** Icon on a diamond plate, used at the start of rows. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, container: Color, size: Int = 40) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(container, CutCornerShape(50)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.45f).dp))
    }
}

@Composable
fun NavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, Gold, Gold.copy(alpha = 0.14f))
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ivory)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Mist)
        }
        if (trailing != null) {
            trailing()
            Spacer(Modifier.width(Spacing.xs))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Mist)
    }
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = {
                    haptics.tap()
                    onCheckedChange(it)
                }
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            IconBadge(icon, Gold, Gold.copy(alpha = 0.14f))
            Spacer(Modifier.width(Spacing.lg))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Mist)
            }
        }
        Spacer(Modifier.width(Spacing.md))
        // The whole row is the toggle target; the switch is just its visual
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

// ---------------------------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------------------------

enum class HsrButtonStyle { Primary, Secondary, Danger }

/**
 * The app's button: angled corners, uppercase Rajdhani label, haptic tap.
 * Primary = gold (the main action), Secondary = outlined, Danger = crimson (alarm/stop).
 */
@Composable
fun HsrButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: HsrButtonStyle = HsrButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    minHeight: Dp = TouchTarget.comfortable
) {
    val shape = MaterialTheme.shapes.small
    val haptics = rememberHaptics()
    val background: Brush
    val border: Brush
    val content: Color
    when (style) {
        HsrButtonStyle.Primary -> {
            background = Brush.verticalGradient(listOf(Color(0xFFF6DC9C), Gold, Color(0xFFD9B055)))
            border = Brush.verticalGradient(listOf(Color(0xFFFFF1C9), Gold))
            content = OnGold
        }
        HsrButtonStyle.Secondary -> {
            background = Brush.verticalGradient(listOf(PanelHigh.copy(alpha = 0.7f), PanelLow.copy(alpha = 0.7f)))
            border = Brush.linearGradient(listOf(GoldSoft.copy(alpha = 0.8f), GoldSoft.copy(alpha = 0.25f)))
            content = Ivory
        }
        HsrButtonStyle.Danger -> {
            background = Brush.verticalGradient(listOf(Crimson, CrimsonDeep))
            border = Brush.verticalGradient(listOf(CrimsonLight, CrimsonDeep))
            content = Color.White
        }
    }
    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(shape)
            .background(background, shape)
            .border(Stroke.hairline, border, shape)
            .clickable(enabled = enabled, role = Role.Button) {
                haptics.tap()
                onClick()
            }
            .padding(horizontal = Spacing.gutter, vertical = Spacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(
                text.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = content,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Status
// ---------------------------------------------------------------------------------------------

enum class BannerTone { Alarm, Warning, Info, Success }

@Composable
fun Banner(
    tone: BannerTone,
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val status = MaterialTheme.statusColors
    val (accent, container) = when (tone) {
        BannerTone.Alarm -> status.alarm to status.alarmContainer
        BannerTone.Warning -> status.warning to status.warningContainer
        BannerTone.Success -> status.success to status.successContainer
        BannerTone.Info -> Gold to PanelHigh
    }
    Row(
        modifier
            .fillMaxWidth()
            .hsrPanel(MaterialTheme.shapes.medium, container.copy(alpha = 0.9f), accent, ornament = false)
            .drawBehind { drawRect(accent, size = Size(3.dp.toPx(), size.height)) }
            .padding(start = Spacing.lg + 3.dp, end = Spacing.lg, top = Spacing.md, bottom = Spacing.md),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = accent)
            if (body != null) {
                Text(body, style = MaterialTheme.typography.bodySmall, color = Ivory.copy(alpha = 0.88f))
            }
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = Spacing.xs)
                ) {
                    Text(actionLabel.uppercase(), style = MaterialTheme.typography.labelLarge, color = accent)
                    Spacer(Modifier.width(Spacing.xs))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Ivory,
    footer: @Composable (() -> Unit)? = null
) {
    Column(
        modifier
            .hsrPanel(MaterialTheme.shapes.medium, ornament = false)
            .padding(horizontal = 14.dp, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, style = WakeType.numeral, color = valueColor, maxLines = 1)
        if (footer != null) footer()
        Text(label, style = MaterialTheme.typography.labelMedium, color = Mist, maxLines = 2)
    }
}

@Composable
fun FooterTagline(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xl),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DiamondGlyph(GoldSoft.copy(alpha = 0.5f), 6.dp)
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.labelLarge,
            color = Mist.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(Spacing.sm))
        DiamondGlyph(GoldSoft.copy(alpha = 0.5f), 6.dp)
    }
}

/** Non-interactive label for a matched keyword. */
@Composable
fun KeywordPill(text: String, container: Color, content: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = content,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(container)
            .padding(horizontal = Spacing.md, vertical = 6.dp)
    )
}
