package com.directline.feature.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.directline.core.designsystem.CountryPickerDialog
import com.directline.core.designsystem.DirectlineSpacing
import com.directline.core.designsystem.ReadableWidthContainer
import com.directline.domain.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onShowSnackbar: (String) -> Unit,
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  var showCountryPicker by remember { mutableStateOf(false) }
  var showDeleteAllConfirm by remember { mutableStateOf(false) }
  var defaultMessageDraft by remember { mutableStateOf(state.preferences.defaultMessagePrefix) }

  LaunchedEffect(state.preferences.defaultMessagePrefix) {
    defaultMessageDraft = state.preferences.defaultMessagePrefix
  }

  LaunchedEffect(state.feedbackMessage) {
    state.feedbackMessage?.let { msg ->
      onShowSnackbar(msg)
      viewModel.dismissFeedback()
    }
  }

  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = { Text("Settings") },
        windowInsets = WindowInsets(0, 0, 0, 0),
      )
    },
  ) { innerPadding ->
    ReadableWidthContainer(modifier = Modifier.padding(innerPadding)) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .imePadding(),
        contentPadding = PaddingValues(DirectlineSpacing.md),
        verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.md),
      ) {
        item(key = "appearance_card") {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(DirectlineSpacing.md),
              verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm),
            ) {
              Text("Appearance", style = MaterialTheme.typography.titleMedium)
              Row(horizontalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm)) {
                ThemeMode.entries.forEach { mode ->
                  FilterChip(
                    selected = state.preferences.themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                    label = {
                      Text(
                        when (mode) {
                          ThemeMode.SYSTEM -> "System"
                          ThemeMode.LIGHT -> "Light"
                          ThemeMode.DARK -> "Dark"
                        },
                      )
                    },
                  )
                }
              }

              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(
                      value = state.preferences.dynamicColor,
                      role = Role.Switch,
                      onValueChange = viewModel::setDynamicColor,
                    ),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text("Dynamic Color", style = MaterialTheme.typography.bodyLarge)
                    Text(
                      text = "Match your Android wallpaper palette (Material You)",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Switch(
                    checked = state.preferences.dynamicColor,
                    onCheckedChange = null,
                  )
                }
              }
            }
          }
        }

        item(key = "defaults_card") {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(DirectlineSpacing.md),
              verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm),
            ) {
              Text("Launcher Defaults", style = MaterialTheme.typography.titleMedium)
              OutlinedButton(
                onClick = { showCountryPicker = true },
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp),
              ) {
                Text(
                  text = state.defaultCountry?.let {
                    val flag = it.flagEmoji.takeIf { f -> f.isNotEmpty() }?.let { f -> "$f " }.orEmpty()
                    "Default Country: $flag${it.name} (${it.formattedCallingCode})"
                  } ?: "Default Country: None (always choose explicitly)",
                )
              }
              OutlinedTextField(
                value = defaultMessageDraft,
                onValueChange = { defaultMessageDraft = it },
                label = { Text("Default message draft (optional)") },
                modifier = Modifier.fillMaxWidth(),
              )
              Button(
                onClick = { viewModel.setDefaultMessagePrefix(defaultMessageDraft) },
                modifier = Modifier.heightIn(min = 48.dp),
              ) {
                Text("Save Default Message")
              }
            }
          }
        }

        item(key = "privacy_card") {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(DirectlineSpacing.md),
              verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm),
            ) {
              Text("Privacy & Local Data", style = MaterialTheme.typography.titleMedium)
              Text(
                text = "• Local-first storage for templates, history, and preferences (no account or backend).\n" +
                  "• Does not access Android Contacts or read WhatsApp conversations.\n" +
                  "• Uses official WhatsApp Click to Chat (https://wa.me) links via Android Intents.\n" +
                  "• Automatic Android cloud backup is disabled.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Button(
                onClick = { showDeleteAllConfirm = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.error,
                  contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp),
              ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete All Local Data")
              }
            }
          }
        }
      }
    }
  }

  if (showCountryPicker) {
    CountryPickerDialog(
      countries = viewModel.countries,
      selectedIso = state.defaultCountry?.isoCode,
      allowClear = true,
      onSelect = viewModel::setDefaultCountry,
      onDismiss = { showCountryPicker = false },
    )
  }

  if (showDeleteAllConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteAllConfirm = false },
      title = { Text("Delete all local data?") },
      text = {
        Text("This permanently deletes all templates, history, and preferences stored on this device.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteAllLocalData()
            showDeleteAllConfirm = false
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
        ) {
          Text("Delete All")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteAllConfirm = false }) {
          Text("Cancel")
        }
      },
    )
  }
}
