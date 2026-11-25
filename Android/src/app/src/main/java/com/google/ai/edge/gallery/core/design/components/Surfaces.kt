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
 * Glass surface modifiers and containers adapted from Artefact
 * (https://github.com/Chethan616/Artefact, MIT OR Apache-2.0, Copyright 2026 Chethan616).
 */

package com.google.ai.edge.gallery.core.design.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.core.design.glass.GlassLuminance
import com.google.ai.edge.gallery.core.design.glass.GlassSettings
import com.google.ai.edge.gallery.core.design.glass.GlassStyle
import com.google.ai.edge.gallery.core.design.glass.LocalBackdrop
import com.google.ai.edge.gallery.core.design.glass.adaptiveBase
import com.google.ai.edge.gallery.core.design.glass.adaptiveGlassEffects
import com.google.ai.edge.gallery.core.design.glass.adaptiveWash
import com.google.ai.edge.gallery.core.design.glass.glassEffects
import com.google.ai.edge.gallery.core.design.glass.glassHighlight
import com.google.ai.edge.gallery.core.design.glass.glassTint
import com.google.ai.edge.gallery.core.design.glass.rememberSurroundLuminance
import com.google.ai.edge.gallery.core.design.haptics.Feel
import com.google.ai.edge.gallery.core.design.haptics.LocalHaptics
import com.google.ai.edge.gallery.core.design.haptics.rememberJellyPress
import com.google.ai.edge.gallery.core.design.motion.Fluid
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop

@Composable
fun Modifier.glass(
  shape: Shape,
  wash: Color = glassColors.glass,
  blur: Dp = 8.dp,
  lensHeight: Dp = 16.dp,
  lensAmount: Dp = 24.dp,
  styleOverride: GlassStyle? = null,
  luminance: GlassLuminance? = null,
): Modifier {
  val c = glassColors
  if (c.clean || GlassSettings.isLiteTier) {
    val rim = Color.White.copy(alpha = if (c.dark) 0.14f else 0.72f)
    return clip(shape)
      .background(if (wash.isSpecified) wash.copy(alpha = if (c.dark) 0.88f else 0.94f) else c.solidPanel)
      .border(1.dp, rim, shape)
  }
  val backdrop = LocalBackdrop.current
  val style = styleOverride ?: GlassSettings.effectiveStyle
  if (luminance != null) {
    return drawBackdrop(
      backdrop = backdrop,
      shape = { shape },
      shadow = null,
      effects = { adaptiveGlassEffects(style, luminance.value, blur.toPx(), lensHeight.toPx(), lensAmount.toPx()) },
      highlight = { glassHighlight(style) },
      onDrawBackdrop = luminance.recorder,
      onDrawSurface = { drawRect(adaptiveWash(luminance.value).glassTint(style)) },
    )
  }
  val around = rememberSurroundLuminance()
  if (around != null && styleOverride == null) {
    val base = style.adaptiveBase
    return then(around.modifier).drawBackdrop(
      backdrop = backdrop,
      shape = { shape },
      shadow = null,
      effects = { adaptiveGlassEffects(base, around.value, blur.toPx(), lensHeight.toPx(), lensAmount.toPx()) },
      highlight = { glassHighlight(base) },
      onDrawSurface = { if (wash.isSpecified) drawRect(wash) },
    )
  }
  return drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    shadow = null,
    effects = { glassEffects(style, blur.toPx(), lensHeight.toPx(), lensAmount.toPx()) },
    highlight = { glassHighlight(style) },
    onDrawSurface = { if (wash.isSpecified) drawRect(wash.glassTint(style)) },
  )
}

@Composable
fun GlassSurface(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedRectangle(24.dp),
  wash: Color = glassColors.glass,
  blur: Dp = 8.dp,
  lensHeight: Dp = 14.dp,
  lensAmount: Dp = 20.dp,
  styleOverride: GlassStyle? = null,
  luminance: GlassLuminance? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier.glass(
      shape = shape,
      wash = wash,
      blur = blur,
      lensHeight = lensHeight,
      lensAmount = lensAmount,
      styleOverride = styleOverride,
      luminance = luminance,
    ),
    content = content,
  )
}

