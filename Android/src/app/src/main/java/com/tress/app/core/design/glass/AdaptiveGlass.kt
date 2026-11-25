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
 * Adaptive backdrop luminance sampling adapted from Artefact
 * (https://github.com/Chethan616/Artefact) and AndroidLiquidGlass (Kyant0, Apache-2.0).
 */

package com.tress.app.core.design.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.util.lerp
import com.tress.app.core.design.theme.glassColors
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import kotlin.math.abs
import kotlin.math.sign

@Stable
class GlassLuminance internal constructor(initial: Float) {
  private val anim = Animatable(initial)

  val value: Float get() = anim.value
  var onLight by mutableStateOf(initial > 0.5f)
    private set
  var measured by mutableStateOf(true)
    private set

  // Keep draw-time backdrop pipeline free of per-frame offscreen graphicsLayer readbacks.
  internal val recorder: DrawScope.(DrawScope.() -> Unit) -> Unit = { drawBackdrop ->
    drawBackdrop()
  }

  internal suspend fun updateFrom(lum: Float) {
    onLight = when {
      lum > 0.56f -> true
      lum < 0.44f -> false
      else -> onLight
    }
    if (abs(lum - anim.targetValue) > 0.08f) {
      anim.animateTo(lum, tween(380))
    }
  }
}

@Composable
fun rememberGlassLuminance(enabled: Boolean = GlassSettings.adaptive): GlassLuminance? {
  if (!enabled || glassColors.clean || GlassSettings.isLiteTier) return null
  val dark = glassColors.dark
  val screen = LocalScreenInk.current
  val initial = screen?.frame?.average() ?: if (dark) 0.14f else 0.86f
  val state = remember { GlassLuminance(initial) }
  if (screen != null) {
    LaunchedEffect(state, screen) {
      snapshotFlow { screen.frame?.average() }.collect { avg ->
        if (avg != null) {
          state.updateFrom(avg)
        }
      }
    }
  }
  return state
}

val LocalMenuInk = compositionLocalOf { Color.Unspecified }

@Composable
fun adaptiveInk(luminance: GlassLuminance?, fallback: Color): Color {
  if (luminance == null) return fallback
  val target = if (luminance.onLight) InkOnLight else Color.White
  val ink by animateColorAsState(target, tween(220), label = "adaptiveGlassInk")
  return ink
}

private fun signed(luminance: Float): Float =
  (luminance * 2f - 1f).let { sign(it) * it * it }

fun BackdropEffectScope.adaptiveGlassEffects(
  style: GlassStyle,
  luminance: Float,
  blurPx: Float,
  refractionHeightPx: Float,
  refractionAmountPx: Float,
) {
  val l = signed(luminance.coerceIn(0f, 1f))
  colorControls(
    brightness = (if (l > 0f) lerp(0.08f, 0.38f, l) else lerp(0.08f, -0.22f, -l)) + style.brightness,
    contrast = if (l > 0f) lerp(1f, 0.30f, l) else 1f,
    saturation = style.saturation,
  )
  blur(blurPx * style.blur * (if (l > 0f) lerp(1f, 1.45f, l) else lerp(1f, 0.65f, -l)))
  lens(
    refractionHeightPx * style.refractionHeight,
    refractionAmountPx * style.refractionAmount,
    depthEffect = style.depthEffect,
    chromaticAberration = style.chromaticAberration,
  )
}

val GlassStyle.adaptiveBase: GlassStyle
  get() = copy(blur = 1f, saturation = 1.45f, brightness = 0f, tint = 1f)

fun adaptiveWash(luminance: Float): Color {
  val l = signed(luminance.coerceIn(0f, 1f))
  return if (l >= 0f) Color.White.copy(alpha = lerp(0.10f, 0.28f, l))
  else Color.Black.copy(alpha = lerp(0.10f, 0.32f, -l))
}
