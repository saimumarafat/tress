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
 * Liquid glass buttons and capsules adapted from Artefact
 * (https://github.com/Chethan616/Artefact, MIT OR Apache-2.0, Copyright 2026 Chethan616).
 */

package com.google.ai.edge.gallery.core.design.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.core.design.glass.GlassSettings
import com.google.ai.edge.gallery.core.design.glass.InkProbe
import com.google.ai.edge.gallery.core.design.glass.LocalBackdrop
import com.google.ai.edge.gallery.core.design.glass.LocalGlassOverlayHost
import com.google.ai.edge.gallery.core.design.glass.SurroundLuminance
import com.google.ai.edge.gallery.core.design.glass.adaptiveBase
import com.google.ai.edge.gallery.core.design.glass.adaptiveGlassEffects
import com.google.ai.edge.gallery.core.design.glass.adaptiveWash
import com.google.ai.edge.gallery.core.design.glass.glassEffects
import com.google.ai.edge.gallery.core.design.glass.glassHighlight
import com.google.ai.edge.gallery.core.design.glass.glassTint
import com.google.ai.edge.gallery.core.design.glass.rememberGlassInk
import com.google.ai.edge.gallery.core.design.glass.rememberSurroundLuminance
import com.google.ai.edge.gallery.core.design.haptics.Feel
import com.google.ai.edge.gallery.core.design.haptics.InteractiveHighlight
import com.google.ai.edge.gallery.core.design.haptics.LocalHaptics
import com.google.ai.edge.gallery.core.design.haptics.liquidPressTransform
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.kyant.backdrop.drawBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LocalGlassInk = compositionLocalOf { Color.Unspecified }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.glassControl(
  shape: Shape,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)?,
  tint: Color,
  surface: Color,
  blur: Dp,
  label: String?,
  enabled: Boolean = true,
  fill: (() -> Color)? = null,
  tap: Feel? = Feel.Tap,
  bloom: Boolean = true,
  tintAt: (() -> Color)? = null,
  candyBase: Boolean = true,
  adaptWash: Boolean = true,
  aroundOut: ((SurroundLuminance?) -> Unit)? = null,
): Modifier {
  val c = glassColors
  val backdrop = LocalBackdrop.current
  if (c.clean || GlassSettings.isLiteTier) {
    val panel = c.solidPanel
    val rim = Color.White.copy(alpha = if (c.dark) 0.14f else 0.72f)
    return clip(shape)
      .drawBehind {
        val accent = tintAt?.invoke() ?: tint
        drawRect(if (accent.isSpecified) accent.compositeOver(panel) else panel)
        if (surface.isSpecified) drawRect(surface)
        fill?.invoke()?.let { drawRect(it) }
      }
      .border(1.dp, rim, shape)
      .pressable(onClick, onLongClick, enabled = enabled, feel = tap)
      .then(if (label == null) Modifier else Modifier.semantics { contentDescription = label })
  }
  val around = rememberSurroundLuminance()
  aroundOut?.invoke(around)
  val scope = rememberCoroutineScope()
  val haptics = LocalHaptics.current
  val currentHaptics by rememberUpdatedState(haptics)
  val hasLongClick by rememberUpdatedState(onLongClick != null)
  val highlight = remember(scope, bloom) {
    InteractiveHighlight(scope, longPressExpand = bloom, onLongPressExpand = {
      if (!hasLongClick) currentHaptics?.play(Feel.Bloom)
    })
  }
  val overlays = LocalGlassOverlayHost.current
  if (overlays != null) {
    LaunchedEffect(overlays, highlight) {
      snapshotFlow { overlays.entries.isNotEmpty() }.collect { if (it) highlight.settle() }
    }
  }
  val source = remember { MutableInteractionSource() }
  val armed = remember { booleanArrayOf(false) }
  val currentLongClick by rememberUpdatedState(onLongClick)
  LaunchedEffect(source) {
    source.interactions.collect { i ->
      when (i) {
        is PressInteraction.Release -> if (armed[0]) { armed[0] = false; currentLongClick?.invoke() }
        is PressInteraction.Cancel -> armed[0] = false
      }
    }
  }
  val rawStyle = GlassSettings.effectiveStyle
  val style = if (around != null) rawStyle.adaptiveBase else rawStyle
  return this
    .then(around?.modifier ?: Modifier)
    .drawBackdrop(
      backdrop = backdrop,
      shape = { shape },
      shadow = null,
      effects = {
        if (around != null) adaptiveGlassEffects(style, around.value, blur.toPx(), 12f.dp.toPx(), 24f.dp.toPx())
        else glassEffects(style, blur.toPx(), 12f.dp.toPx(), 24f.dp.toPx())
      },
      highlight = { glassHighlight(style) },
      layerBlock = { liquidPressTransform(highlight) },
      onDrawSurface = {
        val t = tintAt?.invoke() ?: tint
        if (t.isSpecified) {
          if (candyBase) drawRect(Color.White.copy(alpha = 0.42f))
          drawRect(t, blendMode = BlendMode.Hue)
          drawRect(t.copy(alpha = if (candyBase) 0.8f else 0.75f))
        } else if (around != null && adaptWash) {
          drawRect(adaptiveWash(around.value).let { it.copy(alpha = it.alpha * 0.7f) })
        }
        if (surface.isSpecified) drawRect(surface.glassTint(style))
        val f = fill?.invoke()
        if (f != null && f.alpha > 0f) drawRect(f)
      },
    )
    .combinedClickable(
      interactionSource = source,
      indication = null,
      enabled = enabled,
      role = Role.Button,
      onClick = { if (tap != null && highlight.expandProgress < 0.5f) haptics?.play(tap); onClick() },
      onLongClick = if (onLongClick != null) ({ armed[0] = true; haptics?.play(Feel.Bloom) }) else null,
    )
    .then(highlight.modifier)
    .then(highlight.gestureModifier)
    .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
}

