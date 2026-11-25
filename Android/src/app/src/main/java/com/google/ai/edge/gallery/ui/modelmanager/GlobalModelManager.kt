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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.common.logButtonClick
import com.google.ai.edge.gallery.core.design.components.Capsule
import com.google.ai.edge.gallery.core.design.components.GlassChip
import com.google.ai.edge.gallery.core.design.components.GlassCircle
import com.google.ai.edge.gallery.core.design.components.GlassDialog
import com.google.ai.edge.gallery.core.design.components.GlassFloatingActionButton
import com.google.ai.edge.gallery.core.design.components.GlassPill
import com.google.ai.edge.gallery.core.design.components.GlassRow
import com.google.ai.edge.gallery.core.design.components.GlassSheet
import com.google.ai.edge.gallery.core.design.components.GlassTab
import com.google.ai.edge.gallery.core.design.components.GlassTabs
import com.google.ai.edge.gallery.core.design.components.GlassTextField
import com.google.ai.edge.gallery.core.design.components.HeaderGlassButton
import com.google.ai.edge.gallery.core.design.components.RoundedRectangle
import com.google.ai.edge.gallery.core.design.components.WashPanel
import com.google.ai.edge.gallery.core.design.components.pressable
import com.google.ai.edge.gallery.core.design.components.wash
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.ImportOrigin
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.data.OssModelImportLogger
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.data.supportModelBenchmark
import com.google.ai.edge.gallery.huggingface.extractHfUrlInfo
import com.google.ai.edge.gallery.proto.HfModelItemProto
import com.google.ai.edge.gallery.proto.ImportedModel
import com.google.ai.edge.gallery.ui.common.TaskIcon
import com.google.ai.edge.gallery.ui.common.humanReadableSize
import com.google.ai.edge.gallery.ui.common.isHttpOrHttps
import com.google.ai.edge.gallery.ui.common.modelitem.ModelItem
import com.google.ai.edge.gallery.ui.common.tos.TosViewModel
import com.kyant.backdrop.throttleBackdrops
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "AGGlobalMM"
private const val FOCUS_HIGHLIGHT_DURATION_MS = 1500L

private data class PendingModelImport(
  val uri: Uri,
  val origin: ImportOrigin,
  val info: ImportedModel? = null,
)

private data class ModelDetailsContext(val model: HfModelItemProto, val origin: ImportOrigin)

private enum class ModelLibraryFilter(val label: String) {
  ALL("All"),
  DOWNLOADED("Ready"),
  DOWNLOADS("Active"),
  IMPORTED("Imported"),
}

