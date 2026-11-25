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
 * Liquid glass controls, tabs, and loader adapted from Artefact
 * (https://github.com/Chethan616/Artefact, MIT OR Apache-2.0, Copyright 2026 Chethan616).
 */

package com.tress.app.core.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.tress.app.core.design.glass.GlassSettings
import com.tress.app.core.design.glass.LocalBackdrop
import com.tress.app.core.design.glass.glassEffects
import com.tress.app.core.design.glass.glassHighlight
import com.tress.app.core.design.glass.glassTint
import com.tress.app.core.design.haptics.DampedDragAnimation
import com.tress.app.core.design.haptics.Feel
import com.tress.app.core.design.haptics.InteractiveHighlight
import com.tress.app.core.design.haptics.LocalHaptics
import com.tress.app.core.design.haptics.liquidStretch
import com.tress.app.core.design.haptics.rememberLiquidStretchState
import com.tress.app.core.design.motion.Fluid
import com.tress.app.core.design.motion.GlassMotion
import com.tress.app.core.design.theme.Candy
import com.tress.app.core.design.theme.glassColors
import com.tress.app.core.design.typography.GalleryType
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
private fun Modifier.thumb(
  drag: DampedDragAnimation,
  track: LayerBackdrop,
  lensHeight: Float,
  lensAmount: Float,
  trackScale: Float,
): Modifier {
  val backdrop = LocalBackdrop.current
  val style = GlassSettings.effectiveStyle
  if (glassColors.clean || GlassSettings.isLiteTier) {
    return clip(Capsule).background(Color.White)
  }
  return drawBackdrop(
    backdrop = rememberCombinedBackdrop(
      backdrop,
      rememberBackdrop(track) { draw ->
        val p = drag.pressProgress
        scale(lerp(2f / 3f, trackScale, p), lerp(0f, trackScale, p)) { draw() }
      },
    ),
    shape = { Capsule },
    effects = {
      val p = drag.pressProgress
      if (p > 0f) blur(8f.dp.toPx() * (1f - p) * style.blur)
      lens(
        lensHeight.dp.toPx() * p * style.refractionHeight,
        lensAmount.dp.toPx() * p * style.refractionAmount,
        chromaticAberration = style.chromaticAberration,
      )
    },
    highlight = {
      val p = drag.pressProgress
      if (p <= 0f || style.highlight <= 0f) null
      else Highlight.Ambient.copy(
        width = Highlight.Ambient.width / 1.5f,
        blurRadius = Highlight.Ambient.blurRadius / 1.5f,
        alpha = p * style.highlight.coerceIn(0f, 1f),
      )
    },
    shadow = null,
    innerShadow = {
      val p = drag.pressProgress
      if (p <= 0f) null else InnerShadow(radius = 4f.dp * p, alpha = p)
    },
    onDrawBackdrop = { draw -> if (drag.pressProgress > 0f) draw() },
    layerBlock = {
      scaleX = drag.scaleX
      scaleY = drag.scaleY
    },
    onDrawSurface = { drawRect(Color.White.copy(alpha = 1f - drag.pressProgress)) },
  )
}

@Composable
fun GlassToggle(
  checked: Boolean,
  onChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  accent: Color = Candy.Sky,
) {
  val c = glassColors
  val track = if (c.dark) Color(0xFF787880).copy(0.36f) else Color(0xFF787878).copy(0.22f)
  val density = LocalDensity.current
  val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
  val haptics = LocalHaptics.current
  val dragWidth = with(density) { 20f.dp.toPx() }
  val scope = rememberCoroutineScope()
  val current by rememberUpdatedState(checked)
  val change by rememberUpdatedState(onChange)
  val dragSlopPx = with(density) { 5.dp.toPx() }
  var didDrag by remember { mutableStateOf(false) }
  var totalAbsDragPx by remember { mutableFloatStateOf(0f) }
  var fraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }
  val drag = remember(scope) {
    DampedDragAnimation(
      scope, fraction, 0f..1f, 0.001f, 1f, 1.45f,
      onDragStarted = {
        didDrag = false
        totalAbsDragPx = 0f
      },
      onDragStopped = {
        val nextChecked =
          if (didDrag) fraction >= 0.5f else !current
        fraction = if (nextChecked) 1f else 0f
        didDrag = false
        totalAbsDragPx = 0f
        if (nextChecked != current) {
          haptics?.play(if (nextChecked) Feel.ToggleOn else Feel.ToggleOff)
          change(nextChecked)
        }
      },
      onDrag = { _, d ->
        totalAbsDragPx += kotlin.math.abs(d.x)
        if (!didDrag && totalAbsDragPx > dragSlopPx) {
          didDrag = true
        }
        if (didDrag) {
          val delta = d.x / dragWidth
          fraction = (if (isLtr) fraction + delta else fraction - delta).fastCoerceIn(0f, 1f)
        }
      },
    )
  }
  LaunchedEffect(drag) { snapshotFlow { fraction }.collectLatest { drag.updateValue(it) } }
  LaunchedEffect(checked) {
    val target = if (checked) 1f else 0f
    if (target != fraction) {
      fraction = target
      drag.animateToValue(target)
    }
  }
  val trackLayer = rememberLayerBackdrop()
  val stretch = rememberLiquidStretchState(stretchFactor = 0.4f, minScale = 0.85f, maxScale = 1.2f)
  LaunchedEffect(drag, stretch) { snapshotFlow { drag.value }.collect { stretch.onPositionSample(it * dragWidth) } }
  Box(
    modifier.clip(Capsule).then(drag.modifier).semantics {
      role = Role.Switch
      toggleableState = ToggleableState(current)
      onClick { change(!current); true }
    },
    contentAlignment = Alignment.CenterStart,
  ) {
    Box(
      Modifier.layerBackdrop(trackLayer)
        .clip(Capsule)
        .drawBehind { drawRect(lerp(track, accent, drag.value)) }
        .size(64.dp, 28.dp),
    )
    Box(
      Modifier
        .graphicsLayer {
          val pad = 2f.dp.toPx()
          translationX = if (isLtr) lerp(pad, pad + dragWidth, drag.value) else lerp(-pad, -(pad + dragWidth), drag.value)
        }
        .liquidStretch(stretch)
        .thumb(drag, trackLayer, 5f, 10f, 0.75f)
        .size(40.dp, 24.dp),
    )
  }
}

