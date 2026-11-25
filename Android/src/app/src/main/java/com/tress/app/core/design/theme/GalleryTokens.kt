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
 * Color system and glass design tokens adapted from Artefact
 * (https://github.com/Chethan616/Artefact, MIT OR Apache-2.0, Copyright 2026 Chethan616).
 */

package com.tress.app.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import com.tress.app.core.design.glass.BackgroundPreset
import com.tress.app.core.design.glass.GlassSettings
import com.tress.app.proto.Theme

/** Restrained shadcn-inspired accent hues paired with neutral zinc surfaces. */
object Candy {
  val Sky = Color(0xFF0A84FF)
  val SkySoft = Color(0xFF60A5FA)
  val Tomato = Color(0xFFDC2626)
  val Tangerine = Color(0xFFD97706)
  val Lemon = Color(0xFFCA8A04)
  val Lime = Color(0xFF16A34A)
  val Mint = Color(0xFF0D9488)
  val Grape = Color(0xFF4F46E5)
  val Bubblegum = Color(0xFFDB2777)
  val InkBlue = Color(0xFF18181B)
  val Chip = Color(0xFFF4F4F5)

  val All = listOf(InkBlue, Sky, Mint, Tangerine, Lime, Tomato)
}

@Immutable
data class CardTone(val top: Color, val bottom: Color, val ink: Color, val accent: Color)

enum class Tone { Sky, Sage, Paper, Lavender, Butter, Orchid, Blush, Peach }

@Immutable
data class GalleryGlassColors(
  val dark: Boolean,
  val wall: List<Color>,
  val blooms: List<Pair<Color, Float>>,
  val hero: Color,
  val heroGlow: Color,
  val ink: Color,
  val inkSoft: Color,
  val inkFaint: Color,
  val line: Color,
  val glass: Color,
  val rim: Color,
  val frost: Color,
  val desk: Color,
  val capsule: Color,
  val tones: Map<Tone, CardTone>,
  val clean: Boolean = false,
) {
  val canvas: Color get() = wall[wall.size / 2]
  val solidPanel: Color get() = frost.copy(alpha = 1f)
  fun tone(t: Tone): CardTone = tones.getValue(t)
}

private val InkLight = Color(0xFF09090B)
private val InkDark = Color(0xFFFAFAFA)

// Professional shadcn/ui zinc & slate surface scale with zero noisy rainbow fills.
private val LightTones = mapOf(
  Tone.Sky to CardTone(Color(0xFFF8FAFC), Color(0xFFF1F5F9), Color(0xFF09090B), Candy.Sky),
  Tone.Sage to CardTone(Color(0xFFF4F4F5), Color(0xFFFAFAFA), Color(0xFF09090B), Color(0xFF52525B)),
  Tone.Paper to CardTone(Color(0xFFFFFFFF), Color(0xFFFAFAFA), InkLight, Color(0xFF18181B)),
  Tone.Lavender to CardTone(Color(0xFFF8FAFC), Color(0xFFF4F4F5), Color(0xFF09090B), Color(0xFF3F3F46)),
  Tone.Butter to CardTone(Color(0xFFFAFAFA), Color(0xFFF4F4F5), Color(0xFF09090B), Color(0xFF52525B)),
  Tone.Orchid to CardTone(Color(0xFFF4F4F5), Color(0xFFE4E4E7).copy(alpha = 0.55f), Color(0xFF09090B), Color(0xFF3F3F46)),
  Tone.Blush to CardTone(Color(0xFFFAFAFA), Color(0xFFF1F5F9), Color(0xFF09090B), Color(0xFF3F3F46)),
  Tone.Peach to CardTone(Color(0xFFFFFFFF), Color(0xFFF4F4F5), Color(0xFF09090B), Color(0xFF52525B)),
)

private val DarkTones = mapOf(
  Tone.Sky to CardTone(Color(0xFF18181B), Color(0xFF121215), InkDark, Color(0xFF60A5FA)),
  Tone.Sage to CardTone(Color(0xFF18181B), Color(0xFF141417), InkDark, Color(0xFFA1A1AA)),
  Tone.Paper to CardTone(Color(0xFF141417), Color(0xFF111113), InkDark, Color(0xFFE4E4E7)),
  Tone.Lavender to CardTone(Color(0xFF1C1C20), Color(0xFF141417), InkDark, Color(0xFFA1A1AA)),
  Tone.Butter to CardTone(Color(0xFF18181B), Color(0xFF121215), InkDark, Color(0xFFA1A1AA)),
  Tone.Orchid to CardTone(Color(0xFF1C1C20), Color(0xFF161619), InkDark, Color(0xFFA1A1AA)),
  Tone.Blush to CardTone(Color(0xFF18181B), Color(0xFF131316), InkDark, Color(0xFFA1A1AA)),
  Tone.Peach to CardTone(Color(0xFF1B1B1F), Color(0xFF141417), InkDark, Color(0xFFA1A1AA)),
)

val LightGlassColors = GalleryGlassColors(
  dark = false,
  wall = listOf(Color(0xFFFAFAFA), Color(0xFFFFFFFF), Color(0xFFF4F4F5)),
  blooms = listOf(
    Color(0xFFE4E4E7) to 0.24f,
    Color(0xFFE2E8F0) to 0.20f,
    Color(0xFFF4F4F5) to 0.26f,
    Color(0xFFCBD5E1) to 0.14f,
    Color(0xFFFFFFFF) to 0.30f,
  ),
  hero = InkLight,
  heroGlow = Color.White.copy(alpha = 0.4f),
  ink = InkLight,
  inkSoft = Color(0xFF71717A),
  inkFaint = Color(0xFFA1A1AA),
  line = Color(0xFFE4E4E7),
  glass = Color.White.copy(alpha = 0.78f),
  rim = Color(0xFFE4E4E7),
  frost = Color(0xFFFFFFFF).copy(alpha = 0.96f),
  desk = Color(0xFFFAFAFA),
  capsule = Color(0xFFFFFFFF).copy(alpha = 0.84f),
  tones = LightTones,
)

val DarkGlassColors = GalleryGlassColors(
  dark = true,
  wall = listOf(Color(0xFF09090B), Color(0xFF0C0C0F), Color(0xFF09090B)),
  blooms = listOf(
    Color(0xFF27272A) to 0.22f,
    Color(0xFF1E293B) to 0.14f,
    Color(0xFF18181B) to 0.20f,
    Color(0xFF27272A) to 0.12f,
    Color(0xFF3F3F46) to 0.08f,
  ),
  hero = InkDark,
  heroGlow = Color.Black.copy(alpha = 0.6f),
  ink = InkDark,
  inkSoft = Color(0xFFA1A1AA),
  inkFaint = Color(0xFF71717A),
  line = Color(0xFF27272A),
  glass = Color(0xFF18181B).copy(alpha = 0.82f),
  rim = Color(0xFF27272A),
  frost = Color(0xFF111113).copy(alpha = 0.96f),
  desk = Color(0xFF09090B),
  capsule = Color(0xFF18181B).copy(alpha = 0.86f),
  tones = DarkTones,
)

val GalleryLightSkyScheme: ColorScheme = lightColorScheme(
  primary = Candy.Sky,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFF4F4F5),
  onPrimaryContainer = Color(0xFF09090B),
  secondary = Color(0xFF52525B),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFF4F4F5),
  onSecondaryContainer = Color(0xFF18181B),
  tertiary = Color(0xFF3F3F46),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFF4F4F5),
  onTertiaryContainer = Color(0xFF18181B),
  error = Candy.Tomato,
  onError = Color.White,
  errorContainer = Color(0xFFFEE2E2),
  onErrorContainer = Color(0xFF7F1D1D),
  background = Color(0xFFFAFAFA),
  onBackground = InkLight,
  surface = Color(0xFFFFFFFF),
  onSurface = InkLight,
  surfaceVariant = Color(0xFFF4F4F5),
  onSurfaceVariant = Color(0xFF71717A),
  outline = Color(0xFFD4D4D8),
  outlineVariant = Color(0xFFE4E4E7),
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFFAFAFA),
  surfaceContainer = Color(0xFFF4F4F5),
  surfaceContainerHigh = Color(0xFFECECEE),
  surfaceContainerHighest = Color(0xFFE4E4E7),
)

