package com.messages.di.module

import android.content.Context
import androidx.room.Room
import com.messages.common.PermissionManager
import com.messages.common.common_utils.FontProvider
import com.messages.data.appcurser.AppCursor
import com.messages.data.appcurser.AppCursorImpl
import com.messages.data.database.MessageDatabase
import com.messages.data.database.dao.MessageDao
import com.messages.data.pref.AppPreferences
import com.messages.data.repository.MessageRepository
import com.messages.data.repository.MessageRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
class MessageAppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): MessageDatabase {
        return Room.databaseBuilder(
            context,
            MessageDatabase::class.java,
            "Users"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideMessageDao(messageDatabase: MessageDatabase): MessageDao {
        return messageDatabase.messageDao
    }

    @Provides
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences =
        AppPreferences(context)

    @Provides
    fun provideAppPermissionManager(@ApplicationContext context: Context): PermissionManager =
        PermissionManager(context)


    @Provides
    fun provideMessageRepository(
        @ApplicationContext context: Context,
        messageDao: MessageDao,
        appCursor: AppCursor
    ): MessageRepository {
        return MessageRepositoryImpl(context, messageDao, appCursor)
    }

    @Provides
    fun provideAppCursor(@ApplicationContext context: Context, messageDao: MessageDao): AppCursor =
        AppCursorImpl(context, messageDao)

    @Provides
    fun provideFontProvider(@ApplicationContext context: Context): FontProvider =
        FontProvider(context)

}