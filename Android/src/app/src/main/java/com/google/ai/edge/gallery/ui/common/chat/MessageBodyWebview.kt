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

package com.google.ai.edge.gallery.ui.common.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.core.design.components.GlassChip
import com.google.ai.edge.gallery.core.design.components.GlassCircle
import com.google.ai.edge.gallery.core.design.components.GlassSheet
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType
import com.google.ai.edge.gallery.ui.common.GalleryWebView
import com.google.ai.edge.gallery.ui.common.WebViewUrlPolicy

private const val TAG = "AGMessageBodyWebview"

/** A Composable that displays a WebView to render web content within a chat message. */
@Composable
fun MessageBodyWebview(message: ChatMessageWebView, modifier: Modifier = Modifier) {
  var showBottomSheet by remember { mutableStateOf(false) }
  val palette = glassColors
  val allowPermission = !message.iframe && WebViewUrlPolicy.isTrustedBuiltInAssetUrl(message.url)

  Column(modifier = modifier) {
    GalleryWebView(
      modifier = Modifier.fillMaxWidth().aspectRatio(message.aspectRatio),
      initialUrl = message.url,
      useIframeWrapper = message.iframe,
      preventParentScrolling = true,
      allowRequestPermission = allowPermission,
    )
    Spacer(modifier = Modifier.height(8.dp))
    GlassChip(
      onClick = { showBottomSheet = true },
      surface = palette.glass,
      ink = palette.ink,
    ) {
      Icon(
        Icons.Outlined.FitScreen,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = palette.inkSoft,
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(stringResource(R.string.view_in_full_screen), style = GalleryType.Label)
    }
  }

  if (showBottomSheet) {
    GlassSheet(
      onDismiss = { showBottomSheet = false },
      maxHeightFraction = 0.94f,
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        GalleryWebView(
          modifier = Modifier.fillMaxSize(),
          initialUrl = message.url,
          useIframeWrapper = message.iframe,
          preventParentScrolling = true,
          allowRequestPermission = allowPermission,
        )
        GlassCircle(
          onClick = { showBottomSheet = false },
          size = 38.dp,
          surface = palette.glass,
          ink = palette.ink,
          label = stringResource(R.string.cd_close_icon),
          modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
          Icon(
            Icons.Outlined.Close,
            contentDescription = stringResource(R.string.cd_close_icon),
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}
