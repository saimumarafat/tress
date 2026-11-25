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
 * Per-control adaptive foreground contrast adapted from Artefact
 * (https://github.com/Chethan616/Artefact, MIT OR Apache-2.0, Copyright 2026 Chethan616).
 */

package com.tress.app.core.design.glass

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.PixelCopy
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.tress.app.core.design.theme.glassColors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

@Stable
class ScreenInkSampler internal constructor() {
  internal var frame by mutableStateOf<InkFrame?>(null)
  internal var users by mutableIntStateOf(0)

  internal suspend fun run(window: Window, view: View) {
    val dirty = AtomicBoolean(true)
    val lastDrawMs = AtomicLong(0L)
    val listener = ViewTreeObserver.OnDrawListener {
      dirty.set(true)
      lastDrawMs.set(SystemClock.uptimeMillis())
    }
    view.viewTreeObserver.addOnDrawListener(listener)
    val handler = Handler(Looper.getMainLooper())
    var bitmap: Bitmap? = null
    var firstSample = true
    try {
      while (true) {
        if (users == 0) {
          snapshotFlow { users > 0 }.first { it }
          dirty.set(true)
        }
        val elapsedSinceDraw = SystemClock.uptimeMillis() - lastDrawMs.get()
        // Avoid PixelCopy GPU-CPU stalls while active scrolling or animations are running.
        if (!firstSample && elapsedSinceDraw < SETTLE_IDLE_MS) {
          delay(SETTLE_IDLE_MS - elapsedSinceDraw)
          continue
        }
        val decor = window.decorView
        val w = decor.width
        val h = decor.height
        if (w > 0 && h > 0 && dirty.getAndSet(false)) {
          firstSample = false
          val bw = (w / SCALE_DOWN).coerceAtLeast(1)
          val bh = (h / SCALE_DOWN).coerceAtLeast(1)
          val target = bitmap?.takeIf { it.width == bw && it.height == bh && !it.isRecycled }
            ?: Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888).also { bitmap = it }
          val ok = runCatching {
            suspendCancellableCoroutine { cont ->
              PixelCopy.request(window, target, { result -> if (cont.isActive) cont.resume(result) }, handler)
            } == PixelCopy.SUCCESS
          }.getOrDefault(false)
          if (ok) {
            val px = IntArray(bw * bh)
            target.getPixels(px, 0, bw, 0, 0, bw, bh)
            frame = InkFrame(px, bw, bh, bw / w.toFloat(), bh / h.toFloat())
          }
        }
        delay(SAMPLE_INTERVAL_MS)
      }
    } finally {
      if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnDrawListener(listener)
      bitmap?.recycle()
    }
  }

  private companion object {
    const val SCALE_DOWN = 8
    const val SETTLE_IDLE_MS = 160L
    const val SAMPLE_INTERVAL_MS = 480L
  }
}

internal class InkFrame(
  private val pixels: IntArray,
  private val width: Int,
  private val height: Int,
  private val scaleX: Float,
  private val scaleY: Float,
) {
  private fun luminanceAt(x: Float, y: Float): Double {
    val px = (x * scaleX).toInt().coerceIn(0, width - 1)
    val py = (y * scaleY).toInt().coerceIn(0, height - 1)
    val c = pixels[py * width + px]
    return (0.299 * ((c shr 16) and 0xFF) + 0.587 * ((c shr 8) and 0xFF) + 0.114 * (c and 0xFF)) / 255.0
  }

  fun outsideLuminance(box: Rect, gap: Float): Float {
    val cx = box.center.x
    val cy = box.center.y
    val l = box.left - gap
    val t = box.top - gap
    val r = box.right + gap
    val b = box.bottom + gap
    val sum = luminanceAt(l, cy) + luminanceAt(r, cy) + luminanceAt(cx, t) + luminanceAt(cx, b) +
      luminanceAt(l, t) + luminanceAt(r, t) + luminanceAt(l, b) + luminanceAt(r, b)
    return (sum / 8.0).toFloat()
  }

  fun average(): Float {
    var sum = 0.0
    var n = 0
    var i = 0
    while (i < pixels.size) {
      val c = pixels[i]
      sum += (0.299 * ((c shr 16) and 0xFF) + 0.587 * ((c shr 8) and 0xFF) + 0.114 * (c and 0xFF)) / 255.0
      n++
      i += 37
    }
    return if (n == 0) 0.5f else (sum / n).toFloat()
  }

  fun cornerLuminance(box: Rect, inset: Float): Float {
    val d = inset.coerceAtMost(minOf(box.width, box.height) * 0.14f)
    val corners = luminanceAt(box.left + d, box.top + d) + luminanceAt(box.right - d, box.top + d) +
      luminanceAt(box.left + d, box.bottom - d) + luminanceAt(box.right - d, box.bottom - d)
    val cy = box.center.y
    val sides = luminanceAt(box.left - d * 0.6f, cy) + luminanceAt(box.right + d * 0.6f, cy)
    return ((corners + 2 * sides) / 8.0).toFloat()
  }
}

