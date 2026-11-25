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

package com.google.ai.edge.gallery.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.core.design.components.Capsule
import com.google.ai.edge.gallery.core.design.components.ChromeCapsule
import com.google.ai.edge.gallery.core.design.components.GlassCircle
import com.google.ai.edge.gallery.core.design.components.GlassDialog
import com.google.ai.edge.gallery.core.design.components.GlassIcon
import com.google.ai.edge.gallery.core.design.components.GlassLoader
import com.google.ai.edge.gallery.core.design.components.GlassMenuItem
import com.google.ai.edge.gallery.core.design.components.GlassRow
import com.google.ai.edge.gallery.core.design.components.HeaderGlassButton
import com.google.ai.edge.gallery.core.design.components.MorphButton
import com.google.ai.edge.gallery.core.design.components.RoundedRectangle
import com.google.ai.edge.gallery.core.design.components.WashPanel
import com.google.ai.edge.gallery.core.design.components.glass
import com.google.ai.edge.gallery.core.design.components.pressable
import com.google.ai.edge.gallery.core.design.components.wash
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.CardTone
import com.google.ai.edge.gallery.core.design.theme.Tone
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Category
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.proto.ChatSessionProto
import com.google.ai.edge.gallery.ui.common.humanReadableSize
import com.google.ai.edge.gallery.ui.common.tos.AppTosDialog
import com.google.ai.edge.gallery.ui.common.tos.TosViewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.ui.theme.ThemeSettings
import com.kyant.backdrop.throttleBackdrops
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class HomeBentoFilter(val label: String) {
  ALL("All"),
  CHATS("Chats"),
  MODELS("Models"),
  EXPERIENCES("Experiences"),
}

