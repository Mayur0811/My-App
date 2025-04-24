package com.messages.data.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Keep
@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["threadId"], unique = true),
        Index(value = ["isArchived"]),
        Index(value = ["isPrivate"]),
        Index(value = ["isBlocked"]),
        Index(value = ["isSchedule"]),
        Index(value = ["date"]),
        Index(value = ["isPined"]),
    ]
)
data class Conversation(
    @PrimaryKey @ColumnInfo(name = "threadId") var threadId: Long = 0,
    @ColumnInfo(name = "recipients") var recipients: List<Recipient> = listOf(),
    @ColumnInfo(name = "name") var name: String = "",
    @ColumnInfo(name = "date") var date: Long = 0L,
    @ColumnInfo(name = "snippet") var snippet: String = "",
    @ColumnInfo(name = "read") var read: Boolean = false,
    @ColumnInfo(name = "isGroupConversation") var isGroupConversation: Boolean? = false,
    @ColumnInfo(name = "isPrivate") var isPrivate: Boolean = false,
    @ColumnInfo(name = "isArchived") var isArchived: Boolean = false,
    @ColumnInfo(name = "isBlocked") var isBlocked: Boolean = false,
    @ColumnInfo(name = "isSchedule") var isSchedule: Boolean = false,
    @ColumnInfo(name = "type") var type: Int = 0, // 0 = all , 1 = personal , 2 = transaction , 3 = otp , 4 = offer,
    @ColumnInfo(name = "unreadCount") var unreadCount: Int = 0,
    @ColumnInfo(name = "subId") var subId: Int = -1,
    @ColumnInfo(name = "simSlot") var simSlot: Int = 0,
    @ColumnInfo(name = "isPined") var isPined: Boolean = false
) {
//    @Ignore
//    var isPined: Boolean = false
}