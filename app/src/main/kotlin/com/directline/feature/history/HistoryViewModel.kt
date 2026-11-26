package com.directline.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.directline.core.whatsapp.WhatsAppLaunchResult
import com.directline.domain.model.InteractionHistory
import com.directline.domain.repository.HistoryRepository
import com.directline.domain.usecase.LaunchWhatsAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
  val items: List<InteractionHistory> = emptyList(),
  val feedbackMessage: String? = null,
  val isFeedbackError: Boolean = false,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
  private val historyRepository: HistoryRepository,
  private val launchWhatsAppUseCase: LaunchWhatsAppUseCase,
) : ViewModel() {

  private val feedback = MutableStateFlow<Pair<String?, Boolean>>(null to false)

  val uiState: StateFlow<HistoryUiState> = combine(
    historyRepository.observeHistory(),
    feedback,
  ) { items, (msg, isErr) ->
    HistoryUiState(
      items = items,
      feedbackMessage = msg,
      isFeedbackError = isErr,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = HistoryUiState(),
  )

  fun dismissFeedback() {
    feedback.value = null to false
  }

  fun reopenDestination(item: InteractionHistory) {
    viewModelScope.launch {
      val result = launchWhatsAppUseCase.launchNormalized(
        normalizedE164 = item.phoneNumberSnapshot,
        message = item.messagePreview,
        contactId = item.contactId,
        contactName = item.contactNameSnapshot,
      )
      feedback.update {
        when (result) {
          is WhatsAppLaunchResult.LaunchInitiated -> "Reopening WhatsApp for ${item.phoneNumberSnapshot}." to false
          is WhatsAppLaunchResult.ApplicationUnavailable -> "No compatible WhatsApp app or browser found." to true
          is WhatsAppLaunchResult.InvalidDestination -> result.reason to true
          is WhatsAppLaunchResult.LaunchFailed -> result.reason to true
          is WhatsAppLaunchResult.InvalidPhoneNumber -> "Phone number snapshot is invalid." to true
        }
      }
    }
  }

  fun deleteItem(id: String) {
    viewModelScope.launch {
      historyRepository.deleteHistoryItem(id)
    }
  }

  fun clearHistory() {
    viewModelScope.launch {
      historyRepository.clearHistory()
      feedback.value = "Interaction history cleared." to false
    }
  }
}
