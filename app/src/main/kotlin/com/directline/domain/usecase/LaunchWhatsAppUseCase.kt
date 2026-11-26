package com.directline.domain.usecase

import com.directline.core.whatsapp.WhatsAppLaunchResult
import com.directline.core.whatsapp.WhatsAppLauncher
import com.directline.domain.repository.ContactRepository
import com.directline.domain.repository.HistoryRepository
import javax.inject.Inject

class LaunchWhatsAppUseCase @Inject constructor(
  private val whatsAppLauncher: WhatsAppLauncher,
  private val contactRepository: ContactRepository,
  private val historyRepository: HistoryRepository,
) {
  suspend operator fun invoke(
    rawPhoneNumber: String,
    selectedCountryIso: String?,
    message: String? = null,
    contactId: String? = null,
    contactName: String? = null,
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
        explicitContactId = contactId,
        explicitContactName = contactName,
      )
    }
    return result
  }

  suspend fun launchNormalized(
    normalizedE164: String,
    message: String? = null,
    contactId: String? = null,
    contactName: String? = null,
  ): WhatsAppLaunchResult {
    val result = whatsAppLauncher.launchNormalized(
      normalizedE164 = normalizedE164,
      message = message,
    )
    if (result is WhatsAppLaunchResult.LaunchInitiated) {
      recordAttempt(
        normalizedPhone = result.destination.normalizedPhoneE164,
        message = message,
        explicitContactId = contactId,
        explicitContactName = contactName,
      )
    }
    return result
  }

  private suspend fun recordAttempt(
    normalizedPhone: String,
    message: String?,
    explicitContactId: String?,
    explicitContactName: String?,
  ) {
    val matchedContact = if (explicitContactId == null) {
      contactRepository.findContactByNormalizedPhone(normalizedPhone)
    } else {
      null
    }

    val resolvedContactId = explicitContactId ?: matchedContact?.id
    val resolvedContactName = explicitContactName ?: matchedContact?.name
    val preview = message?.trim()?.ifBlank { null }?.take(120)

    historyRepository.recordLaunchAttempt(
      contactId = resolvedContactId,
      contactNameSnapshot = resolvedContactName,
      phoneNumberSnapshot = normalizedPhone,
      messagePreview = preview,
    )
  }
}
