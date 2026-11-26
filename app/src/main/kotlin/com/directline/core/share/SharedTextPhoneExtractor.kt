package com.directline.core.share

import javax.inject.Inject
import javax.inject.Singleton

data class SharedTextExtractionResult(
  val originalText: String,
  val extractedPhoneNumber: String?,
  val extractedMessage: String?,
)

@Singleton
class SharedTextPhoneExtractor @Inject constructor() {
  companion object {
    private val WA_ME_REGEX = Regex("https?://(?:www\\.)?wa\\.me/(\\d{7,15})(?:\\?.*)?", RegexOption.IGNORE_CASE)
    private val TEL_URI_REGEX = Regex("tel:([+\\d().\\-\\s]{7,25})", RegexOption.IGNORE_CASE)
    private val PHONE_CANDIDATE_REGEX = Regex("(?:\\+|00)?\\d[\\d\\s().\\-]{5,20}\\d")
  }

  fun extract(sharedText: String?): SharedTextExtractionResult {
    val raw = sharedText?.trim().orEmpty()
    if (raw.isEmpty()) {
      return SharedTextExtractionResult(
        originalText = "",
        extractedPhoneNumber = null,
        extractedMessage = null,
      )
    }

    // 1. Check wa.me URL
    WA_ME_REGEX.find(raw)?.let { match ->
      val digits = match.groupValues[1]
      return SharedTextExtractionResult(
        originalText = raw,
        extractedPhoneNumber = "+$digits",
        extractedMessage = null,
      )
    }

    // 2. Check tel: URI
    TEL_URI_REGEX.find(raw)?.let { match ->
      return SharedTextExtractionResult(
        originalText = raw,
        extractedPhoneNumber = match.groupValues[1].trim(),
        extractedMessage = null,
      )
    }

    // 3. Extract first plausible phone number sequence from plain text
    PHONE_CANDIDATE_REGEX.find(raw)?.let { match ->
      val candidate = match.value.trim()
      val digitCount = candidate.count { it.isDigit() }
      if (digitCount in 7..15) {
        return SharedTextExtractionResult(
          originalText = raw,
          extractedPhoneNumber = candidate,
          extractedMessage = null,
        )
      }
    }

    return SharedTextExtractionResult(
      originalText = raw,
      extractedPhoneNumber = null,
      extractedMessage = null,
    )
  }
}
