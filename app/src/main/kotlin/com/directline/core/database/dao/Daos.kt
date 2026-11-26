package com.directline.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.directline.core.database.entity.InteractionHistoryEntity
import com.directline.core.database.entity.MessageTemplateEntity
import com.directline.core.database.entity.TemplateCategoryEntity
import com.directline.core.database.entity.TemplateWithCategory
import kotlinx.coroutines.flow.Flow

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
