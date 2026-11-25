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

package com.google.ai.edge.gallery.ui.common.asr

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.core.design.components.Capsule
import com.google.ai.edge.gallery.core.design.components.ChromeCapsule
import com.google.ai.edge.gallery.core.design.components.GlassCircle
import com.google.ai.edge.gallery.core.design.components.GlassSurface
import com.google.ai.edge.gallery.core.design.components.glass
import com.google.ai.edge.gallery.core.design.motion.Fluid
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType

/**
 * Liquid-glass speech-to-text dock: displays live transcript in a frosted capsule and puts
 * finalized speech directly into the editable composer field when stopped (never auto-sends).
 *
 * @param onCancel Discards the transcript.
 * @param onStop Stops listening and puts the transcript into the editable text box.
 * @param onSend Retained for callsite compatibility; voice capture defaults to edit-before-send.
 */
@Composable
fun ChatAsrDictationPanel(
  uiState: ChatAsrDictationUiState,
  onCancel: () -> Unit,
  onStop: () -> Unit,
  onSend: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val c = glassColors
  val transcriptScrollState = rememberScrollState()
  LaunchedEffect(uiState.transcript) {
    transcriptScrollState.scrollTo(transcriptScrollState.maxValue)
  }

  Column(
    modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // Live recognized speech preview floating directly above the capsule dock.
    GlassSurface(
      shape = Capsule,
      wash = c.frost,
      blur = 12.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(min = 42.dp, max = 84.dp)
            .verticalScroll(state = transcriptScrollState, enabled = false)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = uiState.transcript.ifBlank { stringResource(R.string.listening) },
          style = GalleryType.BodyStrong,
          color = if (uiState.transcript.isBlank()) c.inkSoft else c.ink,
          textAlign = TextAlign.Center,
          modifier =
            Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        )
      }
    }

    // Continuous capsule control bar: [ X ]  ~ waveform ~  [ Done (to editable text) ]
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .glass(shape = ChromeCapsule, wash = c.capsule, blur = 14.dp, lensHeight = 14.dp, lensAmount = 22.dp)
          .padding(5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      GlassCircle(
        onClick = onCancel,
        size = 54.dp,
        label = stringResource(R.string.asr_dictation_cancel_desc),
      ) {
        Icon(
          Icons.Rounded.Close,
          contentDescription = stringResource(R.string.asr_dictation_cancel_desc),
          tint = c.ink,
          modifier = Modifier.size(22.dp),
        )
      }

      Row(
        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        val barMultipliers = listOf(0.45f, 0.78f, 1.0f, 0.85f, 0.55f, 0.92f, 0.65f, 0.42f)
        barMultipliers.forEachIndexed { index, factor ->
          val level = (uiState.audioLevel.coerceIn(0.08f, 1f) * factor)
          val heightDp by
            animateFloatAsState(
              targetValue = (8f + 22f * level),
              animationSpec = Fluid.follow(),
              label = "asrBar$index",
            )
          Box(
            modifier =
              Modifier.width(4.dp)
                .height(heightDp.dp)
                .clip(Capsule)
                .background(Candy.Sky.copy(alpha = 0.85f))
          )
          if (index != barMultipliers.lastIndex) Spacer(Modifier.width(5.dp))
        }
      }

      // Stop dictation and place text into the editable composer input (do not auto-send).
      GlassCircle(
        onClick = onStop,
        size = 54.dp,
        tint = Candy.Sky,
        label = stringResource(R.string.asr_dictation_stop_desc),
      ) {
        Icon(
          Icons.Rounded.Check,
          contentDescription = stringResource(R.string.asr_dictation_stop_desc),
          tint = Color.White,
          modifier = Modifier.size(24.dp),
        )
      }
    }
  }
}
