package com.messages.common.receivers

import android.annotation.SuppressLint
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SubscriptionManager
import com.messages.R
import com.messages.common.backgroundScope
import com.messages.common.mainScope
import com.messages.common.message_utils.NotificationHelper
import com.messages.common.message_utils.sendMessageCompat
import com.messages.data.pref.AppPreferences
import com.messages.data.repository.MessageRepository
import com.messages.extentions.getNotificationBitmap
import com.messages.extentions.getStringValue
import com.messages.extentions.normalizeString
import com.messages.extentions.toast
import com.messages.utils.ACTION_REPLY
import com.messages.utils.THREAD_ID
import com.messages.utils.THREAD_NUMBER
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReplySmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var messageRepository: MessageRepository

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val address = intent.getStringExtra(THREAD_NUMBER)
        val threadId = intent.getLongExtra(THREAD_ID, 0L)
        var body =
            RemoteInput.getResultsFromIntent(intent)?.getCharSequence(ACTION_REPLY)?.toString() ?: return

        body = if (appPreferences.removeAccents) body.normalizeString() else body

        val subscriptionManager =
            context.getSystemService(SubscriptionManager::class.java) as SubscriptionManager

        if (address != null) {
            var subscriptionId: Int? = null
            val availableSIMs = subscriptionManager.activeSubscriptionInfoList
            if ((availableSIMs?.size ?: 0) > 1) {
                val currentSIMCardIndex = 1
                val wantedId = availableSIMs?.getOrNull(currentSIMCardIndex)
                if (wantedId != null) {
                    subscriptionId = wantedId.subscriptionId
                }
            }

            backgroundScope.launch {
                var messageId = 0L
                try {
                    context.sendMessageCompat(body, arrayListOf(address), subscriptionId, emptyList())
                    messageRepository.syncMessages(threadId){}
                    messageRepository.syncConversation { }
                } catch (e: Exception) {
                    context.toast(
                        String.format(
                            context.getStringValue(R.string.an_error_occurred),
                            e.message
                        )
                    )
                }

                val contact = messageRepository.getContactFromAddress(address)
                val bitmap = context.getNotificationBitmap(
                    if (contact.isEmpty()) {
                        ""
                    } else {
                        contact.first().photoUri ?: ""
                    }
                )
                mainScope.launch {
                    NotificationHelper(context).showMessageNotification(
                        messageId,
                        address,
                        body,
                        threadId,
                        bitmap,
                        sender = null,
                        alertOnlyOnce = true
                    )
                }

                messageRepository.markThreadMessagesReadUnRead(listOf(threadId), true) {}

            }
        }
    }
}