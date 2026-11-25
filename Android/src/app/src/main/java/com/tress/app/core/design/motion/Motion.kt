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
 * Motion specifications adapted from Artefact
 * (https://github.com/Chethan616/Artefact, MIT OR Apache-2.0, Copyright 2026 Chethan616).
 */

package com.tress.app.core.design.motion

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt

@Immutable
data class MotionStyle(
  val bounce: Float = 0.5f,
  val speed: Float = 1f,
  val press: Float = 1f,
  val wobble: Float = 1f,
  val stretch: Float = 1f,
  val bloom: Float = 1f,
) {
  companion object {
    val Normal = MotionStyle()
    val Calm = MotionStyle(bounce = 0.2f, speed = 1.45f, press = 0.6f, wobble = 0f, stretch = 0.5f, bloom = 0.5f)
    val Bouncy = MotionStyle(bounce = 0.75f, speed = 0.92f, press = 1.3f, wobble = 1.6f, stretch = 1.2f, bloom = 1.2f)
    val Jelly = MotionStyle(bounce = 0.95f, speed = 0.82f, press = 1.6f, wobble = 2.2f, stretch = 1.5f, bloom = 1.5f)
    val Presets = listOf(Calm, Normal, Bouncy, Jelly)
    val Default = Normal
  }
}

@Immutable
data class EaseCurve(val x1: Float, val y1: Float, val x2: Float, val y2: Float) {
  val easing: Easing get() = CubicBezierEasing(x1, y1, x2, y2)

  companion object {
    val Drawer = EaseCurve(0.32f, 0.72f, 0f, 1f)
    val Standard = EaseCurve(0.4f, 0f, 0.2f, 1f)
    val Emphasized = EaseCurve(0.05f, 0.7f, 0.1f, 1f)
    val EaseInOut = EaseCurve(0.42f, 0f, 0.58f, 1f)
    val Linear = EaseCurve(0f, 0f, 1f, 1f)
    val Presets = listOf(Drawer, Standard, Emphasized, EaseInOut, Linear)
  }
}

object MotionSettings {
  private const val PREFS = "gallery_glass"

  var style: MotionStyle by mutableStateOf(MotionStyle.Default)
    private set

  var curve: EaseCurve by mutableStateOf(EaseCurve.Drawer)
    private set

  var duration: Float by mutableStateOf(1f)
    private set

  var reduceMotion: Boolean by mutableStateOf(false)
    private set

  var systemReducedMotion: Boolean by mutableStateOf(false)
    private set

  val isReducedMotion: Boolean
    get() = reduceMotion || systemReducedMotion

  private var cachedCurve: EaseCurve? = null
  private var cachedEasing: Easing = EaseCurve.Drawer.easing

  val easing: Easing
    get() {
      val c = curve
      if (c != cachedCurve) {
        cachedCurve = c
        cachedEasing = c.easing
      }
      return cachedEasing
    }

  fun refreshSystemScale(context: Context) {
    val scale = runCatching {
      Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
    systemReducedMotion = scale <= 0f
  }

  fun updateCurve(context: Context, c: EaseCurve, save: Boolean = true) {
    curve = c
    if (save) {
      context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        .putString("motion_curve", "${c.x1},${c.y1},${c.x2},${c.y2}")
        .apply()
    }
  }

  fun updateDuration(context: Context, d: Float) {
    duration = d.coerceIn(0.3f, 2.5f)
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
      .putFloat("motion_duration", duration)
      .apply()
  }

  fun updateReduceMotion(context: Context, reduced: Boolean) {
    reduceMotion = reduced
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
      .putBoolean("motion_reduced", reduced)
      .apply()
  }

  fun load(context: Context) {
    refreshSystemScale(context)
    val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    p.getString("motion_curve", null)?.split(',')?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == 4 }?.let {
      curve = EaseCurve(it[0], it[1], it[2], it[3])
    }
    duration = p.getFloat("motion_duration", 1f)
    reduceMotion = p.getBoolean("motion_reduced", false)
    val d = MotionStyle.Default
    style = MotionStyle(
      bounce = p.getFloat("motion_bounce", d.bounce),
      speed = p.getFloat("motion_speed", d.speed),
      press = p.getFloat("motion_press", d.press),
      wobble = p.getFloat("motion_wobble", d.wobble),
      stretch = p.getFloat("motion_stretch", d.stretch),
      bloom = p.getFloat("motion_bloom", d.bloom),
    )
  }

  fun update(context: Context, s: MotionStyle) {
    style = s
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
      .putFloat("motion_bounce", s.bounce)
      .putFloat("motion_speed", s.speed)
      .putFloat("motion_press", s.press)
      .putFloat("motion_wobble", s.wobble)
      .putFloat("motion_stretch", s.stretch)
      .putFloat("motion_bloom", s.bloom)
      .apply()
  }

  fun damping(designed: Float): Float =
    if (isReducedMotion) 1f else (designed * (1f + (0.5f - style.bounce) * 1.2f)).coerceIn(0.18f, 0.98f)

  fun stiffness(designed: Float): Float =
    if (isReducedMotion) designed * 2.5f else designed * style.speed.coerceIn(0.4f, 2.5f)
}

object Fluid {
  val Ease: Easing get() = MotionSettings.easing
  val Soft: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

  private fun ms(designed: Int): Int {
    if (MotionSettings.isReducedMotion) return (designed * 0.25f).roundToInt().coerceAtLeast(16)
    return (designed * MotionSettings.duration.coerceIn(0.3f, 3f)).roundToInt()
  }

  fun <T> push(): TweenSpec<T> = tween(ms(220), easing = Ease)
  fun <T> pop(): TweenSpec<T> = tween(ms(190), easing = Ease)
  fun <T> rise(): TweenSpec<T> = tween(ms(220), easing = Ease)
  fun <T> dismiss(): TweenSpec<T> = tween(ms(160), easing = Ease)
  fun <T> sheetLeave(): TweenSpec<T> = tween(ms(150), easing = Leave)
  private val Leave: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.5f)
  fun <T> quick(): TweenSpec<T> = tween(ms(140), easing = Ease)
  fun <T> fade(): TweenSpec<T> = tween(ms(120), easing = Soft)
  fun <T> follow(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 780f)
  fun <T> pressIn(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 3600f)
  fun <T> pressOut(): SpringSpec<T> = spring(dampingRatio = 0.88f, stiffness = 760f)
  const val PressedScale = 0.965f
}

