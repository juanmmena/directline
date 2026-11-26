package com.directline.core.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
  tableName = "template_categories",
  indices = [Index(value = ["name"], unique = true)],
)
data class TemplateCategoryEntity(
  @PrimaryKey val id: String,
  val name: String,
  val createdAt: Long,
)

@Entity(
  tableName = "message_templates",
  foreignKeys = [
    ForeignKey(
      entity = TemplateCategoryEntity::class,
      parentColumns = ["id"],
      childColumns = ["categoryId"],
      onDelete = ForeignKey.SET_NULL,
    ),
  ],
  indices = [Index(value = ["categoryId"]), Index(value = ["title"])],
)
data class MessageTemplateEntity(
  @PrimaryKey val id: String,
  val title: String,
  val content: String,
  val categoryId: String?,
  val createdAt: Long,
  val updatedAt: Long,
)

@Entity(
  tableName = "interaction_history",
  indices = [Index(value = ["createdAt"])],
)
data class InteractionHistoryEntity(
  @PrimaryKey val id: String,
  val phoneNumberSnapshot: String,
  val messagePreview: String?,
  val actionType: String,
  val createdAt: Long,
)

data class TemplateWithCategory(
  @Embedded val template: MessageTemplateEntity,
  @Relation(
    parentColumn = "categoryId",
    entityColumn = "id",
  )
  val category: TemplateCategoryEntity?,
)
