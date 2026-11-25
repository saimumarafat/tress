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

package com.tress.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tress.app.R
import com.tress.app.core.design.components.Capsule
import com.tress.app.core.design.components.GlassPill
import com.tress.app.core.design.components.GlassSheet
import com.tress.app.core.design.components.RoundedRectangle
import com.tress.app.core.design.components.WashPanel
import com.tress.app.core.design.components.wash
import com.tress.app.core.design.theme.Candy
import com.tress.app.core.design.theme.glassColors
import com.tress.app.core.design.typography.GalleryType
import com.tress.app.data.Model
import com.tress.app.data.ModelDownloadStatus
import com.tress.app.data.ModelDownloadStatusType
import com.kyant.backdrop.throttleBackdrops

/** Extracted technical details derived strictly from the real [Model] metadata without fabrication. */
data class ExtractedModelDetails(
  val displayName: String,
  val author: String?,
  val repository: String?,
  val architecture: String?,
  val parameterCount: String?,
  val quantization: String?,
  val sizeLabel: String,
  val modalities: List<String>,
  val capabilities: List<String>,
  val hardwareAccelerators: List<String>,
  val runtimeLabel: String,
  val minMemoryGb: Int?,
)

private val PARAM_REGEX = Regex("""(?i)\b(\d+(?:\.\d+)?[BM])\b""")
private val QUANT_REGEX = Regex("""(?i)\b(q\d+(?:_k(?:_[ms])?|_\d+)?|int[48]|fp16|bf16|f16|f32|dynamic_int\d+)\b""")