@Composable
fun GlassSlider(
  value: Float,
  onChange: (Float) -> Unit,
  modifier: Modifier = Modifier,
  range: ClosedFloatingPointRange<Float> = 0f..1f,
  accent: Color = Candy.Sky,
) {
  val c = glassColors
  val track = if (c.dark) Color(0xFF787880).copy(0.36f) else Color(0xFF787878).copy(0.22f)
  val trackLayer = rememberLayerBackdrop()
  BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
    val width = constraints.maxWidth.coerceAtLeast(1)
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val scope = rememberCoroutineScope()
    val current = value.coerceIn(range.start, range.endInclusive)
    val change by rememberUpdatedState(onChange)
    var dragging by remember { mutableStateOf(false) }
    val drag = remember(scope, isLtr, width, range.start, range.endInclusive) {
      DampedDragAnimation(
        scope, current, range, 0.001f, 1f, 1.45f,
        onDragStarted = { dragging = true },
        onDragStopped = { dragging = false },
        onDrag = { _, d ->
          val delta = (range.endInclusive - range.start) * (d.x / width.toFloat())
          if (delta != 0f) {
            val next = (if (isLtr) targetValue + delta else targetValue - delta).coerceIn(range.start, range.endInclusive)
            updateValue(next)
            change(next)
          }
        },
      )
    }
    val stretch = rememberLiquidStretchState(stretchFactor = 0.4f, minScale = 0.85f, maxScale = 1.2f)
    LaunchedEffect(drag, stretch, width) { snapshotFlow { drag.progress }.collect { stretch.onPositionSample(it * width) } }
    LaunchedEffect(current, dragging, drag) {
      if (!dragging && abs(drag.targetValue - current) > 0.001f) drag.updateValue(current)
    }
    Box(Modifier.layerBackdrop(trackLayer)) {
      Box(
        Modifier.clip(Capsule).background(track)
          .pointerInput(width, isLtr, range.start, range.endInclusive) {
            detectTapGestures { p ->
              val delta = (range.endInclusive - range.start) * (p.x / width.toFloat())
              val target = (if (isLtr) range.start + delta else range.endInclusive - delta).coerceIn(range)
              drag.animateToValue(target)
              change(target)
            }
          }
          .height(6.dp).fillMaxWidth(),
      )
      Box(
        Modifier.clip(Capsule).background(accent).height(6.dp).layout { m, cons ->
          val p = m.measure(cons)
          layout((cons.maxWidth * drag.progress).fastRoundToInt(), p.height) { p.place(0, 0) }
        },
      )
    }
    Box(
      Modifier
        .graphicsLayer {
          translationX = (-size.width / 2f + width * drag.progress).fastCoerceIn(-size.width / 4f, width - size.width * 3f / 4f) * if (isLtr) 1f else -1f
        }
        .then(drag.modifier)
        .liquidStretch(stretch)
        .thumb(drag, trackLayer, 10f, 14f, 1f)
        .size(40.dp, 24.dp),
    )
  }
}

