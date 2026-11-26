package com.directline.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.directline.core.database.dao.ContactDao
import com.directline.core.database.dao.HistoryDao
import com.directline.core.database.dao.TemplateDao
import com.directline.core.database.entity.ContactEntity
import com.directline.core.database.entity.ContactTagCrossRef
import com.directline.core.database.entity.InteractionHistoryEntity
import com.directline.core.database.entity.MessageTemplateEntity
import com.directline.core.database.entity.PhoneNumberEntity
import com.directline.core.database.entity.TagEntity
import com.directline.core.database.entity.TemplateCategoryEntity

@Database(
  entities = [
    ContactEntity::class,
    PhoneNumberEntity::class,
    TagEntity::class,
    ContactTagCrossRef::class,
    TemplateCategoryEntity::class,
    MessageTemplateEntity::class,
    InteractionHistoryEntity::class,
  ],
  version = 1,
  exportSchema = true,
)
abstract class DirectlineDatabase : RoomDatabase() {
  abstract fun contactDao(): ContactDao
  abstract fun templateDao(): TemplateDao
  abstract fun historyDao(): HistoryDao

  companion object {
    const val DATABASE_NAME = "directline.db"
  }
}
