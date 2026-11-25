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

package com.google.ai.edge.gallery.ui.notifications

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.core.design.components.Capsule
import com.google.ai.edge.gallery.core.design.components.GlassChip
import com.google.ai.edge.gallery.core.design.components.GlassDialog
import com.google.ai.edge.gallery.core.design.components.HeaderGlassButton
import com.google.ai.edge.gallery.core.design.components.RoundedRectangle
import com.google.ai.edge.gallery.core.design.components.WashPanel
import com.google.ai.edge.gallery.core.design.components.pressable
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType
import com.google.ai.edge.gallery.notifications.NotificationScheduleManager
import com.google.ai.edge.gallery.proto.ScheduledNotification
import com.kyant.backdrop.throttleBackdrops
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(val scheduleManager: NotificationScheduleManager) :
  ViewModel() {
  val notifications = scheduleManager.scheduledNotifications

  fun removeNotification(id: String) {
    scheduleManager.removeNotification(id)
  }
}

/** A screen to display the list of scheduled notifications. */
@Composable
fun NotificationsScreen(
  navigateUp: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: NotificationsViewModel = hiltViewModel(),
) {
  val notifications by viewModel.notifications.collectAsState()
  var notificationToDelete by remember { mutableStateOf<ScheduledNotification?>(null) }

  val groupedNotifications by
    remember(notifications) { derivedStateOf { notifications.groupBy { it.channelName } } }
  val expandedStates = remember { mutableStateMapOf<String, Boolean>() }
  val listState = rememberLazyListState()

  val c = glassColors
  Scaffold(
    modifier = modifier,
    containerColor = Color.Transparent,
    topBar = {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = stringResource(R.string.notifications_title),
            color = c.hero,
            style =
              GalleryType.Hero.copy(
                fontSize = 34.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.2).sp,
              ),
          )
          if (notifications.isNotEmpty()) {
            Box(
              modifier =
                Modifier.clip(Capsule)
                  .then(Modifier)
                  .padding(horizontal = 2.dp),
            ) {
              Text(
                text = "${notifications.size}",
                style = GalleryType.Caption.copy(fontWeight = FontWeight.Bold),
                color = Candy.Sky,
              )
            }
          }
        }
        HeaderGlassButton(
          onClick = { navigateUp() },
          label = stringResource(R.string.cd_close_icon),
          size = 40.dp,
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.cd_close_icon),
            tint = c.ink,
            modifier = Modifier.size(19.dp),
          )
        }
      }
    },
  ) { innerPadding ->
    LazyColumn(
      state = listState,
      modifier =
        Modifier.fillMaxSize()
          .throttleBackdrops { listState.isScrollInProgress }
          .padding(innerPadding)
          .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(vertical = 12.dp),
    ) {
      if (notifications.isEmpty()) {
        item {
          Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = stringResource(R.string.notifications_empty_state),
              style = GalleryType.BodyStrong,
              color = c.inkSoft,
            )
          }
        }
      } else {
        for ((channelName, list) in groupedNotifications) {
          val name = channelName.ifEmpty { "Scheduled" }
          item(key = "header_$name") {
            val isExpanded = expandedStates.getOrDefault(name, true)
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedRectangle(16.dp))
                  .pressable(onClick = { expandedStates[name] = !isExpanded })
                  .padding(vertical = 8.dp, horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = name,
                style = GalleryType.Title.copy(fontWeight = FontWeight.Bold),
                color = c.ink,
              )
              Icon(
                imageVector =
                  if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = c.inkSoft,
              )
            }
          }
          if (expandedStates.getOrDefault(name, true)) {
            items(list, key = { it.id }) { notification ->
              NotificationItem(
                notification = notification,
                onDeleteClick = { notificationToDelete = notification },
              )
            }
          }
        }
      }
    }
  }

  if (notificationToDelete != null) {
    GlassDialog(
      onDismiss = { notificationToDelete = null },
      title = stringResource(R.string.notifications_delete_dialog_title),
      confirm = stringResource(R.string.delete),
      destructive = true,
      onConfirm = {
        notificationToDelete?.let { viewModel.removeNotification(it.id) }
        notificationToDelete = null
      },
      dismiss = stringResource(R.string.cancel),
    ) {
      Text(
        stringResource(
          R.string.notifications_delete_dialog_content,
          notificationToDelete?.title ?: "",
        ),
        style = GalleryType.Body,
        color = c.inkSoft,
      )
    }
  }
}

@Composable
fun NotificationItem(notification: ScheduledNotification, onDeleteClick: () -> Unit) {
  val c = glassColors
  val shape = RoundedRectangle(26.dp)
  val rim = Color.White.copy(alpha = if (c.dark) 0.14f else 0.55f)
  WashPanel(
    modifier = Modifier.fillMaxWidth().border(1.dp, rim, shape),
    shape = shape,
    tint = c.glass,
  ) {
    Column(modifier = Modifier.padding(18.dp).fillMaxWidth()) {
      Text(
        text = notification.title,
        style = GalleryType.CardTitle.copy(fontWeight = FontWeight.Bold),
        color = c.ink,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(text = notification.message, style = GalleryType.Body, color = c.inkSoft)
      Spacer(modifier = Modifier.height(12.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
      ) {
        Column {
          val timeStr =
            String.format(Locale.getDefault(), "%02d:%02d", notification.hour, notification.minute)
          Text(
            text = stringResource(R.string.notifications_time_label, timeStr),
            style = GalleryType.Caption,
            color = c.inkSoft,
          )
          if (
            !notification.repeatDaily &&
              notification.hasYear() &&
              notification.hasMonth() &&
              notification.hasDay()
          ) {
            Spacer(modifier = Modifier.height(4.dp))
            val dateStr = "${notification.year}/${notification.month}/${notification.day}"
            Text(
              text = stringResource(R.string.notifications_date_label, dateStr),
              style = GalleryType.Caption,
              color = c.inkSoft,
            )
          }
        }
        val repeatStr =
          if (notification.repeatDaily) {
            stringResource(R.string.notifications_repeat_daily)
          } else {
            stringResource(R.string.notifications_repeat_one_time)
          }
        Text(
          text = repeatStr,
          style = GalleryType.Caption.copy(fontWeight = FontWeight.SemiBold),
          color = Candy.Sky,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
      ) {
        GlassChip(
          onClick = onDeleteClick,
          height = 36.dp,
          ink = Candy.Tomato,
        ) {
          Icon(
            Icons.Outlined.Delete,
            contentDescription = null,
            tint = Candy.Tomato,
            modifier = Modifier.size(16.dp),
          )
          Text(
            stringResource(R.string.delete),
            style = GalleryType.Caption.copy(fontWeight = FontWeight.SemiBold),
            color = Candy.Tomato,
          )
        }
      }
    }
  }
}
