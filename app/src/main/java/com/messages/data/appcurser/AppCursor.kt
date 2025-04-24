package com.messages.data.appcurser

import com.messages.data.models.BlockedNumberModel
import com.messages.data.models.Contact
import com.messages.data.models.Conversation
import com.messages.data.models.Message

interface AppCursor {

    fun getConversations(timeStamp: Long? = null): ArrayList<Conversation>

    fun getBlockedNumbers(): ArrayList<BlockedNumberModel>

    fun getContacts(timeStamp: Long? = null): ArrayList<Contact>

    fun getMessages(timeStamp: Long? = null)

    fun getMessages(threadId: Long, timeStamp: Long? = null): ArrayList<Message>

    fun syncRecipients(onSyncDone: () -> Unit)

    fun getConversationsUsingThreadId(threadId: Long, onGetConversation: (Conversation) -> Unit)

    fun updateMessageType(id: Long, type: Int, status: Int)

    fun getMessageRecipientAddress(messageId: Long): String

    fun getNameOfRecipient(address: String): String

    fun isNumberBlocked(
        number: String,
        blockedNumbers: ArrayList<BlockedNumberModel>? = null
    ): Boolean

    fun insertNewSmsToDevice(message: Message): Long

    fun markFirstAsSent()
}