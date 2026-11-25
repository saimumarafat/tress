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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.core.design.components.Capsule
import com.google.ai.edge.gallery.core.design.components.GlassDialog
import com.google.ai.edge.gallery.core.design.components.GlassIcon
import com.google.ai.edge.gallery.core.design.components.GlassPill
import com.google.ai.edge.gallery.core.design.components.GlassTextField
import com.google.ai.edge.gallery.core.design.components.HeaderGlassButton
import com.google.ai.edge.gallery.core.design.components.RoundedRectangle
import com.google.ai.edge.gallery.core.design.components.pressable
import com.google.ai.edge.gallery.core.design.components.wash
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType
import com.google.ai.edge.gallery.proto.ChatSessionProto
import com.kyant.backdrop.throttleBackdrops
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class HistoryDateBucket(val label: String) {
  TODAY("Today"),
  YESTERDAY("Yesterday"),
  PREVIOUS_7_DAYS("Previous 7 Days"),
  OLDER("Older"),
}

private fun bucketForTimestamp(timestampMs: Long, today: LocalDate): HistoryDateBucket {
  if (timestampMs <= 0L) return HistoryDateBucket.OLDER
  val date = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault()).toLocalDate()
  return when {
    date == today -> HistoryDateBucket.TODAY
    date == today.minusDays(1) -> HistoryDateBucket.YESTERDAY
    date.isAfter(today.minusDays(8)) -> HistoryDateBucket.PREVIOUS_7_DAYS
    else -> HistoryDateBucket.OLDER
  }
}

private fun formatShortTimestamp(timestampMs: Long): String {
  if (timestampMs <= 0L) return ""
  val zdt = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault())
  val today = LocalDate.now()
  return if (zdt.toLocalDate() == today) {
    DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).format(zdt)
  } else {
    DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()).format(zdt)
  }
}