val GalleryDarkSkyScheme: ColorScheme = darkColorScheme(
  primary = Color(0xFF47A1FF),
  onPrimary = Color(0xFF09090B),
  primaryContainer = Color(0xFF27272A),
  onPrimaryContainer = Color(0xFFFAFAFA),
  secondary = Color(0xFFA1A1AA),
  onSecondary = Color(0xFF09090B),
  secondaryContainer = Color(0xFF27272A),
  onSecondaryContainer = Color(0xFFFAFAFA),
  tertiary = Color(0xFFD4D4D8),
  onTertiary = Color(0xFF09090B),
  tertiaryContainer = Color(0xFF27272A),
  onTertiaryContainer = Color(0xFFFAFAFA),
  error = Color(0xFFEF4444),
  onError = Color(0xFF450A0A),
  errorContainer = Color(0xFF7F1D1D),
  onErrorContainer = Color(0xFFFEE2E2),
  background = Color(0xFF09090B),
  onBackground = InkDark,
  surface = Color(0xFF09090B),
  onSurface = InkDark,
  surfaceVariant = Color(0xFF18181B),
  onSurfaceVariant = Color(0xFFA1A1AA),
  outline = Color(0xFF3F3F46),
  outlineVariant = Color(0xFF27272A),
  surfaceContainerLowest = Color(0xFF09090B),
  surfaceContainerLow = Color(0xFF111113),
  surfaceContainer = Color(0xFF18181B),
  surfaceContainerHigh = Color(0xFF1E1E22),
  surfaceContainerHighest = Color(0xFF27272A),
)

