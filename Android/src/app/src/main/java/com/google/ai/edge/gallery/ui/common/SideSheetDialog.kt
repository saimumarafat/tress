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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.ai.edge.gallery.core.design.components.RoundedRectangle
import com.google.ai.edge.gallery.core.design.theme.glassColors

/**
 * Displays a full-height, right-anchored slide-in dialog sheet with a dimmed scrim background.
 *
 * Used by both [ConfigDialog] and custom task side sheets (such as AR Language Teacher's Session
 * history sheet) so they share identical geometry, elevation, insets, and entrance/exit animations.
 */
@Composable
fun SideSheetDialog(
  onDismissed: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable (onDismiss: () -> Unit) -> Unit,
) {
  val visibleState = remember { MutableTransitionState(false) }
  val requestDismiss: () -> Unit = { visibleState.targetState = false }
  val currentOnDismissed by rememberUpdatedState(onDismissed)

  LaunchedEffect(Unit) { visibleState.targetState = true }

  LaunchedEffect(visibleState.isIdle, visibleState.currentState, visibleState.targetState) {
    if (visibleState.isIdle && !visibleState.currentState && !visibleState.targetState) {
      currentOnDismissed()
    }
  }

  Dialog(
    onDismissRequest = requestDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
  ) {
    val focusManager = LocalFocusManager.current
    val c = glassColors

    Box(modifier = Modifier.fillMaxSize()) {
      // 1. Scrim background: tap outside to dismiss.
      AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(150)),
        modifier = Modifier.fillMaxSize(),
      ) {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(Color.Black.copy(alpha = if (c.dark) 0.45f else 0.25f))
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
              ) {
                requestDismiss()
              }
        )
      }

      // 2. Right-anchored sliding glass sheet.
      AnimatedVisibility(
        visibleState = visibleState,
        enter =
          slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300, easing = FastOutSlowInEasing),
          ) + fadeIn(tween(200)),
        exit =
          slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(250, easing = FastOutSlowInEasing),
          ) + fadeOut(tween(150)),
        modifier = Modifier.align(Alignment.CenterEnd),
      ) {
        val sheetShape = RoundedRectangle(28.dp)
        Box(
          modifier =
            modifier
              .statusBarsPadding()
              .navigationBarsPadding()
              .imePadding()
              .padding(vertical = 8.dp, horizontal = 8.dp)
              .fillMaxHeight()
              .fillMaxWidth(0.88f)
              .widthIn(max = 420.dp)
              .clip(sheetShape)
              .background(c.frost.copy(alpha = if (c.dark) 0.92f else 0.94f))
              .border(1.dp, c.rim, sheetShape)
              .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
        ) {
          content(requestDismiss)
        }
      }
    }
  }
}
