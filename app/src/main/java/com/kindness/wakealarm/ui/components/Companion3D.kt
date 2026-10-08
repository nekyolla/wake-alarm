package com.kindness.wakealarm.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import android.view.Surface
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kindness.wakealarm.R
import com.kindness.wakealarm.ui.theme.Crimson
import com.kindness.wakealarm.ui.theme.Gold
import com.kindness.wakealarm.ui.theme.GoldSoft
import com.kindness.wakealarm.ui.theme.PanelLowest
import com.kindness.wakealarm.ui.theme.Violet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class CompanionMood { Calm, Alarm }

/**
 * Finds the bundled companion animation in `app/src/main/assets/`:
 * - `companion.webp`: animated WebP with a transparent background, shown floating freely;
 * - `companion.gif`: shown inside the gold holographic frame (a GIF usually has a solid background).
 * Without either, the app shows the mascot from the launcher icon instead.
 */
object CompanionAssets {
    private val CANDIDATES = listOf("companion.webp", "companion.gif")

    @Volatile
    private var resolved = false
    private var cached: String? = null

    fun find(context: Context): String? {
        if (!resolved) {
            cached = try {
                val listed = context.assets.list("").orEmpty().toSet()
                CANDIDATES.firstOrNull { it in listed }
            } catch (e: Exception) {
                null
            }
            resolved = true
        }
        return cached
    }
}

private const val TAG = "Companion3D"
private const val MAX_DECODE_PX = 720

/** Loading state of the bundled animation. */
private sealed interface Animated {
    data object Loading : Animated
    data class Ready(val drawable: Drawable) : Animated
    data object Failed : Animated
}

/**
 * The app's companion, shown "in 3D": it tilts with the phone (gravity sensor), floats, and is
 * circled by an orbit of card-suit diamonds that pass behind and in front of it.
 *
 * Plays the bundled GIF/animated WebP on Android 9+ inside a holographic frame; otherwise shows
 * the mascot from the app icon. All motion stops when system animations are turned off.
 * Decorative only: screen readers skip it.
 *
 * @param size height of the figure; the orbit and glow take ~25% more room around it.
 */
