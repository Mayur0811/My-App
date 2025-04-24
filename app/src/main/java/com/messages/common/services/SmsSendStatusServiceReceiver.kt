package com.messages.common.services

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import com.klinker.android.send_message.BroadcastUtils
import com.messages.common.backgroundScope
import com.messages.data.events.RefreshMessages
import com.messages.data.repository.MessageRepository
import com.messages.extentions.getUri
import com.messages.utils.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class SmsSendStatusServiceReceiver : BroadcastReceiver() {

    @Inject
    lateinit var messageRepository: MessageRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        AppLogger.d("1234", "SmsSendStatusServiceReceiver")
        if (context != null) {
            onMessageStatusUpdated(intent)
            val uri = intent?.getUri()
            try {
                when (resultCode) {
                    -1 -> {
                        val delivered = Intent("com.klinker.android.send_message.NOTIFY_DELIVERY")
                        delivered.putExtra("result", true)
                        delivered.putExtra("message_uri", uri?.toString() ?: "")
                        BroadcastUtils.sendExplicitBroadcast(
                            context,
                            delivered,
                            "com.klinker.android.send_message.NOTIFY_DELIVERY"
                        )
                        if (uri != null) {
                            val values = ContentValues()
                            values.put("status", "0")
                            values.put("date_sent", Calendar.getInstance().timeInMillis)
                            values.put("read", true)
                            context.contentResolver.update(
                                uri,
                                values,
                                null as String?,
                                null as Array<String?>?
                            )
                        } else {
                            val query = context.contentResolver.query(
                                Uri.parse("content://sms/sent"),
                                null as Array<String?>?,
                                null as String?,
                                null as Array<String?>?,
                                "date desc"
                            )
                            if (query?.moveToFirst() == true) {
                                val id = query.getString(query.getColumnIndexOrThrow("_id"))
                                val values = ContentValues()
                                values.put("status", "0")
                                values.put("date_sent", Calendar.getInstance().timeInMillis)
                                values.put("read", true)
                                context.contentResolver.update(
                                    Uri.parse("content://sms/sent"), values,
                                    "_id=$id", null as Array<String?>?
                                )
                            }

                            query?.close()
                        }
                    }

                    0 -> {
                        val notDelivered =
                            Intent("com.klinker.android.send_message.NOTIFY_DELIVERY")
                        notDelivered.putExtra("result", false)
                        notDelivered.putExtra("message_uri", uri?.toString() ?: "")
                        BroadcastUtils.sendExplicitBroadcast(
                            context,
                            notDelivered,
                            "com.klinker.android.send_message.NOTIFY_DELIVERY"
                        )
                        if (uri != null) {
                            val values = ContentValues()
                            values.put("status", "64")
                            values.put("date_sent", Calendar.getInstance().timeInMillis)
                            values.put("read", true)
                            values.put("error_code", resultCode)
                            context.contentResolver.update(
                                uri,
                                values,
                                null as String?,
                                null as Array<String?>?
                            )
                        } else {
                            val query2 = context.contentResolver.query(
                                Uri.parse("content://sms/sent"),
                                null as Array<String?>?,
                                null as String?,
                                null as Array<String?>?,
                                "date desc"
                            )
                            if (query2?.moveToFirst() == true) {
                                val id = query2.getString(query2.getColumnIndexOrThrow("_id"))
                                val values = ContentValues()
                                values.put("status", "64")
                                values.put("read", true)
                                values.put("error_code", resultCode)
                                context.contentResolver.update(
                                    Uri.parse("content://sms/sent"), values,
                                    "_id=$id", null as Array<String?>?
                                )
                            }

                            query2?.close()
                        }
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }

            BroadcastUtils.sendExplicitBroadcast(
                context,
                Intent(),
                "com.klinker.android.send_message.REFRESH"
            )
        }
    }


    private fun onMessageStatusUpdated(intent: Intent?) {
        if (intent?.extras?.containsKey("message_uri") == true) {
            val uri = Uri.parse(intent.getStringExtra("message_uri"))
            val messageID = uri?.lastPathSegment?.toLong() ?: 0L
            backgroundScope.launch {
                val status = Telephony.Sms.STATUS_COMPLETE
                messageRepository.updateMessagesTypeWithCursor(messageID, status) {
                    if (it == 0) {
                        backgroundScope.launch {
                            delay(2000)
                            messageRepository.updateMessagesStatus(messageID, status)
                        }
                    }
                    EventBus.getDefault().post(RefreshMessages)
                }
            }
        }
    }
}
