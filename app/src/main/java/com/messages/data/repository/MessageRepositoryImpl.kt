package com.messages.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony.Mms
import android.provider.Telephony.Sms
import android.provider.Telephony.Threads
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.messages.R
import com.messages.common.PermissionManager
import com.messages.common.backgroundScope
import com.messages.common.getMessageType
import com.messages.common.mainScope
import com.messages.common.message_utils.getThreadId
import com.messages.data.appcurser.AppCursor
import com.messages.data.database.dao.MessageDao
import com.messages.data.events.DataSyncDone
import com.messages.data.events.OnReceiveMessage
import com.messages.data.events.OnReceiveMessageConversation
import com.messages.data.events.RefreshMessages
import com.messages.data.models.Backup
import com.messages.data.models.BackupMessage
import com.messages.data.models.BackupProgress
import com.messages.data.models.Contact
import com.messages.data.models.Conversation
import com.messages.data.models.Message
import com.messages.data.models.MessagesWithDate
import com.messages.data.models.Recipient
import com.messages.data.models.ScheduleConversation
import com.messages.extentions.formatMessageDate
import com.messages.extentions.fromJson
import com.messages.extentions.getBackupFile
import com.messages.extentions.getStringValue
import com.messages.extentions.normalizePhoneNumber
import com.messages.extentions.toast
import com.messages.utils.AppLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject


