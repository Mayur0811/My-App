package com.messages.common.receivers

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony.Sms
import com.messages.common.backgroundScope
import com.messages.common.message_utils.getSmsMessageFromDeliveryReport
import com.messages.data.events.RefreshMessages
import com.messages.data.repository.MessageRepository
import com.messages.utils.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@AndroidEntryPoint
class SmsStatusDeliveredReceiver : SendStatusReceiver() {

    @Inject
    lateinit var messageRepository: MessageRepository

    private var status: Int = Sms.Sent.STATUS_NONE

    override fun updateAndroidDatabase(context: Context, intent: Intent, receiverResultCode: Int) {
        val messageUri: Uri? = intent.data
        val smsMessage = intent.getSmsMessageFromDeliveryReport() ?: return

        try {
            val format = intent.getStringExtra("format")
            AppLogger.d("1234", "SmsStatusDelivered -> ${smsMessage.status}")
            status = smsMessage.status
            if ("3gpp2" == format) {
                val errorClass = status shr 24 and 0x03
                val statusCode = status shr 16 and 0x3f
                status = when (errorClass) {
                    0 -> {
                        if (statusCode == 0x02 /*STATUS_DELIVERED*/) {
                            Sms.STATUS_COMPLETE
                        } else {
                            Sms.STATUS_PENDING
                        }
                    }

                    2 -> {
                        Sms.STATUS_PENDING
                    }

                    3 -> {
                        Sms.STATUS_FAILED
                    }

                    else -> {
                        Sms.STATUS_PENDING
                    }
                }
            }
        } catch (e: NullPointerException) {
            return
        }

        updateSmsStatusAndDateSent(context, messageUri, System.currentTimeMillis())
    }

    private fun updateSmsStatusAndDateSent(
        context: Context,
        messageUri: Uri?,
        timeSentInMillis: Long = -1L
    ) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            if (status != Sms.Sent.STATUS_NONE) {
                put(Sms.Sent.STATUS, status)
            }
            put(Sms.Sent.DATE_SENT, timeSentInMillis)
        }

        if (messageUri != null) {
            resolver.update(messageUri, values, null, null)
        } else {
            // mark latest sms as delivered, need to check if this is still necessary (or reliable)
            val cursor = resolver.query(Sms.Sent.CONTENT_URI, null, null, null, "date desc")
            cursor?.use {
                if (cursor.moveToFirst()) {
                    @SuppressLint("Range")
                    val id = cursor.getString(cursor.getColumnIndex(Sms.Sent._ID))
                    val selection = "${Sms._ID} = ?"
                    val selectionArgs = arrayOf(id.toString())
                    resolver.update(Sms.Sent.CONTENT_URI, values, selection, selectionArgs)
                }
            }
        }
    }

    override fun updateAppDatabase(context: Context, intent: Intent, receiverResultCode: Int) {
        val messageUri: Uri? = intent.data
        AppLogger.d("1234", "SmsStatusDelivered -> $status")
        if (messageUri != null) {
            val messageId = messageUri.lastPathSegment?.toLong() ?: 0L
            backgroundScope.launch {
                if (status != Sms.Sent.STATUS_NONE) {
                    messageRepository.updateMessagesStatus(messageId, status)
                }
                EventBus.getDefault().post(RefreshMessages)
            }
        }
    }
}
