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
 * Morphing button-to-menu surface adapted from Artefact
 * (https://github.com/Chethan616/Artefact, MIT OR Apache-2.0, Copyright 2026 Chethan616).
 */

package com.google.ai.edge.gallery.core.design.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.google.ai.edge.gallery.core.design.glass.GlassOverlay
import com.google.ai.edge.gallery.core.design.glass.GlassSettings
import com.google.ai.edge.gallery.core.design.glass.LocalGlassOverlayHost
import com.google.ai.edge.gallery.core.design.glass.LocalMenuInk
import com.google.ai.edge.gallery.core.design.glass.LocalOverlayPresence
import com.google.ai.edge.gallery.core.design.glass.SurroundLuminance
import com.google.ai.edge.gallery.core.design.glass.adaptiveInk
import com.google.ai.edge.gallery.core.design.glass.rememberGlassLuminance
import com.google.ai.edge.gallery.core.design.motion.MotionSettings
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val MorphMenuRadius = 24.dp

@Composable
fun MorphButton(
  items: List<GlassMenuItem>,
  label: String,
  modifier: Modifier = Modifier,
  size: Dp = HeaderButtonSize,
  menuWidth: Dp = 248.dp,
  enabled: Boolean = true,
  surface: Color = Color.Unspecified,
  transparent: Boolean = false,
  icon: @Composable () -> Unit,
) {
  var open by remember { mutableStateOf(false) }
  var up by remember { mutableStateOf(false) }
  var anchor by remember { mutableStateOf(Rect.Zero) }
  if (transparent) {
    Box(
      modifier =
        modifier
          .onGloballyPositioned { anchor = it.boundsInWindow() }
          .graphicsLayer { alpha = if (up) 0f else 1f }
          .size(size)
          .clip(androidx.compose.foundation.shape.CircleShape)
          .pressable(
            onClick = { if (enabled && !up) { open = true; up = true } },
            enabled = enabled,
            role = androidx.compose.ui.semantics.Role.Button,
          ),
      contentAlignment = Alignment.Center,
    ) {
      icon()
    }
  } else {
    GlassCircle(
      onClick = { if (enabled && !up) { open = true; up = true } },
      modifier = modifier.onGloballyPositioned { anchor = it.boundsInWindow() }.graphicsLayer { alpha = if (up) 0f else 1f },
      size = size,
      label = label,
      enabled = enabled,
      surface = surface,
    ) { icon() }
  }
  if (up) {
    MorphButtonSurface(
      anchor = anchor,
      open = open,
      onDismiss = { open = false },
      onGone = { up = false },
      items = items,
      menuWidth = menuWidth,
      icon = icon,
    )
  }
}

@Composable
fun MorphButtonSurface(
  anchor: Rect,
  open: Boolean,
  onDismiss: () -> Unit,
  onGone: () -> Unit,
  items: List<GlassMenuItem>,
  menuWidth: Dp = 248.dp,
  maxHeight: Dp = 480.dp,
  icon: (@Composable () -> Unit)? = null,
) = MorphButtonSurface(anchor, open, onDismiss, onGone, menuWidth, maxHeight, icon) { shut, choose ->
  items.forEach { item -> MorphMenuRow(item, shut) { choose(item.onClick) } }
}

@Composable
fun MorphButtonSurface(
  anchor: Rect,
  open: Boolean,
  onDismiss: () -> Unit,
  onGone: () -> Unit,
  menuWidth: Dp = 248.dp,
  maxHeight: Dp = 480.dp,
  icon: (@Composable () -> Unit)? = null,
  ghost: Boolean = false,
  body: @Composable ColumnScope.(shut: Boolean, choose: (() -> Unit) -> Unit) -> Unit,
) {
  val gone: () -> Unit = {
    if (MorphAnchors.active == anchor) MorphAnchors.active = null
    onGone()
  }
  val glyph: @Composable () -> Unit = icon ?: MorphAnchors.glyphAt(anchor) ?: {}
  if (!glassColors.clean && !GlassSettings.isLiteTier && !MotionSettings.isReducedMotion && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    LiquidMorphSurface(anchor, open, onDismiss, gone, menuWidth, maxHeight, glyph, ghost, body)
  } else {
    RectMorphSurface(anchor, open, onDismiss, gone, menuWidth, maxHeight, glyph, ghost, body)
  }
}

internal fun isMorphAnchor(anchor: Rect, density: androidx.compose.ui.unit.Density): Boolean {
  val lo = with(density) { 24.dp.toPx() }
  val hi = with(density) { 72.dp.toPx() }
  return anchor.width in lo..hi && anchor.height in lo..hi && kotlin.math.abs(anchor.width - anchor.height) <= anchor.width * 0.25f
}

object MorphAnchors {
  var active: Rect? by mutableStateOf(null)
  private val glyphs = HashMap<Long, @Composable () -> Unit>()
  private val tints = HashMap<Long, Color>()
  private val surrounds = HashMap<Long, SurroundLuminance>()

  private fun key(r: Rect): Long =
    (r.left.toInt().toLong() / 4 shl 32) or (r.top.toInt().toLong() / 4 and 0xffffffffL)

  internal fun register(r: Rect, glyph: @Composable () -> Unit) { glyphs[key(r)] = glyph }
  internal fun unregister(r: Rect) { glyphs.remove(key(r)) }
  internal fun shownAt(r: Rect) { if (active != r) active = r }
  internal fun glyphAt(r: Rect): (@Composable () -> Unit)? = glyphs[key(r)]
  internal fun setTint(r: Rect, tint: Color) { if (tint.isSpecified) tints[key(r)] = tint else tints.remove(key(r)) }
  internal fun tintAt(r: Rect): Color? = tints[key(r)]
  internal fun setSurround(r: Rect, s: SurroundLuminance?) { if (s != null) surrounds[key(r)] = s else surrounds.remove(key(r)) }
  internal fun surroundAt(r: Rect): SurroundLuminance? = surrounds[key(r)]

  internal fun hides(r: Rect): Boolean {
    val a = active ?: return false
    return kotlin.math.abs(a.left - r.left) < 3f && kotlin.math.abs(a.top - r.top) < 3f && kotlin.math.abs(a.width - r.width) < 3f
  }
}

@Composable
private fun RectMorphSurface(
  anchor: Rect,
  open: Boolean,
  onDismiss: () -> Unit,
  onGone: () -> Unit,
  menuWidth: Dp,
  maxHeight: Dp,
  icon: @Composable () -> Unit,
  ghost: Boolean,
  body: @Composable ColumnScope.(shut: Boolean, choose: (() -> Unit) -> Unit) -> Unit,
) {
  val gone by rememberUpdatedState(onGone)
  val c = glassColors
  GlassOverlay(animatesOut = true) {
    val presence = LocalOverlayPresence.current
    val shut = !open || presence?.leaving == true
    DisposableEffect(Unit) { onDispose { gone() } }
    BackHandler(enabled = !shut, onBack = onDismiss)
    val host = LocalGlassOverlayHost.current ?: return@GlassOverlay
    val bars = WindowInsets.systemBars
    val bottomBars = WindowInsets.systemBars.union(WindowInsets.ime)
    val view = LocalView.current
    val density = LocalDensity.current

    val drop = remember { Animatable(0f) }
    val wide = remember { Animatable(0f) }
    val tall = remember { Animatable(0f) }
    var chosenAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val runChosen = {
      val a = chosenAction
      chosenAction = null
      a?.invoke()
      Unit
    }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(shut) {
      if (!shut) {
        if (MotionSettings.isReducedMotion) {
          drop.snapTo(1f); wide.snapTo(1f); tall.snapTo(1f)
        } else {
          coroutineScope {
            launch { drop.animateTo(1f, spring(maxOf(MotionSettings.damping(0.62f), 0.3f), MotionSettings.stiffness(318f), 0.0005f)) }
            launch {
              snapshotFlow { drop.value >= 0.45f }.first { it }
              wide.animateTo(1f, spring(maxOf(MotionSettings.damping(0.58f), 0.3f), MotionSettings.stiffness(272f), 0.0005f))
            }
            launch {
              snapshotFlow { wide.value >= 0.45f }.first { it }
              tall.animateTo(1f, spring(maxOf(MotionSettings.damping(0.6f), 0.3f), MotionSettings.stiffness(181f), 0.0005f))
            }
          }
        }
      } else if (MotionSettings.isReducedMotion || (!ghost && MorphAnchors.glyphAt(anchor) == null)) {
        fade.animateTo(0f, tween(120))
        gone()
        presence?.done()
        runChosen()
      } else {
        coroutineScope {
          launch { tall.animateTo(0f, spring(1f, MotionSettings.stiffness(469f), 0.0005f)) }
          launch {
            snapshotFlow { tall.value <= 0.4f }.first { it }
            wide.animateTo(0f, spring(1f, MotionSettings.stiffness(393f), 0.0005f))
          }
          launch {
            snapshotFlow { tall.value <= 0.05f && wide.value <= 0.4f }.first { it }
            drop.animateTo(0f, spring(1f, MotionSettings.stiffness(423f), 0.0005f))
          }
        }
        if (ghost) fade.animateTo(0f, tween(110))
        gone()
        presence?.done()
        runChosen()
      }
    }
    val peekPx = LocalContext.current.resources.displayMetrics.ydpi / 2.54f

    Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
    val local = Rect(host.windowToLocal(anchor.topLeft), host.windowToLocal(anchor.bottomRight))
    val margin = with(density) { 12.dp.toPx() }
    val widthPx = with(density) { menuWidth.toPx() }
    var full by remember { mutableIntStateOf(0) }
    var fromRight by remember { mutableStateOf(true) }
    var fromBelow by remember { mutableStateOf(true) }
    val originPre = host.originInWindow
    val rootPre = view.rootView
    val rootImeBottomPre =
      ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
    val bottomInsetPre = max(bottomBars.getBottom(density), rootImeBottomPre)
    val barTopPre = (bars.getTop(density) - originPre.y).coerceAtLeast(0f)
    val barBottomPre = (bottomInsetPre - (rootPre.height - originPre.y - view.height)).coerceAtLeast(0f)
    val spaceAbovePre = (local.bottom - barTopPre - margin).coerceAtLeast(0f)
    val spaceBelowPre = (view.height - barBottomPre - margin - local.top).coerceAtLeast(0f)
    val availableMaxH = with(density) { (if (fromBelow) spaceBelowPre else spaceAbovePre).toDp() }
      .coerceAtLeast(with(density) { local.height.toDp() })
      .coerceAtMost(maxHeight)
    val menuNow = if (full > 0) full.toFloat() else local.height
    val curW = lerp(local.width, widthPx, wide.value.coerceAtMost(MaxSwingWide))
    val curH = morphHeight(local.height, menuNow, peekPx, drop.value, if (full > 0) tall.value else 0f)
    val grown = ((curH - local.height) / (menuNow - local.height).coerceAtLeast(1f)).coerceIn(0f, 1f)
    val radiusPx = min(
      min(curW, curH) / 2f,
      lerp(min(local.width, local.height) / 2f, with(density) { MorphMenuRadius.toPx() }, grown),
    ).coerceAtLeast(0f)
    val shape = RoundedRectangle(with(density) { radiusPx.toDp() })
    val flow = kotlin.math.sin(Math.PI.toFloat() * tall.value.coerceIn(0f, 1f)) +
      0.6f * kotlin.math.sin(Math.PI.toFloat() * wide.value.coerceIn(0f, 1f)) +
      0.4f * kotlin.math.sin(Math.PI.toFloat() * drop.value.coerceIn(0f, 1f))
    val corner = when {
      fromBelow && fromRight -> Alignment.TopEnd
      fromBelow -> Alignment.TopStart
      fromRight -> Alignment.BottomEnd
      else -> Alignment.BottomStart
    }
    val luminance = rememberGlassLuminance(enabled = true)
    val inkColor = adaptiveInk(luminance, c.ink)
    Layout(
      content = {
        CompositionLocalProvider(LocalGlassInk provides inkColor, LocalMenuInk provides inkColor) {
          Box(
            Modifier
              .graphicsLayer { alpha = fade.value }
              .glass(
                shape,
                wash = Color.Transparent,
                blur = 14.dp,
                lensHeight = 14.dp,
                lensAmount = 22.dp,
                luminance = luminance,
              )
              .clip(shape),
          ) {
            Box(
              Modifier.align(corner)
                .wrapContentSize(corner, unbounded = true)
                .size(with(density) { local.width.toDp() }, with(density) { local.height.toDp() })
                .graphicsLayer { alpha = (1f - drop.value / 0.5f).coerceIn(0f, 1f) },
              contentAlignment = Alignment.Center,
            ) { icon() }
            Box(
              Modifier.align(corner)
                .wrapContentWidth(if (fromRight) Alignment.End else Alignment.Start, unbounded = true)
                .wrapContentHeight(if (fromBelow) Alignment.Top else Alignment.Bottom, unbounded = true)
                .width(menuWidth)
                .onSizeChanged { full = it.height }
                .graphicsLayer { alpha = ((tall.value - 0.25f) / 0.5f).coerceIn(0f, 1f) },
            ) {
              Column(Modifier.heightIn(max = availableMaxH).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(6.dp))
                body(shut) { action -> if (chosenAction == null) { chosenAction = action; onDismiss() } }
                Spacer(Modifier.height(6.dp))
              }
            }
          }
        }
      },
      modifier = Modifier.fillMaxSize().onGloballyPositioned { MorphAnchors.shownAt(anchor) },
    ) { measurables, constraints ->
      val origin = host.originInWindow
      val root = view.rootView
      val rootImeBottom =
        ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
      val bottomInsetPx = max(bottomBars.getBottom(this), rootImeBottom)
      val barTop = (bars.getTop(this) - origin.y).coerceAtLeast(0f)
      val barBottom = (bottomInsetPx - (root.height - origin.y - constraints.maxHeight)).coerceAtLeast(0f)
      val barLeft = (bars.getLeft(this, layoutDirection) - origin.x).coerceAtLeast(0f)
      val barRight = (bars.getRight(this, layoutDirection) - (root.width - origin.x - constraints.maxWidth)).coerceAtLeast(0f)
      val menuH = (if (full > 0) full else local.height.roundToInt()).toFloat()
      val right = local.center.x > constraints.maxWidth / 2f
      fromRight = right
      val spaceBelow = (constraints.maxHeight - barBottom - margin - local.top).coerceAtLeast(0f)
      val spaceAbove = (local.bottom - barTop - margin).coerceAtLeast(0f)
      val below = (local.top + menuH < constraints.maxHeight - barBottom - margin) || (spaceAbove < menuH && spaceBelow > spaceAbove)
      fromBelow = below
      val minX = barLeft + margin
      val maxX = (constraints.maxWidth - barRight - widthPx - margin).coerceAtLeast(minX)
      val minY = barTop + margin
      val maxY = (constraints.maxHeight - barBottom - menuH - margin).coerceAtLeast(minY)
      val endL = (if (right) local.right - widthPx else local.left).coerceIn(minX, maxX)
      val endT = (if (below) local.top else local.bottom - menuH).coerceIn(minY, maxY)
      val gw = wide.value.coerceAtMost(MaxSwingWide)
      val gh = if (full > 0) tall.value else 0f
      val l = lerp(local.left, endL, gw)
      val r = lerp(local.right, endL + widthPx, gw)
      val hNow = morphHeight(local.height, menuH, peekPx, drop.value, gh)
      val edgeT = lerp(local.top, endT, gw)
      val edgeB = lerp(local.bottom, endT + menuH, gw)
      val t = if (below) edgeT else edgeB - hNow
      val b = if (below) edgeT + hNow else edgeB
      val w = max(1, (r - l).roundToInt())
      val h = max(1, (b - t).roundToInt())
      val placeable = measurables.first().measure(Constraints.fixed(w, h))
      layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place(IntOffset(l.roundToInt(), t.roundToInt()))
      }
    }
  }
}

