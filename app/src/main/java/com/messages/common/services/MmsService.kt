package com.messages.common.services

import android.app.Service
import android.content.Intent
import android.os.IBinder


class MmsService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}