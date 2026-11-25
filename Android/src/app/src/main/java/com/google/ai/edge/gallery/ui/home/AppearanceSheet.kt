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

package com.google.ai.edge.gallery.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.core.design.components.Capsule
import com.google.ai.edge.gallery.core.design.components.GlassSheet
import com.google.ai.edge.gallery.core.design.components.GlassTab
import com.google.ai.edge.gallery.core.design.components.GlassTabs
import com.google.ai.edge.gallery.core.design.components.GlassToggle
import com.google.ai.edge.gallery.core.design.components.HeaderGlassButton
import com.google.ai.edge.gallery.core.design.components.RoundedRectangle
import com.google.ai.edge.gallery.core.design.components.WashPanel
import com.google.ai.edge.gallery.core.design.components.pressable
import com.google.ai.edge.gallery.core.design.components.wash
import com.google.ai.edge.gallery.core.design.glass.GlassPerformanceTier
import com.google.ai.edge.gallery.core.design.glass.GlassSettings
import com.google.ai.edge.gallery.core.design.glass.GlassStyle
import com.google.ai.edge.gallery.core.design.glass.WallpaperMode
import com.google.ai.edge.gallery.core.design.motion.MotionSettings
import com.google.ai.edge.gallery.core.design.motion.MotionStyle
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.theme.palette
import com.google.ai.edge.gallery.core.design.typography.GalleryType
import com.google.ai.edge.gallery.proto.Theme
import com.kyant.backdrop.throttleBackdrops

@Composable
fun AppearanceSettingsSheet(
  selectedTheme: Theme,
  onSelectTheme: (Theme) -> Unit,
  onDismiss: () -> Unit,
) {
  val c = glassColors
  val context = LocalContext.current
  val themeOptions = listOf(Theme.THEME_AUTO, Theme.THEME_LIGHT, Theme.THEME_DARK)
  val selectedIndex = themeOptions.indexOf(selectedTheme).coerceAtLeast(0)
  val scrollState = rememberScrollState()
  val cardShape = RoundedRectangle(24.dp)
  val rim = if (c.dark) Color(0xFF27272A) else Color(0xFFE4E4E7)

  GlassSheet(onDismiss = onDismiss) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 18.dp, top = 4.dp, bottom = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "Appearance",
        style =
          GalleryType.Hero.copy(
            fontSize = 32.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1.1).sp,
          ),
        color = c.hero,
      )
      HeaderGlassButton(
        onClick = onDismiss,
        label = stringResource(R.string.close),
        size = 38.dp,
      ) {
        Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = c.ink, modifier = Modifier.size(18.dp))
      }
    }

    Column(
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(max = 540.dp)
          .throttleBackdrops { scrollState.isScrollInProgress }
          .verticalScroll(scrollState)
          .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      WashPanel(shape = cardShape, modifier = Modifier.fillMaxWidth().border(1.dp, rim, cardShape)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Theme", style = GalleryType.CardTitle.copy(fontSize = 17.sp), color = c.ink)
          GlassTabs(
            tabs = listOf(
              GlassTab("Auto", accent = c.ink),
              GlassTab("Light", accent = c.ink),
              GlassTab("Dark", accent = c.ink),
            ),
            selected = selectedIndex,
            onSelect = { onSelectTheme(themeOptions[it]) },
            height = 44.dp,
          )
          Text("Background Color", style = GalleryType.BodyStrong, color = c.ink)
          BackgroundColorSwatchRow()
        }
      }

      WashPanel(shape = cardShape, modifier = Modifier.fillMaxWidth().border(1.dp, rim, cardShape)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Liquid Glass", style = GalleryType.CardTitle.copy(fontSize = 17.sp), color = c.ink)
          val tiers = GlassPerformanceTier.entries
          GlassTabs(
            tabs = listOf(
              GlassTab("Full", accent = c.ink),
              GlassTab("Balanced", accent = c.ink),
              GlassTab("Lite", accent = c.ink),
            ),
            selected = tiers.indexOf(GlassSettings.performanceTier).coerceAtLeast(0),
            onSelect = { GlassSettings.updatePerformanceTier(context, tiers[it]) },
            height = 44.dp,
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
              "Default" to GlassStyle.Default,
              "Frosted" to GlassStyle.Frosted,
              "Crystal" to GlassStyle.Crystal,
            ).forEach { (label, preset) ->
              val active = GlassSettings.style == preset
              Box(
                modifier =
                  Modifier.then(
                      if (active) Modifier.clip(Capsule).background(c.ink)
                      else Modifier.wash(shape = Capsule, tint = c.glass)
                    )
                    .pressable(onClick = { GlassSettings.update(context, preset) })
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  label,
                  style = GalleryType.Label.copy(fontWeight = FontWeight.Bold),
                  color = if (active) c.canvas else c.ink,
                )
              }
            }
          }
        }
      }

      WashPanel(shape = cardShape, modifier = Modifier.fillMaxWidth().border(1.dp, rim, cardShape)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Text("Controls", style = GalleryType.CardTitle.copy(fontSize = 17.sp), color = c.ink)
          SettingToggleRow("Adaptive frost", GlassSettings.adaptive) {
            GlassSettings.updateAdaptive(context, it)
          }
          SettingToggleRow("Ambient glow", GlassSettings.wallpaper == WallpaperMode.Aurora) {
            GlassSettings.updateWallpaper(context, if (it) WallpaperMode.Aurora else WallpaperMode.Clean)
          }
          SettingToggleRow("Haptics", GlassSettings.haptics) {
            GlassSettings.updateHaptics(context, it)
          }
          SettingToggleRow("Reduced motion", MotionSettings.reduceMotion) {
            MotionSettings.updateReduceMotion(context, it)
            if (it) MotionSettings.update(context, MotionStyle.Calm)
          }
        }
      }

      Spacer(Modifier.height(6.dp))
    }
  }
}

