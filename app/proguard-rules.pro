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
