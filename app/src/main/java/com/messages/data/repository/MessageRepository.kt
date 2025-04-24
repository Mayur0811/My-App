package com.messages.data.repository

import android.net.Uri
import android.provider.Telephony.Sms
import androidx.lifecycle.LiveData
import com.messages.data.models.BackupProgress
import com.messages.data.models.Contact
import com.messages.data.models.Conversation
import com.messages.data.models.Message
import com.messages.data.models.MessagesWithDate
import com.messages.data.models.Recipient
import com.messages.data.models.ScheduleConversation

interface MessageRepository {

    val backupProgress: LiveData<BackupProgress>

    val restoreProgress: LiveData<BackupProgress>

    fun syncConversation(onSyncDone: () -> Unit)

    fun syncSelectedConversation(threadId: Long, onSyncDone: (Conversation?,Boolean) -> Unit)

    fun syncMessages()

    fun syncMessages(threadId: Long, onSyncDone: (ArrayList<Message>) -> Unit)

    fun syncRecipients(onSyncDone: () -> Unit)

    fun syncContacts(onSyncDone: () -> Unit)

    fun getConversations(): ArrayList<Conversation>

    fun getArchivedConversations(): ArrayList<Conversation>

    fun getMessagesFromThreadId(threadId: Long): ArrayList<MessagesWithDate>

    fun getConversationFromThreadId(threadId: Long): Conversation?

    fun getConversationOfThreadId(threadId: Long): Conversation

    fun updateConversationScheduleStatus(threadId: Long, isSchedule: Boolean)

    fun deleteMessage(id: Long, isMMS: Boolean = false)

    fun deleteConversation(threadIds: List<Long>)

    fun handleConversationForBlocked(threadIds: List<Long>, isBlocked: Boolean)

    fun updateLastConversationMessage(threadId: Long)

    fun updateRecipientInConversation()

    fun handleConversationForArchived(threadIds: List<Long>, isArchive: Boolean)

    fun handleConversationForPrivate(threadIds: List<Long>, isPrivate: Boolean)

    fun markThreadMessagesReadUnRead(threadIds: List<Long>, readUnread: Boolean, onDone: () -> Unit)

    fun findConversationBySearch(searchText: String): ArrayList<Conversation>

    fun getContacts(): ArrayList<Contact>

    fun findContactBySearch(searchText: String): ArrayList<Contact>

    fun getPrivateConversations(): ArrayList<Conversation>


    fun updateMessagesStatus(id: Long, status: Int): Int

    fun updateMessagesTypeWithCursor(
        id: Long,
        type: Int,
        status: Int = Sms.STATUS_NONE,
        onUpdate: (Int) -> Unit = {}
    )

    fun getMessageRecipientAddress(messageId: Long): String

    fun getNameOfRecipient(address: String): String

    fun getContactFromAddress(address: String): List<Contact>

    fun isNumberBlocked(number: String): Boolean

    fun insertNewSmsToDevice(message: Message): Long

    fun insertOrUpdateConversation(conversation: Conversation?, message: Message)

    fun insertOrUpdateConversation(conversation: Conversation)

    fun insertOrUpdateMessage(message: Message)

    fun checkIsPrivateConversation(threadId: Long): Boolean

    fun markFirstAsSent()

    fun getThreadRecipient(threadId: Long): ArrayList<Recipient>

    fun getScheduledMessageWithId(threadId: Long, messageId: Long): Message

    fun deleteScheduledMessage(messageId: Long)

    fun deleteTemporaryThreadId(threadId: Long)

    fun getScheduleConversation(): ArrayList<Conversation>

    fun getBlockedConversations(): ArrayList<Conversation>

    fun updateContactToRecipient()

    fun performBackup()

    fun performRestore(uri: Uri)

    fun stopRestore()

    fun getIdOfMessage(messageId: Long): Long?


    fun updateConversationData(threadId: Long, snippet: String, date: Long)

    fun updateMessageAsRead(threadId: Long)

    fun updateScheduleConversationByThreadId(
        conversations: ArrayList<ScheduleConversation>,
        onSyncDone: () -> Unit
    )

    fun deleteScheduleMessages(id: List<Long>)

    fun getScheduleConversationWithMessages(): ArrayList<ScheduleConversation>

    fun updateConversationPinedData(threadIds: List<Long>, isPined: Boolean)

    fun getConversationsUsingThreadId(threadId: Long): List<Conversation>
}