@Composable
fun GlobalModelManager(
  viewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  onModelSelected: (Task, Model) -> Unit,
  onBenchmarkClicked: (Model) -> Unit,
  modifier: Modifier = Modifier,
  tosViewModel: TosViewModel? = null,
  initialImportUrl: String? = null,
  initialFocusModelName: String? = null,
  initialOpenTryIt: Boolean = false,
  supplementaryModelsViewModel: SupplementaryModelsViewModel? = null,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  // The UI state is replaced several times a second while a model downloads. Only read the fields
  // this screen needs through derivedStateOf so download progress doesn't recompose the whole
  // screen. Each ModelItem observes its own download status.
  val modelImportingUpdateTrigger by remember {
    derivedStateOf { uiState.modelImportingUpdateTrigger }
  }
  val allTasks by remember { derivedStateOf(referentialEqualityPolicy()) { uiState.tasks } }
  val supplementaryModelsSize by remember { derivedStateOf { uiState.supplementaryModels.size } }
  val builtInModels = remember { mutableStateListOf<Model>() }
  val importedModels = remember { mutableStateListOf<Model>() }
  val taskCandidates = remember { mutableStateListOf<Task>() }
  var modelForTaskCandidate by remember { mutableStateOf<Model?>(null) }
  var showTaskSelectorBottomSheet by remember { mutableStateOf(false) }
  var showImportModelSheet by remember { mutableStateOf(false) }
  var showHfExploreScreen by remember { mutableStateOf(false) }
  var showHuggingFaceUrlDialog by remember { mutableStateOf(false) }
  var huggingFaceUrlInput by remember { mutableStateOf("") }
  var showUnsupportedModelDialog by remember { mutableStateOf(false) }
  var unsupportedModelErrorMessage by remember { mutableStateOf("") }
  var pendingModelImport by remember { mutableStateOf<PendingModelImport?>(null) }
  var showImportDialog by remember { mutableStateOf(false) }
  var showImportingDialog by remember { mutableStateOf(false) }
  var modelDetailsContext by remember { mutableStateOf<ModelDetailsContext?>(null) }
  var isLoadingModelCardDetails by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val listState = rememberLazyListState()
  val snackbarHostState = remember { SnackbarHostState() }
  val modelItemExpandedStates = remember { mutableStateMapOf<String, Boolean>() }
  var selectedTab by remember { mutableStateOf(ModelManagerTab.MAIN) }
  var pendingFocusModelName by rememberSaveable { mutableStateOf<String?>(null) }
  var pendingOpenTryIt by rememberSaveable { mutableStateOf(false) }
  var highlightedModelName by remember { mutableStateOf<String?>(null) }
  val supplementaryUiState =
    supplementaryModelsViewModel?.uiState?.collectAsStateWithLifecycle()?.value
  val supplementaryModelCount = supplementaryUiState?.visibleModelCount ?: supplementaryModelsSize
  val supplementaryActions =
    rememberSupplementaryModelsActions(
      supplementaryModelsViewModel = supplementaryModelsViewModel,
      modelManagerViewModel = viewModel,
    )

  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    viewModel.refreshSupplementaryStatuses()
    supplementaryModelsViewModel?.refreshOnResume()
  }

  val processModelUri: (Uri, Boolean, ImportOrigin) -> Unit = { uri, isWebImport, origin ->
    validateAndProcessModelUri(
      uri = uri,
      context = context,
      isWebImport = isWebImport,
      onUnsupportedModelError = { errorMessage ->
        unsupportedModelErrorMessage = errorMessage
        showUnsupportedModelDialog = true
      },
      onValidModelUri = { validUri ->
        pendingModelImport = PendingModelImport(uri = validUri, origin = origin)
        showImportDialog = true
      },
    )
  }

  val handleImportUrl: (String) -> Unit =
    remember(context, viewModel) {
      { rawUrl: String ->
        val url = rawUrl.trim()
        if (url.isNotEmpty()) {
          val origin = ImportOrigin(source = OssModelImportLogger.Source.HF_URL)
          showHuggingFaceUrlDialog = false
          val urlInfo = extractHfUrlInfo(url)
          val targetModelId = urlInfo.modelId
          when {
            urlInfo.isDirectModelFile -> {
              val fileUri = urlInfo.toResolveDownloadUrl(url).toUri()
              processModelUri(fileUri, /* isWebImport= */ true, origin)
            }
            targetModelId != null -> {
              isLoadingModelCardDetails = true
              viewModel.fetchModelDetails(targetModelId) { detailedModel ->
                isLoadingModelCardDetails = false
                if (detailedModel != null) {
                  modelDetailsContext = ModelDetailsContext(model = detailedModel, origin = origin)
                } else {
                  unsupportedModelErrorMessage =
                    getErrorMessage(context, R.string.could_not_fetch_model_details, targetModelId)
                  showUnsupportedModelDialog = true
                }
              }
            }
            else -> {
              processModelUri(url.toUri(), /* isWebImport= */ true, origin)
            }
          }
        }
      }
    }

  // Consumes the deep-link import URL once. Each deep link navigates to a new back stack entry with
  // its own saved state, so this survives configuration changes without blocking a later deep link.
  var isInitialImportUrlHandled by rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(initialImportUrl) {
    if (!isInitialImportUrlHandled && !initialImportUrl.isNullOrBlank()) {
      isInitialImportUrlHandled = true
      handleImportUrl(initialImportUrl)
    }
  }

  var isInitialFocusHandled by rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(initialFocusModelName, initialOpenTryIt) {
    if (!isInitialFocusHandled && !initialFocusModelName.isNullOrBlank()) {
      isInitialFocusHandled = true
      pendingFocusModelName = initialFocusModelName
      pendingOpenTryIt = initialOpenTryIt
    }
  }

  val filePickerLauncher: ActivityResultLauncher<Intent> =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        result.data?.data?.let { uri ->
          processModelUri(
            uri,
            /* isWebImport= */ false,
            ImportOrigin(source = OssModelImportLogger.Source.LOCAL_FILE),
          )
        } ?: run { Log.d(TAG, "No file selected or URI is null.") }
      } else {
        Log.d(TAG, "File picking cancelled.")
      }
    }

  val taskModelsCount by remember {
    derivedStateOf { allTasks.sumOf { it.models.size } }
  }
  LaunchedEffect(modelImportingUpdateTrigger, allTasks, taskModelsCount) {
    val allowlistModels = viewModel.allowlistModels
    val allowlistOrderMap = allowlistModels.withIndex().associate { it.value.name to it.index }

    val sortedModels =
      viewModel
        .getAllModels()
        // Filter to include only top-level models (those without a parent).
        .filter { !it.isVariant }
        .sortedWith(
          compareBy<Model> { model ->
              // Sort by the index in allowlistModels. Models not in the allowlist come last.
              allowlistOrderMap[model.name] ?: Int.MAX_VALUE
            }
            .thenBy { model ->
              // If not in the allowlist, sort by their display names.
              model.displayName.ifEmpty { model.name }
            }
        )
    builtInModels.clear()
    builtInModels.addAll(sortedModels.filter { !it.downloadInfo.imported })
    importedModels.clear()
    importedModels.addAll(sortedModels.filter { it.downloadInfo.imported })
  }

  // Calculate model variants by grouping models with a parentModelName.
  val modelVariants by
    remember(modelImportingUpdateTrigger, allTasks) {
      derivedStateOf {
        val allModels = allTasks.flatMap { it.models }.distinct()
        allModels.filter { it.isVariant }.groupBy { it.hierarchy.parentModelName.orEmpty() }
      }
    }

  LaunchedEffect(pendingFocusModelName, builtInModels.toList(), importedModels.toList()) {
    val focusName = pendingFocusModelName ?: return@LaunchedEffect
    val targetModel = viewModel.getModelByName(focusName)
    val rowModelName =
      targetModel?.hierarchy?.parentModelName?.takeIf { it.isNotEmpty() } ?: focusName
    val builtInIdx = builtInModels.indexOfFirst { it.name == rowModelName }
    val importedIdx = importedModels.indexOfFirst { it.name == rowModelName }
    if (builtInIdx < 0 && importedIdx < 0) {
      return@LaunchedEffect
    }

    val shouldOpenTryIt = pendingOpenTryIt

    selectedTab = ModelManagerTab.MAIN
    if (builtInIdx >= 0) {
      modelItemExpandedStates[rowModelName] = true
    }
    highlightedModelName = focusName

    val targetLazyIndex = if (builtInIdx >= 0) builtInIdx else builtInModels.size + 1 + importedIdx
    // Wait one frame for the MAIN tab switch and expanded card state to compose before scrolling.
    withFrameNanos {}
    listState.scrollToItem(targetLazyIndex)
    // Wait one frame after jumping to targetLazyIndex so visibleItemsInfo reflects its laid-out
    // height before computing the centering scroll offset.
    withFrameNanos {}
    val layoutInfo = listState.layoutInfo
    val viewportHeight =
      (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(0)
    val itemHeight =
      layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetLazyIndex }?.size ?: 0
    if (viewportHeight > itemHeight && itemHeight > 0) {
      listState.scrollToItem(
        index = targetLazyIndex,
        scrollOffset = -((viewportHeight - itemHeight) / 2),
      )
    }

    if (shouldOpenTryIt && targetModel != null) {
      val tasksForModel =
        viewModel.uiState.value.tasks.filter { task ->
          task.models.any { it.name == targetModel.name }
        }
      if (tasksForModel.isNotEmpty()) {
        taskCandidates.clear()
        taskCandidates.addAll(tasksForModel)
        modelForTaskCandidate = targetModel
        showTaskSelectorBottomSheet = true
      }
    }

    // Clear pending focus state only after suspension points complete so LaunchedEffect is not
    // self-cancelled mid-scroll.
    pendingFocusModelName = null
    pendingOpenTryIt = false
  }

  LaunchedEffect(highlightedModelName) {
    val activeHighlight = highlightedModelName ?: return@LaunchedEffect
    delay(FOCUS_HIGHLIGHT_DURATION_MS)
    if (highlightedModelName == activeHighlight) {
      highlightedModelName = null
    }
  }

  val handleClickModel: (Model) -> Unit = { model ->
    val tasks = viewModel.uiState.value.tasks
    val tasksForModel = tasks.filter { task -> task.models.any { it.name == model.name } }
    // If there is only one task for the model, navigate to the model directly.
    if (tasksForModel.size == 1) {
      onModelSelected(tasksForModel[0], model)
    }
    // If there are multiple tasks for the model, show a bottom sheet for the user to choose which
    // task to use.
    else if (tasksForModel.size > 1) {
      taskCandidates.clear()
      taskCandidates.addAll(tasksForModel)
      modelForTaskCandidate = model
      showTaskSelectorBottomSheet = true
    }
  }

  val c = glassColors
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var selectedFilter by rememberSaveable { mutableStateOf(ModelLibraryFilter.ALL) }

  // Handle system's edge swipe.
  BackHandler { navigateUp() }

  val totalModelCount = builtInModels.size + importedModels.size + supplementaryModelCount

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
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Models",
            color = c.hero,
            style =
              GalleryType.Hero.copy(
                fontSize = 36.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.3).sp,
              ),
            modifier = Modifier.semantics { heading() },
          )
          Text(
            text = "$totalModelCount",
            color = Candy.Sky,
            style = GalleryType.BodyStrong,
            modifier = Modifier.padding(bottom = 4.dp),
          )
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          HeaderGlassButton(
            onClick = { showHfExploreScreen = true },
            label = stringResource(R.string.hf_explore_title),
            size = 40.dp,
          ) {
            Icon(
              imageVector = Icons.Rounded.Explore,
              contentDescription = stringResource(R.string.hf_explore_title),
              tint = Candy.Sky,
              modifier = Modifier.size(19.dp),
            )
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
      }
    },
    floatingActionButton = {
      val showImportFab = selectedTab == ModelManagerTab.MAIN
      if (showImportFab) {
        GlassFloatingActionButton(
          onClick = { showImportModelSheet = true },
          label = stringResource(R.string.cd_import_model_button),
          icon = Icons.Filled.Add,
        )
      }
    },
  ) { innerPadding ->
    val builtInSnapshot = builtInModels.toList()
    val importedSnapshot = importedModels.toList()
    val allMainModels = remember(builtInSnapshot, importedSnapshot) { builtInSnapshot + importedSnapshot }
    val statusMap = uiState.modelDownloadStatus
    val activeOrFailedDownloads =
      remember(allMainModels, statusMap) {
        allMainModels.mapNotNull { model ->
          val st = statusMap[model.name] ?: return@mapNotNull null
          val interesting =
            st.status == ModelDownloadStatusType.IN_PROGRESS ||
              st.status == ModelDownloadStatusType.UNZIPPING ||
              st.status == ModelDownloadStatusType.PARTIALLY_DOWNLOADED ||
              st.status == ModelDownloadStatusType.FAILED
          if (interesting) model to st else null
        }
      }
    val downloadedModels =
      remember(allMainModels, statusMap) {
        allMainModels.filter { statusMap[it.name]?.status == ModelDownloadStatusType.SUCCEEDED }
      }
    val totalDownloadedBytes =
      remember(downloadedModels, statusMap) {
        downloadedModels.sumOf { model ->
          val st = statusMap[model.name]
          (st?.totalBytes?.takeIf { it > 0L } ?: model.downloadInfo.sizeInBytes).coerceAtLeast(0L)
        }
      }

    val normalizedQuery = searchQuery.trim().lowercase()
    val filteredBuiltIn =
      remember(builtInModels.toList(), normalizedQuery, selectedFilter, statusMap) {
        builtInModels.filter { model ->
          val matchesQuery =
            normalizedQuery.isEmpty() ||
              model.name.lowercase().contains(normalizedQuery) ||
              model.displayName.lowercase().contains(normalizedQuery) ||
              model.info.lowercase().contains(normalizedQuery)
          val st = statusMap[model.name]?.status
          val matchesFilter =
            when (selectedFilter) {
              ModelLibraryFilter.ALL -> true
              ModelLibraryFilter.DOWNLOADED -> st == ModelDownloadStatusType.SUCCEEDED
              ModelLibraryFilter.DOWNLOADS ->
                st == ModelDownloadStatusType.IN_PROGRESS ||
                  st == ModelDownloadStatusType.UNZIPPING ||
                  st == ModelDownloadStatusType.PARTIALLY_DOWNLOADED ||
                  st == ModelDownloadStatusType.FAILED
              ModelLibraryFilter.IMPORTED -> false
            }
          matchesQuery && matchesFilter
        }
      }
    val filteredImported =
      remember(importedModels.toList(), normalizedQuery, selectedFilter, statusMap) {
        importedModels.filter { model ->
          val matchesQuery =
            normalizedQuery.isEmpty() ||
              model.name.lowercase().contains(normalizedQuery) ||
              model.displayName.lowercase().contains(normalizedQuery)
          val st = statusMap[model.name]?.status
          val matchesFilter =
            when (selectedFilter) {
              ModelLibraryFilter.ALL,
              ModelLibraryFilter.IMPORTED -> true
              ModelLibraryFilter.DOWNLOADED -> st == ModelDownloadStatusType.SUCCEEDED
              ModelLibraryFilter.DOWNLOADS ->
                st == ModelDownloadStatusType.IN_PROGRESS ||
                  st == ModelDownloadStatusType.UNZIPPING ||
                  st == ModelDownloadStatusType.PARTIALLY_DOWNLOADED ||
                  st == ModelDownloadStatusType.FAILED
            }
          matchesQuery && matchesFilter
        }
      }

    Box(modifier = Modifier.fillMaxWidth().throttleBackdrops { listState.isScrollInProgress }) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .padding(top = innerPadding.calculateTopPadding())
      ) {
        val mainCount = builtInModels.size + importedModels.size
        val suppCount = supplementaryModelCount

        GlassTabs(
          tabs =
            listOf(
              GlassTab(label = stringResource(R.string.main_models_tab_title, mainCount)),
              GlassTab(label = stringResource(R.string.supplementary_models_tab_title, suppCount)),
            ),
          selected = selectedTab.ordinal,
          onSelect = { index ->
            val next = ModelManagerTab.entries[index]
            if (selectedTab != next) {
              logButtonClick(
                if (next == ModelManagerTab.MAIN) "model_manager_tab_main"
                else "model_manager_tab_supplementary"
              )
            }
            selectedTab = next
          },
          height = 46.dp,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        )

        if (selectedTab == ModelManagerTab.MAIN) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            GlassTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = "Search models, families, or specs...",
              modifier = Modifier.weight(1f),
            )
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Rounded.Close, contentDescription = null, tint = c.inkSoft)
              }
            }
          }

          Row(
            modifier =
              Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            ModelLibraryFilter.entries.forEach { filter ->
              val isSelected = selectedFilter == filter
              val label =
                when (filter) {
                  ModelLibraryFilter.DOWNLOADED -> "${filter.label} · ${downloadedModels.size}"
                  ModelLibraryFilter.DOWNLOADS -> "${filter.label} · ${activeOrFailedDownloads.size}"
                  else -> filter.label
                }
              Box(
                modifier =
                  Modifier.then(
                      if (isSelected) Modifier.clip(Capsule).background(c.ink)
                      else Modifier.wash(shape = Capsule, tint = c.glass)
                    )
                    .pressable(onClick = { selectedFilter = filter })
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  label,
                  style = GalleryType.BodyStrong.copy(fontWeight = FontWeight.Bold),
                  color = if (isSelected) c.canvas else c.ink,
                  maxLines = 1,
                )
              }
            }
          }
        }

        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding =
            PaddingValues(top = 8.dp, bottom = innerPadding.calculateBottomPadding() + 92.dp),
        ) {
          val showSupplementaryTab = selectedTab == ModelManagerTab.SUPPLEMENTARY
          if (showSupplementaryTab && supplementaryUiState != null) {
            supplementaryModelItems(
              uiState = supplementaryUiState,
              modelItemExpandedStates = modelItemExpandedStates,
              viewModel = viewModel,
              tosViewModel = tosViewModel,
              actions = supplementaryActions,
            )
          }
          if (!showSupplementaryTab) {
            // Download & Storage Overview Panel.
            if (activeOrFailedDownloads.isNotEmpty() || selectedFilter == ModelLibraryFilter.DOWNLOADS || selectedFilter == ModelLibraryFilter.DOWNLOADED) {
              item(key = "downloads_storage_overview") {
                val overviewShape = RoundedRectangle(26.dp)
                val rim = Color.White.copy(alpha = if (c.dark) 0.14f else 0.55f)
                WashPanel(
                  modifier = Modifier.fillMaxWidth().border(1.dp, rim, overviewShape),
                  shape = overviewShape,
                  tint = c.glass,
                ) {
                  Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        Icon(
                          Icons.Rounded.Storage,
                          contentDescription = null,
                          tint = Candy.Sky,
                          modifier = Modifier.size(18.dp),
                        )
                        Text("Downloads & Storage", style = GalleryType.BodyStrong, color = c.ink)
                      }
                      Text(
                        text = "${totalDownloadedBytes.humanReadableSize()} · ${downloadedModels.size} ready",
                        style = GalleryType.Caption,
                        color = c.inkSoft,
                      )
                    }

                    if (activeOrFailedDownloads.isEmpty() && selectedFilter == ModelLibraryFilter.DOWNLOADS) {
                      Text(
                        text = "No active or queued downloads.",
                        style = GalleryType.Caption,
                        color = c.inkSoft,
                      )
                    } else {
                      activeOrFailedDownloads.forEach { (dlModel, dlStatus) ->
                        val progress =
                          if (dlStatus.totalBytes > 0L) {
                            (dlStatus.receivedBytes.toFloat() / dlStatus.totalBytes.toFloat()).coerceIn(0f, 1f)
                          } else {
                            0f
                          }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                          ) {
                            Column(modifier = Modifier.weight(1f)) {
                              Text(
                                text = dlModel.displayName.ifEmpty { dlModel.name },
                                style = GalleryType.BodyStrong,
                                color = c.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                              )
                              val meta =
                                when (dlStatus.status) {
                                  ModelDownloadStatusType.IN_PROGRESS,
                                  ModelDownloadStatusType.UNZIPPING -> {
                                    val speed =
                                      if (dlStatus.bytesPerSecond > 0L) {
                                        " · ${dlStatus.bytesPerSecond.humanReadableSize()}/s"
                                      } else {
                                        ""
                                      }
                                    val eta =
                                      if (dlStatus.remainingMs > 0L) {
                                        " · ${(dlStatus.remainingMs / 1000L)}s left"
                                      } else {
                                        ""
                                      }
                                    "${dlStatus.receivedBytes.humanReadableSize()} / ${dlStatus.totalBytes.humanReadableSize()}$speed$eta"
                                  }
                                  ModelDownloadStatusType.PARTIALLY_DOWNLOADED ->
                                    "Paused · ${dlStatus.receivedBytes.humanReadableSize()} / ${dlStatus.totalBytes.humanReadableSize()}"
                                  ModelDownloadStatusType.FAILED ->
                                    dlStatus.errorMessage.ifBlank { "Download failed" }
                                  else -> ""
                                }
                              Text(
                                text = meta,
                                style = GalleryType.Caption,
                                color =
                                  if (dlStatus.status == ModelDownloadStatusType.FAILED) Candy.Tomato
                                  else c.inkSoft,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                              )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                              when (dlStatus.status) {
                                ModelDownloadStatusType.IN_PROGRESS -> {
                                  IconButton(
                                    onClick = { viewModel.cancelDownloadModel(dlModel) },
                                    modifier = Modifier.size(32.dp),
                                  ) {
                                    Icon(
                                      Icons.Rounded.Close,
                                      contentDescription = stringResource(R.string.cancel),
                                      tint = Candy.Tomato,
                                      modifier = Modifier.size(18.dp),
                                    )
                                  }
                                }
                                ModelDownloadStatusType.PARTIALLY_DOWNLOADED,
                                ModelDownloadStatusType.FAILED -> {
                                  IconButton(
                                    onClick = {
                                      viewModel.downloadModelAfterMobileDataCheck(null, dlModel)
                                    },
                                    modifier = Modifier.size(32.dp),
                                  ) {
                                    Icon(
                                      if (dlStatus.status == ModelDownloadStatusType.FAILED) {
                                        Icons.Rounded.Refresh
                                      } else {
                                        Icons.Rounded.Download
                                      },
                                      contentDescription = "Retry",
                                      tint = Candy.Sky,
                                      modifier = Modifier.size(18.dp),
                                    )
                                  }
                                  IconButton(
                                    onClick = { viewModel.discardPartialDownload(dlModel) },
                                    modifier = Modifier.size(32.dp),
                                  ) {
                                    Icon(
                                      Icons.Rounded.DeleteOutline,
                                      contentDescription = stringResource(R.string.delete),
                                      tint = c.inkSoft,
                                      modifier = Modifier.size(18.dp),
                                    )
                                  }
                                }
                                else -> {}
                              }
                            }
                          }
                          LinearProgressIndicator(
                            progress = { progress },
                            color =
                              if (dlStatus.status == ModelDownloadStatusType.FAILED) Candy.Tomato
                              else Candy.Sky,
                            trackColor = c.inkFaint.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(Capsule),
                          )
                        }
                      }
                    }
                  }
                }
              }
            }

            items(filteredBuiltIn, key = { it.name }) { model ->
              val expanded = modelItemExpandedStates.getOrDefault(model.name, true)
              ModelItem(
                model = model,
                modelVariants = modelVariants.getOrDefault(model.name, listOf()),
                task = null,
                modelManagerViewModel = viewModel,
                onModelClicked = handleClickModel,
                onBenchmarkClicked = onBenchmarkClicked,
                expanded = expanded,
                isBenchmarkSupported = model.supportModelBenchmark,
                showBenchmarkActionButton = true,
                onExpanded = { modelItemExpandedStates[model.name] = it },
                tosViewModel = tosViewModel,
                highlightedModelName = highlightedModelName,
              )
            }

            // Imported models.
            if (filteredImported.isNotEmpty()) {
              item(key = "imported_models_label") {
                Text(
                  stringResource(R.string.model_list_imported_models_title),
                  color = c.ink,
                  style = GalleryType.Label,
                  modifier =
                    Modifier.semantics { heading() }
                      .padding(horizontal = 8.dp)
                      .padding(top = 18.dp, bottom = 4.dp),
                )
              }
            }
            items(filteredImported, key = { it.name }) { model ->
              ModelItem(
                model = model,
                task = null,
                modelManagerViewModel = viewModel,
                onModelClicked = handleClickModel,
                onBenchmarkClicked = onBenchmarkClicked,
                expanded = true,
                isBenchmarkSupported = model.supportModelBenchmark,
                showBenchmarkActionButton = true,
                tosViewModel = tosViewModel,
                highlightedModelName = highlightedModelName,
              )
            }
          }
        }
      }

      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(alignment = Alignment.BottomCenter).padding(bottom = 32.dp),
      )
    }
  }

  if (showTaskSelectorBottomSheet) {
    GlassSheet(
      onDismiss = { showTaskSelectorBottomSheet = false },
      title = stringResource(R.string.model_manager_select_task_title),
    ) {
      Column(
        modifier = Modifier.padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        for (task in taskCandidates) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier =
              Modifier.fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .wash(shape = RoundedRectangle(20.dp), tint = c.glass)
                .pressable(
                  onClick = {
                    val model = modelForTaskCandidate
                    if (model != null) {
                      onModelSelected(task, model)
                    }
                    showTaskSelectorBottomSheet = false
                  }
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
          ) {
            Text(
              if (task.id == BuiltInTaskId.LLM_TEST) {
                stringResource(R.string.test_chat)
              } else {
                task.label
              },
              color = c.ink,
              style = GalleryType.BodyStrong,
            )
            TaskIcon(task = task, width = 36.dp)
          }
        }
      }
    }
  }

  // Import model sheet.
  if (showImportModelSheet) {
    GlassSheet(
      onDismiss = { showImportModelSheet = false },
      title = "Import model",
      subtitle = stringResource(R.string.import_model_terms_subtitle),
    ) {
      GlassRow(
        label = "From local model file",
        icon = Icons.AutoMirrored.Outlined.NoteAdd,
        accent = Candy.Sky,
        onClick = {
          showImportModelSheet = false
          val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
              addCategory(Intent.CATEGORY_OPENABLE)
              type = "*/*"
              putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            }
          filePickerLauncher.launch(intent)
        },
      )
      GlassRow(
        label = stringResource(R.string.hf_explore_title),
        icon = Icons.Rounded.Explore,
        accent = Candy.Mint,
        onClick = {
          showImportModelSheet = false
          showHfExploreScreen = true
        },
      )
      GlassRow(
        label = stringResource(R.string.import_model_from_hugging_face),
        icon = Icons.Rounded.Link,
        accent = Candy.Grape,
        onClick = {
          showImportModelSheet = false
          showHuggingFaceUrlDialog = true
        },
      )
    }
  }

  if (showHfExploreScreen) {
    HfExploreScreen(
      onNavigateUp = { showHfExploreScreen = false },
      onOpenUrlImportDialog = { showHuggingFaceUrlDialog = true },
      onModelCardSelected = { detailedModel ->
        modelDetailsContext =
          ModelDetailsContext(
            model = detailedModel,
            origin =
              ImportOrigin(
                source = OssModelImportLogger.Source.HF_MODEL_CARD,
                modelId = detailedModel.id,
              ),
          )
      },
    )
  }

  // Import dialog
  if (showImportDialog) {
    pendingModelImport?.let { pending ->
      val existingModelNames =
        remember(allTasks, modelImportingUpdateTrigger, supplementaryModelsSize) {
          uiState.existingModelNames
        }
      val existingDisplayNames =
        remember(allTasks, modelImportingUpdateTrigger, supplementaryModelsSize) {
          uiState.existingDisplayNames
        }
      val storedImportedModels by remember { derivedStateOf { uiState.importedModels } }
      ModelImportDialog(
        uri = pending.uri,
        huggingFaceApiClient = viewModel.huggingFaceApiClient,
        onDismiss = { showImportDialog = false },
        onDone = { info ->
          pendingModelImport = pending.copy(info = info)
          showImportDialog = false
          showHfExploreScreen = false
          showImportingDialog = true
        },
        accessTokenProvider = viewModel::readAccessToken,
        existingModelNames = existingModelNames,
        existingDisplayNames = existingDisplayNames,
        existingImportedModels = storedImportedModels,
      )
    }
  }

  // Importing in progress dialog.
  if (showImportingDialog) {
    pendingModelImport?.let { pending ->
      pending.info?.let { info ->
        ModelImportingDialog(
          uri = pending.uri,
          info = info,
          onDismiss = { showImportingDialog = false },
          onError = { viewModel.onModelImportFailed(pending.origin) },
          onDone = {
            viewModel.addImportedLlmModel(info = it, origin = pending.origin)
            showImportingDialog = false
            pendingFocusModelName = it.fileName
            pendingOpenTryIt = false

            // Show a snack bar for successful import.
            scope.launch { snackbarHostState.showSnackbar("Model imported successfully") }
          },
        )
      }
    }
  }

  // Alert dialog for unsupported model.
  if (showUnsupportedModelDialog) {
    GlassDialog(
      onDismiss = { showUnsupportedModelDialog = false },
      title = stringResource(R.string.unsupported_model_title),
      confirm = stringResource(R.string.ok),
      onConfirm = { showUnsupportedModelDialog = false },
      icon = Icons.Rounded.Error,
      accent = Candy.Tomato,
    ) {
      Text(unsupportedModelErrorMessage, style = GalleryType.Body, color = c.inkSoft)
    }
  }

  if (showHuggingFaceUrlDialog) {
    HuggingFaceUrlDialog(
      urlInput = huggingFaceUrlInput,
      onUrlInputChange = { huggingFaceUrlInput = it },
      onDismiss = { showHuggingFaceUrlDialog = false },
      onConfirm = { handleImportUrl(huggingFaceUrlInput) },
    )
  }

  // Model card details sheet
  modelDetailsContext?.let { details ->
    HfModelDetailsSheet(
      modelItem = details.model,
      onDismiss = { modelDetailsContext = null },
      onImportModelFile = { modelId, fileName ->
        val origin = details.origin
        modelDetailsContext = null
        val fileUrl = "https://huggingface.co/$modelId/resolve/main/$fileName?download=true"
        processModelUri(fileUrl.toUri(), true, origin)
      },
    )
  }
}

