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

package com.google.ai.edge.gallery.ui.benchmark

import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.ai.edge.gallery.GalleryEvent
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.core.design.components.GlassBackButton
import com.google.ai.edge.gallery.core.design.components.GlassDialog
import com.google.ai.edge.gallery.core.design.components.GlassPill
import com.google.ai.edge.gallery.core.design.components.RoundedRectangle
import com.google.ai.edge.gallery.core.design.components.WashPanel
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType
import com.google.ai.edge.gallery.data.Accelerator
import com.google.ai.edge.gallery.data.Config
import com.google.ai.edge.gallery.data.ConfigKey
import com.google.ai.edge.gallery.data.ConfigKeys
import com.google.ai.edge.gallery.data.DEFAULT_MAX_TOKEN
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.NumberSliderConfig
import com.google.ai.edge.gallery.data.SegmentedButtonConfig
import com.google.ai.edge.gallery.data.ValueType
import com.google.ai.edge.gallery.data.convertValueToTargetType
import com.google.ai.edge.gallery.data.supportModelBenchmark
import com.google.ai.edge.gallery.firebaseAnalytics
import com.google.ai.edge.gallery.ui.common.ConfigEditorsPanel
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.kyant.backdrop.throttleBackdrops
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(
  initialModel: Model,
  modelManagerViewModel: ModelManagerViewModel,
  modifier: Modifier = Modifier,
  viewModel: BenchmarkViewModel = hiltViewModel(),
  hideProgressIndicator: Boolean = false,
  initialShowConfirmationDialog: Boolean = false,
  onBackClicked: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsState()
  var enableBackButton by remember { mutableStateOf(true) }
  var showRunBenchmarkConfirmationDialog by remember {
    mutableStateOf(initialShowConfirmationDialog)
  }
  val downloadedLlmModelNames = remember {
    modelManagerViewModel
      .getAllDownloadedModels()
      .filter { it.isLlm && it.supportModelBenchmark }
      .map { it.name }
  }
  var selectedModelName by remember { mutableStateOf(initialModel.name) }
  var selectedModel by
    remember(selectedModelName) {
      mutableStateOf(modelManagerViewModel.getModelByName(name = selectedModelName)!!)
    }
  val filteredResults = remember { mutableStateListOf<BenchmarkResultInfo>() }
  val configs =
    remember(selectedModel) {
      mutableStateListOf<Config>().apply {
        add(
          SegmentedButtonConfig(
            key = ConfigKeys.ACCELERATOR,
            defaultValue =
              selectedModel.backendSpec.defaultAccelerator?.label ?: Accelerator.CPU.label,
            options = selectedModel.backendSpec.accelerators.map { it.label },
            allowMultiple = false,
          )
        )
        add(
          NumberSliderConfig(
            key = ConfigKeys.PREFILL_TOKENS,
            sliderMin = 16f,
            sliderMax = (selectedModel.llmProfile?.maxTokens ?: DEFAULT_MAX_TOKEN).toFloat(),
            defaultValue = 256f,
            valueType = ValueType.INT,
          )
        )
        add(
          NumberSliderConfig(
            key = ConfigKeys.DECODE_TOKENS,
            sliderMin = 16f,
            sliderMax = 1024f,
            defaultValue = 256f,
            valueType = ValueType.INT,
          )
        )
        add(
          NumberSliderConfig(
            key = ConfigKeys.NUMBER_OF_RUNS,
            sliderMin = 1f,
            sliderMax = 10f,
            defaultValue = 3f,
            valueType = ValueType.INT,
          )
        )
      }
    }

  val values: SnapshotStateMap<String, Any> =
    remember(configs) {
      mutableStateMapOf<String, Any>().apply {
        for (config in configs) {
          put(config.key.label, config.defaultValue)
        }
      }
    }

  val sumOfPrefillAndDecodeTokens =
    getIntConfigValue(values = values, key = ConfigKeys.PREFILL_TOKENS) +
      getIntConfigValue(values = values, key = ConfigKeys.DECODE_TOKENS)
  val maxToken = selectedModel.llmProfile?.maxTokens ?: DEFAULT_MAX_TOKEN

  // Update filteredResults when selected model is changed.
  LaunchedEffect(selectedModelName, uiState.results) {
    filteredResults.clear()
    filteredResults.addAll(
      uiState.results.filter {
        it.benchmarkResult.llmResult?.baiscInfo?.modelName == selectedModelName
      }
    )
  }

  val c = glassColors
  val scrollState = rememberScrollState()
  val cardShape = RoundedRectangle(26.dp)
  val rim = Color.White.copy(alpha = if (c.dark) 0.14f else 0.55f)

  Box(modifier = Modifier.fillMaxSize()) {
    // Benchmark configs & latest real run metrics.
    Scaffold(
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
          Column(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              stringResource(R.string.benchmark_model),
              style =
                GalleryType.Hero.copy(
                  fontSize = 34.sp,
                  lineHeight = 38.sp,
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = (-1.2).sp,
                ),
              color = c.hero,
            )
            BenchmarkModelPicker(
              selectedModelName = selectedModelName,
              modelNames = downloadedLlmModelNames,
              titleResId = R.string.select_downloaded_model,
              onSelected = { selectedModelName = it },
            )
          }
          GlassBackButton(onBack = onBackClicked)
        }
      },
      modifier = Modifier.imePadding(),
    ) { innerPadding ->
      Box(
        modifier = Modifier.padding(innerPadding).fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
      ) {
        Column(
          modifier =
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
              .navigationBarsPadding()
              .fillMaxSize()
        ) {
          // Scrollable dashboard + configuration cards.
          Column(
            modifier =
              Modifier.weight(1f)
                .fillMaxWidth()
                .throttleBackdrops { scrollState.isScrollInProgress }
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            // Latest measured run summary (only shown when real results exist).
            filteredResults.firstOrNull()?.benchmarkResult?.llmResult?.let { latest ->
              val locale = Locale.getDefault()
              val prefill = latest.stats.prefillSpeed.avg
              val decode = latest.stats.decodeSpeed.avg
              val ttft = latest.stats.timeToFirstToken.avg
              val loadMs = latest.stats.firstInitTimeMs
              val maxSpeed = maxOf(prefill, decode, 1.0)
              WashPanel(
                shape = cardShape,
                tint = c.glass,
                modifier = Modifier.fillMaxWidth().border(1.dp, rim, cardShape),
              ) {
                Column(
                  modifier = Modifier.padding(16.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text("Latest measured run", style = GalleryType.BodyStrong, color = c.ink)
                    Text(
                      latest.baiscInfo.accelerator,
                      style = GalleryType.Caption,
                      color = Candy.Sky,
                    )
                  }
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    BenchmarkMetricTile(
                      label = "TTFT",
                      value = String.format(locale, "%.2f s", ttft),
                      modifier = Modifier.weight(1f),
                    )
                    BenchmarkMetricTile(
                      label = "Decode",
                      value = String.format(locale, "%.1f tok/s", decode),
                      modifier = Modifier.weight(1f),
                    )
                    BenchmarkMetricTile(
                      label = "Load",
                      value = String.format(locale, "%.0f ms", loadMs),
                      modifier = Modifier.weight(1f),
                    )
                  }
                  // Clean horizontal bar comparison of real Prefill vs Decode throughput.
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Text("Prefill vs Decode throughput", style = GalleryType.Caption, color = c.inkSoft)
                      Text(
                        String.format(locale, "%.1f / %.1f tok/s", prefill, decode),
                        style = GalleryType.Caption,
                        color = c.ink,
                      )
                    }
                    Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
                      val trackColor = c.inkFaint.copy(alpha = 0.16f)
                      val barHeight = 8.dp.toPx()
                      val gap = 6.dp.toPx()
                      val prefillW = ((prefill / maxSpeed).toFloat().coerceIn(0f, 1f)) * size.width
                      val decodeW = ((decode / maxSpeed).toFloat().coerceIn(0f, 1f)) * size.width
                      drawRoundRect(trackColor, Offset.Zero, Size(size.width, barHeight), CornerRadius(barHeight / 2f))
                      drawRoundRect(Candy.Sky, Offset.Zero, Size(prefillW, barHeight), CornerRadius(barHeight / 2f))
                      drawRoundRect(trackColor, Offset(0f, barHeight + gap), Size(size.width, barHeight), CornerRadius(barHeight / 2f))
                      drawRoundRect(Candy.Mint, Offset(0f, barHeight + gap), Size(decodeW, barHeight), CornerRadius(barHeight / 2f))
                    }
                  }
                }
              }
            }

            WashPanel(
              shape = cardShape,
              tint = c.glass,
              modifier = Modifier.fillMaxWidth().border(1.dp, rim, cardShape),
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
              ) {
                ConfigEditorsPanel(configs = configs, values = values)

                // Info text on the limit of the sum of prefill and decode tokens.
                Text(
                  stringResource(
                    R.string.benchmark_tokens_limit_message,
                    sumOfPrefillAndDecodeTokens,
                    maxToken,
                  ),
                  style = GalleryType.Caption,
                  color =
                    if (sumOfPrefillAndDecodeTokens > maxToken) Candy.Tangerine
                    else c.inkSoft,
                )
              }
            }
          }

          // Floating bottom pill actions.
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp).fillMaxWidth(),
          ) {
            GlassPill(
              enabled = filteredResults.isNotEmpty(),
              onClick = {
                viewModel.setShowResultsViewer(showResultsViewer = true)
                firebaseAnalytics?.logEvent(
                  GalleryEvent.BUTTON_CLICKED.id,
                  Bundle().apply {
                    putString("event_type", "view_benchmark_results")
                    putString("model_id", selectedModelName)
                  },
                )
              },
              height = 50.dp,
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                Icons.AutoMirrored.Rounded.List,
                contentDescription = null,
                tint = if (filteredResults.isNotEmpty()) c.ink else c.inkFaint,
                modifier = Modifier.size(18.dp),
              )
              Text(
                stringResource(R.string.view_results),
                style = GalleryType.BodyStrong,
                color = if (filteredResults.isNotEmpty()) c.ink else c.inkFaint,
              )
            }
            val canRun = sumOfPrefillAndDecodeTokens <= maxToken
            GlassPill(
              enabled = canRun,
              tint = if (canRun) Candy.Sky else c.inkFaint,
              onClick = {
                modelManagerViewModel.getModelByName(name = selectedModelName)?.let {
                  showRunBenchmarkConfirmationDialog = true
                }
              },
              height = 50.dp,
              modifier = Modifier.weight(1f),
            ) {
              Icon(
                Icons.Rounded.BarChart,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
              Text(
                stringResource(R.string.benchmark),
                style = GalleryType.BodyStrong,
                color = Color.White,
              )
            }
          }
        }
      }
    }

    // Results viewer.
    AnimatedVisibility(
      visible = uiState.showResultsViewer,
      enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut(),
    ) {
      BenchmarkResultsViewer(
        initialModelName = selectedModelName,
        modelManagerViewModel = modelManagerViewModel,
        viewModel = viewModel,
        hideProgressIndicator = hideProgressIndicator,
        onClose = { viewModel.setShowResultsViewer(showResultsViewer = false) },
      )
    }
  }

  // Confirmation dialog for running benchmark.
  if (showRunBenchmarkConfirmationDialog) {
    GlassDialog(
      onDismiss = { showRunBenchmarkConfirmationDialog = false },
      title = stringResource(R.string.run_benchmark),
      confirm = stringResource(R.string.continue_button_label),
      onConfirm = {
        viewModel.runBenchmark(
          model = selectedModel,
          accelerator = getStringConfigValue(values = values, key = ConfigKeys.ACCELERATOR),
          prefillTokens = getIntConfigValue(values = values, key = ConfigKeys.PREFILL_TOKENS),
          decodeTokens = getIntConfigValue(values = values, key = ConfigKeys.DECODE_TOKENS),
          runCount = getIntConfigValue(values = values, key = ConfigKeys.NUMBER_OF_RUNS),
        )
        firebaseAnalytics?.logEvent(
          GalleryEvent.BUTTON_CLICKED.id,
          Bundle().apply {
            putString("event_type", "run_benchmark")
            putString("model_id", selectedModelName)
          },
        )
        showRunBenchmarkConfirmationDialog = false
      },
      dismiss = stringResource(R.string.cancel),
    ) {
      Text(
        stringResource(R.string.run_benchmark_confirmation_msg),
        style = GalleryType.Body,
        color = c.inkSoft,
      )
    }
  }
}

@Composable
private fun BenchmarkMetricTile(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  val c = glassColors
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(label, style = GalleryType.Caption, color = c.inkSoft)
    Text(value, style = GalleryType.BodyStrong, color = c.ink)
  }
}

private fun getStringConfigValue(values: Map<String, Any>, key: ConfigKey): String {
  return convertValueToTargetType(value = values.get(key.label) ?: "", valueType = ValueType.STRING)
    as String
}

private fun getIntConfigValue(values: Map<String, Any>, key: ConfigKey): Int {
  return convertValueToTargetType(value = values.get(key.label) ?: 0, valueType = ValueType.INT)
    as Int
}