@Composable
fun Companion3D(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    mood: CompanionMood = CompanionMood.Calm
) {
    val context = LocalContext.current
    val reduceMotion = rememberReduceMotion()
    val assetName = remember { CompanionAssets.find(context) }
    val animated: Animated = if (assetName != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        rememberAnimatedAsset(assetName)
    } else {
        Animated.Failed
    }

    val tilt = rememberDeviceTilt(enabled = !reduceMotion)
    val clock = rememberInfiniteTransition(label = "companion").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(if (mood == CompanionMood.Alarm) 2_400 else 7_000, easing = LinearEasing)
        ),
        label = "companion_clock"
    )
    fun time(): Float = if (reduceMotion) 0.125f else clock.value

    Box(modifier.size(size * 1.25f), contentAlignment = Alignment.Center) {
        // Behind the figure: glow, alarm ripples and the far half of the orbit
        Canvas(Modifier.fillMaxSize()) {
            val t = time()
            drawGlow(t, mood)
            drawOrbit(t, front = false)
        }

        Box(
            Modifier
                .size(size)
                .graphicsLayer {
                    val t = time()
                    val swing = if (reduceMotion) 0f else sin(2f * PI.toFloat() * t)
                    rotationY = tilt.value.x * 16f + swing * 6f
                    rotationX = -tilt.value.y * 10f
                    translationY = if (reduceMotion) 0f else sin(4f * PI.toFloat() * t) * 6.dp.toPx()
                    cameraDistance = 14f * density
                    if (mood == CompanionMood.Alarm && !reduceMotion) {
                        val beat = 1f + 0.035f * sin(8f * PI.toFloat() * t)
                        scaleX = beat
                        scaleY = beat
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            when (animated) {
                is Animated.Ready -> if (assetName?.endsWith(".gif") == true) {
                    HoloFrame(animated.drawable, size, playing = !reduceMotion, shimmer = { time() })
                } else {
                    FloatingAnimation(animated.drawable, size, playing = !reduceMotion)
                }
                Animated.Loading -> Unit
                Animated.Failed -> Image(
                    painter = painterResource(R.drawable.mascot),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // In front of the figure: the near half of the orbit
        Canvas(Modifier.fillMaxSize()) {
            drawOrbit(time(), front = true)
        }
    }
}

/** A transparent animation (companion.webp), free-floating like the mascot. */
@Composable
private fun FloatingAnimation(drawable: Drawable, size: Dp, playing: Boolean) {
    val ratio = drawable.aspectRatio()
    DisposableEffect(drawable, playing) {
        if (playing) AnimatedImageDrawableCompat.start(drawable) else AnimatedImageDrawableCompat.stop(drawable)
        onDispose { AnimatedImageDrawableCompat.stop(drawable) }
    }
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        },
        update = { view ->
            if (view.drawable !== drawable) view.setImageDrawable(drawable)
        },
        modifier = Modifier
            .width(if (ratio <= 1f) size * ratio else size)
            .height(if (ratio <= 1f) size else size / ratio)
    )
}

private fun Drawable.aspectRatio(): Float =
    if (intrinsicWidth > 0 && intrinsicHeight > 0) {
        (intrinsicWidth.toFloat() / intrinsicHeight).coerceIn(0.5f, 1.6f)
    } else {
        1f
    }

/** The bundled animation in an angled gold frame with a moving holographic sheen. */
@Composable
private fun HoloFrame(drawable: Drawable, size: Dp, playing: Boolean, shimmer: () -> Float) {
    val ratio = drawable.aspectRatio()
    val frameWidth = if (ratio <= 1f) size * ratio else size
    val frameHeight = if (ratio <= 1f) size else size / ratio

    DisposableEffect(drawable, playing) {
        if (playing) AnimatedImageDrawableCompat.start(drawable) else AnimatedImageDrawableCompat.stop(drawable)
        onDispose { AnimatedImageDrawableCompat.stop(drawable) }
    }

    Box(
        Modifier
            .width(frameWidth)
            .height(frameHeight)
            .hsrPanel(MaterialTheme.shapes.large, container = PanelLowest, accent = Gold)
            .drawWithContent {
                drawContent()
                val x = (shimmer() * 2f - 0.5f) * this.size.width
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.14f), Color.Transparent),
                        start = Offset(x - this.size.width * 0.35f, 0f),
                        end = Offset(x + this.size.width * 0.35f, this.size.height)
                    )
                )
            }
    ) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
                }
            },
            update = { view ->
                if (view.drawable !== drawable) view.setImageDrawable(drawable)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/** Keeps API 28 classes out of code paths that run on Android 8. */
private object AnimatedImageDrawableCompat {
    fun start(drawable: Drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) (drawable as? AnimatedImageDrawable)?.start()
    }

    fun stop(drawable: Drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) (drawable as? AnimatedImageDrawable)?.stop()
    }
}

// The lint check misses the assignment below (it doesn't follow it through withContext)
@SuppressLint("ProduceStateDoesNotAssignValue")
@RequiresApi(Build.VERSION_CODES.P)
@Composable
private fun rememberAnimatedAsset(name: String): Animated {
    val context = LocalContext.current
    val state = produceState<Animated>(initialValue = Animated.Loading, name) {
        value = withContext(Dispatchers.IO) { decodeAnimatedAsset(context, name) }
    }
    return state.value
}

@RequiresApi(Build.VERSION_CODES.P)
private fun decodeAnimatedAsset(context: Context, name: String): Animated =
    try {
        val drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(context.assets, name)) { decoder, info, _ ->
            // Large GIFs are scaled down while decoding; the frame is never bigger than this
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > MAX_DECODE_PX) {
                val scale = MAX_DECODE_PX.toFloat() / longest
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1)
                )
            }
        }
        (drawable as? AnimatedImageDrawable)?.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
        Animated.Ready(drawable)
    } catch (e: Exception) {
        Log.w(TAG, "Unable to decode companion asset $name", e)
        Animated.Failed
    }

