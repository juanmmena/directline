package com.directline.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.directline.core.whatsapp.CountryCodeProvider
import com.directline.core.whatsapp.CountryInfo
import com.directline.core.whatsapp.PhoneNumberValidationResult
import com.directline.core.whatsapp.PhoneNumberValidator
import com.directline.core.whatsapp.WhatsAppLaunchResult
import com.directline.domain.model.Contact
import com.directline.domain.model.Tag
import com.directline.domain.repository.ContactRepository
import com.directline.domain.repository.PhoneNumberDraft
import com.directline.domain.repository.PreferencesRepository
import com.directline.domain.usecase.LaunchWhatsAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactsUiState(
  val contacts: List<Contact> = emptyList(),
  val allTags: List<Tag> = emptyList(),
  val searchQuery: String = "",
  val favoritesOnly: Boolean = false,
  val selectedTagName: String? = null,
  val defaultCountryIso: String = "",
  val countries: List<CountryInfo> = emptyList(),
  val feedbackMessage: String? = null,
  val isFeedbackError: Boolean = false,
)

private data class ContactsFilterState(
  val searchQuery: String = "",
  val favoritesOnly: Boolean = false,
  val selectedTagName: String? = null,
  val feedbackMessage: String? = null,
  val isFeedbackError: Boolean = false,
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
  private val contactRepository: ContactRepository,
  private val preferencesRepository: PreferencesRepository,
  private val phoneNumberValidator: PhoneNumberValidator,
  private val countryCodeProvider: CountryCodeProvider,
  private val launchWhatsAppUseCase: LaunchWhatsAppUseCase,
) : ViewModel() {

  private val filterState = MutableStateFlow(ContactsFilterState())

  val uiState: StateFlow<ContactsUiState> = combine(
    contactRepository.observeContacts(),
    contactRepository.observeTags(),
    preferencesRepository.observePreferences(),
    filterState,
  ) { contacts, tags, prefs, filter ->
    val query = filter.searchQuery.trim()
    val filtered = contacts.filter { contact ->
      val matchesFav = !filter.favoritesOnly || contact.isFavorite
      val matchesTag = filter.selectedTagName == null ||
        contact.tags.any { it.name.equals(filter.selectedTagName, ignoreCase = true) }
      val matchesQuery = query.isEmpty() ||
        contact.name.contains(query, ignoreCase = true) ||
        contact.notes.contains(query, ignoreCase = true) ||
        contact.phoneNumbers.any {
          it.normalizedValue.contains(query, ignoreCase = true) ||
            it.rawValue.contains(query, ignoreCase = true)
        } ||
        contact.tags.any { it.name.contains(query, ignoreCase = true) }
      matchesFav && matchesTag && matchesQuery
    }

    ContactsUiState(
      contacts = filtered,
      allTags = tags,
      searchQuery = filter.searchQuery,
      favoritesOnly = filter.favoritesOnly,
      selectedTagName = filter.selectedTagName,
      defaultCountryIso = prefs.defaultCountryIso,
      countries = countryCodeProvider.allCountries,
      feedbackMessage = filter.feedbackMessage,
      isFeedbackError = filter.isFeedbackError,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = ContactsUiState(countries = countryCodeProvider.allCountries),
  )

  fun onSearchQueryChanged(query: String) {
    filterState.update { it.copy(searchQuery = query) }
  }

  fun onToggleFavoritesOnly() {
    filterState.update { it.copy(favoritesOnly = !it.favoritesOnly) }
  }

  fun onSelectTag(tagName: String?) {
    filterState.update {
      it.copy(selectedTagName = if (it.selectedTagName == tagName) null else tagName)
    }
  }

  fun dismissFeedback() {
    filterState.update { it.copy(feedbackMessage = null) }
  }

  fun toggleFavorite(contact: Contact) {
    viewModelScope.launch {
      contactRepository.toggleFavorite(contact.id, !contact.isFavorite)
    }
  }

  fun deleteContact(contactId: String) {
    viewModelScope.launch {
      contactRepository.deleteContact(contactId)
      filterState.update {
        it.copy(feedbackMessage = "Contact deleted.", isFeedbackError = false)
      }
    }
  }

  fun saveContact(
    id: String?,
    name: String,
    notes: String,
    isFavorite: Boolean,
    rawNumbers: List<Pair<String, String?>>,
    tagsCsv: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
  ) {
    val trimmedName = name.trim()
    if (trimmedName.isEmpty()) {
      onError("Contact name is required.")
      return
    }

    val nonBlankNumbers = rawNumbers.filter { it.first.isNotBlank() }
    if (nonBlankNumbers.isEmpty()) {
      onError("At least one valid phone number is required.")
      return
    }

    viewModelScope.launch {
      val defaultIso = preferencesRepository.observePreferences().first().defaultCountryIso
      val drafts = mutableListOf<PhoneNumberDraft>()
      for ((index, pair) in nonBlankNumbers.withIndex()) {
        val (raw, countryIso) = pair
        val effectiveCountry = countryIso?.ifBlank { null } ?: defaultIso.ifBlank { null }
        when (val result = phoneNumberValidator.validate(raw, effectiveCountry)) {
          is PhoneNumberValidationResult.Valid -> {
            drafts.add(
              PhoneNumberDraft(
                rawValue = raw.trim(),
                normalizedValue = result.normalizedE164,
                countryCode = result.countryIso,
              ),
            )
          }
          is PhoneNumberValidationResult.CountryRequired -> {
            onError("Phone #${index + 1} needs a country code or '+' prefix.")
            return@launch
          }
          else -> {
            onError("Phone #${index + 1} is not a valid phone number.")
            return@launch
          }
        }
      }

      val tagNames = tagsCsv.split(",", ";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

      contactRepository.saveContact(
        id = id,
        name = trimmedName,
        notes = notes,
        isFavorite = isFavorite,
        phoneNumbers = drafts,
        tagNames = tagNames,
      )
      filterState.update {
        it.copy(
          feedbackMessage = if (id == null) "Contact saved." else "Contact updated.",
          isFeedbackError = false,
        )
      }
      onSuccess()
    }
  }

  fun launchWhatsApp(contact: Contact, normalizedPhone: String) {
    viewModelScope.launch {
      val result = launchWhatsAppUseCase.launchNormalized(
        normalizedE164 = normalizedPhone,
        contactId = contact.id,
        contactName = contact.name,
      )
      val (msg, isErr) = when (result) {
        is WhatsAppLaunchResult.LaunchInitiated -> "Opening WhatsApp for ${contact.name}." to false
        is WhatsAppLaunchResult.ApplicationUnavailable -> "No compatible WhatsApp app or browser found." to true
        is WhatsAppLaunchResult.InvalidDestination -> result.reason to true
        is WhatsAppLaunchResult.LaunchFailed -> result.reason to true
        is WhatsAppLaunchResult.InvalidPhoneNumber -> "Stored phone number is invalid." to true
      }
      filterState.update { it.copy(feedbackMessage = msg, isFeedbackError = isErr) }
    }
  }
}