val LocalScreenInk = staticCompositionLocalOf<ScreenInkSampler?> { null }

@Composable
fun ProvideScreenInk(enabled: Boolean = true, content: @Composable () -> Unit) {
  val above = LocalScreenInk.current
  val view = LocalView.current
  val window = remember(view) { view.context.findActivity()?.window }
  val sampler = remember { ScreenInkSampler() }
  val runs = enabled && !glassColors.clean && !GlassSettings.isLiteTier && above == null && window != null
  if (runs && window != null) {
    LaunchedEffect(window, view) { sampler.run(window, view) }
  }
  CompositionLocalProvider(LocalScreenInk provides (above ?: if (runs) sampler else null), content = content)
}

@Stable
class SurroundLuminance internal constructor(initial: Float) {
  internal val anim = Animatable(initial)
  internal val box = arrayOfNulls<Rect>(1)
  val value: Float get() = anim.value
  val modifier: Modifier = Modifier.onGloballyPositioned { box[0] = it.boundsInWindow() }
}

@Composable
fun rememberSurroundLuminance(): SurroundLuminance? {
  val sampler = LocalScreenInk.current
  if (!GlassSettings.adaptive || sampler == null) return null
  val dark = glassColors.dark
  val state = remember { SurroundLuminance(sampler.frame?.average() ?: if (dark) 0.14f else 0.86f) }
  val gap = with(LocalDensity.current) { 7.dp.toPx() }
  val overlays = LocalGlassOverlayHost.current
  DisposableEffect(sampler) {
    sampler.users++
    onDispose { sampler.users-- }
  }
  LaunchedEffect(sampler) {
    var first = true
    snapshotFlow { sampler.frame }.collect { frame ->
      val b = state.box[0]
      if (frame == null || b == null || b.width <= 0f || b.height <= 0f) return@collect
      if (!first && overlays != null && overlays.entries.isNotEmpty()) return@collect
      val lum = frame.outsideLuminance(b, gap)
      if (first) {
        state.anim.snapTo(lum)
        first = false
      } else if (abs(lum - state.anim.targetValue) > 0.1f) {
        launch { state.anim.animateTo(lum, tween(360)) }
      }
    }
  }
  return state
}

class InkProbe(val modifier: Modifier, val color: Color) {
  val soft: Color get() = color.copy(alpha = 0.62f)
}

val InkOnLight = Color(0xFF131D2E)

@Composable
fun rememberAdaptiveInk(fallback: Color): InkProbe {
  val sampler = LocalScreenInk.current
  val fallbackOnLight = fallback.luminance() < 0.5f
  var onLight by remember { mutableStateOf(fallbackOnLight) }
  var measured by remember { mutableStateOf(false) }
  val box = remember { arrayOfNulls<Rect>(1) }
  val currentFallback by rememberUpdatedState(fallbackOnLight)
  val inset = with(LocalDensity.current) { 5.dp.toPx() }
  val overlays = LocalGlassOverlayHost.current
  if (sampler != null) {
    DisposableEffect(sampler) {
      sampler.users++
      onDispose { sampler.users-- }
    }
    LaunchedEffect(sampler) {
      snapshotFlow { sampler.frame }.collect { frame ->
        val b = box[0]
        if (frame == null || b == null || b.width <= 0f || b.height <= 0f) return@collect
        if (measured && overlays != null && overlays.entries.isNotEmpty()) return@collect
        val lum = frame.cornerLuminance(b, inset)
        onLight = when {
          !measured -> lum > 0.60f
          lum > 0.62f -> true
          lum < 0.58f -> false
          else -> onLight
        }
        measured = true
      }
    }
  }
  val useLight = if (sampler != null && measured) onLight else currentFallback
  val target = when {
    useLight == fallbackOnLight -> fallback
    useLight -> InkOnLight
    else -> Color.White
  }
  val color by animateColorAsState(target, tween(200), label = "adaptiveInk")
  val modifier = remember { Modifier.onGloballyPositioned { box[0] = it.boundsInWindow() } }
  return InkProbe(modifier, color)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}

@Composable
fun rememberGlassInk(fallback: Color): InkProbe =
  if (glassColors.clean || GlassSettings.isLiteTier) InkProbe(Modifier, fallback)
  else if (LocalScreenInk.current != null) rememberAdaptiveInk(fallback)
  else remember(fallback) { InkProbe(Modifier, fallback) }
