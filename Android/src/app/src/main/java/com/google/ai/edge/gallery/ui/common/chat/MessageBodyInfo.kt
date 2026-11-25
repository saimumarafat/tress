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

package com.google.ai.edge.gallery.ui.common.chat

// import androidx.compose.ui.tooling.preview.Preview
// import com.google.ai.edge.gallery.ui.theme.GalleryTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.core.design.components.RoundedRectangle
import com.google.ai.edge.gallery.core.design.components.wash
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.ui.common.MarkdownText

/**
 * Composable function to display informational message content within a chat.
 *
 * Supports markdown.
 */
@Composable
fun MessageBodyInfo(message: ChatMessageInfo, smallFontSize: Boolean = true) {
  val c = glassColors
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
    Box(
      modifier = Modifier.wash(shape = RoundedRectangle(18.dp), tint = c.glass)
    ) {
      MarkdownText(
        text = message.content,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        smallFontSize = smallFontSize,
        textColor = c.inkSoft,
      )
    }
  }
}

// @Preview(showBackground = true)
// @Composable
// fun MessageBodyInfoPreview() {
//   GalleryTheme {
//     Row(modifier = Modifier.padding(16.dp)) {
//       MessageBodyInfo(message = ChatMessageInfo(content = "This is a model"))
//     }
//   }
// }
