package com.directline.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.directline.core.share.SharedTextPhoneExtractor
import com.directline.core.template.TemplateProcessResult
import com.directline.core.template.TemplateProcessor
import com.directline.core.whatsapp.CountryCodeProvider
import com.directline.core.whatsapp.CountryInfo
import com.directline.core.whatsapp.PhoneNumberValidationResult
import com.directline.core.whatsapp.PhoneNumberValidator
import com.directline.core.whatsapp.PhoneValidationError
import com.directline.core.whatsapp.WhatsAppLaunchResult
import com.directline.domain.model.InteractionHistory
import com.directline.domain.model.MessageTemplate
import com.directline.domain.repository.HistoryRepository
import com.directline.domain.repository.PreferencesRepository
import com.directline.domain.repository.TemplateRepository
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

data class HomeUiState(
  val phoneInput: String = "",
  val selectedCountry: CountryInfo? = null,
  val messageInput: String = "",
  val validationResult: PhoneNumberValidationResult = PhoneNumberValidationResult.Empty,
  val feedbackMessage: String? = null,
  val isFeedbackError: Boolean = false,
  val incomingSharedNotice: String? = null,
  val recentHistory: List<InteractionHistory> = emptyList(),
  val templates: List<MessageTemplate> = emptyList(),
)

