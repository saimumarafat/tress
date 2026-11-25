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
 * Liquid metaball menu morphing shader and surface adapted from Artefact
 * (https://github.com/Chethan616/Artefact, MIT OR Apache-2.0, Copyright 2026 Chethan616).
 */

package com.google.ai.edge.gallery.core.design.components

import android.graphics.RuntimeShader
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.google.ai.edge.gallery.core.design.glass.GlassOverlay
import com.google.ai.edge.gallery.core.design.glass.GlassSettings
import com.google.ai.edge.gallery.core.design.glass.GlassStyle
import com.google.ai.edge.gallery.core.design.glass.LocalBackdrop
import com.google.ai.edge.gallery.core.design.glass.LocalGlassOverlayHost
import com.google.ai.edge.gallery.core.design.glass.LocalMenuInk
import com.google.ai.edge.gallery.core.design.glass.LocalOverlayPresence
import com.google.ai.edge.gallery.core.design.glass.adaptiveBase
import com.google.ai.edge.gallery.core.design.glass.adaptiveGlassEffects
import com.google.ai.edge.gallery.core.design.glass.adaptiveInk
import com.google.ai.edge.gallery.core.design.glass.adaptiveWash
import com.google.ai.edge.gallery.core.design.glass.glassEffects
import com.google.ai.edge.gallery.core.design.glass.glassHighlight
import com.google.ai.edge.gallery.core.design.glass.glassTint
import com.google.ai.edge.gallery.core.design.glass.rememberGlassLuminance
import com.google.ai.edge.gallery.core.design.motion.GlassMotion
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.runtimeShaderEffect
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

private const val LiquidSdf = """
uniform float4 circle;
uniform float4 rect;
uniform float rectRadius;
uniform float k;

float sdCircle(float2 p, float2 c, float r) { return length(p - c) - r; }

float sdRRect(float2 p, float4 b, float r) {
    float2 c = (b.xy + b.zw) * 0.5;
    float2 hs = max((b.zw - b.xy) * 0.5, float2(0.0));
    float rr = min(r, min(hs.x, hs.y));
    float2 q = abs(p - c) - (hs - float2(rr));
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - rr;
}

float smin(float a, float b, float kk) {
    if (kk <= 0.001) return min(a, b);
    float h = clamp(0.5 + 0.5 * (b - a) / kk, 0.0, 1.0);
    return mix(b, a, h) - kk * h * (1.0 - h);
}

float sdLiquid(float2 p) {
    return smin(sdCircle(p, circle.xy, circle.z), sdRRect(p, rect, rectRadius), k);
}

float2 gradLiquid(float2 p) {
    float e = 1.0;
    float2 g = float2(sdLiquid(p + float2(e, 0.0)) - sdLiquid(p - float2(e, 0.0)),
                      sdLiquid(p + float2(0.0, e)) - sdLiquid(p - float2(0.0, e)));
    float l = length(g);
    return l > 0.0001 ? g / l : float2(0.0, -1.0);
}
"""

private const val LiquidRefractionShader = """
uniform shader content;
uniform float2 offset;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depth;
uniform float dispersion;
$LiquidSdf

float circleMap(float x) { return 1.0 - sqrt(max(1.0 - x * x, 0.0)); }

half4 main(float2 coord) {
    float2 p = coord + offset;
    float sd = sdLiquid(p);
    if (sd > 1.0) return half4(0.0);
    float cover = clamp(0.5 - sd, 0.0, 1.0);
    if (-sd >= refractionHeight) return content.eval(coord) * cover;
    float d = circleMap(1.0 - (-min(sd, 0.0)) / refractionHeight) * refractionAmount;
    float dc = sdCircle(p, circle.xy, circle.z);
    float dr = sdRRect(p, rect, rectRadius);
    float w = clamp(0.5 + 0.5 * (dr - dc) / max(k, 1.0), 0.0, 1.0);
    float2 rc = (rect.xy + rect.zw) * 0.5;
    float2 rh = max((rect.zw - rect.xy) * 0.5, float2(1.0));
    float2 center = mix(rc, circle.xy, w);
    float2 halfSize = mix(rh, float2(max(circle.z, 1.0)), w);
    float2 centered = p - center;
    float2 g = gradLiquid(p);
    if (depth > 0.5 && length(centered) > 0.001) g = normalize(g + normalize(centered));
    float2 refracted = coord - d * g;
    if (dispersion > 0.5) {
        float intensity = clamp((centered.x * centered.y) / max(halfSize.x * halfSize.y, 1.0), -1.0, 1.0);
        float2 dc2 = -d * g * intensity;
        if (dot(dc2, dc2) >= 0.25) {
            half4 r = content.eval(refracted + dc2 * 0.4286);
            half4 gg = content.eval(refracted + dc2 * 0.0952);
            half4 bl = content.eval(refracted - dc2 * 0.6667);
            return half4(r.r, gg.g, bl.b, (r.a + gg.a + bl.a) / 3.0) * cover;
        }
    }
    return content.eval(refracted) * cover;
}
"""

private const val LiquidSurfaceShader = """
uniform float4 wash;
uniform float rimAlpha;
$LiquidSdf

half4 main(float2 p) {
    float sd = sdLiquid(p);
    float cover = clamp(0.5 - sd, 0.0, 1.0);
    if (cover <= 0.0) return half4(0.0);
    float2 g = gradLiquid(p);
    float2 light = normalize(float2(-0.55, -1.0));
    float facing = max(dot(g, light), 0.0) + 0.45 * max(dot(-g, light), 0.0);
    float rim = (1.0 - smoothstep(0.0, 1.8, -sd)) * rimAlpha * (0.35 + 0.65 * facing);
    float a = wash.a * cover;
    float r = rim * cover;
    half3 col = half3(wash.rgb) * a + half3(1.0) * r;
    return half4(col, a + r * (1.0 - a));
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
internal fun LiquidMorphSurface(
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
    val tint = MorphAnchors.tintAt(anchor) ?: Color.Unspecified
    BackHandler(enabled = !shut, onBack = onDismiss)
    val host = LocalGlassOverlayHost.current ?: return@GlassOverlay
    val bars = WindowInsets.systemBars
    val bottomBars = WindowInsets.systemBars.union(WindowInsets.ime)
    val view = LocalView.current
    val density = LocalDensity.current

    val p = remember { Animatable(0f) }
    var chosenAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val fade = remember { Animatable(1f) }
    var full by remember { mutableIntStateOf(0) }
    LaunchedEffect(shut) {
      if (!shut) {
        snapshotFlow { full > 0 }.first { it }
        p.animateTo(1f, GlassMotion.morphOpen())
      } else {
        if (!ghost && MorphAnchors.glyphAt(anchor) == null) {
          fade.animateTo(0f, tween(130))
        } else if (chosenAction != null) {
          p.animateTo(0f, GlassMotion.morphChosen())
        } else {
          p.animateTo(0f, GlassMotion.morphClose())
        }
        if (ghost) fade.animateTo(0f, tween(90))
        gone()
        presence?.done()
        val action = chosenAction
        chosenAction = null
        if (action != null) Handler(Looper.getMainLooper()).post { action() }
      }
    }

    fun liquidIn(): Float {
      if (full <= 0) return 0f
      val v = (p.value / 0.12f).coerceIn(0f, 1f)
      return v * v * (3f - 2f * v)
    }

    fun ease(x: Float): Float {
      val v = x.coerceIn(0f, 1f)
      return v * v * (3f - 2f * v)
    }

    Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
    val local = Rect(host.windowToLocal(anchor.topLeft), host.windowToLocal(anchor.bottomRight))
    val px = { d: Dp -> with(density) { d.toPx() } }
    val widthPx = px(menuWidth)
    val menuH = if (full > 0) full.toFloat() else local.height * 4f

    val origin = host.originInWindow
    val root = view.rootView
    val rootImeBottom =
      ViewCompat.getRootWindowInsets(view)?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
    val bottomInsetPx = max(bottomBars.getBottom(density), rootImeBottom)
    val barTop = (bars.getTop(density) - origin.y).coerceAtLeast(0f)
    val barBottom = (bottomInsetPx - (root.height - origin.y - view.height)).coerceAtLeast(0f)
    val margin = px(12.dp)
    val screenW = view.width.toFloat()
    val screenH = view.height.toFloat()
    val spaceBelow = (screenH - barBottom - margin - local.top).coerceAtLeast(0f)
    val spaceAbove = (local.bottom - barTop - margin).coerceAtLeast(0f)
    val fromRight = local.center.x > screenW / 2f
    val below = (local.top + menuH < screenH - barBottom - margin) || (spaceAbove < menuH && spaceBelow > spaceAbove)
    val availableMaxH = with(density) { (if (below) spaceBelow else spaceAbove).toDp() }
      .coerceAtLeast(with(density) { local.height.toDp() })
      .coerceAtMost(maxHeight)
    val endL = (if (fromRight) local.right - widthPx else local.left)
      .coerceIn(margin, (screenW - widthPx - margin).coerceAtLeast(margin))
    val endT = (if (below) local.top else local.bottom - menuH)
      .coerceIn(barTop + margin, (screenH - barBottom - menuH - margin).coerceAtLeast(barTop + margin))
    val endR = endL + widthPx
    val endB = endT + menuH
    val r0 = min(local.width, local.height) / 2f
    val peek = (LocalContext.current.resources.displayMetrics.ydpi / 2.54f * 1.6f)
      .coerceAtMost((menuH - local.height).coerceAtLeast(0f))
    val swellPx = menuH * JellyStretch * JellyMax + px(6.dp)
    val radius24 = px(24.dp)
    val neckPx = px(22.dp)
    val opening = !shut

    val pad = px(28.dp)
    val stage = Rect(
      min(local.left, endL) - pad,
      min(local.top, endT) - pad - swellPx,
      max(local.right, endR) + pad,
      max(local.bottom, endB) + pad + swellPx,
    )

    class Geo(val menu: Rect, val radius: Float, val k: Float, val circleR: Float, val motion: Float)
    val arrived = remember { booleanArrayOf(false) }

    fun geo(): Geo {
      val raw = p.value
      if (opening && raw >= 1f) arrived[0] = true
      val pv = if (opening && arrived[0]) 1f else raw.coerceAtMost(1f)
      val jelly = if (full > 0 && (raw > 1f || (opening && arrived[0]))) (raw - 1f).coerceIn(-JellyMax, JellyMax) else 0f
      val d = ease(pv / 0.38f)
      val sp = ease((pv - 0.16f) / 0.46f)
      val po = if (full > 0) ease((pv - 0.36f) / 0.64f) else 0f
      val dc = d.coerceIn(0f, 1f)
      val dropW = lerp(local.width * 0.5f, local.width * 0.86f, dc)
      val rl = lerp(local.center.x - dropW / 2f, endL, sp)
      val rr = lerp(local.center.x + dropW / 2f, endR, sp)
      val path = if (below) {
        val top = lerp(local.center.y - local.height * 0.25f, endT, sp)
        val bottom = lerp(local.center.y + local.height * 0.25f, local.bottom + peek, d) + (endB - local.bottom - peek) * po
        Rect(rl, min(top, bottom - 1f), rr, max(bottom, top + 1f))
      } else {
        val bottom = lerp(local.center.y + local.height * 0.25f, endB, sp)
        val top = lerp(local.center.y - local.height * 0.25f, local.top - peek, d) - (local.top - peek - endT) * po
        Rect(rl, min(top, bottom - 1f), rr, max(bottom, top + 1f))
      }
      val menu = if (jelly == 0f) path else {
        val h = path.height * (1f + JellyStretch * jelly)
        val w = path.width * (1f - JellySqueeze * jelly)
        val l = if (fromRight) path.right - w else path.left
        val t = if (below) path.top else path.bottom - h
        Rect(l, t, l + w, t + h)
      }
      val radius = min(min(menu.width, menu.height) / 2f, lerp(r0, radius24, po.coerceIn(0f, 1f)))
      val apart = (d / 0.35f).coerceIn(0f, 1f)
      val k = neckPx * apart * apart * (3f - 2f * apart) * (1f - po.coerceIn(0f, 1f))
      val circleR = r0 * (1f - po.coerceIn(0f, 1f)).coerceAtLeast(0f)
      val motion = kotlin.math.sin(Math.PI.toFloat() * ((d + sp + po) / 3f).coerceIn(0f, 1f))
      return Geo(menu, radius, k, circleR, motion)
    }

    val luminance = rememberGlassLuminance(enabled = true)
    val adaptiveOn = GlassSettings.adaptive
    val around = if (adaptiveOn) MorphAnchors.surroundAt(anchor) else null
    val fallbackLum = if (c.dark) 0.14f else 0.86f
    fun aroundLum(): Float = around?.value ?: luminance?.value ?: fallbackLum
    val ink = adaptiveInk(luminance, c.ink)
    val style = GlassSettings.effectiveStyle
    val dark = c.dark
    val surface = remember { RuntimeShader(LiquidSurfaceShader) }
    val rimAlpha = (if (dark) 0.32f else 0.6f) * style.highlight.coerceIn(0f, 1f)
    fun washIn() = ((p.value - 0.08f) / 0.4f).coerceIn(0f, 1f)
    val frame = remember { intArrayOf(0) }

    fun RuntimeShader.shapeUniforms(g: Geo) {
      setFloatUniform("circle", local.center.x - stage.left, local.center.y - stage.top, g.circleR, 0f)
      setFloatUniform("rect", g.menu.left - stage.left, g.menu.top - stage.top, g.menu.right - stage.left, g.menu.bottom - stage.top)
      setFloatUniform("rectRadius", g.radius)
      setFloatUniform("k", g.k)
    }

    Box(
      Modifier
        .offset { IntOffset(stage.left.roundToInt(), stage.top.roundToInt()) }
        .size(with(density) { stage.width.toDp() }, with(density) { stage.height.toDp() })
        .graphicsLayer { alpha = fade.value * liquidIn() }
        .drawBackdrop(
          backdrop = LocalBackdrop.current,
          shape = { RectangleShape },
          effects = {
            val g = geo()
            morphEffects(
              if (adaptiveOn) style.adaptiveBase else style,
              luminance?.value ?: if (dark) 0.14f else 0.86f,
              dark,
              washIn(),
              g.motion,
              px(1.dp),
              if (adaptiveOn) aroundLum() else null,
            )
            runtimeShaderEffect("LiquidMorphRefraction", LiquidRefractionShader, "content") {
              setFloatUniform("offset", -padding, -padding)
              setFloatUniform("refractionHeight", px((12f + 2f * g.motion).dp) * style.refractionHeight)
              setFloatUniform("refractionAmount", px((24f + 4f * g.motion).dp) * style.refractionAmount)
              setFloatUniform("depth", if (style.depthEffect) 1f else 0f)
              setFloatUniform("dispersion", if (style.chromaticAberration) 1f else 0f)
              shapeUniforms(g)
            }
          },
          highlight = null,
          shadow = null,
          onDrawBackdrop = { drawIt ->
            val rec = luminance?.recorder
            if (rec != null && frame[0]++ % 6 == 0) rec(drawIt) else drawIt()
          },
          onDrawSurface = {
            val g = geo()
            val w = washIn()
            val lum = luminance?.value ?: if (dark) 0.14f else 0.86f
            val menuWash = adaptiveWash(lum).let { it.copy(alpha = (it.alpha * 1.7f).coerceAtMost(0.55f)) }.glassTint(style)
            val plain = menuWash.let { it.copy(alpha = it.alpha * w) }
            val wash = when {
              tint.isSpecified -> lerp(tint.copy(alpha = 0.75f), plain, w)
              adaptiveOn -> lerp(adaptiveWash(aroundLum()).let { it.copy(alpha = it.alpha * 0.7f) }, menuWash, w)
              else -> plain
            }
            surface.setFloatUniform("wash", wash.red, wash.green, wash.blue, wash.alpha)
            surface.setFloatUniform("rimAlpha", rimAlpha)
            surface.shapeUniforms(g)
            drawRect(ShaderBrush(surface))
          },
        ),
    )

    Box(
      Modifier
        .offset { IntOffset(local.left.roundToInt(), local.top.roundToInt()) }
        .size(with(density) { local.width.toDp() }, with(density) { local.height.toDp() })
        .onGloballyPositioned { MorphAnchors.shownAt(anchor) }
        .graphicsLayer { alpha = fade.value * (1f - liquidIn()) }
        .drawBackdrop(
          backdrop = LocalBackdrop.current,
          shape = { CircleShape },
          effects = {
            if (adaptiveOn) adaptiveGlassEffects(style.adaptiveBase, aroundLum(), px(2.dp), px(12.dp), px(24.dp))
            else glassEffects(style, px(2.dp), px(12.dp), px(24.dp))
          },
          highlight = { glassHighlight(if (adaptiveOn) style.adaptiveBase else style) },
          shadow = null,
          onDrawSurface = {
            if (tint.isSpecified) {
              drawRect(tint, blendMode = BlendMode.Hue)
              drawRect(tint.copy(alpha = 0.75f))
            } else if (adaptiveOn) {
              drawRect(adaptiveWash(aroundLum()).let { it.copy(alpha = it.alpha * 0.7f) })
            }
          },
        ),
    )

    CompositionLocalProvider(LocalGlassInk provides ink, LocalMenuInk provides ink) {
      Box(Modifier.fillMaxSize().graphicsLayer { alpha = fade.value }) {
        Box(
          Modifier
            .offset { IntOffset(local.left.roundToInt(), local.top.roundToInt()) }
            .size(with(density) { local.width.toDp() }, with(density) { local.height.toDp() })
            .graphicsLayer { alpha = (1f - p.value / 0.22f).coerceIn(0f, 1f) },
          contentAlignment = Alignment.Center,
        ) { icon() }

        Box(
          Modifier
            .offset { IntOffset(endL.roundToInt(), (endT - swellPx).roundToInt()) }
            .size(with(density) { widthPx.toDp() }, with(density) { (menuH + 2f * swellPx).toDp() })
            .graphicsLayer {
              val g = geo()
              val ox = endL
              val oy = endT - swellPx
              val r = Rect(g.menu.left - ox, g.menu.top - oy, g.menu.right - ox, g.menu.bottom - oy)
              shape = RectClip(r, g.radius)
              clip = true
            },
        ) {
          Box(
            Modifier
              .align(if (below) Alignment.TopStart else Alignment.BottomStart)
              .padding(
                top = if (below) with(density) { swellPx.toDp() } else 0.dp,
                bottom = if (below) 0.dp else with(density) { swellPx.toDp() },
              )
              .wrapContentHeight(if (below) Alignment.Top else Alignment.Bottom, unbounded = true)
              .width(menuWidth)
              .onSizeChanged { full = it.height }
              .graphicsLayer { alpha = ((p.value - 0.55f) / 0.35f).coerceIn(0f, 1f) },
          ) {
            Column(Modifier.heightIn(max = availableMaxH).verticalScroll(rememberScrollState())) {
              Spacer(Modifier.height(6.dp))
              body(shut) { action -> if (chosenAction == null) { chosenAction = action; onDismiss() } }
              Spacer(Modifier.height(6.dp))
            }
          }
        }
      }
    }
  }
}

private class RectClip(private val r: Rect, private val radius: Float) : Shape {
  override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: androidx.compose.ui.unit.Density) =
    Outline.Rounded(RoundRect(r, CornerRadius(radius)))
}

private const val JellyStretch = 0.08f
private const val JellySqueeze = 0.04f
private const val JellyMax = 0.14f

private fun signedLum(luminance: Float): Float =
  (luminance * 2f - 1f).let { sign(it) * it * it }

private fun BackdropEffectScope.morphEffects(
  style: GlassStyle,
  luminance: Float,
  dark: Boolean,
  open: Float,
  motion: Float,
  dp: Float,
  buttonLuminance: Float? = null,
) {
  val l = signedLum(luminance.coerceIn(0f, 1f))
  val o = open.coerceIn(0f, 1f)
  val menuBrightness = if (l > 0f) lerp(0.08f, 0.3f, l) else lerp(if (dark) -0.12f else 0.04f, -0.24f, -l)
  val b = buttonLuminance?.let { signedLum(it.coerceIn(0f, 1f)) }
  val buttonBrightness =
    if (b == null) style.brightness
    else (if (b > 0f) lerp(0.08f, 0.42f, b) else lerp(0.08f, -0.2f, -b)) + style.brightness
  val buttonContrast = if (b != null && b > 0f) lerp(1f, 0.25f, b) else 1f
  colorControls(
    brightness = lerp(buttonBrightness, menuBrightness, o),
    contrast = lerp(buttonContrast, if (l > 0f) lerp(1f, 0.5f, l) else 1f, o),
    saturation = lerp(style.saturation, 1.25f, o),
  )
  val buttonBlur = 2f * dp * style.blur * (if (b == null) 1f else if (b > 0f) lerp(1f, 1.5f, b) else lerp(1f, 0.6f, -b))
  val menuBlur = 22f * dp * (if (l > 0f) lerp(1f, 1.35f, l) else lerp(1f, 0.75f, -l))
  blur(lerp(buttonBlur, menuBlur, o) + 5f * dp * motion.coerceIn(0f, 1f))
}