/**
 * Device tilt from the gravity sensor, smoothed, in -1..1 on both axes (x: left/right,
 * y: forward/back relative to a phone held at a normal reading angle). Listens only while the
 * screen is resumed.
 */
@Composable
fun rememberDeviceTilt(enabled: Boolean): State<Offset> {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tilt = remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(enabled, lifecycleOwner, view) {
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (!enabled || sensorManager == null || sensor == null) {
            tilt.value = Offset.Zero
            return@DisposableEffect onDispose { }
        }

        var smoothX = 0f
        var smoothY = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Sensor axes follow the device's natural orientation; map them to the screen's
                val (screenX, screenY) = when (view.display?.rotation ?: Surface.ROTATION_0) {
                    Surface.ROTATION_90 -> -event.values[1] to event.values[0]
                    Surface.ROTATION_180 -> -event.values[0] to -event.values[1]
                    Surface.ROTATION_270 -> event.values[1] to -event.values[0]
                    else -> event.values[0] to event.values[1]
                }
                val x = (-screenX / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
                // ~0.6 g along the screen's vertical axis is how most people hold a phone; treat that as level
                val y = (screenY / SensorManager.GRAVITY_EARTH - 0.6f).coerceIn(-1f, 1f)
                smoothX += (x - smoothX) * 0.12f
                smoothY += (y - smoothY) * 0.12f
                tilt.value = Offset(smoothX, smoothY)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME ->
                    sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
                Lifecycle.Event.ON_PAUSE -> sensorManager.unregisterListener(listener)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sensorManager.unregisterListener(listener)
        }
    }
    return tilt
}

// --- Drawing -----------------------------------------------------------------------------------

private val OrbitColors = listOf(Crimson, Gold, Violet)

private fun DrawScope.drawGlow(t: Float, mood: CompanionMood) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val pulse = if (mood == CompanionMood.Alarm) 0.5f + 0.5f * sin(4f * PI.toFloat() * t) else 0.5f
    val radius = size.minDimension * 0.48f
    drawCircle(
        Brush.radialGradient(
            listOf(Crimson.copy(alpha = 0.22f + 0.25f * pulse), Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
    if (mood == CompanionMood.Alarm) {
        // Two ripples expanding outward, offset by half a cycle
        repeat(2) { i ->
            val p = (t * 2f + i * 0.5f) % 1f
            drawCircle(
                Crimson.copy(alpha = 0.5f * (1f - p)),
                radius = size.minDimension * (0.25f + 0.25f * p),
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/**
 * A tilted ring (an ellipse) with three diamonds travelling along it. The far half (upper arc)
 * is drawn behind the figure and the near half in front, which reads as depth.
 */
private fun DrawScope.drawOrbit(t: Float, front: Boolean) {
    val cx = size.width / 2f
    val cy = size.height * 0.64f
    val rx = size.width * 0.46f
    val ry = rx * 0.22f
    val ringStroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
    drawArc(
        color = GoldSoft.copy(alpha = if (front) 0.55f else 0.22f),
        startAngle = if (front) 0f else 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(cx - rx, cy - ry),
        size = Size(rx * 2f, ry * 2f),
        style = ringStroke
    )
    OrbitColors.forEachIndexed { index, color ->
        val angle = 2f * PI.toFloat() * (t + index / 3f)
        val depth = sin(angle) // > 0: near side (lower arc)
        if ((depth > 0f) != front) return@forEachIndexed
        val x = cx + rx * cos(angle)
        val y = cy + ry * depth
        val scale = 0.65f + 0.35f * (depth + 1f) / 2f
        drawDiamond(Offset(x, y), 7.dp.toPx() * scale, color.copy(alpha = 0.55f + 0.45f * (depth + 1f) / 2f))
    }
}

private fun DrawScope.drawDiamond(center: Offset, half: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - half)
        lineTo(center.x + half * 0.7f, center.y)
        lineTo(center.x, center.y + half)
        lineTo(center.x - half * 0.7f, center.y)
        close()
    }
    drawPath(path, color)
}
