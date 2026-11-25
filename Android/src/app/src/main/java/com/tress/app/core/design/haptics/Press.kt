/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Press, wobble, and velocity stretch physics adapted from Artefact
 * (https://github.com/Chethan616/Artefact, MIT OR Apache-2.0, Copyright 2026 Chethan616)
 * and LiquidTracker from AndroidLiquidGlassView by Donny Yale (QmDeve, MIT).
 */

package com.tress.app.core.design.haptics

import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import com.tress.app.core.design.motion.Fluid
import com.tress.app.core.design.motion.GlassMotion
import com.tress.app.core.design.motion.MotionSettings
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

suspend fun PointerInputScope.inspectDragGestures(
  onDragStart: (down: PointerInputChange) -> Unit = {},
  onDragEnd: (change: PointerInputChange) -> Unit = {},
  onDragCancel: () -> Unit = {},
  onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
  awaitEachGesture {
    val initialDown = awaitFirstDown(false, PointerEventPass.Initial)
    val down = awaitFirstDown(false)
    onDragStart(down)
    onDrag(initialDown, Offset.Zero)
    val up = drag(initialDown.id) { onDrag(it, it.positionChange()) }
    if (up == null) onDragCancel() else onDragEnd(up)
  }
}

private suspend inline fun AwaitPointerEventScope.drag(
  pointerId: PointerId,
  onDrag: (PointerInputChange) -> Unit,
): PointerInputChange? {
  if (currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true) return null
  var pointer = pointerId
  while (true) {
    val change = awaitDragOrUp(pointer) ?: return null
    if (change.isConsumed) return null
    if (change.changedToUpIgnoreConsumed()) return change
    onDrag(change)
    pointer = change.id
  }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(pointerId: PointerId): PointerInputChange? {
  var pointer = pointerId
  while (true) {
    val event = awaitPointerEvent()
    val change = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
    if (change.changedToUpIgnoreConsumed()) {
      val other = event.changes.fastFirstOrNull { it.pressed } ?: return change
      pointer = other.id
    } else if (change.previousPosition != change.position) {
      return change
    }
  }
}

private const val WobbleKick = 30f
private const val PressKick = 9f

class InteractiveHighlight(
  val animationScope: CoroutineScope,
  val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset },
  private val longPressExpand: Boolean = false,
  private val onLongPressExpand: (() -> Unit)? = null,
) {
  private val pressInSpec get() = spring(MotionSettings.damping(0.66f), MotionSettings.stiffness(620f), 0.001f)
  private val pressOutSpec get() = spring(MotionSettings.damping(0.54f), MotionSettings.stiffness(380f), 0.001f)
  private val positionSpec get() = spring(MotionSettings.damping(0.56f), MotionSettings.stiffness(360f), Offset.VisibilityThreshold)
  private val wobbleSpec get() = spring(MotionSettings.damping(0.46f), MotionSettings.stiffness(420f), 0.001f)
  private val expandInSpec get() = spring(MotionSettings.damping(0.56f), MotionSettings.stiffness(340f), 0.001f)
  private val expandOutSpec get() = spring(MotionSettings.damping(0.50f), MotionSettings.stiffness(420f), 0.001f)
  private val glowInSpec get() = spring(1f, MotionSettings.stiffness(900f), 0.001f)
  private val glowOutSpec get() = spring(1f, MotionSettings.stiffness(380f), 0.001f)

  private val pressAnim = Animatable(0f, 0.001f)
  private val positionAnim = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)
  private val expandAnim = Animatable(0f, 0.001f)
  private val wobbleAnim = Animatable(0f, 0.001f)
  private val glowAnim = Animatable(0f, 0.001f)
  private val bloomGlowAnim = Animatable(0f, 0.001f)

  private var startPosition = Offset.Zero
  private var positionJob: Job? = null
  private var longPressJob: Job? = null
  private var releaseJob: Job? = null
  private var downAt = 0L

  val pressProgress: Float get() = pressAnim.value
  val offset: Offset get() = positionAnim.value - startPosition
  val expandProgress: Float get() = expandAnim.value
  val wobble: Float get() = wobbleAnim.value

  private val shader: Shader? by lazy(LazyThreadSafetyMode.NONE) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) PressGlow.create() else null
  }

  val modifier: Modifier = Modifier.drawWithContent {
    val progress = glowAnim.value.fastCoerceIn(0f, 1f)
    val bloom = bloomGlowAnim.value.fastCoerceIn(0f, 1f)
    if (progress > 0f || bloom > 0f) {
      val s = shader
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && s != null) {
        drawRect(Color.White.copy(0.08f * progress + 0.05f * bloom), blendMode = BlendMode.Plus)
        val p = position(size, positionAnim.value)
        PressGlow.update(
          s,
          size,
          Color.White.copy(0.15f * progress + 0.10f * bloom).toArgb(),
          size.minDimension * (1.5f + 0.5f * bloom),
          Offset(p.x.fastCoerceIn(0f, size.width), p.y.fastCoerceIn(0f, size.height)),
        )
        drawRect(ShaderBrush(s), blendMode = BlendMode.Plus)
      } else {
        drawRect(Color.White.copy(0.22f * progress), blendMode = BlendMode.Plus)
      }
    }
    drawContent()
  }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private object PressGlow {
    private var shared: Shader? = null

    fun create(): Shader = shared ?: RuntimeShader(
      """
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 position;

half4 main(float2 coord) {
    float dist = distance(coord, position);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}""",
    ).also { shared = it }

    fun update(shader: Shader, size: Size, color: Int, radius: Float, position: Offset) {
      (shader as RuntimeShader).apply {
        setFloatUniform("size", size.width, size.height)
        setColorUniform("color", color)
        setFloatUniform("radius", radius)
        setFloatUniform("position", position.x, position.y)
      }
    }
  }

  private fun release() {
    positionJob?.cancel()
    longPressJob?.cancel()
    releaseJob?.cancel()
    releaseJob = animationScope.launch {
      val held = SystemClock.uptimeMillis() - downAt
      if (held < GlassMotion.MinPressMillis) delay(GlassMotion.MinPressMillis - held)
      val kick = if (MotionSettings.isReducedMotion) 0f
      else WobbleKick * MotionSettings.style.wobble.fastCoerceIn(0f, 3f) * pressAnim.value.fastCoerceIn(0f, 1.1f)
      launch { wobbleAnim.animateTo(0f, wobbleSpec, initialVelocity = kick) }
      launch { pressAnim.animateTo(0f, pressOutSpec) }
      launch { glowAnim.animateTo(0f, glowOutSpec) }
      launch { positionAnim.animateTo(startPosition, positionSpec) }
      if (expandAnim.value != 0f || expandAnim.targetValue != 0f) {
        launch { expandAnim.animateTo(0f, expandOutSpec) }
        launch { bloomGlowAnim.animateTo(0f, glowOutSpec) }
      }
    }
  }

  fun settle() {
    if (pressAnim.value == 0f && wobbleAnim.value == 0f && glowAnim.value == 0f && expandAnim.value == 0f) return
    releaseJob?.cancel()
    longPressJob?.cancel()
    val quick = spring<Float>(1f, 1400f, 0.004f)
    releaseJob = animationScope.launch {
      launch { wobbleAnim.animateTo(0f, quick) }
      launch { pressAnim.animateTo(0f, quick) }
      launch { glowAnim.animateTo(0f, quick) }
      launch { expandAnim.animateTo(0f, quick) }
      launch { bloomGlowAnim.animateTo(0f, quick) }
      launch { positionAnim.snapTo(startPosition) }
    }
  }

  val gestureModifier: Modifier = Modifier.pointerInput(animationScope) {
    val longPressMs = viewConfiguration.longPressTimeoutMillis
    val slop = viewConfiguration.touchSlop
    inspectDragGestures(
      onDragStart = { down ->
        startPosition = down.position
        downAt = SystemClock.uptimeMillis()
        positionJob?.cancel()
        longPressJob?.cancel()
        releaseJob?.cancel()
        val kick = if (MotionSettings.isReducedMotion) 0f else -PressKick
        animationScope.launch {
          launch { pressAnim.animateTo(1f, pressInSpec) }
          launch { glowAnim.animateTo(1f, glowInSpec) }
          launch { positionAnim.snapTo(startPosition) }
          launch { wobbleAnim.animateTo(0f, wobbleSpec, initialVelocity = kick) }
        }
        if (longPressExpand) {
          longPressJob = animationScope.launch {
            delay(longPressMs)
            onLongPressExpand?.invoke()
            launch { bloomGlowAnim.animateTo(1f, glowInSpec) }
            expandAnim.animateTo(1f, expandInSpec)
          }
        }
      },
      onDragEnd = { release() },
      onDragCancel = { release() },
    ) { change, _ ->
      if (longPressJob?.isActive == true && expandAnim.targetValue == 0f && (change.position - startPosition).getDistance() > slop) {
        longPressJob?.cancel()
      }
      positionJob?.cancel()
      positionJob = animationScope.launch { positionAnim.snapTo(change.position) }
    }
  }
}

