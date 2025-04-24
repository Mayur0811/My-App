package com.messages.common.message_utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony.Sms
import android.provider.Telephony.Threads
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.telephony.SubscriptionManager
import android.widget.Toast.LENGTH_LONG
import com.klinker.android.send_message.Settings
import com.klinker.android.send_message.Transaction
import com.messages.R
import com.messages.common.message_utils.SmsException.Companion.EMPTY_DESTINATION_ADDRESS
import com.messages.common.message_utils.SmsException.Companion.ERROR_PERSISTING_MESSAGE
import com.messages.common.message_utils.SmsException.Companion.ERROR_SENDING_MESSAGE
import com.messages.common.receivers.MmsSentReceiver
import com.messages.data.models.AttachmentData
import com.messages.data.models.Conversation
import com.messages.data.models.Message
import com.messages.data.models.Recipient
import com.messages.data.pref.AppPreferences
import com.messages.extentions.generateRandomId
import com.messages.extentions.getStringValue
import com.messages.extentions.toast
import com.messages.utils.NO_ERROR_CODE


const val ADDRESS_SEPARATOR = "|"

@SuppressLint("NewApi")
fun Context.getThreadIdOfGroup(addresses: Set<String>): Long {
    return try {
        Threads.getOrCreateThreadId(this, addresses)
    } catch (e: Exception) {
        0L
    }
}

@SuppressLint("NewApi")
fun Context.getThreadId(address: String): Long {
    return try {
        Threads.getOrCreateThreadId(this, address)
    } catch (e: Exception) {
        0L
    }
}

fun Context.insertSmsMessage(
    subId: Int, dest: String, text: String, timestamp: Long, threadId: Long,
    status: Int = Sms.STATUS_NONE, type: Int = Sms.MESSAGE_TYPE_OUTBOX, messageId: Long? = null
): Uri {
    val response: Uri?
    val values = ContentValues().apply {
        put(Sms.ADDRESS, dest)
        put(Sms.DATE, timestamp)
        put(Sms.READ, 1)
        put(Sms.SEEN, 1)
        put(Sms.BODY, text)

        // insert subscription id only if it is a valid one.
        if (subId != Settings.DEFAULT_SUBSCRIPTION_ID) {
            put(Sms.SUBSCRIPTION_ID, subId)
        }

        if (status != Sms.STATUS_NONE) {
            put(Sms.STATUS, status)
        }
        if (type != Sms.MESSAGE_TYPE_ALL) {
            put(Sms.TYPE, type)
        }
        if (threadId != -1L) {
            put(Sms.THREAD_ID, threadId)
        }
    }

    try {
        if (messageId != null) {
            val selection = "${Sms._ID} = ?"
            val selectionArgs = arrayOf(messageId.toString())
            val count = contentResolver.update(Sms.CONTENT_URI, values, selection, selectionArgs)
            response = if (count > 0) {
                Uri.parse("${Sms.CONTENT_URI}/${messageId}")
            } else {
                null
            }
        } else {
            response = contentResolver.insert(Sms.CONTENT_URI, values)
        }
    } catch (e: Exception) {
        throw SmsException(ERROR_PERSISTING_MESSAGE, e)
    }
    return response ?: throw SmsException(ERROR_PERSISTING_MESSAGE)
}

/** Send an SMS message given [text] and [addresses]. A [SmsException] is thrown in case any errors occur. */
fun Context.sendSmsMessage(
    text: String,
    addresses: Set<String>,
    subId: Int,
    requireDeliveryReport: Boolean,
    messageId: Long? = null
) {
    if (addresses.size > 1) {
        // insert a dummy message for this thread if it is a group message
        val threadId = getThreadIdOfGroup(addresses.toSet())
        val mergedAddresses = addresses.joinToString(ADDRESS_SEPARATOR)
        insertSmsMessage(
            subId = subId, dest = mergedAddresses, text = text,
            timestamp = System.currentTimeMillis(), threadId = threadId,
            status = Sms.Sent.STATUS_COMPLETE, type = Sms.Sent.MESSAGE_TYPE_SENT,
            messageId = messageId
        )
    }

    for (address in addresses) {
        val threadId = getThreadId(address)
        val messageUri = insertSmsMessage(
            subId = subId, dest = address, text = text,
            timestamp = System.currentTimeMillis(), threadId = threadId,
            messageId = messageId, status = Sms.Sent.STATUS_PENDING
        )
        try {
            SmsSender.getInstance(applicationContext as Application).sendMessage(
                subId = subId, destination = address, body = text, serviceCenter = null,
                requireDeliveryReport = requireDeliveryReport, messageUri = messageUri
            )
        } catch (e: Exception) {
            updateSmsMessageSendingStatus(
                messageUri,
                Sms.Outbox.MESSAGE_TYPE_FAILED,
                Sms.Outbox.STATUS_FAILED
            )
            throw e // propagate error to caller
        }
    }
}

