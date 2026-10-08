package com.kindness.wakealarm.ui.theme

import androidx.compose.ui.graphics.Color

// "Astral Express · Crimson" — a dark, starry palette taken from the app icon:
// crimson clock, gold sparkles, violet eye, on a deep-space background.
// Text colors meet WCAG AA on every panel color below.

// Space (backgrounds)
val SpaceBlack = Color(0xFF07080F)
val SpaceDeep = Color(0xFF0D0E19)
val SpaceWine = Color(0xFF160A15)

// Panels (glass surfaces, lowest to highest)
val PanelLowest = Color(0xFF0A0B14)
val PanelLow = Color(0xFF12131F)
val Panel = Color(0xFF171828)
val PanelHigh = Color(0xFF1B1D2C)
val PanelHighest = Color(0xFF24273A)
val LineSubtle = Color(0xFF2C2F44)
val LineStrong = Color(0xFF454962)

// Gold — primary actions and selection (HSR's "5★")
val Gold = Color(0xFFE9C46A)
val GoldSoft = Color(0xFFE7C27D)
val GoldDeep = Color(0xFFB8913A)
val OnGold = Color(0xFF2A1E00)
val GoldContainer = Color(0xFF3A2C0B)
val OnGoldContainer = Color(0xFFFFE8B0)

// Crimson — brand and alarm
val Crimson = Color(0xFFE8364F)
val CrimsonLight = Color(0xFFFF8A9A)
val CrimsonDeep = Color(0xFFC8233D)
val CrimsonContainer = Color(0xFF4A0F1C)
val OnCrimsonContainer = Color(0xFFFFD9DE)

// Violet — secondary accent (HSR's "4★", the mascot's eye)
val Violet = Color(0xFFA77BFF)
val VioletContainer = Color(0xFF2C1F4F)
val OnVioletContainer = Color(0xFFE6DAFF)

// Text
val Ivory = Color(0xFFF2EEE8)
val Mist = Color(0xFFA7A9BE)
val MistDim = Color(0xFF6E7189)

// Status (content + container)
val Success = Color(0xFF5ED6A0)
val SuccessContainer = Color(0xFF0E2E22)
val Warning = Color(0xFFF4B860)
val WarningContainer = Color(0xFF3A2808)
val Alarm = Color(0xFFFF5A6E)
val AlarmContainer = Color(0xFF4A0A16)

// Full-screen alarm
val AlarmScreenTop = Color(0xFF5C0A1C)
val AlarmScreenMid = Color(0xFF1C0710)
val AlarmScreenBottom = SpaceBlack
val AlarmAccent = Color(0xFFFF4D6A)

/** ARGB accent for notifications (NotificationCompat.Builder.setColor). */
val NotificationAccent: Int = 0xFFE8364F.toInt()
