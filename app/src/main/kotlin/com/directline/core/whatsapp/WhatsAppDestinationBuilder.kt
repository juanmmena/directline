package com.directline.core.whatsapp

import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

data class WhatsAppDestination(
  val normalizedPhoneE164: String,
  val url: String,
  val encodedMessage: String?,
)

sealed interface DestinationValidationResult {
  data class Valid(val destination: WhatsAppDestination) : DestinationValidationResult
  data object InvalidPhoneNumber : DestinationValidationResult
  data class MessageTooLong(val maxLength: Int, val actualLength: Int) : DestinationValidationResult
}

@Singleton
class MessageEncoder @Inject constructor() {
  fun encode(message: String): String {
    return URLEncoder.encode(message, Charsets.UTF_8.name())
      .replace("+", "%20")
  }
}

@Singleton
class WhatsAppDestinationBuilder @Inject constructor(
  private val messageEncoder: MessageEncoder,
) {
  companion object {
    const val BASE_URL = "https://wa.me/"
    const val MAX_MESSAGE_LENGTH = 4096
    private val E164_REGEX = Regex("^\\+[1-9]\\d{6,14}$")
  }

  fun build(normalizedPhoneE164: String, message: String? = null): DestinationValidationResult {
    val trimmedPhone = normalizedPhoneE164.trim()
    if (!E164_REGEX.matches(trimmedPhone)) {
      return DestinationValidationResult.InvalidPhoneNumber
    }

    val digitsOnly = trimmedPhone.removePrefix("+")
    val cleanedMessage = message?.trim()?.ifBlank { null }

    if (cleanedMessage != null && cleanedMessage.length > MAX_MESSAGE_LENGTH) {
      return DestinationValidationResult.MessageTooLong(
        maxLength = MAX_MESSAGE_LENGTH,
        actualLength = cleanedMessage.length,
      )
    }

    val encoded = cleanedMessage?.let { messageEncoder.encode(it) }
    val url = if (encoded != null) {
      "$BASE_URL$digitsOnly?text=$encoded"
    } else {
      "$BASE_URL$digitsOnly"
    }

    return DestinationValidationResult.Valid(
      WhatsAppDestination(
        normalizedPhoneE164 = trimmedPhone,
        url = url,
        encodedMessage = encoded,
      ),
    )
  }
}
