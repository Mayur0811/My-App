package com.messages.data.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Keep
data class AttachmentWithMessageModel(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "text") var text: String,
    @ColumnInfo(name = "attachments") var attachments: ArrayList<AttachmentData>
) {
}