fun Context.maybeShowErrorToast(resultCode: Int, errorCode: Int) {
    if (resultCode != Activity.RESULT_OK) {
        val msg = if (errorCode != NO_ERROR_CODE) {
            getStringValue(R.string.carrier_send_error)
        } else {
            when (resultCode) {
                SmsManager.RESULT_ERROR_NO_SERVICE -> getStringValue(R.string.error_service_is_unavailable)
                SmsManager.RESULT_ERROR_RADIO_OFF -> getStringValue(R.string.error_radio_turned_off)
                SmsManager.RESULT_NO_DEFAULT_SMS_APP -> getStringValue(R.string.sim_card_not_available)
                else -> getString(R.string.unknown_error_occurred_sending_message, resultCode)
            }
        }
        toast(msg = msg, length = LENGTH_LONG)
    }
}

fun Context.updateSmsMessageSendingStatus(messageUri: Uri?, type: Int, status: Int = Sms.STATUS_NONE) {
    val resolver = contentResolver
    val values = ContentValues().apply {
        put(Sms.Outbox.TYPE, type)
        put(Sms.Outbox.STATUS, status)
    }

    try {
        if (messageUri != null) {
            resolver.update(messageUri, values, null, null)
        } else {
            // mark latest sms as sent, need to check if this is still necessary (or reliable)
            // as this was taken from android-smsmms. The messageUri shouldn't be null anyway
            val cursor = resolver.query(Sms.Outbox.CONTENT_URI, null, null, null, null)
            cursor?.use {
                if (cursor.moveToFirst()) {
                    @SuppressLint("Range")
                    val id = cursor.getString(cursor.getColumnIndex(Sms.Outbox._ID))
                    val selection = "${Sms._ID} = ?"
                    val selectionArgs = arrayOf(id.toString())
                    resolver.update(Sms.Outbox.CONTENT_URI, values, selection, selectionArgs)
                }
            }
        }
    } catch (e: Exception) {
        toast(String.format(getString(R.string.an_error_occurred), e.message))
    }
}

fun Intent.getSmsMessageFromDeliveryReport(): SmsMessage? {
    val pdu = this.getByteArrayExtra("pdu")
    val format = this.getStringExtra("format")
    return SmsMessage.createFromPdu(pdu, format)
}


fun Context.sendMmsMessage(
    text: String,
    addresses: List<String>,
    attachment: AttachmentData?,
    settings: Settings,
    messageId: Long? = null
) {
    val transaction = Transaction(this, settings)
    val message = com.klinker.android.send_message.Message(text, addresses.toTypedArray())

    if (attachment?.uri != null) {
        try {
            contentResolver.openInputStream(attachment.uri)?.use {
                val bytes = it.readBytes()
                val mimeType = if (attachment.mimetype.lowercase() == "text/plain") {
                    "application/txt"
                } else {
                    attachment.mimetype
                }
                val name = attachment.filename
                message.addMedia(bytes, mimeType, name)
            }
        } catch (e: Exception) {
            toast(String.format(getString(R.string.an_error_occurred), e.message))
        } catch (e: Error) {
            toast(
                String.format(
                    getString(R.string.an_error_occurred),
                    e.localizedMessage ?: getStringValue(R.string.unknown_error_occurred)
                )
            )
        }
//        message.setImage(BitmapFactory.decodeFile(attachment.path,BitmapFactory.Options()))
    }
    try {
        val mmsSentIntent = Intent(this, MmsSentReceiver::class.java)
        mmsSentIntent.putExtra(MmsSentReceiver.EXTRA_ORIGINAL_RESENT_MESSAGE_ID, messageId)
        transaction.setExplicitBroadcastForSentMms(mmsSentIntent)

        val sentIntent =
            PendingIntent.getBroadcast(this, 0, Intent("MMS_SENT"), PendingIntent.FLAG_IMMUTABLE)
        val deliveryIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent("MMS_DELIVERED"),
            PendingIntent.FLAG_IMMUTABLE
        )

        //    transaction.sendNewMessage(message, Transaction.NO_THREAD_ID)
        transaction.sendNewMessage(message, Transaction.NO_THREAD_ID, sentIntent, deliveryIntent)
    } catch (e: Exception) {
        toast(String.format(getString(R.string.an_error_occurred), e.message))
    }
}

