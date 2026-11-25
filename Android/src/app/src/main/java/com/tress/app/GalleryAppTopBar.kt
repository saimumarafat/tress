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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.tress.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tress.app.core.design.components.GlassBackButton
import com.tress.app.core.design.components.GlassIcon
import com.tress.app.core.design.components.GlassPill
import com.tress.app.core.design.components.HeaderGlassButton
import com.tress.app.core.design.theme.glassColors
import com.tress.app.core.design.typography.GalleryType
import com.tress.app.data.AppBarAction
import com.tress.app.data.AppBarActionType

/** Floating liquid-glass top app bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryTopAppBar(
  title: String,
  modifier: Modifier = Modifier,
  leftAction: AppBarAction? = null,
  rightAction: AppBarAction? = null,
  @Suppress("UNUSED_PARAMETER") scrollBehavior: TopAppBarScrollBehavior? = null,
  subtitle: String = "",
) {
  val c = glassColors
  Row(
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    // Left action button
    when (leftAction?.actionType) {
      AppBarActionType.NAVIGATE_UP -> {
        GlassBackButton(onBack = leftAction.actionFn, immediate = true)
      }
      AppBarActionType.MENU -> {
        HeaderGlassButton(
          onClick = leftAction.actionFn,
          label = stringResource(R.string.cd_menu),
        ) {
          GlassIcon(
            Icons.Rounded.Menu,
            contentDescription = stringResource(R.string.cd_menu),
            modifier = Modifier.size(20.dp),
          )
        }
      }
      else -> androidx.compose.foundation.layout.Spacer(Modifier.size(42.dp))
    }

    // Center title + optional subtitle inside subtle typographic cluster
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        BasicText(
          text = title,
          maxLines = 1,
          color = { c.ink },
          style =
            GalleryType.Headline.copy(
              fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
              letterSpacing = (-0.7).sp,
            ),
          autoSize = TextAutoSize.StepBased(minFontSize = 18.sp, maxFontSize = 24.sp, stepSize = 1.sp),
        )
      }
      if (subtitle.isNotEmpty()) {
        Text(
          subtitle,
          style = GalleryType.Caption,
          color = c.inkSoft,
        )
      }
    }

    // Right action button
    when (rightAction?.actionType) {
      AppBarActionType.APP_SETTING -> {
        HeaderGlassButton(
          onClick = rightAction.actionFn,
          label = stringResource(R.string.cd_app_settings_icon),
        ) {
          GlassIcon(
            Icons.Rounded.Settings,
            contentDescription = stringResource(R.string.cd_app_settings_icon),
            modifier = Modifier.size(20.dp),
          )
        }
      }
      AppBarActionType.NAVIGATE_UP -> {
        GlassPill(onClick = rightAction.actionFn, height = 38.dp, horizontalPadding = 14.dp) {
          GlassIcon(Icons.Rounded.Check, null, Modifier.size(16.dp))
          Text("Done", style = GalleryType.Label, color = c.ink)
        }
      }
      AppBarActionType.IMPORT_FROM_URL -> {
        HeaderGlassButton(
          onClick = rightAction.actionFn,
          label = stringResource(R.string.cd_hf_import_from_url_button),
        ) {
          GlassIcon(
            Icons.Rounded.Link,
            contentDescription = stringResource(R.string.cd_hf_import_from_url_button),
            modifier = Modifier.size(20.dp),
          )
        }
      }
      else -> androidx.compose.foundation.layout.Spacer(Modifier.size(42.dp))
    }
  }
}
