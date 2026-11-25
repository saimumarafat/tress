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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.core.design.components.GlassPill
import com.google.ai.edge.gallery.core.design.components.GlassSheet
import com.google.ai.edge.gallery.core.design.theme.Candy
import com.google.ai.edge.gallery.core.design.theme.glassColors
import com.google.ai.edge.gallery.core.design.typography.GalleryType

@Composable
fun MobileActionsChallengeDialog(
  onDismiss: () -> Unit,
  onLoadModel: () -> Unit,
  onSendEmail: () -> Unit,
) {
  val palette = glassColors
  GlassSheet(onDismiss = onDismiss) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
      Text(text = "🏆", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
      Text(
        modifier = Modifier.fillMaxWidth(),
        text =
          stringResource(
            R.string.mobile_actions_challenge_title,
            stringResource(R.string.mobile_actions_title),
          ),
        style = GalleryType.Headline.copy(fontSize = 18.sp),
        color = palette.ink,
        textAlign = TextAlign.Center,
      )
      Text(
        text = stringResource(R.string.mobile_actions_challenge_subtitle),
        style = GalleryType.Caption,
        color = palette.inkFaint,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text =
          stringResource(
            R.string.mobile_actions_challenge_description,
            stringResource(R.string.mobile_actions_title),
          ),
        style = GalleryType.Body,
        color = palette.inkSoft,
      )
      Spacer(modifier = Modifier.height(20.dp))
      Text(
        text = stringResource(R.string.mobile_actions_challenge_instructions_title),
        style = GalleryType.BodyStrong,
        color = palette.ink,
      )
      val instructions = buildAnnotatedString {
        append(
          AnnotatedString.fromHtml(stringResource(R.string.mobile_actions_challenge_instruction_1))
        )
        append("\n")
        append(
          AnnotatedString.fromHtml(stringResource(R.string.mobile_actions_challenge_instruction_2))
        )
        append("\n")
        append(
          AnnotatedString.fromHtml(stringResource(R.string.mobile_actions_challenge_instruction_3))
        )
        append("\n")
        append(
          AnnotatedString.fromHtml(stringResource(R.string.mobile_actions_challenge_instruction_4))
        )
      }
      Text(
        text = instructions,
        color = palette.inkSoft,
        style = GalleryType.Body,
      )
      Spacer(modifier = Modifier.height(18.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        GlassPill(
          onClick = onSendEmail,
          modifier = Modifier.weight(1f),
          surface = palette.glass,
        ) {
          Text(
            stringResource(R.string.mobile_actions_challenge_email_colab),
            style = GalleryType.BodyStrong,
            color = palette.ink,
          )
        }
        GlassPill(
          onClick = onLoadModel,
          modifier = Modifier.weight(1f),
          tint = Candy.Sky,
        ) {
          Text(
            stringResource(R.string.mobile_actions_challenge_load_model),
            style = GalleryType.BodyStrong,
          )
        }
      }
    }
  }
}
