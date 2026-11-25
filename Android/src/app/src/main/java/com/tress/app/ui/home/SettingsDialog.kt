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

package com.tress.app.ui.home

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tress.app.BuildConfig
import com.tress.app.R
import com.tress.app.core.design.components.GlassChip
import com.tress.app.core.design.components.GlassPill
import com.tress.app.core.design.components.GlassRow
import com.tress.app.core.design.components.GlassSheet
import com.tress.app.core.design.components.GlassTab
import com.tress.app.core.design.components.GlassTabs
import com.tress.app.core.design.components.GlassTextField
import com.tress.app.core.design.components.RoundedRectangle
import com.tress.app.core.design.components.WashPanel
import com.tress.app.core.design.glass.GlassSettings
import com.tress.app.core.design.motion.MotionSettings
import com.tress.app.core.design.theme.Candy
import com.tress.app.core.design.theme.glassColors
import com.tress.app.core.design.typography.GalleryType
import com.tress.app.proto.Theme
import com.tress.app.services.asr.AsrEngineType
import com.tress.app.ui.common.humanReadableSize
import com.tress.app.ui.common.tos.AppTosDialog
import com.tress.app.ui.modelmanager.ModelManagerViewModel
import com.tress.app.ui.modelmanager.checkAsrEngineSupport
import com.tress.app.ui.modelmanager.saveAsrEngineType
import com.tress.app.ui.theme.ThemeSettings
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

private val THEME_OPTIONS = listOf(Theme.THEME_AUTO, Theme.THEME_LIGHT, Theme.THEME_DARK)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
  curThemeOverride: Theme,
  @Suppress("UNUSED_PARAMETER") curFirebaseAnalytics: Boolean = false,
  modelManagerViewModel: ModelManagerViewModel,
  onDismissed: () -> Unit,
) {
  val c = glassColors
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current
  var selectedTheme by remember { mutableStateOf(curThemeOverride) }
  var hfToken by remember { mutableStateOf(modelManagerViewModel.getTokenStatusAndData().data) }
  val dateFormatter = remember {
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      .withZone(ZoneId.systemDefault())
      .withLocale(Locale.getDefault())
  }
  var customHfToken by remember { mutableStateOf("") }
  val focusRequester = remember { FocusRequester() }
  val interactionSource = remember { MutableInteractionSource() }
  var showTos by remember { mutableStateOf(false) }
  var showAppearanceSheet by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) { modelManagerViewModel.checkAsrEngineSupport() }

  val applyTheme: (Theme) -> Unit = { theme ->
    selectedTheme = theme
    ThemeSettings.themeOverride.value = theme
    modelManagerViewModel.saveThemeOverride(theme)
    val uiModeManager =
      context.applicationContext.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    when (theme) {
      Theme.THEME_LIGHT -> uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
      Theme.THEME_DARK -> uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
      Theme.THEME_AUTO,
      Theme.THEME_UNSPECIFIED,
      Theme.UNRECOGNIZED -> uiModeManager.setApplicationNightMode(UiModeManager.MODE_NIGHT_AUTO)
    }
  }

  val downloadedModels = remember { modelManagerViewModel.getAllDownloadedModels() }
  val downloadedStorageBytes =
    remember(downloadedModels) {
      downloadedModels.sumOf { it.downloadInfo.sizeInBytes.coerceAtLeast(0L) }
    }

  GlassSheet(
    onDismiss = onDismissed,
    title = stringResource(R.string.drawer_settings_label),
    subtitle = "Tress v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(max = 560.dp)
          .verticalScroll(rememberScrollState())
          .clickable(interactionSource = interactionSource, indication = null) {
            focusManager.clearFocus()
          }
          .padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Appearance & Liquid Glass Section
      WashPanel(shape = RoundedRectangle(20.dp), tint = c.glass, modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(stringResource(R.string.theme_title), style = GalleryType.BodyStrong, color = c.ink)
          GlassTabs(
            tabs = THEME_OPTIONS.map { GlassTab(label = stringResource(themeLabelRes(it))) },
            selected = THEME_OPTIONS.indexOf(selectedTheme).coerceAtLeast(0),
            onSelect = { idx -> applyTheme(THEME_OPTIONS[idx]) },
            height = 44.dp,
            modifier = Modifier.fillMaxWidth(),
          )
          Text("Background Color", style = GalleryType.BodyStrong, color = c.ink)
          BackgroundColorSwatchRow()
          GlassRow(
            label = "Glass, Motion & Haptics",
            icon = Icons.Rounded.AutoAwesome,
            accent = Candy.Sky,
            value =
              buildString {
                append(GlassSettings.performanceTier.name.lowercase().replaceFirstChar(Char::uppercase))
                if (MotionSettings.reduceMotion) append(" · Reduced")
              },
            trailing = {
              Icon(Icons.Rounded.ChevronRight, null, tint = c.inkSoft, modifier = Modifier.size(18.dp))
            },
            onClick = { showAppearanceSheet = true },
          )
        }
      }

      // Storage Section
      WashPanel(shape = RoundedRectangle(20.dp), tint = c.glass, modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(Icons.Rounded.Storage, null, tint = Candy.Sky, modifier = Modifier.size(20.dp))
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Model Storage", style = GalleryType.BodyStrong, color = c.ink)
            Text(
              "${downloadedModels.size} models stored locally · ${downloadedStorageBytes.humanReadableSize()}",
              style = GalleryType.Caption,
              color = c.inkSoft,
            )
          }
        }
      }

      // Audio & Speech Recognition Section
      val selectedAsrEngine by modelManagerViewModel.selectedAsrEngineType.collectAsStateWithLifecycle()
      val supportedAsrEngines by modelManagerViewModel.supportedAsrEngines.collectAsStateWithLifecycle()
      WashPanel(shape = RoundedRectangle(20.dp), tint = c.glass, modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(14.dp).semantics(mergeDescendants = true) {},
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(stringResource(R.string.asr_engine_title), style = GalleryType.BodyStrong, color = c.ink)
          Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for (engineType in AsrEngineType.entries) {
              val isSupported = engineType in supportedAsrEngines
              AsrEngineOptionRow(
                title = stringResource(asrEngineLabelResId(engineType)),
                subtitle = if (!isSupported) stringResource(R.string.model_unavailable_status) else null,
                selected = selectedAsrEngine == engineType,
                enabled = isSupported,
                onClick = { if (isSupported) modelManagerViewModel.saveAsrEngineType(engineType) },
              )
            }
          }
        }
      }

      // HuggingFace Access Token
      WashPanel(shape = RoundedRectangle(20.dp), tint = c.glass, modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("HuggingFace access token", style = GalleryType.BodyStrong, color = c.ink)
          val curHfToken = hfToken
          if (curHfToken != null && curHfToken.accessToken.isNotEmpty()) {
            Text(
              curHfToken.accessToken.substring(0, min(16, curHfToken.accessToken.length)) + "...",
              style = GalleryType.Caption,
              color = c.inkSoft,
            )
            Text(
              "Expires at: ${dateFormatter.format(Instant.ofEpochMilli(curHfToken.expiresAtMs))}",
              style = GalleryType.Caption,
              color = c.inkSoft,
            )
          } else {
            Text(
              "Not available · automatically retrieved for gated downloads",
              style = GalleryType.Caption,
              color = c.inkSoft,
            )
          }
          val handleSaveToken = {
            modelManagerViewModel.saveAccessToken(
              accessToken = customHfToken,
              refreshToken = "",
              expiresAt = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 10,
            )
            hfToken = modelManagerViewModel.getTokenStatusAndData().data
            focusManager.clearFocus()
          }
          GlassTextField(
            value = customHfToken,
            onValueChange = { customHfToken = it },
            placeholder = "Enter token manually",
            focus = focusRequester,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { handleSaveToken() }),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (curHfToken != null) {
              GlassChip(
                onClick = {
                  modelManagerViewModel.clearAccessToken()
                  hfToken = null
                },
                ink = Candy.Tomato,
              ) {
                Text("Clear", style = GalleryType.Caption, color = Candy.Tomato)
              }
            }
            if (customHfToken.isNotBlank()) {
              GlassChip(
                onClick = handleSaveToken,
                ink = Candy.Sky,
              ) {
                Text("Save token", style = GalleryType.Caption, color = Candy.Sky)
              }
            }
          }
        }
      }

      // Open Source Licenses & Local Notice
      WashPanel(shape = RoundedRectangle(20.dp), tint = c.glass, modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(stringResource(R.string.third_party_libraries), style = GalleryType.BodyStrong, color = c.ink)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassChip(
              onClick = {
                val intent = Intent(context, OssLicensesMenuActivity::class.java)
                context.startActivity(intent)
              },
            ) {
              Text(stringResource(R.string.view_licenses), style = GalleryType.Caption, color = c.ink)
            }
            GlassChip(onClick = { showTos = true }) {
              Text(
                stringResource(R.string.settings_dialog_view_app_terms_of_service),
                style = GalleryType.Caption,
                color = c.ink,
              )
            }
          }
        }
      }

      Spacer(Modifier.height(4.dp))
      GlassPill(
        onClick = onDismissed,
        tint = Candy.Sky,
        height = 46.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(R.string.close), style = GalleryType.BodyStrong, color = Color.White)
      }
      Spacer(Modifier.height(8.dp))
    }
  }

  if (showTos) {
    AppTosDialog(onTosAccepted = { showTos = false }, viewingMode = true)
  }

  if (showAppearanceSheet) {
    AppearanceSettingsSheet(
      selectedTheme = selectedTheme,
      onSelectTheme = applyTheme,
      onDismiss = { showAppearanceSheet = false },
    )
  }
}

