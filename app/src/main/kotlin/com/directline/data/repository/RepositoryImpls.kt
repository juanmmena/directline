package com.directline.data.repository

import androidx.room.withTransaction
import com.directline.core.database.DirectlineDatabase
import com.directline.core.database.dao.ContactDao
import com.directline.core.database.dao.HistoryDao
import com.directline.core.database.dao.TemplateDao
import com.directline.core.database.entity.ContactEntity
import com.directline.core.database.entity.ContactTagCrossRef
import com.directline.core.database.entity.ContactWithDetails
import com.directline.core.database.entity.InteractionHistoryEntity
import com.directline.core.database.entity.MessageTemplateEntity
import com.directline.core.database.entity.PhoneNumberEntity
import com.directline.core.database.entity.TagEntity
import com.directline.core.database.entity.TemplateCategoryEntity
import com.directline.core.database.entity.TemplateWithCategory
import com.directline.core.datastore.UserPreferencesDataStore
import com.directline.domain.model.Contact
import com.directline.domain.model.InteractionActionType
import com.directline.domain.model.InteractionHistory
import com.directline.domain.model.MessageTemplate
import com.directline.domain.model.PhoneNumber
import com.directline.domain.model.Tag
import com.directline.domain.model.TemplateCategory
import com.directline.domain.model.ThemeMode
import com.directline.domain.model.UserPreferences
import com.directline.domain.repository.ContactRepository
import com.directline.domain.repository.HistoryRepository
import com.directline.domain.repository.PhoneNumberDraft
import com.directline.domain.repository.PreferencesRepository
import com.directline.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private fun ContactWithDetails.toDomain(): Contact = Contact(
  id = contact.id,
  name = contact.name,
  notes = contact.notes,
  isFavorite = contact.isFavorite,
  phoneNumbers = phoneNumbers.map {
    PhoneNumber(
      id = it.id,
      contactId = it.contactId,
      rawValue = it.rawValue,
      normalizedValue = it.normalizedValue,
      countryCode = it.countryCode,
      createdAt = it.createdAt,
    )
  },
  tags = tags.map { Tag(id = it.id, name = it.name) },
  createdAt = contact.createdAt,
  updatedAt = contact.updatedAt,
)

private fun TemplateWithCategory.toDomain(): MessageTemplate = MessageTemplate(
  id = template.id,
  title = template.title,
  content = template.content,
  categoryId = template.categoryId,
  categoryName = category?.name,
  createdAt = template.createdAt,
  updatedAt = template.updatedAt,
)

private fun InteractionHistoryEntity.toDomain(): InteractionHistory = InteractionHistory(
  id = id,
  contactId = contactId,
  contactNameSnapshot = contactNameSnapshot,
  phoneNumberSnapshot = phoneNumberSnapshot,
  messagePreview = messagePreview,
  actionType = InteractionActionType.entries.firstOrNull { it.name == actionType }
    ?: InteractionActionType.WHATSAPP_LAUNCH_ATTEMPT,
  createdAt = createdAt,
)

@Singleton
class ContactRepositoryImpl @Inject constructor(
  private val database: DirectlineDatabase,
  private val contactDao: ContactDao,
) : ContactRepository {

  override fun observeContacts(): Flow<List<Contact>> =
    contactDao.observeContactsWithDetails().map { list -> list.map { it.toDomain() } }

  override fun observeFavorites(): Flow<List<Contact>> =
    contactDao.observeFavoriteContactsWithDetails().map { list -> list.map { it.toDomain() } }

  override fun observeTags(): Flow<List<Tag>> =
    contactDao.observeTags().map { list -> list.map { Tag(id = it.id, name = it.name) } }

  override suspend fun getContactById(id: String): Contact? =
    contactDao.getContactWithDetailsById(id)?.toDomain()

  override suspend fun findContactByNormalizedPhone(normalizedPhone: String): Contact? =
    contactDao.findContactByNormalizedPhone(normalizedPhone)?.toDomain()

  override suspend fun saveContact(
    id: String?,
    name: String,
    notes: String,
    isFavorite: Boolean,
    phoneNumbers: List<PhoneNumberDraft>,
    tagNames: List<String>,
  ): String {
    val now = System.currentTimeMillis()
    val contactId = id ?: UUID.randomUUID().toString()

    database.withTransaction {
      val existing = if (id != null) contactDao.getContactWithDetailsById(id) else null
      contactDao.upsertContact(
        ContactEntity(
          id = contactId,
          name = name.trim(),
          notes = notes.trim(),
          isFavorite = isFavorite,
          createdAt = existing?.contact?.createdAt ?: now,
          updatedAt = now,
        ),
      )

      contactDao.deletePhoneNumbersForContact(contactId)
      val phoneEntities = phoneNumbers.map { draft ->
        PhoneNumberEntity(
          id = UUID.randomUUID().toString(),
          contactId = contactId,
          rawValue = draft.rawValue.trim(),
          normalizedValue = draft.normalizedValue.trim(),
          countryCode = draft.countryCode.trim(),
          createdAt = now,
        )
      }
      if (phoneEntities.isNotEmpty()) {
        contactDao.insertPhoneNumbers(phoneEntities)
      }

      contactDao.deleteTagRefsForContact(contactId)
      val cleanedTags = tagNames.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
      val refs = cleanedTags.map { tagName ->
        val existingTag = contactDao.getTagByName(tagName)
        val tagId = if (existingTag != null) {
          existingTag.id
        } else {
          val newId = UUID.randomUUID().toString()
          contactDao.insertTag(TagEntity(id = newId, name = tagName))
          newId
        }
        ContactTagCrossRef(contactId = contactId, tagId = tagId)
      }
      if (refs.isNotEmpty()) {
        contactDao.insertContactTagRefs(refs)
      }
    }

    return contactId
  }

  override suspend fun toggleFavorite(contactId: String, isFavorite: Boolean) {
    contactDao.updateFavorite(contactId, isFavorite, System.currentTimeMillis())
  }

  override suspend fun deleteContact(contactId: String) {
    contactDao.deleteContact(contactId)
  }

  override suspend fun deleteAllContacts() {
    database.withTransaction {
      contactDao.deleteAllContacts()
      contactDao.deleteAllTags()
    }
  }
}

