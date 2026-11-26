package com.directline.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.directline.core.designsystem.CountryPickerDialog
import com.directline.core.designsystem.DirectlineSpacing
import com.directline.core.designsystem.StatusBanner
import com.directline.domain.model.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var showCountryPicker by remember { mutableStateOf(false) }
  var showDeleteAllConfirm by remember { mutableStateOf(false) }
  var defaultMessageDraft by remember { mutableStateOf(state.preferences.defaultMessagePrefix) }

  LaunchedEffect(state.preferences.defaultMessagePrefix) {
    defaultMessageDraft = state.preferences.defaultMessagePrefix
  }

  val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("text/csv"),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        try {
          val csv = viewModel.buildExportCsv()
          context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(csv.toByteArray(Charsets.UTF_8))
          }
          viewModel.reportExportResult(true, "Contacts exported to CSV.")
        } catch (e: Exception) {
          viewModel.reportExportResult(false, "Failed to export CSV file.")
        }
      }
    }
  }

  val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      scope.launch {
        try {
          val text = context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
          }.orEmpty()
          viewModel.prepareCsvImport(text)
        } catch (e: Exception) {
          viewModel.reportExportResult(false, "Failed to read selected CSV file.")
        }
      }
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(DirectlineSpacing.md),
    verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.md),
  ) {
    item {
      Text("Settings & Privacy", style = MaterialTheme.typography.headlineSmall)
    }

    state.feedbackMessage?.let { msg ->
      item {
        StatusBanner(
          message = msg,
          isError = state.isFeedbackError,
          onDismiss = viewModel::dismissFeedback,
        )
      }
    }

    item {
      Card(modifier = Modifier.fillMaxWidth()) {
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
        }
      }
    }

    item {
      Card(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(DirectlineSpacing.md),
          verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm),
        ) {
          Text("Launcher Defaults", style = MaterialTheme.typography.titleMedium)
          OutlinedButton(
            onClick = { showCountryPicker = true },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = state.defaultCountry?.let {
                "Default Country: ${it.name} (${it.formattedCallingCode})"
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
          ) {
            Text("Save Default Message")
          }
        }
      }
    }

    item {
      Card(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(DirectlineSpacing.md),
          verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm),
        ) {
          Text("Contacts Backup (CSV)", style = MaterialTheme.typography.titleMedium)
          Text(
            text = "Format: name,phone_number,tags,notes. Uses Android Storage Access Framework without broad storage permissions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm),
          ) {
            OutlinedButton(
              onClick = { importLauncher.launch(arrayOf("text/*", "application/csv", "application/octet-stream")) },
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Default.FileUpload, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Import CSV")
            }
            OutlinedButton(
              onClick = { exportLauncher.launch("directline-contacts.csv") },
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Default.FileDownload, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Export CSV")
            }
          }
        }
      }
    }

    item {
      Card(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(DirectlineSpacing.md),
          verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm),
        ) {
          Text("Privacy & Local Data", style = MaterialTheme.typography.titleMedium)
          Text(
            text = "• 100% local-first and offline-first (no account, no backend, no cloud sync).\n" +
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
            modifier = Modifier.fillMaxWidth(),
          ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Delete All Local Data")
          }
        }
      }
    }
  }

  if (showCountryPicker) {
    CountryPickerDialog(
      countries = state.countries,
      selectedIso = state.defaultCountry?.isoCode,
      allowClear = true,
      onSelect = viewModel::setDefaultCountry,
      onDismiss = { showCountryPicker = false },
    )
  }

  state.pendingImportPreview?.let { preview ->
    AlertDialog(
      onDismissRequest = viewModel::dismissImportPreview,
      title = { Text("CSV Import Preview") },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text("Ready to import: ${preview.validContacts.size} contact(s)")
          if (preview.duplicatePhoneRows.isNotEmpty()) {
            Text(
              "Duplicates skipped: ${preview.duplicatePhoneRows.size}",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              style = MaterialTheme.typography.bodySmall,
            )
          }
          if (preview.invalidRows.isNotEmpty()) {
            Text(
              "Invalid rows (${preview.invalidRows.size}):",
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.labelMedium,
            )
            preview.invalidRows.take(10).forEach { err ->
              Text(
                "Row ${err.rowNumber}: ${err.message}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = viewModel::confirmCsvImport,
          enabled = preview.validContacts.isNotEmpty(),
        ) {
          Text("Import (${preview.validContacts.size})")
        }
      },
      dismissButton = {
        TextButton(onClick = viewModel::dismissImportPreview) {
          Text("Cancel")
        }
      },
    )
  }

  if (showDeleteAllConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteAllConfirm = false },
      title = { Text("Delete all local data?") },
      text = {
        Text("This permanently deletes all contacts, templates, history, and preferences stored on this device.")
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