fun GraphicsLayerScope.liquidPressTransform(h: InteractiveHighlight) {
  if (MotionSettings.isReducedMotion) return
  val width = size.width
  val height = size.height
  if (!(width > 0f && height > 0f)) return
  val motion = MotionSettings.style
  val progress = h.pressProgress
  val bloomAmount = motion.bloom.fastCoerceIn(0f, 2.5f)
  val bloom = h.expandProgress * (14f.dp.toPx() * bloomAmount / size.maxDimension).fastCoerceAtMost(0.12f * bloomAmount)
  val scale = lerp(1f, 1f + 4f.dp.toPx() * motion.press.fastCoerceIn(0f, 2.5f) / height, progress) + bloom
  val maxOffset = size.minDimension
  val stretch = motion.stretch.fastCoerceIn(0f, 2.5f)
  val k = 0.05f * stretch
  val offset = h.offset
  translationX = maxOffset * tanh(k * offset.x / maxOffset)
  translationY = maxOffset * tanh(k * offset.y / maxOffset)
  val maxDragScale = 4f.dp.toPx() * stretch / height
  val angle = atan2(offset.y, offset.x)
  val w = h.wobble
  val wobbleX = w * (2.2f.dp.toPx() / width).fastCoerceAtMost(0.04f)
  val wobbleY = w * (2.2f.dp.toPx() / height).fastCoerceAtMost(0.04f)
  scaleX = scale + maxDragScale * abs(cos(angle) * offset.x / size.maxDimension) * (width / height).fastCoerceAtMost(1f) + wobbleX
  scaleY = scale + maxDragScale * abs(sin(angle) * offset.y / size.maxDimension) * (height / width).fastCoerceAtMost(1f) - wobbleY
}