@Composable
internal fun MorphMenuRow(item: GlassMenuItem, shut: Boolean, close: () -> Unit) {
  var chosen by remember { mutableStateOf(false) }
  val ink = LocalGlassInk.current
  Row(
    Modifier.fillMaxWidth().pressable({
      if (!chosen && !shut) {
        chosen = true
        close()
      }
    }).padding(horizontal = 16.dp, vertical = 11.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(Modifier.weight(1f)) {
      Text(
        item.label,
        style = GalleryType.BodyStrong,
        color = if (item.destructive) Candy.Tomato else ink,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (!item.subtitle.isNullOrBlank()) {
        Text(
          item.subtitle,
          style = GalleryType.Caption,
          color = ink.copy(alpha = 0.65f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    if (item.icon != null) {
      Icon(
        item.icon,
        null,
        tint = if (item.destructive) Candy.Tomato else ink.copy(alpha = 0.72f),
        modifier = Modifier.padding(start = 10.dp).size(19.dp),
      )
    }
  }
}

private fun morphHeight(button: Float, menu: Float, peek: Float, drop: Float, tall: Float): Float {
  val first = min(peek, (menu - button).coerceAtLeast(0f))
  val rest = (menu - button - first).coerceAtLeast(0f)
  return (button + first * drop.coerceAtMost(1.15f) + rest * tall.coerceAtMost(MaxSwingTall)).coerceAtLeast(1f)
}

private const val MaxSwingWide = 1.05f
private const val MaxSwingTall = 1.12f
