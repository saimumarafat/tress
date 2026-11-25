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
 * Glass style and performance tiers adapted from Artefact
 * (https://github.com/Chethan616/Artefact, MIT OR Apache-2.0, Copyright 2026 Chethan616).
 */

package com.google.ai.edge.gallery.core.design.glass

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.google.ai.edge.gallery.core.design.motion.MotionSettings
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight

enum class GlassPerformanceTier {
  FULL,
  BALANCED,
  LITE,
}

@Immutable
data class GlassStyle(
  val blur: Float = 1f,
  val refractionHeight: Float = 1f,
  val refractionAmount: Float = 1f,
  val depthEffect: Boolean = false,
  val chromaticAberration: Boolean = false,
  val saturation: Float = 1.45f,
  val brightness: Float = 0f,
  val tint: Float = 1f,
  val highlight: Float = 1f,
) {
  companion object {
    val Default = GlassStyle()
    val Frosted = GlassStyle(blur = 2.1f, tint = 1.6f, refractionAmount = 0.7f, saturation = 1.25f)
    val Crystal = GlassStyle(
      blur = 0.45f,
      refractionHeight = 1.35f,
      refractionAmount = 1.45f,
      depthEffect = true,
      chromaticAberration = true,
      tint = 0.75f,
      saturation = 1.6f,
    )
    val MenuFrost = GlassStyle(
      blur = 1.45f,
      refractionHeight = 0f,
      refractionAmount = 0f,
      tint = 1f,
      saturation = 1.2f,
      brightness = 0.06f,
    )
    val MenuFrostDark = MenuFrost.copy(brightness = -0.25f)
    val Lite = GlassStyle(blur = 0f, refractionHeight = 0f, refractionAmount = 0f, tint = 1.35f)
    val Presets = listOf(Default, Frosted, Crystal, Lite)
  }
}

enum class WallpaperMode {
  Aurora,
  Clean,
}

enum class BackgroundPreset(val label: String) {
  AMOLED("Obsidian"),
  TWILIGHT("Twilight"),
  OCEAN("Ocean"),
  FOREST("Forest"),
  BERRY("Berry"),
  SAND("Sand"),
}

object GlassSettings {
  private const val PREFS = "gallery_glass"

  var performanceTier: GlassPerformanceTier by mutableStateOf(GlassPerformanceTier.FULL)
    private set

  var wallpaper: WallpaperMode by mutableStateOf(WallpaperMode.Aurora)
    private set

  var backgroundPreset: BackgroundPreset by mutableStateOf(BackgroundPreset.AMOLED)
    private set

  var style: GlassStyle by mutableStateOf(GlassStyle.Default)
  var haptics: Boolean by mutableStateOf(true)
    private set

  var adaptive: Boolean by mutableStateOf(true)
    private set

  val isLiteTier: Boolean
    get() = performanceTier == GlassPerformanceTier.LITE || wallpaper == WallpaperMode.Clean

  val effectiveStyle: GlassStyle
    get() = when (performanceTier) {
      GlassPerformanceTier.FULL -> style
      GlassPerformanceTier.BALANCED -> style.copy(
        blur = style.blur * 0.65f,
        refractionHeight = style.refractionHeight * 0.6f,
        refractionAmount = style.refractionAmount * 0.6f,
        chromaticAberration = false,
      )
      GlassPerformanceTier.LITE -> GlassStyle.Lite
    }

  fun updatePerformanceTier(context: Context, tier: GlassPerformanceTier) {
    performanceTier = tier
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
      .putString("performance_tier", tier.name)
      .apply()
  }

  fun updateWallpaper(context: Context, mode: WallpaperMode) {
    wallpaper = mode
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
      .putString("wallpaper", mode.name)
      .apply()
  }

  fun updateBackgroundPreset(context: Context, preset: BackgroundPreset) {
    backgroundPreset = preset
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
      .putString("background_preset", preset.name)
      .apply()
  }

  fun updateAdaptive(context: Context, on: Boolean) {
    adaptive = on
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
      .putBoolean("adaptive", on)
      .apply()
  }

  fun updateHaptics(context: Context, on: Boolean) {
    haptics = on
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
      .putBoolean("haptics", on)
      .apply()
  }

  fun update(context: Context, s: GlassStyle) {
    style = s
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
      .putFloat("blur", s.blur)
      .putFloat("refractionHeight", s.refractionHeight)
      .putFloat("refractionAmount", s.refractionAmount)
      .putBoolean("depthEffect", s.depthEffect)
      .putBoolean("chromaticAberration", s.chromaticAberration)
      .putFloat("saturation", s.saturation)
      .putFloat("brightness", s.brightness)
      .putFloat("tint", s.tint)
      .putFloat("highlight", s.highlight)
      .apply()
  }

  fun load(context: Context) {
    val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val d = GlassStyle.Default
    style = GlassStyle(
      blur = p.getFloat("blur", d.blur),
      refractionHeight = p.getFloat("refractionHeight", d.refractionHeight),
      refractionAmount = p.getFloat("refractionAmount", d.refractionAmount),
      depthEffect = p.getBoolean("depthEffect", d.depthEffect),
      chromaticAberration = p.getBoolean("chromaticAberration", d.chromaticAberration),
      saturation = p.getFloat("saturation", d.saturation),
      brightness = p.getFloat("brightness", d.brightness),
      tint = p.getFloat("tint", d.tint),
      highlight = p.getFloat("highlight", d.highlight),
    )
    haptics = p.getBoolean("haptics", true)
    adaptive = p.getBoolean("adaptive", true)
    performanceTier = runCatching {
      GlassPerformanceTier.valueOf(p.getString("performance_tier", null) ?: GlassPerformanceTier.FULL.name)
    }.getOrDefault(GlassPerformanceTier.FULL)
    wallpaper = runCatching {
      WallpaperMode.valueOf(p.getString("wallpaper", null) ?: WallpaperMode.Aurora.name)
    }.getOrDefault(WallpaperMode.Aurora)
    backgroundPreset = runCatching {
      BackgroundPreset.valueOf(p.getString("background_preset", null) ?: BackgroundPreset.AMOLED.name)
    }.getOrDefault(BackgroundPreset.AMOLED)
    MotionSettings.load(context)
  }
}

fun BackdropEffectScope.glassEffects(
  style: GlassStyle,
  blurPx: Float,
  refractionHeightPx: Float,
  refractionAmountPx: Float,
  depthEffect: Boolean = false,
) {
  if (style.saturation == 1.45f && style.brightness == 0f) vibrancy()
  else colorControls(brightness = style.brightness, saturation = style.saturation)
  blur(blurPx * style.blur)
  lens(
    refractionHeightPx * style.refractionHeight,
    refractionAmountPx * style.refractionAmount,
    depthEffect = depthEffect || style.depthEffect,
    chromaticAberration = style.chromaticAberration,
  )
}

fun Color.glassTint(style: GlassStyle): Color =
  if (style.tint == 1f) this else copy(alpha = (alpha * style.tint).coerceIn(0f, 1f))

fun glassHighlight(style: GlassStyle, base: Highlight = Highlight.Default): Highlight? =
  if (style.highlight <= 0f) null else base.copy(alpha = style.highlight.coerceIn(0f, 1f))
