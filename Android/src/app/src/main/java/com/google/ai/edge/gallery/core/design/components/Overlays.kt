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
 * Liquid glass overlays (dialogs, sheets, menus, fields, toasts) adapted from Artefact
 * (https://github.com/Chethan616/Artefact, MIT OR Apache-2.0, Copyright 2026 Chethan616).
 */

package com.google.ai.edge.gallery.core.design.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.google.ai.edge.gallery.core.design.glass.GlassOverlay
import com.google.ai.edge.gallery.core.design.glass.GlassStyle
import com.google.ai.edge.gallery.core.design.glass.LocalGlassOverlayHost
import com.google.ai.edge.gallery.core.design.glass.LocalMenuInk
import com.google.ai.edge.gallery.core.design.glass.LocalOverlayPresence
import com.google.ai.edge.gallery.core.design.glass.adaptiveInk
import com.google.ai.edge.gallery.core.design.glass.rememberGlassLuminance
import com.google.ai.edge.gallery.core.design.glass.rememberOverlayAppear
import com.google.ai.edge.gallery.core.design.motion.Fluid
import com.google.ai.edge.gallery.core.design.motion.GlassMotion
import com.google.ai.edge.gallery.core.design.motion.MotionSettings
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType
import com.kyant.backdrop.floatingLayer
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
private fun BoxScope.Scrim(appear: Animatable<Float, AnimationVector1D>, onDismiss: () -> Unit) {
  val c = glassColors
  Box(
    Modifier.fillMaxSize()
      .graphicsLayer { alpha = appear.value.coerceIn(0f, 1f) }
      .background(Color.Black.copy(alpha = if (c.dark) 0.45f else 0.24f))
      .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
  )
}

@Composable
fun GlassDialog(
  onDismiss: () -> Unit,
  title: String?,
  confirm: String? = null,
  onConfirm: () -> Unit = {},
  dismiss: String? = null,
  onDismissButton: (() -> Unit)? = null,
  confirmEnabled: Boolean = true,
  destructive: Boolean = false,
  icon: ImageVector? = null,
  accent: Color = Candy.Sky,
  dismissible: Boolean = true,
  centered: Boolean = false,
  content: @Composable ColumnScope.() -> Unit = {},
) {
  val c = glassColors
  val tint = if (destructive) Candy.Tomato else accent
  GlassOverlay(animatesOut = true) {
    val leaving = LocalOverlayPresence.current?.leaving == true
    BackHandler(enabled = dismissible && !leaving, onBack = onDismiss)
    val appear = rememberOverlayAppear(enter = GlassMotion.morph(), exit = Fluid.dismiss())
    Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.Center) {
      Scrim(appear) { if (dismissible) onDismiss() }
      GlassCard(
        Modifier
          .padding(24.dp)
          .widthIn(max = 400.dp)
          .fillMaxWidth()
          .floatingLayer {
            val s = lerp(0.92f, 1f, appear.value)
            scaleX = s
            scaleY = s
            alpha = appear.value.coerceIn(0f, 1f)
          }
          .clickable(remember { MutableInteractionSource() }, null) {},
        shape = RoundedRectangle(32.dp),
        wash = c.frost,
        blur = 16.dp,
      ) {
        Column(
          Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp),
          horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        ) {
          if (icon != null) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
              Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(14.dp))
          }
          if (title != null) {
            Text(
              title,
              style = if (centered) GalleryType.Title.copy(fontSize = 19.sp, lineHeight = 24.sp) else GalleryType.Headline,
              color = c.ink,
              textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            )
            Spacer(Modifier.height(10.dp))
          }
          Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) { content() }
          if (confirm != null || dismiss != null) {
            Spacer(Modifier.height(20.dp))
            Row(
              Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp, if (centered) Alignment.CenterHorizontally else Alignment.End),
            ) {
              val each = if (centered) Modifier.weight(1f) else Modifier
              if (dismiss != null) {
                GlassPill(onClick = { (onDismissButton ?: onDismiss)() }, height = 46.dp, modifier = each) {
                  Text(dismiss, style = GalleryType.BodyStrong, color = c.ink, maxLines = 1)
                }
              }
              if (confirm != null) {
                GlassPill(
                  onClick = { if (confirmEnabled) onConfirm() },
                  tint = if (confirmEnabled) tint else c.inkFaint,
                  height = 46.dp,
                  modifier = each,
                ) { Text(confirm, style = GalleryType.BodyStrong, color = Color.White, maxLines = 1) }
              }
            }
          }
        }
      }
    }
  }
}

