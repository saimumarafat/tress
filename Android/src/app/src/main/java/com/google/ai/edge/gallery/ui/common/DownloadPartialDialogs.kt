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

package com.google.ai.edge.gallery.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.core.design.components.GlassDialog
import com.google.ai.edge.gallery.core.design.components.GlassPill
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType
import com.google.ai.edge.gallery.data.Task

/**
 * The buttons of a paused download, styled with Liquid Glass pills: Discard, which deletes the
 * partial files, and Resume, which downloads the rest. Both are icon-only when [compact].
 */
@Composable
internal fun PausedDownloadButtons(
  task: Task?,
  compact: Boolean,
  resumeEnabled: Boolean,
  onResume: () -> Unit,
  onDiscard: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = glassColors
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val buttonModifier = if (compact) Modifier else Modifier.weight(1f)

    // Discard partial files.
    GlassPill(
      onClick = onDiscard,
      modifier = buttonModifier,
      surface = palette.glass,
      height = 42.dp,
      horizontalPadding = 12.dp,
    ) {
      Icon(
        Icons.Outlined.Delete,
        contentDescription =
          if (compact) stringResource(R.string.cd_discard_partial_download) else null,
        tint = palette.ink,
        modifier = Modifier.size(18.dp),
      )
      if (!compact) {
        Text(
          stringResource(R.string.discard_partial_download),
          color = palette.ink,
          style = GalleryType.BodyStrong,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    // Resume download.
    val accentColor = if (task != null) getTaskBgGradientColors(task = task)[1] else Candy.Sky
    GlassPill(
      onClick = onResume,
      modifier = buttonModifier,
      enabled = resumeEnabled,
      tint = accentColor,
      height = 42.dp,
      horizontalPadding = 12.dp,
    ) {
      val contentTint = if (resumeEnabled) LocalContentColor.current else palette.inkFaint
      Icon(
        Icons.Outlined.FileDownload,
        contentDescription = if (compact) stringResource(R.string.cd_resume_download) else null,
        tint = contentTint,
        modifier = Modifier.size(18.dp),
      )
      if (!compact) {
        Text(
          stringResource(R.string.resume_download),
          color = contentTint,
          style = GalleryType.BodyStrong,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

/**
 * Asks the user to confirm stopping a model download, which deletes the [receivedBytes] downloaded
 * so far.
 *
 * @param onStopAndDelete Called when the user confirms.
 * @param onDismiss Called when the user keeps the download running.
 */
@Composable
fun StopDownloadDialog(receivedBytes: Long, onStopAndDelete: () -> Unit, onDismiss: () -> Unit) {
  val palette = glassColors
  GlassDialog(
    onDismiss = onDismiss,
    title = stringResource(R.string.stop_download_dialog_title),
    confirm = stringResource(R.string.stop_and_delete_download),
    onConfirm = onStopAndDelete,
    dismiss = stringResource(R.string.keep_downloading),
    destructive = true,
  ) {
    Text(
      text = partialDownloadWillBeDeletedMessage(receivedBytes),
      style = GalleryType.Body,
      color = palette.inkSoft,
    )
  }
}

/**
 * Asks the user to confirm deleting the [receivedBytes] that a paused model download left.
 *
 * @param onDelete Called when the user confirms.
 * @param onDismiss Called when the user keeps the partial download.
 */
@Composable
fun DiscardPartialDownloadDialog(receivedBytes: Long, onDelete: () -> Unit, onDismiss: () -> Unit) {
  val palette = glassColors
  GlassDialog(
    onDismiss = onDismiss,
    title = stringResource(R.string.discard_partial_download_dialog_title),
    confirm = stringResource(R.string.delete),
    onConfirm = onDelete,
    dismiss = stringResource(R.string.cancel),
    destructive = true,
  ) {
    Text(
      text = partialDownloadWillBeDeletedMessage(receivedBytes),
      style = GalleryType.Body,
      color = palette.inkSoft,
    )
  }
}

/** Returns the message that the [receivedBytes] downloaded so far will be deleted. */
@Composable
private fun partialDownloadWillBeDeletedMessage(receivedBytes: Long): String =
  stringResource(R.string.partial_download_will_be_deleted, receivedBytes.humanReadableSize())
