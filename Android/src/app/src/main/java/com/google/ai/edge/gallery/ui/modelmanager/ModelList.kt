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

package com.google.ai.edge.gallery.ui.modelmanager

// import androidx.compose.ui.tooling.preview.Preview
// import com.google.ai.edge.gallery.ui.preview.PreviewModelManagerViewModel
// import com.google.ai.edge.gallery.ui.preview.TASK_TEST1
// import com.google.ai.edge.gallery.ui.theme.GalleryTheme

import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.core.design.components.Capsule
import com.google.ai.edge.gallery.core.design.components.wash
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.data.supportModelBenchmark
import com.google.ai.edge.gallery.ui.common.ClickableLink
import com.google.ai.edge.gallery.ui.common.RevealingText
import com.google.ai.edge.gallery.ui.common.TaskIcon
import com.google.ai.edge.gallery.ui.common.getTaskBgGradientColors
import com.google.ai.edge.gallery.ui.common.modelitem.ModelItem
import com.google.ai.edge.gallery.ui.common.rememberDelayedAnimationProgress
import com.google.ai.edge.gallery.ui.theme.bodyLargeNarrow
import com.google.ai.edge.gallery.ui.theme.headlineLargeMedium
import com.kyant.backdrop.throttleBackdrops

private const val TAG = "AGModelList"
private val CONTENT_ANIMATION_OFFSET = 6.dp
private const val ANIMATION_INIT_DELAY = 0L
private const val TASK_DESCRIPTION_SECTION_ANIMATION_START = 30
private const val MODEL_LIST_ANIMATION_START = 40
private const val DEFAULT_ANIMATION_DURATION = 150
private const val TASK_ICON_ANIMATION_DURATION = 170

private val ModelBentoToneCycle =
  listOf(
    com.google.ai.edge.gallery.core.design.theme.Tone.Sky,
    com.google.ai.edge.gallery.core.design.theme.Tone.Butter,
    com.google.ai.edge.gallery.core.design.theme.Tone.Orchid,
    com.google.ai.edge.gallery.core.design.theme.Tone.Sage,
    com.google.ai.edge.gallery.core.design.theme.Tone.Lavender,
    com.google.ai.edge.gallery.core.design.theme.Tone.Peach,
  )

/** The list of models in the model manager. */
@Composable
fun ModelList(
  task: Task,
  modelManagerViewModel: ModelManagerViewModel,
  contentPadding: PaddingValues,
  enableAnimation: Boolean,
  onModelClicked: (Model) -> Unit,
  onBenchmarkClicked: (Model) -> Unit,
  modifier: Modifier = Modifier,
) {
  // This is just to update "models" list when task.updateTrigger is updated so that the UI can
  // be properly updated.
  val models by
    remember(task) {
      derivedStateOf {
        val trigger = task.updateTrigger.value
        if (trigger >= 0) {
          task.models.toList().filter { !it.downloadInfo.imported }
        } else {
          listOf()
        }
      }
    }
  val importedModels by
    remember(task) {
      derivedStateOf {
        val trigger = task.updateTrigger.value
        if (trigger >= 0) {
          task.models.toList().filter { it.downloadInfo.imported }
        } else {
          listOf()
        }
      }
    }
  val modelVariants by
    remember(task) {
      derivedStateOf {
        val trigger = task.updateTrigger.value
        if (trigger >= 0) {
          task.models
            .toList()
            .filter { it.isVariant }
            .groupBy { it.hierarchy.parentModelName.orEmpty() }
        } else {
          mapOf()
        }
      }
    }

  val listState = rememberLazyStaggeredGridState()

  val headerAnimation = rememberTaskHeaderAnimation(enableAnimation = enableAnimation)
  val modelListProgress = headerAnimation.modelListProgress
  val modelItemExpandedStates = remember { mutableStateMapOf<String, Boolean>() }

  Box(
    contentAlignment = Alignment.BottomEnd,
    modifier = Modifier.background(Color.Transparent),
  ) {
    val nonVariantModels = remember(models) { models.filter { !it.isVariant } }
    LazyVerticalStaggeredGrid(
      columns = StaggeredGridCells.Fixed(2),
      modifier = modifier.throttleBackdrops { listState.isScrollInProgress }.padding(horizontal = 16.dp),
      contentPadding = contentPadding,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalItemSpacing = 12.dp,
      state = listState,
    ) {
      // Minimal count/badge bar (no API docs, no example code, no duplicate title).
      item(span = StaggeredGridItemSpan.FullLine, key = "taskHeader") {
        TaskHeader(
          task = task,
          modelCount = models.size + importedModels.size,
          animation = headerAnimation,
        )
      }

      // Bento model cards (2-column colorful grid matching Home & Files/Pdf reference).
      itemsIndexed(
        items = nonVariantModels,
        key = { _, model -> model.name },
        span = { _, model ->
          val variants = modelVariants.getOrDefault(model.name, listOf())
          if (variants.isNotEmpty()) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane
        },
      ) { idx, model ->
        val expanded = modelItemExpandedStates.getOrDefault(model.name, null)
        val cardTone = glassColors.tone(ModelBentoToneCycle[idx % ModelBentoToneCycle.size])
        ModelItem(
          model = model,
          modelVariants = modelVariants.getOrDefault(model.name, listOf()),
          task = task,
          modelManagerViewModel = modelManagerViewModel,
          onModelClicked = onModelClicked,
          onBenchmarkClicked = onBenchmarkClicked,
          expanded = expanded,
          onExpanded = { modelItemExpandedStates[model.name] = it },
          isBenchmarkSupported = model.supportModelBenchmark,
          cardTone = if (modelVariants.getOrDefault(model.name, listOf()).isEmpty()) cardTone else null,
          modifier =
            Modifier.graphicsLayer {
              alpha = modelListProgress
              translationY = (CONTENT_ANIMATION_OFFSET * (1 - modelListProgress)).toPx()
            },
        )
      }

      // Title for imported models.
      if (importedModels.isNotEmpty()) {
        item(span = StaggeredGridItemSpan.FullLine, key = "importedModelsTitle") {
          Text(
            stringResource(R.string.model_list_imported_models_title).uppercase(),
            color = glassColors.inkSoft,
            style = GalleryType.Eyebrow,
            modifier =
              Modifier.padding(horizontal = 4.dp)
                .padding(top = 12.dp, bottom = 2.dp)
                .graphicsLayer {
                  alpha = modelListProgress
                  translationY = (CONTENT_ANIMATION_OFFSET * (1 - modelListProgress)).toPx()
                },
          )
        }
      }

      // List of imported models within a task.
      itemsIndexed(items = importedModels, key = { _, model -> model.name }) { idx, model ->
        val cardTone = glassColors.tone(ModelBentoToneCycle[(nonVariantModels.size + idx) % ModelBentoToneCycle.size])
        ModelItem(
          model = model,
          task = task,
          modelManagerViewModel = modelManagerViewModel,
          onModelClicked = onModelClicked,
          onBenchmarkClicked = onBenchmarkClicked,
          isBenchmarkSupported = model.supportModelBenchmark,
          cardTone = cardTone,
          modifier =
            Modifier.graphicsLayer {
              alpha = modelListProgress
              translationY = (CONTENT_ANIMATION_OFFSET * (1 - modelListProgress)).toPx()
            },
        )
      }
    }

    // Subtle bottom fade over navigation bar.
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(contentPadding.calculateBottomPadding())
          .background(
            Brush.verticalGradient(
              colors = listOf(Color.Transparent, glassColors.desk.copy(alpha = 0.72f))
            )
          )
          .align(Alignment.BottomCenter)
    )
  }
}