fun extractRealModelDetails(model: Model): ExtractedModelDetails {
  val rawId = model.modelId.ifBlank { model.downloadInfo.url }
  val hfRepo = rawId.substringAfter("huggingface.co/", "").split("/").takeIf { it.size >= 2 && rawId.contains("huggingface.co/") }?.let { "${it[0]}/${it[1]}" }
    ?: model.modelId.takeIf { it.contains("/") }
  val author = hfRepo?.substringBefore('/')?.takeIf { it.isNotBlank() }
    ?: when {
      model.name.contains("Gemma", ignoreCase = true) -> "Google"
      model.name.contains("Gemini", ignoreCase = true) -> "Google"
      else -> null
    }
  val combinedSearch = "${model.name} ${model.displayName} ${model.downloadInfo.downloadFileName}"
  val arch = when {
    combinedSearch.contains("gemma-3n", ignoreCase = true) || combinedSearch.contains("gemma3n", ignoreCase = true) -> "Gemma 3n"
    combinedSearch.contains("gemma-3", ignoreCase = true) || combinedSearch.contains("gemma3", ignoreCase = true) -> "Gemma 3"
    combinedSearch.contains("gemma-2", ignoreCase = true) || combinedSearch.contains("gemma2", ignoreCase = true) -> "Gemma 2"
    combinedSearch.contains("gemma", ignoreCase = true) -> "Gemma"
    combinedSearch.contains("qwen", ignoreCase = true) -> "Qwen"
    combinedSearch.contains("llama", ignoreCase = true) -> "Llama"
    combinedSearch.contains("phi", ignoreCase = true) -> "Phi"
    combinedSearch.contains("deepseek", ignoreCase = true) -> "DeepSeek"
    combinedSearch.contains("smolvlm", ignoreCase = true) -> "SmolVLM"
    combinedSearch.contains("gemini", ignoreCase = true) -> "Gemini Nano (AICore)"
    else -> null
  }
  val params = PARAM_REGEX.find(combinedSearch)?.groupValues?.getOrNull(1)?.uppercase()
  val quant = QUANT_REGEX.find(combinedSearch)?.groupValues?.getOrNull(1)?.uppercase()
  val totalBytes = model.downloadInfo.sizeInBytes
  val sizeLabel = if (totalBytes > 0L) totalBytes.humanReadableSize() else if (model.backendSpec.isAiCore) "System managed" else "Not specified"

  val modalities = buildList {
    add("Text")
    if (model.supportImage) add("Vision / Image")
    if (model.supportAudio) add("Audio")
  }
  val capabilities = buildList {
    model.capabilities.forEach { add(it.name.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)) }
    if (model.llmProfile?.supportTinyGarden == true) add("Tiny Garden")
    if (model.llmProfile?.supportMobileActions == true) add("Mobile Actions")
  }.distinct()

  val accelerators = model.backendSpec.accelerators.map { it.label }
  val runtime = when {
    model.backendSpec.isAiCore -> "Android AICore"
    model.backendSpec.isLiteRtLm -> "LiteRT-LM"
    else -> model.backendSpec.runtimeType.name
  }

  return ExtractedModelDetails(
    displayName = model.displayName.ifEmpty { model.name },
    author = author,
    repository = hfRepo ?: model.learnMoreUrl.takeIf { it.isNotBlank() },
    architecture = arch,
    parameterCount = params,
    quantization = quant,
    sizeLabel = sizeLabel,
    modalities = modalities,
    capabilities = capabilities,
    hardwareAccelerators = accelerators,
    runtimeLabel = runtime,
    minMemoryGb = model.minDeviceMemoryInGb,
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelDetailsSheet(
  model: Model,
  downloadStatus: ModelDownloadStatus?,
  onDismiss: () -> Unit,
  onOpenBenchmark: (() -> Unit)? = null,
) {
  val c = glassColors
  val details = extractRealModelDetails(model)
  val uriHandler = LocalUriHandler.current
  val scrollState = rememberScrollState()
  val panelShape = RoundedRectangle(26.dp)
  val rim = Color.White.copy(alpha = if (c.dark) 0.14f else 0.55f)

  GlassSheet(
    onDismiss = onDismiss,
    title = details.displayName,
    subtitle = listOfNotNull(details.author, details.runtimeLabel).joinToString(" · "),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 520.dp)
        .throttleBackdrops { scrollState.isScrollInProgress }
        .verticalScroll(scrollState)
        .padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Download status banner
      val statusType = downloadStatus?.status
      val (statusText, statusTint, statusIcon) = when (statusType) {
        ModelDownloadStatusType.SUCCEEDED -> Triple("Downloaded & ready on device", Candy.Lime, Icons.Rounded.CheckCircle)
        ModelDownloadStatusType.IN_PROGRESS -> Triple("Downloading...", Candy.Sky, Icons.Rounded.CloudDownload)
        ModelDownloadStatusType.FAILED -> Triple("Download failed", Candy.Tomato, Icons.Rounded.ErrorOutline)
        else -> Triple("Not downloaded", Candy.Tangerine, Icons.Rounded.CloudDownload)
      }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .wash(Capsule, tint = statusTint.copy(alpha = if (c.dark) 0.16f else 0.12f))
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(statusIcon, null, tint = statusTint, modifier = Modifier.size(18.dp))
        Text(statusText, style = GalleryType.BodyStrong, color = c.ink, modifier = Modifier.weight(1f))
        Text(details.sizeLabel, style = GalleryType.Label, color = c.inkSoft)
      }

      // Specifications card
      WashPanel(shape = panelShape, modifier = Modifier.fillMaxWidth().border(1.dp, rim, panelShape)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("SPECS", style = GalleryType.Eyebrow, color = c.inkSoft)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            val skyTone = c.tone(com.tress.app.core.design.theme.Tone.Sky)
            val sageTone = c.tone(com.tress.app.core.design.theme.Tone.Sage)
            Column(
              modifier =
                Modifier.weight(1f)
                  .clip(RoundedRectangle(18.dp))
                  .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(skyTone.top, skyTone.bottom))
                  )
                  .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Text("RUNTIME", style = GalleryType.Eyebrow, color = Color.White.copy(alpha = 0.78f))
              Text(details.runtimeLabel, style = GalleryType.BodyStrong, color = Color.White, maxLines = 1)
              Text(details.sizeLabel, style = GalleryType.Caption, color = Color.White.copy(alpha = 0.88f))
            }
            Column(
              modifier =
                Modifier.weight(1f)
                  .clip(RoundedRectangle(18.dp))
                  .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(sageTone.top, sageTone.bottom))
                  )
                  .padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Text("PARAMETERS", style = GalleryType.Eyebrow, color = sageTone.ink.copy(alpha = 0.78f))
              Text(
                details.parameterCount ?: details.architecture ?: "On-device",
                style = GalleryType.BodyStrong,
                color = sageTone.ink,
                maxLines = 1,
              )
              Text(
                listOfNotNull(
                    details.quantization,
                    details.minMemoryGb?.let { "${it}GB RAM" },
                  )
                  .joinToString(" · ")
                  .ifBlank { details.author ?: "Local" },
                style = GalleryType.Caption,
                color = sageTone.ink.copy(alpha = 0.85f),
                maxLines = 1,
              )
            }
          }
          model.llmProfile?.maxTokens?.let { DetailSpecRow("Context", "$it tokens") }
        }
      }

      // Modalities & Hardware compatibility
      WashPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("CAPABILITIES", style = GalleryType.Eyebrow, color = c.inkSoft)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            details.modalities.forEach { mod ->
              SpecBadge(label = mod, accent = Candy.Sky)
            }
            details.hardwareAccelerators.forEach { acc ->
              SpecBadge(label = acc, accent = Candy.Mint, icon = Icons.Rounded.Memory)
            }
            details.capabilities.forEach { cap ->
              SpecBadge(label = cap, accent = Candy.Grape, icon = Icons.Rounded.AutoAwesome)
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (model.learnMoreUrl.isNotBlank()) {
          GlassPill(
            onClick = { runCatching { uriHandler.openUri(model.learnMoreUrl) } },
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = c.ink, modifier = Modifier.size(16.dp))
            Text(stringResource(R.string.learn_more), style = GalleryType.BodyStrong, color = c.ink)
          }
        }
        if (onOpenBenchmark != null && statusType == ModelDownloadStatusType.SUCCEEDED) {
          GlassPill(
            onClick = {
              onDismiss()
              onOpenBenchmark()
            },
            tint = Candy.Sky,
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Rounded.Storage, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text("Benchmark", style = GalleryType.BodyStrong, color = Color.White)
          }
        }
      }
      Spacer(Modifier.height(6.dp))
    }
  }
}

@Composable
private fun DetailSpecRow(label: String, value: String) {
  val c = glassColors
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(label, style = GalleryType.Label, color = c.inkSoft)
    Spacer(Modifier.width(12.dp))
    Text(value, style = GalleryType.BodyStrong.copy(fontWeight = FontWeight.Medium), color = c.ink)
  }
}

@Composable
private fun SpecBadge(
  label: String,
  accent: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
  val c = glassColors
  Row(
    modifier = Modifier
      .wash(Capsule, tint = accent.copy(alpha = if (c.dark) 0.18f else 0.12f))
      .padding(horizontal = 10.dp, vertical = 5.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    if (icon != null) {
      Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
    }
    Text(label, style = GalleryType.Caption.copy(fontWeight = FontWeight.SemiBold), color = c.ink)
  }
}
