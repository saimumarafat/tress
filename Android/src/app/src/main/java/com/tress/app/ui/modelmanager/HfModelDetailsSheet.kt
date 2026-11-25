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

package com.tress.app.ui.modelmanager

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tress.app.R
import com.tress.app.core.design.components.GlassCircle
import com.tress.app.core.design.components.GlassPill
import com.tress.app.core.design.components.GlassSheet
import com.tress.app.core.design.components.RoundedRectangle
import com.tress.app.core.design.components.pressable
import com.tress.app.core.design.components.wash
import com.tress.app.core.design.theme.Candy
import com.tress.app.core.design.theme.glassColors
import com.tress.app.core.design.typography.GalleryType
import com.tress.app.huggingface.getCompatibleModelFiles
import com.tress.app.huggingface.getDeviceTotalRamBytes
import com.tress.app.huggingface.isFileCompatibleWithDevice
import com.tress.app.huggingface.isFileTooLarge
import com.tress.app.huggingface.modelName
import com.tress.app.proto.HfModelItemProto
import com.tress.app.ui.common.formatCount
import com.tress.app.ui.common.formatLastModifiedDate
import com.tress.app.ui.common.humanReadableSize
import kotlinx.coroutines.launch

private val SHEET_PADDING = 20.dp
private val SECTION_SPACING = 18.dp
private val ITEM_SPACING_MEDIUM = 14.dp
private val ITEM_SPACING_SMALL = 10.dp
private val ITEM_SPACING_XSMALL = 8.dp
private val ITEM_SPACING_XXSMALL = 4.dp
private val BADGE_VERTICAL_SPACING = 6.dp
private val BADGE_ICON_SIZE = 15.dp

