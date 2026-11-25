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

package com.google.ai.edge.gallery.ui.common.tos

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.core.design.components.GlassDialog
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.ui.common.MarkdownText

/** A composable for Terms of Service dialog, shown once when app is launched. */
@Composable
fun AppTosDialog(onTosAccepted: () -> Unit, viewingMode: Boolean = false) {
  GlassDialog(
    onDismiss = { if (viewingMode) onTosAccepted() },
    title = stringResource(R.string.tos_dialog_title_app),
    confirm =
      stringResource(
        if (viewingMode) R.string.close
        else R.string.tos_dialog_accept_and_continue_button_label
      ),
    onConfirm = onTosAccepted,
    icon = Icons.Rounded.Gavel,
    accent = Candy.Sky,
    dismissible = viewingMode,
  ) {
    MarkdownText(
      stringResource(R.string.tos_dialog_body),
      smallFontSize = true,
      textColor = glassColors.inkSoft,
    )
  }
}
