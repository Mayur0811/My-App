package com.messages.common.services

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import com.messages.common.AppJobScheduler
import com.messages.common.receivers.CallReceiver
import com.messages.utils.AppLogger

class AppCallService : JobService() {
    private var callReceiver: CallReceiver? = null

    private var callRec: CallReceiver?
        get() {
            val callReceiver = callReceiver
            if (callReceiver != null) {
                return callReceiver
            }
            return null
        }
        set(callReceiver) {
            this.callReceiver = callReceiver
        }

    override fun onCreate() {
        super.onCreate()
        callRec = CallReceiver()
        val intentFilter = IntentFilter()
        intentFilter.addAction("android.intent.action.PHONE_STATE")
        intentFilter.priority = 1000
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(callRec, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(callRec, intentFilter)
            }
        } catch (unused: Exception) {
            AppLogger.e(message = unused.message.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (this.callReceiver == null) {
                return
            }
            unregisterReceiver(callRec)
        } catch (unused: Exception) {
            AppLogger.e(message = unused.message.toString())
        }
    }

    override fun onStartJob(jobParameters: JobParameters?): Boolean {
        val shouldReschedule = jobParameters?.extras?.getInt("job_scheduler_source", 0) == 1
        jobFinished(jobParameters, shouldReschedule)
        return true
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        try {
            val jobSchedVar = AppJobScheduler()
            val applicationContext = applicationContext
            jobSchedVar.setScheduleJob(applicationContext)
            return false
        } catch (unused: Exception) {
            return false
        }
    }
}
