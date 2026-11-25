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

package com.tress.app.ui.benchmark

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tress.app.core.design.components.Capsule
import com.tress.app.core.design.components.GlassSheet
import com.tress.app.core.design.components.RoundedRectangle
import com.tress.app.core.design.components.pressable
import com.tress.app.core.design.components.wash
import com.tress.app.core.design.theme.Candy
import com.tress.app.core.design.theme.glassColors
import com.tress.app.core.design.typography.GalleryType

@Composable
fun BenchmarkModelPicker(
  selectedModelName: String,
  modelNames: List<String>,
  @StringRes titleResId: Int,
  onSelected: (String) -> Unit,
) {
  var showBottomSheet by remember { mutableStateOf(false) }
  val palette = glassColors

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    modifier =
      Modifier.wash(shape = Capsule, tint = palette.glass)
        .pressable(onClick = { showBottomSheet = true })
        .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
  ) {
    Text(
      selectedModelName,
      style = GalleryType.Label,
      color = palette.ink,
      maxLines = 1,
      overflow = TextOverflow.MiddleEllipsis,
      modifier = Modifier.weight(1f, fill = false),
    )
    Icon(
      Icons.Rounded.ArrowDropDown,
      tint = palette.inkSoft,
      modifier = Modifier.size(20.dp).sizeIn(minWidth = 20.dp),
      contentDescription = null,
    )
  }

  // Model picker.
  if (showBottomSheet) {
    GlassSheet(
      onDismiss = { showBottomSheet = false },
      title = stringResource(titleResId),
    ) {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
      ) {
        items(modelNames) { modelName ->
          val isSelected = modelName == selectedModelName
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .wash(
                  shape = RoundedRectangle(16.dp),
                  tint = if (isSelected) Candy.Sky.copy(alpha = 0.14f) else palette.glass,
                )
                .pressable(onClick = {
                  onSelected(modelName)
                  showBottomSheet = false
                })
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              Icons.Rounded.CheckCircle,
              contentDescription = null,
              tint = Candy.Sky,
              modifier = Modifier.size(20.dp).alpha(if (isSelected) 1f else 0f),
            )
            Text(
              modelName,
              color = palette.ink,
              style = GalleryType.BodyStrong,
            )
          }
        }
      }
    }
  }
}
