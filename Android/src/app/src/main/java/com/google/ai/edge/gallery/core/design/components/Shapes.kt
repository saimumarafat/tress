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
 * Shape tokens adapted from Artefact
 * (https://github.com/Chethan616/Artefact, MIT OR Apache-2.0, Copyright 2026 Chethan616).
 */

package com.google.ai.edge.gallery.core.design.components

import androidx.compose.ui.unit.Dp
import com.kyant.shapes.RoundedCornerStyle

val Capsule = com.kyant.shapes.Capsule(RoundedCornerStyle.Circular)
val ChromeCapsule = com.kyant.shapes.Capsule

fun RoundedRectangle(radius: Dp) = com.kyant.shapes.RoundedRectangle(radius, RoundedCornerStyle.Circular)

fun Squircle(radius: Dp) = com.kyant.shapes.RoundedRectangle(radius, RoundedCornerStyle.Continuous)
