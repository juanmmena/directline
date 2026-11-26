package com.directline.core.whatsapp

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class CountryInfo(
  val isoCode: String,
  val name: String,
  val callingCode: Int,
) {
  val formattedCallingCode: String get() = "+$callingCode"

  val flagEmoji: String
    get() {
      val code = isoCode.trim().uppercase(Locale.ROOT)
      if (code.length != 2 || !code[0].isLetter() || !code[1].isLetter()) return ""
      val first = Character.toChars(0x1F1E6 + (code[0] - 'A'))
      val second = Character.toChars(0x1F1E6 + (code[1] - 'A'))
      return String(first) + String(second)
    }
}

sealed interface PhoneNumberValidationResult {
  data class Valid(
    val rawInput: String,
    val normalizedE164: String,
    val countryIso: String,
    val callingCode: Int,
  ) : PhoneNumberValidationResult

  data object Empty : PhoneNumberValidationResult

  data object CountryRequired : PhoneNumberValidationResult

  data class Invalid(
    val rawInput: String,
    val reason: PhoneValidationError,
  ) : PhoneNumberValidationResult
}

enum class PhoneValidationError {
  TOO_SHORT,
  TOO_LONG,
  INVALID_COUNTRY_CODE,
  INVALID_FORMAT,
  NOT_A_NUMBER,
}

@Singleton
class CountryCodeProvider @Inject constructor() {
  private val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

  val allCountries: List<CountryInfo> by lazy {
    phoneUtil.supportedRegions
      .mapNotNull { region ->
        val code = phoneUtil.getCountryCodeForRegion(region)
        if (code <= 0) {
          null
        } else {
          val displayName = Locale.Builder().setRegion(region).build().getDisplayCountry(Locale.getDefault())
          CountryInfo(
            isoCode = region,
            name = displayName.ifBlank { region },
            callingCode = code,
          )
        }
      }
      .sortedBy { it.name }
  }

  fun findByIso(isoCode: String?): CountryInfo? {
    if (isoCode.isNullOrBlank()) return null
    val normalized = isoCode.trim().uppercase(Locale.ROOT)
    return allCountries.firstOrNull { it.isoCode == normalized }
  }

  fun search(query: String): List<CountryInfo> {
    val q = query.trim()
    if (q.isEmpty()) return allCountries
    val stripped = q.removePrefix("+")
    return allCountries.filter { country ->
      country.name.contains(q, ignoreCase = true) ||
        country.isoCode.equals(q, ignoreCase = true) ||
        country.callingCode.toString().startsWith(stripped)
    }
  }
}

@Singleton
class PhoneNumberParser @Inject constructor() {
  private val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

  fun parse(rawInput: String, regionIso: String?): Phonenumber.PhoneNumber {
    val sanitized = sanitizeForParsing(rawInput)
    val effectiveRegion = regionIso?.trim()?.uppercase(Locale.ROOT)?.ifBlank { null }
    return phoneUtil.parse(sanitized, effectiveRegion)
  }

  fun isValid(number: Phonenumber.PhoneNumber): Boolean = phoneUtil.isValidNumber(number)

  fun getRegionCode(number: Phonenumber.PhoneNumber): String =
    phoneUtil.getRegionCodeForNumber(number) ?: ""

  fun formatE164(number: Phonenumber.PhoneNumber): String =
    phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164)

  private fun sanitizeForParsing(raw: String): String {
    val trimmed = raw.trim()
    // Convert leading 00 international prefix to +
    return if (trimmed.startsWith("00") && trimmed.length > 4) {
      "+" + trimmed.substring(2)
    } else {
      trimmed
    }
  }
}

@Singleton
class PhoneNumberNormalizer @Inject constructor(
  private val parser: PhoneNumberParser,
) {
  fun toE164(number: Phonenumber.PhoneNumber): String = parser.formatE164(number)
}

@Singleton
class PhoneNumberValidator @Inject constructor(
  private val parser: PhoneNumberParser,
  private val normalizer: PhoneNumberNormalizer,
) {
  fun validate(rawInput: String, selectedCountryIso: String?): PhoneNumberValidationResult {
    val trimmed = rawInput.trim()
    if (trimmed.isEmpty()) {
      return PhoneNumberValidationResult.Empty
    }

    val hasExplicitPrefix = trimmed.startsWith("+") || (trimmed.startsWith("00") && trimmed.length > 4)
    val country = selectedCountryIso?.trim()?.uppercase(Locale.ROOT)?.ifBlank { null }

    // BR-03: Never silently assume a country when an ambiguous local number has no selected country.
    if (!hasExplicitPrefix && country == null) {
      return PhoneNumberValidationResult.CountryRequired
    }

    return try {
      val parsed = parser.parse(trimmed, if (hasExplicitPrefix) null else country)
      if (!parser.isValid(parsed)) {
        PhoneNumberValidationResult.Invalid(
          rawInput = rawInput,
          reason = PhoneValidationError.INVALID_FORMAT,
        )
      } else {
        val e164 = normalizer.toE164(parsed)
        val resolvedIso = parser.getRegionCode(parsed).ifBlank { country.orEmpty() }
        PhoneNumberValidationResult.Valid(
          rawInput = rawInput,
          normalizedE164 = e164,
          countryIso = resolvedIso,
          callingCode = parsed.countryCode,
        )
      }
    } catch (e: NumberParseException) {
      val reason = when (e.errorType) {
        NumberParseException.ErrorType.INVALID_COUNTRY_CODE -> PhoneValidationError.INVALID_COUNTRY_CODE
        NumberParseException.ErrorType.TOO_SHORT_AFTER_IDD,
        NumberParseException.ErrorType.TOO_SHORT_NSN,
        -> PhoneValidationError.TOO_SHORT
        NumberParseException.ErrorType.TOO_LONG -> PhoneValidationError.TOO_LONG
        NumberParseException.ErrorType.NOT_A_NUMBER -> PhoneValidationError.NOT_A_NUMBER
      }
      PhoneNumberValidationResult.Invalid(
        rawInput = rawInput,
        reason = reason,
      )
    }
  }
}