@Composable
fun HfModelDetailsSheet(
  modelItem: HfModelItemProto,
  onDismiss: () -> Unit,
  onImportModelFile: (modelId: String, fileName: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  GlassSheet(
    onDismiss = onDismiss,
    title = modelItem.modelName,
    subtitle = modelItem.author.ifEmpty { modelItem.id.substringBefore("/", "Hugging Face") },
  ) {
    HfModelDetailsContent(
      modelItem = modelItem,
      onImportModelFile = { modelId, fileName ->
        onDismiss()
        onImportModelFile(modelId, fileName)
      },
      modifier = modifier,
    )
  }
}

@Composable
fun HfModelDetailsContent(
  modelItem: HfModelItemProto,
  onImportModelFile: (modelId: String, fileName: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val c = glassColors
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val clipboard = LocalClipboard.current
  val files = remember(modelItem) { modelItem.getCompatibleModelFiles() }
  val totalRam = remember(context) { getDeviceTotalRamBytes(context) }

  val fileInfos =
    remember(files, modelItem, totalRam) {
      files.map { fileName ->
        val isCompatible = isFileCompatibleWithDevice(fileName)
        val sibling = modelItem.siblingsList.firstOrNull { it.rfilename == fileName }
        val fileSize = sibling?.size ?: 0L
        val isTooLarge = isFileTooLarge(sizeBytes = fileSize, deviceRamBytes = totalRam)
        val isDisabled = !isCompatible || isTooLarge
        ModelFileInfo(
          fileName = fileName,
          fileSize = fileSize,
          isCompatible = isCompatible,
          isTooLarge = isTooLarge,
          isDisabled = isDisabled,
        )
      }
    }

  val compatibleFiles = remember(fileInfos) { fileInfos.filter { !it.isDisabled } }
  val otherFiles = remember(fileInfos) { fileInfos.filter { it.isDisabled } }

  var selectedFileName by
    remember(modelItem) { mutableStateOf(compatibleFiles.firstOrNull()?.fileName) }

  val urlCopiedMessage = stringResource(R.string.url_copied)
  val incompatiblePrompt = stringResource(R.string.incompatible_file_selection_prompt)

  Column(modifier = modifier.fillMaxWidth()) {
    Column(
      modifier =
        Modifier.weight(1f, fill = false)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
      // Stats row: Downloads, Likes, Last Updated
      ModelStatsRow(
        downloads = modelItem.downloads,
        likes = modelItem.likes,
        lastModified = modelItem.lastModified,
      )

      Spacer(modifier = Modifier.height(SECTION_SPACING))

      if (files.isEmpty()) {
        Text(
          text = stringResource(R.string.hf_no_model_files_in_card),
          style = GalleryType.Body,
          color = Candy.Tomato,
          modifier = Modifier.padding(vertical = ITEM_SPACING_XSMALL),
        )
      } else {
        // Compatible Files section
        if (compatibleFiles.isNotEmpty()) {
          Text(
            text = stringResource(R.string.compatible_files_header, compatibleFiles.size),
            style = GalleryType.Title.copy(fontWeight = FontWeight.Bold),
            color = c.ink,
          )
          Spacer(modifier = Modifier.height(ITEM_SPACING_XXSMALL))
          Text(
            text = stringResource(R.string.compatible_files_subheader),
            style = GalleryType.Caption,
            color = c.inkSoft,
          )
          Spacer(modifier = Modifier.height(ITEM_SPACING_SMALL))

          for (fileInfo in compatibleFiles) {
            ModelFileItemRow(
              fileInfo = fileInfo,
              isSelected = fileInfo.fileName == selectedFileName,
              modelId = modelItem.id,
              onSelect = { selectedFileName = fileInfo.fileName },
              onCopyUrl = { modelUrl ->
                val clipData = ClipData.newPlainText("model_url", modelUrl)
                scope.launch { clipboard.setClipEntry(clipData.toClipEntry()) }
                Toast.makeText(context, urlCopiedMessage, Toast.LENGTH_SHORT).show()
              },
            )
          }
        }

        // Other Files section
        if (otherFiles.isNotEmpty()) {
          if (compatibleFiles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(SECTION_SPACING))
          }
          Text(
            text = stringResource(R.string.other_files_header, otherFiles.size),
            style = GalleryType.Title.copy(fontWeight = FontWeight.Bold),
            color = c.ink,
          )
          Spacer(modifier = Modifier.height(ITEM_SPACING_XXSMALL))
          Text(
            text = stringResource(R.string.other_files_subheader),
            style = GalleryType.Caption,
            color = c.inkSoft,
          )
          Spacer(modifier = Modifier.height(ITEM_SPACING_SMALL))

          for (fileInfo in otherFiles) {
            ModelFileItemRow(
              fileInfo = fileInfo,
              isSelected = fileInfo.fileName == selectedFileName,
              modelId = modelItem.id,
              onSelect = {
                Toast.makeText(context, incompatiblePrompt, Toast.LENGTH_SHORT).show()
              },
              onCopyUrl = { modelUrl ->
                val clipData = ClipData.newPlainText("model_url", modelUrl)
                scope.launch { clipboard.setClipEntry(clipData.toClipEntry()) }
                Toast.makeText(context, urlCopiedMessage, Toast.LENGTH_SHORT).show()
              },
            )
          }
        }
      }
    }

    // Always-visible Import Selected Model button at bottom
    if (files.isNotEmpty()) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .padding(top = ITEM_SPACING_MEDIUM, bottom = 6.dp)
            .navigationBarsPadding()
      ) {
        val currentSelectedFile = selectedFileName
        GlassPill(
          onClick = {
            if (currentSelectedFile != null) {
              onImportModelFile(modelItem.id, currentSelectedFile)
            }
          },
          enabled = currentSelectedFile != null,
          tint = if (currentSelectedFile != null) Candy.Sky else c.inkFaint,
          height = 50.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.import_selected_model),
            style = GalleryType.BodyStrong,
            color = Color.White,
          )
        }
      }
    }
  }
}

private data class ModelFileInfo(
  val fileName: String,
  val fileSize: Long,
  val isCompatible: Boolean,
  val isTooLarge: Boolean,
  val isDisabled: Boolean,
)

