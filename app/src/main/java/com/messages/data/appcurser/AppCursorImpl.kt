package com.messages.data.appcurser

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.BlockedNumberContract
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.Telephony
import android.provider.Telephony.Mms
import android.provider.Telephony.Sms
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import com.messages.common.backgroundScope
import com.messages.common.common_utils.PhoneNumberUtils
import com.messages.common.getMessageType
import com.messages.common.mainScope
import com.messages.data.database.dao.MessageDao
import com.messages.data.models.AttachmentData
import com.messages.data.models.AttachmentWithMessageModel
import com.messages.data.models.BlockedNumberModel
import com.messages.data.models.Contact
import com.messages.data.models.Conversation
import com.messages.data.models.Message
import com.messages.data.models.PhoneNumber
import com.messages.data.models.Recipient
import com.messages.extentions.getSimSlotForSubscription
import com.messages.extentions.getThreadPhoneNumbers
import com.messages.extentions.normalizePhoneNumber
import com.messages.extentions.parseAttachmentNames
import com.messages.extentions.trimToComparableNumber
import com.messages.utils.AppLogger
import kotlinx.coroutines.launch
import javax.inject.Inject


class AppCursorImpl @Inject constructor(
    private val context: Context,
    private val messageDao: MessageDao
) : AppCursor {

    override fun getConversations(timeStamp: Long?): ArrayList<Conversation> {
        val uri = Uri.parse("${Telephony.Threads.CONTENT_URI}?simple=true")
        val projection = arrayOf(
            Telephony.Threads._ID,               // Thread ID
            Telephony.Threads.DATE,              // Date of the last message
            Telephony.Threads.READ,              // Whether the thread has been read (1 = read, 0 = unread)
            Telephony.Threads.SNIPPET,           // Snippet of the last message
            Telephony.Threads.RECIPIENT_IDS
        )

        var selection = "${Telephony.Threads.MESSAGE_COUNT} > ?"
        val selectionArgs = mutableListOf("0")
        if (timeStamp != null) {
            selection += " AND ${Telephony.Threads.DATE} > ?"
            selectionArgs.add(timeStamp.toString())
        }

        val finalSelectionArgs = selectionArgs.toTypedArray()
        val sortOrder = "${Telephony.Threads.DATE} DESC"

        val conversations = ArrayList<Conversation>()
        val blockedNumbers = getBlockedNumbers()
        val recipients = messageDao.getAllRecipients()

        context.queryCursor(uri, projection, selection, finalSelectionArgs, sortOrder) { cursor ->
            val threadId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Threads._ID))
            val snippet =
                cursor.getStringOrNull(cursor.getColumnIndexOrThrow(Telephony.Threads.SNIPPET))
                    ?: ""
            var date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Threads.DATE))
            if (date.toString().length > 10) date /= 1000

            val rawIds =
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS))

            val recipientIds =
                rawIds.split(" ").filter { it.isNotBlank() }.mapNotNull { it.toLongOrNull() }

            val phoneNumbers = getThreadAddresses(recipientIds, recipients)
            val read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Threads.READ)) == 1
            val type = getMessageType(phoneNumbers.first(), snippet)

            val conversation = Conversation(
                threadId = threadId,
                snippet = snippet,
                date = date,
                read = read,
                isGroupConversation = phoneNumbers.size > 1,
                isBlocked = phoneNumbers.any { isNumberBlocked(it, blockedNumbers) },
                type = type,
                recipients = recipientIds.map { id ->
                    Recipient().apply { recipientId = id }
                },
                name = recipientIds.joinToString { it.toString() },
            )

            conversations.add(conversation)
        }
        return conversations
    }

    private fun getThreadAddresses(
        recipientIds: List<Long>,
        recipients: List<Recipient>
    ): ArrayList<String> {
        val numbers = ArrayList<String>()
        recipientIds.forEach { recipientId ->
            numbers.add(recipients.firstOrNull { it.recipientId == recipientId }?.address ?: "")
        }
        return numbers
    }

    override fun getConversationsUsingThreadId(
        threadId: Long,
        onGetConversation: (Conversation) -> Unit
    ) {
        backgroundScope.launch {

            val uri = Uri.parse("${Telephony.Threads.CONTENT_URI}?simple=true")
            val projection = arrayOf(
                Telephony.Threads._ID,               // Thread ID
                Telephony.Threads.DATE,              // Date of the last message
                Telephony.Threads.READ,              // Whether the thread has been read (1 = read, 0 = unread)
                Telephony.Threads.SNIPPET,           // Snippet of the last message
                Telephony.Threads.RECIPIENT_IDS
            )

            val selection =
                "${Telephony.Threads.MESSAGE_COUNT} > ? AND ${Telephony.Threads._ID} = ?"
            val selectionArgs = arrayOf("0", threadId.toString())

            val sortOrder = "${Telephony.Threads.DATE} DESC"

            val blockedNumbers = getBlockedNumbers()

            val conversations = ArrayList<Conversation>()
            context.queryCursor(uri, projection, selection, selectionArgs, sortOrder) { cursor ->
                val newThreadId =
                    cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Threads._ID))
                val snippet =
                    cursor.getStringOrNull(cursor.getColumnIndexOrThrow(Telephony.Threads.SNIPPET))
                        ?: ""
                var date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Threads.DATE))
                if (date.toString().length > 10) {
                    date /= 1000
                }

                val rawIds =
                    cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Threads.RECIPIENT_IDS))

                val recipientIds =
                    rawIds.split(" ").filter { it.isNotBlank() }.map { it.toLong() }.toMutableList()

                val phoneNumbers = context.getThreadPhoneNumbers(recipientIds)
                val isBlocked = phoneNumbers.any { isNumberBlocked(it, blockedNumbers) }
                val isGroupConversation = phoneNumbers.size > 1
                val read =
                    cursor.getIntOrNull(cursor.getColumnIndexOrThrow(Telephony.Threads.READ)) == 1
                val type = getMessageType(phoneNumbers.first(), snippet)

                val recipients = arrayListOf<Recipient>()
                recipients.addAll(
                    recipientIds.map { idRecipient ->
                        Recipient().apply {
                            val recipient = messageDao.getRecipientsById(idRecipient)
                            recipientId = idRecipient
                            if (recipient != null) {
                                contact = recipient.contact
                                address = recipient.address
                                lastUpdate = recipient.lastUpdate
                            } else {
                                syncRecipientOfId(idRecipient) { newRecipient ->
                                    contact = newRecipient.contact
                                    address = newRecipient.address
                                    lastUpdate = newRecipient.lastUpdate
                                }
                            }
                        }
                    }
                )

                val conversation = Conversation(
                    threadId = newThreadId,
                    snippet = snippet,
                    date = date,
                    read = read,
                    isGroupConversation = isGroupConversation,
                    isBlocked = isBlocked,
                    type = type,
                    recipients = recipients,
                    name = recipients.joinToString { it.getDisplayName() }
                )

                conversations.add(conversation)
            }

            conversations.sortByDescending { it.date }
            mainScope.launch {
                onGetConversation.invoke(conversations.first())
            }
        }
    }

    private fun syncRecipientOfId(recipientId: Long, onGet: (Recipient) -> Unit) {
        backgroundScope.launch {
            val uri = Uri.parse("content://mms-sms/canonical-addresses")
            val contacts = messageDao.getAllContacts()
            val projection = arrayOf(Mms._ID, Mms.Addr.ADDRESS)

            val selection = "${Mms._ID} = ?"
            val selectionArgs = arrayOf(recipientId.toString())

            context.queryCursor(uri, projection, selection, selectionArgs, null) { cursor ->
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(Mms._ID))
                val address = cursor.getString(cursor.getColumnIndexOrThrow(Mms.Addr.ADDRESS))

                val contact = contacts.firstOrNull { contact ->
                    contact.phoneNumbers.any {
                        PhoneNumberUtils(context).compare(
                            address,
                            it.address
                        )
                    }
                }
                val recipient = Recipient(
                    recipientId = id,
                    address = address,
                    lastUpdate = System.currentTimeMillis() / 1000,
                    contact = contact
                )
                backgroundScope.launch {
                    messageDao.insertOrUpdateRecipients(arrayListOf(recipient))
                    onGet.invoke(recipient)
                }
                cursor.close()
            }
        }
    }

    override fun syncRecipients(onSyncDone: () -> Unit) {
        backgroundScope.launch {
            val uri = Uri.parse("content://mms-sms/canonical-addresses")
            val contacts = messageDao.getAllContacts()
            val recipients = ArrayList<Recipient>()

            val projection = arrayOf(
                Mms._ID,
                Mms.Addr.ADDRESS
            )
            context.queryCursor(uri, projection, null, null, null) { cursor ->
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(Mms._ID))
                val address = cursor.getString(cursor.getColumnIndexOrThrow(Mms.Addr.ADDRESS))

                val contact = contacts.firstOrNull { contact ->
                    contact.phoneNumbers.any {
                        PhoneNumberUtils(context).compare(address, it.address)
                    }
                }
                recipients.add(
                    Recipient(
                        recipientId = id,
                        address = address,
                        lastUpdate = System.currentTimeMillis() / 1000,
                        contact = contact
                    )
                )
            }
            messageDao.insertOrUpdateRecipients(recipients)
            onSyncDone.invoke()
        }
    }

    override fun getContacts(timeStamp: Long?): ArrayList<Contact> {
        val contactsMap = LinkedHashMap<String, Contact>()
        val uri = Uri.parse("${Phone.CONTENT_URI}")
        val projection = arrayOf(
            Phone._ID,
            Phone.LOOKUP_KEY,
            Phone.ACCOUNT_TYPE_AND_DATA_SET,
            Phone.NUMBER,
            Phone.TYPE,
            Phone.LABEL,
            Phone.DISPLAY_NAME,
            Phone.PHOTO_URI,
            Phone.STARRED,
            Phone.CONTACT_LAST_UPDATED_TIMESTAMP
        )

        context.queryCursor(uri, projection) { cursor ->

            val lookupKey =
                cursor.getString(cursor.getColumnIndexOrThrow(Phone.LOOKUP_KEY))

            val name =
                cursor.getString(cursor.getColumnIndexOrThrow(Phone.DISPLAY_NAME))

            val photoUri =
                cursor.getStringOrNull(cursor.getColumnIndexOrThrow(Phone.PHOTO_URI))

            var lastUpdate =
                cursor.getLong(cursor.getColumnIndexOrThrow(Phone.CONTACT_LAST_UPDATED_TIMESTAMP))

            if (lastUpdate > 9999999999L) {
                lastUpdate /= 1000
            }

            val isFavorite =
                cursor.getInt(cursor.getColumnIndexOrThrow(Phone.STARRED)) == 1

            val address = cursor.getString(cursor.getColumnIndexOrThrow(Phone.NUMBER))

            val contactId =
                cursor.getLong(cursor.getColumnIndexOrThrow(Phone._ID))

            val accountType =
                cursor.getString(cursor.getColumnIndexOrThrow(Phone.ACCOUNT_TYPE_AND_DATA_SET))

            val type = Phone.getTypeLabel(
                context.resources, cursor.getInt(cursor.getColumnIndexOrThrow(Phone.TYPE)),
                cursor.getString(cursor.getColumnIndexOrThrow(Phone.LABEL))
            ).toString()

            val phoneNumber = PhoneNumber(
                id = contactId,
                address = address,
                normalizeAddress = address.normalizePhoneNumber(),
                accountType = accountType,
                type = type
            )

            val contact = contactsMap[lookupKey]
            if (contact != null) {
                contact.phoneNumbers.add(phoneNumber) // Add phone number to existing contact
            } else {
                contactsMap[lookupKey] = Contact(
                    contactId = contactId,
                    lookupKey = lookupKey,
                    name = name,
                    photoUri = photoUri,
                    lastUpdate = lastUpdate,
                    isFavorite = isFavorite,
                    phoneNumbers = arrayListOf(phoneNumber)
                )
            }
        }
        return ArrayList(contactsMap.values)
    }

    override fun getMessages(timeStamp: Long?) {
        backgroundScope.launch {
            val uri = Sms.CONTENT_URI
            val projection = arrayOf(
                Sms._ID,
                Sms.DATE,
                Sms.READ,
                Sms.THREAD_ID,
                Sms.SUBSCRIPTION_ID,
                Sms.ADDRESS,
                Sms.BODY,
                Sms.SEEN,
                Sms.TYPE, // (1 = inbox, 2 = sent, 3 = draft, 4 = outbox, 5 = failed, 6 = queued)
                Sms.STATUS,
                Sms.ERROR_CODE
            )

            var selection: String? = null
            val selectionArgs = mutableListOf<String>()
            var sortOrder = "${Sms.DATE} DESC"

            if (timeStamp != null) {
                selection = "${Sms.DATE} > ?"
                selectionArgs.add(((timeStamp + 1) * 1000).toString())
            } else {
                sortOrder += " LIMIT 20"
            }
            val finalSelectionArgs = selectionArgs.toTypedArray()

            var messages = ArrayList<Message>()

            var threadId = 0L

            context.queryCursor(
                uri = uri,
                projection = projection,
                selection = selection,
                selectionArgs = finalSelectionArgs,
                sortOrder = sortOrder
            ) { cursor ->
                val messageId = cursor.getLong(cursor.getColumnIndexOrThrow(Sms._ID))
                threadId = cursor.getLong(cursor.getColumnIndexOrThrow(Sms.THREAD_ID))
                var date = cursor.getLong(cursor.getColumnIndexOrThrow(Mms.DATE))
                if (date.toString().length > 10) {
                    date /= 1000
                }
                val read = cursor.getInt(cursor.getColumnIndexOrThrow(Mms.READ)) == 1
                val subId =
                    cursor.getIntOrNull(cursor.getColumnIndexOrThrow(Sms.SUBSCRIPTION_ID)) ?: -1

                val address: String =
                    cursor.getString(cursor.getColumnIndexOrThrow(Sms.ADDRESS)) ?: ""
                val type: Int = cursor.getInt(cursor.getColumnIndexOrThrow(Sms.TYPE))
                val seen: Boolean = cursor.getInt(cursor.getColumnIndexOrThrow(Sms.SEEN)) != 0

                val body: String = cursor.getColumnIndexOrThrow(Sms.BODY)
                    .takeIf { column -> column != -1 } // The column may not be set
                    ?.let { column -> cursor.getString(column) }
                    ?: "" // cursor.getString() may return null

                val error: Int = cursor.getInt(cursor.getColumnIndexOrThrow(Sms.ERROR_CODE))
                val deliveryStatus: Int = cursor.getInt(cursor.getColumnIndexOrThrow(Sms.STATUS))

                val message = Message(
                    messageId = messageId,
                    body = body,
                    type = type,
                    date = date,
                    read = read,
                    threadId = threadId,
                    addresses = listOf(address),
                    seen = seen,
                    errorType = error,
                    deliveryStatus = deliveryStatus,
                    subId = subId,
                    simSlot = context.getSimSlotForSubscription(subId)
                )
                backgroundScope.launch {
                    messageDao.insertOrUpdateMessages(message)
                }
            }

            messages.addAll(getMMS(threadId, timeStamp, sortOrder))
            messages = messages.sortedWith(compareBy { it.date })
                .toMutableList() as ArrayList<Message>
            messages.forEach { message ->
                messageDao.insertOrUpdateMessages(message)
            }
        }
    }

    override fun getMessages(threadId: Long, timeStamp: Long?): ArrayList<Message> {
        val uri = Sms.CONTENT_URI
        val projection = arrayOf(
            Sms._ID,
            Sms.DATE,
            Sms.READ,
            Sms.THREAD_ID,
            Sms.SUBSCRIPTION_ID,
            Sms.ADDRESS,
            Sms.BODY,
            Sms.SEEN,
            Sms.TYPE, // (1 = inbox, 2 = sent, 3 = draft, 4 = outbox, 5 = failed, 6 = queued)
            Sms.STATUS,
            Sms.ERROR_CODE
        )

        var selection = "${Sms.THREAD_ID} = ?"
        val selectionArgs = mutableListOf(threadId.toString())
        var sortOrder = "${Sms.DATE} DESC"
        if (timeStamp != null) {
            selection += " AND ${Sms.DATE} > ?"
            selectionArgs.add(((timeStamp + 1) * 1000).toString())
        } else {
            sortOrder += " LIMIT 20"
        }
        val finalSelectionArgs = selectionArgs.toTypedArray()

        var messages = ArrayList<Message>()

        context.queryCursor(
            uri = uri,
            projection = projection,
            selection = selection,
            selectionArgs = finalSelectionArgs,
            sortOrder = sortOrder
        ) { cursor ->

            val messageId = cursor.getLong(cursor.getColumnIndexOrThrow(Sms._ID))
            val msgThreadId = cursor.getLong(cursor.getColumnIndexOrThrow(Sms.THREAD_ID))
            var date = cursor.getLong(cursor.getColumnIndexOrThrow(Mms.DATE))
            if (date.toString().length > 10) {
                date /= 1000
            }
            val read = cursor.getInt(cursor.getColumnIndexOrThrow(Mms.READ)) == 1
            val subId = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(Sms.SUBSCRIPTION_ID)) ?: -1

            val address: String = cursor.getString(cursor.getColumnIndexOrThrow(Sms.ADDRESS)) ?: ""
            val type: Int = cursor.getInt(cursor.getColumnIndexOrThrow(Sms.TYPE))
            val seen: Boolean = cursor.getInt(cursor.getColumnIndexOrThrow(Sms.SEEN)) != 0

            val body: String = cursor.getColumnIndexOrThrow(Sms.BODY)
                .takeIf { column -> column != -1 } // The column may not be set
                ?.let { column -> cursor.getString(column) }
                ?: "" // cursor.getString() may return null

            val error: Int = cursor.getInt(cursor.getColumnIndexOrThrow(Sms.ERROR_CODE))
            val deliveryStatus: Int = cursor.getInt(cursor.getColumnIndexOrThrow(Sms.STATUS))

            val message = Message(
                messageId = messageId,
                body = body,
                type = type,
                date = date,
                read = read,
                threadId = msgThreadId,
                addresses = listOf(address),
                seen = seen,
                errorType = error,
                deliveryStatus = deliveryStatus,
                subId = subId,
                simSlot = context.getSimSlotForSubscription(subId)
            )
            messages.add(message)
        }

        messages.addAll(getMMS(threadId, timeStamp, sortOrder))
        messages = messages.sortedWith(compareBy { it.date })
            .toMutableList() as ArrayList<Message>
        return messages
    }


    override fun getBlockedNumbers(): ArrayList<BlockedNumberModel> {
        val blockedNumbers = ArrayList<BlockedNumberModel>()

        val uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI
        val projection = arrayOf(
            BlockedNumberContract.BlockedNumbers.COLUMN_ID,
            BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
            BlockedNumberContract.BlockedNumbers.COLUMN_E164_NUMBER
        )

        context.queryCursor(uri, projection) { cursor ->
            val id =
                cursor.getLong(cursor.getColumnIndexOrThrow(BlockedNumberContract.BlockedNumbers.COLUMN_ID))
            val number =
                cursor.getString(cursor.getColumnIndexOrThrow(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER))
                    ?: ""
            val normalizedNumber =
                cursor.getString(cursor.getColumnIndexOrThrow(BlockedNumberContract.BlockedNumbers.COLUMN_E164_NUMBER))
                    ?: number
            val comparableNumber = normalizedNumber.trimToComparableNumber()
            val blockedNumber = BlockedNumberModel(id, number, normalizedNumber, comparableNumber)
            blockedNumbers.add(blockedNumber)
        }

        return blockedNumbers
    }

    override fun isNumberBlocked(
        number: String,
        blockedNumbers: ArrayList<BlockedNumberModel>?
    ): Boolean {
        var numbers = blockedNumbers
        if (numbers == null) {
            numbers = getBlockedNumbers()
        }
        val numberToCompare = number.trimToComparableNumber()
        return numbers.map { it.numberToCompare }
            .contains(numberToCompare) || numbers.map { it.number }.contains(numberToCompare)
    }

    private fun getMMS(
        threadId: Long?,
        timeStamp: Long?,
        sortOrder: String?,
    ): ArrayList<Message> {
        val uri = Mms.CONTENT_URI
        val projection = arrayOf(
            Mms._ID,
            Mms.DATE,
            Mms.READ,
            Mms.MESSAGE_BOX,
            Mms.THREAD_ID,
            Mms.SUBSCRIPTION_ID,
            Mms.STATUS
        )

        var selection: String? = null
        var selectionArgs: MutableList<String>? = null

        if (threadId != null) {
            selection = "${Sms.THREAD_ID} = ?"
            selectionArgs = mutableListOf(threadId.toString())
        }
        if (timeStamp != null) {
            selection += " AND ${Sms.DATE} > ?"
            selectionArgs?.add(timeStamp.toString())
        }

        val messages = ArrayList<Message>()
        val mmsIds = ArrayList<Long?>()
        context.queryCursor(
            uri,
            projection,
            selection,
            selectionArgs?.toTypedArray(),
            sortOrder,
        ) { cursor ->

            val mmsId = cursor.getLong(cursor.getColumnIndexOrThrow(Mms._ID))
            var date = cursor.getLong(cursor.getColumnIndexOrThrow(Mms.DATE))
            if (date.toString().length > 10) {
                date /= 1000
            }
            val read = cursor.getInt(cursor.getColumnIndexOrThrow(Mms.READ)) == 1
            val msgThreadId = cursor.getLong(cursor.getColumnIndexOrThrow(Mms.THREAD_ID))
            val subscriptionId =
                cursor.getInt(cursor.getColumnIndexOrThrow(Mms.SUBSCRIPTION_ID))

            val attachment = getMmsAttachment(mmsId)
            val body = attachment.text

            val address = getMmsAddress(mmsId)
            val type = cursor.getInt(cursor.getColumnIndexOrThrow(Mms.MESSAGE_BOX))
            val mmsStatus = cursor.getInt(cursor.getColumnIndexOrThrow(Mms.STATUS))
            val textContentType = ""

            val message = Message(
                messageId = mmsId,
                body = body,
                type = type,
                date = date,
                read = read,
                threadId = msgThreadId,
                addresses = listOf(address),
                mmsStatus = mmsStatus,
                textContentType = textContentType,
                subId = subscriptionId,
                isMMS = true,
                attachmentWithMessageModel = attachment,
                simSlot = context.getSimSlotForSubscription(subscriptionId)
            )
            if (!mmsIds.contains(message.messageId)) {
                messages.add(message)
                mmsIds.add(message.messageId)
            }
        }

        return messages
    }


    private fun getMmsAttachment(id: Long): AttachmentWithMessageModel {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Mms.Part.CONTENT_URI
        } else {
            Uri.parse("content://mms/part")
        }

        val projection = arrayOf(
            Mms._ID,
            Mms.Part.CONTENT_TYPE,
            Mms.Part.TEXT,
            Mms.Part.CONTENT_LOCATION
        )
        val selection = "${Mms.Part.MSG_ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        val messageAttachment = AttachmentWithMessageModel(id, "", arrayListOf())

        var attachmentNames: List<String>? = null
        var attachmentCount = 0
        context.queryCursor(uri, projection, selection, selectionArgs) { cursor ->
            val partId = cursor.getLong(cursor.getColumnIndexOrThrow(Mms._ID))
            val mimetype =
                cursor.getString(cursor.getColumnIndexOrThrow(Mms.Part.CONTENT_TYPE))
            val fileName = cursor.getString(cursor.getColumnIndexOrThrow(Mms.Part.CONTENT_LOCATION))
            if (mimetype == "text/plain") {
                messageAttachment.text =
                    cursor.getString(cursor.getColumnIndexOrThrow(Mms.Part.TEXT)) ?: ""
            } else if (mimetype.startsWith("image/") || mimetype.startsWith("video/")) {
                val fileUri = Uri.withAppendedPath(uri, partId.toString())

                val attachment =
                    AttachmentData(
                        id = fileUri.toString(),
                        mimetype = mimetype,
                        filename = fileName,
                        uri = fileUri
                    )
                messageAttachment.attachments.add(attachment)
            } else if (mimetype.startsWith("audio/")) {
                val fileUri = Uri.withAppendedPath(uri, partId.toString())
                val attachment = AttachmentData(
                    id = fileUri.toString(),
                    mimetype = mimetype,
                    filename = fileName,
                    uri = fileUri
                )
                messageAttachment.attachments.add(attachment)
            } else if (mimetype != "application/smil") {
                val fileUri = Uri.withAppendedPath(uri, partId.toString())
                val attachmentName = attachmentNames?.getOrNull(attachmentCount) ?: ""
                val attachment = AttachmentData(
                    id = fileUri.toString(),
                    mimetype = mimetype,
                    filename = attachmentName,
                    uri = fileUri
                )
                messageAttachment.attachments.add(attachment)
                attachmentCount++
            } else {
                val text = cursor.getString(cursor.getColumnIndexOrThrow(Mms.Part.TEXT))
                attachmentNames = parseAttachmentNames(text)
            }
        }

        return messageAttachment
    }

    private fun getMmsAddress(messageId: Long): String {
        val uri = Mms.CONTENT_URI.buildUpon()
            .appendPath(messageId.toString())
            .appendPath("addr").build()

        val projection = arrayOf(Mms.Addr.ADDRESS, Mms.Addr.CHARSET)
        val selection = "${Mms.Addr.TYPE} = 0x89"

        val cursor = context.contentResolver.query(uri, projection, selection, null, null)
        cursor?.use {
            if (cursor.moveToFirst()) {
                return cursor.getString(0) ?: ""
            }
        }

        return ""
    }

    override fun updateMessageType(id: Long, type: Int, status: Int) {
        backgroundScope.launch {
            val uri = Sms.CONTENT_URI
            val contentValues = ContentValues().apply {
                put(Sms.TYPE, type)
                put(Sms.STATUS, status)
            }
            val selection = "${Sms._ID} = ?"
            val selectionArgs = arrayOf(id.toString())
            context.contentResolver.update(uri, contentValues, selection, selectionArgs)
        }
    }

    override fun getMessageRecipientAddress(messageId: Long): String {
        val uri = Sms.CONTENT_URI
        val projection = arrayOf(
            Sms.ADDRESS
        )

        val selection = "${Sms._ID} = ?"
        val selectionArgs = arrayOf(messageId.toString())

        try {
            val cursor =
                context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use {
                if (cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndexOrThrow(Sms.ADDRESS))
                }
            }
        } catch (e: Exception) {
            AppLogger.e(message = "getMessageRecipientAddress -> ${e.message}")
        }

        return ""
    }

    override fun getNameOfRecipient(address: String): String {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(address)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        try {
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor.use {
                if (cursor?.moveToFirst() == true) {
                    val name =
                        cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                    return name
                }
            }
        } catch (e: Exception) {
            AppLogger.e(message = "getNameOfRecipient -> ${e.message}")
        }
        return address

    }

    override fun insertNewSmsToDevice(message: Message): Long {
        val uri = Sms.CONTENT_URI
        val contentValues = ContentValues().apply {
            put(Sms.ADDRESS, message.addresses.first())
            put(Sms.SUBJECT, message.subject)
            put(Sms.BODY, message.body)
            put(Sms.DATE, message.date)
            put(Sms.READ, message.read)
            put(Sms.THREAD_ID, message.threadId)
            put(Sms.TYPE, message.type)
            put(Sms.SUBSCRIPTION_ID, message.subId)
        }

        return try {
            val newUri = context.contentResolver.insert(uri, contentValues)
            newUri?.lastPathSegment?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    override fun markFirstAsSent() {
        val query = context.contentResolver.query(
            Uri.parse("content://sms/outbox"),
            null as Array<String?>?,
            null as String?,
            null as Array<String?>?,
            null as String?
        )
        if (query != null && query.moveToFirst()) {
            val id = query.getString(query.getColumnIndexOrThrow("_id"))
            val values = ContentValues()
            values.put("type", 2)
            values.put("read", 1)
            context.contentResolver.update(
                Uri.parse("content://sms/outbox"), values,
                "_id=$id", null as Array<String?>?
            )
            query.close()
        }
    }
}

fun Context.queryCursor(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
    callback: (cursor: Cursor) -> Unit
) {
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    callback(cursor)
                } while (cursor.moveToNext())
            }
        }
    } catch (e: Exception) {
        AppLogger.e(message = "error queryCursor $uri => ${e.message}")
    }
}

