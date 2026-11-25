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

package com.tress.app.ui.common.modelitem

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tress.app.R
import com.tress.app.core.design.components.GlassDialog
import com.tress.app.core.design.theme.glassColors
import com.tress.app.core.design.typography.GalleryType
import com.tress.app.data.Model

/** Composable function to display a confirmation dialog for deleting a model. */
@Composable
fun ConfirmDeleteModelDialog(model: Model, onConfirm: () -> Unit, onDismiss: () -> Unit) {
  val palette = glassColors
  GlassDialog(
    onDismiss = onDismiss,
    title = stringResource(R.string.confirm_delete_model_dialog_title),
    confirm = stringResource(R.string.ok),
    onConfirm = onConfirm,
    dismiss = stringResource(R.string.cancel),
    destructive = true,
  ) {
    Text(
      text =
        stringResource(R.string.confirm_delete_model_dialog_content)
          .format(model.displayName.ifEmpty { model.name }),
      style = GalleryType.Body,
      color = palette.inkSoft,
    )
  }
}