fun Context.getSendMessageSettings(): Settings {
    val settings = Settings()
    settings.useSystemSending = true
    settings.deliveryReports = AppPreferences(context = this).deliveryReports
    settings.sendLongAsMmsAfter = 1
    settings.group = true
    return settings
}

fun Context.isLongMmsMessage(text: String, settings: Settings = getSendMessageSettings()): Boolean {
    val data = SmsMessage.calculateLength(text, false)
    val numPages = data.first()
    return numPages > settings.sendLongAsMmsAfter && settings.sendLongAsMms
}


fun Context.sendMessageCompat(
    text: String,
    addresses: List<String>,
    subId: Int?,
    attachments: List<AttachmentData>,
    messageId: Long? = null
) {
    val settings = getSendMessageSettings()
    if (subId != null) {
        settings.subscriptionId = subId
    }

    val isMms = attachments.isNotEmpty() || isLongMmsMessage(
        text,
        settings
    ) || addresses.size > 1 && settings.group
    if (isMms) {
        // we send all MMS attachments separately to reduces the chances of hitting provider MMS limit.
        if (attachments.isNotEmpty()) {
            val lastIndex = attachments.lastIndex
            if (attachments.size > 1) {
                for (i in 0 until lastIndex) {
                    val attachment = attachments[i]
                    sendMmsMessage("", addresses, attachment, settings, messageId)
                }
            }
            val lastAttachment = attachments[lastIndex]
            sendMmsMessage(text, addresses, lastAttachment, settings, messageId)
        } else {
            sendMmsMessage(text, addresses, null, settings, messageId)
        }
    } else {
        try {
            sendSmsMessage(
                text,
                addresses.toSet(),
                settings.subscriptionId,
                settings.deliveryReports,
                messageId
            )
        } catch (e: SmsException) {
            when (e.errorCode) {
                EMPTY_DESTINATION_ADDRESS -> toast(
                    getStringValue(R.string.empty_destination_address),
                    length = LENGTH_LONG
                )

                ERROR_PERSISTING_MESSAGE -> toast(
                    getStringValue(R.string.unable_to_save_message),
                    length = LENGTH_LONG
                )

                ERROR_SENDING_MESSAGE -> toast(
                    msg = getString(R.string.unknown_error_occurred_sending_message, e.errorCode),
                    length = LENGTH_LONG
                )
            }
        } catch (e: Exception) {
            toast(String.format(getString(R.string.an_error_occurred), e.message))
        }
    }

}

@SuppressLint("MissingPermission")
fun Context.createTemporaryThread(
    message: Message,
    threadId: Long = generateRandomId(),
    sendAddresses: ArrayList<String>,
    cachedConv: Conversation?,
    onDone: (Conversation) -> Unit
) {
    val title = cachedConv?.name ?: message.name ?: ""
    val subscriptionManager =
        getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

    val subscriptionIdToSimId = java.util.HashMap<Int, String>()
    subscriptionIdToSimId[-1] = "?"
    subscriptionManager.activeSubscriptionInfoList?.forEachIndexed { index, subscriptionInfo ->
        subscriptionIdToSimId[subscriptionInfo.subscriptionId] = "${index + 1}"
    }

    val conversation = Conversation(
        threadId = threadId,
        snippet = if (message.isMMS) getStringValue(R.string.attachment) else message.body,
        date = message.date,
        read = true,
        name = title.ifEmpty { sendAddresses.joinToString { it } },
        isGroupConversation = sendAddresses.size > 1,
        isArchived = false,
        isSchedule = true,
        recipients = ArrayList<Recipient>().apply {
            sendAddresses.map { sendAddress -> add(Recipient().apply { address = sendAddress }) }
        }
    )
    try {
        onDone.invoke(conversation)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}