private val SheetEnter = spring<Float>(dampingRatio = 0.78f, stiffness = 400f, visibilityThreshold = 0.001f)
private val SheetExit = spring<Float>(dampingRatio = 1f, stiffness = 620f, visibilityThreshold = 0.001f)

@Composable
fun GlassSheet(
  onDismiss: () -> Unit,
  title: String? = null,
  subtitle: String? = null,
  maxHeightFraction: Float = 0.86f,
  content: @Composable ColumnScope.() -> Unit,
) {
  val c = glassColors
  GlassOverlay(animatesOut = true) {
    val leaving = LocalOverlayPresence.current?.leaving == true
    BackHandler(enabled = !leaving, onBack = onDismiss)
    val appear = rememberOverlayAppear(enter = SheetEnter, exit = SheetExit)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    var height by remember { mutableStateOf(0f) }
    val overlayHeight = LocalGlassOverlayHost.current?.size?.height ?: 0
    Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.BottomCenter) {
      Scrim(appear, onDismiss)
      val maxH = with(density) { if (overlayHeight > 0) (overlayHeight * maxHeightFraction).toDp() else 640.dp }
      GlassCard(
        Modifier
          .navigationBarsPadding()
          .padding(horizontal = 10.dp, vertical = 10.dp)
          .widthIn(max = 580.dp)
          .fillMaxWidth()
          .heightIn(max = maxH)
          .onSizeChanged { height = it.height.toFloat() }
          .floatingLayer {
            val travel = size.height + 48.dp.toPx()
            translationY = (1f - appear.value) * travel + drag.value
            val up = (-drag.value).coerceAtLeast(0f)
            if (up > 0f && size.height > 0f) {
              scaleY = 1f + up / size.height * 0.4f
              transformOrigin = TransformOrigin(0.5f, 1f)
            }
          }
          .clickable(remember { MutableInteractionSource() }, null) {}
          .draggable(
            rememberDraggableState { delta ->
              val cur = drag.value
              val next = if (cur + delta < 0f) cur + delta * 0.25f else cur + delta
              scope.launch { drag.snapTo(next.coerceAtLeast(-with(density) { 40.dp.toPx() })) }
            },
            Orientation.Vertical,
            enabled = !leaving,
            onDragStopped = { velocity ->
              if (drag.value > height * 0.3f || velocity > 1200f) {
                val travel = height + with(density) { 48.dp.toPx() }
                val from = 1f - (drag.value / travel).coerceIn(0f, 1f)
                drag.snapTo(0f)
                appear.snapTo(from)
                onDismiss()
              } else {
                drag.animateTo(0f, spring(MotionSettings.damping(0.7f), MotionSettings.stiffness(500f)), velocity)
              }
            },
          ),
        shape = RoundedRectangle(32.dp),
        wash = c.frost,
        blur = 16.dp,
      ) {
        Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
          Box(Modifier.size(36.dp, 5.dp).clip(Capsule).background(c.inkFaint.copy(alpha = 0.35f)))
        }
        if (title != null) {
          Text(title, style = GalleryType.Headline, color = c.ink, modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp))
          if (subtitle != null) {
            Text(
              subtitle,
              style = GalleryType.Label,
              color = c.inkSoft,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 4.dp),
            )
          }
          Spacer(Modifier.height(10.dp))
        } else {
          Spacer(Modifier.height(8.dp))
        }
        content()
        Spacer(Modifier.height(12.dp))
      }
    }
  }
}