private val BentoToneCycle =
  listOf(Tone.Butter, Tone.Orchid, Tone.Sky, Tone.Sage, Tone.Peach, Tone.Lavender, Tone.Blush)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  modelManagerViewModel: ModelManagerViewModel,
  tosViewModel: TosViewModel,
  navigateToTaskScreen: (Task) -> Unit,
  onModelsClicked: () -> Unit,
  onNotificationsClicked: () -> Unit,
  @Suppress("UNUSED_PARAMETER") enableAnimation: Boolean,
  modifier: Modifier = Modifier,
  onLaunchModelTask: (taskId: String, modelName: String, sessionId: String?) -> Unit = { _, _, _ -> },
  onBenchmarkClicked: (Model) -> Unit = {},
  homeHistoryViewModel: HomeHistoryViewModel = hiltViewModel(),
) {
  val uiState by modelManagerViewModel.uiState.collectAsStateWithLifecycle()
  val recentSessions by homeHistoryViewModel.recentSessions.collectAsStateWithLifecycle()
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showAppearanceSheet by remember { mutableStateOf(false) }
  var showTosDialog by remember { mutableStateOf(!tosViewModel.getIsTosAccepted()) }
  var selectedFilter by remember { mutableStateOf(HomeBentoFilter.ALL) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val c = glassColors

  val tasks = uiState.tasks

  if (!showTosDialog) {
    var loadingModelAllowlistDelayed by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.loadingModelAllowlist) {
      if (uiState.loadingModelAllowlist) {
        delay(200)
        if (uiState.loadingModelAllowlist) loadingModelAllowlistDelayed = true
      } else {
        loadingModelAllowlistDelayed = false
      }
    }

    if (loadingModelAllowlistDelayed) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassLoader(label = stringResource(R.string.loading_model_list))
      }
    }

    if (!loadingModelAllowlistDelayed && !uiState.loadingModelAllowlist) {
      val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

      LaunchedEffect(Unit) {
        delay(2000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
              PackageManager.PERMISSION_GRANTED
          ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
        }
      }

      run {
        val taskModelsVersion = tasks.sumOf { it.updateTrigger.value }
        val chatTask = remember(tasks, taskModelsVersion) { modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_CHAT) }
        val agentTask = remember(tasks, taskModelsVersion) { modelManagerViewModel.getTaskById(BuiltInTaskId.LLM_AGENT_CHAT) }

        val downloadedLlms = remember(tasks, taskModelsVersion, uiState.modelImportingUpdateTrigger, uiState.modelDownloadStatus) {
          tasks
            .flatMap { it.models }
            .distinctBy { it.name }
            .filter {
              it.isLlm && uiState.modelDownloadStatus[it.name]?.status == ModelDownloadStatusType.SUCCEEDED
            }
        }

        val activeModel: Model? = remember(uiState.selectedModel, downloadedLlms, chatTask, taskModelsVersion) {
          val selected = uiState.selectedModel
          when {
            selected.name.isNotBlank() && selected.isLlm &&
              uiState.modelDownloadStatus[selected.name]?.status == ModelDownloadStatusType.SUCCEEDED -> selected
            downloadedLlms.isNotEmpty() -> downloadedLlms.first()
            else -> chatTask?.models?.firstOrNull()
          }
        }

        val startNewConversation: () -> Unit = {
          if (activeModel != null && chatTask != null) {
            val isDownloaded =
              uiState.modelDownloadStatus[activeModel.name]?.status == ModelDownloadStatusType.SUCCEEDED
            if (isDownloaded) {
              modelManagerViewModel.selectModel(activeModel)
              onLaunchModelTask(chatTask.id, activeModel.name, null)
            } else {
              navigateToTaskScreen(chatTask)
            }
          } else if (chatTask != null) {
            navigateToTaskScreen(chatTask)
          } else {
            onModelsClicked()
          }
        }

        val orderedTasks = remember(tasks, taskModelsVersion) {
          tasks.sortedWith(
            compareBy<Task> {
              when (it.category.id) {
                Category.LLM.id -> 0
                Category.EXPERIMENTAL.id -> 1
                else -> 2
              }
            }.thenBy { it.index }
          )
        }

        val gridState = rememberLazyStaggeredGridState()

        com.google.ai.edge.gallery.core.design.glass.GlassLayer(
          modifier = modifier.fillMaxSize(),
          content = {
            LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize().throttleBackdrops { gridState.isScrollInProgress },
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp,
          ) {
            // Bold minimal "Tress" header + compact floating action circles
            item(span = StaggeredGridItemSpan.FullLine, key = "hero_header") {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .statusBarsPadding()
                  .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Text(
                  text = stringResource(R.string.app_name),
                  style = GalleryType.Hero.copy(
                    fontSize = 44.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.6).sp,
                  ),
                  color = c.hero,
                  maxLines = 1,
                )
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  val downloadedCount = uiState.modelDownloadStatus.count {
                    it.value.status == ModelDownloadStatusType.SUCCEEDED
                  }
                  val modelsLabel = stringResource(R.string.drawer_models_label)
                  val settingsLabel = stringResource(R.string.drawer_settings_label)
                  val notificationsLabel = stringResource(R.string.drawer_notifications_label)
                  MorphButton(
                    items =
                      listOf(
                        GlassMenuItem(
                          label = modelsLabel,
                          subtitle = "$downloadedCount downloaded",
                          icon = Icons.AutoMirrored.Rounded.ListAlt,
                          onClick = onModelsClicked,
                        ),
                        GlassMenuItem(
                          label = "Appearance",
                          subtitle = "Theme & background",
                          icon = Icons.Rounded.Palette,
                          onClick = { showAppearanceSheet = true },
                        ),
                        GlassMenuItem(
                          label = settingsLabel,
                          icon = Icons.Rounded.Settings,
                          onClick = { showSettingsDialog = true },
                        ),
                        GlassMenuItem(
                          label = notificationsLabel,
                          icon = Icons.Rounded.Notifications,
                          onClick = onNotificationsClicked,
                        ),
                      ),
                    label = stringResource(R.string.cd_menu),
                    size = 40.dp,
                    menuWidth = 236.dp,
                  ) {
                    GlassIcon(Icons.Rounded.Menu, stringResource(R.string.cd_menu), Modifier.size(19.dp))
                  }
                  HeaderGlassButton(
                    onClick = { showSettingsDialog = true },
                    label = stringResource(R.string.cd_app_settings_icon),
                    size = 40.dp,
                  ) {
                    GlassIcon(Icons.Rounded.Settings, stringResource(R.string.cd_app_settings_icon), Modifier.size(19.dp))
                  }
                }
              }
            }

            // Horizontal filter pills matching screenshot ("All", "Chats", "Models", "Experiences")
            item(span = StaggeredGridItemSpan.FullLine, key = "filter_pills") {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState())
                  .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                HomeBentoFilter.entries.forEach { filter ->
                  val active = filter == selectedFilter
                  BentoFilterPill(
                    label = filter.label,
                    active = active,
                    onClick = { selectedFilter = filter },
                  )
                }
              }
            }

            // Column 1/2 staggered bento items tailored cleanly to selected filter
            if (selectedFilter == HomeBentoFilter.ALL || selectedFilter == HomeBentoFilter.MODELS) {
              item(key = "active_model_bento") {
                ActiveModelBentoCard(
                  model = activeModel,
                  ready = activeModel != null &&
                    uiState.modelDownloadStatus[activeModel.name]?.status == ModelDownloadStatusType.SUCCEEDED,
                  onClick = startNewConversation,
                )
              }

              item(key = "models_overview_bento") {
                ModelsOverviewBentoCard(
                  downloadedCount = downloadedLlms.size,
                  totalModelsCount = tasks.flatMap { it.models }.distinctBy { it.name }.size,
                  onClick = onModelsClicked,
                )
              }
            }

            if (selectedFilter == HomeBentoFilter.ALL) {
              item(key = "quick_start_bento") {
                QuickStartBentoCard(
                  hasModel = downloadedLlms.isNotEmpty(),
                  onNewChat = startNewConversation,
                  onModels = onModelsClicked,
                  onBenchmark = {
                    val target = downloadedLlms.firstOrNull()
                    if (target != null) onBenchmarkClicked(target) else onModelsClicked()
                  },
                )
              }
            }

            if (selectedFilter == HomeBentoFilter.ALL || selectedFilter == HomeBentoFilter.CHATS) {
              val shownSessions = recentSessions.take(if (selectedFilter == HomeBentoFilter.CHATS) 12 else 3)
              shownSessions.forEachIndexed { idx, session ->
                item(key = "session_${session.sessionId}") {
                  val tone = if (idx % 2 == 0) c.tone(Tone.Paper) else c.tone(Tone.Lavender)
                  SessionBentoCard(
                    session = session,
                    cardTone = tone,
                    tall = idx % 2 == 1,
                    onClick = {
                      val modelName = session.originalModel.ifBlank { activeModel?.name.orEmpty() }
                      val taskId = session.taskId.ifBlank { BuiltInTaskId.LLM_CHAT }
                      val modelObj = modelManagerViewModel.getModelByName(modelName)
                      if (modelObj != null) {
                        modelManagerViewModel.selectModel(modelObj)
                        onLaunchModelTask(taskId, modelObj.name, session.sessionId)
                      } else if (chatTask != null) {
                        navigateToTaskScreen(chatTask)
                      }
                    },
                  )
                }
              }
            }

            if (selectedFilter == HomeBentoFilter.ALL || selectedFilter == HomeBentoFilter.EXPERIENCES) {
              orderedTasks.forEachIndexed { idx, task ->
                item(key = "task_${task.id}") {
                  val tone = c.tone(BentoToneCycle[idx % BentoToneCycle.size])
                  ExperienceBentoCard(
                    task = task,
                    cardTone = tone,
                    compact = idx % 3 == 0,
                    onClick = { navigateToTaskScreen(task) },
                  )
                }
              }
            }

            if (selectedFilter == HomeBentoFilter.MODELS) {
              val allModels = tasks.flatMap { it.models }.distinctBy { it.name }.take(12)
              allModels.forEachIndexed { idx, model ->
                item(key = "model_bento_${model.name}") {
                  val isReady = uiState.modelDownloadStatus[model.name]?.status == ModelDownloadStatusType.SUCCEEDED
                  val tone = c.tone(if (isReady) Tone.Sage else Tone.Paper)
                  ModelQuickBentoCard(
                    model = model,
                    ready = isReady,
                    cardTone = tone,
                    onClick = {
                      val targetTask = tasks.firstOrNull { t -> t.models.any { it.name == model.name } }
                      if (isReady && targetTask != null) {
                        modelManagerViewModel.selectModel(model)
                        onLaunchModelTask(targetTask.id, model.name, null)
                      } else if (targetTask != null) {
                        navigateToTaskScreen(targetTask)
                      } else {
                        onModelsClicked()
                      }
                    },
                  )
                }
              }
            }
            }
          },
          above = {
            // Bottom centered floating liquid-glass capsule dock: [ Tap to chat | (Mic) ]
            HomeChatCapsuleDock(
              modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
              onTapToChat = startNewConversation,
              onVoiceClick = {
                val target = downloadedLlms.firstOrNull { it.supportAudio } ?: activeModel
                val targetTask = tasks.firstOrNull { it.id == BuiltInTaskId.LLM_ASK_AUDIO } ?: agentTask ?: chatTask
                if (target != null && targetTask != null &&
                  uiState.modelDownloadStatus[target.name]?.status == ModelDownloadStatusType.SUCCEEDED
                ) {
                  modelManagerViewModel.selectModel(target)
                  onLaunchModelTask(targetTask.id, target.name, null)
                } else {
                  startNewConversation()
                }
              },
            )
          },
        )
      }
    }
  }

  if (showTosDialog) {
    AppTosDialog(
      onTosAccepted = {
        showTosDialog = false
        tosViewModel.acceptTos()
      },
    )
  }

  if (showAppearanceSheet) {
    AppearanceSettingsSheet(
      selectedTheme = ThemeSettings.themeOverride.value,
      onSelectTheme = { theme ->
        ThemeSettings.themeOverride.value = theme
        modelManagerViewModel.saveThemeOverride(theme)
      },
      onDismiss = { showAppearanceSheet = false },
    )
  }

  if (showSettingsDialog) {
    SettingsDialog(
      curThemeOverride = modelManagerViewModel.readThemeOverride(),
      modelManagerViewModel = modelManagerViewModel,
      onDismissed = { showSettingsDialog = false },
    )
  }

  if (uiState.loadingModelAllowlistError.isNotEmpty()) {
    GlassDialog(
      onDismiss = { modelManagerViewModel.clearLoadModelAllowlistError() },
      title = uiState.loadingModelAllowlistError,
      confirm = stringResource(R.string.retry),
      onConfirm = { modelManagerViewModel.loadModelAllowlist() },
      dismiss = stringResource(R.string.cancel),
      icon = Icons.Rounded.Error,
      destructive = true,
    ) {
      Text(
        stringResource(R.string.error_internet_connection),
        style = GalleryType.Body,
        color = glassColors.inkSoft,
      )
    }
  }
}

