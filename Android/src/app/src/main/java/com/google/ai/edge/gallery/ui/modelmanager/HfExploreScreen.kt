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

package com.google.ai.edge.gallery.ui.modelmanager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.core.design.components.Capsule
import com.google.ai.edge.gallery.core.design.components.GlassCircle
import com.google.ai.edge.gallery.core.design.components.GlassLoader
import com.google.ai.edge.gallery.core.design.components.GlassTextField
import com.google.ai.edge.gallery.core.design.components.HeaderGlassButton
import com.google.ai.edge.gallery.core.design.components.RoundedRectangle
import com.google.ai.edge.gallery.core.design.components.pressable
import com.google.ai.edge.gallery.core.design.components.wash
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType
import com.google.ai.edge.gallery.huggingface.isCommunityRecommended
import com.google.ai.edge.gallery.huggingface.isDeviceCompatible
import com.google.ai.edge.gallery.huggingface.modelName
import com.google.ai.edge.gallery.proto.HfModelItemProto
import com.google.ai.edge.gallery.proto.HfSortOptionProto
import com.google.ai.edge.gallery.ui.common.EmptyState
import com.google.ai.edge.gallery.ui.common.EmptyStateButtonConfig
import com.kyant.backdrop.throttleBackdrops

private val SORT_OPTIONS: List<Pair<HfSortOptionProto, Int>> =
  listOf(
    HfSortOptionProto.HF_SORT_OPTION_TRENDING to R.string.hf_explore_sort_trending,
    HfSortOptionProto.HF_SORT_OPTION_DOWNLOADS to R.string.hf_explore_sort_downloads,
    HfSortOptionProto.HF_SORT_OPTION_LIKES to R.string.hf_explore_sort_likes,
    HfSortOptionProto.HF_SORT_OPTION_RECENT to R.string.hf_explore_sort_recent,
  )

@Composable
fun HfExploreScreen(
  onNavigateUp: () -> Unit,
  onOpenUrlImportDialog: () -> Unit,
  onModelCardSelected: (HfModelItemProto) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: HfExploreViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(uiState.selectedModelForDetails) {
    val selectedModel = uiState.selectedModelForDetails ?: return@LaunchedEffect
    onModelCardSelected(selectedModel)
    viewModel.onModelDetailsShown()
  }

  BackHandler(onBack = onNavigateUp)

  val c = glassColors
  val screenBg = if (c.dark) Color.Black else c.canvas

  // Rendered inside the main window canvas so backdrop sampling never blurs over a Dialog window.
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(screenBg)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
  ) {
    HfExploreContent(
      uiState = uiState,
      onNavigateUp = onNavigateUp,
      onOpenUrlImportDialog = onOpenUrlImportDialog,
      onSearchQueryChanged = viewModel::onSearchQueryChanged,
      onSortOptionSelected = viewModel::onSortOptionSelected,
      onRetry = viewModel::refreshModels,
      onModelCardClicked = viewModel::selectModelCard,
      modifier = Modifier.fillMaxSize(),
    )
  }
}

