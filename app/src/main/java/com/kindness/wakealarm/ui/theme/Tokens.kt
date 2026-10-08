package com.kindness.wakealarm.ui.theme

import androidx.compose.ui.unit.dp

/** Spacing scale. Screens use [gutter] horizontally and the scale for everything else. */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val gutter = 20.dp
}

/** Minimum touch targets. [hero] is for the one action a screen is about. */
object TouchTarget {
    val min = 48.dp
    val comfortable = 56.dp
    val hero = 64.dp
}

/** Line widths for panel borders and ornaments. */
object Stroke {
    val hairline = 1.dp
    val ornament = 2.dp
}

/** Animation durations in milliseconds. */
object Motion {
    const val SHORT = 150
    const val MEDIUM = 300
    const val LONG = 600
}
