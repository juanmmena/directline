package com.directline.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.directline.domain.model.ThemeMode
import com.directline.domain.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "directline_preferences",
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
  @param:ApplicationContext private val context: Context,
) {
  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DEFAULT_COUNTRY_ISO = stringPreferencesKey("default_country_iso")
    val DEFAULT_MESSAGE_PREFIX = stringPreferencesKey("default_message_prefix")
  }

  val preferences: Flow<UserPreferences> = context.userPreferencesDataStore.data.map { prefs ->
    val themeMode = prefs[Keys.THEME_MODE]?.let { raw ->
      ThemeMode.entries.firstOrNull { it.name == raw }
    } ?: ThemeMode.SYSTEM

    UserPreferences(
      themeMode = themeMode,
      defaultCountryIso = prefs[Keys.DEFAULT_COUNTRY_ISO].orEmpty(),
      defaultMessagePrefix = prefs[Keys.DEFAULT_MESSAGE_PREFIX].orEmpty(),
    )
  }

  suspend fun setThemeMode(themeMode: ThemeMode) {
    context.userPreferencesDataStore.edit { prefs ->
      prefs[Keys.THEME_MODE] = themeMode.name
    }
  }

  suspend fun setDefaultCountryIso(countryIso: String) {
    context.userPreferencesDataStore.edit { prefs ->
      prefs[Keys.DEFAULT_COUNTRY_ISO] = countryIso.trim().uppercase()
    }
  }

  suspend fun setDefaultMessagePrefix(prefix: String) {
    context.userPreferencesDataStore.edit { prefs ->
      prefs[Keys.DEFAULT_MESSAGE_PREFIX] = prefix
    }
  }

  suspend fun resetPreferences() {
    context.userPreferencesDataStore.edit { it.clear() }
  }
}
