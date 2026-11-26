package com.directline.domain.usecase

import com.directline.core.whatsapp.WhatsAppLaunchResult
import com.directline.core.whatsapp.WhatsAppLauncher
import com.directline.domain.repository.HistoryRepository
import javax.inject.Inject

class LaunchWhatsAppUseCase @Inject constructor(
  private val whatsAppLauncher: WhatsAppLauncher,
  private val historyRepository: HistoryRepository,
) {
  suspend operator fun invoke(
    rawPhoneNumber: String,
    selectedCountryIso: String?,
    message: String? = null,
  ): WhatsAppLaunchResult {
    val result = whatsAppLauncher.launch(
      rawPhoneNumber = rawPhoneNumber,
      selectedCountryIso = selectedCountryIso,
      message = message,
    )
    if (result is WhatsAppLaunchResult.LaunchInitiated) {
      recordAttempt(
        normalizedPhone = result.destination.normalizedPhoneE164,
        message = message,
      )
    }
    return result
  }

  suspend fun launchNormalized(
    normalizedE164: String,
    message: String? = null,
  ): WhatsAppLaunchResult {
    val result = whatsAppLauncher.launchNormalized(
      normalizedE164 = normalizedE164,
      message = message,
    )
    if (result is WhatsAppLaunchResult.LaunchInitiated) {
      recordAttempt(
        normalizedPhone = result.destination.normalizedPhoneE164,
        message = message,
      )
    }
    return result
  }

  private suspend fun recordAttempt(
    normalizedPhone: String,
    message: String?,
  ) {
    val preview = message?.trim()?.ifBlank { null }?.take(120)
    historyRepository.recordLaunchAttempt(
      phoneNumberSnapshot = normalizedPhone,
      messagePreview = preview,
    )
  }
}