@Composable
private fun BentoFilterPill(
  label: String,
  active: Boolean,
  onClick: () -> Unit,
) {
  val c = glassColors
  val activeBg = if (c.dark) Color(0xFFEBE69C) else Color(0xFF131D2E)
  val activeInk = if (c.dark) Color(0xFF11130A) else Color.White
  Box(
    modifier = Modifier
      .height(36.dp)
      .then(
        if (active) {
          Modifier.clip(Capsule).background(activeBg)
        } else {
          Modifier.wash(Capsule, tint = if (c.dark) Color(0xFF281A5C).copy(alpha = 0.56f) else c.glass)
        }
      )
      .pressable(onClick = onClick, role = Role.Tab)
      .padding(horizontal = 16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = GalleryType.BodyStrong,
      color = if (active) activeInk else c.ink,
      maxLines = 1,
    )
  }
}

@Composable
private fun HomeChatCapsuleDock(
  modifier: Modifier = Modifier,
  onTapToChat: () -> Unit,
  onVoiceClick: () -> Unit,
) {
  val c = glassColors
  val dockWash = c.capsule.copy(alpha = if (c.dark) 0.36f else 0.44f)
  val dockRim =
    androidx.compose.ui.graphics.Brush.verticalGradient(
      listOf(
        Color.White.copy(alpha = if (c.dark) 0.42f else 0.85f),
        Color.White.copy(alpha = if (c.dark) 0.14f else 0.38f),
      )
    )

  Row(
    modifier =
      modifier
        .glass(
          shape = ChromeCapsule,
          wash = dockWash,
          blur = 14.dp,
          lensHeight = 16.dp,
          lensAmount = 26.dp,
          styleOverride = com.google.ai.edge.gallery.core.design.glass.GlassSettings.effectiveStyle.copy(
            depthEffect = true,
            saturation = 1.45f,
            tint = 0.65f,
          ),
        )
        .border(1.dp, dockRim, ChromeCapsule)
        .padding(horizontal = 6.dp, vertical = 5.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      modifier =
        Modifier.width(196.dp)
          .height(48.dp)
          .clip(Capsule)
          .pressable(
            onClick = onTapToChat,
            role = Role.Button,
          )
          .semantics { contentDescription = "Tap to chat" }
          .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.Rounded.AutoAwesome,
        contentDescription = null,
        tint = c.ink,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = "Tap to chat",
        style = GalleryType.BodyStrong,
        color = c.ink,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(c.ink.copy(alpha = if (c.dark) 0.14f else 0.08f))
        .border(
          1.dp,
          if (c.dark) Color(0xFF27272A) else Color(0xFFE4E4E7),
          CircleShape,
        )
        .pressable(onClick = onVoiceClick, role = Role.Button)
        .semantics { contentDescription = "Voice" },
      contentAlignment = Alignment.Center,
    ) {
      GlassIcon(Icons.Rounded.Mic, "Voice", Modifier.size(20.dp))
    }
  }
}

