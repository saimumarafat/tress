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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tress.app.R
import com.tress.app.core.design.components.GlassCircle
import com.tress.app.core.design.components.GlassIcon
import com.tress.app.core.design.motion.Fluid
import com.tress.app.core.design.motion.GlassMotion

/**
 * Floating liquid-glass button animated to show when the conversation list is scrolled away from
 * the bottom.
 */
@Composable
fun ScrollToBottomButton(isAtBottom: Boolean, onClick: () -> Unit) {
  AnimatedVisibility(
    visible = !isAtBottom,
    enter = fadeIn(animationSpec = Fluid.fade()) + scaleIn(animationSpec = GlassMotion.morph()),
    exit = fadeOut() + scaleOut(),
  ) {
    GlassCircle(
      onClick = onClick,
      size = 42.dp,
      label = stringResource(R.string.cd_scroll_to_bottom),
    ) {
      GlassIcon(
        icon = Icons.Outlined.ArrowDownward,
        contentDescription = stringResource(R.string.cd_scroll_to_bottom),
        modifier = Modifier.size(18.dp),
      )
    }
  }
}
