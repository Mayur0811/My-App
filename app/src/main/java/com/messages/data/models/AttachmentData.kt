package com.messages.data.models

import android.net.Uri
import androidx.annotation.Keep
import androidx.room.ColumnInfo

@Keep
data class AttachmentData(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "uri") val uri: Uri?,
    @ColumnInfo(name = "mimetype") val mimetype: String,
    @ColumnInfo(name = "filename") val filename: String?,
    @ColumnInfo(name = "isPending") var isPending: Boolean = false,
    @ColumnInfo(name = "viewType") val viewType: Int = getViewTypeForMimeType(mimetype)
) {
    companion object {
        const val ATTACHMENT_DOCUMENT = 0
        const val ATTACHMENT_MEDIA = 1
        const val ATTACHMENT_VCARD = 2

        fun getViewTypeForMimeType(mimetype: String): Int {
            return when {
                mimetype.isImageMimeType() || mimetype.isVideoMimeType() -> ATTACHMENT_MEDIA
                mimetype.isVCardMimeType() -> ATTACHMENT_VCARD
                else -> ATTACHMENT_DOCUMENT
            }
        }

        fun areItemsTheSame(first: AttachmentData, second: AttachmentData): Boolean {
            return first.id == second.id
        }

        fun areContentsTheSame(first: AttachmentData, second: AttachmentData): Boolean {
            return first.uri == second.uri && first.mimetype == second.mimetype && first.filename == second.filename
        }
    }
}

fun String.isImageMimeType(): Boolean {
    return lowercase().startsWith("image")
}

fun String.isVideoMimeType(): Boolean {
    return lowercase().startsWith("video")
}

fun String.isVCardMimeType(): Boolean {
    val lowercase = lowercase()
    return lowercase.endsWith("x-vcard") || lowercase.endsWith("vcard")
}

fun String.isGifMimeType(): Boolean {
    return lowercase().endsWith("gif")
}