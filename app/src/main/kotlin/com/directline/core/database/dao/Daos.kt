package com.directline.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.directline.core.database.entity.ContactEntity
import com.directline.core.database.entity.ContactTagCrossRef
import com.directline.core.database.entity.ContactWithDetails
import com.directline.core.database.entity.InteractionHistoryEntity
import com.directline.core.database.entity.MessageTemplateEntity
import com.directline.core.database.entity.PhoneNumberEntity
import com.directline.core.database.entity.TagEntity
import com.directline.core.database.entity.TemplateCategoryEntity
import com.directline.core.database.entity.TemplateWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
  @Transaction
  @Query("SELECT * FROM contacts ORDER BY isFavorite DESC, name COLLATE NOCASE ASC")
  fun observeContactsWithDetails(): Flow<List<ContactWithDetails>>

  @Transaction
  @Query("SELECT * FROM contacts WHERE isFavorite = 1 ORDER BY name COLLATE NOCASE ASC")
  fun observeFavoriteContactsWithDetails(): Flow<List<ContactWithDetails>>

  @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
  fun observeTags(): Flow<List<TagEntity>>

  @Transaction
  @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
  suspend fun getContactWithDetailsById(id: String): ContactWithDetails?

  @Transaction
  @Query(
    """
    SELECT c.* FROM contacts c
    INNER JOIN phone_numbers p ON p.contactId = c.id
    WHERE p.normalizedValue = :normalizedPhone
    LIMIT 1
    """,
  )
  suspend fun findContactByNormalizedPhone(normalizedPhone: String): ContactWithDetails?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertContact(contact: ContactEntity)

  @Query("UPDATE contacts SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :contactId")
  suspend fun updateFavorite(contactId: String, isFavorite: Boolean, updatedAt: Long)

  @Query("DELETE FROM contacts WHERE id = :contactId")
  suspend fun deleteContact(contactId: String)

  @Query("DELETE FROM contacts")
  suspend fun deleteAllContacts()

  @Query("DELETE FROM phone_numbers WHERE contactId = :contactId")
  suspend fun deletePhoneNumbersForContact(contactId: String)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPhoneNumbers(numbers: List<PhoneNumberEntity>)

  @Query("SELECT * FROM tags WHERE LOWER(name) = LOWER(:name) LIMIT 1")
  suspend fun getTagByName(name: String): TagEntity?

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertTag(tag: TagEntity)

  @Query("DELETE FROM contact_tags WHERE contactId = :contactId")
  suspend fun deleteTagRefsForContact(contactId: String)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertContactTagRefs(refs: List<ContactTagCrossRef>)

  @Query("DELETE FROM tags")
  suspend fun deleteAllTags()
}

@Dao
interface TemplateDao {
  @Transaction
  @Query("SELECT * FROM message_templates ORDER BY updatedAt DESC")
  fun observeTemplatesWithCategory(): Flow<List<TemplateWithCategory>>

  @Query("SELECT * FROM template_categories ORDER BY name COLLATE NOCASE ASC")
  fun observeCategories(): Flow<List<TemplateCategoryEntity>>

  @Transaction
  @Query("SELECT * FROM message_templates WHERE id = :id LIMIT 1")
  suspend fun getTemplateWithCategoryById(id: String): TemplateWithCategory?

  @Query("SELECT * FROM template_categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
  suspend fun getCategoryByName(name: String): TemplateCategoryEntity?

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertCategory(category: TemplateCategoryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertTemplate(template: MessageTemplateEntity)

  @Query("DELETE FROM message_templates WHERE id = :id")
  suspend fun deleteTemplate(id: String)

  @Query("DELETE FROM message_templates")
  suspend fun deleteAllTemplates()

  @Query("DELETE FROM template_categories")
  suspend fun deleteAllCategories()
}

@Dao
interface HistoryDao {
  @Query("SELECT * FROM interaction_history ORDER BY createdAt DESC")
  fun observeAllHistory(): Flow<List<InteractionHistoryEntity>>

  @Query("SELECT * FROM interaction_history ORDER BY createdAt DESC LIMIT :limit")
  fun observeRecentHistory(limit: Int): Flow<List<InteractionHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHistory(item: InteractionHistoryEntity)

  @Query("DELETE FROM interaction_history WHERE id = :id")
  suspend fun deleteHistoryItem(id: String)

  @Query("DELETE FROM interaction_history")
  suspend fun clearHistory()
}