@Stable
class JellyPress internal constructor(private val scope: CoroutineScope, private val pressedScale: Float) {
  private val anim = Animatable(1f, 0.0005f)
  private var downAt = 0L
  private var releaseJob: Job? = null

  val scale: Float get() = anim.value
  val progress: Float get() = ((1f - anim.value) / (1f - pressedScale)).fastCoerceIn(0f, 1f)

  fun press() {
    if (MotionSettings.isReducedMotion) return
    releaseJob?.cancel()
    downAt = SystemClock.uptimeMillis()
    scope.launch { anim.animateTo(pressedScale, Fluid.pressIn()) }
  }

  fun release() {
    if (MotionSettings.isReducedMotion) {
      rest()
      return
    }
    releaseJob?.cancel()
    releaseJob = scope.launch {
      val held = SystemClock.uptimeMillis() - downAt
      if (held < GlassMotion.MinPressMillis) delay(GlassMotion.MinPressMillis - held)
      anim.animateTo(1f, Fluid.pressOut())
    }
  }

  fun rest() {
    releaseJob?.cancel()
    scope.launch { anim.snapTo(1f) }
  }
}

@Composable
fun rememberJellyPress(interaction: InteractionSource, pressedScale: Float = Fluid.PressedScale): JellyPress {
  val scope = rememberCoroutineScope()
  val jelly = remember(scope, pressedScale) { JellyPress(scope, pressedScale) }
  LaunchedEffect(interaction, jelly) {
    interaction.interactions.collect { i ->
      when (i) {
        is PressInteraction.Press -> jelly.press()
        is PressInteraction.Release, is PressInteraction.Cancel -> jelly.release()
      }
    }
  }
  return jelly
}

@Stable
class LiquidStretchState internal constructor(
  private val scope: CoroutineScope,
  private val stretchFactor: Float,
  private val minScale: Float,
  private val maxScale: Float,
  private val settleDelayMs: Long,
) {
  private val spec get() = spring<Float>(MotionSettings.damping(0.35f), MotionSettings.stiffness(180f), 0.0005f)
  private val sx = Animatable(1f, 0.0005f)
  private val sy = Animatable(1f, 0.0005f)
  private var lastX = Float.NaN
  private var lastY = 0f
  private var lastT = 0L
  private var settleJob: Job? = null

  val scaleX: Float get() = sx.value
  val scaleY: Float get() = sy.value

  fun onPositionSample(x: Float, y: Float = 0f) {
    if (MotionSettings.isReducedMotion) return
    val t = SystemClock.uptimeMillis()
    if (lastX.isNaN()) {
      lastX = x
      lastY = y
      lastT = t
      return
    }
    val dt = t - lastT
    if (dt <= 0L) return
    if (dt > 100L) {
      lastX = x
      lastY = y
      lastT = t
      return
    }
    onVelocity((x - lastX) / dt, (y - lastY) / dt)
    lastX = x
    lastY = y
    lastT = t
  }

  fun onVelocity(vx: Float, vy: Float) {
    val ax = abs(vx)
    val ay = abs(vy)
    val k = stretchFactor * MotionSettings.style.stretch
    val (tx, ty) =
      if (ax >= ay) (1f + ax * k) to (1f - ax * k * 0.5f)
      else (1f - ay * k * 0.5f) to (1f + ay * k)
    animateTo(tx.coerceIn(minScale, maxScale), ty.coerceIn(minScale, maxScale))
    settleJob?.cancel()
    settleJob = scope.launch {
      delay(settleDelayMs)
      settle()
    }
  }

  fun settle() {
    lastX = Float.NaN
    animateTo(1f, 1f)
  }

  private fun animateTo(x: Float, y: Float) {
    scope.launch { sx.animateTo(x, spec) }
    scope.launch { sy.animateTo(y, spec) }
  }
}