/** Progress of the staggered entrance animation of [TaskHeader] and the list below it. */
data class TaskHeaderAnimation(
  val taskIconProgress: Float,
  val taskLabelProgress: Float,
  val descriptionProgress: Float,
  val modelListProgress: Float,
)

/** Runs the entrance animation of [TaskHeader], or skips it when [enableAnimation] is false. */
@Composable
fun rememberTaskHeaderAnimation(enableAnimation: Boolean): TaskHeaderAnimation {
  val taskIconProgress =
    if (!enableAnimation) {
      1f
    } else {
      rememberDelayedAnimationProgress(
        initialDelay = ANIMATION_INIT_DELAY,
        animationDurationMs = TASK_ICON_ANIMATION_DURATION,
        animationLabel = "task icon",
      )
    }

  val taskLabelProgress =
    if (!enableAnimation) {
      1f
    } else {
      rememberDelayedAnimationProgress(
        initialDelay = ANIMATION_INIT_DELAY + 90,
        animationDurationMs = TASK_ICON_ANIMATION_DURATION,
        animationLabel = "task label",
      )
    }

  val descriptionProgress =
    if (!enableAnimation) {
      1f
    } else {
      rememberDelayedAnimationProgress(
        initialDelay = ANIMATION_INIT_DELAY + TASK_DESCRIPTION_SECTION_ANIMATION_START,
        animationDurationMs = DEFAULT_ANIMATION_DURATION,
        animationLabel = "description",
      )
    }

  val modelListProgress =
    if (!enableAnimation) {
      1f
    } else {
      rememberDelayedAnimationProgress(
        initialDelay = ANIMATION_INIT_DELAY + MODEL_LIST_ANIMATION_START,
        animationDurationMs = DEFAULT_ANIMATION_DURATION,
        animationLabel = "model_list",
      )
    }
  return TaskHeaderAnimation(
    taskIconProgress = taskIconProgress,
    taskLabelProgress = taskLabelProgress,
    descriptionProgress = descriptionProgress,
    modelListProgress = modelListProgress,
  )
}

/**
 * The header at the top of a task's model list: the task icon, name, description, links, and the
 * number of available models.
 *
 * @param modelCountText Replaces the "N models available" line, for example with per-kind counts.
 */
@Composable
fun TaskHeader(
  task: Task,
  modelCount: Int,
  animation: TaskHeaderAnimation,
  modifier: Modifier = Modifier,
  modelCountText: String? = null,
) {
  val resources = LocalContext.current.resources
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(top = 4.dp, bottom = 2.dp)
        .graphicsLayer {
          alpha = animation.descriptionProgress
          translationY = (CONTENT_ANIMATION_OFFSET * (1 - animation.descriptionProgress)).toPx()
        },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(modifier = Modifier.wash(Capsule, glassColors.glass)) {
      Text(
        text =
          modelCountText
            ?: resources.getQuantityString(
              R.plurals.model_list_number_of_models_available,
              modelCount,
              modelCount,
            ),
        style = GalleryType.Caption.copy(fontWeight = FontWeight.SemiBold),
        color = glassColors.inkSoft,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
      )
    }
    if (task.experimental) {
      Box(modifier = Modifier.wash(Capsule, glassColors.glass)) {
        Text(
          text = stringResource(R.string.model_list_experimental_label),
          style = GalleryType.Caption.copy(fontWeight = FontWeight.Bold),
          color = glassColors.ink,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
      }
    }
  }
}

// @Preview(showBackground = true)
// @Composable
// fun ModelListPreview() {
//   val context = LocalContext.current

//   GalleryTheme {
//     ModelList(
//       task = TASK_TEST1,
//       modelManagerViewModel = PreviewModelManagerViewModel(context = context),
//       onModelClicked = {},
//       contentPadding = PaddingValues(all = 16.dp),
//     )
//   }
// }
