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
/*
 * Design typography adapted from Artefact (https://github.com/Chethan616/Artefact),
 * using Plus Jakarta Sans (SIL Open Font License 1.1).
 */

package com.google.ai.edge.gallery.core.design.typography

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.google.ai.edge.gallery.R

@OptIn(ExperimentalTextApi::class)
fun jakarta(weight: Int, italic: Boolean = false): FontFamily = FontFamily(
  Font(
    resId = if (italic) R.font.jakarta_italic else R.font.jakarta,
    weight = FontWeight(weight),
    style = if (italic) FontStyle.Italic else FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
  ),
)

@OptIn(ExperimentalTextApi::class)
val PlusJakartaSans: FontFamily = FontFamily(
  listOf(300, 400, 500, 600, 700, 800).flatMap { w ->
    listOf(
      Font(
        resId = R.font.jakarta,
        weight = FontWeight(w),
        style = FontStyle.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(w)),
      ),
      Font(
        resId = R.font.jakarta_italic,
        weight = FontWeight(w),
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(w)),
      ),
    )
  },
)

private val Trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

private fun displayStyle(weight: Int, size: TextUnit, line: TextUnit, tracking: TextUnit) =
  TextStyle(
    fontFamily = jakarta(weight),
    fontWeight = FontWeight(weight),
    fontSize = size,
    lineHeight = line,
    letterSpacing = tracking,
    lineHeightStyle = Trim,
  )

/** Centralized typographic scale rooted in Plus Jakarta Sans. */
object GalleryType {
  val Hero = displayStyle(700, 42.sp, 46.sp, (-1.1).sp)
  val Display = displayStyle(700, 34.sp, 38.sp, (-0.8).sp)
  val Headline = displayStyle(700, 24.sp, 29.sp, (-0.4).sp)
  val Title = displayStyle(700, 18.sp, 22.sp, (-0.2).sp)
  val Tile = displayStyle(600, 16.sp, 20.sp, (-0.15).sp)
  val CardTitle = displayStyle(700, 20.sp, 24.sp, (-0.3).sp)
  val Number = displayStyle(700, 22.sp, 24.sp, 0.sp).copy(fontFeatureSettings = "tnum")
  val Eyebrow = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.5.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.2.sp,
  )
  val Body = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,
  )
  val BodyStrong = Body.copy(fontWeight = FontWeight.SemiBold)
  val Label = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp,
  )
  val Caption = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Normal,
    fontSize = 11.5.sp,
    lineHeight = 15.sp,
  )
  val Code = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 19.sp,
  )
}

/** Material 3 Typography backed by Plus Jakarta Sans. */
val GalleryMaterialTypography = Typography(
  displayLarge = GalleryType.Hero,
  displayMedium = GalleryType.Display,
  displaySmall = displayStyle(600, 30.sp, 35.sp, (-0.5).sp),
  headlineLarge = displayStyle(700, 28.sp, 33.sp, (-0.5).sp),
  headlineMedium = GalleryType.Headline,
  headlineSmall = displayStyle(600, 21.sp, 26.sp, (-0.25).sp),
  titleLarge = displayStyle(700, 20.sp, 25.sp, (-0.2).sp),
  titleMedium = GalleryType.Title,
  titleSmall = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 19.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 23.sp,
  ),
  bodyMedium = GalleryType.Body,
  bodySmall = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
  ),
  labelMedium = GalleryType.Label,
  labelSmall = GalleryType.Caption,
)
