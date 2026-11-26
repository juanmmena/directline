package com.directline.feature.contacts

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.directline.core.designsystem.CountryPickerDialog
import com.directline.core.designsystem.DirectlineSpacing
import com.directline.core.designsystem.EmptyStateCard
import com.directline.core.designsystem.ReadableWidthContainer
import com.directline.core.whatsapp.CountryInfo
import com.directline.domain.model.Contact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
  onShowSnackbar: (String) -> Unit,
  viewModel: ContactsViewModel = hiltViewModel(),
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  var editingContact by remember { mutableStateOf<Contact?>(null) }
  var showEditorSheet by remember { mutableStateOf(false) }
  var contactToDelete by remember { mutableStateOf<Contact?>(null) }

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
        title = { Text("Contacts") },
        windowInsets = WindowInsets(0, 0, 0, 0),
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingContact = null
          showEditorSheet = true
        },
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add contact")
      }
    },
  ) { innerPadding ->
    ReadableWidthContainer(modifier = Modifier.padding(innerPadding)) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .imePadding(),
        contentPadding = PaddingValues(
          start = DirectlineSpacing.md,
          end = DirectlineSpacing.md,
          top = DirectlineSpacing.sm,
          bottom = 88.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm),
      ) {
        item(key = "search_contacts") {
          OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            placeholder = { Text("Search name, number, tag, notes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }

        item(key = "filter_chips") {
          LazyRow(horizontalArrangement = Arrangement.spacedBy(DirectlineSpacing.sm)) {
            item(key = "favorites_filter") {
              FilterChip(
                selected = state.favoritesOnly,
                onClick = viewModel::onToggleFavoritesOnly,
                label = { Text("Favorites") },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
              )
            }
            items(state.allTags, key = { it.id }) { tag ->
              FilterChip(
                selected = state.selectedTagName.equals(tag.name, ignoreCase = true),
                onClick = { viewModel.onSelectTag(tag.name) },
                label = { Text(tag.name) },
              )
            }
          }
        }

        if (state.contacts.isEmpty()) {
          item(key = "contacts_empty") {
            EmptyStateCard(
              icon = Icons.Default.Person,
              title = "No local contacts",
              description = "Save WhatsApp contacts locally without touching your Android address book.",
            )
          }
        } else {
          items(state.contacts, key = { it.id }) { contact ->
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
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                  )
                  IconButton(onClick = { viewModel.toggleFavorite(contact) }) {
                    Icon(
                      imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                      contentDescription = if (contact.isFavorite) {
                        "Remove ${contact.name} from favorites"
                      } else {
                        "Add ${contact.name} to favorites"
                      },
                      tint = if (contact.isFavorite) {
                        MaterialTheme.colorScheme.primary
                      } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                      },
                    )
                  }
                  IconButton(
                    onClick = {
                      editingContact = contact
                      showEditorSheet = true
                    },
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${contact.name}")
                  }
                  IconButton(onClick = { contactToDelete = contact }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete ${contact.name}")
                  }
                }

                contact.phoneNumbers.forEach { phone ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = phone.normalizedValue,
                      style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(
                      onClick = { viewModel.launchWhatsApp(contact, phone.normalizedValue) },
                      modifier = Modifier.semantics {
                        contentDescription = "Open WhatsApp for ${contact.name}, ${phone.normalizedValue}"
                      },
                    ) {
                      Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("WhatsApp")
                    }
                  }
                }

                if (contact.notes.isNotBlank()) {
                  Text(
                    text = contact.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }

                if (contact.tags.isNotEmpty()) {
                  LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(contact.tags, key = { it.id }) { tag ->
                      AssistChip(
                        onClick = { viewModel.onSelectTag(tag.name) },
                        label = { Text(tag.name) },
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showEditorSheet) {
    ContactEditorSheet(
      existing = editingContact,
      countries = viewModel.countries,
      defaultCountryIso = state.defaultCountryIso,
      onDismiss = { showEditorSheet = false },
      onSave = { name, notes, isFav, numbers, tagsCsv, onError ->
        viewModel.saveContact(
          id = editingContact?.id,
          name = name,
          notes = notes,
          isFavorite = isFav,
          rawNumbers = numbers,
          tagsCsv = tagsCsv,
          onSuccess = { showEditorSheet = false },
          onError = onError,
        )
      },
    )
  }

  contactToDelete?.let { contact ->
    AlertDialog(
      onDismissRequest = { contactToDelete = null },
      title = { Text("Delete contact?") },
      text = {
        Text("Delete ${contact.name}? History snapshots and templates will not be affected.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteContact(contact.id)
            contactToDelete = null
          },
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { contactToDelete = null }) {
          Text("Cancel")
        }
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactEditorSheet(
  existing: Contact?,
  countries: List<CountryInfo>,
  defaultCountryIso: String,
  onDismiss: () -> Unit,
  onSave: (
    name: String,
    notes: String,
    isFavorite: Boolean,
    numbers: List<Pair<String, String?>>,
    tagsCsv: String,
    onError: (String) -> Unit,
  ) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
  var notes by remember(existing) { mutableStateOf(existing?.notes.orEmpty()) }
  var isFavorite by remember(existing) { mutableStateOf(existing?.isFavorite ?: false) }
  var tagsCsv by remember(existing) {
    mutableStateOf(existing?.tags?.joinToString(", ") { it.name }.orEmpty())
  }
  val phoneEntries = remember(existing) {
    mutableStateListOf<Pair<String, String?>>().apply {
      if (existing != null && existing.phoneNumbers.isNotEmpty()) {
        existing.phoneNumbers.forEach { add(it.normalizedValue to it.countryCode.ifBlank { null }) }
      } else {
        add("" to defaultCountryIso.ifBlank { null })
      }
    }
  }
  var pickerForIndex by remember { mutableStateOf<Int?>(null) }
  var errorText by remember { mutableStateOf<String?>(null) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
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
        text = if (existing == null) "New Contact" else "Edit Contact",
        style = MaterialTheme.typography.titleLarge,
      )

      OutlinedTextField(
        value = name,
        onValueChange = { name = it; errorText = null },
        label = { Text("Name / Alias *") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      Text("Phone Numbers", style = MaterialTheme.typography.labelLarge)
      phoneEntries.forEachIndexed { index, (raw, countryIso) ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = { pickerForIndex = index },
            modifier = Modifier.heightIn(min = 52.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
          ) {
            Text(countryIso?.ifBlank { null } ?: "CC")
          }
          OutlinedTextField(
            value = raw,
            onValueChange = {
              phoneEntries[index] = it to countryIso
              errorText = null
            },
            placeholder = { Text("+57 300 123 4567") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.weight(1f),
          )
          if (phoneEntries.size > 1) {
            IconButton(onClick = { phoneEntries.removeAt(index) }) {
              Icon(
                Icons.Default.RemoveCircleOutline,
                contentDescription = "Remove phone number ${index + 1}",
              )
            }
          }
        }
      }
      TextButton(onClick = { phoneEntries.add("" to defaultCountryIso.ifBlank { null }) }) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add another phone number")
      }

      OutlinedTextField(
        value = tagsCsv,
        onValueChange = { tagsCsv = it },
        label = { Text("Tags (comma separated)") },
        placeholder = { Text("Customer, VIP") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Notes") },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth(),
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 48.dp)
          .toggleable(
            value = isFavorite,
            role = Role.Checkbox,
            onValueChange = { isFavorite = it },
          ),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Checkbox(checked = isFavorite, onCheckedChange = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Mark as favorite", style = MaterialTheme.typography.bodyLarge)
      }

      errorText?.let { err ->
        Spacer(modifier = Modifier.height(4.dp))
        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(onClick = onDismiss) {
          Text("Cancel")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
          onClick = {
            onSave(name, notes, isFavorite, phoneEntries.toList(), tagsCsv) { errorText = it }
          },
        ) {
          Text("Save")
        }
      }
    }
  }

  pickerForIndex?.let { index ->
    CountryPickerDialog(
      countries = countries,
      selectedIso = phoneEntries.getOrNull(index)?.second,
      allowClear = true,
      onSelect = { selected ->
        val current = phoneEntries.getOrNull(index)
        if (current != null) {
          phoneEntries[index] = current.first to selected?.isoCode
        }
      },
      onDismiss = { pickerForIndex = null },
    )
  }
}
