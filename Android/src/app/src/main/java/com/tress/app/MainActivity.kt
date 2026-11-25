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

package com.tress.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tress.app.common.PermissionsResultListener
import com.tress.app.common.VirtualDevicePermissionsHost
import com.tress.app.core.design.glass.GlassSettings
import com.tress.app.ui.common.MobileDataDownloadConfirmationHost
import com.tress.app.ui.modelmanager.ModelManagerViewModel
import com.tress.app.ui.theme.GalleryTheme
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.kyant.backdrop.GlassShaderWarmup
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.CopyOnWriteArraySet

@AndroidEntryPoint
class MainActivity : ComponentActivity(), VirtualDevicePermissionsHost {

  private val modelManagerViewModel: ModelManagerViewModel by viewModels()
  private val permissionsResultListeners = CopyOnWriteArraySet<PermissionsResultListener>()

  override fun registerPermissionsResultListener(listener: PermissionsResultListener) {
    permissionsResultListeners.add(listener)
  }

  override fun unregisterPermissionsResultListener(listener: PermissionsResultListener) {
    permissionsResultListeners.remove(listener)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen()
    super.onCreate(null)

    modelManagerViewModel.loadModelAllowlist()

    splashScreen.setOnExitAnimationListener { splashScreenView ->
      splashScreenView.remove()
    }

    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isNavigationBarContrastEnforced = false
    }
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    setContent {
      GalleryTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          Box(modifier = Modifier.fillMaxSize()) {
            GalleryApp(modelManagerViewModel = modelManagerViewModel)
            MobileDataDownloadConfirmationHost(modelManagerViewModel = modelManagerViewModel)
          }
        }
      }
    }

    @OptIn(ExperimentalApi::class)
    ExperimentalFlags.enableBenchmark = false

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !GlassSettings.isLiteTier) {
      Looper.myQueue().addIdleHandler { GlassShaderWarmup.next() }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray,
    deviceId: Int,
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
    for (listener in permissionsResultListeners) {
      listener.onPermissionsResult(requestCode, permissions, grantResults, deviceId)
    }
  }
}