@Composable
private fun ActiveModelBentoCard(
  model: Model?,
  ready: Boolean,
  onClick: () -> Unit,
) {
  val tone = glassColors.tone(Tone.Paper)
  BentoCardSurface(
    tone = tone,
    onClick = onClick,
    minHeight = 114.dp,
  ) {
    Text(
      text = model?.displayName?.ifBlank { model.name } ?: "Gemma",
      style = GalleryType.Headline.copy(fontSize = 21.sp, lineHeight = 25.sp),
      color = tone.accent,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(4.dp))
    Text(
      text = if (ready) "Ready" else "Tap to setup",
      style = GalleryType.BodyStrong,
      color = tone.ink,
      maxLines = 1,
    )
    Spacer(Modifier.height(10.dp))
    Text(
      text = model?.downloadInfo?.sizeInBytes?.takeIf { it > 0 }?.humanReadableSize() ?: "On-device",
      style = GalleryType.Caption,
      color = tone.ink.copy(alpha = 0.72f),
      modifier = Modifier.align(Alignment.End),
    )
  }
}

@Composable
private fun ModelsOverviewBentoCard(
  downloadedCount: Int,
  totalModelsCount: Int,
  onClick: () -> Unit,
) {
  val tone = glassColors.tone(Tone.Sky)
  BentoCardSurface(
    tone = tone,
    onClick = onClick,
    minHeight = 144.dp,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = "Models",
        style = GalleryType.Title.copy(fontSize = 19.sp),
        color = tone.ink,
      )
      Text(
        text = "$downloadedCount ready",
        style = GalleryType.Caption,
        color = tone.ink.copy(alpha = 0.72f),
      )
    }

    Spacer(Modifier.height(10.dp))

    val isDark = glassColors.dark
    val previewSurface = if (isDark) Color(0xFF27272A) else Color(0xFFFFFFFF)
    val previewLine = if (isDark) Color(0xFFFAFAFA).copy(alpha = 0.16f) else Color(0xFF18181B).copy(alpha = 0.12f)
    val badgeBg = if (isDark) Color(0xFF27272A) else Color(0xFF18181B)
    val badgeFg = Color(0xFFFAFAFA)

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .clip(RoundedRectangle(12.dp))
        .background(previewSurface)
        .border(1.dp, if (isDark) Color(0xFF3F3F46) else Color(0xFFE4E4E7), RoundedRectangle(12.dp))
        .padding(8.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth(0.55f).height(5.dp).clip(Capsule).background(tone.ink.copy(alpha = 0.24f)))
        Box(Modifier.fillMaxWidth(0.85f).height(4.dp).clip(Capsule).background(previewLine))
        Box(Modifier.fillMaxWidth(0.70f).height(4.dp).clip(Capsule).background(previewLine))
      }
      Icon(
        Icons.Rounded.AutoAwesome,
        contentDescription = null,
        tint = tone.ink.copy(alpha = 0.75f),
        modifier = Modifier.align(Alignment.BottomEnd).size(15.dp),
      )
    }

    Spacer(Modifier.height(10.dp))

    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .clip(Capsule)
          .background(badgeBg)
          .padding(horizontal = 9.dp, vertical = 4.dp),
      ) {
        Text("+$totalModelsCount", style = GalleryType.Caption.copy(fontWeight = FontWeight.Bold), color = badgeFg)
      }
      Row(
        modifier = Modifier
          .clip(Capsule)
          .background(badgeBg)
          .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(Icons.AutoMirrored.Rounded.ListAlt, null, tint = badgeFg, modifier = Modifier.size(12.dp))
        Text("Library", style = GalleryType.Caption.copy(fontWeight = FontWeight.SemiBold), color = badgeFg)
      }
    }
  }
}

