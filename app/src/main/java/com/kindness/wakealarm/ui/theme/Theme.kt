package com.kindness.wakealarm.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Status colors that Material 3 has no role for. Read via [MaterialTheme.statusColors].
 */
@Immutable
data class StatusColors(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val alarm: Color,
    val alarmContainer: Color
)

private val WakeStatusColors = StatusColors(
    success = Success,
    successContainer = SuccessContainer,
    warning = Warning,
    warningContainer = WarningContainer,
    alarm = Alarm,
    alarmContainer = AlarmContainer
)

private val LocalStatusColors = staticCompositionLocalOf { WakeStatusColors }

val MaterialTheme.statusColors: StatusColors
    @Composable
    @ReadOnlyComposable
    get() = LocalStatusColors.current

/**
 * One dark scheme only: the app is used at night on call, and the HSR look is built on it.
 * Gold is the primary action color (dark text on gold), crimson the brand/alarm accent.
 */
private val WakeColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = OnGold,
    primaryContainer = GoldContainer,
    onPrimaryContainer = OnGoldContainer,
    inversePrimary = Color(0xFF7A5A10),
    secondary = CrimsonLight,
    onSecondary = CrimsonContainer,
    secondaryContainer = CrimsonContainer,
    onSecondaryContainer = OnCrimsonContainer,
    tertiary = Violet,
    onTertiary = Color(0xFF1E0F45),
    tertiaryContainer = VioletContainer,
    onTertiaryContainer = OnVioletContainer,
    background = SpaceBlack,
    onBackground = Ivory,
    surface = SpaceDeep,
    onSurface = Ivory,
    surfaceVariant = PanelHighest,
    onSurfaceVariant = Mist,
    surfaceTint = Color.Transparent,
    inverseSurface = Ivory,
    inverseOnSurface = Color(0xFF1A1B26),
    error = Alarm,
    onError = Color(0xFF3A0010),
    errorContainer = AlarmContainer,
    onErrorContainer = OnCrimsonContainer,
    outline = LineStrong,
    outlineVariant = LineSubtle,
    scrim = Color.Black,
    surfaceBright = PanelHighest,
    surfaceDim = SpaceBlack,
    surfaceContainerLowest = PanelLowest,
    surfaceContainerLow = PanelLow,
    surfaceContainer = Panel,
    surfaceContainerHigh = PanelHigh,
    surfaceContainerHighest = PanelHighest
)

/** Angled corners, like the panels in HSR's menus: top-end and bottom-start are cut. */
private val WakeAlarmShapes = Shapes(
    extraSmall = CutCornerShape(topEnd = 6.dp, bottomStart = 6.dp),
    small = CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp),
    medium = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp),
    large = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp),
    extraLarge = CutCornerShape(topEnd = 24.dp, bottomStart = 24.dp)
)

@Composable
fun WakeAlarmTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStatusColors provides WakeStatusColors) {
        MaterialTheme(
            colorScheme = WakeColorScheme,
            typography = WakeAlarmTypography,
            shapes = WakeAlarmShapes,
            content = content
        )
    }
}
