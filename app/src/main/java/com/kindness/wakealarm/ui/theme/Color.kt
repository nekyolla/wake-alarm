package com.kindness.wakealarm.ui.theme

import androidx.compose.ui.graphics.Color

// Brand — indigo. Light/dark variants are tuned for WCAG AA contrast on their surfaces.
val BrandIndigo = Color(0xFF4F6BFF)

// Light scheme
val LightPrimary = Color(0xFF3A55E0)
val LightOnPrimary = Color.White
val LightPrimaryContainer = Color(0xFFDDE1FF)
val LightOnPrimaryContainer = Color(0xFF001257)
val LightSecondary = Color(0xFF006B5E)
val LightSecondaryContainer = Color(0xFFB4F0E2)
val LightOnSecondaryContainer = Color(0xFF00201B)
val LightBackground = Color(0xFFF7F8FC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE3E5F0)
val LightSurfaceContainerLow = Color(0xFFF1F2F8)
val LightSurfaceContainer = Color(0xFFEBEDF5)
val LightSurfaceContainerHigh = Color(0xFFE5E7F0)
val LightOnSurface = Color(0xFF1A1C23)
val LightOnSurfaceVariant = Color(0xFF454857)
val LightOutline = Color(0xFF767888)
val LightOutlineVariant = Color(0xFFC6C8D6)

// Dark scheme
val DarkPrimary = Color(0xFFB9C3FF)
val DarkOnPrimary = Color(0xFF00218A)
val DarkPrimaryContainer = Color(0xFF2D45C7)
val DarkOnPrimaryContainer = Color(0xFFDDE1FF)
val DarkSecondary = Color(0xFF7BD8C6)
val DarkSecondaryContainer = Color(0xFF005047)
val DarkOnSecondaryContainer = Color(0xFFB4F0E2)
val DarkBackground = Color(0xFF0E1016)
val DarkSurface = Color(0xFF0E1016)
val DarkSurfaceVariant = Color(0xFF2B2E3A)
val DarkSurfaceContainerLow = Color(0xFF161821)
val DarkSurfaceContainer = Color(0xFF1B1E27)
val DarkSurfaceContainerHigh = Color(0xFF242733)
val DarkOnSurface = Color(0xFFE3E4EE)
val DarkOnSurfaceVariant = Color(0xFFC5C6D4)
val DarkOutline = Color(0xFF8F909E)
val DarkOutlineVariant = Color(0xFF454857)

// Semantic status colors (content color + container)
val LightSuccess = Color(0xFF1B7F3B)
val LightSuccessContainer = Color(0xFFD3F5DC)
val LightWarning = Color(0xFF8A5300)
val LightWarningContainer = Color(0xFFFFE3B8)
val LightAlarm = Color(0xFFC4002B)
val LightAlarmContainer = Color(0xFFFFDADC)

val DarkSuccess = Color(0xFF6EDD8E)
val DarkSuccessContainer = Color(0xFF0F3A1D)
val DarkWarning = Color(0xFFFFC266)
val DarkWarningContainer = Color(0xFF3F2A00)
val DarkAlarm = Color(0xFFFF8A98)
val DarkAlarmContainer = Color(0xFF5C0016)

// Full-screen alarm (always dark, high-energy red)
val AlarmScreenTop = Color(0xFF9B0020)
val AlarmScreenMid = Color(0xFF3A0610)
val AlarmScreenBottom = Color(0xFF0D0D11)
val AlarmAccent = Color(0xFFFF4D6A)

/** ARGB accent for notifications (NotificationCompat.Builder.setColor). */
val NotificationAccent: Int = 0xFFE8364F.toInt()