@Composable
fun rememberLiquidStretchState(
  stretchFactor: Float = 0.5f,
  minScale: Float = 0.6f,
  maxScale: Float = 1.4f,
  settleDelayMs: Long = 200L,
): LiquidStretchState {
  val scope = rememberCoroutineScope()
  return remember(scope, stretchFactor, minScale, maxScale, settleDelayMs) {
    LiquidStretchState(scope, stretchFactor, minScale, maxScale, settleDelayMs)
  }
}

fun Modifier.liquidStretch(state: LiquidStretchState): Modifier = graphicsLayer {
  scaleX = state.scaleX
  scaleY = state.scaleY
}

class DampedDragAnimation(
  private val animationScope: CoroutineScope,
  initialValue: Float,
  val valueRange: ClosedRange<Float>,
  private val visibilityThreshold: Float,
  private val initialScale: Float,
  private val pressedScale: Float,
  val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
  val onDragStopped: DampedDragAnimation.() -> Unit,
  val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {
  private val valueSpec = spring(1f, 1000f, visibilityThreshold)
  private val velocitySpec get() = spring(MotionSettings.damping(0.5f), MotionSettings.stiffness(300f), visibilityThreshold * 10f)
  private val pressSpec = spring(1f, 1000f, 0.001f)
  private val scaleXSpec get() = spring(MotionSettings.damping(0.6f), MotionSettings.stiffness(250f), 0.001f)
  private val scaleYSpec get() = spring(MotionSettings.damping(0.7f), MotionSettings.stiffness(250f), 0.001f)

  private val valueAnim = Animatable(initialValue, visibilityThreshold)
  private val velocityAnim = Animatable(0f, 5f)
  private val pressAnim = Animatable(0f, 0.001f)
  private val scaleXAnim = Animatable(initialScale, 0.001f)
  private val scaleYAnim = Animatable(initialScale, 0.001f)
  private val mutex = MutatorMutex()
  private val tracker = VelocityTracker()

  val value: Float get() = valueAnim.value
  val progress: Float get() = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)).coerceIn(0f, 1f)
  val targetValue: Float get() = valueAnim.targetValue
  val pressProgress: Float get() = pressAnim.value
  val scaleX: Float get() = scaleXAnim.value
  val scaleY: Float get() = scaleYAnim.value
  val velocity: Float get() = velocityAnim.value

  val modifier: Modifier = Modifier.pointerInput(Unit) {
    inspectDragGestures(
      onDragStart = { down -> onDragStarted(down.position); press() },
      onDragEnd = { onDragStopped(); release() },
      onDragCancel = { onDragStopped(); release() },
    ) { _, dragAmount -> onDrag(size, dragAmount) }
  }

  fun press() {
    tracker.resetTracking()
    animationScope.launch {
      launch { pressAnim.animateTo(1f, pressSpec) }
      launch { scaleXAnim.animateTo(pressedScale, scaleXSpec) }
      launch { scaleYAnim.animateTo(pressedScale, scaleYSpec) }
    }
  }

  fun release() {
    animationScope.launch {
      awaitFrame()
      if (value != targetValue) {
        val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
        snapshotFlow { valueAnim.value }.filter { abs(it - valueAnim.targetValue) <= threshold }.first()
      }
      launch { pressAnim.animateTo(0f, pressSpec) }
      launch { scaleXAnim.animateTo(initialScale, scaleXSpec) }
      launch { scaleYAnim.animateTo(initialScale, scaleYSpec) }
    }
  }

  fun updateValue(value: Float) {
    val target = value.coerceIn(valueRange)
    animationScope.launch { valueAnim.animateTo(target, valueSpec) { updateVelocity() } }
  }

  fun animateToValue(value: Float) {
    animationScope.launch {
      mutex.mutate {
        press()
        val target = value.coerceIn(valueRange)
        launch { valueAnim.animateTo(target, valueSpec) }
        if (velocity != 0f) launch { velocityAnim.animateTo(0f, velocitySpec) }
        release()
      }
    }
  }

  private fun updateVelocity() {
    tracker.addPosition(System.currentTimeMillis(), Offset(value, 0f))
    val v = tracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    animationScope.launch { velocityAnim.animateTo(v, velocitySpec) }
  }
}
