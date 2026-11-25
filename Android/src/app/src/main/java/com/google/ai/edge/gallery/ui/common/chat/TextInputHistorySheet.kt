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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.core.design.components.GlassCircle
import com.google.ai.edge.gallery.core.design.components.GlassDialog
import com.google.ai.edge.gallery.core.design.components.GlassSheet
import com.google.ai.edge.gallery.core.design.components.RoundedRectangle
import com.google.ai.edge.gallery.core.design.components.pressable
import com.google.ai.edge.gallery.core.design.components.wash
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType
import com.kyant.backdrop.throttleBackdrops
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TextInputHistorySheet(
  history: List<String>,
  onHistoryItemClicked: (String) -> Unit,
  onHistoryItemDeleted: (String) -> Unit,
  onHistoryItemsDeleteAll: () -> Unit,
  onDismissed: () -> Unit,
) {
  GlassSheet(onDismiss = onDismissed) {
    SheetContent(
      history = history,
      onHistoryItemClicked = { item ->
        onHistoryItemClicked(item)
        onDismissed()
      },
      onHistoryItemDeleted = onHistoryItemDeleted,
      onHistoryItemsDeleteAll = {
        onDismissed()
        onHistoryItemsDeleteAll()
      },
    )
  }
}

@Composable
private fun SheetContent(
  history: List<String>,
  onHistoryItemClicked: (String) -> Unit,
  onHistoryItemDeleted: (String) -> Unit,
  onHistoryItemsDeleteAll: () -> Unit,
) {
  val palette = glassColors
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()
  var showConfirmDeleteDialog by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = stringResource(R.string.text_input_history_title),
        style = GalleryType.Headline,
        color = palette.ink,
      )
      GlassCircle(
        onClick = { showConfirmDeleteDialog = true },
        size = 38.dp,
        surface = palette.glass,
        ink = palette.inkSoft,
        label = stringResource(R.string.cd_clear_input_history_icon),
      ) {
        Icon(
          Icons.Rounded.DeleteSweep,
          contentDescription = stringResource(R.string.cd_clear_input_history_icon),
          modifier = Modifier.size(18.dp),
        )
      }
    }
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxWidth().weight(1f, fill = false).throttleBackdrops { listState.isScrollInProgress },
      verticalArrangement = Arrangement.spacedBy(8.dp),
      contentPadding = PaddingValues(bottom = 8.dp),
    ) {
      items(history, key = { it }) { item ->
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .wash(shape = RoundedRectangle(20.dp), tint = palette.glass)
              .pressable(onClick = { onHistoryItemClicked(item) })
              .padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            item,
            style = GalleryType.Body,
            color = palette.ink,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          GlassCircle(
            onClick = {
              scope.launch {
                delay(200)
                onHistoryItemDeleted(item)
              }
            },
            size = 34.dp,
            surface = palette.glass,
            ink = palette.inkSoft,
            label = stringResource(R.string.cd_delete_input_history_entry_icon),
          ) {
            Icon(
              Icons.Rounded.Delete,
              contentDescription = stringResource(R.string.cd_delete_input_history_entry_icon),
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }
  }

  if (showConfirmDeleteDialog) {
    GlassDialog(
      onDismiss = { showConfirmDeleteDialog = false },
      title = stringResource(R.string.clear_history_title),
      confirm = stringResource(R.string.ok),
      onConfirm = {
        showConfirmDeleteDialog = false
        onHistoryItemsDeleteAll()
      },
      dismiss = stringResource(R.string.cancel),
      destructive = true,
    ) {
      Text(
        text = stringResource(R.string.clear_history_message),
        style = GalleryType.Body,
        color = palette.inkSoft,
      )
    }
  }
}

// @Preview(showBackground = true)
// @Composable
// fun TextInputHistorySheetContentPreview() {
//   GalleryTheme {
//     SheetContent(
//       history =
//         listOf(
//           "Analyze the sentiment of the following Tweets and classify them as POSITIVE, NEGATIVE,
// or NEUTRAL. \"It's so beautiful today!\"",
//           "I have the ingredients above. Not sure what to cook for lunch. Show me a list of foods
// with the recipes.",
//           "You are Santa Claus, write a letter back for this kid.",
//           "Generate a list of cookie recipes. Make the outputs in JSON format.",
//         ),
//       onHistoryItemClicked = {},
//       onHistoryItemDeleted = {},
//       onHistoryItemsDeleteAll = {},
//       onDismissed = {},
//     )
//   }
// }
