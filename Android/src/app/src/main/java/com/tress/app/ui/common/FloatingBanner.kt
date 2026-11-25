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

package com.tress.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tress.app.core.design.components.GlassPill
import com.tress.app.core.design.components.GlassSurface
import com.tress.app.core.design.components.RoundedRectangle
import com.tress.app.core.design.theme.Candy
import com.tress.app.core.design.theme.glassColors
import com.tress.app.core.design.typography.GalleryType

@Composable
fun FloatingBanner(
  visible: Boolean,
  text: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null,
) {
  val c = glassColors
  AnimatedVisibility(
    visible = visible,
    enter = slideInVertically() + fadeIn(),
    exit = fadeOut() + slideOutVertically(),
    modifier = modifier,
  ) {
    val hasAction = actionLabel != null && onActionClick != null
    GlassSurface(
      shape = RoundedRectangle(20.dp),
      wash = c.frost,
      blur = 12.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .padding(
              start = 16.dp,
              end = if (hasAction) 8.dp else 16.dp,
              top = if (hasAction) 8.dp else 12.dp,
              bottom = if (hasAction) 8.dp else 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = text,
          style = GalleryType.Caption,
          color = c.ink,
          modifier = Modifier.weight(1f),
        )
        if (hasAction) {
          GlassPill(
            onClick = onActionClick,
            tint = Candy.Sky,
            height = 34.dp,
            horizontalPadding = 12.dp,
          ) {
            Text(text = actionLabel, style = GalleryType.Label, color = Color.White)
          }
        }
      }
    }
  }
}
