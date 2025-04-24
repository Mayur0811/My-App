package com.messages.common.receivers

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony.Sms
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.messages.common.backgroundScope
import com.messages.common.mainScope
import com.messages.common.message_utils.NotificationHelper
import com.messages.common.message_utils.getSmsMessageFromDeliveryReport
import com.messages.common.message_utils.getThreadId
import com.messages.common.message_utils.maybeShowErrorToast
import com.messages.common.message_utils.updateSmsMessageSendingStatus
import com.messages.data.events.RefreshMessages
import com.messages.data.events.UpdateLastSendMessageStatus
import com.messages.data.repository.MessageRepository
import com.messages.utils.AppLogger
import com.messages.utils.EXTRA_ERROR_CODE
import com.messages.utils.NO_ERROR_CODE
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject


@AndroidEntryPoint
class SmsStatusSentReceiver : BroadcastReceiver() {

    @Inject
    lateinit var messageRepository: MessageRepository

    override fun onReceive(context: Context, intent: Intent) {
        backgroundScope.launch {
            updateAndroidDatabase(context, intent)
            updateAppDatabase(context, intent, resultCode)
        }
    }

    private fun updateAndroidDatabase(context: Context, intent: Intent) {
        val messageUri: Uri? = intent.data
        val resultCode = resultCode
        val type = if (resultCode == Activity.RESULT_OK) {
            Sms.MESSAGE_TYPE_SENT
        } else {
            Sms.MESSAGE_TYPE_FAILED
        }
        context.updateSmsMessageSendingStatus(messageUri, type)
        context.maybeShowErrorToast(
            resultCode = resultCode,
            errorCode = intent.getIntExtra(EXTRA_ERROR_CODE, NO_ERROR_CODE)
        )
    }

    private fun updateAppDatabase(
        context: Context,
        intent: Intent,
        receiverResultCode: Int
    ) {
        val messageUri = intent.data
        if (messageUri != null) {
            val messageId = messageUri.lastPathSegment?.toLong() ?: 0L
            backgroundScope.launch {
                val type = if (receiverResultCode == Activity.RESULT_OK) {
                    Sms.MESSAGE_TYPE_SENT
                } else {
                    showSendingFailedNotification(context, messageId)
                    Sms.MESSAGE_TYPE_FAILED
                }

                messageRepository.updateMessagesTypeWithCursor(messageId, type,Sms.STATUS_COMPLETE)
                delay(500)
                EventBus.getDefault().post(UpdateLastSendMessageStatus(messageId,type,Sms.STATUS_COMPLETE))
                EventBus.getDefault().post(RefreshMessages)
            }
        }
    }

    private fun showSendingFailedNotification(context: Context, messageId: Long) {
        mainScope.launch {
            if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                return@launch
            }
            backgroundScope.launch {
                val address = messageRepository.getMessageRecipientAddress(messageId)
                val threadId = context.getThreadId(address)
                val senderName = messageRepository.getNameOfRecipient(address)
                NotificationHelper(context).showSendingFailedNotification(senderName, threadId)
            }

        }
    }
}