const val BackBeatMillis = 220L

@Composable
fun rememberBeat(action: () -> Unit, millis: Long = BackBeatMillis): () -> Unit {
  val scope = rememberCoroutineScope()
  val latest by rememberUpdatedState(action)
  val going = remember { booleanArrayOf(false) }
  return remember(scope, millis) {
    {
      if (!going[0]) {
        going[0] = true
        scope.launch {
          delay(millis)
          going[0] = false
          latest()
        }
      }
    }
  }
}

@Composable
fun Modifier.glassBar(
  shape: Shape = ChromeCapsule,
  surface: Color = Color.Unspecified,
  blur: Dp = 2.dp,
): Modifier {
  val c = glassColors
  if (c.clean || GlassSettings.isLiteTier) {
    val panel = c.solidPanel
    val rim = Color.White.copy(alpha = if (c.dark) 0.14f else 0.72f)
    return clip(shape)
      .background(if (surface.isSpecified) surface.compositeOver(panel) else panel)
      .border(1.dp, rim, shape)
  }
  val backdrop = LocalBackdrop.current
  val scope = rememberCoroutineScope()
  val highlight = remember(scope) { InteractiveHighlight(scope) }
  val around = rememberSurroundLuminance()
  val rawStyle = GlassSettings.effectiveStyle
  val style = if (around != null) rawStyle.adaptiveBase else rawStyle
  return this
    .then(around?.modifier ?: Modifier)
    .drawBackdrop(
      backdrop = backdrop,
      shape = { shape },
      shadow = null,
      effects = {
        if (around != null) adaptiveGlassEffects(style, around.value, blur.toPx(), 12f.dp.toPx(), 24f.dp.toPx())
        else glassEffects(style, blur.toPx(), 12f.dp.toPx(), 24f.dp.toPx())
      },
      highlight = { glassHighlight(style) },
      layerBlock = { liquidPressTransform(highlight) },
      onDrawSurface = {
        if (around != null) drawRect(adaptiveWash(around.value).let { it.copy(alpha = it.alpha * 0.7f) })
        if (surface.isSpecified) drawRect(surface.glassTint(style))
      },
    )
    .then(highlight.modifier)
    .then(highlight.gestureModifier)
}

@Composable
fun GlassPill(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  tint: Color = Color.Unspecified,
  surface: Color = Color.Unspecified,
  height: Dp = 46.dp,
  blur: Dp = 2.dp,
  horizontalPadding: Dp = 16.dp,
  enabled: Boolean = true,
  onLongClick: (() -> Unit)? = null,
  label: String? = null,
  tintAt: (() -> Color)? = null,
  tap: Feel? = Feel.Tap,
  content: @Composable RowScope.() -> Unit,
) {
  val candy = tint.isSpecified || tintAt != null
  val probe = if (candy) InkProbe(Modifier, Color.White) else rememberGlassInk(glassColors.ink)
  CompositionLocalProvider(LocalGlassInk provides probe.color) {
    Row(
      modifier
        .then(probe.modifier)
        .glassControl(
          shape = ChromeCapsule,
          onClick = onClick,
          onLongClick = onLongClick,
          tint = tint,
          surface = if (tintAt != null) Color.Unspecified else surface,
          blur = blur,
          label = label,
          enabled = enabled,
          tap = tap,
          bloom = true,
          tintAt = tintAt,
        )
        .height(height)
        .padding(horizontal = horizontalPadding),
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
      verticalAlignment = Alignment.CenterVertically,
      content = content,
    )
  }
}

@Composable
fun GlassChip(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  height: Dp = 38.dp,
  surface: Color = Color.Unspecified,
  fill: (() -> Color)? = null,
  blur: Dp = 2.dp,
  contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp),
  horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
  tap: Feel? = Feel.Tap,
  bloom: Boolean = false,
  enabled: Boolean = true,
  onLongClick: (() -> Unit)? = null,
  label: String? = null,
  ink: Color = glassColors.ink,
  content: @Composable RowScope.() -> Unit,
) {
  CompositionLocalProvider(LocalGlassInk provides ink) {
    Row(
      modifier
        .glassControl(
          shape = Capsule,
          onClick = onClick,
          onLongClick = onLongClick,
          tint = Color.Unspecified,
          surface = surface,
          blur = blur,
          label = label,
          enabled = enabled,
          fill = fill,
          tap = tap,
          bloom = bloom,
          adaptWash = false,
        )
        .height(height)
        .padding(contentPadding),
      horizontalArrangement = horizontalArrangement,
      verticalAlignment = Alignment.CenterVertically,
      content = content,
    )
  }
}

