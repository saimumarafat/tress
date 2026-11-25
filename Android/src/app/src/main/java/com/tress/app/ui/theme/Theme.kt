/*
 * Copyright 2025 Google LLC
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

package com.tress.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.tress.app.core.design.glass.GlassSettings
import com.tress.app.core.design.glass.ProvideScreenInk
import com.tress.app.core.design.haptics.ProvideGlassHaptics
import com.tress.app.core.design.theme.GalleryDarkSkyScheme
import com.tress.app.core.design.theme.GalleryLightSkyScheme
import com.tress.app.core.design.theme.ProvideGalleryGlassColors
import com.tress.app.core.design.theme.palette
import com.tress.app.core.design.typography.GalleryMaterialTypography
import com.tress.app.proto.Theme

private val lightScheme =
  lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
  )

private val darkScheme =
  darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
  )

@Immutable
data class CustomColors(
  val appTitleGradientColors: List<Color> = listOf(),
  val tabHeaderBgColor: Color = Color.Transparent,
  val taskCardBgColor: Color = Color.Transparent,
  val taskBgColors: List<Color> = listOf(),
  val taskBgGradientColors: List<List<Color>> = listOf(),
  val taskIconColors: List<Color> = listOf(),
  val taskIconShapeBgColor: Color = Color.Transparent,
  val homeBottomGradient: List<Color> = listOf(),
  val userBubbleBgColor: Color = Color.Transparent,
  val agentBubbleBgColor: Color = Color.Transparent,
  val linkColor: Color = Color.Transparent,
  val successColor: Color = Color.Transparent,
  val positiveStrokeColor: Color = Color.Transparent,
  val negativeStrokeColor: Color = Color.Transparent,
  val lassoStrokeColor: Color = Color.Transparent,
  val recordButtonBgColor: Color = Color.Transparent,
  val waveFormBgColor: Color = Color.Transparent,
  val modelInfoIconColor: Color = Color.Transparent,
  val warningContainerColor: Color = Color.Transparent,
  val warningTextColor: Color = Color.Transparent,
  val errorContainerColor: Color = Color.Transparent,
  val errorTextColor: Color = Color.Transparent,
  val newFeatureContainerColor: Color = Color.Transparent,
  val newFeatureTextColor: Color = Color.Transparent,
  val bgStarColor: Color = Color.Transparent,
  val promoBannerBgBrush: Brush = Brush.verticalGradient(listOf(Color.Transparent)),
  val promoBannerIconBgBrush: Brush = Brush.verticalGradient(listOf(Color.Transparent)),
  /**
   * Glow drawn over the live camera feed (for example the AR Language Teacher's thinking state). It
   * sits on camera pixels rather than a themed surface, so it uses the same colors in light and
   * dark themes.
   */
  val cameraHudGlowColor: Color = Color.Transparent,
  /** Text and icon color drawn on [cameraHudGlowColor]. */
  val onCameraHudGlowColor: Color = Color.Transparent,
  /** The colors of the XR Language Teacher `Vocab Popper` and `Buddy Runner` game screens. */
  val vocabGamePalette: VocabGamePalette = lightVocabGamePalette,
)

val LocalCustomColors: ProvidableCompositionLocal<CustomColors> = staticCompositionLocalOf {
  CustomColors()
}

val lightCustomColors =
  CustomColors(
    appTitleGradientColors = listOf(Color(0xFF18181B), Color(0xFF3F3F46)),
    tabHeaderBgColor = Color(0xFF18181B),
    taskCardBgColor = surfaceContainerLowestLight,
    taskBgColors =
      listOf(
        Color(0xFFFFFFFF),
        Color(0xFFFAFAFA),
        Color(0xFFF8FAFC),
        Color(0xFFF4F4F5),
      ),
    taskBgGradientColors =
      listOf(
        listOf(Color(0xFF27272A), Color(0xFF18181B)),
        listOf(Color(0xFF3F3F46), Color(0xFF27272A)),
        listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
        listOf(Color(0xFF52525B), Color(0xFF334155)),
      ),
    taskIconColors =
      listOf(
        Color(0xFF18181B),
        Color(0xFF27272A),
        Color(0xFF0F172A),
        Color(0xFF3F3F46),
      ),
    taskIconShapeBgColor = Color(0xFFF4F4F5),
    homeBottomGradient = listOf(Color(0x00FAFAFA), Color(0xFFE4E4E7)),
    agentBubbleBgColor = Color(0xFFF4F4F5),
    userBubbleBgColor = Color(0xFF18181B),
    linkColor = Color(0xFF2563EB),
    successColor = Color(0xFF16A34A),
    positiveStrokeColor = Color(0xFF16A34A),
    negativeStrokeColor = Color(0xFFDC2626),
    lassoStrokeColor = Color(0xFF18181B),
    recordButtonBgColor = Color(0xFFDC2626),
    waveFormBgColor = Color(0xFFA1A1AA),
    modelInfoIconColor = Color(0xFF71717A),
    warningContainerColor = Color(0xFFFEF3C7),
    warningTextColor = Color(0xFFB45309),
    errorContainerColor = Color(0xFFFEE2E2),
    errorTextColor = Color(0xFFDC2626),
    newFeatureContainerColor = Color(0xFFF4F4F5),
    newFeatureTextColor = Color(0xFF18181B),
    bgStarColor = Color(0x2271717A),
    promoBannerBgBrush =
      Brush.linearGradient(
        colorStops =
          arrayOf(
            0.0f to Color(0xFFF4F4F5),
            1.0f to Color(0xFFE4E4E7),
          ),
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
      ),
    promoBannerIconBgBrush =
      Brush.linearGradient(
        colorStops =
          arrayOf(
            0.0f to Color(0xFFE4E4E7),
            1.0f to Color(0xFFD4D4D8),
          ),
        start = Offset(0f, 1f),
        end = Offset(1f, 0f),
      ),
    cameraHudGlowColor = primaryLight,
    onCameraHudGlowColor = onPrimaryLight,
    vocabGamePalette = lightVocabGamePalette,
  )

