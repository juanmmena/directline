package com.directline.di

import android.content.Context
import androidx.room.Room
import com.directline.core.database.DirectlineDatabase
import com.directline.core.database.dao.ContactDao
import com.directline.core.database.dao.HistoryDao
import com.directline.core.database.dao.TemplateDao
import com.directline.core.whatsapp.AndroidIntentLauncher
import com.directline.core.whatsapp.DefaultAndroidIntentLauncher
import com.directline.data.repository.ContactRepositoryImpl
import com.directline.data.repository.HistoryRepositoryImpl
import com.directline.data.repository.PreferencesRepositoryImpl
import com.directline.data.repository.TemplateRepositoryImpl
import com.directline.domain.repository.ContactRepository
import com.directline.domain.repository.HistoryRepository
import com.directline.domain.repository.PreferencesRepository
import com.directline.domain.repository.TemplateRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
  @Provides
  @Singleton
  fun provideDirectlineDatabase(
    @ApplicationContext context: Context,
  ): DirectlineDatabase {
    return Room.databaseBuilder(
      context,
      DirectlineDatabase::class.java,
      DirectlineDatabase.DATABASE_NAME,
    ).build()
  }

  @Provides
  fun provideContactDao(db: DirectlineDatabase): ContactDao = db.contactDao()

  @Provides
  fun provideTemplateDao(db: DirectlineDatabase): TemplateDao = db.templateDao()

  @Provides
  fun provideHistoryDao(db: DirectlineDatabase): HistoryDao = db.historyDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
  @Binds
  @Singleton
  abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository

  @Binds
  @Singleton
  abstract fun bindTemplateRepository(impl: TemplateRepositoryImpl): TemplateRepository

  @Binds
  @Singleton
  abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

  @Binds
  @Singleton
  abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WhatsAppModule {
  @Binds
  @Singleton
  abstract fun bindAndroidIntentLauncher(
    impl: DefaultAndroidIntentLauncher,
  ): AndroidIntentLauncher
}
