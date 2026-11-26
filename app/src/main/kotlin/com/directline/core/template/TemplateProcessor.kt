package com.directline.core.template

import javax.inject.Inject
import javax.inject.Singleton

enum class TemplateVariable(val key: String, val placeholder: String, val label: String) {
  NAME("name", "{{name}}", "Name"),
  PHONE("phone", "{{phone}}", "Phone"),
  PRODUCT("product", "{{product}}", "Product"),
  PRICE("price", "{{price}}", "Price"),
  DATE("date", "{{date}}", "Date"),
}

sealed interface TemplateProcessResult {
  data class Success(val renderedMessage: String) : TemplateProcessResult
  data class MissingVariables(
    val missingKeys: List<String>,
    val partialPreview: String,
  ) : TemplateProcessResult
}

@Singleton
class TemplateProcessor @Inject constructor() {
  companion object {
    private val PLACEHOLDER_REGEX = Regex("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*\\}\\}")
  }

  fun extractVariableKeys(templateContent: String): List<String> {
    return PLACEHOLDER_REGEX.findAll(templateContent)
      .map { it.groupValues[1].lowercase() }
      .distinct()
      .toList()
  }

  fun process(
    templateContent: String,
    values: Map<String, String>,
  ): TemplateProcessResult {
    val normalizedValues = values
      .mapKeys { it.key.trim().lowercase() }
      .mapValues { it.value.trim() }

    val missing = mutableListOf<String>()
    val rendered = PLACEHOLDER_REGEX.replace(templateContent) { match ->
      val key = match.groupValues[1].lowercase()
      val replacement = normalizedValues[key]
      if (replacement.isNullOrBlank()) {
        if (key !in missing) missing.add(key)
        match.value
      } else {
        replacement
      }
    }

    return if (missing.isEmpty() && !PLACEHOLDER_REGEX.containsMatchIn(rendered)) {
      TemplateProcessResult.Success(rendered)
    } else {
      TemplateProcessResult.MissingVariables(
        missingKeys = missing,
        partialPreview = rendered,
      )
    }
  }
}