@Composable
private fun QuickStartBentoCard(
  hasModel: Boolean,
  onNewChat: () -> Unit,
  onModels: () -> Unit,
  onBenchmark: () -> Unit,
) {
  val tone = glassColors.tone(Tone.Sage)
  BentoCardSurface(
    tone = tone,
    onClick = onNewChat,
    minHeight = 148.dp,
  ) {
    Text(
      text = "Quick Start",
      style = GalleryType.Title.copy(fontSize = 20.sp, letterSpacing = 0.3.sp),
      color = tone.ink,
    )
    Spacer(Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      ChecklistPill(label = if (hasModel) "New chat" else "Pick model", onClick = onNewChat)
      ChecklistPill(label = "Model hub", onClick = onModels)
      ChecklistPill(label = "Benchmark", onClick = onBenchmark)
    }
  }
}

@Composable
private fun ChecklistPill(label: String, onClick: () -> Unit) {
  val c = glassColors
  val pillBg = if (c.dark) Color(0xFF27272A) else Color(0xFFFFFFFF)
  val pillRim = if (c.dark) Color(0xFF3F3F46) else Color(0xFFE4E4E7)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(Capsule)
      .background(pillBg)
      .border(1.dp, pillRim, Capsule)
      .pressable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(
      modifier = Modifier
        .size(18.dp)
        .border(1.5.dp, c.inkSoft, CircleShape),
    )
    Text(
      text = label,
      style = GalleryType.Label.copy(fontWeight = FontWeight.SemiBold),
      color = c.ink,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun SessionBentoCard(
  session: ChatSessionProto,
  cardTone: CardTone,
  tall: Boolean,
  onClick: () -> Unit,
) {
  val dateStr = remember(session.timestampMs) {
    if (session.timestampMs <= 0L) ""
    else {
      val zdt = Instant.ofEpochMilli(session.timestampMs).atZone(ZoneId.systemDefault())
      if (zdt.toLocalDate() == LocalDate.now()) {
        DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).format(zdt)
      } else {
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()).format(zdt)
      }
    }
  }
  BentoCardSurface(
    tone = cardTone,
    onClick = onClick,
    minHeight = if (tall) 190.dp else 148.dp,
  ) {
    Text(
      text = session.title.ifBlank { "Chat" },
      style = GalleryType.Headline.copy(fontSize = 21.sp, lineHeight = 25.sp),
      color = cardTone.ink,
      maxLines = 3,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = session.originalModel.ifBlank { "Conversation" },
      style = GalleryType.Label,
      color = cardTone.ink.copy(alpha = 0.78f),
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(14.dp))
    if (dateStr.isNotEmpty()) {
      Text(
        text = dateStr,
        style = GalleryType.Caption,
        color = cardTone.ink.copy(alpha = 0.72f),
        modifier = Modifier.align(Alignment.End),
      )
    }
  }
}

