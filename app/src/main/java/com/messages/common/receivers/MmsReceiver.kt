package com.messages.common.receivers

import android.content.Context
import android.content.Intent
import com.android.mms.transaction.PushReceiver
import com.messages.utils.AppLogger

class MmsReceiver : PushReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        AppLogger.d("1234", "MmsReceiver -> ${intent?.getIntExtra("subscription", -1)}")
    }
}