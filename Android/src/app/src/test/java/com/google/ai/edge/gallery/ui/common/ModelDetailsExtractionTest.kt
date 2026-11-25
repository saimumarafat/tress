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

import com.google.ai.edge.gallery.data.Accelerator
import com.google.ai.edge.gallery.data.BackendSpec
import com.google.ai.edge.gallery.data.LlmProfile
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelDownloadInfo
import com.google.ai.edge.gallery.data.RuntimeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelDetailsExtractionTest {

  @Test
  fun extractsRealMetadataFromLiteRtModelWithoutFabrication() {
    val model =
      Model(
        name = "gemma-3n-E4B-it-int4",
        displayName = "Gemma 3n E4B IT",
        modelId = "google/gemma-3n-E4B-it-litert-lm",
        minDeviceMemoryInGb = 6,
        supportImage = true,
        supportAudio = true,
        downloadInfo =
          ModelDownloadInfo(
            url = "https://huggingface.co/google/gemma-3n-E4B-it-litert-lm/resolve/main/model_q4.litertlm",
            sizeInBytes = 3_145_728_000L,
            downloadFileName = "model_q4.litertlm",
          ),
        backendSpec =
          BackendSpec(
            runtimeType = RuntimeType.LITERT_LM,
            accelerators = listOf(Accelerator.GPU, Accelerator.CPU),
          ),
        llmProfile = LlmProfile(maxTokens = 4096),
      )

    val details = extractRealModelDetails(model)
    assertEquals("Gemma 3n E4B IT", details.displayName)
    assertEquals("google", details.author)
    assertEquals("google/gemma-3n-E4B-it-litert-lm", details.repository)
    assertEquals("Gemma 3n", details.architecture)
    assertEquals("INT4", details.quantization)
    assertEquals("LiteRT-LM", details.runtimeLabel)
    assertEquals(6, details.minMemoryGb)
    assertEquals(listOf("Text", "Vision / Image", "Audio"), details.modalities)
    assertTrue(details.hardwareAccelerators.contains(Accelerator.GPU.label))
    assertTrue(details.hardwareAccelerators.contains(Accelerator.CPU.label))
  }

  @Test
  fun leavesUnknownSpecsNullInsteadOfInventingValues() {
    val bareModel = Model(name = "custom-local-model")
    val details = extractRealModelDetails(bareModel)
    assertEquals("custom-local-model", details.displayName)
    assertNull(details.author)
    assertNull(details.repository)
    assertNull(details.architecture)
    assertNull(details.parameterCount)
    assertNull(details.quantization)
    assertEquals("Not specified", details.sizeLabel)
    assertEquals(listOf("Text"), details.modalities)
  }
}
