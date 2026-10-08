package com.kindness.wakealarm.ui.onboarding

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.kindness.wakealarm.R
import com.kindness.wakealarm.ui.components.Banner
import com.kindness.wakealarm.ui.components.BannerTone
import com.kindness.wakealarm.ui.components.GroupCard
import com.kindness.wakealarm.ui.components.GroupDivider
import com.kindness.wakealarm.ui.components.HsrButton
import com.kindness.wakealarm.ui.components.IconBadge
import com.kindness.wakealarm.ui.components.OnResume
import com.kindness.wakealarm.ui.components.SectionHeader
import com.kindness.wakealarm.ui.components.WakeTopBar
import com.kindness.wakealarm.ui.theme.Gold
import com.kindness.wakealarm.ui.theme.Mist
import com.kindness.wakealarm.ui.theme.PanelHighest
import com.kindness.wakealarm.ui.theme.Spacing
import com.kindness.wakealarm.ui.theme.TouchTarget
import com.kindness.wakealarm.ui.theme.statusColors
import com.kindness.wakealarm.util.PermissionHelper
import com.kindness.wakealarm.util.PermissionHelper.AppPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionOnboardingScreen(
    onBack: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = { WakeTopBar(stringResource(R.string.permissions_title), onBack = onBack, scrollBehavior = scrollBehavior) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter)
        ) {
            PermissionChecklist()
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}

/**
 * Live checklist of every permission, grouped by required/recommended. Re-checks on resume
 * because every grant happens in a system Settings screen outside the app.
 */
@Composable
fun PermissionChecklist(onStatusChange: (PermissionHelper.PermissionStatus) -> Unit = {}) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(PermissionHelper.checkAll(context)) }

    fun refresh() {
        status = PermissionHelper.checkAll(context)
        onStatusChange(status)
    }
    OnResume { refresh() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val activity = context as? Activity
        // Denied without a dialog (or denied twice): the only way left is the Settings screen
        if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
        ) {
            PermissionHelper.openSettings(context, AppPermission.POST_NOTIFICATIONS)
        }
        refresh()
    }

    fun request(permission: AppPermission) {
        if (permission == AppPermission.POST_NOTIFICATIONS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            PermissionHelper.openSettings(context, permission)
        }
    }

    val progress by animateFloatAsState(status.grantedCount.toFloat() / status.totalCount, label = "perm_progress")
    val progressText = stringResource(R.string.permissions_progress, status.grantedCount, status.totalCount)
    val success = MaterialTheme.statusColors.success

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.clearAndSetSemantics { contentDescription = progressText }
        ) {
            Text(
                "${status.grantedCount}/${status.totalCount}",
                style = MaterialTheme.typography.displaySmall,
                color = if (status.allGranted) success else Gold
            )
            Spacer(Modifier.width(Spacing.md))
            Text(
                stringResource(R.string.permissions_progress_label),
                style = MaterialTheme.typography.titleSmall,
                color = Mist,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .semantics { contentDescription = progressText },
            color = if (status.allGranted) success else Gold,
            trackColor = PanelHighest,
            drawStopIndicator = {}
        )

        SectionHeader(
            stringResource(R.string.permissions_required_section),
            supporting = stringResource(R.string.permissions_required_supporting)
        )
        PermissionGroup(AppPermission.entries.filter { it.required }, status, ::request)

        SectionHeader(
            stringResource(R.string.permissions_recommended_section),
            supporting = stringResource(R.string.permissions_recommended_supporting)
        )
        PermissionGroup(AppPermission.entries.filter { !it.required }, status, ::request)

        if (PermissionHelper.isXiaomiFamily) {
            Banner(
                tone = BannerTone.Warning,
                icon = Icons.Outlined.PhoneAndroid,
                title = stringResource(R.string.permissions_xiaomi_title),
                body = stringResource(R.string.permissions_xiaomi_body),
                actionLabel = stringResource(R.string.action_open_autostart),
                onAction = { PermissionHelper.openXiaomiAutostart(context) }
            )
        }
    }
}

@Composable
private fun PermissionGroup(
    permissions: List<AppPermission>,
    status: PermissionHelper.PermissionStatus,
    onRequest: (AppPermission) -> Unit
) {
    GroupCard {
        permissions.forEachIndexed { index, permission ->
            PermissionRow(permission, status.isGranted(permission)) { onRequest(permission) }
            if (index != permissions.lastIndex) GroupDivider()
        }
    }
}

private data class PermissionCopy(val icon: ImageVector, val title: Int, val description: Int)

private fun copyFor(permission: AppPermission): PermissionCopy = when (permission) {
    AppPermission.NOTIFICATION_LISTENER ->
        PermissionCopy(Icons.Outlined.Visibility, R.string.perm_listener_title, R.string.perm_listener_desc)
    AppPermission.POST_NOTIFICATIONS ->
        PermissionCopy(Icons.Outlined.NotificationsActive, R.string.perm_notifications_title, R.string.perm_notifications_desc)
    AppPermission.FULL_SCREEN_INTENT ->
        PermissionCopy(Icons.Outlined.Fullscreen, R.string.perm_fullscreen_title, R.string.perm_fullscreen_desc)
    AppPermission.BATTERY_OPTIMIZATION ->
        PermissionCopy(Icons.Outlined.BatteryChargingFull, R.string.perm_battery_title, R.string.perm_battery_desc)
    AppPermission.OVERLAY ->
        PermissionCopy(Icons.Outlined.Layers, R.string.perm_overlay_title, R.string.perm_overlay_desc)
    AppPermission.DND_ACCESS ->
        PermissionCopy(Icons.Outlined.DoNotDisturbOn, R.string.perm_dnd_title, R.string.perm_dnd_desc)
}

@Composable
private fun PermissionRow(permission: AppPermission, granted: Boolean, onGrant: () -> Unit) {
    val copy = copyFor(permission)
    val status = MaterialTheme.statusColors
    val badgeContainer by animateColorAsState(
        if (granted) status.successContainer else Gold.copy(alpha = 0.14f),
        label = "perm_badge"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(copy.icon, if (granted) status.success else Gold, badgeContainer)
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f)) {
            Text(stringResource(copy.title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(copy.description), style = MaterialTheme.typography.bodySmall, color = Mist)
        }
        Spacer(Modifier.width(Spacing.md))
        if (granted) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.perm_granted),
                tint = status.success,
                modifier = Modifier.size(28.dp)
            )
        } else {
            HsrButton(
                text = stringResource(R.string.action_allow),
                onClick = onGrant,
                minHeight = TouchTarget.min
            )
        }
    }
}
