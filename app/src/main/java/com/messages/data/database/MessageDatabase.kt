package com.messages.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.messages.data.database.dao.MessageDao
import com.messages.data.database.type_converters.AppTypeConverters
import com.messages.data.models.Contact
import com.messages.data.models.Conversation
import com.messages.data.models.Message
import com.messages.data.models.PhoneNumber
import com.messages.data.models.Recipient

@Database(
    entities = [Conversation::class, Message::class, Contact::class,Recipient::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(AppTypeConverters::class)
abstract class MessageDatabase : RoomDatabase() {
    abstract val messageDao: MessageDao
}