val LocalGalleryGlassColors = staticCompositionLocalOf { LightGlassColors }

val glassColors: GalleryGlassColors
  @Composable get() = LocalGalleryGlassColors.current

fun TextStyle.glowing(c: GalleryGlassColors): TextStyle =
  copy(shadow = Shadow(c.heroGlow, Offset(0f, 2f), 18f))

@Composable
fun isThemeDark(theme: Theme): Boolean =
  when (theme) {
    Theme.THEME_LIGHT -> false
    Theme.THEME_DARK -> true
    else -> isSystemInDarkTheme()
  }

data class BackgroundPalette(
  val wall: List<Color>,
  val blooms: List<Pair<Color, Float>>,
  val desk: Color,
  val capsule: Color,
)

fun BackgroundPreset.palette(dark: Boolean): BackgroundPalette =
  when (this) {
    BackgroundPreset.AMOLED ->
      if (dark) {
        BackgroundPalette(
          wall = listOf(Color(0xFF09090B), Color(0xFF0C0C0F), Color(0xFF09090B)),
          blooms = listOf(
            Color(0xFF27272A) to 0.20f,
            Color(0xFF1E293B) to 0.12f,
            Color(0xFF18181B) to 0.18f,
            Color(0xFF27272A) to 0.12f,
            Color(0xFF3F3F46) to 0.08f,
          ),
          desk = Color(0xFF09090B),
          capsule = Color(0xFF18181B).copy(alpha = 0.78f),
        )
      } else {
        BackgroundPalette(
          wall = listOf(Color(0xFFFAFAFA), Color(0xFFFFFFFF), Color(0xFFF4F4F5)),
          blooms = listOf(
            Color(0xFFE4E4E7) to 0.24f,
            Color(0xFFE2E8F0) to 0.18f,
            Color(0xFFF4F4F5) to 0.24f,
            Color(0xFFCBD5E1) to 0.12f,
            Color(0xFFFFFFFF) to 0.30f,
          ),
          desk = Color(0xFFFAFAFA),
          capsule = Color(0xFFFFFFFF).copy(alpha = 0.80f),
        )
      }
    BackgroundPreset.TWILIGHT ->
      if (dark) {
        BackgroundPalette(
          wall = listOf(Color(0xFF0B0F19), Color(0xFF0F172A), Color(0xFF090D16)),
          blooms = listOf(
            Color(0xFF1E293B) to 0.24f,
            Color(0xFF334155) to 0.16f,
            Color(0xFF0F172A) to 0.20f,
            Color(0xFF1E293B) to 0.14f,
            Color(0xFF475569) to 0.08f,
          ),
          desk = Color(0xFF0B1120),
          capsule = Color(0xFF1E293B).copy(alpha = 0.72f),
        )
      } else {
        BackgroundPalette(
          wall = listOf(Color(0xFFF1F5F9), Color(0xFFF8FAFC), Color(0xFFE2E8F0)),
          blooms = listOf(
            Color(0xFFCBD5E1) to 0.26f,
            Color(0xFFE2E8F0) to 0.28f,
            Color(0xFFF1F5F9) to 0.22f,
            Color(0xFF94A3B8) to 0.10f,
            Color(0xFFFFFFFF) to 0.32f,
          ),
          desk = Color(0xFFF8FAFC),
          capsule = Color(0xFFFFFFFF).copy(alpha = 0.78f),
        )
      }
    BackgroundPreset.OCEAN ->
      if (dark) {
        BackgroundPalette(
          wall = listOf(Color(0xFF080D14), Color(0xFF0C1420), Color(0xFF080C12)),
          blooms = listOf(
            Color(0xFF162438) to 0.24f,
            Color(0xFF1E3A5F) to 0.14f,
            Color(0xFF0F172A) to 0.18f,
            Color(0xFF1E293B) to 0.14f,
            Color(0xFF38BDF8) to 0.05f,
          ),
          desk = Color(0xFF09101A),
          capsule = Color(0xFF131D2B).copy(alpha = 0.74f),
        )
      } else {
        BackgroundPalette(
          wall = listOf(Color(0xFFF0F4F8), Color(0xFFF8FAFC), Color(0xFFE6EEF5)),
          blooms = listOf(
            Color(0xFFBAE6FD) to 0.20f,
            Color(0xFFE0F2FE) to 0.26f,
            Color(0xFFE2E8F0) to 0.22f,
            Color(0xFF93C5FD) to 0.10f,
            Color(0xFFFFFFFF) to 0.32f,
          ),
          desk = Color(0xFFF5F8FB),
          capsule = Color(0xFFFFFFFF).copy(alpha = 0.78f),
        )
      }
    BackgroundPreset.FOREST ->
      if (dark) {
        BackgroundPalette(
          wall = listOf(Color(0xFF090E0C), Color(0xFF0E1613), Color(0xFF090D0B)),
          blooms = listOf(
            Color(0xFF182722) to 0.22f,
            Color(0xFF1F332C) to 0.16f,
            Color(0xFF14211D) to 0.18f,
            Color(0xFF273F37) to 0.12f,
            Color(0xFF34D399) to 0.04f,
          ),
          desk = Color(0xFF0A100E),
          capsule = Color(0xFF141F1B).copy(alpha = 0.76f),
        )
      } else {
        BackgroundPalette(
          wall = listOf(Color(0xFFF2F5F4), Color(0xFFF8FAF9), Color(0xFFE7EEEC)),
          blooms = listOf(
            Color(0xFFA7F3D0) to 0.16f,
            Color(0xFFD1FAE5) to 0.22f,
            Color(0xFFE2E8F0) to 0.22f,
            Color(0xFFCBD5E1) to 0.14f,
            Color(0xFFFFFFFF) to 0.32f,
          ),
          desk = Color(0xFFF5F8F7),
          capsule = Color(0xFFFFFFFF).copy(alpha = 0.78f),
        )
      }
    BackgroundPreset.BERRY ->
      if (dark) {
        BackgroundPalette(
          wall = listOf(Color(0xFF0E0B10), Color(0xFF151018), Color(0xFF0C0A0E)),
          blooms = listOf(
            Color(0xFF271E2E) to 0.22f,
            Color(0xFF312438) to 0.15f,
            Color(0xFF1E1824) to 0.18f,
            Color(0xFF27272A) to 0.14f,
            Color(0xFFC084FC) to 0.04f,
          ),
          desk = Color(0xFF100C12),
          capsule = Color(0xFF1D1722).copy(alpha = 0.76f),
        )
      } else {
        BackgroundPalette(
          wall = listOf(Color(0xFFF5F3F6), Color(0xFFFAF9FB), Color(0xFFEBE7EE)),
          blooms = listOf(
            Color(0xFFE9D5FF) to 0.16f,
            Color(0xFFF3E8FF) to 0.20f,
            Color(0xFFE4E4E7) to 0.22f,
            Color(0xFFD4D4D8) to 0.14f,
            Color(0xFFFFFFFF) to 0.32f,
          ),
          desk = Color(0xFFF8F7FA),
          capsule = Color(0xFFFFFFFF).copy(alpha = 0.78f),
        )
      }
    BackgroundPreset.SAND ->
      if (dark) {
        BackgroundPalette(
          wall = listOf(Color(0xFF0E0D0B), Color(0xFF171512), Color(0xFF0C0B09)),
          blooms = listOf(
            Color(0xFF292524) to 0.24f,
            Color(0xFF44403C) to 0.14f,
            Color(0xFF1C1917) to 0.20f,
            Color(0xFF292524) to 0.14f,
            Color(0xFF78716C) to 0.08f,
          ),
          desk = Color(0xFF100F0D),
          capsule = Color(0xFF1F1D1B).copy(alpha = 0.76f),
        )
      } else {
        BackgroundPalette(
          wall = listOf(Color(0xFFF5F4F1), Color(0xFFFAF9F7), Color(0xFFEAE8E3)),
          blooms = listOf(
            Color(0xFFE7E5E4) to 0.28f,
            Color(0xFFD6D3D1) to 0.20f,
            Color(0xFFF5F5F4) to 0.26f,
            Color(0xFFA8A29E) to 0.10f,
            Color(0xFFFFFFFF) to 0.32f,
          ),
          desk = Color(0xFFF9F8F6),
          capsule = Color(0xFFFFFFFF).copy(alpha = 0.80f),
        )
      }
  }

@Composable
fun ProvideGalleryGlassColors(
  darkTheme: Boolean,
  liteMode: Boolean,
  content: @Composable () -> Unit,
) {
  val base = if (darkTheme) DarkGlassColors else LightGlassColors
  val preset = GlassSettings.backgroundPreset
  val resolved = remember(base, liteMode, darkTheme, preset) {
    val pal = preset.palette(darkTheme)
    val tinted = base.copy(
      wall = pal.wall,
      blooms = pal.blooms,
      desk = pal.desk,
      capsule = pal.capsule,
    )
    if (liteMode) {
      val panel = if (darkTheme) Color(0xFF0D0F14) else Color(0xFFFFFFFF)
      tinted.copy(
        clean = true,
        hero = tinted.ink,
        heroGlow = Color.Transparent,
        wall = pal.wall,
        desk = pal.desk,
        glass = panel.copy(alpha = 0.94f),
        frost = panel,
        capsule = panel,
      )
    } else {
      tinted
    }
  }
  CompositionLocalProvider(LocalGalleryGlassColors provides resolved, content = content)
}
