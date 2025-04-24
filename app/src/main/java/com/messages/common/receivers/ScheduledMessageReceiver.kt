package com.messages.common.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.messages.R
import com.messages.common.backgroundScope
import com.messages.common.mainScope
import com.messages.common.message_utils.sendMessageCompat
import com.messages.data.events.RefreshMessages
import com.messages.data.events.RefreshScheduleMessage
import com.messages.data.repository.MessageRepository
import com.messages.extentions.toast
import com.messages.utils.IS_TEMPORARY_THREAD
import com.messages.utils.SCHEDULED_MESSAGE_ID
import com.messages.utils.THREAD_ID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@AndroidEntryPoint
class ScheduledMessageReceiver : BroadcastReceiver() {

    @Inject
    lateinit var messageRepository: MessageRepository

    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakelock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "simple.messenger:scheduled.message.receiver"
        )
        wakelock.acquire(3000)
        backgroundScope.launch {
            handleIntent(context, intent)
        }
    }

    private fun handleIntent(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra(THREAD_ID, 0L)
        val messageId = intent.getLongExtra(SCHEDULED_MESSAGE_ID, 0L)
        val isTemporaryThread = intent.getBooleanExtra(IS_TEMPORARY_THREAD, false)
        val message = try {
            messageRepository.getScheduledMessageWithId(threadId, messageId)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        try {
            mainScope.launch {
                context.sendMessageCompat(
                    message.body, message.addresses, message.subId,
                    message.attachmentWithMessageModel?.attachments ?: arrayListOf()
                )
            }
            messageRepository.deleteScheduledMessage(messageId)
            if (isTemporaryThread) {
                messageRepository.deleteTemporaryThreadId(threadId)
            }
            EventBus.getDefault().post(RefreshScheduleMessage(messageId))
            EventBus.getDefault().post(RefreshMessages)
        } catch (e: Exception) {
            context.toast(e.message.toString())
        } catch (e: Error) {
            context.toast(
                e.localizedMessage ?: context.getString(R.string.unknown_error_occurred)
            )
        }
    }
}