@Singleton
class TemplateRepositoryImpl @Inject constructor(
  private val database: DirectlineDatabase,
  private val templateDao: TemplateDao,
) : TemplateRepository {

  override fun observeTemplates(): Flow<List<MessageTemplate>> =
    templateDao.observeTemplatesWithCategory().map { list -> list.map { it.toDomain() } }

  override fun observeCategories(): Flow<List<TemplateCategory>> =
    templateDao.observeCategories().map { list ->
      list.map { TemplateCategory(id = it.id, name = it.name, createdAt = it.createdAt) }
    }

  override suspend fun getTemplateById(id: String): MessageTemplate? =
    templateDao.getTemplateWithCategoryById(id)?.toDomain()

  override suspend fun saveTemplate(
    id: String?,
    title: String,
    content: String,
    categoryName: String?,
  ): String {
    val now = System.currentTimeMillis()
    val templateId = id ?: UUID.randomUUID().toString()

    database.withTransaction {
      val existing = if (id != null) templateDao.getTemplateWithCategoryById(id) else null
      val trimmedCategory = categoryName?.trim()?.ifBlank { null }
      val categoryId = if (trimmedCategory != null) {
        val existingCat = templateDao.getCategoryByName(trimmedCategory)
        if (existingCat != null) {
          existingCat.id
        } else {
          val newCatId = UUID.randomUUID().toString()
          templateDao.insertCategory(
            TemplateCategoryEntity(id = newCatId, name = trimmedCategory, createdAt = now),
          )
          newCatId
        }
      } else {
        null
      }

      templateDao.upsertTemplate(
        MessageTemplateEntity(
          id = templateId,
          title = title.trim(),
          content = content,
          categoryId = categoryId,
          createdAt = existing?.template?.createdAt ?: now,
          updatedAt = now,
        ),
      )
    }

    return templateId
  }

  override suspend fun duplicateTemplate(templateId: String): String? {
    val existing = templateDao.getTemplateWithCategoryById(templateId) ?: return null
    val now = System.currentTimeMillis()
    val newId = UUID.randomUUID().toString()
    templateDao.upsertTemplate(
      existing.template.copy(
        id = newId,
        title = "${existing.template.title} (Copy)",
        createdAt = now,
        updatedAt = now,
      ),
    )
    return newId
  }

  override suspend fun deleteTemplate(templateId: String) {
    templateDao.deleteTemplate(templateId)
  }

  override suspend fun deleteAllTemplates() {
    database.withTransaction {
      templateDao.deleteAllTemplates()
      templateDao.deleteAllCategories()
    }
  }
}

@Singleton
class HistoryRepositoryImpl @Inject constructor(
  private val historyDao: HistoryDao,
) : HistoryRepository {

  override fun observeHistory(): Flow<List<InteractionHistory>> =
    historyDao.observeAllHistory().map { list -> list.map { it.toDomain() } }

  override fun observeRecentHistory(limit: Int): Flow<List<InteractionHistory>> =
    historyDao.observeRecentHistory(limit).map { list -> list.map { it.toDomain() } }

  override suspend fun recordLaunchAttempt(
    contactId: String?,
    contactNameSnapshot: String?,
    phoneNumberSnapshot: String,
    messagePreview: String?,
  ) {
    historyDao.insertHistory(
      InteractionHistoryEntity(
        id = UUID.randomUUID().toString(),
        contactId = contactId,
        contactNameSnapshot = contactNameSnapshot,
        phoneNumberSnapshot = phoneNumberSnapshot,
        messagePreview = messagePreview,
        actionType = InteractionActionType.WHATSAPP_LAUNCH_ATTEMPT.name,
        createdAt = System.currentTimeMillis(),
      ),
    )
  }

  override suspend fun deleteHistoryItem(id: String) {
    historyDao.deleteHistoryItem(id)
  }

  override suspend fun clearHistory() {
    historyDao.clearHistory()
  }
}

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
  private val dataStore: UserPreferencesDataStore,
) : PreferencesRepository {

  override fun observePreferences(): Flow<UserPreferences> = dataStore.preferences

  override suspend fun setThemeMode(themeMode: ThemeMode) = dataStore.setThemeMode(themeMode)

  override suspend fun setDynamicColor(enabled: Boolean) = dataStore.setDynamicColor(enabled)

  override suspend fun setDefaultCountryIso(countryIso: String) =
    dataStore.setDefaultCountryIso(countryIso)

  override suspend fun setDefaultMessagePrefix(prefix: String) =
    dataStore.setDefaultMessagePrefix(prefix)

  override suspend fun resetPreferences() = dataStore.resetPreferences()
}