@StringRes
private fun themeLabelRes(theme: Theme): Int =
  when (theme) {
    Theme.THEME_AUTO -> R.string.theme_auto
    Theme.THEME_LIGHT -> R.string.theme_light
    Theme.THEME_DARK -> R.string.theme_dark
    else -> R.string.unknown
  }

@StringRes
private fun asrEngineLabelResId(engineType: AsrEngineType): Int =
  when (engineType) {
    AsrEngineType.SYSTEM_DEFAULT -> R.string.asr_engine_system_default
    AsrEngineType.MLKIT_BASIC -> R.string.asr_engine_mlkit_basic
    AsrEngineType.MLKIT_ADVANCED -> R.string.asr_engine_mlkit_advanced
    AsrEngineType.SPEECH_PROCESSOR -> R.string.asr_engine_speech_processor
  }

@Composable
private fun AsrEngineOptionRow(
  title: String,
  subtitle: String?,
  selected: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  val c = glassColors
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .selectable(
          selected = selected,
          enabled = enabled,
          role = Role.RadioButton,
          onClick = onClick,
        )
        .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = GalleryType.Body,
        color = if (enabled) c.ink else c.inkFaint,
      )
      if (subtitle != null) {
        Text(
          text = subtitle,
          style = GalleryType.Caption,
          color = Candy.Tomato,
        )
      }
    }
    RadioButton(selected = selected, onClick = null, enabled = enabled)
  }
}
