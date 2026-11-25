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

package com.google.ai.edge.gallery.core.design

import androidx.compose.ui.graphics.Color
import com.google.ai.edge.gallery.core.design.glass.GlassPerformanceTier
import com.google.ai.edge.gallery.core.design.glass.GlassSettings
import com.google.ai.edge.gallery.core.design.glass.GlassStyle
import com.google.ai.edge.gallery.core.design.motion.MotionSettings
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.GalleryDarkSkyScheme
import com.google.ai.edge.gallery.core.design.theme.GalleryLightSkyScheme
import com.google.ai.edge.gallery.core.design.theme.Tone
import com.google.ai.edge.gallery.core.design.theme.LightGlassColors
import com.google.ai.edge.gallery.core.design.theme.DarkGlassColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassDesignSystemTest {

  @Test
  fun skyBluePrimaryTokensConsistentAcrossLightAndDarkSchemes() {
    assertEquals(Color(0xFF0A84FF), Candy.Sky)
    assertEquals(Candy.Sky, GalleryLightSkyScheme.primary)
    assertEquals(Color(0xFF47A1FF), GalleryDarkSkyScheme.primary)
    assertTrue(DarkGlassColors.dark)
    assertFalse(LightGlassColors.dark)
    assertEquals(Candy.Sky, LightGlassColors.tone(Tone.Sky).accent)
  }

  @Test
  fun liteGlassPresetDisablesExpensiveBlurAndRefraction() {
    assertEquals(0f, GlassStyle.Lite.blur, 0.0001f)
    assertEquals(0f, GlassStyle.Lite.refractionHeight, 0.0001f)
    assertEquals(0f, GlassStyle.Lite.refractionAmount, 0.0001f)
    assertFalse(GlassStyle.Lite.chromaticAberration)
    assertTrue(GlassStyle.Lite.tint > 1f)
  }

  @Test
  fun defaultGlassSettingsStartInFullAuroraAdaptiveTier() {
    assertEquals(GlassPerformanceTier.FULL, GlassSettings.performanceTier)
    assertTrue(GlassSettings.adaptive)
    assertTrue(GlassSettings.haptics)
    assertEquals(GlassSettings.style, GlassSettings.effectiveStyle)
  }

  @Test
  fun motionSettingsProducesBoundedDampingAndStiffness() {
    val damping = MotionSettings.damping(0.7f)
    val stiffness = MotionSettings.stiffness(420f)
    assertTrue(damping in 0.18f..1f)
    assertTrue(stiffness > 0f)
  }
}