private fun validateAndProcessModelUri(
  uri: Uri,
  context: Context,
  isWebImport: Boolean,
  onUnsupportedModelError: (String) -> Unit,
  onValidModelUri: (Uri) -> Unit,
) {
  val fileName = getFileName(context = context, uri = uri)
  Log.d(TAG, "Validating URI: $uri, fileName: $fileName, isWebImport: $isWebImport")
  val hasValidExtension =
    if (isWebImport) {
      fileName != null && fileName.endsWith(".litertlm")
    } else {
      fileName != null && (fileName.endsWith(".task") || fileName.endsWith(".litertlm"))
    }

  if (!hasValidExtension) {
    onUnsupportedModelError(getErrorMessage(context, R.string.unsupported_file_type_error))
  } else if (fileName != null && fileName.lowercase().contains("-web")) {
    onUnsupportedModelError(getErrorMessage(context, R.string.unsupported_web_model_error))
  } else {
    onValidModelUri(uri)
  }
}

private fun getErrorMessage(context: Context, resId: Int, vararg formatArgs: Any): String {
  return context.getString(resId, *formatArgs)
}

// Helper function to get the file name from a URI
private fun getFileName(context: Context, uri: Uri): String? {
  if (uri.scheme == "content") {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      if (cursor.moveToFirst()) {
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1) {
          return cursor.getString(nameIndex)
        }
      }
    }
  } else if (uri.scheme == "file" || isHttpOrHttps(uri)) {
    return uri.lastPathSegment
  }
  return null
}