object GlassMotion {
  fun <T> settle(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = Spring.StiffnessMedium)
  private const val Rest = 0.0005f
  private const val ShapeMinDamping = 0.56f
  private const val MorphMinDamping = 0.68f

  fun unfold(): SpringSpec<Float> = spring(MotionSettings.damping(0.76f), MotionSettings.stiffness(540f), Rest)
  fun unfoldLag(): SpringSpec<Float> = spring(MotionSettings.damping(0.70f), MotionSettings.stiffness(460f), Rest)
  fun morph(): SpringSpec<Float> = spring(MotionSettings.damping(0.72f), MotionSettings.stiffness(520f), Rest)
  fun pop(): SpringSpec<Float> = spring(MotionSettings.damping(0.62f), MotionSettings.stiffness(580f), Rest)
  fun fold(): SpringSpec<Float> = spring(1f, MotionSettings.stiffness(1100f), Rest)
  fun shape(): SpringSpec<Float> = spring(maxOf(MotionSettings.damping(0.68f), ShapeMinDamping), MotionSettings.stiffness(280f), Rest)
  fun shapeBack(): SpringSpec<Float> = spring(maxOf(MotionSettings.damping(0.70f), ShapeMinDamping), MotionSettings.stiffness(300f), Rest)
  fun morphOpen(): SpringSpec<Float> = spring(maxOf(MotionSettings.damping(0.74f), MorphMinDamping), MotionSettings.stiffness(420f), Rest)
  fun morphClose(): SpringSpec<Float> = spring(1f, MotionSettings.stiffness(440f), 0.01f)
  fun morphChosen(): SpringSpec<Float> = spring(1f, MotionSettings.stiffness(540f), 0.03f)

  const val MinPressMillis = 16L
}
