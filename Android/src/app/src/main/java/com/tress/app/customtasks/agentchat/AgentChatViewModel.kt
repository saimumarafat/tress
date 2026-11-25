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

package com.tress.app.customtasks.agentchat

import com.tress.app.agent.AgentChatExecutor
import com.tress.app.agent.AgentRuntimeExecutor
import com.tress.app.agent.sessions.LlmSessionManager
import com.tress.app.data.DataStoreRepository
import com.tress.app.data.SystemPromptRepository
import com.tress.app.ui.llmchat.LlmChatViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AgentChatViewModel
@Inject
constructor(
  systemPromptRepository: SystemPromptRepository,
  @AgentChatExecutor runtimeExecutor: AgentRuntimeExecutor,
  llmSessionManager: LlmSessionManager,
  dataStoreRepository: DataStoreRepository? = null,
) :
LlmChatViewModel(systemPromptRepository, runtimeExecutor,
llmSessionManager, dataStoreRepository)
