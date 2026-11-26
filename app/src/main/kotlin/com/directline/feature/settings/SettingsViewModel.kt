package com.directline.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.directline.core.csv.CsvContactProcessor
import com.directline.core.csv.CsvImportPreview
import com.directline.core.whatsapp.CountryCodeProvider
import com.directline.core.whatsapp.CountryInfo
import com.directline.domain.model.ThemeMode
import com.directline.domain.model.UserPreferences
import com.directline.domain.repository.ContactRepository
import com.directline.domain.repository.HistoryRepository
import com.directline.domain.repository.PhoneNumberDraft
import com.directline.domain.repository.PreferencesRepository
import com.directline.domain.repository.TemplateRepository
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

data class SettingsUiState(
  val preferences: UserPreferences = UserPreferences(),
  val defaultCountry: CountryInfo? = null,
  val pendingImportPreview: CsvImportPreview? = null,
  val feedbackMessage: String? = null,
  val isFeedbackError: Boolean = false,
)

private data class SettingsTransientState(
  val pendingImportPreview: CsvImportPreview? = null,
  val feedbackMessage: String? = null,
  val isFeedbackError: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
  private val preferencesRepository: PreferencesRepository,
  private val contactRepository: ContactRepository,
  private val templateRepository: TemplateRepository,
  private val historyRepository: HistoryRepository,
  private val countryCodeProvider: CountryCodeProvider,
  private val csvContactProcessor: CsvContactProcessor,
) : ViewModel() {

  val countries: List<CountryInfo> = countryCodeProvider.allCountries

  private val transientState = MutableStateFlow(SettingsTransientState())

  val uiState: StateFlow<SettingsUiState> = combine(
    preferencesRepository.observePreferences(),
    transientState,
  ) { prefs, transient ->
    SettingsUiState(
      preferences = prefs,
      defaultCountry = countryCodeProvider.findByIso(prefs.defaultCountryIso),
      pendingImportPreview = transient.pendingImportPreview,
      feedbackMessage = transient.feedbackMessage,
      isFeedbackError = transient.isFeedbackError,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = SettingsUiState(),
  )

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
  }

  fun setDynamicColor(enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.setDynamicColor(enabled) }
  }

  fun setDefaultCountry(country: CountryInfo?) {
    viewModelScope.launch {
      preferencesRepository.setDefaultCountryIso(country?.isoCode.orEmpty())
    }
  }

  fun setDefaultMessagePrefix(prefix: String) {
    viewModelScope.launch {
      preferencesRepository.setDefaultMessagePrefix(prefix)
      transientState.update {
        it.copy(feedbackMessage = "Default message updated.", isFeedbackError = false)
      }
    }
  }

  fun dismissFeedback() {
    transientState.update { it.copy(feedbackMessage = null) }
  }

  fun dismissImportPreview() {
    transientState.update { it.copy(pendingImportPreview = null) }
  }

  fun prepareCsvImport(csvContent: String) {
    viewModelScope.launch {
      val existingContacts = contactRepository.observeContacts().first()
      val existingPhones = existingContacts
        .flatMap { it.phoneNumbers }
        .map { it.normalizedValue }
        .toSet()
      val defaultIso = preferencesRepository.observePreferences().first().defaultCountryIso
      val preview = csvContactProcessor.parseAndValidate(
        csvContent = csvContent,
        defaultCountryIso = defaultIso.ifBlank { null },
        existingNormalizedPhones = existingPhones,
      )
      transientState.update { it.copy(pendingImportPreview = preview) }
    }
  }

  fun confirmCsvImport() {
    val preview = transientState.value.pendingImportPreview ?: return
    viewModelScope.launch {
      var importedCount = 0
      for (item in preview.validContacts) {
        contactRepository.saveContact(
          id = null,
          name = item.name,
          notes = item.notes,
          isFavorite = false,
          phoneNumbers = listOf(
            PhoneNumberDraft(
              rawValue = item.rawPhone,
              normalizedValue = item.normalizedPhone,
              countryCode = item.countryIso,
            ),
          ),
          tagNames = item.tags,
        )
        importedCount++
      }
      transientState.update {
        it.copy(
          pendingImportPreview = null,
          feedbackMessage = "Imported $importedCount contact(s) from CSV.",
          isFeedbackError = false,
        )
      }
    }
  }

  suspend fun buildExportCsv(): String {
    val contacts = contactRepository.observeContacts().first()
    return csvContactProcessor.exportContacts(contacts)
  }

  fun reportExportResult(success: Boolean, message: String) {
    transientState.update {
      it.copy(feedbackMessage = message, isFeedbackError = !success)
    }
  }

  fun deleteAllLocalData() {
    viewModelScope.launch {
      historyRepository.clearHistory()
      contactRepository.deleteAllContacts()
      templateRepository.deleteAllTemplates()
      preferencesRepository.resetPreferences()
      transientState.update {
        it.copy(feedbackMessage = "All local data has been deleted.", isFeedbackError = false)
      }
    }
  }
}
