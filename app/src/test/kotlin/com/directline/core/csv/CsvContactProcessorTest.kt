package com.directline.core.csv

import com.directline.core.whatsapp.PhoneNumberNormalizer
import com.directline.core.whatsapp.PhoneNumberParser
import com.directline.core.whatsapp.PhoneNumberValidator
import com.directline.domain.model.Contact
import com.directline.domain.model.PhoneNumber
import com.directline.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CsvContactProcessorTest {

  private lateinit var processor: CsvContactProcessor

  @Before
  fun setUp() {
    val parser = PhoneNumberParser()
    val validator = PhoneNumberValidator(parser, PhoneNumberNormalizer(parser))
    processor = CsvContactProcessor(validator)
  }

  @Test
  fun `parses valid CSV rows reports invalid rows and skips duplicates`() {
    val csv = """
      name,phone_number,tags,notes
      Maria,+573001234567,Customer;VIP,"Interested in premium, fast shipping"
      John,+14155552671,Personal,Friend
      DuplicateMaria,+57 300 123 4567,Customer,Duplicate
      BrokenRow,not-a-phone,Tag,Note
    """.trimIndent()

    val preview = processor.parseAndValidate(csv, defaultCountryIso = null)
    assertEquals(2, preview.validContacts.size)
    assertEquals(1, preview.duplicatePhoneRows.size)
    assertEquals(1, preview.invalidRows.size)

    val maria = preview.validContacts[0]
    assertEquals("Maria", maria.name)
    assertEquals("+573001234567", maria.normalizedPhone)
    assertEquals(listOf("Customer", "VIP"), maria.tags)
    assertEquals("Interested in premium, fast shipping", maria.notes)
  }

  @Test
  fun `exported CSV can be imported again cleanly`() {
    val contact = Contact(
      id = "c1",
      name = "Ana",
      notes = "Prefers WhatsApp, afternoons",
      isFavorite = true,
      phoneNumbers = listOf(
        PhoneNumber("p1", "c1", "+57 300 123 4567", "+573001234567", "CO", 1L),
      ),
      tags = listOf(Tag("t1", "Client")),
      createdAt = 1L,
      updatedAt = 1L,
    )

    val exported = processor.exportContacts(listOf(contact))
    val reimported = processor.parseAndValidate(exported, defaultCountryIso = null)
    assertTrue(reimported.invalidRows.isEmpty())
    assertEquals(1, reimported.validContacts.size)
    assertEquals("Ana", reimported.validContacts.first().name)
    assertEquals("+573001234567", reimported.validContacts.first().normalizedPhone)
    assertEquals("Prefers WhatsApp, afternoons", reimported.validContacts.first().notes)
  }
}