@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedRectangle(28.dp),
  wash: Color = glassColors.glass,
  blur: Dp = 8.dp,
  styleOverride: GlassStyle? = null,
  luminance: GlassLuminance? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val below = LocalBackdrop.current
  val layer = rememberLayerBackdrop()
  val combined = rememberCombinedBackdrop(below, layer)
  Box(modifier) {
    Box(
      Modifier.matchParentSize()
        .layerBackdrop(layer)
        .glass(shape, wash, blur, styleOverride = styleOverride, luminance = luminance),
    )
    CompositionLocalProvider(LocalBackdrop provides combined) {
      Column(content = content)
    }
  }
}

@Composable
fun GlassHost(
  modifier: Modifier = Modifier,
  paint: @Composable BoxScope.() -> Unit,
  opaque: Boolean = false,
  content: @Composable BoxScope.() -> Unit,
) {
  val below = LocalBackdrop.current
  val layer = rememberLayerBackdrop()
  val combined = if (opaque) layer else rememberCombinedBackdrop(below, layer)
  Box(modifier) {
    Box(Modifier.matchParentSize().layerBackdrop(layer), content = paint)
    CompositionLocalProvider(LocalBackdrop provides combined) { content() }
  }
}

/**
 * Lightweight frosted wash panel for scrolling list cards (avoids expensive real-time backdrop
 * blur inside LazyColumn/LazyVerticalGrid items while keeping glass tint and specular border).
 */
@Composable
fun Modifier.wash(shape: Shape = RoundedRectangle(22.dp), tint: Color = glassColors.glass): Modifier {
  val c = glassColors
  val dark = c.dark
  if (c.clean || GlassSettings.isLiteTier) {
    val border = if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    return clip(shape).background(c.solidPanel).border(1.dp, border, shape)
  }
  val rimTop = Color.White.copy(alpha = if (dark) 0.18f else 0.72f)
  val rimBottom = Color.White.copy(alpha = if (dark) 0.04f else 0.20f)
  return clip(shape)
    .background(tint.glassTint(GlassSettings.effectiveStyle))
    .border(1.dp, Brush.verticalGradient(listOf(rimTop, rimBottom)), shape)
}

@Composable
fun washPanel(radius: Dp = 20.dp): Modifier = Modifier.wash(RoundedRectangle(radius))

@Composable
fun WashPanel(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedRectangle(24.dp),
  tint: Color = glassColors.glass,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(modifier.wash(shape, tint), content = content)
}

@Composable
fun GlassPanel(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedRectangle(28.dp),
  wash: Color = glassColors.glass,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(modifier.glass(shape, wash), content = content)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pressable(
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
  pressedScale: Float = Fluid.PressedScale,
  role: Role = Role.Button,
  enabled: Boolean = true,
  feel: Feel? = Feel.Tap,
): Modifier {
  val source = remember { MutableInteractionSource() }
  val jelly = rememberJellyPress(source, pressedScale)
  val haptics = LocalHaptics.current
  return graphicsLayer {
    scaleX = jelly.scale
    scaleY = jelly.scale
  }.combinedClickable(
    interactionSource = source,
    indication = null,
    enabled = enabled,
    role = role,
    onClick = { if (feel != null) haptics?.play(feel); onClick() },
    onLongClick = onLongClick?.let { l -> { haptics?.play(Feel.Bloom); jelly.rest(); l() } },
  )
}

@Composable
fun GlassIcon(
  icon: ImageVector,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  tint: Color = Color.Unspecified,
) {
  val ink = when {
    tint.isSpecified -> tint
    LocalGlassInk.current.isSpecified -> LocalGlassInk.current
    else -> glassColors.ink
  }
  Icon(icon, contentDescription, modifier, tint = ink)
}
