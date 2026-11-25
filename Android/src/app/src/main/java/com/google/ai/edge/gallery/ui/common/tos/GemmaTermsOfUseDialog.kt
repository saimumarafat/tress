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

package com.google.ai.edge.gallery.ui.common.tos

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.core.design.components.GlassDialog
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType
import com.google.ai.edge.gallery.ui.common.buildTrackableUrlAnnotatedString

/** A composable for Gemma Terms of Use dialog, shown once before a Gemma model is downloaded. */
@Composable
fun GemmaTermsOfUseDialog(
  onTosAccepted: () -> Unit,
  onCancel: () -> Unit = {},
  viewingMode: Boolean = false,
) {
  val palette = glassColors
  GlassDialog(
    onDismiss = onCancel,
    title = stringResource(R.string.tos_dialog_title_gemma),
    confirm =
      stringResource(
        if (viewingMode) R.string.close else R.string.tos_dialog_agree_and_continue_button_label
      ),
    onConfirm = onTosAccepted,
    dismiss = if (!viewingMode) stringResource(R.string.cancel) else null,
    accent = Candy.Sky,
  ) {
    Text(
      buildAnnotatedString {
        append("Gemma models in Tress are governed by the ")
        append(
          buildTrackableUrlAnnotatedString(
            url = "https://ai.google.dev/gemma/terms",
            linkText = "Gemma Terms of Service",
          )
        )
        append(". Please review these terms and ensure you agree before continuing.")
      },
      style = GalleryType.Body,
      color = palette.inkSoft,
      modifier = Modifier.padding(top = 4.dp),
    )
  }
}
