package com.kindness.wakealarm.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kindness.wakealarm.R
import com.kindness.wakealarm.ui.components.Companion3D
import com.kindness.wakealarm.ui.components.HsrButton
import com.kindness.wakealarm.ui.components.IconBadge
import com.kindness.wakealarm.ui.components.LanguageSelector
import com.kindness.wakealarm.ui.components.applyLanguage
import com.kindness.wakealarm.ui.theme.Gold
import com.kindness.wakealarm.ui.theme.GoldSoft
import com.kindness.wakealarm.ui.theme.LineStrong
import com.kindness.wakealarm.ui.theme.Mist
import com.kindness.wakealarm.ui.theme.PanelLow
import com.kindness.wakealarm.ui.theme.Spacing
import com.kindness.wakealarm.ui.theme.TouchTarget
import com.kindness.wakealarm.ui.theme.WakeType
import com.kindness.wakealarm.util.AppLocale
import com.kindness.wakealarm.util.PermissionHelper

private enum class Step { Welcome, Privacy, Permissions, Done }

/**
 * First-run flow. The privacy step is the prominent disclosure for notification access:
 * the user must consent before being sent to grant it.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = Step.entries[stepIndex]
    var requiredGranted by remember { mutableStateOf(PermissionHelper.checkAll(context).requiredGranted) }

    BackHandler(enabled = stepIndex > 0) { stepIndex-- }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(PanelLow.copy(alpha = 0.9f))
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.gutter, vertical = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                StepDots(current = stepIndex, total = Step.entries.size)
                when (step) {
                    Step.Welcome -> PrimaryButton(stringResource(R.string.action_get_started)) { stepIndex++ }
                    Step.Privacy -> {
                        PrimaryButton(stringResource(R.string.action_agree_continue)) { stepIndex++ }
                        // Declining skips the permission step entirely; the app stays usable for setup
                        TextButton(onClick = { stepIndex = Step.Done.ordinal }, modifier = Modifier.heightIn(min = TouchTarget.min)) {
                            Text(stringResource(R.string.action_decline), color = Mist)
                        }
                    }
                    Step.Permissions -> {
                        PrimaryButton(
                            stringResource(if (requiredGranted) R.string.action_continue else R.string.action_skip_for_now)
                        ) { stepIndex++ }
                    }
                    Step.Done -> PrimaryButton(stringResource(R.string.action_start_using), onFinish)
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally { if (forward) it / 4 else -it / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut())
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            label = "onboarding_step"
        ) { current ->
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.gutter, vertical = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                when (current) {
                    Step.Welcome -> WelcomeStep()
                    Step.Privacy -> PrivacyStep()
                    Step.Permissions -> {
                        StepHeader(
                            icon = Icons.Outlined.Shield,
                            eyebrow = stringResource(R.string.onboarding_step_of, 3, Step.entries.size),
                            title = stringResource(R.string.onboarding_permissions_title),
                            body = stringResource(R.string.onboarding_permissions_body)
                        )
                        PermissionChecklist(onStatusChange = { requiredGranted = it.requiredGranted })
                    }
                    Step.Done -> DoneStep(requiredGranted)
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    val context = LocalContext.current
    var language by remember { mutableStateOf(AppLocale.current(context)) }

    LanguageSelector(
        current = language,
        onSelect = { option ->
            language = option
            applyLanguage(context, option)
        }
    )
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Companion3D(size = 180.dp)
    }
    Text(stringResource(R.string.onboarding_welcome_eyebrow).uppercase(), style = WakeType.eyebrow, color = GoldSoft)
    Text(
        stringResource(R.string.onboarding_welcome_title),
        style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier.semantics { heading() }
    )
    Text(
        stringResource(R.string.onboarding_welcome_body),
        style = MaterialTheme.typography.bodyLarge,
        color = Mist
    )
    FeatureRow(Icons.Outlined.Key, R.string.onboarding_feature_keywords_title, R.string.onboarding_feature_keywords_body)
    FeatureRow(Icons.AutoMirrored.Outlined.VolumeUp, R.string.onboarding_feature_loud_title, R.string.onboarding_feature_loud_body)
    FeatureRow(Icons.Outlined.Dashboard, R.string.onboarding_feature_toggle_title, R.string.onboarding_feature_toggle_body)
}

@Composable
private fun PrivacyStep() {
    StepHeader(
        icon = Icons.Outlined.Lock,
        eyebrow = stringResource(R.string.onboarding_step_of, 2, Step.entries.size),
        title = stringResource(R.string.onboarding_privacy_title),
        body = stringResource(R.string.onboarding_privacy_body)
    )
    FeatureRow(Icons.Outlined.Visibility, R.string.onboarding_privacy_read_title, R.string.onboarding_privacy_read_body)
    FeatureRow(Icons.Outlined.PhoneAndroid, R.string.onboarding_privacy_device_title, R.string.onboarding_privacy_device_body)
    FeatureRow(Icons.Outlined.Storage, R.string.onboarding_privacy_store_title, R.string.onboarding_privacy_store_body)
    FeatureRow(Icons.Outlined.Block, R.string.onboarding_privacy_share_title, R.string.onboarding_privacy_share_body)
}

@Composable
private fun DoneStep(requiredGranted: Boolean) {
    StepHeader(
        icon = Icons.Filled.TaskAlt,
        eyebrow = stringResource(R.string.onboarding_step_of, 4, Step.entries.size),
        title = stringResource(R.string.onboarding_done_title),
        body = stringResource(if (requiredGranted) R.string.onboarding_done_body else R.string.onboarding_done_body_incomplete)
    )
    FeatureRow(Icons.Outlined.NotificationsActive, R.string.onboarding_tip_standby_title, R.string.onboarding_tip_standby_body)
    FeatureRow(Icons.Outlined.BugReport, R.string.onboarding_tip_test_title, R.string.onboarding_tip_test_body)
    FeatureRow(Icons.Outlined.Dashboard, R.string.onboarding_tip_tile_title, R.string.onboarding_tip_tile_body)
}

@Composable
private fun StepHeader(icon: ImageVector, eyebrow: String, title: String, body: String) {
    IconBadge(icon, Gold, Gold.copy(alpha = 0.14f), size = 56)
    Text(eyebrow.uppercase(), style = WakeType.eyebrow, color = GoldSoft)
    Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
    Text(body, style = MaterialTheme.typography.bodyLarge, color = Mist)
}

@Composable
private fun FeatureRow(icon: ImageVector, title: Int, body: Int) {
    Row(verticalAlignment = Alignment.Top) {
        IconBadge(icon, Gold, Gold.copy(alpha = 0.14f))
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = Mist)
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    HsrButton(
        text = text,
        onClick = onClick,
        minHeight = TouchTarget.hero,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun StepDots(current: Int, total: Int) {
    val description = stringResource(R.string.onboarding_step_of, current + 1, total)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { contentDescription = description }
    ) {
        repeat(total) { index ->
            val active = index == current
            val width by animateDpAsState(if (active) 28.dp else 8.dp, label = "dot_width")
            val color by animateColorAsState(
                when {
                    active -> Gold
                    index < current -> GoldSoft.copy(alpha = 0.5f)
                    else -> LineStrong
                },
                label = "dot_color"
            )
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .background(color, CutCornerShape(topEnd = 4.dp, bottomStart = 4.dp))
            )
        }
    }
}