@Composable
fun ChatHistorySideSheetContent(
  history: List<ChatSessionProto>,
  onHistoryItemClicked: (String) -> Unit,
  onHistoryItemDeleted: (String) -> Unit,
  onHistoryItemsDeleteAll: () -> Unit,
  onNewChatClicked: () -> Unit,
  onDismissed: () -> Unit,
  activeSessionId: String? = null,
  onHistoryItemRenamed: (sessionId: String, newTitle: String) -> Unit = { _, _ -> },
) {
  val c = glassColors
  var searchQuery by remember { mutableStateOf("") }
  var showConfirmDeleteDialog by remember { mutableStateOf(false) }
  var itemToDelete by remember { mutableStateOf<String?>(null) }
  var sessionToRename by remember { mutableStateOf<ChatSessionProto?>(null) }
  var renameText by remember { mutableStateOf("") }

  val filteredHistory = remember(history, searchQuery) {
    val q = searchQuery.trim()
    if (q.isEmpty()) history
    else history.filter {
      it.title.contains(q, ignoreCase = true) || it.originalModel.contains(q, ignoreCase = true)
    }
  }

  val groupedHistory = remember(filteredHistory) {
    val today = LocalDate.now()
    filteredHistory.groupBy { bucketForTimestamp(it.timestampMs, today) }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(horizontal = 20.dp, vertical = 16.dp),
  ) {
    // Top Row: Bold minimal Title + Close button
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          stringResource(R.string.chat_history_title),
          style =
            GalleryType.Hero.copy(
              fontSize = 30.sp,
              lineHeight = 34.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = (-1.0).sp,
            ),
          color = c.hero,
        )
        Text(
          "${history.size}",
          style = GalleryType.BodyStrong,
          color = Candy.Sky,
          modifier = Modifier.padding(bottom = 3.dp),
        )
      }
      HeaderGlassButton(
        onClick = onDismissed,
        label = stringResource(R.string.cd_close_icon),
        size = 38.dp,
      ) {
        GlassIcon(Icons.Rounded.Close, stringResource(R.string.cd_close_icon), Modifier.size(18.dp))
      }
    }

    // Search conversations
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp)
        .wash(Capsule)
        .padding(horizontal = 12.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(Icons.Rounded.Search, null, tint = c.inkSoft, modifier = Modifier.size(18.dp))
      Box(Modifier.weight(1f)) {
        GlassTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = "Search conversations...",
        )
      }
      if (searchQuery.isNotEmpty()) {
        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
          Icon(Icons.Rounded.Close, "Clear search", tint = c.inkSoft, modifier = Modifier.size(16.dp))
        }
      }
    }

    // Actions Row: "+ New chat" pill + Clear All
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      GlassPill(
        onClick = onNewChatClicked,
        tint = Candy.Sky,
        height = 42.dp,
        horizontalPadding = 16.dp,
      ) {
        Icon(Icons.Rounded.AddComment, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        Text(stringResource(R.string.new_chat), style = GalleryType.BodyStrong, color = Color.White)
      }
      if (history.isNotEmpty()) {
        GlassPill(
          onClick = { showConfirmDeleteDialog = true },
          height = 38.dp,
          horizontalPadding = 12.dp,
        ) {
          Text(stringResource(R.string.clear_all), style = GalleryType.Label, color = Candy.Tomato)
        }
      }
    }

    // Date-grouped History List
    if (filteredHistory.isEmpty()) {
      Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = if (searchQuery.isNotBlank()) "No matching conversations" else "No saved conversations yet",
          style = GalleryType.Body,
          color = c.inkSoft,
        )
      }
    } else {
      val listState = rememberLazyListState()
      LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f).throttleBackdrops { listState.isScrollInProgress },
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        HistoryDateBucket.entries.forEach { bucket ->
          val bucketItems = groupedHistory[bucket].orEmpty()
          if (bucketItems.isNotEmpty()) {
            item(key = "header_${bucket.name}") {
              Text(
                text = bucket.label.uppercase(),
                style = GalleryType.Eyebrow,
                color = c.inkSoft,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp),
              )
            }
            items(bucketItems, key = { it.sessionId }) { session ->
              val isActive = session.sessionId == activeSessionId
              val rowTint = if (isActive) Candy.Sky.copy(alpha = if (c.dark) 0.22f else 0.14f) else c.glass
              val rowShape = RoundedRectangle(22.dp)
              val rim = Color.White.copy(alpha = if (c.dark) 0.12f else 0.45f)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .wash(rowShape, tint = rowTint)
                  .border(1.dp, if (isActive) Candy.Sky.copy(alpha = 0.55f) else rim, rowShape)
                  .pressable(onClick = { onHistoryItemClicked(session.sessionId) })
                  .padding(vertical = 12.dp, horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                  Text(
                    session.title.ifBlank { "Untitled conversation" },
                    style = GalleryType.BodyStrong.copy(
                      fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                    ),
                    color = if (isActive) Candy.Sky else c.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    if (session.originalModel.isNotBlank()) {
                      Text(
                        session.originalModel,
                        style = GalleryType.Caption,
                        color = c.inkSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                      )
                    }
                    val ts = formatShortTimestamp(session.timestampMs)
                    if (ts.isNotEmpty()) {
                      Text("· $ts", style = GalleryType.Caption, color = c.inkFaint)
                    }
                  }
                }
                IconButton(
                  onClick = {
                    renameText = session.title
                    sessionToRename = session
                  },
                  modifier = Modifier.size(32.dp),
                ) {
                  Icon(
                    Icons.Rounded.Edit,
                    contentDescription = "Rename conversation",
                    tint = c.inkSoft,
                    modifier = Modifier.size(16.dp),
                  )
                }
                IconButton(
                  onClick = { itemToDelete = session.sessionId },
                  modifier = Modifier.size(32.dp),
                ) {
                  Icon(
                    Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.cd_delete_input_history_entry_icon),
                    tint = c.inkSoft,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
            }
          }
        }
      }
    }

    // Notice footer
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Info,
        contentDescription = null,
        tint = c.inkSoft,
        modifier = Modifier.size(15.dp),
      )
      Text(
        stringResource(R.string.chat_history_demo_notice),
        style = GalleryType.Caption,
        color = c.inkSoft,
      )
    }
  }

  if (sessionToRename != null) {
    GlassDialog(
      onDismiss = { sessionToRename = null },
      title = "Rename Conversation",
      confirm = "Save",
      onConfirm = {
        val target = sessionToRename
        val newTitle = renameText.trim()
        sessionToRename = null
        if (target != null && newTitle.isNotEmpty()) {
          onHistoryItemRenamed(target.sessionId, newTitle)
        }
      },
      dismiss = stringResource(R.string.cancel),
      confirmEnabled = renameText.trim().isNotEmpty(),
    ) {
      GlassTextField(
        value = renameText,
        onValueChange = { renameText = it },
        placeholder = "Conversation title",
      )
    }
  }

  if (showConfirmDeleteDialog) {
    GlassDialog(
      onDismiss = { showConfirmDeleteDialog = false },
      title = stringResource(R.string.clear_history_dialog_title),
      confirm = stringResource(R.string.ok),
      onConfirm = {
        showConfirmDeleteDialog = false
        onHistoryItemsDeleteAll()
      },
      dismiss = stringResource(R.string.cancel),
      destructive = true,
    ) {
      Text(stringResource(R.string.clear_history_dialog_content), style = GalleryType.Body, color = c.inkSoft)
    }
  }

  if (itemToDelete != null) {
    GlassDialog(
      onDismiss = { itemToDelete = null },
      title = "Delete conversation?",
      confirm = "Delete",
      onConfirm = {
        val toDel = itemToDelete
        itemToDelete = null
        if (toDel != null) onHistoryItemDeleted(toDel)
      },
      dismiss = stringResource(R.string.cancel),
      destructive = true,
    ) {
      Text("This conversation will be removed from local history.", style = GalleryType.Body, color = c.inkSoft)
    }
  }
}
