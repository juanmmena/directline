package com.directline.core.template

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateProcessorTest {

  private val processor = TemplateProcessor()

  @Test
  fun `extracts distinct variable keys`() {
    val template = "Hello {{name}}! Your {{product}} costs {{price}}. Thanks {{name}}."
    assertEquals(listOf("name", "product", "price"), processor.extractVariableKeys(template))
  }

  @Test
  fun `replaces all variables when values provided`() {
    val template = "Hello {{name}}! Price for {{product}} is {{price}} on {{date}}."
    val result = processor.process(
      templateContent = template,
      values = mapOf(
        "name" to "Maria",
        "product" to "Directline Pro",
        "price" to "$25",
        "date" to "2026-11-25",
      ),
    )
    assertTrue(result is TemplateProcessResult.Success)
    assertEquals(
      "Hello Maria! Price for Directline Pro is $25 on 2026-11-25.",
      (result as TemplateProcessResult.Success).renderedMessage,
    )
  }

  @Test
  fun `detects missing variables and prevents unresolved placeholders`() {
    val template = "Hello {{name}}, regarding {{product}} ({{price}})."
    val result = processor.process(
      templateContent = template,
      values = mapOf("name" to "John", "product" to "   "),
    )
    assertTrue(result is TemplateProcessResult.MissingVariables)
    val missing = result as TemplateProcessResult.MissingVariables
    assertEquals(listOf("product", "price"), missing.missingKeys)
  }
}
