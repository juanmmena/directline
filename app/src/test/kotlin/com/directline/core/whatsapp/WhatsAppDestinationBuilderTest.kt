package com.directline.core.whatsapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WhatsAppDestinationBuilderTest {

  private lateinit var builder: WhatsAppDestinationBuilder

  @Before
  fun setUp() {
    builder = WhatsAppDestinationBuilder(MessageEncoder())
  }

  @Test
  fun `builds click to chat URL without message`() {
    val result = builder.build("+573001234567", null)
    assertTrue(result is DestinationValidationResult.Valid)
    val dest = (result as DestinationValidationResult.Valid).destination
    assertEquals("https://wa.me/573001234567", dest.url)
    assertNull(dest.encodedMessage)
  }

  @Test
  fun `whitespace-only message is treated as no message`() {
    val result = builder.build("+573001234567", "   \n\t  ")
    assertTrue(result is DestinationValidationResult.Valid)
    assertEquals("https://wa.me/573001234567", (result as DestinationValidationResult.Valid).destination.url)
  }

  @Test
  fun `encodes spaces punctuation accents emoji and line breaks`() {
    val message = "Hola! ¿Cómo estás?\nPrecio: $50 & más 😊"
    val result = builder.build("+573001234567", message)
    assertTrue(result is DestinationValidationResult.Valid)
    val url = (result as DestinationValidationResult.Valid).destination.url
    assertTrue(url.startsWith("https://wa.me/573001234567?text="))
    assertTrue(url.contains("%20"))
    assertTrue(url.contains("%0A"))
    assertTrue(url.contains("%26"))
    assertTrue(!url.contains(" "))
  }

  @Test
  fun `rejects non-E164 phone number`() {
    assertEquals(DestinationValidationResult.InvalidPhoneNumber, builder.build("3001234567", "Hi"))
    assertEquals(DestinationValidationResult.InvalidPhoneNumber, builder.build("+00123", "Hi"))
    assertEquals(DestinationValidationResult.InvalidPhoneNumber, builder.build("", "Hi"))
  }

  @Test
  fun `rejects overly long messages`() {
    val longMsg = "a".repeat(WhatsAppDestinationBuilder.MAX_MESSAGE_LENGTH + 1)
    val result = builder.build("+573001234567", longMsg)
    assertTrue(result is DestinationValidationResult.MessageTooLong)
  }
}
