# WakeAlarm ProGuard Rules

# Keep NotificationListenerService (referenced by system via manifest)
-keep class com.kindness.wakealarm.service.WaNotificationListenerService { *; }

# Keep BroadcastReceiver (referenced by system via manifest)
-keep class com.kindness.wakealarm.receiver.PersistentToggleReceiver { *; }

# Keep DataStore Preferences keys
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences$Key { *; }

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**

# Keep Compose runtime
-dontwarn androidx.compose.**

# Release builds: drop verbose/debug/info logging (it can reveal when urgent messages arrive).
# Warnings and errors stay for crash diagnostics.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
