package com.messages.common.message_utils

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.PhoneNumberUtils
import com.messages.common.message_utils.SmsException.Companion.EMPTY_DESTINATION_ADDRESS
import com.messages.common.message_utils.SmsException.Companion.ERROR_SENDING_MESSAGE
import com.messages.common.receivers.SmsStatusDeliveredReceiver
import com.messages.common.receivers.SmsStatusSentReceiver
import com.messages.utils.ACTION_SMS_DELIVERED
import com.messages.utils.ACTION_SMS_SENT
import com.messages.utils.AppLogger
import com.messages.utils.EXTRA_SUB_ID

/** Class that sends chat message via SMS. */
class SmsSender(val app: Application) {

    // not sure what to do about this yet. this is the default as per android-smsmms
    private val sendMultipartSmsAsSeparateMessages = false

    // This should be called from a RequestWriter queue thread
    fun sendMessage(
        subId: Int, destination: String, body: String, serviceCenter: String?,
        requireDeliveryReport: Boolean, messageUri: Uri
    ) {
        var dest = destination
        if (body.isEmpty()) {
            throw IllegalArgumentException("SmsSender: empty text message")
        }
        // remove spaces and dashes from destination number
        // (e.g. "801 555 1212" -> "8015551212")
        // (e.g. "+8211-123-4567" -> "+82111234567")
        dest = PhoneNumberUtils.stripSeparators(dest)

        if (dest.isEmpty()) {
            throw SmsException(EMPTY_DESTINATION_ADDRESS)
        }
        // Divide the input message by SMS length limit
        val smsManager = getSmsManager(subId)
        val messages = smsManager.divideMessage(body)
        if (messages == null || messages.size < 1) {
            throw SmsException(ERROR_SENDING_MESSAGE)
        }
        // Actually send the sms
        sendInternal(
            subId, dest, messages, serviceCenter, requireDeliveryReport, messageUri
        )
    }


    // Actually sending the message using SmsManager
    private fun sendInternal(
        subId: Int, dest: String,
        messages: ArrayList<String>, serviceCenter: String?,
        requireDeliveryReport: Boolean, messageUri: Uri
    ) {
        val smsManager = getSmsManager(subId)
        val messageCount = messages.size
        val deliveryIntents = ArrayList<PendingIntent?>(messageCount)
        val sentIntents = ArrayList<PendingIntent>(messageCount)

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= 31) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }

        for (i in 0 until messageCount) {
            // Make pending intents different for each message part
            val partId = if (messageCount <= 1) 0 else i + 1
            if (requireDeliveryReport && i == messageCount - 1) {
                deliveryIntents.add(
                    PendingIntent.getBroadcast(
                        app,
                        partId,
                        getDeliveredStatusIntent(messageUri, subId),
                        flags
                    )
                )
            } else {
                deliveryIntents.add(null)
            }
            sentIntents.add(
                PendingIntent.getBroadcast(
                    app,
                    partId,
                    getSendStatusIntent(messageUri, subId),
                    flags
                )
            )
        }
        try {
            if (sendMultipartSmsAsSeparateMessages) {
                // If multipart sms is not supported, send them as separate messages
                for (i in 0 until messageCount) {
                    smsManager.sendTextMessage(
                        dest,
                        serviceCenter,
                        messages[i],
                        sentIntents[i],
                        deliveryIntents[i]
                    )
                }
            } else {
                smsManager.sendMultipartTextMessage(
                    dest, serviceCenter, messages, sentIntents, deliveryIntents
                )
            }
        } catch (e: Exception) {
            AppLogger.e("1234","sendInternal -> ${e.message}")
            throw SmsException(ERROR_SENDING_MESSAGE, e)
        }
    }

    private fun getSendStatusIntent(requestUri: Uri, subId: Int): Intent {

        val intent = Intent(
            ACTION_SMS_SENT,
            requestUri,
            app,
            SmsStatusSentReceiver::class.java
        )
        intent.putExtra(EXTRA_SUB_ID, subId)
        return intent
    }

    private fun getDeliveredStatusIntent(requestUri: Uri, subId: Int): Intent {
        val intent = Intent(
            ACTION_SMS_DELIVERED,
            requestUri,
            app,
            SmsStatusDeliveredReceiver::class.java
        )
        intent.putExtra(EXTRA_SUB_ID, subId)
        return intent
    }

    companion object {
        private var instance: SmsSender? = null
        fun getInstance(app: Application): SmsSender {
            if (instance == null) {
                instance = SmsSender(app)
            }
            return instance!!
        }
    }
}
