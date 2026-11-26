package com.directline.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.directline.core.designsystem.CountryPickerDialog
import com.directline.core.designsystem.DirectlineLogoMark
import com.directline.core.designsystem.DirectlineSpacing
import com.directline.core.designsystem.EmptyStateCard
import com.directline.core.designsystem.ReadableWidthContainer
import com.directline.core.designsystem.SectionHeader
import com.directline.core.designsystem.StatusBanner
import com.directline.core.template.TemplateProcessResult
import com.directline.core.whatsapp.PhoneNumberValidationResult
import com.directline.domain.model.InteractionHistory
import com.directline.domain.model.MessageTemplate
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  incomingSharedText: String?,
  onConsumeSharedText: () -> Unit,
  pendingTemplateMessage: String?,
  onConsumePendingTemplate: () -> Unit,
  onNavigateToHistory: () -> Unit,
  onBeforeWhatsAppLaunch: (proceed: () -> Unit) -> Unit,
  onShowSnackbar: (String) -> Unit,
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
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            DirectlineLogoMark(size = 32.dp)
            Text("Directline")
          }
        },
        windowInsets = WindowInsets(0, 0, 0, 0),
      )
    },
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .imePadding(),
    ) {
      val isTwoPane = maxWidth >= 840.dp

      if (isTwoPane) {
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(DirectlineSpacing.md),
          horizontalArrangement = Arrangement.spacedBy(DirectlineSpacing.lg),
        ) {
          Column(
            modifier = Modifier
              .weight(1.1f)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.md),
          ) {
            state.incomingSharedNotice?.let { notice ->
              StatusBanner(
                message = notice,
                isError = false,
                onDismiss = viewModel::dismissSharedNotice,
              )
            }
            LauncherCard(
              state = state,
              validationMessage = viewModel.validationErrorMessage(state.validationResult),
              onOpenCountryPicker = { showCountryPicker = true },
              onPhoneChanged = viewModel::onPhoneInputChanged,
              onMessageChanged = viewModel::onMessageInputChanged,
              onOpenTemplatePicker = { showTemplatePicker = true },
              onClearForm = viewModel::clearForm,
              onLaunchWhatsApp = { onBeforeWhatsAppLaunch(viewModel::launchWhatsApp) },
            )
          }

          LazyColumn(
            modifier = Modifier.weight(0.9f),
            verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.md),
          ) {
            item(key = "recent_header") {
              SectionHeader(
                title = "Recent Destinations",
                actionLabel = if (state.recentHistory.isNotEmpty()) "See all" else null,
                onActionClick = if (state.recentHistory.isNotEmpty()) onNavigateToHistory else null,
              )
            }

            if (state.recentHistory.isEmpty()) {
              item(key = "recent_empty") {
                EmptyStateCard(
                  icon = Icons.Default.History,
                  title = "No recent launches yet",
                  description = "Numbers you open in WhatsApp will appear here for quick access.",
                )
              }
            } else {
              items(state.recentHistory, key = { it.id }) { item ->
                RecentHistoryCard(
                  item = item,
                  onPrefill = {
                    viewModel.prefillFromHistory(
                      phoneE164 = item.phoneNumberSnapshot,
                      message = item.messagePreview,
                    )
                  },
                  onLaunch = {
                    onBeforeWhatsAppLaunch {
                      viewModel.launchNormalizedNumber(item.phoneNumberSnapshot)
                    }
                  },
                )
              }
            }
          }
        }
      } else {
        ReadableWidthContainer {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(DirectlineSpacing.md),
            verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.md),
          ) {
            state.incomingSharedNotice?.let { notice ->
              item(key = "shared_notice") {
                StatusBanner(
                  message = notice,
                  isError = false,
                  onDismiss = viewModel::dismissSharedNotice,
                )
              }
            }

            item(key = "launcher_card") {
              LauncherCard(
                state = state,
                validationMessage = viewModel.validationErrorMessage(state.validationResult),
                onOpenCountryPicker = { showCountryPicker = true },
                onPhoneChanged = viewModel::onPhoneInputChanged,
                onMessageChanged = viewModel::onMessageInputChanged,
                onOpenTemplatePicker = { showTemplatePicker = true },
                onClearForm = viewModel::clearForm,
                onLaunchWhatsApp = { onBeforeWhatsAppLaunch(viewModel::launchWhatsApp) },
              )
            }

            item(key = "recent_header") {
              SectionHeader(
                title = "Recent Destinations",
                actionLabel = if (state.recentHistory.isNotEmpty()) "See all" else null,
                onActionClick = if (state.recentHistory.isNotEmpty()) onNavigateToHistory else null,
              )
            }

            if (state.recentHistory.isEmpty()) {
              item(key = "recent_empty") {
                EmptyStateCard(
                  icon = Icons.Default.History,
                  title = "No recent launches yet",
                  description = "Numbers you open in WhatsApp will appear here for quick access.",
                )
              }
            } else {
              items(state.recentHistory, key = { it.id }) { item ->
                RecentHistoryCard(
                  item = item,
                  onPrefill = {
                    viewModel.prefillFromHistory(
                      phoneE164 = item.phoneNumberSnapshot,
                      message = item.messagePreview,
                    )
                  },
                  onLaunch = {
                    onBeforeWhatsAppLaunch {
                      viewModel.launchNormalizedNumber(item.phoneNumberSnapshot)
                    }
                  },
                )
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
                  .heightIn(min = 48.dp)
                  .clickable(onClickLabel = "Use ${template.title}") {
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
                Text(
                  text = template.title,
                  style = MaterialTheme.typography.titleSmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = template.content,
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
    val valuesSnapshot = values.toMap()
    val processResult = remember(template.id, valuesSnapshot) {
      viewModel.processTemplate(template.content, valuesSnapshot)
    }

    ModalBottomSheet(
      onDismissRequest = { templateToFill = null },
      sheetState = sheetState,
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .imePadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = DirectlineSpacing.md)
          .padding(bottom = DirectlineSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm),
      ) {
        Text(
          text = "Fill Template: ${template.title}",
          style = MaterialTheme.typography.titleLarge,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
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
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
        ) {
          Text(
            text = previewText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
          )
        }
        if (processResult is TemplateProcessResult.MissingVariables) {
          Text(
            text = "Fill all variables: ${processResult.missingKeys.joinToString(", ")}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = { templateToFill = null }) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(8.dp))
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
        }
      }
    }
  }
}

@Composable
private fun LauncherCard(
  state: HomeUiState,
  validationMessage: String,
  onOpenCountryPicker: () -> Unit,
  onPhoneChanged: (String) -> Unit,
  onMessageChanged: (String) -> Unit,
  onOpenTemplatePicker: () -> Unit,
  onClearForm: () -> Unit,
  onLaunchWhatsApp: () -> Unit,
) {
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
      Text(
        text = "Start a WhatsApp chat",
        style = MaterialTheme.typography.titleMedium,
      )

      val countrySemantics = state.selectedCountry?.let {
        "Country ${it.name}, ${it.formattedCallingCode}. Select country"
      } ?: "Select country calling code"

      BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stackPhoneInputs = maxWidth < 320.dp
        if (stackPhoneInputs) {
          Column(verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm)) {
            OutlinedButton(
              onClick = onOpenCountryPicker,
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { contentDescription = countrySemantics },
            ) {
              Text(
                text = state.selectedCountry?.let {
                  val flag = it.flagEmoji.takeIf { f -> f.isNotEmpty() }?.let { f -> "$f " }.orEmpty()
                  "$flag${it.name} (${it.formattedCallingCode})"
                } ?: "Select country",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            PhoneNumberField(
              value = state.phoneInput,
              isError = state.phoneInput.isNotBlank() &&
                state.validationResult !is PhoneNumberValidationResult.Valid,
              onValueChange = onPhoneChanged,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        } else {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm),
            verticalAlignment = Alignment.Top,
          ) {
            OutlinedButton(
              onClick = onOpenCountryPicker,
              modifier = Modifier
                .heightIn(min = 56.dp)
                .semantics { contentDescription = countrySemantics },
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            ) {
              Text(
                text = state.selectedCountry?.let {
                  val flag = it.flagEmoji.takeIf { f -> f.isNotEmpty() }?.let { f -> "$f " }.orEmpty()
                  "$flag${it.formattedCallingCode}"
                } ?: "Country",
                maxLines = 1,
                softWrap = false,
              )
              Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            PhoneNumberField(
              value = state.phoneInput,
              isError = state.phoneInput.isNotBlank() &&
                state.validationResult !is PhoneNumberValidationResult.Valid,
              onValueChange = onPhoneChanged,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      if (state.phoneInput.isNotBlank()) {
        val isValid = state.validationResult is PhoneNumberValidationResult.Valid
        Text(
          text = validationMessage,
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
        onValueChange = onMessageChanged,
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
          onClick = onOpenTemplatePicker,
          label = { Text("Use template") },
          leadingIcon = {
            Icon(Icons.Default.Description, contentDescription = null)
          },
        )
        if (state.phoneInput.isNotBlank() || state.messageInput.isNotBlank()) {
          TextButton(onClick = onClearForm) {
            Text("Clear")
          }
        }
      }

      Button(
        onClick = onLaunchWhatsApp,
        enabled = state.validationResult is PhoneNumberValidationResult.Valid,
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp),
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

@Composable
private fun PhoneNumberField(
  value: String,
  isError: Boolean,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text("Phone number") },
    placeholder = { Text("+57 300 123 4567") },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
    trailingIcon = {
      if (value.isNotEmpty()) {
        IconButton(onClick = { onValueChange("") }) {
          Icon(Icons.Default.Clear, contentDescription = "Clear phone number")
        }
      }
    },
    isError = isError,
    modifier = modifier,
  )
}

@Composable
private fun RecentHistoryCard(
  item: InteractionHistory,
  onPrefill: () -> Unit,
  onLaunch: () -> Unit,
) {
  val label = item.phoneNumberSnapshot
  val formattedTime = remember(item.createdAt) {
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
      .format(Date(item.createdAt))
  }
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(
        onClickLabel = "Prefill $label in launcher",
        onClick = onPrefill,
      ),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
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
          text = label,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = formattedTime,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      IconButton(onClick = onLaunch) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = "Open $label in WhatsApp",
          tint = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}
