package com.messages.common.receivers

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.messages.R
import com.messages.common.backgroundScope
import com.messages.common.getOtp
import com.messages.common.isOtpType
import com.messages.common.mainScope
import com.messages.common.message_utils.getThreadId
import com.messages.data.models.Message
import com.messages.data.pref.AppPreferences
import com.messages.data.repository.MessageRepository
import com.messages.extentions.getContactLetterIcon
import com.messages.extentions.getNotificationBitmap
import com.messages.extentions.getSimSlotForSubscription
import com.messages.extentions.getStringValue
import com.messages.ui.base.BaseActivity
import com.messages.ui.chat.ui.ChatActivity
import com.messages.utils.ACTION_COPY_OTP
import com.messages.utils.ACTION_MARK_AS_READ
import com.messages.utils.ACTION_REPLY
import com.messages.utils.AppLogger
import com.messages.utils.CONVERSATION_TITLE
import com.messages.utils.IS_FROM_NOTIFICATION
import com.messages.utils.LOCK_SCREEN_SENDER
import com.messages.utils.LOCK_SCREEN_SENDER_MESSAGE
import com.messages.utils.NOTIFICATION_CHANNEL
import com.messages.utils.OTP
import com.messages.utils.THREAD_ID
import com.messages.utils.THREAD_NUMBER
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var appPreferences: AppPreferences


    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            var address = ""
            var body = ""
            var subject = ""
            var date = 0L
            var threadId = 0L
            val subscriptionId = intent?.getIntExtra("subscription", -1)

            backgroundScope.launch {
                messages.forEach {
                    address = it.originatingAddress ?: ""
                    subject = it.pseudoSubject
                    body += it.messageBody
                    date = System.currentTimeMillis()
                    threadId = context.getThreadId(address)
                }

                handleMessage(
                    context = context,
                    address = address,
                    subject = subject,
                    body = body,
                    date = date,
                    threadId = threadId,
                    subscriptionId = subscriptionId
                )
            }
        }
    }

    private fun handleMessage(
        context: Context,
        address: String,
        subject: String,
        body: String,
        date: Long,
        read: Int = 0,
        threadId: Long,
        type: Int = Telephony.Sms.MESSAGE_TYPE_INBOX,
        subscriptionId: Int?,
    ) {

        val contact = messageRepository.getContactFromAddress(address)
        val bitmap = context.getNotificationBitmap(
            if (contact.isEmpty()) {
                ""
            } else {
                contact.first().photoUri ?: ""
            }
        )
        mainScope.launch {
            if (!messageRepository.isNumberBlocked(address)) {
                backgroundScope.launch {
                    val newMessage = Message(
                        addresses = listOf(address),
                        subject = subject,
                        body = body,
                        date = date,
                        read = read == 1,
                        threadId = threadId,
                        type = type,
                        subId = subscriptionId ?: -1
                    )
                    var newMessageId = 0L
                    if (!BaseActivity.isSyncInProgress || !isFirstDayFirstSession()) {
                        newMessageId = messageRepository.insertNewSmsToDevice(message = newMessage)
                    }

                    val senderName = messageRepository.getNameOfRecipient(address)

                    val message = Message(
                        messageId = newMessageId,
                        body = body,
                        type = type,
                        date = date / 1000,
                        read = read == 1,
                        threadId = threadId,
                        addresses = listOf(address),
                        seen = false,
                        subject = subject,
                        subId = subscriptionId ?: -1,
                        name = senderName,
                        simSlot = context.getSimSlotForSubscription(subscriptionId ?: -1)
                    )

                    if (!BaseActivity.isSyncInProgress || !isFirstDayFirstSession()) {
                        val conversation = messageRepository.getConversationFromThreadId(threadId)
                        try {
                            messageRepository.insertOrUpdateMessage(message)
                            messageRepository.insertOrUpdateConversation(conversation, message)
                        } catch (ignored: Exception) {
                            AppLogger.e(message = "OnReceive insertOrUpdate -> ${ignored.message}")
                        }
                    }

                    if (messageRepository.checkIsPrivateConversation(threadId)) {
                        if (appPreferences.isPrivateChatNotify) {
                            showMessageNotification(
                                context = context,
                                address = address,
                                body = body,
                                threadId = threadId,
                                bitmap = bitmap,
                                sender = senderName
                            )
                        }
                    } else {
                        showMessageNotification(
                            context = context,
                            address = address,
                            body = body,
                            threadId = threadId,
                            bitmap = bitmap,
                            sender = senderName
                        )
                    }
                }
            }
        }
    }

    private fun showMessageNotification(
        context: Context,
        address: String,
        body: String,
        threadId: Long,
        bitmap: Bitmap?,
        sender: String?,
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)


        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
            .build()

        val name = context.getStringValue(R.string.channel_received_sms)
        val importance = NotificationManager.IMPORTANCE_HIGH
        NotificationChannel(NOTIFICATION_CHANNEL, name, importance).apply {
            setBypassDnd(true)
            enableLights(true)
            setSound(soundUri, audioAttributes)
            enableVibration(true)
            notificationManager.createNotificationChannel(this)
        }

        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra(THREAD_ID, threadId)
            putExtra(IS_FROM_NOTIFICATION, true)
            putExtra(CONVERSATION_TITLE, sender)
            putExtra(THREAD_ID, threadId)
            putExtra(THREAD_NUMBER, address)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            threadId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val summaryText = context.getStringValue(R.string.new_message)
        val markAsReadIntent = Intent(context, MarkingAsReadReceiver::class.java).apply {
            action = ACTION_MARK_AS_READ
            putExtra(THREAD_ID, threadId)
        }

        val markAsReadPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            markAsReadIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction: NotificationCompat.Action?

        val replyLabel = context.getStringValue(R.string.reply)
        val remoteInput = RemoteInput.Builder(ACTION_REPLY)
            .setLabel(replyLabel)
            .build()

        val replyIntent = Intent(context, ReplySmsReceiver::class.java).apply {
            putExtra(THREAD_ID, threadId)
            putExtra(THREAD_NUMBER, address)
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            context.applicationContext,
            threadId.hashCode(),
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_send,
            replyLabel,
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()


        val largeIcon = bitmap ?: context.getContactLetterIcon(sender)

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL).apply {
            when (appPreferences.lockScreenNotify) {
                LOCK_SCREEN_SENDER_MESSAGE -> {
                    setContentTitle(sender)
                    setLargeIcon(largeIcon)
                    setContentText(body)
                }

                LOCK_SCREEN_SENDER -> {
                    setContentTitle(sender)
                    setLargeIcon(largeIcon)
                }
            }

            setSmallIcon(R.mipmap.ic_launcher)
            setStyle(NotificationCompat.BigTextStyle().setSummaryText(summaryText).bigText(body))
            setContentIntent(pendingIntent)
            priority = NotificationCompat.PRIORITY_MAX
            setDefaults(Notification.DEFAULT_LIGHTS)
            setCategory(Notification.CATEGORY_MESSAGE)
            setAutoCancel(true)
            setSound(soundUri, AudioManager.STREAM_NOTIFICATION)
        }

        if (appPreferences.lockScreenNotify == LOCK_SCREEN_SENDER_MESSAGE) {
            builder.addAction(replyAction)
        }

        builder.addAction(
            R.drawable.ic_check,
            context.getStringValue(R.string.mark_as_read),
            markAsReadPendingIntent
        ).setChannelId(NOTIFICATION_CHANNEL)


        if (body.isOtpType()) {
            val otp = body.getOtp()
            if (otp.isNotEmpty()) {
                val copyOTPIntent = Intent(context, MarkingAsReadReceiver::class.java).apply {
                    action = ACTION_COPY_OTP
                    putExtra(OTP, otp)
                    putExtra(THREAD_ID, threadId)
                }
                val copyOTPPendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    copyOTPIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                builder.addAction(
                    R.drawable.ic_copy,
                    context.getString(R.string.copy) + " " + otp,
                    copyOTPPendingIntent
                ).setChannelId(NOTIFICATION_CHANNEL)
            }
        }

        notificationManager.notify(threadId.toInt(), builder.build())
    }

    private fun isFirstDayFirstSession(): Boolean {
        return appPreferences.appDay == 1 && appPreferences.appSession == 1
    }

}