private data class HomeFormState(
  val phoneInput: String = "",
  val selectedCountry: CountryInfo? = null,
  val messageInput: String = "",
  val feedbackMessage: String? = null,
  val isFeedbackError: Boolean = false,
  val incomingSharedNotice: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
  private val countryCodeProvider: CountryCodeProvider,
  private val phoneNumberValidator: PhoneNumberValidator,
  private val templateProcessor: TemplateProcessor,
  private val sharedTextPhoneExtractor: SharedTextPhoneExtractor,
  private val launchWhatsAppUseCase: LaunchWhatsAppUseCase,
  private val historyRepository: HistoryRepository,
  private val templateRepository: TemplateRepository,
  private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

  val countries: List<CountryInfo> = countryCodeProvider.allCountries

  private val formState = MutableStateFlow(HomeFormState())

  val uiState: StateFlow<HomeUiState> = combine(
    formState,
    historyRepository.observeRecentHistory(limit = 5),
    templateRepository.observeTemplates(),
  ) { form, recent, templates ->
    val validation = phoneNumberValidator.validate(
      rawInput = form.phoneInput,
      selectedCountryIso = form.selectedCountry?.isoCode,
    )
    HomeUiState(
      phoneInput = form.phoneInput,
      selectedCountry = form.selectedCountry,
      messageInput = form.messageInput,
      validationResult = validation,
      feedbackMessage = form.feedbackMessage,
      isFeedbackError = form.isFeedbackError,
      incomingSharedNotice = form.incomingSharedNotice,
      recentHistory = recent,
      templates = templates,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = HomeUiState(),
  )

  init {
    viewModelScope.launch {
      val prefs = preferencesRepository.observePreferences().first()
      val defaultCountry = countryCodeProvider.findByIso(prefs.defaultCountryIso)
      formState.update { current ->
        current.copy(
          selectedCountry = current.selectedCountry ?: defaultCountry,
          messageInput = current.messageInput.ifBlank { prefs.defaultMessagePrefix },
        )
      }
    }
  }

  fun onPhoneInputChanged(value: String) {
    formState.update { it.copy(phoneInput = value, feedbackMessage = null) }
  }

  fun onCountrySelected(country: CountryInfo?) {
    formState.update { it.copy(selectedCountry = country, feedbackMessage = null) }
  }

  fun onMessageInputChanged(value: String) {
    formState.update { it.copy(messageInput = value, feedbackMessage = null) }
  }

  fun clearForm() {
    formState.update {
      it.copy(
        phoneInput = "",
        messageInput = "",
        feedbackMessage = null,
        incomingSharedNotice = null,
      )
    }
  }

  fun dismissFeedback() {
    formState.update { it.copy(feedbackMessage = null) }
  }

  fun dismissSharedNotice() {
    formState.update { it.copy(incomingSharedNotice = null) }
  }

  fun applyIncomingSharedText(sharedText: String) {
    val extracted = sharedTextPhoneExtractor.extract(sharedText)
    if (extracted.extractedPhoneNumber != null) {
      formState.update {
        it.copy(
          phoneInput = extracted.extractedPhoneNumber,
          incomingSharedNotice = "Shared number loaded for review. Verify the number and country before opening WhatsApp.",
          feedbackMessage = null,
        )
      }
    } else {
      formState.update {
        it.copy(
          incomingSharedNotice = "No valid phone number was found in the shared content.",
          feedbackMessage = null,
        )
      }
    }
  }

  fun prefillFromHistory(phoneE164: String, message: String? = null) {
    formState.update {
      it.copy(
        phoneInput = phoneE164,
        messageInput = message ?: it.messageInput,
        feedbackMessage = null,
      )
    }
  }

  fun applyRenderedTemplate(renderedMessage: String) {
    formState.update {
      it.copy(
        messageInput = renderedMessage,
        feedbackMessage = "Template applied to message.",
        isFeedbackError = false,
      )
    }
  }

  fun extractTemplateVariables(content: String): List<String> =
    templateProcessor.extractVariableKeys(content)

  fun processTemplate(content: String, values: Map<String, String>): TemplateProcessResult =
    templateProcessor.process(content, values)

  fun launchWhatsApp() {
    val current = formState.value
    viewModelScope.launch {
      val result = launchWhatsAppUseCase(
        rawPhoneNumber = current.phoneInput,
        selectedCountryIso = current.selectedCountry?.isoCode,
        message = current.messageInput,
      )
      handleLaunchResult(result)
    }
  }

  fun launchNormalizedNumber(normalizedE164: String) {
    viewModelScope.launch {
      val result = launchWhatsAppUseCase.launchNormalized(
        normalizedE164 = normalizedE164,
        message = formState.value.messageInput,
      )
      handleLaunchResult(result)
    }
  }

  private fun handleLaunchResult(result: WhatsAppLaunchResult) {
    val (msg, isError) = when (result) {
      is WhatsAppLaunchResult.LaunchInitiated -> {
        "Opening WhatsApp for ${result.destination.normalizedPhoneE164}. Review and send inside WhatsApp." to false
      }
      is WhatsAppLaunchResult.InvalidPhoneNumber -> {
        validationErrorMessage(result.validationResult) to true
      }
      is WhatsAppLaunchResult.ApplicationUnavailable -> {
        "No compatible app (WhatsApp, WhatsApp Business, or browser) was found to open this link." to true
      }
      is WhatsAppLaunchResult.InvalidDestination -> {
        result.reason to true
      }
      is WhatsAppLaunchResult.LaunchFailed -> {
        result.reason to true
      }
    }
    formState.update { it.copy(feedbackMessage = msg, isFeedbackError = isError) }
  }

  fun validationErrorMessage(result: PhoneNumberValidationResult): String = when (result) {
    is PhoneNumberValidationResult.Empty -> "Enter a phone number to continue."
    is PhoneNumberValidationResult.CountryRequired -> "Select a country or include a '+' international prefix."
    is PhoneNumberValidationResult.Invalid -> when (result.reason) {
      PhoneValidationError.TOO_SHORT -> "Phone number is too short."
      PhoneValidationError.TOO_LONG -> "Phone number is too long."
      PhoneValidationError.INVALID_COUNTRY_CODE -> "Invalid country calling code."
      PhoneValidationError.NOT_A_NUMBER -> "Input does not appear to be a phone number."
      PhoneValidationError.INVALID_FORMAT -> "Phone number format is invalid for the selected country."
    }
    is PhoneNumberValidationResult.Valid -> "Valid E.164: ${result.normalizedE164}"
  }
}
