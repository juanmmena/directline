package com.directline.domain.repository

import com.directline.domain.model.Contact
import com.directline.domain.model.InteractionHistory
import com.directline.domain.model.MessageTemplate
import com.directline.domain.model.Tag
import com.directline.domain.model.TemplateCategory
import com.directline.domain.model.ThemeMode
import com.directline.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
  fun observeContacts(): Flow<List<Contact>>
  fun observeFavorites(): Flow<List<Contact>>
  fun observeTags(): Flow<List<Tag>>
  suspend fun getContactById(id: String): Contact?
  suspend fun findContactByNormalizedPhone(normalizedPhone: String): Contact?
  suspend fun saveContact(
    id: String?,
    name: String,
    notes: String,
    isFavorite: Boolean,
    phoneNumbers: List<PhoneNumberDraft>,
    tagNames: List<String>,
  ): String
  suspend fun toggleFavorite(contactId: String, isFavorite: Boolean)
  suspend fun deleteContact(contactId: String)
  suspend fun deleteAllContacts()
}

data class PhoneNumberDraft(
  val rawValue: String,
  val normalizedValue: String,
  val countryCode: String,
)

interface TemplateRepository {
  fun observeTemplates(): Flow<List<MessageTemplate>>
  fun observeCategories(): Flow<List<TemplateCategory>>
  suspend fun getTemplateById(id: String): MessageTemplate?
  suspend fun saveTemplate(
    id: String?,
    title: String,
    content: String,
    categoryName: String?,
  ): String
  suspend fun duplicateTemplate(templateId: String): String?
  suspend fun deleteTemplate(templateId: String)
  suspend fun deleteAllTemplates()
}

interface HistoryRepository {
  fun observeHistory(): Flow<List<InteractionHistory>>
  fun observeRecentHistory(limit: Int = 5): Flow<List<InteractionHistory>>
  suspend fun recordLaunchAttempt(
    contactId: String?,
    contactNameSnapshot: String?,
    phoneNumberSnapshot: String,
    messagePreview: String?,
  )
  suspend fun deleteHistoryItem(id: String)
  suspend fun clearHistory()
}

interface PreferencesRepository {
  fun observePreferences(): Flow<UserPreferences>
  suspend fun setThemeMode(themeMode: ThemeMode)
  suspend fun setDefaultCountryIso(countryIso: String)
  suspend fun setDefaultMessagePrefix(prefix: String)
  suspend fun resetPreferences()
}
