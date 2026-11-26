package com.directline.core.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedTextPhoneExtractorTest {

  private val extractor = SharedTextPhoneExtractor()

  @Test
  fun `extracts phone from wa me URL`() {
    val res = extractor.extract("Check out https://wa.me/573001234567?text=Hello")
    assertEquals("+573001234567", res.extractedPhoneNumber)
  }

  @Test
  fun `extracts phone from tel URI`() {
    val res = extractor.extract("tel:+14155552671")
    assertEquals("+14155552671", res.extractedPhoneNumber)
  }

  @Test
  fun `extracts formatted phone embedded in plain text`() {
    val res = extractor.extract("Call Carlos at +57 (300) 123-4567 tomorrow")
    assertEquals("+57 (300) 123-4567", res.extractedPhoneNumber)
  }

  @Test
  fun `returns null when no phone is present`() {
    val res = extractor.extract("Just a plain message without numbers")
    assertNull(res.extractedPhoneNumber)
  }
}
