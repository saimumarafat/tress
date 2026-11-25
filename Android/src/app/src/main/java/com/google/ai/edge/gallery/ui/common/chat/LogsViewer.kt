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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
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
import com.google.ai.edge.gallery.common.clearFocusOnKeyboardDismiss
import com.google.ai.edge.gallery.core.design.components.GlassChip
import com.google.ai.edge.gallery.core.design.components.GlassCircle
import com.google.ai.edge.gallery.core.design.components.GlassSheet
import com.google.ai.edge.gallery.core.design.components.GlassTextField
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType
import com.kyant.backdrop.throttleBackdrops

/**
 * A Composable function to display console logs within a [GlassSheet].
 *
 * @param logs The list of [LogMessage] to display.
 * @param onDismissRequest Callback to be invoked when the sheet is dismissed.
 */
@Composable
fun LogsViewer(logs: List<LogMessage>, onDismissRequest: () -> Unit) {
  val palette = glassColors
  val listState = rememberLazyListState()

  GlassSheet(onDismiss = onDismissRequest) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Top Bar: Title and Close Button
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          stringResource(R.string.logs_viewer_title),
          style = GalleryType.Headline,
          color = palette.ink,
        )
        GlassCircle(
          onClick = onDismissRequest,
          size = 38.dp,
          surface = palette.glass,
          ink = palette.inkSoft,
          label = "Close viewer",
        ) {
          Icon(
            Icons.Filled.Close,
            contentDescription = "Close viewer",
            modifier = Modifier.size(18.dp),
          )
        }
      }

      // Filter Field
      var filterText by remember { mutableStateOf("") }
      GlassTextField(
        value = filterText,
        onValueChange = { filterText = it },
        placeholder = stringResource(R.string.logs_viewer_filter_text_input_placeholder),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clearFocusOnKeyboardDismiss(),
        singleLine = true,
      )

      // Level Filter
      var selectedLevels by remember {
        mutableStateOf(setOf(LogMessageLevel.Info, LogMessageLevel.Warning, LogMessageLevel.Error))
      }
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        LogMessageLevel.entries.forEach { level ->
          val selected = selectedLevels.contains(level)
          GlassChip(
            onClick = {
              selectedLevels =
                if (selected) selectedLevels - level else selectedLevels + level
            },
            modifier = Modifier.weight(1f),
            height = 36.dp,
            fill = if (selected) ({ Candy.Sky.copy(alpha = 0.22f) }) else null,
            ink = if (selected) Candy.Sky else palette.inkSoft,
          ) {
            Text(
              level.name,
              style = GalleryType.Label,
            )
          }
        }
      }

      // LazyColumn for Logs
      val filteredLogs =
        remember(logs, filterText, selectedLevels) {
          logs.filter { log ->
            log.message.contains(filterText.trim(), ignoreCase = true) &&
              selectedLevels.contains(log.level)
          }
        }

      LazyColumn(
        state = listState,
        modifier =
          Modifier.fillMaxWidth()
            .weight(1f, fill = false)
            .throttleBackdrops { listState.isScrollInProgress },
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        if (filteredLogs.isEmpty()) {
          item {
            Text(
              stringResource(R.string.logs_viewer_no_matching_logs),
              modifier = Modifier.padding(16.dp),
              style = GalleryType.Body,
              color = palette.inkSoft,
            )
          }
        } else {
          items(filteredLogs) { log -> LogItem(log = log) }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}

/**
 * Composable to display a single [LogMessage].
 *
 * @param log The [LogMessage] to display.
 */
@Composable
private fun LogItem(log: LogMessage) {
  val palette = glassColors
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // Severity Icon
    val icon =
      when (log.level) {
        LogMessageLevel.Info -> Icons.Filled.Info
        LogMessageLevel.Warning -> Icons.Filled.Warning
        LogMessageLevel.Error -> Icons.Filled.Error
      }
    val tint =
      when (log.level) {
        LogMessageLevel.Info -> palette.inkFaint
        LogMessageLevel.Warning -> Candy.Tangerine
        LogMessageLevel.Error -> Candy.Tomato
      }
    Icon(
      imageVector = icon,
      contentDescription = log.level.name,
      tint = tint,
      modifier = Modifier.size(20.dp),
    )

    // Source, LineNumber, and Message Column
    Column(modifier = Modifier.weight(1f)) {
      if (log.source.isNotEmpty() && log.lineNumber != -1) {
        Text(
          "${log.source}:${log.lineNumber}",
          style = GalleryType.Caption,
          color = palette.inkFaint,
        )
      } else if (log.source.isNotEmpty()) {
        Text(
          log.source,
          style = GalleryType.Caption,
          color = palette.inkFaint,
        )
      }
      Text(
        log.message,
        style = GalleryType.Body,
        color = palette.ink,
        modifier = Modifier.padding(top = 4.dp),
      )
    }
  }
}
