package com.directline.core.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
  tableName = "contacts",
  indices = [Index(value = ["name"]), Index(value = ["isFavorite"])],
)
data class ContactEntity(
  @PrimaryKey val id: String,
  val name: String,
  val notes: String,
  val isFavorite: Boolean,
  val createdAt: Long,
  val updatedAt: Long,
)

@Entity(
  tableName = "phone_numbers",
  foreignKeys = [
    ForeignKey(
      entity = ContactEntity::class,
      parentColumns = ["id"],
      childColumns = ["contactId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index(value = ["contactId"]), Index(value = ["normalizedValue"])],
)
data class PhoneNumberEntity(
  @PrimaryKey val id: String,
  val contactId: String,
  val rawValue: String,
  val normalizedValue: String,
  val countryCode: String,
  val createdAt: Long,
)

@Entity(
  tableName = "tags",
  indices = [Index(value = ["name"], unique = true)],
)
data class TagEntity(
  @PrimaryKey val id: String,
  val name: String,
)

@Entity(
  tableName = "contact_tags",
  primaryKeys = ["contactId", "tagId"],
  foreignKeys = [
    ForeignKey(
      entity = ContactEntity::class,
      parentColumns = ["id"],
      childColumns = ["contactId"],
      onDelete = ForeignKey.CASCADE,
    ),
    ForeignKey(
      entity = TagEntity::class,
      parentColumns = ["id"],
      childColumns = ["tagId"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index(value = ["tagId"])],
)
data class ContactTagCrossRef(
  val contactId: String,
  val tagId: String,
)

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
  foreignKeys = [
    ForeignKey(
      entity = ContactEntity::class,
      parentColumns = ["id"],
      childColumns = ["contactId"],
      onDelete = ForeignKey.SET_NULL,
    ),
  ],
  indices = [Index(value = ["contactId"]), Index(value = ["createdAt"])],
)
data class InteractionHistoryEntity(
  @PrimaryKey val id: String,
  val contactId: String?,
  val contactNameSnapshot: String?,
  val phoneNumberSnapshot: String,
  val messagePreview: String?,
  val actionType: String,
  val createdAt: Long,
)

data class ContactWithDetails(
  @Embedded val contact: ContactEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "contactId",
  )
  val phoneNumbers: List<PhoneNumberEntity>,
  @Relation(
    parentColumn = "id",
    entityColumn = "id",
    associateBy = Junction(
      value = ContactTagCrossRef::class,
      parentColumn = "contactId",
      entityColumn = "tagId",
    ),
  )
  val tags: List<TagEntity>,
)

data class TemplateWithCategory(
  @Embedded val template: MessageTemplateEntity,
  @Relation(
    parentColumn = "categoryId",
    entityColumn = "id",
  )
  val category: TemplateCategoryEntity?,
)
