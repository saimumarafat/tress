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
 * Backdrop stage, layer composition, Aurora procedural wallpaper, and overlay portal adapted from
 * Artefact (https://github.com/Chethan616/Artefact, MIT OR Apache-2.0, Copyright 2026 Chethan616).
 */

package com.tress.app.core.design.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize
import com.tress.app.core.design.motion.Fluid
import com.tress.app.core.design.motion.MotionSettings
import com.tress.app.core.design.theme.glassColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

val LocalBackdrop = staticCompositionLocalOf<Backdrop> { emptyBackdrop() }

@Composable
fun GlassLayer(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
  above: @Composable BoxScope.() -> Unit,
) {
  val below = LocalBackdrop.current
  val layer = rememberLayerBackdrop()
  val combined = rememberCombinedBackdrop(below, layer)
  Box(modifier) {
    Box(Modifier.matchParentSize().layerBackdrop(layer), content = content)
    CompositionLocalProvider(LocalBackdrop provides combined) {
      Box(Modifier.matchParentSize(), content = above)
    }
  }
}

data class AuroraMood(val blobs: List<Offset>, val radius: Float = 0.68f)

object AuroraMoods {
  val Home = AuroraMood(
    listOf(Offset(0.05f, 0.28f), Offset(0.92f, 0.42f), Offset(0.88f, 0.12f), Offset(0.32f, 1.02f), Offset(0.42f, 0.02f)),
    0.70f,
  )
  val Chat = AuroraMood(
    listOf(Offset(0.08f, 0.34f), Offset(0.90f, 0.44f), Offset(0.84f, 0.14f), Offset(0.36f, 0.96f), Offset(0.36f, 0.04f)),
    0.70f,
  )
  val Models = AuroraMood(
    listOf(Offset(0.12f, 0.24f), Offset(0.90f, 0.72f), Offset(0.82f, 0.04f), Offset(0.22f, 1.04f), Offset(0.58f, -0.08f)),
    0.66f,
  )
  val Benchmark = AuroraMood(
    listOf(Offset(0.88f, 0.22f), Offset(0.12f, 0.64f), Offset(0.04f, 0.06f), Offset(0.78f, 1.02f), Offset(0.45f, -0.05f)),
    0.66f,
  )
  val Skills = AuroraMood(
    listOf(Offset(0.10f, 0.32f), Offset(0.88f, 0.42f), Offset(0.86f, 0.12f), Offset(0.34f, 0.98f), Offset(0.38f, 0.02f)),
    0.70f,
  )
  val Settings = AuroraMood(
    listOf(Offset(0.18f, 0.14f), Offset(0.92f, 0.82f), Offset(0.98f, 0.32f), Offset(0.12f, 0.98f), Offset(0.62f, 0.02f)),
    0.58f,
  )
}

private val OffsetPairConverter = TwoWayConverter<Pair<Offset, Offset>, AnimationVector4D>(
  { (a, b) -> AnimationVector4D(a.x, a.y, b.x, b.y) },
  { v -> Offset(v.v1, v.v2) to Offset(v.v3, v.v4) },
)

@Composable
fun Aurora(mood: AuroraMood, modifier: Modifier = Modifier) {
  val colors = glassColors
  if (colors.clean || GlassSettings.isLiteTier) {
    Spacer(modifier.fillMaxSize().background(colors.desk))
    return
  }
  val cache = remember { arrayOfNulls<ImageBitmap>(2) } // [0] = previous, [1] = current
  val cachedKey = remember { arrayOfNulls<Any>(2) }
  val fade = remember { Animatable(1f) }
  val scope = rememberCoroutineScope()

  Box(
    modifier.fillMaxSize().drawWithCache {
      val w = (size.width / 8f).toInt().coerceAtLeast(1)
      val h = (size.height / 8f).toInt().coerceAtLeast(1)
      val key = Triple(mood, colors, IntSize(w, h))
      if (cachedKey[1] != key) {
        if (cachedKey[1] != null) {
          cache[0] = cache[1]
          cachedKey[0] = cachedKey[1]
        }
        val next = ImageBitmap(w, h)
        val small = Size(w.toFloat(), h.toFloat())
        CanvasDrawScope().draw(this, layoutDirection, Canvas(next), small) {
          drawRect(Brush.verticalGradient(colors.wall))
          val r = small.maxDimension
          colors.blooms.forEachIndexed { i, (c, strength) ->
            val p = mood.blobs[i % mood.blobs.size]
            val rad = mood.radius
            if (r * rad > 1f) {
              val center = Offset(p.x * small.width, p.y * small.height)
              drawCircle(
                Brush.radialGradient(
                  listOf(c.copy(alpha = strength), c.copy(alpha = strength * 0.55f), c.copy(alpha = strength * 0.18f), Color.Transparent),
                  center,
                  r * rad,
                ),
                radius = r * rad,
                center = center,
              )
            }
          }
        }
        cache[1] = next
        cachedKey[1] = key
        if (cache[0] != null && !MotionSettings.isReducedMotion) {
          scope.launch {
            fade.snapTo(0f)
            fade.animateTo(1f, tween(340, easing = Fluid.Ease))
          }
        } else {
          scope.launch { fade.snapTo(1f) }
        }
      }
      val dst = IntSize(size.width.toInt(), size.height.toInt())
      onDrawBehind {
        val alpha = fade.value.coerceIn(0f, 1f)
        val prev = cache[0]
        val curr = cache[1]
        if (prev != null && alpha < 1f) {
          drawImage(prev, dstSize = dst, alpha = 1f - alpha, filterQuality = FilterQuality.Low)
        }
        if (curr != null) {
          drawImage(curr, dstSize = dst, alpha = if (prev != null) alpha else 1f, filterQuality = FilterQuality.Low)
        }
      }
    },
  )
}

@Stable
class GlassOverlayHost {
  internal val entries = mutableStateListOf<OverlayEntry>()
  internal var originInWindow by mutableStateOf(Offset.Zero)
  var size by mutableStateOf(IntSize.Zero)
    internal set
  var stillHolds by mutableStateOf(0)

  internal fun register(entry: OverlayEntry) {
    entries.removeAll { it.key === entry.key }
    entries.add(entry)
  }

  internal fun unregister(key: Any) {
    val e = entries.firstOrNull { it.key === key } ?: return
    if (e.animatesOut) e.leaving = true else entries.remove(e)
  }

  internal fun remove(key: Any) {
    entries.removeAll { it.key === key }
  }

  fun windowToLocal(point: Offset): Offset = point - originInWindow
}

internal class OverlayEntry(val key: Any, val animatesOut: Boolean, val content: @Composable BoxScope.() -> Unit) {
  var leaving by mutableStateOf(false)
}

@Stable
class OverlayPresence internal constructor(private val host: GlassOverlayHost, private val entry: OverlayEntry) {
  val leaving: Boolean get() = entry.leaving
  fun done() = host.remove(entry.key)
}

val LocalOverlayPresence = staticCompositionLocalOf<OverlayPresence?> { null }
val LocalGlassOverlayHost = staticCompositionLocalOf<GlassOverlayHost?> { null }

@Composable
fun GlassOverlayLayer(host: GlassOverlayHost, modifier: Modifier = Modifier) {
  Box(
    modifier.fillMaxSize().onGloballyPositioned {
      host.originInWindow = it.positionInWindow()
      host.size = it.size
    },
  ) {
    host.entries.forEach { entry ->
      key(entry.key) {
        val presence = remember(entry) { OverlayPresence(host, entry) }
        CompositionLocalProvider(LocalOverlayPresence provides presence) { entry.content(this) }
        if (entry.leaving) {
          Box(
            Modifier.matchParentSize().pointerInput(Unit) {
              awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
            },
          )
        }
      }
    }
  }
}

@Composable
fun GlassOverlay(animatesOut: Boolean = false, content: @Composable BoxScope.() -> Unit) {
  val host = LocalGlassOverlayHost.current ?: return
  val latest by rememberUpdatedState(content)
  val key = remember { Any() }
  DisposableEffect(host, key) {
    host.register(OverlayEntry(key, animatesOut) { latest() })
    onDispose { host.unregister(key) }
  }
}

@Composable
fun rememberOverlayAppear(
  enter: AnimationSpec<Float> = Fluid.rise(),
  exit: AnimationSpec<Float> = Fluid.dismiss(),
): Animatable<Float, AnimationVector1D> {
  val presence = LocalOverlayPresence.current
  val a = remember { Animatable(0f) }
  val leaving = presence?.leaving == true
  LaunchedEffect(leaving) {
    if (!leaving) {
      if (MotionSettings.isReducedMotion) a.snapTo(1f) else a.animateTo(1f, enter)
    } else {
      if (MotionSettings.isReducedMotion) a.snapTo(0f) else a.animateTo(0f, exit)
      presence?.done()
    }
  }
  return a
}

@Composable
fun GlassStage(
  wallpaper: @Composable BoxScope.() -> Unit,
  screens: @Composable BoxScope.() -> Unit,
  chrome: @Composable BoxScope.() -> Unit,
) {
  val provided = LocalGlassOverlayHost.current
  val host = provided ?: remember { GlassOverlayHost() }
  val wall = rememberLayerBackdrop()
  val screensLayer = rememberLayerBackdrop()
  val chromeLayer = rememberLayerBackdrop()
  val underChrome = rememberCombinedBackdrop(wall, screensLayer)
  val underOverlay = rememberCombinedBackdrop(underChrome, chromeLayer)
  CompositionLocalProvider(LocalGlassOverlayHost provides host) {
    Box(Modifier.fillMaxSize()) {
      Box(
        Modifier.fillMaxSize().graphicsLayer {
          compositingStrategy =
            if (host.entries.isEmpty() && host.stillHolds == 0) CompositingStrategy.Auto
            else CompositingStrategy.Offscreen
        },
      ) {
        Box(Modifier.fillMaxSize().layerBackdrop(wall), content = wallpaper)
        CompositionLocalProvider(LocalBackdrop provides wall) {
          Box(Modifier.fillMaxSize().layerBackdrop(screensLayer), content = screens)
        }
      }
      Box(
        Modifier.fillMaxSize().graphicsLayer {
          compositingStrategy =
            if (host.entries.isEmpty()) CompositingStrategy.Auto else CompositingStrategy.Offscreen
        },
      ) {
        CompositionLocalProvider(LocalBackdrop provides underChrome) {
          Box(Modifier.fillMaxSize().layerBackdrop(chromeLayer), content = chrome)
        }
      }
      CompositionLocalProvider(LocalBackdrop provides underOverlay) { GlassOverlayLayer(host) }
    }
  }
}
