package com.directline.feature.templates

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.directline.core.designsystem.DirectlineSpacing
import com.directline.core.designsystem.EmptyStateCard
import com.directline.core.designsystem.StatusBanner
import com.directline.core.template.TemplateProcessResult
import com.directline.core.template.TemplateVariable
import com.directline.domain.model.MessageTemplate

@Composable
fun TemplatesScreen(
  onUseTemplateInLauncher: (String) -> Unit,
  viewModel: TemplatesViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  var editingTemplate by remember { mutableStateOf<MessageTemplate?>(null) }
  var showEditorDialog by remember { mutableStateOf(false) }
  var templateToUse by remember { mutableStateOf<MessageTemplate?>(null) }

  Scaffold(
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingTemplate = null
          showEditorDialog = true
        },
      ) {
        Icon(Icons.Default.Add, contentDescription = "Create template")
      }
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(
        start = DirectlineSpacing.md,
        end = DirectlineSpacing.md,
        top = DirectlineSpacing.md,
        bottom = 88.dp,
      ),
      verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm),
    ) {
      item {
        Text("Message Templates", style = MaterialTheme.typography.headlineSmall)
      }

      state.feedbackMessage?.let { msg ->
        item {
          StatusBanner(
            message = msg,
            isError = false,
            onDismiss = viewModel::dismissFeedback,
          )
        }
      }

      item {
        OutlinedTextField(
          value = state.searchQuery,
          onValueChange = viewModel::onSearchQueryChanged,
          placeholder = { Text("Search templates...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      if (state.categories.isNotEmpty()) {
        item {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm)) {
            items(state.categories, key = { it.id }) { category ->
              FilterChip(
                selected = state.selectedCategoryId == category.id,
                onClick = { viewModel.onSelectCategory(category.id) },
                label = { Text(category.name) },
              )
            }
          }
        }
      }

      if (state.templates.isEmpty()) {
        item {
          EmptyStateCard(
            icon = Icons.Default.Description,
            title = "No message templates",
            description = "Create reusable WhatsApp templates with variables like {{name}}, {{product}}, or {{price}}.",
          )
        }
      } else {
        items(state.templates, key = { it.id }) { template ->
          Card(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(DirectlineSpacing.md),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                  )
                  template.categoryName?.let { cat ->
                    Text(
                      text = cat,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                }
                IconButton(onClick = { viewModel.duplicateTemplate(template.id) }) {
                  Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate template")
                }
                IconButton(
                  onClick = {
                    editingTemplate = template
                    showEditorDialog = true
                  },
                ) {
                  Icon(Icons.Default.Edit, contentDescription = "Edit template")
                }
                IconButton(onClick = { viewModel.deleteTemplate(template.id) }) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete template")
                }
              }

              Text(
                text = template.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
              ) {
                TextButton(
                  onClick = {
                    val vars = viewModel.extractVariables(template.content)
                    if (vars.isEmpty()) {
                      onUseTemplateInLauncher(template.content)
                    } else {
                      templateToUse = template
                    }
                  },
                ) {
                  Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Use in Launcher")
                }
              }
            }
          }
        }
      }
    }
  }

  if (showEditorDialog) {
    TemplateEditorDialog(
      existing = editingTemplate,
      onDismiss = { showEditorDialog = false },
      onSave = { title, content, category, onError ->
        viewModel.saveTemplate(
          id = editingTemplate?.id,
          title = title,
          content = content,
          categoryName = category,
          onSuccess = { showEditorDialog = false },
          onError = onError,
        )
      },
    )
  }

  templateToUse?.let { template ->
    val keys = remember(template.id) { viewModel.extractVariables(template.content) }
    val values = remember(template.id) { mutableStateMapOf<String, String>() }
    val result = viewModel.processTemplate(template.content, values)

    AlertDialog(
      onDismissRequest = { templateToUse = null },
      title = { Text("Preview & Fill: ${template.title}") },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          keys.forEach { key ->
            OutlinedTextField(
              value = values[key].orEmpty(),
              onValueChange = { values[key] = it },
              label = { Text("{{$key}}") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text("Preview", style = MaterialTheme.typography.labelLarge)
          Text(
            text = when (result) {
              is TemplateProcessResult.Success -> result.renderedMessage
              is TemplateProcessResult.MissingVariables -> result.partialPreview
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (result is TemplateProcessResult.MissingVariables) {
            Text(
              text = "Missing values: ${result.missingKeys.joinToString(", ")}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (result is TemplateProcessResult.Success) {
              onUseTemplateInLauncher(result.renderedMessage)
              templateToUse = null
            }
          },
          enabled = result is TemplateProcessResult.Success,
        ) {
          Text("Use Message")
        }
      },
      dismissButton = {
        TextButton(onClick = { templateToUse = null }) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun TemplateEditorDialog(
  existing: MessageTemplate?,
  onDismiss: () -> Unit,
  onSave: (title: String, content: String, category: String?, onError: (String) -> Unit) -> Unit,
) {
  var title by remember(existing) { mutableStateOf(existing?.title.orEmpty()) }
  var category by remember(existing) { mutableStateOf(existing?.categoryName.orEmpty()) }
  var content by remember(existing) { mutableStateOf(existing?.content.orEmpty()) }
  var errorText by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existing == null) "New Template" else "Edit Template") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it; errorText = null },
          label = { Text("Title *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = category,
          onValueChange = { category = it },
          label = { Text("Category (optional)") },
          placeholder = { Text("Sales, Support, Personal") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Text("Insert Variable", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(TemplateVariable.entries) { variable ->
            AssistChip(
              onClick = {
                content = if (content.isEmpty() || content.endsWith(" ") || content.endsWith("\n")) {
                  content + variable.placeholder
                } else {
                  "$content ${variable.placeholder}"
                }
              },
              label = { Text(variable.placeholder) },
            )
          }
        }
        OutlinedTextField(
          value = content,
          onValueChange = { content = it; errorText = null },
          label = { Text("Message Content *") },
          placeholder = { Text("Hello {{name}}! I'm contacting you regarding {{product}}.") },
          minLines = 4,
          maxLines = 8,
          modifier = Modifier.fillMaxWidth(),
        )
        errorText?.let { err ->
          Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(title, content, category.ifBlank { null }) { errorText = it } },
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}
