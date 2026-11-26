package com.directline.data.repository

import androidx.room.withTransaction
import com.directline.core.database.DirectlineDatabase
import com.directline.core.database.dao.HistoryDao
import com.directline.core.database.dao.TemplateDao
import com.directline.core.database.entity.InteractionHistoryEntity
import com.directline.core.database.entity.MessageTemplateEntity
import com.directline.core.database.entity.TemplateCategoryEntity
import com.directline.core.database.entity.TemplateWithCategory
import com.directline.core.datastore.UserPreferencesDataStore
import com.directline.domain.model.InteractionActionType
import com.directline.domain.model.InteractionHistory
import com.directline.domain.model.MessageTemplate
import com.directline.domain.model.TemplateCategory
import com.directline.domain.model.ThemeMode
import com.directline.domain.model.UserPreferences
import com.directline.domain.repository.HistoryRepository
import com.directline.domain.repository.PreferencesRepository
import com.directline.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

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
  phoneNumberSnapshot = phoneNumberSnapshot,
  messagePreview = messagePreview,
  actionType = InteractionActionType.entries.firstOrNull { it.name == actionType }
    ?: InteractionActionType.WHATSAPP_LAUNCH_ATTEMPT,
  createdAt = createdAt,
)

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
    phoneNumberSnapshot: String,
    messagePreview: String?,
  ) {
    historyDao.insertHistory(
      InteractionHistoryEntity(
        id = UUID.randomUUID().toString(),
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