class MessageRepositoryImpl @Inject constructor(
    private var context: Context,
    private var messageDao: MessageDao,
    private val appCursor: AppCursor,
) : MessageRepository {

    private val _backupProgress = MutableLiveData<BackupProgress>().apply {
        value = BackupProgress.Idle()
    }
    override val backupProgress: LiveData<BackupProgress> = _backupProgress

    private val _restoreProgress = MutableLiveData<BackupProgress>().apply {
        value = BackupProgress.Idle()
    }
    override val restoreProgress: LiveData<BackupProgress> = _restoreProgress

    @Volatile
    private var stopFlag: Boolean = false

    override fun syncConversation(onSyncDone: () -> Unit) {
        try {
            backgroundScope.launch {
                val lastDate = messageDao.getLatestConversationDate()
                val newConversations = appCursor.getConversations(lastDate)
                val currentThreadIds = messageDao.getThreadIdsOfCurrentConversations()
                newConversations.forEach { conversation ->
                    if (currentThreadIds.contains(conversation.threadId)) {
                        messageDao.updateConversationData(
                            conversation.threadId, conversation.snippet, conversation.date
                        )
                    } else {
                        val recipientDataMap =
                            conversation.recipients.associateBy({ it.recipientId },
                                { messageDao.getRecipientsById(it.recipientId) })
                        conversation.recipients.forEach { recipient ->
                            recipientDataMap[recipient.recipientId]?.let { data ->
                                recipient.apply {
                                    address = data.address
                                    contact = data.contact
                                    lastUpdate = data.lastUpdate
                                }
                                if (data.contact != null) {
                                    conversation.type = 1
                                }
                            }
                        }
                        conversation.name =
                            conversation.recipients.joinToString { recipient -> recipient.getDisplayName() }
                        messageDao.insertOrUpdateConversations(conversation)
                    }
                    syncMessages(conversation.threadId) {}
                }
                mainScope.launch {
                    EventBus.getDefault().post(DataSyncDone)
                }
            }
        } catch (e: Exception) {
            AppLogger.e(message = "syncConversation error-> ${e.message}")
        }
    }

    override fun syncRecipients(onSyncDone: () -> Unit) {
        backgroundScope.launch {
            appCursor.syncRecipients(onSyncDone)
        }
    }

    override fun syncMessages() {
        try {
            backgroundScope.launch {
                val lastDate = messageDao.getLatestMessageDate()
                appCursor.getMessages(lastDate)
            }
        } catch (e: Exception) {
            AppLogger.e(message = "syncMessages error-> ${e.message}")
        }
    }

    override fun syncMessages(threadId: Long, onSyncDone: (ArrayList<Message>) -> Unit) {
        try {
            backgroundScope.launch {
                val lastDate = messageDao.getLatestMessageDate(threadId)
                val newMessages = appCursor.getMessages(threadId, lastDate)
                val currentMessageIds = messageDao.getMessageIdsFromThreadId(threadId)
                newMessages.forEach { message ->
                    if (!currentMessageIds.contains(message.messageId)) {
                        messageDao.insertOrUpdateMessages(message)
                    }
                }
                if (newMessages.isNotEmpty()) {
                    onSyncDone.invoke(newMessages)
                }
            }
        } catch (e: Exception) {
            AppLogger.e(message = "syncMessages error-> ${e.message}")
        }
    }

    override fun syncContacts(onSyncDone: () -> Unit) {
        try {
            backgroundScope.launch {
                val lastDate = messageDao.getLatestContactModifyDate()
                val contacts = appCursor.getContacts(lastDate)
                messageDao.insertOrUpdateContacts(contacts)

                val recipients =
                    messageDao.getAllRecipients().associateBy { it.address.normalizePhoneNumber() }
                        .toMutableMap()
                val contactMap = contacts.associateBy { it.phoneNumbers }

                contactMap.forEach { (phoneNumbers, contact) ->
                    phoneNumbers.forEach { phoneNumber ->
                        val recipient = recipients[phoneNumber.address.normalizePhoneNumber()]
                        if (recipient != null) {
                            recipient.contact = contact
                            recipients[phoneNumber.address] = recipient
                            recipient.lastUpdate = System.currentTimeMillis() / 1000
                        }
                    }

                }
                messageDao.insertOrUpdateRecipients(recipients.values.toList())
                mainScope.launch {
                    onSyncDone.invoke()
                }
            }
        } catch (e: Exception) {
            AppLogger.e(message = "syncContacts error-> ${e.message}")
        }
    }

    override fun getConversations(): ArrayList<Conversation> {
        val conversations = messageDao.getConversationsWithUnreadAndSubId() as ArrayList
        return conversations
    }

    override fun getConversationsUsingThreadId(threadId: Long): List<Conversation> {
        return messageDao.getConversationsUsingThreadId(threadId)
    }

    override fun getArchivedConversations() = messageDao.getArchivedConversation() as ArrayList

    override fun getMessagesFromThreadId(threadId: Long): ArrayList<MessagesWithDate> {
        val messages = arrayListOf<MessagesWithDate>()
        val message = messageDao.getMessagesFromThreadId(threadId)
        for (i in message.indices) {
            val newMessage = message[i].apply {
                isShowDate =
                    ((i < message.size - 1) && message[i].date.formatMessageDate() != message[i + 1].date.formatMessageDate()) || (i == (message.size - 1))
            }
            messages.add(
                if (newMessage.isReceivedMessage()) MessagesWithDate.ReceivedMessage(newMessage) else MessagesWithDate.SendMessage(
                    newMessage
                )
            )

        }
        return messages
    }

    override fun getConversationFromThreadId(threadId: Long): Conversation? =
        messageDao.getConversationFromThreadId(threadId)

    override fun getConversationOfThreadId(threadId: Long): Conversation =
        messageDao.getConversationOfThreadId(threadId)

    override fun deleteMessage(id: Long, isMMS: Boolean) {
        backgroundScope.launch {
            val uri = if (isMMS) Mms.CONTENT_URI else Sms.CONTENT_URI
            val selection = "${Sms._ID} = ?"
            val selectionArgs = arrayOf(id.toString())
            try {
                context.contentResolver.delete(uri, selection, selectionArgs)
                messageDao.deleteMessage(id)
            } catch (e: Exception) {
                AppLogger.e(message = e.message.toString())
                context.toast(msg = context.getStringValue(R.string.something_went_wrong_please_try_after_some_time))
            }
        }
    }

    override fun handleConversationForBlocked(threadIds: List<Long>, isBlocked: Boolean) {
        backgroundScope.launch {
            try {
                messageDao.handleConversationForBlocked(threadIds, isBlocked)
            } catch (e: Exception) {
                AppLogger.e(message = e.message.toString())
            }
        }
    }

    override fun updateRecipientInConversation() {
        backgroundScope.launch {
            val recipientsMap =
                messageDao.getAllRecipients().associateBy { it.recipientId }.toMutableMap()
            val conversations = messageDao.getAllConversation()
            conversations.forEach { conversation ->
                val updatedRecipients = conversation.recipients.map { recipient ->
                    recipientsMap[recipient.recipientId] ?: recipient
                }

                val displayName = updatedRecipients.joinToString { it.getDisplayName() }

                messageDao.updateThreadRecipient(
                    conversation.threadId, updatedRecipients, displayName
                )
            }
        }
    }

    override fun updateContactToRecipient() {
        backgroundScope.launch {
            val lastUpdateRecipientTime = messageDao.getLatestRecipientDate()
            val contacts = messageDao.getContactsByTime(lastUpdateRecipientTime)
            val recipientsMap = mutableMapOf<Long, Recipient>()

            contacts.forEach { contact ->
                contact.phoneNumbers.forEach { phoneNumber ->
                    val recipientList = messageDao.getRecipientsByAddress(phoneNumber.address)
                    if (recipientList.isNotEmpty()) {
                        val recipient = recipientList.first()
                        recipient.contact = contact
                        recipientsMap[recipient.recipientId] = recipient
                    }
                }
            }

            if (recipientsMap.isNotEmpty()) {
                messageDao.insertOrUpdateRecipients(recipientsMap.values.toList())
            }
        }
    }

    override fun updateLastConversationMessage(threadId: Long) {
        backgroundScope.launch {
            val uri = Threads.CONTENT_URI
            val selection = "${Threads._ID} = ?"
            val selectionArgs = arrayOf(threadId.toString())
            try {
                context.contentResolver.delete(uri, selection, selectionArgs)
                appCursor.getConversationsUsingThreadId(threadId) { newConversation ->
                    backgroundScope.launch {
                        messageDao.insertOrUpdateConversations(newConversation)
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(message = e.message.toString())
            }
        }
    }

    override fun handleConversationForArchived(threadIds: List<Long>, isArchive: Boolean) {
        backgroundScope.launch {
            messageDao.handleConversationForArchived(threadIds, isArchive)
        }
    }

    override fun handleConversationForPrivate(threadIds: List<Long>, isPrivate: Boolean) {
        backgroundScope.launch {
            messageDao.handleConversationForPrivate(threadIds, isPrivate)
        }
    }

    override fun deleteConversation(threadIds: List<Long>) {
        backgroundScope.launch {
            val uri = Threads.CONTENT_URI
            val selection =
                threadIds.joinToString(prefix = "${Threads._ID} IN (", postfix = ")") { "?" }
            val selectionArgs = threadIds.map { it.toString() }.toTypedArray()
            try {
                context.contentResolver.delete(uri, selection, selectionArgs)
                messageDao.deleteConversations(threadIds)
                deleteThreadMessageFromDevice(threadIds)
                deleteThreadMessageFromDevice(threadIds, true)
            } catch (e: Exception) {
                AppLogger.e(message = "deleteConversation -> ${e.message.toString()}")
                context.toast(msg = context.getStringValue(R.string.something_went_wrong_please_try_after_some_time))
            }
        }
    }

    private fun deleteThreadMessageFromDevice(threadIds: List<Long>, isMMS: Boolean = false) {
        backgroundScope.launch {
            if (threadIds.isEmpty()) return@launch

            val uri = if (isMMS) Mms.CONTENT_URI else Sms.CONTENT_URI
            val selection =
                threadIds.joinToString(prefix = "${Sms.THREAD_ID} IN (", postfix = ")") { "?" }
            val selectionArgs = threadIds.map { it.toString() }.toTypedArray()
            try {
                context.contentResolver.delete(uri, selection, selectionArgs)
                messageDao.deleteThreadMessages(threadIds)
            } catch (e: Exception) {
                AppLogger.e(message = "deleteMessage ->$isMMS  ${e.message.toString()}")
                context.toast(msg = context.getStringValue(R.string.something_went_wrong_please_try_after_some_time))
            }
        }
    }

    override fun markThreadMessagesReadUnRead(
        threadIds: List<Long>, readUnread: Boolean, onDone: () -> Unit
    ) {
        backgroundScope.launch {
            arrayOf(Sms.CONTENT_URI, Mms.CONTENT_URI).forEach { uri ->
                val contentValues = ContentValues().apply {
                    put(Sms.READ, if (readUnread) 1 else 0)
                    put(Sms.SEEN, if (readUnread) 1 else 0)
                }
                val selection = "${Sms.THREAD_ID} = ?"
                val selectionArgs = threadIds.map { it.toString() }.toTypedArray()
                context.contentResolver.update(uri, contentValues, selection, selectionArgs)
            }
            messageDao.handleConversationForRead(threadIds, readUnread)
            messageDao.updateMessagesAsRead(threadIds, readUnread)
            onDone.invoke()
        }
    }

    override fun findConversationBySearch(searchText: String) =
        messageDao.findConversationBySearch(searchText) as ArrayList

    override fun getContacts(): ArrayList<Contact> {
        val favoriteContact = messageDao.getFavoriteContacts().sortedBy { it.name }
        if (favoriteContact.isNotEmpty()) {
            favoriteContact[0].isShowAlphabet = true
        }

        val newContacts = arrayListOf<Contact>()
        val contacts = messageDao.getContacts().sortedBy {
            (if (it.name.contains(" ")) {
                it.name.split(" ").first()
            } else {
                it.name
            })
        }
        for (i in contacts.indices) {
            if (i > 0 && contacts[i].name[0] != contacts[i - 1].name[0] && contacts[i].name[0].isLetter()) {
                contacts[i].isShowAlphabet = true
            } else if (i == 0) {
                contacts[i].isShowAlphabet = true
            }
            newContacts.add(contacts[i])
        }
        return (favoriteContact + contacts) as ArrayList
    }

    override fun findContactBySearch(searchText: String): ArrayList<Contact> {
        val searchContact = messageDao.findContactBySearch(searchText)
        val favoriteContact = searchContact.filter { it.isFavorite }
        if (favoriteContact.isNotEmpty()) {
            favoriteContact[0].isShowAlphabet = true
        }
        val unFavoriteContact = searchContact.filter { !it.isFavorite }
        for (i in unFavoriteContact.indices) {
            if (i > 0 && unFavoriteContact[i].name[0] != unFavoriteContact[i - 1].name[0] && unFavoriteContact[i].name[0].isLetter()) {
                unFavoriteContact[i].isShowAlphabet = true
            } else if (i == 0) {
                unFavoriteContact[i].isShowAlphabet = true
            }
        }
        return (favoriteContact + unFavoriteContact) as ArrayList
    }


    override fun getPrivateConversations(): ArrayList<Conversation> =
        messageDao.getPrivateConversation() as ArrayList

    override fun updateMessagesStatus(id: Long, status: Int): Int {
        return messageDao.updateMessagesStatus(id, status)
    }

    override fun updateMessagesTypeWithCursor(
        id: Long,
        type: Int,
        status: Int,
        onUpdate: (Int) -> Unit
    ) {
        backgroundScope.launch {
            appCursor.updateMessageType(id, type, status)
            val messagesType = messageDao.updateMessagesType(id, type, status)
            onUpdate.invoke(messagesType)
        }
    }

    override fun getMessageRecipientAddress(messageId: Long): String =
        appCursor.getMessageRecipientAddress(messageId)

    override fun getNameOfRecipient(address: String): String {
        if (!PermissionManager(context).hasReadContact()) {
            return address
        }
        return appCursor.getNameOfRecipient(address)
    }

    override fun getContactFromAddress(address: String): List<Contact> =
        messageDao.getContactsFromNumber(address)

    override fun isNumberBlocked(number: String): Boolean {
        return appCursor.isNumberBlocked(number)
    }

    override fun insertNewSmsToDevice(message: Message): Long =
        appCursor.insertNewSmsToDevice(message)

    override fun insertOrUpdateConversation(conversation: Conversation?, message: Message) {
        backgroundScope.launch {
            if (conversation == null) {
                var recipients = messageDao.getRecipientsByAddress(message.addresses.first())
                if (recipients.isEmpty()) {
                    val newRecipient = Recipient(
                        address = message.addresses.first(),
                        lastUpdate = System.currentTimeMillis() / 1000,
                        contact = messageDao.getContactFromNumber(message.addresses.first())
                    )
                    val idRecipient = messageDao.insertOrUpdateRecipient(newRecipient)
                    recipients = arrayListOf(newRecipient.apply { recipientId = idRecipient })
                }

                val newConversation = Conversation(
                    threadId = context.getThreadId(message.addresses.first()),
                    snippet = if (message.isMMS) context.getStringValue(R.string.attachment) else message.body,
                    date = message.date,
                    read = message.read,
                    isGroupConversation = false,
                    isBlocked = false,
                    type = getMessageType(message.addresses.first(), message.body),
                    recipients = listOf(recipients.first()),
                    name = recipients.first().getDisplayName()
                )
                messageDao.insertOrUpdateConversations(newConversation)
            } else {
                val updatedConversation = conversation.apply {
                    snippet =
                        if (message.isMMS) context.getStringValue(R.string.attachment) else message.body
                    date = message.date
                    read = message.read
                }
                messageDao.insertOrUpdateConversations(updatedConversation)
            }
            EventBus.getDefault().post(OnReceiveMessage(message))
            EventBus.getDefault().post(OnReceiveMessageConversation(message))
        }
    }

    override fun insertOrUpdateConversation(conversation: Conversation) {
        backgroundScope.launch {
            messageDao.insertOrUpdateConversations(conversation)
        }
    }

    override fun insertOrUpdateMessage(message: Message) {
        backgroundScope.launch {
            messageDao.insertOrUpdateMessages(message)
        }
    }

    override fun checkIsPrivateConversation(threadId: Long): Boolean =
        messageDao.checkIsPrivateConversation(threadId)

    override fun markFirstAsSent() = appCursor.markFirstAsSent()

    override fun getThreadRecipient(threadId: Long): ArrayList<Recipient> {
        val recipientString = messageDao.getThreadRecipient(threadId)
        return recipientString?.fromJson<ArrayList<Recipient>>() ?: arrayListOf()
    }

    override fun getScheduledMessageWithId(threadId: Long, messageId: Long) =
        messageDao.getScheduledMessageWithId(threadId, messageId)

    override fun deleteScheduledMessage(messageId: Long) {
        backgroundScope.launch {
            messageDao.deleteScheduledMessage(messageId)
        }
    }

    override fun deleteTemporaryThreadId(threadId: Long) {
        backgroundScope.launch {
            messageDao.deleteTemporaryThreadId(threadId)
        }
    }

    override fun getScheduleConversation() = messageDao.getScheduleConversations() as ArrayList

    override fun getBlockedConversations(): ArrayList<Conversation> =
        messageDao.getBlockedConversations() as ArrayList

    private fun isBackupOrRestoreRunning(): Boolean {
        return (backupProgress.value?.running == true) || (restoreProgress.value?.running == true)
    }

    override fun performBackup() {
        backgroundScope.launch {
            val backupMessages = arrayListOf<BackupMessage>()
            // If a backup or restore is already running, don't do anything
            if (isBackupOrRestoreRunning()) return@launch


            // Map all the messages into our object we'll use for the Json mapping
            val messages = messageDao.getAllMessages()
            // Get the messages from realm
            val messageCount = messages.size

            // Map the messages to the new format
            messages.mapIndexed { index, message ->
                // Update the progress
                mainScope.launch {
                    _backupProgress.value = BackupProgress.Running(messageCount, index)
                }
                backupMessages.add(messageToBackupMessage(message))
            }


            // Update the status, and set the progress to be indeterminate since we can no longer calculate progress
            mainScope.launch {
                _backupProgress.value = BackupProgress.Saving()
            }

            // Convert the data to json
            val gson = GsonBuilder().setPrettyPrinting().create()
            val json = gson.toJson(Backup(messageCount, backupMessages)).toByteArray()

            try {
                val backupTime = System.currentTimeMillis()
                val timestamp =
                    SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(backupTime)
                val newFile = context.getBackupFile("message_backup-$timestamp.json")

                context.contentResolver.openOutputStream(Uri.fromFile(File(newFile)))
                    ?.use { stream ->
                        stream.write(json)
                    }
                mainScope.launch {
                    _backupProgress.value = BackupProgress.Saved(backupTime)
                }
            } catch (e: Exception) {
                AppLogger.e("1234", message = e.message.toString())
                mainScope.launch {
                    _backupProgress.value = BackupProgress.ErrorOnSave()
                }
            }

            mainScope.launch {
                delay(1000)
                // Mark the task finished, and set it as Idle a second later
                _backupProgress.value = BackupProgress.Finished()
                delay(1000)
                _backupProgress.value = BackupProgress.Idle()
            }
        }
    }

    private fun messageToBackupMessage(message: Message): BackupMessage = BackupMessage(
        type = message.type,
        addresses = message.addresses,
        date = message.date,
        dateSent = message.dateSent,
        read = message.read,
        status = message.deliveryStatus,
        body = message.body,
        protocol = 0,
        serviceCenter = null,
        subId = message.subId,
        threadId = message.threadId
    )

    override fun performRestore(uri: Uri) {
        backgroundScope.launch {
            // If a backupFile or restore is already running, don't do anything
            if (isBackupOrRestoreRunning()) return@launch

            mainScope.launch {
                _restoreProgress.value = BackupProgress.Parsing()
            }

            val backup = context.contentResolver.openInputStream(uri)?.bufferedReader()
                ?.use { reader -> Gson().fromJson(reader, Backup::class.java) }

            val messageCount = backup?.messages?.size ?: 0
            var errorCount = 0

            backup?.messages?.forEachIndexed { index, message ->
                if (stopFlag) {
                    stopFlag = false
                    mainScope.launch {
                        _restoreProgress.value = BackupProgress.Idle()
                    }
                    return@forEachIndexed
                }

                // Update the progress
                mainScope.launch {
                    _restoreProgress.value = BackupProgress.Running(messageCount, index)
                }

                try {
                    val values = contentValuesOf(
                        Sms.TYPE to message.type,
                        Sms.ADDRESS to message.addresses.first(),
                        Sms.DATE to message.date,
                        Sms.DATE_SENT to message.dateSent,
                        Sms.READ to message.read,
                        Sms.SEEN to 1,
                        Sms.STATUS to message.status,
                        Sms.BODY to message.body,
                        Sms.PROTOCOL to message.protocol,
                        Sms.SERVICE_CENTER to message.serviceCenter,
                        Sms.SUBSCRIPTION_ID to message.subId,
                        Sms.THREAD_ID to message.threadId
                    )
                    context.contentResolver.insert(Sms.CONTENT_URI, values)
                } catch (e: Exception) {
                    AppLogger.d(message = e.message.toString())
                    errorCount++
                }
            }

            if (errorCount > 0) {
                AppLogger.d(message = "Failed to backup $errorCount/$messageCount messages")
            }

            // Sync the messages
            mainScope.launch {
                _restoreProgress.value = BackupProgress.Syncing()
            }

            // Mark the task finished, and set it as Idle a second later
            mainScope.launch {
                EventBus.getDefault().postSticky(RefreshMessages)
                _restoreProgress.value = BackupProgress.Finished()
                delay(1000)
                _restoreProgress.value = BackupProgress.Idle()
            }
        }
    }

    override fun stopRestore() {
        stopFlag = true
    }

    override fun getIdOfMessage(messageId: Long) = messageDao.getIdOfMessage(messageId)

    override fun updateConversationData(threadId: Long, snippet: String, date: Long) {
        backgroundScope.launch {
            messageDao.updateConversationData(threadId, snippet, date)
        }
    }

    override fun updateMessageAsRead(threadId: Long) {
        backgroundScope.launch {
            messageDao.updateMessagesAsRead(threadId)
        }
    }

    override fun syncSelectedConversation(
        threadId: Long,
        onSyncDone: (Conversation?, Boolean) -> Unit
    ) {
        backgroundScope.launch {
            val conversation = messageDao.getConversationFromThreadId(threadId)
            if (conversation != null) {
                val lastMessage = messageDao.getLastMessageFromThreadId(threadId)
                if (lastMessage != null) {
                    messageDao.updateConversationData(
                        threadId, lastMessage.body, lastMessage.date
                    )
                    val updatedConversation = conversation.apply {
                        snippet =
                            if (lastMessage.isMMS) context.getStringValue(R.string.attachment) else lastMessage.body
                        date = lastMessage.date
                        unreadCount = 0
                        read = true
                        simSlot = lastMessage.simSlot
                        subId = lastMessage.subId
                    }
                    mainScope.launch {
                        onSyncDone.invoke(updatedConversation, false)
                    }
                } else {
                    messageDao.deleteConversations(arrayListOf(threadId))
                    mainScope.launch {
                        onSyncDone.invoke(null, true)
                    }
                }
            }
        }
    }

    override fun updateConversationScheduleStatus(threadId: Long, isSchedule: Boolean) {
        backgroundScope.launch {
            messageDao.updateScheduleStatusOfConversation(threadId, isSchedule)
        }
    }

    override fun updateScheduleConversationByThreadId(
        conversations: ArrayList<ScheduleConversation>, onSyncDone: () -> Unit
    ) {
        backgroundScope.launch {
            conversations.forEach { conv ->
                val conversation = messageDao.getConversationOfThreadId(conv.conversation.threadId)
                val updatedRecipients = conversation.recipients.map { recipient ->
                    recipient.apply {
                        this.contact = messageDao.getContactFromNumber(recipient.address)
                        this.lastUpdate = System.currentTimeMillis() / 1000
                    }
                }

                val displayName = updatedRecipients.joinToString { it.getDisplayName() }

                messageDao.updateThreadRecipient(
                    conversation.threadId, updatedRecipients, displayName
                )
            }
            onSyncDone.invoke()
        }
    }

    override fun deleteScheduleMessages(id: List<Long>) {
        backgroundScope.launch {
            messageDao.deleteScheduleMessages(id)
        }
    }

    override fun getScheduleConversationWithMessages(): ArrayList<ScheduleConversation> {
        val scheduleConversation = ArrayList<ScheduleConversation>()
        val scheduleMessage = messageDao.getScheduleMessages()
        scheduleMessage.forEach {
            val conversation = messageDao.getConversationOfThreadId(it.threadId)
            scheduleConversation.add(
                ScheduleConversation(
                    conversation = conversation, message = it
                )
            )
        }
        return scheduleConversation
    }

    override fun updateConversationPinedData(threadIds: List<Long>, isPined: Boolean) {
        backgroundScope.launch {
            messageDao.updateConversationPinedData(threadIds, isPined)
        }
    }
}