val HeaderButtonSize = 42.dp

@Composable
fun HeaderGlassButton(
  onClick: () -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  size: Dp = HeaderButtonSize,
  content: @Composable () -> Unit,
) {
  GlassCircle(onClick = onClick, modifier = modifier, size = size, label = label, ink = glassColors.ink, content = content)
}

val BackArrow: ImageVector by lazy {
  ImageVector.Builder("BackArrow", 24.dp, 24.dp, 24f, 24f).addPath(
    PathParser().parsePathString(
      "M16.62,2.99c-0.49,-0.49 -1.28,-0.49 -1.77,0L6.54,11.3c-0.39,0.39 -0.39,1.02 0,1.41l8.31,8.31c0.49,0.49 1.28,0.49 1.77,0 0.49,-0.49 0.49,-1.28 0,-1.77L9.38,12l7.25,-7.25c0.48,-0.48 0.48,-1.28 -0.01,-1.76z"
    ).toNodes(),
    fill = SolidColor(Color.Black),
  ).build()
}

@Composable
fun GlassBackButton(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  label: String? = null,
  immediate: Boolean = false,
) {
  val beat = rememberBeat(onBack)
  GlassCircle(
    onClick = if (immediate) onBack else beat,
    modifier = modifier,
    size = HeaderButtonSize,
    label = label ?: stringResource(R.string.cd_navigate_back_icon),
  ) {
    GlassIcon(BackArrow, null, Modifier.size(16.dp))
  }
}

@Composable
fun BarGlyphButton(
  onClick: () -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  size: Dp = 40.dp,
  enabled: Boolean = true,
  content: @Composable () -> Unit,
) {
  Box(
    modifier.size(size)
      .clip(CircleShape)
      .pressable(onClick, enabled = enabled, pressedScale = 0.88f)
      .semantics { contentDescription = label },
    contentAlignment = Alignment.Center,
  ) { content() }
}

@Composable
fun GlassCircle(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = HeaderButtonSize,
  tint: Color = Color.Unspecified,
  surface: Color = Color.Unspecified,
  enabled: Boolean = true,
  onLongClick: (() -> Unit)? = null,
  label: String? = null,
  ink: Color = Color.Unspecified,
  tap: Feel? = Feel.Tap,
  content: @Composable () -> Unit,
) {
  val c = glassColors
  val probe = if (tint.isSpecified) InkProbe(Modifier, Color.White)
  else rememberGlassInk(if (ink.isSpecified) ink else c.ink)
  val at = remember { arrayOfNulls<Rect>(1) }
  val glyph by rememberUpdatedState(content)
  val inkNow = probe.color
  val around = remember { arrayOfNulls<SurroundLuminance>(1) }
  val syncMorphAnchor = { rect: Rect ->
    at[0]?.let { prev ->
      if (prev != rect) {
        MorphAnchors.unregister(prev)
        MorphAnchors.setSurround(prev, null)
      }
    }
    at[0] = rect
    MorphAnchors.register(rect) { CompositionLocalProvider(LocalGlassInk provides inkNow) { glyph() } }
    MorphAnchors.setTint(rect, tint)
    MorphAnchors.setSurround(rect, around[0])
  }
  DisposableEffect(Unit) {
    onDispose {
      at[0]?.let { rect ->
        MorphAnchors.unregister(rect)
        MorphAnchors.setSurround(rect, null)
      }
    }
  }
  CompositionLocalProvider(LocalGlassInk provides probe.color) {
    Box(
      modifier.then(probe.modifier)
        .onGloballyPositioned { syncMorphAnchor(it.boundsInWindow()) }
        .drawWithContent { drawContent() }
        .size(size)
        .glassControl(
          shape = CircleShape,
          onClick = onClick,
          onLongClick = onLongClick,
          tint = tint,
          surface = surface,
          blur = 2.dp,
          label = label,
          enabled = enabled,
          tap = tap,
          bloom = true,
          candyBase = false,
          aroundOut = { around[0] = it },
        ),
      contentAlignment = Alignment.Center,
    ) { content() }
  }
}

@Composable
fun GlassFloatingActionButton(
  onClick: () -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  size: Dp = 56.dp,
  tint: Color = Candy.Sky,
  icon: ImageVector,
) {
  GlassCircle(
    onClick = onClick,
    modifier = modifier,
    size = size,
    tint = tint,
    label = label,
    tap = Feel.Rise,
  ) {
    GlassIcon(icon, label, Modifier.size(24.dp), tint = Color.White)
  }
}
