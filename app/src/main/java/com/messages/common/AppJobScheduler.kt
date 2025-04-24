package com.messages.common

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.PersistableBundle
import com.messages.common.services.AppCallService
import com.messages.utils.AppLogger

class AppJobScheduler {

    fun setScheduleJob(context: Context) {
        if (PermissionManager(context).hasReadPhoneState()) {
            val systemService = context.getSystemService(Context.JOB_SCHEDULER_SERVICE)
            val jobScheduler = systemService as JobScheduler
            val persistableBundle = PersistableBundle()
            persistableBundle.putInt("job_scheduler_source", 1)
            val minimumLatency = JobInfo.Builder(
                666, ComponentName(
                    context,
                    AppCallService::class.java
                )
            ).setExtras(persistableBundle).setPersisted(true).setMinimumLatency(0L)
            minimumLatency.setRequiresBatteryNotLow(true)
            if (jobScheduler.allPendingJobs.size > 50) {
                for (info in jobScheduler.allPendingJobs) {
                    AppLogger.w("ScheduledJobTag", "job = $info")
                }
                jobScheduler.cancelAll()
            }
            val jobInfo = jobScheduler.getPendingJob(666)
            if (jobInfo != null) {
                jobScheduler.cancel(666)
            }
            jobScheduler.schedule(minimumLatency.build())
        }
    }
}