val darkCustomColors =
  CustomColors(
    appTitleGradientColors = listOf(Color(0xFFFAFAFA), Color(0xFFA1A1AA)),
    tabHeaderBgColor = Color(0xFF27272A),
    taskCardBgColor = surfaceContainerHighDark,
    taskBgColors =
      listOf(
        Color(0xFF18181B),
        Color(0xFF141417),
        Color(0xFF1C1C20),
        Color(0xFF18181B),
      ),
    taskBgGradientColors =
      listOf(
        listOf(Color(0xFF27272A), Color(0xFF18181B)),
        listOf(Color(0xFF3F3F46), Color(0xFF27272A)),
        listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
        listOf(Color(0xFF3F3F46), Color(0xFF27272A)),
      ),
    taskIconColors =
      listOf(
        Color(0xFFFAFAFA),
        Color(0xFFE4E4E7),
        Color(0xFFD4D4D8),
        Color(0xFFA1A1AA),
      ),
    taskIconShapeBgColor = Color(0xFF27272A),
    homeBottomGradient = listOf(Color(0x0009090B), Color(0x1F27272A)),
    agentBubbleBgColor = Color(0xFF18181B),
    userBubbleBgColor = Color(0xFF27272A),
    linkColor = Color(0xFF60A5FA),
    successColor = Color(0xFF22C55E),
    positiveStrokeColor = Color(0xFF22C55E),
    negativeStrokeColor = Color(0xFFEF4444),
    lassoStrokeColor = Color(0xFFFAFAFA),
    recordButtonBgColor = Color(0xFFEF4444),
    waveFormBgColor = Color(0xFF71717A),
    modelInfoIconColor = Color(0xFFA1A1AA),
    warningContainerColor = Color(0xFF451A03),
    warningTextColor = Color(0xFFFBBF24),
    errorContainerColor = Color(0xFF450A0A),
    errorTextColor = Color(0xFFF87171),
    newFeatureContainerColor = Color(0xFF27272A),
    newFeatureTextColor = Color(0xFFFAFAFA),
    bgStarColor = Color(0x1A71717A),
    promoBannerBgBrush =
      Brush.linearGradient(
        colorStops = arrayOf(0.0f to Color(0xFF18181B), 1.0f to Color(0xFF27272A)),
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY),
      ),
    promoBannerIconBgBrush =
      Brush.linearGradient(
        colorStops =
          arrayOf(
            0.0f to Color(0xFF27272A),
            1.0f to Color(0xFF3F3F46),
          ),
        start = Offset(0f, 1f),
        end = Offset(1f, 0f),
      ),
    cameraHudGlowColor = primaryLight,
    onCameraHudGlowColor = onPrimaryLight,
    vocabGamePalette = darkVocabGamePalette,
  )

val MaterialTheme.customColors: CustomColors
  @Composable @ReadOnlyComposable get() = LocalCustomColors.current

/**
 * Controls the color of the phone's status bar icons based on whether the app is using a dark
 * theme.
 */
@Composable
fun StatusBarColorController(useDarkTheme: Boolean) {
  val view = LocalView.current
  val currentWindow = (view.context as? Activity)?.window

  if (currentWindow != null) {
    SideEffect {
      WindowCompat.setDecorFitsSystemWindows(currentWindow, false)
      val controller = WindowCompat.getInsetsController(currentWindow, view)
      controller.isAppearanceLightStatusBars = !useDarkTheme
      controller.isAppearanceLightNavigationBars = !useDarkTheme
    }
  }
}

@Composable
fun GalleryTheme(content: @Composable () -> Unit) {
  val context = LocalContext.current
  remember(context) {
    GlassSettings.load(context)
    true
  }
  val themeOverride = ThemeSettings.themeOverride
  val darkTheme: Boolean =
    (isSystemInDarkTheme() || themeOverride.value == Theme.THEME_DARK) &&
      themeOverride.value != Theme.THEME_LIGHT
  val view = LocalView.current

  StatusBarColorController(useDarkTheme = darkTheme)

  val bgColor = GlassSettings.backgroundPreset.palette(darkTheme).desk
  val colorScheme =
    when {
      darkTheme -> GalleryDarkSkyScheme.copy(background = bgColor)
      else -> GalleryLightSkyScheme.copy(background = bgColor)
    }

  val customColorsPalette = if (darkTheme) darkCustomColors else lightCustomColors

  ProvideGalleryGlassColors(darkTheme = darkTheme, liteMode = GlassSettings.isLiteTier) {
    ProvideGlassHaptics {
      ProvideScreenInk {
        CompositionLocalProvider(LocalCustomColors provides customColorsPalette) {
          MaterialTheme(
            colorScheme = colorScheme,
            typography = GalleryMaterialTypography,
            content = content,
          )
        }
      }
    }
  }

  // Make sure the navigation bar stays transparent on manual theme changes.
  LaunchedEffect(darkTheme) {
    val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
    window.isNavigationBarContrastEnforced = false
  }
}
