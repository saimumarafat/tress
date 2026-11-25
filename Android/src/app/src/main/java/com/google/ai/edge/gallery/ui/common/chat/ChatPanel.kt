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

package com.google.ai.edge.gallery.ui.common.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.core.design.components.GlassCard
import com.google.ai.edge.gallery.core.design.components.GlassLoader
import com.google.ai.edge.gallery.core.design.components.RoundedRectangle
import com.google.ai.edge.gallery.core.design.components.wash
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.common.AudioAnimation
import com.google.ai.edge.gallery.ui.common.ErrorDialog
import com.google.ai.edge.gallery.ui.common.FloatingBanner
import com.google.ai.edge.gallery.ui.common.ScrollToBottomButton
import com.google.ai.edge.gallery.ui.common.asr.rememberChatAsrDictationViewModel
import com.google.ai.edge.gallery.ui.common.tts.ChatTtsControlBanner
import com.google.ai.edge.gallery.ui.common.tts.TtsChatEffects
import com.google.ai.edge.gallery.ui.common.tts.TtsPlaybackController
import com.google.ai.edge.gallery.ui.common.tts.TtsSpeakerButton
import com.google.ai.edge.gallery.ui.common.tts.rememberChatTtsScrollState
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.ui.modelmanager.isTaskEligibleForAudioConfigOnboarding
import com.google.ai.edge.gallery.ui.modelmanager.refreshAudioConfigOnboardingState
import com.google.ai.edge.gallery.ui.theme.customColors
import com.kyant.backdrop.throttleBackdrops
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TAG = "AGChatPanel"
private const val SCROLL_ANIMATION_DURATION_MS = 300

private const val TEST_CHAT_LIMITED_NOTE_PROMO_ID = "test_chat_limited_functionality_note"

