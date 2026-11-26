package com.directline.core.csv

import com.directline.core.whatsapp.PhoneNumberValidationResult
import com.directline.core.whatsapp.PhoneNumberValidator
import com.directline.domain.model.Contact
import javax.inject.Inject
import javax.inject.Singleton

data class CsvParsedContact(
  val rowNumber: Int,
  val name: String,
  val rawPhone: String,
  val normalizedPhone: String,
  val countryIso: String,
  val tags: List<String>,
  val notes: String,
)

data class CsvRowError(
  val rowNumber: Int,
  val message: String,
)

data class CsvImportPreview(
  val validContacts: List<CsvParsedContact>,
  val duplicatePhoneRows: List<CsvRowError>,
  val invalidRows: List<CsvRowError>,
)

@Singleton
class CsvContactProcessor @Inject constructor(
  private val phoneNumberValidator: PhoneNumberValidator,
) {
  companion object {
    const val HEADER = "name,phone_number,tags,notes"
  }

  fun exportContacts(contacts: List<Contact>): String {
    val sb = StringBuilder()
    sb.appendLine(HEADER)
    for (contact in contacts) {
      val tagsString = contact.tags.joinToString(";") { it.name }
      if (contact.phoneNumbers.isEmpty()) {
        sb.appendLine(
          listOf(contact.name, "", tagsString, contact.notes).joinToString(",") { escapeCsv(it) },
        )
      } else {
        for (phone in contact.phoneNumbers) {
          sb.appendLine(
            listOf(
              contact.name,
              phone.normalizedValue,
              tagsString,
              contact.notes,
            ).joinToString(",") { escapeCsv(it) },
          )
        }
      }
    }
    return sb.toString()
  }

  fun parseAndValidate(
    csvContent: String,
    defaultCountryIso: String?,
    existingNormalizedPhones: Set<String> = emptySet(),
  ): CsvImportPreview {
    val lines = csvContent
      .replace("\r\n", "\n")
      .replace("\r", "\n")
      .split("\n")
      .filter { it.isNotBlank() }

    if (lines.isEmpty()) {
      return CsvImportPreview(
        validContacts = emptyList(),
        duplicatePhoneRows = emptyList(),
        invalidRows = listOf(CsvRowError(1, "CSV file is empty.")),
      )
    }

    val startIndex = if (lines.first().trim().lowercase().startsWith("name,")) 1 else 0
    val valid = mutableListOf<CsvParsedContact>()
    val duplicates = mutableListOf<CsvRowError>()
    val errors = mutableListOf<CsvRowError>()
    val seenPhones = existingNormalizedPhones.toMutableSet()

    for (i in startIndex until lines.size) {
      val rowNum = i + 1
      val fields = parseCsvLine(lines[i])
      if (fields.size < 2) {
        errors.add(CsvRowError(rowNum, "Expected at least name and phone_number columns."))
        continue
      }

      val name = fields[0].trim()
      val rawPhone = fields[1].trim()
      val tagsRaw = fields.getOrNull(2)?.trim().orEmpty()
      val notes = fields.getOrNull(3)?.trim().orEmpty()

      if (name.isEmpty()) {
        errors.add(CsvRowError(rowNum, "Missing contact name."))
        continue
      }

      when (val validation = phoneNumberValidator.validate(rawPhone, defaultCountryIso)) {
        is PhoneNumberValidationResult.Valid -> {
          if (validation.normalizedE164 in seenPhones) {
            duplicates.add(CsvRowError(rowNum, "Duplicate phone number skipped."))
          } else {
            seenPhones.add(validation.normalizedE164)
            val tags = tagsRaw.split(";", "|")
              .map { it.trim() }
              .filter { it.isNotEmpty() }
              .distinct()
            valid.add(
              CsvParsedContact(
                rowNumber = rowNum,
                name = name,
                rawPhone = rawPhone,
                normalizedPhone = validation.normalizedE164,
                countryIso = validation.countryIso,
                tags = tags,
                notes = notes,
              ),
            )
          }
        }
        is PhoneNumberValidationResult.Empty -> {
          errors.add(CsvRowError(rowNum, "Missing phone number."))
        }
        is PhoneNumberValidationResult.CountryRequired -> {
          errors.add(CsvRowError(rowNum, "Phone number missing country code (select a default country or use +prefix)."))
        }
        is PhoneNumberValidationResult.Invalid -> {
          errors.add(CsvRowError(rowNum, "Invalid phone number format."))
        }
      }
    }

    return CsvImportPreview(
      validContacts = valid,
      duplicatePhoneRows = duplicates,
      invalidRows = errors,
    )
  }

  private fun escapeCsv(value: String): String {
    val needsQuoting = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
    return if (needsQuoting) {
      "\"${value.replace("\"", "\"\"")}\""
    } else {
      value
    }
  }

  private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
      val c = line[i]
      if (inQuotes) {
        if (c == '"') {
          if (i + 1 < line.length && line[i + 1] == '"') {
            current.append('"')
            i++
          } else {
            inQuotes = false
          }
        } else {
          current.append(c)
        }
      } else {
        when (c) {
          '"' -> inQuotes = true
          ',' -> {
            result.add(current.toString())
            current.clear()
          }
          else -> current.append(c)
        }
      }
      i++
    }
    result.add(current.toString())
    return result
  }
}
