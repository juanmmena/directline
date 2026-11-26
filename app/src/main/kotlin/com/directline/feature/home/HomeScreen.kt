package com.directline.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.directline.core.designsystem.CountryPickerDialog
import com.directline.core.designsystem.DirectlineSpacing
import com.directline.core.designsystem.EmptyStateCard
import com.directline.core.designsystem.SectionHeader
import com.directline.core.designsystem.StatusBanner
import com.directline.core.template.TemplateProcessResult
import com.directline.core.whatsapp.PhoneNumberValidationResult
import com.directline.domain.model.MessageTemplate
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(
  incomingSharedText: String?,
  onConsumeSharedText: () -> Unit,
  pendingTemplateMessage: String?,
  onConsumePendingTemplate: () -> Unit,
  onNavigateToHistory: () -> Unit,
  onNavigateToContacts: () -> Unit,
  viewModel: HomeViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  var showCountryPicker by remember { mutableStateOf(false) }
  var showTemplatePicker by remember { mutableStateOf(false) }
  var templateToFill by remember { mutableStateOf<MessageTemplate?>(null) }

  LaunchedEffect(incomingSharedText) {
    if (!incomingSharedText.isNullOrBlank()) {
      viewModel.applyIncomingSharedText(incomingSharedText)
      onConsumeSharedText()
    }
  }

  LaunchedEffect(pendingTemplateMessage) {
    if (!pendingTemplateMessage.isNullOrBlank()) {
      viewModel.applyRenderedTemplate(pendingTemplateMessage)
      onConsumePendingTemplate()
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(DirectlineSpacing.md),
    verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.md),
  ) {
    item {
      Column {
        Text(
          text = "Directline",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "Start a WhatsApp chat without saving the number.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    state.incomingSharedNotice?.let { notice ->
      item {
        StatusBanner(
          message = notice,
          isError = false,
          onDismiss = viewModel::dismissSharedNotice,
        )
      }
    }

    state.feedbackMessage?.let { feedback ->
      item {
        StatusBanner(
          message = feedback,
          isError = state.isFeedbackError,
          onDismiss = viewModel::dismissFeedback,
        )
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(DirectlineSpacing.md),
          verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm),
        ) {
          Text(
            text = "WhatsApp Launcher",
            style = MaterialTheme.typography.titleMedium,
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm),
            verticalAlignment = Alignment.Top,
          ) {
            OutlinedButton(
              onClick = { showCountryPicker = true },
              modifier = Modifier.height(56.dp),
              contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
              Text(
                text = state.selectedCountry?.let {
                  "${it.isoCode} ${it.formattedCallingCode}"
                } ?: "Country",
              )
              Icon(Icons.Default.ArrowDropDown, contentDescription = "Select country")
            }

            OutlinedTextField(
              value = state.phoneInput,
              onValueChange = viewModel::onPhoneInputChanged,
              label = { Text("Phone number") },
              placeholder = { Text("+57 300 123 4567") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              trailingIcon = {
                if (state.phoneInput.isNotEmpty()) {
                  IconButton(onClick = { viewModel.onPhoneInputChanged("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear phone")
                  }
                }
              },
              isError = state.phoneInput.isNotBlank() &&
                state.validationResult !is PhoneNumberValidationResult.Valid,
              modifier = Modifier.weight(1f),
            )
          }

          if (state.phoneInput.isNotBlank()) {
            val isValid = state.validationResult is PhoneNumberValidationResult.Valid
            Text(
              text = viewModel.validationErrorMessage(state.validationResult),
              style = MaterialTheme.typography.bodySmall,
              color = if (isValid) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.error
              },
            )
          }

          OutlinedTextField(
            value = state.messageInput,
            onValueChange = viewModel::onMessageInputChanged,
            label = { Text("Optional message") },
            placeholder = { Text("Write a message to prefill in WhatsApp...") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            AssistChip(
              onClick = { showTemplatePicker = true },
              label = { Text("Use template") },
              leadingIcon = {
                Icon(Icons.Default.Description, contentDescription = null)
              },
            )
            if (state.phoneInput.isNotBlank() || state.messageInput.isNotBlank()) {
              TextButton(onClick = viewModel::clearForm) {
                Text("Clear")
              }
            }
          }

          Button(
            onClick = viewModel::launchWhatsApp,
            enabled = state.validationResult is PhoneNumberValidationResult.Valid,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp),
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(DirectlineSpacing.sm))
            Text("Open in WhatsApp")
          }

          Text(
            text = "Directline never sends messages automatically. WhatsApp opens with your draft ready to review.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    if (state.favorites.isNotEmpty()) {
      item {
        SectionHeader(
          title = "Favorite Contacts",
          actionLabel = "All",
          onActionClick = onNavigateToContacts,
        )
      }
      item {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm),
        ) {
          items(state.favorites, key = { it.id }) { contact ->
            val primaryPhone = contact.phoneNumbers.firstOrNull()
            Card(
              modifier = Modifier
                .width(170.dp)
                .clickable(enabled = primaryPhone != null) {
                  primaryPhone?.let {
                    viewModel.prefillFromContactOrHistory(it.normalizedValue)
                  }
                },
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
              ),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Text(
                  text = primaryPhone?.normalizedValue ?: "No phone",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                if (primaryPhone != null) {
                  TextButton(
                    onClick = {
                      viewModel.launchNormalizedNumber(
                        normalizedE164 = primaryPhone.normalizedValue,
                        contactId = contact.id,
                        contactName = contact.name,
                      )
                    },
                    contentPadding = PaddingValues(0.dp),
                  ) {
                    Text("Chat now")
                  }
                }
              }
            }
          }
        }
      }
    }

    item {
      SectionHeader(
        title = "Recent Destinations",
        actionLabel = if (state.recentHistory.isNotEmpty()) "See all" else null,
        onActionClick = if (state.recentHistory.isNotEmpty()) onNavigateToHistory else null,
      )
    }

    if (state.recentHistory.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Default.History,
          title = "No recent launches yet",
          description = "Numbers you open in WhatsApp will appear here for quick access.",
        )
      }
    } else {
      items(state.recentHistory, key = { it.id }) { item ->
        val formattedTime = remember(item.createdAt) {
          DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(item.createdAt))
        }
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              viewModel.prefillFromContactOrHistory(
                phoneE164 = item.phoneNumberSnapshot,
                message = item.messagePreview,
              )
            },
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Default.Phone,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = item.contactNameSnapshot ?: item.phoneNumberSnapshot,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
              )
              if (item.contactNameSnapshot != null) {
                Text(
                  text = item.phoneNumberSnapshot,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(
              onClick = {
                viewModel.launchNormalizedNumber(
                  normalizedE164 = item.phoneNumberSnapshot,
                  contactId = item.contactId,
                  contactName = item.contactNameSnapshot,
                )
              },
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Open in WhatsApp",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }
  }

  if (showCountryPicker) {
    CountryPickerDialog(
      countries = state.countries,
      selectedIso = state.selectedCountry?.isoCode,
      allowClear = true,
      onSelect = viewModel::onCountrySelected,
      onDismiss = { showCountryPicker = false },
    )
  }

  if (showTemplatePicker) {
    AlertDialog(
      onDismissRequest = { showTemplatePicker = false },
      title = { Text("Choose Template") },
      text = {
        if (state.templates.isEmpty()) {
          Text("No templates saved yet. Create one in the Templates tab.")
        } else {
          LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
            items(state.templates, key = { it.id }) { template ->
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    showTemplatePicker = false
                    val vars = viewModel.extractTemplateVariables(template.content)
                    if (vars.isEmpty()) {
                      viewModel.applyRenderedTemplate(template.content)
                    } else {
                      templateToFill = template
                    }
                  }
                  .padding(vertical = 10.dp),
              ) {
                Text(template.title, style = MaterialTheme.typography.titleSmall)
                Text(
                  template.content,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              HorizontalDivider()
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showTemplatePicker = false }) {
          Text("Close")
        }
      },
    )
  }

  templateToFill?.let { template ->
    val keys = remember(template.id) { viewModel.extractTemplateVariables(template.content) }
    val values = remember(template.id) {
      mutableStateMapOf<String, String>().apply {
        val currentPhone = (state.validationResult as? PhoneNumberValidationResult.Valid)
          ?.normalizedE164 ?: state.phoneInput
        if ("phone" in keys && currentPhone.isNotBlank()) {
          put("phone", currentPhone)
        }
      }
    }
    val processResult = viewModel.processTemplate(template.content, values)

    AlertDialog(
      onDismissRequest = { templateToFill = null },
      title = { Text("Fill Template: ${template.title}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
          val previewText = when (processResult) {
            is TemplateProcessResult.Success -> processResult.renderedMessage
            is TemplateProcessResult.MissingVariables -> processResult.partialPreview
          }
          Text(
            text = previewText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (processResult is TemplateProcessResult.MissingVariables) {
            Text(
              text = "Fill all variables: ${processResult.missingKeys.joinToString(", ")}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (processResult is TemplateProcessResult.Success) {
              viewModel.applyRenderedTemplate(processResult.renderedMessage)
              templateToFill = null
            }
          },
          enabled = processResult is TemplateProcessResult.Success,
        ) {
          Text("Apply")
        }
      },
      dismissButton = {
        TextButton(onClick = { templateToFill = null }) {
          Text("Cancel")
        }
      },
    )
  }
}
