package com.messages.common.services

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import com.klinker.android.send_message.Settings
import com.messages.common.message_utils.sendMessageCompat

class SmsSendService :Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (intent == null) {
                return START_NOT_STICKY
            }

            val number = Uri.decode(
                intent.dataString?.removePrefix("sms:")?.removePrefix("smsto:")?.removePrefix("mms")
                    ?.removePrefix("mmsto:")?.trim()
            )
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!text.isNullOrEmpty()) {
                val addresses = arrayListOf(number)
                val subId = Settings.DEFAULT_SUBSCRIPTION_ID
                sendMessageCompat(text, addresses, subId, emptyList())
            }
        } catch (ignored: Exception) {
        }
        return super.onStartCommand(intent, flags, startId)
    }
}