@Composable
private fun ExperienceBentoCard(
  task: Task,
  cardTone: CardTone,
  compact: Boolean,
  onClick: () -> Unit,
) {
  val modelCount by remember {
    derivedStateOf {
      val trigger = task.updateTrigger.value
      if (trigger >= 0) task.models.size else 0
    }
  }
  val icon = task.icon ?: task.iconVectorResourceId?.let { ImageVector.vectorResource(it) } ?: Icons.Rounded.Hub

  BentoCardSurface(
    tone = cardTone,
    onClick = onClick,
    minHeight = if (compact) 152.dp else 174.dp,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top,
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedRectangle(14.dp))
          .background(glassColors.ink.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, contentDescription = null, tint = cardTone.ink, modifier = Modifier.size(22.dp))
      }
      Box(
        modifier = Modifier
          .clip(Capsule)
          .background(glassColors.ink.copy(alpha = 0.08f))
          .padding(horizontal = 9.dp, vertical = 4.dp),
      ) {
        Text("$modelCount", style = GalleryType.Caption.copy(fontWeight = FontWeight.Bold), color = cardTone.ink)
      }
    }

    Spacer(Modifier.height(18.dp))

    Text(
      text = task.label,
      style = GalleryType.Headline.copy(fontSize = 22.sp, lineHeight = 25.sp, fontWeight = FontWeight.ExtraBold),
      color = cardTone.ink,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ModelQuickBentoCard(
  model: Model,
  ready: Boolean,
  cardTone: CardTone,
  onClick: () -> Unit,
) {
  BentoCardSurface(
    tone = cardTone,
    onClick = onClick,
    minHeight = 148.dp,
  ) {
    Icon(
      Icons.Rounded.Description,
      contentDescription = null,
      tint = cardTone.accent,
      modifier = Modifier.size(24.dp),
    )
    Spacer(Modifier.height(10.dp))
    Text(
      text = model.displayName.ifBlank { model.name },
      style = GalleryType.CardTitle,
      color = cardTone.ink,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(6.dp))
    Text(
      text = if (ready) "Ready" else model.downloadInfo.sizeInBytes.takeIf { it > 0 }?.humanReadableSize() ?: "Available",
      style = GalleryType.Caption,
      color = cardTone.ink.copy(alpha = 0.72f),
    )
  }
}

@Composable
private fun BentoCardSurface(
  tone: CardTone,
  onClick: () -> Unit,
  minHeight: androidx.compose.ui.unit.Dp = 152.dp,
  content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
  val shape = RoundedRectangle(24.dp)
  val rim = if (glassColors.dark) Color(0xFF27272A) else Color(0xFFE4E4E7)
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .background(Brush.verticalGradient(listOf(tone.top, tone.bottom)))
      .border(1.dp, rim, shape)
      .pressable(onClick = onClick)
      .padding(16.dp),
    verticalArrangement = Arrangement.Top,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().heightIn(min = minHeight - 32.dp),
      content = content,
    )
  }
}
