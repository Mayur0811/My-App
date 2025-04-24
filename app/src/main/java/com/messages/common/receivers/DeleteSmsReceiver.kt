package com.messages.common.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.messages.common.backgroundScope
import com.messages.data.events.RefreshMessages
import com.messages.data.repository.MessageRepository
import com.messages.utils.IS_MMS
import com.messages.utils.MESSAGE_ID
import com.messages.utils.THREAD_ID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@AndroidEntryPoint
class DeleteSmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var messageRepository: MessageRepository

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val threadId = intent.getLongExtra(THREAD_ID, 0L)
        val messageId = intent.getLongExtra(MESSAGE_ID, 0L)
        val isMms = intent.getBooleanExtra(IS_MMS, false)
        notificationManager.cancel(threadId.hashCode())
        backgroundScope.launch {
            messageRepository.deleteMessage(messageId, isMms)
            messageRepository.syncConversation { }
            EventBus.getDefault().post(RefreshMessages)
        }
    }
}
