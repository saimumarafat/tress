/*
 * Copyright 2025 Google LLC
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

package com.google.ai.edge.gallery.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.core.design.components.GlassPill
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType

data class EmptyStateButtonConfig(
  @StringRes val buttonLabelResId: Int,
  val buttonIcon: ImageVector? = null,
  val onButtonClick: () -> Unit = {},
  val extraContent: @Composable () -> Unit = {},
)

/**
 * A composable function to display an empty state with an icon, title, description, and an optional
 * button.
 */
@Composable
fun EmptyState(
  @StringRes titleResId: Int,
  @StringRes descriptionResId: Int,
  icon: ImageVector? = null,
  buttonConfig: EmptyStateButtonConfig? = null,
  horizontalPadding: Dp = 40.dp,
  iconSize: Dp = 52.dp,
  iconTint: Color = glassColors.inkSoft,
) {
  val c = glassColors
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.padding(horizontal = horizontalPadding),
  ) {
    if (icon != null) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize), tint = iconTint)
    }
    Text(
      stringResource(titleResId),
      style = GalleryType.Headline.copy(fontWeight = FontWeight.Bold),
      color = c.ink,
      textAlign = TextAlign.Center,
    )
    Text(
      stringResource(descriptionResId),
      style = GalleryType.Body,
      color = c.inkSoft,
      textAlign = TextAlign.Center,
    )
    if (buttonConfig != null) {
      Box(modifier = Modifier.padding(top = 6.dp)) {
        GlassPill(
          onClick = buttonConfig.onButtonClick,
          tint = Candy.Sky,
          height = 46.dp,
        ) {
          if (buttonConfig.buttonIcon != null) {
            Icon(
              buttonConfig.buttonIcon,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
          }
          Text(
            stringResource(buttonConfig.buttonLabelResId),
            style = GalleryType.BodyStrong,
            color = Color.White,
          )
        }
        buttonConfig.extraContent()
      }
    }
  }
}
