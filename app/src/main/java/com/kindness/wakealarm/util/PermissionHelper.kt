package com.kindness.wakealarm.util

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Utility to check permission statuses and generate deep-link intents to settings.
 */
object PermissionHelper {

    /**
     * Every special access the app asks for. [required] ones are needed for the alarm to work at all;
     * the rest make it more reliable.
     */
    enum class AppPermission(val required: Boolean) {
        NOTIFICATION_LISTENER(true),
        POST_NOTIFICATIONS(true),
        FULL_SCREEN_INTENT(true),
        BATTERY_OPTIMIZATION(true),
        OVERLAY(false),
        DND_ACCESS(false)
    }

    data class PermissionStatus(private val granted: Map<AppPermission, Boolean>) {
        fun isGranted(permission: AppPermission): Boolean = granted[permission] == true

        val notificationListener: Boolean get() = isGranted(AppPermission.NOTIFICATION_LISTENER)
        val allGranted: Boolean get() = AppPermission.entries.all { isGranted(it) }
        val requiredGranted: Boolean get() = AppPermission.entries.filter { it.required }.all { isGranted(it) }
        val missingRequiredCount: Int get() = AppPermission.entries.count { it.required && !isGranted(it) }
        val grantedCount: Int get() = AppPermission.entries.count { isGranted(it) }
        val totalCount: Int get() = AppPermission.entries.size
    }

    /**
     * Check all required permission statuses.
     */
    fun checkAll(context: Context): PermissionStatus = PermissionStatus(
        AppPermission.entries.associateWith { isGranted(context, it) }
    )

    fun isGranted(context: Context, permission: AppPermission): Boolean = when (permission) {
        AppPermission.NOTIFICATION_LISTENER -> isNotificationListenerEnabled(context)
        AppPermission.POST_NOTIFICATIONS -> NotificationManagerCompat.from(context).areNotificationsEnabled()
        AppPermission.FULL_SCREEN_INTENT -> canUseFullScreenIntent(context)
        AppPermission.BATTERY_OPTIMIZATION -> isBatteryOptimizationExempt(context)
        AppPermission.OVERLAY -> Settings.canDrawOverlays(context)
        AppPermission.DND_ACCESS -> isDndAccessGranted(context)
    }

    // --- Individual checks ---

    fun isNotificationListenerEnabled(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun isBatteryOptimizationExempt(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isDndAccessGranted(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return nm.canUseFullScreenIntent()
        }
        return true // Pre-Android 14, full-screen intent is always allowed
    }

    // --- Deep links ---

    /**
     * Open the system screen where [permission] is granted. Falls back to the app's details page
     * when a device doesn't support the specific screen.
     */
    fun openSettings(context: Context, permission: AppPermission) {
        val pkg = Uri.parse("package:${context.packageName}")
        val primary = when (permission) {
            AppPermission.NOTIFICATION_LISTENER -> notificationListenerIntent(context)
            AppPermission.POST_NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            AppPermission.FULL_SCREEN_INTENT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg)
            } else {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            // The alarm is this app's core function and must survive Doze, which is an
            // accepted use case for the direct exemption request.
            AppPermission.BATTERY_OPTIMIZATION -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg)
            AppPermission.OVERLAY -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkg)
            AppPermission.DND_ACCESS -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        }
        val secondary = when (permission) {
            AppPermission.NOTIFICATION_LISTENER -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            AppPermission.BATTERY_OPTIMIZATION -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            else -> null
        }
        startFirstAvailable(context, listOfNotNull(primary, secondary, appDetailsIntent(context)))
    }

    /**
     * On Android 11+ this jumps straight to this app's toggle instead of the full listener list.
     */
    private fun notificationListenerIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(context, "com.kindness.wakealarm.service.WaNotificationListenerService").flattenToString()
            )
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }
    }

    fun appDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

    // --- OEM specifics ---

    val isXiaomiFamily: Boolean
        get() = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")

    /**
     * Best-effort deep link into MIUI/HyperOS Autostart. Falls back to the app's details page.
     */
    fun openXiaomiAutostart(context: Context) {
        val candidates = listOf(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")
        ).map { Intent().setComponent(it) }
        startFirstAvailable(context, candidates + appDetailsIntent(context))
    }

    private fun startFirstAvailable(context: Context, intents: List<Intent>) {
        for (intent in intents) {
            try {
                if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
    }
}
