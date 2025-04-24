package com.messages.data.models

import androidx.annotation.Keep

@Keep
data class BackupMessage(
    val type: Int,
    val addresses: List<String>,
    val date: Long,
    val dateSent: Long,
    val read: Boolean,
    val status: Int,
    val body: String,
    val protocol: Int,
    val serviceCenter: String?,
    val subId: Int,
    val threadId: Long
)

@Keep
data class Backup(
    val messageCount: Int = 0,
    val messages: List<BackupMessage> = listOf()
)