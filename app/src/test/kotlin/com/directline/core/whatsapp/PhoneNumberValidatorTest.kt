package com.directline.core.whatsapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PhoneNumberValidatorTest {

  private lateinit var validator: PhoneNumberValidator

  @Before
  fun setUp() {
    val parser = PhoneNumberParser()
    val normalizer = PhoneNumberNormalizer(parser)
    validator = PhoneNumberValidator(parser, normalizer)
  }

  @Test
  fun `valid Colombian mobile number with country selected normalizes to E164`() {
    val result = validator.validate("300 123 4567", "CO")
    assertTrue(result is PhoneNumberValidationResult.Valid)
    val valid = result as PhoneNumberValidationResult.Valid
    assertEquals("+573001234567", valid.normalizedE164)
    assertEquals("CO", valid.countryIso)
    assertEquals(57, valid.callingCode)
  }

  @Test
  fun `international number with plus prefix works without selected country`() {
    val result = validator.validate("+1 (415) 555-2671", null)
    assertTrue(result is PhoneNumberValidationResult.Valid)
    val valid = result as PhoneNumberValidationResult.Valid
    assertEquals("+14155552671", valid.normalizedE164)
    assertEquals("US", valid.countryIso)
  }

  @Test
  fun `international number with 00 prefix normalizes to E164`() {
    val result = validator.validate("00 57 300-123-4567", null)
    assertTrue(result is PhoneNumberValidationResult.Valid)
    assertEquals("+573001234567", (result as PhoneNumberValidationResult.Valid).normalizedE164)
  }

  @Test
  fun `local number without country selection returns CountryRequired`() {
    val result = validator.validate("3001234567", null)
    assertEquals(PhoneNumberValidationResult.CountryRequired, result)
  }

  @Test
  fun `empty or blank input returns Empty`() {
    assertEquals(PhoneNumberValidationResult.Empty, validator.validate("", "CO"))
    assertEquals(PhoneNumberValidationResult.Empty, validator.validate("   ", "US"))
  }

  @Test
  fun `invalid phone number is never converted to valid`() {
    val result = validator.validate("12345", "CO")
    assertTrue(result is PhoneNumberValidationResult.Invalid)
  }

  @Test
  fun `non-numeric input returns Invalid`() {
    val result = validator.validate("hello world", "CO")
    assertTrue(result is PhoneNumberValidationResult.Invalid)
  }

  @Test
  fun `CountryInfo computes regional indicator flag emoji for alpha-2 codes`() {
    assertEquals("\uD83C\uDDE8\uD83C\uDDF4", CountryInfo("CO", "Colombia", 57).flagEmoji)
    assertEquals("\uD83C\uDDFA\uD83C\uDDF8", CountryInfo("us", "United States", 1).flagEmoji)
    assertEquals("", CountryInfo("001", "World", 800).flagEmoji)
  }
}
