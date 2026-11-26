package com.directline.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.directline.core.database.dao.HistoryDao
import com.directline.core.database.dao.TemplateDao
import com.directline.core.database.entity.InteractionHistoryEntity
import com.directline.core.database.entity.MessageTemplateEntity
import com.directline.core.database.entity.TemplateCategoryEntity

@Database(
  entities = [
    TemplateCategoryEntity::class,
    MessageTemplateEntity::class,
    InteractionHistoryEntity::class,
  ],
  version = 2,
  exportSchema = true,
)
abstract class DirectlineDatabase : RoomDatabase() {
  abstract fun templateDao(): TemplateDao
  abstract fun historyDao(): HistoryDao

  companion object {
    const val DATABASE_NAME = "directline.db"
  }
}
