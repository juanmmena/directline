package com.directline

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.directline.core.designsystem.DirectlineTheme
import com.directline.core.navigation.DirectlineApp
import com.directline.domain.model.UserPreferences
import com.directline.domain.repository.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject
  lateinit var preferencesRepository: PreferencesRepository

  private val incomingSharedText = MutableStateFlow<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    extractSharedText(intent)
    enableEdgeToEdge()

    setContent {
      val prefs by preferencesRepository.observePreferences()
        .collectAsStateWithLifecycle(initialValue = UserPreferences())
      val sharedText by incomingSharedText.collectAsStateWithLifecycle()

      DirectlineTheme(themeMode = prefs.themeMode) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background,
        ) {
          DirectlineApp(
            incomingSharedText = sharedText,
            onConsumeSharedText = { incomingSharedText.value = null },
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    extractSharedText(intent)
  }

  private fun extractSharedText(intent: Intent?) {
    if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
      val shared = intent.getStringExtra(Intent.EXTRA_TEXT)
      if (!shared.isNullOrBlank()) {
        incomingSharedText.value = shared
      }
    }
  }
}
