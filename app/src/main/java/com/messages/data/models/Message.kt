package com.messages.data.models

import android.provider.Telephony.Sms
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

@Keep
@Entity(
    tableName = "messages",
    indices = [(Index(value = ["id"], unique = true)),
        Index(value = ["threadId", "read"]),
        Index(value = ["threadId", "date"])]
)
data class Message(
    @ColumnInfo(name = "threadId") val threadId: Long = 0,
    @ColumnInfo(name = "messageId") val messageId: Long? = null,
    @ColumnInfo(name = "addresses") val addresses: List<String> = listOf(),
    @ColumnInfo(name = "type") var type: Int = 0,
    @ColumnInfo(name = "date") val date: Long = 0,
    @ColumnInfo(name = "dateSent") var dateSent: Long = 0,
    @ColumnInfo(name = "read") var read: Boolean = false,
    @ColumnInfo(name = "seen") var seen: Boolean = false,
    @ColumnInfo(name = "subId") var subId: Int = -1,
    @ColumnInfo(name = "simSlot") var simSlot: Int = -1,
    @ColumnInfo(name = "body") var body: String = "",
    @ColumnInfo(name = "isMMS") var isMMS: Boolean = false,
    @ColumnInfo(name = "deliveryStatus") var deliveryStatus: Int = Sms.STATUS_NONE,
    @ColumnInfo(name = "name") var name: String? = "",
    @ColumnInfo(name = "isScheduled") var isScheduled: Boolean = false,
    @ColumnInfo(name = "mmsDeliveryStatus") var mmsDeliveryStatus: String = "",
    @ColumnInfo(name = "readReport") var readReport: String = "",
    @ColumnInfo(name = "error") var errorType: Int = 0,
    @ColumnInfo(name = "messageSize") var messageSize: Int = 0,
    @ColumnInfo(name = "messageType") var messageType: Int = 0,
    @ColumnInfo(name = "mmsStatus") var mmsStatus: Int = 0,
    @ColumnInfo(name = "subject") var subject: String = "",
    @ColumnInfo(name = "textContentType") var textContentType: String = "",
    @ColumnInfo(name = "attachmentWithText") var attachmentWithMessageModel: AttachmentWithMessageModel? = null
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0

    fun isReceivedMessage() = type == Sms.MESSAGE_TYPE_INBOX

    @Ignore
    var isShowDate: Boolean = false

    @Ignore
    var isShowTranslateBody: Boolean = false


    @Ignore
    var isLoading: Boolean = false

    @Ignore
    var translatedBody: String = ""

}
