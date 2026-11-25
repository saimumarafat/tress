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

package com.tress.app.ui.common.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tress.app.core.design.components.Capsule
import com.tress.app.core.design.components.RoundedRectangle
import com.tress.app.core.design.components.pressable
import com.tress.app.core.design.theme.Tone
import com.tress.app.core.design.theme.glassColors
import com.tress.app.core.design.typography.GalleryType
import com.tress.app.data.Model
import com.tress.app.ui.common.humanReadableSize

data class BentoStarterItem(
  val label: String,
  val prompt: String = label,
  val icon: ImageVector? = null,
  val onClick: (() -> Unit)? = null,
)

/**
 * Compact Home-screen-style Bento empty state shared by AI Chat, Test Chat, and Agent Skills.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BentoChatEmptyState(
  model: Model,
  statusLabel: String,
  startersTitle: String,
  starters: List<BentoStarterItem>,
  startersEnabled: Boolean = true,
  startersTone: Tone = Tone.Sage,
  capabilityTags: List<String> = emptyList(),
  onStarterClick: (BentoStarterItem) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val c = glassColors
  val modelTone = c.tone(Tone.Paper)
  val actionTone = c.tone(startersTone)
  val cardShape = RoundedRectangle(24.dp)
  val rim = if (c.dark) Color(0xFF27272A) else Color(0xFFE4E4E7)
  val modelName = model.displayName.ifBlank { model.name }
  val sizeText = model.downloadInfo.sizeInBytes.takeIf { it > 0L }?.humanReadableSize()

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 520.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
      ) {
        // Active model Bento tile matching HomeScreen styling.
        Column(
          modifier =
            Modifier.weight(1f)
              .clip(cardShape)
              .background(Brush.verticalGradient(listOf(modelTone.top, modelTone.bottom)))
              .border(1.dp, rim, cardShape)
              .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = modelName,
            style = GalleryType.Headline.copy(fontSize = 20.sp, lineHeight = 24.sp),
            color = modelTone.accent,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = listOfNotNull(statusLabel.takeIf { it.isNotBlank() }, sizeText).joinToString(" · "),
            style = GalleryType.BodyStrong.copy(fontSize = 13.sp),
            color = modelTone.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (capabilityTags.isNotEmpty()) {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              for (tag in capabilityTags) {
                Box(
                  modifier =
                    Modifier.clip(Capsule)
                      .background(modelTone.ink.copy(alpha = if (c.dark) 0.14f else 0.08f))
                      .padding(horizontal = 9.dp, vertical = 4.dp),
                ) {
                  Text(
                    text = tag,
                    style = GalleryType.Caption.copy(fontWeight = FontWeight.SemiBold),
                    color = modelTone.ink,
                    maxLines = 1,
                  )
                }
              }
            }
          }
        }

        // Interactive quick-prompts / skills Bento tile.
        if (starters.isNotEmpty()) {
          Column(
            modifier =
              Modifier.weight(1.15f)
                .clip(cardShape)
                .background(Brush.verticalGradient(listOf(actionTone.top, actionTone.bottom)))
                .border(1.dp, rim, cardShape)
                .padding(14.dp)
                .alpha(if (startersEnabled) 1f else 0.62f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = startersTitle,
              style = GalleryType.Title.copy(fontSize = 18.sp),
              color = actionTone.ink,
              maxLines = 1,
            )
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
              val pillBg = if (c.dark) Color(0xFF27272A) else Color(0xFFFFFFFF)
              val pillRim = if (c.dark) Color(0xFF3F3F46) else Color(0xFFE4E4E7)
              for (starter in starters) {
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .height(36.dp)
                      .clip(Capsule)
                      .background(pillBg)
                      .border(1.dp, pillRim, Capsule)
                      .then(
                        if (startersEnabled) {
                          Modifier.pressable(
                            onClick = { starter.onClick?.invoke() ?: onStarterClick(starter) },
                            role = Role.Button,
                          )
                        } else {
                          Modifier
                        }
                      )
                      .padding(horizontal = 11.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                  if (starter.icon != null) {
                    Icon(
                      imageVector = starter.icon,
                      contentDescription = null,
                      tint = actionTone.ink,
                      modifier = Modifier.size(15.dp),
                    )
                  }
                  Text(
                    text = starter.label,
                    style = GalleryType.Caption.copy(fontWeight = FontWeight.SemiBold),
                    color = actionTone.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                  )
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = actionTone.ink.copy(alpha = 0.78f),
                    modifier = Modifier.size(13.dp),
                  )
                }
              }
            }
          }
        }
      }
      Spacer(Modifier.height(2.dp))
    }
  }
}
