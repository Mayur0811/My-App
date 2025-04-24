package com.messages.common.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.messages.common.backgroundScope
import com.messages.data.events.RefreshMessages
import com.messages.data.repository.MessageRepository
import com.messages.extentions.copyToClipboard
import com.messages.utils.ACTION_COPY_OTP
import com.messages.utils.ACTION_MARK_AS_READ
import com.messages.utils.OTP
import com.messages.utils.THREAD_ID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@AndroidEntryPoint
class MarkingAsReadReceiver : BroadcastReceiver() {

    @Inject
    lateinit var messageRepository: MessageRepository


    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (intent.action) {
            ACTION_MARK_AS_READ -> {
                val threadId = intent.getLongExtra(THREAD_ID, 0L)
                notificationManager.cancel(threadId.hashCode())
                backgroundScope.launch {
                    messageRepository.markThreadMessagesReadUnRead(listOf(threadId), true) {}
                    EventBus.getDefault().post(RefreshMessages)
                }
                return
            }

            ACTION_COPY_OTP -> {
                val otpCopy = intent.getStringExtra(OTP)
                val threadId = intent.getLongExtra(THREAD_ID, 0L)
                notificationManager.cancel(threadId.hashCode())
                backgroundScope.launch {
                    if (otpCopy != null) {
                        context.copyToClipboard(otpCopy)
                    }
                }
                return
            }
        }
    }
}
