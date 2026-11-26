package com.directline.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.directline.core.whatsapp.CountryCodeProvider
import com.directline.core.whatsapp.CountryInfo
import com.directline.domain.model.ThemeMode
import com.directline.domain.model.UserPreferences
import com.directline.domain.repository.HistoryRepository
import com.directline.domain.repository.PreferencesRepository
import com.directline.domain.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
  val preferences: UserPreferences = UserPreferences(),
  val defaultCountry: CountryInfo? = null,
  val feedbackMessage: String? = null,
  val isFeedbackError: Boolean = false,
)

private data class SettingsTransientState(
  val feedbackMessage: String? = null,
  val isFeedbackError: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
  private val preferencesRepository: PreferencesRepository,
  private val templateRepository: TemplateRepository,
  private val historyRepository: HistoryRepository,
  private val countryCodeProvider: CountryCodeProvider,
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

  fun deleteAllLocalData() {
    viewModelScope.launch {
      historyRepository.clearHistory()
      templateRepository.deleteAllTemplates()
      preferencesRepository.resetPreferences()
      transientState.update {
        it.copy(feedbackMessage = "All local data has been deleted.", isFeedbackError = false)
      }
    }
  }
}
