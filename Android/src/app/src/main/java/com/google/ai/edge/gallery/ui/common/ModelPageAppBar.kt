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

package com.google.ai.edge.gallery.ui.common

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.ai.edge.gallery.BuildConfig
import com.google.ai.edge.gallery.GalleryEvent
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.common.logButtonClick
import com.google.ai.edge.gallery.core.design.components.Capsule
import com.google.ai.edge.gallery.core.design.components.GlassBackButton
import com.google.ai.edge.gallery.core.design.components.GlassIcon
import com.google.ai.edge.gallery.core.design.components.GlassMenuItem
import com.google.ai.edge.gallery.core.design.components.GlassSheet
import com.google.ai.edge.gallery.core.design.components.HeaderButtonSize
import com.google.ai.edge.gallery.core.design.components.HeaderGlassButton
import com.google.ai.edge.gallery.core.design.components.MorphButton
import com.google.ai.edge.gallery.core.design.components.glass
import com.google.ai.edge.gallery.core.design.components.pressable
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType
import com.google.ai.edge.gallery.customtasks.agentchat.agentSkillTopK
import com.google.ai.edge.gallery.customtasks.agentchat.agentSkillTopKAdjusted
import com.google.ai.edge.gallery.customtasks.xrlanguageteacher.models.xrTeacherTopK
import com.google.ai.edge.gallery.customtasks.xrlanguageteacher.models.xrTeacherTopKAdjusted
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Config
import com.google.ai.edge.gallery.data.ConfigKeys
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelCapability
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.data.ModelUtils
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.data.convertValueToTargetType
import com.google.ai.edge.gallery.firebaseAnalytics
import com.google.ai.edge.gallery.ui.home.AppearanceSettingsSheet
import com.google.ai.edge.gallery.ui.home.SettingsDialog
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.ui.modelmanager.buildAudioTabUiState
import com.google.ai.edge.gallery.ui.modelmanager.createAudioTabActions
import com.google.ai.edge.gallery.ui.modelmanager.dismissAudioConfigOnboardingForSession
import com.google.ai.edge.gallery.ui.modelmanager.dismissAudioConfigOnboardingPermanently
import com.google.ai.edge.gallery.ui.modelmanager.isTaskEligibleForAudioConfigOnboarding
import com.google.ai.edge.gallery.ui.modelmanager.observeAudioTabUiState
import com.google.ai.edge.gallery.ui.modelmanager.onAudioConfigOpened
import com.google.ai.edge.gallery.ui.modelmanager.refreshAudioConfigOnboardingState
import com.google.ai.edge.gallery.ui.theme.ThemeSettings
import com.google.ai.edge.litertlm.ModelInfo
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPageAppBar(
  task: Task,
  model: Model,
  modelManagerViewModel: ModelManagerViewModel,
  onBackClicked: () -> Unit,
  onModelSelected: (prev: Model, cur: Model) -> Unit,
  inProgress: Boolean,
  modelPreparing: Boolean,
  modifier: Modifier = Modifier,
  hideModelSelector: Boolean = false,
  hideConfigButton: Boolean = false,
  useThemeColor: Boolean = false,
  onConfigChanged: (oldConfigValues: Map<String, Any>, newConfigValues: Map<String, Any>) -> Unit =
    { _, _ -> },
  allowEditingSystemPrompt: Boolean = false,
  curSystemPrompt: String = "",
  defaultSystemPrompt: String = task.defaultSystemPrompt,
  onSystemPromptChanged: (String) -> Unit = {},
  shouldShowHistoryButton: Boolean = false,
  onHistoryClicked: (Model) -> Unit = {},
  isAutoCompactEnabled: Boolean = false,
  actionsDisabled: Boolean = false,
  @Suppress("UNUSED_PARAMETER") expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
  useGlobalChatChrome: Boolean = false,
) {
  val c = glassColors
  val context = LocalContext.current
  var showConfigDialog by remember { mutableStateOf(false) }
  var showModelSelectionSheet by remember { mutableStateOf(false) }
  var showModelDetailsSheet by remember { mutableStateOf(false) }
  var showAppearanceSheet by remember { mutableStateOf(false) }
  var showAppSettingsDialog by remember { mutableStateOf(false) }

  val modelManagerUiState by modelManagerViewModel.uiState.collectAsStateWithLifecycle()
  val curDownloadStatus = modelManagerUiState.modelDownloadStatus[model.name]
  val initStatus by model.initStatusFlow.collectAsStateWithLifecycle()
  val isModelInitializing = initStatus is Model.InitializationStatus.Initializing
  val isModelInitialized = initStatus is Model.InitializationStatus.Initialized
  val audioConfigOnboardingDismissedInSession by
    modelManagerViewModel.audioConfigOnboardingDismissedInSession.collectAsStateWithLifecycle()
  val audioConfigOnboardingPromoViewed by
    modelManagerViewModel.audioConfigOnboardingPromoViewed.collectAsStateWithLifecycle()
  LaunchedEffect(modelManagerViewModel, task.id) {
    val unused = modelManagerViewModel.refreshAudioConfigOnboardingState()
  }
  var configButtonBounds by remember { mutableStateOf<IntRect?>(null) }
  val enableAsrForOnboarding = true
  val enableTtsForOnboarding = true
  var showAudioConfigOnboarding by
    remember(
      task.id,
      audioConfigOnboardingDismissedInSession,
      audioConfigOnboardingPromoViewed,
      enableAsrForOnboarding,
      enableTtsForOnboarding,
    ) {
      mutableStateOf(
        !audioConfigOnboardingDismissedInSession &&
          audioConfigOnboardingPromoViewed == false &&
          modelManagerViewModel.isTaskEligibleForAudioConfigOnboarding(task)
      )
    }

  val downloadSucceeded = curDownloadStatus?.status == ModelDownloadStatusType.SUCCEEDED
  val enableControls = !isModelInitializing && !inProgress && !actionsDisabled
  val enableConfigButton = enableControls && isModelInitialized && downloadSucceeded
  val enableHistoryButton = !inProgress && !actionsDisabled && !modelPreparing
  val enableBackButton = !inProgress

  if (useGlobalChatChrome) {
    val menuItems = buildList {
      add(
        GlassMenuItem(
          label = model.displayName.ifEmpty { model.name },
          subtitle = "Current model · ${if (isModelInitialized) "Ready" else if (isModelInitializing) "Loading..." else "Standby"}",
          icon = Icons.Rounded.AutoAwesome,
          onClick = { showModelDetailsSheet = true },
        )
      )
      if (!hideModelSelector && task.models.size > 1) {
        add(
          GlassMenuItem(
            label = "Model selection",
            subtitle = "${task.models.size} models",
            icon = Icons.Rounded.SwapHoriz,
            onClick = { if (enableControls) showModelSelectionSheet = true },
          )
        )
      }
      add(
        GlassMenuItem(
          label = "Model details",
          icon = Icons.Rounded.Info,
          onClick = { showModelDetailsSheet = true },
        )
      )
      if (!hideConfigButton && model.configs.isNotEmpty() && downloadSucceeded) {
        add(
          GlassMenuItem(
            label = "Inference settings",
            icon = Icons.Rounded.Tune,
            onClick = {
              if (enableConfigButton) {
                showAudioConfigOnboarding = false
                modelManagerViewModel.dismissAudioConfigOnboardingForSession()
                showConfigDialog = true
              }
            },
          )
        )
      }
      add(
        GlassMenuItem(
          label = "Appearance",
          icon = Icons.Rounded.Palette,
          onClick = { showAppearanceSheet = true },
        )
      )
      add(
        GlassMenuItem(
          label = "Application settings",
          icon = Icons.Rounded.Settings,
          onClick = { showAppSettingsDialog = true },
        )
      )
    }

    Row(
      modifier = modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      // TOP LEFT: Transparent circular back button.
      Box(
        modifier =
          Modifier.size(HeaderButtonSize)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .pressable(
              onClick = onBackClicked,
              role = androidx.compose.ui.semantics.Role.Button,
            )
            .alpha(if (enableBackButton) 1f else 0.45f),
        contentAlignment = Alignment.Center,
      ) {
        GlassIcon(
          Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.cd_navigate_back_icon),
          modifier = Modifier.size(19.dp),
        )
      }

      // CENTER: Clean transparent model selector pill with text only.
      Row(
        modifier =
          Modifier.widthIn(max = 196.dp)
            .height(38.dp)
            .clip(Capsule)
            .background(Color.Transparent)
            .pressable(
              onClick = {
                if (!hideModelSelector && task.models.size > 1 && enableControls) {
                  showModelSelectionSheet = true
                } else {
                  showModelDetailsSheet = true
                }
              },
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        Text(
          text = model.displayName.ifEmpty { model.name },
          style = GalleryType.Label,
          color = c.ink,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      // TOP RIGHT: Transparent [ 3-dot menu | 3-bar History ] controls without background box border.
      Row(
        modifier =
          Modifier.height(HeaderButtonSize)
            .clip(com.google.ai.edge.gallery.core.design.components.ChromeCapsule)
            .background(Color.Transparent)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        MorphButton(
          items = menuItems,
          label = "Chat menu",
          size = 36.dp,
          menuWidth = 264.dp,
          enabled = !actionsDisabled,
          transparent = true,
          modifier = Modifier.onGloballyPositioned { coordinates ->
            if (showAudioConfigOnboarding) {
              val pos = coordinates.positionInWindow()
              val newBounds = IntRect(IntOffset(pos.x.roundToInt(), pos.y.roundToInt()), coordinates.size)
              if (configButtonBounds != newBounds) configButtonBounds = newBounds
            }
          },
        ) {
          GlassIcon(Icons.Rounded.MoreHoriz, "Menu", Modifier.size(20.dp))
        }
        if (downloadSucceeded && shouldShowHistoryButton) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(androidx.compose.foundation.shape.CircleShape)
              .pressable(onClick = { if (enableHistoryButton) onHistoryClicked(model) })
              .alpha(if (enableHistoryButton) 1f else 0.5f),
            contentAlignment = Alignment.Center,
          ) {
            GlassIcon(Icons.Rounded.Menu, stringResource(R.string.cd_chat_history), Modifier.size(19.dp))
          }
        }
      }
    }
  } else {
    Row(
      modifier = modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      GlassBackButton(
        onBack = onBackClicked,
        immediate = true,
        modifier = Modifier.alpha(if (enableBackButton) 1f else 0.45f),
      )

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          val tintColor =
            if (useThemeColor) c.ink else getTaskIconColor(task = task)
          val icon = task.icon ?: task.iconVectorResourceId?.let { ImageVector.vectorResource(it) }
          if (icon != null) {
            Icon(icon, tint = tintColor, modifier = Modifier.size(20.dp), contentDescription = null)
          }
          Text(
            task.label,
            style = GalleryType.Title,
            color = c.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (!hideModelSelector) {
          ModelPickerChip(
            enabled = enableControls,
            task = task,
            initialModel = model,
            modelManagerViewModel = modelManagerViewModel,
            onModelSelected = onModelSelected,
          )
        }
      }

      val showConfigButton =
        !hideConfigButton &&
          model.configs.isNotEmpty() &&
          downloadSucceeded &&
          task.id != BuiltInTaskId.LLM_XR_LANGUAGE_TEACHER
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (showConfigButton) {
          HeaderGlassButton(
            onClick = {
              if (enableConfigButton) {
                showAudioConfigOnboarding = false
                modelManagerViewModel.dismissAudioConfigOnboardingForSession()
                showConfigDialog = true
              }
            },
            label = stringResource(R.string.cd_model_settings_icon),
            size = 40.dp,
            modifier = Modifier
              .onGloballyPositioned { coordinates ->
                if (showAudioConfigOnboarding) {
                  val pos = coordinates.positionInWindow()
                  val newBounds = IntRect(IntOffset(pos.x.roundToInt(), pos.y.roundToInt()), coordinates.size)
                  if (configButtonBounds != newBounds) configButtonBounds = newBounds
                }
              }
              .alpha(if (enableConfigButton) 1f else 0.5f),
          ) {
            GlassIcon(Icons.Rounded.Tune, stringResource(R.string.cd_model_settings_icon), Modifier.size(19.dp))
          }
        }
        if (downloadSucceeded && shouldShowHistoryButton) {
          HeaderGlassButton(
            onClick = { if (enableHistoryButton) onHistoryClicked(model) },
            label = stringResource(R.string.cd_chat_history),
            size = 40.dp,
            modifier = Modifier.alpha(if (enableHistoryButton) 1f else 0.5f),
          ) {
            GlassIcon(Icons.Rounded.History, stringResource(R.string.cd_chat_history), Modifier.size(19.dp))
          }
        }
        if (!showConfigButton && !(downloadSucceeded && shouldShowHistoryButton)) {
          Spacer(Modifier.size(HeaderButtonSize))
        }
      }
    }
  }

  val downloadSucceededForOnboarding =
    curDownloadStatus?.status == ModelDownloadStatusType.SUCCEEDED
  val configButtonReadyForOnboarding =
    !hideConfigButton &&
      model.configs.isNotEmpty() &&
      downloadSucceededForOnboarding &&
      task.id != BuiltInTaskId.LLM_XR_LANGUAGE_TEACHER &&
      !isModelInitializing &&
      !modelPreparing &&
      !inProgress &&
      !actionsDisabled &&
      isModelInitialized &&
      !showConfigDialog
  val currentConfigButtonBounds = configButtonBounds
  if (
    showAudioConfigOnboarding && configButtonReadyForOnboarding && currentConfigButtonBounds != null
  ) {
    AudioConfigOnboardingTooltip(
      anchorBounds = currentConfigButtonBounds,
      onGotIt = {
        logButtonClick("audio_config_onboarding_got_it", capabilityName = task.id)
        showAudioConfigOnboarding = false
        modelManagerViewModel.dismissAudioConfigOnboardingPermanently()
      },
      onSpotlightClick = {
        logButtonClick("audio_config_onboarding_spotlight", capabilityName = task.id)
        showAudioConfigOnboarding = false
        modelManagerViewModel.dismissAudioConfigOnboardingPermanently()
        showConfigDialog = true
      },
    )
  }

  if (showModelSelectionSheet) {
    GlassSheet(
      onDismiss = { showModelSelectionSheet = false },
      title = "Select Model",
      subtitle = task.label,
    ) {
      ModelPicker(
        task = task,
        modelManagerViewModel = modelManagerViewModel,
        onModelSelected = { next ->
          showModelSelectionSheet = false
          onModelSelected(model, next)
        },
      )
    }
  }

  if (showModelDetailsSheet) {
    ModelDetailsSheet(
      model = model,
      downloadStatus = curDownloadStatus,
      onDismiss = { showModelDetailsSheet = false },
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

  if (showAppSettingsDialog) {
    SettingsDialog(
      curThemeOverride = modelManagerViewModel.readThemeOverride(),
      curFirebaseAnalytics = modelManagerViewModel.readFirebaseAnalytics(),
      modelManagerViewModel = modelManagerViewModel,
      onDismissed = { showAppSettingsDialog = false },
    )
  }

  // Config dialog.
  if (showConfigDialog) {
    TaskConfigDialog(
      task = task,
      model = model,
      modelManagerViewModel = modelManagerViewModel,
      onDismissed = { showConfigDialog = false },
      onConfigChanged = onConfigChanged,
      allowEditingSystemPrompt = allowEditingSystemPrompt,
      curSystemPrompt = curSystemPrompt,
      defaultSystemPrompt = defaultSystemPrompt,
      onSystemPromptChanged = onSystemPromptChanged,
      isAutoCompactEnabled = isAutoCompactEnabled,
    )
  }
}

/**
 * Displays [ConfigDialog] for [task] and [model], computing task-filtered model configs, observing
 * the Audio tab state, and handling model re-initialization and analytics when configs change.
 */
@Composable
fun TaskConfigDialog(
  task: Task,
  model: Model,
  modelManagerViewModel: ModelManagerViewModel,
  onDismissed: () -> Unit,
  onConfigChanged: (oldConfigValues: Map<String, Any>, newConfigValues: Map<String, Any>) -> Unit =
    { _, _ ->
    },
  allowEditingSystemPrompt: Boolean = false,
  curSystemPrompt: String = "",
  defaultSystemPrompt: String = task.defaultSystemPrompt,
  onSystemPromptChanged: (String) -> Unit = {},
  isAutoCompactEnabled: Boolean = false,
  initialTab: ConfigTab = ConfigTab.MODEL_CONFIGS,
) {
  val context = LocalContext.current
  var supportsSpeculativeDecoding by
    remember(model.name, task.id) {
      mutableStateOf(model.configs.any { it.key == ConfigKeys.ENABLE_SPECULATIVE_DECODING })
    }
  var currentConfigs by remember(model.name, task.id) { mutableStateOf(model.configs) }
  var currentConfigValues by remember(model.name, task.id) { mutableStateOf(model.configValues) }

  LaunchedEffect(model.name, task.id) {
    // Check if the model file supports speculative decoding and visual token budget selection.
    // The model file is read off the main thread; the model configs are updated on the main
    // thread.
    try {
      val (specSupport, visionOptions, visionCeiling) =
        withContext(Dispatchers.IO) {
          ModelInfo.from(model.getPath(context)).use { modelInfo ->
            Triple(
              (modelInfo as? ModelInfo.Llm)?.hasSpeculativeDecodingSupport(),
              modelInfo.visionSignatureSelection(),
              modelInfo.maxVisionTokenBudget(),
            )
          }
        }
      if (specSupport != null) {
        supportsSpeculativeDecoding = specSupport
      }
      val unused =
        ModelUtils.configureVisualTokenBudgetForModel(
          model = model,
          shouldEnableImage = task.isVisionEnabled(model),
          rawOptions = visionOptions,
          ceiling = visionCeiling,
        )
      currentConfigs = model.configs
      currentConfigValues = model.configValues
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      // Ignore exceptions and assume not supported.
    }
  }

  // Remove the reset conversation turn count config for non-tiny-garden tasks.
  //
  // This may happen when user imports a model with "enable tiny garden" turned on and use the
  // model in another non-tiny-garden task.
  val modelConfigs =
    remember(currentConfigs, supportsSpeculativeDecoding, task, model) {
      currentConfigs.toMutableList().apply {
        if (task.id != BuiltInTaskId.LLM_TINY_GARDEN) {
          removeIf { it.key == ConfigKeys.RESET_CONVERSATION_TURN_COUNT }
        }
        if (!task.allowCapability(ModelCapability.LLM_THINKING, model)) {
          removeIf { it.key == ConfigKeys.ENABLE_THINKING }
        }
        if (!task.isVisionEnabled(model)) {
          removeIf { it.key == ConfigKeys.VISUAL_TOKEN_BUDGET }
        }
        if (
          !supportsSpeculativeDecoding ||
            !task.allowCapability(ModelCapability.SPECULATIVE_DECODING, model)
        ) {
          removeIf { it.key == ConfigKeys.ENABLE_SPECULATIVE_DECODING }
        }
        if (task.isContextCompactSupported(model)) {
          if (none { it.key == ConfigKeys.ENABLE_AUTO_CONTEXT_COMPACT }) {
            add(Config.createAutoContextCompactConfig())
          }
        } else {
          removeIf { it.key == ConfigKeys.ENABLE_AUTO_CONTEXT_COMPACT }
        }
      }
    }
  val dialogInitialValues =
    remember(currentConfigValues, modelConfigs, task, model, isAutoCompactEnabled) {
      currentConfigValues.toMutableMap().apply {
        for (config in modelConfigs) {
          putIfAbsent(config.key.label, config.defaultValue)
        }
        if (task.isContextCompactSupported(model)) {
          put(ConfigKeys.ENABLE_AUTO_CONTEXT_COMPACT.label, isAutoCompactEnabled)
        }
      }
    }
  LaunchedEffect(modelManagerViewModel, task) { modelManagerViewModel.onAudioConfigOpened(task) }
  val initialAudioUiState =
    remember(modelManagerViewModel, task) { modelManagerViewModel.buildAudioTabUiState(task) }
  val audioUiState by
    remember(modelManagerViewModel, task) { modelManagerViewModel.observeAudioTabUiState(task) }
      .collectAsStateWithLifecycle(initialValue = initialAudioUiState)
  val audioActions =
    remember(modelManagerViewModel, task) { modelManagerViewModel.createAudioTabActions(task) }
  ConfigDialog(
    title = stringResource(R.string.config_dialog_title),
    configs = modelConfigs,
    initialValues = dialogInitialValues,
    onDismissed = onDismissed,
    onOk = { curConfigValues, oldSystemPrompt, newSystemPrompt ->
      // Hide config dialog.
      onDismissed()

      // Check if the configs are changed or not. Also check if the model needs to be
      // re-initialized.
      var same = true
      var modelConfigChanged = false
      var needReinitialization = false
      for (config in modelConfigs) {
        val key = config.key.label
        val oldValue =
          convertValueToTargetType(
            value = dialogInitialValues[key] ?: config.defaultValue,
            valueType = config.valueType,
          )
        val newValue =
          convertValueToTargetType(
            value = curConfigValues[key] ?: config.defaultValue,
            valueType = config.valueType,
          )
        if (oldValue != newValue) {
          same = false
          if (config.key != ConfigKeys.ENABLE_AUTO_CONTEXT_COMPACT) {
            modelConfigChanged = true
          }
          if (config.needReinitialization) {
            needReinitialization = true
          }
          if (modelConfigChanged && needReinitialization) {
            break
          }
        }
      }
      val systemPromptChanged = newSystemPrompt != oldSystemPrompt

      if (modelConfigChanged || systemPromptChanged) {
        firebaseAnalytics?.logEvent(
          GalleryEvent.MODEL_CONFIG_CHANGE.id,
          Bundle().apply {
            putString("model_id", model.name)
            putString("capability_name", task.id)
            putString("model_version", model.downloadInfo.version)
            putString("app_version", BuildConfig.VERSION_NAME)
          },
        )
      }

      if (same) {
        if (systemPromptChanged) {
          onSystemPromptChanged(newSystemPrompt)
        }
        return@ConfigDialog
      }

      // Save the config values to Model (excluding task-level auto context compaction setting).
      val oldConfigValues = model.configValues
      model.prevConfigValues = oldConfigValues
      model.configValues = curConfigValues.filterKeys {
        it != ConfigKeys.ENABLE_AUTO_CONTEXT_COMPACT.label
      }
      when (task.id) {
        BuiltInTaskId.LLM_AGENT_CHAT -> {
          model.agentSkillTopKAdjusted = true
          model.agentSkillTopK = curConfigValues[ConfigKeys.TOPK.label]
        }
        BuiltInTaskId.LLM_XR_LANGUAGE_TEACHER -> {
          model.xrTeacherTopKAdjusted = true
          model.xrTeacherTopK = curConfigValues[ConfigKeys.TOPK.label]
        }
      }
      modelManagerViewModel.updateConfigValuesUpdateTrigger()

      if (!task.handleModelConfigChangesInTask) {
        // Force to re-initialize the model with the new configs.
        if (needReinitialization) {
          modelManagerViewModel.initializeModel(
            context = context,
            task = task,
            model = model,
            force = true,
            onDone = {
              if (oldSystemPrompt != newSystemPrompt) {
                onSystemPromptChanged(newSystemPrompt)
              }
            },
          )
        }

        // Notify.
        onConfigChanged(dialogInitialValues, curConfigValues)
      }
    },
    showSystemPromptEditorTab = allowEditingSystemPrompt,
    defaultSystemPrompt = defaultSystemPrompt,
    curSystemPrompt = curSystemPrompt,
    audioUiState = audioUiState,
    audioActions = audioActions,
    initialTab = initialTab,
  )
}
