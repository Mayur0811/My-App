package com.messages.common.message_utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import com.messages.common.receivers.ScheduledMessageReceiver
import com.messages.data.models.Message
import com.messages.utils.IS_TEMPORARY_THREAD
import com.messages.utils.SCHEDULED_MESSAGE_ID
import com.messages.utils.THREAD_ID

fun Context.getScheduleSendPendingIntent(
    message: Message,
    isTemporaryThread: Boolean
): PendingIntent {
    val intent = Intent(this, ScheduledMessageReceiver::class.java)
    intent.putExtra(THREAD_ID, message.threadId)
    intent.putExtra(SCHEDULED_MESSAGE_ID, message.messageId)
    intent.putExtra(IS_TEMPORARY_THREAD, isTemporaryThread)

    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    return PendingIntent.getBroadcast(this, message.id.toInt(), intent, flags)
}

fun Context.scheduleMessage(message: Message, isTemporaryThread: Boolean) {
    val pendingIntent = getScheduleSendPendingIntent(message, isTemporaryThread)
    val triggerAtMillis = message.date * 1000

    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    AlarmManagerCompat.setExactAndAllowWhileIdle(
        alarmManager,
        AlarmManager.RTC_WAKEUP,
        triggerAtMillis,
        pendingIntent
    )
}

fun Context.cancelScheduleSendPendingIntent(messageId: Long) {
    val intent = Intent(this, ScheduledMessageReceiver::class.java)
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    PendingIntent.getBroadcast(this, messageId.toInt(), intent, flags).cancel()
}