/** Composable function for the main chat panel, displaying messages and handling user input. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPanel(
  modelManagerViewModel: ModelManagerViewModel,
  task: Task,
  selectedModel: Model,
  viewModel: ChatViewModel,
  innerPadding: PaddingValues,
  modifier: Modifier = Modifier,
  skillCount: Int = 0,
  mcpCount: Int = 0,
  onSendMessage: (Model, List<ChatMessage>) -> Unit,
  onRunAgainClicked: (Model, ChatMessage) -> Unit,
  onBenchmarkClicked: (Model, ChatMessage, warmUpIterations: Int, benchmarkIterations: Int) -> Unit,
  navigateUp: () -> Unit,
  onStreamImageMessage: (Model, ChatMessageImage) -> Unit = { _, _ -> },
  onStreamEnd: (Int) -> Unit = {},
  onStopButtonClicked: () -> Unit = {},
  onSkillClicked: () -> Unit = {},
  onMcpClicked: () -> Unit = {},
  onImageSelected: (bitmaps: List<Bitmap>, selectedBitmapIndex: Int) -> Unit = { _, _ -> },
  onFileSelected: (ChatMessageFile) -> Unit = {},
  showStopButtonInInputWhenInProgress: Boolean = false,
  showImagePicker: Boolean = false,
  showAudioPicker: Boolean = false,
  showTextFilePicker: Boolean = false,
  emptyStateComposable: @Composable (Model) -> Unit = {},
  onNewChatClicked: () -> Unit = {},
  ttsController: TtsPlaybackController? = null,
) {
  val uiState by viewModel.uiState.collectAsState()
  val messages = uiState.messagesByModel[selectedModel.name] ?: listOf()
  val modelInitStatus by selectedModel.initStatusFlow.collectAsState()
  val isLiveMetricsFlagEnabled = false
  val isTextFileInputEnabled = false
  val isLiveMetricsSupported = isLiveMetricsFlagEnabled && !selectedModel.isAiCore
  val hasLiveMetrics by
    remember(viewModel, selectedModel.name, isLiveMetricsSupported) {
        if (isLiveMetricsSupported) {
          viewModel.liveMetrics
            .map { metrics ->
              metrics != null &&
                (metrics.metadata.modelName.isEmpty() ||
                  metrics.metadata.modelName == selectedModel.name)
            }
            .distinctUntilChanged()
        } else {
          flowOf(false)
        }
      }
      .collectAsStateWithLifecycle(initialValue = false)
  val showLiveMetrics = uiState.showLiveMetrics
  // The live metrics toggle button is interactive only when metrics are available for the
  // initialized model and no inference turn or session reset is active.
  val isLiveMetricsToggleEnabled =
    hasLiveMetrics &&
      modelInitStatus is Model.InitializationStatus.Initialized &&
      !uiState.isResettingSession &&
      !uiState.inProgress

  val audioConfigOnboardingDismissedInSession by
    modelManagerViewModel.audioConfigOnboardingDismissedInSession.collectAsStateWithLifecycle()
  val audioConfigOnboardingPromoViewed by
    modelManagerViewModel.audioConfigOnboardingPromoViewed.collectAsStateWithLifecycle()

  if (isLiveMetricsFlagEnabled) {
    LaunchedEffect(viewModel, task.id, selectedModel) {
      viewModel.loadLiveMetricsVisibility(task.id)
      viewModel.observeLiveMetrics(selectedModel)
    }
    LaunchedEffect(
      isLiveMetricsToggleEnabled,
      task.id,
      audioConfigOnboardingDismissedInSession,
      audioConfigOnboardingPromoViewed,
    ) {
      if (isLiveMetricsToggleEnabled) {
        if (
          !audioConfigOnboardingDismissedInSession &&
            modelManagerViewModel.isTaskEligibleForAudioConfigOnboarding(task)
        ) {
          val promoViewed =
            audioConfigOnboardingPromoViewed
              ?: modelManagerViewModel.refreshAudioConfigOnboardingState()
          if (!promoViewed) {
            return@LaunchedEffect
          }
        }
        viewModel.maybeShowLiveMetricsPrompt(task.id)
      }
    }
  }
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  val clipboard =
    remember(context) { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
  val copyToClipboard: (String) -> Unit =
    remember(clipboard) {
      { text -> clipboard.setPrimaryClip(ClipData.newPlainText("message", text)) }
    }
  val imageCountToLastConfigChange =
    remember(messages) {
      var imageCount = 0
      for (message in messages.reversed()) {
        if (message is ChatMessageConfigValuesChange) {
          break
        }
        if (message is ChatMessageImage) {
          imageCount += message.bitmaps.size
        }
      }
      imageCount
    }
  val audioClipMesssageCountToLastconfigChange =
    remember(messages) {
      var audioClipMessageCount = 0
      for (message in messages.reversed()) {
        if (message is ChatMessageConfigValuesChange) {
          break
        }
        if (message is ChatMessageAudioClip) {
          audioClipMessageCount++
        }
      }
      audioClipMessageCount
    }

  var curMessage by remember { mutableStateOf("") } // Correct state
  val focusManager = LocalFocusManager.current

  // List state to control scrolling.
  val listState = rememberLazyListState()
  val density = LocalDensity.current
  var showBenchmarkConfigsDialog by remember { mutableStateOf(false) }
  val benchmarkMessage: MutableState<ChatMessage?> = remember { mutableStateOf(null) }

  var showErrorDialog by remember { mutableStateOf(false) }
  var customErrorMessage by remember { mutableStateOf<String?>(null) }
  var showFeedbackDialog by remember { mutableStateOf(false) }
  var isPositiveFeedback by remember { mutableStateOf(true) }
  var feedbackMessageIndex by remember { mutableIntStateOf(-1) }

  var showAudioRecorder by remember { mutableStateOf(false) }
  var curAmplitude by remember { mutableIntStateOf(0) }
  var pickedImagesCount by remember { mutableIntStateOf(0) }
  var pickedAudioClipsCount by remember { mutableIntStateOf(0) }

  var showImageLimitBanner by remember { mutableStateOf(false) }
  var showTestChatLimitedBanner by remember(task.id) { mutableStateOf(false) }
  LaunchedEffect(task.id) {
    if (task.id == BuiltInTaskId.LLM_TEST) {
      val viewed =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
          modelManagerViewModel.dataStoreRepository.hasViewedPromo(TEST_CHAT_LIMITED_NOTE_PROMO_ID)
        }
      showTestChatLimitedBanner = !viewed
    }
  }

  // Turn messages into a derived state to trigger updates when the list is updated.
  val currentMessages by rememberUpdatedState(messages)

  // Stores the height of the viewport in pixels.
  var viewportHeightPx by remember { mutableIntStateOf(0) }

  val lastAgentMessage =
    (currentMessages.lastOrNull() as? ChatMessageText)?.takeIf { it.side == ChatSide.AGENT }
  if (ttsController != null) {
    TtsChatEffects(
      controller = ttsController,
      modelManagerViewModel = modelManagerViewModel,
      selectedModelName = selectedModel.name,
      isResettingSession = uiState.isResettingSession,
      isStreaming = uiState.inProgress,
      lastAgentMessageId = lastAgentMessage?.id,
      lastAgentMessageContent = lastAgentMessage?.content,
      capabilityName = task.id,
    )
  }
  val ttsScrollState =
    rememberChatTtsScrollState(
      ttsController = ttsController,
      listState = listState,
      viewportHeightPx = viewportHeightPx,
    )
  val ttsViewportPositionModifier = ttsScrollState.viewportPositionModifier
  val ttsUserScrollConnection = ttsScrollState.userScrollConnection

  // Stores if the list is at the scrollable area's bottom.
  //
  // It will only be updated when the state holds for at least 500ms to improve user experience.
  var isAtBottom by remember { mutableStateOf(true) }
  LaunchedEffect(listState) {
    snapshotFlow {
        // Read the raw scroll state here
        !listState.canScrollForward
      }
      .collectLatest { rawAtBottom ->
        if (!rawAtBottom) {
          delay(500)
        }
        // Update the actual state.
        isAtBottom = rawAtBottom
      }
  }

  // Stores the index of the last user message as a derived state.
  val lastUserMessageIndex by
    remember(currentMessages) {
      derivedStateOf {
        currentMessages.indexOfLast { it is ChatMessageText && it.side == ChatSide.USER }
      }
    }

  // Scroll gently to keep the latest message visible when a new message is added,
  // without inserting a full-screen bottom spacer that shoves the conversation off-screen.
  LaunchedEffect(currentMessages.size) {
    if (currentMessages.isNotEmpty()) {
      val unused = awaitFrame()
      scrollToBottom(
        listState = listState,
        itemCount = currentMessages.size,
        animate = true,
      )
    }
  }

  val streamingContentLength =
    if (uiState.inProgress) (currentMessages.lastOrNull() as? ChatMessageText)?.content?.length ?: 0
    else 0
  LaunchedEffect(streamingContentLength) {
    if (uiState.inProgress && isAtBottom && currentMessages.isNotEmpty() && streamingContentLength > 0) {
      scrollToBottom(
        listState = listState,
        itemCount = currentMessages.size,
        animate = false,
      )
    }
  }

  // Nested scroll connection to handle scrolling behavior.
  val nestedScrollConnection = remember {
    object : NestedScrollConnection {
      override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // If downward scroll, clear the focus from any currently focused composable.
        // This is useful for dismissing software keyboards or hiding text input fields
        // when the user starts scrolling down a list.
        if (available.y > 0) {
          focusManager.clearFocus()
        }
        // Let LazyColumn handle the scroll
        return Offset.Zero
      }
    }
  }

  // Show the image limit banner for 3 seconds.
  LaunchedEffect(showImageLimitBanner) {
    if (showImageLimitBanner) {
      delay(3000) // 3 seconds
      showImageLimitBanner = false
    }
  }

  // Show the error dialog when the model initialization status is error.
  LaunchedEffect(modelInitStatus) {
    showErrorDialog = modelInitStatus is Model.InitializationStatus.Failed
  }

  // Scroll to bottom when new user prompt is added without shoving prior chat off-screen.

  // Dismiss the one-time discovery pill on any user touch or scroll completion across the screen
  // without consuming the event (so tapping the pill itself still toggles live metrics).
  val dismissPromptModifier =
    if (uiState.showLiveMetricsPrompt && isLiveMetricsToggleEnabled) {
      Modifier.pointerInput(viewModel) {
        awaitEachGesture {
          awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
          do {
            val event = awaitPointerEvent(PointerEventPass.Final)
          } while (event.changes.any { it.pressed })
          viewModel.dismissLiveMetricsPrompt()
        }
      }
    } else {
      Modifier
    }

  Box(
    modifier = modifier.fillMaxSize().then(dismissPromptModifier),
    contentAlignment = Alignment.BottomCenter,
  ) {
    // Audio record animation.
    AnimatedVisibility(
      showAudioRecorder,
      enter =
        slideInVertically(
          animationSpec =
            spring(
              stiffness = Spring.StiffnessLow,
              visibilityThreshold = IntOffset.VisibilityThreshold,
            )
        ) {
          it
        } + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)),
      exit = fadeOut(),
      modifier = Modifier.graphicsLayer { alpha = 0.8f },
    ) {
      AudioAnimation(bgColor = Color.Transparent, amplitude = curAmplitude)
    }

    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding).imePadding()
    ) {
      ChatTtsControlBanner(ttsController = ttsController)
      val asrDictationViewModel = rememberChatAsrDictationViewModel(context)
      val asrDictationUiState =
        if (asrDictationViewModel != null) {
          asrDictationViewModel.uiState.collectAsStateWithLifecycle().value
        } else {
          null
        }

      com.google.ai.edge.gallery.core.design.glass.GlassLayer(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        content = {
          Box(
            contentAlignment = Alignment.BottomCenter,
            modifier =
              Modifier.fillMaxSize()
                .onSizeChanged {
                  // Update the viewport height when the size of the box changes.
                  viewportHeightPx = it.height
                }
                .then(ttsViewportPositionModifier),
          ) {
        val cdChatPanel = stringResource(R.string.cd_chat_panel)
        LazyColumn(
          state = listState,
          modifier =
            Modifier.fillMaxSize()
              .throttleBackdrops { listState.isScrollInProgress }
              .nestedScroll(nestedScrollConnection)
              .nestedScroll(ttsUserScrollConnection)
              .semantics { contentDescription = cdChatPanel },
          verticalArrangement = Arrangement.Top,
        ) {
          itemsIndexed(
            items = messages,
            key = { _, message -> message.id },
            contentType = { _, message -> message.type },
          ) { index, message ->
            val imageHistoryCurIndex = remember { mutableIntStateOf(0) }
            var hAlign: Alignment.Horizontal = Alignment.End
            var backgroundColor: Color = MaterialTheme.customColors.userBubbleBgColor
            var hardCornerAtLeftOrRight = false
            var extraPaddingStart = 48.dp
            var extraPaddingEnd = 0.dp
            if (message.side == ChatSide.AGENT) {
              hAlign = Alignment.Start
              backgroundColor = MaterialTheme.customColors.agentBubbleBgColor
              hardCornerAtLeftOrRight = true
              extraPaddingStart = 0.dp
              if (
                message.type !== ChatMessageType.LOADING &&
                  message.type !== ChatMessageType.WEBVIEW &&
                  message.type !== ChatMessageType.COLLAPSABLE_PROGRESS_PANEL
              ) {
                extraPaddingEnd = 48.dp
              }
              if (message.type == ChatMessageType.TEXT) {
                extraPaddingStart = 0.dp
                extraPaddingEnd = 0.dp
              }
            } else if (message.side == ChatSide.SYSTEM) {
              extraPaddingStart = 24.dp
              extraPaddingEnd = 24.dp
              if (message.type == ChatMessageType.PROMPT_TEMPLATES) {
                extraPaddingStart = 12.dp
                extraPaddingEnd = 12.dp
              } else if (message.type == ChatMessageType.DIVIDER) {
                extraPaddingStart = 0.dp
                extraPaddingEnd = 4.dp
              }
            }
            if (message.type == ChatMessageType.IMAGE) {
              backgroundColor = Color.Transparent
            }
            val bubbleBorderRadius = dimensionResource(R.dimen.chat_bubble_corner_radius)

            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .padding(
                    start = 16.dp + extraPaddingStart,
                    end = 12.dp + extraPaddingEnd,
                    top = 6.dp,
                    bottom = 6.dp,
                  ),
              horizontalAlignment = hAlign,
            ) messageColumn@{
              // Sender row.
              var agentName = stringResource(task.agentNameRes)
              if (message.accelerator.isNotEmpty()) {
                agentName = "$agentName on ${message.accelerator}"
              }
              if (!message.hideSenderLabel) {
                if (message.side == ChatSide.AGENT && ttsController != null) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    MessageSender(
                      message = message,
                      agentName = agentName,
                      imageHistoryCurIndex = imageHistoryCurIndex.intValue,
                    )
                    if (
                      ttsController.isAvailable &&
                        message is ChatMessageText &&
                        message.content.isNotBlank()
                    ) {
                      TtsSpeakerButton(
                        messageId = message.id,
                        messageText = message.content,
                        controller = ttsController,
                        capabilityName = task.id,
                      )
                    }
                  }
                } else {
                  MessageSender(
                    message = message,
                    agentName = agentName,
                    imageHistoryCurIndex = imageHistoryCurIndex.intValue,
                  )
                }
              }

              // Message body.
              when (message) {
                // Loading.
                is ChatMessageLoading -> MessageBodyLoading(message = message)

                // Info.
                is ChatMessageInfo -> MessageBodyInfo(message = message)

                // Warning
                is ChatMessageWarning -> MessageBodyWarning(message = message)

                // Error
                is ChatMessageError -> MessageBodyError(message = message)

                // Config values change.
                is ChatMessageConfigValuesChange -> MessageBodyConfigUpdate(message = message)

                // Divider.
                is ChatMessageDivider -> MessageBodyDivider(message = message)

                // Prompt templates.
                is ChatMessagePromptTemplates ->
                  MessageBodyPromptTemplates(
                    message = message,
                    task = task,
                    onPromptClicked = { template ->
                      onSendMessage(
                        selectedModel,
                        listOf(ChatMessageText(content = template.prompt, side = ChatSide.USER)),
                      )
                    },
                  )

                // Non-system messages.
                else -> {
                  val c = glassColors
                  var messageBubbleModifier: Modifier = Modifier
                  // Open editorial layout for agent response text messages.
                  val isAgentResponseText =
                    message.type == ChatMessageType.TEXT && message.side == ChatSide.AGENT
                  if (!message.disableBubbleShape && !isAgentResponseText) {
                    val userPillShape =
                      if (message is ChatMessageText && !message.content.contains('\n') && message.content.length <= 28) {
                        com.google.ai.edge.gallery.core.design.components.Capsule
                      } else {
                        RoundedRectangle(20.dp)
                      }
                    val bubbleShape =
                      if (message is ChatMessageImage && message.bitmaps.size > 1) {
                        RoundedRectangle(14.dp)
                      } else if (message.side == ChatSide.USER) {
                        userPillShape
                      } else {
                        MessageBubbleShape(
                          radius = 20.dp,
                          hardCornerAtLeftOrRight = hardCornerAtLeftOrRight,
                        )
                      }
                    messageBubbleModifier =
                      if (message.type == ChatMessageType.IMAGE) {
                        messageBubbleModifier.clip(bubbleShape)
                      } else if (message.side == ChatSide.USER) {
                        messageBubbleModifier.wash(
                          shape = bubbleShape,
                          tint = c.capsule.copy(alpha = if (c.dark) 0.44f else 0.56f),
                        )
                      } else {
                        messageBubbleModifier.wash(shape = bubbleShape, tint = c.glass)
                      }
                  }
                  Box(modifier = messageBubbleModifier) {
                    when (message) {
                      // Text
                      is ChatMessageText ->
                        MessageBodyText(
                          message = message,
                          inProgress = uiState.inProgress && index == messages.lastIndex,
                          horizontalPadding =
                            if (isAgentResponseText) {
                              0.dp
                            } else {
                              14.dp
                            },
                          onCopyClicked = copyToClipboard,
                          ttsController = ttsController,
                        )

                      // Image
                      is ChatMessageImage -> {
                        MessageBodyImage(message = message, onImageClicked = onImageSelected)
                      }

                      // Audio clip.
                      is ChatMessageAudioClip -> MessageBodyAudioClip(message = message)

                      // File.
                      is ChatMessageFile ->
                        MessageBodyFile(
                          message = message,
                          onCopyClicked = copyToClipboard,
                          onFileClicked = onFileSelected,
                        )

                      // Benchmark result.
                      is ChatMessageBenchmarkResult -> MessageBodyBenchmark(message = message)

                      // Benchmark LLM result.
                      is ChatMessageBenchmarkLlmResult ->
                        MessageBodyBenchmarkLlm(
                          message = message,
                          modifier = Modifier.wrapContentWidth(),
                        )

                      // Webview.
                      is ChatMessageWebView -> MessageBodyWebview(message = message)

                      // Collapsable progress panel.
                      is ChatMessageCollapsableProgressPanel ->
                        MessageBodyCollapsableProgressPanel(message = message)

                      // Thinking
                      is ChatMessageThinking ->
                        MessageBodyThinking(
                          thinkingText = message.content,
                          inProgress = message.inProgress,
                          onCopyClicked = copyToClipboard,
                        )

                      else -> {}
                    }
                  }

                  if (message.side == ChatSide.AGENT) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      LatencyText(message = message)
                      if (message is ChatMessageText && !uiState.inProgress) {
                        IconButton(
                          onClick = { copyToClipboard(message.content) },
                          modifier = Modifier.size(28.dp),
                        ) {
                          Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = stringResource(R.string.copy),
                            tint = c.inkSoft,
                            modifier = Modifier.size(17.dp),
                          )
                        }
                        if (selectedModel.showRunAgainButton) {
                          val precedingUserMessage =
                            remember(messages, index) {
                              (index - 1 downTo 0).firstNotNullOfOrNull { prevIdx ->
                                messages.getOrNull(prevIdx)?.takeIf { it.side == ChatSide.USER }
                              }
                            }
                          if (precedingUserMessage != null) {
                            IconButton(
                              onClick = { onRunAgainClicked(selectedModel, precedingUserMessage) },
                              modifier = Modifier.size(28.dp),
                            ) {
                              Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(R.string.run_again),
                                tint = c.inkSoft,
                                modifier = Modifier.size(17.dp),
                              )
                            }
                          }
                        }
                        if (
                          ttsController != null &&
                            ttsController.isAvailable &&
                            message.content.isNotBlank()
                        ) {
                          TtsSpeakerButton(
                            messageId = message.id,
                            messageText = message.content,
                            controller = ttsController,
                            capabilityName = task.id,
                          )
                        }
                      }
                    }
                  } else if (message.side == ChatSide.USER) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                      // Run again button.
                      if (selectedModel.showRunAgainButton) {
                        MessageActionButton(
                          label = stringResource(R.string.run_again),
                          icon = Icons.Rounded.Refresh,
                          onClick = { onRunAgainClicked(selectedModel, message) },
                          enabled = !uiState.inProgress,
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          // Clearance above the floating input dock so the last message stays unobstructed.
          item(key = "bottom_clearance") {
            Spacer(modifier = Modifier.height(92.dp).fillMaxWidth())
          }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(bottom = 86.dp, top = 4.dp))

        // Loading screen when model is initialized for that first time.
        val isFirstInitializing =
          modelInitStatus is Model.InitializationStatus.Initializing &&
            modelManagerViewModel.isFirstInitialization(selectedModel)

        // Show empty state immediately when no messages are present.
        if (
          messages.isEmpty() &&
            pickedImagesCount == 0 &&
            pickedAudioClipsCount == 0
        ) {
          emptyStateComposable(selectedModel)
        }

        androidx.compose.animation.AnimatedVisibility(
          visible = isFirstInitializing,
          enter = fadeIn() + slideInVertically { -it / 2 },
          exit = fadeOut() + androidx.compose.animation.slideOutVertically { -it / 2 },
          modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp),
        ) {
          val c = glassColors
          Row(
            modifier = Modifier.clip(com.google.ai.edge.gallery.core.design.components.Capsule)
              .background(Color.Transparent)
              .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            GlassLoader(color = Candy.Sky)
            Text(
              stringResource(R.string.aichat_initializing_title),
              style = GalleryType.Caption.copy(fontWeight = FontWeight.SemiBold),
              color = c.ink,
            )
          }
        }

        FloatingBanner(
          visible = showImageLimitBanner,
          text =
            pluralStringResource(
              R.plurals.model_image_limit_message,
              selectedModel.maxImageCount,
              selectedModel.maxImageCount,
            ),
          modifier =
            Modifier.align(Alignment.TopCenter).padding(horizontal = 16.dp, vertical = 8.dp),
        )

        FloatingBanner(
          visible = showTestChatLimitedBanner,
          text = stringResource(R.string.test_chat_limited_functionality_note),
          actionLabel = stringResource(R.string.dont_show_again),
          onActionClick = {
            showTestChatLimitedBanner = false
            modelManagerViewModel.dataStoreRepository.addViewedPromoId(
              TEST_CHAT_LIMITED_NOTE_PROMO_ID
            )
          },
          modifier =
            Modifier.align(Alignment.TopCenter).padding(horizontal = 16.dp, vertical = 8.dp),
        )

        DictationBottomFade(
          visible = asrDictationUiState?.isDictating == true,
          modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(28.dp),
        )

        // "Scroll to bottom" button, only shown when the list is not at the bottom.
        Column(
          modifier =
            Modifier.align(alignment = Alignment.BottomCenter)
              .fillMaxWidth()
              .padding(bottom = 90.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          ScrollToBottomButton(
            isAtBottom = isAtBottom,
            onClick = { scope.launch { scrollToBottom(listState, currentMessages.size, animate = true) } },
          )
        }
      }
        },
        above = {
          Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
          ) {

      val modelNotSupportImageMsg = stringResource(R.string.model_not_support_image_message)
      val modelNotSupportAudioMsg = stringResource(R.string.model_not_support_audio_message)
      val asrUnavailableMsg = stringResource(R.string.asr_error_unavailable)
      val imageLimitIgnoredMsg = stringResource(R.string.image_limit_ignored_message)
      val contextCompactionFailedMsg = stringResource(R.string.context_compaction_failed_message)

      val contextCompactEnabled = task.isContextCompactSupported(selectedModel)
      val contextCompactionStatus =
        if (contextCompactEnabled) {
          uiState.contextCompactionStatusByModel[selectedModel.name] ?: ContextCompactionStatus.IDLE
        } else {
          ContextCompactionStatus.IDLE
        }
      val isContextLimitReached =
        contextCompactEnabled &&
          (contextCompactionStatus == ContextCompactionStatus.TOKEN_LIMIT_REACHED ||
            contextCompactionStatus == ContextCompactionStatus.COMPACTING)

      if (contextCompactEnabled) {
        LaunchedEffect(task.id) { viewModel.loadAutoCompact(task.id) }
        val isAutoCompactEnabled = uiState.autoCompactByTask[task.id] ?: false

        ContextCompactionBanner(
          status = contextCompactionStatus,
          initialDefaultToBehavior = isAutoCompactEnabled,
          onDefaultToBehaviorChanged = { defaultToBehavior ->
            viewModel.setAutoCompact(
              taskId = task.id,
              enabled = defaultToBehavior,
              model = selectedModel,
              source = AutoCompactionEnableSource.BANNER,
            )
            modelManagerViewModel.updateConfigValuesUpdateTrigger()
          },
          onCompressClicked = { defaultToBehavior ->
            viewModel.setAutoCompact(
              taskId = task.id,
              enabled = defaultToBehavior,
              model = selectedModel,
              source = AutoCompactionEnableSource.BANNER,
            )
            modelManagerViewModel.updateConfigValuesUpdateTrigger()
            viewModel.compactContext(
              model = selectedModel,
              taskId = task.id,
              triggerType = ContextCompactionTriggerType.MANUAL,
              onError = { customErrorMessage = contextCompactionFailedMsg },
            )
          },
          onNewChatClicked = {
            viewModel.setContextCompactionStatus(selectedModel, ContextCompactionStatus.IDLE)
            onNewChatClicked()
          },
          onDismissClicked = {
            viewModel.setContextCompactionStatus(selectedModel, ContextCompactionStatus.IDLE)
          },
        )
      }

      if (hasLiveMetrics) {
        AnimatedVisibility(
          visible = showLiveMetrics,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically(),
        ) {
          LiveMetricsBar(
            liveMetricsFlow = viewModel.liveMetrics,
            modelName = selectedModel.name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            isCompactingContext = contextCompactionStatus == ContextCompactionStatus.COMPACTING,
          )
        }
      }

      val modelManagerUiState by modelManagerViewModel.uiState.collectAsStateWithLifecycle()

      MessageInputText(
        task = task,
        selectedModel = modelManagerUiState.selectedModel,
        curMessage = curMessage,
        inProgress = uiState.inProgress,
        isResettingSession = uiState.isResettingSession,
        modelPreparing = uiState.preparing,
        imageCount = imageCountToLastConfigChange,
        audioClipMessageCount = audioClipMesssageCountToLastconfigChange,
        skillCount = skillCount,
        mcpCount = mcpCount,
        modelInitializing = modelInitStatus is Model.InitializationStatus.Initializing,
        textFieldPlaceHolderRes = task.textInputPlaceHolderRes,
        onValueChanged = { curMessage = it },
        onSendMessage = {
          viewModel.dismissLiveMetricsPrompt()
          ttsController?.stop()
          onSendMessage(selectedModel, it)
          curMessage = ""
          // Hide software keyboard.
          focusManager.clearFocus()
        },
        onOpenPromptTemplatesClicked = {
          ttsController?.stop()
          onSendMessage(
            selectedModel,
            listOf(
              ChatMessagePromptTemplates(
                templates = selectedModel.llmProfile?.promptTemplates ?: emptyList(),
                showMakeYourOwn = false,
              )
            ),
          )
        },
        onStopButtonClicked = onStopButtonClicked,
        onSetAudioRecorderVisible = { start ->
          showAudioRecorder = start
          if (!showAudioRecorder) {
            curAmplitude = 0
          }
        },
        onAmplitudeChanged = { curAmplitude = it },
        onSkillsClicked = onSkillClicked,
        onMcpClicked = onMcpClicked,
        onPickedImagesChanged = { pickedImagesCount = it.size },
        onPickedAudioClipsChanged = { pickedAudioClipsCount = it.size },
        showPromptTemplatesInMenu = false,
        showSkillsPicker = task.id === BuiltInTaskId.LLM_AGENT_CHAT,
        showMcpPicker = task.id === BuiltInTaskId.LLM_AGENT_CHAT,
        showLiveMetricsToggle = hasLiveMetrics,
        isLiveMetricsVisible = showLiveMetrics,
        showLiveMetricsPrompt = uiState.showLiveMetricsPrompt,
        onDismissLiveMetricsPrompt = viewModel::dismissLiveMetricsPrompt,
        onToggleLiveMetrics = {
          viewModel.toggleLiveMetrics(taskId = task.id, model = selectedModel)
        },
        showImagePicker = showImagePicker,
        showAudioPicker = showAudioPicker,
        showTextFilePicker = showTextFilePicker && isTextFileInputEnabled,
        showStopButtonWhenInProgress = showStopButtonInInputWhenInProgress,
        isContextLimitReached = isContextLimitReached,
        onImageLimitExceeded = { showImageLimitBanner = true },
        onImagesIgnored = {
          scope.launch {
            snackbarHostState.showSnackbar(
              message = imageLimitIgnoredMsg,
              duration = SnackbarDuration.Short,
            )
          }
        },
        onModelNotSupportImage = { customErrorMessage = modelNotSupportImageMsg },
        onModelNotSupportAudio = { customErrorMessage = modelNotSupportAudioMsg },
        textInputHistory = modelManagerUiState.textInputHistory,
        onPromoteTextInputHistoryItem = modelManagerViewModel::promoteTextInputHistoryItem,
        onDeleteTextInputHistoryItem = modelManagerViewModel::deleteTextInputHistory,
        onClearTextInputHistory = modelManagerViewModel::clearTextInputHistory,
        asrDictationUiState = asrDictationUiState,
        onStartAsrDictation = asrDictationViewModel?.let { vm -> vm::start },
        onFinishAsrDictation = asrDictationViewModel?.let { vm -> vm::finish },
        onSendAsrDictation = asrDictationViewModel?.let { vm -> vm::finishAndSend },
        onCancelAsrDictation = asrDictationViewModel?.let { vm -> vm::cancel },
        onAsrDictationError = { customErrorMessage = asrUnavailableMsg },
      )
          }
        },
      )
    }
  }

  // Error dialog.
  if (showErrorDialog || customErrorMessage != null) {
    val initErrorMessage =
      (modelInitStatus as? Model.InitializationStatus.Failed)?.error?.message ?: ""
    ErrorDialog(
      error = customErrorMessage ?: initErrorMessage,
      onDismiss = {
        if (customErrorMessage != null) {
          customErrorMessage = null
        } else {
          showErrorDialog = false
        }
      },
    )
  }

  // Benchmark config dialog.
  if (showBenchmarkConfigsDialog) {
    BenchmarkConfigDialog(
      onDismissed = { showBenchmarkConfigsDialog = false },
      messageToBenchmark = benchmarkMessage.value,
      onBenchmarkClicked = { message, warmUpIterations, benchmarkIterations ->
        onBenchmarkClicked(selectedModel, message, warmUpIterations, benchmarkIterations)
      },
    )
  }
}

@Composable
private fun DictationBottomFade(visible: Boolean, modifier: Modifier = Modifier) {
  AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
    val surfaceColor = glassColors.desk
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(Brush.verticalGradient(colors = listOf(Color.Transparent, surfaceColor)))
    )
  }
}

private suspend fun scrollToBottom(
  listState: LazyListState,
  itemCount: Int,
  animate: Boolean = false,
) {
  if (itemCount <= 0) return
  if (animate) {
    listState.animateScrollToItem(itemCount)
  } else {
    listState.scrollToItem(itemCount)
  }
}