@Composable
fun HfExploreContent(
  uiState: HfExploreUiState,
  onNavigateUp: () -> Unit,
  onOpenUrlImportDialog: () -> Unit,
  onSearchQueryChanged: (String) -> Unit,
  onSortOptionSelected: (HfSortOptionProto) -> Unit,
  onRetry: () -> Unit,
  onModelCardClicked: (HfModelItemProto) -> Unit,
  modifier: Modifier = Modifier,
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val c = glassColors

  Scaffold(
    modifier = modifier.fillMaxSize(),
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
        Text(
          text = stringResource(R.string.hf_explore_title),
          style =
            GalleryType.Hero.copy(
              fontSize = 36.sp,
              lineHeight = 40.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = (-1.3).sp,
            ),
          color = c.hero,
          maxLines = 1,
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          HeaderGlassButton(
            onClick = onOpenUrlImportDialog,
            label = stringResource(R.string.cd_hf_import_from_url_button),
            size = 40.dp,
          ) {
            Icon(
              imageVector = Icons.Rounded.Link,
              contentDescription = stringResource(R.string.cd_hf_import_from_url_button),
              tint = Candy.Sky,
              modifier = Modifier.size(19.dp),
            )
          }
          HeaderGlassButton(
            onClick = onNavigateUp,
            label = stringResource(R.string.cd_navigate_back_icon),
            size = 40.dp,
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.cd_navigate_back_icon),
              tint = c.ink,
              modifier = Modifier.size(19.dp),
            )
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Liquid-glass search field.
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          GlassTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = stringResource(R.string.hf_explore_search_placeholder),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
            modifier = Modifier.weight(1f),
          )
          if (uiState.searchQuery.isNotEmpty()) {
            GlassCircle(
              onClick = { onSearchQueryChanged("") },
              size = 40.dp,
              surface = c.glass,
              ink = c.ink,
              label = stringResource(R.string.cd_clear_search_button),
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.cd_clear_search_button),
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }

        // Bold minimal inverted sort pills matching Homepage.
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .horizontalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          for ((option, labelRes) in SORT_OPTIONS) {
            val selected = uiState.selectedSort == option
            Box(
              modifier =
                Modifier.then(
                    if (selected) Modifier.clip(Capsule).background(c.ink)
                    else Modifier.wash(shape = Capsule, tint = c.glass)
                  )
                  .pressable(onClick = { onSortOptionSelected(option) })
                  .padding(horizontal = 18.dp, vertical = 10.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = stringResource(labelRes),
                style = GalleryType.BodyStrong.copy(fontWeight = FontWeight.Bold),
                color = if (selected) c.canvas else c.ink,
                maxLines = 1,
              )
            }
          }
        }

        // Model cards or loading/empty state.
        when {
          uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              GlassLoader(color = Candy.Sky)
            }
          }
          uiState.hasError || uiState.models.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              EmptyState(
                titleResId = R.string.hf_explore_title,
                descriptionResId =
                  if (uiState.hasError) {
                    R.string.hf_explore_error_loading
                  } else {
                    R.string.hf_explore_empty_results
                  },
                icon =
                  if (uiState.hasError) {
                    Icons.Rounded.ErrorOutline
                  } else {
                    Icons.Rounded.SearchOff
                  },
                buttonConfig =
                  if (uiState.hasError) {
                    EmptyStateButtonConfig(
                      buttonLabelResId = R.string.hf_explore_retry,
                      onButtonClick = onRetry,
                    )
                  } else {
                    null
                  },
              )
            }
          }
          else -> {
            HfExploreModelList(
              models = uiState.models,
              contentPadding =
                PaddingValues(top = 6.dp, bottom = innerPadding.calculateBottomPadding() + 28.dp),
              onModelCardClicked = onModelCardClicked,
            )
          }
        }
      }

      if (uiState.isFetchingDetails) {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(Color.Black.copy(alpha = 0.42f))
              .pointerInput(Unit) {},
          contentAlignment = Alignment.Center,
        ) {
          GlassLoader(color = Candy.Sky)
        }
      }
    }
  }
}

@Composable
private fun HfExploreModelList(
  models: List<HfModelItemProto>,
  contentPadding: PaddingValues,
  onModelCardClicked: (HfModelItemProto) -> Unit,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()
  LazyColumn(
    modifier =
      modifier
        .fillMaxSize()
        .throttleBackdrops { listState.isScrollInProgress }
        .padding(horizontal = 16.dp)
        .navigationBarsPadding(),
    state = listState,
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = contentPadding,
  ) {
    items(models, key = { it.id }) { model ->
      HfExploreModelCard(model = model, onClick = { onModelCardClicked(model) })
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HfExploreModelCard(
  model: HfModelItemProto,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val c = glassColors
  val isCompatible = model.isDeviceCompatible()
  val isRecommended = model.isCommunityRecommended()
  val authorName = model.author.ifEmpty { model.id.substringBefore("/", "") }
  val shape = RoundedRectangle(26.dp)
  val rim = Color.White.copy(alpha = if (c.dark) 0.14f else 0.55f)

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .wash(shape = shape, tint = c.glass)
        .border(1.dp, rim, shape)
        .pressable(onClick = onClick)
        .padding(18.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = model.modelName,
        style = GalleryType.CardTitle.copy(fontWeight = FontWeight.Bold),
        color = c.ink,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (authorName.isNotEmpty()) {
        Text(
          text = authorName,
          style = GalleryType.Label,
          color = c.inkSoft,
        )
      }
    }

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (isRecommended) {
        Box(
          modifier =
            Modifier.clip(Capsule)
              .background(Candy.Sky.copy(alpha = 0.18f))
              .padding(horizontal = 11.dp, vertical = 4.dp)
        ) {
          Text(
            text = stringResource(R.string.hf_explore_recommended_badge),
            style = GalleryType.Caption.copy(fontWeight = FontWeight.SemiBold),
            color = Candy.Sky,
          )
        }
      }

      Box(
        modifier =
          Modifier.clip(Capsule)
            .background(
              if (isCompatible) Candy.Mint.copy(alpha = 0.18f)
              else c.inkFaint.copy(alpha = 0.12f)
            )
            .padding(horizontal = 11.dp, vertical = 4.dp)
      ) {
        Text(
          text =
            stringResource(
              if (isCompatible) {
                R.string.hf_explore_compatible_badge
              } else {
                R.string.hf_explore_incompatible_badge
              }
            ),
          style = GalleryType.Caption.copy(fontWeight = FontWeight.SemiBold),
          color = if (isCompatible) Candy.Mint else c.inkSoft,
        )
      }
    }

    ModelStatsRow(
      downloads = model.downloads,
      likes = model.likes,
      lastModified = model.lastModified,
    )
  }
}
