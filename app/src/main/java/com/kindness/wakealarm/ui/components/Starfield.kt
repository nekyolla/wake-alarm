package com.kindness.wakealarm.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.kindness.wakealarm.ui.theme.Crimson
import com.kindness.wakealarm.ui.theme.GoldSoft
import com.kindness.wakealarm.ui.theme.Ivory
import com.kindness.wakealarm.ui.theme.SpaceBlack
import com.kindness.wakealarm.ui.theme.SpaceDeep
import com.kindness.wakealarm.ui.theme.SpaceWine
import com.kindness.wakealarm.ui.theme.Violet
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private class Star(
    val x: Float,
    val y: Float,
    val radiusDp: Float,
    val phase: Float,
    val speed: Int,
    val sparkle: Boolean
)

/**
 * Deep-space backdrop: a dark gradient, two soft nebulae (crimson and violet) and twinkling stars,
 * a few of them drawn as gold four-point sparkles like the ones on the app icon.
 *
 * Only the draw phase reads the animation clock, so twinkling never recomposes the screen.
 *
 * @param nebula how strong the nebulae glow; the alarm screen turns it up.
 */
@Composable
fun StarfieldBackground(
    modifier: Modifier = Modifier,
    nebula: Float = 1f,
    nebulaColor: Color = Crimson,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val reduceMotion = rememberReduceMotion()
    val stars = remember {
        val random = Random(7)
        List(90) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radiusDp = 0.5f + random.nextFloat() * 1.3f,
                phase = random.nextFloat(),
                // Whole cycles per loop, so the twinkle is seamless when the clock wraps
                speed = 1 + random.nextInt(3),
                sparkle = random.nextFloat() < 0.07f
            )
        }
    }
    val clock: State<Float> = if (reduceMotion) {
        remember { mutableFloatStateOf(0.25f) }
    } else {
        rememberInfiniteTransition(label = "starfield").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing)),
            label = "starfield_clock"
        )
    }
    val glow = rememberUpdatedState(nebula)
    val glowColor = rememberUpdatedState(nebulaColor)

    Box(
        modifier.drawBehind {
            drawSpace(glow.value, glowColor.value)
            drawStars(stars, clock.value)
        },
        content = content
    )
}

private fun DrawScope.drawSpace(nebula: Float, nebulaColor: Color) {
    drawRect(Brush.verticalGradient(listOf(SpaceBlack, SpaceDeep, SpaceWine)))
    val w = size.width
    val h = size.height
    drawCircle(
        brush = Brush.radialGradient(
            listOf(nebulaColor.copy(alpha = (0.20f * nebula).coerceAtMost(0.6f)), Color.Transparent),
            center = Offset(w * 0.9f, h * 0.12f),
            radius = w * 0.85f
        ),
        radius = w * 0.85f,
        center = Offset(w * 0.9f, h * 0.12f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Violet.copy(alpha = (0.13f * nebula).coerceAtMost(0.4f)), Color.Transparent),
            center = Offset(w * 0.05f, h * 0.78f),
            radius = w * 0.75f
        ),
        radius = w * 0.75f,
        center = Offset(w * 0.05f, h * 0.78f)
    )
}

private fun DrawScope.drawStars(stars: List<Star>, clock: Float) {
    stars.forEach { star ->
        val twinkle = 0.5f + 0.5f * sin(2f * PI.toFloat() * (clock * star.speed + star.phase))
        val center = Offset(star.x * size.width, star.y * size.height)
        if (star.sparkle) {
            val arm = (3.dp.toPx() + 2.dp.toPx() * twinkle)
            val color = GoldSoft.copy(alpha = 0.35f + 0.55f * twinkle)
            val stroke = 1.dp.toPx()
            drawLine(color, center.copy(x = center.x - arm), center.copy(x = center.x + arm), stroke)
            drawLine(color, center.copy(y = center.y - arm), center.copy(y = center.y + arm), stroke)
            drawCircle(color, radius = 1.2.dp.toPx(), center = center)
        } else {
            drawCircle(
                Ivory.copy(alpha = 0.15f + 0.6f * twinkle),
                radius = star.radiusDp.dp.toPx(),
                center = center
            )
        }
    }
}