@Composable
private fun SettingToggleRow(
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  val c = glassColors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedRectangle(14.dp))
      .pressable(
        onClick = { onCheckedChange(!checked) },
        role = androidx.compose.ui.semantics.Role.Switch,
      )
      .padding(vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(title, style = GalleryType.BodyStrong, color = c.ink, modifier = Modifier.weight(1f))
    GlassToggle(checked = checked, onChange = onCheckedChange, accent = Candy.Sky)
  }
}

@Composable
fun BackgroundColorSwatchRow(modifier: Modifier = Modifier) {
  val c = glassColors
  val context = LocalContext.current
  val currentPreset = GlassSettings.backgroundPreset
  Row(
    modifier =
      modifier.fillMaxWidth()
        .clip(Capsule)
        .padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    for (preset in com.google.ai.edge.gallery.core.design.glass.BackgroundPreset.entries) {
      val selected = preset == currentPreset
      val pal = preset.palette(c.dark)
      Column(
        modifier =
          Modifier.weight(1f)
            .clip(RoundedRectangle(14.dp))
            .pressable(onClick = { GlassSettings.updateBackgroundPreset(context, preset) })
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Box(
          modifier =
            Modifier.size(34.dp)
              .clip(androidx.compose.foundation.shape.CircleShape)
              .background(androidx.compose.ui.graphics.Brush.verticalGradient(pal.wall))
              .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) Candy.Sky else Color.White.copy(alpha = if (c.dark) 0.28f else 0.72f),
                shape = androidx.compose.foundation.shape.CircleShape,
              ),
          contentAlignment = Alignment.Center,
        ) {
          if (selected) {
            Box(
              Modifier.size(10.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (c.dark) Color.White else Color(0xFF131D2E))
            )
          }
        }
        Text(
          text = preset.label,
          style = GalleryType.Caption.copy(
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
          ),
          color = if (selected) c.ink else c.inkSoft,
          maxLines = 1,
        )
      }
    }
  }
}
