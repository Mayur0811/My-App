package com.messages.common.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.messages.common.AppJobScheduler

class AppBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            AppJobScheduler().setScheduleJob(context)
        }
    }
}