data class GlassTab(val label: String, val icon: ImageVector? = null, val accent: Color = Candy.Sky)

@Composable
fun GlassTabs(
  tabs: List<GlassTab>,
  selected: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
  height: Dp = 56.dp,
  textStyle: TextStyle = GalleryType.Label,
) {
  if (tabs.isEmpty()) return
  val c = glassColors
  if (c.clean || GlassSettings.isLiteTier) {
    Row(
      modifier.clip(Capsule).background(c.solidPanel).selectableGroup().padding(3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      tabs.forEachIndexed { index, tab ->
        val active = index == selected
        Row(
          Modifier.weight(1f).heightIn(min = height - 6.dp).clip(Capsule)
            .background(if (active) tab.accent.copy(alpha = 0.16f) else Color.Transparent)
            .pressable({ onSelect(index) }, role = Role.Tab)
            .semantics { this.selected = active; contentDescription = tab.label },
          horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          tab.icon?.let { Icon(it, null, Modifier.size(18.dp), tint = if (active) tab.accent else c.ink) }
          if (tab.label.isNotEmpty()) {
            Text(
              tab.label,
              style = textStyle,
              color = if (active) tab.accent else c.ink,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }
    return
  }
  val backdrop = LocalBackdrop.current
  val haptics = LocalHaptics.current
  val count = tabs.size
  val container = c.glass
  val inner = (height - 8.dp).coerceAtLeast(0.dp)
  val accent by animateColorAsState(tabs.getOrNull(selected)?.accent ?: Candy.Sky, GlassMotion.settle(), label = "tabAccent")
  val select by rememberUpdatedState(onSelect)
  val selectedNow by rememberUpdatedState(selected)
  val style = GlassSettings.effectiveStyle

  BoxWithConstraints(modifier, contentAlignment = Alignment.CenterStart) {
    val density = LocalDensity.current
    val tabWidth = with(density) { (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / count }
    val offsetAnim = remember { Animatable(0f) }
    val panelOffset by remember(density) {
      derivedStateOf {
        val f = (offsetAnim.value / constraints.maxWidth.coerceAtLeast(1)).fastCoerceIn(-1f, 1f)
        with(density) { 4f.dp.toPx() * f.sign * EaseOut.transform(abs(f)) }
      }
    }
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val scope = rememberCoroutineScope()
    var current by remember { mutableIntStateOf(selected.coerceIn(0, count - 1)) }
    val drag = remember(scope, count) {
      DampedDragAnimation(
        scope, current.toFloat(), 0f..(count - 1).toFloat(), 0.001f, 1f, 78f / 56f,
        onDragStarted = {},
        onDragStopped = {
          val target = targetValue.fastRoundToInt().fastCoerceIn(0, count - 1)
          current = target
          animateToValue(target.toFloat())
          scope.launch { offsetAnim.animateTo(0f, spring(1f, 300f, 0.5f)) }
          if (target != selectedNow) {
            haptics?.play(Feel.Tick)
            select(target)
          }
        },
        onDrag = { _, d ->
          updateValue((targetValue + d.x / tabWidth.coerceAtLeast(1f) * if (isLtr) 1f else -1f).fastCoerceIn(0f, (count - 1).toFloat()))
          scope.launch { offsetAnim.snapTo(offsetAnim.value + d.x) }
        },
      )
    }
    LaunchedEffect(selected) {
      val clamped = selected.coerceIn(0, count - 1)
      current = clamped
      if (drag.targetValue.fastRoundToInt() != clamped) {
        drag.animateToValue(clamped.toFloat())
      }
    }
    val highlight = remember(scope) {
      InteractiveHighlight(scope, position = { size, _ ->
        Offset(
          if (isLtr) (drag.value + 0.5f) * tabWidth + panelOffset else size.width - (drag.value + 0.5f) * tabWidth + panelOffset,
          size.height / 2f,
        )
      })
    }
    val pressScale = { lerp(1f, 1.06f, drag.pressProgress) }

    Box(
      Modifier
        .padding(horizontal = 4.dp)
        .graphicsLayer {
          translationX = if (isLtr) drag.value * tabWidth + panelOffset else size.width - (drag.value + 1f) * tabWidth + panelOffset
          scaleX = drag.scaleX
          scaleY = drag.scaleY
          val v = drag.velocity / 10f
          scaleX /= 1f - (v * 0.65f).fastCoerceIn(-0.16f, 0.16f)
          scaleY *= 1f - (v * 0.22f).fastCoerceIn(-0.16f, 0.16f)
        }
        .clip(Capsule)
        .background(accent.copy(alpha = if (c.dark) 0.28f else 0.18f))
        .height(inner)
        .fillMaxWidth(1f / count),
    )

    Row(
      Modifier
        .graphicsLayer { translationX = panelOffset }
        .drawBackdrop(
          backdrop = backdrop,
          shape = { Capsule },
          shadow = null,
          effects = { glassEffects(style, 4f.dp.toPx(), 14f.dp.toPx(), 16f.dp.toPx()) },
          highlight = { glassHighlight(style) },
          layerBlock = {
            val s = if (size.width <= 0f) 1f else lerp(1f, 1f + 10f.dp.toPx() / size.width, drag.pressProgress)
            scaleX = s
            scaleY = s
          },
          onDrawSurface = { drawRect(container.glassTint(style)) },
        )
        .then(highlight.modifier)
        .then(highlight.gestureModifier)
        .then(drag.modifier)
        .height(height)
        .fillMaxWidth()
        .padding(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      tabs.forEachIndexed { i, t ->
        Column(
          Modifier.weight(1f).fillMaxHeight()
            .clickable(remember { MutableInteractionSource() }, null, role = Role.Tab) {
              current = i
              drag.animateToValue(i.toFloat())
              if (i != selectedNow) {
                haptics?.play(Feel.Tick)
                select(i)
              }
            }
            .semantics { this.selected = i == current; contentDescription = t.label; role = Role.Tab }
            .graphicsLayer { val s = pressScale(); scaleX = s; scaleY = s },
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          val ink = if (i == current) accent else c.ink
          if (t.icon != null) Icon(t.icon, null, tint = ink, modifier = Modifier.size(if (t.label.isEmpty()) 22.dp else 19.dp))
          if (t.icon != null && t.label.isNotEmpty()) Spacer(Modifier.height(2.dp))
          if (t.label.isNotEmpty()) Text(t.label, style = textStyle, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    }
  }
}

@Composable
fun GlassLoader(
  modifier: Modifier = Modifier,
  size: Dp = 56.dp,
  color: Color = Candy.Sky,
  label: String? = null,
) {
  val appear = remember { Animatable(0f) }
  LaunchedEffect(Unit) { appear.animateTo(1f, Fluid.quick()) }
  Box(
    modifier
      .graphicsLayer {
        val s = 0.84f + 0.16f * appear.value
        scaleX = s
        scaleY = s
        alpha = appear.value.coerceIn(0f, 1f)
      }
      .size(size)
      .glass(CircleShape, glassColors.frost, blur = 12.dp)
      .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
    contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator(
      modifier = Modifier.size(size * 0.48f),
      color = color,
      strokeWidth = 3.dp,
      strokeCap = StrokeCap.Round,
    )
  }
}
