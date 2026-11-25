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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate

private const val FADE_INTERVAL_MS = 85

/**
 * A Composable that displays Markdown text with a smooth fading transition while actively
 * streaming ([inProgress] is true), and renders a single lightweight [MarkdownText] once
 * complete so idle messages and scrolling use zero offscreen compositing layers.
 */
@Composable
fun BufferedFadingMarkdownText(text: String, inProgress: Boolean, modifier: Modifier = Modifier) {
  if (!inProgress) {
    SelectionContainer(modifier = modifier) {
      MarkdownText(text = text)
    }
    return
  }

  var text1 by remember { mutableStateOf(text) }
  var text2 by remember { mutableStateOf("") }
  val alpha2 = remember { Animatable(0f) }
  val currentText by rememberUpdatedState(text)

  LaunchedEffect(Unit) {
    snapshotFlow { currentText }
      .conflate()
      .collect { newText ->
        if (newText == text1) return@collect

        // Set the new text onto the hidden overlay layer
        text2 = newText
        alpha2.snapTo(0f)

        // Smoothly fade the new layout in.
        // Text1 remains 100% solid, unchanged, and stationary underneath.
        alpha2.animateTo(
          1f,
          animationSpec = tween(FADE_INTERVAL_MS, easing = LinearOutSlowInEasing),
        )

        // Swap the background text to match the new text instantly.
        text1 = newText

        // Wait a frame for the base layer to lay out and draw.
        val unused = awaitFrame()

        // Instantly hide the overlay so we don't double-render the text.
        alpha2.snapTo(0f)
      }
  }

  Box(modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
    SelectionContainer {
      MarkdownText(text = text1, modifier = Modifier.graphicsLayer { alpha = 1f - alpha2.value })
    }

    if (alpha2.value > 0.001f) {
      MarkdownText(
        text = text2,
        modifier =
          Modifier.clearAndSetSemantics {}
            .graphicsLayer {
              alpha = alpha2.value
              this.blendMode = BlendMode.Plus
            },
      )
    }
  }
}