@Composable
private fun ModelFileItemRow(
  fileInfo: ModelFileInfo,
  isSelected: Boolean,
  modelId: String,
  onSelect: () -> Unit,
  onCopyUrl: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val modelUrl = "https://huggingface.co/$modelId/resolve/main/${fileInfo.fileName}?download=true"
  val c = glassColors
  val rowTint =
    when {
      isSelected -> Candy.Sky.copy(alpha = if (c.dark) 0.22f else 0.14f)
      fileInfo.isDisabled -> c.glass.copy(alpha = 0.40f)
      else -> c.glass
    }
  val shape = RoundedRectangle(20.dp)
  val rim =
    if (isSelected) Candy.Sky.copy(alpha = 0.6f)
    else Color.White.copy(alpha = if (c.dark) 0.12f else 0.45f)

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(vertical = ITEM_SPACING_XXSMALL)
        .wash(shape, rowTint)
        .border(1.dp, rim, shape)
        .pressable(onClick = onSelect)
        .padding(horizontal = 14.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Minimal rounded indicator circle matching Homepage ChecklistPill.
    Box(
      modifier =
        Modifier.size(20.dp)
          .clip(CircleShape)
          .then(
            if (isSelected) Modifier.background(Candy.Sky)
            else Modifier.border(1.5.dp, if (fileInfo.isDisabled) c.inkFaint else c.inkSoft, CircleShape)
          ),
      contentAlignment = Alignment.Center,
    ) {
      if (isSelected) {
        Icon(
          Icons.Rounded.Check,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(13.dp),
        )
      }
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = fileInfo.fileName,
        style = GalleryType.BodyStrong,
        color = if (fileInfo.isDisabled) c.inkFaint else c.ink,
        modifier = Modifier.fillMaxWidth(),
      )
      if (fileInfo.fileSize > 0L) {
        Spacer(modifier = Modifier.height(ITEM_SPACING_XXSMALL))
        Text(
          text = fileInfo.fileSize.humanReadableSize(),
          style = GalleryType.Caption,
          color = c.inkSoft,
        )
      }
      if (!fileInfo.isCompatible) {
        Text(
          text = stringResource(R.string.incompatible_device_warning),
          style = GalleryType.Caption,
          color = Candy.Tomato,
        )
      } else if (fileInfo.isTooLarge) {
        Text(
          text = stringResource(R.string.file_too_large_warning),
          style = GalleryType.Caption,
          color = Candy.Tomato,
        )
      }
    }
    GlassCircle(
      onClick = { onCopyUrl(modelUrl) },
      size = 34.dp,
      surface = c.glass,
      ink = c.inkSoft,
      label = stringResource(R.string.copy_url),
    ) {
      Icon(
        imageVector = Icons.Rounded.ContentCopy,
        contentDescription = stringResource(R.string.copy_url),
        modifier = Modifier.size(16.dp),
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelStatsRow(
  downloads: Long,
  likes: Long,
  lastModified: String,
  modifier: Modifier = Modifier,
) {
  val c = glassColors
  FlowRow(
    horizontalArrangement = Arrangement.spacedBy(ITEM_SPACING_MEDIUM),
    verticalArrangement = Arrangement.spacedBy(BADGE_VERTICAL_SPACING),
    modifier = modifier.fillMaxWidth(),
  ) {
    if (downloads > 0L) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Rounded.CloudDownload,
          contentDescription = null,
          modifier = Modifier.size(BADGE_ICON_SIZE),
          tint = c.inkSoft,
        )
        Spacer(modifier = Modifier.width(ITEM_SPACING_XXSMALL))
        Text(
          text = formatCount(downloads),
          style = GalleryType.Caption,
          color = c.inkSoft,
        )
      }
    }

    if (likes > 0L) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Rounded.Favorite,
          contentDescription = null,
          modifier = Modifier.size(BADGE_ICON_SIZE),
          tint = c.inkSoft,
        )
        Spacer(modifier = Modifier.width(ITEM_SPACING_XXSMALL))
        Text(
          text = formatCount(likes),
          style = GalleryType.Caption,
          color = c.inkSoft,
        )
      }
    }

    if (lastModified.isNotBlank()) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Rounded.Schedule,
          contentDescription = null,
          modifier = Modifier.size(BADGE_ICON_SIZE),
          tint = c.inkSoft,
        )
        Spacer(modifier = Modifier.width(ITEM_SPACING_XXSMALL))
        Text(
          text = stringResource(R.string.hf_last_updated, formatLastModifiedDate(lastModified)),
          style = GalleryType.Caption,
          color = c.inkSoft,
        )
      }
    }
  }
}