@Composable
fun GlassRow(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  accent: Color = Candy.Sky,
  value: String? = null,
  destructive: Boolean = false,
  trailing: (@Composable () -> Unit)? = null,
) {
  val c = glassColors
  Row(
    modifier.fillMaxWidth().pressable(onClick).padding(horizontal = 18.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (icon != null) {
      val blob = if (destructive) Candy.Tomato else accent
      Box(
        Modifier.size(34.dp).clip(RoundedRectangle(11.dp)).background(blob.copy(alpha = if (c.dark) 0.26f else 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, null, tint = blob, modifier = Modifier.size(19.dp))
      }
      Spacer(Modifier.width(14.dp))
    }
    Text(
      label,
      style = GalleryType.BodyStrong,
      color = if (destructive) Candy.Tomato else c.ink,
      modifier = Modifier.weight(1f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    if (value != null) {
      Text(value, style = GalleryType.Label, color = c.inkSoft, modifier = Modifier.padding(start = 8.dp))
    }
    trailing?.invoke()
  }
}

data class GlassMenuItem(
  val label: String,
  val subtitle: String? = null,
  val icon: ImageVector? = null,
  val destructive: Boolean = false,
  val onClick: () -> Unit,
)

private const val MenuGoneMillis = 200L
private val MenuLightWash = Color.White.copy(alpha = 0.56f)
private val MenuDarkWash = Color(0xFF0A0B0F).copy(alpha = 0.78f)

@Composable
fun MenuCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedRectangle(24.dp),
  content: @Composable ColumnScope.() -> Unit,
) {
  val c = glassColors
  val luminance = rememberGlassLuminance()
  GlassCard(
    modifier,
    shape = shape,
    wash = if (c.dark) MenuDarkWash else MenuLightWash,
    blur = 16.dp,
    styleOverride = if (c.dark) GlassStyle.MenuFrostDark else GlassStyle.MenuFrost,
    luminance = luminance,
  ) {
    val ink = adaptiveInk(luminance, c.ink)
    CompositionLocalProvider(LocalGlassInk provides ink, LocalMenuInk provides ink) { content() }
  }
}

@Composable
fun GlassMenu(
  anchor: Rect,
  onDismiss: () -> Unit,
  items: List<GlassMenuItem>,
  menuWidth: Dp = 248.dp,
) {
  if (isMorphAnchor(anchor, LocalDensity.current)) {
    MorphButtonSurface(anchor, open = true, onDismiss = onDismiss, onGone = {}, items = items, menuWidth = menuWidth)
    return
  }
  var closing by remember { mutableStateOf(false) }
  GlassMenuLayout(anchor, onDismiss, width = menuWidth) {
    Spacer(Modifier.height(6.dp))
    items.forEach { item ->
      Row(
        Modifier.fillMaxWidth().pressable({
          if (!closing) {
            closing = true
            onDismiss()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ item.onClick() }, MenuGoneMillis)
          }
        }).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(Modifier.weight(1f)) {
          Text(
            item.label,
            style = GalleryType.BodyStrong,
            color = if (item.destructive) Candy.Tomato else LocalGlassInk.current,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (!item.subtitle.isNullOrBlank()) {
            Text(
              item.subtitle,
              style = GalleryType.Caption,
              color = LocalGlassInk.current.copy(alpha = 0.65f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        if (item.icon != null) {
          Spacer(Modifier.width(10.dp))
          Icon(
            item.icon,
            null,
            tint = if (item.destructive) Candy.Tomato else LocalGlassInk.current.copy(alpha = 0.72f),
            modifier = Modifier.size(19.dp),
          )
        }
      }
    }
    Spacer(Modifier.height(6.dp))
  }
}

@Composable
fun GlassMenuLayout(
  anchor: Rect,
  onDismiss: () -> Unit,
  width: Dp = 248.dp,
  maxHeight: Dp = 480.dp,
  content: @Composable ColumnScope.() -> Unit,
) {
  GlassOverlay(animatesOut = true) {
    val leaving = LocalOverlayPresence.current?.leaving == true
    BackHandler(enabled = !leaving, onBack = onDismiss)
    val host = LocalGlassOverlayHost.current ?: return@GlassOverlay
    val bars = WindowInsets.systemBars
    val bottomBars = WindowInsets.systemBars.union(WindowInsets.ime)
    val view = LocalView.current
    val appear = rememberOverlayAppear(enter = GlassMotion.unfold(), exit = GlassMotion.fold())
    val lag = remember { Animatable(0f) }
    LaunchedEffect(leaving) {
      if (!leaving) lag.animateTo(1f, GlassMotion.unfoldLag()) else lag.animateTo(0f, GlassMotion.fold())
    }
    Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
    val local = Rect(host.windowToLocal(anchor.topLeft), host.windowToLocal(anchor.bottomRight))
    val margin = with(LocalDensity.current) { 12.dp.toPx() }
    var fromRight by remember { mutableStateOf(true) }
    var fromBelow by remember { mutableStateOf(true) }
    Layout(
      content = {
        MenuCard(
          Modifier
            .width(width)
            .floatingLayer {
              val p = appear.value
              scaleX = lerp(0.5f, 1f, p).coerceAtLeast(0.2f)
              scaleY = lerp(0.35f, 1f, lag.value).coerceAtLeast(0.2f)
              transformOrigin = TransformOrigin(if (fromRight) 1f else 0f, if (fromBelow) 0f else 1f)
              alpha = (p * 1.6f).coerceIn(0f, 1f)
            },
        ) {
          Column(Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState()), content = content)
        }
      },
      modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
      val p = measurables.first().measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight))
      val origin = host.originInWindow
      val root = view.rootView
      val rootImeBottom =
        ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
      val bottomInsetPx = kotlin.math.max(bottomBars.getBottom(this), rootImeBottom)
      val barTop = (bars.getTop(this) - origin.y).coerceAtLeast(0f)
      val barBottom = (bottomInsetPx - (root.height - origin.y - constraints.maxHeight)).coerceAtLeast(0f)
      val barLeft = (bars.getLeft(this, layoutDirection) - origin.x).coerceAtLeast(0f)
      val barRight = (bars.getRight(this, layoutDirection) - (root.width - origin.x - constraints.maxWidth)).coerceAtLeast(0f)
      layout(constraints.maxWidth, constraints.maxHeight) {
        val right = local.center.x > constraints.maxWidth / 2f
        fromRight = right
        val x = if (right) (local.right - p.width) else local.left
        val below = local.bottom + 6f + p.height < constraints.maxHeight - barBottom - margin
        fromBelow = below
        val y = if (below) local.bottom + 6f else local.top - 6f - p.height
        val minX = barLeft + margin
        val maxX = (constraints.maxWidth - barRight - p.width - margin).coerceAtLeast(minX)
        val minY = barTop + margin
        val maxY = (constraints.maxHeight - barBottom - p.height - margin).coerceAtLeast(minY)
        p.place(IntOffset(x.coerceIn(minX, maxX).roundToInt(), y.coerceIn(minY, maxY).roundToInt()))
      }
    }
  }
}

@Composable
fun GlassTextField(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
  password: Boolean = false,
  error: Boolean = false,
  focus: FocusRequester? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  singleLine: Boolean = true,
) {
  val c = glassColors
  BasicTextField(
    value = value,
    onValueChange = onValueChange,
    singleLine = singleLine,
    textStyle = GalleryType.Body.copy(color = c.ink),
    cursorBrush = SolidColor(Candy.Sky),
    visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    modifier = modifier.fillMaxWidth().then(if (focus != null) Modifier.focusRequester(focus) else Modifier),
    decorationBox = { inner ->
      Box(
        Modifier.fillMaxWidth().clip(RoundedRectangle(16.dp))
          .background(if (c.dark) Color.White.copy(alpha = 0.08f) else c.ink.copy(alpha = 0.05f))
          .then(if (error) Modifier.background(Candy.Tomato.copy(alpha = 0.12f)) else Modifier)
          .padding(horizontal = 16.dp, vertical = 14.dp),
      ) {
        if (value.isEmpty()) {
          Text(placeholder, style = GalleryType.Body, color = if (error) Candy.Tomato else c.inkFaint)
        }
        inner()
      }
    },
  )
}

@Composable
fun GlassToast(
  text: String,
  action: String? = null,
  onAction: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val c = glassColors
  val appear = remember { Animatable(0f) }
  LaunchedEffect(Unit) { appear.animateTo(1f, GlassMotion.morph()) }
  val luminance = rememberGlassLuminance(enabled = true)
  val ink = adaptiveInk(luminance, c.ink)
  Row(
    modifier
      .floatingLayer {
        val p = appear.value
        translationY = (1f - p) * 24.dp.toPx()
        alpha = p.coerceIn(0f, 1f)
      }
      .widthIn(max = 520.dp)
      .glass(Capsule, c.frost, blur = 12.dp, lensHeight = 12.dp, lensAmount = 24.dp, luminance = luminance)
      .padding(start = 20.dp, end = if (action != null) 6.dp else 20.dp, top = 6.dp, bottom = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text,
      style = GalleryType.Label,
      color = ink,
      modifier = Modifier.weight(1f, fill = false).padding(vertical = 8.dp),
      maxLines = 3,
    )
    if (action != null) {
      Spacer(Modifier.width(10.dp))
      GlassPill(onClick = onAction, tint = Candy.Sky, height = 38.dp, horizontalPadding = 14.dp) {
        Text(action, style = GalleryType.Label, color = Color.White)
      }
    }
  }
}
