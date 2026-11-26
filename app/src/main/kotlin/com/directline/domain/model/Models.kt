package com.directline.domain.model

data class TemplateCategory(
  val id: String,
  val name: String,
  val createdAt: Long,
)

data class MessageTemplate(
  val id: String,
  val title: String,
  val content: String,
  val categoryId: String?,
  val categoryName: String?,
  val createdAt: Long,
  val updatedAt: Long,
)

enum class InteractionActionType {
  WHATSAPP_LAUNCH_ATTEMPT,
}

data class InteractionHistory(
  val id: String,
  val phoneNumberSnapshot: String,
  val messagePreview: String?,
  val actionType: InteractionActionType,
  val createdAt: Long,
)

enum class ThemeMode {
  SYSTEM,
  LIGHT,
  DARK,
}

data class UserPreferences(
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val dynamicColor: Boolean = false,
  val defaultCountryIso: String = "",
  val defaultMessagePrefix: String = "",
)
